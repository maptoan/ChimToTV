package vn.mtv.tv

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder

/**
 * Thêm danh sách kênh: dán / gõ địa chỉ, gửi từ điện thoại / máy tính qua Wi-Fi (trang web nhỏ trên tivi — chỉ mở khi hộp
 * «Gửi từ điện thoại / máy tính» đang hiện), hoặc chọn tệp .m3u có trên máy (điện thoại). Cùng các cách của bản Legacy
 * (CBListImport) — gõ địa chỉ dài bằng remote rất mệt.
 */
object IptvImport {
    const val REQ_FILE = 4201
    const val PORT = 8795
    private var pending: ((IptvList) -> Unit)? = null

    fun ask(a: Activity, done: (IptvList) -> Unit) {
        val canPick = Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*")
            .resolveActivity(a.packageManager) != null && !Screen.isTv(a)
        val items = mutableListOf("Nhập địa chỉ danh sách (http…)", "Gửi từ điện thoại / máy tính (cùng Wi-Fi)")
        if (canPick) items += "Chọn tệp .m3u trên máy"
        AlertDialog.Builder(a, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Thêm danh sách kênh")
            .setItems(items.toTypedArray()) { _, w ->
                when (w) {
                    0 -> askUrl(a, done)
                    1 -> receive(a, done)
                    else -> pickFile(a, done)
                }
            }
            .setNegativeButton("Huỷ", null)
            .show()
    }

    /** Câu báo sau khi thêm nhiều danh sách; null = chưa thêm được danh sách nào. */
    fun addedText(r: Iptv.AddMany): String? {
        val first = r.added.firstOrNull() ?: return null
        val skip = if (r.existing > 0) " (bỏ qua ${r.existing} đã có)" else ""
        return if (r.added.size == 1) "Đã thêm danh sách «${first.name}»$skip — đang tải kênh…"
        else "Đã thêm ${r.added.size} danh sách$skip — đang tải «${first.name}»; danh sách khác tải khi chọn trong «Đổi danh sách»."
    }

    fun noneText(r: Iptv.AddMany): String = when (r.existing) {
        0 -> "Chưa có địa chỉ danh sách."
        1 -> "Danh sách này đã có trong app."
        else -> "Các danh sách này đã có trong app."
    }

    private fun added(a: Activity, l: IptvList, done: (IptvList) -> Unit, text: String? = null) {
        Toast.makeText(a, text ?: "Đã thêm danh sách «${l.name}» — đang tải kênh…", Toast.LENGTH_LONG).show()
        Iptv.refresh(true) { err -> if (err != null && !a.isFinishing) Toast.makeText(a, err, Toast.LENGTH_LONG).show() }
        done(l)
    }

    private fun askUrl(a: Activity, done: (IptvList) -> Unit) {
        val box = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(a.dp(22), a.dp(8), a.dp(22), 0)
        }
        // Nhiều dòng: dán một lần nhiều địa chỉ, mỗi dòng một danh sách
        val url = EditText(a).apply {
            hint = "http://…/danh-sach.m3u\n(dán được nhiều địa chỉ, mỗi dòng một danh sách)"
            isSingleLine = false
            minLines = 2
            maxLines = 6
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                android.text.InputType.TYPE_TEXT_VARIATION_URI or android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            leaveWithUpDown()
        }
        val name = EditText(a).apply {
            hint = "Tên hiện trong app (không bắt buộc)"
            isSingleLine = true
            leaveWithUpDown()
        }
        box.addView(url)
        box.addView(name)
        val d = AlertDialog.Builder(a, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Địa chỉ danh sách M3U / M3U8")
            .setView(box)
            .setPositiveButton("Thêm", null)
            .setNegativeButton("Huỷ", null)
            .show()
        d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val r = Iptv.addLists(url.text.toString(), name.text.toString())
            val first = r.added.firstOrNull()
            if (first == null) {
                Toast.makeText(a, noneText(r), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            d.dismiss()
            added(a, first, done, addedText(r))
        }
        url.requestFocus()
        url.openKeyboard()
    }

    // ---- Tệp trên máy (điện thoại) ----

    private fun pickFile(a: Activity, done: (IptvList) -> Unit) {
        pending = done
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*")
        runCatching { a.startActivityForResult(i, REQ_FILE) }
            .onFailure { Toast.makeText(a, "Máy không có trình chọn tệp.", Toast.LENGTH_LONG).show() }
    }

    /** Gọi từ onActivityResult của màn đã mở trình chọn tệp. */
    fun onResult(a: Activity, req: Int, ok: Boolean, data: Intent?) {
        if (req != REQ_FILE) return
        val done = pending ?: return
        pending = null
        val uri = data?.data ?: return
        if (!ok) return
        val name = runCatching {
            a.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull() ?: uri.lastPathSegment ?: "danh-sach.m3u"
        val tmp = Iptv.tempFile()
        a.background({
            a.contentResolver.openInputStream(uri)!!.use { copyLimited(it, tmp) }
            Iptv.addListFromFile(tmp, name, null).getOrThrow()
        }) { r ->
            r.onSuccess { added(a, it, done) }
            r.onFailure { Toast.makeText(a, it.message ?: "Không đọc được tệp.", Toast.LENGTH_LONG).show() }
        }
    }

    private fun copyLimited(input: InputStream, dest: File, limit: Long = Iptv.MAX_FILE_BYTES, first: ByteArray? = null) {
        dest.outputStream().use { out ->
            var total = 0L
            first?.let { out.write(it); total += it.size }
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                total += n
                if (total > limit) {
                    dest.delete()
                    throw IllegalStateException("Tệp quá lớn (giới hạn ${limit / 1024 / 1024} MB) — thêm danh sách bằng địa chỉ.")
                }
                out.write(buf, 0, n)
            }
        }
    }

    // ---- Gửi qua Wi-Fi ----

    /** Địa chỉ IPv4 của máy trong mạng nhà (Wi-Fi / dây); null = chưa nối mạng. */
    fun lanAddress(): String? = runCatching {
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback }
            .sortedBy { if (it.name.startsWith("wlan") || it.name.startsWith("eth")) 0 else 1 }
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { it is Inet4Address && it.isSiteLocalAddress }?.hostAddress
    }.getOrNull()

    private fun receive(a: Activity, done: (IptvList) -> Unit) {
        val ip = lanAddress()
        if (ip == null) {
            Toast.makeText(a, "Máy chưa nối mạng trong nhà (Wi-Fi / dây).", Toast.LENGTH_LONG).show()
            return
        }
        var dialog: AlertDialog? = null
        val server = Receiver { result ->
            mainHandler.post {
                if (a.isFinishing) return@post
                result.onSuccess { l ->
                    dialog?.dismiss()
                    added(a, l, done)
                }
            }
        }
        val port = server.start()
        if (port <= 0) {
            Toast.makeText(a, "Không mở được cổng nhận tệp.", Toast.LENGTH_LONG).show()
            return
        }
        val addr = "http://$ip" + (if (port == 80) "" else ":$port")
        val view = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(a.dp(28), a.dp(12), a.dp(28), a.dp(4))
            addView(TextView(a).apply {
                text = "Trên điện thoại hoặc máy tính dùng cùng mạng Wi-Fi với máy này, mở trình duyệt và vào địa chỉ:"
                textSize = 16f
                setTextColor(a.colorOf(R.color.text))
            })
            addView(TextView(a).apply {
                text = addr
                textSize = 30f
                setTextColor(a.colorOf(R.color.focus))
                setPadding(0, a.dp(14), 0, a.dp(14))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            Qr.view(a, addr, a.dp(200))?.let { addView(it) }
            addView(TextView(a).apply {
                text = "Trang đó cho dán địa chỉ danh sách, chọn tệp .m3u, hoặc dán nội dung danh sách. Giữ hộp này mở tới khi gửi xong."
                textSize = 14f
                setTextColor(a.colorOf(R.color.text_dim))
                setPadding(0, a.dp(10), 0, 0)
            })
        }
        dialog = AlertDialog.Builder(a, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Gửi danh sách kênh tới máy này")
            .setView(view)
            .setNegativeButton("Đóng", null)
            .setOnDismissListener { server.stop() }
            .show()
        TvLog.i("IPTV: mở trang nhận danh sách $addr")
    }

    /** Trang nhận danh sách trên mạng nhà: GET / → trang; POST /url (địa chỉ), POST /file (thân = nội dung tệp). */
    private class Receiver(private val onAdded: (Result<IptvList>) -> Unit) {
        @Volatile private var server: ServerSocket? = null

        fun start(): Int {
            val s = runCatching { ServerSocket().apply { reuseAddress = true; bind(InetSocketAddress(PORT)) } }.getOrNull()
                ?: runCatching { ServerSocket(0) }.getOrNull() ?: return -1
            server = s
            Thread({
                while (true) {
                    val c = runCatching { s.accept() }.getOrNull() ?: break
                    Thread({ runCatching { serve(c) }; runCatching { c.close() } }, "iptv-upload").start()
                }
            }, "iptv-upload-accept").start()
            return s.localPort
        }

        fun stop() {
            runCatching { server?.close() }
            server = null
        }

        private fun serve(sock: Socket) {
            sock.soTimeout = 60_000
            val input = BufferedInputStream(sock.getInputStream(), 64 * 1024)
            val head = readHead(input) ?: return
            val lines = head.split("\r\n")
            val (method, target) = lines[0].split(' ').let { (it.getOrNull(0) ?: "") to (it.getOrNull(1) ?: "/") }
            val headers = lines.drop(1).mapNotNull { l -> l.indexOf(':').takeIf { it > 0 }?.let { l.substring(0, it).trim().lowercase() to l.substring(it + 1).trim() } }.toMap()
            val path = target.substringBefore('?')
            val query = target.substringAfter('?', "").split('&').mapNotNull { kv ->
                kv.indexOf('=').takeIf { it > 0 }?.let { kv.substring(0, it) to runCatching { URLDecoder.decode(kv.substring(it + 1), "UTF-8") }.getOrDefault("") }
            }.toMap()
            val out = sock.getOutputStream()
            val length = headers["content-length"]?.toLongOrNull() ?: 0
            when {
                method == "GET" && (path == "/" || path == "/index.html") -> reply(out, 200, "text/html; charset=utf-8", PAGE.toByteArray())
                method == "POST" && path == "/url" -> {
                    if (length > 8192) return json(out, false, "Địa chỉ quá dài.")
                    val body = String(readN(input, length.toInt()), Charsets.UTF_8)
                    val o = runCatching { JSONObject(body) }.getOrNull()
                    val u = o?.optString("url").orEmpty()
                    val r = runOnMain { Iptv.addLists(u, o?.optString("name")) }
                    val l = r.added.firstOrNull() ?: return json(out, false, noneText(r))
                    val skip = if (r.existing > 0) " (bỏ qua ${r.existing} đã có)" else ""
                    json(out, true, if (r.added.size == 1) "Đã thêm «${l.name}»$skip — tivi đang tải danh sách."
                        else "Đã thêm ${r.added.size} danh sách$skip — tivi đang tải «${l.name}».")
                    onAdded(Result.success(l))
                }
                method == "POST" && path == "/file" -> {
                    if (length <= 0) return json(out, false, "Tệp trống.")
                    if (length > Iptv.MAX_FILE_BYTES) return json(out, false, "Tệp quá lớn — thêm danh sách bằng địa chỉ.")
                    val tmp = Iptv.tempFile()
                    tmp.outputStream().use { o -> copyN(input, o, length) }
                    val r = Iptv.addListFromFile(tmp, query["fn"]?.takeIf { it.isNotEmpty() } ?: "danh-sach.m3u", query["name"])
                    r.onSuccess { l ->
                        json(out, true, "Đã thêm «${l.name}» — tivi đang đọc danh sách.")
                        onAdded(Result.success(l))
                    }.onFailure {
                        tmp.delete()
                        json(out, false, it.message ?: "Không đọc được tệp.")
                    }
                }
                else -> reply(out, 404, "text/plain; charset=utf-8", "Không có".toByteArray())
            }
        }

        private fun <T> runOnMain(f: () -> T): T {
            val lock = Object()
            var r: Result<T>? = null
            mainHandler.post {
                val x = runCatching(f)
                synchronized(lock) { r = x; lock.notifyAll() }
            }
            synchronized(lock) { while (r == null) lock.wait(5000) }
            return r!!.getOrThrow()
        }

        private fun readHead(input: InputStream): String? {
            val b = StringBuilder()
            var last4 = 0
            while (b.length < 16 * 1024) {
                val c = input.read()
                if (c < 0) return null
                b.append(c.toChar())
                last4 = (last4 shl 8) or c
                if (last4 == 0x0D0A0D0A) return b.toString().trimEnd()
            }
            return null
        }

        private fun readN(input: InputStream, n: Int): ByteArray {
            val b = ByteArray(n)
            var off = 0
            while (off < n) {
                val r = input.read(b, off, n - off)
                if (r < 0) break
                off += r
            }
            return b.copyOf(off)
        }

        private fun copyN(input: InputStream, out: OutputStream, n: Long) {
            val buf = ByteArray(64 * 1024)
            var left = n
            while (left > 0) {
                val r = input.read(buf, 0, minOf(buf.size.toLong(), left).toInt())
                if (r < 0) break
                out.write(buf, 0, r)
                left -= r
            }
        }

        private fun json(out: OutputStream, ok: Boolean, msg: String) =
            reply(out, 200, "application/json; charset=utf-8", JSONObject().put("ok", ok).put("msg", msg).toString().toByteArray())

        private fun reply(out: OutputStream, code: Int, type: String, body: ByteArray) {
            val reason = if (code == 200) "OK" else "Not Found"
            out.write("HTTP/1.1 $code $reason\r\nContent-Type: $type\r\nContent-Length: ${body.size}\r\nCache-Control: no-store\r\nConnection: close\r\n\r\n".toByteArray())
            out.write(body)
            out.flush()
        }
    }

    private const val PAGE = """<!doctype html><html lang="vi"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Gửi danh sách kênh — MTV</title><style>
body{font:16px/1.5 -apple-system,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;background:#0e1014;color:#f2f4f8;margin:0;padding:16px}
main{max-width:560px;margin:0 auto;background:#171a21;border-radius:12px;padding:20px 22px}
h1{font-size:21px;margin:0 0 6px}p{color:#9aa3b2;margin:0 0 14px}h2{font-size:16px;margin:22px 0 6px;color:#ffb020}
input,textarea{display:block;width:100%;box-sizing:border-box;margin-top:6px;font:inherit;background:#232833;color:#f2f4f8;border:1px solid #3a4150;border-radius:8px;padding:9px}
textarea{height:110px;font-family:monospace;font-size:13px}
button{margin-top:10px;font:inherit;font-weight:600;background:#2baf85;color:#fff;border:0;border-radius:8px;padding:10px 22px;cursor:pointer}
#msg{display:none;border-radius:8px;padding:10px 14px;margin-top:16px}.ok{background:#16382c;color:#9de0c3}.err{background:#3d1d1b;color:#f4b4ad}
.small{font-size:13px;color:#7d8696;margin-top:20px}</style></head><body><main>
<h1>Gửi danh sách kênh tới tivi</h1><p>MTV không kèm sẵn kênh — chỉ gửi danh sách bạn có quyền xem.</p>
<label>Tên hiện trên tivi <span style="color:#7d8696">(không bắt buộc)</span><input id="name" maxlength="40"></label>
<h2>1. Địa chỉ danh sách (M3U / M3U8) — dán được nhiều, mỗi dòng một danh sách</h2><textarea id="url" rows="4" style="height:96px" placeholder="http://…/danh-sach.m3u" inputmode="url" autocapitalize="off" spellcheck="false"></textarea>
<button onclick="sendUrl()">Thêm địa chỉ</button>
<h2>2. Tệp .m3u trên máy này</h2><input type="file" id="file" accept=".m3u,.m3u8,.txt"><button onclick="sendFile()">Gửi tệp</button>
<h2>3. Hoặc dán nội dung danh sách</h2><textarea id="text" placeholder="#EXTM3U&#10;#EXTINF:-1 group-title=&quot;Tin tức&quot;,Tên kênh&#10;http://…/kenh.m3u8"></textarea>
<button onclick="sendText()">Gửi nội dung</button><div id="msg"></div>
<p class="small">Trang này chỉ mở khi hộp «Gửi danh sách kênh tới máy này» đang hiện trên tivi.</p></main><script>
function show(r){var m=document.getElementById('msg');m.style.display='block';m.className=r.ok?'ok':'err';m.textContent=r.msg}
function nm(){return document.getElementById('name').value.trim()}
function post(path,body){show({ok:true,msg:'Đang gửi…'});fetch(path,{method:'POST',body:body}).then(function(r){return r.json()}).then(show).catch(function(){show({ok:false,msg:'Không gửi được — hộp nhận trên tivi đã đóng?'})})}
function sendUrl(){post('/url',JSON.stringify({url:document.getElementById('url').value.trim(),name:nm()}))}
function sendFile(){var f=document.getElementById('file').files[0];if(!f){show({ok:false,msg:'Chưa chọn tệp.'});return}post('/file?fn='+encodeURIComponent(f.name)+'&name='+encodeURIComponent(nm()),f)}
function sendText(){var t=document.getElementById('text').value;if(!t.trim()){show({ok:false,msg:'Chưa dán nội dung.'});return}post('/file?fn=danh-sach.m3u&name='+encodeURIComponent(nm()),new Blob([t],{type:'text/plain'}))}
</script></body></html>"""
}
