import Foundation

// TubeMini — xem YouTube qua máy chủ tubemini-server của anh (dự án E:\DEV_CODE\TubeMini: Rust + yt-dlp + Deno, chạy trong nhà).
// App chỉ là «app khách» theo đúng hợp đồng docs/API.md của dự án đó: danh sách video là feed Atom kiểu GData (tìm kiếm, phổ biến,
// liên quan, video của kênh), các tính năng riêng ở /tm/v1 (link phát, kênh theo dõi, feed, lịch sử kèm chỗ xem). Video không đi qua
// máy chủ: máy chủ trả playlist HLS đã lọc (tiếng gốc, bỏ lồng tiếng AI), đoạn video tải thẳng từ googlevideo.
//
// Phát bằng chính trình phát phim (PlayerModel): danh sách video đang xem = các «tập» của một phim giả (tubemini://list/…), mỗi
// video = tubemini://video/<id>, các mức chất lượng = «server» (1080p / 720p / 360p / Chỉ nghe) → có sẵn thanh nút, tua, video kế,
// giới hạn giờ xem, màn xe CarPlay. Luôn phát bằng AVPlayer (NativeFilm). Theo dõi kênh và lịch sử nằm trên máy chủ (dùng chung với
// app TubeMini, không chia theo người xem — anh chọn 25/11/2026), không ghi vào lịch sử phim của MTV.
//
// Máy chủ 0.7.0 (D39–D44 của TubeMini_Server, 25/11/2026): ra Internet qua Cloudflare, mỗi thiết bị một mã nằm trong địa chỉ
// (`https://<tên miền>/u/<mã>`, bot Telegram @tube_mini_bot cấp; trong nhà không mã = chủ máy chủ), kênh theo dõi / lịch sử theo
// người dùng của mã; bỏ X-Auth-Token và chế độ phát qua máy chủ. App chỉ ghép đường dẫn vào sau địa chỉ (giữ nguyên mã), link máy chủ
// trả về đã có sẵn mã.

struct TubeVideo: Identifiable, Hashable {
    let id: String
    var title: String
    var author: String = ""
    var authorId: String = ""
    var duration = 0
    var views: Int?
    var published: Date?
    /// Chỗ đang xem (giây) — có ở danh sách Lịch sử
    var position: Int?

    /// Ảnh đại diện một tấm (tranh của «phim giả», màn khoá, màn xe): hqdefault 480×360 — video nào cũng có.
    var thumb: String { "https://i.ytimg.com/vi/\(id)/hqdefault.jpg" }
    /// Ảnh nét cho ô video / màn «Chỉ nghe» (anh yêu cầu 26/11/2026 — mqdefault 320×180 mờ trên màn Retina), thử lần lượt: hq720
    /// 1280×720 (video HD có), sddefault 640×480, hqdefault 480×360 (luôn có). Ảnh 4:3 có viền đen trên dưới — khung 16:9 cắt mất.
    var thumbs: [String] { TubeVideo.thumbs(id) }
    static func thumbs(_ id: String) -> [String] { ["hq720", "sddefault", "hqdefault"].map { "https://i.ytimg.com/vi/\(id)/\($0).jpg" } }
}

struct TubeChannel: Identifiable, Hashable {
    let id: String
    var name: String
    var avatar: String?
    var subscribers: Int?
}

struct TubePage {
    var videos: [TubeVideo] = []
    var channels: [TubeChannel] = []
    /// Link trang sau (rel='next' của feed) — gọi lại nguyên link.
    var next: String?
}

struct TubeStreams {
    struct Item {
        let quality: Int
        let kind: String
        let url: String
    }

    var items: [Item] = []
    var audio: String?
    var availableIn = 0

    /// Mục có chất lượng lớn nhất không vượt `max`; không có thì mục nhỏ nhất (như app TubeMini).
    func best(_ max: Int) -> Item? {
        items.filter { $0.quality <= max }.max { $0.quality < $1.quality } ?? items.min { $0.quality < $1.quality }
    }
}

enum TubeMini {
    // MARK: Cài đặt (dùng chung cả máy — máy chủ không chia theo người xem)

    /// Địa chỉ máy chủ: «http://máy:8080» trong nhà, «https://tên-miền/u/<mã>» ngoài nhà (rỗng = chưa cài).
    static var server: String {
        get {
            _ = dropOldToken
            return Prefs.store.string(forKey: "tube_server") ?? ""
        }
        set { Prefs.store.set(normalize(newValue), forKey: "tube_server") }
    }

    /// Máy chủ / app TubeMini 0.7.0 bỏ mã X-Auth-Token (mã thiết bị nằm trong địa chỉ) → xoá mã bản 1.25 đã lưu, như app TubeMini.
    private static let dropOldToken: Void = Prefs.store.removeObject(forKey: "tube_token")

    static var configured: Bool { !server.isEmpty }

    /// Bot Telegram cấp địa chỉ có mã thiết bị (trang giới thiệu và app TubeMini cũng chỉ tới đây): `botApp` mở thẳng khung chat
    /// trong app Telegram, `botUrl` cho máy chưa cài Telegram (trang t.me trên trình duyệt).
    static let botApp = URL(string: "tg://resolve?domain=tube_mini_bot")!
    static let botUrl = URL(string: "https://t.me/tube_mini_bot")!

    // Cách lấy mã — theo trang giới thiệu TubeMini (mục «Đăng ký nhận địa chỉ máy chủ») và tin «❓ Hướng dẫn» của bot (tên nút giữ
    // đúng như bot hiện: 📝 Đăng ký, 📋 Chép địa chỉ, 👤 Tài khoản của tôi → Cấp lại mã, ➕ Thiết bị mới).
    static let botSteps = [
        "Bấm «Chat với bot» để mở thẳng khung chat @tube_mini_bot trong Telegram, rồi bấm «📝 Đăng ký».",
        "Bot gửi địa chỉ máy chủ riêng cho máy này, dạng tên-miền/u/k7pq-4xmz-9tbn. Chạm vào địa chỉ hoặc nút «📋 Chép địa chỉ» "
            + "để chép — tin tự xoá sau một lúc, nên chép ngay.",
        "Quay lại MTV, dán vào ô địa chỉ máy chủ rồi bấm «Lưu». Không cần gõ https://, chữ hoa hay thường đều được.",
    ]

    static let botNotes = [
        "Mất địa chỉ: vào bot bấm «👤 Tài khoản của tôi» → «Cấp lại mã» (mã cũ hết dùng được).",
        "Mỗi người tối đa 2 máy; máy thứ hai (vd iPad sau iPhone) bấm «➕ Thiết bị mới» trong bot rồi chờ duyệt. Mỗi máy một địa chỉ "
            + "riêng — đừng dùng chung hay chia sẻ.",
        "Máy chủ chỉ nhận mạng Việt Nam: đang bật VPN / proxy đặt ở nước ngoài thì tắt đi.",
        "Có máy chủ TubeMini riêng trong nhà: gõ địa chỉ của nó (vd 192.168.1.10:8080), không cần mã.",
    ]

    /// Bản chữ thường cho hộp thoại (nút ⚙︎ → «Cách lấy mã»).
    static var botGuide: String {
        botSteps.enumerated().map { "\($0.offset + 1). \($0.element)" }.joined(separator: "\n")
            + "\n\n" + botNotes.map { "• " + $0 }.joined(separator: "\n")
    }

    /// Như `normalizeURL` của app TubeMini 0.7.0: bỏ khoảng trắng và «/» cuối; chưa có scheme thì địa chỉ trong nhà
    /// («192.168.1.10:8080», «nas», «may.local») thêm http://, tên miền («tm.example.com/u/k7pq-4xmz-9tbn») thêm https://.
    /// Scheme và tên máy viết thường; đường dẫn (mã thiết bị) giữ nguyên — mã cũ 24 ký tự phân biệt hoa thường.
    static func normalize(_ s: String) -> String {
        var t = cleanAddress(s)
        while t.hasSuffix("/") { t.removeLast() }
        if t.isEmpty { return "" }
        if !t.contains("://") {
            while t.hasPrefix("/") { t.removeFirst() }
            t = (isLocalAddress(t) ? "http://" : "https://") + t
        }
        guard let r = t.range(of: "://") else { return t }
        let rest = t[r.upperBound...]
        let end = rest.firstIndex(of: "/") ?? t.endIndex
        return t[..<end].lowercased() + t[end...]
    }

    /// Làm sạch chữ dán vào ô địa chỉ (26/11/2026: iPhone 14 dán địa chỉ chép từ Telegram → máy chủ báo 404). Chép từ Telegram /
    /// bàn phím có thể kèm ký tự vô hình (dấu định hướng chữ U+2066–U+2069 / U+200E, dấu cách không ngắt…): `trimming` không bỏ được
    /// khi nó nằm sau ký tự khác, và từ iOS 17 URL(string:) không báo sai mà mã hoá luôn (`%E2%81%A9`) → mã sai → 404. Nên:
    /// bỏ mọi ký tự định dạng (Cf) và khoảng trắng, gạch nối «giả» (‐ ‑ – — −) thành «-»; dán cả tin của bot thì lấy «từ» có
    /// `/u/` (hoặc `://`); dán dính vào địa chỉ cũ thì lấy từ `http(s)://` cuối cùng; bỏ dấu câu cuối.
    static func cleanAddress(_ s: String) -> String {
        let dashes: Set<UInt32> = [0x2010, 0x2011, 0x2012, 0x2013, 0x2014, 0x2015, 0x2212, 0xFE58, 0xFE63, 0xFF0D]
        var out = String.UnicodeScalarView()
        for u in s.unicodeScalars {
            if u.properties.generalCategory == .format { continue }
            out.append(dashes.contains(u.value) ? "-" : u)
        }
        let words = String(out).split(whereSeparator: { $0.isWhitespace }).map(String.init)
        var t = words.last(where: { $0.contains("/u/") }) ?? words.last(where: { $0.contains("://") }) ?? words.joined()
        let starts = ["http://", "https://"].compactMap { t.range(of: $0, options: [.backwards, .caseInsensitive])?.lowerBound }
        if let last = starts.max(), last != t.startIndex { t = String(t[last...]) }
        while let c = t.last, ".,;)]}>»\"'".contains(c) { t.removeLast() }
        return t
    }

    /// Địa chỉ trong nhà: IPv4, tên không có dấu chấm, đuôi .local / .lan / .home / .internal, hoặc cổng khác 443 → http;
    /// tên miền trên Internet (Cloudflare) → https.
    static func isLocalAddress(_ hostAndPath: String) -> Bool {
        let hostPort = hostAndPath.split(separator: "/", omittingEmptySubsequences: false).first.map(String.init) ?? ""
        let parts = hostPort.split(separator: ":", omittingEmptySubsequences: false).map(String.init)
        let host = parts[0].lowercased()
        if parts.count > 1 && parts[1] != "443" { return true }
        if host.isEmpty || !host.contains(".") { return true }
        if host.allSatisfy({ $0.isNumber || $0 == "." }) { return true }
        return [".local", ".lan", ".home", ".internal"].contains { host.hasSuffix($0) }
    }

    /// Cùng địa chỉ, đổi http ↔ https.
    static func otherScheme(_ url: String) -> String {
        if url.hasPrefix("https://") { return "http://" + url.dropFirst(8) }
        if url.hasPrefix("http://") { return "https://" + url.dropFirst(7) }
        return url
    }

    // MARK: Địa chỉ trong app

    static let scheme = "tubemini://"
    static func isTubeUrl(_ u: String) -> Bool { u.hasPrefix(scheme) }
    static func videoUrl(_ id: String) -> String { "\(scheme)video/\(id)" }
    static func videoId(_ u: String) -> String? {
        let p = "\(scheme)video/"
        guard u.hasPrefix(p) else { return nil }
        let id = String(u.dropFirst(p.count))
        return id.isEmpty ? nil : id
    }

    // MARK: Video đã gặp (tên, kênh — gửi kèm lịch sử) và danh sách đang phát

    private static let lock = NSLock()
    private static var known: [String: TubeVideo] = [:]
    private static var queues: [String: FilmDetail] = [:]
    private static var queueOrder: [String] = []
    private static var positions: [String: Int] = [:]
    private static var freshIds = Set<String>()
    private static var lastReport: [String: Date] = [:]
    /// Chất lượng chọn một lần (vd «Chỉ nghe» từ menu ô video) — dùng cho lần mở tới.
    private static var oneShot: String?

    static func remember(_ vs: [TubeVideo]) {
        lock.lock()
        for v in vs { known[v.id] = v }
        if known.count > 3000 { known.removeAll() }
        lock.unlock()
    }

    static func info(_ id: String) -> TubeVideo? {
        lock.lock()
        defer { lock.unlock() }
        return known[id]
    }

    /// Dựng «phim giả» cho trình phát: danh sách video = các tập (video kế / trước, tự sang video sau khi hết).
    static func makeQueue(title: String, videos: [TubeVideo], audioOnly: Bool = false) -> String {
        remember(videos)
        let url = "\(scheme)list/\(UUID().uuidString)"
        let eps = videos.map { Episode(title: $0.title, url: videoUrl($0.id)) }
        let d = FilmDetail(url: url, title: title, otherTitle: nil, poster: videos.first?.thumb, description: nil, info: nil,
                           groups: [EpisodeGroup(name: "TubeMini", episodes: eps)])
        lock.lock()
        queues[url] = d
        queueOrder.append(url)
        if queueOrder.count > 20 { queues[queueOrder.removeFirst()] = nil }
        if audioOnly { oneShot = "tmaudio" }
        lock.unlock()
        return url
    }

    /// Site.detail(tubemini://…): danh sách đã dựng, hoặc một video lẻ.
    static func detail(_ url: String) async throws -> FilmDetail {
        lock.lock()
        let q = queues[url]
        lock.unlock()
        if let q = q { return q }
        guard let id = videoId(url) else { throw AppError("Danh sách TubeMini đã hết hạn — mở lại từ mục TubeMini.") }
        let v = info(id) ?? TubeVideo(id: id, title: "Video YouTube")
        return FilmDetail(url: url, title: v.title, otherTitle: nil, poster: v.thumb, description: nil, info: nil,
                          groups: [EpisodeGroup(name: "TubeMini", episodes: [Episode(title: v.title, url: url)])])
    }

    // MARK: Trình phát: chất lượng = server

    /// 1080p đứng đầu = mặc định (anh chọn 26/11/2026; trước là 720p).
    static let qualities = [
        Server(label: "1080p", type: "tm1080"), Server(label: "720p", type: "tm720"),
        Server(label: "360p", type: "tm360"), Server(label: "Chỉ nghe (tiếng)", type: "tmaudio"),
    ]

    /// Trình phát nhớ server theo tên máy của địa chỉ tập — với tubemini://video/<id> là «video».
    static let qualityHost = "video"

    /// Chất lượng mặc định cho các video sau (cùng khoá với lựa chọn trong trình phát, theo hồ sơ người xem); chưa chọn = 1080p.
    static var quality: String {
        get { Prefs.serverType(qualityHost).flatMap { t in qualities.contains { $0.type == t } ? t : nil } ?? "tm1080" }
        set { Prefs.setServer(qualityHost, type: newValue, label: qualities.first { $0.type == newValue }?.label) }
    }

    static func qualityLabel(_ type: String?) -> String {
        type == "tmaudio" ? "Chỉ nghe" : qualities.first { $0.type == type }?.label ?? "1080p"
    }

    /// KkPhim.source(tubemini://video/…): các mức chất lượng + chỗ xem dở trên máy chủ.
    static func source(_ ep: Episode) async throws -> EpisodeSource {
        var order = qualities
        lock.lock()
        let one = oneShot
        oneShot = nil
        lock.unlock()
        if let o = one, let i = order.firstIndex(where: { $0.type == o }) { order.insert(order.remove(at: i), at: 0) }
        if let id = videoId(ep.url), let h = try? await position(id) {
            lock.lock()
            positions[ep.url] = h.duration > 0 && (h.position < 10 || h.position >= h.duration - 20) ? 0 : h.position
            lock.unlock()
        }
        return EpisodeSource(epUrl: ep.url, servers: order, postId: "", ep: "", sv: "", playerUrl: nil, directEmbed: nil,
                             pageMode: false)
    }

    /// Chỗ xem dở của video (đọc ở source()) — PlayerModel dùng thay cho chỗ xem lưu trong máy.
    static func resume(_ epUrl: String) -> Int {
        lock.lock()
        defer { lock.unlock() }
        return positions[epUrl] ?? 0
    }

    /// «Tải lại» trong trình phát: lần xin link tới bỏ bộ nhớ đệm của máy chủ (link hết hạn / 403).
    static func markFresh(_ epUrl: String) {
        guard let id = videoId(epUrl) else { return }
        lock.lock()
        freshIds.insert(id)
        lock.unlock()
    }

    /// Site.video: link phát theo mức chất lượng; video có quảng cáo đầu thì chờ YouTube mở link (available_in).
    static func video(_ epUrl: String, _ server: Server?) async throws -> Video {
        guard let id = videoId(epUrl) else { throw AppError("Video TubeMini không hợp lệ") }
        lock.lock()
        let fresh = freshIds.remove(id) != nil
        lock.unlock()
        let st = try await streams(id, fresh: fresh)
        if st.availableIn > 0 {
            Log.d("TubeMini", "\(id): chờ quảng cáo đầu \(st.availableIn) s")
            try await Task.sleep(nanoseconds: UInt64(min(30, st.availableIn)) * 1_000_000_000)
        }
        let type = server?.type ?? quality
        if type == "tmaudio" {
            guard let a = st.audio else { throw AppError("Video này không có luồng chỉ tiếng — chọn chất lượng khác trong Menu.") }
            var v = Video(url: a)
            v.avPlayer = true
            return v
        }
        let want = type == "tm1080" ? 1080 : type == "tm360" ? 360 : 720
        guard let item = st.best(want) else { throw AppError("Máy chủ TubeMini không có link phát cho video này.") }
        var v = Video(url: item.url, file: item.kind == "mp4")
        v.avPlayer = true
        return v
    }

    // MARK: API của máy chủ

    private static func url(_ path: String) throws -> String {
        guard configured else { throw AppError("Chưa nhập địa chỉ máy chủ TubeMini.") }
        return path.hasPrefix("http") ? path : server + path
    }

    private static func get(_ path: String) async throws -> Data {
        do {
            return try await Http.get(try url(path)).data
        } catch let e as Http.HttpError {
            throw AppError(message(e.code))
        }
    }

    static func message(_ code: Int) -> String {
        switch code {
        case 401: return "Máy chủ TubeMini cần mã thiết bị — dán địa chỉ có mã (…/u/<mã>) nhận từ bot Telegram @tube_mini_bot."
        case 403: return "Máy chủ TubeMini từ chối (403): máy chủ chỉ nhận mạng Việt Nam — tắt VPN / proxy đặt ở nước ngoài rồi thử lại."
        case 404: return "Máy chủ TubeMini không nhận địa chỉ này (404): mã thiết bị sai hoặc đã bị thu hồi (lấy mã mới ở bot "
            + "@tube_mini_bot), ngoài mạng nhà mà thiếu mã, hoặc máy chủ quá cũ."
        case 502: return "YouTube không trả lời hoặc không lấy được link (máy chủ TubeMini báo lỗi 502)."
        default: return "Máy chủ TubeMini trả về lỗi HTTP \(code)."
        }
    }

    private static func q(_ s: String) -> String { s.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed.subtracting(CharacterSet(charactersIn: "&=+?"))) ?? s }

    /// Kiểm tra kết nối: «máy chủ 0.7.0 · yt-dlp 2026.10.26».
    static func ping() async throws -> String {
        try await probe(server).info
    }

    /// Lưu địa chỉ / «Kiểm tra kết nối» (như `probeServer` của app TubeMini 0.7.0): ping `base`; không tới được (lỗi mạng / TLS,
    /// không phải câu trả lời HTTP) thì thử scheme còn lại; http bị chuyển sang https (Cloudflare «Always Use HTTPS») thì giữ https,
    /// vì POST (theo dõi kênh, chỗ xem) không đi qua được lệnh chuyển hướng. Trả địa chỉ dùng được + dòng thông tin máy chủ.
    static func probe(_ base: String) async throws -> (url: String, info: String) {
        guard !base.isEmpty else { throw AppError("Chưa nhập địa chỉ máy chủ TubeMini.") }
        do {
            return try await pingAt(base)
        } catch let e where !(e is AppError) {
            let other = otherScheme(base)
            guard other != base, let r = try? await pingAt(other) else { throw e }
            return r
        }
    }

    private static func pingAt(_ base: String) async throws -> (url: String, info: String) {
        let r: Http.Response
        do {
            r = try await Http.get(base + "/tm/v1/ping")
        } catch let e as Http.HttpError {
            throw AppError(message(e.code))
        } catch is WebFetch.ChallengeError {
            throw AppError("Máy chủ TubeMini đang bật lớp chống bot của Cloudflare — tắt Bot Fight Mode / Under Attack cho tên miền này.")
        }
        let a = TubeXML.attrs(r.data, "tm:ping")
        guard let server = a["server"] else {
            throw AppError(a["message"] ?? "Địa chỉ này không phải máy chủ TubeMini (hoặc máy chủ quá cũ).")
        }
        var parts = ["máy chủ \(server)"]
        if let y = a["yt_dlp"], !y.isEmpty { parts.append("yt-dlp \(y)") } else { parts.append("THIẾU yt-dlp") }
        if (a["deno"] ?? "").isEmpty { parts.append("THIẾU Deno") }
        let upgraded = base.hasPrefix("http://") && r.url.lowercased().hasPrefix("https://")
        return (upgraded ? otherScheme(base) : base, parts.joined(separator: " · "))
    }

    /// Phổ biến: "" = trộn, "Music" / "News" / "Sports" / "Games" / "Film" = từng chủ đề.
    static func popular(_ cat: String) async throws -> TubePage {
        let feed = cat.isEmpty ? "most_popular" : "most_popular_\(cat)"
        return try await page("/feeds/api/standardfeeds/\(feed)?max-results=50")
    }

    static func search(_ text: String) async throws -> TubePage {
        try await page("/feeds/api/videos?q=\(q(text))&max-results=25")
    }

    static func searchChannels(_ text: String) async throws -> [TubeChannel] {
        try await page("/feeds/api/channels?q=\(q(text))&max-results=25").channels
    }

    static func related(_ id: String) async throws -> TubePage { try await page("/feeds/api/videos/\(id)/related?max-results=25") }
    static func uploads(_ channel: String) async throws -> TubePage { try await page("/feeds/api/users/\(channel)/uploads?max-results=25") }
    static func feed(fresh: Bool) async throws -> TubePage { try await page("/tm/v1/feed?max-results=25" + (fresh ? "&fresh=1" : "")) }
    static func history() async throws -> TubePage { try await page("/tm/v1/history?max-results=25") }
    static func subscriptions() async throws -> [TubeChannel] { try await page("/tm/v1/subscriptions").channels }

    /// Trang sau / trang bất kỳ (link next của feed là địa chỉ đầy đủ).
    static func page(_ path: String) async throws -> TubePage {
        let p = TubeXML.feed(try await get(path))
        remember(p.videos)
        return p
    }

    static func subscribe(_ channel: String) async throws {
        do {
            _ = try await Http.post(try url("/tm/v1/subscriptions"), form: ["channel_id": channel])
        } catch let e as Http.HttpError {
            throw AppError(message(e.code))
        }
    }

    static func unsubscribe(_ channel: String) async throws { try await delete("/tm/v1/subscriptions/\(channel)") }
    static func deleteHistory(_ id: String) async throws { try await delete("/tm/v1/history/\(id)") }

    private static func delete(_ path: String) async throws {
        var r = try Http.request(try url(path))
        r.httpMethod = "DELETE"
        let (_, resp) = try await Http.session.data(for: r)
        if let c = (resp as? HTTPURLResponse)?.statusCode, c >= 400 { throw AppError(message(c)) }
    }

    static func position(_ id: String) async throws -> (position: Int, duration: Int) {
        let a = TubeXML.attrs(try await get("/tm/v1/history/\(id)"), "tm:history")
        return (Int(a["position"] ?? "") ?? 0, Int(a["duration"] ?? "") ?? 0)
    }

    static func streams(_ id: String, fresh: Bool) async throws -> TubeStreams {
        TubeXML.streams(try await get("/tm/v1/videos/\(id)/streams" + (fresh ? "?fresh=1" : "")))
    }

    /// Lưu chỗ đang xem lên máy chủ (app TubeMini gửi ~15 giây một lần và khi đóng trình phát). `force`: gửi ngay.
    static func report(_ epUrl: String, position: Int, duration: Int, force: Bool) {
        guard configured, let id = videoId(epUrl) else { return }
        let now = Date()
        lock.lock()
        if !force, let last = lastReport[id], now.timeIntervalSince(last) < 15 {
            lock.unlock()
            return
        }
        lastReport[id] = now
        positions[epUrl] = position
        let v = known[id]
        lock.unlock()
        var form = ["video_id": id, "position": String(max(0, position))]
        if duration > 0 { form["duration"] = String(duration) }
        if let v = v {
            form["title"] = v.title
            if !v.author.isEmpty { form["author"] = v.author }
            if !v.authorId.isEmpty { form["author_id"] = v.authorId }
        }
        let target = server + "/tm/v1/history"
        let body = form
        Task.detached {
            do { _ = try await Http.post(target, form: body) } catch {
                Log.d("TubeMini", "không lưu được chỗ xem \(id): \(error.localizedDescription)")
            }
        }
    }

    // MARK: Hiện số liệu

    static func durationText(_ s: Int) -> String {
        s >= 3600 ? String(format: "%d:%02d:%02d", s / 3600, s / 60 % 60, s % 60) : String(format: "%d:%02d", s / 60, s % 60)
    }

    /// 1.234.567 → «1,2 Tr»; 35.000 → «35 N».
    static func countText(_ n: Int) -> String {
        func one(_ x: Double) -> String {
            let r = (x * 10).rounded() / 10
            return (r.truncatingRemainder(dividingBy: 1) == 0 ? String(Int(r)) : String(r)).replacingOccurrences(of: ".", with: ",")
        }
        if n >= 1_000_000_000 { return one(Double(n) / 1e9) + " T" }
        if n >= 1_000_000 { return one(Double(n) / 1e6) + " Tr" }
        if n >= 1_000 { return one(Double(n) / 1e3) + " N" }
        return String(n)
    }

    static func agoText(_ d: Date, now: Date = Date()) -> String {
        let s = max(0, now.timeIntervalSince(d))
        if s < 3600 { return "\(max(1, Int(s / 60))) phút trước" }
        if s < 86_400 { return "\(Int(s / 3600)) giờ trước" }
        if s < 86_400 * 30 { return "\(Int(s / 86_400)) ngày trước" }
        if s < 86_400 * 365 { return "\(Int(s / (86_400 * 30))) tháng trước" }
        return "\(Int(s / (86_400 * 365))) năm trước"
    }

    /// «kênh · 1,2 Tr lượt xem · 3 ngày trước»
    static func metaText(_ v: TubeVideo) -> String {
        var parts: [String] = []
        if !v.author.isEmpty { parts.append(v.author) }
        if let n = v.views, n > 0 { parts.append("\(countText(n)) lượt xem") } // video vừa đăng: feed ghi 0
        if let p = v.published, p.timeIntervalSince1970 > 86_400 { parts.append(agoText(p)) }
        return parts.joined(separator: " · ")
    }
}

// MARK: - Đọc XML của máy chủ (Atom kiểu GData + phần tử tm:)

enum TubeXML {
    /// Thuộc tính của phần tử `name` đầu tiên (tm:ping, tm:history…); gặp tm:error thì trả thuộc tính của nó.
    static func attrs(_ data: Data, _ name: String) -> [String: String] {
        let p = Reader(want: [name, "tm:error"])
        p.run(data)
        return p.found[name] ?? p.found["tm:error"] ?? [:]
    }

    static func streams(_ data: Data) -> TubeStreams {
        let r = StreamsReader()
        r.run(data)
        return r.out
    }

    static func feed(_ data: Data) -> TubePage {
        let r = FeedReader()
        r.run(data)
        return r.page
    }

    class Base: NSObject, XMLParserDelegate {
        func run(_ data: Data) {
            let x = XMLParser(data: data)
            x.shouldProcessNamespaces = false
            x.delegate = self
            x.parse()
        }
    }

    final class Reader: Base {
        let want: Set<String>
        var found: [String: [String: String]] = [:]
        init(want: [String]) { self.want = Set(want) }
        func parser(_ parser: XMLParser, didStartElement name: String, namespaceURI: String?, qualifiedName: String?,
                    attributes: [String: String] = [:]) {
            if want.contains(name) && found[name] == nil { found[name] = attributes }
        }
    }

    final class StreamsReader: Base {
        var out = TubeStreams()
        func parser(_ parser: XMLParser, didStartElement name: String, namespaceURI: String?, qualifiedName: String?,
                    attributes a: [String: String] = [:]) {
            switch name {
            case "tm:streams": out.availableIn = Int(a["available_in"] ?? "") ?? 0
            case "tm:stream":
                if let u = a["url"], let q = Int(a["quality"] ?? "") {
                    out.items.append(.init(quality: q, kind: a["kind"] ?? "hls", url: u))
                }
            case "tm:audio": out.audio = a["url"]
            default: break
            }
        }
    }

    final class FeedReader: Base {
        var page = TubePage()
        private var inEntry = false
        private var inAuthor = false
        private var text = ""
        private var e: [String: String] = [:]
        private var thumbs: [String: String] = [:]
        private static let iso: ISO8601DateFormatter = {
            let f = ISO8601DateFormatter()
            f.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
            return f
        }()

        func parser(_ parser: XMLParser, didStartElement name: String, namespaceURI: String?, qualifiedName: String?,
                    attributes a: [String: String] = [:]) {
            text = ""
            switch name {
            case "entry":
                inEntry = true
                e = [:]
                thumbs = [:]
            case "author": inAuthor = true
            case "link":
                if !inEntry, a["rel"] == "next", let h = a["href"] { page.next = h }
            case "media:thumbnail":
                if let u = a["url"] { thumbs[a["yt:name"] ?? "default"] = u }
            case "yt:duration": e["duration"] = a["seconds"]
            case "yt:statistics": e["views"] = a["viewCount"]
            case "yt:channelStatistics": e["subs"] = a["subscriberCount"]
            case "tm:position": e["position"] = a["seconds"]
            default: break
            }
        }

        func parser(_ parser: XMLParser, foundCharacters string: String) { text += string }

        func parser(_ parser: XMLParser, didEndElement name: String, namespaceURI: String?, qualifiedName: String?) {
            let t = text.trimmingCharacters(in: .whitespacesAndNewlines)
            defer { text = "" }
            guard inEntry else { return }
            switch name {
            case "author": inAuthor = false
            case "name": if inAuthor { e["author"] = t }
            case "yt:userId": if inAuthor { e["authorId"] = t }
            case "yt:uploaderId": if e["authorId"] == nil { e["authorId"] = t }
            case "title": if e["title"] == nil { e["title"] = t }
            case "yt:videoid": e["videoid"] = t
            case "yt:channelId": e["channel"] = t
            case "id": e["id"] = t
            case "published": e["published"] = t
            case "entry":
                inEntry = false
                finish()
            default: break
            }
        }

        private func finish() {
            let tagId = e["id"] ?? ""
            let vid = e["videoid"] ?? (tagId.contains(":video:") ? String(tagId.split(separator: ":").last ?? "") : nil)
            if let v = vid, !v.isEmpty {
                var x = TubeVideo(id: v, title: e["title"] ?? "")
                x.author = e["author"] ?? ""
                x.authorId = e["authorId"] ?? ""
                x.duration = Int(e["duration"] ?? "") ?? 0
                x.views = Int(e["views"] ?? "")
                x.published = e["published"].flatMap { Self.iso.date(from: $0) }
                x.position = Int(e["position"] ?? "")
                page.videos.append(x)
                return
            }
            let ch = e["channel"] ?? (tagId.contains(":channel:") ? String(tagId.split(separator: ":").last ?? "") : nil)
            if let c = ch, !c.isEmpty {
                page.channels.append(TubeChannel(id: c, name: e["title"] ?? e["author"] ?? "",
                                                 avatar: thumbs["default"] ?? thumbs.values.first, subscribers: Int(e["subs"] ?? "")))
            }
        }
    }
}
