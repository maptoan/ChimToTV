import XCTest

/// Mã Downloader + tin nhắn gửi người quen (Invite.swift) — cùng ca với InviteTest.kt của bản TV và InviteTests.cs của bản Windows.
final class InviteTests: XCTestCase {
    func testCodeAndLinks() {
        XCTAssertNotNil(Invite.code.range(of: "^\\d{5,9}$", options: .regularExpression))
        XCTAssertEqual(Invite.shortUrl, "aftv.news/\(Invite.code)")
        // Link cố định: release đang là Latest, tệp tên cố định (release.ps1 đính kèm ở mọi bản TV)
        XCTAssertTrue(Invite.apkUrl.hasSuffix("/releases/latest/download/MTV.apk"))
        XCTAssertTrue(Invite.guideUrl.hasPrefix("https://github.com/maptoan/MTV/"))
    }

    /// Ba bản gửi cùng một tin nhắn — sửa câu chữ thì sửa cả ba bản và cả ba test.
    func testMessageIsTheSameOnEveryPlatform() {
        XCTAssertEqual(Invite.message,
                       "Cài MTV trên tivi Android:\n" +
                       "1. Trên tivi mở CH Play, cài app Downloader (của AFTVnews).\n" +
                       "2. Mở Downloader, nhập 1806154 rồi bấm Go.\n" +
                       "3. Tải xong chọn Install. Lần đầu tivi hỏi cho phép Downloader cài ứng dụng thì cho phép.\n" +
                       "Cài xong app tự cập nhật các bản sau.\n" +
                       "Hướng dẫn: https://github.com/maptoan/MTV/blob/main/README.md#downloader")
    }
}
