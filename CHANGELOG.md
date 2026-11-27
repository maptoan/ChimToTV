# Nhật ký phiên bản

MTV — phát triển từ mã nguồn mở Côn Bằng TV (GPL-3.0). Tải bản mới nhất ở [GitHub Releases](https://github.com/maptoan/MTV/releases/latest). Bản mới nhất ở trên cùng.
Mọi bản MTV ký cùng một khoá (chứng chỉ SHA-256 `5e8b3fdf3b9adefad9b9ad573b7f56048555bdffc586dd346718331864f441d2`), cài đè
được lên nhau và giữ nguyên lịch sử xem.

Bản iPad / iPhone (iOS / iPadOS 15+, cài bằng TrollStore) ghi riêng với tiêu đề "iPad …", tag `ios-v…`.
Bản Windows (Windows 10 / 11, một tệp `.exe`) ghi riêng với tiêu đề "Windows …", tag `win-v…`.
Bản Mac (macOS 12+) ghi riêng với tiêu đề "Mac …", tag `mac-v…`.
Bản Linux (Ubuntu 22.04+ / Linux Mint 21+ / Debian 12+, tệp `.deb`) ghi riêng với tiêu đề "Linux …", tag `linux-v…`.

## iPad / iPhone 1.28 — 27/11/2026 (Chưa phát hành — đang chờ cài thử trên máy thật)

- Đây là bản **MTV** đầu tiên cho iPad / iPhone (`vn.mtv.tv`, repo `maptoan/MTV`).
- Cài song song được với bản cũ (dùng định danh `vn.mtv.tv` và App Group `group.vn.mtv.tv` riêng; có thể chuyển lịch sử từ bản cũ qua tính năng «Đồng bộ trong nhà» trong mạng LAN).
- Cần cài đặt bằng **TrollStore** (hỗ trợ iOS / iPadOS 15.0 trở lên trong dải TrollStore iOS 14.0–17.0; app yêu cầu tối thiểu iOS 15.0).
- Cập nhật luồng tự kiểm tra phiên bản mới (`maptoan/MTV`), màn «Cài lên tivi / mời bạn bè» (mã Downloader `1806154`), thông tin miễn trừ trách nhiệm và kênh báo lỗi (`github.com/maptoan/MTV/issues`).
- Sẽ phát hành tệp `MTV-iPad-1.28.ipa` (build `36`) sau khi cài thử Beta trên máy thật đạt. Bản thử Beta `MTV-iPad-1.28-thu-2.ipa` (5.502.595 byte, SHA-256 `5fdff44bbc8cd2fd50cac9b78001af45d98c0e85c53120ef078b659df47fdacd`) đã chạy xong 27/11/2026 (184 test, 0 fail).

## Windows 1.22 — 27/11/2026 (Phát hành MTV đầu tiên)

- Đây là bản **MTV** đầu tiên cho Windows (`maptoan/MTV`).
- Logo mới Mẫu 5 (monogram MTV trong lục giác đôi, cam đồng) thay cho ô kính của tác giả.
- Cập nhật luồng tự cập nhật (`maptoan/MTV`), màn «Mời bạn bè» (mã Downloader `1806154`), thông tin miễn trừ trách nhiệm và kênh báo lỗi (`github.com/maptoan/MTV/issues`).
- Phát hành tệp `MTV-Windows-1.22.exe` (119.839.927 byte, SHA-256 `c9d58fd2c4dd0f6066928c048915a54dbca71b28ddb643a8622b90df51f0c769`) và link cố định `mtv_windows/MTV-Windows.exe`.

## 2.24 — 27/11/2026 (Phát hành MTV đầu tiên)

- Chuyển định danh chính thức của fork sang **MTV** (`vn.mtv.tv`, repo `maptoan/MTV`).
- Cập nhật luồng tự cập nhật OTA (`UPDATE_URL`), màn «Mời bạn bè cài app» (mã Downloader `1806154`, mã QR hướng dẫn mới), thông tin miễn trừ trách nhiệm và liên hệ (`github.com/maptoan/MTV/issues`).
- Phát hành tệp `MTV-2.24.apk` và `MTV.apk` (`versionCode = 48`, `versionName = "2.24"`).

---

*Các mục dưới đây là lịch sử các bản của tác giả (repo `conbangtv/ChimToTV`) trước khi fork đổi tên thành MTV — tên tệp, số liệu và mã Downloader trong đó là của bản gốc, không phải bản MTV.*

## 2.24 — 26/11/2026 (biểu tượng mới như bản Côn Bằng TV Legacy)

Anh: «đồng bộ toàn bộ icon app của Côn Bằng TV giống với icon logo hiện tại của Côn Bằng TV Legacy», rồi «phát hành trên 5 nền tảng nha
e». Cùng đợt với iPad / iPhone / Mac 1.28, Windows 1.22, Linux 1.8.

- **Biểu tượng mới:** ô kính vuông bo tròn 4 góc, trong suốt, viền sáng, vòng ensō vàng và ấn 鵬 nổi khối — đúng biểu tượng bản Legacy từ
  18/11 (trước: đĩa kính tròn). Đổi icon app (icon thích ứng: ô kính trong vùng an toàn 66dp, nền trong suốt), banner trên màn hình chính tivi,
  logo đầu menu. Vẽ bằng `design/logo.html` phương án E (`node design/render.mjs export e`).
- bản 2.24 · versionCode 48 · tag `v2.24` (Latest) · commit `351278b` (icon: `d3fdb97`) · `ConBangTV-2.24.apk` = `ConBangTV.apk` = `ChimToTV.apk`
  151 566 380 byte · SHA-256 `4949d3dae05eb8fe36ec58630e5c769264f7cd61c99bab175380e0d1ad3915ef` · chứng chỉ `949ef2b5…177b`
- Thử: APK thử chứa đủ 14 tệp icon / banner / logo mới (khớp byte với tệp nguồn); `aapt2`: `vn.chimto.tv` 48 / 2.24. TX3 không bật nên thử trên
  điện thoại S10e (launcher TiviKit).
- Đăng lúc 11:24:15 bằng `release.ps1 tv` (Latest = `v2.24`, ba tệp cùng SHA-256, hai link cố định đúng 2.24). **Tự cập nhật thật 2.23 → 2.24
  trên S10e**: «Có bản mới 2.24 (đang dùng 2.23)» đủ «Có gì mới» → «Cài đặt ngay» → **2.24 lúc 11:34:36**; logo trong app và banner trên màn
  hình chính của launcher đã là ô kính mới. Tivi Skyworth (2.23) chờ người nhà cập nhật khi mở app (cài trên tivi đó vướng khoá phụ huynh).
- Trang giới thiệu: logo đầu trang + biểu tượng màn hình chính iPhone theo icon mới (site `e519a00`), dòng số bản → TV 2.24 · iPad / iPhone 1.28 ·
  Windows 1.22 · Mac 1.28 · Linux 1.8 (site `b0d47f0`).

## iPad / iPhone 1.28 và Mac 1.28 — 26/11/2026 (biểu tượng mới như bản Legacy)

- **Biểu tượng mới:** ô kính vuông bo tròn, vòng vàng và ấn 鵬 nổi khối như bản Legacy. iPad / iPhone: ô kính phủ cả khung trên nền nâu tối
  (iOS tô đen phần trong suốt của icon); Mac: ô kính trong suốt theo lưới icon macOS, bóng đổ ngoài ô; logo đầu menu đổi theo.
- iPad / iPhone: bản 1.28 (build 36) · tag `ios-v1.28` · `ConBangTV-iPad-1.28.ipa` 6 452 630 byte · SHA-256
  `a4127d48c309942123a88136b6d4b1b7a9b2041844066e1edd96905fe6ab2865`; Mac: bản 1.28 (build 36) · tag `mac-v1.28` · `ConBangTV-Mac-1.28.zip`
  9 756 205 byte (x86_64 + arm64, ký ad-hoc, `codesign --verify` đạt, không cổng gỡ lỗi) · SHA-256
  `e6dff45e4c7238274b01399d3adec85608c6a902c69757f13f9af293b0a7c2b6` · commit `351278b` (icon: `d3fdb97`) · dựng trên Mac mini
- Thử: test lõi Swift 184 đạt. IPA chính `vn.chimto.tv` 1.28 / 36 (cảnh CarPlay + `SBStarkCapable`, không `no-container`, không cổng gỡ lỗi)
  cài lên iPad thử bằng TrollStore, mở được. Icon `AppIcon.icns` trong gói Mac là ô kính mới.
- Đăng lúc 11:28:02 (iPad) và 11:28:19 (Mac) bằng `release.ps1 ipad|mac -File dist\mac\…` (`--latest=false`); `-Verify` đạt cả hai. **Tự cập
  nhật thật Mac 1.27 → 1.28** (Mac mini): «Có bản mới 1.28» đủ «Có gì mới» → «Cài đặt ngay» → mở lại 1.28 / 36 lúc 11:30:55, `codesign` đạt,
  logo đầu menu là ô kính mới.

## Windows 1.22 — 26/11/2026 (biểu tượng mới như bản Legacy)

- **Biểu tượng mới:** ô kính vuông bo tròn, vòng vàng và ấn 鵬 nổi khối như bản Legacy — `app.ico` (9 cỡ, biểu tượng tệp chạy / thanh tác vụ)
  và logo trong app.
- bản 1.22.0 · tag `win-v1.22` · `ConBangTV-Windows-1.22.exe` 120 421 861 byte · SHA-256
  `a881ec52c9aa3ad57976afc18761e6a0016bb68750117606c209ea469c976771` · commit `351278b` (icon: `d3fdb97`)
- Thử: `windows\build.ps1` — test lõi Rust + tích hợp đạt, .NET 38 đạt (lần dựng đầu hết RAM khi biên dịch Rust vì Gradle còn giữ bộ nhớ →
  dựng lại với `CARGO_BUILD_JOBS=2`). Icon lấy ra từ tệp `.exe` là ô kính mới; tệp đóng gói chạy với thư mục dữ liệu riêng, mở cửa sổ «Côn Bằng TV».
- Đăng lúc 11:25:55 bằng `release.ps1 windows`; link cố định `conbangtv_windows` = 1.22. **Tự cập nhật thật 1.21 → 1.22** (bản 1.21 tải từ
  GitHub, chạy riêng): hộp «Có bản mới» → «Cập nhật ngay» → tệp chạy thành 1.22.0 (SHA-256 trùng bản phát hành) lúc 11:36:50, mở lại `--updated`.

## Linux 1.8 — 26/11/2026 (biểu tượng mới như bản Legacy)

- **Biểu tượng mới:** ô kính vuông bo tròn, vòng vàng và ấn 鵬 nổi khối như bản Legacy — icon gói (6 cỡ, menu ứng dụng / thanh tác vụ).
- bản 1.8.0 · tag `linux-v1.8` · `ConBangTV-Linux-1.8.deb` 6 302 844 byte · SHA-256 `ad00b4a192a4f397e5f368b713a53c14b83a26a6e32e7cfee622326ef49bf802` ·
  commit `351278b` (icon: `d3fdb97`) · dựng trong container Ubuntu 22.04 (glibc ≤ 2.34), tệp chạy không có cổng gỡ lỗi
- Thử: `dpkg-deb -f` = `conbangtv` 1.8.0 amd64; icon 256 trong gói khớp byte với icon mới.
- Đăng lúc 11:26:26 bằng `release.ps1 linux`; link cố định `conbangtv_linux` = 1.8. **Tự cập nhật thật 1.7 → 1.8** trên máy ảo (mở từ biểu tượng
  trên thanh dock): «Có bản mới: Côn Bằng TV 1.8» → «Cập nhật ngay» → gói 1.8.0, mở lại `--updated` lúc ~11:33; biểu tượng thanh dock và logo
  đầu menu đã là ô kính mới.

## iPad / iPhone 1.27 và Mac 1.27 — 26/11/2026 (TubeMini: ảnh thu nhỏ nét hơn, chọn chất lượng video — mặc định 1080p)

Anh: «tăng độ phân giải của Thumbnail video», «chọn được độ phân giải của video, mặc định 1080p», rồi «ok phát hành nha e».

- **Ảnh thu nhỏ nét hơn:** ô video dùng hq720 (1280×720) → sddefault (640×480) → hqdefault (480×360, luôn có) thay cho mqdefault 320×180
  (mờ trên màn Retina); ảnh máy chủ trả 404 được nhớ 7 ngày (dấu `.404` cạnh tệp trong bộ nhớ ảnh). Màn «Chỉ nghe» cũng dùng ảnh nét.
- **Chọn chất lượng video:** mặc định 1080p (trước 720p); nút chất lượng ngay cạnh nút Menu khi đang xem (1080p / 720p / 360p / Chỉ
  nghe) — đổi xong xem tiếp đúng chỗ và nhớ cho các video sau; mục «Server» trong Menu đổi tên thành «Chất lượng video»; hộp ⚙︎ của trang
  TubeMini có «Chất lượng video: …» để đặt mặc định.
- iPad / iPhone: bản 1.27 (build 35) · tag `ios-v1.27` · `ConBangTV-iPad-1.27.ipa` 6 323 485 byte · SHA-256
  `bd43ad7a2d546bce829ccf0e92406cbe2576f9befd99b60f8b39aeaab27dbc07`; Mac: bản 1.27 (build 35) · tag `mac-v1.27` · `ConBangTV-Mac-1.27.zip`
  9 780 384 byte (x86_64 + arm64, ký ad-hoc, `codesign --verify` đạt, không cổng gỡ lỗi) · SHA-256
  `37d9480fba31465037449ed96a57c46a2ef89b5b67c18f8c00fc1df07ced3662` · commit `6c6618b` (tính năng: `7869b86`) · dựng trên Mac mini
- Thử: test lõi Swift 184 đạt (thêm: thứ tự ảnh, mặc định / nhớ chất lượng). Bản Mac thử: ô video nét; video mở 1080p; nút chất lượng →
  720p phát tiếp đúng giây và nhớ; video cũ không có hq720 / sddefault (404) lùi về hqdefault trên màn «Chỉ nghe». IPA chính `vn.chimto.tv`
  1.27 / 35 (cảnh CarPlay + `SBStarkCapable`, không `no-container`, không cổng gỡ lỗi) **cài lên iPad thử bằng TrollStore** và mở được.
- Đăng lúc 10:38:45 (iPad) và 10:39:17 (Mac) bằng `release.ps1 ipad|mac -File dist\mac\…` (`--latest=false`, Latest vẫn `v2.23`);
  `-Verify` đạt cả hai. **Tự cập nhật thật Mac 1.26.1 → 1.27** trên Mac mini: hộp «Có bản mới 1.27» đủ «Có gì mới» → «Cài đặt ngay» → app
  mở lại 1.27 / 35 lúc 10:41, `codesign` đạt, trang TubeMini hiện ảnh nét. Trang hướng dẫn: dòng số bản → iPad / iPhone 1.27 · Mac 1.27
  (site `212516d`).

## iPad / iPhone 1.26.1 và Mac 1.26.1 — 26/11/2026 (TubeMini: sửa lỗi «404» khi dán địa chỉ chép từ Telegram)

Anh: dán địa chỉ có mã từ bot vào Côn Bằng trên iPhone 14 → «404» (nhập trên màn điện thoại), rồi «Phát hành nha e». Máy chủ chạy đúng
(ping / feed / link phát 200 từ máy tính; bản Mac 1.26 với cùng địa chỉ kết nối và phát được); máy chủ trả 404 khi mã dính ký tự lạ.
Dựng lại được: địa chỉ kèm ký tự vô hình (dấu định hướng chữ U+2069 / U+200E mà Telegram hay chèn khi chép, dấu cách không ngắt) →
`trimming` không bỏ được khi nằm sau ký tự khác, từ iOS 17 `URL(string:)` mã hoá luôn thay vì báo sai → mã sai → 404.

- **TubeMini tự làm sạch địa chỉ dán vào** (`TubeMini.cleanAddress`): bỏ mọi ký tự vô hình (định dạng Cf) và khoảng trắng, gạch nối lạ
  (‐ ‑ – — −) thành «-»; dán cả tin của bot thì tự lấy đúng địa chỉ có `/u/`; dán dính vào địa chỉ cũ thì lấy địa chỉ `http(s)://` cuối;
  bỏ dấu câu cuối.
- iPad / iPhone: bản 1.26.1 (build 34) · tag `ios-v1.26.1` · `ConBangTV-iPad-1.26.1.ipa` 6 311 779 byte · SHA-256
  `8f098ffa10ab8561e8c81a457c17a67505f13cec1d38b4c61c441d26317e1bdf`; Mac: bản 1.26.1 (build 34) · tag `mac-v1.26.1` ·
  `ConBangTV-Mac-1.26.1.zip` 9 758 425 byte (x86_64 + arm64, ký ad-hoc, `codesign --verify` đạt, không cổng gỡ lỗi) · SHA-256
  `677683567ee1dfd211a5e315c1768f0dace4c74eab2cb65d90735d72ea795f4b` · commit `cf0fad2` (sửa: `6e9331b`) · dựng trên Mac mini
- Thử: test lõi Swift 184 đạt (thêm 7 ca làm sạch địa chỉ). Bản Mac thử với địa chỉ công khai thật có mã: 4 kiểu địa chỉ dính ký tự vô
  hình trước báo 404 / «không hợp lệ», giờ đều kết nối. IPA chính `vn.chimto.tv` 1.26.1 / 34 (cảnh CarPlay + `SBStarkCapable`, không
  `no-container`, không cổng gỡ lỗi) **cài lên iPad thử bằng TrollStore** (thay bản 1.23) và mở được. Chưa cài lên iPhone 14 (SSH bằng
  tài khoản của 6s Plus bị từ chối) — anh cập nhật qua hộp «Có bản mới» rồi dán lại địa chỉ.
- Đăng lúc 09:27:27 (iPad) và 09:27:46 (Mac) bằng `release.ps1 ipad|mac -File dist\mac\…` (`--latest=false`, Latest vẫn `v2.23`);
  `-Verify` đạt cả hai. **Tự cập nhật thật Mac 1.26 → 1.26.1** trên Mac mini: hộp «Có bản mới 1.26.1» đủ «Có gì mới» → «Cài đặt ngay» →
  app mở lại 1.26.1 / 34 lúc 09:30, `codesign` đạt. Trang hướng dẫn: dòng số bản → iPad / iPhone 1.26.1 · Mac 1.26.1 (site `f262e9f`).

## iPad / iPhone 1.26 và Mac 1.26 — 26/11/2026 (TubeMini theo máy chủ 0.7.0: mã thiết bị qua bot Telegram, dùng được ngoài nhà)

Anh giao: «E qua repo tubemini app xem cập nhật lõi mới của tubemini», rồi «bổ sung vào luôn để user biết cách lấy mã», «làm nút bấm để
bấm trực tiếp trên app chuyển qua chat trên tele bot cho tiện», và «Commit và phát hành nha e». Máy chủ TubeMini 0.7.0 (25/11, repo riêng):
ra Internet qua Cloudflare (chỉ nhận mạng Việt Nam), nhiều người dùng, **mỗi thiết bị một mã trong địa chỉ** `https://<tên miền>/u/<mã>` do
bot @tube_mini_bot cấp, bỏ `X-Auth-Token` và phát qua máy chủ. Bản 1.25 tự thêm `http://` vào mọi địa chỉ → với tên miền đi http rồi bị
chuyển sang https (theo dõi kênh / lưu chỗ xem hỏng, mã đi không mã hoá).

- **TubeMini dùng được cả ngoài nhà (4G, trên xe):** dán địa chỉ có mã thiết bị bot gửi — không cần gõ `https://`: như app TubeMini 0.7.0,
  địa chỉ trong nhà thêm `http://`, tên miền thêm `https://`, tên máy viết thường, mã giữ nguyên; «Lưu và kiểm tra» tự thử scheme còn lại
  khi không tới được, giữ https khi http bị chuyển hướng.
- **Cách lấy mã ngay trong app:** khung 3 bước ở trang TubeMini (Đăng ký → chép địa chỉ → dán, Lưu) + lưu ý (cấp lại mã, tối đa 2 máy,
  mỗi máy một địa chỉ, chỉ mạng Việt Nam, máy chủ riêng trong nhà không cần mã), tên nút đúng như bot; nút ⚙︎ → «Cách lấy mã».
- **Nút «Chat với bot @tube_mini_bot»** (xanh Telegram) mở thẳng khung chat trong app Telegram; máy chưa cài Telegram thì mở trang t.me.
  Cũng có trong hộp ⚙︎ và hộp «Cách lấy mã».
- Bỏ ô mã (token) cũ (xoá mã đã lưu); báo rõ khi mã sai / bị thu hồi (404) hoặc đang dùng mạng nước ngoài (403); đổi máy chủ qua ⚙︎ báo
  kết quả bằng dòng nổi; nhật ký che mã `/u/<mã>`.
- iPad / iPhone: bản 1.26 (build 33) · tag `ios-v1.26` · `ConBangTV-iPad-1.26.ipa` 6 309 221 byte · SHA-256
  `84b15e42124ea8973625c3fec18ce85c75068ae727d9ecfe3745602d3f22ef0c`; Mac: bản 1.26 (build 33) · tag `mac-v1.26` · `ConBangTV-Mac-1.26.zip`
  9 751 634 byte (x86_64 + arm64, ký ad-hoc, `codesign --verify` đạt, không cổng gỡ lỗi) · SHA-256
  `4e93c7ca4035afa42b6b2d66b77cb1cb2f5e673375e8a1a8067655fad6acfc61` · commit `41b0035` (tính năng: `ae410ce`, `0b259ef`, `ea4c18c`) · dựng
  trên Mac mini
- Thử: test lõi Swift 184 đạt (thêm ca địa chỉ / scheme, che mã trong nhật ký, chữ hướng dẫn). Bản Mac thử với máy chủ thử 0.7.0 trong nhà:
  địa chỉ cũ vẫn chạy; gõ `https://` cho IP trong nhà → tự lùi về `http://`; mã sai → câu báo 404; tên miền viết hoa → `https://`, chữ
  thường, mã giữ nguyên; phát 1080p; màn hướng dẫn, hộp «Cách lấy mã»; nút «Chat với bot» bật Telegram Desktop lên ngay. IPA chính:
  `vn.chimto.tv` 1.26 / 33, có cảnh CarPlay + `SBStarkCapable`, không `no-container`, không cổng gỡ lỗi. **Chưa thử:** địa chỉ công khai thật
  có mã; cài IPA chính lên iPhone / iPad (lúc đăng cả hai máy thử không tới được — iPad vừa ngủ, iPhone không mở SSH / bản thử).
- Đăng lúc 08:31:05 (iPad) và 08:31:36 (Mac) bằng `release.ps1 ipad|mac -File dist\mac\…` (`--latest=false`, Latest vẫn `v2.23`); `-Verify`
  đạt cả hai: tải từ link công khai và link cố định khớp SHA-256. **Tự cập nhật thật Mac 1.25 → 1.26** trên Mac mini (`/Applications`): hộp
  «Có bản mới 1.26» đủ «Có gì mới» → «Cài đặt ngay» → app mở lại 1.26 / 33 lúc 08:33, `codesign` đạt; trên bản chính gõ địa chỉ máy chủ
  thử không có `http://` → kết nối, ra danh sách «Đã theo dõi». Trang hướng dẫn: dòng số bản → iPad / iPhone 1.26 · Mac 1.26 (site `9f2940e`).

## iPad / iPhone 1.25 và Mac 1.25 — 25/11/2026 (mục TubeMini — xem YouTube qua máy chủ TubeMini)

Anh giao: «lên phương án tích hợp tubemini vào Côn Bằng TV, vị trí menu bên dưới nút Truyền hình - radio», chốt tên «TubeMini», làm iPad /
iPhone / Mac trước, chưa làm phần ra mạng ngoài nhà, ẩn với hồ sơ trẻ em + tính giờ xem; rồi «ok rồi đó e, đẩy và phát hành giúp a». Mac
nhảy 1.23 → 1.25 cho cùng số với bản iPad.

- **Mục TubeMini** (dưới «Truyền hình · Radio»): app khách của máy chủ **tubemini-server** (dự án TubeMini, theo `docs/API.md` của dự án đó)
  — Đã theo dõi, Phổ biến, Âm nhạc, Tin tức, Thể thao, Trò chơi, Phim, Lịch sử, Kênh; ô tìm video / kênh có kính lúp ngay cạnh tiêu đề;
  thẻ video 16:9; trang kênh, theo dõi / bỏ theo dõi, xoá khỏi lịch sử; nhập địa chỉ máy chủ + mã (token) ngay trên trang.
- **Phát** bằng trình phát phim của app, luôn qua AVPlayer: danh sách đang xem thành «tập» (video kế, tự sang video sau), chất lượng 720p /
  1080p / 360p / Chỉ nghe là «server»; chờ quảng cáo đầu; xem tiếp + lưu chỗ xem trên máy chủ (dùng chung với app TubeMini). Màn xe CarPlay:
  có hình cả khi tắt màn iPhone.
- Cuộn xuống thì thanh trên (tiêu đề, ô tìm, chip) thu gọn, cuộn lên thì hiện lại (như menu nổi của Roza — anh yêu cầu).
- Hồ sơ trẻ em không có mục TubeMini; giới hạn giờ xem tính cả TubeMini.
- iPad / iPhone: bản 1.25 (build 32) · tag `ios-v1.25` · `ConBangTV-iPad-1.25.ipa` 6 287 089 byte · SHA-256
  `6e1b7844c47ff725417d4db771656a433587b0339052fad6b10042c3290d239a`; Mac: bản 1.25 (build 32) · tag `mac-v1.25` · `ConBangTV-Mac-1.25.zip`
  9 711 554 byte (x86_64 + arm64, ký ad-hoc, `codesign --verify` đạt, không cổng gỡ lỗi) · SHA-256
  `e5e9b253c49d840a27a9ec0d45c6789667c79a3d5ca3b5036b4152e10ccdac18` · commit `5b7b397` (tính năng: `f2ec7a2`, `ee42488`) · dựng trên Mac mini
- Thử: test lõi Swift 184 đạt (mới: `TubeMiniTests` — feed video / kênh, link phát, lịch sử, chữ hiện). Bản Mac thử (Mac mini cùng mạng máy
  chủ thử): feed, phát 720p (chờ quảng cáo 3 giây), xem tiếp đúng chỗ đã xem trong app TubeMini, chỗ xem ghi ngược lên máy chủ, đổi 1080p /
  Chỉ nghe giữ chỗ xem, thanh trên ẩn / hiện khi cuộn. iPhone trên màn xe CarPlay: anh tự mở và xem video TubeMini (720p, AVPlayer). IPA chính:
  `vn.chimto.tv` 1.25 / 32, có `SBStarkCapable`, không cổng gỡ lỗi; chưa cài lên iPhone lúc đăng (bản thử trên iPhone không mở để đẩy tệp).
- Đăng lúc 16:43:03 (iPad) và 16:43:34 (Mac) bằng `release.ps1 ipad|mac -File dist\mac\…` (`--latest=false`, Latest vẫn `v2.23`); link cố
  định chép đè, tải từ link công khai và link cố định khớp SHA-256. **Tự cập nhật thật Mac 1.23 → 1.25** trên Mac mini (`/Applications`):
  hộp «Có bản mới 1.25» đủ «Có gì mới» → «Cài đặt ngay» → app mở lại 1.25 / 32 lúc 16:46, `codesign` đạt, menu có TubeMini. Trang hướng
  dẫn: dòng số bản → iPad / iPhone 1.25 · Mac 1.25 (site `1e0d750`).

## iPad / iPhone 1.24 — 25/11/2026 (Côn Bằng TV trên màn xe CarPlay)

Anh hỏi «code cho nó hiện ở carplay» rồi muốn «giao diện app trên carplay full như trên màn hình iphone»; thử trên màn CarPlay độc lập
của anh (nối dây USB) với iPhone 6s Plus iOS 15.8.3. Chỉ bản iPad / iPhone (Mac giữ 1.23 — không có gì mới).

- **Màn xe CarPlay (chỉ iPhone, màn xe cảm ứng):** biểu tượng Côn Bằng TV trên CarPlay; màn xe hiện nguyên giao diện app, chạm trên màn
  xe; điện thoại thành bảng điều khiển (đang phát + nút, tìm phim, tắt sáng màn, «Dùng trên điện thoại»). Đổi qua lại giữa màn xe và điện
  thoại lúc đang xem thì phim mở lại đúng chỗ.
- **Phim KKPhim / link .m3u8 trên màn xe phát bằng AVPlayer:** có hình cả khi tắt màn iPhone (trình phát web thì không — WebKit bỏ khung
  hình khi màn điện thoại tắt), tự bỏ quảng cáo KKPhim chèn giữa phim, đi qua mạng của app (DNS mã hoá / vượt chặn). Server mã hoá của
  trang thì app tự đổi sang server KKPhim dự phòng nếu có.
- **Xe đang chạy** (tốc độ GPS): màn xe chỉ phát tiếng. **Bàn phím riêng** cho ô gõ chữ trên màn xe.
- iPad / iPhone: bản 1.24 (build 31) · tag `ios-v1.24` · `ConBangTV-iPad-1.24.ipa` 6 124 474 byte · SHA-256
  `d1847c09c6c3baf129adc707cad832b24c2dfd05deb37d14c239b0c4238d3568` · commit `a166e5a` (tính năng: `76b3911`, `ffce26f`; bỏ dòng nhắc
  «Bật màn iPhone…»: `a166e5a`) · dựng trên Mac mini (`mac-build.ps1 -Release`, test lõi đạt)
- Thử trên màn CarPlay thật: biểu tượng hiện, giao diện chính lên màn xe (535×300 điểm @3x), phim chạy 0 khung rớt; đổi màn xe ↔ điện
  thoại hai chiều (mở lại đúng giây); giả xe chạy → màn che + nút lớn, tiếng vẫn chạy; bàn phím riêng gõ «tien nghich» → Tìm ra 11 nhóm;
  AVPlayer (server KKPhim): xem tiếp đúng chỗ, dừng / phát / tua / tập sau, bỏ 1 khối quảng cáo, **anh xác nhận tắt màn iPhone mà màn xe
  vẫn có hình, mượt**; Truyền hình VTV1 cũng có hình khi tắt màn iPhone. IPA chính (`vn.chimto.tv` 1.24 / 31, có `SBStarkCapable`, không
  cổng gỡ lỗi) cài bằng TrollStore, anh mở trên màn xe — chạy bình thường. Chưa thử: khoá hình theo GPS khi xe chạy thật (anh thử sau).
- Đăng lúc 15:16:08 bằng `release.ps1 ipad -File dist\mac\ConBangTV-iPad-1.24.ipa` (`--latest=false`; Latest vẫn `v2.23`); link cố định
  `conbangtv_ios/ConBangTV-iPad.ipa` chép đè — tải từ link công khai và link cố định, khớp SHA-256.

## 2.23 — 18/11/2026 (sửa lỗi sau audit cả 5 bản)

Anh yêu cầu audit lại cả 5 bản («báo cáo lỗi và fix lỗi nếu có»), rồi duyệt thêm 5 mục đề xuất. Cùng đợt với iPad / iPhone / Mac 1.23,
Windows 1.21, Linux 1.7.

- **Giới hạn giờ xem ở Truyền hình:** hết giờ rồi bấm Home mở lại app thì kênh phát tiếp sau màn «Hết giờ xem» — nay không mở luồng khi
  màn chặn đang hiện; ra nền thì huỷ mọi việc chờ (mở lại sau 1 giây, thử lại trận sau 20 giây, đọc kênh) và nhịp đếm giây — không còn
  tự phát khi app ở nền.
- **«Ai đang xem?» không còn bị vượt qua:** quay lại app sau ≥ 30 phút mà màn hiện lên đầu không phải màn chính (Truyền hình, trang phim,
  trình phát, Cài đặt…) thì về màn chính hỏi người xem; thông báo nhắc giờ phát mở qua màn chính (qua màn miễn trừ / «Ai đang xem?» rồi
  mới mở kênh). «Sửa người xem…» ở màn chặn cửa của người có mã PIN chỉ mở phần Người xem.
- **Đồng bộ:** nhập sai mã ghép 20 lần (kể cả qua các lần đổi mã) thì ngừng cho ghép tới khi mở lại hộp Đồng bộ — mã 4 số trước đây máy lạ
  trong mạng dò hết được trong vài giây.
- **Kết nối:** DNS mã hoá giữ mọi địa chỉ của tên miền, cổng nội bộ thử lần lượt tối đa 4 địa chỉ rồi tới DNS của thiết bị.
- **Truyền hình:** lịch phát sóng dựng vào tệp tạm rồi đổi tên (không mất lịch lúc đang tải, lịch cũ không bị xoá khi tải lỗi); đổi danh sách
  lúc danh sách khác đang tải thì tải tiếp danh sách mới; mở kênh khi phim đang thu nhỏ thì tắt phim đó; kênh bị lọc (hồ sơ trẻ em) thì báo
  thay vì phát kênh số 1; nhật ký che username / mật khẩu trong link Xtream.
- **Khác:** «Kiểm tra cập nhật» không còn kẹt khi đóng Cài đặt giữa lúc tải; tên miền sai không làm mọi máy chủ DNS bị coi là hỏng; dán nhiều
  trang bỏ qua «v.v.», «e.g.»; xoá người xem thì xoá cả chất lượng video / ngôn ngữ phụ đề của người đó.
- bản 2.23 · versionCode 47 · tag `v2.23` (Latest) · commit `dbbb331` (sửa: `cb0a27d`, `037d9e3`) · `ConBangTV-2.23.apk` = `ConBangTV.apk` =
  `ChimToTV.apk` 151 569 152 byte · SHA-256 `ec7fcca761fc461e243bde1cbd530f6b79b3103cd8cb36c9b8efdca71d561c05` · chứng chỉ `949ef2b5…177b`
- Thử: test đơn vị 193 đạt (mới: `TvLogTest`, ca «v.v.» trong `SitePasteTest`). Bản thử trên TX3: hết giờ → Home → mở lại, kênh không phát
  lại; Home khi đang xem thì dừng hẳn, mở lại phát tiếp; chỉnh đồng hồ giả lập 31 phút → về màn chính hỏi «Ai đang xem?»; ý định thông báo
  nhắc giờ → hỏi người xem rồi mới mở VTV1; «Sửa người xem…» ở màn chặn của hồ sơ có khoá chỉ còn phần Người xem; lịch phát sóng giữ nguyên
  lúc đang tải lại.
- Đăng lúc 11:45:53 bằng `release.ps1 tv` (Latest = `v2.23`, ba tệp cùng SHA-256, hai link cố định đúng 2.23). **Tự cập nhật thật 2.22 → 2.23
  trên TX3**: «Có bản mới 2.23 (đang dùng 2.22)» (đủ «Có gì mới») → «Cài đặt ngay» → màn cài của hệ thống (▼ rồi ▶ tới «CÀI ĐẶT») → **2.23
  lúc 12:03:27**, mở lại chạy bình thường.

## iPad / iPhone 1.23 và Mac 1.23 — 18/11/2026 (sửa lỗi sau audit)

- **Hết giờ xem:** phim ở cửa sổ hình trong hình / bấm ▶ trên màn hình khoá, AirPods thì dừng lại ngay (trước chỉ dừng một lần); không vào
  hình trong hình khi đang hết giờ; truyền hình huỷ việc chờ khi hết giờ (không tự phát sau màn «Hết giờ xem»).
- **«Ai đang xem?»:** xoá người đang xem từ màn này (mở từ menu) mà người còn lại đều có mã PIN thì màn này thành màn chặn cửa; ở màn chặn của
  người có mã PIN, nút thành «Quản lý người xem…» và chỉ mở phần Người xem (không xoá lịch sử / đổi trang phim… khi chưa nhập mã).
- **Truyền hình:** không còn nhảy về kênh cũ khi danh sách vừa tải lại; danh sách tải ra tệp tạm, đọc được kênh mới thay tệp cũ (trang HTML
  của Wi‑Fi khách sạn không còn xoá danh sách); bộ lọc «Yêu thích» nhớ theo người xem; lưới kênh không tràn màn iPhone khi chữ cỡ lớn; chạm
  widget «Xem tiếp» lúc đang nghe radio thì tắt kênh trước.
- **Đồng bộ:** sai mã ghép 20 lần thì ngừng cho ghép. **Kết nối:** DNS mã hoá dùng mọi địa chỉ, hết thì DNS của máy (mạng IPv6 / NAT64); tải
  danh sách kênh thử lại bằng mạng của máy với mọi lỗi kết nối. **Khác:** không văng với JSON lồng quá sâu / số tập quá lớn; bộ cập nhật đọc
  100 release; nhật ký che tài khoản trong link; dán nhiều trang bỏ qua «v.v.».
- iPad / iPhone: bản 1.23 (build 30) · tag `ios-v1.23` · `ConBangTV-iPad-1.23.ipa` 5 966 967 byte · SHA-256
  `117d3a7b0c1a1c2a33204b4f7284d9c3654045fe3a6805bd45952882a92f7139`; Mac: bản 1.23 (build 30) · tag `mac-v1.23` · `ConBangTV-Mac-1.23.zip`
  9 069 372 byte (Intel + Apple Silicon, ký ad-hoc, `codesign --verify` đạt, không cổng gỡ lỗi) · SHA-256
  `e68a787aaea731a2c13848b4a862fdfd92c6eedcde4412640d5b65a0f1a1ab60` · commit `dbbb331` (sửa: `cb8052b`, `13b140e`) · dựng trên Mac mini
- Thử: test lõi Swift 179 đạt (mới: `LogTests`, ca độ lồng JSON / số lớn, «v.v.»). Bản Mac thử: phát / chuyển / đóng kênh; DNS trả đủ các địa
  chỉ; màn chặn của hồ sơ có khoá → «Quản lý người xem…» chỉ mở trang «Người xem». IPA chính (`vn.chimto.tv` 1.23 / 30, không `/tvstatus`) cài
  lên iPhone 6s Plus bằng TrollStore, mở được, không có báo cáo sập.
- Đăng lúc 11:48:31 (iPad) và 12:04:16 (Mac) bằng `release.ps1 ipad|mac -File dist\mac\…`; link cố định chép đè, khớp SHA-256. **Tự cập nhật
  thật Mac 1.22.1 → 1.23** (Mac mini): «Có bản mới 1.23 — Đang dùng 1.22.1» → «Cài đặt ngay» → mở lại 1.23 trong ~5 giây (12:06).

## Windows 1.21 — 18/11/2026 (sửa lỗi sau audit)

- **Truyền hình:** không còn nhảy về kênh cũ khi danh sách vừa tải lại; «Kênh trước / sau nữa» chỉ lật trang bảng kênh (trước đổi luôn kênh
  đang xem); trang Truyền hình không còn hai hàng nhóm kênh khi vẽ lại chồng nhau.
- **Hộp thoại khuất sau trình phát:** «Có bản mới» không hiện lúc đang xem phim / truyền hình (lỡ bấm OK là cập nhật giữa lúc xem) — hỏi khi
  đóng trình phát, và hỏi sau màn «Ai đang xem?»; nhắc giờ phát báo ngay trong trình phát phim.
- Mở trình duyệt chỉ nhận link http(s). Từ lõi Rust: sai mã ghép 20 lần thì ngừng cho ghép (bỏ SO_REUSEADDR — hai cửa sổ app không nghe chồng
  cổng); danh sách kênh tải lỗi không xoá danh sách đang dùng; DNS mã hoá dùng mọi địa chỉ; bộ cập nhật đọc 100 release; log.txt che tài khoản
  trong link; JSON lồng quá sâu không làm app văng; ghi prefs.json lần lượt.
- bản 1.21.0 · tag `win-v1.21` · `ConBangTV-Windows-1.21.exe` 120 364 438 byte · SHA-256
  `97a66f7a6cd53ac00a13118fe07b9e30b23bba09dc48721f5656342c3b965aaa` · commit `dbbb331` (sửa: `799da2e`, `8c7454c`)
- Thử: `windows\build.ps1` — test lõi Rust 195 + tích hợp đạt, .NET 38 đạt. Bản thử: lật trang bảng kênh giữ kênh 1, chọn kênh 141 ở trang mới
  mở được, ▼ sang 142; trang chủ và kênh phát qua đường kết nối mới. Tệp đóng gói chạy với thư mục dữ liệu riêng.
- Đăng lúc 11:51:04 bằng `release.ps1 windows`; link cố định `conbangtv_windows` = 1.21. **Tự cập nhật thật 1.20 → 1.21** (bản sao chạy riêng):
  «Có bản mới» → «Cập nhật ngay» (UI Automation) 12:07:59 → tệp chạy 1.21.0 lúc 12:08:18, mở lại bình thường.

## Linux 1.7 — 18/11/2026 (sửa lỗi sau audit)

- **Đỡ tốn bộ nhớ:** trang rời khỏi ngăn trang (mở mục menu khác, thay trang, Back) được huỷ từng ô — trước đây hàm bấm giữ tham chiếu tới
  chính trang nên không trang nào được giải phóng (ảnh bìa, hẹn giờ mỗi giây của màn Truyền hình). Thử 120 lần chuyển trang: 120/120 trang
  được giải phóng, bộ nhớ đứng ~150 MB (trước tăng 159 → 183 MB).
- **Truyền hình:** không còn nhảy về kênh cũ khi danh sách vừa tải lại (số lượt không về 0 khi mở lại); «Kênh trước / sau nữa» chỉ lật trang;
  link kênh có ký tự NUL không làm app tắt; đóng app ghi nốt giờ đã xem truyền hình; không đưa con trỏ về menu ẩn sau trình phát.
- **Hộp thoại:** Tab / Shift+Tab / Enter không lọt xuống trang phía sau (màn «Ai đang xem?» chặn cửa) — Tab đi vòng trong hộp thoại.
- Từ lõi Rust: như bản Windows 1.21 (mã ghép, danh sách kênh, DNS mọi địa chỉ, cập nhật 100 release, log che tài khoản, JSON sâu).
- bản 1.7.0 · tag `linux-v1.7` · `ConBangTV-Linux-1.7.deb` 6 224 572 byte · SHA-256 `3854fe04ac7baec0f941deeb3c5b3aac6c023fdbb1ebcd58d02ab5053d60ea2f` ·
  commit `dbbb331` (sửa: `799da2e`, `8c7454c`) · dựng trong container Ubuntu 22.04 (glibc ≤ 2.34), tệp chạy không có cổng gỡ lỗi
- Thử: test lõi đạt trên máy ảo (195 + tích hợp). Bản thử: Tab trong hộp miễn trừ đi vòng «Đồng ý» ↔ «Thoát»; lật trang bảng kênh giữ kênh đang
  xem, chọn kênh 141 mở được; 120 lần chuyển trang không văng, không lỗi GTK.
- Đăng lúc 11:53:31 bằng `release.ps1 linux`; link cố định `conbangtv_linux` = 1.7. **Tự cập nhật thật 1.6 → 1.7** trên máy ảo: «Có bản mới:
  Côn Bằng TV 1.7» → «Cập nhật ngay» → gói 1.7.0 lúc ~12:05:20, mở lại `--updated`.

## iPad / iPhone 1.22.1 và Mac 1.22.1 — 17/11/2026 (sửa: bàn phím che ô dán nhiều trang)

- **Sửa lỗi của 1.22 trên iPhone / iPad:** ô dán nhiều trang (SwiftUI `TextEditor`) ở **Chọn trang phim** → «Thêm trang» và **Cài đặt** → **Địa chỉ trang phim** không tự cuộn lên trên bàn phím như ô một dòng cũ — chạm ô ở nửa dưới màn hình là bàn phím che ô, gõ không thấy chữ (đo trên iPhone 6s Plus iOS 15: bàn phím từ y 435; ô ở y 415 / 513 → bị che). Giờ chọn ô (và khi ô thêm / bớt dòng) thì danh sách cuộn ô lên sát thanh tiêu đề, nút Thêm / Lưu ngay dưới (`@FocusState` + `ScrollViewReader`, neo mép trên — neo nút vào mép bàn phím thì Form Cài đặt cuộn quá ~200 pt). Mac không đổi cách dùng (không cuộn), chỉ giữ cùng số bản.
- Bản thử: cổng gỡ lỗi có thêm `/focus` (đặt con trỏ vào ô nhập, báo khung bàn phím và ô / con trỏ có bị che) và `/type?back=N` — để thử gõ phím thật trên máy.
- iPad / iPhone: bản 1.22.1 (build 29) · tag `ios-v1.22.1` · `ConBangTV-iPad-1.22.1.ipa` 5 956 912 byte · SHA-256 `11ae12cb1e1ec288c2a38cc2a6cc6c332ccecb844b7fbe5e4541518512562056`; Mac: bản 1.22.1 (build 29) · tag `mac-v1.22.1` · `ConBangTV-Mac-1.22.1.zip` 9 050 802 byte (Intel + Apple Silicon, ký ad-hoc, `codesign --verify` đạt, không cổng gỡ lỗi) · SHA-256 `b01aa7dbf3c4ea39c4b9783b4f80090506012f76f956902bbf7529fffe7fafec` · commit `2ea045c` (sửa: `349927b`) · dựng trên Mac mini
- Thử: test lõi Swift 178 đạt. iPhone thật (bản thử, gõ từng phím qua `/type`): trước sửa ô bị che; sau sửa ô lên y 160–163, con trỏ thấy được sau khi gõ 2 dòng ở cả hai màn; «Lưu» 2 trang mới + 1 đã có → «Đã thêm 2 trang … (bỏ qua 1 đã có).», trang đang dùng giữ nguyên. IPA chính cài lên iPhone 1.22 → 1.22.1, mở được. Chưa thử trên iPad.
- Đăng lúc 12:45:41 (iPad) và 12:45:58 (Mac) bằng `release.ps1 ipad|mac -File dist\mac\…`; Latest vẫn `v2.22`; link cố định chép đè, khớp SHA-256. **Tự cập nhật thật Mac 1.22 → 1.22.1** (Mac mini): «Có bản mới 1.22.1 — Đang dùng 1.22» → «Cài đặt ngay» → mở lại 1.22.1 (build 29) lúc 12:46:50.

## iPad / iPhone 1.22 và Mac 1.22 — 17/11/2026 (dán một lần nhiều trang phim)

- **Thêm trang phim: dán một lần nhiều tên miền** (anh yêu cầu, «tương tự như nhập list danh sách kênh truyền hình») — ở **Chọn trang phim** → ô «Thêm trang» và **Cài đặt** → **Địa chỉ trang phim** (cả hai thành ô nhiều dòng). Mỗi dòng / mỗi chữ một trang; bỏ dòng trống, dòng `#`, chữ không có dấu chấm, số bản kiểu `1.5`; bỏ ký tự bao quanh và dấu chấm cuối câu; rút về gốc trang; trang đã có (không tính `www.`, http / https, `/` cuối, đường dẫn) tự bỏ qua. Nhiều trang mới → chỉ thêm vào danh sách (đo tốc độ luôn), trang đang dùng giữ nguyên, báo «Đã thêm N trang: a, b, c (bỏ qua K đã có)»; một trang thì như trước (thêm rồi chuyển sang trang đó). «Kiểm tra kết nối» khi đang dán nhiều trang thì nhắc kiểm từng trang.
- `Prefs.parseSites` / `splitSites` / `addSites` cùng luật với bản TV, Windows, Linux (`Core/Prefs.swift`); test `CoreTests/SitePasteTests.swift`.
- iPad / iPhone: bản 1.22 (build 28) · tag `ios-v1.22` · `ConBangTV-iPad-1.22.ipa` 5 951 171 byte · SHA-256 `56211fa67b6b8469639ed4af789d61df8ecaa84a869cca4d2281171ec42bfe71`; Mac: bản 1.22 (build 28) · tag `mac-v1.22` · `ConBangTV-Mac-1.22.zip` 9 037 622 byte (Intel + Apple Silicon, ký ad-hoc, `codesign --verify` đạt) · SHA-256 `dadc6597009d5a4f8b4186c43ff2f0594bf67d94ae8403c426703b4fda84308a` · commit `0af3e7f` (tính năng: `c967170`) · dựng trên Mac mini
- Thử: test lõi Swift 178 đạt (Mac mini). Bản Mac thử: một trang → thêm và chuyển trang; dán 3 dòng → «Đã thêm 2 trang … (bỏ qua 1 đã có)», trang đang dùng giữ nguyên; dán toàn trang đã có → «Các trang này đã có trong danh sách.», chữ còn trong ô; chữ không có tên miền → câu báo đúng. IPA chính cài lên iPhone 1.20 → 1.22, mở được. Chưa gõ tay trên iPad / iPhone thật.
- Đăng lúc 10:41:55 (iPad) và 10:46:05 (Mac) bằng `release.ps1 ipad|mac -File dist\mac\…`; link cố định chép đè, khớp SHA-256. **Tự cập nhật thật Mac 1.21 → 1.22** (Mac mini): «Có bản mới 1.22 — Đang dùng 1.21» → «Cài đặt ngay» → mở lại 1.22 (build 28) lúc 10:51:22.

## Windows 1.20 — 17/11/2026 (dán một lần nhiều trang phim)

- **Thêm trang phim: dán một lần nhiều tên miền** — **Đổi trang phim** → ô «Thêm trang» thành ô nhiều dòng (Enter xuống dòng, **Ctrl+Enter = Thêm**; ▲ ▼ đi giữa các dòng, ở dòng đầu / cuối thì sang mục khác). Cùng luật tách / bỏ trùng với các bản khác (số bản kiểu `1.5` bỏ qua, `bad..host` báo không hợp lệ, tính cổng). Nhiều trang → chỉ thêm vào danh sách và đo từng trang, trang đang dùng giữ nguyên; một trang như trước.
- `Prefs.SiteKey` / `SplitSites` / `AddSites` + `AddSitesResult` (câu báo giống lõi Rust); test `ConBangTV.Tests/SitesTests.cs`.
- bản 1.20.0 · tag `win-v1.20` · `ConBangTV-Windows-1.20.exe` 120 339 145 byte · SHA-256 `cf4c6ea6ad83aafe73b4eb96131cce89c38dccfdb1eac9508fb97f330dbda780` · commit `0af3e7f` (tính năng: `73f1483`)
- Thử: `windows\build.ps1` — test lõi Rust đạt (cả test tích hợp mới `core/tests/sites.rs`), .NET 38 đạt. Bản thử (thư mục dữ liệu riêng): dán 3 dòng → «Đã thêm 2 trang … (bỏ qua 1 đã có).», trang đang dùng giữ nguyên; trang đã có / tên sai báo đúng, chữ giữ trong ô; ▲ ▼ trong ô đúng.
- Đăng lúc 10:41:10 bằng `release.ps1 windows`; link cố định `conbangtv_windows` = 1.20. **Tự cập nhật thật 1.19 → 1.20** (bản sao chạy riêng, bỏ ghép đồng bộ): «Có bản mới: Côn Bằng TV 1.20» → «Cập nhật ngay» (UI Automation) → «Cài bản 1.20» 10:50:02 → «Mở bản 1.20 (--updated)» 10:50:04.

## Linux 1.6 — 17/11/2026 (dán một lần nhiều trang phim)

- **Thêm trang phim: dán một lần nhiều tên miền** — **Đổi trang phim** → ô «Thêm trang» thành TextView nhiều dòng (**Ctrl+Enter = Thêm**; Backspace trong ô không còn là «quay lại»; ▲ ▼ đi giữa các dòng). Cùng luật với các bản khác; nhiều trang → chỉ thêm vào danh sách, trang đang dùng giữ nguyên; một trang như trước.
- Lõi Rust `prefs::split_sites` / `add_sites` / `site_key` (dùng chung với bản Windows); test `core/tests/sites.rs` + test trong `prefs.rs`.
- bản 1.6.0 · tag `linux-v1.6` · `ConBangTV-Linux-1.6.deb` 6 213 472 byte · SHA-256 `9b74a343c365af7855e0114e393713adb0a00effc7657305b145a777dbf2bb1c` · commit `0af3e7f` (tính năng: `73f1483`) · dựng trong container Ubuntu 22.04 (glibc ≤ 2.34), tệp chạy không có cổng gỡ lỗi
- Thử: test lõi đạt (192 + tích hợp). Bản thử trên máy ảo: dán thật qua clipboard + Ctrl+Enter → «Đã thêm 2 trang … (bỏ qua 1 đã có).», trang đang dùng giữ nguyên; trang đã có, tên sai, một trang mới đều đúng; ▲ ▼ / Backspace trong ô (cả hộp thêm danh sách kênh) đúng.
- Đăng lúc 10:41:34 bằng `release.ps1 linux`; link cố định `conbangtv_linux` = 1.6. **Tự cập nhật thật 1.5 → 1.6** trên máy ảo: «Có bản mới: Côn Bằng TV 1.6» → «Cập nhật ngay» → gói 1.6.0 lúc 10:48:23, mở lại `--updated`.

## 2.22 — 17/11/2026 (dán một lần nhiều trang phim)

- **Thêm trang phim: dán một lần nhiều tên miền** (anh yêu cầu) — **Cài đặt** → **Danh sách trang phim** → nút mới **＋ Thêm nhiều trang…**, và **Đổi trang phim** → **＋ Thêm trang khác…** (hộp thành ô nhiều dòng, Enter = xuống dòng). Mỗi dòng / mỗi chữ một trang; bỏ dòng trống, dòng `#`, chữ không có dấu chấm, số bản kiểu `1.5`; bỏ ký tự bao quanh và dấu chấm cuối câu; rút về gốc trang (`kkphim.com` → trang KKPhim mẫu như trước); trang đã có (không tính `www.`, http / https, `/` cuối, đường dẫn) tự bỏ qua. Nhiều trang → chỉ thêm vào cuối danh sách (đồng bộ trong nhà ghi nhận), trang đang dùng giữ nguyên, báo «Đã thêm N trang: a, b, c (bỏ qua K đã có).»; không có trang mới → báo ngay trong ô, chữ giữ lại để sửa. Một trang như trước (ở «Đổi trang phim» là thêm rồi chuyển sang).
- `SitePaste.kt` (tách / chuẩn hoá / bỏ trùng, thuần để test JVM) + `Prefs.addSites`; test `SitePasteTest` (5).
- bản 2.22 · versionCode 46 · tag `v2.22` (Latest) · commit `0af3e7f` (tính năng: `1266275`) · `ConBangTV-2.22.apk` = `ConBangTV.apk` = `ChimToTV.apk` 151 566 660 byte · SHA-256 `52d4dde6a5885df181830249d2d645532fa65fabf0e5950bb5ab8f233664ddcc` · chứng chỉ `949ef2b5…177b`
- Thử: test đơn vị đạt. Bản thử trên TX3: dán nhiều trang ở Cài đặt và ở «Đổi trang phim», trang đã có / chữ sai báo đúng. Bản chính chạy thử trên TX3 (dán 3 dòng toàn trang đã có + số bản «1.5» → «Các trang này đã có trong danh sách.») rồi hạ về 2.21.
- Đăng lúc 10:40:12 bằng `release.ps1 tv` (Latest = `v2.22`, ba tệp cùng SHA-256, hai link cố định đúng 2.22). **Tự cập nhật thật 2.21 → 2.22 trên TX3**: «Kiểm tra cập nhật» → «Có bản mới 2.22 (đang dùng 2.21)» (hiện đủ «Có gì mới») → «Cài đặt ngay» → màn cài của hệ thống → **2.22 lúc 10:47:22**.

## iPad / iPhone 1.21 và Mac 1.21 — 17/11/2026 (hàng Truyền hình trên trang chủ, dán nhiều danh sách)

- **4 hàng Truyền hình trên trang chủ** — «Kênh vừa xem», «Kênh yêu thích», «Trận đang / sắp đá», «Radio» (anh chọn): dải thẻ kênh (logo, số kênh, ★, chương trình đang phát + tiến độ, nhãn ĐANG ĐÁ / SẮP ĐÁ + giờ bóng lăn), bấm là phát (kênh của danh sách khác thì đổi danh sách trước; hàng yêu thích phát trong nhóm yêu thích), «Xem thêm» mở Truyền hình (yêu thích chọn sẵn). Là hàng trang chủ thật (`tv-recent` / `tv-favs` / `tv-live` / `tv-radio`): mặc định bật ngay sau «Xem tiếp» (người dùng cũ tự có), bật / tắt / đổi thứ tự trong Cài đặt → Trang chủ → «Các hàng trên trang chủ…» (ghi «(Truyền hình)») và sửa nhanh trên hàng; hàng rỗng tự ẩn; hồ sơ trẻ em không có hàng trận / radio; dữ liệu dựng tại chỗ, đọc ở nền.
- **Thêm danh sách kênh: dán một lần nhiều địa chỉ** (mỗi dòng một danh sách; thiếu `http://` tự thêm; địa chỉ đã có — kể cả khác http / https, dấu `/` cuối — tự bỏ qua; danh sách đầu tiên tải ngay, các danh sách khác tải khi chọn; mỗi danh sách một mã riêng).
- Dựng bản chính ngay trên Mac mini (`ios/scripts/mac-build.ps1 -Release`, `-Mac -Release -NoTest`) — lần đầu theo quy trình mới (anh chọn), không qua GitHub Actions.
- iPad / iPhone: bản 1.21 (build 27) · tag `ios-v1.21` · `ConBangTV-iPad-1.21.ipa` 5 942 986 byte · SHA-256 `f1ee3017e6bab915357d4f36dbb21831dd47f7144444faefc86eef8b7afe626c`; Mac: bản 1.21 (build 27) · tag `mac-v1.21` · `ConBangTV-Mac-1.21.zip` 9 023 625 byte (Intel + Apple Silicon, ký ad-hoc, `codesign --verify` đạt) · SHA-256 `ca9382ba4ca91cd029fc2b3574b1f2520cb60fdfde82fe506cf9a1daddf0c60a` · commit `98ba8ca` (tính năng: `bb3fe3d`, `b9352f4`)
- Thử: test lõi Swift đạt (HomeRows 14, Iptv 8 — trên Mac mini). Bản Mac thử: các hàng đúng thứ tự, sửa nhanh qua `/row`, «Các hàng…» có 4 dòng mới, bấm thẻ kênh của danh sách khác đổi danh sách và phát 720p, hàng trận với danh sách thể thao; dán 4 địa chỉ → «Đã thêm … (bỏ qua 3 đã có)». iPhone 6s Plus dọc: dải vuốt ngang, chạm thẻ mở trình phát; IPA chính cài lên iPhone 1.20 → 1.21, mở được, không có báo cáo sập. Chưa thử trên iPad và hồ sơ trẻ em (chỉ test).
- Đăng lúc 09:43:04 (iPad) và 09:43:19 (Mac) bằng `release.ps1 ipad|mac -File dist\mac\…`; link cố định chép đè, khớp SHA-256. **Tự cập nhật thật Mac 1.20 → 1.21** (Mac mini): «Có bản mới 1.21 — Đang dùng 1.20» → «Cài đặt ngay» 09:48:07 → mở lại 1.21 lúc 09:48:13.

## Windows 1.19 — 17/11/2026 (hàng Truyền hình trên trang chủ, dán nhiều danh sách)

- **4 hàng Truyền hình trên trang chủ** — «Kênh vừa xem», «Kênh yêu thích», «Trận đang / sắp đá», «Radio» (anh chọn): dải thẻ kênh (logo, số kênh, ★, chương trình đang phát + tiến độ, nhãn ĐANG ĐÁ / SẮP ĐÁ + giờ bóng lăn), bấm là phát (kênh của danh sách khác thì đổi danh sách trước; hàng yêu thích phát trong nhóm yêu thích), «Xem thêm» mở Truyền hình (yêu thích chọn sẵn). Là hàng trang chủ thật (`tv-recent` / `tv-favs` / `tv-live` / `tv-radio`): mặc định bật ngay sau «Xem tiếp» (người dùng cũ tự có), bật / tắt / đổi thứ tự trong Cài đặt → Trang chủ → «Các hàng trên trang chủ…» (ghi «(Truyền hình)») và sửa nhanh trên hàng; hàng rỗng tự ẩn; hồ sơ trẻ em không có hàng trận / radio; dữ liệu dựng tại chỗ, đọc ở nền.
- **Thêm danh sách kênh: dán một lần nhiều địa chỉ** (mỗi dòng một danh sách; thiếu `http://` tự thêm; địa chỉ đã có — kể cả khác http / https, dấu `/` cuối — tự bỏ qua; danh sách đầu tiên tải ngay, các danh sách khác tải khi chọn; mỗi danh sách một mã riêng).
- Lõi Rust: `iptv::split_urls` / `add_lists` (FFI `iptv.addMany`), dữ liệu 4 hàng; `HomeFeed.cs` giữ khớp luật hàng của lõi. Hộp thêm danh sách: ô nhiều dòng (Ctrl+Enter = Thêm). Sửa nhanh hàng: chuột phải / Apps / Shift+F10 / M.
- bản 1.19.0 · tag `win-v1.19` · `ConBangTV-Windows-1.19.exe` 120 345 677 byte · SHA-256 `65eeb986144acd9a12fd120a8991b4a35405c7164ac9a447b44f9e0f5a2c154f` · commit `98ba8ca` (tính năng: `bb3fe3d`, `fc03186`)
- Thử: `windows\build.ps1` — test lõi Rust đạt (190 + 37 test tích hợp), .NET 33 đạt. Bản thử: dán đúng 4 địa chỉ → thêm 4 danh sách, dán lại → «Các danh sách này đã có trong app.»; trang chủ có 3 hàng (vừa xem / yêu thích / radio), hàng trận với danh sách thể thao, menu sửa nhanh chuyển hàng lưu đúng, «Các hàng…» có 4 dòng mới, thẻ kênh của danh sách khác đổi danh sách rồi phát.
- Đăng lúc 09:42:15 bằng `release.ps1 windows`; link cố định `conbangtv_windows` = 1.19. **Tự cập nhật thật 1.18 → 1.19** (bản sao chạy riêng): tải + «Cài bản 1.19» 09:47:08 → «Mở bản 1.19 (--updated)» 09:47:09 (nút «Cập nhật ngay» được bấm trên màn hình — lệnh UI Automation của em không tìm thấy nút).

## Linux 1.5 — 17/11/2026 (hàng Truyền hình trên trang chủ, dán nhiều danh sách)

- **4 hàng Truyền hình trên trang chủ** — «Kênh vừa xem», «Kênh yêu thích», «Trận đang / sắp đá», «Radio» (anh chọn): dải thẻ kênh (logo, số kênh, ★, chương trình đang phát + tiến độ, nhãn ĐANG ĐÁ / SẮP ĐÁ + giờ bóng lăn), bấm là phát (kênh của danh sách khác thì đổi danh sách trước; hàng yêu thích phát trong nhóm yêu thích), «Xem thêm» mở Truyền hình (yêu thích chọn sẵn). Là hàng trang chủ thật (`tv-recent` / `tv-favs` / `tv-live` / `tv-radio`): mặc định bật ngay sau «Xem tiếp» (người dùng cũ tự có), bật / tắt / đổi thứ tự trong Cài đặt → Trang chủ → «Các hàng trên trang chủ…» (ghi «(Truyền hình)») và sửa nhanh trên hàng; hàng rỗng tự ẩn; hồ sơ trẻ em không có hàng trận / radio; dữ liệu dựng tại chỗ, đọc ở nền.
- **Thêm danh sách kênh: dán một lần nhiều địa chỉ** (mỗi dòng một danh sách; thiếu `http://` tự thêm; địa chỉ đã có — kể cả khác http / https, dấu `/` cuối — tự bỏ qua; danh sách đầu tiên tải ngay, các danh sách khác tải khi chọn; mỗi danh sách một mã riêng).
- `pages/home_tv.rs` (mới) dùng chung cho hai kiểu trang chủ; hộp thêm danh sách là TextView nhiều dòng (giữ riêng bộ đệm chữ — đóng hộp là ô bị huỷ).
- bản 1.5.0 · tag `linux-v1.5` · `ConBangTV-Linux-1.5.deb` 6 209 512 byte · SHA-256 `3457cd7208b9f078b6f9b36a0ba1714e44f2847c40ee9255d46533be1ee6d34b` · commit `98ba8ca` (tính năng: `bb3fe3d`, `fc03186`) · dựng trong container Ubuntu 22.04 (glibc ≤ 2.34)
- Thử: bản thử trên máy ảo: dán 4 địa chỉ bằng Ctrl+V → «Đã thêm 3 danh sách (bỏ qua 1 đã có)» (danh sách cũ lưu `http`, địa chỉ dán `https`), danh sách thể thao tải ngay; trang chủ: vừa xem + trận, yêu thích + chương trình đang phát, menu sửa nhanh / ẩn / chuyển, «Các hàng…», «Xem thêm» mở Truyền hình đúng bộ lọc, trang chủ theo trang.
- Đăng lúc 09:42:41 bằng `release.ps1 linux`; link cố định `conbangtv_linux` = 1.5. **Tự cập nhật thật 1.4 → 1.5** trên máy ảo: «Có bản mới: Côn Bằng TV 1.5» → «Cập nhật ngay» → gói 1.5.0 lúc 09:46:31, mở lại `--updated`.

## 2.21 — 17/11/2026 (hàng Truyền hình trên trang chủ, dán nhiều danh sách)

- **4 hàng Truyền hình trên trang chủ** — «Kênh vừa xem», «Kênh yêu thích», «Trận đang / sắp đá», «Radio» (anh chọn): dải thẻ kênh (logo, số kênh, ★, chương trình đang phát + tiến độ, nhãn ĐANG ĐÁ / SẮP ĐÁ + giờ bóng lăn), bấm là phát (kênh của danh sách khác thì đổi danh sách trước; hàng yêu thích phát trong nhóm yêu thích), «Xem thêm» mở Truyền hình (yêu thích chọn sẵn). Là hàng trang chủ thật (`tv-recent` / `tv-favs` / `tv-live` / `tv-radio`): mặc định bật ngay sau «Xem tiếp» (người dùng cũ tự có), bật / tắt / đổi thứ tự trong Cài đặt → Trang chủ → «Các hàng trên trang chủ…» (ghi «(Truyền hình)») và sửa nhanh trên hàng; hàng rỗng tự ẩn; hồ sơ trẻ em không có hàng trận / radio; dữ liệu dựng tại chỗ, đọc ở nền.
- **Thêm danh sách kênh: dán một lần nhiều địa chỉ** (mỗi dòng một danh sách; thiếu `http://` tự thêm; địa chỉ đã có — kể cả khác http / https, dấu `/` cuối — tự bỏ qua; danh sách đầu tiên tải ngay, các danh sách khác tải khi chọn; mỗi danh sách một mã riêng).
- Thay dải chip «Kênh vừa xem» cũ (2.20) bằng hàng thẻ kênh; ô kênh ngang trong dải, OK phát, Menu / giữ OK sửa nhanh hàng. Trang gửi danh sách từ điện thoại cũng nhận nhiều địa chỉ. Sửa kèm: trang chủ theo trang — chuyển hàng vẫn đứng đúng ô.
- bản 2.21 · versionCode 45 · tag `v2.21` (Latest) · commit `98ba8ca` (tính năng: `bb3fe3d`, `59c9bd6`) · `ConBangTV-2.21.apk` = `ConBangTV.apk` = `ChimToTV.apk` 151 565 087 byte · SHA-256 `b7054342e94f9dab767949ae21f66d49660312e1040e5e1e9e25fef36e5dabaa` · chứng chỉ `949ef2b5…177b`
- Thử: test đơn vị đạt. Bản thử trên TX3: các hàng đúng chỗ, OK phát kênh và quay về đúng ô, Menu → ẩn / chuyển hàng, «Các hàng…» có 4 dòng mới, «Xem thêm kênh» mở Truyền hình với Yêu thích chọn sẵn, hàng trận với danh sách thể thao; gõ 4 địa chỉ (Enter giữa các dòng) → «Các danh sách này đã có trong app.». Bản chính chạy thử trên TX3 rồi hạ về 2.20.
- Đăng lúc 09:41:12 bằng `release.ps1 tv` (Latest = `v2.21`, ba tệp cùng SHA-256, hai link cố định đúng 2.21). **Tự cập nhật thật 2.20 → 2.21 trên TX3**: «Kiểm tra cập nhật» → «Có bản mới 2.21 (đang dùng 2.20)» → «Cài đặt ngay» → màn cài của hệ thống → **2.21 lúc 09:45:51**.

## Lịch sử repo viết lại — 17/11/2026 (không phải bản mới)

- Commit «dán nhiều danh sách» lúc đầu có hai link rút gọn thật (danh sách thử) trong bài test; đã viết lại 6 commit (thay bằng `short.example/…`) và đẩy đè `main` / `cachang` trước khi gắn tag phát hành (anh đồng ý). Mã app không đổi.

## iPad / iPhone 1.20 và Mac 1.20 — 17/11/2026 (Truyền hình · Radio, logo mới)

- **Mục mới «Truyền hình · Radio»** (port từ bản Legacy, giao diện theo kiểu app hiện đại — anh dặn không theo kiểu iOS 6): một mục
  trong menu, vẽ ngay trong khung chính. Đầu trang có ô tìm (tên / số kênh), hàng «Kênh vừa xem» vuốt ngang, dải chip nhóm kênh;
  thẻ kênh có logo, số kênh, nhãn ĐANG ĐÁ / SẮP ĐÁ / RADIO / PHIM, chương trình đang phát + tiến độ + chương trình kế; giữ tay /
  chuột phải mở menu nhanh (yêu thích, lịch, nhắc bóng lăn). Lõi Swift `Core/IptvParse.swift` + `Core/Iptv.swift` (bản Swift của
  `linux/core/src/iptv.rs`: M3U, XMLTV cả `.gz`, giờ trận, gộp nguồn, kênh lỗi, yêu thích / vừa xem theo người xem, nhắc giờ).
- **Trình phát truyền hình** (`Player/LivePlayer.swift`, AVPlayer) cùng kiểu trình phát phim: nút tròn, nhãn TRỰC TIẾP, bảng kênh trượt
  từ phải, vuốt dọc đổi kênh; nguồn lỗi tự sang nguồn sau, luồng FLV (AVPlayer không phát được) đánh dấu lỗi và chuyển nguồn; tạm
  dừng → CHẬM, «Về trực tiếp»; radio hiện thẻ đài trên nền logo làm mờ, tên bài (ICY), «MP3 · 56 kbps».
  Bàn phím (iPad / Mac): ↑ ↓ / PgUp PgDn đổi kênh, số, Space, G lịch, F yêu thích, Esc.
- Lịch phát sóng dạng dòng thời gian (hôm qua / hôm nay / ngày mai), chạm chương trình sắp tới để được nhắc; nhắc giờ phát / bóng lăn
  hiện hộp «Xem ngay».
- Sửa sau khi thử trên iPhone 6s Plus / iPad thật: tiêu đề bị cắt trên iPhone dọc (màn hẹp chỉ còn 2 nút tròn, «Tải lại» vào menu
  danh sách); thẻ radio trên iPhone ngang chạm thanh nút; **lối tắt truyền hình đóng được màn miễn trừ / «Ai đang xem?» và «Xem ngay»
  mở kênh đè lên màn chọn người xem (qua mặt mã PIN)** — nay chờ qua màn chặn; quay lại app sau ≥ 30 phút lúc đang nghe radio không
  hỏi lại người xem.
- **Sửa lỗi anh báo: thoát trình phát ra lưới kênh thì không vuốt về được đầu danh sách** (iPad + iPhone). Đo bằng cổng gỡ lỗi
  `/scrollinfo`: sau mỗi lần thoát, chiều cao nội dung phình (25 921 → 48 370 điểm), cuộn về đầu dừng ở 16 155. Hai nguyên nhân: ô
  kênh định danh bằng khoá kênh mà một kênh có thể nằm ở nhiều nhóm (trùng mã trong «Tất cả»), và lưới kênh là một phần tử con của
  LazyVStack (dựng lại thì SwiftUI ước sai cỡ các phần phía trên). Nay cả trang là một LazyVGrid (đầu trang = tiêu đề mục, nút trang
  = chân mục), ô định danh bằng số thứ tự. Thử đổi sang VStack thường thì iOS 15 sập (IDViewList) — đã bỏ.
- **Logo / icon mới** (đĩa kính tròn, vòng vàng, ấn 鵬 nổi khối — như icon bản Legacy): iPadOS tô đen phần trong suốt của icon trên
  màn hình chính (ảnh trong gói vẫn có kênh trong suốt — đã kiểm), nên icon iPad / iPhone đặt đĩa kính trên nền nâu tối (anh chọn);
  Mac giữ dáng tròn trong suốt; logo thanh bên trong suốt.
- iPad / iPhone: bản 1.20 (build 26) · tag `ios-v1.20` · `ConBangTV-iPad-1.20.ipa` 6 630 021 byte · SHA-256
  `99727e1af65b6806639185757c433ec299258e612fe998fface39df262b80cc1`
- Mac: bản 1.20 (build 26) · tag `mac-v1.20` · `ConBangTV-Mac-1.20.zip` 10 523 201 byte (Intel + Apple Silicon, ký ad-hoc) · SHA-256
  `2e37acf927b99008161c710a830dbb08d67688085c33a148e6b60916e7dc2207`
- commit `ab171e7` (tính năng: `661d2b2`, `26a41da`, `ab171e7`; logo `ccfa577`, `2b0bb00`; chữ `f0e16fd`; số bản `94b14e2`; không bản
  nào là "latest" — Latest là `v2.20`).
- Thử:
  - Bản thử trên iPhone 6s Plus (iOS 15.8.3) và iPad Pro 11″ M1 (iPadOS 15.4.1): danh sách 467 kênh + lịch 144 kênh nạp ~2 s; VTV1 /
    VTV2 720p, VTV3 1080p; radio BBC (MP3 · 56 kbps) và VOV1 (AAC · 256 kbps, tên chương trình); bảng kênh, lịch dọc / ngang, danh
    sách thể thao (nhãn ĐANG ĐÁ; luồng trận lúc đó bị máy chủ trả 403 — app báo «Trận đang đá nhưng kênh chưa phát hình», tự thử
    lại); danh sách Xtream 59 834 kênh trên iPhone ~12 s, app ~120 MB. Màn miễn trừ / «Ai đang xem?» chặn được lối tắt truyền hình.
  - Lỗi cuộn: bản sửa trên iPhone — xem rồi thoát 3 lần, chiều cao giữ ~19 300–20 600 điểm, cuộn xuống cuối / về đầu nhiều lần đều
    tới đúng; bản Mac thử cũng vậy (danh sách Xtream).
  - CI `ios.yml` (release=true, run 43983144202): test lõi, IPA, bản Mac đạt. Info.plist đúng (`vn.chimto.tv`, 1.20, build 26),
    không có cổng gỡ lỗi; bản Mac hai kiến trúc. IPA chính cài lên iPhone (`vn.chimto.tv` 1.19 → 1.20), app mở, không có báo cáo sập.
- Đăng lúc 07:07:53 (iPad) và 07:08:30 (Mac) bằng `release.ps1 ipad` / `release.ps1 mac`; link cố định `conbangtv_ios` /
  `conbangtv_macos` chép đè sang 1.20, khớp SHA-256.
  - **Tự cập nhật thật Mac 1.19 → 1.20** (bản trong `/Applications` của Mac mini): xoá mốc kiểm cập nhật, mở app → «Có bản mới 1.20 —
    Đang dùng 1.19» kèm «Có gì mới» → «Cài đặt ngay» 07:09:37 → mở lại ở 1.20 lúc 07:09:42.

## Windows 1.18 — 17/11/2026 (Truyền hình · Radio, logo mới)

- **Mục mới «Truyền hình · Radio»** như bản tivi: lõi Rust chung `linux/core/src/iptv.rs` (đọc M3U trong bộ nhớ, 353 nghìn kênh
  ~1,6 s; XMLTV; giờ trận; kênh lỗi; mạng của app lỗi mà không bật proxy thì thử mạng của máy) qua FFI `iptv.*`; trang
  `UI/TvPages.cs` (lưới kênh, nhóm, tìm tên / số, yêu thích, vừa xem, lịch phát sóng, cài đặt danh sách, thêm tệp .m3u), trình phát
  `Player/LiveView.cs` bằng mpv (Referer / User-Agent theo luồng, ICY cho radio). Phím: PgUp / PgDn đổi kênh, gõ số nhảy kênh.
- Luồng đứt giữa chừng thì nối lại, không báo lỗi thoáng qua / nhảy nguồn nhầm (`661d2b2`).
- Nhắc giờ phát chờ tới khi không có hộp thoại nào: hộp mới đóng hộp đang mở, trước đó nhắc giờ đóng được màn miễn trừ / «Ai đang
  xem?» (qua mặt mã PIN). Quay lại app sau ≥ 30 phút lúc đang xem truyền hình không hỏi lại người xem. Thẻ radio bỏ dòng ♪ chỉ lặp
  tên đài.
- **Logo mới:** `app.ico` (9 cỡ, xuất bằng `node design/render.mjs desktop d`) và logo thanh bên — đĩa kính tròn nền trong suốt.
- bản 1.18.0 · tag `win-v1.18` · `ConBangTV-Windows-1.18.exe` 120 332 421 byte · SHA-256
  `7b31ad5290661faab932e947c217c71a1d3dd8e06609a140c71b074341f7fe0e` · commit `94b14e2` (tính năng: `ebb892e`, `661d2b2`, `9aa2d22`,
  `f0e16fd`; logo `ccfa577`)
- Thử: `windowsuild.ps1` — test lõi Rust đạt (187 ở `conbang-core`), .NET 32 đạt. Bản thử (thư mục dữ liệu riêng): thêm danh sách
  467 kênh, VTV1 1280×720 (giải mã d3d11va, không rớt khung), radio BBC «MP3 · 56 kbps» không còn dòng lặp tên.
- Đăng lúc 06:14:32 bằng `release.ps1 windows`; link cố định `conbangtv_windows` = 1.18. **Tự cập nhật thật 1.17 → 1.18** (bản sao chạy
  riêng): hộp «Có bản mới» → «Cập nhật ngay» (UI Automation) 06:26:16 → cài 06:26:33 → «Mở bản 1.18 (--updated)» 06:26:35.

## Linux 1.4 — 17/11/2026 (Truyền hình · Radio, logo mới)

- **Mục mới «Truyền hình · Radio»** như bản Windows (lõi `cbtv::iptv`): `pages/tv.rs`, trình phát `live.rs` (mpv dưới lớp GTK
  Overlay, ICY cho radio). Phím: PgUp / PgDn đổi kênh, gõ số nhảy kênh.
- Luồng đứt giữa chừng thì nối lại không báo lỗi thoáng qua; nhắc giờ phát không vượt màn miễn trừ / «Ai đang xem?» (hộp mới huỷ hộp
  đang mở — trước đó có thể coi như bấm «Thoát»); quay lại sau ≥ 30 phút lúc đang xem truyền hình không hỏi lại người xem; thẻ radio bỏ
  dòng ♪ chỉ lặp tên đài.
- **Logo mới:** icon gói `.deb` (`linux/packaging/icons`) và logo trong app (`design/logo-512.png`) — đĩa kính tròn nền trong suốt.
- bản 1.4.0 · tag `linux-v1.4` · `ConBangTV-Linux-1.4.deb` 6 149 828 byte · SHA-256
  `871796eed330deb8fa8932309e1606f2e480838271bea16071b1202cc6c6cf55` · commit `94b14e2` (tính năng: `5472dd1`, `661d2b2`, `9aa2d22`,
  `f0e16fd`; logo `ccfa577`) · dựng trong container Ubuntu 22.04 (glibc ≤ 2.34)
- Thử: bản thử trên máy ảo Ubuntu 26.04 — VTV1 1280×720, radio BBC MP3 56 kbps, không còn dòng lặp tên; `dpkg-deb -f` đúng gói
  `conbangtv` 1.4.0 amd64.
- Đăng lúc 06:15:03 bằng `release.ps1 linux`; link cố định `conbangtv_linux` = 1.4. **Tự cập nhật thật 1.3 → 1.4** trên máy ảo: hộp
  «Có bản mới: Côn Bằng TV 1.4» → «Cập nhật ngay» → `pkexec apt-get install` → mở lại `--updated`, gói 1.4.0 lúc 06:25:31.

## 2.20 — 17/11/2026 (Truyền hình · Radio, logo mới)

- **Mục mới «Truyền hình · Radio»** (port từ bản Legacy — `CBIPTV.m`, `CBLivePlayerViewController.m`…): xem kênh truyền hình / nghe
  radio từ **danh sách M3U / M3U8 người xem tự thêm** — app không kèm sẵn kênh. Thêm bằng địa chỉ, gửi tệp / dán nội dung từ điện
  thoại / máy tính cùng Wi-Fi (trang tải lên ở cổng 8795, mã QR tự vẽ `Qr.kt`) hoặc chọn tệp .m3u. `Iptv.kt` đọc danh sách vào SQLite
  (mỗi lần nhập một tệp, chèn theo lô; danh sách không đổi thì bỏ qua đọc lại), `IptvParse.kt` (thuần, có test JVM).
- Đọc nhóm, logo, số kênh, `#EXTVLCOPT` Referer / User-Agent, `địa-chỉ|Referer=…`; gộp kênh trùng tên trong nhóm thành nhiều nguồn;
  nhận đài radio, kênh thiếu nhi (hồ sơ trẻ em chỉ thấy kênh thiếu nhi), phim; bỏ mã nước đầu tên («|UK| SPORT 1» → «SPORT 1»).
- **Lịch phát sóng (XMLTV)**, màn lịch `TvGuideActivity`, **nhắc giờ phát** (hộp trong app + thông báo) và **nhắc lúc bóng lăn** cho kênh
  trận đấu (giờ trận đọc từ tên kênh, nhãn ĐANG ĐÁ / SẮP ĐÁ). Nhắc giờ không hiện đè lên màn «Ai đang xem?» / miễn trừ trách nhiệm.
- Yêu thích, «Kênh vừa xem» (cả một hàng ở trang chủ — bấm là mở kênh; kênh của danh sách khác thì đổi sang danh sách đó), tìm tên /
  số kênh.
- **Trình phát truyền hình** (`LiveActivity`, Media3 ExoPlayer 1.5.1 — trình phát WebView chỉ phát được HLS): ▲ ▼ / CH+ CH− đổi kênh,
  phím số nhảy kênh, bảng kênh; nguồn lỗi tự chuyển nguồn sau, cả kênh lỗi thì tải lại danh sách một lần; trận chưa phát hình thì thử
  lại 20 giây một lần; tạm dừng → CHẬM, «Về trực tiếp»; radio hiện logo đài + tên bài (ICY). Thẻ radio bỏ dòng ♪ chỉ lặp tên đài.
- **Logo / biểu tượng mới:** đĩa kính tròn, vòng vàng và ấn 鵬 nổi khối (phương án D trong `design/logo.html`, như icon bản Legacy);
  icon Android 8+ nền trong suốt (lớp nền = màu trong suốt); banner tivi đổi dấu hình.
- bản 2.20 · versionCode 44 · tag `v2.20` (Latest) · commit `94b14e2` (tính năng: `757f95f`, `b7c189a`, `fff278d`, `9aa2d22`, `f0e16fd`;
  logo `ccfa577`) · `ConBangTV-2.20.apk` = `ConBangTV.apk` = `ChimToTV.apk` 151 557 659 byte · SHA-256
  `7643470fa101831a60d512bd585a7cbd1f34411ad438a550267cb4f30bd92251` · chứng chỉ `949ef2b5…177b`
- Thử: test đơn vị đạt. Bản thử trên TX3: danh sách 467 kênh (làm mới, lịch 144 kênh), VTV1 1280×720 (vẽ 107 khung, rớt 0), bấm số 438 →
  radio BBC (MP3); Xtream 63 nghìn kênh ~42 s (hôm trước). Icon tròn trên điện thoại S10e (Android 14), banner mới trên launcher TX3. Bản
  chính chạy thử trên TX3 (màn chính, mục Truyền hình mở được) rồi hạ về 2.19.
- Đăng lúc 06:13:22 bằng `release.ps1 tv` (Latest = `v2.20`, ba tệp cùng SHA-256, hai link cố định tải về đúng 2.20). **Tự cập nhật thật
  2.19 → 2.20 trên TX3**: Cài đặt → «Kiểm tra cập nhật» → «Có bản mới 2.20 (đang dùng 2.19)» (mục «Có gì mới» 421 ký tự, hiện đủ) →
  «Cài đặt ngay» → màn cài của hệ thống → **2.20 lúc 06:22:43**.

## iPad / iPhone 1.19 và Mac 1.19 — 15/11/2026 (dexter.pw, phụ đề chọn được)

- **Trang mới dexter.pw — «Phim Âu Mỹ 4K (dexter.pw)»** (chọn trong «Đổi trang phim»): khoảng 6 400 phim lẻ (nhiều phim 4K) và hơn
  1 000 phim bộ; đọc API JSON của trang (`Core/Dexter.swift`, mã phim là mã TMDB), menu Phim mới & nổi bật / Phim bộ / **Thể loại…** /
  **Thập niên…**, tìm kiếm chung. Tên phim và giới thiệu là tiếng Anh.
- **Phụ đề tiếng Việt từ OpenSubtitles / Subsource** (kho phụ đề của trang): Menu → «Phụ đề» để đổi bản hoặc tắt; «Chữ sớm hơn / Chữ
  muộn hơn 0,5 s» khi lệch tiếng; app nhớ lựa chọn theo người xem. Tệp lỗi / chưa có thì tự thử tệp sau; hết tiếng Việt thì tạm dùng
  tiếng Anh (có báo) rồi tự thử lại; phim bộ ít người xem có khi vài phút máy chủ của trang mới có phụ đề.
- **Phim bộ chia theo mùa:** hết mùa tự sang mùa sau; nút «Xem từ / Mới nhất» ghi rõ mùa; tập đặc biệt (mùa 0) gom riêng.
- Chất lượng: WKWebView không có MediaSource nên WebKit tự phát HLS và tự chọn chất lượng theo màn hình / mạng (nút «Chất lượng» của
  bản tivi / máy tính ẩn ở đây).
- **DNS mã hoá:** máy chủ trả 0.0.0.0 / 127.x / `::` (hồ sơ NextDNS chặn nhầm, vd máy chủ video của dexter.pw) thì hỏi máy chủ sau
  (Cloudflare → Google); NXDOMAIN vẫn là trả lời thật.
- **Link tự cập nhật và màn «Mời bạn bè» theo repo mới `conbangtv/ChimToTV`** (bản cũ vẫn chạy nhờ chuyển hướng của GitHub).
- iPad / iPhone: bản 1.19 (build 25) · tag `ios-v1.19` · `ConBangTV-iPad-1.19.ipa` 4 721 411 byte · SHA-256
  `62398c26e3dff0dfa8fde4093e86c06452de07a1e6578ca54a6f0f2b45536694`
- Mac: bản 1.19 (build 25) · tag `mac-v1.19` · `ConBangTV-Mac-1.19.zip` 8 260 488 byte (Intel + Apple Silicon, ký ad-hoc) · SHA-256
  `9823c81e1e78c947852a07a36a909b83662f6b0d7e350b5532eb28f24771da32`
- commit `b0cf6c0` (tính năng: `21d41c7`, `eda5c5d`, `c2747d2`, `500958b`; không bản nào là "latest" — Latest là `v2.19`).
- Thử:
  - CI `ios.yml` (release=true): test lõi, IPA, bản Mac đều đạt. Info.plist đúng (`vn.chimto.tv`, 1.19, build 25), có `hls.min.js`,
    không có cổng gỡ lỗi; bản Mac hai kiến trúc.
  - Trước đó bản Mac thử trên Mac mini: Coco + phụ đề tiếng Việt, Dexter 1/2 có tiếng, KKPhim vẫn phát; 167 test lõi đạt.
  - IPA bản chính cài lên iPad bằng TrollStore (`trollstorehelper` trả 0, `vn.chimto.tv` 1.18 → 1.19). **Chưa mở giao diện trên iPad /
    iPhone thật** (màn iPad tắt; iPhone không trả lời mạng lúc phát hành).
- Đăng lúc 20:06:50 (iPad) và 20:07:11 (Mac) bằng `release.ps1 ipad` / `release.ps1 mac`; link cố định `conbangtv_ios` /
  `conbangtv_macos` chép đè sang 1.19, khớp SHA-256 (`-Verify` đạt).
  - **Tự cập nhật thật Mac 1.18 → 1.19** (bản trong `/Applications` của Mac mini, mang link API cũ `Circling5714/…` → 301): hộp «Có bản
    mới 1.19 — Đang dùng 1.18» kèm «Có gì mới» → «Cài đặt ngay» 20:08:21 → app mở lại ở 1.19 trước 20:08:57.
  - «Có gì mới» trên các release 15/11 rút còn dưới 600 ký tự sau khi đăng (hộp «Có bản mới» chỉ hiện ~600 ký tự, bản đầu bị cắt giữa chữ).

## Windows 1.17 — 15/11/2026 (dexter.pw, phụ đề chọn được, chọn chất lượng)

- **Trang mới dexter.pw — «Phim Âu Mỹ 4K (dexter.pw)»**: như bản iPad 1.19 (lõi Rust chung `linux/core/src/dexter.rs`, FFI
  `dexter.subtitlesFrom`). Phim của trang này phát bằng trang HLS trong WebView2 (hls.js bản đầy đủ `hls.min.js`, cần cho máy chủ 20 tách
  âm thanh riêng) để có phụ đề chọn được; các trang khác vẫn dùng trình phát mpv như 1.16.
- **Phụ đề tiếng Việt** (OpenSubtitles / Subsource): «Phụ đề» + «Chữ sớm / muộn hơn 0,5 s» trong bảng điều khiển (phím M); nhớ theo
  người xem.
- **Nút «Chất lượng»:** Tự động / 4K / 1080p / 720p / 480p (phim có bản nào hiện bản đó; đổi giữa chừng được; bản 4K khung rộng
  3840×1600 tính là 4K). Tự động: hls.js chọn theo cỡ cửa sổ và tự hạ khi rớt khung hình.
- Phim bộ chia mùa, tự sang mùa sau; «Thập niên…» trong menu.
- DNS mã hoá: câu trả lời bị chặn (0.0.0.0 / 127.x) thì hỏi máy chủ sau (lõi Rust).
- Link tự cập nhật + màn mời bạn bè theo `conbangtv/ChimToTV`.
- bản 1.17.0 · tag `win-v1.17` · `ConBangTV-Windows-1.17.exe` 120 002 320 byte · SHA-256
  `e59ef13f4d8fa569dadb618056f24843ae40afb942ce97fb10487642df317220` · commit `b0cf6c0` (tính năng: `3580658`, `60f36ff`, `c2747d2`,
  `500958b`)
- Thử: `windows\build.ps1` — test lõi Rust 219 đạt, .NET 32 đạt. Gói chính chạy với thư mục dữ liệu riêng: «Đổi trang phim» có dexter.pw,
  tìm «Coco» → trang phim (105 phút, «Cùng bộ», «Cùng thể loại») → phát (server 21, 16 phụ đề), phụ đề tiếng Việt hiện trên hình,
  «Chất lượng: Tự động (4K)»; «Kiểm tra cập nhật» báo đúng.
- Đăng lúc 20:03:38 bằng `release.ps1 windows` (không phải Latest; link cố định `conbangtv_windows` → 1.17, `-Verify` đạt).
  **Tự cập nhật thật 1.16 → 1.17** (bản 1.16 đã phát hành chép riêng, mang link API cũ → 301): lúc mở tự thấy «Có bản mới: Côn Bằng TV
  1.17» (20:04:43); thử cả đường Cài đặt → «Kiểm tra cập nhật» → «Cập nhật ngay» 20:05:23 → tải 120 MB trong ~15 s → mở lại bằng
  `--updated` 20:05:40 ở 1.17; tệp sau cập nhật cùng SHA-256 với release, dữ liệu giữ nguyên, `.old` đã dọn.
- Lưu ý cho sau: bộ kiểm tra cập nhật chỉ đọc 30 release mới nhất — nếu giữa hai bản Windows có hơn ~30 bản nền tảng khác thì app sẽ
  tưởng mình là bản mới nhất.

## Linux 1.3 — 15/11/2026 (dexter.pw, phụ đề chọn được, chọn chất lượng)

- Giống Windows 1.17: trang **dexter.pw**, **phụ đề tiếng Việt** chọn được + chỉnh sớm / muộn, nút **«Chất lượng»**, phim bộ chia mùa,
  «Thập niên…», DNS mã hoá bỏ qua câu trả lời bị chặn, link tự cập nhật theo `conbangtv/ChimToTV`. Phim của dexter.pw phát bằng trang
  HLS trong WebKitGTK (để có phụ đề); các trang khác vẫn dùng trình phát mpv như 1.2.
- bản 1.3.0 · tag `linux-v1.3` · `ConBangTV-Linux-1.3.deb` 5 569 208 byte · SHA-256
  `16e0470792a957315e0baf98717b3a35f6b2bc35ff12a311444591b22686a0fd` · commit `b0cf6c0` (tính năng: `3580658`, `6193d87`, `c2747d2`,
  `500958b`)
- Thử: dựng trong container Ubuntu 22.04 (`release-build.sh`), tệp chạy cần tối đa `GLIBC_2.34`, không có cổng gỡ lỗi, năm link GitHub
  trong tệp chạy đều là `conbangtv/ChimToTV`; `dpkg-deb -f`: `conbangtv` 1.3.0 amd64. Cài bằng `apt-get install ./…deb` trên máy Ubuntu
  thử, mở từ biểu tượng: trang chủ, «Giới thiệu» ghi bản 1.3, «Kiểm tra cập nhật» báo đúng.
- Đăng lúc 20:01:24 bằng `release.ps1 linux` (không phải Latest; link cố định `conbangtv_linux` → 1.3, `-Verify` đạt).
  **Tự cập nhật thật 1.2 → 1.3** trên máy Ubuntu thử (bản 1.2 cài bằng .deb, mang link API cũ → 301): mở app 20:02:04 → hộp «Có bản mới:
  Côn Bằng TV 1.3» kèm «Có gì mới» → «Cập nhật ngay» 20:02:43 → tải .deb xong 20:02:44 → `pkexec apt-get install -y` mã 0 → app mở lại ở
  1.3.0 lúc 20:02:48 (tải về và cài trong ~5 giây); `.deb` tạm đã xoá.
- Còn từ trước (không do 1.3): thỉnh thoảng lần đọc đầu tiên trang chủ hoathinh3d.pw qua WebKitGTK ẩn báo «Load failed» (1.2 cũng
  vậy) → app dùng danh sách lần trước; phim MP4 của CLB Phim Xưa trong trình phát WebView dừng sớm (mpv mặc định vẫn phát).

## 2.19 — 15/11/2026 (dexter.pw, phụ đề chọn được, chọn chất lượng, sửa bàn phím ô tìm kiếm)

- **Trang mới dexter.pw — «Phim Âu Mỹ 4K (dexter.pw)»** (chọn trong «Đổi trang phim»): khoảng 6 400 phim lẻ (nhiều phim 4K) và hơn
  1 000 phim bộ (Dexter, The Mentalist, Modern Family…). App đọc API JSON của trang (`Dexter.kt`, mã phim là mã TMDB): trang chủ,
  Phim mới & nổi bật, Phim bộ, **Thể loại…**, **Thập niên…**, tìm kiếm chung. Luồng HLS fMP4 không mã hoá, không quảng cáo; app phát
  bằng **hls.js bản đầy đủ** (`assets/hls.min.js` 1.5.20 — cần cho máy chủ 20 tách âm thanh riêng; các trang khác vẫn dùng bản nhẹ).
- **Phụ đề tiếng Việt từ kho OpenSubtitles / Subsource** của trang: ▼ → «Phụ đề» để đổi bản phụ đề hoặc tắt; «Chữ sớm hơn / Chữ muộn
  hơn 0,5 s» khi lệch tiếng; nhớ theo người xem. Tệp lỗi / chưa có thì tự thử tệp sau; hết tiếng Việt thì tạm dùng tiếng Anh (có báo)
  rồi tự thử lại (3 lần, cách 45 giây); danh sách phụ đề phim bộ «đang chuẩn bị» thì hỏi lại mỗi 4 giây tới 3 phút.
- **▼ → «Chất lượng»:** Tự động / 4K / 1080p / 720p / 480p (phim có bản nào hiện bản đó; đổi giữa chừng được; nhớ theo người xem; bản 4K
  khung rộng 3840×1600 tính là 4K). Tự động: hls.js chọn theo cỡ màn hình và tự hạ khi rớt khung hình (TX3 giải mã 1080p rớt ~37% khung
  → tự xuống 720p / 480p). 4K cần tivi đủ mạnh.
- **Phim bộ chia theo mùa:** hết mùa tự sang mùa sau; nút «Xem từ / Mới nhất» ghi rõ mùa; tập đặc biệt (mùa 0) gom riêng.
- **Sửa lỗi bàn phím không hiện khi chọn «Tìm kiếm» trong menu** (anh gặp trên Skyworth; TX3 cũng bị): tiêu điểm vào ô trong lúc menu
  xử lý phím OK nên bàn phím không được báo — nay app đặt lại tiêu điểm rồi gọi bàn phím, thử lại tới 10 lần (`View.openKeyboard`).
- **DNS mã hoá:** máy chủ trả 0.0.0.0 / 127.x / `::` (hồ sơ NextDNS chặn nhầm máy chủ video `v.lumenbox.top`,
  `v.cloudsstrservices.top` của dexter.pw — anh đã thêm vào danh sách cho phép) thì hỏi máy chủ sau (Cloudflare → Google); NXDOMAIN
  vẫn là trả lời thật.
- **Link tự cập nhật (`UPDATE_URL`) và màn «Mời bạn bè cài app» theo repo mới `conbangtv/ChimToTV`**; mã QR vẽ sẵn của màn đó dựng lại
  theo link mới.
- bản 2.19 · versionCode 43 · tag `v2.19` (Latest) · commit `b0cf6c0` (tính năng: `4268009`, `eda5c5d`, `c2747d2`, `500958b`) ·
  `ConBangTV-2.19.apk` = `ConBangTV.apk` = `ChimToTV.apk` 150 273 275 byte · SHA-256
  `8649f6e3b5debfa1c102837b8a7438ee8a36cb9b131b7b1f942100c690d8fe3f` · chứng chỉ `949ef2b5…177b`
- Thử: 174 test (2 bỏ qua) đạt. Bản debug trên TX3: Coco 1080p + phụ đề tiếng Việt khớp hình, đổi bản, chỉnh ±; Fight Club; Dexter
  1/1 có tiếng; phim bộ 8 mùa; tìm «coco»; 4K → 2160p và đổi chất lượng giữa chừng; Spider-Man: Brand New Day 3840×1600 sau khi sửa DNS;
  KKPhim vẫn phát. Bàn phím: TX3 + Skyworth (bản 2.18 tái hiện lỗi, bản sửa hiện Gboard).
- Đăng lúc 19:59:24 bằng `release.ps1 tv` (Latest = `v2.19`, ba tệp cùng SHA-256, hai link cố định tải về đúng 2.19; `-Verify` đạt).
  **Tự cập nhật thật 2.18 → 2.19 trên TX3** (bản 2.18 mang link API cũ `Circling5714/…` → 301): Cài đặt → «Kiểm tra cập nhật» 20:01 →
  «Có bản mới 2.19 (đang dùng 2.18)» → tải 150 MB → «Cài đặt ngay» → màn cài của hệ thống → **2.19 lúc 20:02:52**, mở lại màn chính.
- Mục «Có gì mới» rút còn 388 ký tự sau khi đăng (hộp cập nhật chỉ hiện ~600 ký tự).

## Chuyển repo sang tổ chức conbangtv — 15/11/2026 (không phải bản mới)

- Repo chuyển từ `Circling5714/ChimToTV` sang **`conbangtv/ChimToTV`** (cùng tổ chức với trang giới thiệu `conbangtv.github.io`).
  Release, tag, tệp và secret đi theo repo; tên repo, tên tệp, bốn tag link cố định, mã Downloader 9192365 giữ nguyên.
- **App đã cài không cần làm gì:** bộ tự cập nhật của TV ≤ 2.18, iPad / iPhone / Mac ≤ 1.18, Windows ≤ 1.16, Linux ≤ 1.2 vẫn hỏi
  đường dẫn cũ, GitHub chuyển hướng 301 sang chỗ mới. Link tải cũ đã gửi đi (APK, bốn link cố định, mã Downloader) cũng vậy.
- Đã kiểm ngay sau khi chuyển: OTA thật TV 2.16.1 → 2.18 trên TX3 (app mang link cũ, 15:25 → 15:37); sáu link cũ tải về đúng
  SHA-256; `release.ps1 <tv|windows|linux|ipad|mac> -Verify` đạt cả năm nền tảng với tên mới; trang giới thiệu đổi link (site 30a80f8).
- Link trong app đã đổi sang tên mới ở TV 2.19, iPad / iPhone / Mac 1.19, Windows 1.17, Linux 1.3 (cùng ngày, xem các mục trên).

## iPad / iPhone 1.18 và Mac 1.18 — 14/11/2026 (đọc trang bật chống bot)

- **hoathinh3d.pw:** trang vừa bật lớp chống bot của Cloudflare (403 «Just a moment…» với các bản Android / Windows / Linux). Đo ngày
  14/11/2026 trên iPhone (iOS 15.8) và Mac (macOS 14): URLSession của Apple **chưa bị chặn** (trang, API JSON, ảnh đều 200). Bản này
  thêm đường dự phòng: `Core/WebFetch.swift` nhận ra lớp chặn (`cf-mitigated: challenge` / «Just a moment…» / `_cf_chl_opt`), nhớ
  trang 12 giờ, rồi đọc hộ bằng **WKWebView ẩn** gắn ngoài màn hình (`Player/WebFetcher.swift`, cùng kho cookie / proxy / User-Agent
  với trình phát): mở trang chủ cho trình duyệt tự qua lớp chặn, đứng ở robots.txt, mỗi yêu cầu là `fetch()` trong trang
  (`web-fetch.js` dùng chung với Windows / Linux). Trang đòi bấm xác nhận / chặn hẳn WebKit thì báo lỗi (không giả, không giải hộ);
  5 phút không dùng thì đóng WebView. Cùng luật với lõi Rust (`web_fetch.rs`) và bản TV.
- **Lên bản mới thì bỏ tạm nghỉ của các nguồn trang chủ** (một lần, `Feed.clearRestOnUpgrade`).
- Hộp «Có bản mới» đọc đúng mục «Có gì mới» cả khi ghi chú phát hành lỡ lưu kèm BOM.
- Cổng gỡ lỗi (chỉ bản thử): `/web` — `?remember=<tên miền>` ép đường WebView, `?fetch=<url>` tải thử, `?forget=1`.
- iPad / iPhone: bản 1.18 (build 24) · tag `ios-v1.18` · `ConBangTV-iPad-1.18.ipa` 4 532 999 byte · SHA-256
  `8fe885adf3b9435dbebbff66ef16333fd68f3a1931354199fda432154efd3469`
- Mac: bản 1.18 (build 24) · tag `mac-v1.18` · `ConBangTV-Mac-1.18.zip` 8 009 607 byte (Intel + Apple Silicon, ký ad-hoc) · SHA-256
  `511ea87af18fa1c83b02ee3484dfbaf643bbbfea7dbe32522f5d8329c9007a60`
- commit `0f5b023` (không bản nào là "latest" — Latest vẫn là `v2.18`).
- Thử:
  - Test lõi 160/160.
  - Bản Mac thử trên Mac mini (Xcode 15.4), ép đường WebView: qua lớp chặn 3,3 giây, các yêu cầu sau 0,1–0,3 giây; ảnh PNG qua WebView
    khớp từng byte với đọc thẳng. Trang chủ, danh mục «Huyền Huyễn» + ảnh bìa, trang phim «Tru Tiên», phát tập 1 đều chạy.
  - IPA thử trên iPhone 6s Plus (iOS 15.8.3), cả đọc thẳng lẫn ép WebView (qua lớp chặn 2,3 giây): «Đang Chiếu» (API JSON) + ảnh
    bìa, tìm «tru tiên» mọi trang (hoathinh3d.pw 20 phim), trang phim.
  - Tệp chính: Info.plist đúng (`vn.chimto.tv`, 1.18, build 24), không có cổng gỡ lỗi; bản Mac `lipo` x86_64 + arm64,
    `codesign --verify` đạt. Lần dựng IPA bản chính đầu tiên trên CI hỏng không kèm thông báo lỗi (cùng commit vừa dựng qua) → chạy
    lại job thì qua.
- Đăng lúc 14:36:32 (iPad) và 14:36:57 (Mac) bằng `release.ps1 ipad` / `release.ps1 mac`; link cố định `conbangtv_ios` /
  `conbangtv_macos` chép đè sang 1.18, khớp SHA-256 (`-Verify` đạt).
  - **Tự cập nhật thật Mac 1.17 → 1.18** (bản trong `/Applications` của Mac mini): hộp «Có bản mới 1.18» hiện đúng mục «Có gì mới»
    → «Cài đặt ngay» → khoảng 25 giây sau app mở lại ở 1.18 (build 24), `codesign` đạt, không bị cách ly, trang chủ hiện đủ.
  - IPA bản chính cài bằng TrollStore lên iPhone: app mở, không có báo lỗi văng.

## Linux 1.2 — 14/11/2026 (đọc trang bật chống bot)

- **Xem lại được hoathinh3d.pw:** trang vừa bật lớp chống bot của Cloudflare (403 «Just a moment…» với mọi trình tải không phải
  trình duyệt). Lõi nhận ra lớp chặn và nhờ app đọc hộ bằng một **WebKitGTK ẩn** (`linux/app/src/webfetch.rs`, cửa sổ ngoài màn hình,
  cùng WebContext / cookie / cổng SOCKS nội bộ với trình phát): mở trang chủ cho trình duyệt tự qua lớp chặn (~1,6–1,8 giây), đứng ở
  robots.txt rồi mỗi yêu cầu là `fetch()` trong trang (`web-fetch.js`, dùng chung với bản Windows). Ảnh bìa đi qua lõi nên tự được đọc
  hộ. Phát phim bằng trình phát của trang được luôn (WebKitGTK của trình phát dùng chung cookie). Trang đòi bấm xác nhận / chặn hẳn
  WebKit thì báo lỗi; 5 phút không dùng thì đóng WebKit ẩn.
- **Lên bản mới thì bỏ tạm nghỉ của các nguồn trang chủ** (một lần): bản cũ lỗi liên tục với trang bị chặn → nguồn bị nghỉ tới 6 giờ;
  bản mới thử lại ngay (`feed::clear_rest_on_upgrade`, bản Windows cũng có từ bản sau).
- bản 1.2.0 · tag `linux-v1.2` (không phải "latest") · commit `bbc4d05` · `ConBangTV-Linux-1.2.deb` 5 461 388 byte · SHA-256
  `1416ac1c451ff5f8e3bd353bde0ba55782643af4d324e561614ab0e23b2c1da1` · glibc ≥ 2.34 (dựng trong Ubuntu 22.04)
- Thử: bản thử trên máy Ubuntu 26.04 (trang chủ, menu của trang, danh sách «Huyền Huyễn» + ảnh bìa, trang phim, tìm kiếm, phát tập 1);
  gói: `conbangtv` 1.2.0 amd64, không có cổng gỡ lỗi, cài bằng `apt-get install`, mở từ biểu tượng (`gio launch …vn.chimto.tv.desktop`).
  Lỗi tìm ra khi thử và đã sửa: `fetch()` chạy lúc tài liệu đang bị thay («Load failed» — chỉ báo sẵn sàng khi chính robots.txt nạp
  xong); bộ dò qua lớp chặn chạy hai lần; đăng ký đọc hộ sau khi dựng app làm menu của trang trống.
- Đăng lúc 13:44:43 bằng `release.ps1 linux` (Latest vẫn là `v2.18`; link cố định `conbangtv_linux` chép đè sang 1.2, khớp SHA-256).
  **Tự cập nhật thật 1.1 → 1.2** (máy Ubuntu thử): hộp «Có bản mới: Côn Bằng TV 1.2» → «Cập nhật ngay» 13:45:58 → `pkexec apt-get
  install` mã 0 (13:46:03) → mở lại 1.2.0 `--updated` 13:46:04 → WebKit qua lớp chặn 1,7 s, trang chủ hoathinh3d.pw 20 phim.

## 2.18 — 14/11/2026 (đọc trang bật chống bot bằng engine Firefox)

- **Xem lại được hoathinh3d.pw:** từ 14/11/2026 Cloudflare của trang chặn hẳn WebView của Android («Attention Required! … you have
  been blocked») và bắt thử thách mọi trình tải khác. App gói kèm **GeckoView 141** (engine của Firefox, MPL 2.0) và chỉ dùng nó cho
  trang bị chặn: lần đầu gặp, một phiên Gecko ẩn mở trang chủ cho trình duyệt tự qua lớp chặn (~6–12 giây trên TX3), rồi mọi yêu cầu
  (trang, API, ảnh bìa, POST) đi qua mạng của Gecko — chung cookie, vẫn theo proxy / Shadowsocks / DNS mã hoá / vượt chặn của app (qua
  cổng SOCKS nội bộ riêng). Trang bình thường vẫn như cũ (HttpURLConnection + WebView). Trang bị chặn được nhớ 12 giờ.
- Phát phim của trang đó: «Trình phát của trang» (WebView) không mở được trang tập nên app tự chọn server KKPhim / NguonC, không gợi ý
  lại «Trình phát của trang» khi phim giật.
- Màn «Mời bạn bè cài app» in link tải mới `…/releases/latest/download/ConBangTV.apk`.
- Bộ đọc «Có gì mới» bỏ BOM đầu ghi chú.
- **APK ~143 MB** (trước 1,25 MB): thư viện native của GeckoView cho armeabi-v7a + arm64-v8a (anh chọn gói cả 64-bit để máy chỉ chạy
  64-bit vẫn cài được). Gecko chỉ khởi động khi gặp trang bị chặn; lúc đó thêm ~150 MB RAM trong tiến trình app + ~75 MB tiến trình
  `crashhelper` (đã tắt tiến trình GPU, không giữ tiến trình trang sau khi qua lớp chặn). Tiến trình con của Gecko không khởi tạo app
  (`App.onCreate` chỉ ở tiến trình chính — WebView không cho hai tiến trình dùng chung thư mục dữ liệu).
- Đã thử: Firefox Focus (GeckoView) trên TX3 vào được trang trong khi WebView bị chặn hẳn; bản debug + bản release (R8: giữ lớp
  `org.mozilla.**` — mã native gọi qua JNI, lần đầu bản release văng SIGSEGV) trên TX3 (32-bit): trang chủ, danh sách «Đánh Giá Cao»
  (API wp-json), ảnh bìa, trang phim, phát tập qua KKPhim; S10e (Android 14, 64-bit): danh sách, trang phim, tìm kiếm. 4 test mới.
- **Google Play Protect** hiện chặn tệp 2.17 chính thức («Đã chặn ứng dụng có hại», thấy trên TX3 và S10e khi bật quét) trong khi tệp
  2.18 cài được và quét sạch — Google xét theo từng tệp, không nêu lý do; đã soạn khiếu nại để anh gửi.
- bản 2.18 · versionCode 42 · tag `v2.18` (Latest) · commit `6d86d14` · `ConBangTV-2.18.apk` = `ConBangTV.apk` = `ChimToTV.apk`
  150 133 290 byte · SHA-256 `794c177f2fae3530da90cdf8fabbc56a3dedc954ae9307d0ae2ef63b68231121`
- Đăng lúc 13:00:30 bằng `release.ps1 tv` (Latest = `v2.18`, ba tệp cùng SHA-256, hai link cố định tải về đúng 2.18; `-Verify` đạt).
  **Tự cập nhật thật 2.17 → 2.18 trên TX3:** Cài đặt → «Kiểm tra cập nhật» → hộp «Có bản mới 2.18» (chỉ mục «Có gì mới») → «Cài
  đặt ngay» 13:03:51 → tải xong 13:04:04 → màn cài của hệ thống (▼ → HỦY, ▶ → CÀI ĐẶT, OK) → cài xong 13:05:17; mở lại: Gecko qua lớp
  chặn 7,7 s, trang chủ hoathinh3d.pw 200. Play Protect (đang bật trên TX3) không chặn bản 2.18.

## Windows 1.16 — 14/11/2026 (lõi Rust, trình phát native, đọc trang bật chống bot)

- **Trình phát native (mpv)** như bản Linux 1.1 — luồng phim trực tiếp (KKPhim, VSMOV, CLB Phim Xưa, server dự phòng KKPhim…)
  phát bằng **libmpv** vẽ thẳng vào cửa sổ app, giải mã bằng card đồ hoạ (Direct3D 11). Trình phát nhúng của trang (JW Player) thì
  app thử lấy link luồng rồi chuyển sang mpv; luồng có mã hoá riêng của trang (yanhh3d / streamfree, NguonC) vẫn phát bằng trang.
  mpv lỗi / 20 giây chưa chạy → tự quay về trình phát web đúng chỗ đang xem. Cài đặt → «Trình phát (thử nghiệm)»: Native / Web.
  Đo khi xem KK 1080p: 1 tiến trình ~520 MB, CPU ~0,3% một nhân (trước: 8 tiến trình ~920 MB, 1,3–1,9%).
- **Xem lại được hoathinh3d.pw:** từ 14/11/2026 trang bật lớp chống bot của Cloudflare (403 «Just a moment…» với mọi trình tải
  không phải trình duyệt — cả trang, API, ảnh). App đọc hộ bằng một WebView2 ẩn: trình duyệt tự qua lớp chặn (~0,4–1 giây), rồi
  mỗi yêu cầu là `fetch()` ngay trong trang. Áp dụng cho mọi trang bật lớp chặn này; trang đòi bấm xác nhận thì báo lỗi.
- **Đồng bộ trong nhà:** máy tính tự đồng bộ 3 phút một lần khi app đang mở — tường lửa Windows chặn tivi gọi vào thì thay đổi
  trên tivi vẫn sang máy tính (chậm nhất 3 phút); màn Đồng bộ khuyên ghép từ phía máy tính.
- **Lõi app mới:** phần đọc trang, trang chủ tổng hợp, cài đặt, mạng (DNS mã hoá, vượt chặn, proxy / Shadowsocks), đồng bộ là
  **lõi Rust dùng chung với bản Linux** (`conbang_core.dll`), thay ~8 000 dòng lõi C#; giao diện giữ nguyên. Dữ liệu, mật khẩu proxy
  / khoá Shadowsocks (DPAPI) của bản cũ đọc được nguyên.
- Sửa thêm: kết nối DNS mã hoá cũ treo lại sau khi bật proxy (cả bản Linux, bản sau); trình phát nhúng không lấy được link thì
  không chờ mãi.
- Tệp lớn hơn (~114 MB) vì kèm libmpv — bản dựng LGPL của zhongfly/mpv-winbuild (mpv: LGPL 2.1+).
- bản 1.16.0 · tag `win-v1.16` (không phải "latest") · commit `d0531d4` · `ConBangTV-Windows-1.16.exe` 119 812 848 byte ·
  SHA-256 `6f99d472c96cef9a231e1a2f45d8259de9395bf06104427faec1b5d2936847e8`
- 211 test lõi + 27 test C#. Thử trước phát hành (bản Debug + tệp đóng gói): proxy SOCKS5 (sai / đúng mật khẩu, proxy tắt thì báo
  lỗi chứ không đi thẳng), Shadowsocks thật (chỉ đi qua máy chủ SS), vượt chặn Mức 1 / 2 với nhà mạng thật, đồng bộ với TV (ghép,
  gộp lịch sử, xoá, theo dõi; TV → máy tính qua lượt 3 phút), người xem / PIN / giới hạn giờ xem, hàng trang chủ tự thêm, phim liên
  quan, trình phát (tốc độ, tập trước / sau, đổi bản / server, cửa sổ nhỏ, toàn màn hình), hoathinh3d.pw (trang chủ, menu, API,
  lịch chiếu, trang phim, tìm kiếm, «Đo», ảnh bìa, phát tập). Tự cập nhật thật (GitHub giả trên máy) 1.15 → 1.16 và 1.16 → 1.17
  (tải qua lõi mới); tệp đóng gói không có cổng gỡ lỗi.
- Đăng lúc 11:08:27 bằng `release.ps1 windows` (`releases/latest` vẫn là bản TV v2.17; link cố định `conbangtv_windows` chép đè sang
  1.16, khớp SHA-256; `-Verify` đạt). **Tự cập nhật thật 1.15 → 1.16 qua GitHub** (bản chép riêng): bấm «Cập nhật ngay» 11:09:35 →
  tải xong, mở lại bản 1.16 lúc 11:09:54. Ghi chú phát hành lúc đầu có BOM ở đầu tệp nên hộp «Có bản mới» hiện cả mục «Lưu ý» — đã
  sửa ghi chú trên GitHub (`gh release edit`), các bản sau bộ đọc ghi chú tự bỏ BOM.

## Linux 1.1 — 14/11/2026 (trình phát native)

- **Trình phát native (thử nghiệm)** — luồng phim trực tiếp phát bằng **mpv** thay cho trang web: KKPhim (cả trang KKPhim, mephim,
  server KK của phimmois và server dự phòng KKPhim), VSMOV, CLB Phim Xưa (MP4). Đo trên máy ảo Ubuntu không card đồ hoạ, cùng tập
  KKPhim 2542×1080 toàn màn hình: WebView 120% CPU · 490 MB riêng → mpv **87% CPU · 333 MB**; VSMOV 66% CPU, 0 khung rơi; CLB Phim
  Xưa chạy sau 1 giây. Mpv chỉ tải từ **cổng phát nội bộ** của app (127.0.0.1, mã phiên ngẫu nhiên) nên vẫn đi qua DNS mã hoá / vượt
  chặn / proxy và vẫn làm những việc trang phát cũ làm: bỏ khối quảng cáo chèn giữa phim của KKPhim, bỏ phần ngụy trang ảnh PNG của
  VSMOV, bỏ phần ảnh trước phụ đề rời; nối các khúc 8 MB của CLB Phim Xưa thành một luồng liền. Mọi tính năng của trình phát giữ
  nguyên (xem tiếp, bỏ nhạc mở đầu, đổi server, tốc độ, lấp đầy, giới hạn giờ xem, tự sang tập, cửa sổ phim nhỏ).
- **Tự chuyển sang trình phát web** (cùng server, phát tiếp đúng chỗ đang xem) khi mpv không mở được luồng, mất luồng giữa chừng,
  hoặc quá 20 giây chưa chạy mà không báo lỗi; máy không có libmpv thì phát bằng trình phát web như trước.
- **yanhh3d, hoathinh3d, hhkungfu (streamfree) và NguonC vẫn phát bằng trình phát của trang:** luồng của các trang này được mã hoá
  riêng (streamfree: SAMPLE-AES với khoá gói kín; NguonC: cả danh sách phát mã hoá AES-GCM) — app không vượt lớp bảo vệ nội dung. App
  kiểm trước link luồng trong lúc trang vẫn mở nên không phải nạp lại trang.
- Chọn lại trình phát cũ: **Cài đặt → Trình phát (thử nghiệm) → Web**. Mặc định vẽ hình bằng phần mềm trên mọi máy (máy có card đồ hoạ
  vẫn giải mã bằng card khi được — hwdec auto-copy-safe); vẽ bằng OpenGL chỉ khi đặt `CONBANG_NATIVE_RENDER=gl` (chưa thử trên máy
  thật có card đồ hoạ).
- **Trả bộ nhớ khi thoát phim:** tắt hẳn tiến trình WebKit của trình phát (trước đây giữ thêm 200–300 MB sau khi xem bằng trình phát
  web) và trình phát mpv.
- Gói `.deb` thêm `Recommends: libmpv2 | libmpv1` — apt cài theo mặc định, cả lúc app tự cập nhật.
- bản 1.1.0 · gói `conbangtv` (amd64, cần glibc ≥ 2.34) · tag `linux-v1.1` (không phải "latest") · commit `a7b696d` (đăng 07:43:42) ·
  `ConBangTV-Linux-1.1.deb` 5 430 968 byte · SHA-256 `67843b98dd71d50c1470ab4fdeea288da104e0b7fea4daf02375e01c2a26aa4c` · dựng
  trong container Ubuntu 22.04 trên máy Ubuntu thử. Link cố định `conbangtv_linux` đã chép đè bản 1.1.
- 213 test lõi (210 đạt, 3 test cần mạng thật để riêng; 10 test mới cho cổng phát). Thử trên máy ảo Ubuntu 26.04: KKPhim, VSMOV, CLB
  Phim Xưa phát bằng mpv; streamfree / NguonC giữ trình phát của trang, không nạp lại; tua / dừng / phát bằng phím; cửa sổ phim nhỏ;
  ba đường tự chuyển sang trình phát web (giả lỗi lúc mở, lỗi giữa chừng, treo 20 giây); thoát phim trả bộ nhớ. Bản phát hành: cài
  bằng apt, mở từ biểu tượng app, phát CLB Phim Xưa bằng mpv (không có cổng gỡ lỗi). Sau khi đăng: tự cập nhật thật 1.0 → 1.1 — «Có
  bản mới: Côn Bằng TV 1.1», Cập nhật ngay 07:45:14, `pkexec apt-get install` mã 0, mở lại 1.1.0 lúc 07:45:18, tệp chạy đã cài trùng
  tệp phát hành; `releases/latest` vẫn `v2.17`; `-Verify` cả 5 nền tảng đạt. **Chưa thử: máy thật có card đồ hoạ, phụ đề rời (server
  HD).**

## Linux 1.0 — 13/11/2026 (bản Linux đầu tiên)

- **Côn Bằng TV cho máy tính Linux**: Ubuntu 22.04 trở lên, Linux Mint 21+, Debian 12+ (máy 64-bit x86). Viết mới bằng Rust + GTK3 +
  WebKitGTK, giao diện và tính năng như bản Windows: trang chủ tổng hợp nhiều nguồn (sửa nhanh hàng, Các hàng / Thêm hàng / Nguồn),
  phim liên quan, Điểm IMDb cao, tìm kiếm (một trang hoặc mọi trang), xem tiếp, theo dõi tập mới, thể loại / quốc gia, người xem +
  mã PIN + giới hạn giờ xem, đồng bộ trong nhà với TV / iPad / Windows / Mac, proxy SOCKS5 / Shadowsocks, vượt chặn nhà mạng (tự lên
  Mức 2), DNS mã hoá, chặn tên miền quảng cáo / theo dõi, mời bạn bè cài app. Dùng chuột, bàn phím hoặc remote (phím như bản Windows).
- **Trình phát** dùng chung trang phát của bản Android; **cửa sổ phim nhỏ** (nút «▣ Thu nhỏ» / phím W) — phim chạy tiếp trong lúc dùng
  các trang khác. Máy không có card đồ hoạ (máy ảo…) tự tắt dựng hình bằng OpenGL giả lập: phim 1080p 72% CPU thay vì 240%.
- **Font chữ Inter** (gần giống San Francisco của iOS nhất trong các font mở), nhúng sẵn trong app.
- **Tự cập nhật**: app tìm release `linux-v…` trên GitHub, tải `.deb`, kiểm (cỡ, SHA-256, tên gói, số bản, kiến trúc), cài bằng
  `pkexec apt-get install` (hệ thống hỏi mật khẩu máy) rồi mở lại.
- **Cài:** tải `ConBangTV-Linux-1.0.deb` (hoặc link luôn là bản mới nhất
  `https://github.com/Circling5714/ChimToTV/releases/download/conbangtv_linux/ConBangTV-Linux.deb`), bấm đúp để mở bằng App Center
  rồi Cài đặt, hoặc `sudo apt install ./ConBangTV-Linux-1.0.deb`. Máy có card Intel / AMD nên cài thêm `gstreamer1.0-vaapi`.
- bản 1.0.0 · gói `conbangtv` (amd64, cần glibc ≥ 2.34) · tag `linux-v1.0` (không phải "latest") · commit `a8e65ac` (đăng 21:29) ·
  `ConBangTV-Linux-1.0.deb` 5 371 044 byte · SHA-256 `3421bcb4ab74df1f6152e660ab3d8f1e96a49ad9e3d9ce3fecd94dd62b27ef28` · dựng
  trong container Ubuntu 22.04 trên máy Ubuntu thử. Link cố định `conbangtv_linux` tạo lúc 21:29, cùng tệp.
- 203 test lõi (200 đạt, 3 test cần mạng thật để riêng). Thử trên máy ảo Ubuntu 26.04 (GNOME 50 Wayland, không card đồ hoạ):
  trang chủ, chi tiết, tìm kiếm tiếng Việt, theo dõi, lịch sử, người xem + PIN + giờ đi ngủ + cho xem thêm, sửa hàng trang chủ, phát
  phim (streamfree JW Player, KKPhim HLS 1080p, 0 khung rơi), toàn màn hình, chuyển tập, nhạc mở đầu, cửa sổ phim nhỏ, Shadowsocks
  thật (thử kết nối, trang chủ và phát phim qua proxy), đồng bộ với tivi TX3 (TV 2.17) hai chiều kể cả khi xoá. Sau khi đăng: tự cập
  nhật thật từ bản giả 0.9 (cài bằng .deb) — thấy «Có bản mới 1.0», tải từ GitHub, `pkexec apt-get install`, mở lại 1.0 trong 4 giây,
  tệp chạy đã cài trùng tệp phát hành; `releases/latest` vẫn `v2.17`; tệp tải từ link công khai và link cố định khớp SHA-256.
  **Chưa thử trên máy thật có card đồ hoạ / phiên X11.**

## Mac 1.17 — 13/11/2026 (bản Mac đầu tiên)

- **Côn Bằng TV cho máy Mac**: macOS 12 trở lên, máy Intel và máy chip Apple (M1…). Là chính app iPad dựng bằng Mac Catalyst nên
  có đủ tính năng của bản iPad: trang chủ tổng hợp nhiều nguồn, người xem + mã PIN + hồ sơ trẻ em, theo dõi phim, Điểm IMDb, tìm
  cùng lúc mọi trang, đồng bộ trong nhà, proxy SOCKS5 / Shadowsocks, vượt chặn nhà mạng (tự lên Mức 2), DNS mã hoá. Dùng chuột, bàn
  di chuột, bàn phím (phím tắt như bản iPad). Thanh menu tiếng Việt; cửa sổ nhỏ nhất 900 × 600.
- **Tự cập nhật**: app tìm bản mới (release `mac-v…`) trên GitHub; bấm «Cài đặt ngay» thì app tự tải, kiểm, thay bản đang dùng rồi mở
  lại — cần để app trong thư mục Applications.
- **Cài:** tải `ConBangTV-Mac-1.17.zip` (hoặc link luôn là bản mới nhất
  `https://github.com/Circling5714/ChimToTV/releases/download/conbangtv_macos/ConBangTV-Mac.zip`), giải nén, kéo vào Applications.
  App không mua chứng chỉ của Apple nên lần đầu macOS chặn: OK → Cài đặt hệ thống → Quyền riêng tư & Bảo mật → **Vẫn mở** → nhập
  mật khẩu máy → **Mở** (chỉ một lần).
- Khác bản iPad: không có widget; mật khẩu proxy cất trong tệp riêng của tài khoản macOS thay cho Keychain.
- bản 1.17 (CFBundleVersion 23) · mã gói `vn.chimto.tv` · tag `mac-v1.17` (không phải "latest") · commit `c416cd5` (tính năng:
  `24cb97b`, `c416cd5`) · `ConBangTV-Mac-1.17.zip` 7 945 079 byte · SHA-256
  `e7726b03fbae6668d566ef961164ce09c31ddcb2cf5075c38f6e1b107ada7d7f` · Intel + Apple Silicon, ký ad-hoc · dựng trên GitHub (run
  `43382966442`, Xcode 16.4, SDK macOS 15.5). Link cố định `conbangtv_macos` tạo lúc 14:15, cùng tệp.
- 155 test lõi (thêm 3; đạt cả Xcode 15.4 trên Mac mini lẫn Xcode 16.4 trên GitHub). Thử trên Mac mini 2012 (Intel, macOS 14.4):
  bản thử — trang chủ, xem phim (11 server, tua / dừng / phát / đóng), DNS mã hoá, vượt chặn với máy soi Viettel thật (lần đầu 4,6 s
  rồi lên Mức 2, sau đó 0,5 s; HTTP thường 0,3 s), tự cập nhật (tải .zip, thay `.app`, tắt, mở bản mới), cất mật khẩu; bản chính —
  mở lần đầu đúng các bước Gatekeeper ở trên (tệp mang cờ «Safari đã tải về»), màn miễn trừ, trang chủ, không văng. Sau khi đăng: bản
  thử (giả đang 1.16) tự thấy «Có bản mới 1.17» từ GitHub thật; `releases/latest` vẫn `v2.17`; tệp tải từ link công khai và link
  cố định khớp SHA-256. **Chưa thử trên máy chip Apple**; tự cập nhật thật từ GitHub sẽ thử ở bản Mac sau (bản 1.17 để sẵn trong
  Applications của Mac mini).
- Máy thử 2012 chạy macOS nhờ OCLP nên video rớt khung hình (WindowServer ~480% CPU, app ~6%) — giới hạn của máy, không phải app.

## Tệp APK bản TV đổi sang tên mới — 13/11/2026

- Từ bản 2.17 tệp APK mang tên mới: **`ConBangTV-<phiên bản>.apk`**, link cố định mới
  `https://github.com/Circling5714/ChimToTV/releases/latest/download/ConBangTV.apk` (trang giới thiệu đã dùng link này).
- Tệp tên cũ **`ChimToTV.apk` vẫn có trong mọi bản TV** (bản sao giống hệt): mã Downloader **9192365** trỏ vào link tên cũ, và màn
  «Cài lên tivi» của các bản đã cài (TV 2.16–2.17, iPad 1.16–1.17, Windows 1.14–1.15) in link đó — cả hai link tải cùng một tệp.
- Release `v2.17` đã sửa sau khi đăng (10:33): thêm `ConBangTV-2.17.apk` + `ConBangTV.apk`, bỏ `ChimToTV-2.17.apk`, giữ
  `ChimToTV.apk` — ba tệp cùng SHA-256 `663dc083…1206`. Tự cập nhật không bị ảnh hưởng: mọi bản từ 1.3 lấy tệp `.apk` đầu tiên của
  bản mới nhất, không xét tên. Đã kiểm: hai link cố định tải về đúng bản 2.17, trang chuyển hướng của mã 9192365 vẫn trỏ link tên
  cũ.
- Trong app (có hiệu lực từ bản sau 2.17 / iPad 1.17 / Windows 1.15): màn «Cài lên tivi / mời bạn bè» in link tên mới.
  `build.ps1` dựng ra `dist\ConBangTV-<phiên bản>.apk`; `release.ps1 tv` đính kèm ba tệp và kiểm cả hai link.

## iPad 1.17 — 13/11/2026

- **Vượt chặn nhà mạng tự lên Mức 2:** đang ở Mức 1 mà một trang vẫn bị chặn (gửi gói bắt tay đã chia nhỏ mà trang im 4 giây
  hoặc cắt kết nối), app tự kết nối lại theo Mức 2 cho riêng trang đó và nhớ 7 ngày — không phải vào Cài đặt đổi mức. Chỉ lần đầu
  chậm khoảng 4 giây.
- **Vượt chặn chia nhỏ cả yêu cầu HTTP thường** (dòng `Host:`), không chỉ HTTPS.
- **DNS:** nhận ra thêm kiểu chặn bằng địa chỉ trang cảnh báo của nhà mạng (vd DNS Viettel trả 127.0.0.1; cùng một địa chỉ trả
  cho hai trang khác nhau) và tự hỏi lại qua DNS mã hoá. Chi tiết: README mục «Vượt chặn nhà mạng» và «DNS mã hoá».
- bản 1.17 (CFBundleVersion 23) · mã gói `vn.chimto.tv` · tag `ios-v1.17` (không phải "latest") · commit `c52e32f` (tính năng:
  `c474288`) · `ConBangTV-iPad-1.17.ipa` 4 501 255 byte · SHA-256
  `5ca703428bd7003d68be236438dcc2195f9b3f72fe2bc89264eab8ed7572746d`
- 153 test lõi trên macOS (thêm 5). IPA: Info.plist đúng (mã gói, tên, bản 1.17 / 23), không có cổng gỡ lỗi, danh sách thư viện
  nạp giống hệt 1.16.1. iPhone thật iOS 15.8.3, bản thử 139 (cùng mã nguồn): máy soi Viettel thật, giả «Mức 1 không lọt» —
  lần đầu 4,6 s rồi lọt ở Mức 2, lần sau 0,57 s; HTTP thường tới trang bị chặn 0,32 s; trình phát mở bình thường (đo trên bản thử
  137 cùng mã). Bản chính thức cài đè 1.16.1: mở được, không có báo cáo văng. Link cố định `conbangtv_ios` đã chép sang 1.17, tệp
  tải từ link công khai và link cố định khớp SHA-256; `releases/latest` vẫn là bản TV. Chưa thử trên iPad (máy thử không bật).

## Windows 1.15 — 13/11/2026

- **Vượt chặn nhà mạng tự lên Mức 2:** như bản TV 2.17 — Mức 1 mà trang im / cắt kết nối thì tự thử lại theo Mức 2 cho riêng
  trang đó và nhớ 7 ngày; áp dụng cho cả trang phim (WebView2) lẫn kết nối riêng của app (đọc danh sách phim, trình phát).
- **Vượt chặn chia nhỏ cả yêu cầu HTTP thường**, không chỉ HTTPS.
- **DNS:** nhận ra địa chỉ trang cảnh báo của nhà mạng, tự hỏi lại qua DNS mã hoá.
- bản 1.15.0 · tag `win-v1.15` (không phải "latest") · commit `c52e32f` (tính năng: `c474288`) · `ConBangTV-Windows-1.15.exe`
  72 930 621 byte · SHA-256 `3de526fb96dabe13e74f01fbc1c5e908e3041270a736d2df045966db4b1233c1`
- 135 test lõi (thêm 4). Lỗi tìm ra khi thử bản gỡ lỗi và đã sửa trước phát hành: SslStream của .NET 8 đọc 0 byte trước để chờ
  dữ liệu — bị coi là «trang im» nên kết nối nào cũng lên Mức 2; có test. Máy soi Viettel thật (bản gỡ lỗi): cổng nội bộ 4,3 s →
  0,3 s, HttpClient 4,2 s. Tệp đóng gói: mở bản 1.15, trang chủ đủ 5 nguồn, không có cổng gỡ lỗi. **Tự cập nhật thật 1.14 → 1.15**
  (bản chép riêng) lúc 10:21:56 → mở lại bản 1.15 lúc 10:22:09; hộp cập nhật hiện mục «Có gì mới». Link cố định
  `conbangtv_windows` chép đè sang 1.15 (lần chép đè thật đầu tiên — chỉ còn một tệp, khớp SHA-256); `releases/latest` vẫn là
  bản TV.

## 2.17 — 13/11/2026

- **Vượt chặn nhà mạng tự lên Mức 2:** đang ở Mức 1 mà một trang vẫn bị chặn (gửi gói bắt tay đã chia nhỏ mà trang im 4 giây
  hoặc cắt kết nối — máy soi đã ghép được các mẩu), app tự kết nối lại theo Mức 2 cho riêng trang đó và nhớ 7 ngày. Không phải
  vào Cài đặt đổi mức; chỉ lần đầu chậm khoảng 4 giây.
- **Vượt chặn chia nhỏ cả yêu cầu HTTP thường** (dòng `Host:`), không chỉ HTTPS — trang mở bằng `http://` cũng không còn treo.
- **DNS:** nhận ra thêm kiểu chặn bằng địa chỉ trang cảnh báo của nhà mạng (DNS Viettel trả 127.0.0.1 cho trang bị chặn; cùng một
  địa chỉ trả cho hai trang khác nhau) và tự hỏi lại qua DNS mã hoá — dùng khi bật Vượt chặn mà tắt DNS mã hoá.
- versionCode 41 · tag `v2.17` · commit `c52e32f` (tính năng: `c474288`) · `ChimToTV-2.17.apk` 1 250 817 byte · SHA-256
  `663dc0830a0550c8cb5cf1a75d839b78a47b5bd990bd0ed6b8100b7b06f21206` · kèm `ChimToTV.apk` (bản sao giống hệt, cho link cố định).
- 163 test (thêm 7). Máy soi Viettel thật, bản gỡ lỗi trên TV box (giả «Mức 1 không lọt»): lần đầu 4,4 s rồi lọt ở Mức 2, sau đó
  0,3 s; HTTP thường tới 3 trang bị chặn 0,3–0,6 s (gửi thẳng từ máy tính: treo 15 s); giả DNS trả địa chỉ cảnh báo cho 2 trang →
  đi qua DNS mã hoá; trình phát vẫn phát bình thường. Bản chính thức trên TV box: trang bị chặn (HTTPS và HTTP) qua cổng nội bộ
  0,3–0,7 s, trang chủ tải đủ, phát được phim. Đăng bằng `release.ps1`: Latest = `v2.17`, hai tệp `.apk` cùng SHA-256, link cố
  định của Downloader tải đúng bản 2.17. **Cập nhật tự động thật 2.16.1 → 2.17** trên TV box lúc 10:20.

## Link tải cố định cho bản Windows và iPad / iPhone — 13/11/2026

Như link APK của bản TV, hai link này **luôn tải bản mới nhất** và không bao giờ đổi (trang giới thiệu
<https://conbangtv.github.io> đã dùng):

- Windows: `https://github.com/Circling5714/ChimToTV/releases/download/conbangtv_windows/ConBangTV-Windows.exe`
- iPad / iPhone: `https://github.com/Circling5714/ChimToTV/releases/download/conbangtv_ios/ConBangTV-iPad.ipa`

Mỗi nền tảng có một release riêng với tag không đổi (`conbangtv_windows`, `conbangtv_ios`); mỗi lần phát hành `release.ps1` chép
đè tệp mới vào đó và ghi ở ghi chú của release đang là bản nào. Lúc tạo (07:47 và 07:50): Windows = bản 1.14, iPad / iPhone = bản
1.16.1; tệp tải qua hai link khớp SHA-256 với tệp của `win-v1.14` / `ios-v1.16.1`. Ba bộ tự cập nhật không bị ảnh hưởng: bản
Windows 1.13 chạy thử vẫn được mời lên đúng 1.14, bản iPhone 1.16.1 không bị mời cài gì, `releases/latest` vẫn là bản TV.

## iPad 1.16.1 — 13/11/2026

- **Sửa nhãn IMDb đè lên nhãn tập** trên thẻ phim (có từ 1.15): nhãn tập ở góc dưới trái và điểm IMDb ở góc dưới phải trước đây
  là hai lớp riêng, nên thẻ hẹp (iPhone cầm dọc, dải vuốt ngang) hoặc nhãn tập dài («Tập 189 mới») thì chồng lên nhau — rõ nhất ở
  trang «Điểm IMDb cao» và hàng «Có tập mới». Giờ đủ chỗ thì vẫn chung một hàng, không đủ thì nhãn IMDb lên hàng trên.
- **Màn «Cài lên tivi / mời bạn bè» trên iPhone cầm dọc** (lỗi của 1.16): nội dung không còn tràn sát hai mép màn hình — nút
  «Gửi hướng dẫn…» một hàng riêng, «Chép» / «Đóng» chia đôi hàng dưới; màn ngang và iPad như cũ.
- bản 1.16.1 (CFBundleVersion 22) · mã gói `vn.chimto.tv` · tag `ios-v1.16.1` (không phải "latest") · commit `3a3e862` (sửa:
  `9a972eb`, `9e706e7`) · `ConBangTV-iPad-1.16.1.ipa` 4 489 492 byte · SHA-256
  `fb6986e9e859eb5cd4e59a9f708f1c653b1ac155b1d0dc8f0bd1f40ae768eab4`
- 148 test lõi trên macOS (thêm 1). iPhone thật iOS 15.8.3 (414 pt), bản thử cùng mã nguồn: trang chủ (thẻ «Già Thiên» trước bị
  đè → nhãn IMDb nằm trên nhãn tập), «Điểm IMDb cao» (một hàng, vừa khít), Phim bộ xếp theo IMDb (hai hàng), màn «Cài lên tivi»
  dọc / ngang. Máy ảo iPhone SE 375 pt trên CI: các trang trên đúng, không đè, có lề. Bản chính thức cài lên iPhone (đè 1.16): mở
  được, không có báo cáo văng. Tệp tải từ link công khai và từ link cố định khớp SHA-256; `releases/latest` vẫn là bản TV. Chưa thử
  trên iPad (máy thử không bật).

## 2.16.1 — 13/11/2026

- **Sửa nhãn IMDb đè lên nhãn tập** trên thẻ phim (có từ 2.15): khi không đủ chỗ chung một hàng — điện thoại cầm dọc, thẻ hẹp ở
  dải vuốt ngang, nhãn tập dài như «Tập 108 mới» — nhãn IMDb nằm ở hàng trên, sát mép phải; nhãn tập quá dài thì cắt bằng dấu ….
  Đủ chỗ thì vẫn một hàng như cũ (nhãn tập trái, IMDb phải). Thấy rõ nhất ở trang «Điểm IMDb cao» và hàng «Có tập mới».
- versionCode 40 · tag `v2.16.1` · commit `3a3e862` (sửa: `9a972eb`) · `ChimToTV-2.16.1.apk` 1 248 537 byte · SHA-256
  `5eaa6c99e24f90d4191dcd0f0883cd6ca189aa98e59f15028b88f3d80166a82c` · kèm `ChimToTV.apk` (bản sao giống hệt, cho link cố định).
- 156 test (thêm 1). Thử trên TV box: bản gỡ lỗi ở chế độ TV («Điểm IMDb cao», Phim bộ xếp theo IMDb — vẫn một hàng) và giả điện
  thoại cầm dọc 3 cột (các trang trên — nhãn IMDb ở hàng trên, không đè); bản chính thức: «Điểm IMDb cao», trang chủ (thẻ có
  «Tập 108 mới» + IMDb xếp hai hàng). Đăng bằng `release.ps1`: Latest = `v2.16.1`, hai tệp `.apk` cùng SHA-256, link cố định của
  Downloader tải đúng bản 2.16.1, tệp tải từ link công khai khớp SHA-256. **Cập nhật tự động thật 2.16 → 2.16.1** trên TV box lúc
  07:47 (hộp cập nhật chỉ hiện mục «Có gì mới»). Chưa thử trên điện thoại Android thật. Bản Windows không dính lỗi này (nhãn tập ở
  góc trên trái) nên không có bản mới.

## iPad 1.16 — 13/11/2026

- **Cài lên tivi / mời bạn bè** — mục mới ở Cài đặt → Liên hệ («Cài lên tivi / mời bạn bè — mã Downloader 9192365»): màn hiện
  **mã Downloader `9192365`** thật to cùng ba bước cài bản TV lên tivi / box Android, không cần USB hay máy tính. Nút **«Gửi
  hướng dẫn…»** mở bảng chia sẻ của máy (Zalo, Tin nhắn, AirDrop…) với sẵn tin nhắn hướng dẫn — cùng nội dung với bản TV và
  Windows; **«Chép»** chép tin nhắn đó; «Đóng». Dùng được cả dọc lẫn ngang, iPad và iPhone. Không có mã QR.
- bản 1.16 (CFBundleVersion 21) · mã gói `vn.chimto.tv` · tag `ios-v1.16` (không phải "latest") · commit `c4472af` (tính năng:
  `2d96a65`) · `ConBangTV-iPad-1.16.ipa` 4 481 661 byte · SHA-256
  `22cc5039b80199b9e98c7475916bf5ea6f6ee7445c53293520e3d175579e9295`
- 147 test lõi trên macOS (thêm 2). Bản thử 117 (cùng mã nguồn app) trên iPhone iOS 15.8.3: Cài đặt → cuộn tới «Liên hệ» → bấm
  mục mới, màn hiện đúng khi cầm dọc và ngang; «Chép» đổi thành «Đã chép»; «Gửi hướng dẫn…» mở bảng chia sẻ có đúng tin nhắn;
  «Đóng» về lại Cài đặt. Bản chính thức cài lên iPhone (đè 1.15): mở được, không có báo cáo văng (dừng ở màn «Miễn trừ trách
  nhiệm» — bản chính thức trên máy thử chưa bấm «Đồng ý», nên chưa xem được màn mới trong chính bản này). Bản thử thấy bản mới
  trên GitHub và hiện hộp «Có bản mới 1.16» lúc 05:20:50. **Bản iPad đầu tiên đăng bằng `release.ps1`** (tự đặt
  `--latest=false`): `releases/latest` vẫn `v2.16`, tệp tải từ link công khai khớp SHA-256. Chưa thử trên iPad (máy thử không
  bật).
- Máy ảo iPhone trên CI lúc phát hành không mở được app: bộ máy ảo iOS 18.5 của máy chạy GitHub không có
  `/usr/lib/swift/libswiftWebKit.dylib` mà tệp chạy đặt mức tối thiểu iOS 15 lại cần. Chỉ máy ảo bị — tệp chạy trong IPA 1.16 nạp
  cùng danh sách thư viện, cùng SDK 18.5 với IPA 1.15, và chạy trên iPhone thật. (Sau phát hành đã sửa job chụp màn hình máy ảo:
  dựng riêng bản máy ảo với mức tối thiểu = đời SDK, qua màn miễn trừ lần đầu mở, dừng với lỗi khi app không trả lời.)
- **Lỗi đã biết (tìm ra sau phát hành, nhờ ảnh máy ảo iPhone SE):** trên iPhone cầm dọc màn rộng 375 pt (SE / 6s / 7 / 8 / X…),
  màn «Cài lên tivi» tràn sát hai mép màn hình vì ba nút xếp một hàng không đủ chỗ — vẫn bấm được; màn 320 pt hoặc cỡ chữ lớn thì
  bị cắt hai bên. iPhone 6s Plus (414 pt) dùng để thử không lộ ra. Đã sửa trong mã nguồn (nút «Gửi hướng dẫn…» một hàng riêng,
  «Chép» / «Đóng» chia đôi hàng dưới; thử trên máy ảo 375 pt và iPhone thật) — **đã sửa ở bản 1.16.1**.

## Windows 1.14 — 13/11/2026

- **Cài lên tivi / mời bạn bè** — mục mới ở Cài đặt → Giới thiệu («Cài lên tivi / mời bạn bè — mã Downloader 9192365»): trang
  hiện **mã Downloader `9192365`** thật to cùng ba bước cài bản TV lên tivi / box Android (cài app Downloader → nhập mã → Install),
  không cần USB. Ba nút: **«Chép hướng dẫn để gửi bạn bè»** (chép sẵn tin nhắn để dán vào Zalo, tin nhắn — cùng nội dung với nút
  «Gửi hướng dẫn…» của bản TV), **«Chép mã»**, **«Mở trang hướng dẫn»**. Không có mã QR (máy tính thì gửi thẳng tin nhắn).
- Ghi chú bản này trên trang Releases có thêm link trang giới thiệu của app: <https://conbangtv.github.io>.
- bản 1.14.0 · tag `win-v1.14` (không phải "latest") · commit `c4472af` (tính năng: `a8bd585`) · `ConBangTV-Windows-1.14.exe`
  72 924 136 byte · SHA-256 `9fdd8581683f6745cb048abb112c11e47f15d6400d5a06bd138fa504106e63e8`
- 131 test lõi (thêm 2: mã, link và tin nhắn gửi người quen đúng từng chữ như bản TV). Thử tệp đóng gói (điều khiển bằng UI
  Automation): Cài đặt → mục mới mở đúng trang, đủ chữ; «Chép mã» đưa `9192365` vào bộ nhớ tạm, «Chép hướng dẫn…» đưa đúng tin
  nhắn; không có cổng gỡ lỗi. **Bản Windows đầu tiên đăng bằng `release.ps1`** (tự đặt `--latest=false`): `releases/latest` vẫn
  `v2.16`, tệp tải từ link công khai khớp SHA-256. **Tự cập nhật thật 1.13 → 1.14** (bản chép riêng) lúc 04:40:31 — hộp «Có bản
  mới» hiện đúng mục «Có gì mới».

## 2.16 — 12/11/2026

- **Mời bạn bè cài app** — mục mới trong menu bên trái (ngay trên «Cài đặt») và trong Cài đặt → Liên hệ: màn hiện **mã Downloader
  `9192365`** thật to, ba bước cài (cài app Downloader trên tivi → nhập mã → Install) và mã QR mở trang hướng dẫn tiếng Việt, để
  người quen tự cài Côn Bằng TV lên tivi của họ mà không cần USB hay máy tính. Mã do aftv.news cấp, trỏ vào link cố định
  `…/releases/latest/download/ChimToTV.apk` (luôn là bản TV mới nhất).
- Dùng app trên điện thoại / máy tính bảng: màn này xếp dọc, có nút **«Gửi hướng dẫn…»** gửi cách cài qua Zalo, tin nhắn.
- Trang Releases: ghi chú mỗi bản TV từ nay có thêm mục «Cài mới trên Android TV» ghi mã Downloader (hộp cập nhật trên TV không
  đổi — vẫn chỉ hiện «Có gì mới»).
- versionCode 39 · tag `v2.16` · commit `2892584` (tính năng: `28abd2a`, `5249319`) · `ChimToTV-2.16.apk` 1 248 194 byte (lớn hơn
  2.15 4 KB — mã QR vẽ sẵn, không thêm thư viện) · SHA-256 `16a6c2f338a1ae5383f26f628a4bac94b0b33697dd166f4fe727c76be7974e79` ·
  kèm `ChimToTV.apk` (bản sao giống hệt, cho link cố định).
- 154 test (thêm 3: mã QR trong mã nguồn được dựng lại và đọc lại bằng ZXing — chỉ trong test). Thử trên TV box, bản gỡ lỗi và
  bản chính thức: vào màn từ menu bằng remote và từ Cài đặt, OK / Back đóng màn, mã QR trên ảnh chụp màn hình đọc ra đúng link
  hướng dẫn; giả điện thoại cầm dọc: bố cục dọc, nút «Gửi hướng dẫn…» mở bảng chọn app. **Bản đầu tiên đăng bằng `release.ps1`:**
  Latest = `v2.16`, hai tệp `.apk` cùng SHA-256, link cố định tải về đúng bản 2.16; API `releases/latest` trả `v2.16` + `.apk`, tệp
  tải từ link công khai khớp SHA-256. **Cập nhật tự động thật 2.15 → 2.16** trên TV box lúc 21:07:50 (hộp cập nhật chỉ hiện mục
  «Có gì mới»). Chưa thử: nhập mã bằng app Downloader thật trên một tivi (TV box thử không cài Downloader), bấm «Gửi hướng dẫn…»
  trên điện thoại thật.

## Windows 1.13 — 12/11/2026

- **Sửa «Vượt chặn nhà mạng» Mức 2 trên Windows:** trước đây chọn Mức 2 thì trang bị nhà mạng chặn lại không vào được, trong khi
  Mức 1 vẫn vào được. Windows gửi lại mọi byte chưa được xác nhận trong một gói, nên gói gửi lại chứa nguyên gói bắt tay (có tên
  trang) và máy soi của nhà mạng đọc được. Bản này gửi theo nhịp riêng cho Windows: byte đầu (TTL 1), ba byte kế mỗi byte một gói,
  chờ trang xác nhận rồi mới gửi phần còn lại như Mức 1 — không gói nào còn chứa tên trang nguyên vẹn. Mỗi kết nối mới ở Mức 2
  chậm thêm khoảng 0,1 giây.
- Mức 1 (mặc định) và DNS mã hoá không đổi. Bản TV không bị lỗi này (Android gửi lại đúng 1 byte — bắt gói trên TV box 12/11);
  iPad / iPhone chưa đo lại.
- bản 1.13.0 · tag `win-v1.13` (không phải "latest") · commit `3250e9b` (sửa: `821dcdf`) · `ConBangTV-Windows-1.13.exe`
  72 922 753 byte · SHA-256 `862b248946b67962678ccd6cfb45a8656000e1fa2ad5f07760693b9a713fc3c8`
- 129 test lõi (thêm 2). Thử với trang đang bị chặn theo tên trang: mã cũ ở Mức 2 bị chặn 2/2 lần; mã mới qua 4/4 lần, bắt gói ở
  đầu nhận thấy gói gửi lại chỉ có 4 byte đầu. Bản gỡ lỗi ở Mức 2: trang chủ của trang bị chặn tải được, trình phát (WebView2) tải
  được ảnh từ trang bị chặn, phim vẫn phát; đối chứng qua cổng nội bộ: Mức 1 qua, tắt thì treo rồi lỗi, Mức 2 qua. Tệp đóng gói
  chạy ở Mức 2 tải được trang bị chặn. Tệp tải từ link công khai khớp SHA-256; `releases/latest` vẫn `v2.15`. **Tự cập nhật thật
  1.12 → 1.13** (bản chép riêng) lúc 20:01:49.

## iPad 1.15 — 10/11/2026

- **Điểm IMDb** như TV 2.15: nhãn «IMDb 8.5» trên thẻ phim, dòng IMDb ở trang phim, mục menu «Điểm IMDb cao», nút «Mới cập nhật» /
  «Điểm IMDb cao» ở danh mục KKPhim.
- bản 1.15 (CFBundleVersion 20) · mã gói `vn.chimto.tv` · tag `ios-v1.15` (không phải "latest") · commit `8425261` ·
  `ConBangTV-iPad-1.15.ipa` 4 453 281 byte · SHA-256 `484d5ff393285221074af8d580744964ce7ba8124b6b37355f497b751e00bc7a`
- 145 test lõi trên macOS (thêm 5). Bản thử 109 trên iPhone: trang «Điểm IMDb cao», nút xếp trong «Phim bộ» KKPhim. Bản chính
  thức cài lên iPhone (đè 1.14): chạy. Tệp tải từ link công khai khớp SHA-256; `releases/latest` vẫn `v2.15`.

## Windows 1.12 — 10/11/2026

- **Điểm IMDb** như TV 2.15: nhãn «IMDb 8.5» trên thẻ phim, dòng IMDb ở trang phim, mục menu «Điểm IMDb cao», nút «Mới cập nhật» /
  «Điểm IMDb cao» ở danh mục KKPhim.
- bản 1.12.0 · tag `win-v1.12` (không phải "latest") · commit `8425261` · `ConBangTV-Windows-1.12.exe` 72 922 413 byte · SHA-256
  `2ec0af487d33914b343eb008ebf28aa55f10e364ed2bd8ef3f38a240cd3d6b5b`
- 127 test lõi (thêm 4). Thử bản gỡ lỗi: trang «Điểm IMDb cao», nút xếp trong «Phim bộ» KKPhim. Tệp tải từ link công khai khớp
  SHA-256. **Tự cập nhật thật 1.11 → 1.12** (bản chép riêng) lúc 19:25:08.

## 2.15 — 10/11/2026

- **Điểm IMDb** (lấy từ dữ liệu KKPhim — phimapi.com có sẵn điểm + số lượt chấm IMDb; không gọi IMDb):
  - Thẻ phim có nhãn vàng «IMDb 8.5» ở góc dưới bên phải (phim nguồn KKPhim, từ 100 lượt chấm); trang phim KKPhim ghi
    «IMDb 8.7 (448 nghìn lượt chấm)».
  - **Mục menu «Điểm IMDb cao»** (sau «Phim theo dõi»): hàng Phim lẻ, Phim bộ, Phim Hàn Quốc, Phim Âu Mỹ, Phim Trung Quốc, Hoạt
    hình, mỗi hàng 60 phim — xếp theo điểm có trọng số (như Top 250 của IMDb: phim ít lượt chấm bị kéo về điểm trung bình) trong
    vài trăm phim mới cập nhật mỗi nhóm (API KKPhim không sắp theo điểm, kho ≈ 26 nghìn phim). Hồ sơ trẻ em chỉ hàng Hoạt hình.
    Lần đầu mở ~20 giây trên TV box, sau đó lưu 12 giờ.
  - **Danh mục KKPhim**: nút «Mới cập nhật» / «Điểm IMDb cao» ở đầu mục — xếp ≈ 320 phim mới cập nhật của mục theo điểm IMDb.
- Màn miễn trừ trách nhiệm: bấm OK khi đang ở vùng chữ cũng là «Đồng ý» (trước đây mở app ở chế độ chạm thì phải ▼ tới nút).
- versionCode 38 · tag `v2.15` · commit `8425261` (tính năng: `9445879`, `f2539cd`) · `ChimToTV-2.15.apk` 1 244 099 byte · SHA-256
  `beaa11b18e345916467216f8ce778b4df0f6e212c29aede87a6071633cf1e96d`
- 151 test (thêm 5). Thử bản gỡ lỗi trên TV box: trang «Điểm IMDb cao», nút xếp trong «Phim bộ», dòng IMDb ở trang phim. API
  `releases/latest` trả `v2.15` + `.apk`, tệp tải từ link công khai khớp SHA-256. **Cập nhật tự động thật 2.14 → 2.15** trên TV box
  lúc 19:22:43, bản chính thức mở «Điểm IMDb cao» được.
- **Bổ sung 12/11/2026 — link tải cố định cho app Downloader:** release `v2.15` có thêm tệp `ChimToTV.apk`, bản sao giống hệt
  `ChimToTV-2.15.apk` (cùng 1 244 099 byte, cùng SHA-256), để
  `https://github.com/Circling5714/ChimToTV/releases/latest/download/ChimToTV.apk` luôn tải thẳng bản TV mới nhất. App không đổi.
  Từ nay mọi bản TV đăng bằng `release.ps1` đều kèm hai tệp. Thử lại cập nhật tự động với release hai tệp: TV box hạ về 2.14 →
  2.15 lúc 20:22:45.

## iPad 1.14 — 10/11/2026

- **Miễn trừ trách nhiệm & quyền riêng tư** như TV 2.14: lần đầu mở app (kể cả máy cập nhật từ bản cũ) hiện màn «Trước khi bắt
  đầu» phủ toàn màn — «Đồng ý» mới vào app, «Thoát» đưa app về màn hình chính (lần mở sau vẫn hỏi). Xem lại: Cài đặt → Liên hệ →
  «Miễn trừ trách nhiệm & quyền riêng tư».
- bản 1.14 (CFBundleVersion 19) · mã gói `vn.chimto.tv` · tag `ios-v1.14` (không phải "latest") · commit `f179d63` ·
  `ConBangTV-iPad-1.14.ipa` 4 418 891 byte · SHA-256 `5de3efdbc1693811c12aa64a2d81912657dcf0eafffc637dbc50e235cf7ab801`
- 140 test lõi trên macOS. Bản thử 105 trên iPhone: hiện màn «Trước khi bắt đầu», «Đồng ý» → trang chủ. Bản chính thức cài lên
  iPhone (đè 1.13): chạy. Tệp tải từ link công khai khớp SHA-256; `releases/latest` vẫn `v2.14`.

## Windows 1.11 — 10/11/2026

- **Miễn trừ trách nhiệm & quyền riêng tư** như TV 2.14: lần đầu mở app (kể cả máy cập nhật từ bản cũ) hiện hộp «Trước khi bắt
  đầu» — «Đồng ý» mới vào app, «Thoát» / Esc là đóng app; hỏi cập nhật và «Ai đang xem?» chờ sau khi đồng ý. Xem lại: Cài đặt →
  «Miễn trừ trách nhiệm & quyền riêng tư».
- bản 1.11.0 · tag `win-v1.11` (không phải "latest") · commit `f179d63` · `ConBangTV-Windows-1.11.exe` 72 915 844 byte · SHA-256
  `f4023adc65894cfa28ea7a0319c89a749babcb7abedaa64311345278bce5ab5f`
- 123 test lõi. Tệp tải từ link công khai khớp SHA-256. **Tự cập nhật thật 1.10 → 1.11** (bản chép riêng) lúc 16:55:50 — mở lại
  là hiện hộp «Trước khi bắt đầu».

## 2.14 — 10/11/2026

- **Miễn trừ trách nhiệm & quyền riêng tư:** lần đầu mở app (kể cả TV cập nhật từ bản cũ) hiện màn «Trước khi bắt đầu» với 9 mục
  (app chỉ là trình xem, bản quyền, trách nhiệm người dùng, trang bên thứ ba, tính năng mạng, dữ liệu và quyền riêng tư — DNS mã hoá
  không lưu nhật ký truy cập —, không bảo đảm, trẻ em, liên hệ). Chọn «Đồng ý» mới vào app; «Thoát» / Back là đóng app. Remote: ▲
  lên vùng chữ, ▲▼ cuộn, hết chữ ▼ về nút. Hỏi «Ai đang xem?» chờ sau khi đồng ý; đã đồng ý thì không hỏi lại (mỗi máy riêng).
- Xem lại: Cài đặt → Liên hệ → «Miễn trừ trách nhiệm & quyền riêng tư». README có mục cùng nội dung.
- versionCode 37 · tag `v2.14` · commit `f179d63` (tính năng: `a418a6c`) · `ChimToTV-2.14.apk` 1 239 190 byte · SHA-256
  `950a36576c4bfed95c11d63fecd12fb5b8d71a184af2b3cb14ff2f20c634d6d3`
- 146 test. Thử bản gỡ lỗi trên TV box (hiện trước «Ai đang xem?», cuộn bằng remote, mở lại không hỏi). API `releases/latest` trả
  `v2.14` + `.apk`, tệp tải từ link công khai khớp SHA-256. **Cập nhật tự động thật 2.13 → 2.14** trên TV box lúc 16:43:55 — mở
  lại là hiện màn «Trước khi bắt đầu», đồng ý xong vào app.
- Biết trước: mở app ở chế độ chạm (chuột bay / màn cảm ứng) thì lần bấm phím đầu chọn vùng chữ, OK ở đó chưa tính là «Đồng ý» —
  bấm ▼ tới nút. Đã sửa cho bản sau.

## iPad 1.13 — 10/11/2026

- **Proxy kiểu Shadowsocks (mã hoá)** như TV 2.13: Cài đặt → Proxy → «Kiểu: Shadowsocks (mã hoá)»; nút «Dán link ss:// từ bộ
  nhớ tạm» (hoặc dán vào ô máy chủ) tự điền máy chủ, cổng, kiểu mã hoá, khoá. Khoá lưu trong Keychain. «Kiểm tra proxy» báo IP ra
  ngoài; SS 2022 không trả lời thì xem giờ trên máy có lệch quá 30 giây không.
- bản 1.13 (CFBundleVersion 18) · mã gói `vn.chimto.tv` · tag `ios-v1.13` (không phải "latest") · commit `693ae51` ·
  `ConBangTV-iPad-1.13.ipa` 4 396 000 byte · SHA-256 `84a4f7265e9cc370c77e8a04c1640038b71db94ad5a7c31084b78b8054af78e2`
- 140 test lõi trên macOS (thêm 12). Bản thử trên iPhone: «Kiểm tra proxy» qua máy chủ 3X-UI thật ✓, phát phim KKPhim qua
  Shadowsocks. Bản chính thức cài lên iPhone (đè 1.12.1): chạy. Tệp tải từ link công khai khớp SHA-256; `releases/latest` vẫn `v2.13`.

## Windows 1.10 — 10/11/2026

- **Proxy kiểu Shadowsocks (mã hoá)** như TV 2.13: Cài đặt → Proxy → «Kiểu proxy: Shadowsocks (mã hoá)», ô dán link `ss://` tự
  điền, chọn kiểu mã hoá, khoá mã hoá bằng tài khoản Windows (DPAPI); «Thử kết nối» báo IP ra ngoài và độ trễ. Trang, ảnh và trình
  phát đều đi qua máy chủ Shadowsocks. Kiểu ChaCha20 chỉ có khi Windows hỗ trợ (Windows 10 22H2 không có — nút ghi «máy này không
  có»); kiểu AES (mặc định của 3X-UI) chạy mọi máy.
- bản 1.10.0 · tag `win-v1.10` (không phải "latest") · commit `693ae51` · `ConBangTV-Windows-1.10.exe` 72 913 038 byte · SHA-256
  `206226cfc63cb30c9a12de29b2d83e739016c5f2c5d4fbcf7ddd447e5ac9ff04`
- 123 test lõi (thêm 14). Thử bản gỡ lỗi qua máy chủ 3X-UI thật: «Thử kết nối» ✓, phim VSMOV (server HLS) phát đều 90 giây. Tệp tải từ link công khai khớp SHA-256. **Tự cập nhật thật 1.9 → 1.10** (bản chép riêng) lúc 15:00:04.

## 2.13 — 10/11/2026

- **Proxy kiểu Shadowsocks (mã hoá):** Cài đặt → Proxy → nút «Kiểu: SOCKS5 / Shadowsocks». Dán link `ss://` chép từ 3X-UI (nút
  Copy / QR của người dùng) vào ô máy chủ là app tự điền máy chủ, cổng, kiểu mã hoá, khoá. Nhà mạng chỉ thấy dữ liệu đã mã hoá —
  không đọc được tên trang hay mật khẩu như với SOCKS5.
  - Kiểu mã hoá: `2022-blake3-aes-256-gcm` / `2022-blake3-aes-128-gcm` (mặc định của 3X-UI, máy chủ nhiều người dùng),
    `2022-blake3-chacha20-poly1305`, `aes-256-gcm`, `aes-128-gcm`, `chacha20-ietf-poly1305` (ChaCha20 cần Android 9+). Qua TCP
    thường (3X-UI: Transmission = TCP, Security = None); link dùng ws / grpc / tls / plugin thì app báo chưa hỗ trợ.
  - «Kiểm tra proxy» báo IP ra ngoài và độ trễ; sai khoá / sai kiểu mã hoá, giờ trên TV lệch quá 30 giây (SS 2022 cần giờ đúng)
    thì báo rõ. Khoá lưu mã hoá bằng Android Keystore. SOCKS5 dùng như cũ — mỗi kiểu giữ máy chủ riêng; DNS của proxy / của thiết
    bị và «chỉ trang phim» dùng chung.
  - Lưu ý: TV box yếu xem «Trình phát VSMOV» qua Shadowsocks có thể giật (TV box thử rớt ~30% khung hình cả khi đi thẳng) — chọn
    server «HLS» (app tự gợi ý khi thấy giật).
- versionCode 36 · tag `v2.13` · commit `693ae51` (tính năng: `9307a07`, `cd04e6f`, `f206ece`, `0863a56`) · `ChimToTV-2.13.apk`
  1 234 912 byte · SHA-256 `b994f314e39b98abcd5e6d033071afcc1aa49b9ba2116d557761ca6f78234977`
- 146 test (thêm 14: BLAKE3 theo bộ số chuẩn, link 3X-UI / SIP002, máy chủ thử viết theo đặc tả). Thử với Xray chính thức (cả 6
  kiểu mã hoá) và máy chủ 3X-UI thật: TV box dán link → «Kiểm tra proxy» ✓ 0,3 s, trang chủ + phim KKPhim (HLS) phát mượt qua
  Shadowsocks. Bản chính thức thử trên TV box trước khi đăng, hạ về 2.12, **cập nhật tự động thật 2.12 → 2.13 lúc 14:53:54**.
  API `releases/latest` trả `v2.13` + `.apk`.

## iPad 1.12.1 — 10/11/2026

- **Sửa:** chạm / giữ một phim (nhất là phim VSMOV) có khi trúng phim bên cạnh. Ảnh bìa phóng cho đầy khung 2:3 (ảnh ngang của
  VSMOV rộng gấp mấy lần thẻ); phần thừa bị cắt khi vẽ nhưng vẫn là vùng chạm / vùng giữ, phủ lên thẻ kế bên. Giờ ảnh bìa nằm gọn
  trong khung, không nhận chạm; vùng chạm của thẻ đúng bằng khung thẻ. TV / Windows không bị lỗi này.
- bản 1.12.1 (CFBundleVersion 17) · mã gói `vn.chimto.tv` · tag `ios-v1.12.1` (không phải "latest") · commit `423e9f2` (sửa:
  `eae166c`) · `ConBangTV-iPad-1.12.1.ipa` 4 345 736 byte · SHA-256 `09f7a3abc398a188c9b898aff73de26b503d23f46341a8a6cb5169a04a3f7351`
- 128 test lõi trên macOS. Anh thử bằng tay trên iPhone (bản thử 96, danh sách VSMOV → Phim bộ): chạm / giữ đúng thẻ. Bản chính thức
  cài lên iPhone (đè 1.12): chạy, không văng. Tệp tải từ link công khai khớp SHA-256; `releases/latest` vẫn `v2.12`.

## iPad 1.12 — 10/11/2026

- **Phim liên quan ở trang phim** như TV 2.12: hàng «Cùng bộ» và «Cùng thể loại» (thẻ phim như trang chủ); phim lẻ ngay dưới nút
  «Xem phim», phim bộ ở cuối danh sách tập + nút «Phim liên quan ▼» cuộn thẳng xuống.
- bản 1.12 (CFBundleVersion 16) · mã gói `vn.chimto.tv` · tag `ios-v1.12` (không phải "latest") · commit `f4df900` ·
  `ConBangTV-iPad-1.12.ipa` 4 344 479 byte · SHA-256 `b0eba931829776a0e65719b1fd0688bcbc6e0e452552aa43802a3a03593373c5`
- 128 test lõi trên macOS (thêm 5). Bản thử 93 thử thật trên iPhone: phim bộ «Đấu Phá Thương Khung (Phần 5)» → nút «Phim liên
  quan ▼» → hàng «Cùng bộ «Đấu Phá Thương Khung»» và «Cùng thể loại · Hoạt hình Trung Quốc · Phiêu lưu»; phim lẻ «… Thức Tỉnh» hàng
  ngay dưới nút Xem. Bản chính thức đã cài lên iPhone (gói app ghi 1.12; máy đang khoá màn hình nên chưa mở lên được). Tệp tải từ
  link công khai khớp SHA-256; `releases/latest` vẫn `v2.12`.

## Windows 1.9 — 10/11/2026

- **Phim liên quan ở trang phim** như TV 2.12: hàng «Cùng bộ» (các phần / mùa khác, phim ngoại truyện) và «Cùng thể loại»; phim
  lẻ ngay dưới thông tin phim, phim bộ ở cuối danh sách tập + nút «▼ Phim liên quan» (chọn sẵn phim đầu).
- bản 1.9.0 · tag `win-v1.9` (không phải "latest") · commit `f4df900` · `ConBangTV-Windows-1.9.exe` 72 893 026 byte · SHA-256
  `858c4e7ba911c84c72ee67951f293373ca39facd90373134cd6b7f67de3d3ebf`
- 109 test lõi (thêm 5). Thử bản gỡ lỗi: trang «Đấu Phá Thương Khung (Phần 5)» → nút «▼ Phim liên quan» cuộn xuống hàng «Cùng bộ»,
  chọn phim đầu. Tệp tải từ link công khai khớp SHA-256. **Tự cập nhật thật 1.8.1 → 1.9** (bản chép riêng).

## 2.12 — 10/11/2026

- **Phim liên quan ở trang phim:**
  - **Cùng bộ** — các phần / mùa khác và phim ngoại truyện của cùng bộ, xếp theo số phần (tìm theo tên gốc trên trang của phim +
    KKPhim; bỏ phim trùng ở nguồn khác).
  - **Cùng thể loại** — KKPhim / VSMOV theo loại + nước + thể loại đặc trưng nhất; bỏ phim đang xem và các phần cùng bộ, phim
    chưa xem lên trước; hồ sơ trẻ em chỉ hoạt hình.
  - Phim lẻ: hàng nằm ngay dưới nút «Xem phim» (bỏ ô tập «Full» thừa). Phim bộ: ở cuối danh sách tập + nút «Liên quan ▼» nhảy
    xuống. Đang chọn trong hàng liên quan thì dòng phía trên lưới ghi tên hàng (màn TV thấp, tên hàng hay bị cuộn khuất). Tải sau
    khi trang phim đã hiện.
- Chưa có «cùng đạo diễn / diễn viên» — KKPhim, VSMOV, NguonC, ophim chỉ tìm được theo tên phim.
- versionCode 35 · tag `v2.12` · commit `f4df900` · `ChimToTV-2.12.apk` 1 222 867 byte · SHA-256
  `ae65639e09a94ff68917fda84178100602c3df6e4cbc1787eb04582d1138d3b5`
- 132 test (thêm 5). Thử bản gỡ lỗi trên TV box: phim bộ (nút «Liên quan ▼» → hàng «Cùng bộ», ▼ → «Cùng thể loại», ▲ về tập, nhãn
  đổi theo hàng), phim lẻ (hàng ngay dưới nút Xem). Bản chính thức thử trên TV box trước khi đăng (phim yanhh3d có nút + hàng «Cùng
  bộ»). API `releases/latest` trả `v2.12` + `.apk`, tệp tải từ link công khai khớp SHA-256. **Cập nhật tự động thật 2.11.1 → 2.12**
  trên TV box.

## iPad 1.11.1 — 09/11/2026

- **DNS mã hoá nhanh hơn** như TV 2.11.1: NextDNS dùng máy chủ gần nhất (Hà Nội) thay cho máy chủ Hồng Kông, giữ kết nối tới
  máy chủ DNS để dùng lại (Network.framework, tối đa 90 giây).
- bản 1.11.1 (CFBundleVersion 15) · mã gói `vn.chimto.tv` · tag `ios-v1.11.1` (không phải "latest") · commit `a775794` ·
  `ConBangTV-iPad-1.11.1.ipa` 4 316 750 byte · SHA-256 `64547dce3321e7b7ff75728e101efc9b698302a48bb7d7d380a46b7175f14147`
- 123 test lõi trên macOS (thêm 2). Bản thử 90 thử thật trên iPhone: «NextDNS gần nhất: 38.54.31.178, 171.244.57.202»; 9/9 tập
  phát (NguonC, VSMOV, yanhh3d, mephim, hoathinh3d, KKPhim); 28 lần hỏi DNS trung vị 108 ms, dùng lại kết nối 19–22 ms, không lần
  nào > 300 ms, không lỗi. Bản chính thức cài lên iPhone (đè 1.11): chạy, dữ liệu giữ nguyên. Chưa cài lên iPad (anh đang dùng).
  Tệp tải từ link công khai khớp SHA-256; `releases/latest` vẫn `v2.11.1`.

## Windows 1.8.1 — 09/11/2026

- **DNS mã hoá nhanh hơn** như TV 2.11.1: NextDNS dùng máy chủ gần nhất (Hà Nội) thay cho máy chủ Hồng Kông, giữ kết nối tới
  máy chủ DNS để dùng lại (tối đa 90 giây).
- bản 1.8.1 · tag `win-v1.8.1` (không phải "latest") · commit `a775794` · `ConBangTV-Windows-1.8.1.exe` 72 890 281 byte ·
  SHA-256 `e4d15cdd92c3b11299c87c9ac85a3099b447fb8fe1ea7bfd78cc997822b4fdab`
- 104 test lõi (thêm: mọi bản ghi A, đọc ba trả lời keep-alive liên tiếp, chỉ NextDNS lấy máy chủ gần nhất). Thử bản gỡ lỗi
  trên máy Windows: «NextDNS gần nhất: 38.54.31.178, 171.244.57.202» (Hà Nội), hỏi dùng lại kết nối 70–260 ms lúc mở app. Tệp tải
  từ link công khai khớp SHA-256. **Tự cập nhật thật 1.8 → 1.8.1** (bản chép riêng): mở lại 1.8.1, tìm được NextDNS Hà Nội ngay.

## 2.11.1 — 09/11/2026

- **DNS mã hoá nhanh hơn:** NextDNS dùng máy chủ gần mạng nhà nhất thay cho IP anycast (từ mạng Viettel bị đưa sang Hồng Kông):
  lần đầu app hỏi chính NextDNS tên `dns.nextdns.io` (đã mã hoá; NextDNS trả máy chủ gần IP của mạng này), không được thì DNS của
  máy; nhớ 6 giờ; anycast vẫn đứng sau để dự phòng. Giữ kết nối TLS tới máy chủ DNS để dùng lại (DoT; DoH bằng HTTP/1.1
  keep-alive), tối đa 4 kết nối nghỉ mỗi máy chủ, bỏ sau 90 giây; máy chủ đóng kết nối thì mở mới ngay (không tính là máy chủ lỗi).
- Đo từ mạng nhà (máy tính cùng mạng TV): NextDNS Hà Nội hỏi «lạnh» ~140–160 ms / «ấm» ~18 ms, anycast Hồng Kông ~210 / ~40 ms,
  Cloudflare ~200 / ~35–41 ms, Google ~174 / ~30–37 ms; mở kết nối TLS mới chiếm phần lớn thời gian.
- versionCode 34 · tag `v2.11.1` · commit `a775794` · `ChimToTV-2.11.1.apk` 1 216 343 byte · SHA-256
  `0ba4163740303b5e6ec2a83a7578f7195139c8b12447a481a8cb1e065ef95f24`
- 127 test (thêm 2). Thử bản gỡ lỗi trên TV box: «NextDNS gần nhất: 38.54.31.178, 171.244.57.202»; 6 tập (yanhh3d, VSMOV, mephim,
  hoathinh3d, KKPhim, NguonC) đều phát, 23 lần hỏi DNS trung vị 109 ms (dùng lại kết nối 39–140 ms; >300 ms chỉ lúc mở app — TV box
  bắt tay TLS ~400–750 ms), không lỗi. Bản chính thức thử trên TV box trước khi đăng (phát yanhh3d). API `releases/latest` trả
  `v2.11.1` + `.apk`, tệp tải từ link công khai khớp SHA-256. **Cập nhật tự động thật 2.11 → 2.11.1** trên TV box.
- Sau 2.11 (không phải lỗi app): hồ sơ NextDNS của app đang chặn một số tên miền app cần (yanhh3d.ee, máy chủ video KKPhim
  `*.phim1280.tv` / `kvp726.com` / `kkphimplayer1–7.com`, ảnh `clbphimxua.com` / `img.ophim.live`, `imasdk.googleapis.com`) →
  video lỗi; đã thêm vào danh sách cho phép của hồ sơ, kiểm lại 290 tên miền không còn bị chặn.

## iPad 1.11 — 09/11/2026

- **DNS mã hoá** (mới trên iPad / iPhone, bật sẵn; Cài đặt → mục «DNS mã hoá», ngay trên «Vượt chặn nhà mạng»): như TV 2.11 —
  DoH rồi DoT, lần lượt NextDNS → Cloudflare → Google; bị chặn hết thì dùng DNS của máy — app không hỏng. Kết nối tới máy chủ DNS
  bằng Network.framework (IP cố định, kiểm chứng chỉ theo tên máy chủ), đi thẳng không qua cổng nội bộ.
- **Vượt chặn nhà mạng mức 1 bật sẵn.** Máy đang dùng cũng tự bật cả hai một lần khi cập nhật (đang mức 2 thì giữ), sau đó ai
  tắt thì giữ tắt. Như trước: trên iOS 15–16, lúc bật app không giữ cookie của trang web sau khi tắt app.
- **Sửa:** ảnh bìa CLB Phim Xưa không hiện khi DNS trong nhà chặn tên miền ảnh (`clbphimxua.com` — DNS trả 0.0.0.0).
- bản 1.11 (CFBundleVersion 14) · mã gói `vn.chimto.tv` · tag `ios-v1.11` (không phải "latest") · commit `4908aa4` ·
  `ConBangTV-iPad-1.11.ipa` 4 311 267 byte · SHA-256 `4ee6ec823ae9ce77f3aafcf44f5ac6a8f580913e8b8402c63e3ca1451aaf7f28`
- 121 test lõi trên macOS (thêm: gói DNS, trả lời HTTP, thứ tự máy chủ, mặc định mới, máy chủ chết → DNS máy). Bản thử 86–87 thử
  thật trên iPad và iPhone (mạng có bộ lọc DNS chặn thật): tắt Vượt chặn thì ảnh CLB Phim Xưa trống, bật thì hiện; mọi tên miền
  qua DoH NextDNS; giả chặn 3 DoH → DoT NextDNS (ảnh vẫn hiện); giả chặn cả 6 → DNS của máy (danh sách vẫn mở); iPad đang mức 2
  giữ mức 2, iPhone đang tắt tự bật mức 1. Bản chính thức cài lên iPad và iPhone (đè 1.10): chạy, dữ liệu đã ghi mặc định mới
  (`doh` = AUTO, `net_defaults_v1`). Tệp tải từ link công khai khớp SHA-256; `releases/latest` vẫn `v2.11`.

## Windows 1.8 — 09/11/2026

- **DNS mã hoá bật sẵn** như TV 2.11 (Cài đặt → «DNS mã hoá (DoH / DoT)»: Bật / Tắt): DoH rồi DoT, lần lượt NextDNS → Cloudflare →
  Google; bị chặn hết thì dùng DNS của máy — app không hỏng. **Vượt chặn nhà mạng mức 1 bật sẵn**; máy đang dùng cũng tự bật cả
  hai một lần khi cập nhật (đang mức 2 thì giữ), sau đó ai tắt thì giữ tắt.
- **Sửa:** trang chủ báo «An item with the same key has already been added» (cả trang thành «Không tải được») khi hai hàng cùng
  một danh sách — vd hàng tự thêm «Hoạt hình Trung Quốc» và hàng «Hoạt hình Trung Quốc mới cập nhật · mọi nguồn». Ảnh bìa CLB
  Phim Xưa không hiện khi DNS trong nhà chặn tên miền ảnh (`clbphimxua.com` — DNS trả 0.0.0.0).
- bản 1.8.0 · tag `win-v1.8` (không phải "latest") · commit `4908aa4` · `ConBangTV-Windows-1.8.exe` 72 885 597 byte · SHA-256
  `9a226978cb863679883bdff8db35a5b487b49e6bc838d391dc38643ca562d2b3`
- 102 test lõi (thêm 11: gói DNS, trả lời HTTP, thứ tự máy chủ, mặc định mới, hai hàng cùng danh sách…). Thử trên máy Windows 10
  có bộ lọc DNS chặn thật: ảnh CLB Phim Xưa hiện qua DoH NextDNS; giả chặn 4 máy chủ đầu → DoT Cloudflare; giả chặn cả 6 → DNS
  của máy (danh sách vẫn mở). Tệp tải từ link công khai khớp SHA-256. **Tự cập nhật thật 1.7 → 1.8** từ GitHub (bản chép riêng,
  dữ liệu mới): hộp thoại «Có bản mới: Côn Bằng TV 1.8» → Cập nhật ngay → mở lại 1.8, Vượt chặn mức 1 + DNS mã hoá đã bật.

## 2.11 — 09/11/2026

- **DNS mã hoá bật sẵn:** app hỏi địa chỉ trang qua DoH (RFC 8484) rồi DoT (RFC 7858), lần lượt NextDNS (hồ sơ riêng của app) →
  Cloudflare → Google; bị chặn hết thì dùng DNS của máy — app không bao giờ hỏng vì DNS mã hoá bị chặn. Máy chủ bị chặn nghỉ 2
  phút; nhiều kết nối cùng hỏi một tên miền thì chỉ hỏi một lần; tên miền không có (hồ sơ DNS chặn) thì không hỏi máy chủ khác.
  Cài đặt → «DNS mã hoá: Bật (DoH / DoT: NextDNS → Cloudflare → Google)» / Tắt (trước: chọn Cloudflare / Google / Tắt).
- **Vượt chặn nhà mạng mức 1 bật sẵn.** Máy đang dùng cũng tự bật cả hai một lần khi cập nhật (đang mức 2 thì giữ mức 2); sau đó
  ai tắt thì giữ tắt.
- **Sửa:** ảnh bìa CLB Phim Xưa không hiện khi DNS trong nhà chặn tên miền ảnh (`clbphimxua.com` — DNS trả 0.0.0.0): tắt DNS mã
  hoá mà vẫn bật Vượt chặn thì DNS của máy trả 0.0.0.0 / :: là app tự hỏi lại qua DNS mã hoá.
- Bỏ chữ «· DoH» cạnh tên trang (nay là mặc định).
- versionCode 33 · tag `v2.11` · commit `4908aa4` · `ChimToTV-2.11.apk` 1 215 137 byte · SHA-256
  `d6d2218d3fe067f3df6b237d9c7f9508b61a6facc6fdf9e20708e5fd5f766ea7`
- 125 test (thêm 9). Thử bản gỡ lỗi trên TV box: mọi tên miền qua DoH NextDNS; giả chặn 3 DoH → DoT NextDNS (phim vẫn phát);
  giả chặn cả 6 → DNS của máy (trang phim mở đủ ảnh); giả DNS máy trả 0.0.0.0 → ảnh CLB Phim Xưa vẫn hiện. Bản chính thức thử trên
  TV box trước khi đăng (Cài đặt hiện NextDNS, Vượt chặn mức 1). API `releases/latest` trả `v2.11` + `.apk`, tệp tải từ link công
  khai khớp SHA-256. **Cập nhật tự động thật 2.10 → 2.11** trên TV box.

## iPad 1.10 — 09/11/2026

- **Hàng vuốt ngang cả trên iPad và iPhone xoay ngang** (trước chỉ iPhone cầm dọc): «Xem thêm ›» ở tên hàng + ô hẹp cuối dải.
- **Sửa nhanh ngay trên trang chủ:** giữ ngón trên một phim, tên hàng hoặc ô «Xem thêm» (chuột: nhấn phải) → ẩn hàng, chuyển
  hàng lên / xuống (qua hàng đang hiện kề nó), «Các hàng trên trang chủ…», «Nguồn cho trang chủ…» (bảng riêng mở từ trang chủ,
  thanh tiêu đề tự vẽ; đóng bảng là trang chủ xếp lại / tải lại). Hồ sơ trẻ em có mã PIN phụ huynh: chỉ mở bảng các hàng (hỏi mã).
- **Nguồn cho trang chủ** (Cài đặt → Trang chủ, riêng từng người xem): bật / tắt KKPhim, NguonC, VSMOV, CLB Phim Xưa, trang đang
  dùng + trang xem gần đây; còn một nguồn thì không tắt được; tắt hết (nguồn còn bật bị xoá khỏi danh sách trang) thì coi như
  bật hết. Tắt KKPhim thì hàng lọc theo thể loại / nước lấy từ VSMOV.
- bản 1.10 (CFBundleVersion 13) · mã gói `vn.chimto.tv` · tag `ios-v1.10` (không phải "latest") · commit `947c004` ·
  `ConBangTV-iPad-1.10.ipa` 4 284 165 byte · SHA-256 `38735a306dc8decf5860684846ca5d836af0ff3476d7df533864224b737026d8`
- 112 test lõi trên macOS (thêm 4 test: chuyển qua hàng đang hiện, ẩn hàng, loại hàng bản cũ, nguồn tắt). Bản thử 83 thử thật:
  iPad — dải, ẩn / chuyển hàng, bảng «Nguồn» mở từ trang chủ (tắt VSMOV → đóng bảng → trang chủ tải lại, «2 nguồn» thành
  «kkphim»), không còn lỗi bảng mở / đóng liên tục; iPhone — dải dọc / ngang, bảng «Các hàng» trên màn hẹp, bật lại hàng → đóng
  bảng tải lại. Bản chính thức cài lên iPad và iPhone (đè 1.9): chạy, tải trang chủ, không văng. Tệp tải từ link công khai khớp
  SHA-256; `releases/latest` vẫn `v2.10`.

## Windows 1.7 — 09/11/2026

- **Hàng vuốt ngang** như TV 2.10: tới 12 phim một dải, ô «Xem thêm» hẹp cuối dải, «Xem thêm ›» ở tên hàng (chuột), nút ‹ ›
  khi rê chuột, Shift + lăn chuột cuộn dải (lăn chuột thường vẫn cuộn trang). Phím: ◄ ► chỉ đi trong dải (cuối dải đứng yên, ◄
  ở phim đầu ra menu bên trái), ▲ ▼ sang phim đang thấy của dải kia.
- **Sửa nhanh ngay trên trang chủ:** nhấn phải / phím Menu / Shift + F10 / M trên phim của một hàng hoặc tên hàng → «Hàng
  «…»»: ẩn hàng này, ▲ / ▼ chuyển (giữ ô đang chọn), «Các hàng…», «Nguồn…». Hồ sơ trẻ em: mã PIN phụ huynh.
- **Nguồn cho trang chủ** (Cài đặt → «Nguồn cho trang chủ: n / m…», riêng từng người xem) như bản TV.
- **Sửa:** mở lại cửa sổ luôn kéo về trong màn hình (trước chỉ kiểm chiều ngang — lưu sát mép dưới / màn hình phụ đã rút thì lần
  sau cửa sổ gần như nằm ngoài màn hình, trông như trắng một nửa). Biến môi trường `CONBANG_SOFTWARE_RENDER=1`: vẽ bằng CPU.
- bản 1.7.0 · tag `win-v1.7` (không phải "latest") · commit `947c004` · `ConBangTV-Windows-1.7.exe` 72 876 194 byte · SHA-256
  `fa5db5b359bbb8877b57bf42cb76160c1b49f90798c6af27fee6d44feb4771ef`
- 91 test lõi (thêm 4). Thử trên máy Windows 10 (bản gỡ lỗi): dải + phím mũi tên, phím M / Menu → bảng, chuyển xuống (giữ ô),
  ẩn hàng (chọn sang phim đầu hàng kế), trang «Nguồn» (tắt / bật NguonC). Tệp tải từ link công khai khớp SHA-256. **Tự cập nhật
  thật 1.6 → 1.7** từ GitHub (bản chép riêng, dữ liệu mới): hộp thoại «Có bản mới: Côn Bằng TV 1.7» → Cập nhật ngay → mở lại 1.7
  với trang chủ dải vuốt ngang.

## 2.10 — 09/11/2026

- **Trang chủ dạng hàng vuốt ngang trên TV / màn ngang** (như điện thoại cầm dọc từ 2.8.1): thấy 5 phim và một phần phim kế, ◄ ►
  cuộn trong hàng (tới 12 phim; ◄ ở phim đầu ra menu bên trái), ô «Xem thêm» hẹp ở cuối hàng; Back từ «Xem thêm» về đúng ô.
- **Sửa nhanh ngay trên trang chủ:** nhấn Menu hoặc giữ OK trên phim của một hàng / ô «Xem thêm» (cảm ứng: giữ tên hàng) →
  «Hàng «…»»: ẩn hàng này, ▲ chuyển lên / ▼ chuyển xuống (qua hàng đang hiện kề nó), «Các hàng trên trang chủ…», «Nguồn cho
  trang chủ…». Hộp xoá phim ở «Xem tiếp» / bỏ theo dõi ở «Có tập mới» có nút «Ẩn hàng này». Hồ sơ trẻ em: mã PIN phụ huynh.
- **Nguồn cho trang chủ** (Cài đặt → Trang chủ → «Nguồn cho trang chủ: n / m…», riêng từng người xem): bật / tắt KKPhim, NguonC,
  VSMOV, CLB Phim Xưa, trang đang dùng + trang xem gần đây; còn một nguồn thì không tắt được; tắt hết thì coi như bật hết. Tắt
  KKPhim thì hàng lọc theo thể loại / nước lấy từ VSMOV. Đổi nguồn là tải lại trang chủ.
- TV box: dải không vẽ vệt cuộn quá đầu / cuối; vẽ lại khung lưới sau hiệu ứng phóng to ô (trước còn vệt viền cam ở mép lưới).
- versionCode 32 · tag `v2.10` · commit `947c004` · `ChimToTV-2.10.apk` 1 212 683 byte · SHA-256
  `b777935cd2928bdd66fa0838b12daed5a11eb0e547eacbaf58ed73fed5086411`
- 116 test (thêm 4). Thử bản gỡ lỗi trên TV box bằng remote: Menu → bảng, ▲ chuyển (giữ ô), ẩn hàng (chọn sang hàng kế), tắt
  KKPhim (hàng lọc lấy VSMOV, không còn nhãn «kkphim»), dải: ► tới ô «Xem thêm», ◄ ra menu, ▼ sang dải dưới, Back từ «Xem thêm» về
  đúng ô. Bản chính thức thử trên TV box trước khi đăng (dải, bảng sửa nhanh). API `releases/latest` trả `v2.10` + `.apk`, tệp tải từ
  link công khai khớp SHA-256. **Cập nhật tự động thật 2.9 → 2.10** trên TV box.

## iPad 1.9 — 09/11/2026

- **Các hàng trên trang chủ** như TV 2.9: Cài đặt → Trang chủ → «Các hàng trên trang chủ» (riêng từng người xem) — bật / tắt
  từng hàng (hàng tắt thì không tải), ▲ ▼ đổi thứ tự, «Thêm hàng…» (chủ đề có sẵn, hoặc loại phim + nước + thể loại + ngôn ngữ,
  tối đa 6 hàng), xoá hàng tự thêm, «Khôi phục mặc định». «Có tập mới» / «Xem tiếp» chỉ bật / tắt, luôn ở đầu trang. Chủ đề
  người xem mới chọn giờ là hàng tự thêm. Hồ sơ trẻ em: chỉ các hàng hoạt hình, sửa cần mã PIN phụ huynh (hỏi ngay trong trang).
  Đóng Cài đặt là trang chủ xếp lại, tải hàng mới.
- bản 1.9 (CFBundleVersion 12) · mã gói `vn.chimto.tv` · tag `ios-v1.9` (không phải "latest") · commit `e139253` ·
  `ConBangTV-iPad-1.9.ipa` 4 251 574 byte · SHA-256 `48bb2034f25feb39c5527529caa349fa4cc6fc8c51872cc9f3e56992e2dd7cfb`
- 108 test lõi trên macOS (9 test các hàng). Bản thử 78 thử thật trên iPad: bật / tắt, đổi thứ tự, thêm «Phim Hàn» → đóng Cài
  đặt → trang chủ xếp đúng (sửa lỗi bảng Cài đặt tự mở / đóng liên tục khi đóng lúc đang ở trang các hàng — iOS 15); iPhone: trang
  các hàng hiện đúng. Bản chính thức cài lên iPad (đè 1.8.2): mở được, không văng; cài lên iPhone (đè 1.8.2): mở được, tải
  xong trang chủ (ảnh poster mới ghi), không văng. `releases/latest` vẫn `v2.9`; tệp tải từ link công khai khớp SHA-256.

## Windows 1.6 — 09/11/2026

- **Các hàng trên trang chủ** như TV 2.9: Cài đặt → «☰ Các hàng trên trang chủ…» (riêng từng người xem) — bật / tắt từng hàng
  (hàng tắt thì không tải), ▲ ▼ đổi thứ tự (đi bằng phím, giữ nguyên ô đang chọn), «＋ Thêm hàng…» (chủ đề có sẵn, hoặc loại phim
  / nước / thể loại / ngôn ngữ bằng hộp chọn của app, tối đa 6 hàng), ✕ xoá hàng tự thêm, «Khôi phục mặc định». «Có tập mới» /
  «Xem tiếp» chỉ bật / tắt, luôn ở đầu trang. Hồ sơ trẻ em: chỉ các hàng hoạt hình, sửa cần mã PIN phụ huynh.
- bản 1.6.0 · tag `win-v1.6` (không phải "latest") · commit `e139253` · `ConBangTV-Windows-1.6.exe` 72 869 613 byte ·
  SHA-256 `5c558c9c8b9cca70731f04131ccddc380520d2690123be0c242a641fc12b005b`
- 87 test lõi (8 test các hàng). Thử trên máy Windows 10 (bản gỡ lỗi): bật / tắt, ▲ ▼ bằng phím, thêm «Phim Hàn» → trang chủ
  hiện ngay sau «Xem tiếp». Tệp tải từ link công khai khớp SHA-256. **Tự cập nhật thật 1.5 → 1.6** từ GitHub: hộp thoại «Có bản
  mới: Côn Bằng TV 1.6» → Cập nhật ngay → mở lại 1.6.

## 2.9 — 09/11/2026

- **Các hàng trên trang chủ** (Cài đặt → Trang chủ → «Các hàng trên trang chủ…», riêng từng người xem): bật / tắt từng hàng —
  hàng tắt thì không tải; ▲ ▼ đổi thứ tự; «Mặc định» trả thứ tự cũ và bật lại mọi hàng (giữ hàng tự thêm). «Có tập mới» và «Xem
  tiếp» chỉ bật / tắt, luôn ở đầu trang.
- **Hàng tự thêm** («＋ Thêm hàng…», tối đa 6): chủ đề có sẵn (Phim Hàn, Anime Nhật, Kiếm hiệp…) hoặc tự chọn loại phim + nước +
  thể loại + ngôn ngữ (vd «Hoạt hình Nhật Bản · viễn tưởng»). Phim lấy từ KKPhim (không có thì VSMOV), phim cùng tên ở trang khác
  tự gộp vào. Hàng mới đứng sau «Xem tiếp», đổi chỗ được như các hàng khác, ✕ để xoá. Chủ đề người xem mới chọn lúc chưa có lịch
  sử giờ là hàng tự thêm — vẫn còn khi đã xem phim.
- Hồ sơ trẻ em: chỉ các hàng hoạt hình (chủ đề hợp trẻ em, loại phim hoạt hình, bỏ thể loại người lớn); sửa cần mã PIN phụ huynh.
- versionCode 31 · tag `v2.9` · commit `e139253` · `ChimToTV-2.9.apk` 1 207 471 byte · SHA-256
  `471808e304abc2b289dcad514e9da2d6ceda14b5b423460c8e2fc8a22eaccfaf`
- 112 test (8 test các hàng). Thử bản gỡ lỗi trên TV box bằng remote: bật / tắt, ▲ ▼, «Mặc định», tắt «Xem tiếp», thêm «Phim
  Hàn» và «Hoạt hình Nhật Bản · viễn tưởng» — tải đúng phim. Bản chính thức thử trên TV box trước khi đăng (trang chủ, hộp các
  hàng). API `releases/latest` trả `v2.9` + `.apk`, tệp tải từ link công khai khớp SHA-256. **Cập nhật tự động thật 2.8.1 → 2.9**
  trên TV box.

## iPad 1.8.2 — 09/11/2026

- **Cập nhật trên iOS / iPadOS 15:** bấm «Kiểm tra cập nhật» trong Cài đặt giờ hiện hộp thoại «Có bản mới … — Cài đặt ngay»
  → TrollStore tự tải và hỏi Install (trước đây trên iOS 15 hộp thoại SwiftUI bị bảng Cài đặt che, chỉ thấy dòng «Có bản
  mới …» nên phải tải tay; đóng bảng thì app văng). Hộp thoại giờ hiện trong cửa sổ riêng, đè lên mọi màn. Bản 1.8.1 trở về
  trước trên iOS 15 cần tải tay một lần lên 1.8.2.
- **Vượt chặn nhà mạng / proxy:** app nằm nền lâu thì iOS thu hồi cổng nội bộ — vào lại mọi trang báo mất kết nối tới khi tắt
  app. Giờ quay lại app là tự mở lại cổng (đúng số cổng cũ); vòng nhận kết nối thấy cổng hỏng thì dừng, không quay vòng ăn CPU.
- **iPad không còn văng app khi đóng Cài đặt** (lỗi bố cục SwiftUI trên iPadOS 15 ở nút «Xong» của thanh tiêu đề): thanh tiêu
  đề Cài đặt tự vẽ (Xong / Esc); cập nhật màn chính sau khi bảng đóng hẳn.
- **Trình phát → Menu → «Danh sách tập»** mở trang phim, cuộn tới tập đang xem (mở từ hàng Xem tiếp trước đây chỉ về trang chủ).
- **Ô phim:** nhãn tập ở góc dưới bên trái như bản TV — trên iPhone dọc không còn đè nhãn «N nguồn»; hàng Xem tiếp có nút ▶
  giữa poster.
- bản 1.8.2 (CFBundleVersion 11) · mã gói `vn.chimto.tv` · tag `ios-v1.8.2` (không phải "latest") · commit `d6010ea` ·
  `ConBangTV-iPad-1.8.2.ipa` 4 186 872 byte · SHA-256 `9b583a21a9b1f3957e9cc305ceaf682c86e0b3f11ec99a68595f973f275ef36f`
- 99 test lõi trên macOS. Bản thử thử thật: iPad (iPadOS 15.4.1) — hộp thoại cập nhật hiện đè bảng Cài đặt → «Cài đặt ngay» →
  TrollStore tự tải đủ 1.8.1; «Để sau» rồi đóng Cài đặt 3 lần không văng; «Danh sách tập» mở đúng trang phim. iPhone 6s Plus
  (iOS 15.8.3) — gọi TrollStore bằng `apple-magnifier://install` mở được; giả cổng nội bộ hỏng → ra nền → vào lại: «mở lại
  cổng nội bộ: 54733 → 54733», trang tải qua cổng bình thường. Bản chính thức cài lên iPad (đè 1.8) và iPhone (đè 1.8.1), mở
  được, không văng. Tệp tải từ link công khai khớp SHA-256.

## iPad 1.8.1 — 09/11/2026

- **iPhone cầm dọc:** mỗi hàng của trang chủ tổng hợp là một **dải vuốt ngang** — thấy trọn 3 phim và một phần phim kế, tới 12
  phim một dải. «Xem thêm ›» nằm bên phải tên hàng (trước đây ô «Xem thêm» chiếm một trong 3 ô của hàng, chỉ còn 2 phim); cuối
  dải có ô «Xem thêm» hẹp. «Có tập mới» / «Xem tiếp» cũng là dải. Bấm «Xem thêm» rồi quay lại (‹) thì cuộn về đúng hàng đó.
  Áp dụng khi lưới hẹp hơn 500 pt (iPhone dọc, iPad Slide Over / chia màn hẹp); iPad và iPhone xoay ngang giữ lưới như cũ, xoay
  qua lại thì tự đổi. Bàn phím: mỗi dải là một dòng (◄ / ► đi trong dải, ▲ / ▼ sang dải khác giữ thứ tự ô).
- bản 1.8.1 (CFBundleVersion 10) · mã gói `vn.chimto.tv` · tag `ios-v1.8.1` (không phải "latest") · commit `7204ee0` ·
  `ConBangTV-iPad-1.8.1.ipa` 4 177 680 byte · SHA-256 `df2abda78b92b50a0387d3ace5348538e5880dcb5d086ca0cf930c434f5497f0`
- 99 test lõi trên macOS (thêm test phím trên dải). Bản thử 64 thử thật trên iPhone 6s Plus: dải, ô «Xem thêm» cuối dải, «Xem thêm»
  ở tiêu đề và ở cuối dải + quay lại về đúng hàng, xoay ngang ra lưới 5 cột rồi xoay dọc lại. Bản chính thức cài lên iPhone (đè
  1.7), mở được, tải xong trang chủ. iPad khoá màn hình nên chưa cài. Tệp tải từ link công khai khớp SHA-256.

## 2.8.1 — 09/11/2026

- **Điện thoại cầm dọc:** trang chủ tổng hợp xếp mỗi hàng thành **dải vuốt ngang** như bản iPhone — thấy trọn 3 phim và một phần
  phim kế, tới 12 phim một dải; «Xem thêm ›» bên phải tên hàng (chạm) + ô «Xem thêm» hẹp cuối dải (remote / bàn phím). «Có tập
  mới» / «Xem tiếp» cũng là dải. Dải nhớ chỗ đã vuốt tới; ◄ / ► ở đầu / cuối dải đứng yên; quay lại từ «Xem thêm» thì về đúng
  hàng (dùng remote: chọn lại đúng ô). Xoay máy thì xếp lại trang chủ từ lần tải gần nhất. TV và điện thoại xoay ngang giữ lưới
  như cũ.
- versionCode 30 · tag `v2.8.1` · commit `7204ee0` · `ChimToTV-2.8.1.apk` 1 197 959 byte · SHA-256
  `b50e819ff4130e6092ed2dfcabf823b8f55a73304faa799d39e2623902c42ba9`
- 104 test. Thử bản gỡ lỗi trên TV box giả điện thoại dọc (720×1280): vuốt dải, ô cuối dải, «Xem thêm» ở tiêu đề + quay lại,
  remote ▲ ▼ ◄ ► trong / giữa các dải, OK ở ô cuối dải + Back về đúng ô, xoay ngang ↔ dọc; trên TV lưới vẫn như cũ. Bản chính
  thức thử trên TV box trước khi đăng. API `releases/latest` trả `v2.8.1` + `.apk`, tệp tải từ link công khai khớp SHA-256.
  **Cập nhật tự động thật 2.8 → 2.8.1** trên TV box.

## Windows 1.5 — 09/11/2026

- **Trang chủ tổng hợp** như TV 2.8 (`Core/Feed.cs`, `Core/HomeFeed.cs` — cùng tên hàm với bản TV): phim mới từ mọi trang trong
  danh sách, gộp phim trùng, hàng theo lịch sử từng người, «Có tập mới» cả phim đang xem dở ở trang khác, hồ sơ trẻ em, hàng
  thuyết minh, chọn chủ đề cho người chưa có lịch sử. Mỗi hàng một dòng + «Xem thêm ›» (Esc về đúng ô); nhãn «N nguồn» dưới tên
  phim; trang phim có nút «⇄ Nguồn: … ▾» (hộp chọn của app, đi bằng phím). Cài đặt → Trang chủ: Tổng hợp / Theo trang đang
  dùng, Hồ sơ trẻ em, «↻ Thử lại ngay» khi có trang đang tạm nghỉ.
- **Phím:** ▲ / ▼ trong trang không còn nhảy sang menu bên trái khi hàng dưới cách xa (có tiêu đề hàng); ◄ / ► ưu tiên mục cùng
  hàng (hàng nút xuống dòng ở trang phim). Đổi nguồn rồi quay lại vẫn về đúng ô ở trang chủ.
- bản 1.5.0 · tag `win-v1.5` (không phải "latest") · commit `d1e0b8d` · `ConBangTV-Windows-1.5.exe` 72 861 002 byte ·
  SHA-256 `0781cb21f7c05b7dd905ae30e33081b04640830fb038232adf9dd22230e6daee`
- 79 test lõi (8 test trang chủ). Thử trên máy Windows 10 (bản gỡ lỗi): các hàng, «Xem thêm» + Esc, đổi nguồn hoathinh3d → kkphim,
  hồ sơ trẻ em bật / tắt, kiểu theo trang, giả KKPhim lỗi → «lỗi» → 3 lượt → «tạm nghỉ» → «Thử lại ngay». Tệp tải từ link
  công khai khớp SHA-256. **Tự cập nhật thật 1.4 → 1.5** từ GitHub: hộp thoại «Có bản mới: Côn Bằng TV 1.5» → Cập nhật ngay →
  mở lại 1.5 với trang chủ tổng hợp.

## iPad 1.8 — 09/11/2026

- **Trang chủ tổng hợp** như TV 2.8 (`Feed.swift`, `HomeFeed.swift`): các hàng theo lịch sử từng người, «Có tập mới» cả phim
  đang xem dở ở trang khác, hồ sơ trẻ em, hàng thuyết minh, chọn chủ đề; mỗi hàng một dòng + ô «Xem thêm» (nút ‹ / Esc quay lại);
  nhãn «N nguồn» ở góc ô phim; trang phim có nút «Nguồn: … ▾». Cài đặt → Trang chủ: tổng hợp / theo trang, hồ sơ trẻ em,
  «Thử lại ngay».
- bản 1.8 (CFBundleVersion 9) · mã gói `vn.chimto.tv` · tag `ios-v1.8` (không phải "latest") · commit `d1e0b8d` ·
  `ConBangTV-iPad-1.8.ipa` 4 150 419 byte · SHA-256 `706ed1641b966ff75764aec04b9075bdbbf26267a060978acbee2b264b3b68bf`
- 98 test lõi trên macOS. Bản thử 59 đã thử thật trên iPad Pro: các hàng, nhãn nguồn, «Xem thêm» + quay lại, đổi nguồn phim 3
  nguồn (hoathinh3d.pw / yanhh3d / kkphim), hồ sơ trẻ em bật / tắt, kiểu theo trang. Bản chính thức đã cài lên iPad (1.8); chưa
  mở được vì iPad khoá màn hình ngay sau đó. iPhone mới cài bản thử 59, chưa thử. Tệp tải từ link công khai khớp SHA-256.

## 2.8 — 09/11/2026

- **Trang chủ tổng hợp** (`Feed.kt`, `HomeFeed.kt`) — mặc định cho mọi người xem:
  - Lấy phim mới từ mọi trang trong danh sách (KKPhim, NguonC, VSMOV, CLB Phim Xưa + trang đang dùng / xem gần đây), gộp phim
    trùng giữa các trang (tên đã chuẩn hoá, năm lệch ≤ 1).
  - Hàng theo lịch sử xem của từng người: **Có tập mới** (cả phim đang xem dở có tập mới ở trang khác — «có ở nguonc»), Xem tiếp,
    Giống «phim vừa xem», Vì … hay xem <thể loại>, <loại phim hay xem> mới cập nhật · mọi nguồn, **Thuyết minh mới** (khi từ nửa
    số lần mở tập gần đây là bản thuyết minh / lồng tiếng), Mới cập nhật · mọi nguồn, Phim lẻ mới. Người chưa có lịch sử chọn
    chủ đề.
  - Mỗi hàng một dòng + ô «Xem thêm»; ô phim ghi «N nguồn»; trang phim có nút «Nguồn: … ▾» để mở ở trang khác.
  - Trang lỗi / chậm: mỗi trang hạn 6 giây, dùng bản lưu (dưới 24 giờ), về muộn thì trang chủ tự cập nhật (giữ ô đang chọn), lỗi
    3 lượt liền thì tạm nghỉ 30 phút / 2 giờ / 6 giờ; dòng trạng thái bên phải tiêu đề; Cài đặt → «Thử lại ngay».
  - Mở app hiện ngay trang chủ lần trước của người xem (~0,8 s trên TV box), rồi làm mới (~5–9 s).
- **Hồ sơ trẻ em** (Cài đặt → Trang chủ, riêng từng người; tắt cần mã PIN phụ huynh nếu đã đặt): trang chủ chỉ có hoạt hình hợp
  trẻ em — thiếu nhi, gia đình, lồng tiếng; bỏ phim 18+ / kinh dị / hình sự / chiến tranh và tên phim ma. Chỉ lọc trang chủ.
- Cài đặt → Trang chủ: «Tổng hợp mọi nguồn» hoặc «Theo trang đang dùng» (như trước 2.8).
- versionCode 29 · tag `v2.8` · commit `d1e0b8d` · `ChimToTV-2.8.apk` 1 195 968 byte · SHA-256
  `2f13f8b9573a49cba6e44bb2786e1c8b48207d1b3a9a4ef037992bac688b5cf7`
- 104 test (17 test trang chủ). Thử bản gỡ lỗi trên TV box: gộp nguồn (Đấu Phá Thương Khung Phần 5 = yanhh3d + hoathinh3d.pw +
  KKPhim), đổi nguồn, «Xem thêm», giả KKPhim lỗi → bản lưu / «lần kiểm trước» / tạm nghỉ / «Thử lại ngay», kiểu cũ, người xem
  mới chọn chủ đề, hàng thuyết minh, hồ sơ trẻ em. Bản chính thức thử trên TV box (lịch sử cũ → «Có tập mới» 4 phim). API
  `releases/latest` trả `v2.8` + `.apk`, tệp tải từ link công khai khớp SHA-256. **Cập nhật tự động thật 2.7 → 2.8** trên TV box.

## Windows 1.4 — 08/11/2026

- **Thêm 3 trang** như TV 2.7 (`NguonC.cs`, `Stremio.cs`, VSMOV trong `ApiSite.cs`): NguonC, VSMOV, CLB Phim Xưa (qua add-on k-20);
  có sẵn trong Đổi trang phim (người đã cài được thêm một lần). **Server dự phòng NguonC** cạnh KKPhim; Cài đặt: «Server dự phòng
  NguonC».
- **Thoát được toàn màn hình** (anh báo lỗi, có từ 1.0): «Mở app ở chế độ toàn màn hình: Tắt» nay thoát luôn; khi toàn màn hình
  (cửa sổ không còn thanh tiêu đề) menu có «⤡ Thoát toàn màn hình (F11)»; Esc ở menu cũng thoát.
- Cập nhật app: link tải github.com lỗi thì tải cùng tệp qua API GitHub, thử lại cả hai đường sau 5 s / 15 s.
- bản 1.4.0 · tag `win-v1.4` (không phải "latest") · commit `7c879e4` · `ConBangTV-Windows-1.4.exe` 72 813 124 byte ·
  SHA-256 `de0435cf258bac80b7743fa9df260602e79ffd5eda00e8a2675116ff508b5401`
- 71 test lõi. Thử trên máy Windows 10 (bản gỡ lỗi): danh sách trang được thêm 3 trang đúng một lần; trang chủ NguonC / VSMOV /
  CLB Phim Xưa; phát VSMOV (HLS), NguonC, CLB Phim Xưa (MP4); NguonC dự phòng trên yanhh3d; thoát toàn màn hình bằng menu / F11
  / Esc; đường tải dự phòng (máy chủ giả trả 504 → tải qua API → bước kiểm tệp vẫn chạy). Tệp tải từ link công khai khớp SHA-256.
  **Tự cập nhật thật 1.3 → 1.4** từ GitHub: hộp thoại «Có bản mới: Côn Bằng TV 1.4» → Cập nhật ngay → mở lại 1.4.

## iPad 1.7 — 08/11/2026

- **Thêm 3 trang** như TV 2.7 (`NguonC.swift`, `Stremio.swift`, VSMOV trong `ApiSite.swift`) và **server dự phòng NguonC** cạnh
  KKPhim (Cài đặt → Phát video: «Server dự phòng NguonC»). VSMOV phát HLS được cả trên iPhone (trình phát của iOS tự bỏ phần ảnh
  PNG đứng trước dữ liệu video).
- bản 1.7 (CFBundleVersion 8) · mã gói `vn.chimto.tv` · tag `ios-v1.7` (không phải "latest") · commit `7c879e4` ·
  `ConBangTV-iPad-1.7.ipa` 3 912 873 byte · SHA-256 `76d585319ba2638d8011087bf0321e922216fa41657a6b3cb50de619a5a028c6`
- 90 test lõi trên macOS. Bản thử 54 đã thử thật trên iPad Pro và iPhone 6s Plus: trang chủ 3 trang, phát VSMOV (HLS), NguonC,
  CLB Phim Xưa (MP4 mượt), NguonC dự phòng trên yanhh3d (iPad). **Bản chính thức đã cài thật**: iPad 1.6 → 1.7 (danh sách trang
  được thêm 3 trang đúng một lần), iPhone 1.7; app mở và chạy. Tệp tải từ link công khai khớp SHA-256.

## 2.7 — 08/11/2026

- **Thêm 3 trang**, có sẵn trong ☰ → Đổi trang phim (người đã cài được thêm một lần):
  - **NguonC** (phim.nguonc.com, `NguonC.kt`): API riêng — trang chủ Hoạt hình mới cập nhật / Phim mới / Phim bộ, lẻ mới /
    Thuyết minh; menu theo loại phim, ngôn ngữ, nước; «Thể loại…», «Quốc gia…», tìm kiếm. Phát bằng trình phát của NguonC
    (iframe, app tự bỏ quảng cáo trước phim).
  - **VSMOV** (vsmov.com, trong `ApiSite.kt`): API kiểu KKPhim khác đường dẫn; app đọc luồng HLS (link có chữ ký) trong trang
    trình phát và tự phát — `hls.html` cắt phần ảnh PNG ngụy trang trước dữ liệu TS; còn «Trình phát VSMOV» dự phòng.
  - **CLB Phim Xưa** (add-on Stremio k-20, `Stremio.kt`): phim Hồng Kông / TVB / kiếm hiệp cũ; mỗi chất lượng một server MP4 (phát
    thẳng bằng `<video>`). Qua máy chủ trung gian nên trên TV mở chậm (~25–35 s), thỉnh thoảng khựng, tua xa có khi không được.
- **Server dự phòng NguonC** cạnh KKPhim cho yanhh3d / hoathinh3d / hhkungfu / hoathinh3d.pw (NguonC hay có tập mới sớm, vd Mục
  Thần Ký tập 108 khi KKPhim chưa có). Hai nguồn dự phòng tìm song song, nguồn chậm không làm mất nguồn nhanh. Cài đặt → Phát video:
  «Server dự phòng NguonC».
- **Cập nhật app**: link tải github.com lỗi thì tải cùng tệp qua API GitHub (8/11 GitHub vùng Đông Nam Á trả 504 / 500 xen kẽ
  nhiều giờ); thử lại cả hai đường sau 5 s / 15 s; tệp vẫn qua bước kiểm gói + chữ ký + số hiệu.
- Tên tập VSMOV ghi số trần → «Tập N»; tên bản có xuống dòng được gộp lại; nhãn trang «nguonc», «CLB Phim Xưa».
- versionCode 28 · tag `v2.7` · commit `7c879e4` · `ChimToTV-2.7.apk` 1 160 565 byte · SHA-256
  `a879c8aef5e6ec847f3b003bd53b929746577daffd71f3c001767e7a642f5ee0`
- 87 test. Thử bản gỡ lỗi trên TV box và Skyworth: trang chủ / chi tiết 3 trang, đo tốc độ, phát VSMOV (HLS, server nhúng),
  NguonC, CLB Phim Xưa, NguonC dự phòng trên yanhh3d; đường tải dự phòng (máy chủ giả trả 504 → tải qua API). Bản chính thức thử
  trên TV box (Tiên Nghịch tập 165 có «KKPhim Vietsub» và «NguonC Vietsub»). API `releases/latest` trả `v2.7` + `.apk`, tệp tải
  từ link công khai khớp SHA-256. **Cập nhật tự động thật 2.6 → 2.7** trên TV box (cũng đã thử 2.5 → 2.6 cho bản 2.6). Skyworth
  (bản 1.8.1) tải được bản mới nhưng bước cài bị khoá trẻ em TiviKit chặn (cần mã bố mẹ).

## Windows 1.3 — 08/11/2026

- **Server dự phòng KKPhim** như TV 2.6 (`KkPhim.cs`): yanhh3d / hoathinh3d / hhkungfu / hoathinh3d.pw có thêm «KKPhim Vietsub /
  Thuyết Minh» trong bảng ▼ (HLS, bỏ quảng cáo; đúng ngôn ngữ của bản đang xem; «Trình phát của trang» cho hoathinh3d.pw);
  trang lỗi mà KKPhim có tập thì vẫn xem được. Cài đặt: «Server dự phòng KKPhim» (mặc định bật).
- **Thêm trang KKPhim** (www.kkphim.com, gọi thẳng API phimapi.com), có sẵn trong Đổi trang phim (người đã cài được thêm
  một lần). Gõ «kkphim.com» khi thêm trang là trang KKPhim có sẵn (tên miền trần bị nhà mạng chặn).
- Sửa lỗi có từ 1.0: trang «Đổi trang phim» bấm Đo / Xoá / Thêm / + trang mẫu một lần là mất ô thêm trang.
- Sửa: mở phim mới mà trang phim tải lỗi thì bảng ▼ không còn giữ server / bản / tập / tiêu đề của phim trước (bấm vào có thể
  phát nhầm phim); «Tải lại» lúc đó tải lại thông tin phim.
- bản 1.3.0 · tag `win-v1.3` (không phải "latest") · commit `b2a92de` · `ConBangTV-Windows-1.3.exe` 72 796 716 byte ·
  SHA-256 `98950fc9d9c37e69e58a3afc86d87356c03d6f728b923f3b0bfa2a844cbb349d`
- Đã thử trên máy Windows 10 (bản gỡ lỗi): 63 test lõi; danh sách trang của người dùng 1.2 được thêm KKPhim đúng một lần;
  Đo tốc độ; trang KKPhim (trang chủ, menu, chi tiết, phát HLS 1080p); server dự phòng không trùng; đổi qua lại trình phát
  của hoathinh3d.pw ↔ KKPhim giữ chỗ xem; tắt / bật trong Cài đặt. Tệp tải từ link công khai khớp SHA-256.

## iPad 1.6 — 08/11/2026

- **Server dự phòng KKPhim** như TV 2.6 (`KkPhim.swift`): «KKPhim Vietsub / Thuyết Minh» trong Menu của trình phát cho
  yanhh3d / hoathinh3d / hhkungfu / hoathinh3d.pw; Cài đặt → Phát video: «Server dự phòng KKPhim».
- **Thêm trang KKPhim** (www.kkphim.com, gọi thẳng phimapi.com), có sẵn trong Đổi trang phim (thêm một lần cho người đã
  cài); gõ «kkphim.com» khi thêm trang là trang KKPhim có sẵn; gợi ý thêm trang có nhắc KKPhim.
- bản 1.6 (CFBundleVersion 7) · mã gói `vn.chimto.tv` · tag `ios-v1.6` (không phải "latest") · commit `b2a92de` ·
  `ConBangTV-iPad-1.6.ipa` 3 853 799 byte · SHA-256 `8d7aaba7fdcdfc2b0e6b11eb1cfc7304a47439a974cb0607f9f3445bfd46c1ac`
- 81 test lõi trên macOS. Bản thử 50 đã thử trên iPad (server dự phòng, trang KKPhim). **Bản chính thức đã cài thật**: iPad
  Pro (1.5 → 1.6, danh sách trang được thêm www.kkphim.com đúng một lần) và iPhone 6s Plus (cài mới), app mở và chạy.

## 2.6 — 08/11/2026

- **Server dự phòng KKPhim** (`KkPhim.kt`, API công khai phimapi.com) cho yanhh3d, hoathinh3d.ee, hhkungfu (nhúng chung
  streamfree.vip — hỏng là hỏng cả ba) và hoathinh3d.pw: mở tập là app tìm cùng phim (slug của trang, không khớp thì theo
  tên / tên gốc), lấy tập cùng số rồi thêm «KKPhim Vietsub / Thuyết Minh» vào menu ▼ — luồng HLS, app tự phát, bỏ quảng cáo.
  Trang chia bản thì chỉ thêm đúng ngôn ngữ đang xem; hoathinh3d.pw có thêm «Trình phát của trang». Tải song song với trang
  tập, KKPhim chậm / lỗi thì bỏ qua; trang lỗi mà KKPhim có tập thì vẫn xem được. Cài đặt → Phát video: «Server dự phòng
  KKPhim» (mặc định bật).
- **Thêm trang KKPhim** (www.kkphim.com; mọi dữ liệu gọi thẳng phimapi.com): trang chủ Hoạt hình Trung Quốc mới cập nhật /
  thuyết minh, Phim mới, Phim bộ / lẻ mới; menu thêm «Hoạt hình Trung Quốc thuyết minh», «Phim chiếu rạp»; Thể loại… /
  Quốc gia…, tìm kiếm, bản Vietsub / Thuyết Minh, server HLS (bỏ quảng cáo) + trình phát KKPhim. Có sẵn trong ☰ → Đổi
  trang phim (người đã cài được thêm một lần). Gõ «kkphim.com» khi thêm trang là trang KKPhim có sẵn (tên miền trần bị nhà
  mạng chặn, trước đây thành dòng trùng).
- Lưu ý: bản trên KKPhim có khi dài ngắn khác bản của trang (Tiên Nghịch tập 165: yanhh3d 23:41, KKPhim 18:49) — đổi server
  giữa chừng giữ số giây nên có thể lệch cảnh.
- versionCode 27 · tag `v2.6` · commit `b2a92de` · `ChimToTV-2.6.apk` 1 149 262 byte · SHA-256
  `15041c01e996dea66ccb358131d759412aeea26e178b787a579cecc76318928c`
- Thử bản gỡ lỗi trên Skyworth và TV box (server dự phòng cả 4 trang, Thuyết Minh / Vietsub đúng bản, trang lỗi vẫn phát,
  trang KKPhim, tắt / bật trong Cài đặt, gõ «kkphim.com»); bản chính thức thử trên TV box trước khi phát hành (yanhh3d phát
  được, có «KKPhim Vietsub» trong menu ▼). API `releases/latest` trả `v2.6` + tệp `.apk`; tệp tải từ link công khai khớp SHA-256.

## Windows 1.2 — 06/11/2026

- **Người xem**: mỗi người một lịch sử, Xem tiếp, chỗ đang xem, phim theo dõi, trang phim / server / cách xem riêng; khoá bằng mã
  PIN; hỏi «Ai đang xem?» khi mở app và khi rời app hơn 30 phút (lịch sử của bản 1.0 thuộc «Cả nhà»).
- **Giới hạn giờ xem** theo từng người (mỗi ngày 30 phút – 3 giờ, giờ đi ngủ tới 6 giờ sáng) + mã PIN phụ huynh; còn 5 phút thì
  nhắc; hết giờ phim dừng, cho xem thêm 15 / 30 / 60 phút hoặc tới 6 giờ sáng.
- **Cửa sổ phim nhỏ**: «▣ Thu nhỏ» / phím W / thu nhỏ cửa sổ app lúc xem → phim chạy tiếp ở cửa sổ nhỏ luôn nổi trên cùng.
- **Đồng bộ trong nhà** với TV 2.2+ và iPad / iPhone 1.3+ (cùng giao thức, theo từng người xem cùng tên).
- **Proxy SOCKS5** (đăng nhập, DNS của proxy / máy, toàn app / chỉ trang phim); proxy và vượt chặn đổi là có hiệu lực ngay.
- bản 1.2.0 (không có bản 1.1) · tag `win-v1.2` (không phải "latest") · commit `7209da7` · `ConBangTV-Windows-1.2.exe`
  72 792 073 byte · SHA-256 `f17b404f5b44b04acf29ba0db8c3c91a21578562cfd588197f7744ab4260966c`
- Đã thử trên máy Windows 10: 53 test lõi; cửa sổ phim nhỏ (thu nhỏ / phóng to / thu nhỏ cửa sổ app — phim chạy liền, không
  tải lại; tắt từ menu lưu đúng chỗ xem); thêm người xem, khoá PIN, mở lại app hiện «Ai đang xem?» nền kín; giới hạn 30 phút →
  phim dừng đúng lúc hết giờ, sai mã phụ huynh bị từ chối, đúng mã + «Thêm 15 phút» mở lại tập; nhắc «Còn 4 phút»; proxy SOCKS5
  giả trên máy (sai mật khẩu / đúng / sai cổng → báo lỗi, không đi thẳng; video cũng đi qua proxy); vượt chặn mức 1 / 2 với
  phimmois.fun; đồng bộ thật với TV box (TV 2.5): ghép bằng IP, hai chiều dữ liệu, xoá lan sang TV (rồi bỏ ghép).
  **Tự cập nhật thật 1.0 → 1.2 từ GitHub**: hộp thoại «Có bản mới 1.2» → Cập nhật ngay → mở lại 1.2, lịch sử còn nguyên.
  Chưa thử: TV / iPad gọi sang máy tính (tường lửa), iPad / iPhone, remote thật.

## Windows 1.0 — 06/11/2026

- **Bản đầu tiên cho máy Windows 10 / 11** (máy tính hoặc mini PC cắm tivi), dùng chuột hoặc bàn phím / remote: đủ 6 trang
  phim như bản TV (yanhh3d, hoathinh3d.pw / .ee, hhkungfu, phimmois → phimmoiw, mephim), trang chủ, menu mục của trang,
  Thể loại… / Quốc gia…, tìm trên mọi trang, chi tiết phim (bản Vietsub / Thuyết Minh, tập đã xem), Xem tiếp / Lịch sử,
  Theo dõi (báo tập mới), Đổi trang phim (đo nhanh, thêm / xoá trang), trình phát (server nhớ theo trang, xem tiếp đúng
  chỗ, tự chuyển tập, tốc độ, khung hình, nhạc mở đầu tự bỏ qua, HLS bỏ quảng cáo, chặn quảng cáo), toàn màn hình,
  cỡ giao diện 100–175%, **vượt chặn nhà mạng mức 1 / 2**, tự cập nhật từ GitHub.
- Chưa có: người xem / mã PIN, giới hạn giờ xem, cửa sổ phim nhỏ, đồng bộ trong nhà, proxy SOCKS5.
- bản 1.0.0 · tag `win-v1.0` (không phải "latest") · commit `9624e00` · tệp `ConBangTV-Windows-1.0.exe` 72 736 562 byte ·
  SHA-256 `bd2600678c1825ac4c788c6ac00f12519aa1daf46635db05886ccf4d34bd2938`
- Cài: tải tệp .exe, mở; Windows báo «Windows protected your PC» thì **More info → Run anyway** (app chưa có chữ ký số).
- Đã thử trên máy Windows 10 thật: 28 test lõi; phát phim ở yanhh3d (iframe), hoathinh3d.pw (mở thẳng trang tập), mephim
  (HLS), phimmoiw (HLS qua vượt chặn mức 1 và mức 2 — phimmois.fun bị nhà mạng chặn, tắt vượt chặn thì hết giờ chờ); phím
  mũi tên / Enter / Esc, chuột thật (bấm phát / dừng, bấm đúp toàn màn hình, bấm thanh tua); tự cập nhật trọn vẹn
  1.0 → 1.0.1 qua release giả; tệp tải từ link công khai khớp SHA-256, tự kiểm tra bản mới với GitHub thật. Chưa thử với
  remote thật, máy chưa có WebView2.

## iPad 1.5 — 05/11/2026

- **Thêm trang mephim.ddns.net** như TV 2.5 (`ApiSite.swift`): có sẵn trong Đổi trang phim, trang chủ / menu / Thể loại… /
  Quốc gia…, tìm kiếm, bản Vietsub / Thuyết Minh, server HLS (bỏ quảng cáo) + trình phát KKPhim, dự phòng phimapi.com.
- bản 1.5 (CFBundleVersion 6) · mã gói `vn.chimto.tv` · tag `ios-v1.5` · commit `e1294b0` · SHA-256 IPA
  `4e5d3f7fb9975d7a6dd52b167cea3290e3672a0f6a139433ffe42e3a9c063045`
- Test lõi trên macOS qua (thêm `ApiSiteTests`). **Chưa cài thử trên iPad / iPhone thật** (iPhone không nối SSH được lúc
  phát hành; anh tự thử sau).

## 2.5 — 05/11/2026

- **Thêm trang mephim.ddns.net** (ứng dụng React, API JSON `/api/movies/…`, nguồn KKPhim): có sẵn trong ☰ → Đổi trang phim
  (người đã cài được thêm một lần). Trang chủ: Hoạt hình Trung Quốc mới cập nhật, Phim mới cập nhật, Thịnh hành, Song ngữ /
  Thuyết minh; menu Hoạt hình Trung Quốc, Hoạt hình, Phim bộ, Phim lẻ, TV Shows, Thuyết minh, Lồng tiếng, 5 nước;
  «Thể loại…» / «Quốc gia…»; tìm kiếm mọi trang. Bản Vietsub / Thuyết Minh riêng; mỗi tập 2 server: **HLS (bỏ quảng cáo)**
  và **Trình phát KKPhim** (dự phòng, có quảng cáo chèn). Máy chủ của trang lỗi (hay báo 500) thì app gọi thẳng phimapi.com.
- versionCode 26 · tag `v2.5` · commit `e1294b0` · SHA-256 APK
  `26e879b19b76ad053011ddb0f2eebba373da2c96dbc685f034a627344ddec98c`
- Thử trên TV box (bản gỡ lỗi + bản chính): trang chủ, phân trang, Thể loại, tìm kiếm, Thuyết Minh, Xem tiếp, cả hai server;
  anh thử trên vivo X200 Pro. Cập nhật tự động 2.4.3 → 2.5 đã thử trên TV box.

## 2.4.3 — 05/11/2026

- **Cử chỉ chạm khi xem phim** (điện thoại, màn cảm ứng), như bản iPhone: **giữ tay** xem nhanh 2× tới khi nhấc tay;
  **vuốt ngang** để tua (giữa màn hiện giờ sẽ tới, hết bề ngang = 2 phút); **vuốt dọc nửa trái** chỉnh độ sáng (chỉ trong
  lúc xem), **nửa phải** chỉnh âm lượng; **chụm / mở hai ngón** vừa màn / lấp đầy. Vuốt từ sát mép trên / dưới để dành cho
  điện thoại. Remote trên TV không đổi.
- versionCode 25 · tag `v2.4.3` · commit `ae34126` · SHA-256 APK
  `8ae31e339430e2c08efe429a5663f4456ff2a33ba79d3dba398ec2de3e8d0e61`
- Thử trên TV box qua cổng gỡ lỗi (giữ tay → tốc độ 2 rồi về 1, vuốt ngang tua ~1 phút, độ sáng); anh thử trên vivo X200 Pro
  (bản debug5). Cập nhật tự động 2.4.2 → 2.4.3 đã thử trên TV box.

## 2.4.2 — 05/11/2026

- **Điện thoại màn dài (vd 20:9): xem phim không còn bị lẹm mất phần dưới hình.** Một số trang phát đặt khung video theo
  bề ngang (cao = ngang × 9/16) — trên màn dài hơn 16:9 khung cao hơn màn, phần dưới (phụ đề, chữ) bị cắt. Nay video tràn
  khung thì app ghim vừa màn, giữ tỉ lệ (viền đen hai bên); «Khung hình: Lấp đầy» vẫn như cũ. TV 16:9 không đổi.
- versionCode 24 · tag `v2.4.2` · commit `49b43b7` · SHA-256 APK
  `ec6f6290cd4e8fa18c2ad33fa3157ae33bbbf339cdd0c106c950683d3a7d2724`
- Anh thử trên vivo X200 Pro (bản debug) thấy hết lẹm. Cập nhật tự động 2.4.1 → 2.4.2 đã thử trên TV box.

## 2.4.1 — 05/11/2026

Sửa lỗi sau khi thử 2.4 trên điện thoại thật (vivo X200 Pro, Android 15):

- Cầm dọc: nút «‹ Quay lại» (Cài đặt, chi tiết phim) có dải riêng ở trên cùng, nội dung cuộn không còn chui xuống dưới nút.
- Menu ☰: mở ra vẫn thấy vài mục trước mục đang chọn (trước đây «Người xem», «Trang chủ» bị khuất phía trên).
- Android 15: chỗ thanh điều hướng cùng màu nền app (hết dải xám nhạt hệ thống tự phủ).
- Tìm kiếm trên điện thoại: Back tới thẳng bước «Nhấn Back lần nữa để thoát», không còn một lần bấm thừa.
- versionCode 23 · tag `v2.4.1` · commit `69ad195` · SHA-256 APK
  `b5f7437cc702d3ff4d03645318569b94a81e3dfb4ba28fd64ef5aa4b6ea99a0d`
- Đã thử trên vivo X200 Pro qua cổng gỡ lỗi của bản debug: màn chính, menu ☰, chi tiết phim, Cài đặt, Tìm kiếm, Duyệt web
  (cầm dọc), trình phát (ngang), xoay ngang. Cập nhật tự động 2.4 → 2.4.1 đã thử trên TV box.

## 2.4 — 05/11/2026

- **Dùng tốt trên điện thoại Android:** xoay dọc được (trước đây luôn nằm ngang). Cầm dọc thì màn chính giống bản iPhone:
  thanh trên cùng ☰ · tên mục · tên trang, bấm ☰ để mở menu trượt ra từ bên trái; lưới 3 cột. Chi tiết phim: poster nhỏ,
  hàng nút xuống dưới rộng cả màn. Trình phát luôn ngang, ẩn thanh điều hướng. Xoay lúc đang dùng thì tự đổi bố cục.
- Cài đặt: hàng nút tự xuống dòng khi không đủ chỗ (cả trên TV — trước đây nút cuối của hàng dài bị tràn ra ngoài),
  nút chữ dài không bị cắt; cầm dọc thì các ô proxy xếp dọc.
- «Ai đang xem?»: các ô tự xuống dòng, căn giữa; sửa lỗi có lúc phím ◀ ▶ của remote không chọn được người xem.
- Điện thoại Android 15: nội dung không bị tai thỏ và thanh điều hướng che; Duyệt web không bị bàn phím ảo che.
- versionCode 22 · tag `v2.4` · commit `9f78cef` · SHA-256 APK
  `9501df8fb614d9d3b51dafa7790fd56b03e59141a47c9b9b39d2ee511ab8b77f`
- Cập nhật tự động 2.3 → 2.4 đã thử trên TV box. Giao diện cầm dọc thử trên TV box giả lập màn điện thoại 360dp (sau đó thử
  trên điện thoại thật — xem 2.4.1).

## iPad 1.4 — 05/11/2026

- **Vượt chặn nhà mạng mức 2** (Cài đặt → Vượt chặn nhà mạng → Mức 1 / Mức 2), như TV 2.3: mẩu đầu của gói mở kết nối HTTPS
  gửi với TTL 1 (chết ở router trong nhà), phần sau đi trước, hệ thống tự gửi lại mẩu đầu — nhà mạng thấy gói thiếu đầu, sai
  thứ tự. Dành cho khi mức 1 không còn vào được trang bị chặn; qua proxy thì dùng cách mức 1.
- bản 1.4 (CFBundleVersion 5) · mã gói `vn.chimto.tv` · tag `ios-v1.4` · commit `4e9b37b` · SHA-256 IPA
  `47f4fe7f0a96d21a99f940b7ca56aaeba131ae20ce5baafe8608d6d88e905c74`

## 2.3 — 05/11/2026

- **Vượt chặn nhà mạng mức 2:** nút Cài đặt → Vượt chặn nhà mạng đổi vòng Tắt → Mức 1 (chia nhỏ) → Mức 2 (đảo thứ tự). Mức 2:
  mẩu đầu của gói mở kết nối HTTPS gửi với TTL 1 nên chết ở router trong nhà, phần sau đi trước, hệ thống tự gửi lại mẩu đầu —
  máy soi của nhà mạng thấy gói thiếu đầu, sai thứ tự. Chậm thêm vài chục mili giây; không cần VPN / root; qua proxy thì dùng
  cách mức 1. Thử trên TV box: phimmoir.fun (nhà mạng chặn theo tên) tắt vượt chặn thì lỗi, mức 2 vào được.
- versionCode 21 · tag `v2.3` · commit `4e9b37b` · SHA-256 APK
  `564656d089739e2d3fa47350bc68c67a29014f3e15a10cc200f760c73373dacf`
- Cập nhật tự động 2.2 → 2.3 đã thử trên TV box (Cài đặt → "Kiểm tra cập nhật" → "Cài đặt ngay").

## iPad 1.3 — 05/11/2026

- **Có bản cho iPhone:** cùng một tệp `ConBangTV-iPad-1.3.ipa` cài được cả iPhone và iPad. Lưới phim, trang phim, «Ai đang
  xem?», bàn phím mã PIN, màn «Hết giờ xem» co theo màn hẹp (dọc / ngang); trên iPhone trình phát luôn xem ngang, thanh nút
  gọn lại.
- **Widget «Xem tiếp»** trên màn hình chính (nhỏ 1 phim / vừa 3 phim / lớn 6 phim): phim đang xem dở của người xem đang dùng,
  chạm là mở app phát tiếp đúng chỗ.
- **Đồng bộ trong nhà** với TV (Côn Bằng TV 2.2+) hoặc iPad / iPhone khác (Cài đặt → Đồng bộ trong nhà): lịch sử xem, chỗ
  đang xem, tập đã xem, phim theo dõi, danh sách trang phim; ghép bằng mã 4 số, tự tìm máy cùng Wi‑Fi hoặc gõ IP; tự đồng
  bộ khi mở app / xem xong.
- **Cỡ chữ** (Cài đặt → Cỡ chữ: Nhỏ – Cực lớn).
- **Bàn phím rời / chuột:** mũi tên chọn phim / chọn tập, Enter mở, Delete xoá khỏi lịch sử, ⌘F / ⌘1–3 / ⌘R / ⌘O / ⌘E /
  ⌘, / Esc; trình phát thêm F / S / W; gõ mã PIN bằng bàn phím; rê chuột hiện thanh nút, cuộn để tua / chỉnh âm lượng.
- hoathinh3d.pw: các mục Phim Lẻ, Đang Chiếu, Lịch Chiếu, Hoàn Thành, Top 10, Đánh Giá Cao hiện phim trở lại (như TV 2.2).
- Cài đè giữ lịch sử xem và mật khẩu proxy đã lưu (IPA ký kèm đúng nhóm Keychain như bản cũ).
- bản 1.3 (CFBundleVersion 4) · mã gói `vn.chimto.tv` (widget `vn.chimto.tv.widget`) · tag `ios-v1.3` · commit `a4c405e` ·
  SHA-256 IPA `bdb0da1b50b381628050d95b9e4f2a75b569e10000967b0e0c8243328380eb54`

## 2.2 — 05/11/2026

- **Đồng bộ với iPad / iPhone** (Cài đặt → «Đồng bộ với iPad / iPhone…»): xem dở trên TV thì mở iPad / iPhone xem tiếp đúng
  chỗ, và ngược lại. Đồng bộ lịch sử xem, Xem tiếp, chỗ đang xem, tập đã xem, phim theo dõi, danh sách trang phim; người xem
  đầu tiên ghép với nhau, người khác ghép theo tên. Ghép bằng mã 4 số (chỉ dùng được lúc hộp thoại mở), mỗi lần đồng bộ ký
  bằng khoá chung; tự tìm máy cùng Wi‑Fi, không thấy thì nhập IP. Tự đồng bộ khi mở app / xem xong (tắt được), có «Đồng bộ
  ngay»; xoá / bỏ theo dõi ở máy này thì máy kia cũng xoá.
- **hoathinh3d.pw:** Phim Lẻ, Đang Chiếu, Lịch Chiếu, Hoàn Thành, Top 10 HH3D, Đánh Giá Cao trống vì trang đổi sang nạp phim
  bằng JavaScript — app đọc thẳng dữ liệu của trang (có trang sau; Lịch Chiếu chia theo ngày từ hôm nay, phim chiếu sớm ghi
  giờ; Top 10 ghi thứ hạng).
- versionCode 20 · tag `v2.2` · commit `a4c405e` · SHA-256 APK
  `54b448b7b168708737bbc6944dd783923afe1dac481cec81ea11da99168097e4`
- Cập nhật tự động 2.1 → 2.2 đã thử trên TV box (Cài đặt → "Kiểm tra cập nhật" → "Cài đặt ngay").

## iPad 1.2 — 05/11/2026

- **Proxy SOCKS5** (Cài đặt → Proxy SOCKS5), như bản TV: máy chủ / cổng / tên đăng nhập / mật khẩu (dán được
  `socks5://tên:mật-khẩu@máy:cổng`), DNS của proxy hoặc của iPad, dùng cho toàn bộ app hoặc chỉ trang phim, nút "Kiểm tra
  proxy" (báo IP ra ngoài). Mật khẩu lưu trong Keychain; ô mật khẩu không tự viết hoa, có nút con mắt để xem lại. Proxy
  báo "không cho phép" thì app nói rõ "thường do sai tên đăng nhập / mật khẩu".
- **Vượt chặn nhà mạng** (Cài đặt): vào được trang phim bị nhà mạng chặn theo tên trang mà không cần VPN; trang không vào
  được thì app gợi ý bật. Lúc bật proxy / vượt chặn, trên iPadOS 15–16 app không giữ cookie trang web sau khi tắt app.
- **phimmoir.fun đổi tên thành phimmois.fun:** danh sách trang, lịch sử, phim theo dõi, chỗ đang xem… tự đổi sang tên mới;
  trang tự chuyển tên miền thì app thay tên cũ trong danh sách (không thêm trùng).
- **Cử chỉ lúc xem:** vuốt ngang để tua (hiện giờ tới), vuốt dọc nửa trái chỉnh độ sáng / nửa phải chỉnh âm lượng, giữ tay
  xem nhanh 2×, chụm / mở hai ngón: vừa / lấp đầy màn hình. Vuốt bắt đầu sát mép trên / dưới vẫn để cho iPad.
- **Màn hình khoá, Trung tâm điều khiển, AirPods:** tên phim, tập, ảnh phim; phát / dừng, tua, kéo thanh thời gian, tập
  trước / sau (AirPods chạm hai lần: tập sau).
- **Giới hạn giờ xem** cho từng người xem (Cài đặt → Người xem → "Giới hạn giờ xem…"), như bản TV 2.1.
- **Chọn trang phim:** thêm lại trang mẫu đã xoá, gõ tên miền để thêm trang khác, vuốt trái một trang để xoá.
- **Tìm kiếm trên mọi trang:** trước đây chỉ ra kết quả của một trang và chậm — nay kết quả hiện dần theo từng trang,
  tối đa 3 trang cùng lúc, mỗi trang chờ tối đa 12 giây.
- **Hình trong hình:** vuốt lên từ mép dưới lúc phim đang phát thì chắc chắn thu nhỏ phim (lần vuốt đầu dành cho app);
  phim dừng / chưa chạy thì vuốt lên về thẳng màn hình chính. Tên người xem luôn hiện ở đầu menu.
- bản 1.2 (CFBundleVersion 3) · mã gói `vn.chimto.tv` · tag `ios-v1.2` · commit `52a15e9` · SHA-256 IPA
  `fd412842d36c0f51b1242d5b49d43cf204b60b999177551d3e383dab804e2cf8`

## 2.1 — 05/11/2026

- **Vượt chặn nhà mạng** (Cài đặt → Vượt chặn nhà mạng): một số nhà mạng chặn trang phim bằng cách đọc tên trang lúc mở
  kết nối HTTPS (trang mở mãi không được, dùng VPN thì vào được). Bật mục này: app chia nhỏ gói mở kết nối nên nhà mạng
  không đọc được tên trang — không cần VPN hay proxy; dùng được cùng DNS mã hoá và proxy. Trang không vào được (quá giờ /
  lỗi SSL) thì app gợi ý bật mục này.
- **phimmoir.fun đổi tên thành phimmois.fun:** trang mẫu đổi theo; danh sách trang, trang đang dùng của từng người xem,
  lịch sử, Xem tiếp, phim theo dõi, chỗ đang xem, server ưu tiên, điểm server tự đổi sang tên mới khi mở app. Trang tự
  chuyển tên miền thì app thay tên cũ trong danh sách (trước đây thêm tên mới bên cạnh).
- **Giới hạn giờ xem** cho từng người xem (Cài đặt → Người xem → "Giới hạn giờ xem…"): đặt **mã PIN phụ huynh**, rồi chọn
  số phút được xem mỗi ngày (30 phút – 3 giờ) và giờ đi ngủ (không xem được tới 6 giờ sáng). Chỉ đếm lúc phim thật sự chạy;
  còn 5 phút thì nhắc; hết giờ thì phim dừng, màn "Hết giờ xem" cho người lớn nhập mã để cho xem thêm 15 / 30 / 60 phút
  hoặc không giới hạn tới 6 giờ sáng. Đã có giới hạn thì thêm / xoá người xem cũng cần mã phụ huynh.
- **Đổi trang phim:** thêm "＋ Thêm lại: …" (trang mẫu đã xoá) và "＋ Thêm trang khác…" (gõ tên miền); Cài đặt có nút
  thêm lại trang mẫu.
- **Proxy:** proxy trả "không cho phép" khi có đăng nhập thì app báo "thường do sai tên đăng nhập / mật khẩu" (nhiều proxy
  nhận mọi mật khẩu lúc đăng nhập rồi mới từ chối).
- **Tìm kiếm trên mọi trang:** kết quả hiện dần theo từng trang (ghi rõ trang nào đang tìm / không có phim / không trả
  lời); hỏi cùng lúc tối đa 3 trang, mỗi trang chờ tối đa 12 giây — một trang chậm hay bị chặn không làm chậm cả lượt tìm.
- Mục **Người xem** luôn hiện ở đầu menu, kể cả khi mới có một người xem.
- versionCode 19 · tag `v2.1` · commit `52a15e9` · SHA-256 APK
  `0b7c1aad0b849a4270eebbbdb6e2fa95a0f3988a98cd4b8fe5ea2bf0c53f5d2d`
- Cập nhật tự động 2.0 → 2.1 đã thử trên TV box (Cài đặt → "Kiểm tra cập nhật" → "Cài đặt ngay").

## iPad 1.1 — 05/11/2026

- **Người xem cho cả nhà** (như bản TV 2.0): mỗi người một hồ sơ (tên + màu) với lịch sử xem, Xem tiếp, chỗ đang xem, phim
  theo dõi, trang phim, server ưu tiên, Xem riêng, cách xem riêng; lịch sử cũ thành của người đầu tiên "Cả nhà". Mở app
  (và quay lại sau 30 phút) hỏi **"Ai đang xem?"** khi có từ 2 người; đổi người bằng tên người xem ở đầu menu; thêm / sửa /
  xoá ở Cài đặt → Người xem. **Mã PIN** 4 số tuỳ từng người, gõ trên bàn phím số ngay trên màn hình.
- **Hình trong hình:** nút thu nhỏ cạnh nút Menu của trình phát — phim chạy tiếp ở cửa sổ nổi của iPad, vẫn chọn phim khác
  / dùng app khác được; nút phóng to của cửa sổ mở lại màn xem phim, nút đóng lưu chỗ đang xem. Vuốt về màn hình chính
  lúc đang xem là tự thu nhỏ (tắt được: Cài đặt → Phát video).
- bản 1.1 (CFBundleVersion 2) · mã gói `vn.chimto.tv` · tag `ios-v1.1` · commit `0edf596` · SHA-256 IPA
  `c2ce6ebe992451ad80b3f53927d8ccd782fd156ba11a669fc8719fe7bb7239d1`

## 2.0 — 05/11/2026

- **Người xem cho cả nhà:** TV dùng chung thì mỗi người một hồ sơ (tên + màu, tối đa 8) với lịch sử xem, hàng Xem tiếp,
  chỗ đang xem dở, phim theo dõi, trang phim đang dùng, server ưu tiên, Xem riêng, bước tua, tự chuyển tập, khung hình
  riêng. Danh sách trang phim, proxy, DNS, dữ liệu duyệt web, điểm server, mốc nhạc mở đầu dùng chung.
  - Lịch sử có từ trước thành của người xem đầu tiên "Cả nhà" — cài đè không mất gì.
  - Nhà có từ 2 người: mở app (và quay lại app sau 30 phút không dùng) thì hỏi **"Ai đang xem?"** — tắt được trong Cài
    đặt. Đổi người: mục "Người xem: …" đầu menu. Thêm / đổi tên / đổi màu / xoá: Cài đặt → Người xem.
  - **Mã PIN** 4 số bật / tắt cho từng người: người có mã phải nhập đúng mới mở / sửa / xoá được hồ sơ.
- **Thu nhỏ phim vào góc:** lúc xem nhấn ▼ → "Thu nhỏ vào góc" (hoặc phím Window) — phim chạy tiếp ở góc, vẫn chọn phim
  khác được; đầu menu có "Phóng to" / "Tắt phim thu nhỏ", phím phát / dừng / chuyển tập của remote điều khiển phim nhỏ.
  - TV / máy có hình trong hình của Android: cửa sổ nhỏ của hệ thống (nổi cả trên app khác); bấm Home lúc đang xem là tự
    thu nhỏ (tắt được trong Cài đặt).
  - TV không có (TV box ít RAM như TX3, TV Skyworth…): cửa sổ phim nhỏ ngay trong app — rời app thì phim dừng, thoát app
    thì phim tắt.
- Chọn phim khác lúc đang thu nhỏ thì phim đó mở ngay trong trình phát đang chạy (lưu chỗ xem phim cũ).
- versionCode 18 · tag `v2.0` · commit `0edf596` · SHA-256 APK
  `bf86165ba925f40d959f8df9cc0db359d00ea66f9657c6eb75a6ba65f6078c33`

## iPad 1.0 — 04/11/2026

- **Bản đầu tiên cho iPad** (iPadOS 15 trở lên, cài bằng TrollStore — tải `ConBangTV-iPad-1.0.ipa`, mở bằng TrollStore):
  cùng 5 trang phim và các tính năng chính của bản TV, điều khiển bằng chạm và bàn phím rời — trang chủ, thể loại, lịch
  chiếu, tìm trên mọi trang, trang phim (Việt Sub / Thuyết Minh, đến tập), Xem tiếp, phim theo dõi + Có tập mới, Đổi
  trang phim (đo tốc độ), Duyệt web, Xem riêng, tự xoá dữ liệu web.
- Trình phát: chạm để hiện thanh nút (tập trước / sau, lùi / tới 10 giây, dừng / phát, Menu, Thoát), kéo thanh thời gian,
  chạm đúp hai bên để tua; menu đổi server (kèm điểm), đổi bản, nhạc mở đầu, tốc độ, khung hình, hẹn giờ tắt; xem tiếp
  đúng chỗ, tự sang tập, gợi ý đổi server khi giật, chặn quảng cáo; trình phát phủ kín màn iPad.
- Tự kiểm tra bản iPad mới trên GitHub và mở TrollStore để cài đè.
- Chưa có: proxy SOCKS5, DNS mã hoá (iOS 15 không cho app đặt riêng cho trình duyệt trong app).
- bản 1.0 (CFBundleVersion 1) · mã gói `vn.chimto.tv` · tag `ios-v1.0` · commit `2ac27fc` · SHA-256 IPA
  `649f3d49c33959ec71428bb299757ebf02a64597b29d6541c8da9a5cff6384d1`

## 1.9.4 — 04/11/2026

- **Hàng Xem tiếp / Lịch sử xem:** phim của hoathinh3d.ee và hoathinh3d.pw trước đây cùng ghi "· hoathinh3d" — nay ghi
  đủ tên miền (vd "Linh Cảnh Hành Giả · hoathinh3d.pw"); phim của các trang khác vẫn ghi tên ngắn.
- Chặn thêm quảng cáo trong trang tập của hoathinh3d.pw (`atroblxac.in`, gặp khi thử bản iPad).
- versionCode 17 · tag `v1.9.4` · commit `2ac27fc` · SHA-256 APK
  `fbb10fedfaa0effe4764e1165e8cedc36736bf2c12de9d55b030bdd9eb0d95b6`

## 1.9.3 — 04/11/2026

- **Điều khiển bằng màn hình cảm ứng** (TV box nối màn cảm ứng, điện thoại) — trước đây màn xem phim không điều khiển
  được bằng chạm và cả app không có nút quay lại trên màn hình:
  - Màn xem phim: chạm vào phim để hiện / ẩn thanh nút to — Tập trước, lùi 10 giây, Dừng / Phát, tới 10 giây, Tập sau,
    Menu (menu ▼: server, bản phim, tốc độ, nhạc mở đầu…), Thoát — và thanh thời gian kéo để tua; chạm đúp nửa trái /
    phải: lùi / tới 10 giây. Thanh tự ẩn sau 5 giây, phim đang dừng thì ở lại. Chạm không còn lọt vào trang web / trình
    phát của trang (khỏi bấm nhầm quảng cáo).
  - Các màn khác: có chạm thì hiện nút **"‹ Quay lại"** ở góc trên (trang chủ: **"Thoát"**; Duyệt web: góc dưới trái,
    khỏi che thanh đầu của trang web); bấm remote / bàn phím là nút tự ẩn. Duyệt web ẩn con trỏ của remote khi đang chạm.
  - Remote và bàn phím dùng như cũ; nút trên màn hình đổi màu khi đang nhấn.
- versionCode 16 · tag `v1.9.3` · commit `d0f531c` · SHA-256 APK
  `05e4ddaa3d1c898595bce4595b4c7f33a4e4ae9eba91330169860f75518699bf`

## 1.9.2 — 04/11/2026

- **Sửa lỗi phím ESC của bàn phím rời** (điện thoại / TV box cắm bàn phím): ESC giờ luôn là Back ở mọi màn hình —
  thoát được Cài đặt và màn xem phim (anh báo trên Samsung S10e gắn bàn phím). Trước đây app trông vào việc Android tự
  đổi ESC thành Back, mà Android chỉ đổi khi không ai nhận ESC trước: trong màn xem phim, WebView chuyển ESC cho trang
  web / trình phát; trong Cài đặt, ô nhập (bàn phím ảo) có thể nhận. Giờ app tự xử lý ESC; trong màn xem phim, ESC khi
  đang mở menu ▼ thì đóng menu, đang hiện gợi ý đổi server thì bỏ gợi ý. Phím Back của remote không đổi.
- versionCode 15 · tag `v1.9.2` · commit `5d2a70b` · SHA-256 APK
  `90c18b47f147f7362df35d684262665ea47da9b24e739612b1e5cc6fc0bb095b`

## 1.9.1 — 04/11/2026

- **Tìm trên mọi trang:** mục Tìm kiếm tìm cùng lúc trên mọi trang trong danh sách, mỗi trang một nhóm ("yanhh3d.ee —
  18 phim"), trang đang dùng đứng đầu; trang lỗi / quá 20 giây thì bỏ qua.
- **Menu ▼ trong trình phát:** **Tốc độ** 1× → 1,25× → 1,5× → 2× → 0,75× (cho cả lần xem này); **Khung hình**: Vừa màn
  hình / Lấp đầy (phim màn ảnh rộng 21:9 phóng cho hết viền đen, cắt bớt hai bên; app nhớ lựa chọn); **Hẹn giờ tắt**:
  30 / 60 / 90 phút hoặc hết tập này — hết giờ thì dừng phim, 60 giây không ai bấm phím thì ra màn hình chính của TV.
- **phimmoir.fun:** menu có thêm **Thể loại…** (đủ 36 thể loại) và **Quốc gia…** (mọi nước, bỏ mục rác như năm / mã).
- **Xem tiếp nhanh hơn:** app báo trước chỗ đang xem cho trình phát (trình phát streamfree của yanhh3d / hoathinh3d.ee /
  hhkungfu và server KK Phim) → tải thẳng đoạn video ở chỗ đó thay vì tải đoạn đầu tập rồi mới tua (mỗi đoạn trên CDN
  có khi mất 6–7 giây trên TV box); phim đã đánh dấu nhạc mở đầu cũng bắt đầu thẳng ở chỗ hết nhạc. NguonC vẫn tua như
  cũ.
- **Riêng tư:** **DNS mã hoá (DoH)** trong Cài đặt (Cloudflare 1.1.1.1 / Google 8.8.8.8, qua HTTPS, cả khi không bật
  proxy; lỗi thì báo, không quay về DNS nhà mạng); **mật khẩu proxy mã hoá** bằng khoá Android Keystore (Android 6+, tự
  chuyển từ bản lưu chữ thường); chặn thêm trình theo dõi / quảng cáo đo được trên 5 trang (thống kê Cloudflare, JW
  Player, Ahrefs, quảng cáo Adcash…).
- versionCode 14 · tag `v1.9.1` · commit `c3e3da7` · SHA-256 APK
  `48f6375241806c3023d411c2bff64a3dc7526485962657e7dffdce00b35011e8`

## 1.9 — 04/11/2026

- **Phim theo dõi + báo tập mới:** trang phim có nút "＋ Theo dõi"; menu có mục **Phim theo dõi** (ghi "Theo dõi (2 mới)"
  khi có phim ra tập). App hỏi lại trang các phim đang theo dõi khi mở trang chủ (tối đa 30 phút một lần); phim có tập
  mới hiện ở hàng **"Có tập mới"** đầu trang chủ (vd "Tập 166 mới", "+2 · Tập 168") kèm thông báo; mở phim là hết "mới".
  Giữ OK / nhấn Menu trên phim theo dõi để bỏ theo dõi.
- **Bỏ qua nhạc mở đầu:** lúc xem nhấn ▼ → hàng "Nhạc mở đầu": "Bắt đầu ở …" (nếu nhạc không nằm ngay đầu tập) và
  "Hết nhạc ở …". Mốc lưu cho cả phim; các tập sau vừa chạy tới mốc đầu là tự tua qua (◀ để xem lại). Xem tiếp từ giữa
  tập thì không tua; "Bỏ mốc" để tắt.
- **Sửa lỗi trên TV chậm:** xem tiếp một tập xem dở mà thoát / chuyển tập trước khi trình phát kịp tua tới chỗ cũ thì
  chỗ xem dở bị ghi đè bằng giây đầu tập; chuyển tập có thể ghi giờ của tập cũ sang tập mới (trang tập cũ còn trả lời
  vài giây). Cả hai đã sửa.
- versionCode 13 · tag `v1.9` · commit `5c5bf8c` · SHA-256 APK
  `b221ffc045d4c9161f60f64211b3588bdc9fade237d6030247441bca2d18be7a`

## 1.8.2 — 04/11/2026

- **Hàng "Xem tiếp"** ở đầu trang chủ (5 phim xem gần nhất, mọi trang): bấm là phát tiếp đúng chỗ, tập đã xem hết thì
  sang tập kế; xem xong quay ra hàng tự cập nhật. Phim của trang khác ghi thêm tên trang (vd "Tiên Nghịch · yanhh3d").
- **Xoá từng phim khỏi lịch sử:** giữ OK (hoặc nhấn Menu) trên phim ở Xem tiếp / Lịch sử xem — xoá cả chỗ đang xem
  các tập của phim đó.
- **Đến tập theo số:** trang phim có nút "Đến tập…" (phim trên 20 tập) và nhận phím số trên remote (gõ 1 2 0 → Tập 120).
- **Cài đặt → Dữ liệu:** "Xem riêng" (không lưu lịch sử / chỗ đang xem) và "Tự xoá dữ liệu web khi rời app" (mặc định
  tắt).
- **Riêng tư:** không sao lưu dữ liệu app lên đám mây / sang máy mới / qua `adb backup`; WebView không gửi số liệu sử
  dụng của app về Google.
- Sửa lỗi văng trên TV Android 5 (màu chữ / ảnh bìa dùng hàm chỉ có từ Android 6).
- versionCode 12 · tag `v1.8.2` · commit `99513d3` · SHA-256 APK
  `71b83c314b6bb3e414415b6da48c531daef6d194fffb9d15ee03c2520068efbb`

## 1.8.1 — 04/11/2026

- **Xoá dữ liệu web nhanh:** mục mới **Xoá dữ liệu web** ở menu chính (ngay trên Cài đặt), hỏi xác nhận một lần rồi
  xoá cookie, dữ liệu các trang đã lưu và bộ nhớ đệm của trình duyệt trong app. Lịch sử xem, chỗ đang xem dở, cài đặt
  và proxy giữ nguyên.
- Nút trong Cài đặt đổi thành **"Xoá ảnh & dữ liệu duyệt web"**: trước chỉ xoá ảnh và cookie, nay xoá cả dữ liệu
  trang và bộ nhớ đệm.
- **Riêng tư hơn:** cả app dùng chung một User-Agent rút gọn, không còn gửi tên mẫu TV hay số bản build Android;
  client hints ghi "Chromium" thay vì "Android WebView"; khi app tự tải trang hộ trình phát thì không gửi tên gói app.
  (Yêu cầu do WebView tự gửi vẫn kèm tên gói trên WebView 138 — giới hạn của WebView.)
- versionCode 11 · tag `v1.8.1` · commit `5700ff5` · SHA-256 APK
  `8941f6eb1c200608252592a00bdf42d6c72c82b324b1ea0aabfae5f09730e1b1`

## 1.8.0 — 03/11/2026

- **Proxy SOCKS5** (Cài đặt → Proxy SOCKS5, mặc định tắt): bật / tắt; proxy có hoặc không tên đăng nhập / mật khẩu
  (dán được cả `socks5://user:pass@máy-chủ:cổng`); **DNS của proxy** (proxy tự tìm địa chỉ trang, vượt chặn tên miền
  của nhà mạng) hoặc **DNS của thiết bị**; **dùng cho toàn bộ app** hoặc **chỉ trang phim** (video, ảnh CDN, cập nhật
  app đi thẳng mạng nhà). Nút "Kiểm tra proxy" báo IP ra ngoài, nơi đặt máy và thời gian.
- Proxy lỗi (chết, sai mật khẩu) thì app báo rõ, không tự đi thẳng; màn hình chính ghi "· proxy" khi đang bật.
- Máy chủ video có thể chặn IP của proxy (vd CDN của KK Phim trên phimmoir.fun): app gợi ý chọn "chỉ trang phim";
  điểm server khi xem qua proxy được chấm riêng, không làm app né server đó lúc xem thẳng.
- versionCode 10 · tag `v1.8.0` · commit `e0f1662` · SHA-256 APK
  `fda0f275eeb8c889a9af6e73bc502391f4722a329f74c3eb55104b0c6939af16`

## 1.7.1 — 03/11/2026

Sửa lỗi của bản 1.7 (rà soát code sau phát hành):

- Server không phát được thì thông báo lỗi không còn bị lời nhắc "Video chưa chạy?" đè mất sau 25 giây; lời nhắc
  cũng không bật ngay sau khi trình phát vừa chạy xong quảng cáo dài.
- Mạng chập chờn lúc mở tập không còn bị coi là server hỏng (app tự thử tải lại vài lần trước khi báo lỗi); đang
  xem phim dài mà mạng rớt nhiều lần cách xa nhau thì vẫn tự phát tiếp.
- Thoát / đổi server lúc trình phát đang chạy quảng cáo không còn làm server bị chấm "hay lỗi".
- hoathinh3d.pw: video nạp xong mà không tự phát thì app nhắc nhấn OK, không còn kẹt ở màn "Đang mở trang tập phim…".
- phimmoir.fun: không mất nhóm Thuyết Minh / Lồng Tiếng của phim mà bản đó không bắt đầu từ tập 1; một ảnh bìa lỗi
  không làm hỏng cả danh sách; khối quảng cáo nằm cuối phim không làm hỏng thanh thời gian / tự sang tập.
- Trang tải lỗi hẳn (tên miền chết…) không còn bắt chờ gấp đôi; Cài đặt ghi thêm trang kiểu PhimMoi.
- versionCode 9 · tag `v1.7.1` · commit `a929b1d` · SHA-256 APK
  `f8beb32160be9fc7e1a5b7e2ff7fb4e37f8ccb8b0e0824ad85787d41ef7db78e`

## 1.7 — 03/11/2026

- **Trang phim mới phimmoir.fun** (chọn ở **Đổi trang phim**; TV đã cài tự có trang này trong danh sách sau khi cập
  nhật): hoạt hình 3D Trung Quốc — khối "Hoạt hình Trung Quốc mới cập nhật" ở đầu trang chủ — và cả phim bộ / phim
  lẻ Hàn, Trung, Âu Mỹ, Nhật, Thái. Menu có Hoạt hình, Phim bộ mới, Phim lẻ mới, Phim Hàn Quốc / Trung Quốc / Âu
  Mỹ / Nhật Bản / Thái Lan và 10 thể loại; tìm kiếm theo tên. Có Vietsub, Thuyết Minh, Lồng Tiếng và Song Ngữ (có
  phụ đề tiếng Việt).
- **Video mở nhanh, không quảng cáo cờ bạc:** server KK Phim mở khoảng 5 giây trên TV box (trang yanhh3d ~17 giây);
  app tự bỏ đoạn quảng cáo 30 giây chèn giữa phim. Server NguonC: app tự bỏ qua quảng cáo trước phim (~7 giây, trước
  phải xem hết hơn 2 phút) và xem tiếp đúng chỗ.
- Xem tiếp đúng chỗ cả khi trình phát chạy quảng cáo trước; server không phát được (vd OPhim đang hỏng) thì app báo
  ngay, mở menu đổi server, và lần sau tự mở server khác cùng ngôn ngữ.
- Đổi Vietsub ↔ Thuyết Minh mà bản kia chưa có tập đang xem thì app báo rõ, mở tập gần nhất của bản đó (không mang
  giờ đang xem sang tập khác).
- Lưu ý: bản Vietsub KK của một số phim hoạt hình là video rộng 2542 điểm — TV box yếu (TX3) rớt nhiều khung hình,
  hơi khựng; chọn Thuyết Minh hoặc NguonC nếu bị. TV Skyworth (đã chạy tốt 4K) không ảnh hưởng.
- versionCode 8 · tag `v1.7` · commit `b103bbf` · SHA-256 APK
  `5166cd79293ebad818ad9497541eda98c76a90261ac152637d588690477e920a`

## 1.6 — 03/11/2026

- **Video 1080 mượt hơn trên yanhh3d.ee, hoathinh3d.ee, hhkungfu.ee** (3 trang dùng chung trình phát streamfree): trình
  phát này trước chỉ tải trước đúng 1 đoạn phim (1–13 giây), mạng TV hụt một nhịp là đứng hình; nay tải trước tới
  2 phút. Đo trên TV box TX3, cùng tập, cùng server Thuyết minh 1080, 3 phút: trước giật 12 lần (18 giây), nay 0 lần.
- Thời gian mở phim không đổi; hoathinh3d.pw (trình phát riêng) không bị ảnh hưởng.
- versionCode 7 · tag `v1.6` · commit `6dc1522` · SHA-256 APK
  `774a9395501e5d623be6614020c890ffd75c5039f5c5f32a977c8a8faaee661d`

## 1.5 — 02/11/2026

- **ChimTo TV đổi tên thành Côn Bằng TV** (鯤鵬 — chim thần khổng lồ trong truyện tiên hiệp). Vẫn là app cũ: cài đè, giữ
  nguyên lịch sử xem, cài đặt và điểm server; sau khi cập nhật, tên và banner trên màn hình chính của TV đổi theo.
- **Logo mới:** ấn triện đỏ chữ 鵬 trong vòng bút mực vàng; banner mới "Côn Bằng TV".
- **Liên hệ:** Cài đặt → Liên hệ → nhóm Telegram [t.me/tivikit](https://t.me/tivikit) (mở ngay trong app).
- Mã gói, khoá ký và tên tệp APK (`ChimToTV-1.5.apk`) giữ nguyên, nên bản 1.3 / 1.4 tự cập nhật lên như thường.
- versionCode 6 · tag `v1.5` · commit `83bc2a5` · SHA-256 APK
  `2908a6c60c7f72058c83237934044f1fd7a3e6166f5552c6778629456c8e4497`

## 1.4 — 02/11/2026

- **Biết trang nào nhanh hơn:** mở **Đổi trang phim** là app đo cùng lúc mọi trang: tải trang chủ mất bao lâu, đọc được
  bao nhiêu phim (0 phim = có thể đang bị nhà mạng chặn), trang nhanh nhất có dấu ★, kèm điểm video các lần xem trước.
- **Chấm điểm server video:** app tự ghi lại mỗi server mở mất bao lâu, giật mấy lần, đứng hình bao lâu (8 lần xem gần
  nhất, riêng từng TV). Trong lúc xem nhấn ▼ để thấy điểm từng server (vd "mượt, mở 4 s") và tình hình lần xem này; TV
  rớt nhiều khung hình thì app báo "TV xử lý không kịp, đổi server không đỡ".
- **Gợi ý đổi server khi giật nhiều** (từ 3 lần trong 3 phút, hoặc đứng hình liền 10 giây): **OK** để chuyển sang server
  tốt hơn và xem tiếp đúng chỗ, **Back** để bỏ qua. Chỉ gợi ý server cùng ngôn ngữ (không tự đổi Vietsub ↔ Thuyết minh)
  và không gợi ý server nét hơn (4K) khi chưa thử.
- Mở tập mới: app vẫn dùng server đã chọn, chỉ đổi khi server đó hay giật mà có server khác đã chấm tốt (có báo lý do).
- Bản 1.3 tự cập nhật lên (hoặc Cài đặt → "Kiểm tra cập nhật"); bản 1.2 trở về trước cài tay một lần.
- versionCode 5 · tag `v1.4` · commit `db9e94d` · SHA-256 APK
  `27b3529ba5b0546d4d099357fee3adc510a6e12e26a5a81861f4c641c95e4523`

## 1.3 — 02/11/2026

- **Tự động cập nhật (OTA):** app tự tìm bản mới trên GitHub khi mở (tối đa 2 giờ một lần), tải sẵn, kiểm tra cùng khoá ký
  rồi hỏi "Cài đặt ngay". Lần đầu Android hỏi cho phép "cài ứng dụng không rõ nguồn"; mỗi lần cập nhật cần bấm xác nhận
  (Android 12+ thì từ lần thứ hai tự động hẳn). Cài đặt có nút "Kiểm tra cập nhật" và công tắc tự động cập nhật.
- **Lưới phim mượt hơn trên TV yếu:** khung hình giật khi di chuyển giảm từ 3,6–7,3% xuống 0–1,3%, khung chậm nhất từ
  48 ms xuống 16 ms (đo trên TV Skyworth SWTV-22AE).
- **Lưu ý:** TV đang dùng bản 1.2 trở về trước phải cài tay 1.3 một lần; từ đó các bản sau tự cập nhật.
- versionCode 4 · tag `v1.3` · commit `b194ecb` · SHA-256 APK
  `eac3d276d7b26f94e54aa13a6bf62eb87b9da7dde73ca33b072d5c7c50fdc755`

## 1.2 — 02/11/2026

- **Nhiều trang phim:** yanhh3d.ee, hoathinh3d.ee, hhkungfu.ee, hoathinh3d.pw — chọn ở mục **Đổi trang phim**; thêm trang
  mới cùng kiểu (yanhh3d / theme HaLim) trong Cài đặt.
- Trang chia tập theo bản: chọn **Việt Sub / Thuyết Minh** ở trang phim hoặc trong menu ▼ khi đang xem.
- hoathinh3d.pw phát bằng cách mở thẳng trang tập rồi phóng trình phát ra toàn màn hình (trình phát có khoá xác thực riêng).
- Server ưu tiên nhớ theo từng trang; tự gỡ kẹt khi video treo sau lúc tua.
- versionCode 3 · tag `v1.2` · commit `8cb96ef` · SHA-256 APK
  `ce71d9b99264a4cb3c9104d1be96e405e7356cc5f31906d8352f71003a991e98`

## 1.1 — 02/11/2026

- **Logo mới** phong cách hoạt hình 3D tiên hiệp (ấn triện chữ 仙 trong vòng bút mực vàng): banner TV đủ cỡ 720p → 4K,
  icon thích ứng Android 8+, logo ở đầu menu.
- **Poster nét hơn trên TV 4K:** tải ảnh gốc thay cho ảnh thu nhỏ 300×450.
- versionCode 2 · tag `v1.1` · commit `319113b` · SHA-256 APK
  `370b783d56efd68f2063da98b7356336724ec286f4788df5bb380e5306e9da38`

## 1.0 — 02/11/2026

- Bản đầu tiên: giao diện TV (menu thể loại, lưới poster, tìm kiếm, lịch chiếu theo ngày, lịch sử xem), trang phim với
  "Xem tiếp", trình phát điều khiển bằng remote / bàn phím (dừng–phát, tua, đổi server, nhớ vị trí, tự chuyển tập),
  chế độ Duyệt web bằng con trỏ ảo, Cài đặt đổi link trang phim.
- versionCode 1 · tag `v1.0` · commit `5d9b87b` · SHA-256 APK
  `94975cd3220a96b31e24d5b41af0a7892d33c3a91dcc9c25343c8fd58f997e24`
