package vn.mtv.tv

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * dexter.pw («Stream», từ 2.19): phim Âu Mỹ (khoảng 6 400 phim lẻ, nhiều phim 4K) và phim bộ (Dexter và hơn 1 000 bộ khác).
 * Ứng dụng React có API JSON, mọi mã phim là mã TMDB:
 *  - /api/site/dexter?v=8: trang chủ (hero + shelves); /api/site/dexter/series: trang phim bộ (cùng dạng).
 *  - /api/browse?type=movie&page=N&site=dexter[&genre=<mã thể loại TMDB>][&sort=added][&decade=1990]: 40 phim / trang.
 *  - /api/search?q=…&type=all&page=1&site=dexter.
 *  - /api/movie/<id>?site=dexter: chi tiết + sources; /api/tv/<id>?site=dexter: chi tiết + seasonList;
 *    /api/tv/<id>/season/<n>?site=dexter: tập của mùa; /api/tv/<id>/episode/<mùa>/<tập>?site=dexter: sources của tập.
 *  - sources: [{kind:"hls", url:"/api/s/21/movie/<id>?v=3", label:"Server 21", family:21}] → danh sách HLS chính
 *    (4K / 1080p / 720p / 480p, fMP4, đoạn video đổi đuôi .png / .gif / .webp — hls.js không quan tâm đuôi). Máy chủ 20 của
 *    phim bộ tách âm thanh ra luồng riêng → phát bằng bản hls.js đầy đủ (Video.hls).
 *  - /api/subs/list/movie/<id> | /api/subs/list/tv/<id>/<mùa>/<tập>: phụ đề OpenSubtitles + Subsource
 *    ({files:[{id, lang, label, source, release}]}; phim bộ thường trả {pending:true} một lúc rồi mới có),
 *    tệp: /api/subs/file/movie/<id>/<mã tệp>.vtt (vài tệp trả 404 «No such subtitle» → trình phát thử tệp sau).
 * Luồng và phụ đề chỉ trả cho User-Agent kiểu trình duyệt (UA "okhttp" bị 403) — UA của app và WebView đều qua.
 *
 * Địa chỉ trong app theo đường dẫn của chính trang (mở web vẫn đúng chỗ): phim = /movie/<id>-<slug>, phim bộ =
 * /show/<id>-<slug>, tập phim lẻ = /watch/<id>-<slug>, tập phim bộ = /watch/show/<id>-<slug>/<mùa>/<tập>.
 * Danh sách = địa chỉ API.
 */
object Dexter {
    const val HOST = "dexter.pw"
    private const val SITE = "dexter"
    private const val IMG = "https://image.tmdb.org/t/p/"

    /** Ngôn ngữ phụ đề rời app cho chọn (người xem Việt): tiếng Việt trước, tiếng Anh sau. */
    private val SUB_LANGS = linkedMapOf("vi" to "Tiếng Việt", "en" to "Tiếng Anh")
    private val SUB_MAX = mapOf("vi" to 12, "en" to 4)

    /** Thể loại TMDB (phim lẻ + phim bộ) → tên tiếng Việt. */
    private val GENRE_VI = mapOf(
        28 to "Hành động", 12 to "Phiêu lưu", 16 to "Hoạt hình", 35 to "Hài", 80 to "Hình sự", 99 to "Tài liệu",
        18 to "Chính kịch", 10751 to "Gia đình", 14 to "Giả tưởng", 36 to "Lịch sử", 27 to "Kinh dị", 10402 to "Âm nhạc",
        9648 to "Bí ẩn", 10749 to "Lãng mạn", 878 to "Khoa học viễn tưởng", 10770 to "Phim truyền hình", 53 to "Gay cấn",
        10752 to "Chiến tranh", 37 to "Miền Tây", 10759 to "Hành động & phiêu lưu", 10762 to "Thiếu nhi", 10763 to "Tin tức",
        10764 to "Truyền hình thực tế", 10765 to "Viễn tưởng & giả tưởng", 10766 to "Phim dài tập", 10767 to "Talk show",
        10768 to "Chiến tranh & chính trị",
    )
    private val GENRE_NAME_VI = mapOf(
        "Action" to 28, "Adventure" to 12, "Animation" to 16, "Comedy" to 35, "Crime" to 80, "Documentary" to 99,
        "Drama" to 18, "Family" to 10751, "Fantasy" to 14, "History" to 36, "Horror" to 27, "Music" to 10402,
        "Mystery" to 9648, "Romance" to 10749, "Science Fiction" to 878, "TV Movie" to 10770, "Thriller" to 53, "War" to 10752,
        "Western" to 37, "Action & Adventure" to 10759, "Kids" to 10762, "News" to 10763, "Reality" to 10764,
        "Sci-Fi & Fantasy" to 10765, "Soap" to 10766, "Talk" to 10767, "War & Politics" to 10768,
    )

    /** Thể loại cho mục «Thể loại…» (phim lẻ — browse chỉ lọc phim lẻ). */
    private val FILTER_GENRES = listOf(28, 12, 16, 35, 80, 99, 18, 10751, 14, 36, 27, 10402, 9648, 10749, 878, 53, 10752, 37)

    /** Mục «Thập niên…» (thay cho «Quốc gia…» — trang không lọc theo nước). */
    private val DECADES = listOf(2020, 2010, 2000, 1990, 1980, 1970, 1960, 1950)

    /** Tên khối trang chủ của trang (tiếng Anh) → tiếng Việt; khối thể loại đọc theo mã (dexter-genre-<mã>). */
    private val SHELF_VI = mapOf(
        "Recently Added" to "Phim mới thêm", "Series" to "Phim bộ", "Top Rated" to "Điểm cao", "Classics" to "Kinh điển",
        "Most Liked on Stream" to "Được thích nhất", "Top 10 Films on Stream" to "Top 10 phim lẻ",
    )

    fun isSite(url: String) = Site.hostOf(url) == HOST

    // ---- Trang chủ / danh sách ----

    fun home(base: String): Home {
        val o = ApiSite.origin(base)
        val sections = parseShelves(get(siteApi(o)), o)
        if (sections.isEmpty()) throw IOException("Không đọc được trang chủ của $HOST")
        return Home(sections, menu(base), null)
    }

    private fun siteApi(o: String) = "$o/api/site/$SITE?v=8"
    private fun seriesApi(o: String) = "$o/api/site/$SITE/series"
    private fun browse(o: String, query: String, page: Int = 1) =
        "$o/api/browse?type=movie" + (if (query.isEmpty()) "" else "&$query") + "&page=$page&site=$SITE"

    /** Trang chủ / trang phim bộ: hero thành «Nổi bật», mỗi shelf một khối (shelf bộ sưu tập không có phim thì bỏ). */
    fun parseShelves(body: String, o: String): List<Section> {
        val root = JSONObject(body)
        val out = ArrayList<Section>()
        films(root.optJSONArray("hero"), o).takeIf { it.isNotEmpty() }?.let { out += Section("Nổi bật", it) }
        for (s in objects(root.optJSONArray("shelves"))) {
            val f = films(s.optJSONArray("items"), o)
            if (f.isNotEmpty()) out += Section(shelfTitle(s), f)
        }
        return out
    }

    private fun shelfTitle(s: JSONObject): String {
        val title = str(s, "title")
        Regex("""genre-(\d+)$""").find(str(s, "id"))?.groupValues?.get(1)?.toIntOrNull()?.let { GENRE_VI[it] }?.let { return it }
        return SHELF_VI[title] ?: title
    }

    fun menu(base: String): List<Category> {
        val o = ApiSite.origin(base)
        return listOf(
            Category("Phim mới & nổi bật", browse(o, "")),
            Category("Phim mới thêm", browse(o, "sort=added")),
            Category("Phim bộ", seriesApi(o)),
            Category("Hoạt hình", browse(o, "genre=16")),
            Category("Gia đình", browse(o, "genre=10751")),
            Category("Hành động", browse(o, "genre=28")),
            Category("Khoa học viễn tưởng", browse(o, "genre=878")),
            Category("Hài", browse(o, "genre=35")),
            Category("Kinh dị", browse(o, "genre=27")),
        )
    }

    /** Thể loại (phim lẻ) và thập niên. */
    fun filters(base: String): Pair<List<Category>, List<Category>> {
        val o = ApiSite.origin(base)
        return FILTER_GENRES.map { Category(GENRE_VI.getValue(it), browse(o, "genre=$it")) } to
            DECADES.map { Category("Thập niên $it", browse(o, "decade=$it")) }
    }

    fun newestUrl(base: String) = browse(ApiSite.origin(base), "sort=added")

    fun searchUrl(base: String, query: String): String =
        "${ApiSite.origin(base)}/api/search?q=" + URLEncoder.encode(query.trim(), "UTF-8") + "&type=all&page=1&site=$SITE"

    fun list(url: String): FilmPage {
        val body = get(url)
        // Trang phim bộ: các khối như trang chủ
        if (URI(url).path.startsWith("/api/site/")) return FilmPage(emptyList(), null, parseShelves(body, ApiSite.origin(url)))
        return parseList(body, url)
    }

    /** {page, totalPages, results:[…]} (browse / search) → trang sau = page + 1. */
    fun parseList(body: String, url: String): FilmPage {
        val root = JSONObject(body)
        val films = films(root.optJSONArray("results"), ApiSite.origin(url))
        val page = root.optInt("page", 1)
        val next = if (films.isNotEmpty() && page < root.optInt("totalPages", 0)) ApiSite.withPage(url, page + 1) else null
        return FilmPage(films, next)
    }

    private fun films(a: JSONArray?, o: String): List<Film> = objects(a).mapNotNull { film(it, o) }.distinctBy { it.url }

    /** Thẻ phim: phim bộ ghi số mùa, phim lẻ ghi năm. Phim trang chưa phát được (playable=false) thì bỏ. */
    private fun film(it: JSONObject, o: String): Film? {
        if (it.has("playable") && !it.optBoolean("playable")) return null
        val id = it.optLong("id").takeIf { id -> id > 0 } ?: return null
        val title = str(it, "title").ifEmpty { return null }
        val tv = str(it, "type") == "tv"
        val badge = if (tv) it.optInt("seasons").takeIf { n -> n > 0 }?.let { n -> "Phim bộ · $n mùa" } ?: "Phim bộ"
        else it.optInt("year").takeIf { y -> y > 0 }?.toString()
        return Film(filmUrl(o, tv, id, title), title, image(str(it, "poster"), "w342"), badge)
    }

    fun filmUrl(o: String, tv: Boolean, id: Long, title: String) = "$o/${if (tv) "show" else "movie"}/$id-${slug(title)}"

    /** "I'm Not Ashamed" → "i-m-not-ashamed" (như đường dẫn của trang; trang chỉ đọc mã số đứng trước). */
    fun slug(title: String): String =
        Normalizer.normalize(title, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").replace('đ', 'd').replace('Đ', 'D')
            .lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "-").trim('-').ifEmpty { "phim" }

    // ---- Chi tiết phim ----

    /** Phim / phim bộ trong địa chỉ của app: (phim bộ?, mã TMDB, slug). */
    private fun ref(url: String): Triple<Boolean, Long, String> {
        val p = URI(url).path
        Regex("""^/(?:watch/)?(show|movie)/(\d+)(?:-([^/]*))?""").find(p)?.let {
            return Triple(it.groupValues[1] == "show", it.groupValues[2].toLong(), it.groupValues[3])
        }
        Regex("""^/watch/(\d+)(?:-([^/]*))?""").find(p)?.let { return Triple(false, it.groupValues[1].toLong(), it.groupValues[2]) }
        throw IOException("Địa chỉ phim lạ: $url")
    }

    fun detail(url: String): FilmDetail {
        val o = ApiSite.origin(url)
        val (tv, id, _) = ref(url)
        if (!tv) return parseMovie(get("$o/api/movie/$id?site=$SITE"), o)
        val show = JSONObject(get("$o/api/tv/$id?site=$SITE"))
        val seasons = objects(show.optJSONArray("seasonList"))
            .filter { !it.has("playable") || it.optBoolean("playable") }
            .map { it.optInt("number") }
            .sortedWith(compareBy({ it == 0 }, { it })) // mùa đặc biệt (0) xuống cuối
        // Các mùa tải song song (tối đa 6 cùng lúc) — phim bộ dài có hàng chục mùa
        val pool = Executors.newFixedThreadPool(6)
        val bodies = try {
            pool.invokeAll(seasons.map { n -> Callable { runCatching { get("$o/api/tv/$id/season/$n?site=$SITE") }.getOrNull() } }, 60, TimeUnit.SECONDS)
                .map { f -> runCatching { f.get() }.getOrNull() }
        } finally {
            pool.shutdownNow()
        }
        return parseShow(show, seasons.zip(bodies).mapNotNull { (n, b) -> b?.let { n to it } }, o)
    }

    fun parseMovie(body: String, o: String): FilmDetail {
        val m = JSONObject(body)
        val id = m.optLong("id").takeIf { it > 0 } ?: throw IOException("Không đọc được thông tin phim")
        val title = str(m, "title")
        val info = listOf(
            m.optInt("year").takeIf { it > 0 }?.toString().orEmpty(),
            m.optInt("runtime").takeIf { it > 0 }?.let { "$it phút" }.orEmpty(),
            str(m, "certification"),
            genres(m),
            rating(m),
        ).filter { it.isNotEmpty() }.joinToString("  •  ").ifEmpty { null }
        val url = filmUrl(o, false, id, title)
        val playable = !m.has("playable") || m.optBoolean("playable")
        val groups = if (playable) listOf(EpisodeGroup("Phim", listOf(Episode("Xem phim", "$o/watch/$id-${slug(title)}")))) else emptyList()
        return FilmDetail(url, title.ifEmpty { url }, str(m, "tagline").ifEmpty { null }, image(str(m, "poster"), "w500"),
            str(m, "overview").ifEmpty { null }, info, groups)
    }

    /** seasons: (số mùa, JSON của mùa) — mỗi mùa một nhóm tập («Mùa 1»…); tập trang chưa phát được thì bỏ. */
    fun parseShow(show: JSONObject, seasons: List<Pair<Int, String>>, o: String): FilmDetail {
        val id = show.optLong("id").takeIf { it > 0 } ?: throw IOException("Không đọc được thông tin phim")
        val title = str(show, "title")
        val s = slug(title)
        val groups = seasons.mapNotNull { (n, body) ->
            val eps = objects(JSONObject(body).optJSONArray("episodes")).mapNotNull { e ->
                if (e.has("playable") && !e.optBoolean("playable")) return@mapNotNull null
                val ep = e.optInt("episode").takeIf { it > 0 } ?: return@mapNotNull null
                val name = str(e, "title").takeIf { it.isNotEmpty() && !it.matches(Regex("""(?i)episode\s*\d+""")) }
                Episode("Tập $ep" + (name?.let { " · $it" } ?: ""), "$o/watch/show/$id-$s/$n/$ep")
            }
            if (eps.isEmpty()) null else EpisodeGroup(if (n == 0) FilmDetail.SPECIALS else "Mùa $n", eps)
        }
        val years = listOf(show.optInt("year"), show.optInt("endYear")).filter { it > 0 }.distinct().joinToString("–")
        val info = listOf(
            years,
            show.optInt("seasons").takeIf { it > 0 }?.let { "$it mùa" }.orEmpty(),
            show.optInt("episodeCount").takeIf { it > 0 }?.let { "$it tập" }.orEmpty(),
            str(show, "certification"),
            when (str(show, "status")) { "Ended" -> "Đã kết thúc"; "Returning Series" -> "Đang chiếu"; else -> "" },
            genres(show),
            rating(show),
        ).filter { it.isNotEmpty() }.joinToString("  •  ").ifEmpty { null }
        return FilmDetail(filmUrl(o, true, id, title), title.ifEmpty { "Phim bộ" }, str(show, "tagline").ifEmpty { null },
            image(str(show, "poster"), "w500"), str(show, "overview").ifEmpty { null }, info, groups, seasons = true)
    }

    private fun genres(o: JSONObject): String {
        val a = o.optJSONArray("genres") ?: return ""
        return (0 until a.length()).mapNotNull { a.optString(it).takeIf { g -> g.isNotEmpty() } }
            .map { g -> GENRE_NAME_VI[g]?.let { GENRE_VI[it] } ?: g }.joinToString(", ")
    }

    private fun rating(o: JSONObject): String =
        o.optDouble("rating", 0.0).takeIf { it > 0 && o.optInt("votes") >= 20 }?.let { "TMDB " + String.format(Locale.ROOT, "%.1f", it) }.orEmpty()

    // ---- Tập phim ----

    /**
     * Server = mỗi nguồn HLS của trang (phim lẻ: «Server 21»; phim bộ: thêm «Server 20»). Kèm danh sách phụ đề tải song
     * song (tiếng Việt trước); phim bộ hay trả «đang tìm» (pending) → trình phát hỏi lại sau (Video.subsLater).
     */
    fun source(epUrl: String): EpisodeSource {
        val o = ApiSite.origin(epUrl)
        val p = URI(epUrl).path
        val tvEp = Regex("""^/watch/show/(\d+)[^/]*/(\d+)/(\d+)""").find(p)?.groupValues
        val key = if (tvEp != null) "tv/${tvEp[1]}/${tvEp[2]}/${tvEp[3]}" else "movie/${ref(epUrl).second}"
        val api = if (tvEp != null) "$o/api/tv/${tvEp[1]}/episode/${tvEp[2]}/${tvEp[3]}?site=$SITE" else "$o/api/movie/${ref(epUrl).second}?site=$SITE"
        val subsList = "$o/api/subs/list/$key"
        var subs: Pair<List<SubTrack>, Boolean>? = null
        val t = Thread { subs = runCatching { subtitles(get(subsList), "$o/api/subs/file/$key/") }.getOrNull() }.apply { start() }
        val body = get(api)
        t.join(8000)
        val (list, pending) = subs ?: (emptyList<SubTrack>() to true)
        return parseSource(body, epUrl, list, if (pending) subsList else null)
    }

    /** sources của phim / tập → server + link (HLS, kèm phụ đề). Nguồn «sealed» (mã hoá riêng của trang) bỏ qua. */
    fun parseSource(body: String, epUrl: String, subs: List<SubTrack>, subsLater: String?): EpisodeSource {
        val o = ApiSite.origin(epUrl)
        val root = JSONObject(body)
        val links = LinkedHashMap<String, Video>()
        val servers = ArrayList<Server>()
        for (s in objects(root.optJSONArray("sources"))) {
            if (str(s, "kind") != "hls" || s.optBoolean("sealed")) continue
            val url = abs(o, str(s, "url")) ?: continue
            val type = s.optInt("family").takeIf { it > 0 }?.let { "s$it" } ?: str(s, "id").ifEmpty { "s${servers.size + 1}" }
            if (type in links) continue
            // Phụ đề riêng của nguồn (đã khớp thời gian với video) đứng trước phụ đề tìm theo tên phim
            val own = ownSubtitles(s.optJSONArray("subtitles"), o)
            links[type] = Video(url, hls = true, subs = mergeSubs(own, subs), subsLater = subsLater)
            servers += Server(str(s, "label").ifEmpty { "Server ${servers.size + 1}" }, type)
        }
        if (links.isEmpty()) throw IOException("Phim này chưa có video trên $HOST")
        return EpisodeSource(epUrl, servers, "", "", "", null, null, false, links, next = true)
    }

    /** Hỏi lại danh sách phụ đề còn «đang tìm»: (phụ đề, còn đang tìm?). */
    fun subtitlesFrom(listUrl: String): Pair<List<SubTrack>, Boolean> {
        val key = listUrl.substringAfter("/api/subs/list/")
        return subtitles(get(listUrl), "${ApiSite.origin(listUrl)}/api/subs/file/$key/")
    }

    /**
     * {files:[{id, lang, label, source, release, url?}], pending?} → phụ đề tiếng Việt (tối đa 12) rồi tiếng Anh (tối đa 4),
     * giữ thứ tự của trang (xếp theo độ phổ biến). fileBase: …/api/subs/file/movie/<id>/
     */
    fun subtitles(body: String, fileBase: String): Pair<List<SubTrack>, Boolean> {
        val root = JSONObject(body)
        val files = objects(root.optJSONArray("files"))
        val out = ArrayList<SubTrack>()
        for ((lang, name) in SUB_LANGS) {
            val mine = files.filter { str(it, "lang").lowercase(Locale.ROOT).let { l -> l == lang || l.startsWith("$lang-") } }
                .take(SUB_MAX[lang] ?: 4)
            mine.forEachIndexed { i, f ->
                val url = str(f, "url").takeIf { it.isNotEmpty() }?.let { abs(ApiSite.origin(fileBase), it) }
                    ?: (fileBase + URLEncoder.encode(str(f, "id"), "UTF-8").replace("+", "%20") + ".vtt")
                out += SubTrack(if (mine.size > 1) "$name ${i + 1}" else name, url, lang)
            }
        }
        return out to (files.isEmpty() && root.optBoolean("pending"))
    }

    private fun ownSubtitles(a: JSONArray?, o: String): List<SubTrack> = objects(a).mapNotNull { s ->
        val lang = str(s, "lang").lowercase(Locale.ROOT).substringBefore('-')
        val name = SUB_LANGS[lang] ?: return@mapNotNull null
        val url = abs(o, str(s, "url")) ?: return@mapNotNull null
        SubTrack("$name (khớp video)", url, lang)
    }

    /** Phụ đề riêng của nguồn trước, mỗi ngôn ngữ vẫn theo thứ tự tiếng Việt → tiếng Anh. */
    private fun mergeSubs(own: List<SubTrack>, found: List<SubTrack>): List<SubTrack> =
        if (own.isEmpty()) found else SUB_LANGS.keys.flatMap { l -> own.filter { it.lang == l } + found.filter { it.lang == l } }

    // ---- Tiện ích ----

    private fun get(url: String): String {
        val body = Http.get(url, mapOf("Accept" to "application/json")).body
        val o = runCatching { JSONObject(body) }.getOrNull() ?: throw IOException("$HOST trả dữ liệu lạ")
        if (o.length() == 1 && o.has("error")) throw IOException(o.optString("error"))
        return body
    }

    private fun image(path: String, size: String): String? = when {
        path.isEmpty() -> null
        path.startsWith("http") -> path
        else -> IMG + size + (if (path.startsWith("/")) path else "/$path")
    }

    private fun abs(o: String, ref: String): String? = when {
        ref.isEmpty() -> null
        ref.startsWith("http") -> ref
        ref.startsWith("/") -> o + ref
        else -> null
    }

    private fun objects(a: JSONArray?): List<JSONObject> =
        if (a == null) emptyList() else (0 until a.length()).mapNotNull { a.optJSONObject(it) }

    private fun str(o: JSONObject, key: String): String = (o.opt(key) as? String)?.trim().orEmpty()
}
