package vn.mtv.tv

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Calendar
import kotlin.math.ceil

/**
 * Lịch phát sóng của một kênh: hôm qua / hôm nay / ngày mai, chương trình đang phát tô màu, «Nhắc tôi» (5 phút trước giờ
 * phát) cho chương trình sắp tới. Như CBTVGuideViewController của bản Legacy.
 */
class TvGuideActivity : BaseActivity() {
    companion object {
        fun open(a: Activity, c: Channel, fromPlayer: Boolean) {
            a.startActivity(Intent(a, TvGuideActivity::class.java).putExtra("k", c.k).putExtra("player", fromPlayer))
        }
    }

    private var channel: Channel? = null
    private var all: List<Programme> = emptyList()
    private var rows: List<Programme> = emptyList()
    private var day = 0
    private val adapter = Adapter()
    private lateinit var list: RecyclerView
    private lateinit var dayButtons: List<TextView>
    private lateinit var nameView: TextView
    private lateinit var subView: TextView
    private lateinit var logo: ImageView
    private lateinit var emptyView: TextView
    private val ticker = object : Runnable {
        override fun run() {
            adapter.notifyDataSetChanged()
            mainHandler.postDelayed(this, 30_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(colorOf(R.color.bg))
            setPadding(dp(28), dp(18), dp(28), 0)
        }
        val head = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val box = FrameLayout(this).apply { setBackgroundResource(R.drawable.bg_logo_box) }
        logo = ImageView(this).apply { scaleType = ImageView.ScaleType.FIT_CENTER; setPadding(dp(4), dp(4), dp(4), dp(4)) }
        box.addView(logo, FrameLayout.LayoutParams(dp(96), dp(64)))
        head.addView(box, LinearLayout.LayoutParams(dp(96), dp(64)))
        val texts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), 0, dp(16), 0) }
        nameView = TextView(this).apply { textSize = 24f; setTextColor(colorOf(R.color.text)); setTypeface(typeface, android.graphics.Typeface.BOLD) }
        subView = TextView(this).apply { textSize = 15f; setTextColor(colorOf(R.color.text_dim)) }
        texts.addView(nameView)
        texts.addView(subView)
        head.addView(texts, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val watch = button("▶ Xem trực tiếp") { watch() }
        head.addView(watch)
        root.addView(head)
        val days = LinearLayout(this).apply { setPadding(0, dp(16), 0, dp(10)) }
        dayButtons = listOf("Hôm qua", "Hôm nay", "Ngày mai").mapIndexed { i, t ->
            button(t) { selectDay(i - 1) }.also { days.addView(it) }
        }
        root.addView(days)
        emptyView = TextView(this).apply {
            textSize = 17f
            setTextColor(colorOf(R.color.text_dim))
            setPadding(0, dp(20), 0, 0)
            visibility = View.GONE
        }
        root.addView(emptyView)
        list = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@TvGuideActivity)
            adapter = this@TvGuideActivity.adapter
            clipToPadding = false
            setPadding(0, 0, 0, dp(20))
        }
        root.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
        val key = intent.getStringExtra("k") ?: return finish()
        inBackground({
            val c = Iptv.store()?.byKey(key)
            c to (c?.let { Iptv.guide()?.programmes(it) } ?: emptyList())
        }) { r ->
            val (c, progs) = r.getOrNull() ?: (null to emptyList())
            if (c == null) {
                Toast.makeText(this, "Danh sách kênh đang chọn không có kênh này.", Toast.LENGTH_LONG).show()
                finish()
                return@inBackground
            }
            channel = c
            all = progs
            nameView.text = c.n
            subView.text = "Kênh ${c.num} · ${c.g}" + if (Iptv.isFavorite(c)) " · ★ Yêu thích" else ""
            ImageLoader.load(logo, c.logo, 200)
            selectDay(0)
            watch.requestFocus()
        }
    }

    override fun onResume() {
        super.onResume()
        mainHandler.postDelayed(ticker, 30_000)
    }

    override fun onPause() {
        super.onPause()
        mainHandler.removeCallbacks(ticker)
    }

    private fun button(t: String, click: () -> Unit) = TextView(this, null, 0, R.style.TvButton).apply {
        text = t
        setOnClickListener { click() }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginEnd = dp(12) }
    }

    private fun dayStart(offset: Int): Double {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        c.add(Calendar.DAY_OF_MONTH, offset)
        return c.timeInMillis / 1000.0
    }

    private fun selectDay(d: Int) {
        day = d
        dayButtons.forEachIndexed { i, b -> b.isActivated = i - 1 == d }
        val from = dayStart(d)
        val to = dayStart(d + 1)
        rows = all.filter { it.s >= from && it.s < to }
        adapter.notifyDataSetChanged()
        emptyView.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
        emptyView.text = when {
            all.isNotEmpty() -> "Ngày này chưa có lịch."
            Iptv.epgUrl != null -> "Kênh này chưa có lịch phát sóng (lịch của danh sách không có kênh này)."
            else -> "Danh sách kênh không ghi địa chỉ lịch phát sóng. Đặt trong Truyền hình › Cài đặt."
        }
        val now = nowSec()
        val at = rows.indexOfFirst { it.e > now }.coerceAtLeast(0)
        (list.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(at, 0)
    }

    private fun watch() {
        val c = channel ?: return
        if (intent.getBooleanExtra("player", false)) finish() else LiveActivity.playKey(this, c.k)
    }

    private inner class Adapter : RecyclerView.Adapter<Adapter.Holder>() {
        inner class Holder(v: LinearLayout) : RecyclerView.ViewHolder(v) {
            val title: TextView = v.getChildAt(0) as TextView
            val detail: TextView = v.getChildAt(1) as TextView
        }

        override fun getItemCount() = rows.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val v = LinearLayout(parent.context).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.bg_channel_row)
                isFocusable = true
                isClickable = true
                setPadding(dp(14), dp(10), dp(14), dp(10))
                layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                addView(TextView(parent.context).apply {
                    textSize = 18f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    isSingleLine = true
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    isDuplicateParentStateEnabled = true
                })
                addView(TextView(parent.context).apply { textSize = 14f })
            }
            val h = Holder(v)
            v.setOnClickListener {
                val i = h.bindingAdapterPosition
                val c = channel ?: return@setOnClickListener
                val p = rows.getOrNull(i) ?: return@setOnClickListener
                val now = nowSec()
                when {
                    p.s <= now && now < p.e -> watch()
                    p.s > now -> {
                        val on = IptvReminders.toggle(this@TvGuideActivity, c, p)
                        Toast.makeText(this@TvGuideActivity,
                            if (on) "Sẽ nhắc lúc ${IptvParse.clock(maxOf(now, p.s - 300))} (5 phút trước giờ phát)." else "Đã bỏ nhắc.",
                            Toast.LENGTH_SHORT).show()
                        notifyItemChanged(i)
                    }
                }
            }
            v.setOnFocusChangeListener { _, _ -> h.bindingAdapterPosition.takeIf { it >= 0 }?.let { notifyItemChanged(it) } }
            return h
        }

        override fun onBindViewHolder(h: Holder, i: Int) {
            val p = rows[i]
            val c = channel
            val now = nowSec()
            val live = p.s <= now && now < p.e
            val past = p.e <= now
            val focused = h.itemView.isFocused
            h.title.text = "${IptvParse.clock(p.s)}    ${p.t}"
            h.title.setTextColor(
                when {
                    focused -> colorOf(R.color.on_focus)
                    live -> colorOf(R.color.brand)
                    past -> colorOf(R.color.text_dim)
                    else -> colorOf(R.color.text)
                },
            )
            val remind = c != null && p.s > now && IptvReminders.has(c, p)
            h.detail.text = when {
                live -> "ĐANG PHÁT · còn ${maxOf(1, ceil((p.e - now) / 60).toInt())} phút · OK: xem"
                p.s > now -> "${IptvParse.clock(p.s)}–${IptvParse.clock(p.e)} · " + (if (remind) "🔔 Đã đặt nhắc (OK: bỏ)" else "OK: Nhắc tôi")
                else -> "${IptvParse.clock(p.s)}–${IptvParse.clock(p.e)}"
            }
            h.detail.setTextColor(if (focused) 0xCC14110A.toInt() else if (live) 0xFFFF6B6F.toInt() else colorOf(R.color.text_dim))
            h.itemView.isActivated = live
        }
    }
}
