package vn.mtv.tv

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.ceil
import kotlin.math.max

/**
 * Màn Truyền hình (IPTV): danh sách kênh M3U người xem tự thêm — nhóm bên trái («★ Yêu thích», «Tất cả», các nhóm của danh
 * sách), lưới kênh (logo, chương trình đang phát + thanh tiến độ, trạng thái trận), tìm theo tên / gõ số kênh. App không kèm
 * sẵn kênh. Cùng cách chạy với màn Truyền hình của bản Legacy (CBTVViewController).
 */
class TvActivity : BaseActivity() {
    companion object {
        private const val FAV = -2L
        private const val ALL = -1L

        /** favs: true = mở sẵn nhóm «★ Yêu thích», false = «Tất cả» (null: lần đầu có kênh yêu thích thì «★ Yêu thích»). */
        fun open(a: Activity, favs: Boolean? = null) =
            a.startActivity(Intent(a, TvActivity::class.java).apply { favs?.let { putExtra("favs", it) } })
    }

    private data class Group(val id: Long, val title: String, val count: Int)

    private lateinit var info: TextView
    private lateinit var search: EditText
    private lateinit var groupsView: RecyclerView
    private lateinit var grid: RecyclerView
    private lateinit var empty: View
    private lateinit var spinner: ProgressBar
    private lateinit var message: TextView
    private lateinit var btnAdd: View
    private lateinit var layout: GridLayoutManager

    private val groupAdapter = MenuAdapter<Group>({ "${it.title}  ${it.count}" }, ::selectGroup)
    private val pager = ChannelPager(ChannelFilter())
    private val adapter = ChannelAdapter()
    private var group = ALL
    private var favsChosen = false
    private var firstFocused = false
    private var onScreen = false
    private var shownStore: String? = null
    private val changed: () -> Unit = { rebuild() }
    private val searchGo = Runnable { applyFilter() }
    private val ticker = object : Runnable {
        override fun run() {
            tick()
            mainHandler.postDelayed(this, 30_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tv)
        info = findViewById(R.id.info)
        search = findViewById(R.id.search)
        groupsView = findViewById(R.id.groups)
        grid = findViewById(R.id.grid)
        empty = findViewById(R.id.empty)
        spinner = findViewById(R.id.spinner)
        message = findViewById(R.id.message)
        btnAdd = findViewById(R.id.btnAdd)

        groupsView.layoutManager = LinearLayoutManager(this)
        groupsView.adapter = groupAdapter
        layout = GridLayoutManager(this, 3)
        grid.layoutManager = layout
        grid.adapter = adapter
        grid.itemAnimator = null
        grid.setHasFixedSize(true)
        grid.addOnLayoutChangeListener { _, l, _, r, _, ol, _, or_, _ ->
            if (r - l != or_ - ol) updateSpan()
        }
        pager.onRange = { start, n -> adapter.notifyItemRangeChanged(start, n) }

        findViewById<View>(R.id.btnSettings).setOnClickListener { startActivity(Intent(this, TvSettingsActivity::class.java)) }
        findViewById<View>(R.id.btnLists).setOnClickListener { chooseList() }
        btnAdd.setOnClickListener { IptvImport.ask(this) { rebuild() } }
        search.leaveWithUpDown()
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                mainHandler.removeCallbacks(searchGo)
                mainHandler.postDelayed(searchGo, 400)
            }
        })
        search.setOnEditorActionListener { v, id, _ ->
            if (id == EditorInfo.IME_ACTION_SEARCH || id == EditorInfo.IME_ACTION_DONE) {
                applyFilter()
                hideKeyboard(v)
                grid.post { focusGrid() }
                true
            } else false
        }
        if (intent.hasExtra("favs")) {
            favsChosen = true
            group = if (intent.getBooleanExtra("favs", false)) FAV else ALL
        }
        Iptv.listen(changed)
        debugIntent(intent)
        rebuild()
        if (Iptv.activeList != null) Iptv.refresh(false) { err -> if (err != null) toast(err) }
    }

    override fun onDestroy() {
        Iptv.unlisten(changed)
        mainHandler.removeCallbacks(ticker)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        onScreen = true
        // Quay lại màn (xem xong kênh, đổi người xem, đổi danh sách trong Cài đặt): vẽ lại, tải lại danh sách nếu đã quá hạn
        rebuild()
        Iptv.autoRefresh()
        mainHandler.removeCallbacks(ticker)
        mainHandler.postDelayed(ticker, 30_000)
    }

    override fun onPause() {
        super.onPause()
        onScreen = false
        mainHandler.removeCallbacks(ticker)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        IptvImport.onResult(this, requestCode, resultCode == RESULT_OK, data)
    }

    /** Chạm thông báo nhắc giờ phát: mở kênh. Bản gỡ lỗi: thêm danh sách / mở kênh bằng lệnh adb (am start … --es add_url …). */
    private fun debugIntent(i: Intent?) {
        if (i == null) return
        i.getStringExtra("play_key")?.let { k ->
            i.removeExtra("play_key")
            mainHandler.postDelayed({ if (!isFinishing) LiveActivity.playKey(this, k) }, 300)
        }
        if (!BuildConfig.DEBUG) return
        i.getStringExtra("add_url")?.let {
            Iptv.addList(it, i.getStringExtra("name"))
            Iptv.refresh(true) { err -> if (err != null) toast(err) }
        }
        i.getStringExtra("use")?.let { n -> Iptv.lists.getOrNull(n.toIntOrNull() ?: -1)?.let { l -> Iptv.setActive(l.id); Iptv.refresh(false, null) } }
        i.getStringExtra("play")?.let { k -> mainHandler.postDelayed({ LiveActivity.playKey(this, k) }, 1500) }
        i.getStringExtra("refresh")?.let { Iptv.refresh(true) { err -> if (err != null) toast(err) } }
        i.getStringExtra("reparse")?.let {
            Iptv.forgetSignature()
            Iptv.refresh(true) { err -> if (err != null) toast(err) }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        debugIntent(intent)
    }

    private fun updateSpan() {
        val w = grid.width - grid.paddingLeft - grid.paddingRight
        if (w <= 0) return
        val span = max(1, w / dp(225))
        if (layout.spanCount != span) layout.spanCount = span
    }

    private fun tick() {
        // Giờ trận / chương trình đổi theo phút: vẽ lại các ô; màn đang mở: tự làm mới danh sách khi quá hạn
        adapter.notifyItemRangeChanged(0, adapter.itemCount)
        if (onScreen) Iptv.autoRefresh()
    }

    // ---- Dữ liệu ----

    private fun rebuild() {
        if (isFinishing) return
        val list = Iptv.activeList
        val storeName = list?.db
        val kids = Prefs.kids
        // Thông tin đầu màn
        info.text = when {
            list == null -> ""
            Iptv.status != null -> "${list.name} · ${Iptv.status}"
            else -> buildString {
                append(list.name)
                if (list.count > 0) append(" · ${list.count} kênh")
                if (list.updated > 0) append(" · cập nhật ${IptvParse.clock(list.updated / 1000.0)}")
                if (kids) append(" · hồ sơ trẻ em: kênh thiếu nhi")
            }
        }
        btnAdd.visibility = if (list == null) View.VISIBLE else View.GONE
        if (list == null) {
            groupAdapter.submit(emptyList())
            showEmpty("Chưa có danh sách kênh.\nApp không kèm sẵn kênh: thêm địa chỉ danh sách M3U / M3U8 mà bạn có quyền xem.", false)
            btnAdd.post { if (!isFinishing) btnAdd.focusForRemote() }
            return
        }
        if (storeName == null) {
            groupAdapter.submit(emptyList())
            if (Iptv.loading) showEmpty(Iptv.status ?: "Đang tải danh sách kênh…", true)
            else showEmpty(list.err?.let { "Chưa tải được danh sách «${list.name}».\n$it" } ?: "Chưa tải danh sách kênh.", false)
            return
        }
        if (shownStore == storeName && !pager.stale && pager.ready) {
            adapter.notifyItemRangeChanged(0, adapter.itemCount)
            return
        }
        shownStore = storeName
        loadGroups()
    }

    /** Danh sách đang nạp nhóm / mã kênh (mở màn gọi vẽ lại hai lần — khỏi nạp hai lần cùng lúc). */
    private var loadingStore: String? = null

    /** Tệp kênh không đọc được đã tự tải lại một lần. */
    private val reloadAsked = HashSet<String>()

    private fun loadGroups() {
        val storeName = Iptv.activeList?.db
        if (storeName != null && storeName == loadingStore) return
        loadingStore = storeName
        val kids = Prefs.kids
        val hide = Iptv.hideBad
        val favKeys = Iptv.favoriteKeys
        inBackground({
            val s = Iptv.store() ?: return@inBackground null
            val groups = s.groupCounts(kids, hide)
            val total = groups.sumOf { it.third }
            val favs = if (favKeys.isEmpty()) 0 else s.countOf(ChannelFilter(favs = true), kids, hide, favKeys)
            Triple(groups, total, favs)
        }) { r ->
            if (isFinishing) return@inBackground
            val (groups, total, favs) = r.getOrNull() ?: run {
                loadingStore = null
                // Tệp đọc theo cách cũ (vừa cập nhật app) / hỏng: tải lại danh sách — một lần cho mỗi tệp (tải hỏng thì báo lỗi,
                // không thử lại liên tục)
                if (storeName != null && reloadAsked.add(storeName) && !Iptv.loading) {
                    shownStore = null
                    Iptv.refresh(true, null)
                    showEmpty(Iptv.status ?: "Đang đọc lại danh sách kênh…", true)
                } else {
                    shownStore = storeName
                    val l = Iptv.activeList
                    showEmpty(l?.err?.let { "Chưa tải được danh sách «${l.name}».\n$it" } ?: "Không đọc được danh sách kênh đã lưu.", Iptv.loading)
                }
                return@inBackground
            }
            val items = listOf(Group(FAV, "★ Yêu thích", favs), Group(ALL, "Tất cả", total)) +
                groups.map { Group(it.first, it.second, it.third) }
            // Lần đầu vào: có kênh yêu thích thì mở sẵn nhóm «★ Yêu thích»
            if (!favsChosen && total > 0) {
                favsChosen = true
                if (favs > 0) group = FAV
            }
            if (items.none { it.id == group }) group = ALL
            groupAdapter.submit(items)
            groupAdapter.active = items.first { it.id == group }
            applyFilter(keepFocus = true)
        }
    }

    private fun selectGroup(g: Group) {
        group = g.id
        groupAdapter.active = g
        applyFilter()
    }

    private fun applyFilter(keepFocus: Boolean = false) {
        pager.filter = ChannelFilter(group = group.takeIf { it >= 0 }, favs = group == FAV, q = search.text.toString().trim())
        pager.reload {
            loadingStore = null
            adapter.notifyDataSetChanged()
            if (!keepFocus) grid.scrollToPosition(0)
            val q = pager.filter.q
            when {
                pager.size > 0 -> {
                    empty.visibility = View.GONE
                    // Lần đầu (ô tìm tự nhận tiêu điểm khi mở màn): đưa tiêu điểm vào lưới
                    if (currentFocus == null || currentFocus === btnAdd || (currentFocus === search && (!firstFocused || search.text.isEmpty()))) focusGrid()
                    firstFocused = true
                }
                q.isNotEmpty() -> showEmpty("Không có kênh nào khớp «$q».", false)
                group == FAV -> showEmpty("Chưa có kênh yêu thích — nhấn giữ OK (hoặc MENU) trên ô kênh để thêm.", false)
                Prefs.kids -> showEmpty("Danh sách này không có kênh thiếu nhi.", false)
                else -> showEmpty("Nhóm này không có kênh.", false)
            }
            if (Iptv.loading && pager.size == 0) showEmpty(Iptv.status ?: "Đang tải danh sách kênh…", true)
        }
    }

    private fun showEmpty(text: String, loading: Boolean) {
        if (text.isNotEmpty() && !loading) adapter.notifyDataSetChanged()
        empty.visibility = View.VISIBLE
        spinner.visibility = if (loading) View.VISIBLE else View.GONE
        message.text = text
    }

    private fun focusGrid() {
        grid.post {
            val first = layout.findFirstVisibleItemPosition().coerceAtLeast(0)
            grid.findViewHolderForAdapterPosition(first)?.itemView?.focusForRemote()
                ?: groupsView.getChildAt(0)?.focusForRemote()
        }
    }

    // ---- Mở kênh ----

    private fun openAt(i: Int) {
        val c = pager.get(i) ?: return
        WatchLimit.block()?.let {
            toast("Đã hết giờ xem hôm nay của ${Prefs.profile.name}.")
            return
        }
        if (c.match() == IptvParse.Match.LATER) {
            askBeforeKick(c, i)
            return
        }
        LiveActivity.open(this, pager.filter, i, c.id)
    }

    /** Trận còn hơn 30 phút mới đá: kênh thường chưa lên sóng — hỏi đặt nhắc thay vì mở ra màn lỗi. */
    private fun askBeforeKick(c: Channel, i: Int) {
        val now = nowSec()
        val m = ceil((c.kick - now) / 60).toLong()
        val left = when {
            m >= 24 * 60 -> "${(m + 720) / 1440} ngày"
            m >= 60 -> "${m / 60} giờ %02d phút".format(m % 60)
            else -> "$m phút"
        }
        val has = IptvReminders.hasKick(c)
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Trận chưa bắt đầu")
            .setMessage("${c.n}\nBóng lăn lúc ${IptvParse.kickText(c.kick, now)} (còn $left).\nKênh trận đấu thường chỉ lên sóng trước giờ đá 15–30 phút.")
            .setPositiveButton("Vẫn mở") { _, _ -> LiveActivity.open(this, pager.filter, i, c.id) }
            .setNeutralButton(if (has) "Bỏ nhắc" else "Nhắc lúc bóng lăn") { _, _ -> toggleKick(c) }
            .setNegativeButton("Đóng", null)
            .show()
    }

    private fun toggleKick(c: Channel) {
        if (IptvReminders.toggleKick(this, c)) toast("Sẽ nhắc lúc bóng lăn (${IptvParse.kickText(c.kick, nowSec())}).")
        else toast("Đã bỏ nhắc.")
        adapter.notifyItemRangeChanged(0, adapter.itemCount)
    }

    private fun toggleFavorite(c: Channel) {
        val on = !Iptv.isFavorite(c)
        Iptv.setFavorite(c, on)
        toast(if (on) "Đã thêm ${c.n} vào Yêu thích." else "Đã bỏ ${c.n} khỏi Yêu thích.")
        shownStore = null // đếm lại số kênh yêu thích
    }

    /** Nhấn giữ OK / MENU trên ô kênh. */
    private fun channelMenu(i: Int) {
        val c = pager.get(i) ?: return
        val now = nowSec()
        val ms = c.match(now)
        val items = ArrayList<Pair<String, () -> Unit>>()
        items += "Xem" to { openAt(i) }
        if (ms == IptvParse.Match.LATER || ms == IptvParse.Match.SOON) {
            items += (if (IptvReminders.hasKick(c)) "Bỏ nhắc bóng lăn" else "🔔 Nhắc lúc bóng lăn") to { toggleKick(c) }
        }
        if (ms == IptvParse.Match.NONE && !c.vod) items += "Lịch phát sóng" to { TvGuideActivity.open(this, c, fromPlayer = false) }
        items += (if (Iptv.isFavorite(c)) "Bỏ khỏi Yêu thích" else "★ Thêm vào Yêu thích") to { toggleFavorite(c) }
        if (Iptv.channelBadReason(c) != null) items += "Thử lại kênh này" to {
            Iptv.clearBadFor(c)
            LiveActivity.open(this, pager.filter, i, c.id)
        }
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(c.n)
            .setItems(items.map { it.first }.toTypedArray()) { _, w -> items[w].second() }
            .setNegativeButton("Huỷ", null)
            .show()
    }

    /** «Đổi danh sách»: chọn nhanh danh sách khác, thêm danh sách, hoặc vào Cài đặt › Truyền hình. */
    private fun chooseList() {
        val lists = Iptv.lists
        val active = Iptv.activeList?.id
        val labels = lists.map { (if (it.id == active) "✓ " else "") + it.name + (if (it.count > 0) "  (${it.count} kênh)" else "") } +
            listOf("+ Thêm danh sách…", "Quản lý danh sách, lịch phát sóng…")
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Danh sách kênh")
            .setItems(labels.toTypedArray()) { _, w ->
                when {
                    w < lists.size -> {
                        Iptv.setActive(lists[w].id)
                        shownStore = null
                        favsChosen = false
                        Iptv.refresh(false) { err -> if (err != null) toast(err) }
                    }
                    w == lists.size -> IptvImport.ask(this) { rebuild() }
                    else -> startActivity(Intent(this, TvSettingsActivity::class.java))
                }
            }
            .show()
    }

    // ---- Phím ----

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        if (escapeAsBack(e)) return true
        val f = currentFocus
        // MENU trên ô kênh: tuỳ chọn kênh
        if (e.keyCode == KeyEvent.KEYCODE_MENU && e.action == KeyEvent.ACTION_UP && f != null && f.parent === grid) {
            channelMenu(grid.getChildAdapterPosition(f))
            return true
        }
        // Gõ số trên remote khi đang ở lưới: tìm theo số kênh
        if (e.action == KeyEvent.ACTION_DOWN && e.keyCode in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 && f !is EditText) {
            search.setText(search.text.toString().filter { it.isDigit() } + (e.keyCode - KeyEvent.KEYCODE_0))
            return true
        }
        return super.dispatchKeyEvent(e)
    }

    private fun hideKeyboard(v: View) {
        (getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager).hideSoftInputFromWindow(v.windowToken, 0)
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()

    // ---- Lưới ----

    private inner class ChannelAdapter : RecyclerView.Adapter<ChannelAdapter.Holder>() {
        inner class Holder(v: View) : RecyclerView.ViewHolder(v) {
            val logo: ImageView = v.findViewById(R.id.logo)
            val abbr: TextView = v.findViewById(R.id.abbr)
            val name: TextView = v.findViewById(R.id.name)
            val star: TextView = v.findViewById(R.id.star)
            val prog: TextView = v.findViewById(R.id.prog)
            val track: ProgressBar = v.findViewById(R.id.track)
            val time: TextView = v.findViewById(R.id.time)
            val next: TextView = v.findViewById(R.id.next)
        }

        override fun getItemCount() = if (empty.visibility == View.VISIBLE && pager.size == 0) 0 else pager.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_channel, parent, false)
            v.focusZoom(1.04f)
            val h = Holder(v)
            v.setOnClickListener { h.bindingAdapterPosition.takeIf { it >= 0 }?.let(::openAt) }
            v.setOnLongClickListener {
                h.bindingAdapterPosition.takeIf { it >= 0 }?.let(::channelMenu)
                true
            }
            return h
        }

        override fun onBindViewHolder(h: Holder, i: Int) {
            val c = pager.get(i)
            if (c == null) {
                h.name.text = "…"
                h.abbr.text = ""
                h.prog.text = ""
                h.next.text = ""
                h.time.text = ""
                h.star.visibility = View.INVISIBLE
                h.track.visibility = View.INVISIBLE
                h.logo.setImageDrawable(null)
                return
            }
            val now = nowSec()
            val progs = pager.programmes(c)
            val p = progs.nowAt(now)
            val n = progs.nextAt(now)
            val bad = Iptv.channelBadReason(c)
            h.abbr.text = c.n
            h.name.text = android.text.SpannableString("${c.num}  ${c.n}").apply {
                setSpan(android.text.style.ForegroundColorSpan(0xFF8D959E.toInt()), 0, c.num.length, 0)
                setSpan(android.text.style.StyleSpan(android.graphics.Typeface.NORMAL), 0, c.num.length, 0)
            }
            h.star.visibility = if (Iptv.isFavorite(c)) View.VISIBLE else View.INVISIBLE
            ImageLoader.load(h.logo, c.logo, 160)
            h.abbr.visibility = if (c.logo == null) View.VISIBLE else View.INVISIBLE
            var frac = -1.0
            val ms = c.match(now)
            val line = c.matchLine(now)
            h.prog.setTextColor(colorOf(R.color.text))
            when {
                bad != null -> {
                    h.prog.text = "Máy này không phát được"
                    h.prog.setTextColor(0xFFF0B44C.toInt())
                    h.next.text = bad
                    h.time.text = ""
                }
                line != null -> {
                    h.prog.text = line
                    h.prog.setTextColor(
                        when (ms) {
                            IptvParse.Match.LIVE -> 0xFFFF6166.toInt()
                            IptvParse.Match.SOON -> colorOf(R.color.focus)
                            IptvParse.Match.OVER -> colorOf(R.color.text_dim)
                            else -> colorOf(R.color.text)
                        },
                    )
                    h.time.text = ""
                    h.next.text = (if (IptvReminders.hasKick(c)) "🔔 Nhắc lúc bóng lăn · " else "") + c.g
                    if (ms == IptvParse.Match.LIVE) frac = (now - c.kick) / (c.dur * 60)
                }
                else -> {
                    val g = when {
                        c.radio -> "📻 ${c.g}"
                        c.vod -> "🎬 ${c.g}"
                        else -> c.g
                    }
                    h.prog.text = p?.t ?: g
                    h.time.text = p?.let { "${IptvParse.clock(it.s)}–${IptvParse.clock(it.e)}" } ?: ""
                    h.next.text = n?.let { "Tiếp: ${IptvParse.clock(it.s)} ${it.t}" } ?: if (p != null) g else ""
                    if (p != null) frac = (now - p.s) / (p.e - p.s)
                }
            }
            h.track.visibility = if (frac >= 0 && bad == null) View.VISIBLE else View.INVISIBLE
            if (frac >= 0) h.track.progress = (frac.coerceIn(0.0, 1.0) * 1000).toInt()
            h.itemView.alpha = if (bad != null) 0.55f else if (ms == IptvParse.Match.OVER) 0.7f else 1f
        }
    }
}
