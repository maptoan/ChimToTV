package vn.mtv.tv

/**
 * Kênh khớp một bộ lọc của danh sách đang chọn, nạp dần theo trang (danh sách Xtream vài trăm nghìn mục): chỉ giữ mã kênh
 * theo thứ tự hiện + vài trang gần nhất (kèm lịch phát sóng của các kênh đó). Dùng ở luồng chính.
 */
class ChannelPager(var filter: ChannelFilter) {
    companion object {
        const val PAGE = 40
        private const val KEEP_PAGES = 24
    }

    var ids: LongArray = LongArray(0)
        private set
    val size get() = ids.size

    /** Đã nạp danh sách mã lần nào chưa. */
    var ready = false
        private set

    /** Trang vừa nạp xong (vị trí đầu, số kênh) — báo cho bảng / lưới vẽ lại các ô đó. */
    var onRange: ((Int, Int) -> Unit)? = null

    private val pages = LinkedHashMap<Int, List<Channel>>(16, 0.75f, true)
    private val loading = HashSet<Int>()
    private val progs = HashMap<Long, List<Programme>>()
    private var gen = 0
    private var dataVersion = -1

    /** Nạp lại mã kênh (bộ lọc / danh sách / dấu hỏng vừa đổi). done chạy ở luồng chính. */
    fun reload(done: (() -> Unit)? = null) {
        val g = ++gen
        val f = filter
        val kids = Prefs.kids
        val hide = Iptv.hideBad
        val favs = Iptv.favoriteKeys
        val v = Iptv.version
        val t0 = System.currentTimeMillis()
        inBackground({ Iptv.store()?.ids(f, kids, hide, favs) ?: LongArray(0) }) { r ->
            val ms = System.currentTimeMillis() - t0
            if (ms > 1000 || r.isFailure) TvLog.i("Kênh: nạp ${r.getOrNull()?.size} mã ($ms ms)" + (r.exceptionOrNull()?.let { " lỗi $it" } ?: ""))
            if (g != gen) return@inBackground
            ids = r.getOrDefault(LongArray(0))
            ready = true
            dataVersion = v
            pages.clear()
            loading.clear()
            progs.clear()
            done?.invoke()
        }
    }

    /** Dữ liệu (danh sách / lịch / dấu hỏng) đã đổi từ lần nạp trước. */
    val stale get() = dataVersion != Iptv.version

    fun indexOf(id: Long) = ids.indexOf(id)

    /** Kênh ở vị trí [i]; chưa nạp thì nạp trang đó (báo qua [onRange]) và trả null. */
    fun get(i: Int): Channel? {
        if (i !in ids.indices) return null
        val p = i / PAGE
        pages[p]?.let { return it.getOrNull(i - p * PAGE) }
        load(p, null)
        return null
    }

    fun programmes(c: Channel): List<Programme> = progs[c.id] ?: emptyList()

    /** Nạp trang chứa [i] rồi gọi done(kênh) — trình phát đổi kênh. */
    fun fetch(i: Int, done: (Channel?) -> Unit) {
        if (i !in ids.indices) {
            done(null)
            return
        }
        val p = i / PAGE
        pages[p]?.let {
            done(it.getOrNull(i - p * PAGE))
            return
        }
        load(p) { done(pages[p]?.getOrNull(i - p * PAGE)) }
    }

    private fun load(p: Int, done: (() -> Unit)?) {
        if (p in loading && done == null) return
        loading += p
        val g = gen
        val slice = ids.copyOfRange(p * PAGE, minOf(ids.size, (p + 1) * PAGE))
        inBackground({
            val chans = Iptv.store()?.byIds(slice) ?: emptyList()
            val guide = Iptv.guide()
            val pr = if (guide == null) emptyMap() else chans.associate { it.id to runCatching { guide.programmes(it) }.getOrDefault(emptyList()) }
            chans to pr
        }) { r ->
            if (g != gen) return@inBackground
            loading -= p
            r.getOrNull()?.let { (chans, pr) ->
                pages[p] = chans
                progs.putAll(pr)
                while (pages.size > KEEP_PAGES) {
                    val old = pages.keys.first()
                    pages.remove(old)?.forEach { progs.remove(it.id) }
                }
                onRange?.invoke(p * PAGE, slice.size)
            }
            done?.invoke()
        }
    }

    /** Thay một kênh bằng bản mới của danh sách (cùng khoá) — trình phát vừa tải lại danh sách tìm link mới. */
    fun replace(i: Int, c: Channel) {
        val p = i / PAGE
        val page = pages[p] ?: return
        val j = i - p * PAGE
        if (j in page.indices) {
            pages[p] = page.toMutableList().also { it[j] = c }
            if (c.id != ids[i]) {
                ids = ids.copyOf().also { it[i] = c.id }
            }
        }
    }
}

/** Chương trình đang phát / kế tiếp lúc [now]. */
fun List<Programme>.nowAt(now: Double) = firstOrNull { it.s <= now && now < it.e }
fun List<Programme>.nextAt(now: Double) = firstOrNull { it.s > now }
