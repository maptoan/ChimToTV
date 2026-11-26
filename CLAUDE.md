# MTV — hướng dẫn phát triển & phát hành

App Android TV (Kotlin, WebView) xem phim hoạt hình 3D từ các trang web, điều khiển bằng remote / bàn phím.
Dự án `maptoan/MTV` được fork và phát triển từ mã nguồn mở gốc `conbangtv/ChimToTV` (tác giả `Circling5714`) theo giấy phép GNU GPL-3.0, sử dụng định danh gói `vn.mtv.tv`, tên hiển thị `MTV` và tệp phát hành `MTV-*`.
Đọc `README.md` để biết cấu trúc và cách phát video; `CHANGELOG.md` để biết các bản đã phát hành.

## Repo `maptoan/MTV`

- Repo là **`maptoan/MTV`**.
- Link OTA / mời bạn bè trong mã trỏ thẳng về `maptoan/MTV` (`UPDATE_URL`, `APK_URL`, `GUIDE_URL`, `ReleasesApi`, `RELEASES_API`) và mã Downloader **`1806154`** (`aftv.news/1806154`).

## Cách làm việc với anh

- Trả lời bằng tiếng Việt, xưng "anh".
- **Chỉ đẩy GitHub / phát hành khi anh bảo** (vd "Ok e phát hành 1.4"). Commit trong máy thì được.
- Thử trên tivi thật qua adb trước khi báo xong (thiết bị thử: xem bộ nhớ và file `TIEN-DO-*.md` gần nhất).

## Phát hành một bản

1. Tăng `versionCode` / `versionName` trong `app/build.gradle.kts`.
2. `.\build.ps1` → `dist\MTV-<bản>.apk`, ký bằng `keys\` (không có trong git — mất là app trên mọi TV không nhận
   bản cập nhật OTA nữa).
3. Kiểm APK: `apksigner verify --print-certs` (chứng chỉ SHA-256 `5e8b3fdf...`), `aapt2 dump badging` (đúng bản).
4. `git push`, rồi `.\release.ps1 tv -Notes <tệp ghi chú>` — **không gọi `gh release create` tay**. Script đăng `v<bản>`, luôn
   đánh dấu Latest, kèm hai tệp giống hệt nhau: `MTV-<bản>.apk` và `MTV.apk` (link cố định
   `…/releases/latest/download/MTV.apk`, mã Downloader `1806154`), rồi tự kiểm (Latest đúng tag, hai tệp cùng SHA-256
   với tệp trong máy, link cố định tải về đúng tệp). Ghi chú phải có mục `## Có gì mới` — app hiện mục này trong hộp thoại cập
   nhật trên TV. `-DryRun` để xem lệnh trước.
5. Kiểm sau phát hành: API `releases/latest` trả đúng tag + có tệp `.apk`; tải từ link công khai, SHA-256 khớp
   (`.\release.ps1 tv -Verify` chạy lại các bước kiểm này bất cứ lúc nào).

## Phát hành bản iPad (`ios/`, cài bằng TrollStore)

1. Tăng `MARKETING_VERSION` và `CURRENT_PROJECT_VERSION` (số build) trong `ios/project.yml`, commit.
2. **Dựng trên Mac mini trong nhà**:
   thư mục làm việc đúng commit phát hành, rồi `.\ios\scripts\mac-build.ps1 -Release` (~4 phút) →
   **`dist\mac\MTV-iPad-<bản>.ipa`** (mã gói `vn.mtv.tv`, số build lấy từ `project.yml`, mã NextDNS từ `keys\nextdns.txt`).
   Mac mini có Xcode 15.4 / Swift 5.10. Kiểm Info.plist (mã gói, tên, bản, build), không có cổng gỡ lỗi (tệp chạy không có chuỗi
   `/tvstatus`), cài thử lên iPad / iPhone thật.
3. `.\release.ps1 ipad -Notes <tệp ghi chú> -File dist\mac\MTV-iPad-<bản>.ipa` — script **luôn đặt `--latest=false`**: app TV đọc
   `releases/latest` + tệp `.apk`, link Downloader cũng đi theo Latest. Ghi chú cũng phải có `## Có gì mới`. Script tự chép tệp
   vào link cố định (xem dưới).
4. Kiểm: `releases/latest` vẫn là bản Android (script tự kiểm, lệch thì trả về bản TV mới nhất); release iPad có `.ipa`; tải từ
   link công khai và từ link cố định, SHA-256 khớp.

## Phát hành bản Mac (Mac Catalyst từ `ios/`)

1. Cùng số bản với iPad (`MARKETING_VERSION` trong `ios/project.yml`). Mã phải dựng được bằng Xcode 15.4 / Swift 5.10 của Mac mini.
2. **Dựng trên Mac mini**: `.\ios\scripts\mac-build.ps1 -Mac -Release -NoTest`
   (~8 phút) → **`dist\mac\MTV-Mac-<bản>.zip`** (Intel + Apple Silicon, ký ad-hoc, mã gói `vn.mtv.tv`, không có cổng gỡ lỗi;
   script in ra kiến trúc và mã gói). Kiểm Info.plist, `lipo -archs`, `codesign --verify`; chạy thử trên Mac.
3. `.\release.ps1 mac -Notes <tệp ghi chú> -File dist\mac\MTV-Mac-<bản>.zip` — tag `mac-v<bản>`, script **luôn đặt `--latest=false`**,
   tự chép tệp vào link cố định `mtv_macos`. Ghi chú cũng phải có `## Có gì mới` (app hiện trong hộp «Có bản mới»).
4. Kiểm: `releases/latest` vẫn là bản Android; release có `.zip`; tải từ link công khai và link cố định, SHA-256 khớp; tự cập nhật
   thật từ bản trước (bản trong `/Applications` của Mac mini).

## Phát hành bản Windows (`windows/`)

1. Tăng `<Version>` trong `windows/ConBangTV/ConBangTV.csproj`.
2. `.\windows\build.ps1` → chạy test rồi đóng gói `dist\MTV-Windows-<bản>.exe`. Chạy thử tệp đóng gói (thư mục dữ liệu riêng:
   `CONBANG_DATA_DIR`). App dùng **lõi Rust chung với bản Linux** (`linux/core` → `conbang_core.dll` qua `linux/ffi`) và
   trình phát native **libmpv** (bản LGPL của zhongfly/mpv-winbuild, `build.ps1` tự tải đúng SHA-256 vào `windows\vendor`).
3. `git push`, rồi `.\release.ps1 windows -Notes <tệp ghi chú>` — tag `win-v<bản>`, script **luôn đặt `--latest=false`**. Ghi chú
   cũng phải có `## Có gì mới`. Script tự chép tệp vào link cố định (xem dưới).
4. Kiểm: `releases/latest` vẫn là bản Android; release có `.exe`; tải từ link công khai và từ link cố định, SHA-256 khớp; tự cập
   nhật thật từ bản trước (bản chép riêng).

## Phát hành bản Linux (`linux/`, Rust + GTK3 + WebKitGTK)

1. Tăng `version` trong `linux/Cargo.toml` (tag bỏ `.0` cuối: 1.0.0 → `linux-v1.0`, tệp `MTV-Linux-1.0.deb`).
2. `.\linux\build.ps1 -SyncOnly`, rồi trên máy Ubuntu thử: `bash linux/packaging/release-build.sh <thư mục ra>`. Kiểm `dpkg-deb -f` (gói `mtv`, đúng bản, amd64), tệp chạy không có cổng gỡ lỗi (chỉ bản thử có),
   cài bằng `apt install ./…deb` và mở từ biểu tượng app.
3. `git push`, rồi `.\release.ps1 linux -Notes <tệp ghi chú>` — tag `linux-v<bản>`, script **luôn đặt `--latest=false`**, tự chép
   tệp vào link cố định `mtv_linux`. Ghi chú phải có `## Có gì mới` (hộp «Có bản mới» của app hiện mục này).
4. Kiểm: `releases/latest` vẫn là bản Android; tải từ link công khai và link cố định, SHA-256 khớp; tự cập nhật thật từ bản trước.

## Link tải cố định của bản Windows / iPad / Mac / Linux

- `…/releases/download/mtv_windows/MTV-Windows.exe`, `…/releases/download/mtv_ios/MTV-iPad.ipa`,
  `…/releases/download/mtv_macos/MTV-Mac.zip` và `…/releases/download/mtv_linux/MTV-Linux.deb` luôn là bản
  mới nhất. Các release đó là release «cuốn chiếu»: `release.ps1` chép đè tệp mỗi lần phát hành — **đừng đổi tag / tên tệp, đừng xoá, đừng
  để thành Latest**.
- Link lệch (script `-Verify` báo) thì sửa bằng `.\release.ps1 <windows|ipad|mac|linux> -FixedOnly`.

## Luật lâu dài: phát hành thành công xong thì luôn cập nhật markdown

Sau **mỗi** lần phát hành thành công (đã qua bước 5 ở trên), làm đủ các việc sau rồi mới báo xong:

1. **`CHANGELOG.md`** — thêm mục bản mới lên đầu: ngày, các thay đổi chính (viết cho người dùng), versionCode, tag,
   commit, SHA-256 của APK, lưu ý cài đặt nếu có.
2. **`README.md`** — sửa nếu tính năng, cách dùng, danh sách trang hỗ trợ, phím điều khiển hay quy trình đã đổi.
3. **`TIEN-DO-<YYYY-MM-DD>.md`** (ngày làm việc, để ở gốc dự án, không đưa lên git) — tạo hoặc cập nhật theo kiểu
   TiviKit: tình trạng lúc dừng (nhánh, commit, đã đẩy chưa), bản phát hành mới nhất (versionCode, tag, commit,
   giờ phát hành, SHA-256, đã kiểm những gì), các commit từ bản trước, tivi thử đang chạy bản nào, việc còn dở.
4. **Bộ nhớ** của Claude cho dự án — cập nhật điều gì mới quan trọng cho các phiên sau.
5. Commit tài liệu (`Tài liệu: …`) và đẩy lên GitHub, rồi **báo anh danh sách file đã sửa**.
