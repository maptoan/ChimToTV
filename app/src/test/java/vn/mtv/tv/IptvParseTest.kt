package vn.mtv.tv

import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

/** Truyền hình: đọc M3U, giờ trận, giờ XMLTV, tên chuẩn hoá — cùng kết quả với bản Legacy (CBIPTV.m). */
class IptvParseTest {
    private val vn = TimeZone.getTimeZone("GMT+07:00")

    private fun read(text: String): Pair<String?, List<IptvParse.Entry>> {
        var epg: String? = null
        val out = ArrayList<IptvParse.Entry>()
        val r = IptvParse.Reader({ epg = it }) { out += it }
        text.lines().forEach(r::line)
        return epg to out
    }

    @Test
    fun readsExtinfAttributesAndOptions() {
        val (epg, e) = read(
            "﻿#EXTM3U url-tvg=\"https://a.example/epg.xml,https://b.example/epg.xml.gz\"\n" +
                "#EXTINF:-1 tvg-id=\"vtv1.vn\" tvg-name=\"VTV1\" tvg-logo=\"http://l/vtv1.png\" group-title=\"Tin tức, thời sự\",VTV1 HD\n" +
                "#EXTVLCOPT:http-referrer=https://ref.example/\n" +
                "#EXTVLCOPT:http-user-agent=Player/1.0\n" +
                "https://s.example/vtv1.m3u8\n" +
                "#EXTINF:-1,Kênh B\n" +
                "#EXTGRP:Giải trí\n" +
                "http://s.example/b.m3u8|Referer=https%3A%2F%2Fr.example%2F&User-Agent=UA2\n" +
                "#EXTINF:-1 radio=\"true\" group-title=\"Radio\",VOV1\n" +
                "rtmp://bo-qua\n" +
                "http://s.example/vov1\n",
        )
        assertEquals("https://a.example/epg.xml", epg)
        assertEquals(3, e.size)
        assertEquals("VTV1 HD", e[0].name)
        assertEquals("Tin tức, thời sự", e[0].group)
        assertEquals("vtv1.vn", e[0].attrs["tvg-id"])
        assertEquals("https://ref.example/", e[0].ref)
        assertEquals("Player/1.0", e[0].ua)
        assertEquals("Giải trí", e[1].group)
        assertEquals("http://s.example/b.m3u8", e[1].url)
        assertEquals("https://r.example/", e[1].ref)
        assertEquals("UA2", e[1].ua)
        // Dòng rtmp bị bỏ qua nhưng không làm mất #EXTINF? — Legacy: dòng không phải http xoá thuộc tính đang đọc
        assertEquals("http://s.example/vov1", e[2].url)
    }

    @Test
    fun normalizesNames() {
        assertEquals("vtv1", IptvParse.norm("VTV1 HD"))
        assertEquals("anninhtv", IptvParse.norm("An Ninh TV HD (1080p) [Geo-blocked]"))
        assertEquals("dongthap", IptvParse.norm("Đồng Tháp FHD"))
        assertEquals("albsupersport1", IptvParse.norm("|ALB| SUPER SPORT 1 4K"))
        assertEquals("новоерадио", IptvParse.norm("Новое Радио"))
        assertEquals("Can Tho TV 2", IptvParse.displayName("Can Tho TV 2 (1080p) [Geo-blocked]"))
        assertEquals("SUPER SPORT 1 HD", IptvParse.displayName("|ALB| SUPER SPORT 1 HD"))
        assertEquals("ZDF", IptvParse.displayName("DE| ZDF"))
        assertEquals("A|B kênh", IptvParse.displayName("A|B kênh"))
    }

    @Test
    fun detectsKinds() {
        assertTrue(IptvParse.isFlv("http://x/live/abc.flv?token=1"))
        assertTrue(IptvParse.isPlaceholder("https://freem3u.xyz/static/no-signal/low.m3u8"))
        assertEquals(listOf(0, 1, 2), listOf("http://a/b.m3u8", "http://a/b.flv", "http://a/no_signal.m3u8").map(IptvParse::sourceRank))
        assertTrue(IptvParse.isVod("http://h:80/series/u/p/123.mkv"))
        assertTrue(IptvParse.isVod("http://h/movie/u/p/9.mp4"))
        assertFalse(IptvParse.isVod("http://h:80/u/p/3396"))
        assertTrue(IptvParse.isRadio(mapOf("radio" to "true"), "Âm nhạc", "http://a/b"))
        assertTrue(IptvParse.isRadio(emptyMap(), "KÊNH RADIO NƯỚC NGOÀI", "http://a/b"))
        assertTrue(IptvParse.isRadio(emptyMap(), "Khác", "http://a/stream.mp3"))
        assertFalse(IptvParse.isRadio(emptyMap(), "Thể thao", "http://a/b.m3u8"))
        assertTrue(IptvParse.isKids("Kênh Thiếu Nhi", "pops"))
    }

    @Test
    fun readsKickoffTimes() {
        // 16/11/2026 12:00 giờ Việt Nam
        val now = IptvParse.daysFromCivil(2026, 11, 16) * 86400.0 + 5 * 3600
        val k = IptvParse.kickoff("20:00 16/11 ⚽ Việt Nam vs Thái Lan (BLV Quang Huy) [hls]", now)!!
        assertEquals(IptvParse.daysFromCivil(2026, 11, 16) * 86400.0 + 13 * 3600, k.at, 0.1)
        assertEquals("⚽ Việt Nam vs Thái Lan (BLV Quang Huy)", IptvParse.matchName("20:00 16/11 ⚽ Việt Nam vs Thái Lan (BLV Quang Huy) [hls]", k))
        val k2 = IptvParse.kickoff("16/11 02h45 Real - Barca", now)!!
        assertEquals(IptvParse.daysFromCivil(2026, 11, 15) * 86400.0 + 19 * 3600 + 45 * 60, k2.at, 0.1)
        // Qua giao thừa: «01/01» gần bây giờ nhất là năm sau
        val dec = IptvParse.daysFromCivil(2026, 12, 31) * 86400.0
        val k3 = IptvParse.kickoff("19:30 01/01 A vs B", dec)!!
        assertEquals(IptvParse.daysFromCivil(2027, 1, 1) * 86400.0 + 12 * 3600 + 30 * 60, k3.at, 0.1)
        assertNull(IptvParse.kickoff("VTV1 HD", now))
        assertFalse(IptvParse.isMatchName("Updated 16/11 15:36"))
        assertTrue(IptvParse.isMatchName("⚽ A vs B"))
        assertEquals(130, IptvParse.matchMinutes("⚽ A vs B"))
        assertEquals("🎾 A vs B", IptvParse.oldEmoji("🥎 A vs B 🟢"))
    }

    @Test
    fun matchLines() {
        val kick = 1_800_000_000.0
        assertEquals(IptvParse.Match.LATER, IptvParse.matchState(kick, 130, kick - 3600))
        assertEquals(IptvParse.Match.SOON, IptvParse.matchState(kick, 130, kick - 600))
        assertEquals(IptvParse.Match.LIVE, IptvParse.matchState(kick, 130, kick + 600))
        assertEquals(IptvParse.Match.OVER, IptvParse.matchState(kick, 130, kick + 131 * 60))
        assertEquals(IptvParse.Match.NONE, IptvParse.matchState(0.0, 0, kick))
        val clock = IptvParse.clock(kick, vn)
        assertEquals("Sắp đá $clock · còn 25 phút", IptvParse.matchLine(kick, 130, false, kick - 25 * 60, vn))
        assertEquals("Sắp đá $clock · còn 1 giờ 05 phút", IptvParse.matchLine(kick, 130, false, kick - 65 * 60, vn))
        assertEquals("● Đang đá · bóng lăn $clock", IptvParse.matchLine(kick, 130, false, kick + 60, vn))
        assertEquals("● Đang đá · danh sách chưa có link", IptvParse.matchLine(kick, 130, true, kick + 60, vn))
        assertEquals("$clock mai", IptvParse.kickText(kick, kick - 86400, vn))
    }

    @Test
    fun readsXmltvTimes() {
        val t = IptvParse.xmltvTime("20261116190000 +0700")!!
        assertEquals(IptvParse.daysFromCivil(2026, 11, 16) * 86400.0 + 12 * 3600, t, 0.1)
        assertEquals(IptvParse.daysFromCivil(2026, 11, 16) * 86400.0 + 19 * 3600, IptvParse.xmltvTime("202611161900")!!, 0.1)
        assertNull(IptvParse.xmltvTime("abc"))
    }

    @Test
    fun placeholderPlaylists() {
        assertTrue(IptvParse.isPlaceholderPlaylist("#EXTM3U\n#EXTINF:10.0,\nlow0.ts\n#EXT-X-ENDLIST\n"))
        assertFalse(IptvParse.isPlaceholderPlaylist("#EXTM3U\n#EXT-X-TARGETDURATION:6\n#EXTINF:6.0,\na.ts\n"))
    }

    @Test
    fun listNames() {
        assertEquals("4k viptv-playlist", IptvParse.nameFromFile("4k viptv-playlist---2026-11-11T131814-437.m3u"))
        assertEquals("iptv-org.github.io", IptvParse.nameFromUrl("https://iptv-org.github.io/iptv/countries/vn.m3u"))
        assertEquals("playlist", IptvParse.nameFromUrl("https://x.example/a/playlist.m3u8"))
        assertEquals("short.example", IptvParse.nameFromUrl("http://short.example/ab"))
    }

    @Test
    fun sniffClassifiesStreams() {
        fun k(s: String, type: String = "") = LiveSniff.classify(s.toByteArray(Charsets.ISO_8859_1), type, "http://x", emptyMap(), 2)
        assertEquals("hls", k("#EXTM3U\n#EXT-X-STREAM-INF:BANDWIDTH=1\nlow.m3u8\n").kind)
        assertEquals("html", k("<!DOCTYPE html><html>").kind)
        assertEquals("flv", k("FLV\u0001\u0005").codec)
        assertEquals("mp3", k("ID3\u0004\u0000").codec)
        assertEquals("playlist", k("[playlist]\nNumberOfEntries=0\n").kind)
        val ts = ByteArray(400).also { it[0] = 0x47; it[188] = 0x47; it[376] = 0x47 }
        assertEquals("ts", LiveSniff.classify(ts, "", "http://x", emptyMap(), 2).codec)
        val aac = byteArrayOf(0x12, 0xFF.toByte(), 0xF1.toByte(), 0x50, 0x80.toByte())
        assertEquals("aac", LiveSniff.classify(aac, "", "http://x", emptyMap(), 2).codec)
    }

    /** Dán nhiều địa chỉ danh sách một lần: mỗi dòng / mỗi chữ một địa chỉ, thêm http://, bỏ trùng (http / https, dấu / cuối). */
    @Test
    fun splitsPastedUrls() {
        val u = IptvParse.splitUrls(
            "https://a.example/list1\n  http://b.example/x.m3u  \n\n# ghi chú\nshort.example/aaa111\nshort.example/bbb222 short.example/bbb222\n" +
                "«Danh sách 1»: <https://c.example/y.m3u8>, chữ thường\r\nHTTPS://D.example/Z\nhttps://short.example/aaa111/\n",
        )
        assertEquals(
            listOf("https://a.example/list1", "http://b.example/x.m3u", "http://short.example/aaa111", "http://short.example/bbb222",
                "https://c.example/y.m3u8", "HTTPS://D.example/Z"),
            u,
        )
        assertTrue(IptvParse.splitUrls("  \n# chỉ chú thích\nkhông có địa chỉ").isEmpty())
        assertEquals(2, IptvParse.splitUrls("http://A.example/x/\nhttps://a.example/x\na.example/X").size)
        assertEquals("short.gy/AbC", IptvParse.listUrlKey("HTTPS://Short.Gy/AbC/"))
    }

    /** Mã QR của hộp «Gửi danh sách kênh tới máy này»: zxing đọc ra đúng địa chỉ (đủ các cỡ địa chỉ trong mạng nhà). */
    @Test
    fun qrCodesDecode() {
        for (text in listOf("http://10.0.0.5:8795", "http://192.168.100.123:8795", "http://172.16.254.200:8795/?x=" + "a".repeat(60))) {
            val m = Qr.encode(text)
            assertNotNull(m)
            val scale = 4
            val n = m!!.size + 8
            val px = IntArray(n * scale * n * scale) { 0xFFFFFFFF.toInt() }
            for (y in m.indices) for (x in m.indices) if (m[y][x]) {
                for (dy in 0 until scale) for (dx in 0 until scale) px[((y + 4) * scale + dy) * n * scale + (x + 4) * scale + dx] = 0xFF000000.toInt()
            }
            val bmp = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(n * scale, n * scale, px)))
            assertEquals(text, QRCodeReader().decode(bmp).text)
        }
    }
}
