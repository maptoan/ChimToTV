import AVKit
import UIKit
import WebKit

/// Phát một tập phim (bản Android: PlayerActivity.kt — giữ y logic).
///
/// Trình phát của trang chỉ chạy khi nằm trong iframe của chính trang phim, nên app dựng trang bọc (player.html) với
/// origin = địa chỉ tập (loadHTMLString + baseURL), đặt trình phát vào iframe; player-agent.js được WKWebView chèn vào đầu
/// mọi khung (WKUserScript, cả iframe khác tên miền). Agent nhận lệnh qua postMessage, báo trạng thái lên trang bọc;
/// app đọc trạng thái mỗi giây. Link HLS (.m3u8) thì phát bằng hls.html + hls.js (bỏ quảng cáo chèn giữa phim).
@MainActor
final class PlayerModel: NSObject, ObservableObject, WKNavigationDelegate, WKUIDelegate {
    struct PlayState: Equatable {
        var t: Double
        var d: Double
        var paused: Bool
        var ended: Bool
        var waiting: Bool
        var ready: Bool
        var droppedFrames: Int
        var totalFrames: Int
    }

    /// Trình phát đang mở (cho cổng gỡ lỗi).
    static weak var current: PlayerModel?

    static let rates: [Double] = [1.0, 1.25, 1.5, 2.0, 0.75]
    /// Hẹn giờ tắt (phút); -1 = hết tập đang xem.
    static let sleepOptions = [0, 30, 60, 90, -1]

    let web: WKWebView
    let filmUrl: String
    /// Tập lúc mở trình phát (màn xe đổi chỗ khi tập chưa kịp mở: mở lại đúng tập này).
    let startEpUrl: String
    private let continueMode: Bool

    /// Đóng trình phát (PlayerController gắn vào); mở trang web của tập (trên trình phát).
    var onClose: () -> Void = {}
    var onOpenWeb: (String) -> Void = { _ in }

    @Published private(set) var title = ""
    @Published private(set) var loading: String? = "Đang tải…"
    @Published private(set) var loadingIsError = false
    @Published private(set) var state: PlayState?
    @Published private(set) var started = false
    @Published private(set) var barShown = false
    @Published private(set) var panelShown = false
    @Published private(set) var notice: String?
    @Published private(set) var seekTarget: Double?
    @Published private(set) var servers: [Server] = []
    @Published private(set) var server: Server?
    @Published private(set) var groups: [EpisodeGroup] = []
    @Published private(set) var group: EpisodeGroup?
    @Published private(set) var suggestion: Server?
    @Published private(set) var suggestionText = ""
    @Published private(set) var rate = 1.0
    @Published private(set) var sleepChoice = 0
    @Published private(set) var sleepAt: Int64 = 0
    /// Phụ đề rời của tập đang phát (dexter.pw): danh sách, tệp đang hiện (-1 = tắt), độ lệch (giây, + = chữ muộn hơn).
    @Published private(set) var subs: [SubTrack] = []
    @Published private(set) var subIdx = -1
    @Published private(set) var subDelay = 0.0
    /// Các bản của luồng HLS đang phát (chiều cao, cao → thấp) và chiều cao đang phát — đọc từ hls.html khi mở Menu.
    @Published private(set) var levels: [Int] = []
    @Published private(set) var levelNow = 0
    /// Lời nhắn phụ đề mới nhất đã hiện (số thứ tự hls.html đánh; trang nạp lại thì đếm lại từ 1).
    private var subNoteShown = 0
    @Published private(set) var pageMode = false
    /// Đang kéo thanh thời gian (khỏi tự ẩn thanh, khỏi nhảy số).
    @Published var dragging = false {
        didSet { if !dragging { showBar() } }
    }

    private var detail: FilmDetail?
    private var episodes: [Episode] { group?.episodes ?? [] }
    private var index = 0
    private var source: EpisodeSource?
    private var token = 0
    /// Số thứ tự lần nạp trang phát (window.__chimtoLoad): trang cũ còn trả lời vài giây sau khi chuyển tập.
    private var pageLoad = 0
    private var embedUrl: String?
    private(set) var episodeUrl = ""
    private var hlsUrl: String?
    /// Màn xe (CarPlay): luồng .m3u8 / tệp video phát bằng AVPlayer (NativeFilm.swift) — tắt màn iPhone vẫn có hình; nil = web.
    private var native: NativeFilm?
    @Published private(set) var nativePlayer: AVPlayer?
    @Published private(set) var nativeFill = false
    /// Luồng vừa mở (AVPlayer lỗi trước khi phim chạy thì phát lại bằng trình phát web).
    private var lastVideo: Video?
    /// Đang phát video TubeMini (Core/TubeMini.swift).
    var isTube: Bool { TubeMini.isTubeUrl(filmUrl) }
    private var errShown = false
    private var lastAdAt: Int64 = 0
    private var resumeAt = 0
    /// Đã ra lệnh tua tới chỗ xem dở mà trình phát chưa tua xong — thời gian báo lúc đó là giây đầu tập, không lưu đè.
    private var resumePending = false
    private var resumeIssuedAt: Int64 = 0
    private var endedHandled = false
    private var hintShown = false
    private var loadStartedAt: Int64 = 0
    private var lastSave: Int64 = 0
    private var sleepGraceUntil: Int64 = 0
    /// Chỗ bắt đầu báo trước cho trình phát của lần nạp tới (chỗ xem dở / hết nhạc mở đầu).
    private var startHint = 0
    private var introDone = false
    private var session: PlaySession?
    private var suggested = false
    private var shownByTap = false
    /// Số lần tự bấm phát hộ người xem (trình phát JW trên iPad không tự tải trước video như trên Android).
    private var autoPlays = 0
    private var lastAutoPlay: Int64 = 0
    private var expectedURL = ""
    private var expectedHost: String?
    private var timers: [String: Task<Void, Never>] = [:]
    private var polling: Task<Void, Never>?
    private var logRelay: ScriptRelay?
    /// Khung con đã gặp trong lần nạp này: địa chỉ → khung (cho cổng gỡ lỗi, hình trong hình).
    private(set) var frames: [(url: String, frame: WKFrameInfo)] = []

    // MARK: Hình trong hình

    /// Đang ở cửa sổ hình trong hình của iPad (agent báo theo video).
    @Published private(set) var inPip = false
    /// Màn trình phát đã đóng lúc phim đang hình trong hình (người xem dùng màn khác của app / app khác): trình phát vẫn
    /// chạy tới khi phóng to (mở lại màn trình phát) hoặc đóng cửa sổ nhỏ.
    private(set) var detached = false
    static var detachedModel: PlayerModel?
    /// PlayerController đóng màn trình phát mà không huỷ trình phát.
    var onDetach: () -> Void = {}
    /// Phim bắt đầu / thôi đang phát (PlayerController: chỉ để lại lần vuốt đầu từ mép dưới khi đang phát).
    var onPlayingChanged: () -> Void = {}

    /// Đang phát thật (vuốt lên từ mép dưới thì thu nhỏ phim).
    var playing: Bool { started && state?.paused == false && !inPip }
    private var pipRequestedAt: Int64 = 0
    /// Phóng to từ cửa sổ nhỏ lúc app chưa hiện lên hẳn (đang ở màn hình chính iPad): đợi app hiện rồi mở màn trình phát.
    private var waitingToReattach = false
    private var begun = false
    /// Đổi người xem: đã lưu chỗ xem cho người cũ, không ghi gì nữa.
    private var saveDisabled = false

    init(filmUrl: String, epUrl: String, continueMode: Bool) {
        self.filmUrl = filmUrl
        startEpUrl = epUrl
        self.continueMode = continueMode
        let config = WebSetup.configuration()
        if CarScreen.shared.connected {
            // Đang nối CarPlay: hệ thống coi màn xe là «màn ngoài» — video cho phép AirPlay chuyển sang phát ra ngoài, lớp hình
            // trong app đen thui (trên màn xe lẫn điện thoại). Tắt AirPlay / hình trong hình như CarTube.
            config.allowsAirPlayForMediaPlayback = false
            config.allowsPictureInPictureMediaPlayback = false
        }
        web = WebSetup.make(config)
        super.init()
        web.navigationDelegate = self
        web.uiDelegate = self
        web.scrollView.isScrollEnabled = false
        #if DEVTOOLS
        let relay = ScriptRelay { m in Log.d("JS", "\(m.body)") }
        config.userContentController.add(relay, name: "chimtoLog")
        logRelay = relay
        #endif
        PlayerModel.current = self
    }

    // MARK: - Mở phim / tập / server

    func begin() {
        guard !begun else { return } // mở lại màn trình phát của phim đang hình trong hình: trình phát vẫn chạy
        begun = true
        startPolling()
        if let cached = Site.cachedDetail(filmUrl) {
            start(cached, epUrl: startEpUrl)
            return
        }
        showLoading("Đang tải thông tin phim…")
        Task {
            do {
                start(try await Site.detail(filmUrl), epUrl: startEpUrl)
            } catch {
                showError("Không tải được phim.\n\(friendly(error))")
            }
        }
    }

    private func start(_ d: FilmDetail, epUrl: String) {
        detail = d
        // Phim đang theo dõi: đã vào xem thì không còn là "tập mới"
        if let latest = d.latestEpisode { Prefs.updateFollow(d.url, count: d.episodeCount, latest: latest.title, seen: true) }
        groups = d.groups
        guard let g = d.groupOf(epUrl) ?? d.groups.first else {
            showError("Phim này chưa có tập nào.")
            return
        }
        group = g
        let i = max(0, g.episodes.firstIndex { $0.url == epUrl } ?? 0)
        let next = continueMode && Prefs.finished(epUrl) && i + 1 < g.episodes.count
        openEpisode(next ? i + 1 : i)
    }

    func openEpisode(_ i: Int) {
        guard let d = detail, let ep = episodes[safe: i] else { return }
        // Người đang xem đã hết giờ xem (WatchLimit): chưa mở tập, hỏi mã phụ huynh trước
        if let b = WatchLimit.block() {
            limitBlock = b
            limitResume = { [weak self] in self?.openEpisode(i) }
            hideLoading()
            return
        }
        index = i
        remoteSeq = 0
        token += 1
        let t = token
        stopVideo()
        episodeUrl = ep.url
        source = nil
        servers = []
        let groupName = d.groups.count > 1 ? (group.map { " (\($0.name))" } ?? "") : ""
        // TubeMini: tên video; lịch sử + chỗ xem nằm trên máy chủ TubeMini (không ghi vào lịch sử phim)
        title = isTube ? ep.title : "\(d.title) — \(ep.title)\(groupName)"
        resumeAt = isTube ? 0 : Prefs.position(ep.url)
        introDone = false
        if !Prefs.privateMode && !isTube {
            Prefs.addHistory(HistoryItem(filmUrl: d.url, title: d.title, poster: d.poster, epTitle: ep.title, epUrl: ep.url, time: nowMs()))
            // Trang chủ tổng hợp: người hay xem bản thuyết minh / lồng tiếng thì có hàng riêng
            if let l = group.flatMap({ PlayStats.lang($0.name) }) { Prefs.recordWatchLang(l) }
        }
        if !isTube { HomeFeed.markSeen(d) }
        showLoading("Đang mở \(ep.title)…")
        let g = group
        Task {
            do {
                // Trang tập + server dự phòng KKPhim (tải song song; trang lỗi mà KKPhim có tập này thì vẫn phát được)
                let src = try await KkPhim.source(d, g, ep)
                guard t == token else { return }
                source = src
                servers = src.pageOnly ? [] : src.servers
                if isTube { resumeAt = TubeMini.resume(ep.url) } // chỗ xem dở trên máy chủ (đọc trong TubeMini.source)
                if src.pageOnly {
                    playPage(src)
                } else {
                    let host = Site.hostOf(src.epUrl)
                    // TubeMini: chất lượng nhớ như server; «Chỉ nghe» chọn từ menu ô video đứng đầu danh sách → dùng luôn
                    let oneShot = isTube && src.servers.first?.type == "tmaudio"
                    let usual = oneShot ? src.servers.first : (src.servers.first { $0.type == Prefs.serverType(host) } ?? src.servers.first)
                    var pick = isTube ? usual : PlayStats.choose(AppProxy.statsHost(host), src.servers, usual)
                    if let u = usual, let p = pick, p != u {
                        let why = PlayStats.score(AppProxy.statsHost(host), u.type).label ?? "hay giật"
                        showNotice("\(u.label) \(why) gần đây — mở \(p.label) (Menu để đổi server).", 6)
                    }
                    if CarScreen.shared.uiOnCar, let p = pick, let alt = Self.carServer(src.servers, like: p) {
                        showNotice("Màn xe: mở \(alt.label) — có hình cả khi tắt màn iPhone (Menu để đổi server).", 6)
                        pick = alt
                    }
                    playServer(pick)
                }
            } catch {
                guard t == token else { return }
                showError("Không mở được \(ep.title).\n\(friendly(error))")
            }
        }
    }

    /// Mở thẳng trang tập: trang tự nạp trình phát, page-focus.js phóng nó ra toàn màn hình.
    private func playPage(_ src: EpisodeSource) {
        stopVideo()
        pageMode = true
        loadStartedAt = nowMs()
        session = PlaySession(host: AppProxy.statsHost(Site.hostOf(src.epUrl)), server: "page", openedAt: loadStartedAt)
        showLoading("Đang mở trang tập phim…")
        startHint = startFor()
        installScripts(hls: false, page: true)
        guard let u = URL(string: src.epUrl) else { return }
        expect(src.epUrl)
        web.load(URLRequest(url: u))
    }

    private func playServer(_ s: Server?) {
        guard let src = source else { return }
        server = s
        // "Trình phát của trang" (trang mở thẳng trang tập, có thêm server KKPhim)
        if s?.type == EpisodeSource.pageServer {
            token += 1
            playPage(src)
            return
        }
        token += 1
        let t = token
        stopVideo()
        showLoading("Đang tải \(s?.label ?? "trình phát")…")
        Task {
            do {
                let v = try await Site.video(src, s)
                guard t == token else { return }
                loadStartedAt = nowMs()
                session = PlaySession(host: AppProxy.statsHost(Site.hostOf(src.epUrl)), server: s?.type ?? "embed", openedAt: loadStartedAt)
                startHint = startFor()
                Log.d("Player", "phát \(v.url) (server \(s?.type ?? "-"), báo trước \(startHint) s)")
                let m3u8 = URL(string: v.url)?.path.hasSuffix(".m3u8") == true
                if v.avPlayer || v.file || v.hls || m3u8 {
                    hlsUrl = v.url
                    lastVideo = v
                    if Self.nativeOK(v, m3u8: m3u8) {
                        playNative(v, src)
                    } else {
                        playHls(v, src, m3u8: m3u8, t)
                    }
                } else {
                    embedUrl = v.url
                    installScripts(hls: false, page: false)
                    loadHTML(Assets.wrapperPage(src: v.url), base: src.epUrl)
                }
            } catch {
                guard t == token else { return }
                showError("\(friendly(error))\nMở Menu để chọn server khác.")
                showPanel()
            }
        }
    }

    /// Luồng HLS / tệp video bằng trang hls.html (+ hls.js nếu máy có MediaSource).
    private func playHls(_ v: Video, _ src: EpisodeSource, m3u8: Bool, _ t: Int) {
        let listed = !v.subs.isEmpty || v.subsLater != nil
        setSubs(v.subs)
        subDelay = listed ? Prefs.subDelay(src.epUrl) : 0
        installScripts(hls: true, page: false, fullHls: v.hls)
        loadHTML(Assets.hlsPage(src: v.url, start: startHint, sub: v.subtitle, subs: listed ? subs : nil, subIdx: subIdx,
                                subDelay: subDelay, file: v.file || !(v.hls || m3u8), cap: v.hls), base: src.epUrl)
        if let later = v.subsLater { pollSubs(later, t) }
    }

    // MARK: - Trình phát gốc (AVPlayer) trên màn xe

    /// Đang chiếu lên màn xe và luồng là HLS thường / tệp video, không phụ đề rời, không âm thanh tách rời (dexter.pw) → AVPlayer.
    private static func nativeOK(_ v: Video, m3u8: Bool) -> Bool {
        v.avPlayer || (CarScreen.shared.uiOnCar && (m3u8 || v.file) && !v.hls && v.subs.isEmpty && v.subsLater == nil)
    }

    private func playNative(_ v: Video, _ src: EpisodeSource) {
        guard let u = URL(string: v.url) else { return }
        Log.d("Player", isTube ? "TubeMini: phát bằng AVPlayer" : "màn xe: phát bằng AVPlayer (tắt màn iPhone vẫn có hình)")
        // Luồng HLS (đuôi .m3u8, hoặc HLS của máy chủ TubeMini) đi qua app; tệp MP4 thì AVPlayer tải thẳng
        let hls = !v.file && (v.avPlayer || u.path.lowercased().hasSuffix(".m3u8"))
        let n = NativeFilm(url: u, hls: hls, referer: isTube ? nil : src.epUrl, start: Double(startHint))
        n.onRemote = { [weak self] r in
            if r == "next" { self?.next() } else if r == "prev" { self?.prev() }
        }
        native = n
        nativeFill = Prefs.videoFill
        nativePlayer = n.player
    }

    private func stopNative() {
        native?.stop()
        native = nil
        nativePlayer = nil
    }

    /// AVPlayer không mở được luồng trước khi phim chạy (vd đoạn video ngụy trang ảnh của VSMOV): phát lại bằng trình phát web.
    private func nativeFallback(_ why: String) {
        guard let v = lastVideo, let src = source, native != nil else { return }
        if isTube {
            // TubeMini chỉ phát bằng AVPlayer: báo lỗi, lần «Tải lại» xin link mới (bỏ bộ nhớ đệm của máy chủ)
            Log.d("Player", "TubeMini: AVPlayer không phát được (\(why))")
            stopNative()
            TubeMini.markFresh(episodeUrl)
            showError("Không phát được video (\(why)).\nMenu → Tải lại, hoặc chọn chất lượng khác.")
            showPanel()
            return
        }
        Log.d("Player", "AVPlayer không phát được (\(why)) — chuyển sang trình phát web")
        stopNative()
        playHls(v, src, m3u8: URL(string: v.url)?.path.hasSuffix(".m3u8") == true, token)
        showNotice("Luồng này phải phát bằng trình phát web: màn iPhone cần bật thì màn xe mới có hình.", 8)
    }

    /// Màn xe: server riêng của trang (trang nhúng mã hoá) chỉ phát được bằng trình phát web → có server KKPhim dự phòng thì
    /// dùng nó (AVPlayer, tắt màn iPhone vẫn có hình), cùng kiểu Vietsub / Thuyết minh với server định mở.
    private static func carServer(_ servers: [Server], like p: Server) -> Server? {
        guard !p.type.hasPrefix("kk-") else { return nil }
        let kk = servers.filter { $0.type.hasPrefix("kk-") }
        guard !kk.isEmpty else { return nil }
        let l = p.label.lowercased()
        let dub = l.contains("thuyết") || l.contains("lồng") || p.type.lowercased().contains("tm")
        return kk.first { dub ? $0.type.hasSuffix("tm") : $0.type.hasSuffix("vs") } ?? kk.first
    }

    /// Script chèn vào trang phát: agent (mọi khung, kèm chỗ bắt đầu + số lần nạp), hls.js, phóng trình phát (trang tập).
    private func installScripts(hls: Bool, page: Bool, fullHls: Bool = false) {
        let ucc = web.configuration.userContentController
        ucc.removeAllUserScripts()
        // __chimtoWantH: chất lượng muốn xem (hls.html đọc lúc có danh sách bản)
        let prefix = "window.__chimtoStart=\(startHint);window.__chimtoLoad=\(pageLoad);window.__chimtoWantH=\(Prefs.videoQuality);\n"
        ucc.addUserScript(WKUserScript(source: prefix + Assets.agent, injectionTime: .atDocumentStart, forMainFrameOnly: false))
        ucc.addUserScript(WKUserScript(source: WebSetup.fillCss, injectionTime: .atDocumentStart, forMainFrameOnly: false))
        // dexter.pw (Video.hls): bản hls.js đầy đủ — luồng âm thanh tách rời
        if hls { ucc.addUserScript(WKUserScript(source: fullHls ? Assets.hlsJsFull : Assets.hlsJs, injectionTime: .atDocumentStart, forMainFrameOnly: true)) }
        if page { ucc.addUserScript(WKUserScript(source: Assets.pageFocus, injectionTime: .atDocumentStart, forMainFrameOnly: true)) }
        #if DEVTOOLS
        if !page { ucc.addUserScript(WKUserScript(source: WebSetup.consoleHook, injectionTime: .atDocumentStart, forMainFrameOnly: true)) }
        #endif
    }

    private func loadHTML(_ html: String, base: String) {
        expect(base)
        web.loadHTMLString(html, baseURL: URL(string: base))
    }

    private func expect(_ url: String) {
        expectedURL = url
        expectedHost = Site.hostOf(url)
    }

    private func stopVideo() {
        savePosition()
        session?.save(nowMs())
        session = nil
        suggested = false
        dismissSuggestion()
        pageMode = false
        embedUrl = nil
        hlsUrl = nil
        stopNative()
        lastVideo = nil
        subs = []
        subIdx = -1
        subDelay = 0
        subNoteShown = 0
        levels = []
        levelNow = 0
        errShown = false
        lastAdAt = 0
        state = nil
        started = false
        resumePending = false
        endedHandled = false
        hintShown = false
        autoPlays = 0
        lastAutoPlay = 0
        seekTarget = nil
        cancel("seek")
        pageLoad += 1
        frames.removeAll()
        expect("about:blank")
        if let blank = URL(string: "about:blank") { web.load(URLRequest(url: blank)) }
    }

    // MARK: - Server / bản / tập

    func serverText(_ s: Server) -> String {
        if isTube { return s.label } // chất lượng TubeMini: không chấm điểm server
        let host = source.flatMap { Site.hostOf($0.epUrl) }
        return PlayStats.score(AppProxy.statsHost(host), s.type).summary.map { "\(s.label)  ·  \($0)" } ?? s.label
    }

    var sessionText: String? { session?.describe() }

    /// TubeMini: nút chất lượng trên thanh điều khiển → chọn 1080p / 720p / 360p / Chỉ nghe; xem tiếp đúng chỗ, nhớ cho các
    /// video sau (switchServer lưu như server của trang, khoá «video»).
    func chooseTubeQuality() {
        guard isTube, !servers.isEmpty else { return }
        let list = servers
        let cur = server
        TvAlert.show("Chất lượng video", "Nhớ cho các video sau.", list.map { $0 == cur ? "\($0.label)  ✓" : $0.label } + ["Huỷ"]) { [weak self] i in
            guard let self, let s = list[safe: i], s != self.server else { return }
            self.switchServer(s)
        }
    }

    /// Người xem chọn server khác: nhớ lựa chọn, xem tiếp từ chỗ đang xem.
    func switchServer(_ s: Server) {
        guard let src = source else { return }
        Prefs.setServer(Site.hostOf(src.epUrl), type: s.type, label: s.label)
        if let t = currentTime() { resumeAt = t }
        hidePanel()
        playServer(s)
    }

    /// Sang bản khác (Vietsub / Thuyết Minh…): mở tập cùng tên và xem tiếp đúng chỗ; bản đó chưa có tập này thì mở tập
    /// cùng vị trí / tập cuối của bản đó, không mang giờ sang.
    func switchGroup(_ g: EpisodeGroup) {
        hidePanel()
        guard let d = detail, g != group, !g.episodes.isEmpty else { return }
        // Phim chia mùa: chọn mùa là mở tập đầu của mùa đó
        if d.seasons {
            group = g
            openEpisode(0)
            return
        }
        let cur = episodes[safe: index]
        let same = g.episodes.firstIndex { $0.title == cur?.title }
        let target = same ?? min(index, g.episodes.count - 1)
        if same != nil {
            if let t = currentTime() { Prefs.savePosition(g.episodes[target].url, seconds: t, filmUrl: d.url) }
        } else {
            showNotice("\(g.name) chưa có \(cur?.title ?? "tập này") — mở \(g.episodes[target].title).", 6)
        }
        group = g
        openEpisode(target)
    }

    func next() {
        if index + 1 < episodes.count { openEpisode(index + 1) } else if nextSeason() != nil { openNextSeason() } else { showNotice("Đây là tập mới nhất.") }
    }

    /// Phim chia mùa (dexter.pw): mùa sau của mùa đang xem (không sang nhóm tập đặc biệt); phim khác: nil.
    private func nextSeason() -> EpisodeGroup? {
        guard let d = detail, d.seasons, let g = group, let i = d.groups.firstIndex(of: g) else { return nil }
        guard let n = d.groups[safe: i + 1], n.name != FilmDetail.specials, !n.episodes.isEmpty else { return nil }
        return n
    }

    private func openNextSeason() {
        guard let g = nextSeason() else { return }
        group = g
        openEpisode(0)
    }

    func prev() {
        if index > 0 { openEpisode(index - 1) } else { showNotice("Đây là tập đầu tiên.") }
    }

    func reload() {
        hidePanel()
        if isTube { TubeMini.markFresh(episodeUrl) } // xin link mới (link cũ hết hạn / 403)
        if let t = currentTime() { resumeAt = t }
        guard let src = source else {
            openEpisode(index)
            return
        }
        if src.pageOnly { playPage(src) } else { playServer(server) }
    }

    func openWebPage() {
        hidePanel()
        cmd("pause")
        // TubeMini: trang của video trên YouTube
        onOpenWeb(TubeMini.videoId(episodeUrl).map { "https://m.youtube.com/watch?v=\($0)" } ?? episodeUrl)
    }

    // MARK: - Server đang giật → gợi ý server khác (người xem chọn mới chuyển)

    private func suggestServer(_ now: Int64) {
        suggested = true // mỗi lần phát chỉ gợi ý một lần
        guard let src = source, !src.pageOnly, let cur = server,
              let pair = PlayStats.alternative(AppProxy.statsHost(Site.hostOf(src.epUrl)), src.servers, cur) else { return }
        let (alt, score) = pair
        suggestion = alt
        suggestionText = "\(cur.label) đang giật nhiều. Chuyển sang \(alt.label)\(score.summary.map { " (\($0))" } ?? "")?"
        after("suggest", 15) { [weak self] in self?.dismissSuggestion() }
    }

    func acceptSuggestion() {
        guard let alt = suggestion else { return }
        dismissSuggestion()
        switchServer(alt)
    }

    func dismissSuggestion() {
        suggestion = nil
        cancel("suggest")
    }

    // MARK: - Trạng thái phát

    private func startPolling() {
        polling?.cancel()
        polling = Task { [weak self] in
            while !Task.isCancelled {
                self?.poll()
                try? await Task.sleep(nanoseconds: 1_000_000_000)
            }
        }
    }

    private func poll() {
        if let n = native {
            if !started, let why = n.failed {
                nativeFallback(why)
                return
            }
            onState([NSNumber(value: pageLoad), n.state(), NSNull()])
            if started, let s = state, let d = detail {
                let ep = episodes[safe: index]?.title ?? ""
                let author = TubeMini.videoId(episodeUrl).flatMap { TubeMini.info($0)?.author } ?? ""
                n.nowPlaying(title: isTube ? ep : d.title, subtitle: isTube ? author : ep, duration: s.d, elapsed: s.t, playing: !s.paused)
            }
            return
        }
        web.evaluateJavaScript("[window.__chimtoLoad||0,window.__chimtoState||null,window.__chimtoSubNote||null]") { [weak self] result, _ in
            Task { @MainActor in self?.onState(result) }
        }
    }

    private static func num(_ v: Any?) -> Double { (v as? NSNumber)?.doubleValue ?? 0 }
    private static func flag(_ v: Any?) -> Bool { (v as? NSNumber)?.boolValue ?? false }

    private func onState(_ raw: Any?) {
        // [lần nạp trang, trạng thái, lời nhắn phụ đề]: trạng thái của trang tập cũ (còn trả lời ngay sau khi chuyển tập) thì bỏ
        var o: [String: Any]?
        if let arr = raw as? [Any], arr.count >= 2, (arr[0] as? NSNumber)?.intValue == pageLoad {
            o = arr[1] as? [String: Any]
            if arr.count >= 3, let note = arr[2] as? String { subNote(note) }
        }
        let now = nowMs()
        let inAd = Self.flag(o?["ad"])
        if inAd { lastAdAt = now }
        // Mở 25 giây mà video chưa chạy. Không tính lúc quảng cáo, không đè lên thông báo lỗi server.
        let waited = now - max(loadStartedAt, lastAdAt)
        if !started && !errShown && !hintShown && !inAd && (embedUrl != nil || hlsUrl != nil || pageMode) && waited > 25_000 {
            hintShown = true
            hideLoading()
            let other = (source?.pageOnly ?? true) ? "Menu → Tải lại / Mở trang web" : "Menu để đổi server"
            showNotice("Video chưa chạy? Chạm ▶ để thử phát, hoặc \(other).", 10)
        }
        guard let o = o else { return }
        if inAd {
            // Trình phát đang chạy quảng cáo (agent tự bỏ qua khi được phép) — chưa phải phim
            session?.ad()
            if !started { showLoading("Trình phát đang chạy quảng cáo — tự bỏ qua khi được phép…") }
            return
        }
        let s = PlayState(t: Self.num(o["t"]), d: Self.num(o["d"]), paused: Self.flag(o["paused"]), ended: Self.flag(o["ended"]),
                          waiting: Self.flag(o["waiting"]), ready: Self.flag(o["ready"]),
                          droppedFrames: Int(Self.num(o["df"])), totalFrames: Int(Self.num(o["tf"])))
        let wasPaused = state?.paused
        let wasPlaying = playing
        if state != s { state = s }
        let pip = Self.flag(o["pip"])
        if pip != inPip { pipChanged(pip) }
        WatchLimit.tick(playing: started && !s.paused && !s.waiting, now: now)
        // Đã hết giờ xem mà phim lại chạy (nút ▶ của cửa sổ hình trong hình, màn hình khoá, AirPods): dừng lại ngay
        if limitBlock != nil && !s.paused { cmd("pause") }
        checkLimit()
        // Màn hình khoá / Trung tâm điều khiển / AirPods bấm tập trước / sau (Media Session của trang, player-agent.js)
        let rs = Int(Self.num(o["rs"]))
        if rs > remoteSeq {
            remoteSeq = rs
            switch o["rc"] as? String {
            case "next": next(); return
            case "prev": prev(); return
            default: break
            }
        }
        let err = (o["err"] as? String) ?? ""
        if !err.isEmpty && !errShown {
            errShown = true
            session?.fail()
            if started {
                showNotice("Mất kết nối video (\(err)).\nMenu → Tải lại, hoặc chọn server khác." + AppProxy.videoNote(), 10)
            } else {
                showError("\(server?.label ?? "Server") không phát được video (\(err)).\nMở Menu để chọn server khác." + AppProxy.videoNote())
                showPanel()
            }
        }
        // Trình phát JW trên iPad (Safari) không tự tải trước video khi chưa ai bấm phát — khác Android (agent chỉ tự
        // phát khi video đã có dữ liệu). Còn dừng, chưa có dữ liệu sau vài giây → bấm ▶ hộ người xem, tối đa 4 lần.
        if !started && s.paused && !s.ready && !errShown && now - loadStartedAt > 2500 && autoPlays < 4 && now - lastAutoPlay > 4000 {
            autoPlays += 1
            lastAutoPlay = now
            Log.d("Player", "trình phát đứng chờ — tự bấm phát (lần \(autoPlays))")
            cmd("play")
        }
        if let sess = session {
            sess.update(now, paused: s.paused, waiting: s.waiting, ready: s.ready, droppedFrames: s.droppedFrames, totalFrames: s.totalFrames)
            if !suggested && sess.struggling(now) { suggestServer(now) }
        }
        // Bắt đầu khi video thật sự chạy (không phải lúc mới nạp xong mà còn dừng)
        if !started && s.ready && !s.paused {
            started = true
            sendNowPlaying()
            Log.d("Player", "video chạy t=\(Int(s.t)), báo trước \(startHint), \(now - loadStartedAt) ms")
            hideLoading()
            if resumeAt > 30 && (s.d <= 0 || Double(resumeAt) < s.d - 30) {
                introDone = true // xem tiếp đúng chỗ, không tua thêm qua nhạc mở đầu
                if abs(s.t - Double(resumeAt)) > 10 {
                    resumePending = true
                    resumeIssuedAt = now
                    session?.seeked()
                    cmd("seekTo", Double(resumeAt))
                }
                showNotice("Xem tiếp từ \(formatTime(Double(resumeAt)))")
            } else if startHint > 0 && s.t >= Double(startHint - 10) {
                introDone = true
                showNotice("Đã bỏ qua nhạc mở đầu (tới \(formatTime(Double(startHint)))) — lùi lại để xem.")
            }
            if rate != 1.0 { cmd("rate", rate) }
            if Prefs.videoFill { cmd("fit", 1) }
            showBar()
        }
        if sleepAt > 0 && sleepAt <= now { sleepNow() }
        if sleepGraceUntil > 0 && sleepGraceUntil <= now {
            sleepGraceUntil = 0
            exitForSleep()
            return
        }
        if playing != wasPlaying { onPlayingChanged() }
        if resumePending && (s.t >= Double(resumeAt - 5) || now - resumeIssuedAt > 30_000) { resumePending = false }
        if started && !introDone && !s.paused { skipIntro(s) }
        if let w = wasPaused, w != s.paused { showBar() }
        if started && now - lastSave > 5000 {
            lastSave = now
            savePosition(periodic: true)
        }
        if s.ended && !endedHandled {
            endedHandled = true
            if Self.sleepOptions[sleepChoice] < 0 {
                sleepNow()
            } else if Prefs.autoNext && index + 1 < episodes.count {
                showNotice("Hết tập — chuyển sang \(episodes[index + 1].title)")
                openEpisode(index + 1)
            } else if Prefs.autoNext, let g = nextSeason() {
                showNotice("Hết mùa — chuyển sang \(g.name), \(g.episodes[0].title)")
                openNextSeason()
            }
        }
    }

    // MARK: - Nhạc mở đầu

    private var introKey: String { detail?.url ?? filmUrl }

    /// Phim đã đánh dấu nhạc mở đầu: video vừa chạy tới mốc đầu (trễ tối đa 8 giây) thì tua tới mốc hết, một lần mỗi tập.
    private func skipIntro(_ s: PlayState) {
        guard let mark = Prefs.intro(introKey), mark.end > mark.start, seekTarget == nil else { return }
        if s.t >= Double(mark.start + 8) || s.t >= Double(mark.end - 3) {
            introDone = true
            return
        }
        if s.t < Double(mark.start - 1) || (s.d > 0 && Double(mark.end) > s.d - 30) { return }
        introDone = true
        session?.seeked()
        cmd("seekTo", Double(mark.end))
        showNotice("Đã bỏ qua nhạc mở đầu (tới \(formatTime(Double(mark.end)))) — lùi lại để xem.")
    }

    /// Chỗ bắt đầu báo trước cho lần nạp trang này: chỗ xem dở; hoặc hết nhạc mở đầu nếu nhạc nằm ngay đầu tập.
    private func startFor() -> Int {
        if resumeAt > 30 { return resumeAt }
        guard let mark = Prefs.intro(introKey) else { return 0 }
        return (!introDone && mark.start <= 1 && mark.end > mark.start) ? mark.end : 0
    }

    var introText: String {
        guard let mark = Prefs.intro(introKey) else { return "chưa đánh dấu — đang ở đầu / cuối nhạc thì chạm nút tương ứng" }
        if mark.end < 0 { return "bắt đầu \(formatTime(Double(mark.start))), chưa đánh dấu chỗ hết" }
        return "\(formatTime(Double(mark.start))) → \(formatTime(Double(mark.end))), tự bỏ qua ở các tập"
    }

    var hasIntro: Bool { Prefs.intro(introKey) != nil }

    var nowSeconds: Int { Int(state?.t ?? 0) }

    func markIntroStart() {
        hidePanel()
        let t = nowSeconds
        let end = Prefs.intro(introKey).flatMap { $0.end > t ? $0.end : nil } ?? -1
        Prefs.setIntro(introKey, start: t, end: end)
        showNotice(end > t ? "Nhạc mở đầu: \(formatTime(Double(t))) → \(formatTime(Double(end))), các tập sẽ tự bỏ qua."
                   : "Đã đánh dấu đầu nhạc mở đầu. Hết nhạc thì mở Menu → «Hết nhạc ở …».", 6)
        objectWillChange.send()
    }

    func markIntroEnd() {
        hidePanel()
        let t = nowSeconds
        let start = Prefs.intro(introKey).flatMap { $0.start < t ? $0.start : nil } ?? 0
        Prefs.setIntro(introKey, start: start, end: t)
        introDone = true
        showNotice("Nhạc mở đầu: \(formatTime(Double(start))) → \(formatTime(Double(t))), các tập sẽ tự bỏ qua.", 6)
        objectWillChange.send()
    }

    func clearIntro() {
        hidePanel()
        Prefs.clearIntro(introKey)
        showNotice("Đã bỏ mốc nhạc mở đầu của phim này.")
        objectWillChange.send()
    }

    // MARK: - Phụ đề rời (dexter.pw)

    /// Danh sách phụ đề mới → chọn sẵn theo ngôn ngữ người xem ưa dùng (không có tiếng Việt thì bản đầu tiên; "off" = tắt).
    private func setSubs(_ list: [SubTrack]) {
        subs = list
        let want = Prefs.subLang
        subIdx = (list.isEmpty || want == "off") ? -1 : (list.firstIndex { $0.lang == want } ?? 0)
    }

    var subText: String {
        let name = subs[safe: subIdx]?.label ?? "Tắt"
        return subDelay != 0 && subIdx >= 0 ? "\(name) · \(delayText(subDelay))" : name
    }

    private func delayText(_ d: Double) -> String {
        let n = Swift.abs(d)
        let v = n.truncatingRemainder(dividingBy: 1) == 0 ? String(Int(n)) : String(n).replacingOccurrences(of: ".", with: ",")
        return (d > 0 ? "muộn " : "sớm ") + v + " s"
    }

    /// Đổi bản phụ đề (hết danh sách thì tắt, bấm nữa lại bản đầu); nhớ ngôn ngữ cho người xem này.
    func cycleSub() {
        guard !subs.isEmpty else { return }
        subIdx = subIdx + 1 >= subs.count ? -1 : subIdx + 1
        Prefs.subLang = subs[safe: subIdx]?.lang ?? "off"
        web.evaluateJavaScript("window.__chimtoSub&&__chimtoSub(\(subIdx))", completionHandler: nil)
    }

    /// Chữ lệch với tiếng (bản phụ đề làm cho bản phim khác): dời 0,5 giây mỗi lần bấm, nhớ cho tập này.
    func nudgeSub(_ d: Double) {
        guard !subs.isEmpty else { return }
        subDelay = (((subDelay + d) * 10).rounded()) / 10
        Prefs.setSubDelay(source?.epUrl ?? "", subDelay)
        web.evaluateJavaScript("window.__chimtoSubDelay&&__chimtoSubDelay(\(subDelay))", completionHandler: nil)
        showNotice("Phụ đề: " + (subDelay == 0 ? "đúng như bản gốc" : delayText(subDelay) + " so với bản gốc"), 1.5)
    }

    /// Tệp phụ đề hỏng thì trang tự sang tệp sau — hỏi lại tệp đang hiện cho đúng chữ trên nút.
    func syncSubIdx() {
        guard !subs.isEmpty else { return }
        web.evaluateJavaScript("window.__chimtoSubNow?__chimtoSubNow():null") { [weak self] r, _ in
            guard let self = self, let i = (r as? NSNumber)?.intValue, i != self.subIdx else { return }
            self.subIdx = i
        }
    }

    /// hls.html báo "số|lời nhắn" (vd hết tệp tiếng Việt tải được → tạm dùng tiếng Anh; thử lại được) → hiện một lần.
    private func subNote(_ raw: String) {
        let parts = raw.split(separator: "|", maxSplits: 1).map(String.init)
        guard parts.count == 2, let n = Int(parts[0]), n > subNoteShown else { return }
        subNoteShown = n
        showNotice(parts[1], 7)
        syncSubIdx()
    }

    /// Trang còn đang tìm phụ đề (phim bộ: tập ít người xem có khi vài phút mới có, trình duyệt cũng vậy): hỏi lại mỗi
    /// 4 giây, tối đa 3 phút như trang; có thì gắn ngay vào trình phát (phụ đề riêng của nguồn, nếu có, vẫn đứng đầu).
    private func pollSubs(_ listUrl: String, _ t: Int) {
        Task { [weak self] in
            for _ in 0..<45 {
                try? await Task.sleep(nanoseconds: 4_000_000_000)
                guard let self = self, t == self.token else { return }
                guard let r = try? await Dexter.subtitlesFrom(listUrl) else { continue }
                guard t == self.token else { return }
                if r.subs.isEmpty {
                    if r.pending { continue }
                    return
                }
                let keep: Int? = self.subs.isEmpty || self.subIdx < 0 ? nil : self.subIdx
                self.setSubs(self.subs + r.subs.filter { n in !self.subs.contains { $0.url == n.url } })
                if let k = keep { self.subIdx = k }
                self.web.evaluateJavaScript("window.__chimtoSubs&&__chimtoSubs(\(Assets.subsJson(self.subs)),\(self.subIdx))", completionHandler: nil)
                if let sub = self.subs[safe: self.subIdx] {
                    self.showNotice("Đã có phụ đề \(sub.label.prefix(1).lowercased() + sub.label.dropFirst()) — mở Menu để chọn bản khác hoặc chỉnh chữ sớm / muộn.", 5)
                }
                return
            }
        }
    }

    // MARK: - Chất lượng (luồng có nhiều bản, vd 4K của dexter.pw)

    private func qualityName(_ h: Int) -> String { h >= 2160 ? "4K" : h >= 1440 ? "2K" : "\(h)p" }

    var qualityText: String {
        let want = Prefs.videoQuality
        if want <= 0 { return "Tự động" + (levelNow > 0 ? " (\(qualityName(levelNow)))" : "") }
        return qualityName(want) + (levelNow > 0 && levelNow != want ? " (đang \(qualityName(levelNow)))" : "")
    }

    /// {levels, want, now} từ hls.html (__chimtoLevels / __chimtoQuality trả chuỗi JSON).
    private func showQuality(_ r: Any?) {
        guard let s = r as? String, let o = try? JSONSerialization.jsonObject(with: Data(s.utf8)) as? [String: Any] else {
            levels = []
            return
        }
        levels = ((o["levels"] as? [Any]) ?? []).compactMap { ($0 as? NSNumber)?.intValue }.filter { $0 > 0 }
        levelNow = (o["now"] as? NSNumber)?.intValue ?? 0
    }

    func syncQuality() {
        guard hlsUrl != nil, native == nil else { levels = []; return } // AVPlayer tự chọn bản theo mạng
        web.evaluateJavaScript("window.__chimtoLevels?__chimtoLevels():null") { [weak self] r, _ in self?.showQuality(r) }
    }

    /// Tự động → bản cao nhất (4K) → … → bản thấp nhất → tự động; nhớ cho các phim sau (bản thiếu thì lấy bản thấp hơn gần nhất).
    func cycleQuality() {
        guard levels.count > 1 else { return }
        let options = [0] + levels
        let cur = Prefs.videoQuality
        let i = options.firstIndex(of: cur) ?? options.firstIndex(where: { $0 > 0 && $0 <= cur }) ?? 0
        let next = options[(i + 1) % options.count]
        Prefs.videoQuality = next
        web.evaluateJavaScript("window.__chimtoQuality?__chimtoQuality(\(next)):null") { [weak self] r, _ in self?.showQuality(r) }
        showNotice("Chất lượng: " + (next == 0 ? "Tự động (vừa màn hình)" : qualityName(next)), 1.5)
        objectWillChange.send()
    }

    // MARK: - Tốc độ / khung hình / hẹn giờ tắt

    func cycleRate() {
        let i = Self.rates.firstIndex(of: rate) ?? 0
        rate = Self.rates[(i + 1) % Self.rates.count]
        cmd("rate", rate)
    }

    var rateText: String {
        (rate.truncatingRemainder(dividingBy: 1) == 0 ? String(Int(rate)) : String(rate).replacingOccurrences(of: ".", with: ",")) + "×"
    }

    func toggleFit() {
        Prefs.videoFill.toggle()
        cmd("fit", Prefs.videoFill ? 1 : 0)
        objectWillChange.send()
    }

    var fitText: String { Prefs.videoFill ? "Lấp đầy" : "Vừa màn hình" }

    func cycleSleep() {
        sleepChoice = (sleepChoice + 1) % Self.sleepOptions.count
        let m = Self.sleepOptions[sleepChoice]
        sleepAt = m > 0 ? nowMs() + Int64(m) * 60_000 : 0
    }

    var sleepText: String {
        if sleepAt > 0 { return "còn \(max(1, (sleepAt - nowMs()) / 60_000 + 1)) phút" }
        return Self.sleepOptions[sleepChoice] < 0 ? "hết tập này" : "Tắt"
    }

    /// Hết giờ hẹn: dừng phim, cho 60 giây chạm / bấm phím bất kỳ để xem tiếp.
    private func sleepNow() {
        sleepAt = 0
        sleepChoice = 0
        cmd("pause")
        sleepGraceUntil = nowMs() + 60_000
        showNotice("Hết giờ hẹn tắt. Chạm hoặc bấm phím bất kỳ trong 60 giây để xem tiếp, không thì app tự đóng trình phát.", 60)
    }

    /// Không ai bấm: lưu chỗ xem, đóng trình phát (iPad tự tắt màn hình như thường).
    private func exitForSleep() { close() }

    /// Có người chạm / bấm phím: huỷ đếm ngược sau giờ hẹn.
    func userActivity() {
        if sleepGraceUntil > 0 {
            sleepGraceUntil = 0
            showNotice("Đã huỷ hẹn giờ tắt — xem tiếp.")
        }
    }

    // MARK: - Điều khiển

    func togglePlay() {
        cmd(started ? "toggle" : "play")
        showBar()
    }

    func play() { cmd("play") }
    func pause() { cmd("pause") }

    /// Gộp các lần tua liên tiếp thành một lệnh tua (đỡ giật khi giữ phím / chạm nhiều lần).
    func seekBy(_ delta: Double) {
        guard let s = state, started else { return }
        let maxT = s.d > 0 ? s.d - 1 : Double.greatestFiniteMagnitude
        seekTarget = min(maxT, max(0, (seekTarget ?? s.t) + delta))
        showBar()
        after("seek", 0.5) { [weak self] in self?.flushSeek() }
    }

    /// Bước tua của phím ◀ ▶ (giữ phím: tăng dần, tối đa 4 lần).
    func step(repeatCount: Int) -> Double { Double(Prefs.seekStep) * min(4, 1 + Double(repeatCount) / 8) }

    private func flushSeek() {
        guard let target = seekTarget else { return }
        seekTarget = nil
        resumePending = false // người xem tự tua: từ giờ lấy theo trình phát
        session?.seeked()
        cmd("seekTo", target)
        state?.t = target
    }

    /// Kéo thanh thời gian xong (0…1).
    func seek(toFraction f: Double) {
        guard started, let d = state?.d, d > 0 else { return }
        let target = min(d - 1, max(0, d * f))
        resumePending = false
        session?.seeked()
        cmd("seekTo", target)
        state?.t = target
        showBar()
    }

    /// Phím số 0–9: nhảy tới 0–90%.
    func seekPercent(_ digit: Int) {
        guard let s = state, s.d > 0 else { return }
        seekTarget = s.d * Double(digit) / 10
        showBar()
        flushSeek()
    }

    func cmd(_ name: String, _ arg: Double? = nil) {
        if let n = native {
            if name == "fit" { nativeFill = (arg ?? 0) == 1 } else { n.command(name, arg) }
            return
        }
        let a = arg.map { String($0) } ?? "null"
        web.evaluateJavaScript("window.chimtoCmd&&chimtoCmd('\(name)',\(a))", completionHandler: nil)
    }

    /// Chỗ đang xem để mang sang (đổi server / tải lại / đổi bản): lệnh tua xem tiếp chưa xong thì vẫn là chỗ cũ.
    private func currentTime() -> Int? {
        if resumePending { return resumeAt }
        guard started, let s = state, s.t > 5 else { return nil }
        return Int(s.t)
    }

    /// `periodic`: lần lưu định kỳ khi đang xem (TubeMini gửi lên máy chủ tối đa 15 giây một lần; đóng / đổi video thì gửi ngay).
    func savePosition(periodic: Bool = false) {
        guard let s = state, started, !episodeUrl.isEmpty, !resumePending, !saveDisabled else { return }
        if isTube {
            // Như app TubeMini: xem gần hết (còn < 20 giây) thì lần sau phát từ đầu
            let done = s.ended || (s.d > 0 && s.t > s.d - 20)
            TubeMini.report(episodeUrl, position: done ? 0 : Int(s.t), duration: Int(s.d), force: !periodic)
            return
        }
        // Xem gần hết thì coi như xong (lần sau mở lại từ đầu)
        let done = s.ended || (s.d > 0 && s.t > s.d - 90)
        Prefs.savePosition(episodeUrl, seconds: done ? 0 : Int(s.t), filmUrl: detail?.url ?? filmUrl)
    }

    // MARK: - Chạm

    /// Nhấc tay: thanh nút hiện ngay (không chờ xem có chạm đúp không).
    func tapUp() {
        userActivity()
        if !panelShown && !barShown {
            showBar()
            shownByTap = true
        }
    }

    /// Chắc chắn là chạm một lần: thanh / menu đang hiện từ trước thì ẩn đi.
    func tapConfirmed() {
        if shownByTap {
            shownByTap = false
            return
        }
        if panelShown { hidePanel() } else { hideBar() }
    }

    func doubleTap(left: Bool) {
        shownByTap = false
        seekBy(left ? -10 : 10)
        showNotice(left ? "◀◀ 10 giây" : "10 giây ▶▶", 1.2)
        if !panelShown { showBar() }
    }

    // MARK: - Giới hạn giờ xem (WatchLimit)

    /// Người đang xem đã hết giờ xem: màn «Hết giờ xem» (PlayerView) đang hiện, phim dừng.
    @Published private(set) var limitBlock: WatchLimit.Block?
    /// Cho xem thêm xong thì làm gì (phát tiếp, hoặc mở tập đang chờ).
    private var limitResume: (() -> Void)?
    /// Đã nhắc "còn 5 phút" trong lần xem này.
    private var limitWarned = false

    /// Mỗi lần đọc trạng thái: sắp hết giờ thì nhắc, hết giờ thì dừng phim và hiện màn hết giờ xem.
    private func checkLimit() {
        guard limitBlock == nil else { return }
        if let b = WatchLimit.block() {
            cmd("pause")
            hideBar()
            hidePanel()
            limitBlock = b
            limitResume = { [weak self] in self?.cmd("play") }
            return
        }
        if started, !limitWarned, let r = WatchLimit.remaining(), r > 0, r <= 300 {
            limitWarned = true
            showNotice("Còn \(WatchLimit.minutesText(r)) xem hôm nay (\(Prefs.profile.name)).", 8)
        }
    }

    /// Người lớn đã nhập mã PIN phụ huynh và chọn cho xem thêm (phút; nil = không giới hạn tới 6 giờ sáng).
    func grantLimit(_ minutes: Int?) {
        if let m = minutes { WatchLimit.grant(m) } else { WatchLimit.grantUntilMorning() }
        limitBlock = nil
        limitWarned = false
        let resume = limitResume
        limitResume = nil
        resume?()
    }

    /// Lời giải thích trên màn hết giờ xem.
    var limitText: String {
        let who = Prefs.profile.name
        switch limitBlock {
        case .bedtime: return "Đã tới giờ đi ngủ của \(who) (từ \(WatchLimit.bedtimeLabel(Prefs.bedtime())) tới 6 giờ sáng)."
        case .daily: return "\(who) đã xem hết \(WatchLimit.dailyLabel(Prefs.dailyLimit())) của hôm nay."
        case nil: return ""
        }
    }

    // MARK: - Màn hình khoá / Trung tâm điều khiển / AirPods

    /// Lần bấm tập trước / sau ở màn hình khoá đã xử lý (đếm theo trang phát; mở tập khác thì đếm lại).
    private var remoteSeq = 0

    /// Tên phim, tập, ảnh cho màn hình khoá / Trung tâm điều khiển / AirPods: gửi xuống trang phát (Media Session của
    /// WebKit — player-agent.js); nút phát / dừng / tua do WebKit tự lo, nút tập trước / sau báo ngược lên app.
    private func sendNowPlaying() {
        guard native == nil, let d = detail else { return } // AVPlayer: poll() cập nhật màn hình khoá
        var meta: [String: Any] = ["title": d.title, "artist": episodes[safe: index]?.title ?? "", "album": "MTV"]
        if let p = d.poster, !p.isEmpty { meta["artwork"] = [["src": p, "sizes": "512x512"]] }
        guard let data = try? JSONSerialization.data(withJSONObject: meta), let json = String(data: data, encoding: .utf8) else { return }
        let js = "window.chimtoCmd&&chimtoCmd('meta'," + json.replacingOccurrences(of: "</", with: "<\\/") + ")"
        web.evaluateJavaScript(js, completionHandler: nil)
    }

    // MARK: - Cử chỉ chạm (vuốt, giữ tay, chụm) — xem TouchCatcher

    /// Chữ giữa màn hình lúc vuốt / giữ tay (biểu tượng SF Symbols, chữ chính, dòng phụ).
    struct GestureHint: Equatable {
        let icon: String
        let text: String
        var detail: String?
    }

    @Published private(set) var gestureHint: GestureHint?
    private var scrubFrom: Double?
    private var holdRate: Double?

    /// Đang mở menu thì không nhận cử chỉ (chạm để đóng menu như cũ).
    var gesturesAllowed: Bool { !panelShown && limitBlock == nil }

    /// Bắt đầu vuốt ngang để tua; false = chưa tua được (phim chưa chạy / chưa biết độ dài).
    func scrubBegan() -> Bool {
        guard gesturesAllowed, started, let s = state, s.d > 0 else { return false }
        userActivity()
        cancel("seek")
        scrubFrom = seekTarget ?? s.t
        return true
    }

    /// fraction = quãng ngón tay đã vuốt / bề ngang màn hình (−1…1). Cả bề ngang = 2 phút (phim ngắn hơn: cả phim).
    func scrubChanged(_ fraction: Double) {
        guard let from = scrubFrom, let d = state?.d, d > 0 else { return }
        let target = min(d - 1, max(0, from + fraction * min(d, 120)))
        seekTarget = target
        let delta = target - from
        gestureHint = GestureHint(icon: delta < 0 ? "backward.fill" : "forward.fill",
                                  text: "\(formatTime(target)) / \(formatTime(d))",
                                  detail: (delta < 0 ? "−" : "+") + formatTime(abs(delta)))
        showBar()
    }

    /// Nhấc tay: tua tới chỗ đã chọn; bị huỷ (iPad lấy chạm) thì về như cũ.
    func scrubEnded(apply: Bool) {
        guard scrubFrom != nil else { return }
        scrubFrom = nil
        gestureHint = nil
        if apply { flushSeek() } else { seekTarget = nil }
    }

    /// Giữ tay: xem nhanh 2× tới khi nhấc tay (đang phát mới có tác dụng).
    @discardableResult
    func holdFast(_ on: Bool) -> Bool {
        if on {
            guard gesturesAllowed, started, state?.paused == false, holdRate == nil, rate < 2 else { return false }
            userActivity()
            holdRate = rate
            cmd("rate", 2)
            gestureHint = GestureHint(icon: "hare.fill", text: "Xem nhanh 2×", detail: "nhấc tay để xem tiếp như thường")
            return true
        }
        guard let r = holdRate else { return false }
        holdRate = nil
        cmd("rate", r)
        gestureHint = nil
        return true
    }

    /// Vuốt dọc: hiện mức độ sáng / âm lượng (0…1).
    func showLevel(brightness: Bool, _ v: Double) {
        cancel("hint")
        let icon = brightness ? "sun.max.fill" : (v == 0 ? "speaker.slash.fill" : "speaker.wave.2.fill")
        gestureHint = GestureHint(icon: icon, text: "\(brightness ? "Độ sáng" : "Âm lượng") \(Int((v * 100).rounded()))%")
    }

    func levelEnded() {
        after("hint", 0.6) { [weak self] in self?.gestureHint = nil }
    }

    /// Chụm / mở hai ngón: vừa màn hình / lấp đầy.
    func setFill(_ fill: Bool) {
        userActivity()
        if Prefs.videoFill != fill { toggleFit() }
        showNotice(fitText, 1.2)
    }

    // MARK: - Lớp phủ

    func showBar() {
        barShown = true
        cancel("bar")
        // Đang dừng thì giữ thanh trên màn hình
        if !(started && state?.paused == true) && !dragging {
            after("bar", 5) { [weak self] in self?.barShown = false }
        }
    }

    func hideBar() {
        cancel("bar")
        barShown = false
    }

    func showPanel() {
        hideBar()
        panelShown = true
        syncSubIdx()
        syncQuality()
    }

    /// Phim chia mùa (nhóm tập = mùa, không phải bản Vietsub / Thuyết Minh).
    var seasons: Bool { detail?.seasons == true }

    func hidePanel() { panelShown = false }

    func showNotice(_ text: String, _ seconds: Double = 3.5) {
        notice = text
        after("notice", seconds) { [weak self] in self?.notice = nil }
    }

    private func showLoading(_ text: String) {
        loading = text
        loadingIsError = false
    }

    private func showError(_ text: String) {
        loading = text
        loadingIsError = true
    }

    private func hideLoading() { loading = nil }

    /// ESC / nút Quay lại: đóng menu / bỏ gợi ý / thoát.
    func escape() {
        if panelShown { hidePanel() } else if suggestion != nil { dismissSuggestion() } else { close() }
    }

    /// Menu → «Danh sách tập»: đóng trình phát rồi mở trang phim (mở từ hàng Xem tiếp thì trước đây chỉ về trang chủ).
    func closeToEpisodes() {
        if !isTube { AppModel.shared.episodesAfterPlayer = filmUrl } // TubeMini: về lại danh sách video
        close()
    }

    func close() {
        WatchLimit.flush()
        savePosition()
        session?.save(nowMs())
        session = nil
        onClose()
    }

    // MARK: - Hình trong hình

    static var pipSupported: Bool { AVPictureInPictureController.isPictureInPictureSupported() }

    /// Thu nhỏ phim (cửa sổ hình trong hình của iPad). Gọi thẳng trong khung có <video>: WebKit chỉ cho vào hình trong
    /// hình khi "người dùng vừa bấm" — evaluateJavaScript của app được tính là vậy, qua postMessage của trang bọc thì mất.
    func startPip() {
        guard started, !inPip, limitBlock == nil, native == nil else { return }
        pipRequestedAt = nowMs()
        hidePanel()
        hideBar()
        pipInFrames(true)
    }

    func stopPip() { pipInFrames(false) }

    private func pipInFrames(_ on: Bool) {
        let js = "(window.__chimtoHere&&window.__chimtoHere('pip',\(on ? 1 : 0)))||false"
        web.evaluateJavaScript(js, in: nil, in: .page) { r in Log.d("Pip", "khung chính: \(r)") }
        for f in frames {
            web.evaluateJavaScript(js, in: f.frame, in: .page) { r in Log.d("Pip", "\(f.url.prefix(60)): \(r)") }
        }
    }

    private func pipChanged(_ on: Bool) {
        inPip = on
        Log.d("Pip", on ? "vào hình trong hình" : "ra khỏi hình trong hình (\(state?.paused == true ? "đang dừng" : "đang phát"))")
        if on {
            // Đang dùng app (bấm Thu nhỏ): đóng màn trình phát, phim chạy tiếp ở cửa sổ nổi — dùng các màn khác được
            if UIApplication.shared.applicationState == .active && !detached {
                detached = true
                PlayerModel.detachedModel = self
                onDetach()
            }
        } else if detached {
            if state?.paused == false {
                reattach() // nút phóng to của cửa sổ nhỏ: mở lại màn trình phát (trình phát vẫn chạy)
            } else {
                closeDetached() // nút ✕ của cửa sổ nhỏ (phim dừng)
            }
        }
    }

    /// Mở lại màn trình phát cho phim vừa ra khỏi cửa sổ nhỏ. App chưa hiện lên hẳn (bấm phóng to lúc đang ở màn hình
    /// chính iPad — iOS mở app rồi mới hiện) thì đợi tới appBecameActive, không thì màn trình phát không mở được mà phim
    /// vẫn chạy ngầm.
    private func reattach() {
        guard UIApplication.shared.applicationState == .active else {
            waitingToReattach = true
            return
        }
        waitingToReattach = false
        detached = false
        PlayerModel.detachedModel = nil
        Router.present(self)
    }

    /// App vừa hiện lên (ConBangTVApp): đang đợi mở lại màn trình phát thì mở.
    func appBecameActive() {
        if waitingToReattach { reattach() }
    }

    /// Vuốt lên từ mép dưới màn hình lúc đang xem (PlayerController để lại lần vuốt đầu cho app): thu nhỏ phim — app vẫn
    /// đang mở hẳn nên iPad cho vào hình trong hình; vuốt tiếp mới về màn hình chính, phim vẫn nổi.
    func swipeUpFromEdge() {
        guard Prefs.pipOnHome, Self.pipSupported, started, state?.paused == false, !inPip, !detached else { return }
        Log.d("Pip", "vuốt lên từ mép dưới — thu nhỏ phim")
        startPip()
    }

    /// Đóng phim đang ở cửa sổ hình trong hình (màn trình phát đã đóng): lưu chỗ xem, huỷ trình phát.
    func closeDetached() {
        guard detached else { return }
        detached = false
        if PlayerModel.detachedModel === self { PlayerModel.detachedModel = nil }
        savePosition()
        session?.save(nowMs())
        session = nil
        teardown()
        AppModel.shared.playerClosed()
    }

    /// Đổi người xem: lưu chỗ xem cho người cũ rồi đóng trình phát (đang hình trong hình / đang mở).
    static func closeForProfileSwitch() {
        if let m = detachedModel {
            m.closeDetached()
            m.saveDisabled = true
        }
        if let c = Router.topController as? PlayerController {
            c.model.close()
            c.model.saveDisabled = true
        }
    }

    // MARK: - Vòng đời

    // Không tự thu nhỏ lúc app sắp ra nền (willResignActive): đo trên iPad 15.4 — lệnh tới đúng khung video nhưng iPad
    // không mở cửa sổ nổi khi app đã thôi "đang mở hẳn" (5/11/2026). Vì vậy dùng lần vuốt đầu từ mép dưới (swipeUpFromEdge).

    func didEnterBackground() {
        WatchLimit.flush()
        savePosition()
        session?.save(nowMs())
        // Hình trong hình (hoặc vừa bấm thu nhỏ, cửa sổ đang mở ra): phim chạy tiếp; không vào được thì dừng (khỏi phát
        // tiếng ở nền)
        if inPip { return }
        if nowMs() - pipRequestedAt < 3000 {
            after("pipcheck", 2.5) { [weak self] in
                guard let self = self, !self.inPip, UIApplication.shared.applicationState == .background else { return }
                Log.d("Pip", "không vào được hình trong hình — dừng phim")
                self.cmd("pause")
            }
            return
        }
        cmd("pause")
    }

    func teardown() {
        WatchLimit.flush()
        if PlayerModel.detachedModel === self { PlayerModel.detachedModel = nil }
        token += 1
        polling?.cancel()
        polling = nil
        for t in timers.values { t.cancel() }
        timers.removeAll()
        stopNative()
        web.stopLoading()
        web.configuration.userContentController.removeAllUserScripts()
        web.configuration.userContentController.removeScriptMessageHandler(forName: "chimtoLog")
        if let blank = URL(string: "about:blank") { web.load(URLRequest(url: blank)) }
        if PlayerModel.current === self { PlayerModel.current = nil }
    }

    private func after(_ key: String, _ seconds: Double, _ body: @escaping @MainActor () -> Void) {
        timers[key]?.cancel()
        timers[key] = Task { @MainActor in
            try? await Task.sleep(nanoseconds: UInt64(seconds * 1_000_000_000))
            if Task.isCancelled { return }
            body()
        }
    }

    private func cancel(_ key: String) {
        timers[key]?.cancel()
        timers[key] = nil
    }

    // MARK: - WKNavigationDelegate / WKUIDelegate

    // Ghi rõ bộ chọn Objective-C: SDK mới gắn @MainActor @Sendable vào khối hoàn tất, chữ ký Swift lệch một chút
    // thì phương thức vẫn được WebKit gọi.

    /// Chặn mọi điều hướng cả trang do trang tự gây ra (quảng cáo); app tự nạp trang bằng code.
    /// Chuyển hướng trong cùng trang (vd http → https của trang tập) thì cho qua.
    @objc(webView:decidePolicyForNavigationAction:decisionHandler:)
    func webView(_ webView: WKWebView, decidePolicyFor action: WKNavigationAction,
                 decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
        // Nhớ các khung con (iframe trình phát…) để cổng gỡ lỗi chạy JS ngay trong khung đó
        if let f = action.targetFrame, !f.isMainFrame, let u = action.request.url?.absoluteString {
            frames.removeAll { $0.url == u }
            frames.append((u, f))
            if frames.count > 40 { frames.removeFirst() }
        }
        let isMain = action.targetFrame?.isMainFrame ?? true
        guard isMain, let url = action.request.url else {
            decisionHandler(.allow)
            return
        }
        let s = url.absoluteString
        if url.scheme == "about" || s == expectedURL || (expectedHost != nil && Site.hostOf(s) == expectedHost) {
            decisionHandler(.allow)
            return
        }
        Log.d("Player", "chặn điều hướng cả trang: \(s)")
        decisionHandler(.cancel)
    }

    /// Không mở cửa sổ mới (popup quảng cáo).
    @objc(webView:createWebViewWithConfiguration:forNavigationAction:windowFeatures:)
    func webView(_ webView: WKWebView, createWebViewWith configuration: WKWebViewConfiguration,
                 for navigationAction: WKNavigationAction, windowFeatures: WKWindowFeatures) -> WKWebView? {
        Log.d("Player", "chặn cửa sổ mới: \(navigationAction.request.url?.absoluteString ?? "")")
        return nil
    }

    @objc(webViewWebContentProcessDidTerminate:)
    func webViewWebContentProcessDidTerminate(_ webView: WKWebView) {
        Log.d("Player", "tiến trình web bị tắt — nạp lại")
        reload()
    }

    @objc(webView:didFailNavigation:withError:)
    func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
        Log.d("Player", "lỗi nạp trang: \(error.localizedDescription)")
    }

    @objc(webView:didFailProvisionalNavigation:withError:)
    func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
        let e = error as NSError
        if e.code == NSURLErrorCancelled { return }
        Log.d("Player", "lỗi mở trang: \(e.localizedDescription)")
        if pageMode && !started { showError("Không mở được trang tập phim.\n\(friendly(error))") }
    }

    // MARK: - Gỡ lỗi

    /// Chạy JS trong khung con có địa chỉ chứa `match` (vd "streamfree"); trả (kết quả, lỗi).
    func evalInFrame(_ match: String, _ js: String) async -> (Any?, String?) {
        guard let f = frames.last(where: { $0.url.contains(match) })?.frame else {
            return (nil, "không thấy khung \(match) — có: \(frames.map { $0.url })")
        }
        return await withCheckedContinuation { cont in
            web.evaluateJavaScript(js, in: f, in: .page) { r in
                switch r {
                case .success(let v): cont.resume(returning: (v, nil))
                case .failure(let e): cont.resume(returning: (nil, e.localizedDescription))
                }
            }
        }
    }

    func debugState() -> [String: Any] {
        var o: [String: Any] = [
            "title": title, "episode": episodeUrl, "started": started, "pageMode": pageMode, "native": native != nil,
            "nativeDebug": native?.debugInfo ?? NSNull(),
            "loading": loading ?? NSNull(), "notice": notice ?? NSNull(), "bar": barShown, "panel": panelShown,
            "server": server?.type ?? NSNull(), "servers": servers.map { ["label": $0.label, "type": $0.type] },
            "groups": groups.map { $0.name }, "group": group?.name ?? NSNull(),
            "embed": embedUrl ?? NSNull(), "hls": hlsUrl ?? NSNull(), "startHint": startHint, "resumeAt": resumeAt,
            "pageLoad": pageLoad, "rate": rate, "session": sessionText ?? NSNull(),
            "pip": inPip, "detached": detached,
        ]
        if let h = gestureHint { o["gestureHint"] = [h.icon, h.text, h.detail ?? ""] }
        o["seekTarget"] = seekTarget ?? NSNull()
        o["fill"] = Prefs.videoFill
        if let s = state {
            o["state"] = ["t": s.t, "d": s.d, "paused": s.paused, "ended": s.ended, "waiting": s.waiting, "ready": s.ready,
                          "df": s.droppedFrames, "tf": s.totalFrames]
        }
        return o
    }
}
