using System.Globalization;
using System.IO;
using System.Reflection;
using System.Text.Json;
using System.Text.Json.Nodes;
using System.Text.RegularExpressions;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Interop;
using System.Windows.Media;
using System.Windows.Threading;
using ConBangTV.Core;
using ConBangTV.UI;
using Microsoft.Web.WebView2.Core;
using Microsoft.Web.WebView2.Wpf;

namespace ConBangTV.Player;

/// Trình phát (bản Android: PlayerActivity.kt). Video phát trong WebView2 bằng đúng cách của bản TV:
/// - trang có server (player.php / Next.js / API): trang bọc (player.html) chứa iframe trình phát của trang, hoặc hls.html
///   tự phát luồng .m3u8 (bỏ quảng cáo chèn giữa phim). Trang bọc được trả lời tại chính địa chỉ trang tập (như
///   loadDataWithBaseURL của Android) để trình phát nhúng thấy Referer của trang phim.
/// - trang không chia server (pageMode): mở thẳng trang tập, page-focus.js phóng trình phát ra cả màn hình.
/// player-agent.js chạy trong mọi khung (AddScriptToExecuteOnDocumentCreated), điều khiển &lt;video&gt; và báo trạng thái.
public sealed class PlayerView : UserControl
{
    private static readonly double[] Rates = { 0.75, 1.0, 1.25, 1.5, 2.0 };

    /// CSS của agent (agent thêm lúc tài liệu vừa tạo có thể chưa có &lt;head&gt; → thêm lại khi trang dựng xong).
    private const string StyleShim =
        "(function(){var css='.jw-controlbar,.jw-title{display:none!important}.chimto-pin{position:fixed!important;left:0!important;" +
        "top:0!important;right:auto!important;bottom:auto!important;width:100vw!important;height:100vh!important;max-width:none!important;" +
        "max-height:none!important;margin:0!important;transform:none!important;z-index:2147483646!important;background:#000!important}';" +
        "function add(){try{if(document.getElementById('chimto-css'))return;var s=document.createElement('style');s.id='chimto-css';" +
        "s.textContent=css;(document.head||document.documentElement).appendChild(s);}catch(e){}}" +
        "if(document.documentElement)add();document.addEventListener('DOMContentLoaded',add);})();\n";

    /// Trình phát native: trình phát nhúng (JW Player) tự dựng link luồng — đọc link đó trong khung nhúng rồi báo lên khung trên
    /// cùng (window.__chimtoSrc = {u: link, p: trang của khung, s: phụ đề, h: là HLS}); app kiểm trước, phát được thì chuyển sang
    /// mpv (như bản Linux — app/src/player.rs).
    private const string ExtractJs = """
        (function(){
          if (window.__chimtoExtract) return; window.__chimtoExtract = 1;
          if (window.top === window) {
            window.addEventListener('message', function (e) { if (e.data && e.data.chimtoSrc && !window.__chimtoSrc) window.__chimtoSrc = e.data.chimtoSrc; });
          }
          var n = 0, ok = /^data:application\/(vnd\.apple\.mpegurl|x-mpegurl)[;,]/i, file = /\.(m3u8|mp4)(\?|#|$)/i;
          var t = setInterval(function () {
            if (++n > 150) { clearInterval(t); return; }
            try {
              var p = window.jwplayer && window.jwplayer(), it = p && p.getPlaylistItem && p.getPlaylistItem();
              var f = it && (it.file || (it.sources && it.sources[0] && it.sources[0].file));
              var hls = !!(it && /hls|m3u8/i.test(String(it.type || ((it.sources && it.sources[0]) || {}).type || ''))) ||
                !!(p && p.getProvider && /hls/i.test((p.getProvider() || {}).name || ''));
              if (!f || typeof f !== 'string') {
                var v = document.querySelector('video'), s = v && (v.currentSrc || v.src);
                if (s && /^https?:/.test(s) && file.test(s)) f = s; else return;
              }
              if (!ok.test(f) && !file.test(f) && !(hls && /^https?:/.test(f))) return;
              clearInterval(t);
              var msg = { u: /^data:/.test(f) ? f : new URL(f, location.href).href, p: location.href, h: hls ? 1 : 0 };
              try { var tr = ((it && it.tracks) || []).filter(function (x) { return x.kind === 'captions' || x.kind === 'subtitles'; })[0];
                if (tr && tr.file) msg.s = new URL(tr.file, location.href).href; } catch (x) {}
              if (window.top === window) window.__chimtoSrc = msg; else window.top.postMessage({ chimtoSrc: msg }, '*');
            } catch (x) {}
          }, 200);
        })();

        """;

    private static PlayerView? _instance;

    private readonly MainWindow _main;
    private readonly WebView2 _web;
    /// Khung hình: WebView2 (trang phát) hoặc trình phát native (mpv) — cửa sổ phim nhỏ mang cả khung này đi.
    private readonly System.Windows.Controls.Grid _stage = new() { Background = Brushes.Black };
    /// Trình phát native (libmpv) — tạo khi cần, tắt phim thì huỷ.
    private NativeVideo? _native;
    /// Đang phát bằng mpv / đang mở trình phát nhúng chỉ để lấy link luồng / mpv lỗi với server này (phát bằng WebView2 tới khi
    /// đổi tập / đổi server).
    private bool _nativeOn, _extract, _forceWeb;
    /// mpv quá chừng này (ms) chưa chạy được mà cũng không báo lỗi → chuyển sang WebView2 (như bản Linux).
    private const long NativeStallMs = 20_000;
    private readonly PlayerOverlay _ov;
    private Task? _init;
    private readonly string _agentJs, _focusJs, _wrapperHtml, _hlsHtml;
    private readonly byte[] _hlsJs;
    /// Bản hls.js đầy đủ (luồng âm thanh tách rời) — chỉ trang dùng Video.Hls (dexter.pw); đọc lần đầu cần.
    private byte[]? _hlsFullJs;
    private string? _scriptId;

    /// hls.html tải hls.js từ các đường dẫn này (cùng origin với trang tập), app trả tệp đóng kèm (OnResource).
    private const string HlsJsPath = "/__chimto/hls.js";
    private const string HlsFullJsPath = "/__chimto/hls-full.js";

    // Trang bọc trả lời tại địa chỉ trang tập (một lần)
    private string? _serveUrl;
    private string? _serveHtml;
    private string? _expectNav;

    private readonly DispatcherTimer _poll = new() { Interval = TimeSpan.FromSeconds(1) };
    private readonly DispatcherTimer _osdTimer = new();
    private readonly DispatcherTimer _noticeTimer = new();
    private readonly DispatcherTimer _hudTimer = new() { Interval = TimeSpan.FromMilliseconds(900) };
    private readonly DispatcherTimer _seekTimer = new() { Interval = TimeSpan.FromMilliseconds(500) };
    private readonly DispatcherTimer _clickTimer = new() { Interval = TimeSpan.FromMilliseconds(260) };

    public bool IsOpen { get; private set; }

    // ---- Trạng thái lần xem (giống PlayerActivity) ----
    private sealed record PlayState(double T, double D, bool Paused, bool Ended, bool Waiting, bool Ready);

    private string _filmUrl = "";
    private FilmDetail? _detail;
    private EpisodeGroup? _group;
    private List<Episode> Episodes => _group?.Episodes ?? new();
    private int _index;
    private bool _resumeOpen;
    private string _openEpUrl = "";
    private int _loadToken;

    private bool _pageMode;
    private EpisodeSource? _source;
    private Server? _server;
    private int _token;
    private int _pageLoad;
    private string? _embedUrl;
    private string? _hlsUrl;

    /// Phụ đề rời của tập đang phát (dexter.pw): danh sách, tệp đang hiện (-1 = tắt), độ lệch (giây, + = chữ muộn hơn).
    private List<SubTrack> _subs = new();
    private int _subIdx = -1;
    private double _subDelay;
    /// Lời nhắn phụ đề mới nhất đã hiện (số thứ tự hls.html đánh; trang nạp lại thì đếm lại từ 1).
    private int _subNoteShown;
    /// Nút «Phụ đề: …» trong bảng tuỳ chọn đang mở (đổi chữ tại chỗ, khỏi dựng lại bảng — giữ ô đang chọn).
    private Button? _subButton;

    /// Các bản của luồng HLS đang phát (chiều cao, cao → thấp) — hls.html báo khi mở bảng ▼; nút «Chất lượng» của bảng đang mở.
    private List<int> _levels = new();
    private Button? _qualityButton;
    private string _episodeUrl = "";
    private bool _errShown;
    private long _lastAdAt;

    private PlayState? _state;
    private bool _started;
    private int _resumeAt;
    private bool _resumePending;
    private long _resumeIssuedAt;
    private bool _endedHandled;
    private bool _hintShown;
    private long _loadStartedAt;
    private long _lastSave;
    private double? _seekTarget;
    private double _rate = 1;
    private int _startHint;
    private bool _introDone;
    private int _repeat;
    private bool _dragging;
    private bool _clickThrough;
    private bool _polling;

    // Cửa sổ phim nhỏ (MiniWindow) và giới hạn giờ xem
    private MiniWindow? _miniWin;
    private bool _limitWarned;

    /// Đang ở cửa sổ phim nhỏ (phím / chuột của cửa sổ app dành cho app, không cho trình phát).
    public bool IsMini => _miniWin != null;

    public PlayerView(MainWindow main)
    {
        _instance = this;
        _main = main;
        Background = Brushes.Black;
        Focusable = false;
        _web = new WebView2 { DefaultBackgroundColor = System.Drawing.Color.Black, Focusable = false };
        _stage.Children.Add(_web);
        Content = _stage;

        _agentJs = Res("player-agent.js");
        _focusJs = Res("page-focus.js");
        _wrapperHtml = Res("player.html");
        _hlsHtml = Res("hls.html");
        _hlsJs = ResBytes("hls.light.min.js");

        _ov = new PlayerOverlay { Owner = main };
        SetupOverlay();
        _poll.Tick += async (_, _) => await Poll();
        _osdTimer.Tick += (_, _) => HideOsd();
        _noticeTimer.Tick += (_, _) =>
        {
            _noticeTimer.Stop();
            _ov.NoticeBox.Visibility = Visibility.Collapsed;
        };
        _hudTimer.Tick += (_, _) =>
        {
            _hudTimer.Stop();
            _ov.HudBox.Visibility = Visibility.Collapsed;
        };
        _seekTimer.Tick += (_, _) => FlushSeek();
        _clickTimer.Tick += (_, _) =>
        {
            _clickTimer.Stop();
            Toggle();
        };
        SizeChanged += (_, _) => Follow();
    }

    private static string Res(string name)
    {
        using var s = Assembly.GetExecutingAssembly().GetManifestResourceStream(name)!;
        using var r = new StreamReader(s);
        return r.ReadToEnd();
    }

    private static byte[] ResBytes(string name)
    {
        using var s = Assembly.GetExecutingAssembly().GetManifestResourceStream(name)!;
        using var ms = new MemoryStream();
        s.CopyTo(ms);
        return ms.ToArray();
    }

    // ---- WebView2 ----

    private Task EnsureWeb() => _init ??= InitWeb();

    private async Task InitWeb()
    {
        Log.D("player", $"Khởi tạo WebView2 (khung {_web.Visibility}, {_web.ActualWidth:0}x{_web.ActualHeight:0})");
        try
        {
            var env = await App.WebEnv();
            await _web.EnsureCoreWebView2Async(env);
        }
        catch (Exception e)
        {
            Log.D("player", "Khởi tạo WebView2 lỗi: " + e.Message);
            throw;
        }
        var c = _web.CoreWebView2;
        var st = c.Settings;
        st.AreDefaultContextMenusEnabled = false;
        st.IsStatusBarEnabled = false;
        st.IsZoomControlEnabled = false;
        st.AreBrowserAcceleratorKeysEnabled = false;
        st.IsGeneralAutofillEnabled = false;
        st.IsPasswordAutosaveEnabled = false;
        st.IsSwipeNavigationEnabled = false;
#if DEBUG
        st.AreDevToolsEnabled = true;
        // Thử trên máy làm việc: tắt tiếng (biến môi trường CONBANG_MUTE=1)
        if (Environment.GetEnvironmentVariable("CONBANG_MUTE") == "1") c.IsMuted = true;
#else
        st.AreDevToolsEnabled = false;
#endif
        Http.UserAgent = st.UserAgent;
        c.NewWindowRequested += (_, e) => e.Handled = true; // cửa sổ quảng cáo
        c.NavigationStarting += OnNavigationStarting;
        c.DOMContentLoaded += (_, _) => InjectPage();
        c.NavigationCompleted += (_, _) => InjectPage();
        c.ProcessFailed += (_, e) =>
        {
            Log.D("player", $"Trình duyệt lỗi: {e.ProcessFailedKind}");
            if (IsOpen) ShowError("Trình duyệt bị lỗi khi phát.\nNhấn ▼ → «Tải lại».");
        };
        foreach (var f in AdBlock.Filters()) c.AddWebResourceRequestedFilter(f, CoreWebView2WebResourceContext.All);
        c.AddWebResourceRequestedFilter("*" + HlsJsPath, CoreWebView2WebResourceContext.All);
        c.AddWebResourceRequestedFilter("*" + HlsFullJsPath, CoreWebView2WebResourceContext.All);
        c.AddWebResourceRequestedFilter("*", CoreWebView2WebResourceContext.Document);
        c.WebResourceRequested += OnResource;
        Log.D("player", $"WebView2 sẵn sàng, UA: {st.UserAgent}");
    }

    private void OnResource(object? sender, CoreWebView2WebResourceRequestedEventArgs e)
    {
        var env = _web.CoreWebView2.Environment;
        var uri = e.Request.Uri;
        if (!Uri.TryCreate(uri, UriKind.Absolute, out var u)) return;
        if (AdBlock.IsAd(u.Host))
        {
            e.Response = env.CreateWebResourceResponse(null, 403, "Blocked", "Content-Type: text/plain");
            return;
        }
        var full = u.AbsolutePath.EndsWith(HlsFullJsPath);
        if (full || u.AbsolutePath.EndsWith(HlsJsPath))
        {
            var js = full ? _hlsFullJs ??= ResBytes("hls.min.js") : _hlsJs;
            e.Response = env.CreateWebResourceResponse(new MemoryStream(js), 200, "OK",
                "Content-Type: application/javascript; charset=utf-8\nCache-Control: no-store");
            return;
        }
        if (e.ResourceContext == CoreWebView2WebResourceContext.Document && _serveUrl != null && _serveHtml != null &&
            e.Request.Method == "GET" && SameUrl(uri, _serveUrl))
        {
            var bytes = System.Text.Encoding.UTF8.GetBytes(_serveHtml);
            _serveUrl = null;
            e.Response = env.CreateWebResourceResponse(new MemoryStream(bytes), 200, "OK",
                "Content-Type: text/html; charset=utf-8\nCache-Control: no-store");
        }
    }

    private static bool SameUrl(string a, string b) =>
        Uri.TryCreate(a, UriKind.Absolute, out var x) && Uri.TryCreate(b, UriKind.Absolute, out var y) &&
        string.Equals(x.GetLeftPart(UriPartial.Query), y.GetLeftPart(UriPartial.Query), StringComparison.OrdinalIgnoreCase);

    /// Chặn mọi điều hướng cả trang do trang tự gây ra (quảng cáo); app tự nạp trang. Chuyển hướng phía máy chủ thì cho qua.
    private void OnNavigationStarting(object? sender, CoreWebView2NavigationStartingEventArgs e)
    {
        if (_expectNav != null && SameUrl(e.Uri, _expectNav))
        {
            _expectNav = null;
            return;
        }
        if (e.IsRedirected || e.Uri == "about:blank") return;
        Log.D("player", "Chặn điều hướng: " + e.Uri);
        e.Cancel = true;
    }

    private void InjectPage()
    {
        if (!_pageMode || _web.CoreWebView2 == null) return;
        _ = _web.CoreWebView2.ExecuteScriptAsync(_focusJs);
    }

    /// Nạp trang (html != null: trả lời bằng trang bọc của app tại địa chỉ url).
    private async Task Navigate(string url, string? html, int start)
    {
        var init = EnsureWeb();
        // 14/11/2026 gặp một lần WebView2 không khởi tạo xong (chưa tái hiện được): ghi lại để lần sau biết lúc đó khung ra sao
        if (await Task.WhenAny(init, Task.Delay(15_000)) != init)
            Log.D("player", $"WebView2 chưa sẵn sàng sau 15 giây (khung {_web.Visibility}, {_web.ActualWidth:0}x{_web.ActualHeight:0}, " +
                            $"đã nạp {_web.IsLoaded}, trình phát {(IsMini ? "nhỏ" : "lớn")})");
        await init;
        var c = _web.CoreWebView2;
        if (_scriptId != null) c.RemoveScriptToExecuteOnDocumentCreated(_scriptId);
        var js = $"window.__chimtoStart={start};if(window.top===window)window.__chimtoLoad={_pageLoad};\n" + StyleShim + _agentJs;
        if (_extract) js += ExtractJs;
        _scriptId = await c.AddScriptToExecuteOnDocumentCreatedAsync(js);
        _serveUrl = html != null ? url : null;
        _serveHtml = html;
        _expectNav = url;
        c.Navigate(url);
    }

    private void Blank()
    {
        if (_web.CoreWebView2 == null) return;
        _expectNav = "about:blank";
        _serveUrl = null;
        _web.CoreWebView2.Navigate("about:blank");
    }

    private void Cmd(string name, double? arg = null)
    {
        if (_nativeOn)
        {
            _native?.Cmd(name, arg);
            return;
        }
        if (_web.CoreWebView2 == null) return;
        var a = arg?.ToString(CultureInfo.InvariantCulture) ?? "null";
        _ = _web.CoreWebView2.ExecuteScriptAsync($"window.chimtoCmd&&chimtoCmd('{name}',{a})");
    }

    /// Xoá cookie / bộ nhớ đệm của trình duyệt trong app và ảnh bìa đã lưu.
    public static async Task ClearBrowsingData()
    {
        try
        {
            if (_instance?._web.CoreWebView2 is { } c)
            {
                await c.Profile.ClearBrowsingDataAsync();
            }
            else
            {
                var env = await App.WebEnv();
                var hwnd = new WindowInteropHelper(Application.Current.MainWindow).Handle;
                var ctl = await env.CreateCoreWebView2ControllerAsync(hwnd);
                await ctl.CoreWebView2.Profile.ClearBrowsingDataAsync();
                ctl.Close();
            }
        }
        catch (Exception e)
        {
            Log.D("player", "Xoá dữ liệu lỗi: " + e.Message);
        }
        try
        {
            var img = Path.Combine(Prefs.DataDir, "img");
            if (Directory.Exists(img)) Directory.Delete(img, true);
        }
        catch (Exception)
        {
        }
    }

    // ---- Mở / đóng ----

    public void Open(string filmUrl, string epUrl, bool resume)
    {
        if (IsMini) ExitMini(false);
        var wasOpen = IsOpen;
        IsOpen = true;
        if (wasOpen)
        {
            _token++;
            StopVideo();
        }
        HidePanel();
        _ov.Show();
        Follow();
        _ov.Activate();
        _filmUrl = filmUrl;
        _openEpUrl = epUrl;
        _resumeOpen = resume;
        _detail = null;
        // Phim mới chưa tải xong / tải lỗi: bảng ▼ không còn server, bản, tập của phim trước
        _source = null;
        _group = null;
        _index = 0;
        _limitWarned = false;
        _poll.Start();
        ApplyOverlayScale();
        var t = ++_loadToken;
        if (Site.CachedDetail(filmUrl) is { } cached)
        {
            Start(cached, epUrl);
            return;
        }
        ShowLoading("Đang tải thông tin phim…");
        _ov.TitleText.Text = "";
        _main.Title = "MTV";
        _ = LoadDetail(t, filmUrl, epUrl);
    }

    private async Task LoadDetail(int t, string filmUrl, string epUrl)
    {
        try
        {
            var d = await Site.Detail(filmUrl);
            if (t != _loadToken || !IsOpen) return;
            Start(d, epUrl);
        }
        catch (Exception e)
        {
            if (t != _loadToken || !IsOpen) return;
            ShowError("Không tải được phim.\n" + Util.Friendly(e));
        }
    }

    private void Start(FilmDetail d, string epUrl)
    {
        _detail = d;
        // Phim đang theo dõi: đã vào xem thì không còn là "tập mới"
        if (d.LatestEpisode is { } last) Prefs.UpdateFollow(d.Url, d.EpisodeCount, last.Title, true);
        var g = d.GroupOf(epUrl) ?? d.Groups.FirstOrDefault();
        if (g == null)
        {
            ShowError("Phim này chưa có tập nào.");
            return;
        }
        _group = g;
        var i = Math.Max(0, g.Episodes.FindIndex(e => e.Url == epUrl));
        var finished = _resumeOpen && Prefs.Finished(epUrl);
        // Xem tiếp sau tập cuối của một mùa (phim chia mùa): sang tập đầu của mùa sau
        if (finished && i + 1 >= g.Episodes.Count && NextSeason(g) is { } ns)
        {
            _group = ns;
            OpenEpisode(0);
            return;
        }
        OpenEpisode(finished && i + 1 < g.Episodes.Count ? i + 1 : i);
    }

    /// Thoát trình phát (lưu chỗ xem, về trang trước).
    public void Close(bool notify = true)
    {
        if (!IsOpen) return;
        if (IsMini) ExitMini(false);
        IsOpen = false;
        _token++;
        _loadToken++;
        StopVideo();
        // Trả bộ nhớ của trình phát native (mpv) khi không xem phim; lần xem sau tạo lại
        DropNative();
        WatchLimit.Flush();
        _poll.Stop();
        _clickTimer.Stop();
        ExitClickThrough();
        HidePanel();
        _ov.Hide();
        _main.Title = "MTV";
        if (notify) _main.PlayerClosed();
    }

    /// Cửa sổ chính được chọn (vd bấm thanh tác vụ): đưa lớp phủ lên lại để giữ phím.
    public void Reactivate()
    {
        if (!IsOpen || _clickThrough || IsMini) return;
        Dispatcher.BeginInvoke(() =>
        {
            if (IsOpen && _ov.IsVisible) _ov.Activate();
        }, DispatcherPriority.Input);
    }

    /// Đặt lớp phủ đúng chỗ trình phát (theo cửa sổ chính).
    public void Follow()
    {
        if (!IsOpen || IsMini) return;
        if (_main.WindowState == WindowState.Minimized) return;
        var src = PresentationSource.FromVisual(this);
        if (src?.CompositionTarget == null || ActualWidth <= 0) return;
        var m = src.CompositionTarget.TransformFromDevice;
        var a = m.Transform(PointToScreen(new Point(0, 0)));
        var b = m.Transform(PointToScreen(new Point(ActualWidth, ActualHeight)));
        _ov.Left = a.X;
        _ov.Top = a.Y;
        _ov.Width = Math.Max(1, b.X - a.X);
        _ov.Height = Math.Max(1, b.Y - a.Y);
        ApplyOverlayScale();
    }

    private void ApplyOverlayScale()
    {
        // Màn nhỏ: thu nhỏ thanh điều khiển cho vừa; màn lớn theo cỡ giao diện
        var s = Prefs.UiScale * Math.Clamp(_ov.Width / 1280, 0.75, 1.0);
        _ov.Scaled.LayoutTransform = new ScaleTransform(s, s);
        _ov.Panel.Width = Math.Min(470, Math.Max(300, _ov.Width / s * 0.45));
    }

    // ---- Mở tập / server ----

    private void OpenEpisode(int i)
    {
        var d = _detail;
        if (d == null || i < 0 || i >= Episodes.Count) return;
        var ep = Episodes[i];
        // Người đang xem đã hết giờ xem: chưa mở tập, hỏi mã phụ huynh trước
        if (WatchLimit.Blocked() is { } blocked)
        {
            LimitBlock(blocked, d.Url, ep.Url);
            return;
        }
        _index = i;
        _forceWeb = false;
        var t = ++_token;
        StopVideo();
        _episodeUrl = ep.Url;
        _source = null;
        var groupName = d.Groups.Count > 1 ? $" ({_group?.Name})" : "";
        _ov.TitleText.Text = $"{d.Title} — {ep.Title}{groupName}";
        _main.Title = $"{d.Title} — {ep.Title} · MTV";
        _resumeAt = Prefs.PositionOf(ep.Url);
        _introDone = false;
        Prefs.AddHistory(new HistoryItem
        {
            FilmUrl = d.Url, Title = d.Title, Poster = d.Poster, EpTitle = ep.Title, EpUrl = ep.Url, Time = Util.NowMs(),
        });
        // Trang chủ tổng hợp: hết báo «tập mới» tới khi có tập sau; người hay xem bản thuyết minh / lồng tiếng thì có hàng riêng
        HomeFeed.MarkSeen(d);
        if (_group != null && KkPhim.Lang(_group.Name) is { } lang) Prefs.RecordWatchLang(lang);
        ShowLoading($"Đang mở {ep.Title}…");
        ShowOsd();
        _ = LoadSource(t, d, _group, ep);
    }

    private async Task LoadSource(int t, FilmDetail d, EpisodeGroup? group, Episode ep)
    {
        try
        {
            // Trang tập + server dự phòng KKPhim (tải song song; trang lỗi mà KKPhim có tập này thì vẫn phát được)
            var src = await KkPhim.Source(d, group, ep);
            if (t != _token || !IsOpen) return;
            _source = src;
            if (src.PageOnly)
            {
                await PlayPage(src);
                return;
            }
            var host = Site.HostOf(src.EpUrl);
            var usual = src.Servers.FirstOrDefault(s => s.Type == Prefs.ServerType(host)) ?? src.Servers.FirstOrDefault();
            await PlayServer(usual);
        }
        catch (Exception e)
        {
            if (t != _token || !IsOpen) return;
            ShowError($"Không mở được {ep.Title}.\n{Util.Friendly(e)}");
        }
    }

    /// Mở thẳng trang tập: trang tự nạp trình phát, page-focus.js phóng nó ra toàn màn hình.
    private async Task PlayPage(EpisodeSource src, Server? asServer = null)
    {
        StopVideo();
        _server = asServer;
        _pageMode = true;
        _loadStartedAt = Util.NowMs();
        ShowLoading("Đang mở trang tập phim…");
        _startHint = StartFor();
        await Navigate(src.EpUrl, null, _startHint);
    }

    private async Task PlayServer(Server? s)
    {
        var src = _source;
        if (src == null) return;
        // "Trình phát của trang" (trang mở thẳng trang tập, có thêm server KKPhim)
        if (s?.Type == EpisodeSource.PageServer)
        {
            ++_token;
            await PlayPage(src, s);
            return;
        }
        _server = s;
        var t = ++_token;
        StopVideo();
        ShowLoading($"Đang tải {s?.Label ?? "trình phát"}…");
        try
        {
            var v = await Site.VideoOf(src, s);
            if (t != _token || !IsOpen) return;
            _loadStartedAt = Util.NowMs();
            _startHint = StartFor();
            var m3u8 = Uri.TryCreate(v.Url, UriKind.Absolute, out var vu) && vu.AbsolutePath.EndsWith(".m3u8");
            var isHls = v.File || v.Hls || m3u8;
            var native = NativeAllowed;
            // Trình phát native: luồng .m3u8 / tệp video phát thẳng bằng mpv. Luồng Video.Hls (dexter.pw) luôn phát bằng hls.html:
            // phụ đề chọn được / chỉnh lệch nằm ở trang đó (mpv cho dexter.pw làm sau)
            if (isHls && native && !v.Hls)
            {
                _hlsUrl = v.Url;
                if (PlayNative(v.Url, v.Subtitle, src.EpUrl, _startHint, false)) return;
                _hlsUrl = null;
            }
            // Trình phát nhúng: mở như cũ, đọc link luồng trong JW Player rồi chuyển sang mpv (ExtractJs)
            _extract = native && !isHls;
            string page;
            if (isHls)
            {
                _hlsUrl = v.Url;
                var listed = v.Subs is { Count: > 0 } || v.SubsLater != null;
                SetSubs(v.Subs ?? new());
                _subDelay = listed ? Prefs.SubDelay(src.EpUrl) : 0;
                page = HlsPage(v, file: v.File || !(v.Hls || m3u8), subs: listed ? SubsJson(_subs) : null);
                if (v.SubsLater is { } later) _ = PollSubs(later, t);
            }
            else
            {
                _embedUrl = v.Url;
                page = _wrapperHtml.Replace("{{SRC}}", System.Net.WebUtility.HtmlEncode(v.Url));
            }
            Log.D("player", $"Phát {(isHls ? v.Hls ? "HLS (hls.js đầy đủ)" : "HLS" : "iframe")} {v.Url} (server {s?.Label}, bắt đầu {_startHint}s" +
                            (_subs.Count > 0 || v.SubsLater != null ? $", {_subs.Count} phụ đề, chọn {_subIdx}{(v.SubsLater != null ? ", còn đang tìm" : "")}" : "") + ")");
            await Navigate(src.EpUrl, page, _startHint);
        }
        catch (Exception e)
        {
            if (t != _token || !IsOpen) return;
            ShowError($"{Util.Friendly(e)}\nNhấn ▼ (hoặc ☰) để chọn server khác.");
            ShowPanel();
        }
    }

    /// Trang phát HLS / tệp video của app (hls.html dùng chung với bản Android — Android điền ở PlayerActivity): điền mọi chỗ
    /// {{…}} trong một lượt (link / tên phụ đề có lỡ chứa «{{…}}» cũng không bị thay tiếp). Sót chỗ nào là trang lỗi cú pháp JS,
    /// mọi luồng HLS / MP4 không phát được → ghi nhật ký. file: phát thẳng bằng &lt;video&gt;; subs: JSON [{u, l, g}] hoặc null
    /// (không có danh sách — chỉ dùng phụ đề rời Subtitle nếu có).
    private string HlsPage(Video v, bool file, string? subs)
    {
        var fill = new Dictionary<string, string>
        {
            // dexter.pw (Video.Hls): bản hls.js đầy đủ — luồng âm thanh tách rời
            ["HLSJS"] = v.Hls ? HlsFullJsPath : HlsJsPath,
            // agent đã được chèn vào mọi khung (Navigate → AddScriptToExecuteOnDocumentCreated); thẻ script này (trong <head>,
            // chạy trước script phát) chỉ báo chất lượng muốn xem — hls.html đọc window.__chimtoWantH lúc có danh sách bản
            ["AGENT"] = $"window.__chimtoWantH={Prefs.VideoQuality.ToString(CultureInfo.InvariantCulture)};",
            ["SRC"] = JsonSerializer.Serialize(v.Url),
            ["SUB"] = v.Subtitle != null ? JsonSerializer.Serialize(v.Subtitle) : "null",
            ["SUBS"] = subs ?? "null",
            ["SUBI"] = _subIdx.ToString(CultureInfo.InvariantCulture),
            ["SUBD"] = _subDelay.ToString(CultureInfo.InvariantCulture),
            ["FILE"] = file ? "true" : "false",
            // chọn bản không lớn hơn khung phát + tự hạ bản khi rớt khung hình (dexter.pw có 4K / 1080p / 720p)
            ["CAP"] = v.Hls ? "true" : "false",
            ["START"] = _startHint.ToString(CultureInfo.InvariantCulture),
        };
        return Regex.Replace(_hlsHtml, @"\{\{([A-Z]+)\}\}", m =>
        {
            if (fill.TryGetValue(m.Groups[1].Value, out var s)) return s;
            Log.D("player", $"hls.html có chỗ app chưa điền: {m.Value}");
            return m.Value;
        });
    }

    // ---- Phụ đề rời (dexter.pw) ----

    /// [{u, l, g}] cho hls.html (SUBS / __chimtoSubs). JsonSerializer mặc định mã hoá cả «&lt;» → an toàn trong thẻ script.
    private static string SubsJson(List<SubTrack> list) => JsonSerializer.Serialize(list.Select(s => new { u = s.Url, l = s.Label, g = s.Lang }));

    /// Danh sách phụ đề mới → chọn sẵn theo ngôn ngữ người xem ưa dùng (không có tiếng Việt thì bản đầu tiên; "off" = tắt).
    private void SetSubs(List<SubTrack> list)
    {
        _subs = list;
        var want = Prefs.SubLang;
        _subIdx = list.Count == 0 || want == "off" ? -1 : Math.Max(0, list.FindIndex(s => s.Lang == want));
    }

    private string SubText() =>
        "Phụ đề: " + (_subIdx >= 0 && _subIdx < _subs.Count ? _subs[_subIdx].Label : "Tắt") +
        (_subDelay != 0 && _subIdx >= 0 ? " · " + DelayText(_subDelay) : "");

    private static string DelayText(double d) =>
        (d > 0 ? "muộn " : "sớm ") + Math.Abs(d).ToString("0.#", CultureInfo.InvariantCulture).Replace('.', ',') + " s";

    private void ShowSubButton()
    {
        if (_subButton != null) _subButton.Content = SubText();
    }

    /// Chạy lệnh trong trang phát (hls.html) — không đợi kết quả.
    private void PageJs(string code)
    {
        if (_nativeOn || _web.CoreWebView2 == null) return;
        _ = _web.CoreWebView2.ExecuteScriptAsync(code);
    }

    /// Đổi bản phụ đề (hết danh sách thì tắt, bấm nữa lại bản đầu); nhớ ngôn ngữ cho người xem này.
    private void CycleSub()
    {
        if (_subs.Count == 0) return;
        _subIdx = _subIdx + 1 >= _subs.Count ? -1 : _subIdx + 1;
        Prefs.SubLang = _subIdx >= 0 ? _subs[_subIdx].Lang : "off";
        PageJs($"window.__chimtoSub&&__chimtoSub({_subIdx})");
        ShowSubButton();
    }

    /// Chữ lệch với tiếng (bản phụ đề làm cho bản phim khác): dời 0,5 giây mỗi lần bấm, nhớ cho tập này.
    private void NudgeSub(double d)
    {
        if (_subs.Count == 0) return;
        _subDelay = Math.Round((_subDelay + d) * 10) / 10.0;
        Prefs.SetSubDelay(_source?.EpUrl ?? _episodeUrl, _subDelay);
        PageJs($"window.__chimtoSubDelay&&__chimtoSubDelay({_subDelay.ToString(CultureInfo.InvariantCulture)})");
        ShowSubButton();
        Notice("Phụ đề: " + (_subDelay == 0 ? "đúng như bản gốc" : DelayText(_subDelay) + " so với bản gốc"), 1500);
    }

    /// Tệp phụ đề hỏng thì trang tự sang tệp sau — hỏi lại tệp đang hiện cho đúng chữ trên nút.
    private async Task SyncSubIdx()
    {
        if (_subs.Count == 0 || _nativeOn || _web.CoreWebView2 == null) return;
        var load = _pageLoad;
        string r;
        try
        {
            r = await _web.CoreWebView2.ExecuteScriptAsync("window.__chimtoSubNow?__chimtoSubNow():null");
        }
        catch (Exception)
        {
            return;
        }
        if (load != _pageLoad || !int.TryParse(r, NumberStyles.Integer, CultureInfo.InvariantCulture, out var i)) return;
        if (i == _subIdx || i < -1 || i >= _subs.Count) return;
        _subIdx = i;
        ShowSubButton();
    }

    /// hls.html báo "số|lời nhắn" (vd hết tệp tiếng Việt tải được → tạm dùng tiếng Anh; thử lại được) → hiện một lần.
    private void SubNote(string raw)
    {
        var bar = raw.IndexOf('|');
        if (bar <= 0 || !int.TryParse(raw[..bar], NumberStyles.Integer, CultureInfo.InvariantCulture, out var n) || n <= _subNoteShown) return;
        _subNoteShown = n;
        Notice(raw[(bar + 1)..], 7000);
        _ = SyncSubIdx();
    }

    /// Trang còn đang tìm phụ đề (phim bộ: tập ít người xem có khi vài phút mới có, trình duyệt cũng vậy): hỏi lại mỗi 4 giây,
    /// tối đa 3 phút như trang, chừng nào còn mở đúng lần phát này (t); có thì gắn ngay vào trình phát (phụ đề riêng của nguồn,
    /// nếu có, vẫn đứng đầu).
    private async Task PollSubs(string listUrl, int t)
    {
        for (var tries = 0; tries < 45; tries++)
        {
            await Task.Delay(4000);
            if (t != _token || !IsOpen) return;
            Dexter.SubsFrom r;
            try
            {
                r = await Dexter.SubtitlesFrom(listUrl);
            }
            catch (Exception)
            {
                continue; // lỗi mạng: coi như trang vẫn đang tìm
            }
            if (t != _token || !IsOpen) return;
            var found = r.Subs ?? new();
            if (found.Count == 0)
            {
                if (r.Pending) continue;
                return;
            }
            int? keep = _subs.Count > 0 && _subIdx >= 0 ? _subIdx : null;
            SetSubs(_subs.Concat(found.Where(n => _subs.All(s => s.Url != n.Url))).ToList());
            if (keep is { } k) _subIdx = k;
            ShowSubButton();
            PageJs($"window.__chimtoSubs&&__chimtoSubs({SubsJson(_subs)},{_subIdx})");
            Log.D("player", $"Có thêm phụ đề sau {(tries + 1) * 4}s: {found.Count} tệp, chọn {_subIdx}");
            if (_subIdx >= 0 && _subIdx < _subs.Count)
            {
                var label = _subs[_subIdx].Label;
                var low = label.Length > 0 ? char.ToLower(label[0]) + label[1..] : label;
                Notice($"Đã có phụ đề {low} — ▼ để chọn bản khác hoặc chỉnh chữ sớm / muộn.", 5000);
            }
            return;
        }
    }

    // ---- Chất lượng (luồng HLS có nhiều bản, vd 4K của dexter.pw) ----

    private static string QualityName(int h) => h >= 2160 ? "4K" : h >= 1440 ? "2K" : $"{h}p";

    /// Hỏi hls.html danh sách bản (__chimtoLevels) hoặc đổi bản (__chimtoQuality) → nút «Chất lượng». Trình phát native / trang
    /// không phải hls.html: không có nút.
    private async Task SyncQuality(string code = "window.__chimtoLevels?__chimtoLevels():null")
    {
        if (_hlsUrl == null || _nativeOn || _web.CoreWebView2 == null)
        {
            ShowQuality(null);
            return;
        }
        var load = _pageLoad;
        string? r;
        try
        {
            r = await _web.CoreWebView2.ExecuteScriptAsync(code);
        }
        catch (Exception)
        {
            r = null;
        }
        if (load != _pageLoad) return;
        ShowQuality(r);
    }

    /// {levels: [chiều cao…] (cao → thấp), want, now} (chuỗi JSON hls.html trả) → chữ trên nút; dưới hai bản thì ẩn nút.
    private void ShowQuality(string? raw)
    {
        var levels = new List<int>();
        var now = 0;
        try
        {
            if (raw != null && JsonSerializer.Deserialize<string>(raw) is { } s && JsonNode.Parse(s) is JsonObject o)
            {
                if (o["levels"] is JsonArray a) levels = a.Select(x => x is JsonValue jv && jv.TryGetValue<int>(out var h) ? h : 0).Where(h => h > 0).ToList();
                if (o["now"] is JsonValue nv && nv.TryGetValue<int>(out var n)) now = n;
            }
        }
        catch (Exception)
        {
        }
        _levels = levels;
        if (_qualityButton == null) return;
        _qualityButton.Visibility = levels.Count > 1 ? Visibility.Visible : Visibility.Collapsed;
        if (levels.Count < 2) return;
        var want = Prefs.VideoQuality;
        _qualityButton.Content = "Chất lượng: " + (want <= 0
            ? "Tự động" + (now > 0 ? $" ({QualityName(now)})" : "")
            : QualityName(want) + (now > 0 && now != want ? $" (đang {QualityName(now)})" : ""));
    }

    /// Tự động → bản cao nhất (4K) → … → bản thấp nhất → tự động; nhớ cho các phim sau (người xem này). Bản đã lưu không có ở
    /// phim này thì tính từ bản thấp hơn gần nhất.
    private void CycleQuality()
    {
        if (_levels.Count < 2) return;
        var options = new List<int> { 0 };
        options.AddRange(_levels);
        var cur = Prefs.VideoQuality;
        var i = options.IndexOf(cur);
        if (i < 0) i = options.FindIndex(h => h >= 1 && h <= cur);
        if (i < 0) i = 0;
        var next = options[(i + 1) % options.Count];
        Prefs.VideoQuality = next;
        _ = QualityChanged(next);
        Notice("Chất lượng: " + (next == 0 ? "Tự động (vừa màn hình)" : QualityName(next)), 1500);
    }

    /// Đổi bản ngay trong trang; vài giây sau (bản mới đã chạy) hỏi lại để chữ «đang …» trên nút đúng.
    private async Task QualityChanged(int h)
    {
        var load = _pageLoad;
        await SyncQuality($"window.__chimtoQuality?__chimtoQuality({h.ToString(CultureInfo.InvariantCulture)}):null");
        await Task.Delay(3000);
        if (load == _pageLoad && _ov.Panel.Visibility == Visibility.Visible) await SyncQuality();
    }

    // ---- Trình phát native (libmpv) ----

    /// Được dùng trình phát native cho lần phát này: đã bật trong cài đặt, có libmpv, server này chưa lỗi với mpv.
    private bool NativeAllowed => Prefs.NativePlayer && !_forceWeb && NativeVideo.Available;

    private NativeVideo? NativeInstance()
    {
        if (_native != null) return _native;
        _native = NativeVideo.Create();
        if (_native == null) return null;
        _native.Host.Visibility = Visibility.Collapsed;
        _stage.Children.Add(_native.Host);
        return _native;
    }

    private void DropNative()
    {
        var n = _native;
        if (n == null) return;
        _native = null;
        _stage.Children.Remove(n.Host);
        n.Dispose();
        try
        {
            Native.SyncVoid("media.closeAll");
        }
        catch (Exception)
        {
        }
        Log.D("native", "Đã tắt trình phát native");
    }

    private void ShowNative(bool on)
    {
        if (_native != null) _native.Host.Visibility = on ? Visibility.Visible : Visibility.Collapsed;
        _web.Visibility = on ? Visibility.Collapsed : Visibility.Visible;
    }

    private sealed class Opened
    {
        public string Url { get; set; } = "";
        public string? Subtitle { get; set; }
    }

    /// Phát src (luồng .m3u8 / tệp video / data:…mpegurl) bằng mpv qua cổng phát nội bộ của lõi; hls = biết chắc là luồng HLS dù
    /// link không có đuôi .m3u8. false = không dùng được (bên gọi tự phát bằng WebView2).
    private bool PlayNative(string src, string? sub, string page, int start, bool hls)
    {
        if (NativeInstance() is not { } n) return false;
        Opened? o;
        try
        {
            o = Native.Sync<Opened?>("media.open", new { src, sub, page, hls });
        }
        catch (Exception e)
        {
            Log.D("native", "Cổng phát nội bộ lỗi: " + e.Message);
            return false;
        }
        if (o == null) return false;
        Log.D("native", $"Phát native {(src.StartsWith("data:") ? "data:… (link do trình phát nhúng giải mã)" : src)} (trang {page}, bắt đầu {start}s)");
        _nativeOn = true;
        _extract = false;
        _loadStartedAt = Util.NowMs();
        // Trang nhúng (nếu đang mở để lấy link) không cần nữa: tắt cho đỡ tốn, khỏi phát tiếng
        Blank();
        ShowNative(true);
        n.Load(o.Url, start, o.Subtitle);
        return true;
    }

    /// Đọc trạng thái mpv, đưa vào cùng đường xử lý với trạng thái của player-agent.js. mpv hỏng thì phát lại bằng WebView2
    /// (cùng server, đúng chỗ đang xem): lỗi trước khi phát / lỗi giữa chừng / quá NativeStallMs chưa chạy mà không báo lỗi.
    private void PollNative()
    {
        if (_native is not { } n) return;
        var st = n.Read();
        if (st.Err is { } e)
        {
            NativeFallback($"mpv lỗi ({e}){(_started ? " giữa chừng" : "")}");
            return;
        }
        var waited = Util.NowMs() - _loadStartedAt;
        if (!_started && waited > NativeStallMs)
        {
            NativeFallback($"mpv chưa chạy được sau {waited / 1000} giây");
            return;
        }
        var o = new Dictionary<string, object?>
        {
            ["t"] = st.T, ["d"] = st.D, ["paused"] = st.Paused, ["ended"] = st.Ended, ["waiting"] = st.Waiting, ["ready"] = st.Ready, ["err"] = "",
        };
        OnState(JsonSerializer.Serialize(JsonSerializer.Serialize(new object[] { _pageLoad, o })));
    }

    /// Link luồng của trình phát nhúng: luồng có lớp bảo vệ riêng của trang (streamfree: SAMPLE-AES + khoá gói kín; NguonC: cả
    /// danh sách mã hoá AES-GCM) thì không vượt — trình phát của trang phát như trước; HLS thường / tệp video thì chuyển sang mpv.
    private async Task ProbeAndPlay(string u, string page, string sub, bool hls)
    {
        if (u.Length == 0 || page.Length == 0) return;
        var t = _token;
        bool ok;
        try
        {
            ok = await Native.Call<bool>("media.probe", new { src = u, page, hls });
        }
        catch (Exception)
        {
            ok = false;
        }
        if (t != _token || !IsOpen) return;
        if (!ok)
        {
            Log.D("native", "Luồng của trình phát nhúng có lớp bảo vệ riêng — giữ trình phát của trang");
            return;
        }
        // Trang đã kịp phát thì mpv bắt đầu đúng chỗ đang xem
        PlayNative(u, sub.Length > 0 ? sub : null, page, CurrentTime() ?? _startHint, hls);
    }

    private void NativeFallback(string why)
    {
        Log.D("native", why + " — chuyển sang trình phát web");
        Notice("Trình phát native không phát được — chuyển sang trình phát web.", 5000);
        if (CurrentTime() is { } t) _resumeAt = t;
        _forceWeb = true;
        _ = PlayServer(_server);
    }

    private void StopVideo()
    {
        SavePosition();
        if (_nativeOn)
        {
            _native?.Stop();
            ShowNative(false);
        }
        _nativeOn = false;
        _extract = false;
        _pageMode = false;
        _embedUrl = null;
        _hlsUrl = null;
        _subs = new();
        _subIdx = -1;
        _subDelay = 0;
        _subNoteShown = 0;
        ShowSubButton();
        _levels = new();
        if (_qualityButton != null) _qualityButton.Visibility = Visibility.Collapsed;
        _errShown = false;
        _lastAdAt = 0;
        _state = null;
        _started = false;
        _resumePending = false;
        _endedHandled = false;
        _hintShown = false;
        _seekTarget = null;
        _seekTimer.Stop();
        _pageLoad++;
        Blank();
    }

    /// Đổi server (nút trong bảng tuỳ chọn): nhớ lựa chọn, xem tiếp từ chỗ đang xem.
    private void SwitchServer(Server s)
    {
        var src = _source;
        if (src == null) return;
        Prefs.SetServer(Site.HostOf(src.EpUrl), s.Type, s.Label);
        if (CurrentTime() is { } t) _resumeAt = t;
        _forceWeb = false;
        HidePanel();
        _ = PlayServer(s);
    }

    private void Reload()
    {
        HidePanel();
        if (CurrentTime() is { } t) _resumeAt = t;
        var src = _source;
        if (_detail == null) Open(_filmUrl, _openEpUrl, _resumeOpen); // chưa tải được thông tin phim: tải lại
        else if (src == null) OpenEpisode(_index);
        else if (src.PageOnly) _ = PlayPage(src);
        else _ = PlayServer(_server);
    }

    private void Next()
    {
        if (_index + 1 < Episodes.Count) OpenEpisode(_index + 1);
        else if (NextSeason() != null) OpenNextSeason();
        else Notice("Đây là tập mới nhất.");
    }

    /// Còn tập sau (kể cả tập đầu của mùa sau — phim chia mùa).
    private bool HasNext => _index + 1 < Episodes.Count || NextSeason() != null;

    /// Phim chia mùa (dexter.pw): mùa sau của mùa g (mặc định mùa đang xem), không sang nhóm tập đặc biệt; phim khác: null.
    private EpisodeGroup? NextSeason(EpisodeGroup? g = null)
    {
        g ??= _group;
        if (_detail is not { Seasons: true } d || g == null) return null;
        var i = d.Groups.IndexOf(g);
        if (i < 0 || i + 1 >= d.Groups.Count) return null;
        var n = d.Groups[i + 1];
        return n.Name != FilmDetail.Specials && n.Episodes.Count > 0 ? n : null;
    }

    private void OpenNextSeason()
    {
        if (NextSeason() is not { } g) return;
        _group = g;
        OpenEpisode(0);
    }

    private void Prev()
    {
        if (_index > 0) OpenEpisode(_index - 1);
        else Notice("Đây là tập đầu tiên.");
    }

    /// Sang bản khác (Vietsub / Thuyết Minh…): mở tập cùng tên và xem tiếp đúng chỗ; bản đó chưa có tập này thì mở tập cùng vị trí.
    private void SwitchGroup(EpisodeGroup g)
    {
        HidePanel();
        if (g == _group || _detail == null || g.Episodes.Count == 0) return;
        // Phim chia mùa: chọn mùa là mở tập đầu của mùa đó (mùa nào cũng có «Tập 1» — so theo tên tập là sai)
        if (_detail.Seasons)
        {
            _group = g;
            OpenEpisode(0);
            return;
        }
        var cur = _index < Episodes.Count ? Episodes[_index] : null;
        var same = g.Episodes.FindIndex(e => e.Title == cur?.Title);
        var target = same >= 0 ? same : Math.Min(_index, g.Episodes.Count - 1);
        if (same >= 0)
        {
            if (CurrentTime() is { } t) Prefs.SavePosition(g.Episodes[target].Url, t, _detail.Url);
        }
        else
        {
            Notice($"{g.Name} chưa có {cur?.Title} — mở {g.Episodes[target].Title}.", 6000);
        }
        _group = g;
        OpenEpisode(target);
    }

    // ---- Trạng thái phát ----

    private async Task Poll()
    {
        if (!IsOpen || _polling) return;
        if (_nativeOn)
        {
            PollNative();
            return;
        }
        if (_web.CoreWebView2 == null)
        {
            // WebView2 chưa sẵn sàng: vẫn chạy phần «video chưa chạy? → đổi server» để người xem không phải chờ mãi
            OnState(null);
            return;
        }
        _polling = true;
        string? raw;
        try
        {
            // [lần nạp trang, trạng thái, link luồng của trình phát nhúng (ExtractJs), lời nhắn phụ đề của hls.html]
            raw = await _web.CoreWebView2.ExecuteScriptAsync(
                "JSON.stringify([window.__chimtoLoad||0,window.__chimtoState||null,window.__chimtoSrc||null,window.__chimtoSubNote||null])");
        }
        catch (Exception)
        {
            raw = null;
        }
        finally
        {
            _polling = false;
        }
        OnState(raw);
    }

    private void OnState(string? raw)
    {
        Dictionary<string, object?>? o = null;
        try
        {
            if (raw != null && JsonSerializer.Deserialize<string>(raw) is { } s && JsonScan.Parse(s) is List<object?> arr &&
                arr.Count >= 2 && arr[0] is long load && load == _pageLoad)
            {
                o = arr[1] as Dictionary<string, object?>;
                // Trình phát nhúng đã lộ link luồng (ExtractJs): kiểm trước (trang vẫn mở) rồi mới chuyển sang mpv
                if (_extract && arr.Count >= 3 && arr[2] is Dictionary<string, object?> found)
                {
                    _extract = false;
                    _ = ProbeAndPlay(J.Str(found, "u"), J.Str(found, "p"), J.Str(found, "s"), found.TryGetValue("h", out var h) && h is long hv && hv > 0);
                }
                if (arr.Count >= 4 && arr[3] is string note) SubNote(note);
            }
        }
        catch (Exception e)
        {
            Log.D("player", "Đọc trạng thái lỗi: " + e.Message);
        }
        var now = Util.NowMs();
        var inAd = o != null && J.Bool(o, "ad");
        if (inAd) _lastAdAt = now;
        var waited = now - Math.Max(_loadStartedAt, _lastAdAt);
        if (!_started && !_errShown && !_hintShown && !inAd && (_embedUrl != null || _hlsUrl != null || _pageMode) && waited > 25_000)
        {
            _hintShown = true;
            HideLoading();
            var other = _source?.PageOnly != false ? "▼ → «Tải lại» / «Bấm thẳng vào trang»" : "▼ để đổi server";
            Notice($"Video chưa chạy? Nhấn Enter để thử phát, hoặc {other}.", 10_000);
        }
        if (o == null) return;
        if (inAd)
        {
            // Trình phát đang chạy quảng cáo (agent tự bỏ qua khi được phép) — chưa phải phim
            if (!_started) ShowLoading("Trình phát đang chạy quảng cáo — tự bỏ qua khi được phép…");
            return;
        }
        double Num(string k) => o.TryGetValue(k, out var v) ? v switch { long l => l, double d => d, _ => 0 } : 0;
        var st = new PlayState(Num("t"), Num("d"), J.Bool(o, "paused"), J.Bool(o, "ended"), J.Bool(o, "waiting"), J.Bool(o, "ready"));
        var wasPaused = _state?.Paused;
        _state = st;
        var err = J.Str(o, "err");
        if (err.Length > 0 && !_errShown)
        {
            _errShown = true;
            if (_started)
            {
                Notice($"Mất kết nối video ({err}).\nNhấn ▼ → «Tải lại», hoặc chọn server khác.", 10_000);
            }
            else
            {
                ShowError($"{_server?.Label ?? "Server"} không phát được video ({err}).\nNhấn ▼ để chọn server khác.");
                ShowPanel();
            }
        }
        // Trình phát nhúng tự phát mà ExtractJs không lấy được link (vd trang dùng Artplayer + link blob:) → thôi chờ, giữ trình phát
        // của trang
        if (_extract && _started && now - _loadStartedAt > 35_000)
        {
            _extract = false;
            Log.D("native", "Không lấy được link luồng từ trình phát nhúng — giữ trình phát của trang");
        }
        WatchLimit.Tick(_started && !st.Paused && !st.Waiting, now);
        if (CheckLimit()) return;
        _miniWin?.Update(_ov.TitleText.Text, st.Paused);
        // Bắt đầu khi video thật sự chạy (không phải lúc mới nạp xong mà còn dừng)
        if (!_started && st.Ready && !st.Paused)
        {
            _started = true;
            HideLoading();
            Log.D("player", $"Video chạy t={(int)st.T}, báo trước {_startHint}");
            if (_resumeAt > 30 && (st.D <= 0 || _resumeAt < st.D - 30))
            {
                _introDone = true;
                // Trình phát đã bắt đầu đúng chỗ (app báo trước) thì khỏi tua lại
                if (Math.Abs(st.T - _resumeAt) > 10)
                {
                    _resumePending = true;
                    _resumeIssuedAt = now;
                    Cmd("seekTo", _resumeAt);
                }
                Notice($"Xem tiếp từ {Util.FormatTime(_resumeAt)}");
            }
            else if (_startHint > 0 && st.T >= _startHint - 10)
            {
                _introDone = true;
                Notice($"Đã bỏ qua nhạc mở đầu (tới {Util.FormatTime(_startHint)}) — ◀ để xem lại.");
            }
            if (Math.Abs(_rate - 1) > 0.01) Cmd("rate", _rate);
            if (Prefs.VideoFill) Cmd("fit", 1);
            ShowOsd();
        }
        if (_resumePending && (st.T >= _resumeAt - 5 || now - _resumeIssuedAt > 30_000)) _resumePending = false;
        if (_started && !_introDone && !st.Paused) SkipIntro(st);
        if (_ov.Osd.Visibility == Visibility.Visible) UpdateOsd();
        if (wasPaused != null && wasPaused != st.Paused) ShowOsd();
        if (_started && now - _lastSave > 5000)
        {
            _lastSave = now;
            SavePosition();
        }
        if (st.Ended && !_endedHandled)
        {
            _endedHandled = true;
            if (Prefs.AutoNext && _index + 1 < Episodes.Count)
            {
                Notice($"Hết tập — chuyển sang {Episodes[_index + 1].Title}");
                OpenEpisode(_index + 1);
            }
            else if (Prefs.AutoNext && NextSeason() is { } g)
            {
                Notice($"Hết mùa — chuyển sang {g.Name}, {g.Episodes[0].Title}");
                OpenNextSeason();
            }
        }
    }

    // ---- Giới hạn giờ xem (WatchLimit) ----

    /// Mỗi lần đọc trạng thái: sắp hết giờ thì nhắc, hết giờ thì dừng phim, thoát trình phát và hỏi mã PIN phụ huynh.
    /// true = đã chặn.
    private bool CheckLimit()
    {
        var b = WatchLimit.Blocked();
        if (b == null)
        {
            if (_started && !_limitWarned && WatchLimit.Remaining() is { } r and > 0 and <= 300)
            {
                _limitWarned = true;
                Notice($"Còn {WatchLimit.MinutesText(r)} xem hôm nay ({Prefs.CurrentProfile.Name}).", 8000);
            }
            return false;
        }
        Cmd("pause");
        LimitBlock(b.Value, _detail?.Url ?? _filmUrl, _episodeUrl);
        return true;
    }

    private void LimitBlock(WatchLimit.Block b, string film, string ep)
    {
        Log.D("player", $"Hết giờ xem ({b}) — {Prefs.CurrentProfile.Name}");
        Close();
        _ = _main.LimitReached(b, film, ep);
    }

    // ---- Cửa sổ phim nhỏ ----

    /// Thu nhỏ: chuyển trình phát sang cửa sổ nhỏ góc màn hình, cửa sổ app trở lại các trang.
    public void EnterMini()
    {
        if (!IsOpen || IsMini) return;
        HidePanel();
        ExitClickThrough();
        _ov.Hide();
        Content = null;
        _miniWin = new MiniWindow(Toggle, () => ExitMini(true), () => Close());
        _miniWin.Host.Content = _stage;
        _miniWin.Update(_ov.TitleText.Text, _state?.Paused);
        var mw = _miniWin;
        mw.Closed += (_, _) =>
        {
            // Đóng bằng Alt+F4: lấy trình phát về rồi tắt phim
            if (_miniWin != mw) return;
            _miniWin = null;
            mw.Host.Content = null;
            Content = _stage;
            Dispatcher.BeginInvoke(() => Close());
        };
        _miniWin.Show();
        _main.PlayerMinimized();
    }

    /// Phóng to lại (show = hiện trình phát trong cửa sổ app); false khi sắp đóng / mở phim khác.
    public void ExitMini(bool show)
    {
        var w = _miniWin;
        if (w == null) return;
        _miniWin = null;
        w.Host.Content = null;
        Content = _stage;
        w.Close();
        if (!show) return;
        _main.PlayerRestored();
        _ov.Show();
        Follow();
        _ov.Activate();
        ShowOsd();
    }

    private string IntroKey => _detail?.Url ?? _filmUrl;

    /// Phim đã đánh dấu nhạc mở đầu: video vừa chạy tới mốc đầu (trễ tối đa 8 giây) thì tua tới mốc hết, một lần mỗi tập.
    private void SkipIntro(PlayState s)
    {
        if (Prefs.Intro(IntroKey) is not { } mark || mark.End <= mark.Start || _seekTarget != null) return;
        if (s.T >= mark.Start + 8 || s.T >= mark.End - 3)
        {
            _introDone = true;
            return;
        }
        if (s.T < mark.Start - 1 || (s.D > 0 && mark.End > s.D - 30)) return;
        _introDone = true;
        Cmd("seekTo", mark.End);
        Notice($"Đã bỏ qua nhạc mở đầu (tới {Util.FormatTime(mark.End)}) — ◀ để xem lại.");
    }

    /// Chỗ bắt đầu báo trước cho lần nạp trang này: chỗ xem dở; hoặc hết nhạc mở đầu nếu nhạc nằm ngay đầu tập.
    private int StartFor()
    {
        if (_resumeAt > 30) return _resumeAt;
        if (Prefs.Intro(IntroKey) is not { } mark) return 0;
        return !_introDone && mark.Start <= 1 && mark.End > mark.Start ? mark.End : 0;
    }

    /// Chỗ đang xem để mang sang (đổi server / tải lại / đổi bản).
    private int? CurrentTime() => _resumePending ? _resumeAt : _state is { } s && _started && s.T > 5 ? (int)s.T : null;

    private void SavePosition()
    {
        var s = _state;
        if (s == null || !_started || _episodeUrl.Length == 0 || _resumePending) return;
        // Xem gần hết thì coi như xong (lần sau mở lại từ đầu)
        var done = s.Ended || (s.D > 0 && s.T > s.D - 90);
        Prefs.SavePosition(_episodeUrl, done ? 0 : (int)s.T, _detail?.Url ?? _filmUrl);
    }

    // ---- Điều khiển ----

    private void Toggle()
    {
        Cmd(_started ? "toggle" : "play");
        ShowOsd();
    }

    private double Step(int repeat) => Prefs.SeekStep * Math.Min(4.0, 1 + repeat / 8.0);

    /// Gộp các lần bấm tua liên tiếp thành một lệnh tua (đỡ giật khi giữ phím).
    private void SeekBy(double delta)
    {
        var s = _state;
        if (s == null || !_started) return;
        var max = s.D > 0 ? s.D - 1 : double.MaxValue;
        _seekTarget = Math.Clamp((_seekTarget ?? s.T) + delta, 0, max);
        ShowOsd();
        _seekTimer.Stop();
        _seekTimer.Start();
    }

    private void SeekTo(double target)
    {
        _seekTarget = target;
        ShowOsd();
        _seekTimer.Stop();
        FlushSeek();
    }

    private void FlushSeek()
    {
        _seekTimer.Stop();
        if (_seekTarget is not { } target) return;
        _seekTarget = null;
        _resumePending = false; // người xem tự tua: từ giờ lấy theo trình phát
        Cmd("seekTo", target);
        if (_state != null) _state = _state with { T = target };
        UpdateOsd();
    }

    public void HandleKey(KeyEventArgs e)
    {
        var key = e.Key == Key.System ? e.SystemKey : e.Key;
        _repeat = e.IsRepeat ? _repeat + 1 : 0;
        if (_clickThrough)
        {
            if (key == Key.Escape)
            {
                e.Handled = true;
                ExitClickThrough();
            }
            return;
        }
        if (key == Key.F11 || (key == Key.Enter && Keyboard.Modifiers == ModifierKeys.Alt) || key == Key.F)
        {
            e.Handled = true;
            if (!e.IsRepeat) _main.ToggleFullscreen();
            return;
        }
        if (_ov.Panel.Visibility == Visibility.Visible)
        {
            if (key is Key.Escape or Key.BrowserBack or Key.Back or Key.M or Key.Apps)
            {
                e.Handled = true;
                HidePanel();
                return;
            }
            if (KeyNav.DirOf(key) is { } dir)
            {
                e.Handled = true;
                var focused = Keyboard.FocusedElement as FrameworkElement;
                if (focused == null || !KeyNav.IsInside(focused, _ov.PanelStack))
                {
                    if (KeyNav.First(_ov.PanelStack) is { } f) KeyNav.FocusAndShow(f);
                    return;
                }
                if (KeyNav.Next(_ov.PanelStack, focused, dir) is { } n) KeyNav.FocusAndShow(n);
            }
            return; // Enter / Space: nút đang chọn tự xử lý
        }
        var first = !e.IsRepeat;
        e.Handled = true;
        switch (key)
        {
            case Key.Escape or Key.BrowserBack or Key.Back:
                if (first) Close();
                break;
            case Key.W:
                if (first) EnterMini();
                break;
            case Key.Space or Key.Enter or Key.K or Key.MediaPlayPause or Key.Play:
                if (first) Toggle();
                break;
            case Key.MediaStop or Key.Pause:
                if (first) Cmd("pause");
                break;
            case Key.Left or Key.J:
                SeekBy(-Step(_repeat));
                break;
            case Key.Right or Key.L:
                SeekBy(Step(_repeat));
                break;
            case Key.Up or Key.I:
                ShowOsd(6000);
                break;
            case Key.Down or Key.M or Key.Apps:
                if (first) ShowPanel();
                break;
            case Key.N or Key.PageDown or Key.MediaNextTrack:
                if (first) Next();
                break;
            case Key.P or Key.PageUp or Key.MediaPreviousTrack:
                if (first) Prev();
                break;
            case >= Key.D0 and <= Key.D9 or >= Key.NumPad0 and <= Key.NumPad9:
                var n = key >= Key.NumPad0 ? key - Key.NumPad0 : key - Key.D0;
                if (first && _state is { D: > 0 } s) SeekTo(s.D * n / 10.0);
                break;
            default:
                e.Handled = false;
                break;
        }
    }

    // ---- Giao diện phủ ----

    private void SetupOverlay()
    {
        var o = _ov;
        o.PreviewKeyDown += (_, e) => HandleKey(e);
        o.BtnBack.Click += (_, _) => Close();
        o.BtnFull.Click += (_, _) => _main.ToggleFullscreen();
        o.BtnMini.Click += (_, _) => EnterMini();
        o.BtnPlay.Click += (_, _) => Toggle();
        o.BtnBack10.Click += (_, _) => SeekBy(-10);
        o.BtnFwd10.Click += (_, _) => SeekBy(10);
        o.BtnPrev.Click += (_, _) => Prev();
        o.BtnNext.Click += (_, _) => Next();
        o.BtnMenu.Click += (_, _) => ShowPanel();
        o.BtnPanelClose.Click += (_, _) => HidePanel();
        o.BtnDone.Click += (_, _) => ExitClickThrough();
        o.Closing += (_, e) =>
        {
            // Alt+F4 trên lớp phủ = thoát phim, không đóng hẳn
            e.Cancel = true;
            Close();
        };
        o.HitArea.MouseMove += (_, _) =>
        {
            if (_clickThrough) return;
            o.HitArea.Cursor = Cursors.Arrow;
            ShowOsd(3000);
        };
        o.HitArea.MouseLeftButtonDown += (_, e) =>
        {
            if (_clickThrough || (!ReferenceEquals(e.OriginalSource, o.HitArea) && !ReferenceEquals(e.OriginalSource, o.Scaled))) return;
            if (o.Panel.Visibility == Visibility.Visible)
            {
                HidePanel();
                return;
            }
            if (e.ClickCount >= 2)
            {
                _clickTimer.Stop();
                _main.ToggleFullscreen();
                return;
            }
            _clickTimer.Stop();
            _clickTimer.Start();
        };
        // Thanh trượt tự đánh dấu đã xử lý sự kiện chuột (nhảy tới chỗ bấm) → nghe cả sự kiện đã xử lý
        o.Seek.AddHandler(UIElement.PreviewMouseLeftButtonDownEvent, new MouseButtonEventHandler((_, _) => _dragging = true), true);
        o.Seek.AddHandler(UIElement.PreviewMouseLeftButtonUpEvent, new MouseButtonEventHandler((_, _) =>
        {
            if (!_dragging) return;
            _dragging = false;
            if (_state is { D: > 0 }) SeekTo(o.Seek.Value);
        }), true);
        o.Seek.ValueChanged += (_, _) =>
        {
            if (_dragging && _state is { D: > 0 } s) o.TimeText.Text = $"{Util.FormatTime(o.Seek.Value)} / {Util.FormatTime(s.D)}";
        };
        o.Deactivated += (_, _) => _repeat = 0;
    }

    private void ShowOsd(int ms = 4000)
    {
        if (!IsOpen || _clickThrough || IsMini) return;
        _ov.Osd.Visibility = Visibility.Visible;
        _ov.TopBar.Visibility = Visibility.Visible;
        UpdateOsd();
        _osdTimer.Stop();
        _osdTimer.Interval = TimeSpan.FromMilliseconds(ms);
        _osdTimer.Start();
    }

    private void HideOsd()
    {
        _osdTimer.Stop();
        // Đang dừng / chuột đang ở trên thanh / đang tua: giữ thanh
        if (_state?.Paused == true || _seekTarget != null || _dragging || _ov.Osd.IsMouseOver || _ov.TopBar.IsMouseOver ||
            _ov.Panel.Visibility == Visibility.Visible || !_started)
        {
            _osdTimer.Interval = TimeSpan.FromSeconds(2);
            _osdTimer.Start();
            return;
        }
        _ov.Osd.Visibility = Visibility.Collapsed;
        _ov.TopBar.Visibility = Visibility.Collapsed;
        _ov.HitArea.Cursor = Cursors.None;
    }

    private void UpdateOsd()
    {
        var s = _state;
        var target = _seekTarget;
        var t = target ?? s?.T ?? 0;
        var d = s?.D ?? 0;
        if (!_dragging)
        {
            _ov.TimeText.Text = d > 0 ? $"{Util.FormatTime(t)} / {Util.FormatTime(d)}" : Util.FormatTime(t);
            _ov.Seek.Maximum = Math.Max(1, d);
            _ov.Seek.Value = Math.Min(t, _ov.Seek.Maximum);
        }
        _ov.StateText.Text = s == null ? "Đang tải"
            : target != null ? (target >= s.T ? "Tua tới" : "Tua lùi")
            : s.Waiting ? "Đang tải"
            : s.Paused ? "Tạm dừng"
            : Math.Abs(_rate - 1) > 0.01 ? $"Đang phát · {RateText(_rate)}" : "Đang phát";
        _ov.BtnPlay.Content = s is { Paused: false } ? "⏸  Dừng" : "▶  Phát";
        _ov.BtnPrev.IsEnabled = _index > 0;
        _ov.BtnNext.IsEnabled = HasNext;
    }

    public void Notice(string text, int ms = 4000)
    {
        if (!IsOpen || IsMini) return;
        _ov.NoticeText.Text = text;
        _ov.NoticeBox.Visibility = Visibility.Visible;
        _noticeTimer.Stop();
        _noticeTimer.Interval = TimeSpan.FromMilliseconds(ms);
        _noticeTimer.Start();
    }

    private void ShowLoading(string text)
    {
        _ov.LoadingBox.Visibility = Visibility.Visible;
        _ov.LoadingBar.Visibility = Visibility.Visible;
        _ov.LoadingText.Text = text;
    }

    private void ShowError(string text)
    {
        _ov.LoadingBox.Visibility = Visibility.Visible;
        _ov.LoadingBar.Visibility = Visibility.Collapsed;
        _ov.LoadingText.Text = text;
        ShowOsd(8000);
    }

    private void HideLoading() => _ov.LoadingBox.Visibility = Visibility.Collapsed;

    private static string RateText(double r) => (r % 1 == 0 ? ((int)r).ToString() : r.ToString(CultureInfo.InvariantCulture).Replace('.', ',')) + "×";

    // ---- Bảng tuỳ chọn ----

    private void Section(string title, string? note = null)
    {
        _ov.PanelStack.Children.Add(Ui.Text(title, 16, weight: FontWeights.SemiBold, margin: new Thickness(0, 14, 0, 8)));
        if (note != null) _ov.PanelStack.Children.Add(Ui.Dim(note, 13, new Thickness(0, -4, 0, 8)));
    }

    private WrapPanel Row()
    {
        var r = new WrapPanel();
        _ov.PanelStack.Children.Add(r);
        return r;
    }

    private void ShowPanel()
    {
        if (!IsOpen) return;
        var p = _ov.PanelStack;
        p.Children.Clear();
        _ov.PanelTitle.Text = _ov.TitleText.Text;
        Button? focus = null;

        var src = _source;
        if (src != null && !src.PageOnly && src.Servers.Count > 0)
        {
            Section("Server", "Video giật / không chạy: chọn server khác (app nhớ cho lần sau).");
            var row = Row();
            foreach (var s in src.Servers)
            {
                var active = s.Type == _server?.Type;
                var b = Ui.Pill(s.Label, () => SwitchServer(s), active ? "active" : null);
                row.Children.Add(b);
                if (active) focus = b;
            }
        }
        else if (src is { PageOnly: true })
        {
            Section("Server", "Trang này không chia server: app mở thẳng trang tập phim.");
        }

        if (_detail is { Groups.Count: > 1 } d)
        {
            // Phim chia mùa (dexter.pw): mỗi nhóm là một mùa — chọn mùa là mở tập đầu
            Section(d.Seasons ? "Mùa" : "Bản");
            var row = Row();
            foreach (var g in d.Groups)
                row.Children.Add(Ui.Pill(g.Name, () => SwitchGroup(g), g == _group ? "active" : null));
        }

        // Phụ đề rời (dexter.pw): chỉ hiện khi tập có phụ đề chọn được
        _subButton = null;
        if (_subs.Count > 0)
        {
            Section("Phụ đề", "Đổi bản / tắt phụ đề; chữ lệch với tiếng thì dời sớm / muộn (app nhớ cho tập này).");
            var row = Row();
            _subButton = Ui.Pill(SubText(), CycleSub);
            row.Children.Add(_subButton);
            row.Children.Add(Ui.Pill("Chữ sớm hơn 0,5 s", () => NudgeSub(-0.5)));
            row.Children.Add(Ui.Pill("Chữ muộn hơn 0,5 s", () => NudgeSub(0.5)));
            // Tệp phụ đề hỏng thì trang đã tự sang tệp sau — hỏi lại cho đúng chữ trên nút
            _ = SyncSubIdx();
        }

        Section("Xem");
        var view = Row();
        var prev = Ui.Pill("⏮  Tập trước", () =>
        {
            HidePanel();
            Prev();
        });
        prev.IsEnabled = _index > 0;
        var next = Ui.Pill("Tập sau  ⏭", () =>
        {
            HidePanel();
            Next();
        });
        next.IsEnabled = HasNext;
        view.Children.Add(prev);
        view.Children.Add(next);
        view.Children.Add(Ui.Pill("↻  Tải lại", Reload));
        focus ??= next.IsEnabled ? next : prev;

        var rates = Row();
        foreach (var r in Rates)
        {
            var rr = r;
            rates.Children.Add(Ui.Pill(RateText(r), () =>
            {
                _rate = rr;
                Cmd("rate", rr);
                Notice($"Tốc độ {RateText(rr)}");
                ShowPanel();
            }, Math.Abs(_rate - r) < 0.01 ? "active" : null, 64));
        }
        var fitRow = Row();
        fitRow.Children.Add(Ui.Pill(Prefs.VideoFill ? "Khung hình: Lấp đầy" : "Khung hình: Vừa màn hình", () =>
        {
            Prefs.VideoFill = !Prefs.VideoFill;
            Cmd("fit", Prefs.VideoFill ? 1 : 0);
            ShowPanel();
        }));
        // Chất lượng (luồng HLS có nhiều bản, vd 4K của dexter.pw): nút ẩn tới khi hls.html báo từ hai bản trở lên
        _qualityButton = null;
        if (_hlsUrl != null && !_nativeOn)
        {
            _qualityButton = Ui.Pill("Chất lượng", CycleQuality);
            _qualityButton.Visibility = Visibility.Collapsed;
            fitRow.Children.Add(_qualityButton);
            _ = SyncQuality();
        }

        // Nhạc mở đầu
        var t = (int)(_state?.T ?? 0);
        var mark = Prefs.Intro(IntroKey);
        Section("Nhạc mở đầu (cả phim)", mark == null ? "Chưa đánh dấu — đang ở đầu / cuối nhạc thì bấm nút tương ứng."
            : mark.Value.End < 0 ? $"Bắt đầu {Util.FormatTime(mark.Value.Start)}, chưa đánh dấu chỗ hết."
            : $"{Util.FormatTime(mark.Value.Start)} → {Util.FormatTime(mark.Value.End)}, tự bỏ qua ở các tập.");
        var intro = Row();
        var bStart = Ui.Pill($"Bắt đầu ở {Util.FormatTime(t)}", () =>
        {
            HidePanel();
            var end = Prefs.Intro(IntroKey) is { } m && m.End > t ? m.End : -1;
            Prefs.SetIntro(IntroKey, t, end);
            Notice(end > t ? $"Nhạc mở đầu: {Util.FormatTime(t)} → {Util.FormatTime(end)}, các tập sẽ tự bỏ qua."
                : "Đã đánh dấu đầu nhạc mở đầu. Hết nhạc thì nhấn ▼ → «Hết nhạc ở …».", 6000);
        });
        var bEnd = Ui.Pill($"Hết nhạc ở {Util.FormatTime(t)}", () =>
        {
            HidePanel();
            var start = Prefs.Intro(IntroKey) is { } m && m.Start < t ? m.Start : 0;
            Prefs.SetIntro(IntroKey, start, t);
            _introDone = true;
            Notice($"Nhạc mở đầu: {Util.FormatTime(start)} → {Util.FormatTime(t)}, các tập sẽ tự bỏ qua.", 6000);
        });
        bStart.IsEnabled = bEnd.IsEnabled = _started;
        intro.Children.Add(bStart);
        intro.Children.Add(bEnd);
        if (mark != null)
            intro.Children.Add(Ui.Pill("Bỏ mốc", () =>
            {
                HidePanel();
                Prefs.ClearIntro(IntroKey);
                Notice("Đã bỏ mốc nhạc mở đầu của phim này.");
            }));

        // Danh sách tập
        if (Episodes.Count > 1)
        {
            Section($"Tập ({Episodes.Count})");
            var eps = Row();
            var watched = Prefs.WatchedEpisodes();
            for (var i = 0; i < Episodes.Count; i++)
            {
                var idx = i;
                var e = Episodes[i];
                var b = Ui.Pill(e.Title, () =>
                {
                    HidePanel();
                    OpenEpisode(idx);
                }, i == _index ? "active" : watched.Contains(e.Url) ? "watched" : null, 84);
                b.Padding = new Thickness(10, 7, 10, 7);
                eps.Children.Add(b);
            }
        }

        Section("Khác");
        var other = Row();
        other.Children.Add(Ui.Pill("▣  Thu nhỏ (cửa sổ nhỏ)", EnterMini));
        other.Children.Add(Ui.Pill("🖱  Bấm thẳng vào trang", EnterClickThrough));
        other.Children.Add(Ui.Pill("↗  Mở trang tập trên trình duyệt", () =>
        {
            HidePanel();
            Ui.OpenBrowser(_episodeUrl);
        }));
        other.Children.Add(Ui.Pill("✕  Thoát phim", () => Close()));

        _ov.Panel.Visibility = Visibility.Visible;
        _ov.HitArea.Cursor = Cursors.Arrow;
        ShowOsd();
        var f = focus;
        Dispatcher.BeginInvoke(() =>
        {
            _ov.Activate();
            if (f != null) KeyNav.FocusAndShow(f);
        }, DispatcherPriority.Loaded);
    }

    private void HidePanel()
    {
        _ov.Panel.Visibility = Visibility.Collapsed;
        Keyboard.Focus(_ov);
        _ov.Focus();
    }

    /// Chế độ bấm thẳng vào trang (trình phát cần bấm nút riêng, vd «Bấm để xem»): bỏ nền của lớp phủ để chuột đi xuyên.
    private void EnterClickThrough()
    {
        HidePanel();
        _clickThrough = true;
        _ov.Osd.Visibility = Visibility.Collapsed;
        _ov.TopBar.Visibility = Visibility.Collapsed;
        _ov.HitArea.Background = null;
        _ov.HitArea.Cursor = Cursors.Arrow;
        _ov.BtnDone.Visibility = Visibility.Visible;
        Notice("Dùng chuột bấm vào trang. Xong thì bấm nút «Xong» ở trên cùng.", 6000);
    }

    private void ExitClickThrough()
    {
        if (!_clickThrough) return;
        _clickThrough = false;
        _ov.HitArea.Background = new SolidColorBrush(Color.FromArgb(1, 0, 0, 0));
        _ov.BtnDone.Visibility = Visibility.Collapsed;
        _ov.Activate();
        ShowOsd();
    }

#if DEBUG
    /// Cổng gỡ lỗi: chạy JS trong trình phát.
    public async Task<string> Js(string code) => _web.CoreWebView2 == null ? "null" : await _web.CoreWebView2.ExecuteScriptAsync(code);

    public string Debug() =>
        $"native={(_nativeOn && _native != null ? "on " + _native.Debug() : _extract ? "đang lấy link từ trình phát nhúng" : $"off (forceWeb={_forceWeb})")} " +
        $"open={IsOpen} pageMode={_pageMode} started={_started} server={_server?.Label} ep={_episodeUrl} t={_state?.T:0.0} d={_state?.D:0.0} " +
        $"paused={_state?.Paused} embed={_embedUrl} hls={_hlsUrl} load={_pageLoad} " +
        $"subs={_subs.Count} subIdx={_subIdx} subDelay={_subDelay.ToString(CultureInfo.InvariantCulture)} group={_group?.Name} " +
        $"quality={Prefs.VideoQuality} levels={string.Join("/", _levels)} " +
        $"web={(_web.CoreWebView2 != null ? "sẵn sàng" : _init == null ? "chưa tạo" : "đang tạo " + _init.Status)} " +
        $"webVis={_web.Visibility} webSize={_web.ActualWidth:0}x{_web.ActualHeight:0} webLoaded={_web.IsLoaded}";

    /// Cổng gỡ lỗi: giả mpv báo lỗi giữa chừng (thử đường tự chuyển sang WebView2).
    public string DebugNativeFail()
    {
        if (!_nativeOn || _native == null) return "không đang phát native";
        _native.DebugFail();
        return "ok";
    }

    /// Cổng gỡ lỗi: giả trang còn «đang tìm» phụ đề (bỏ danh sách đang có rồi hỏi lại địa chỉ list như Video.SubsLater) — thử
    /// đường PollSubs mà không cần tìm tập ít người xem.
    public string DebugSubsLater(string listUrl)
    {
        if (_hlsUrl == null || _nativeOn) return "không đang phát bằng hls.html";
        _subs = new();
        _subIdx = -1;
        ShowSubButton();
        PageJs("window.__chimtoSubs&&__chimtoSubs([],-1)");
        _ = PollSubs(listUrl, _token);
        return "ok";
    }

    public PlayerOverlay Overlay => _ov;
#endif
}
