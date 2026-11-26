package vn.mtv.tv

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** dexter.pw (11/2026): mẫu rút gọn từ phản hồi thật của API. */
class DexterTest {
    private val o = "https://dexter.pw"

    @Test
    fun parsesHomeShelves() {
        val json = """{"id":"dexter","name":"Stream",
            "hero":[{"type":"tv","id":1405,"title":"Dexter","year":2006,"poster":"/i8OR.jpg","seasons":8,"playable":true},
                    {"type":"movie","id":354912,"title":"Coco","year":2017,"poster":"/6Ryi.jpg","playable":true}],
            "shelves":[
              {"id":"dexter-new","title":"Recently Added","kind":"landscape","items":[
                 {"type":"movie","id":409502,"title":"I'm Not Ashamed","year":2016,"poster":"/a.jpg","playable":true},
                 {"type":"movie","id":7,"title":"Chưa có video","year":2020,"poster":"/b.jpg","playable":false}]},
              {"id":"dexter-film-series","title":"Film Series","kind":"collections","items":[],"collections":[{"id":1}]},
              {"id":"dexter-genre-878","title":"Science Fiction","kind":"landscape","items":[
                 {"type":"movie","id":687163,"title":"Project Hail Mary","year":2026,"poster":"/c.jpg","playable":true}]},
              {"id":"dexter-other","title":"Something New","items":[{"type":"movie","id":5,"title":"X","poster":null}]}]}"""
        val s = Dexter.parseShelves(json, o)
        assertEquals(listOf("Nổi bật", "Phim mới thêm", "Khoa học viễn tưởng", "Something New"), s.map { it.title })
        assertEquals(listOf("$o/show/1405-dexter", "$o/movie/354912-coco"), s[0].films.map { it.url })
        assertEquals(listOf("Phim bộ · 8 mùa", "2017"), s[0].films.map { it.badge })
        assertEquals("https://image.tmdb.org/t/p/w342/6Ryi.jpg", s[0].films[1].poster)
        assertEquals(listOf("$o/movie/409502-i-m-not-ashamed"), s[1].films.map { it.url }) // phim chưa phát được bị bỏ
        assertNull(s[3].films[0].poster)
    }

    @Test
    fun parsesBrowseAndSearchPages() {
        val json = """{"page":1,"totalPages":161,"totalResults":6416,"results":[
            {"type":"movie","id":687163,"title":"Project Hail Mary","year":2026,"poster":"/c.jpg","playable":true}]}"""
        val url = "$o/api/browse?type=movie&genre=16&page=1&site=dexter"
        val p = Dexter.parseList(json, url)
        assertEquals(listOf("$o/movie/687163-project-hail-mary"), p.films.map { it.url })
        assertEquals("$o/api/browse?type=movie&genre=16&page=2&site=dexter", p.nextUrl)
        assertNull(Dexter.parseList("""{"page":3,"totalPages":3,"results":[{"type":"movie","id":1,"title":"A"}]}""", url).nextUrl)
        assertEquals("$o/api/search?q=frozen+ii&type=all&page=1&site=dexter", Dexter.searchUrl(o, " frozen ii "))
        val (genres, decades) = Dexter.filters(o)
        assertTrue(genres.any { it.title == "Hoạt hình" && it.url == "$o/api/browse?type=movie&genre=16&page=1&site=dexter" })
        assertEquals("$o/api/browse?type=movie&decade=1990&page=1&site=dexter", decades.first { it.title == "Thập niên 1990" }.url)
    }

    @Test
    fun parsesMovieDetail() {
        val json = """{"type":"movie","id":354912,"title":"Coco","year":2017,"overview":"Despite his family's ban on music…",
            "poster":"/6Ryi.jpg","genres":["Family","Animation","Music"],"rating":8.2,"votes":21873,"runtime":105,
            "certification":"PG","tagline":"The celebration of a lifetime.","sources":[],"playable":true}"""
        val d = Dexter.parseMovie(json, o)
        assertEquals("$o/movie/354912-coco", d.url)
        assertEquals("Coco", d.title)
        assertEquals("2017  •  105 phút  •  PG  •  Gia đình, Hoạt hình, Âm nhạc  •  TMDB 8.2", d.info)
        assertEquals("https://image.tmdb.org/t/p/w500/6Ryi.jpg", d.poster)
        assertEquals(listOf("$o/watch/354912-coco"), d.groups.single().episodes.map { it.url })
    }

    @Test
    fun parsesShowSeasons() {
        val show = JSONObject("""{"type":"tv","id":1405,"title":"Dexter","year":2006,"endYear":2013,"seasons":8,
            "episodeCount":96,"status":"Ended","genres":["Crime","Drama"],"rating":8.2,"votes":6028,"poster":"/i8OR.jpg"}""")
        val s1 = """{"showId":1405,"number":1,"episodes":[
            {"season":1,"episode":1,"title":"Dexter","playable":true},
            {"season":1,"episode":2,"title":"Episode 2","playable":true},
            {"season":1,"episode":3,"title":"Popping Cherry","playable":false}]}"""
        val s0 = """{"showId":1405,"number":0,"episodes":[{"season":0,"episode":1,"title":"Early Cuts","playable":true}]}"""
        val d = Dexter.parseShow(show, listOf(1 to s1, 0 to s0), o)
        assertEquals(listOf("Mùa 1", "Tập đặc biệt"), d.groups.map { it.name })
        assertEquals(listOf("Tập 1 · Dexter", "Tập 2"), d.groups[0].episodes.map { it.title })
        assertEquals("$o/watch/show/1405-dexter/1/2", d.groups[0].episodes[1].url)
        assertEquals("2006–2013  •  8 mùa  •  96 tập  •  Đã kết thúc  •  Hình sự, Chính kịch  •  TMDB 8.2", d.info)
        // Phim chia mùa: tổng số tập, tập mới nhất = tập cuối của mùa cuối (không tính nhóm tập đặc biệt)
        assertTrue(d.seasons)
        assertEquals(3, d.episodeCount)
        assertEquals("$o/watch/show/1405-dexter/1/2", d.latestEpisode?.url)
    }

    @Test
    fun picksVietnameseThenEnglishSubtitles() {
        val json = """{"files":[
            {"id":"os-1","lang":"en","label":"English","source":"OpenSubtitles"},
            {"id":"os-2","lang":"vi","label":"Vietnamese","source":"OpenSubtitles"},
            {"id":"ss-3","lang":"vi","label":"Vietnamese","source":"Subsource"},
            {"id":"os-4","lang":"ar","label":"Arabic"},
            {"id":"x 5","lang":"en","label":"English","url":"/api/s/20/subs/5.vtt"}]}"""
        val (subs, pending) = Dexter.subtitles(json, "$o/api/subs/file/movie/354912/")
        assertEquals(false, pending)
        assertEquals(listOf("Tiếng Việt 1", "Tiếng Việt 2", "Tiếng Anh 1", "Tiếng Anh 2"), subs.map { it.label })
        assertEquals("$o/api/subs/file/movie/354912/os-2.vtt", subs[0].url)
        assertEquals("$o/api/s/20/subs/5.vtt", subs[3].url)
        val (none, later) = Dexter.subtitles("""{"files":[],"pending":true}""", "$o/api/subs/file/tv/1405/1/1/")
        assertTrue(none.isEmpty() && later)
    }

    @Test
    fun parsesSourcesAsHlsServers() {
        val json = """{"show":{"id":1405},"episode":{"season":1,"episode":1},"sources":[
            {"id":"s20-tv-1405-1-1","kind":"hls","url":"/api/s/20/tv/1405/1/1","label":"Server 20","family":20,"ownSubtitles":true},
            {"id":"s21-tv-1405-1-1","kind":"hls","url":"/api/s/21/tv/1405/1/1?v=3","label":"Server 21","family":21},
            {"id":"x","kind":"hls","url":"/api/s/9/tv/1405/1/1","label":"Server 9","family":9,"sealed":true},
            {"id":"e","kind":"embed","url":"https://example.com/embed/1","label":"Embed"}]}"""
        val ep = "$o/watch/show/1405-dexter/1/1"
        val subs = listOf(SubTrack("Tiếng Việt", "$o/api/subs/file/tv/1405/1/1/os-2.vtt", "vi"))
        val src = Dexter.parseSource(json, ep, subs, "$o/api/subs/list/tv/1405/1/1")
        assertEquals(listOf("s20", "s21"), src.servers.map { it.type })
        assertEquals(listOf("Server 20", "Server 21"), src.servers.map { it.label })
        val v = src.links.getValue("s21")
        assertEquals("$o/api/s/21/tv/1405/1/1?v=3", v.url)
        assertTrue(v.hls)
        assertEquals(subs, v.subs)
        assertEquals("$o/api/subs/list/tv/1405/1/1", v.subsLater)
        assertTrue(src.next)
    }

    @Test
    fun makesSiteLikeSlugs() {
        assertEquals("spider-man-brand-new-day", Dexter.slug("Spider-Man: Brand New Day"))
        assertEquals("i-m-not-ashamed", Dexter.slug("I'm Not Ashamed"))
        assertEquals("amelie", Dexter.slug("Amélie"))
        assertEquals("phim", Dexter.slug("!!!"))
    }
}
