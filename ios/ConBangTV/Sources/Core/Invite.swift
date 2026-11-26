import Foundation

/// Cài bản TV lên tivi bằng app Downloader (AFTVnews): người mới chỉ cần cài Downloader trên tivi rồi nhập `code`. `apkUrl`:
/// link cố định luôn tải bản TV mới nhất (tệp MTV.apk). Cùng giá trị với bản TV (Invite.kt), Windows (Invite.cs), release.ps1
/// và README (mục «Cài trên Android TV bằng Downloader») — đổi mã / link thì sửa đủ các chỗ đó.
enum Invite {
    static let code = "1806154"
    static let shortUrl = "aftv.news/\(code)"
    static let apkUrl = "https://github.com/maptoan/MTV/releases/latest/download/MTV.apk"

    /// Trang hướng dẫn tiếng Việt (mục Downloader của README).
    static let guideUrl = "https://github.com/maptoan/MTV/blob/main/README.md#downloader"

    /// Tin nhắn gửi cho người quen — cùng nội dung ở cả ba bản.
    static let message = [
        "Cài MTV trên tivi Android:",
        "1. Trên tivi mở CH Play, cài app Downloader (của AFTVnews).",
        "2. Mở Downloader, nhập \(code) rồi bấm Go.",
        "3. Tải xong chọn Install. Lần đầu tivi hỏi cho phép Downloader cài ứng dụng thì cho phép.",
        "Cài xong app tự cập nhật các bản sau.",
        "Hướng dẫn: \(guideUrl)",
    ].joined(separator: "\n")
}
