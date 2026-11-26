import CoreLocation
import SwiftUI
import UIKit

// Màn hình xe (CarPlay) — chỉ iPhone, app cài bằng TrollStore.
//
// Apple chỉ cho app lên CarPlay bằng giao diện mẫu (nghe nhạc, dẫn đường…), không cho phát video. App cài bằng TrollStore tự
// khai quyền `SBStarkCapable` (scripts/package.sh) và cảnh `UIWindowSceneSessionRoleCarPlay` (project.yml, khoá `~iphone`) → có
// một cửa sổ UIKit thường trên màn xe, chạm được — cách của CarTube (github.com/Avangelista/CarTube).
//
// Giao diện chính (RootView) chỉ chạy ở MỘT chỗ: cắm xe thì lên màn xe, điện thoại hiện bảng điều khiển (CarPhonePanel);
// «Dùng trên điện thoại» thì ngược lại. Đổi chỗ thì đóng trình phát ở màn cũ (lưu chỗ xem) rồi mở lại ở màn mới.
// Xe đang chạy (tốc độ GPS) thì che màn xe, chỉ phát tiếng. Ô nhập chữ trên màn xe dùng bàn phím riêng (CarKeyboard,
// UI/CarInput.m): bàn phím hệ thống hiện trên màn xe bị lỗi.

@MainActor
final class CarScreen: NSObject, ObservableObject, CLLocationManagerDelegate {
    static let shared = CarScreen()
    static let sessionRole = UISceneSession.Role(rawValue: "UIWindowSceneSessionRoleCarPlay")

    /// Cửa sổ app trên màn xe (nil: chưa cắm xe / chưa mở app trên CarPlay).
    private(set) var window: UIWindow?
    private var overlay: CarOverlayWindow?
    /// Lớp phủ (bàn phím riêng / màn «xe đang chạy») — cho ảnh chụp của cổng gỡ lỗi.
    var overlayWindow: UIWindow? { overlay }
    /// Màn xe đang nối với app.
    @Published private(set) var connected = false
    /// Cảnh trên màn xe đang hiện (không bị app CarPlay khác che).
    @Published private(set) var carVisible = false
    /// Người dùng chọn «Dùng trên điện thoại» — lần cắm xe sau lại lên màn xe.
    @Published var onPhone = false {
        didSet { if onPhone != oldValue && connected { reconcile() } }
    }
    /// Giao diện chính đang ở màn xe (đổi sau khi đã đóng trình phát ở màn cũ).
    @Published private(set) var uiOnCar = false
    /// Xe đang chạy: màn xe bị che, chỉ phát tiếng.
    @Published private(set) var moving = false
    @Published private(set) var locationDenied = false
    @Published private(set) var phoneDimmed = false
    // Phim phát bằng trình phát web (server nhúng mã hoá) chỉ ra hình trên màn xe khi màn iPhone bật — WebKit bỏ khung hình lúc
    // màn điện thoại tắt (đo 25/11/2026). Đã từng có dòng nhắc «Bật màn iPhone để thấy hình»; anh bảo bỏ (25/11/2026): bật màn
    // một lần rồi tắt thì màn xe đã có hình, dòng nhắc chen ngang gây khó chịu. Phim .m3u8 dùng AVPlayer (NativeFilm) không bị.
    /// Tốc độ GPS gần nhất (km/h, -1 = chưa biết). Không @Published: đổi mỗi giây, bảng điều khiển tự đọc lại.
    private(set) var speedKmh: Double = -1
    /// Cổng gỡ lỗi: giả xe đang chạy / đứng (nil = theo GPS).
    var fakeMoving: Bool? {
        didSet { updateMoving() }
    }

    private var switching = false
    private let loc = CLLocationManager()
    private var gpsMoving = false
    private var fastSince: Date?
    private var slowSince: Date?
    private var savedBrightness: CGFloat?

    static func isCar(_ scene: UIScene?) -> Bool { scene?.session.role == sessionRole }

    /// Cửa sổ đang hiện giao diện chính: màn xe khi đang chiếu lên xe, không thì cửa sổ chính của điện thoại / iPad / Mac.
    static var mainWindow: UIWindow? {
        if shared.uiOnCar, let w = shared.window { return w }
        return phoneWindow
    }

    /// Cửa sổ chính của điện thoại / iPad / Mac (không tính màn xe).
    static var phoneWindow: UIWindow? {
        let windows = phoneScenes.flatMap { $0.windows }
        return windows.first { $0.isKeyWindow } ?? windows.first
    }

    /// Cảnh để mở hộp hỏi / báo nằm trên mọi thứ (AppModel.askUpdate, TvAlert).
    static var mainScene: UIWindowScene? {
        if shared.uiOnCar, let s = shared.window?.windowScene { return s }
        let scenes = phoneScenes
        return scenes.first { $0.activationState == .foregroundActive } ?? scenes.first
    }

    private static var phoneScenes: [UIWindowScene] {
        UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }.filter { !isCar($0) }
    }

    /// Màn điện thoại đừng tự tắt khi đang nối xe (khoá máy thì màn xe có thể đứng hình) — trình phát gọi khi đóng.
    static var keepPhoneAwake: Bool { shared.connected }

    // MARK: - Cảnh CarPlay nối / ngắt

    fileprivate func attach(_ w: UIWindow) {
        window = w
        connected = true
        let s = w.screen
        Log.d("Car", "màn xe nối: \(Int(w.bounds.width))×\(Int(w.bounds.height)) pt, @\(s.scale)x, "
              + "vùng an toàn \(w.safeAreaInsets)")
        UIApplication.shared.isIdleTimerDisabled = true
        CarKeyboard.shared.install()
        startMotion()
        reconcile()
    }

    fileprivate func setVisible(_ v: Bool) {
        carVisible = v
        Log.d("Car", v ? "màn xe: app hiện" : "màn xe: app bị che / nằm nền")
        // Hộp hỏi quyền vị trí bị tắt ngang (khoá máy…) thì hỏi lại khi app hiện lên màn xe
        if v && loc.authorizationStatus == .notDetermined { loc.requestWhenInUseAuthorization() }
    }

    fileprivate func detach() {
        Log.d("Car", "màn xe ngắt")
        connected = false
        carVisible = false
        stopMotion()
        undimPhone()
        CarKeyboard.shared.hide()
        overlay?.isHidden = true
        if !Self.keepPhoneAwakeForPlayer { UIApplication.shared.isIdleTimerDisabled = false }
        // Rút xe: giao diện về điện thoại, phim đang xem trên màn xe đóng lại (đã lưu chỗ xem) — không tự phát tiếp trên điện
        // thoại (có thể đang khoá máy, tự nhiên có tiếng thì phiền). Lần cắm sau lại lên màn xe.
        onPhone = false
        reconcile(reopen: false)
    }

    private static var keepPhoneAwakeForPlayer: Bool {
        let top = Router.topController
        return top is PlayerController || top is LiveController
    }

    // MARK: - Đổi chỗ giao diện chính

    private func reconcile(reopen: Bool = true) {
        let want = connected && !onPhone && window != nil
        guard want != uiOnCar, !switching else {
            if !connected && !switching && !uiOnCar { releaseWindow() }
            return
        }
        switching = true
        let again = closePresented(reopen: reopen)
        AppModel.shared.sheet = nil
        CarKeyboard.shared.hide()
        DispatchQueue.main.asyncAfter(deadline: .now() + (again == nil ? 0.2 : 0.8)) { [self] in
            uiOnCar = want
            // Cửa sổ đang hiện giao diện chính làm «cửa sổ chính» (key): SwiftUI chỉ theo dõi ô đang gõ / phím tắt ở cửa sổ này
            if want { window?.makeKey() } else { Self.phoneWindow?.makeKey() }
            updateOverlay()
            Log.d("Car", want ? "giao diện chính → màn xe" : "giao diện chính → điện thoại")
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) { [self] in
                again?()
                switching = false
                reconcile()
            }
        }
    }

    private func releaseWindow() {
        overlay?.isHidden = true
        overlay = nil
        window = nil
    }

    /// Đóng trình phát / trình duyệt đang mở ở màn hiện tại; trả về việc làm sau khi đổi chỗ (nil: không có gì đang mở).
    private func closePresented(reopen: Bool) -> (() -> Void)? {
        var chain: [UIViewController] = []
        var vc = Self.mainWindow?.rootViewController
        while let p = vc?.presentedViewController {
            chain.append(p)
            vc = p
        }
        if chain.isEmpty { return nil }
        if let p = chain.first(where: { $0 is PlayerController }) as? PlayerController {
            let m = p.model
            let film = m.filmUrl, ep = m.episodeUrl.isEmpty ? m.startEpUrl : m.episodeUrl
            if p.presentedViewController != nil { p.dismiss(animated: false) }
            m.close() // lưu chỗ xem, đóng màn trình phát
            if !connected { m.teardown() } // cảnh màn xe đang bị gỡ: màn có thể không kịp đóng — dừng tiếng ngay
            Log.d("Car", "đóng phim ở màn cũ: \(m.title)")
            return reopen ? { Router.play(filmUrl: film, epUrl: ep) } : {}
        }
        if let l = chain.first(where: { $0 is LiveController }) as? LiveController {
            let filter = l.model.filter, index = l.model.index
            if l.presentedViewController != nil { l.dismiss(animated: false) }
            l.model.close()
            return reopen ? { Router.playLive(filter, index) } : {}
        }
        if let b = chain.first(where: { $0 is BrowserController }) {
            b.presentingViewController?.dismiss(animated: false)
        }
        return {}
    }

    // MARK: - Tìm phim từ điện thoại (kết quả hiện trên màn xe)

    func search(_ q: String) {
        let app = AppModel.shared
        guard !app.gateUp else { return }
        app.sheet = nil
        // Đang ở mục TubeMini: tìm video YouTube
        if app.active?.kind == .tube {
            TubeModel.shared.search(q)
            return
        }
        if let e = app.menu.first(where: { $0.kind == .search }) { app.select(e) }
        app.searchFocus = false
        app.searchText = q
        app.doSearch()
    }

    // MARK: - Lớp phủ trên màn xe: bàn phím riêng / màn «xe đang chạy»

    func updateOverlay() {
        guard let w = window, let scene = w.windowScene else { return }
        let cover = uiOnCar && moving
        let keys = uiOnCar && CarKeyboard.shared.shown && !moving
        (w.rootViewController as? CarHostController)?.keyboardHeight = keys ? keyboardHeight : 0
        guard cover || keys else {
            overlay?.isHidden = true
            return
        }
        let o: CarOverlayWindow
        if let existing = overlay {
            o = existing
        } else {
            o = CarOverlayWindow(windowScene: scene)
            o.windowLevel = .alert + 1
            o.backgroundColor = .clear
            o.overrideUserInterfaceStyle = .dark
            let host = UIHostingController(rootView: CarOverlayView())
            host.view.backgroundColor = .clear
            o.rootViewController = host
            overlay = o
        }
        let b = w.bounds, safe = w.safeAreaInsets
        if cover {
            o.frame = b
        } else {
            let h = keyboardHeight
            o.frame = CGRect(x: b.minX + safe.left, y: b.maxY - safe.bottom - h, width: b.width - safe.left - safe.right, height: h)
        }
        o.isHidden = false
    }

    var keyboardHeight: CGFloat {
        let h = (window?.bounds.height ?? 480) - (window?.safeAreaInsets.top ?? 0) - (window?.safeAreaInsets.bottom ?? 0)
        return min(330, max(190, h * 0.5))
    }

    // MARK: - Xe chạy / đứng (tốc độ GPS)

    private func startMotion() {
        loc.delegate = self
        loc.desiredAccuracy = kCLLocationAccuracyBestForNavigation
        loc.activityType = .automotiveNavigation
        loc.distanceFilter = kCLDistanceFilterNone
        switch loc.authorizationStatus {
        case .notDetermined: loc.requestWhenInUseAuthorization()
        case .denied, .restricted: locationDenied = true
        default: break
        }
        loc.startUpdatingLocation()
    }

    private func stopMotion() {
        loc.stopUpdatingLocation()
        fastSince = nil
        slowSince = nil
        speedKmh = -1
        gpsMoving = false
        updateMoving()
    }

    nonisolated func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let l = locations.last else { return }
        let kmh = l.speed >= 0 ? l.speed * 3.6 : -1
        Task { @MainActor in CarScreen.shared.gotSpeed(kmh) }
    }

    nonisolated func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        let st = manager.authorizationStatus
        Task { @MainActor in
            CarScreen.shared.locationDenied = st == .denied || st == .restricted
            Log.d("Car", "quyền vị trí: \(st.rawValue)")
        }
    }

    nonisolated func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        let text = error.localizedDescription
        Task { @MainActor in Log.d("Car", "vị trí lỗi: \(text)") }
    }

    /// Chạy: ≥ 10 km/h trong 2 giây; đứng: < 4 km/h trong 6 giây. Ở giữa / mất GPS (hầm) thì giữ trạng thái cũ.
    private func gotSpeed(_ kmh: Double) {
        guard connected else { return }
        speedKmh = kmh
        let now = Date()
        if kmh >= 10 {
            slowSince = nil
            let since = fastSince ?? now
            fastSince = since
            if now.timeIntervalSince(since) >= 2 { gpsMoving = true }
        } else if kmh >= 0 && kmh < 4 {
            fastSince = nil
            let since = slowSince ?? now
            slowSince = since
            if now.timeIntervalSince(since) >= 6 { gpsMoving = false }
        } else {
            fastSince = nil
            slowSince = nil
        }
        updateMoving()
    }

    private func updateMoving() {
        let m = connected && (fakeMoving ?? gpsMoving)
        guard m != moving else { return }
        moving = m
        Log.d("Car", m ? "xe đang chạy (\(Int(speedKmh)) km/h): che hình" : "xe dừng: hiện hình")
        if m { CarKeyboard.shared.hide() }
        updateOverlay()
    }

    // MARK: - Tắt sáng màn điện thoại (đỡ tốn pin, màn không khoá)

    func dimPhone() {
        guard !phoneDimmed else { return }
        savedBrightness = UIScreen.main.brightness
        UIScreen.main.brightness = 0
        phoneDimmed = true
    }

    func undimPhone() {
        guard phoneDimmed else { return }
        phoneDimmed = false
        if let b = savedBrightness { UIScreen.main.brightness = b }
        savedBrightness = nil
    }

    /// Cho cổng gỡ lỗi.
    var debugInfo: [String: Any] {
        var o: [String: Any] = [
            "connected": connected, "carVisible": carVisible, "onPhone": onPhone, "uiOnCar": uiOnCar, "switching": switching,
            "moving": moving, "fakeMoving": fakeMoving.map { $0 ? "1" : "0" } ?? "auto", "speedKmh": speedKmh,
            "locationAuth": loc.authorizationStatus.rawValue, "phoneDimmed": phoneDimmed,
            "keyboard": CarKeyboard.shared.shown, "carKeyboardHook": CBTCarKeyboardEnabled(),
            "manifest": Bundle.main.object(forInfoDictionaryKey: "UIApplicationSceneManifest") != nil,
            "multiScene": UIApplication.shared.supportsMultipleScenes,
            "scenes": UIApplication.shared.connectedScenes.map { "\($0.session.role.rawValue):\($0.activationState.rawValue)" },
        ]
        if let w = window {
            let i = w.safeAreaInsets
            o["window"] = [Int(w.bounds.width), Int(w.bounds.height)]
            o["scale"] = w.screen.scale
            o["safeArea"] = [i.top, i.left, i.bottom, i.right].map { Double($0) }
            o["idiom"] = w.traitCollection.userInterfaceIdiom.rawValue
            o["sizeClass"] = [w.traitCollection.horizontalSizeClass.rawValue, w.traitCollection.verticalSizeClass.rawValue]
            o["overlay"] = overlay.map { $0.isHidden ? "ẩn" : "\($0.frame)" } ?? "chưa có"
            o["carKeyWindow"] = w.isKeyWindow
            o["phoneKeyWindow"] = Self.phoneWindow?.isKeyWindow ?? false
            o["searchText"] = AppModel.shared.searchText
        }
        return o
    }
}

// MARK: - Cảnh CarPlay

/// Lớp cảnh của màn xe — tên lớp ghi trong Info.plist (project.yml: UIApplicationSceneManifest~iphone).
@MainActor
final class CarSceneDelegate: UIResponder, UIWindowSceneDelegate {
    var window: UIWindow?

    func scene(_ scene: UIScene, willConnectTo session: UISceneSession, options connectionOptions: UIScene.ConnectionOptions) {
        guard let ws = scene as? UIWindowScene else { return }
        let w = UIWindow(windowScene: ws)
        w.overrideUserInterfaceStyle = .dark
        w.backgroundColor = UIColor(Theme.bg)
        w.rootViewController = CarHostController()
        w.makeKeyAndVisible()
        window = w
        CarScreen.shared.attach(w)
    }

    func sceneWillEnterForeground(_ scene: UIScene) { CarScreen.shared.setVisible(true) }
    func sceneDidEnterBackground(_ scene: UIScene) { CarScreen.shared.setVisible(false) }

    func sceneDidDisconnect(_ scene: UIScene) {
        CarScreen.shared.detach()
        window = nil
    }
}

/// Gốc cửa sổ màn xe: giao diện SwiftUI, coi như iPhone xoay ngang (màn xe báo kiểu máy «carPlay» — SwiftUI không có kiểu vẽ
/// riêng cho nó); chừa chỗ cho bàn phím riêng khi đang gõ.
final class CarHostController: UIViewController {
    private let host = UIHostingController(rootView: CarRoot())

    var keyboardHeight: CGFloat = 0 {
        didSet { host.additionalSafeAreaInsets.bottom = keyboardHeight }
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor(Theme.bg)
        addChild(host)
        host.view.frame = view.bounds
        host.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        host.view.backgroundColor = .clear
        view.addSubview(host.view)
        host.didMove(toParent: self)
        setOverrideTraitCollection(UITraitCollection(traitsFrom: [
            UITraitCollection(userInterfaceIdiom: .phone),
            UITraitCollection(horizontalSizeClass: .regular),
            UITraitCollection(verticalSizeClass: .compact),
        ]), forChild: host)
    }
}

/// Cửa sổ lớp phủ trên màn xe — không giành «cửa sổ chính» (ô đang gõ giữ con trỏ khi bấm phím).
final class CarOverlayWindow: UIWindow {
    override var canBecomeKey: Bool { false }
}

// MARK: - Giao diện

/// Giao diện chính của app — chạy ở một chỗ tại một thời điểm: cửa sổ điện thoại / iPad / Mac, hoặc màn xe.
struct MainUI: View {
    var body: some View {
        RootView()
            .environmentObject(AppModel.shared)
            .preferredColorScheme(.dark)
            .modifier(DisclaimerGate())
    }
}

/// Nội dung cửa sổ điện thoại / iPad / Mac: giao diện chính, hoặc bảng điều khiển khi giao diện đang ở màn xe.
struct PhoneRoot: View {
    @ObservedObject private var car = CarScreen.shared

    var body: some View {
        if car.uiOnCar {
            CarPhonePanel()
        } else {
            MainUI()
        }
    }
}

/// Nội dung cửa sổ màn xe.
struct CarRoot: View {
    @ObservedObject private var car = CarScreen.shared

    var body: some View {
        ZStack {
            Theme.bg.ignoresSafeArea()
            if car.uiOnCar {
                MainUI()
            } else {
                CarIdleView()
            }
        }
    }
}

/// Màn xe khi giao diện đang ở điện thoại.
private struct CarIdleView: View {
    var body: some View {
        VStack(spacing: 18) {
            Image("Logo")
                .resizable()
                .frame(width: 80, height: 80)
                .clipShape(RoundedRectangle(cornerRadius: 18))
            Text("MTV đang dùng trên điện thoại")
                .appFont(20, .bold)
                .foregroundColor(Theme.text)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
            Button {
                CarScreen.shared.onPhone = false
            } label: {
                Label("Chiếu lên màn xe", systemImage: "car.fill")
                    .appFont(22, .semibold)
                    .foregroundColor(Theme.onBrand)
                    .padding(.horizontal, 30)
                    .padding(.vertical, 16)
                    .background(Capsule().fill(Theme.brand))
            }
            .buttonStyle(.plain)
        }
        .padding(30)
    }
}

/// Lớp phủ trên màn xe: màn «xe đang chạy» (che hết) hoặc bàn phím riêng.
private struct CarOverlayView: View {
    @ObservedObject private var car = CarScreen.shared
    @ObservedObject private var keys = CarKeyboard.shared

    var body: some View {
        if car.moving {
            DrivingCover()
        } else if keys.shown {
            CarKeyboardView()
        }
    }
}

/// Xe đang chạy: không hiện hình, chỉ phát tiếng (điều khiển cỡ lớn).
private struct DrivingCover: View {
    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            VStack(spacing: 12) {
                HStack(spacing: 12) {
                    Image(systemName: "car.fill").appFont(24, .semibold)
                    Text("Xe đang chạy — chỉ phát tiếng").appFont(22, .bold).lineLimit(1).minimumScaleFactor(0.6)
                }
                .foregroundColor(Theme.text)
                Text("Dừng xe để xem hình.")
                    .appFont(16)
                    .foregroundColor(Theme.dim)
                CarNowPlaying(big: true)
                    .padding(.top, 4)
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
        }
    }
}

/// Phim / kênh đang phát và nút điều khiển (bảng điều khiển trên điện thoại, màn «xe đang chạy»).
struct CarNowPlaying: View {
    let big: Bool

    var body: some View {
        // Trình phát mở / đóng không báo ra ngoài: xem lại mỗi giây
        TimelineView(.periodic(from: .now, by: 1)) { ctx in
            CarNowPlayingContent(big: big, at: ctx.date)
        }
    }
}

private struct CarNowPlayingContent: View {
    let big: Bool
    let at: Date

    var body: some View {
        if let m = PlayerModel.current {
            CarFilmControls(m: m, big: big)
        } else if let l = LiveModel.current {
            CarLiveControls(m: l, big: big)
        } else {
            Text("Chưa phát phim / kênh nào.")
                .appFont(big ? 18 : 15)
                .foregroundColor(Theme.dim)
                .frame(maxWidth: .infinity)
        }
    }
}

private struct CarFilmControls: View {
    @ObservedObject var m: PlayerModel
    let big: Bool

    var body: some View {
        let playing = m.state?.paused == false
        let size: CGFloat = big ? 64 : 50
        VStack(spacing: big ? 16 : 12) {
            Text(m.title.isEmpty ? "Đang mở phim…" : m.title)
                .appFont(big ? 22 : 17, .semibold)
                .foregroundColor(Theme.text)
                .lineLimit(2)
                .multilineTextAlignment(.center)
            if let s = m.state, s.d > 0 {
                Text("\(clock(s.t)) / \(clock(s.d))")
                    .appFont(big ? 16 : 13, mono: true)
                    .foregroundColor(Theme.dim)
            }
            HStack(spacing: big ? 26 : 16) {
                CarRoundButton(icon: "gobackward.10", label: "Lùi 10 giây", size: size) { m.seekBy(-10) }
                CarRoundButton(icon: playing ? "pause.fill" : "play.fill", label: playing ? "Dừng" : "Phát", size: size * 1.25,
                               primary: true) { m.togglePlay() }
                CarRoundButton(icon: "goforward.10", label: "Tới 10 giây", size: size) { m.seekBy(10) }
                CarRoundButton(icon: "forward.end.fill", label: "Tập sau", size: size) { m.next() }
            }
        }
        .frame(maxWidth: .infinity)
    }

    private func clock(_ t: Double) -> String {
        let s = max(0, Int(t))
        return s >= 3600 ? String(format: "%d:%02d:%02d", s / 3600, s / 60 % 60, s % 60) : String(format: "%d:%02d", s / 60, s % 60)
    }
}

private struct CarLiveControls: View {
    @ObservedObject var m: LiveModel
    let big: Bool

    var body: some View {
        let size: CGFloat = big ? 64 : 50
        VStack(spacing: big ? 16 : 12) {
            Text(m.ch?.n ?? "Đang mở kênh…")
                .appFont(big ? 22 : 17, .semibold)
                .foregroundColor(Theme.text)
                .lineLimit(2)
                .multilineTextAlignment(.center)
            if let t = m.icy, !t.isEmpty {
                Text(t).appFont(big ? 16 : 13).foregroundColor(Theme.dim).lineLimit(1)
            }
            HStack(spacing: big ? 26 : 16) {
                CarRoundButton(icon: "backward.end.fill", label: "Kênh trước", size: size) { m.step(-1) }
                CarRoundButton(icon: m.paused ? "play.fill" : "pause.fill", label: m.paused ? "Phát" : "Dừng", size: size * 1.25,
                               primary: true) { m.togglePlay() }
                CarRoundButton(icon: "forward.end.fill", label: "Kênh sau", size: size) { m.step(1) }
            }
        }
        .frame(maxWidth: .infinity)
    }
}

private struct CarRoundButton: View {
    let icon: String
    let label: String
    let size: CGFloat
    var primary = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: icon)
                .font(.system(size: size * 0.4, weight: .semibold))
                .foregroundColor(primary ? Theme.onBrand : Theme.text)
                .frame(width: size, height: size)
                .background(Circle().fill(primary ? Theme.brand : Theme.surface2))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
    }
}

/// Bảng điều khiển trên điện thoại khi giao diện chính đang ở màn xe.
private struct CarPhonePanel: View {
    @ObservedObject private var car = CarScreen.shared
    @State private var query = ""
    @FocusState private var typing: Bool

    var body: some View {
        ZStack {
            Theme.bg.ignoresSafeArea()
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    HStack(spacing: 14) {
                        Image("Logo")
                            .resizable()
                            .frame(width: 56, height: 56)
                            .clipShape(RoundedRectangle(cornerRadius: 13))
                        VStack(alignment: .leading, spacing: 3) {
                            Text("Đang chiếu lên màn xe").appFont(22, .bold).foregroundColor(Theme.text)
                            Text("Chạm trên màn hình xe để dùng app.").appFont(14).foregroundColor(Theme.dim)
                        }
                        Spacer(minLength: 0)
                    }
                    if car.moving {
                        note("Xe đang chạy: màn xe chỉ phát tiếng, không hiện hình.", icon: "car.fill")
                    }
                    if car.locationDenied {
                        note("Chưa cho phép vị trí nên app không biết xe đang chạy hay dừng. Vào Cài đặt → MTV → Vị trí → "
                             + "«Khi dùng app».", icon: "location.slash")
                    }
                    card { CarNowPlaying(big: false) }
                    card {
                        VStack(alignment: .leading, spacing: 10) {
                            Text("Tìm phim").appFont(16, .semibold).foregroundColor(Theme.text)
                            HStack(spacing: 10) {
                                TextField("Tên phim — kết quả hiện trên màn xe", text: $query)
                                    .focused($typing)
                                    // Cửa sổ màn xe đang là cửa sổ chính: chạm ô này thì trả lại cho điện thoại (bàn phím hiện)
                                    .simultaneousGesture(TapGesture().onEnded { CarScreen.phoneWindow?.makeKey() })
                                    .submitLabel(.search)
                                    .onSubmit(search)
                                    .disableAutocorrection(true)
                                    .foregroundColor(Theme.text)
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 10)
                                    .background(RoundedRectangle(cornerRadius: 10).fill(Theme.surface2))
                                Button("Tìm", action: search)
                                    .buttonStyle(.borderedProminent)
                                    .tint(Theme.brand)
                            }
                        }
                    }
                    action("Tắt sáng màn điện thoại", icon: "moon.fill") { car.dimPhone() }
                    action("Dùng trên điện thoại", icon: "iphone") { car.onPhone = true }
                    Text("Phim KKPhim / link .m3u8 và Truyền hình có hình trên màn xe cả khi tắt màn iPhone. Server riêng của trang "
                         + "(mã hoá) cần màn iPhone bật — bấm «Tắt sáng màn điện thoại» cho tối hẳn mà vẫn có hình. Khi đang nối xe, "
                         + "màn điện thoại không tự tắt.")
                        .appFont(13)
                        .foregroundColor(Theme.dim)
                }
                .padding(20)
                .frame(maxWidth: 560)
                .frame(maxWidth: .infinity)
            }
            if car.phoneDimmed {
                Color.black
                    .ignoresSafeArea()
                    .overlay(Text("Chạm để sáng lại").appFont(13).foregroundColor(.white.opacity(0.25)))
                    .onTapGesture { car.undimPhone() }
            }
        }
        .appDynamicType()
        .preferredColorScheme(.dark)
        .onOpenURL { AppModel.shared.openURL($0) } // chạm phim trên widget «Xem tiếp»
    }

    private func search() {
        let q = query.trimmed
        guard !q.isEmpty else { return }
        typing = false
        car.search(q)
    }

    private func card<C: View>(@ViewBuilder _ content: () -> C) -> some View {
        content()
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(RoundedRectangle(cornerRadius: 14).fill(Theme.surface))
    }

    private func note(_ text: String, icon: String) -> some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: icon).appFont(15, .semibold)
            Text(text).appFont(14).fixedSize(horizontal: false, vertical: true)
        }
        .foregroundColor(Theme.onBrand)
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 12).fill(Theme.brand))
    }

    private func action(_ title: String, icon: String, _ run: @escaping () -> Void) -> some View {
        Button(action: run) {
            HStack(spacing: 12) {
                Image(systemName: icon).appFont(17, .semibold).frame(width: 24)
                Text(title).appFont(16, .medium)
                Spacer(minLength: 0)
            }
            .foregroundColor(Theme.text)
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .background(RoundedRectangle(cornerRadius: 12).fill(Theme.surface2))
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Bàn phím riêng của màn xe

extension View {
    /// Ô nhập có .onSubmit: trên màn xe, phím «Tìm / Xong» của bàn phím riêng gọi `action` (gắn cùng .focused(...) của ô đó —
    /// Enter giả không tới được .onSubmit của SwiftUI).
    func carSubmit(_ focused: Bool, _ action: @escaping () -> Void) -> some View {
        onChange(of: focused) { f in
            if f { Task { @MainActor in CarKeyboard.shared.submitAction = action } }
        }
    }
}

@MainActor
final class CarKeyboard: ObservableObject {
    static let shared = CarKeyboard()

    @Published private(set) var shown = false
    @Published var upper = false
    @Published var symbols = false
    @Published private(set) var returnLabel = "Xong"
    private weak var field: UIView?
    private var installed = false
    /// Việc của phím «Tìm / Xong» cho ô đang gõ (View.carSubmit): Enter giả không tới được .onSubmit của SwiftUI.
    var submitAction: (() -> Void)?

    func install() {
        guard !installed else { return }
        installed = true
        CBTInstallCarInput()
        let nc = NotificationCenter.default
        for name in [UITextField.textDidBeginEditingNotification, UITextView.textDidBeginEditingNotification] {
            nc.addObserver(forName: name, object: nil, queue: .main) { n in
                let v = n.object as? UIView
                Task { @MainActor in CarKeyboard.shared.began(v) }
            }
        }
        for name in [UITextField.textDidEndEditingNotification, UITextView.textDidEndEditingNotification] {
            nc.addObserver(forName: name, object: nil, queue: .main) { n in
                let v = n.object as? UIView
                Task { @MainActor in CarKeyboard.shared.ended(v) }
            }
        }
    }

    private func began(_ v: UIView?) {
        guard let v = v, v.isFirstResponder, CarScreen.isCar(v.window?.windowScene), CBTCarKeyboardEnabled() else { return }
        if v.window?.isKeyWindow == false { v.window?.makeKey() }
        field = v
        upper = false
        symbols = false
        let type = (v as? UITextField)?.returnKeyType ?? .default
        returnLabel = v is UITextView ? "Xuống dòng" : type == .search ? "Tìm" : type == .go ? "Đi" : "Xong"
        shown = true
        CarScreen.shared.updateOverlay()
    }

    private func ended(_ v: UIView?) {
        guard let v = v, v === field else { return }
        field = nil
        submitAction = nil
        shown = false
        CarScreen.shared.updateOverlay()
    }

    func hide() {
        let f = field
        field = nil
        if shown {
            shown = false
            CarScreen.shared.updateOverlay()
        }
        f?.resignFirstResponder()
    }

    func type(_ s: String) {
        insert(upper ? s.uppercased() : s)
        if upper { upper = false }
    }

    func back() {
        guard let f = field else { return }
        // Hỏi ô trước như bàn phím hệ thống (xoá vùng đang chọn, hoặc ký tự trước con trỏ)
        if let tf = f as? UITextField, var r = selection(tf) {
            if r.length == 0 {
                guard r.location > 0 else { return }
                r = NSRange(location: r.location - 1, length: 1)
            }
            if tf.delegate?.textField?(tf, shouldChangeCharactersIn: r, replacementString: "") == false { return }
        }
        (f as? UIKeyInput)?.deleteBackward()
        changed()
    }

    private func insert(_ s: String) {
        guard let f = field else { return }
        if let tf = f as? UITextField, let r = selection(tf),
           tf.delegate?.textField?(tf, shouldChangeCharactersIn: r, replacementString: s) == false { return }
        if let tv = f as? UITextView,
           tv.delegate?.textView?(tv, shouldChangeTextIn: tv.selectedRange, replacementText: s) == false { return }
        (f as? UIKeyInput)?.insertText(s)
        changed()
    }

    private func selection(_ tf: UITextField) -> NSRange? {
        guard let r = tf.selectedTextRange else { return nil }
        return NSRange(location: tf.offset(from: tf.beginningOfDocument, to: r.start), length: tf.offset(from: r.start, to: r.end))
    }

    /// Báo ô đã đổi chữ như bàn phím hệ thống — SwiftUI cập nhật biến của TextField / TextEditor.
    private func changed() {
        if let tf = field as? UITextField {
            tf.sendActions(for: .editingChanged)
            NotificationCenter.default.post(name: UITextField.textDidChangeNotification, object: tf)
            tf.delegate?.textFieldDidChangeSelection?(tf)
            // Ô tìm phim / ô tìm TubeMini: nút bên cạnh đọc biến tìm kiếm — chắc chắn cập nhật dù SwiftUI chưa nhận
            let app = AppModel.shared
            let hint = tf.placeholder ?? tf.attributedPlaceholder?.string
            if hint == "Tên phim…" && app.active?.kind == .search {
                app.searchText = tf.text ?? ""
            } else if hint == "Tìm video, kênh" && app.active?.kind == .tube {
                TubeModel.shared.query = tf.text ?? ""
            }
        } else if let tv = field as? UITextView {
            tv.delegate?.textViewDidChange?(tv)
            NotificationCenter.default.post(name: UITextView.textDidChangeNotification, object: tv)
        }
    }

    func submit() {
        guard let f = field else { return }
        if let tf = f as? UITextField {
            let action = submitAction
            let app = AppModel.shared
            let titleBefore = app.title
            // Ô tìm phim (RootView — trên màn xe chỉ có ô này đặt chữ mờ «Tên phim…»): chép chữ sang biến tìm kiếm phòng SwiftUI
            // chưa kịp cập nhật
            let isSearch = (tf.placeholder ?? tf.attributedPlaceholder?.string) == "Tên phim…" && app.active?.kind == .search
            if isSearch, let t = tf.text { app.searchText = t }
            if tf.delegate?.textFieldShouldReturn?(tf) ?? true {
                tf.sendActions(for: .editingDidEndOnExit)
            }
            hide()
            if app.title != titleBefore {
                Log.d("Car", "bàn phím: phím \(returnLabel) — .onSubmit đã chạy")
            } else if let a = action {
                Log.d("Car", "bàn phím: phím \(returnLabel) — chạy việc của carSubmit")
                a()
            } else if isSearch {
                Log.d("Car", "bàn phím: phím \(returnLabel) — tìm thẳng")
                app.doSearch()
            } else if (tf.placeholder ?? tf.attributedPlaceholder?.string) == "Tìm video, kênh" && app.active?.kind == .tube {
                Log.d("Car", "bàn phím: phím \(returnLabel) — tìm TubeMini")
                TubeModel.shared.search(tf.text ?? "")
            } else {
                Log.d("Car", "bàn phím: phím \(returnLabel) — ô không có việc riêng")
            }
        } else {
            (f as? UIKeyInput)?.insertText("\n")
        }
    }

    /// Cổng gỡ lỗi: gõ cả chuỗi (\n = phím Tìm / Xong).
    func typeText(_ text: String) -> Bool {
        guard field != nil else { return false }
        for ch in text {
            if ch == "\n" { submit() } else { insert(String(ch)) }
        }
        return true
    }
}

private struct CarKeyboardView: View {
    @ObservedObject private var kb = CarKeyboard.shared

    private static let letters = ["qwertyuiop", "asdfghjkl", "zxcvbnm"]
    private static let symbols = ["1234567890", "-/:;()&@\"", ".,?!'#%+"]

    var body: some View {
        GeometryReader { geo in
            let gap: CGFloat = 6
            let unit = (geo.size.width - gap * 11) / 10
            let rowH = (geo.size.height - gap * 5) / 4
            let rows = kb.symbols ? Self.symbols : Self.letters
            VStack(spacing: gap) {
                HStack(spacing: gap) {
                    ForEach(Array(rows[0]), id: \.self) { c in key(String(c), unit, rowH) }
                }
                HStack(spacing: gap) {
                    ForEach(Array(rows[1]), id: \.self) { c in key(String(c), unit, rowH) }
                }
                HStack(spacing: gap) {
                    if kb.symbols {
                        Spacer(minLength: 0)
                    } else {
                        special(kb.upper ? "shift.fill" : "shift", label: "Chữ hoa", width: unit * 1.4, rowH) { kb.upper.toggle() }
                    }
                    ForEach(Array(rows[2]), id: \.self) { c in key(String(c), unit, rowH) }
                    special("delete.left", label: "Xoá", width: unit * 1.4, rowH) { kb.back() }
                }
                HStack(spacing: gap) {
                    textKey(kb.symbols ? "ABC" : "123", width: unit * 1.6, rowH) { kb.symbols.toggle() }
                    textKey("dấu cách", width: unit * 4.6, rowH) { kb.type(" ") }
                    textKey(kb.returnLabel, width: unit * 2, rowH, primary: true) { kb.submit() }
                    special("keyboard.chevron.compact.down", label: "Ẩn bàn phím", width: unit * 1.2, rowH) { kb.hide() }
                }
            }
            .padding(gap)
            .frame(width: geo.size.width, height: geo.size.height)
        }
        .background(Color(hex: 0x1B1E25).ignoresSafeArea())
    }

    private func key(_ c: String, _ w: CGFloat, _ h: CGFloat) -> some View {
        Button { kb.type(c) } label: {
            Text(kb.upper ? c.uppercased() : c)
                .font(.system(size: min(30, h * 0.48), weight: .medium))
                .foregroundColor(Theme.text)
                .frame(width: w, height: h)
                .background(RoundedRectangle(cornerRadius: 8).fill(Theme.surface2))
        }
        .buttonStyle(.plain)
    }

    private func special(_ icon: String, label: String, width: CGFloat, _ h: CGFloat, _ run: @escaping () -> Void) -> some View {
        Button(action: run) {
            Image(systemName: icon)
                .font(.system(size: min(26, h * 0.42), weight: .semibold))
                .foregroundColor(Theme.text)
                .frame(width: width, height: h)
                .background(RoundedRectangle(cornerRadius: 8).fill(Theme.surface))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
    }

    private func textKey(_ title: String, width: CGFloat, _ h: CGFloat, primary: Bool = false,
                         _ run: @escaping () -> Void) -> some View {
        Button(action: run) {
            Text(title)
                .font(.system(size: min(22, h * 0.36), weight: .semibold))
                .foregroundColor(primary ? Theme.onBrand : Theme.text)
                .frame(width: width, height: h)
                .background(RoundedRectangle(cornerRadius: 8).fill(primary ? Theme.brand : Theme.surface))
        }
        .buttonStyle(.plain)
    }
}
