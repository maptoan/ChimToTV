package vn.mtv.tv

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Metadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.metadata.icy.IcyHeaders
import androidx.media3.extractor.metadata.icy.IcyInfo
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import kotlin.math.ceil

/**
 * Trình phát truyền hình / radio (IPTV) — ExoPlayer, toàn màn hình. Cùng cách chạy với trình phát trực tiếp của bản Legacy
 * (CBLivePlayerViewController): nguồn lỗi tự sang nguồn sau; hỏng hết nguồn thì tải lại danh sách (cũ quá 10 phút) tìm link
 * mới một lần; trận sắp / đang đá mà chưa có hình thì chờ, tải lại danh sách, thử lại mỗi 20 giây; link giả «no-signal» và
 * danh sách phát hình chờ ngắn không phát; đài radio (danh sách ghi, hoặc luồng không có hình) hiện thẻ đài + tên bài ICY.
 *
 * Remote: ▲ / ▼ (CH+ / CH−) đổi kênh, OK hiện nút, ◀ / MENU mở bảng kênh, số 0–9 gõ số kênh, ▶ hiện nút; phim (VOD) thì ◀ ▶
 * tua 10 giây; Back đóng bảng / nút rồi mới thoát.
 */
@OptIn(UnstableApi::class)
class LiveActivity : BaseActivity() {
    override val touchBackLabel: String? = null

    companion object {
        private const val SNIFF_TTL = 10 * 60 * 1000L
        private const val HIDE_MS = 6000L

        /** Mở kênh ở vị trí [index] của danh sách lọc [filter] (lưới kênh / bảng kênh dùng cùng thứ tự). */
        fun open(a: Activity, filter: ChannelFilter, index: Int, channelId: Long) {
            val i = Intent(a, LiveActivity::class.java)
            filter.extras(i)
            i.putExtra("index", index)
            i.putExtra("id", channelId)
            a.startActivity(i)
        }

        /**
         * Mở kênh theo khoá (kênh vừa xem ở trang chủ, nhắc giờ phát): trong cả danh sách đang chọn. [listId] = danh sách
         * của kênh (kênh vừa xem): danh sách đang chọn không có kênh này thì đổi sang danh sách đó.
         */
        fun playKey(a: Activity, key: String, listId: String? = null) {
            fun go() {
                inBackground({ Iptv.store()?.byKey(key) }) { r ->
                    val c = r.getOrNull()
                    val other = listId?.takeIf { id -> id != Iptv.activeList?.id && Iptv.lists.any { it.id == id } }
                    if (c == null && other != null) {
                        Iptv.setActive(other)
                        playKey(a, key)
                        return@inBackground
                    }
                    if (c == null) {
                        android.widget.Toast.makeText(a, "Danh sách kênh đang chọn không có kênh này.", android.widget.Toast.LENGTH_LONG).show()
                        return@inBackground
                    }
                    open(a, ChannelFilter(), -1, c.id)
                }
            }
            if (Iptv.store() != null) go() else Iptv.refresh(false) { err ->
                if (err != null) android.widget.Toast.makeText(a, err, android.widget.Toast.LENGTH_LONG).show() else go()
            }
        }
    }

    private lateinit var pager: ChannelPager
    private var index = 0
    private var channel: Channel? = null
    private var source = 0
    private var player: ExoPlayer? = null
    private var token = 0
    private var started = false
    private var startedAt = 0L
    private var closed = false
    private var reopens = 0
    private var retriedReset = false
    private var listChecked = false
    private var srcUrl: String? = null
    private var srcKind = ""
    private val sniffed = HashMap<String, Pair<Long, LiveSniff.Result>>()
    private var baseBehind = -1L
    private var limitShowing = false
    private var limitWarned = false
    private var pausedLive = false
    /** Màn đang dừng (onStop → onStart): việc chờ không được mở luồng. */
    private var stopped = false

    // Đài radio: thẻ đài thay cho hình (danh sách ghi là radio, hoặc luồng không có hình)
    private var radio = false
    private var radioTitle: String? = null
    private var radioDesc: String? = null
    private var sawVideo = false
    private var programmes: List<Programme> = emptyList()

    private lateinit var video: PlayerView
    private lateinit var top: View
    private lateinit var bottom: View
    private lateinit var numView: TextView
    private lateinit var titleView: TextView
    private lateinit var badge: TextView
    private lateinit var clockView: TextView
    private lateinit var progView: TextView
    private lateinit var nextView: TextView
    private lateinit var timeView: TextView
    private lateinit var progress: ProgressBar
    private lateinit var btnPlay: TextView
    private lateinit var btnLive: TextView
    private lateinit var btnSource: TextView
    private lateinit var btnFav: TextView
    private lateinit var btnGuide: TextView
    private lateinit var status: View
    private lateinit var spinner: View
    private lateinit var statusText: TextView
    private lateinit var statusButtons: View
    private lateinit var btnRetry: TextView
    private lateinit var btnSkip: TextView
    private lateinit var radioCard: View
    private lateinit var radioLogo: ImageView
    private lateinit var radioIcon: View
    private lateinit var radioName: TextView
    private lateinit var radioSong: TextView
    private lateinit var radioInfo: TextView
    private lateinit var panel: View
    private lateinit var panelTitle: TextView
    private lateinit var panelList: RecyclerView
    private lateinit var digitsView: TextView
    private lateinit var notice: Notice
    private val panelAdapter = PanelAdapter()

    private var controls = false
    private val hide = Runnable { autoHide() }
    private var digits = ""
    private val digitsGo = Runnable { jumpToNumber() }
    private val ticker = object : Runnable {
        override fun run() {
            tick()
            mainHandler.postDelayed(this, 1000)
        }
    }
    private var ticks = 0
    private val changed: () -> Unit = { onIptvChanged() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Phim đang thu nhỏ (hình trong hình / cửa sổ nhỏ trong app): tắt phim đó (lưu chỗ xem) — không thì hai luồng tiếng
        // chạy cùng lúc và phím phát / chuyển tập của remote điều khiển phim thay vì kênh
        PlayerActivity.closePip()
        setContentView(R.layout.activity_live)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        video = findViewById(R.id.video)
        video.resizeMode = if (Prefs.videoFill) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT
        top = findViewById(R.id.top)
        bottom = findViewById(R.id.bottom)
        numView = findViewById(R.id.num)
        titleView = findViewById(R.id.title)
        badge = findViewById(R.id.badge)
        clockView = findViewById(R.id.clock)
        progView = findViewById(R.id.prog)
        nextView = findViewById(R.id.next)
        timeView = findViewById(R.id.time)
        progress = findViewById(R.id.progress)
        btnPlay = findViewById(R.id.btnPlay)
        btnLive = findViewById(R.id.btnLive)
        btnSource = findViewById(R.id.btnSource)
        btnFav = findViewById(R.id.btnFav)
        btnGuide = findViewById(R.id.btnGuide)
        status = findViewById(R.id.status)
        spinner = findViewById(R.id.spinner)
        statusText = findViewById(R.id.statusText)
        statusButtons = findViewById(R.id.statusButtons)
        btnRetry = findViewById(R.id.btnRetry)
        btnSkip = findViewById(R.id.btnSkip)
        radioCard = findViewById(R.id.radioCard)
        radioLogo = findViewById(R.id.radioLogo)
        radioIcon = findViewById(R.id.radioIcon)
        radioName = findViewById(R.id.radioName)
        radioSong = findViewById(R.id.radioSong)
        radioInfo = findViewById(R.id.radioInfo)
        panel = findViewById(R.id.panel)
        panelTitle = findViewById(R.id.panelTitle)
        panelList = findViewById(R.id.panelList)
        digitsView = findViewById(R.id.digits)
        notice = Notice(findViewById(R.id.notice))

        findViewById<View>(R.id.btnChannels).setOnClickListener { togglePanel() }
        findViewById<View>(R.id.btnPrev).setOnClickListener { previousChannel() }
        findViewById<View>(R.id.btnNext).setOnClickListener { nextChannel() }
        findViewById<View>(R.id.btnClose).setOnClickListener { close() }
        btnPlay.setOnClickListener { togglePlay() }
        btnLive.setOnClickListener { goLive() }
        btnSource.setOnClickListener { nextSource() }
        btnGuide.setOnClickListener { openGuide() }
        btnFav.setOnClickListener { toggleFavorite() }
        btnRetry.setOnClickListener { retry() }
        btnSkip.setOnClickListener { nextChannel() }
        findViewById<View>(R.id.btnBack).setOnClickListener { close() }
        for (id in listOf(R.id.btnChannels, R.id.btnPrev, R.id.btnPlay, R.id.btnNext, R.id.btnLive, R.id.btnSource, R.id.btnGuide,
            R.id.btnFav, R.id.btnClose)) {
            findViewById<View>(id).setOnFocusChangeListener { _, has -> if (has) bumpHide() }
        }

        panelList.layoutManager = LinearLayoutManager(this)
        panelList.adapter = panelAdapter
        panelList.itemAnimator = null

        // Chạm: hiện / ẩn nút; vuốt lên / xuống đổi kênh (như bản iPad / Legacy)
        val gestures = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (panel.visibility == View.VISIBLE) togglePanel() else setControls(!controls)
                return true
            }

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                if (abs(vy) > abs(vx) * 1.5f && abs(vy) > 800) {
                    if (vy < 0) nextChannel() else previousChannel()
                    return true
                }
                return false
            }
        })
        video.setOnTouchListener { _, ev -> gestures.onTouchEvent(ev); true }

        player = ExoPlayer.Builder(this, DefaultRenderersFactory(this).setEnableDecoderFallback(true))
            .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true)
            .build().also {
                it.addListener(listener)
                video.player = it
            }

        pager = ChannelPager(ChannelFilter.from(intent))
        pager.onRange = { start, n -> panelAdapter.notifyItemRangeChanged(start, n) }
        val wantId = intent.getLongExtra("id", -1)
        index = intent.getIntExtra("index", 0)
        showLoading("Đang mở kênh…")
        setControls(true)
        Iptv.listen(changed)
        pager.reload {
            if (pager.size == 0) {
                showError("Danh sách không còn kênh này.", retry = false)
                return@reload
            }
            val at = pager.indexOf(wantId)
            // Mở theo mã kênh (nhắc giờ phát, Kênh vừa xem, trang chủ) mà kênh bị lọc khỏi danh sách đang xem (hồ sơ trẻ em, kênh
            // máy này không phát được đang ẩn): báo, đừng lặng lẽ phát kênh số 1
            if (at < 0 && wantId >= 0 && index < 0) {
                showError("Kênh này không có trong danh sách đang xem.", retry = false)
                return@reload
            }
            playChannelAt(if (at >= 0) at else index.coerceIn(0, pager.size - 1))
        }
        mainHandler.post(ticker)
    }

    override fun onDestroy() {
        Iptv.unlisten(changed)
        mainHandler.removeCallbacks(ticker)
        mainHandler.removeCallbacks(hide)
        mainHandler.removeCallbacks(digitsGo)
        player?.release()
        player = null
        super.onDestroy()
    }

    override fun onStop() {
        super.onStop()
        // Ra màn hình chính / mở Lịch đè lên: dừng hẳn (truyền hình trực tiếp — quay lại thì mở lại đúng giờ phát). Huỷ mọi việc
        // đang chờ (mở lại sau 1 giây, thử lại trận sau 20 giây, đọc kênh) và nhịp mỗi giây: không thì luồng tự phát lại khi app
        // đang ở nền, và nhịp đếm giờ xem của màn đã dừng làm lệch giờ xem của trình phát khác
        if (!closed && !isFinishing) {
            WatchLimit.flush()
            token++
            teardown()
            started = false
            pausedLive = true
            stopped = true
            mainHandler.removeCallbacks(ticker)
        }
    }

    override fun onStart() {
        super.onStart()
        if (stopped && !closed) {
            stopped = false
            mainHandler.removeCallbacks(ticker)
            mainHandler.post(ticker)
        }
        // Quay lại app sau ≥ 30 phút phải hỏi "Ai đang xem?": onResume (BaseActivity) đưa về màn chính — đừng mở luồng
        if (AppUse.isIdleReturn(this) && ProfileUi.mustAsk()) return
        if (pausedLive && !closed) {
            pausedLive = false
            reopens = 0
            // Hết giờ xem (màn «Hết giờ xem» còn đó, hoặc tới giờ lúc app ở nền): không mở lại luồng sau màn chặn — cho xem thêm
            // xong thì màn đó tự mở lại kênh
            if (limitShowing || WatchLimit.block() != null) {
                checkLimit()
                return
            }
            if (channel == null) playChannelAt(index) else openSource()
        }
    }

    private fun close() {
        if (closed) return
        closed = true
        token++
        WatchLimit.flush()
        teardown()
        finish()
    }

    // ---- Kênh / nguồn ----

    private fun playChannelAt(i: Int) {
        if (i !in 0 until pager.size) return
        index = i
        val t = ++token
        teardown()
        showLoading("Đang mở kênh…")
        pager.fetch(i) { c ->
            if (t != token || closed) return@fetch
            if (c == null) {
                showError("Không đọc được kênh này.", retry = false)
                return@fetch
            }
            channel = c
            source = 0
            listChecked = false
            while (source + 1 < c.urls.size && Iptv.badReason(c.urls[source]) != null) source++
            retriedReset = false
            reopens = 0
            loadProgrammes()
            openSource()
            scrollPanelToCurrent()
        }
    }

    private fun nextChannel() {
        if (pager.size < 2) return
        playChannelAt((index + 1) % pager.size)
        bumpHide()
    }

    private fun previousChannel() {
        if (pager.size < 2) return
        playChannelAt((index - 1 + pager.size) % pager.size)
        bumpHide()
    }

    private fun nextSource() {
        val c = channel ?: return
        if (c.urls.size < 2) return
        source = (source + 1) % c.urls.size
        retriedReset = false
        reopens = 0
        openSource()
        bumpHide()
    }

    private fun loadProgrammes() {
        val c = channel ?: return
        inBackground({ Iptv.guide()?.programmes(c) ?: emptyList() }) { r ->
            if (c.id != channel?.id) return@inBackground
            programmes = r.getOrDefault(emptyList())
            updateLabels()
        }
    }

    private fun openSource() {
        val t = ++token
        teardown()
        // Màn đã dừng (app ở nền) hoặc đang hiện «Hết giờ xem»: không mở luồng (quay lại app thì onStart mở)
        if (stopped || limitShowing) {
            if (stopped) pausedLive = true
            return
        }
        started = false
        pausedProgressive = false
        baseBehind = -1
        radioTitle = null
        radioDesc = null
        sawVideo = false
        val c = channel ?: return
        // Danh sách ghi là đài radio: hiện thẻ đài ngay lúc mở (đài không ghi thì nhận ra khi phát — luồng không có hình)
        setRadio(c.radio)
        updateLabels()
        panelAdapter.notifyDataSetChanged()
        if (source >= c.urls.size) {
            showError("${c.n} không có địa chỉ phát.", retry = false)
            return
        }
        val url = c.urls[source]
        srcUrl = url
        Iptv.badReason(url)?.let {
            sourceFailed(it, permanent = true, t = t)
            return
        }
        if (IptvParse.isPlaceholder(url)) {
            // Link giả «không có tín hiệu»: đừng phát hình chờ (hết 10 s lại mở lại) — coi như nguồn chưa có
            sourceFailed("Danh sách chưa có link phát (đang để hình «không có tín hiệu»).", permanent = false, t = t)
            return
        }
        showLoading("Đang mở ${c.n}…")
        when (IptvParse.extOf(url)) {
            "m3u8" -> start(url, "hls", null, t)
            "mpd" -> start(url, "dash", null, t)
            "flv", "ts", "mp4", "mkv", "avi", "mov", "m4v", "webm", "mp3", "aac", "ogg", "opus", "mpg", "mpeg" ->
                start(url, "progressive", IptvParse.extOf(url), t)
            else -> sniffThenOpen(url, t)
        }
    }

    /** Link không rõ loại theo đuôi: đọc vài KB đầu rồi mở đúng kiểu. Nhớ kết quả 10 phút (mở lại khi luồng đứt). */
    private fun sniffThenOpen(url: String, t: Int) {
        sniffed[url]?.let { (at, r) ->
            if (System.currentTimeMillis() - at < SNIFF_TTL) {
                openSniffed(r, url, t)
                return
            }
        }
        val c = channel ?: return
        val headers = c.headersFor(url)
        val t0 = System.currentTimeMillis()
        inBackground({ LiveSniff.sniff(url, headers) }) { res ->
            if (t != token || closed) return@inBackground
            val r = res.getOrNull() ?: LiveSniff.Result("error", error = res.exceptionOrNull()?.message)
            TvLog.i("Truyền hình: nhận dạng $url → ${r.kind}${r.codec?.let { " $it" } ?: ""} (HTTP ${r.status}, ${System.currentTimeMillis() - t0} ms)" +
                (r.error?.let { " — $it" } ?: ""))
            if (r.kind in setOf("hls", "dash", "progressive")) sniffed[url] = System.currentTimeMillis() to r
            openSniffed(r, url, t)
        }
    }

    private fun openSniffed(r: LiveSniff.Result, url: String, t: Int) {
        // Danh sách .pls / .m3u của đài: mở địa chỉ luồng ghi bên trong; còn lại giữ link gốc (link chuyển tiếp đổi token mỗi lần)
        val target = r.url ?: url
        when (r.kind) {
            "hls", "dash", "progressive" -> start(target, r.kind, r.codec, t)
            "http" -> sourceFailed("Link không còn phát (máy chủ trả lỗi HTTP ${r.status}).", permanent = false, t = t)
            "html" -> sourceFailed("Link dẫn tới một trang web, không phải luồng phát.", permanent = false, t = t)
            "playlist" -> sourceFailed("Danh sách của đài không ghi địa chỉ luồng nào dùng được.", permanent = false, t = t)
            else -> sourceFailed("Máy chủ của kênh không trả lời." + (r.error?.let { "\n($it)" } ?: ""), permanent = false, t = t)
        }
    }

    /** Mở một nguồn đã biết loại: hls | dash | progressive (FLV / TS liền / MP4 / MKV / đài MP3 · AAC…). */
    private fun start(url: String, kind: String, codec: String?, t: Int) {
        val c = channel ?: return
        val p = player ?: return
        srcKind = kind
        TvLog.i("Truyền hình: ${c.n} (nguồn ${source + 1}/${c.urls.size}) $kind${codec?.let { " $it" } ?: ""} $url")
        val item = MediaItem.Builder().setUri(url).apply {
            when (kind) {
                "hls" -> setMimeType(MimeTypes.APPLICATION_M3U8)
                "dash" -> setMimeType(MimeTypes.APPLICATION_MPD)
            }
        }.build()
        val factory = DefaultMediaSourceFactory(LiveHttpDataSource.Factory(c.headersFor(url)))
        p.setMediaSource(factory.createMediaSource(item))
        p.prepare()
        p.playWhenReady = true
        if (codec in setOf("mp3", "aac", "ogg", "opus") && !c.vod) {
            radioDesc = codec!!.uppercase()
            setRadio(true)
        }
        startedAt = System.currentTimeMillis()
    }

    private fun teardown() {
        player?.let {
            it.stop()
            it.clearMediaItems()
        }
    }

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            val p = player ?: return
            when (state) {
                Player.STATE_READY -> {
                    if (!started) onStarted()
                    spinner.visibility = View.GONE
                    if (status.visibility == View.VISIBLE && started && statusButtons.visibility != View.VISIBLE) hideStatus()
                }
                Player.STATE_BUFFERING -> if (started) showSpinnerOnly()
                Player.STATE_ENDED -> onEnded()
                Player.STATE_IDLE -> Unit
            }
            updatePlayButton(p)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            player?.let(::updatePlayButton)
            updateBadge()
        }

        override fun onPlayerError(error: PlaybackException) = onError(error)

        override fun onTracksChanged(tracks: Tracks) {
            if (tracks.containsType(C.TRACK_TYPE_VIDEO)) sawVideo = true
            radioTick()
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            if (videoSize.width > 0) {
                sawVideo = true
                radioTick()
            }
        }

        override fun onMetadata(metadata: Metadata) {
            for (i in 0 until metadata.length()) {
                val e = metadata.get(i)
                if (e is IcyInfo) {
                    val title = e.title?.trim()?.takeIf { it.isNotEmpty() }
                    if (title != radioTitle) {
                        title?.let { TvLog.i("Truyền hình: ${channel?.n} đang phát «$it»") }
                        radioTitle = title
                        updateRadioCard()
                    }
                }
            }
        }
    }

    private fun onStarted() {
        val p = player ?: return
        val c = channel ?: return
        // Danh sách phát là đoạn phim ngắn có hồi kết (hình chờ «không có tín hiệu» thay cho luồng trực tiếp)
        if (srcKind == "hls" && !c.vod && !p.isCurrentMediaItemLive && p.duration in 1..120_000) {
            sourceFailed("Kênh đang phát hình chờ (đoạn phim ngắn), chưa có luồng trực tiếp.", permanent = false, t = token)
            return
        }
        started = true
        startedAt = System.currentTimeMillis()
        listChecked = false // phát được rồi: lần sau hỏng (link hết hạn sau vài giờ) lại được tìm link mới
        hideStatus()
        Iptv.addRecent(c)
        TvLog.i("Truyền hình: ${c.n} đang phát (nguồn ${source + 1})")
        setControls(true)
        radioTick()
    }

    private fun onEnded() {
        val c = channel ?: return
        if (c.vod) {
            teardown()
            showError("Đã xem hết ${c.n}.", retry = true, retryLabel = "Xem lại")
            return
        }
        // Luồng trực tiếp «hết» (máy chủ ngừng phát, mạng chập chờn): mở lại nguồn này sau 1 giây, tối đa 5 lần liền
        reopenLater("hết")
    }

    private fun reopenLater(why: String) {
        val c = channel ?: return
        TvLog.i("Truyền hình: luồng ${c.n} $why — mở lại lần ${reopens + 1}")
        if (++reopens > 5) {
            sourceFailed("Luồng liên tục bị đứt.", permanent = false, t = token)
            return
        }
        val t = token
        showSpinnerOnly()
        mainHandler.postDelayed({ if (t == token && !closed) openSource() }, 1000)
    }

    private fun onError(e: PlaybackException) {
        val c = channel ?: return
        val p = player ?: return
        val cause = e.cause
        TvLog.i("Truyền hình: lỗi phát ${c.n}: ${e.errorCodeName} ${e.message} ${cause?.javaClass?.simpleName} ${cause?.message}")
        when (e.errorCode) {
            PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW -> {
                p.seekToDefaultPosition()
                p.prepare()
                return
            }
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED, PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED -> {
                val why = undecodable(p)
                srcUrl?.let { Iptv.markBad(c, it, why) }
                sourceFailed(why, permanent = true, t = token)
                return
            }
        }
        if (started) {
            // Đang phát thì đứt (mạng chập chờn, máy chủ ngắt): mở lại nguồn này
            reopenLater("đứt (${e.errorCodeName})")
            return
        }
        val why = when (e.errorCode) {
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                "Máy chủ của kênh trả lỗi HTTP ${(cause as? HttpDataSource.InvalidResponseCodeException)?.responseCode ?: ""}.".replace(" .", ".")
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "Không kết nối được máy chủ của kênh."
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "Máy chủ của kênh không trả lời (quá giờ chờ)."
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED, PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED, PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED ->
                "Không đọc được luồng của kênh (định dạng lạ hoặc hỏng)."
            else -> "Không mở được luồng (${e.errorCodeName})."
        }
        sourceFailed(why, permanent = false, t = token)
    }

    /** Lý do máy không giải mã được: mã hình / khổ của luồng. */
    private fun undecodable(p: ExoPlayer): String {
        val f = p.videoFormat
        val mime = f?.sampleMimeType ?: f?.codecs
        val codec = when {
            mime == null -> null
            "hevc" in mime || "hvc1" in mime || "hev1" in mime -> "H.265 (HEVC)"
            "av01" in mime || "av1" in mime -> "AV1"
            "vp9" in mime -> "VP9"
            "avc" in mime -> "H.264"
            else -> mime
        }
        val size = if (f != null && f.width > 0) " ${f.width}×${f.height}" else ""
        return if (codec != null) "Video $codec$size — máy này không giải mã được." else "Máy không giải mã được định dạng của luồng này."
    }

    private fun sourceFailed(reason: String, permanent: Boolean, t: Int) {
        if (t != token || closed) return
        teardown()
        val c = channel ?: return
        if (Iptv.autoSource && source + 1 < c.urls.size) {
            TvLog.i("Truyền hình: ${c.n} nguồn ${source + 1} lỗi ($reason) — sang nguồn ${source + 2}")
            source++
            retriedReset = false
            reopens = 0
            openSource()
            return
        }
        // Trận sắp / đang đá mà chưa nguồn nào có hình (kênh trận chỉ lên sóng gần giờ bóng lăn, có khi trễ): chờ rồi thử lại từ
        // nguồn đầu — trừ khi mọi nguồn đều đã biết là máy không phát được
        val now = nowSec()
        val ms = c.match(now)
        val usable = c.urls.any { Iptv.badReason(it) == null }
        if (usable && ms in setOf(IptvParse.Match.LATER, IptvParse.Match.SOON, IptvParse.Match.LIVE)) {
            val msg = if (now < c.kick) {
                "Trận chưa bắt đầu — bóng lăn lúc ${IptvParse.kickText(c.kick, now)}.\nKênh chưa lên sóng, tự thử lại sau 20 giây…"
            } else "Trận đang đá nhưng kênh chưa phát hình.\nTự thử lại sau 20 giây…"
            TvLog.i("Truyền hình: ${c.n} chưa có hình ($reason) — chờ 20 s")
            showError(msg, retry = true)
            mainHandler.postDelayed({ if (!closed && t == token && !started) refreshThenRetry() }, 20_000)
            return
        }
        val msg = "${c.n} không phát được.\n$reason"
        // Kênh thường hỏng hết nguồn: link có hạn dùng / người làm danh sách vừa thay link — tải lại danh sách (cũ quá 10 phút)
        // một lần; nguồn của kênh đổi thì thử lại từ đầu
        if (!permanent && !listChecked && ms == IptvParse.Match.NONE && Iptv.activeList?.url != null) {
            listChecked = true
            TvLog.i("Truyền hình: ${c.n} hỏng hết nguồn ($reason) — xem danh sách có link mới không")
            showLoading("Đang kiểm tra link mới…")
            refreshChannel(600) { changed -> if (changed) retryFromFirstSource() else showError(msg, retry = true) }
            return
        }
        showError(msg, retry = !permanent)
    }

    /**
     * Tải lại danh sách nếu cũ hơn [age] giây (đang tải thì chờ), lấy bản mới của kênh đang xem (cùng khoá). done(true) = nguồn
     * của kênh vừa đổi. Không gọi done nếu trong lúc chờ người xem đã đổi kênh / đóng trình phát.
     */
    private fun refreshChannel(age: Int, done: (Boolean) -> Unit) {
        val t = token
        val old = channel ?: return
        Iptv.refreshIfOlderThan(age) {
            if (closed || t != token || started) return@refreshIfOlderThan
            inBackground({ Iptv.store()?.byKey(old.k) }) { r ->
                if (closed || t != token || started) return@inBackground
                val fresh = r.getOrNull()
                val changed = fresh != null && fresh.urls != old.urls
                if (changed) TvLog.i("Truyền hình: ${old.n} — danh sách vừa cập nhật nguồn (${old.urls.size} → ${fresh!!.urls.size})")
                if (fresh != null) {
                    channel = fresh
                    if (fresh.id != old.id || pager.stale) {
                        // Danh sách mới (tệp mới, mã kênh mới): nạp lại thứ tự kênh, giữ kênh đang xem
                        pager.reload {
                            pager.indexOf(fresh.id).takeIf { it >= 0 }?.let { index = it }
                            panelAdapter.notifyDataSetChanged()
                            done(changed)
                        }
                        return@inBackground
                    }
                }
                done(changed)
            }
        }
    }

    /** Kênh trận đấu chờ lên sóng: tải lại danh sách (cũ quá 2 phút), lấy bản mới của kênh rồi thử lại từ nguồn đầu. */
    private fun refreshThenRetry() = refreshChannel(120) { retryFromFirstSource() }

    private fun retryFromFirstSource() {
        val c = channel ?: return
        source = 0
        while (source + 1 < c.urls.size && Iptv.badReason(c.urls[source]) != null) source++
        retriedReset = false
        reopens = 0
        openSource()
    }

    private fun retry() {
        val c = channel ?: return
        if (c.vod && statusText.text.startsWith("Đã xem hết")) {
            openSource()
            return
        }
        showLoading("Đang kiểm tra link mới…")
        // Kênh trận đấu: lấy danh sách mới (link thật hay được thêm sát giờ) rồi thử lại từ nguồn đầu
        if (c.kick > 0) {
            refreshThenRetry()
            return
        }
        // Kênh thường: danh sách cũ quá 10 phút thì tải lại (link có hạn dùng); nguồn đổi thì thử từ đầu, không thì mở lại nguồn này
        if (Iptv.activeList?.url == null) {
            reopens = 0
            openSource()
            return
        }
        refreshChannel(600) { changed ->
            if (changed) retryFromFirstSource() else {
                retriedReset = false
                reopens = 0
                openSource()
            }
        }
    }

    /** Danh sách vừa tải lại / lịch mới: nạp lại thứ tự kênh (giữ kênh đang xem), lịch của kênh. */
    private fun onIptvChanged() {
        if (closed || !pager.stale || Iptv.loading) return
        val cur = channel
        pager.reload {
            if (cur != null) {
                inBackground({ Iptv.store()?.byKey(cur.k) }) { r ->
                    r.getOrNull()?.let { f -> pager.indexOf(f.id).takeIf { it >= 0 }?.let { index = it } }
                    panelAdapter.notifyDataSetChanged()
                }
            }
        }
        loadProgrammes()
    }

    // ---- Phát / dừng / trực tiếp ----

    private fun togglePlay() {
        val p = player
        val c = channel
        // Chưa phát (lỗi / đã dừng), hoặc luồng liền (radio, FLV, TS) đang tạm dừng: mở lại — nghe / xem tiếp đúng giờ phát
        if (p == null || c == null || !started || (p.playbackState == Player.STATE_IDLE)) {
            reopens = 0
            openSource()
            return
        }
        if (p.isPlaying || p.playWhenReady) {
            if (srcKind == "progressive" && !c.vod) {
                teardown()
                started = false
                pausedProgressive = true
                updateBadge()
                updatePlayButton(p)
            } else p.pause()
        } else p.play()
        bumpHide()
    }

    private var pausedProgressive = false

    private fun updatePlayButton(p: ExoPlayer) {
        btnPlay.text = if (p.playWhenReady && p.playbackState != Player.STATE_IDLE) "Tạm dừng" else "Phát"
    }

    private fun goLive() {
        val p = player ?: return
        if (!p.isCurrentMediaItemLive) {
            openSource()
            return
        }
        p.seekToDefaultPosition()
        p.play()
        updateBadge()
    }

    /** Độ trễ sau mép trực tiếp (ms) so với lúc vừa phát. */
    private fun delayMs(): Long {
        val p = player ?: return 0
        if (!started || !p.isCurrentMediaItemLive) return 0
        val off = p.currentLiveOffset
        if (off == C.TIME_UNSET) return 0
        return (off - maxOf(0, baseBehind)).coerceAtLeast(0)
    }

    private fun updateBadge() {
        val p = player
        val c = channel
        val playing = p != null && started && p.isPlaying
        val text: String
        val red: Boolean
        when {
            c?.vod == true -> {
                text = if (!started) "PHIM" else if (playing) "▶ PHIM" else "TẠM DỪNG"
                red = false
            }
            !started -> {
                text = if (pausedProgressive) "TẠM DỪNG" else "CHƯA PHÁT"
                red = false
            }
            else -> {
                val d = delayMs()
                text = when {
                    p?.playWhenReady == false -> "TẠM DỪNG"
                    d > 8000 -> "CHẬM ${formatTime(d / 1000.0)}"
                    else -> "● TRỰC TIẾP"
                }
                red = text == "● TRỰC TIẾP"
            }
        }
        if (badge.text != text) badge.text = text
        badge.backgroundTintList = ColorStateList.valueOf(if (red) 0xFFD93A3F.toInt() else 0xFF5A6068.toInt())
        val showLive = started && c?.vod != true && p?.isCurrentMediaItemLive == true && (delayMs() > 8000 || p.playWhenReady == false)
        val vis = if (showLive) View.VISIBLE else View.GONE
        if (btnLive.visibility != vis) {
            btnLive.visibility = vis
            if (!showLive && btnLive.isFocused) btnPlay.requestFocus()
        }
    }

    // ---- Nhãn ----

    private fun updateLabels() {
        val c = channel ?: return
        numView.text = c.num
        btnSource.text = "Nguồn ${source + 1}/${c.urls.size}"
        btnSource.visibility = if (c.urls.size > 1) View.VISIBLE else View.GONE
        btnFav.text = if (Iptv.isFavorite(c)) "★" else "☆"
        btnGuide.visibility = if (programmes.isNotEmpty()) View.VISIBLE else View.GONE
        panelTitle.text = "${Iptv.activeList?.name ?: "Kênh"} · ${pager.size} kênh"
        updateProgramme()
        updateBadge()
        if (radio) updateRadioCard()
    }

    private fun updateProgramme() {
        val c = channel ?: return
        val now = nowSec()
        val p = programmes.nowAt(now)
        val n = programmes.nextAt(now)
        titleView.text = if (p != null) "${c.n} · ${p.t}" else c.n
        progView.text = p?.t ?: c.n
        val pl = player
        var frac = -1.0
        when {
            c.vod && pl != null && started && pl.duration > 0 -> {
                timeView.text = "${formatTime(pl.currentPosition / 1000.0)} / ${formatTime(pl.duration / 1000.0)} · ◀ ▶ tua 10 giây"
                frac = pl.currentPosition.toDouble() / pl.duration
            }
            p != null -> {
                timeView.text = "${IptvParse.clock(p.s)}–${IptvParse.clock(p.e)} · còn ${maxOf(1, ceil((p.e - now) / 60).toInt())} phút"
                frac = (now - p.s) / (p.e - p.s)
            }
            c.matchLine(now) != null -> {
                timeView.text = "${c.g} · ${c.matchLine(now)}"
                if (c.match(now) == IptvParse.Match.LIVE) frac = (now - c.kick) / (c.dur * 60)
            }
            c.vod -> timeView.text = "${c.g} · phim"
            else -> timeView.text = "${c.g}${if (c.radio || radio) " · 📻 radio" else ""} · chưa có lịch phát sóng"
        }
        nextView.text = n?.let { "Tiếp theo: ${IptvParse.clock(it.s)} ${it.t}" } ?: ""
        progress.visibility = if (frac >= 0) View.VISIBLE else View.INVISIBLE
        if (frac >= 0) progress.progress = (frac.coerceIn(0.0, 1.0) * 1000).toInt()
        clockView.text = IptvParse.clock(now)
    }

    // ---- Radio ----

    /** Bật / tắt kiểu đài radio: thẻ đài thay cho hình, nút không tự ẩn. */
    private fun setRadio(on: Boolean) {
        if (radio != on) {
            radio = on
            if (on) setControls(true) else bumpHide()
        }
        radioCard.visibility = if (on && status.visibility != View.VISIBLE) View.VISIBLE else View.GONE
        if (on) updateRadioCard()
    }

    private var radioLogoUrl: String? = null

    private fun updateRadioCard() {
        val c = channel ?: return
        radioName.text = c.n
        val p = programmes.nowAt(nowSec())
        // Đài chỉ gửi lại tên đài làm «bài đang phát» (BBC…): bỏ, khỏi lặp tên ba lần
        val icy = radioTitle?.takeUnless { it.equals(c.n, ignoreCase = true) }
        radioSong.text = icy?.let { "♪ $it" } ?: p?.t ?: ""
        radioSong.visibility = if (radioSong.text.isNullOrEmpty()) View.GONE else View.VISIBLE
        radioInfo.text = radioDesc?.let { "📻 Radio · $it" } ?: "📻 Radio"
        if (c.logo != radioLogoUrl) {
            radioLogoUrl = c.logo
            ImageLoader.load(radioLogo, c.logo, 320)
        }
        radioIcon.visibility = if (c.logo == null) View.VISIBLE else View.GONE
    }

    /** Mỗi giây khi đang phát: luồng không có hình (đài HLS như VOV, Icecast) → kiểu radio; có hình trở lại thì thôi. */
    private fun radioTick() {
        val p = player ?: return
        val c = channel ?: return
        if (!started) return
        val video = sawVideo || p.videoSize.width > 0
        val want = !video && !c.vod && (c.radio || System.currentTimeMillis() - startedAt > 4000)
        if (want != radio) {
            TvLog.i("Truyền hình: ${c.n} ${if (want) "chỉ có tiếng — hiện kiểu đài radio" else "có hình — thôi kiểu radio"}")
            setRadio(want)
        }
        if (radio) {
            val f = p.audioFormat
            val codec = when (f?.sampleMimeType) {
                MimeTypes.AUDIO_MPEG -> "MP3"
                MimeTypes.AUDIO_AAC -> "AAC"
                MimeTypes.AUDIO_OPUS -> "Opus"
                MimeTypes.AUDIO_VORBIS -> "Vorbis"
                MimeTypes.AUDIO_FLAC -> "FLAC"
                null -> null
                else -> f.sampleMimeType?.substringAfter('/')?.uppercase()
            }
            var kbps = 0
            f?.metadata?.let { m -> for (i in 0 until m.length()) (m.get(i) as? IcyHeaders)?.let { kbps = it.bitrate / 1000 } }
            if (kbps <= 0 && f != null && f.bitrate > 0) kbps = f.bitrate / 1000
            val desc = listOfNotNull(codec ?: radioDesc, if (kbps > 0) "$kbps kbps" else null).joinToString(" · ").ifEmpty { null }
            if (desc != radioDesc) {
                radioDesc = desc
                updateRadioCard()
            }
        }
    }

    // ---- Trạng thái ----

    private fun showLoading(text: String) {
        status.visibility = View.VISIBLE
        spinner.visibility = View.VISIBLE
        statusText.visibility = View.VISIBLE
        statusText.text = text
        statusButtons.visibility = View.GONE
        radioCard.visibility = View.GONE
    }

    private fun showSpinnerOnly() {
        status.visibility = View.VISIBLE
        spinner.visibility = View.VISIBLE
        statusText.visibility = View.GONE
        statusButtons.visibility = View.GONE
    }

    private fun showError(text: String, retry: Boolean, retryLabel: String = "Thử lại") {
        TvLog.i("Truyền hình: ${text.replace('\n', ' ')}")
        status.visibility = View.VISIBLE
        spinner.visibility = View.GONE
        statusText.visibility = View.VISIBLE
        statusText.text = text
        statusButtons.visibility = View.VISIBLE
        btnRetry.visibility = if (retry) View.VISIBLE else View.GONE
        btnRetry.text = retryLabel
        btnSkip.visibility = if (pager.size > 1) View.VISIBLE else View.GONE
        radioCard.visibility = View.GONE
        setControls(true, focus = false)
        mainHandler.removeCallbacks(hide)
        if (panel.visibility != View.VISIBLE) (if (retry) btnRetry else if (pager.size > 1) btnSkip else findViewById(R.id.btnBack)).requestFocus()
    }

    private fun hideStatus() {
        status.visibility = View.GONE
        statusButtons.visibility = View.GONE
        if (radio) radioCard.visibility = View.VISIBLE
    }

    // ---- Nút / bảng kênh ----

    private fun setControls(on: Boolean, focus: Boolean = true) {
        val was = controls && bottom.visibility == View.VISIBLE
        controls = on
        top.visibility = if (on) View.VISIBLE else View.GONE
        bottom.visibility = if (on) View.VISIBLE else View.GONE
        // Vừa hiện nút: chọn sẵn «Tạm dừng» (đang hiện rồi thì giữ nút người xem đang chọn)
        if (on && focus && panel.visibility != View.VISIBLE && statusButtons.visibility != View.VISIBLE &&
            (!was || currentFocus?.parent !== findViewById<View>(R.id.buttons))
        ) btnPlay.requestFocus()
        bumpHide()
    }

    private fun bumpHide() {
        mainHandler.removeCallbacks(hide)
        if (controls || panel.visibility == View.VISIBLE) mainHandler.postDelayed(hide, if (panel.visibility == View.VISIBLE) 10_000 else HIDE_MS)
    }

    /** Không bấm gì một lúc khi đang phát: ẩn nút và bảng kênh. Radio: không có hình để xem — giữ nút. */
    private fun autoHide() {
        val p = player ?: return
        if (!started || !p.isPlaying || statusButtons.visibility == View.VISIBLE) return
        if (panel.visibility == View.VISIBLE) {
            panel.visibility = View.GONE
            if (radio) btnPlay.requestFocus()
        }
        if (!radio) setControls(false)
    }

    private fun togglePanel() {
        if (panel.visibility == View.VISIBLE) {
            panel.visibility = View.GONE
            if (statusButtons.visibility == View.VISIBLE) btnRetry.takeIf { it.visibility == View.VISIBLE }?.requestFocus() ?: btnSkip.requestFocus()
            else setControls(true)
            return
        }
        panel.visibility = View.VISIBLE
        panelAdapter.notifyDataSetChanged()
        scrollPanelToCurrent(focus = true)
        bumpHide()
    }

    private fun scrollPanelToCurrent(focus: Boolean = false) {
        if (panel.visibility != View.VISIBLE || index !in 0 until pager.size) return
        (panelList.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(index, panelList.height / 3)
        if (focus) panelList.post { panelList.findViewHolderForAdapterPosition(index)?.itemView?.requestFocus() }
    }

    private fun toggleFavorite() {
        val c = channel ?: return
        val on = !Iptv.isFavorite(c)
        Iptv.setFavorite(c, on)
        btnFav.text = if (on) "★" else "☆"
        notice.show(if (on) "Đã thêm ${c.n} vào Yêu thích." else "Đã bỏ ${c.n} khỏi Yêu thích.")
        bumpHide()
    }

    private fun openGuide() {
        val c = channel ?: return
        TvGuideActivity.open(this, c, fromPlayer = true)
    }

    // ---- Gõ số kênh ----

    private fun typeDigit(d: Int) {
        if (digits.length >= 5) digits = ""
        digits += d
        digitsView.text = digits + "_"
        digitsView.visibility = View.VISIBLE
        mainHandler.removeCallbacks(digitsGo)
        mainHandler.postDelayed(digitsGo, 1500)
    }

    private fun jumpToNumber() {
        val num = digits.trimStart('0').ifEmpty { "0" }
        digits = ""
        digitsView.visibility = View.GONE
        val ids = pager.ids
        inBackground({ Iptv.store()?.indexOfNum(ids, num) ?: -1 }) { r ->
            val i = r.getOrDefault(-1)
            if (i >= 0 && ids === pager.ids) playChannelAt(i) else notice.show("Không có kênh số $num trong danh sách đang xem.")
        }
    }

    // ---- Mỗi giây ----

    private fun tick() {
        if (closed) return
        ticks++
        val p = player
        val playing = started && p?.isPlaying == true
        WatchLimit.tick(playing)
        checkLimit()
        if (playing && reopens > 0 && System.currentTimeMillis() - startedAt > 60_000) reopens = 0 // phát ổn một phút: đếm lại
        // Độ trễ tự nhiên sau mép trực tiếp: đo lúc đã phát được 2 giây
        if (playing && baseBehind < 0 && p!!.isCurrentMediaItemLive && System.currentTimeMillis() - startedAt > 2000) {
            baseBehind = p.currentLiveOffset.takeIf { it != C.TIME_UNSET } ?: 0
        }
        updateBadge()
        if (ticks % 2 == 0) radioTick()
        if (channel?.vod == true || ticks % 15 == 0) updateProgramme()
        if (ticks % 30 == 0 && panel.visibility == View.VISIBLE) panelAdapter.notifyDataSetChanged()
    }

    /** Hết giờ xem (giới hạn của người xem): dừng, hiện màn hết giờ — người lớn nhập mã PIN cho xem thêm. */
    private fun checkLimit() {
        if (limitShowing) return
        val b = WatchLimit.block()
        if (b == null) {
            val r = WatchLimit.remaining()
            if (started && !limitWarned && r != null && r in 1..300) {
                limitWarned = true
                notice.show("Còn ${WatchLimit.minutesText(r)} xem hôm nay (${Prefs.profile.name}).", 8000)
            }
            return
        }
        token++ // việc đang chờ (nhận dạng link, mở lại sau 1 giây…) không được mở luồng sau màn hết giờ
        teardown()
        started = false
        limitShowing = true
        val who = Prefs.profile.name
        val why = when (b) {
            WatchLimit.Block.BEDTIME -> "Đã tới giờ đi ngủ của $who (từ ${WatchLimit.bedtimeLabel(Prefs.bedtime())} tới 6 giờ sáng)."
            WatchLimit.Block.DAILY -> "$who đã xem hết ${WatchLimit.dailyLabel(Prefs.dailyLimit())} của hôm nay."
        }
        val d = AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Hết giờ xem")
            .setMessage("$why\nNgười lớn nhập mã PIN phụ huynh để cho xem thêm.")
            .setPositiveButton("Cho xem thêm…") { _, _ ->
                ProfileUi.grant(this, cancelled = { limitShowing = false; checkLimit() }) {
                    limitShowing = false
                    limitWarned = false
                    // Hết giờ đúng lúc đang đọc kênh (việc đọc đã bị huỷ): đọc lại kênh rồi mới mở
                    if (channel == null) playChannelAt(index) else openSource()
                }
            }
            .setNegativeButton("Thoát") { _, _ -> close() }
            .setOnCancelListener { close() }
            .show()
        d.getButton(AlertDialog.BUTTON_NEGATIVE)?.requestFocus()
    }

    // ---- Phím ----

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        if (escapeAsBack(e)) return true
        if (e.action == KeyEvent.ACTION_DOWN) bumpHide()
        return super.dispatchKeyEvent(e)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val c = channel
        when (keyCode) {
            KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_PAGE_UP -> {
                nextChannel(); return true
            }
            KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_PAGE_DOWN -> {
                previousChannel(); return true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                togglePlay(); return true
            }
            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                nextChannel(); return true
            }
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                previousChannel(); return true
            }
            KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_GUIDE -> {
                if (keyCode == KeyEvent.KEYCODE_GUIDE && programmes.isNotEmpty()) openGuide() else togglePanel()
                return true
            }
            in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> {
                typeDigit(keyCode - KeyEvent.KEYCODE_0); return true
            }
            in KeyEvent.KEYCODE_NUMPAD_0..KeyEvent.KEYCODE_NUMPAD_9 -> {
                typeDigit(keyCode - KeyEvent.KEYCODE_NUMPAD_0); return true
            }
        }
        // Đang ẩn nút / bảng: phím điều hướng là lệnh của trình phát
        if (!controls && panel.visibility != View.VISIBLE && statusButtons.visibility != View.VISIBLE) {
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> {
                    previousChannel(); return true
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    nextChannel(); return true
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (c?.vod == true) seekBy(-10_000) else togglePanel()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (c?.vod == true) seekBy(10_000) else setControls(true)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                    setControls(true); return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun seekBy(ms: Long) {
        val p = player ?: return
        if (!started || p.duration <= 0) return
        p.seekTo((p.currentPosition + ms).coerceIn(0, p.duration))
        setControls(true, focus = false)
        updateProgramme()
    }

    @Deprecated("Back của remote")
    override fun onBackPressed() {
        when {
            panel.visibility == View.VISIBLE -> togglePanel()
            controls && started -> setControls(false)
            else -> close()
        }
    }

    /** Trạng thái cho cổng gỡ lỗi (bản gỡ lỗi): kênh, nguồn, đang phát, hình, radio, dòng trạng thái. */
    fun debugStatus(): String {
        val p = player
        val c = channel
        val v = p?.videoFormat
        return "kênh ${c?.num} «${c?.n}» (${index + 1}/${pager.size}) nguồn ${source + 1}/${c?.urls?.size} $srcKind | " +
            "${if (started) "đang phát" else "chưa phát"} | ${badge.text} | " +
            "hình ${v?.width}x${v?.height} ${v?.sampleMimeType} (vẽ ${p?.videoDecoderCounters?.renderedOutputBufferCount}, " +
            "rớt ${p?.videoDecoderCounters?.droppedBufferCount}) | tiếng ${p?.audioFormat?.sampleMimeType} | " +
            "radio=$radio «${radioTitle ?: ""}» ${radioDesc ?: ""} | trễ ${delayMs()} ms (gốc $baseBehind) | " +
            "trạng thái: ${if (status.visibility == View.VISIBLE) statusText.text.toString().replace('\n', ' ') else "—"} | url $srcUrl"
    }

    // ---- Bảng kênh ----

    private inner class PanelAdapter : RecyclerView.Adapter<PanelAdapter.Holder>() {
        inner class Holder(v: View) : RecyclerView.ViewHolder(v) {
            val logo: ImageView = v.findViewById(R.id.logo)
            val name: TextView = v.findViewById(R.id.name)
            val detail: TextView = v.findViewById(R.id.detail)
        }

        override fun getItemCount() = pager.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_live_row, parent, false)
            val h = Holder(v)
            v.setOnClickListener {
                val i = h.bindingAdapterPosition
                if (i >= 0 && i != index) playChannelAt(i)
                bumpHide()
            }
            v.setOnFocusChangeListener { _, has ->
                h.detail.setTextColor(if (has) 0xCC14110A.toInt() else colorOf(R.color.text_dim))
                if (has) bumpHide()
            }
            return h
        }

        override fun onBindViewHolder(h: Holder, i: Int) {
            val c = pager.get(i)
            h.itemView.isActivated = i == index
            if (c == null) {
                h.name.text = "…"
                h.detail.text = ""
                h.logo.setImageDrawable(null)
                return
            }
            h.name.text = "${c.num}   ${c.n}"
            val now = nowSec()
            val bad = Iptv.channelBadReason(c)
            val p = pager.programmes(c).nowAt(now)
            val g = if (c.radio) "📻 ${c.g}" else c.g
            h.detail.text = when {
                bad != null -> "Máy này không phát được"
                p != null -> p.t
                else -> c.matchLine(now) ?: g
            }
            ImageLoader.load(h.logo, c.logo, 120)
        }
    }
}
