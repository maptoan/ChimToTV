package vn.mtv.tv

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/**
 * Cài đặt › Truyền hình: các danh sách kênh (dùng / tải lại / đổi tên / xoá, thêm), lịch phát sóng (địa chỉ EPG, tải lại),
 * phát (ẩn kênh máy không phát được, tự sang nguồn khác, xoá dấu «không phát được»). Như CBTVSettingsViewController (Legacy).
 */
class TvSettingsActivity : BaseActivity() {
    private lateinit var body: LinearLayout
    private val changed: () -> Unit = { build() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(40), dp(20), dp(40), dp(30))
        }
        val scroll = ScrollView(this).apply {
            setBackgroundColor(colorOf(R.color.bg))
            addView(body)
        }
        setContentView(scroll)
        Iptv.listen(changed)
        build()
    }

    override fun onDestroy() {
        Iptv.unlisten(changed)
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        IptvImport.onResult(this, requestCode, resultCode == RESULT_OK, data)
    }

    private fun build() {
        val focusedTag = currentFocus?.tag
        body.removeAllViews()
        body.addView(TextView(this).apply {
            text = "Truyền hình — Cài đặt"
            textSize = 26f
            setTextColor(colorOf(R.color.text))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        section("Danh sách kênh")
        val active = Iptv.activeList?.id
        for (l in Iptv.lists) {
            val detail = when {
                l.err != null -> "Lỗi: ${l.err}"
                l.updated == 0L -> "Chưa tải"
                else -> "${l.count} kênh · cập nhật ${IptvParse.clock(l.updated / 1000.0)}"
            } + " · " + (l.url ?: "tệp «${l.src ?: l.file}»")
            row((if (l.id == active) "✓ " else "") + l.name, detail, "list:${l.id}") { listMenu(l) }
        }
        row("+ Thêm danh sách (địa chỉ / tệp M3U)", null, "add") { IptvImport.ask(this) { build() } }
        note(
            "App không kèm sẵn kênh. Thêm danh sách M3U / M3U8 mà bạn có quyền xem: dán địa chỉ, gửi từ điện thoại / máy tính " +
                "qua Wi-Fi, hoặc chọn tệp .m3u; app đọc nhóm, logo, đài radio và địa chỉ lịch phát sóng ghi trong danh sách. " +
                "Danh sách theo địa chỉ tự tải lại khi cũ; danh sách từ tệp thì thêm lại khi có tệp mới.",
        )
        section("Lịch phát sóng")
        val epgDetail = when {
            Iptv.epgOverride != null -> "Tự đặt: ${Iptv.epgOverride}"
            Iptv.activeList?.epg != null -> "Lấy từ danh sách: ${Iptv.activeList?.epg}"
            else -> "Không có"
        }
        row("Địa chỉ EPG (XMLTV)", epgDetail, "epg") { editEpg() }
        val g = Iptv.guide()
        val guideDetail = when {
            Iptv.guideLoading -> "Đang tải…"
            g != null -> "${g.channels} kênh có lịch · tải lúc ${IptvParse.clock(g.fetched / 1000.0)}"
            else -> "Chưa tải"
        }
        row("Tải lại lịch ngay", guideDetail, "guide") {
            Iptv.refreshGuide(true) { err -> toast(err ?: "Đã tải lịch: ${Iptv.guide()?.channels ?: 0} kênh có lịch.") }
            build()
        }
        note(
            "Lịch phát sóng (EPG, dạng XMLTV — .xml hoặc .xml.gz) lấy theo địa chỉ ghi trong danh sách; danh sách không ghi thì " +
                "đặt ở đây. App chỉ giữ lịch của các kênh trong danh sách, từ hôm qua tới ngày mai.",
        )
        section("Phát")
        row("Ẩn kênh máy không phát được", if (Iptv.hideBad) "Bật" else "Tắt", "hide") { Iptv.hideBad = !Iptv.hideBad }
        row("Nguồn lỗi thì tự sang nguồn khác", if (Iptv.autoSource) "Bật" else "Tắt", "auto") {
            Iptv.autoSource = !Iptv.autoSource
            build()
        }
        row("Xoá dấu «không phát được»", null, "clear") {
            Iptv.clearBad()
            toast("Đã xoá — lần mở sau app thử lại các kênh đó.")
        }
        note(
            "Kênh máy không giải mã được (vd H.265 trên TV box đời cũ, khổ lớn hơn màn hình máy) được ghi là «không phát được» " +
                "sau lần mở đầu tiên. Danh sách có nhiều nguồn cho một kênh thì nguồn lỗi tự sang nguồn sau.",
        )
        val target = body.findViewWithTag<android.view.View>(focusedTag) ?: body.findViewWithTag("add")
        target?.post { target.focusForRemote() }
    }

    private fun section(t: String) = body.addView(TextView(this, null, 0, R.style.SettingsLabel).apply {
        text = t
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(22); bottomMargin = dp(8)
        }
    })

    private fun note(t: String) = body.addView(TextView(this).apply {
        text = t
        textSize = 14f
        setTextColor(colorOf(R.color.text_dim))
        setPadding(dp(4), dp(8), 0, 0)
    })

    private fun row(title: String, detail: String?, tag: String, click: () -> Unit) {
        val v = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_channel_row)
            isFocusable = true
            isClickable = true
            this.tag = tag
            setPadding(dp(14), dp(10), dp(14), dp(10))
            gravity = Gravity.CENTER_VERTICAL
            setOnClickListener { click() }
        }
        val t = TextView(this).apply {
            text = title
            textSize = 18f
            setTextColor(colorStateOf(R.color.text_on_focus))
            isDuplicateParentStateEnabled = true
        }
        v.addView(t)
        if (detail != null) {
            val d = TextView(this).apply {
                text = detail
                textSize = 14f
                isSingleLine = true
                ellipsize = android.text.TextUtils.TruncateAt.END
                setTextColor(colorOf(R.color.text_dim))
            }
            v.addView(d)
            v.setOnFocusChangeListener { _, has -> d.setTextColor(if (has) 0xCC14110A.toInt() else colorOf(R.color.text_dim)) }
        }
        body.addView(v, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(4) })
    }

    private fun listMenu(l: IptvList) {
        val items = listOf("Dùng danh sách này", "Tải lại", "Đổi tên…", "Xoá danh sách")
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("${l.name}\n${l.url ?: "Tệp «${l.src ?: "?"}»"}")
            .setItems(items.toTypedArray()) { _, w ->
                when (w) {
                    0 -> {
                        Iptv.setActive(l.id)
                        Iptv.refresh(false) { err -> if (err != null) toast(err) }
                    }
                    1 -> {
                        Iptv.setActive(l.id)
                        Iptv.refresh(true) { err -> toast(err ?: "Đã tải ${Iptv.activeList?.count ?: 0} kênh.") }
                    }
                    2 -> rename(l)
                    else -> AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                        .setTitle("Xoá danh sách «${l.name}»?")
                        .setPositiveButton("Xoá") { _, _ -> Iptv.removeList(l.id) }
                        .setNegativeButton("Huỷ", null)
                        .show().getButton(AlertDialog.BUTTON_NEGATIVE)?.requestFocus()
                }
            }
            .setNegativeButton("Huỷ", null)
            .show()
    }

    private fun rename(l: IptvList) {
        val e = EditText(this).apply {
            setText(l.name)
            isSingleLine = true
            selectAll()
        }
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Đổi tên danh sách")
            .setView(e)
            .setPositiveButton("Lưu") { _, _ -> Iptv.renameList(l.id, e.text.toString()) }
            .setNegativeButton("Huỷ", null)
            .show()
        e.requestFocus()
        e.openKeyboard()
    }

    private fun editEpg() {
        val e = EditText(this).apply {
            setText(Iptv.epgOverride ?: "")
            hint = Iptv.activeList?.epg ?: "http://…/epg.xml.gz"
            isSingleLine = true
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
        }
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Địa chỉ lịch phát sóng")
            .setMessage("XMLTV (.xml / .xml.gz). Để trống = lấy từ danh sách kênh.")
            .setView(e)
            .setPositiveButton("Lưu") { _, _ ->
                Iptv.epgOverride = e.text.toString()
                Iptv.refreshGuide(true) { err -> if (err != null) toast(err) }
                build()
            }
            .setNegativeButton("Huỷ", null)
            .show()
        e.requestFocus()
        e.openKeyboard()
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()
}
