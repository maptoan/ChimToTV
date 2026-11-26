import AVFoundation
import MediaPlayer
import Network
import SwiftUI
import UIKit

// Trình phát gốc (AVPlayer) cho phim khi chiếu lên màn xe CarPlay — luồng HLS (.m3u8) / tệp video trực tiếp (KKPhim…).
//
// Vì sao: trình phát web (WebKit) đặt lớp video ở tiến trình khác, chạy theo nhịp màn điện thoại — màn iPhone tắt thì khung
// hình bị bỏ, màn xe đen thui, chỉ có tiếng (đo 25/11/2026 trên iPhone 6s Plus). AVPlayer vẽ ngay trong app, theo màn xe: tắt màn
// iPhone vẫn có hình (thử bằng trình phát Truyền hình). Luồng mã hoá trong trang nhúng (server riêng của trang) vẫn phát bằng
// trình phát web — app không phá lớp bảo vệ.
//
// Trả trạng thái cùng dạng window.__chimtoState của player-agent.js và nhận cùng lệnh chimtoCmd (play / pause / toggle / seekTo /
// rate) → PlayerModel dùng chung mọi phần còn lại: xem tiếp, tập sau, nhạc mở đầu, chấm điểm server, hết giờ xem…
// Danh sách phát đọc qua mạng của app (DoH / vượt chặn / proxy) và bỏ khối quảng cáo KKPhim như hls.html (trên iPhone hls.html
// giao luồng cho <video> của iOS nên không bỏ được); các đoạn video, khoá giải mã thì AVPlayer tự tải.

@MainActor
final class NativeFilm: NSObject {
    let player = AVPlayer()
    private let loader: PlaylistLoader
    private var item: AVPlayerItem?
    private var statusObs: NSKeyValueObservation?
    private var noteObs: [NSObjectProtocol] = []
    private let startAt: Double
    /// Đã tua tới chỗ bắt đầu (hoặc không cần) — từ lúc này mới phát / báo «sẵn sàng».
    private var primed = false
    /// Người xem muốn phát (tự phát khi mở như hls.html; bấm dừng thì thôi).
    private var wantPlay = true
    private var ended = false
    private(set) var failed: String?
    private var rate: Float = 1
    /// Nút trên màn hình khoá / «Đang phát» của CarPlay / tai nghe: "next" | "prev" (PlayerModel mở tập).
    var onRemote: (String) -> Void = { _ in }
    private var remoteTargets: [(MPRemoteCommand, Any)] = []

    /// `hls`: danh sách phát HLS (đọc qua app, viết lại địa chỉ); false = tệp video (MP4) AVPlayer tải thẳng.
    init(url: URL, hls: Bool, referer: String?, start: Double) {
        loader = PlaylistLoader(referer: referer)
        startAt = start
        super.init()
        MediaRelay.shared.headers = loader.headers
        player.automaticallyWaitsToMinimizeStalling = true
        player.allowsExternalPlayback = false
        let real = hls ? PlaylistLoader.wrap(url) : url
        let asset = AVURLAsset(url: real, options: ["AVURLAssetHTTPHeaderFieldsKey": loader.headers])
        asset.resourceLoader.setDelegate(loader, queue: loader.queue)
        let it = AVPlayerItem(asset: asset)
        it.preferredForwardBufferDuration = 20
        item = it
        statusObs = it.observe(\.status, options: [.new]) { [weak self] it, _ in
            let st = it.status, err = it.error?.localizedDescription
            guard let me = self else { return }
            Task { @MainActor in me.statusChanged(st, err) }
        }
        let nc = NotificationCenter.default
        noteObs.append(nc.addObserver(forName: .AVPlayerItemDidPlayToEndTime, object: it, queue: .main) { [weak self] _ in
            guard let me = self else { return }
            Task { @MainActor in me.ended = true }
        })
        noteObs.append(nc.addObserver(forName: .AVPlayerItemFailedToPlayToEndTime, object: it, queue: .main) { [weak self] n in
            let why = (n.userInfo?[AVPlayerItemFailedToPlayToEndTimeErrorKey] as? Error)?.localizedDescription ?? "luồng bị ngắt"
            guard let me = self else { return }
            Task { @MainActor in me.failed = why }
        })
        player.replaceCurrentItem(with: it)
        registerRemote()
    }

    private func statusChanged(_ st: AVPlayerItem.Status, _ err: String?) {
        switch st {
        case .readyToPlay:
            guard !primed else { return }
            if startAt > 1 {
                let tol = CMTime(seconds: 1, preferredTimescale: 600)
                player.seek(to: CMTime(seconds: startAt, preferredTimescale: 600), toleranceBefore: .zero, toleranceAfter: tol) { [weak self] _ in
                    guard let me = self else { return }
                    Task { @MainActor in me.primedNow() }
                }
            } else {
                primedNow()
            }
        case .failed:
            failed = err ?? "không mở được luồng"
        default:
            break
        }
    }

    private func primedNow() {
        primed = true
        if wantPlay { play() }
    }

    /// Cùng dạng window.__chimtoState (player-agent.js).
    func state() -> [String: Any] {
        let t = player.currentTime().seconds
        var d = item?.duration.seconds ?? 0
        if !d.isFinite || d <= 0 {
            d = item?.seekableTimeRanges.last.map { CMTimeRangeGetEnd($0.timeRangeValue).seconds } ?? 0
        }
        let tc = player.timeControlStatus
        return [
            "t": t.isFinite ? t : 0, "d": d.isFinite ? d : 0, "paused": tc == .paused, "ended": ended,
            "waiting": tc == .waitingToPlayAtSpecifiedRate, "ready": primed && item?.status == .readyToPlay,
            "df": 0, "tf": 0, "err": failed ?? "", "ad": false, "pip": false,
        ]
    }

    /// Cho cổng gỡ lỗi (/state → player.nativeDebug).
    var debugInfo: [String: Any] {
        [
            "status": item?.status.rawValue ?? -1, "timeControl": player.timeControlStatus.rawValue,
            "waitReason": player.reasonForWaitingToPlay?.rawValue ?? "", "primed": primed, "wantPlay": wantPlay,
            "failed": failed ?? "", "loader": loader.notes,
            "errorLog": item?.errorLog()?.events.suffix(4).map {
                "\($0.errorStatusCode) \($0.errorDomain) \($0.errorComment ?? "") \($0.uri ?? "")"
            } ?? [],
            "loaded": item?.loadedTimeRanges.map { r -> String in
                let t = r.timeRangeValue
                return "\(Int(t.start.seconds))+\(Int(t.duration.seconds))"
            } ?? [],
        ]
    }

    /// Cùng lệnh chimtoCmd (player-agent.js); "fit" do PlayerModel đổi lớp hình.
    func command(_ name: String, _ arg: Double?) {
        switch name {
        case "play": play()
        case "pause": pause()
        case "toggle":
            if player.timeControlStatus == .paused && !(wantPlay && !primed) { play() } else { pause() }
        case "seekTo":
            if let s = arg { seek(s) }
        case "rate":
            if let r = arg {
                rate = Float(r)
                if player.rate != 0 { player.rate = rate }
            }
        default:
            break
        }
    }

    private func play() {
        wantPlay = true
        guard primed else { return }
        if ended {
            ended = false
            seek(0)
        }
        player.play()
        if rate != 1 { player.rate = rate }
    }

    private func pause() {
        wantPlay = false
        player.pause()
    }

    private func seek(_ s: Double) {
        ended = false
        let tol = CMTime(seconds: 0.5, preferredTimescale: 600)
        player.seek(to: CMTime(seconds: max(0, s), preferredTimescale: 600), toleranceBefore: tol, toleranceAfter: tol)
    }

    /// Màn hình khoá / «Đang phát» của CarPlay: tên phim, tập, thời gian.
    func nowPlaying(title: String, subtitle: String, duration: Double, elapsed: Double, playing: Bool) {
        MPNowPlayingInfoCenter.default().nowPlayingInfo = [
            MPMediaItemPropertyTitle: title, MPMediaItemPropertyArtist: subtitle, MPMediaItemPropertyAlbumTitle: "MTV",
            MPMediaItemPropertyPlaybackDuration: duration, MPNowPlayingInfoPropertyElapsedPlaybackTime: elapsed,
            MPNowPlayingInfoPropertyPlaybackRate: playing ? Double(rate) : 0,
        ]
    }

    private func registerRemote() {
        let c = MPRemoteCommandCenter.shared()
        func add(_ command: MPRemoteCommand, _ run: @escaping @MainActor (NativeFilm) -> Void) {
            command.isEnabled = true
            let target = command.addTarget { [weak self] _ in
                guard let me = self else { return .commandFailed }
                Task { @MainActor in run(me) }
                return .success
            }
            remoteTargets.append((command, target))
        }
        add(c.playCommand) { $0.play() }
        add(c.pauseCommand) { $0.pause() }
        add(c.togglePlayPauseCommand) { $0.command("toggle", nil) }
        add(c.nextTrackCommand) { $0.onRemote("next") }
        add(c.previousTrackCommand) { $0.onRemote("prev") }
    }

    func stop() {
        player.pause()
        player.replaceCurrentItem(with: nil)
        statusObs = nil
        noteObs.forEach { NotificationCenter.default.removeObserver($0) }
        noteObs = []
        for (command, target) in remoteTargets { command.removeTarget(target) }
        remoteTargets = []
        loader.cancelAll()
        MPNowPlayingInfoCenter.default().nowPlayingInfo = nil
    }
}

// MARK: - Danh sách phát HLS qua mạng của app

/// AVPlayer hỏi danh sách phát qua lược đồ riêng (cbhls / cbhlss) → app tải bằng Http (DoH / vượt chặn / proxy), viết lại địa
/// chỉ: danh sách con tiếp tục đi qua đây, đoạn video / khoá giải mã thành địa chỉ đầy đủ để AVPlayer tự tải; bỏ khối quảng
/// cáo KKPhim.
final class PlaylistLoader: NSObject, AVAssetResourceLoaderDelegate {
    let queue = DispatchQueue(label: "vn.chimto.playlist")
    let headers: [String: String]
    private let lock = NSLock()
    private var tasks: [Task<Void, Never>] = []
    private var noteList: [String] = []
    /// Các lần AVPlayer hỏi danh sách phát (cho cổng gỡ lỗi).
    var notes: [String] {
        lock.lock()
        defer { lock.unlock() }
        return noteList
    }

    private func note(_ s: String) {
        lock.lock()
        noteList.append(s)
        if noteList.count > 12 { noteList.removeFirst(noteList.count - 12) }
        lock.unlock()
    }

    init(referer: String?) {
        var h = ["User-Agent": Http.userAgent]
        if let r = referer { h["Referer"] = r }
        headers = h
    }

    private static let schemes = ["http": "cbhls", "https": "cbhlss"]

    static func wrap(_ u: URL) -> URL {
        guard var c = URLComponents(url: u, resolvingAgainstBaseURL: true), let s = c.scheme?.lowercased(),
              let w = schemes[s] else { return u }
        c.scheme = w
        return c.url ?? u
    }

    static func unwrap(_ u: URL) -> URL? {
        guard var c = URLComponents(url: u, resolvingAgainstBaseURL: true), let s = c.scheme?.lowercased(),
              let real = schemes.first(where: { $0.value == s })?.key else { return nil }
        c.scheme = real
        return c.url
    }

    func resourceLoader(_ resourceLoader: AVAssetResourceLoader,
                        shouldWaitForLoadingOfRequestedResource req: AVAssetResourceLoadingRequest) -> Bool {
        guard let u = req.request.url, let real = Self.unwrap(u) else { return false }
        let headers = self.headers
        let dr = req.dataRequest
        note("hỏi \(real.lastPathComponent) info=\(req.contentInformationRequest != nil) "
             + "range=\(dr?.requestedOffset ?? -1)+\(dr?.requestedLength ?? -1) all=\(dr?.requestsAllDataToEndOfResource ?? false)")
        let t = Task {
            do {
                let r = try await Http.get(real.absoluteString, headers: headers)
                guard !Task.isCancelled else { return }
                await MediaRelay.shared.ready()
                var data = r.data
                let text = String(decoding: r.data, as: UTF8.self)
                if text.contains("#EXTM3U") {
                    let out = Self.rewrite(text, base: URL(string: r.url) ?? real)
                    data = Data(out.text.utf8)
                    if out.ads > 0 { Log.d("Player", "AVPlayer: bỏ \(out.ads) khối quảng cáo trong danh sách phát") }
                }
                if let info = req.contentInformationRequest {
                    info.contentType = "public.m3u-playlist"
                    info.contentLength = Int64(data.count)
                    info.isByteRangeAccessSupported = false
                }
                req.dataRequest?.respond(with: data)
                req.finishLoading()
                self.note("trả \(real.lastPathComponent) \(data.count) byte")
            } catch {
                self.note("lỗi \(real.lastPathComponent): \(error.localizedDescription)")
                Log.d("Player", "AVPlayer: không tải được danh sách phát \(real.host ?? "") — \(error.localizedDescription)")
                req.finishLoading(with: error)
            }
        }
        lock.lock()
        tasks.append(t)
        lock.unlock()
        return true
    }

    func cancelAll() {
        lock.lock()
        tasks.forEach { $0.cancel() }
        tasks = []
        lock.unlock()
    }

    // MARK: Viết lại danh sách phát

    static func rewrite(_ text: String, base: URL) -> (text: String, ads: Int) {
        var out: [String] = []
        var nextIsVariant = false
        for raw in text.components(separatedBy: .newlines) {
            let line = raw.trimmingCharacters(in: .whitespaces)
            if line.isEmpty { continue }
            if line.hasPrefix("#") {
                if line.hasPrefix("#EXT-X-STREAM-INF") { nextIsVariant = true }
                out.append(rewriteUriAttr(line, base: base))
                continue
            }
            guard let abs = URL(string: line, relativeTo: base)?.absoluteURL else {
                out.append(line)
                continue
            }
            let variant = nextIsVariant || abs.path.lowercased().hasSuffix(".m3u8")
            nextIsVariant = false
            out.append((variant ? wrap(abs) : MediaRelay.shared.url(for: abs)).absoluteString)
        }
        return stripAds(out)
    }

    /// URI="…" trong thẻ (#EXT-X-KEY, #EXT-X-MAP, #EXT-X-MEDIA…): danh sách con (EXT-X-MEDIA) đi qua đây, khoá / đoạn đầu
    /// qua cổng chuyển tiếp.
    private static func rewriteUriAttr(_ line: String, base: URL) -> String {
        guard let r = line.range(of: "URI=\"") else { return line }
        let rest = line[r.upperBound...]
        guard let end = rest.firstIndex(of: "\""), let abs = URL(string: String(rest[..<end]), relativeTo: base)?.absoluteURL,
              abs.scheme == "http" || abs.scheme == "https" else { return line }
        let playlist = line.hasPrefix("#EXT-X-MEDIA") && abs.path.lowercased().hasSuffix(".m3u8")
        return line.replacingCharacters(in: r.upperBound..<end, with: (playlist ? wrap(abs) : MediaRelay.shared.url(for: abs)).absoluteString)
    }

    /// KKPhim chèn quảng cáo cờ bạc ~30 s giữa phim: một khối DISCONTINUITY mà mọi đoạn đều có dạng
    /// /vN/<32 ký tự hex>/segment_NNNN.ts → bỏ cả khối (cùng quy tắc hls.html; khối convertvN/… là phim có chữ đè — giữ).
    private static let ad = try? NSRegularExpression(pattern: #"/v\d+/[0-9a-f]{32}/segment_\d+\.ts"#)

    static func stripAds(_ lines: [String]) -> (text: String, ads: Int) {
        guard lines.contains(where: { $0.hasPrefix("#EXT-X-DISCONTINUITY") && !$0.hasPrefix("#EXT-X-DISCONTINUITY-SEQUENCE") }),
              let ad = ad else { return (lines.joined(separator: "\n"), 0) }
        func isUri(_ l: String) -> Bool { !l.isEmpty && !l.hasPrefix("#") }
        var head: [String] = []
        var blocks: [[String]] = []
        for l in lines {
            if l.hasPrefix("#EXT-X-DISCONTINUITY") && !l.hasPrefix("#EXT-X-DISCONTINUITY-SEQUENCE") {
                blocks.append([l])
            } else if !blocks.isEmpty || l.hasPrefix("#EXTINF") || isUri(l) {
                if blocks.isEmpty { blocks.append([]) }
                blocks[blocks.count - 1].append(l)
            } else {
                head.append(l)
            }
        }
        var out = head
        var removed = 0
        for b in blocks {
            let segs = b.filter(isUri)
            let allAds = !segs.isEmpty && segs.allSatisfy { s in
                ad.firstMatch(in: s, range: NSRange(s.startIndex..., in: s)) != nil
            }
            if allAds {
                // Khối quảng cáo nằm cuối thì giữ dấu hết danh sách
                out += b.filter { $0.hasPrefix("#EXT-X-ENDLIST") }
                removed += 1
                continue
            }
            out += b
        }
        return (out.joined(separator: "\n"), removed)
    }
}

// MARK: - Cổng chuyển tiếp nội bộ cho đoạn video

/// Cổng HTTP nội bộ (127.0.0.1) cho đoạn video / khoá giải mã của AVPlayer: tải qua mạng của app (Http — DNS mã hoá / vượt
/// chặn / proxy). Mạng của máy hay chặn tên miền bằng DNS (trả 0.0.0.0 → AVPlayer báo «URL không hợp lệ» — đo 25/11/2026 với
/// máy chủ phim1280.tv của KKPhim; nhà mạng cũng chặn kiểu này) mà trình phát web / Http của app đi qua được.
final class MediaRelay: @unchecked Sendable {
    static let shared = MediaRelay()

    /// Referer / User-Agent của phim đang phát (mỗi lúc một phim).
    var headers: [String: String] {
        get { lock.lock(); defer { lock.unlock() }; return hdrs }
        set { lock.lock(); hdrs = newValue; lock.unlock() }
    }

    private let lock = NSLock()
    private var hdrs: [String: String] = [:]
    private var listener: NWListener?
    private var port: UInt16 = 0
    private var waiters: [CheckedContinuation<Void, Never>] = []
    private var settled = false
    private let queue = DispatchQueue(label: "vn.chimto.relay")

    /// Mở cổng (lần đầu) và chờ cổng sẵn sàng — tối đa ~2 giây; không mở được thì đoạn video đi thẳng như cũ.
    func ready() async {
        lock.lock()
        if settled {
            lock.unlock()
            return
        }
        let first = listener == nil
        lock.unlock()
        if first { start() }
        await withCheckedContinuation { (c: CheckedContinuation<Void, Never>) in
            lock.lock()
            if settled {
                lock.unlock()
                c.resume()
            } else {
                waiters.append(c)
                lock.unlock()
            }
        }
    }

    private func start() {
        let params = NWParameters.tcp
        params.requiredLocalEndpoint = .hostPort(host: "127.0.0.1", port: .any)
        guard let l = try? NWListener(using: params) else {
            settle(port: 0)
            return
        }
        lock.lock()
        listener = l
        lock.unlock()
        l.stateUpdateHandler = { [weak self] st in
            switch st {
            case .ready: self?.settle(port: l.port?.rawValue ?? 0)
            case .failed, .cancelled: self?.settle(port: 0)
            default: break
            }
        }
        l.newConnectionHandler = { [weak self] c in self?.serve(c) }
        l.start(queue: queue)
        queue.asyncAfter(deadline: .now() + 2) { [weak self] in self?.settle(port: 0) }
    }

    private func settle(port p: UInt16) {
        lock.lock()
        guard !settled else {
            lock.unlock()
            return
        }
        settled = true
        port = p
        let w = waiters
        waiters = []
        lock.unlock()
        Log.d("Player", p > 0 ? "cổng chuyển tiếp đoạn video: 127.0.0.1:\(p)" : "không mở được cổng chuyển tiếp — đoạn video đi thẳng")
        w.forEach { $0.resume() }
    }

    /// Địa chỉ qua cổng chuyển tiếp (chưa có cổng thì giữ nguyên).
    func url(for real: URL) -> URL {
        lock.lock()
        let p = port
        lock.unlock()
        guard p > 0 else { return real }
        let token = Data(real.absoluteString.utf8).base64EncodedString()
            .replacingOccurrences(of: "+", with: "-").replacingOccurrences(of: "/", with: "_").replacingOccurrences(of: "=", with: "")
        // Giữ đuôi tệp (.ts / .key…) cho AVPlayer đoán loại dữ liệu
        let ext = real.pathExtension.isEmpty ? "" : "." + real.pathExtension
        return URL(string: "http://127.0.0.1:\(p)/r/\(token)\(ext)") ?? real
    }

    private static func decode(_ token: String) -> URL? {
        var b = token.replacingOccurrences(of: "-", with: "+").replacingOccurrences(of: "_", with: "/")
        while b.count % 4 != 0 { b += "=" }
        guard let d = Data(base64Encoded: b), let s = String(data: d, encoding: .utf8) else { return nil }
        return URL(string: s)
    }

    // MARK: Phục vụ một kết nối (một yêu cầu, xong thì đóng)

    private func serve(_ c: NWConnection) {
        c.start(queue: queue)
        read(c, Data())
    }

    private func read(_ c: NWConnection, _ buf: Data) {
        c.receive(minimumIncompleteLength: 1, maximumLength: 16_384) { [weak self] data, _, done, err in
            guard let self = self else { return c.cancel() }
            var b = buf
            if let d = data { b.append(d) }
            if let end = b.range(of: Data("\r\n\r\n".utf8)) {
                self.handle(c, String(decoding: b[..<end.lowerBound], as: UTF8.self))
            } else if done || err != nil || b.count > 65_536 {
                c.cancel()
            } else {
                self.read(c, b)
            }
        }
    }

    private func handle(_ c: NWConnection, _ head: String) {
        let lines = head.components(separatedBy: "\r\n")
        let parts = lines.first?.split(separator: " ") ?? []
        guard parts.count >= 2, parts[0] == "GET" || parts[0] == "HEAD", parts[1].hasPrefix("/r/") else {
            return reply(c, 400, "text/plain", Data("bad request".utf8), extra: [])
        }
        var token = String(parts[1].dropFirst(3))
        if let dot = token.firstIndex(of: ".") { token = String(token[..<dot]) }
        guard let real = Self.decode(token) else { return reply(c, 400, "text/plain", Data("bad token".utf8), extra: []) }
        var headers = self.headers
        // Chuyển tiếp yêu cầu một khúc (Range) nếu AVPlayer có hỏi
        for l in lines.dropFirst() {
            let kv = l.split(separator: ":", maxSplits: 1).map { $0.trimmingCharacters(in: .whitespaces) }
            if kv.count == 2, kv[0].lowercased() == "range" { headers["Range"] = kv[1] }
        }
        let headOnly = parts[0] == "HEAD"
        let sendHeaders = headers
        Task {
            do {
                let req = try Http.request(real.absoluteString, headers: sendHeaders)
                let (data, resp) = try await Http.session.data(for: req)
                let h = resp as? HTTPURLResponse
                let code = h?.statusCode ?? 200
                var extra: [String] = []
                if let cr = h?.value(forHTTPHeaderField: "Content-Range") { extra.append("Content-Range: \(cr)") }
                if h?.value(forHTTPHeaderField: "Accept-Ranges") != nil { extra.append("Accept-Ranges: bytes") }
                let type = h?.value(forHTTPHeaderField: "Content-Type") ?? "application/octet-stream"
                self.reply(c, code, type, headOnly ? Data() : data, extra: extra, length: data.count)
            } catch {
                self.reply(c, 502, "text/plain", Data(error.localizedDescription.utf8), extra: [])
            }
        }
    }

    private func reply(_ c: NWConnection, _ code: Int, _ type: String, _ body: Data, extra: [String], length: Int? = nil) {
        let reason = code == 200 ? "OK" : code == 206 ? "Partial Content" : "Error"
        var head = "HTTP/1.1 \(code) \(reason)\r\nContent-Type: \(type)\r\nContent-Length: \(length ?? body.count)\r\n"
        for e in extra { head += e + "\r\n" }
        head += "Connection: close\r\n\r\n"
        var out = Data(head.utf8)
        out.append(body)
        c.send(content: out, completion: .contentProcessed { _ in c.cancel() })
    }
}

// MARK: - Lớp hình

/// Lớp hình AVPlayer của trình phát phim (màn xe).
struct NativeVideoSurface: UIViewRepresentable {
    let player: AVPlayer
    let fill: Bool

    final class LayerView: UIView {
        override class var layerClass: AnyClass { AVPlayerLayer.self }
        var playerLayer: AVPlayerLayer { layer as! AVPlayerLayer }
    }

    func makeUIView(context: Context) -> LayerView {
        let v = LayerView()
        v.backgroundColor = .black
        v.isUserInteractionEnabled = false
        v.playerLayer.player = player
        return v
    }

    func updateUIView(_ v: LayerView, context: Context) {
        if v.playerLayer.player !== player { v.playerLayer.player = player }
        v.playerLayer.videoGravity = fill ? .resizeAspectFill : .resizeAspect
    }
}
