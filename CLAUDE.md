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

1. Tăng `versionCode` / `versionName` trong `app/build.gradle.kts`, commit.
2. `git push` tag `v<bản>` → workflow `.github/workflows/android.yml` tự dựng (ký bằng keystore trong secrets — keystore
   gốc **không có trong git**, mất là app trên mọi TV không nhận bản cập nhật OTA nữa) và phát hành release `v<bản>`,
   **luôn đánh dấu Latest**, kèm hai tệp giống hệt nhau: `MTV-<bản>.apk` và `MTV.apk` (link cố định
   `…/releases/latest/download/MTV.apk`, mã Downloader `1806154`). **Không gọi `gh release create` tay.**
3. Kiểm sau phát hành: API `releases/latest` trả đúng tag + có tệp `.apk`; tải từ link công khai, SHA-256 khớp
   (`.\release.ps1 tv -Verify` chạy lại các bước kiểm này bất cứ lúc nào). Ghi chú phải có mục `## Có gì mới` — app hiện mục này trong hộp thoại cập
   nhật trên TV.
4. Dựng tay (dự phòng khi CI lỗi): `.\build.ps1` → `dist\MTV-<bản>.apk`, ký bằng `keys\`; kiểm APK: `apksigner verify --print-certs`
   (chứng chỉ SHA-256 `5e8b3fdf...`), `aapt2 dump badging` (đúng bản); rồi `.\release.ps1 tv -Notes <tệp ghi chú>` (`-DryRun` để xem lệnh trước).

## Phát hành bản iPad (`ios/`, cài bằng TrollStore)

1. Tăng `MARKETING_VERSION` và `CURRENT_PROJECT_VERSION` (số build) trong `ios/project.yml`, commit, đẩy lên nhánh `main`.
2. **Dựng Beta bằng CI**: đẩy nhánh `ios` → workflow `.github/workflows/ios.yml` chạy test lõi + dựng IPA Beta
   (mã gói `vn.mtv.tv.debug`, tên «MTV (thử)», artifact `MTV-iPad-<bản>-thu-<số lượt chạy>-ipa`). Cài thử Beta lên iPad / iPhone thật
   (TrollStore) trước khi phát hành.
3. **Phát hành**: đẩy tag `ios-v<bản>` lên commit thuộc `main` → workflow tự tạo release `ios-v<bản>` (**luôn `--latest=false`**:
   app TV đọc `releases/latest` + tệp `.apk`, link Downloader cũng đi theo Latest) + cuốn chiếu `mtv_ios`, kèm IPA bản chính
   (mã gói `vn.mtv.tv`, tên «MTV», không có cổng gỡ lỗi). Kiểm Info.plist (mã gói, tên, bản, build). Ghi chú phải có `## Có gì mới`.
   Dispatch tay cũng được: `gh workflow run ios.yml --ref ios -f build_release=true -f publish_release=true`.
4. Kiểm: `releases/latest` vẫn là bản Android; release iPad có `.ipa`; tải từ link công khai và từ link cố định, SHA-256 khớp.
5. Dựng tay trên Mac mini (dự phòng): thư mục làm việc đúng commit phát hành, rồi `.\ios\scripts\mac-build.ps1 -Release` (~4 phút) →
   **`dist\mac\MTV-iPad-<bản>.ipa`** (mã gói `vn.mtv.tv`, số build lấy từ `project.yml`, mã NextDNS từ `keys\nextdns.txt`).
   Mac mini có Xcode 15.4 / Swift 5.10. Rồi `.\release.ps1 ipad -Notes <tệp ghi chú> -File dist\mac\MTV-iPad-<bản>.ipa` (script tự chép tệp vào link cố định).

## Phát hành bản Mac (Mac Catalyst từ `ios/`)

1. Cùng số bản với iPad (`MARKETING_VERSION` trong `ios/project.yml`). Mã phải dựng được bằng Xcode 15.4 / Swift 5.10 của Mac mini.
2. **Dựng bằng CI**: dispatch tay workflow `.github/workflows/ios.yml` với `mac=true` (job «Bản Mac», chỉ chạy khi nhập tay, không bao giờ
   chạy theo tag) → artifact `mac` (**`MTV-Mac-<bản>.zip`**, Intel + Apple Silicon, ký ad-hoc, mã gói `vn.mtv.tv`, không có cổng gỡ lỗi).
   Kiểm Info.plist, `lipo -archs`, `codesign --verify`; chạy thử trên Mac. (Job release của CI hiện chỉ đăng IPA — bản Mac vẫn đăng tay ở bước 3.)
3. `.\release.ps1 mac -Notes <tệp ghi chú> -File <đường dẫn MTV-Mac-<bản>.zip>` — tag `mac-v<bản>`, script **luôn đặt `--latest=false`**,
   tự chép tệp vào link cố định `mtv_macos`. Ghi chú cũng phải có `## Có gì mới` (app hiện trong hộp «Có bản mới»).
4. Kiểm: `releases/latest` vẫn là bản Android; release có `.zip`; tải từ link công khai và link cố định, SHA-256 khớp; tự cập nhật
   thật từ bản trước (bản trong `/Applications` của Mac mini).
5. Dựng tay trên Mac mini (dự phòng): `.\ios\scripts\mac-build.ps1 -Mac -Release -NoTest` (~8 phút) → `dist\mac\MTV-Mac-<bản>.zip`.

## Phát hành bản Windows (`windows/`)

1. Tăng `<Version>` trong `windows/ConBangTV/ConBangTV.csproj`, commit.
2. `git push` tag `win-v<bản>` → workflow `.github/workflows/windows.yml` tự dựng (chạy test rồi đóng gói) và phát hành release
   `win-v<bản>`, **luôn `--latest=false`**, tự chép tệp vào link cố định `mtv_windows`. Ghi chú phải có `## Có gì mới`.
   Dispatch tay cũng được (`publish_release` mặc định `true`).
3. Kiểm: `releases/latest` vẫn là bản Android; release có `.exe`; tải từ link công khai và từ link cố định, SHA-256 khớp; tự cập
   nhật thật từ bản trước (bản chép riêng).
4. Dựng tay (dự phòng): `.\windows\build.ps1` → chạy test rồi đóng gói `dist\MTV-Windows-<bản>.exe`. Chạy thử tệp đóng gói (thư mục dữ liệu riêng:
   `CONBANG_DATA_DIR`). App dùng **lõi Rust chung với bản Linux** (`linux/core` → `conbang_core.dll` qua `linux/ffi`) và
   trình phát native **libmpv** (bản LGPL của zhongfly/mpv-winbuild, `build.ps1` tự tải đúng SHA-256 vào `windows\vendor`).
   Rồi `.\release.ps1 windows -Notes <tệp ghi chú>`.

## Phát hành bản Linux (`linux/`, Rust + GTK3 + WebKitGTK)

> Chưa có CI cho Linux — chỉ dựng tay trong máy (khác 4 nền tảng còn lại).

1. Tăng `version` trong `linux/Cargo.toml` (tag bỏ `.0` cuối: 1.0.0 → `linux-v1.0`, tệp `MTV-Linux-1.0.deb`).
2. `.\linux\build.ps1 -SyncOnly`, rồi trên máy Ubuntu thử: `bash linux/packaging/release-build.sh <thư mục ra>`. Kiểm `dpkg-deb -f` (gói `mtv`, đúng bản, amd64), tệp chạy không có cổng gỡ lỗi (chỉ bản thử có),
   cài bằng `apt install ./…deb` và mở từ biểu tượng app.
3. `git push`, rồi `.\release.ps1 linux -Notes <tệp ghi chú>` — tag `linux-v<bản>`, script **luôn đặt `--latest=false`**, tự chép
   tệp vào link cố định `mtv_linux`. Ghi chú phải có `## Có gì mới` (hộp «Có bản mới» của app hiện mục này).
4. Kiểm: `releases/latest` vẫn là bản Android; tải từ link công khai và link cố định, SHA-256 khớp; tự cập nhật thật từ bản trước.

## Link tải cố định của bản Windows / iPad / Mac / Linux

- **Đang sống:** `…/releases/download/mtv_windows/MTV-Windows.exe` (bản Windows mới nhất).
- **Chưa có:** `…/releases/download/mtv_ios/MTV-iPad.ipa`, `…/releases/download/mtv_macos/MTV-Mac.zip` và
  `…/releases/download/mtv_linux/MTV-Linux.deb` — chưa có release cuốn chiếu nào cho 3 nền tảng này; link sẽ sống sau lần phát hành
  đầu tiên của từng nền tảng (iOS: sau khi đẩy tag `ios-v1.28`).
- Các release cuốn chiếu: `release.ps1` chép đè tệp mỗi lần phát hành — **đừng đổi tag / tên tệp, đừng xoá, đừng
  để thành Latest**.
- Link lệch (script `-Verify` báo) thì sửa bằng `.\release.ps1 <windows|ipad|mac|linux> -FixedOnly`.

## Luật lâu dài: phát hành thành công xong thì luôn cập nhật markdown

Sau **mỗi** lần phát hành thành công (đã qua bước 5 ở trên), làm đủ các việc sau rồi mới báo xong:

1. **`CHANGELOG.md`** — thêm mục bản mới lên đầu: ngày, các thay đổi chính (viết cho người dùng), versionCode, tag,
   commit, SHA-256 của APK, lưu ý cài đặt nếu có.
2. **`README.md`** — sửa nếu tính năng, cách dùng, danh sách trang hỗ trợ, phím điều khiển hay quy trình đã đổi.
3. **Hộ chiếu bàn giao + bộ nhớ** — cuối đợt việc lớn đã nghiệm thu: ghi hộ chiếu bàn giao vào workspace dự án
   (mẫu `.agent/HANDOVER_LATEST.md`, tối đa 60 dòng, tiếng Việt) và cập nhật `~/MEMORY.md` (trạng thái lúc dừng: nhánh, commit, đã đẩy
   chưa; bản phát hành mới nhất: versionCode, tag, commit, giờ phát hành, SHA-256, đã kiểm những gì; các commit từ bản trước; việc còn dở).
   Dự án này không dùng file `TIEN-DO-*.md` kiểu TiviKit.
4. **Bộ nhớ** của Claude cho dự án — cập nhật điều gì mới quan trọng cho các phiên sau.
5. Commit tài liệu (`Tài liệu: …`) và đẩy lên GitHub, rồi **báo anh danh sách file đã sửa**.
