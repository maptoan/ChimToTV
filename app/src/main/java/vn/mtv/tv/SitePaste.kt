package vn.mtv.tv

import java.net.URL

/**
 * Dán một lần nhiều trang phim (Cài đặt → Danh sách trang phim, «Đổi trang phim» → «Thêm trang khác…»): tách, chuẩn hoá, bỏ trùng.
 * Thuần (không đụng SharedPreferences) để test JVM; thêm vào danh sách là [Prefs.addSites].
 */
object SitePaste {
    /** Kết quả tách: gốc trang theo thứ tự dán (đã bỏ trùng), số chữ có dấu chấm mà không đọc được thành địa chỉ. */
    class Split(val sites: List<String>, val invalid: Int)

    /** Kết quả thêm: trang mới theo thứ tự dán, số trang đã có trong danh sách, số chữ không hợp lệ. */
    class Added(val added: List<String>, val existing: Int, val invalid: Int)

    private val WRAP = charArrayOf('"', '\'', '<', '>', ',', ';', '(', ')', '[', ']')
    private val WRAP_END = WRAP + '.'

    /**
     * Tách theo dòng và chữ cách nhau; bỏ dòng trống, dòng `#`, chữ không có dấu chấm, số bản kiểu «1.5» / «2.21»; bỏ ký tự
     * bao quanh (và dấu chấm cuối câu); chuẩn hoá bằng [Prefs.normalizeUrl] (thiếu scheme thì https://, kkphim.com → trang KKPhim
     * mẫu) rồi rút về gốc trang scheme://host[:port]; bỏ trùng theo [key]. Cùng cách với lõi Rust `prefs::split_sites` (Windows /
     * Linux) và `Prefs.splitSites` (iPad).
     */
    fun split(text: String): Split {
        val out = mutableListOf<String>()
        val keys = HashSet<String>()
        var invalid = 0
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            for (tok in line.split(Regex("""\s+"""))) {
                val t = tok.trimStart(*WRAP).trimEnd(*WRAP_END)
                if (t.isEmpty() || '.' !in t || t.startsWith('.')) continue
                // Số bản kiểu «1.5», «2.21», «1.2.3» — không phải trang (địa chỉ IP đủ 4 phần vẫn nhận)
                if (t.all { it.isDigit() || it == '.' } && t.count { it == '.' } < 3) continue
                // Chữ viết tắt trong câu («v.v.», «e.g.»): đuôi một chữ cái — không phải trang, bỏ qua không báo
                if (!plausibleHost(t)) continue
                val site = Prefs.normalizeUrl(t)?.let(::origin)
                if (site == null) {
                    invalid++
                    continue
                }
                if (keys.add(key(site))) out += site
            }
        }
        return Split(out, invalid)
    }

    /**
     * Đuôi tên miền đọc được: từ 2 chữ cái trở lên (hoặc «xn--…»), hay địa chỉ IPv4. «v.v», «e.g», «tr.5» (chữ viết tắt trong câu)
     * → false. Cùng luật với lõi Rust `prefs::plausible_host`, Windows `Prefs.PlausibleHost`, iPad `Prefs.plausibleHost`.
     */
    fun plausibleHost(t: String): Boolean {
        val host = t.substringAfter("://").substringBefore('/').substringBefore('?').substringBefore('#')
            .substringAfterLast('@').substringBefore(':')
        if (host.isNotEmpty() && host.all { it.isDigit() || it == '.' }) return true
        val tld = host.substringAfterLast('.')
        return tld.startsWith("xn--", ignoreCase = true) || (tld.length >= 2 && tld.all { it.isLetter() })
    }

    /** Gốc trang: scheme://host[:port], scheme và tên máy chủ chữ thường; không đọc được hoặc tên sai («bad..host») thì null. */
    fun origin(url: String): String? = try {
        val u = URL(url)
        if (u.host.isNullOrBlank() || u.host.split('.').any { it.isEmpty() }) null
        else "${u.protocol.lowercase()}://${u.host.lowercase()}" + if (u.port != -1) ":${u.port}" else ""
    } catch (e: Exception) {
        null
    }

    /**
     * Khoá so trùng trang: tên máy chủ chữ thường (kèm cổng nếu có), bỏ «www.» đầu — không phân biệt http / https, dấu / cuối,
     * đường dẫn; thiếu scheme thì coi như https://.
     */
    fun key(url: String): String {
        val s = url.trim()
        val full = if (s.startsWith("http://", true) || s.startsWith("https://", true)) s else "https://$s"
        val host = try {
            val u = URL(full)
            if (u.host.isNullOrBlank()) null else u.host.lowercase() + if (u.port != -1) ":${u.port}" else ""
        } catch (e: Exception) {
            null
        } ?: s.lowercase().trimEnd('/')
        return host.removePrefix("www.")
    }

    /** Tên ngắn để báo: tên máy chủ (bỏ www.). */
    private fun name(url: String): String = Site.hostOf(url) ?: url

    /** Câu báo khi đã thêm từ hai trang trở lên: «Đã thêm 3 trang: a, b, c (bỏ qua 1 đã có).» — dài thì rút gọn. */
    fun addedText(r: Added): String {
        val names = r.added.map(::name)
        val shown = if (names.size > 4) names.take(3).joinToString(", ") + "… (+${names.size - 3})" else names.joinToString(", ")
        val skip = listOfNotNull(
            "bỏ qua ${r.existing} đã có".takeIf { r.existing > 0 },
            "${r.invalid} tên miền không hợp lệ".takeIf { r.invalid > 0 },
        )
        return "Đã thêm ${r.added.size} trang: $shown" + (if (skip.isEmpty()) "" else " (${skip.joinToString("; ")})") + "."
    }

    /** Câu báo khi không có trang mới nào (hộp vẫn mở để sửa). */
    fun noneText(existing: Int): String = when (existing) {
        0 -> "Chưa có tên miền hợp lệ (vd ten-trang.example)."
        1 -> "Trang này đã có trong danh sách."
        else -> "Các trang này đã có trong danh sách."
    }

    /** Gợi ý trong ô nhập nhiều dòng. */
    const val HINT = "Tên miền, vd ten-trang.example — dán được nhiều trang, mỗi dòng một trang"
}
