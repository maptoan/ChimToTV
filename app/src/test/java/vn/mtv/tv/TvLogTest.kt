package vn.mtv.tv

import org.junit.Assert.assertEquals
import org.junit.Test

/** Nhật ký Truyền hình che tài khoản trong link (cùng ca với lõi Rust log::redact). */
class TvLogTest {
    @Test
    fun hidesCredentialsInLinks() {
        assertEquals(
            "tải http://tv.example:8080/get.php?username=***&password=***&type=m3u_plus lỗi",
            TvLog.redact("tải http://tv.example:8080/get.php?username=an&password=b1&type=m3u_plus lỗi"),
        )
        assertEquals(
            "VTV1 (nguồn 1/2) http://tv.example/live/***/***/123.ts",
            TvLog.redact("VTV1 (nguồn 1/2) http://tv.example/live/an/b1/123.ts"),
        )
        assertEquals("https://***@tv.example/x.m3u?token=***#t", TvLog.redact("https://an:b1@tv.example/x.m3u?token=abc#t"))
        assertEquals("danh sách «Thử» — 467 kênh", TvLog.redact("danh sách «Thử» — 467 kênh"))
    }
}
