package vn.mtv.tv

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Xml
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream
import kotlin.math.floor

/**
 * Kênh truyền hình / đài radio / phim (VOD) của danh sách M3U.
 * [urls] = các nguồn (HLS trước FLV, link giả «no-signal» cuối); [hdr] = Referer / User-Agent riêng của nguồn gộp vào từ dòng
 * khác của danh sách; [kick] = giờ bóng lăn (giây từ 1970, 0 = kênh thường), [dur] = thời lượng trận (phút).
 */
data class Channel(
    val id: Long, val k: String, val n: String, val g: String, val num: String, val logo: String?, val tid: String?,
    val urls: List<String>, val ref: String?, val ua: String?, val hdr: Map<String, Pair<String, String>>,
    val kick: Double, val dur: Int, val radio: Boolean, val nolink: Boolean, val vod: Boolean, val bad: Boolean,
) {
    fun match(now: Double = nowSec()) = IptvParse.matchState(kick, dur, now)
    fun matchLine(now: Double = nowSec()) = IptvParse.matchLine(kick, dur, nolink, now)

    /** Referer / User-Agent gửi lên máy chủ cho nguồn [url]. */
    fun headersFor(url: String): Map<String, String> {
        val h = hdr[url]
        val r = h?.first ?: ref
        val u = h?.second ?: ua
        return buildMap {
            if (!r.isNullOrEmpty()) put("Referer", r)
            if (!u.isNullOrEmpty()) put("User-Agent", u)
        }
    }

    /** Khoá lịch phát sóng: tvg-id, không có thì tên chuẩn hoá. */
    val guideKeys: List<String> get() = listOfNotNull(tid, "n:$k")
}

fun nowSec() = System.currentTimeMillis() / 1000.0

/** Nhật ký của phần Truyền hình (logcat thẻ ChimToIptv). */
internal object TvLog {
    fun i(s: String) {
        android.util.Log.i("ChimToIptv", redact(s))
    }

    private val QUERY_SECRET = Regex("""(?i)([?&](?:username|user|password|pass|pwd|token|key)=)[^&#\s]*""")
    private val XTREAM_PATH = Regex("""(?i)(/(?:live|movie|series|timeshift)/)[^/\s]+/[^/\s]+/""")
    private val USER_INFO = Regex("""(://)[^/\s@]+@""")

    /**
     * Che tài khoản trong link trước khi ghi nhật ký (logcat ai cắm adb / gửi báo lỗi cũng đọc được): tham số username / password
     * / token… của link, đường dẫn Xtream `/live/<tên>/<mật khẩu>/…`, `user:pass@` trước tên máy chủ.
     */
    fun redact(s: String): String = s
        .replace(QUERY_SECRET) { it.groupValues[1] + "***" }
        .replace(XTREAM_PATH) { it.groupValues[1] + "***/***/" }
        .replace(USER_INFO) { it.groupValues[1] + "***@" }
}

/** Danh sách kênh người xem đã thêm (địa chỉ hoặc tệp). [db] = tệp SQLite đang dùng của danh sách. */
data class IptvList(
    val id: String, val name: String, val url: String?, val file: String?, val src: String?,
    val count: Int = 0, val updated: Long = 0, val epg: String? = null, val err: String? = null, val db: String? = null,
    /** Dấu của tệp lần tải trước (phiên bản đọc:CRC32:cỡ) — tệp không đổi thì khỏi đọc lại. */
    val sig: String? = null,
) {
    fun json(): JSONObject = JSONObject().apply {
        put("id", id); put("name", name)
        url?.let { put("url", it) }; file?.let { put("file", it) }; src?.let { put("src", it) }
        if (count > 0) put("count", count)
        if (updated > 0) put("updated", updated)
        epg?.let { put("epg", it) }; err?.let { put("err", it) }; db?.let { put("db", it) }; sig?.let { put("sig", it) }
    }

    companion object {
        fun from(o: JSONObject) = IptvList(
            o.optString("id"), o.optString("name"), o.optString("url").ifEmpty { null }, o.optString("file").ifEmpty { null },
            o.optString("src").ifEmpty { null }, o.optInt("count"), o.optLong("updated"), o.optString("epg").ifEmpty { null },
            o.optString("err").ifEmpty { null }, o.optString("db").ifEmpty { null }, o.optString("sig").ifEmpty { null },
        )
    }
}

/** Một chương trình của lịch phát sóng (giây từ 1970). */
data class Programme(val s: Double, val e: Double, val t: String)

/**
 * Một ô kênh của hàng Truyền hình trên trang chủ (Iptv.homeRows). id = mã kênh trong danh sách đang dùng (-1: kênh của danh
 * sách khác — chỉ có tên / logo / nhóm đã lưu trong «Kênh vừa xem»); progs = chương trình đang phát và vài chương trình sau.
 */
data class HomeChannel(
    val key: String, val listId: String?, val id: Long, val name: String, val num: String, val logo: String?, val group: String,
    val radio: Boolean, val vod: Boolean, val kick: Double, val dur: Int, val nolink: Boolean, val fav: Boolean,
    val progs: List<Programme>,
)

/** Lọc kênh của lưới / bảng kênh: nhóm (null = tất cả), chỉ yêu thích, chữ tìm (tên hoặc số kênh). */
data class ChannelFilter(val group: Long? = null, val favs: Boolean = false, val q: String = "") {
    fun extras(i: android.content.Intent) {
        i.putExtra("f_group", group ?: -1L); i.putExtra("f_favs", favs); i.putExtra("f_q", q)
    }

    companion object {
        fun from(i: android.content.Intent) =
            ChannelFilter(i.getLongExtra("f_group", -1L).takeIf { it >= 0 }, i.getBooleanExtra("f_favs", false), i.getStringExtra("f_q") ?: "")
    }
}

/**
 * Kênh của một danh sách trong SQLite (cacheDir/iptv/<mã>-<giờ>.db): danh sách Xtream có thể tới vài trăm nghìn mục — đọc
 * dần theo trang, không giữ cả danh sách trong RAM (TX3 chỉ ~1 GB). Mỗi lần tải lại ghi ra tệp mới rồi mới đổi.
 */
class IptvStore(val file: File) {
    val db: SQLiteDatabase = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE)
    val groups: List<Pair<Long, String>>
    private val groupName: Map<Long, String>
    val hasMatches: Boolean
    val version: Int
    val count: Int
    val epg: String?

    init {
        groups = db.rawQuery("SELECT id, name FROM grp ORDER BY id", null).use { c ->
            buildList { while (c.moveToNext()) add(c.getLong(0) to c.getString(1)) }
        }
        groupName = groups.toMap()
        fun meta(k: String) = db.rawQuery("SELECT v FROM meta WHERE k=?", arrayOf(k)).use { if (it.moveToFirst()) it.getString(0) else null }
        hasMatches = meta("matches") == "1"
        version = meta("v")?.toIntOrNull() ?: 0
        count = meta("count")?.toIntOrNull() ?: 0
        epg = meta("epg")
    }

    fun close() = runCatching { db.close() }

    private fun where(f: ChannelFilter, kids: Boolean, hideBad: Boolean, favKeys: List<String>): Pair<String, Array<String>> {
        val w = ArrayList<String>()
        val args = ArrayList<String>()
        f.group?.let { w += "g = $it" }
        if (f.favs) {
            val keys = favKeys.take(900)
            if (keys.isEmpty()) w += "0" else {
                w += "k IN (" + keys.joinToString(",") { "?" } + ")"
                args += keys
            }
        }
        if (kids) w += "kids = 1"
        if (hideBad) w += "bad = 0"
        val q = f.q.trim()
        if (q.isNotEmpty()) {
            val nq = IptvParse.norm(q)
            if (nq.isNotEmpty()) {
                w += "(k LIKE ? OR num = ?)"
                args += "%$nq%"; args += q
            } else {
                w += "num = ?"
                args += q
            }
        }
        return (if (w.isEmpty()) "" else "WHERE " + w.joinToString(" AND ")) to args.toTypedArray()
    }

    /** Thứ tự: danh sách có trận → đang đá (có link trước), sắp đá (sớm trước), kênh thường, đã xong (mới xong trước). */
    private fun order(now: Double): String = if (!hasMatches) "ORDER BY id" else {
        val n = "%.0f".format(java.util.Locale.US, now)
        "ORDER BY CASE WHEN kick IS NULL THEN 3 WHEN $n >= kick + dur * 60 THEN 4 WHEN $n >= kick THEN nolink ELSE 2 END, " +
            "CASE WHEN kick IS NULL THEN 0 WHEN $n >= kick + dur * 60 THEN -kick ELSE kick END, id"
    }

    /**
     * Mã các kênh khớp bộ lọc, đúng thứ tự hiện. Đọc theo khúc 50.000 mã gộp thành một chuỗi (group_concat — một dòng mỗi khúc):
     * đọc từng dòng qua Cursor của Android mất ~20 giây cho 350 nghìn kênh trên TV box yếu.
     */
    fun ids(f: ChannelFilter, kids: Boolean, hideBad: Boolean, favKeys: List<String>, now: Double = nowSec()): LongArray {
        val (w, a) = where(f, kids, hideBad, favKeys)
        val total = db.rawQuery("SELECT count(*) FROM ch $w", a).use { if (it.moveToFirst()) it.getInt(0) else 0 }
        val out = LongArray(total)
        var n = 0
        var off = 0
        val chunk = 50_000
        while (off < total) {
            db.rawQuery("SELECT group_concat(id) FROM (SELECT id FROM ch $w ${order(now)} LIMIT $chunk OFFSET $off)", a).use { c ->
                if (c.moveToFirst() && !c.isNull(0)) {
                    val s = c.getString(0)
                    var v = 0L
                    var any = false
                    for (ch in s) {
                        if (ch == ',') {
                            if (any && n < total) out[n++] = v
                            v = 0
                            any = false
                        } else {
                            v = v * 10 + (ch - '0')
                            any = true
                        }
                    }
                    if (any && n < total) out[n++] = v
                }
            }
            off += chunk
        }
        return if (n == total) out else out.copyOf(n)
    }

    /** Nhóm có kênh (theo bộ lọc trẻ em / ẩn kênh hỏng): (mã, tên, số kênh), thứ tự xuất hiện trong danh sách. */
    fun groupCounts(kids: Boolean, hideBad: Boolean): List<Triple<Long, String, Int>> {
        val (w, a) = where(ChannelFilter(), kids, hideBad, emptyList())
        return db.rawQuery("SELECT g, count(*) FROM ch $w GROUP BY g ORDER BY g", a).use { c ->
            buildList { while (c.moveToNext()) add(Triple(c.getLong(0), groupName[c.getLong(0)] ?: "Khác", c.getInt(1))) }
        }
    }

    fun countOf(f: ChannelFilter, kids: Boolean, hideBad: Boolean, favKeys: List<String>): Int {
        val (w, a) = where(f, kids, hideBad, favKeys)
        return db.rawQuery("SELECT count(*) FROM ch $w", a).use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }

    private val cols = "id, k, g, n, num, logo, tid, urls, ref, ua, hdr, kick, dur, radio, nolink, vod, bad"

    private fun read(c: Cursor): Channel {
        val hdr = c.getString(10)?.let { s ->
            runCatching {
                val o = JSONObject(s)
                o.keys().asSequence().associateWith { u -> o.getJSONObject(u).let { it.optString("ref") to it.optString("ua") } }
            }.getOrNull()
        } ?: emptyMap()
        return Channel(
            c.getLong(0), c.getString(1), c.getString(3), groupName[c.getLong(2)] ?: "Khác", c.getString(4) ?: "",
            c.getString(5), c.getString(6), c.getString(7).split('\n').filter { it.isNotEmpty() }, c.getString(8), c.getString(9),
            hdr, if (c.isNull(11)) 0.0 else c.getDouble(11), c.getInt(12), c.getInt(13) != 0, c.getInt(14) != 0,
            c.getInt(15) != 0, c.getInt(16) != 0,
        )
    }

    private fun select(cond: String, order: String, limit: Int, kids: Boolean, hideBad: Boolean): List<Channel> {
        val (w, a) = where(ChannelFilter(), kids, hideBad, emptyList())
        val sql = "SELECT $cols FROM ch " + (if (w.isEmpty()) "WHERE $cond" else "$w AND $cond") + " $order LIMIT $limit"
        return db.rawQuery(sql, a).use { c -> buildList { while (c.moveToNext()) add(read(c)) } }
    }

    /** Kênh trận đấu chưa xong (đang đá / sắp đá / chưa tới giờ), giờ bóng lăn sớm trước — trang chủ «Trận đang / sắp đá». */
    fun unfinishedMatches(now: Double, kids: Boolean, hideBad: Boolean, limit: Int): List<Channel> {
        if (!hasMatches) return emptyList()
        val n = "%.0f".format(java.util.Locale.US, now)
        return select("kick IS NOT NULL AND $n < kick + dur * 60", "ORDER BY kick, id", limit, kids, hideBad)
    }

    /** Đài radio theo thứ tự danh sách — trang chủ «Radio». */
    fun radios(kids: Boolean, hideBad: Boolean, limit: Int): List<Channel> = select("radio = 1", "ORDER BY id", limit, kids, hideBad)

    /** Kênh theo mã (giữ thứ tự của [ids]). */
    fun byIds(ids: LongArray): List<Channel> {
        if (ids.isEmpty()) return emptyList()
        val m = HashMap<Long, Channel>(ids.size)
        db.rawQuery("SELECT $cols FROM ch WHERE id IN (${ids.joinToString(",")})", null).use { c ->
            while (c.moveToNext()) read(c).let { m[it.id] = it }
        }
        return ids.toList().mapNotNull { m[it] }
    }

    fun byKey(k: String): Channel? =
        db.rawQuery("SELECT $cols FROM ch WHERE kh = ${strHash(k)} AND k = ? ORDER BY id LIMIT 1", arrayOf(k))
            .use { if (it.moveToFirst()) read(it) else null }

    /** Kênh có số [num] trong [ids] (gõ số kênh bằng remote): vị trí trong [ids], -1 = không có. */
    fun indexOfNum(ids: LongArray, num: String): Int {
        val found = db.rawQuery("SELECT id FROM ch WHERE num = ?", arrayOf(num)).use { c ->
            buildSet { while (c.moveToNext()) add(c.getLong(0)) }
        }
        return ids.indexOfFirst { it in found }
    }

    fun setBad(id: Long, bad: Boolean) = runCatching { db.execSQL("UPDATE ch SET bad = ${if (bad) 1 else 0} WHERE id = $id") }

    fun clearBad() = runCatching { db.execSQL("UPDATE ch SET bad = 0") }

    /** EPG cần: kênh có tvg-id này / có tên chuẩn hoá này không. */
    fun hasTid(id: String) = db.rawQuery("SELECT 1 FROM ch WHERE th = ${strHash(id)} AND tid = ? LIMIT 1", arrayOf(id)).use { it.moveToFirst() }
    fun hasKey(k: String) = db.rawQuery("SELECT 1 FROM ch WHERE kh = ${strHash(k)} AND k = ? LIMIT 1", arrayOf(k)).use { it.moveToFirst() }

    companion object {
        /** Đổi cách đọc M3U / lược đồ → đệm cũ đọc lại (2: cột băm kh / th thay chỉ mục chữ; 3: bỏ mã nước «|UK| » đầu tên). */
        const val VERSION = 3

        /** Mã băm 64 bit của chuỗi (khoá kênh, tvg-id) — cột kh / th có chỉ mục số. */
        fun strHash(s: String): Long = LongLongMap.hash(s, 0)

        fun schema(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE meta(k TEXT PRIMARY KEY, v TEXT)")
            db.execSQL("CREATE TABLE grp(id INTEGER PRIMARY KEY, name TEXT, n INTEGER DEFAULT 0)")
            db.execSQL(
                "CREATE TABLE ch(id INTEGER PRIMARY KEY, k TEXT NOT NULL, g INTEGER NOT NULL, n TEXT, num TEXT, logo TEXT, tid TEXT, " +
                    "urls TEXT, ref TEXT, ua TEXT, hdr TEXT, kick REAL, dur INTEGER DEFAULT 0, radio INTEGER DEFAULT 0, " +
                    "nolink INTEGER DEFAULT 0, vod INTEGER DEFAULT 0, kids INTEGER DEFAULT 0, bad INTEGER DEFAULT 0, kh INTEGER, th INTEGER)",
            )
        }
    }
}

/**
 * Ghi danh sách vừa tải vào tệp SQLite mới: kênh trùng tên (đã chuẩn hoá) trong cùng nhóm gộp thành nhiều nguồn (HLS trước
 * FLV, link giả cuối); kênh thể thao ghi giờ trận trong tên thì tách giờ ra, mỗi BLV một kênh.
 */
class IptvBuilder(private val file: File, private val bad: Map<String, String>) {
    private companion object {
        /** Số dòng mỗi câu INSERT (20 cột × 48 = 960 tham số < 999 — giới hạn của SQLite đời cũ trên Android 5). */
        const val BATCH = 48
        const val COLS = 20
    }

    /** Một kênh chưa ghi xuống tệp (đang chờ đủ lô). */
    private class Row(
        val id: Long, val k: String, val g: Long, val n: String, val num: String, val logo: String?, val tid: String?,
        val urls: MutableList<String>, val ref: String?, val ua: String?, var hdr: JSONObject?, val kick: Double?, val dur: Int,
        val radio: Boolean, val vod: Boolean, val kids: Boolean,
    )

    private val db = SQLiteDatabase.openOrCreateDatabase(file, null)
    private val groups = LinkedHashMap<String, Long>()
    private val groupNorms = HashMap<String, String>()
    private val kidsGroups = HashMap<String, Boolean>()
    /** Số kênh của từng nhóm (đếm lúc chèn — đếm lại bằng SQL là quét cả bảng cho mỗi nhóm). */
    private val groupCount = HashMap<Long, Int>()
    private var index = 0
    private var nextId = 1L
    private var matches = false
    var epg: String? = null
    var entries = 0
        private set
    private val now = nowSec()

    /** Đo (nhật ký): thời gian tính khoá / tên, thời gian ghi SQLite (ns). */
    var keyNs = 0L
    var sqlNs = 0L

    /** Khoá gộp (tên chuẩn hoá + nhóm) → mã dòng — bảng băm trong RAM (~8 MB cho 350 nghìn mục), khỏi chỉ mục UNIQUE lúc chèn. */
    private val seen = LongLongMap()
    private val pending = ArrayList<Row>(BATCH)
    private var firstPending = 1L

    init {
        db.execSQL("PRAGMA synchronous = OFF")
        // Tệp tạm: hỏng giữa chừng thì xoá cả tệp — khỏi ghi nhật ký khôi phục
        db.rawQuery("PRAGMA journal_mode = OFF", null).use { it.moveToFirst() }
        db.rawQuery("PRAGMA cache_size = -8000", null).use { it.moveToFirst() }
        IptvStore.schema(db)
        db.beginTransaction()
    }

    private fun insertSql(rows: Int) = "INSERT INTO ch(id, k, g, n, num, logo, tid, urls, ref, ua, hdr, kick, dur, radio, nolink, vod, kids, bad, kh, th) VALUES " +
        List(rows) { "(" + List(COLS) { "?" }.joinToString(",") + ")" }.joinToString(",")

    private val insBatch = db.compileStatement(insertSql(BATCH))
    private val insGroup = db.compileStatement("INSERT INTO grp(id, name) VALUES(?, ?)")
    private val upd = db.compileStatement("UPDATE ch SET urls = ?, hdr = ?, nolink = ?, bad = ? WHERE id = ?")

    fun add(e: IptvParse.Entry) {
        val t0 = System.nanoTime()
        entries++
        index++
        var n = e.name
        val kick = IptvParse.kickoff(n, now)?.takeIf { IptvParse.isMatchName(n) }
        val k: String
        var minutes = 0
        if (kick != null) {
            matches = true
            minutes = IptvParse.matchMinutes(n)
            n = IptvParse.matchName(n, kick)
            k = IptvParse.norm(n.replace('(', ' ').replace(')', ' ')) + "@" + floor(kick.at / 60).toLong()
        } else {
            n = IptvParse.displayName(n)
            k = IptvParse.norm(n).ifEmpty { n.lowercase() }
        }
        val g = groups.getOrPut(e.group) {
            val id = groups.size + 1L
            insGroup.bindLong(1, id); insGroup.bindString(2, e.group); insGroup.executeInsert()
            id
        }
        val mk = LongLongMap.hash(k, g)
        val existing = seen.get(mk)
        if (existing >= 0) {
            keyNs += System.nanoTime() - t0
            merge(existing, k, g, e, mk)
            return
        }
        val path = IptvParse.pathOf(e.url)
        val gn = groupNorms.getOrPut(e.group) { IptvParse.norm(e.group) }
        val row = Row(
            nextId++, k, g, n, e.attrs["tvg-chno"]?.takeIf { it.isNotEmpty() } ?: index.toString(),
            e.attrs["tvg-logo"]?.takeIf { it.startsWith("http") }, e.attrs["tvg-id"]?.takeIf { it.isNotEmpty() },
            mutableListOf(e.url), e.ref?.takeIf { it.isNotEmpty() }, e.ua?.takeIf { it.isNotEmpty() }, null, kick?.at, minutes,
            IptvParse.isRadioNorm(e.attrs, gn, IptvParse.extOfPath(path)), IptvParse.isVodPath(path),
            kidsGroups.getOrPut(e.group) { IptvParse.isKidsGroup(gn) } || IptvParse.isKidsNorm("", k),
        )
        seen.put(mk, row.id)
        groupCount[g] = (groupCount[g] ?: 0) + 1
        pending += row
        keyNs += System.nanoTime() - t0
        if (pending.size >= BATCH) flush()
    }

    /** Ghi các kênh đang chờ: một câu INSERT nhiều dòng (mỗi câu lệnh riêng tốn ~0,2 ms trên TV box yếu). */
    private fun flush() {
        if (pending.isEmpty()) return
        val t1 = System.nanoTime()
        val st = if (pending.size == BATCH) insBatch else db.compileStatement(insertSql(pending.size))
        st.clearBindings()
        var p = 1
        for (r in pending) {
            st.bindLong(p++, r.id)
            st.bindString(p++, r.k)
            st.bindLong(p++, r.g)
            st.bindString(p++, r.n)
            st.bindString(p++, r.num)
            r.logo?.let { st.bindString(p, it) }; p++
            r.tid?.let { st.bindString(p, it) }; p++
            st.bindString(p++, r.urls.joinToString("\n"))
            r.ref?.let { st.bindString(p, it) }; p++
            r.ua?.let { st.bindString(p, it) }; p++
            r.hdr?.let { st.bindString(p, it.toString()) }; p++
            r.kick?.let { st.bindDouble(p, it) }; p++
            st.bindLong(p++, r.dur.toLong())
            st.bindLong(p++, if (r.radio) 1 else 0)
            st.bindLong(p++, if (r.urls.all(IptvParse::isPlaceholder)) 1 else 0)
            st.bindLong(p++, if (r.vod) 1 else 0)
            st.bindLong(p++, if (r.kids) 1 else 0)
            st.bindLong(p++, if (r.urls.all { it in bad }) 1 else 0)
            st.bindLong(p++, IptvStore.strHash(r.k))
            r.tid?.let { st.bindLong(p, IptvStore.strHash(it)) }; p++
        }
        st.executeInsert()
        if (st !== insBatch) st.close()
        pending.clear()
        firstPending = nextId
        sqlNs += System.nanoTime() - t1
    }

    /** Thêm [url] vào danh sách nguồn (HLS trước FLV, link giả cuối); false = đã có. */
    private fun addSource(urls: MutableList<String>, url: String): Boolean {
        if (url in urls) return false
        val rank = IptvParse.sourceRank(url)
        val at = urls.indexOfFirst { IptvParse.sourceRank(it) > rank }.let { if (it < 0) urls.size else it }
        urls.add(at, url)
        return true
    }

    /** Nguồn gộp vào có Referer / User-Agent khác kênh: ghi riêng cho nguồn. */
    private fun sourceHeaders(hdr: JSONObject?, ref: String?, ua: String?, e: IptvParse.Entry): JSONObject? {
        val sameRef = (e.ref.isNullOrEmpty() && ref == null) || e.ref == ref
        val sameUa = (e.ua.isNullOrEmpty() && ua == null) || e.ua == ua
        if (sameRef && sameUa) return hdr
        return (hdr ?: JSONObject()).put(e.url, JSONObject().put("ref", e.ref ?: "").put("ua", e.ua ?: ""))
    }

    /** Trùng tên trong nhóm: thêm thành nguồn của kênh đã có (dòng [id] — còn chờ ghi thì sửa ngay trong RAM). */
    private fun merge(id: Long, k: String, g: Long, e: IptvParse.Entry, mk: Long) {
        if (id >= firstPending) {
            val r = pending.firstOrNull { it.id == id } ?: return
            if (r.k != k || r.g != g) {
                // Hai khoá khác nhau trùng mã băm (rất hiếm): giữ thành kênh riêng
                seen.remove(mk)
                return add(e)
            }
            if (addSource(r.urls, e.url)) r.hdr = sourceHeaders(r.hdr, r.ref, r.ua, e)
            return
        }
        db.rawQuery("SELECT id, urls, ref, ua, hdr, k, g FROM ch WHERE id = $id", null).use { c ->
            if (!c.moveToFirst()) return
            if (c.getString(5) != k || c.getLong(6) != g) {
                seen.remove(mk)
                return add(e)
            }
            val urls = c.getString(1).split('\n').filter { it.isNotEmpty() }.toMutableList()
            if (!addSource(urls, e.url)) return
            val old = c.getString(4)?.let { runCatching { JSONObject(it) }.getOrNull() }
            val hdr = sourceHeaders(old, c.getString(2), c.getString(3), e)
            upd.clearBindings()
            upd.bindString(1, urls.joinToString("\n"))
            hdr?.let { upd.bindString(2, it.toString()) }
            upd.bindLong(3, if (urls.all(IptvParse::isPlaceholder)) 1 else 0)
            upd.bindLong(4, if (urls.all { it in bad }) 1 else 0)
            upd.bindLong(5, id)
            upd.executeUpdateDelete()
        }
    }

    /** Ghi xong: số kênh (0 = danh sách không có kênh nào — tệp bị xoá). */
    fun finish(): Int {
        flush()
        val setCount = db.compileStatement("UPDATE grp SET n = ? WHERE id = ?")
        for ((g, n) in groupCount) {
            setCount.bindLong(1, n.toLong())
            setCount.bindLong(2, g)
            setCount.executeUpdateDelete()
        }
        setCount.close()
        val count = groupCount.values.sum()
        fun meta(k: String, v: String?) = v?.let { db.execSQL("INSERT OR REPLACE INTO meta(k, v) VALUES(?, ?)", arrayOf(k, it)) }
        meta("v", IptvStore.VERSION.toString())
        meta("count", count.toString())
        meta("matches", if (matches) "1" else "0")
        meta("epg", epg)
        db.setTransactionSuccessful()
        db.endTransaction()
        if (count > 0) {
            // Sắp xếp trong RAM (tệp tạm của SQLite trên bộ nhớ trong của TV box rất chậm)
            db.rawQuery("PRAGMA temp_store = MEMORY", null).use { it.moveToFirst() }
            db.execSQL("CREATE INDEX ch_kh ON ch(kh)")
            db.execSQL("CREATE INDEX ch_g ON ch(g, id)")
            db.execSQL("CREATE INDEX ch_th ON ch(th)")
        }
        db.close()
        if (count == 0) file.delete()
        return count
    }

    fun abort() {
        runCatching { db.endTransaction() }
        runCatching { db.close() }
        file.delete()
    }
}

/**
 * Lịch phát sóng (XMLTV) của một danh sách trong SQLite (cacheDir/iptv/epg-<mã>-<giờ>.db): chỉ chương trình của kênh có
 * trong danh sách, từ 24 giờ trước tới 48 giờ sau. Khoá chương trình = tvg-id của kênh, không khớp thì «n:<tên chuẩn hoá>».
 */
class EpgStore(val file: File) {
    val db: SQLiteDatabase = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
    val fetched: Long
    val src: String?
    val channels: Int

    init {
        fun meta(k: String) = db.rawQuery("SELECT v FROM meta WHERE k=?", arrayOf(k)).use { if (it.moveToFirst()) it.getString(0) else null }
        fetched = meta("fetched")?.toLongOrNull() ?: 0
        src = meta("src")
        channels = meta("channels")?.toIntOrNull() ?: 0
    }

    fun close() = runCatching { db.close() }

    fun programmes(c: Channel): List<Programme> {
        for (key in c.guideKeys) {
            val list = db.rawQuery("SELECT s, e, t FROM prog WHERE key = ? ORDER BY s", arrayOf(key)).use { cur ->
                buildList { while (cur.moveToNext()) add(Programme(cur.getDouble(0), cur.getDouble(1), cur.getString(2))) }
            }
            if (list.isNotEmpty()) return list
        }
        return emptyList()
    }

    companion object {
        /**
         * Đọc XMLTV (đã giải nén nếu .gz) từ [input] vào tệp [file]. Kênh cần lấy: [hasTid] (tvg-id) hoặc [hasKey] (tên chuẩn
         * hoá của display-name). Trả số kênh có lịch.
         */
        fun build(input: InputStream, file: File, src: String, hasTid: (String) -> Boolean, hasKey: (String) -> Boolean): Int {
            val db = SQLiteDatabase.openOrCreateDatabase(file, null)
            try {
                db.execSQL("PRAGMA synchronous = OFF")
                db.execSQL("CREATE TABLE meta(k TEXT PRIMARY KEY, v TEXT)")
                db.execSQL("CREATE TABLE prog(key TEXT, s REAL, e REAL, t TEXT)")
                db.beginTransaction()
                val ins = db.compileStatement("INSERT INTO prog(key, s, e, t) VALUES(?, ?, ?, ?)")
                val now = nowSec()
                val from = now - 24 * 3600
                val to = now + 48 * 3600
                val keyOf = HashMap<String, String?>() // id kênh trong EPG → khoá chương trình (null = không cần)
                val keys = HashSet<String>()
                val p = Xml.newPullParser()
                p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                p.setInput(input, null)
                var chan: String? = null
                var progKey: String? = null
                var start = 0.0
                var stop: Double? = null
                var title: String? = null
                var ev = p.eventType
                while (ev != XmlPullParser.END_DOCUMENT) {
                    if (ev == XmlPullParser.START_TAG) {
                        when (p.name) {
                            "channel" -> chan = p.getAttributeValue(null, "id")
                            "display-name" -> {
                                val id = chan
                                if (id != null && keyOf[id] == null) {
                                    val n = IptvParse.norm(p.nextText())
                                    keyOf[id] = when {
                                        hasTid(id) -> id
                                        n.isNotEmpty() && hasKey(n) -> "n:$n"
                                        else -> null
                                    }
                                }
                            }
                            "programme" -> {
                                progKey = null
                                title = null
                                val cid = p.getAttributeValue(null, "channel")
                                if (cid != null) {
                                    val key = if (keyOf.containsKey(cid)) keyOf[cid] else (if (hasTid(cid)) cid else null).also { keyOf[cid] = it }
                                    val s = IptvParse.xmltvTime(p.getAttributeValue(null, "start"))
                                    stop = IptvParse.xmltvTime(p.getAttributeValue(null, "stop"))
                                    val e = stop
                                    if (key != null && s != null && s <= to && (if (e != null) e >= from else s >= from)) {
                                        progKey = key
                                        start = s
                                    }
                                }
                            }
                            "title" -> if (progKey != null && title == null) title = p.nextText().trim()
                        }
                    } else if (ev == XmlPullParser.END_TAG) {
                        when (p.name) {
                            "channel" -> chan = null
                            "programme" -> progKey?.let { key ->
                                ins.bindString(1, key)
                                ins.bindDouble(2, start)
                                ins.bindDouble(3, stop ?: (start + 1800))
                                ins.bindString(4, title?.takeIf { it.isNotEmpty() } ?: "(không tên)")
                                ins.executeInsert()
                                keys += key
                                progKey = null
                            }
                        }
                    }
                    ev = p.next()
                }
                db.execSQL("INSERT INTO meta(k, v) VALUES('fetched', ?)", arrayOf(System.currentTimeMillis().toString()))
                db.execSQL("INSERT INTO meta(k, v) VALUES('src', ?)", arrayOf(src))
                db.execSQL("INSERT INTO meta(k, v) VALUES('channels', ?)", arrayOf(keys.size.toString()))
                db.setTransactionSuccessful()
                db.endTransaction()
                db.execSQL("CREATE INDEX prog_ks ON prog(key, s)")
                return keys.size
            } finally {
                runCatching { if (db.inTransaction()) db.endTransaction() }
                db.close()
            }
        }
    }
}

/**
 * Truyền hình (IPTV): danh sách kênh M3U / M3U8 người xem tự thêm (app không kèm sẵn kênh), lịch phát sóng XMLTV.
 * Cùng khoá cài đặt với bản Legacy: iptv_lists, iptv_active, iptv_epg_url (cả nhà); iptv_favs, iptv_recent (từng người
 * xem); iptv_bad (nguồn máy không phát được), iptv_hide_bad, iptv_auto_source.
 * Tải lại khi cũ quá 6 giờ (danh sách có trận: 30 phút; trận sắp / đang đá mà chưa có link thật: 5 phút) — lúc vào màn
 * Truyền hình và tự làm mới khi màn đang mở.
 */
object Iptv {
    private const val LIST_MAX_AGE = 6L * 3600 * 1000
    private const val MATCH_LIST_MAX_AGE = 30L * 60 * 1000
    private const val WAITING_LIST_MAX_AGE = 5L * 60 * 1000
    private const val AUTO_RETRY_GAP = 3L * 60 * 1000
    private const val GUIDE_MAX_AGE = 6L * 3600 * 1000

    /** Tệp danh sách lớn nhất nhận / đọc từ tệp (danh sách Xtream lớn cỡ vài chục MB). */
    const val MAX_FILE_BYTES = 120L * 1024 * 1024

    private lateinit var cache: File
    private lateinit var files: File

    fun init(c: Context) {
        cache = File(c.cacheDir, "iptv").apply { mkdirs() }
        files = File(c.filesDir, "iptv").apply { mkdirs() }
        inBackground({ cleanup() }) {}
    }

    // ---- Thông báo thay đổi (danh sách / lịch vừa tải xong, trạng thái tải) ----

    private val listeners = LinkedHashSet<() -> Unit>()

    fun listen(l: () -> Unit) {
        listeners += l
    }

    fun unlisten(l: () -> Unit) {
        listeners -= l
    }

    /** Lần đổi dữ liệu (danh sách mới / lịch mới / dấu hỏng) — trang kênh đã nạp trong bộ đệm cũ đi. */
    @Volatile var version = 0
        private set

    /** [data] = dữ liệu đổi (danh sách / lịch / dấu hỏng / yêu thích); false = chỉ dòng trạng thái tải. */
    private fun changed(data: Boolean = true) {
        if (data) version++
        mainHandler.post { listeners.toList().forEach { it() } }
    }

    // ---- Danh sách ----

    val lists: List<IptvList>
        get() = runCatching {
            val a = JSONArray(Prefs.iptvGet("iptv_lists") ?: "[]")
            (0 until a.length()).map { IptvList.from(a.getJSONObject(it)) }
        }.getOrDefault(emptyList())

    @Synchronized
    private fun saveLists(l: List<IptvList>) = Prefs.iptvSet("iptv_lists", JSONArray(l.map { it.json() }).toString())

    @Synchronized
    private fun updateList(id: String, f: (IptvList) -> IptvList) = saveLists(lists.map { if (it.id == id) f(it) else it })

    val activeList: IptvList?
        get() {
            val l = lists
            val a = Prefs.iptvGet("iptv_active")
            return l.firstOrNull { it.id == a } ?: l.firstOrNull()
        }

    fun setActive(id: String?) {
        Prefs.iptvSet("iptv_active", id)
        changed()
    }

    fun addList(input: String, name: String? = null): IptvList {
        val url = IptvParse.listUrl(input)
        val l = IptvList("l${System.currentTimeMillis()}", name?.trim()?.takeIf { it.isNotEmpty() } ?: IptvParse.nameFromUrl(url), url, null, null)
        synchronized(this) { saveLists(lists + l) } // đọc + ghi danh sách liền một mạch (luồng nhận tệp / tải xong cùng ghi)
        setActive(l.id)
        return l
    }

    /** Kết quả thêm nhiều danh sách một lần: danh sách vừa thêm (theo thứ tự dán), số địa chỉ đã có sẵn (bỏ qua). */
    class AddMany(val added: List<IptvList>, val existing: Int)

    /**
     * Thêm nhiều danh sách cùng lúc (địa chỉ tách bằng `IptvParse.splitUrls` — dán mỗi dòng một danh sách). Địa chỉ đã có thì bỏ
     * qua. Danh sách đầu tiên vừa thêm thành danh sách đang dùng (các danh sách khác tải khi được chọn). Tên: một danh sách thì dùng
     * [name]; nhiều thì «name 1», «name 2»…; không có tên thì đặt theo địa chỉ.
     */
    fun addLists(text: String, name: String? = null): AddMany = synchronized(this) { addListsLocked(text, name) }.also { r ->
        if (r.added.isNotEmpty()) setActive(r.added.first().id)
    }

    private fun addListsLocked(text: String, name: String?): AddMany {
        val all = lists.toMutableList()
        var existing = 0
        val fresh = IptvParse.splitUrls(text).filter { u ->
            val k = IptvParse.listUrlKey(u)
            val dup = all.any { l -> l.url?.let { IptvParse.listUrlKey(it) } == k }
            if (dup) existing++
            !dup
        }
        val nm = name?.trim()?.takeIf { it.isNotEmpty() }
        var stamp = System.currentTimeMillis()
        val added = fresh.mapIndexed { i, url ->
            while (all.any { it.id == "l$stamp" }) stamp++ // mã riêng cho từng danh sách (cùng một mili-giây)
            val n = when {
                nm == null -> IptvParse.nameFromUrl(url)
                fresh.size == 1 -> nm
                else -> "$nm ${i + 1}"
            }
            IptvList("l$stamp", n, url, null, null).also { stamp++; all += it }
        }
        if (added.isNotEmpty()) saveLists(all)
        return AddMany(added, existing)
    }

    /** Thêm danh sách từ nội dung tệp M3U (điện thoại / máy tính gửi qua Wi-Fi, tệp trên máy): chép vào filesDir/iptv. */
    fun addListFromFile(src: File, fileName: String, name: String?): Result<IptvList> {
        val head = src.inputStream().use { s -> ByteArray(64 * 1024).let { b -> String(b, 0, maxOf(0, s.read(b)), Charsets.UTF_8) } }
        if (src.length() == 0L) return Result.failure(IllegalArgumentException("Tệp trống."))
        if ("#EXTINF" !in head && "http" !in head) {
            return Result.failure(IllegalArgumentException("Tệp không phải danh sách M3U (không có dòng #EXTINF / địa chỉ kênh nào)."))
        }
        // Chép vào thư mục danh sách + ghi danh sách liền một mạch: dọn tệp (cleanup) chạy giữa chừng thì xoá mất tệp chưa có
        // trong danh sách; hai tệp gửi tới cùng lúc thì một danh sách bị ghi đè mất
        val l = synchronized(this) {
            var stamp = System.currentTimeMillis()
            while (lists.any { it.id == "l$stamp" }) stamp++
            val id = "l$stamp"
            val dest = File(files, "$id.m3u")
            if (!src.renameTo(dest)) {
                runCatching { src.copyTo(dest, overwrite = true); src.delete() }
                    .onFailure { return Result.failure(IllegalStateException("Không ghi được tệp vào máy (bộ nhớ đầy?).")) }
            }
            IptvList(id, name?.trim()?.takeIf { it.isNotEmpty() } ?: IptvParse.nameFromFile(fileName), null, dest.name,
                fileName.substringAfterLast('/')).also { saveLists(lists + it) }
        }
        val id = l.id
        val dest = File(files, l.file!!)
        setActive(id)
        TvLog.i("IPTV: thêm danh sách từ tệp «$fileName» (${dest.length()} byte) → $id")
        return Result.success(l)
    }

    /** Tệp tạm để nhận danh sách gửi tới (trước khi addListFromFile). */
    fun tempFile(): File = File(cache, "in-${System.nanoTime()}.m3u")

    /** Bản gỡ lỗi: quên dấu của tệp lần trước — lần tải sau đọc lại cả danh sách (đo thời gian). */
    fun forgetSignature() {
        activeList?.let { l -> updateList(l.id) { it.copy(sig = null) } }
    }

    fun renameList(id: String, name: String) {
        if (name.isNotBlank()) updateList(id) { it.copy(name = name.trim()) }
        changed()
    }

    fun removeList(id: String) {
        val l = lists.firstOrNull { it.id == id } ?: return
        synchronized(this) { saveLists(lists.filter { it.id != id }) }
        l.file?.let { File(files, it).delete() }
        synchronized(this) {
            if (storeList == id) retire()
            if (guideList == id) retireGuide()
        }
        inBackground({ cleanup() }) {}
        if (Prefs.iptvGet("iptv_active") == id) setActive(lists.firstOrNull()?.id) else changed()
    }

    // ---- Kênh của danh sách đang chọn ----

    private var store: IptvStore? = null
    private var storeList: String? = null
    private var guide: EpgStore? = null
    private var guideList: String? = null

    /** Kênh của danh sách đang chọn (đã có tệp); null = chưa tải. Mở được ở luồng nào cũng được. */
    @Synchronized
    fun store(): IptvStore? {
        val l = activeList ?: return null
        val name = l.db ?: return null
        store?.let { if (storeList == l.id && it.file.name == name) return it }
        retire()
        val f = File(cache, name)
        if (!f.exists()) return null
        val s = runCatching { IptvStore(f) }.getOrNull() ?: return null
        if (s.version < IptvStore.VERSION) {
            s.close()
            return null
        }
        store = s
        storeList = l.id
        return s
    }

    /** Lịch phát sóng của danh sách đang chọn (null = chưa có). */
    @Synchronized
    fun guide(): EpgStore? {
        val l = activeList ?: return null
        val name = epgFileOf(l.id) ?: return null
        guide?.let { if (guideList == l.id && it.file.name == name) return it }
        retireGuide()
        val g = runCatching { EpgStore(File(cache, name)) }.getOrNull() ?: return null
        guide = g
        guideList = l.id
        return g
    }

    private fun epgFileOf(listId: String): String? =
        cache.listFiles()?.filter { it.name.startsWith("epg-$listId-") && it.name.endsWith(".db") }?.maxByOrNull { it.name }?.name

    /** Bỏ tệp cũ: đóng sau một lúc (truy vấn đang chạy ở luồng khác xong đã). */
    private fun retire() {
        store?.let { old -> mainHandler.postDelayed({ old.close(); cleanupLater() }, 30_000) }
        store = null
        storeList = null
    }

    private fun retireGuide() {
        guide?.let { old -> mainHandler.postDelayed({ old.close(); cleanupLater() }, 30_000) }
        guide = null
        guideList = null
    }

    private fun cleanupLater() = inBackground({ cleanup() }) {}

    /** Xoá tệp SQLite không còn dùng (danh sách đã xoá / bản cũ trước lần tải lại). */
    @Synchronized
    private fun cleanup() {
        val l = lists
        val keep = HashSet<String>()
        l.mapNotNullTo(keep) { it.db }
        store?.file?.name?.let(keep::add)
        guide?.file?.name?.let(keep::add)
        l.forEach { x -> epgFileOf(x.id)?.let(keep::add) }
        cache.listFiles()?.forEach { f ->
            // Tệp phụ của SQLite (-journal, -wal, -shm) đi theo tệp chính: không thì mỗi lần làm mới lịch để lại 64 KB tệp mồ côi
            val base = f.name.removeSuffix("-journal").removeSuffix("-wal").removeSuffix("-shm")
            if (base.endsWith(".db") && base !in keep && System.currentTimeMillis() - f.lastModified() > 60_000) f.delete()
            if ((f.name.startsWith("in-") || f.name.startsWith("dl-") || f.name.contains(".db.part")) &&
                System.currentTimeMillis() - f.lastModified() > 3600_000
            ) f.delete()
        }
        val files = l.mapNotNull { it.file }.toSet()
        this.files.listFiles()?.forEach { if (it.name !in files) it.delete() }
    }

    // ---- Tải ----

    @Volatile var loading = false
        private set
    @Volatile var guideLoading = false
        private set
    @Volatile var status: String? = null
        private set
    private var lastTry = 0L
    private val waiters = ArrayList<(String?) -> Unit>()

    val busy get() = loading || guideLoading

    private fun finishWaiters(err: String?) {
        val w = waiters.toList()
        waiters.clear()
        w.forEach { it(err) }
    }

    /** Danh sách cũ quá bao lâu thì tải lại (ms): trận sắp / đang đá mà chưa có link thật → 5 phút; có trận → 30 phút; 6 giờ. */
    private fun maxAge(s: IptvStore?): Long {
        if (s == null || !s.hasMatches) return LIST_MAX_AGE
        val n = "%.0f".format(java.util.Locale.US, nowSec())
        val waiting = runCatching {
            s.db.rawQuery(
                "SELECT 1 FROM ch WHERE kick IS NOT NULL AND nolink = 1 AND $n >= kick - ${IptvParse.MATCH_EARLY.toLong()} " +
                    "AND $n < kick + dur * 60 LIMIT 1", null,
            ).use { it.moveToFirst() }
        }.getOrDefault(false)
        return if (waiting) WAITING_LIST_MAX_AGE else MATCH_LIST_MAX_AGE
    }

    /** Màn Truyền hình đang mở (mỗi 30 giây / lúc app trở lại): tải lại danh sách nếu đã quá hạn; vừa tải hỏng thì 3 phút sau. */
    fun autoRefresh() {
        val l = activeList ?: return
        if (l.file != null || busy || System.currentTimeMillis() - lastTry < AUTO_RETRY_GAP) return
        inBackground({ store().let { s -> s != null && System.currentTimeMillis() - l.updated < maxAge(s) } }) { r ->
            if (r.getOrDefault(true) || busy) return@inBackground
            TvLog.i("IPTV: tự làm mới danh sách «${l.name}» (lần trước ${if (l.updated > 0) IptvParse.clock(l.updated / 1000.0) else "chưa"})")
            refresh(false, null)
        }
    }

    /** Tải lại danh sách đang chọn nếu cũ hơn [seconds] (đang tải thì chờ xong). */
    fun refreshIfOlderThan(seconds: Int, done: (String?) -> Unit) {
        val old = System.currentTimeMillis() - (activeList?.updated ?: 0) > seconds * 1000L
        if (old || loading) refresh(old, done) else done(null)
    }

    /**
     * Tải danh sách đang chọn (force: bỏ qua bản đã có còn mới), rồi lịch nếu cũ. done (luồng chính): null = được, không thì
     * lý do. Danh sách thêm từ tệp: đọc lại tệp đã chép (không tải mạng).
     */
    fun refresh(force: Boolean, done: ((String?) -> Unit)?) {
        val l = activeList
        if (l == null || (l.url == null && l.file == null)) {
            done?.invoke("Chưa có danh sách kênh")
            return
        }
        done?.let { waiters += it }
        if (loading) return
        loading = true
        inBackground({ store() to (store()?.let { System.currentTimeMillis() - l.updated < maxAge(it) } ?: false) }) { r ->
            val (have, freshEnough) = r.getOrDefault(null to false)
            val upToDate = (l.file != null && l.updated > 0 && have != null) || freshEnough
            if (!force && upToDate && have != null) {
                loading = false
                finishWaiters(null)
                refreshGuide(false, null)
                return@inBackground
            }
            load(l)
        }
    }

    /** Kết quả một lần tải: [count] = 0 → danh sách hỏng; [same] = tệp y như lần trước (khỏi đọc lại). */
    private class Loaded(val count: Int, val epg: String?, val sawExtinf: Boolean, val sig: String?, val same: Boolean = false)

    private fun load(l: IptvList) {
        lastTry = System.currentTimeMillis()
        status = if (l.file != null) "Đang đọc danh sách kênh…" else "Đang tải danh sách kênh…"
        changed(false)
        val t0 = System.currentTimeMillis()
        val bad = badMap()
        val out = File(cache, "${l.id}-${System.currentTimeMillis()}.db")
        inBackground({
            // Danh sách theo địa chỉ: tải hết ra tệp tạm trước (mạng không làm chậm khâu đọc), so dấu với lần trước — danh
            // sách Xtream vài chục MB tự làm mới mỗi 6 giờ mà thường không đổi
            val (src, sig) = if (l.file != null) File(files, l.file) to null else download(l)
            try {
                if (sig != null && sig == l.sig && l.db != null && File(cache, l.db).exists()) {
                    return@inBackground Loaded(l.count, l.epg, true, sig, same = true)
                }
                parse(src, out, bad).let { Loaded(it.first, it.second, it.third, sig) }
            } finally {
                if (l.file == null) src.delete()
            }
        }) { r ->
            loading = false
            status = null
            // Trong lúc tải người dùng đã chọn / thêm danh sách khác (refresh lúc đang tải chỉ xếp hàng chờ): tải tiếp danh sách
            // đó, người chờ chờ tiếp — không thì báo «xong» mà danh sách mới chưa tải, màn Truyền hình hiện «Chưa tải danh sách»
            fun finish(err: String?, guide: Boolean = false) {
                val a = activeList
                if (a != null && a.id != l.id && (a.url != null || a.file != null)) {
                    refresh(false, null)
                    return
                }
                finishWaiters(err)
                if (guide) refreshGuide(false, null)
            }
            r.onFailure { e ->
                val why = if (e is java.io.IOException) e.friendly() else (e.message ?: e.javaClass.simpleName)
                TvLog.i("IPTV: tải danh sách ${l.url ?: l.file} lỗi: $why")
                updateList(l.id) { it.copy(err = why) }
                changed()
                finish("Không tải được danh sách kênh: $why")
            }
            r.onSuccess { x ->
                if (x.same) {
                    TvLog.i("IPTV: danh sách ${l.url} không đổi từ lần trước (${System.currentTimeMillis() - t0} ms)")
                    updateList(l.id) { it.copy(updated = System.currentTimeMillis(), err = null) }
                    changed(false)
                    finish(null, guide = true)
                    return@onSuccess
                }
                if (x.count == 0) {
                    val why = if (!x.sawExtinf) "Tệp tải về không phải danh sách M3U." else "Danh sách không có kênh nào."
                    updateList(l.id) { it.copy(err = why) }
                    changed()
                    finish(why)
                    return@onSuccess
                }
                TvLog.i("IPTV: danh sách ${l.url ?: l.file} — ${x.count} kênh, lịch ${x.epg ?: "không có"} (${System.currentTimeMillis() - t0} ms)")
                synchronized(this) {
                    updateList(l.id) {
                        it.copy(count = x.count, updated = System.currentTimeMillis(), err = null, epg = x.epg, db = out.name, sig = x.sig)
                    }
                }
                changed()
                finish(null, guide = true)
            }
        }
    }

    /**
     * Mở địa chỉ danh sách / lịch qua mạng của app (DNS mã hoá, vượt chặn, proxy); lỗi kết nối (không phải mã lỗi HTTP) mà
     * không bật proxy thì thử lại bằng mạng của máy — DNS mã hoá chỉ lấy một địa chỉ của tên miền, máy chủ rút gọn link có địa
     * chỉ chết thì hỏng (gặp trên TX3 16/11: một trong hai địa chỉ của dịch vụ rút gọn link không nhận kết nối). Bản Legacy cũng làm vậy.
     */
    internal fun openUrl(url: String, headers: Map<String, String> = emptyMap()): java.net.HttpURLConnection {
        val c = try {
            Http.open(url, headers)
        } catch (e: java.io.IOException) {
            if (AppProxy.active) throw e
            TvLog.i("IPTV: mạng của app lỗi với $url (${e.message}) — thử mạng của máy")
            Http.openDirect(url, headers)
        }
        c.readTimeout = 60_000
        if (c.responseCode >= 400) {
            c.disconnect()
            throw Http.HttpError(c.responseCode)
        }
        return c
    }

    /** Tải danh sách theo địa chỉ ra tệp tạm (qua mạng của app — proxy như phần còn lại): (tệp, dấu = phiên bản đọc:CRC32:cỡ). */
    private fun download(l: IptvList): Pair<File, String> {
        val c = openUrl(l.url!!)
        val tmp = File(cache, "dl-${l.id}.m3u")
        val crc = java.util.zip.CRC32()
        var total = 0L
        var shown = 0L
        try {
            c.inputStream.use { input ->
                tmp.outputStream().use { o ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        o.write(buf, 0, n)
                        crc.update(buf, 0, n)
                        total += n
                        if (total - shown >= 2 shl 20) {
                            shown = total
                            status = "Đang tải danh sách kênh… ${total shr 20} MB"
                            changed(false)
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            tmp.delete()
            throw e
        } finally {
            c.disconnect()
        }
        return tmp to "${IptvStore.VERSION}:${crc.value}:$total"
    }

    /** Đọc tệp M3U [src] (tự giải nén .gz) vào tệp SQLite [out]: (số kênh, địa chỉ lịch, có dòng #EXTINF). */
    private fun parse(src: File, out: File, bad: Map<String, String>): Triple<Int, String?, Boolean> {
        val b = IptvBuilder(out, bad)
        try {
            val raw = BufferedInputStream(src.inputStream(), 64 * 1024)
            raw.mark(4)
            val gz = raw.read() == 0x1f && raw.read() == 0x8b
            raw.reset()
            (if (gz) GZIPInputStream(raw, 64 * 1024) else raw).use { input ->
                val reader = BufferedReader(InputStreamReader(input, Charsets.UTF_8), 64 * 1024)
                val m3u = IptvParse.Reader({ b.epg = it }) { e ->
                    b.add(e)
                    if (b.entries % 5000 == 0) {
                        status = "Đang đọc danh sách kênh… %,d mục".format(b.entries).replace(',', '.')
                        changed(false)
                    }
                }
                var lines = 0
                var sawExtinf = false
                var lineNs = 0L
                while (true) {
                    val line = reader.readLine() ?: break
                    if (!sawExtinf && lines < 200 && line.startsWith("#EXTINF")) sawExtinf = true
                    lines++
                    val l0 = System.nanoTime()
                    m3u.line(line)
                    lineNs += System.nanoTime() - l0
                }
                val f0 = System.nanoTime()
                val count = b.finish()
                TvLog.i("IPTV: ${b.entries} mục — đọc dòng ${(lineNs - b.keyNs - b.sqlNs) / 1_000_000} ms, khoá ${b.keyNs / 1_000_000} ms, " +
                    "SQLite ${b.sqlNs / 1_000_000} ms, ghi xong + chỉ mục ${(System.nanoTime() - f0) / 1_000_000} ms")
                return Triple(count, b.epg, sawExtinf)
            }
        } catch (e: Throwable) {
            b.abort()
            throw e
        }
    }

    // ---- Lịch ----

    var epgOverride: String?
        get() = Prefs.iptvGet("iptv_epg_url")?.takeIf { it.isNotBlank() }
        set(v) = Prefs.iptvSet("iptv_epg_url", v?.trim()?.takeIf { it.isNotEmpty() })

    /** Địa chỉ EPG sẽ dùng cho danh sách đang chọn. */
    val epgUrl: String? get() = epgOverride ?: activeList?.epg

    fun refreshGuide(force: Boolean, done: ((String?) -> Unit)?) {
        val l = activeList
        val url = epgUrl
        if (l == null || url == null) {
            done?.invoke(if (url == null) "Danh sách không ghi địa chỉ lịch phát sóng (EPG)" else "Chưa có danh sách kênh")
            return
        }
        if (guideLoading) {
            done?.invoke(null)
            return
        }
        guideLoading = true
        inBackground({ store() to guide() }) { r ->
            val (s, g) = r.getOrDefault(null to null)
            if (s == null || (!force && g != null && g.src == url && System.currentTimeMillis() - g.fetched < GUIDE_MAX_AGE)) {
                guideLoading = false
                done?.invoke(if (s == null) "Chưa tải danh sách kênh" else null)
                return@inBackground
            }
            status = "Đang tải lịch phát sóng…"
            changed(false)
            val out = File(cache, "epg-${l.id}-${System.currentTimeMillis()}.db")
            // Dựng vào tệp tạm rồi mới đổi tên: tệp .db mới nhất là lịch đang dùng (epgFileOf) — dựng thẳng vào tên đó thì màn
            // đang mở đọc phải tệp dở dang (mất lịch suốt lúc tải) và dọn tệp xoá luôn lịch cũ còn tốt
            val part = File(cache, out.name + ".part")
            val t0 = System.currentTimeMillis()
            inBackground({
                val c = openUrl(url)
                val b = BufferedInputStream(c.inputStream, 64 * 1024)
                b.mark(4)
                val gz = b.read() == 0x1f && b.read() == 0x8b
                b.reset()
                (if (gz) GZIPInputStream(b, 64 * 1024) else b).use { input ->
                    try {
                        val n = EpgStore.build(input, part, url, s::hasTid, s::hasKey)
                        File(part.path + "-journal").delete()
                        if (!part.renameTo(out)) throw java.io.IOException("không ghi được tệp lịch")
                        n
                    } catch (e: Throwable) {
                        part.delete()
                        File(part.path + "-journal").delete()
                        throw e
                    }
                }
            }) { r2 ->
                guideLoading = false
                status = null
                r2.onSuccess { n ->
                    TvLog.i("IPTV: lịch $url — $n kênh có lịch (${System.currentTimeMillis() - t0} ms)")
                    synchronized(this) { retireGuide() }
                    cleanupLater()
                }
                r2.onFailure { TvLog.i("IPTV: tải lịch $url lỗi: ${it.message}") }
                changed()
                done?.invoke(r2.exceptionOrNull()?.let { "Không tải được lịch phát sóng: " + (if (it is java.io.IOException) it.friendly() else it.message) })
            }
        }
    }

    // ---- Yêu thích / vừa xem / kênh hỏng (khoá kênh = k) ----

    val favoriteKeys: List<String>
        get() = runCatching { JSONArray(Prefs.iptvGet("iptv_favs", own = true) ?: "[]").let { a -> (0 until a.length()).map(a::getString) } }
            .getOrDefault(emptyList())

    fun isFavorite(c: Channel) = c.k in favoriteKeys

    fun setFavorite(c: Channel, on: Boolean) {
        val f = favoriteKeys.filter { it != c.k }.toMutableList()
        if (on) f += c.k
        Prefs.iptvSet("iptv_favs", JSONArray(f).toString(), own = true)
        changed()
    }

    /** Kênh vừa xem (mới nhất trước, tối đa 12): {k, n, logo, g}. */
    val recent: List<JSONObject>
        get() = runCatching { JSONArray(Prefs.iptvGet("iptv_recent", own = true) ?: "[]").let { a -> (0 until a.length()).map(a::getJSONObject) } }
            .getOrDefault(emptyList())

    fun addRecent(c: Channel) {
        if (Prefs.privateMode) return
        val item = JSONObject().put("k", c.k).put("n", c.n).put("g", c.g)
        activeList?.let { item.put("l", it.id) } // danh sách của kênh — bấm ở trang chủ thì đổi sang danh sách đó
        c.logo?.let { item.put("logo", it) }
        if (c.radio) item.put("radio", true)
        val r = listOf(item) + recent.filter { it.optString("k") != c.k }.take(11)
        Prefs.iptvSet("iptv_recent", JSONArray(r).toString(), own = true)
    }

    /**
     * Dữ liệu các hàng Truyền hình của trang chủ (gọi ở luồng nền): [ids] = các hàng cần (HomeFeed.TV_IDS đang bật), tối đa
     * [max] ô mỗi hàng; hàng không có kênh thì không có trong kết quả. Chưa có danh sách kênh → rỗng. Danh sách chưa đọc vào máy
     * (chưa tải lần nào) thì chỉ có «Kênh vừa xem» (tên / logo đã lưu).
     *  - tv-recent: kênh vừa xem của người xem, đúng thứ tự (kênh của danh sách đang dùng: kèm chương trình đang phát).
     *  - tv-favs: kênh yêu thích của danh sách đang dùng.
     *  - tv-live: trận đang đá (có link trước) rồi sắp đá (sớm trước).
     *  - tv-radio: đài vừa nghe trước, rồi các đài khác theo thứ tự danh sách.
     */
    fun homeRows(ids: Set<String>, max: Int): Map<String, List<HomeChannel>> {
        val l = activeList ?: return emptyMap()
        val kids = Prefs.kids
        val hide = hideBad
        val now = nowSec()
        val s = store()
        val g = if (s != null) guide() else null
        val favKeys = favoriteKeys
        val favs = favKeys.toSet()
        val rec = recent
        fun progs(c: Channel): List<Programme> =
            if (g == null) emptyList() else runCatching { g.programmes(c) }.getOrDefault(emptyList()).filter { it.e > now }.take(3)
        fun home(c: Channel) = HomeChannel(c.k, l.id, c.id, c.n, c.num, c.logo, c.g, c.radio, c.vod, c.kick, c.dur, c.nolink,
            c.k in favs, progs(c))
        val out = LinkedHashMap<String, List<HomeChannel>>()
        if ("tv-recent" in ids) {
            val list = rec.take(max).map { o ->
                val k = o.optString("k")
                val lid = o.optString("l").ifEmpty { null }
                val c = if (s != null && (lid == null || lid == l.id)) runCatching { s.byKey(k) }.getOrNull() else null
                c?.let(::home) ?: HomeChannel(k, lid, -1, o.optString("n"), "", o.optString("logo").ifEmpty { null }, o.optString("g"),
                    o.optBoolean("radio"), false, 0.0, 0, false, false, emptyList())
            }
            if (list.isNotEmpty()) out["tv-recent"] = list
        }
        if (s == null) return out
        if ("tv-favs" in ids && favKeys.isNotEmpty()) {
            val f = s.ids(ChannelFilter(favs = true), kids, hide, favKeys, now)
            val list = s.byIds(f.copyOf(minOf(max, f.size))).map(::home)
            if (list.isNotEmpty()) out["tv-favs"] = list
        }
        if ("tv-live" in ids && !kids) {
            fun rank(c: Channel) = when (c.match(now)) {
                IptvParse.Match.LIVE -> if (c.nolink) 1 else 0
                IptvParse.Match.SOON -> 2
                else -> 3
            }
            val list = s.unfinishedMatches(now, kids, hide, 300).filter { rank(it) < 3 }
                .sortedWith(compareBy<Channel>({ rank(it) }, { it.kick }, { it.id })).take(max).map(::home)
            if (list.isNotEmpty()) out["tv-live"] = list
        }
        if ("tv-radio" in ids && !kids) {
            val heard = rec.filter { it.optBoolean("radio") && (it.optString("l").ifEmpty { null } ?: l.id) == l.id }
                .mapNotNull { o -> runCatching { s.byKey(o.optString("k")) }.getOrNull() }
            val list = (heard + s.radios(kids, hide, max + heard.size)).distinctBy { it.k }.take(max).map(::home)
            if (list.isNotEmpty()) out["tv-radio"] = list
        }
        return out
    }

    private fun badMap(): Map<String, String> = runCatching {
        val o = JSONObject(Prefs.iptvGet("iptv_bad") ?: "{}")
        o.keys().asSequence().associateWith { o.getString(it) }
    }.getOrDefault(emptyMap())

    fun badReason(url: String): String? = badMap()[url]

    fun markBad(c: Channel, url: String, reason: String) {
        val m = badMap().toMutableMap()
        if (m.size > 500) m.clear()
        m[url] = reason
        Prefs.iptvSet("iptv_bad", JSONObject(m as Map<*, *>).toString())
        if (c.urls.all { it in m }) store()?.setBad(c.id, true)
        changed()
    }

    fun clearBad() {
        Prefs.iptvSet("iptv_bad", null)
        inBackground({ store()?.clearBad() }) { changed() }
    }

    /** Bỏ dấu «không phát được» của mọi nguồn của kênh (người xem bấm «Thử lại kênh này»). */
    fun clearBadFor(c: Channel) {
        val m = badMap().toMutableMap()
        c.urls.forEach { m.remove(it) }
        Prefs.iptvSet("iptv_bad", JSONObject(m as Map<*, *>).toString())
        store()?.setBad(c.id, false)
        changed()
    }

    /** Mọi nguồn của kênh đều đã ghi là không phát được → lý do của nguồn đầu. */
    fun channelBadReason(c: Channel): String? {
        if (c.urls.isEmpty()) return null
        val m = badMap()
        return if (c.urls.all { it in m }) m[c.urls[0]] else null
    }

    // ---- Cài đặt ----

    /** Ẩn kênh mà mọi nguồn đều đã ghi «máy không giải mã được» (lỗi mạng thoáng qua không ghi). */
    var hideBad: Boolean
        get() = Prefs.iptvFlag("iptv_hide_bad", true)
        set(v) {
            Prefs.setIptvFlag("iptv_hide_bad", v)
            changed()
        }

    var autoSource: Boolean
        get() = Prefs.iptvFlag("iptv_auto_source", true)
        set(v) = Prefs.setIptvFlag("iptv_auto_source", v)
}

/**
 * Bảng băm long → long (địa chỉ mở, dò tuyến tính) cho IptvBuilder: khoá gộp của vài trăm nghìn mục mà không tốn bộ nhớ như
 * HashMap<Long, Long> (mỗi mục ~16 byte thay vì ~70). Khoá 0 dành cho ô trống; giá trị -1 = không có.
 */
class LongLongMap {
    private var keys = LongArray(1 shl 12)
    private var vals = LongArray(1 shl 12)
    private var size = 0

    companion object {
        private const val TOMB = Long.MIN_VALUE

        /** Mã băm 64 bit (FNV-1a) của tên chuẩn hoá + mã nhóm. */
        fun hash(k: String, g: Long): Long {
            var h = -0x340d631b7bdddcdbL
            for (ch in k) {
                h = (h xor ch.code.toLong()) * 0x100000001b3L
            }
            h = (h xor g) * 0x100000001b3L
            return if (h == 0L || h == TOMB) 1 else h
        }
    }

    private fun slot(k: Long, mask: Int): Int = ((k xor (k ushr 29)).toInt() * -0x61c88647) and mask

    fun get(k: Long): Long {
        val mask = keys.size - 1
        var i = slot(k, mask)
        while (true) {
            val x = keys[i]
            if (x == 0L) return -1
            if (x == k) return vals[i]
            i = (i + 1) and mask
        }
    }

    fun put(k: Long, v: Long) {
        if ((size + 1) * 4 > keys.size * 3) grow()
        val mask = keys.size - 1
        var i = slot(k, mask)
        while (keys[i] != 0L && keys[i] != TOMB && keys[i] != k) i = (i + 1) and mask
        if (keys[i] == 0L || keys[i] == TOMB) size++
        keys[i] = k
        vals[i] = v
    }

    fun remove(k: Long) {
        val mask = keys.size - 1
        var i = slot(k, mask)
        while (true) {
            val x = keys[i]
            if (x == 0L) return
            if (x == k) {
                keys[i] = TOMB
                return
            }
            i = (i + 1) and mask
        }
    }

    private fun grow() {
        val ok = keys
        val ov = vals
        keys = LongArray(ok.size * 2)
        vals = LongArray(ok.size * 2)
        size = 0
        for (i in ok.indices) if (ok[i] != 0L && ok[i] != TOMB) put(ok[i], ov[i])
    }
}
