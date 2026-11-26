package vn.mtv.tv

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/**
 * Cài app lên tivi khác bằng app Downloader (AFTVnews): người mới chỉ cần cài Downloader trên tivi rồi nhập CODE. APK_URL:
 * link cố định luôn tải bản TV mới nhất (mỗi release TV đính kèm tệp tên MTV.apk, xem release.ps1). Đổi mã / link thì sửa đủ
 * năm chỗ: ở đây, bản iPad (Invite.swift), bản Windows (Invite.cs), release.ps1 và README (mục «Cài trên Android TV bằng Downloader»).
 */
object Invite {
    const val TITLE = "Mời bạn bè cài app"
    const val CODE = "1806154"
    const val SHORT_URL = "aftv.news/$CODE"
    const val APK_URL = "https://github.com/maptoan/MTV/releases/latest/download/MTV.apk"

    /** Trang hướng dẫn tiếng Việt (mục Downloader của README) — mã QR trên màn mở trang này. */
    const val GUIDE_URL = "https://github.com/maptoan/MTV/blob/main/README.md#downloader"

    /** Tin nhắn gửi cho người quen (nút «Gửi hướng dẫn…» trên điện thoại / máy tính bảng). */
    val MESSAGE = """
        Cài MTV trên tivi Android:
        1. Trên tivi mở CH Play, cài app Downloader (của AFTVnews).
        2. Mở Downloader, nhập $CODE rồi bấm Go.
        3. Tải xong chọn Install. Lần đầu tivi hỏi cho phép Downloader cài ứng dụng thì cho phép.
        Cài xong app tự cập nhật các bản sau.
        Hướng dẫn: $GUIDE_URL
    """.trimIndent()

    /**
     * Mã QR của GUIDE_URL, vẽ sẵn (mức sửa lỗi M; mỗi chuỗi một hàng, '1' = ô đen) — app không cần thư viện QR. Đổi GUIDE_URL
     * thì chạy InviteTest: test dựng lại bằng ZXing, báo lệch và in ra các hàng mới để dán vào đây; test thứ hai đọc lại mã
     * như điện thoại.
     */
    val QR: List<String> = listOf(
        "111111101001110011000001001111111",
        "100000100010001001010010001000001",
        "101110100110100101100111101011101",
        "101110101001000100100101001011101",
        "101110101110111110000000001011101",
        "100000101101100010111100001000001",
        "111111101010101010101010101111111",
        "000000001110000101001111000000000",
        "100010111010101011011100011111001",
        "101110011001101010000101010001110",
        "101110101111011100101101001001010",
        "100110011011010101101101010000000",
        "001111110001110101001011101011000",
        "010011001101111110100100010101000",
        "100001100011101111101001111010110",
        "110001011110011101010111111110000",
        "101101110110101001011110101010010",
        "001000011011000011100001010001110",
        "000001101111001100101101011101010",
        "011101001010101001011101101100001",
        "000100101100100101001100111011011",
        "111001011111000111101000010100110",
        "000011100110010100100011010011110",
        "001111011000010111101100011001010",
        "111001101010100001011110111110000",
        "000000001100000101101101100010110",
        "111111101100111111101000101011010",
        "100000100000011001001110100010011",
        "101110101110111101001110111111000",
        "101110100101110110100101101111000",
        "101110100100001101001001101110100",
        "100000100000100001101100011000000",
        "111111101110010101001111100000001",
    )
}

/** Vẽ Invite.QR: nền trắng, lề trắng 4 ô quanh mã (máy đọc cần), mỗi ô một số nguyên điểm ảnh cho nét. */
class QrView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val dark = Paint().apply { color = Color.BLACK; isAntiAlias = false }
    private val rows = Invite.QR

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        super.onMeasure(widthSpec, heightSpec)
        val side = minOf(measuredWidth, measuredHeight)
        setMeasuredDimension(side, side)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.WHITE)
        if (rows.isEmpty()) return
        val cells = rows.size + 2 * QUIET
        val cell = minOf(width, height) / cells
        if (cell <= 0) return
        val left = (width - cell * rows.size) / 2f
        val top = (height - cell * rows.size) / 2f
        for ((y, row) in rows.withIndex()) {
            for ((x, c) in row.withIndex()) {
                if (c == '1') canvas.drawRect(left + x * cell, top + y * cell, left + (x + 1) * cell, top + (y + 1) * cell, dark)
            }
        }
    }

    private companion object {
        const val QUIET = 4
    }
}

/**
 * Màn «Mời bạn bè cài app» (menu bên trái và Cài đặt → Liên hệ): mã Downloader thật to, ba bước cài, mã QR mở trang hướng
 * dẫn để khách chụp bằng điện thoại. Chỉ có nút «Đóng»; Back cũng đóng.
 */
class InviteActivity : BaseActivity() {
    companion object {
        fun open(ctx: Context) {
            ctx.startActivity(Intent(ctx, InviteActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_invite)
        findViewById<TextView>(R.id.inviteCode).text = Invite.CODE.chunked(1).joinToString(" ")
        findViewById<TextView>(R.id.inviteLinks).text =
            "Trên trình duyệt: ${Invite.SHORT_URL}\nLink tải trực tiếp: ${Invite.APK_URL.removePrefix("https://")}"
        findViewById<TextView>(R.id.btnInviteShare).apply {
            visibility = if (Screen.isTv(this@InviteActivity)) View.GONE else View.VISIBLE
            setOnClickListener { share() }
        }
        findViewById<TextView>(R.id.btnInviteClose).apply {
            setOnClickListener { finish() }
            requestFocus()
        }
        arrange()
    }

    /** Điện thoại / máy tính bảng: gửi cách cài (Invite.MESSAGE) qua app nhắn tin người xem chọn. */
    private fun share() {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, Invite.MESSAGE)
        try {
            startActivity(Intent.createChooser(send, "Gửi hướng dẫn cài app"))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Máy không có app nào để gửi — đọc mã ${Invite.CODE} cho bạn bè.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        arrange()
    }

    /** Màn ngang (TV, máy tính bảng): các bước bên trái, mã QR bên phải. Điện thoại cầm dọc: xếp dọc, lề và chữ nhỏ hơn. */
    private fun arrange() {
        val narrow = Screen.narrow(this)
        val d = resources.displayMetrics.density
        val pad = ((if (narrow) 16 else 48) * d).toInt()
        findViewById<View>(R.id.inviteRoot).setPadding(pad, ((if (narrow) 16 else 28) * d).toInt(), pad, ((if (narrow) 16 else 20) * d).toInt())
        findViewById<LinearLayout>(R.id.inviteContent).orientation = if (narrow) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
        findViewById<TextView>(R.id.inviteCode).setTextSize(TypedValue.COMPLEX_UNIT_SP, if (narrow) 44f else 64f)
        // Màn hẹp: dòng link dài chiếm hết đáy màn — bỏ (tin nhắn «Gửi hướng dẫn…» đã có link); nút «Đóng» co lại cho nút
        // «Gửi hướng dẫn…» đủ chỗ hiện hết chữ
        findViewById<View>(R.id.inviteLinks).visibility = if (narrow) View.GONE else View.VISIBLE
        findViewById<TextView>(R.id.btnInviteClose).apply {
            // android:minWidth trong layout đặt cả hai mức tối thiểu (của TextView và của View) — phải hạ cả hai
            val w = ((if (narrow) 0 else 140) * d).toInt()
            minWidth = w
            minimumWidth = w
        }
        findViewById<View>(R.id.inviteSteps).layoutParams = LinearLayout.LayoutParams(
            if (narrow) LinearLayout.LayoutParams.MATCH_PARENT else 0, LinearLayout.LayoutParams.WRAP_CONTENT, if (narrow) 0f else 1f,
        )
        findViewById<View>(R.id.inviteQrBox).layoutParams = LinearLayout.LayoutParams(
            if (narrow) LinearLayout.LayoutParams.MATCH_PARENT else (290 * d).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            if (narrow) topMargin = (20 * d).toInt() else marginStart = (40 * d).toInt()
        }
    }
}
