package vn.mtv.tv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.widget.ImageView
import android.widget.LinearLayout

/**
 * Tạo mã QR (chế độ byte, mức sửa lỗi M, phiên bản 1–10 — đủ cho địa chỉ trong mạng nhà) theo tiêu chuẩn ISO/IEC 18004, cách
 * làm của thư viện QR Code generator (Nayuki, MIT). Dùng cho hộp «Gửi danh sách kênh tới máy này»: địa chỉ tivi đổi theo mạng
 * nên không vẽ sẵn được như mã của màn «Mời bạn bè». Có test đọc lại bằng zxing (QrTest).
 */
object Qr {
    private val ECC_PER_BLOCK = intArrayOf(-1, 10, 16, 26, 18, 24, 16, 18, 22, 22, 26)
    private val BLOCKS = intArrayOf(-1, 1, 1, 1, 2, 2, 4, 4, 4, 5, 5)

    /** Ma trận ô (true = đen), null nếu chuỗi quá dài. */
    fun encode(text: String): Array<BooleanArray>? {
        val data = text.toByteArray(Charsets.UTF_8)
        val ver = (1..10).firstOrNull { v -> 4 + (if (v < 10) 8 else 16) + data.size * 8 <= dataCodewords(v) * 8 } ?: return null
        val cap = dataCodewords(ver) * 8
        val bits = ArrayList<Boolean>()
        fun put(v: Int, n: Int) = (n - 1 downTo 0).forEach { bits += ((v ushr it) and 1) != 0 }
        put(4, 4)
        put(data.size, if (ver < 10) 8 else 16)
        data.forEach { put(it.toInt() and 0xFF, 8) }
        put(0, minOf(4, cap - bits.size))
        put(0, (8 - bits.size % 8) % 8)
        var pad = 0xEC
        while (bits.size < cap) {
            put(pad, 8)
            pad = pad xor 0xEC xor 0x11
        }
        val bytes = IntArray(bits.size / 8) { i -> (0 until 8).fold(0) { acc, j -> (acc shl 1) or (if (bits[i * 8 + j]) 1 else 0) } }
        return Matrix(ver).build(addEcc(bytes, ver))
    }

    private fun rawModules(v: Int): Int {
        var r = (16 * v + 128) * v + 64
        if (v >= 2) {
            val n = v / 7 + 2
            r -= (25 * n - 10) * n - 55
            if (v >= 7) r -= 36
        }
        return r
    }

    private fun dataCodewords(v: Int) = rawModules(v) / 8 - ECC_PER_BLOCK[v] * BLOCKS[v]

    private fun mul(x: Int, y: Int): Int {
        var z = 0
        for (i in 7 downTo 0) {
            z = (z shl 1) xor ((z ushr 7) * 0x11D)
            z = z xor (((y ushr i) and 1) * x)
        }
        return z
    }

    private fun divisor(degree: Int): IntArray {
        val r = IntArray(degree).also { it[degree - 1] = 1 }
        var root = 1
        repeat(degree) {
            for (j in 0 until degree) {
                r[j] = mul(r[j], root)
                if (j + 1 < degree) r[j] = r[j] xor r[j + 1]
            }
            root = mul(root, 0x02)
        }
        return r
    }

    private fun remainder(data: IntArray, div: IntArray): IntArray {
        val r = IntArray(div.size)
        for (b in data) {
            val f = b xor r[0]
            System.arraycopy(r, 1, r, 0, r.size - 1)
            r[r.size - 1] = 0
            for (i in r.indices) r[i] = r[i] xor mul(div[i], f)
        }
        return r
    }

    private fun addEcc(data: IntArray, ver: Int): IntArray {
        val numBlocks = BLOCKS[ver]
        val eccLen = ECC_PER_BLOCK[ver]
        val raw = rawModules(ver) / 8
        val numShort = numBlocks - raw % numBlocks
        val shortLen = raw / numBlocks
        val div = divisor(eccLen)
        val blocks = ArrayList<IntArray>()
        var k = 0
        for (i in 0 until numBlocks) {
            val len = shortLen - eccLen + (if (i < numShort) 0 else 1)
            val dat = data.copyOfRange(k, k + len)
            k += len
            val ecc = remainder(dat, div)
            // Khối ngắn chèn một ô trống để các khối dài bằng nhau khi xen kẽ
            val block = if (i < numShort) dat + intArrayOf(0) + ecc else dat + ecc
            blocks += block
        }
        val out = ArrayList<Int>()
        for (i in blocks[0].indices) {
            for ((j, b) in blocks.withIndex()) {
                if (i != shortLen - eccLen || j >= numShort) out += b[i]
            }
        }
        return out.toIntArray()
    }

    private class Matrix(val ver: Int) {
        val size = ver * 4 + 17
        val m = Array(size) { BooleanArray(size) }
        val fn = Array(size) { BooleanArray(size) }

        fun set(x: Int, y: Int, dark: Boolean) {
            m[y][x] = dark
            fn[y][x] = true
        }

        fun build(code: IntArray): Array<BooleanArray> {
            for (i in 0 until size) {
                set(6, i, i % 2 == 0)
                set(i, 6, i % 2 == 0)
            }
            finder(3, 3); finder(size - 4, 3); finder(3, size - 4)
            val pos = alignPositions()
            for (i in pos.indices) for (j in pos.indices) {
                if ((i == 0 && j == 0) || (i == 0 && j == pos.size - 1) || (i == pos.size - 1 && j == 0)) continue
                align(pos[i], pos[j])
            }
            format(0)
            version()
            // Ô dữ liệu theo đường zigzag
            var i = 0
            var right = size - 1
            while (right >= 1) {
                if (right == 6) right = 5
                for (vert in 0 until size) {
                    for (j in 0..1) {
                        val x = right - j
                        val up = ((right + 1) and 2) == 0
                        val y = if (up) size - 1 - vert else vert
                        if (!fn[y][x] && i < code.size * 8) {
                            m[y][x] = ((code[i ushr 3] ushr (7 - (i and 7))) and 1) != 0
                            i++
                        }
                    }
                }
                right -= 2
            }
            // Chọn mặt nạ có điểm phạt thấp nhất
            var best = 0
            var bestScore = Int.MAX_VALUE
            for (mask in 0 until 8) {
                apply(mask)
                format(mask)
                val s = penalty()
                if (s < bestScore) {
                    best = mask
                    bestScore = s
                }
                apply(mask)
            }
            apply(best)
            format(best)
            return m
        }

        fun finder(cx: Int, cy: Int) {
            for (dy in -4..4) for (dx in -4..4) {
                val d = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy))
                val x = cx + dx
                val y = cy + dy
                if (x in 0 until size && y in 0 until size) set(x, y, d != 2 && d != 4)
            }
        }

        fun align(cx: Int, cy: Int) {
            for (dy in -2..2) for (dx in -2..2) set(cx + dx, cy + dy, maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy)) != 1)
        }

        fun alignPositions(): IntArray {
            if (ver == 1) return IntArray(0)
            val n = ver / 7 + 2
            val step = (ver * 4 + n * 2 + 1) / (n * 2 - 2) * 2
            val r = IntArray(n)
            r[0] = 6
            var p = size - 7
            for (i in n - 1 downTo 1) {
                r[i] = p
                p -= step
            }
            return r
        }

        fun format(mask: Int) {
            val data = (0 shl 3) or mask // mức M: 00
            var rem = data
            repeat(10) { rem = (rem shl 1) xor ((rem ushr 9) * 0x537) }
            val bits = ((data shl 10) or rem) xor 0x5412
            fun bit(i: Int) = ((bits ushr i) and 1) != 0
            for (i in 0..5) set(8, i, bit(i))
            set(8, 7, bit(6)); set(8, 8, bit(7)); set(7, 8, bit(8))
            for (i in 9 until 15) set(14 - i, 8, bit(i))
            for (i in 0 until 8) set(size - 1 - i, 8, bit(i))
            for (i in 8 until 15) set(8, size - 15 + i, bit(i))
            set(8, size - 8, true)
        }

        fun version() {
            if (ver < 7) return
            var rem = ver
            repeat(12) { rem = (rem shl 1) xor ((rem ushr 11) * 0x1F25) }
            val bits = (ver shl 12) or rem
            for (i in 0 until 18) {
                val b = ((bits ushr i) and 1) != 0
                val a = size - 11 + i % 3
                val c = i / 3
                set(a, c, b)
                set(c, a, b)
            }
        }

        fun apply(mask: Int) {
            for (y in 0 until size) for (x in 0 until size) {
                if (fn[y][x]) continue
                val inv = when (mask) {
                    0 -> (x + y) % 2 == 0
                    1 -> y % 2 == 0
                    2 -> x % 3 == 0
                    3 -> (x + y) % 3 == 0
                    4 -> (x / 3 + y / 2) % 2 == 0
                    5 -> x * y % 2 + x * y % 3 == 0
                    6 -> (x * y % 2 + x * y % 3) % 2 == 0
                    else -> ((x + y) % 2 + x * y % 3) % 2 == 0
                }
                if (inv) m[y][x] = !m[y][x]
            }
        }

        fun penalty(): Int {
            var score = 0
            fun line(get: (Int) -> Boolean) {
                var run = 1
                for (i in 1 until size) {
                    if (get(i) == get(i - 1)) {
                        run++
                        if (run == 5) score += 3 else if (run > 5) score++
                    } else run = 1
                }
                // Mẫu giống ô định vị: 1011101 kèm 4 ô trắng một bên
                for (i in 0..size - 11) {
                    val a = (0 until 11).map { get(i + it) }
                    val p1 = listOf(true, false, true, true, true, false, true, false, false, false, false)
                    if (a == p1 || a == p1.reversed()) score += 40
                }
            }
            for (y in 0 until size) line { m[y][it] }
            for (x in 0 until size) line { m[it][x] }
            for (y in 0 until size - 1) for (x in 0 until size - 1) {
                val c = m[y][x]
                if (c == m[y][x + 1] && c == m[y + 1][x] && c == m[y + 1][x + 1]) score += 3
            }
            val dark = m.sumOf { r -> r.count { it } }
            val total = size * size
            val k = (kotlin.math.abs(dark * 20 - total * 10) + total - 1) / total - 1
            score += k * 10
            return score
        }
    }

    /** Ảnh mã QR (đen trên trắng, chừa lề 4 ô) cạnh ~[px] điểm ảnh; null nếu chuỗi quá dài. */
    fun bitmap(text: String, px: Int): Bitmap? {
        val m = encode(text) ?: return null
        val n = m.size + 8
        val scale = maxOf(1, px / n)
        val bmp = Bitmap.createBitmap(n * scale, n * scale, Bitmap.Config.RGB_565)
        bmp.eraseColor(Color.WHITE)
        for (y in m.indices) for (x in m.indices) {
            if (!m[y][x]) continue
            for (dy in 0 until scale) for (dx in 0 until scale) bmp.setPixel((x + 4) * scale + dx, (y + 4) * scale + dy, Color.BLACK)
        }
        return bmp
    }

    fun view(c: Context, text: String, px: Int): ImageView? {
        val b = bitmap(text, px) ?: return null
        return ImageView(c).apply {
            setImageBitmap(b)
            layoutParams = LinearLayout.LayoutParams(px, px)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
    }
}
