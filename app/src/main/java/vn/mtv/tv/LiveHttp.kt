package vn.mtv.tv

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Nguồn dữ liệu HTTP của trình phát truyền hình (ExoPlayer): đi cùng đường mạng với phần còn lại của app (proxy SOCKS5 /
 * Shadowsocks, DNS mã hoá, vượt chặn — cổng nội bộ của AppProxy), gửi Referer / User-Agent riêng của nguồn kênh (danh sách
 * M3U ghi `#EXTVLCOPT:http-referrer`, `url|Referer=…`: máy chủ thể thao chỉ trả cho trình phát nó quen). Tự theo chuyển
 * hướng (kể cả http ↔ https — HttpURLConnection không tự làm), trả tiêu đề trả lời (ExoPlayer đọc icy-metaint của đài radio).
 */
@OptIn(UnstableApi::class)
class LiveHttpDataSource(private val headers: Map<String, String>) : BaseDataSource(true), HttpDataSource {
    class Factory(private val headers: Map<String, String>) : DataSource.Factory {
        override fun createDataSource(): DataSource = LiveHttpDataSource(headers)
    }

    private val extra = HashMap<String, String>()
    private var conn: HttpURLConnection? = null
    private var input: InputStream? = null
    private var uri: Uri? = null
    private var spec: DataSpec? = null
    private var left = C.LENGTH_UNSET.toLong()
    private var code = -1
    private var opened = false

    override fun open(dataSpec: DataSpec): Long {
        spec = dataSpec
        uri = dataSpec.uri
        transferInitializing(dataSpec)
        var current = dataSpec.uri.toString()
        var c: HttpURLConnection? = null
        var direct = false
        for (hop in 0..9) {
            val proxy = if (direct) java.net.Proxy.NO_PROXY else AppProxy.javaProxy()
            val x = (if (proxy != null) URL(current).openConnection(proxy) else URL(current).openConnection()) as HttpURLConnection
            x.instanceFollowRedirects = false
            x.connectTimeout = 15_000
            x.readTimeout = 20_000
            x.setRequestProperty("User-Agent", Http.UA)
            x.setRequestProperty("Accept-Encoding", "identity")
            for ((k, v) in headers) x.setRequestProperty(k, v)
            for ((k, v) in extra) x.setRequestProperty(k, v)
            for ((k, v) in dataSpec.httpRequestHeaders) x.setRequestProperty(k, v)
            if (dataSpec.position != 0L || dataSpec.length != C.LENGTH_UNSET.toLong()) {
                val end = if (dataSpec.length != C.LENGTH_UNSET.toLong()) (dataSpec.position + dataSpec.length - 1).toString() else ""
                x.setRequestProperty("Range", "bytes=${dataSpec.position}-$end")
            }
            val rc = try {
                x.responseCode
            } catch (e: IOException) {
                x.disconnect()
                // Mạng của app (DNS mã hoá chỉ lấy một địa chỉ — địa chỉ đó chết) hỏng: thử mạng của máy, trừ khi đang bật proxy
                if (!direct && !AppProxy.active && AppProxy.javaProxy() != null) {
                    direct = true
                    continue
                }
                throw HttpDataSource.HttpDataSourceException.createForIOException(e, dataSpec, HttpDataSource.HttpDataSourceException.TYPE_OPEN)
            }
            if (rc in 300..399 && rc != 304) {
                val loc = x.getHeaderField("Location")
                x.disconnect()
                if (loc.isNullOrBlank()) break
                current = URL(URL(current), loc).toString()
                continue
            }
            c = x
            break
        }
        val cc = c ?: throw HttpDataSource.HttpDataSourceException(
            "Chuyển hướng quá nhiều lần", dataSpec, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            HttpDataSource.HttpDataSourceException.TYPE_OPEN,
        )
        conn = cc
        uri = Uri.parse(current)
        code = cc.responseCode
        if (code !in 200..299) {
            val body = runCatching { cc.errorStream?.use { it.readBytes() } }.getOrNull() ?: ByteArray(0)
            val h = cc.headerFields.filterKeys { it != null }
            cc.disconnect()
            conn = null
            if (code == 416 && dataSpec.position > 0) {
                // Đã tới cuối tệp
                opened = true
                transferStarted(dataSpec)
                left = 0
                return 0
            }
            throw HttpDataSource.InvalidResponseCodeException(code, cc.responseMessage, null, h, dataSpec, body)
        }
        // Máy chủ bỏ qua Range (trả 200 cả tệp): tự bỏ phần đầu
        var skip = if (code == 200 && dataSpec.position > 0) dataSpec.position else 0L
        val len = cc.getHeaderField("Content-Length")?.toLongOrNull() ?: -1L
        left = when {
            dataSpec.length != C.LENGTH_UNSET.toLong() -> dataSpec.length
            len >= 0 -> len - skip
            else -> C.LENGTH_UNSET.toLong()
        }
        val s = try {
            cc.inputStream
        } catch (e: IOException) {
            cc.disconnect()
            throw HttpDataSource.HttpDataSourceException.createForIOException(e, dataSpec, HttpDataSource.HttpDataSourceException.TYPE_OPEN)
        }
        input = s
        while (skip > 0) {
            val n = s.skip(skip)
            if (n <= 0) {
                if (s.read() < 0) break
                skip--
            } else skip -= n
        }
        opened = true
        transferStarted(dataSpec)
        return left
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (left == 0L) return C.RESULT_END_OF_INPUT
        val want = if (left == C.LENGTH_UNSET.toLong()) length else minOf(length.toLong(), left).toInt()
        val n = try {
            input?.read(buffer, offset, want) ?: return C.RESULT_END_OF_INPUT
        } catch (e: IOException) {
            throw HttpDataSource.HttpDataSourceException.createForIOException(e, spec!!, HttpDataSource.HttpDataSourceException.TYPE_READ)
        }
        if (n < 0) return C.RESULT_END_OF_INPUT
        if (left != C.LENGTH_UNSET.toLong()) left -= n
        bytesTransferred(n)
        return n
    }

    override fun getUri(): Uri? = uri

    override fun getResponseHeaders(): Map<String, List<String>> =
        conn?.headerFields?.filterKeys { it != null } ?: emptyMap()

    override fun getResponseCode(): Int = code

    override fun close() {
        runCatching { input?.close() }
        runCatching { conn?.disconnect() }
        input = null
        conn = null
        if (opened) {
            opened = false
            transferEnded()
        }
    }

    override fun setRequestProperty(name: String, value: String) {
        extra[name] = value
    }

    override fun clearRequestProperty(name: String) {
        extra.remove(name)
    }

    override fun clearAllRequestProperties() = extra.clear()
}

/**
 * Nhận dạng link không rõ loại theo đuôi (đài radio «…/radio_128», link chuyển tiếp «…/?id=vtv6», .php, Xtream «…/123»):
 * đọc ≤ 4 KB đầu (theo chuyển hướng) → loại luồng. Danh sách .pls / .m3u của đài thì đọc tiếp địa chỉ bên trong (tối đa 2
 * lớp). Cùng loại với `media.sniff` của lõi (bản Legacy).
 */
object LiveSniff {
    class Result(
        /** hls | dash | progressive | playlist | html | http | error */
        val kind: String,
        /** Địa chỉ luồng thật (đọc trong danh sách .pls / .m3u của đài); null = giữ link gốc. */
        val url: String? = null,
        val status: Int = 0,
        val codec: String? = null,
        val error: String? = null,
    )

    fun sniff(url: String, headers: Map<String, String>, depth: Int = 0): Result {
        val h = headers + mapOf("Icy-MetaData" to "0", "Accept-Encoding" to "identity")
        val c = try {
            Http.open(url, h)
        } catch (e: IOException) {
            if (AppProxy.active) return Result("error", error = e.message ?: e.javaClass.simpleName)
            try {
                Http.openDirect(url, h)
            } catch (e2: IOException) {
                return Result("error", error = e.message ?: e.javaClass.simpleName)
            }
        }
        try {
            val code = c.responseCode
            if (code >= 400) return Result("http", status = code)
            val type = c.contentType?.lowercase().orEmpty()
            val buf = ByteArray(4096)
            var n = 0
            c.inputStream.use { s ->
                while (n < buf.size) {
                    val r = s.read(buf, n, buf.size - n)
                    if (r < 0) break
                    n += r
                }
            }
            return classify(buf.copyOf(n), type, url, headers, depth)
        } catch (e: IOException) {
            return Result("error", error = e.message ?: e.javaClass.simpleName)
        } finally {
            c.disconnect()
        }
    }

    internal fun classify(b: ByteArray, type: String, url: String, headers: Map<String, String>, depth: Int): Result {
        val head = String(b, 0, minOf(b.size, 1024), Charsets.ISO_8859_1).trimStart('\uFEFF', '\u00EF', '\u00BB', '\u00BF', ' ', '\r', '\n', '\t')
        val low = head.lowercase()
        fun u8(i: Int) = if (i < b.size) b[i].toInt() and 0xFF else -1
        when {
            head.startsWith("#EXTM3U") && "#EXT-X-" in head -> return Result("hls")
            "mpegurl" in type && "#EXT-X-" in head -> return Result("hls")
            low.startsWith("<?xml") && "<mpd" in low || low.startsWith("<mpd") -> return Result("dash")
            low.startsWith("[playlist]") -> {
                val inner = Regex("(?im)^file\\d+\\s*=\\s*(https?://\\S+)").find(head)?.groupValues?.get(1)
                return inner(inner, headers, depth)
            }
            head.startsWith("#EXTM3U") || ("mpegurl" in type && head.lineSequence().any { it.trim().startsWith("http") }) -> {
                val inner = head.lineSequence().map { it.trim() }.firstOrNull { it.startsWith("http://") || it.startsWith("https://") }
                return inner(inner, headers, depth)
            }
            low.startsWith("<!doctype html") || low.startsWith("<html") || "text/html" in type -> return Result("html")
            head.startsWith("FLV") -> return Result("progressive", codec = "flv")
            u8(0) == 0x47 && (b.size < 189 || u8(188) == 0x47) -> return Result("progressive", codec = "ts")
            head.startsWith("OggS") -> return Result("progressive", codec = "ogg")
            head.startsWith("ID3") -> return Result("progressive", codec = "mp3")
            b.size > 8 && String(b, 4, 4, Charsets.ISO_8859_1) == "ftyp" -> return Result("progressive", codec = "mp4")
            u8(0) == 0x1A && u8(1) == 0x45 && u8(2) == 0xDF && u8(3) == 0xA3 -> return Result("progressive", codec = "mkv")
        }
        // Luồng tiếng liền (Icecast cắt giữa khung): tìm khung ADTS (AAC) / MPEG (MP3)
        for (i in 0 until b.size - 1) {
            if (u8(i) != 0xFF) continue
            val x = u8(i + 1)
            if ((x and 0xF6) == 0xF0) return Result("progressive", codec = "aac")
            if ((x and 0xE0) == 0xE0 && (x and 0x06) != 0) return Result("progressive", codec = "mp3")
        }
        return when {
            "audio/aac" in type || "audio/aacp" in type -> Result("progressive", codec = "aac")
            "audio/" in type -> Result("progressive", codec = "mp3")
            "video/" in type || "octet-stream" in type -> Result("progressive")
            else -> Result("progressive")
        }
    }

    private fun inner(inner: String?, headers: Map<String, String>, depth: Int): Result {
        if (inner == null || depth >= 2) return Result("playlist")
        val r = sniff(inner, headers, depth + 1)
        return Result(r.kind, r.url ?: inner, r.status, r.codec, r.error)
    }
}
