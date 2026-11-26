package vn.mtv.tv

import java.net.URI
import java.net.URLDecoder
import java.text.Normalizer
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.ceil

/**
 * Truyền hình (IPTV) — các hàm đọc thuần (không chạm Android, có test JVM): dòng M3U, giờ trận trong tên kênh thể thao,
 * giờ XMLTV, tên chuẩn hoá. Cùng cách đọc với bản Legacy (CBIPTV.m) để danh sách hiện giống nhau trên mọi máy.
 */
object IptvParse {
    /** Kênh trận đấu thường lên sóng trước giờ bóng lăn chừng này (giây). */
    const val MATCH_EARLY = 30 * 60.0

    /** Một mục của danh sách M3U (một dòng #EXTINF + dòng địa chỉ). */
    class Entry(
        val name: String, val group: String, val url: String, val attrs: Map<String, String>,
        val ref: String?, val ua: String?,
    )

    enum class Match { NONE, LATER, SOON, LIVE, OVER }

    private fun keyChar(c: Char) = c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c == '_' || c == '-'

    /**
     * Thuộc tính `khoá="giá trị"` của dòng #EXTINF / #EXTM3U (khoá chữ thường) — như biểu thức `([A-Za-z0-9_-]+)="([^"]*)"`,
     * viết tay vì danh sách Xtream có vài trăm nghìn dòng #EXTINF.
     */
    fun attrs(s: String): Map<String, String> {
        val out = HashMap<String, String>(8)
        var i = s.indexOf("=\"")
        while (i > 0) {
            var k = i
            while (k > 0 && keyChar(s[k - 1])) k--
            val end = s.indexOf('"', i + 2)
            if (end < 0) break
            if (k < i) out[s.substring(k, i).lowercase()] = s.substring(i + 2, end)
            i = s.indexOf("=\"", end + 1)
        }
        return out
    }

    /** Tên kênh sau dấu phẩy đầu tiên nằm ngoài ngoặc kép của dòng #EXTINF. */
    fun extinfName(line: String): String? {
        var quoted = false
        for (i in line.indices) {
            val c = line[i]
            if (c == '"') quoted = !quoted
            else if (c == ',' && !quoted) return line.substring(i + 1).trim()
        }
        return null
    }

    /** Địa chỉ http đầu tiên trong chuỗi nhiều địa chỉ cách nhau bằng dấu phẩy (url-tvg="a.xml,b.xml.gz"). */
    fun firstUrl(s: String): String? =
        s.split(',').map { it.trim() }.firstOrNull { it.startsWith("http://") || it.startsWith("https://") }

    /**
     * Đọc danh sách M3U theo từng dòng (không giữ cả tệp trong bộ nhớ — danh sách Xtream có thể tới vài trăm nghìn mục):
     * #EXTINF (tên sau dấu phẩy ngoài ngoặc kép, không có thì tvg-name), #EXTGRP, #EXTVLCOPT http-referrer / http-user-agent,
     * «url|Referer=…&User-Agent=…» (kiểu Kodi). [epg] nhận địa chỉ lịch phát sóng ở dòng #EXTM3U.
     */
    class Reader(private val epg: (String) -> Unit, private val entry: (Entry) -> Unit) {
        private var attrs: Map<String, String> = emptyMap()
        private var name: String? = null
        private var group: String? = null
        private var ref: String? = null
        private var ua: String? = null
        private var first = true

        fun line(raw: String) {
            var l = raw.trim()
            if (first) {
                first = false
                l = l.removePrefix("\uFEFF")
            }
            if (l.isEmpty()) return
            when {
                l.startsWith("#EXTM3U") -> {
                    val h = attrs(l)
                    (h["url-tvg"] ?: h["x-tvg-url"] ?: h["tvg-url"])?.let(::firstUrl)?.let(epg)
                }
                l.startsWith("#EXTINF") -> {
                    val n = extinfName(l)
                    val head = if (n != null) l.substring(0, l.length - n.length) else l
                    attrs = attrs(head)
                    name = if (!n.isNullOrEmpty()) n else attrs["tvg-name"]
                    group = attrs["group-title"]
                    ref = null
                    ua = null
                }
                l.startsWith("#EXTGRP:") -> group = l.substring(8).trim()
                l.startsWith("#EXTVLCOPT:") -> {
                    val o = l.substring(11)
                    val eq = o.indexOf('=')
                    if (eq < 0) return
                    val k = o.substring(0, eq).lowercase()
                    val v = o.substring(eq + 1)
                    if (k == "http-referrer" || k == "http-referer") ref = v
                    if (k == "http-user-agent") ua = v
                }
                !l.startsWith("#") -> {
                    var url = l
                    val bar = url.indexOf('|')
                    if (bar >= 0) {
                        for (kv in url.substring(bar + 1).split('&')) {
                            val e = kv.indexOf('=')
                            if (e < 0) continue
                            val k = kv.substring(0, e).lowercase()
                            val v = runCatching { URLDecoder.decode(kv.substring(e + 1), "UTF-8") }.getOrDefault(kv.substring(e + 1))
                            if (k == "referer" || k == "referrer") ref = v
                            if (k == "user-agent") ua = v
                        }
                        url = url.substring(0, bar)
                    }
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        val n = name?.takeIf { it.isNotEmpty() } ?: lastSegment(url)
                        entry(Entry(n, group?.takeIf { it.isNotEmpty() } ?: "Khác", url, attrs, ref, ua))
                    }
                    attrs = emptyMap()
                    name = null
                    group = null
                    ref = null
                    ua = null
                }
            }
        }
    }

    private fun lastSegment(url: String): String =
        url.substringBefore('?').trimEnd('/').substringAfterLast('/').ifEmpty { url }

    /** Phần đường dẫn (chữ thường) của địa chỉ — tách bằng chuỗi (java.net.URI chậm với danh sách vài trăm nghìn mục). */
    fun pathOf(url: String): String {
        val s = url.indexOf("://").let { if (it < 0) 0 else it + 3 }
        val slash = url.indexOf('/', s)
        if (slash < 0) return "/"
        var end = url.length
        val q = url.indexOf('?', slash)
        if (q >= 0) end = q
        val h = url.indexOf('#', slash)
        if (h in 0 until end) end = h
        return url.substring(slash, end).lowercase()
    }

    fun extOf(url: String) = extOfPath(pathOf(url))

    fun extOfPath(path: String): String {
        val last = path.substring(path.lastIndexOf('/') + 1)
        val dot = last.lastIndexOf('.')
        return if (dot >= 0) last.substring(dot + 1) else ""
    }

    /** Luồng HTTP-FLV (kênh thể thao): xếp sau HLS. */
    fun isFlv(url: String) = extOf(url) == "flv"

    private val NO_SIGNAL = Regex("(?i)no[-_]?signal")

    /** Link giả «không có tín hiệu» mà danh sách thể thao để cho trận chưa có link thật (…/no-signal/low.m3u8: một ảnh 10 s). */
    fun isPlaceholder(url: String) = isPlaceholderPath(pathOf(url))

    fun isPlaceholderPath(path: String) = ("signal" in path) && NO_SIGNAL.containsMatchIn(path)

    /** Thứ tự nguồn của một kênh: HLS / khác → FLV → link giả. */
    fun sourceRank(url: String) = if (isPlaceholder(url)) 2 else if (isFlv(url)) 1 else 0

    private val VOD_EXT = setOf("mp4", "mkv", "avi", "mov", "m4v", "webm", "mpg", "mpeg", "wmv", "3gp")

    /** Phim / tập phim (VOD) của danh sách Xtream: tệp video, hoặc đường dẫn /movie/ /series/. Phát được tua. */
    fun isVod(url: String) = isVodPath(pathOf(url))

    fun isVodPath(p: String) = extOfPath(p) in VOD_EXT || "/movie/" in p || "/series/" in p

    /**
     * Đài radio: danh sách ghi `radio="true"` (kiểu Kodi / iptv-org), nhóm tên «Radio» / «Phát thanh» / «Truyền thanh», hoặc
     * link tệp tiếng / danh sách của đài (.mp3, .aac, .pls). Đài không đánh dấu (link không đuôi) trình phát vẫn nhận ra lúc mở.
     */
    fun isRadio(attrs: Map<String, String>, group: String, url: String) = isRadioNorm(attrs, norm(group), extOf(url))

    private val RADIO_EXT = setOf("mp3", "aac", "pls")

    /** Như [isRadio], tên nhóm đã chuẩn hoá ([norm]) và đuôi tệp đã tách — danh sách lớn đọc mỗi thứ một lần. */
    fun isRadioNorm(attrs: Map<String, String>, g: String, ext: String): Boolean {
        val r = attrs["radio"]?.lowercase()
        if (r == "true" || r == "1") return true
        if ("radio" in g || "phatthanh" in g || "truyenthanh" in g) return true
        return ext in RADIO_EXT
    }

    private val KIDS = arrayOf("thieunhi", "kid", "child", "cartoon", "hoathinh", "disney", "nick", "boomerang", "baby", "junior", "anime")

    /** Hồ sơ trẻ em: kênh nhóm thiếu nhi / hoạt hình. */
    fun isKids(group: String, key: String) = isKidsNorm(norm(group), key)

    fun isKidsGroup(groupNorm: String) = KIDS.any { it in groupNorm }

    fun isKidsNorm(groupNorm: String, key: String) = isKidsGroup(groupNorm) || KIDS.any { it in key }

    private val BRACKETS = Regex("""\([^)]*\)|\[[^\]]*\]""")
    /** Đuôi bỏ khỏi tên chuẩn hoá (lặp lại được: «hdfhd»); dài trước ngắn sau như phép so khớp sớm nhất của biểu thức cũ. */
    private val TAILS = arrayOf("backup", "1080p", "720p", "uhd", "fhd", "hd", "sd", "4k")

    /**
     * Tên chuẩn hoá để so / tìm: bỏ phần trong ngoặc, bỏ dấu, chữ thường, chỉ giữ chữ / số, bỏ «HD», «FHD»… ở cuối. Viết tay
     * (không biểu thức chính quy cho tên chỉ có chữ ASCII) — danh sách Xtream vài trăm nghìn mục đọc trên TV box yếu.
     */
    fun norm(name: String): String {
        if (name.isEmpty()) return ""
        var s = name
        if ('(' in s || '[' in s) {
            val bare = BRACKETS.replace(s, "")
            if (bare.isNotBlank()) s = bare
        }
        s = s.lowercase()
        if (s.any { it.code >= 128 }) s = Normalizer.normalize(s.replace('đ', 'd'), Normalizer.Form.NFD)
        val sb = StringBuilder(s.length)
        // Chỉ giữ chữ / số (dấu thanh, dấu mũ sau NFD là ký tự riêng — bỏ luôn)
        for (ch in s) if (Character.isLetterOrDigit(ch)) sb.append(ch)
        val t = sb.toString()
        var end = t.length
        var cut = true
        while (cut && end > 0) {
            cut = false
            for (x in TAILS) {
                if (end >= x.length && t.regionMatches(end - x.length, x, 0, x.length)) {
                    end -= x.length
                    cut = true
                    break
                }
            }
        }
        return if (end > 0) t.substring(0, end) else t
    }

    // ---- Giờ trận (danh sách thể thao: «20:00 16/11 ⚽ A vs B (BLV …) [hls]») ----

    private val TIME_FIRST =
        Regex("""(?<![\d:/])(\d{1,2})[:hH](\d{2})\s*[-,|·]?\s*(?:ngày\s*)?(\d{1,2})/(\d{1,2})(?:/(\d{2,4}))?(?![\d:/])""")
    private val DATE_FIRST =
        Regex("""(?<![\d:/])(\d{1,2})/(\d{1,2})(?:/(\d{2,4}))?\s*[-,|·]?\s*(\d{1,2})[:hH](\d{2})(?![\d:/])""")

    /** Chỉ tên kiểu trận đấu («A vs B», có BLV / biểu tượng môn) — «Updated 16/11 15:36» không phải trận. */
    private val MATCHY = Regex("""(?i)\bvs?\.?\s|\s[-–]\s|BLV|⚽|🏀|🎾|🥎|🏐|🏸|🏒|🥊|🏈|⚾""")

    class Kick(val at: Double, val range: IntRange)

    /**
     * Giờ trận ghi trong tên kênh → giây từ 1970. Giờ Việt Nam (UTC+7) — danh sách thể thao là của trang Việt; không ghi năm
     * thì lấy năm gần bây giờ nhất (qua giao thừa). null = tên không ghi giờ trận.
     */
    fun kickoff(name: String, now: Double): Kick? {
        if ('/' !in name) return null
        val tf = TIME_FIRST.find(name)
        val m = tf ?: DATE_FIRST.find(name) ?: return null
        fun g(i: Int) = m.groups[i]?.value?.toIntOrNull() ?: 0
        val h = if (tf != null) g(1) else g(4)
        val mi = if (tf != null) g(2) else g(5)
        val d = if (tf != null) g(3) else g(1)
        val mo = if (tf != null) g(4) else g(2)
        var y = if (tf != null) g(5) else g(3)
        if (h > 23 || mi > 59 || d < 1 || d > 31 || mo < 1 || mo > 12) return null
        if (y in 1..99) y += 2000
        val nowYear = Calendar.getInstance(TimeZone.getTimeZone("GMT+07:00")).apply { timeInMillis = (now * 1000).toLong() }
            .get(Calendar.YEAR)
        var best = Double.NaN
        val years = if (y != 0) y..y else nowYear - 1..nowYear + 1
        for (yy in years) {
            val t = daysFromCivil(yy.toLong(), mo, d) * 86400.0 + h * 3600 + mi * 60 - 7 * 3600
            if (best.isNaN() || abs(t - now) < abs(best - now)) best = t
        }
        return Kick(best, m.range)
    }

    /** Thời lượng trận (phút) theo môn ghi trong tên — để biết lúc nào «Đã xong». */
    fun matchMinutes(name: String): Int = when {
        "⚽" in name -> 130 // 90' + nghỉ giữa hiệp + bù giờ
        "🎾" in name || "🥎" in name -> 200
        else -> 150
    }

    /** Tên trận hiển thị + khoá gộp: bỏ chỗ ghi giờ và các thẻ «[hls]», «[FHD]»; giữ «(BLV …)» — mỗi BLV là một kênh. */
    fun matchName(name: String, kick: Kick): String {
        val n = name.replaceRange(kick.range, " ")
        val bare = oldEmoji(TAGS.replace(n, ""))
        return bare.ifEmpty { n.trim() }
    }

    private val TAGS = Regex("""\[[^\]]*]""")
    private val SPACES = Regex("\\s{2,}")
    private val PARENS = Regex("""\s*(\([^)]*\)|\[[^\]]*])""")

    fun isMatchName(name: String) = MATCHY.containsMatchIn(name)

    /** Emoji mới hơn máy cũ vẽ được (🟢 🥅…) thành ô vuông trên TV box Android 5–7: bỏ (🥎 bóng mềm → 🎾 quần vợt). */
    fun oldEmoji(input: String): String {
        var s = input.replace("🥎", "🎾")
        if (s.any { Character.isSurrogate(it) }) {
            val out = StringBuilder(s.length)
            var i = 0
            while (i < s.length) {
                val cp = s.codePointAt(i)
                val newer = cp >= 0x1F900 || cp in 0x1F7E0..0x1F7FF || cp in 0x1F3CF..0x1F3D3 ||
                    cp in 0x1F3F8..0x1F3FF || cp in 0x1F6D0..0x1F6FF
                if (!newer) out.appendCodePoint(cp)
                i += Character.charCount(cp)
            }
            s = out.toString()
        }
        return SPACES.replace(s, " ").trim()
    }

    private val COUNTRY_TAG = Regex("""^\|?[A-Z0-9]{2,5}\|\s*""")

    /**
     * Tên hiển thị của kênh thường: bỏ mã nước đầu tên của danh sách kiểu Xtream («|UK| SPORT 1 HD», «DE| ZDF» — tên ô kênh
     * khỏi cụt; Legacy 0.5.5) và phần trong ngoặc («Can Tho TV 2 (1080p) [Geo-blocked]» → «Can Tho TV 2»).
     */
    fun displayName(name: String): String {
        var n = name
        if ('|' in n) n = COUNTRY_TAG.replace(n, "").ifEmpty { n }
        return if ('(' !in n && '[' !in n) n.trim() else PARENS.replace(n, "").trim().ifEmpty { n }
    }

    fun matchState(kick: Double, dur: Int, now: Double): Match = when {
        kick <= 0 -> Match.NONE
        now < kick - MATCH_EARLY -> Match.LATER
        now < kick -> Match.SOON
        now < kick + dur * 60 -> Match.LIVE
        else -> Match.OVER
    }

    /** «20:00», «02:45 mai», «20:00 hôm qua», «20:00 18/11» (giờ máy). */
    fun kickText(kick: Double, now: Double, tz: TimeZone = TimeZone.getDefault()): String {
        val k = Calendar.getInstance(tz).apply { timeInMillis = (kick * 1000).toLong() }
        val n = Calendar.getInstance(tz).apply { timeInMillis = (now * 1000).toLong() }
        val days = (daysFromCivil(k.get(Calendar.YEAR).toLong(), k.get(Calendar.MONTH) + 1, k.get(Calendar.DAY_OF_MONTH)) -
            daysFromCivil(n.get(Calendar.YEAR).toLong(), n.get(Calendar.MONTH) + 1, n.get(Calendar.DAY_OF_MONTH))).toInt()
        val clock = "%02d:%02d".format(k.get(Calendar.HOUR_OF_DAY), k.get(Calendar.MINUTE))
        return when (days) {
            0 -> clock
            1 -> "$clock mai"
            -1 -> "$clock hôm qua"
            else -> "$clock ${k.get(Calendar.DAY_OF_MONTH)}/${k.get(Calendar.MONTH) + 1}"
        }
    }

    /** «Sắp đá 20:00 · còn 25 phút», «● Đang đá · bóng lăn 20:00», «Đã xong · đá lúc 20:00»; null = kênh thường. */
    fun matchLine(kick: Double, dur: Int, nolink: Boolean, now: Double, tz: TimeZone = TimeZone.getDefault()): String? {
        val whenText = kickText(kick, now, tz)
        return when (matchState(kick, dur, now)) {
            Match.NONE -> null
            Match.LATER -> {
                val m = ceil((kick - now) / 60).toLong()
                when {
                    m >= 24 * 60 -> "Sắp đá $whenText"
                    m >= 60 -> "Sắp đá $whenText · còn ${m / 60} giờ %02d phút".format(m % 60)
                    else -> "Sắp đá $whenText · còn $m phút"
                }
            }
            Match.SOON -> "Sắp đá $whenText · còn ${maxOf(1L, ceil((kick - now) / 60).toLong())} phút"
            Match.LIVE -> if (nolink) "● Đang đá · danh sách chưa có link" else "● Đang đá · bóng lăn $whenText"
            Match.OVER -> "Đã xong · đá lúc $whenText"
        }
    }

    /** Ngày (từ 1970-01-01) của một ngày dương lịch (thuật toán của Howard Hinnant). */
    fun daysFromCivil(year: Long, m: Int, d: Int): Long {
        val y = year - if (m <= 2) 1 else 0
        val era = (if (y >= 0) y else y - 399) / 400
        val yoe = y - era * 400
        val doy = (153 * (m + (if (m > 2) -3 else 9)) + 2) / 5 + d - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097 + doe - 719468
    }

    /** «20261116190000 +0700» → giây từ 1970 (UTC); null nếu sai dạng. */
    fun xmltvTime(s: String?): Double? {
        if (s == null || s.length < 12) return null
        val v = IntArray(6)
        val widths = intArrayOf(4, 2, 2, 2, 2, 2)
        var p = 0
        for (i in 0 until 6) {
            if (i == 5 && (p >= s.length || !s[p].isDigit())) break // không có giây
            repeat(widths[i]) {
                if (p >= s.length || !s[p].isDigit()) return null
                v[i] = v[i] * 10 + (s[p] - '0')
                p++
            }
        }
        var t = daysFromCivil(v[0].toLong(), v[1], v[2]) * 86400.0 + v[3] * 3600 + v[4] * 60 + v[5]
        while (p < s.length && s[p] == ' ') p++
        if (p + 4 < s.length && (s[p] == '+' || s[p] == '-') && (1..4).all { s[p + it].isDigit() }) {
            val off = ((s[p + 1] - '0') * 10 + (s[p + 2] - '0')) * 3600 + ((s[p + 3] - '0') * 10 + (s[p + 4] - '0')) * 60
            t -= if (s[p] == '+') off else -off
        }
        return t
    }

    /** «19:00» theo giờ máy. */
    fun clock(t: Double, tz: TimeZone = TimeZone.getDefault()): String {
        val c = Calendar.getInstance(tz).apply { timeInMillis = (t * 1000).toLong() }
        return "%02d:%02d".format(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
    }

    /** Danh sách phát là đoạn phim ngắn có hồi kết (hình chờ «không có tín hiệu» thay cho luồng trực tiếp). */
    fun isPlaceholderPlaylist(m3u8: String): Boolean {
        if ("#EXT-X-ENDLIST" !in m3u8 || "#EXT-X-STREAM-INF" in m3u8) return false
        var total = 0.0
        for (l in m3u8.lineSequence()) {
            if (l.startsWith("#EXTINF:")) total += l.substring(8).substringBefore(',').trim().toDoubleOrNull() ?: 0.0
        }
        return total < 120
    }

    private val STAMP = Regex("""[-_ ]*\d{4}-\d{2}-\d{2}([T_ ]?[\d.:-]*)?$""")

    /** Tên danh sách lấy từ tên tệp: «4k viptv-playlist---2026-11-11T131814-437.m3u» → «4k viptv-playlist». */
    fun nameFromFile(fileName: String): String {
        val n = STAMP.replace(fileName.substringAfterLast('/').substringBeforeLast('.'), "")
            .replace('_', ' ').trim(' ', '-', '.')
        return if (n.isEmpty()) "Danh sách từ tệp" else n.take(40)
    }

    /** Địa chỉ danh sách: thiếu `http(s)://` thì thêm `http://`. */
    fun listUrl(input: String): String {
        val u = input.trim()
        val l = u.lowercase()
        return if (l.startsWith("http://") || l.startsWith("https://")) u else "http://$u"
    }

    /**
     * Khoá so trùng địa chỉ danh sách: bỏ `http(s)://` và dấu `/` cuối, tên miền chữ thường (đường dẫn giữ nguyên — link rút gọn
     * phân biệt chữ hoa / thường).
     */
    fun listUrlKey(url: String): String {
        val u = url.trim()
        val l = u.lowercase()
        val rest = when {
            l.startsWith("https://") -> u.substring(8)
            l.startsWith("http://") -> u.substring(7)
            else -> u
        }
        val slash = rest.indexOf('/')
        val host = if (slash < 0) rest else rest.substring(0, slash)
        val path = if (slash < 0) "" else rest.substring(slash)
        return host.lowercase() + path.trimEnd('/')
    }

    /**
     * Tách nhiều địa chỉ danh sách dán một lần — mỗi dòng (hoặc mỗi chữ cách nhau) một địa chỉ. Bỏ dòng trống, dòng chú thích `#`,
     * chữ không giống địa chỉ (không có dấu chấm); thiếu `http(s)://` thì thêm; bỏ trùng (`listUrlKey`), giữ thứ tự.
     */
    fun splitUrls(text: String): List<String> {
        val out = mutableListOf<String>()
        val keys = HashSet<String>()
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            for (tok in line.split(Regex("""\s+"""))) {
                val t = tok.trim('"', '\'', '<', '>', ',', ';', '(', ')')
                if (t.length < 4 || '.' !in t || t.startsWith(".") || t.endsWith(".")) continue
                val u = listUrl(t)
                if (keys.add(listUrlKey(u))) out += u
            }
        }
        return out
    }

    /** Tên danh sách lấy từ địa chỉ: tên tệp (bỏ đuôi), không có thì tên máy chủ. */
    fun nameFromUrl(url: String): String {
        val u = runCatching { URI(url) }.getOrNull()
        val file = (u?.path ?: "").trimEnd('/').substringAfterLast('/').substringBeforeLast('.').replace('_', ' ')
        return if (file.length > 2) file else u?.host ?: "Danh sách kênh"
    }
}
