package vn.mtv.tv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Dán một lần nhiều trang phim (Cài đặt → Danh sách trang phim): tách, chuẩn hoá về gốc trang, bỏ trùng. Chỉ tên miền giả. */
class SitePasteTest {
    @Test
    fun splitsNormalizesAndDedups() {
        val text = "a1.example\n" +
            "  https://B2.Example/phim/abc?x=1#y  \n" +
            "\n" +
            "# ghi chú\n" +
            "www.a1.example http://a1.example/\n" +
            "«Trang»: <c3.example>, chữ thường\r\n" +
            "không-có-dấu-chấm\n" +
            "http://d4.example:8080/xyz\n" +
            "kkphim.com/abc\n" +
            "x.example:abc\n" +
            "bản 1.5 và 2.21 [e5.example]. bad..host .an.example localhost\n" +
            "http://d4.example:9090/"
        val r = SitePaste.split(text)
        assertEquals(
            listOf(
                "https://a1.example",
                "https://b2.example",
                "https://c3.example",
                "http://d4.example:8080",
                Prefs.KK_SITE,
                "https://e5.example",
                "http://d4.example:9090",
            ),
            r.sites,
        )
        // «x.example:abc» và «bad..host» là tên sai; số bản, «.an.example», «localhost»: bỏ qua không báo
        assertEquals(2, r.invalid)
    }

    @Test
    fun abbreviationsAreNotSites() {
        // «v.v.», «e.g.», «tr.5» trong câu: đuôi một chữ cái / không phải chữ — bỏ qua không báo; IP đủ 4 phần, xn--… vẫn nhận
        val r = SitePaste.split("Các trang a1.example, b2.example v.v. (e.g. tr.5)\nhttp://10.0.0.5:8080/x xn--fiq228c.xn--p1ai")
        assertEquals(listOf("https://a1.example", "https://b2.example", "http://10.0.0.5:8080", "https://xn--fiq228c.xn--p1ai"), r.sites)
        assertEquals(0, r.invalid)
        assertTrue(SitePaste.plausibleHost("https://phim.example:8443/a@b"))
        assertTrue(!SitePaste.plausibleHost("v.v") && !SitePaste.plausibleHost("e.g") && !SitePaste.plausibleHost("tr.5"))
    }

    @Test
    fun emptyAndCommentOnly() {
        val r = SitePaste.split("\n   \n# a1.example\nchữ không dấu chấm\n")
        assertTrue(r.sites.isEmpty())
        assertEquals(0, r.invalid)
    }

    @Test
    fun quotesAndSeparators() {
        val r = SitePaste.split("\"a1.example\"; 'b2.example' (c3.example)")
        assertEquals(listOf("https://a1.example", "https://b2.example", "https://c3.example"), r.sites)
    }

    @Test
    fun keyIgnoresSchemeWwwAndSlashButKeepsPort() {
        assertEquals("a1.example", SitePaste.key("http://WWW.A1.example/"))
        assertEquals("a1.example:8080", SitePaste.key("http://WWW.A1.example:8080/"))
        assertEquals("a1.example", SitePaste.key("www.a1.example"))
        assertEquals(SitePaste.key("https://a1.example"), SitePaste.key("http://www.a1.example/phim/x"))
    }

    @Test
    fun messages() {
        val two = SitePaste.Added(listOf("https://a1.example", "https://www.b2.example"), existing = 1, invalid = 0)
        assertEquals("Đã thêm 2 trang: a1.example, b2.example (bỏ qua 1 đã có).", SitePaste.addedText(two))
        val many = SitePaste.Added((1..6).map { "https://s$it.example" }, existing = 0, invalid = 2)
        assertEquals(
            "Đã thêm 6 trang: s1.example, s2.example, s3.example… (+3) (2 tên miền không hợp lệ).",
            SitePaste.addedText(many),
        )
        assertEquals("Chưa có tên miền hợp lệ (vd ten-trang.example).", SitePaste.noneText(0))
        assertEquals("Các trang này đã có trong danh sách.", SitePaste.noneText(3))
    }
}
