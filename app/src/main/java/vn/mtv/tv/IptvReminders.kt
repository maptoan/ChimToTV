package vn.mtv.tv

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import org.json.JSONArray
import org.json.JSONObject

/**
 * Nhắc giờ phát (lịch phát sóng: 5 phút trước giờ phát) và nhắc lúc bóng lăn (kênh trận đấu). App đang mở: hộp «Xem ngay» trên
 * màn đang hiện (tivi không hiện thông báo của hệ thống); app ở nền: thông báo của máy (điện thoại). Lưu ở khoá iptv_reminders:
 * [{k (khoá kênh), n (tên kênh), t (tên chương trình / trận), s (giờ phát, giây), at (giờ nhắc, ms), kick (nhắc bóng lăn)}].
 */
object IptvReminders {
    private const val CHANNEL = "iptv_reminders"
    private var resumed: Activity? = null
    private var showing = false

    private fun all(): List<JSONObject> = runCatching {
        val a = JSONArray(Prefs.iptvGet("iptv_reminders") ?: "[]")
        (0 until a.length()).map(a::getJSONObject)
    }.getOrDefault(emptyList())

    private fun save(list: List<JSONObject>, c: Context) {
        Prefs.iptvSet("iptv_reminders", JSONArray(list).toString())
        schedule(c)
    }

    fun hasKick(c: Channel) = all().any { it.optString("k") == c.k && it.optBoolean("kick") }

    fun has(c: Channel, p: Programme) = all().any { it.optString("k") == c.k && !it.optBoolean("kick") && kotlin.math.abs(it.optDouble("s") - p.s) < 1 }

    /** true = vừa đặt nhắc lúc bóng lăn, false = vừa bỏ (hoặc trận đã bắt đầu). */
    fun toggleKick(a: Activity, c: Channel): Boolean {
        val list = all()
        if (list.any { it.optString("k") == c.k && it.optBoolean("kick") }) {
            save(list.filterNot { it.optString("k") == c.k && it.optBoolean("kick") }, a)
            return false
        }
        if (c.kick <= nowSec()) return false
        askPermission(a)
        save(list + JSONObject().put("k", c.k).put("n", c.n).put("t", c.n).put("g", c.g).put("s", c.kick)
            .put("at", (c.kick * 1000).toLong()).put("kick", true), a)
        TvLog.i("Truyền hình: nhắc lúc bóng lăn ${c.n} (${IptvParse.clock(c.kick)})")
        return true
    }

    /** «Nhắc tôi» của lịch phát sóng: 5 phút trước giờ phát. true = vừa đặt. */
    fun toggle(a: Activity, c: Channel, p: Programme): Boolean {
        val list = all()
        if (has(c, p)) {
            save(list.filterNot { it.optString("k") == c.k && !it.optBoolean("kick") && kotlin.math.abs(it.optDouble("s") - p.s) < 1 }, a)
            return false
        }
        askPermission(a)
        val at = maxOf(System.currentTimeMillis() + 5000, ((p.s - 300) * 1000).toLong())
        save(list + JSONObject().put("k", c.k).put("n", c.n).put("t", p.t).put("s", p.s).put("at", at), a)
        TvLog.i("Truyền hình: nhắc «${p.t}» ${c.n} lúc ${IptvParse.clock(p.s)}")
        return true
    }

    /** Điện thoại Android 13+: xin quyền thông báo (nhắc khi app ở nền). Tivi chỉ nhắc trong app. */
    private fun askPermission(a: Activity) {
        if (Build.VERSION.SDK_INT >= 33 && !Screen.isTv(a) &&
            a.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            a.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 41)
        }
    }

    private fun message(r: JSONObject) = if (r.optBoolean("kick")) "Bóng lăn: ${r.optString("n")} — ${r.optString("g", "Truyền hình")}"
    else "${r.optString("t")} lúc ${IptvParse.clock(r.optDouble("s"))} trên ${r.optString("n")}"

    // ---- Nhắc trong app ----

    /** Theo dõi màn đang hiện; mỗi 15 giây xem có nhắc nào tới giờ. */
    fun attach(app: Application) {
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(a: Activity) {
                resumed = a
                check()
            }

            override fun onActivityPaused(a: Activity) {
                if (resumed === a) resumed = null
            }

            override fun onActivityCreated(a: Activity, b: Bundle?) = Unit
            override fun onActivityStarted(a: Activity) = Unit
            override fun onActivityStopped(a: Activity) = Unit
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) = Unit
            override fun onActivityDestroyed(a: Activity) = Unit
        })
        val loop = object : Runnable {
            override fun run() {
                check()
                mainHandler.postDelayed(this, 15_000)
            }
        }
        mainHandler.postDelayed(loop, 15_000)
    }

    private fun check() {
        val a = resumed ?: return
        if (showing || a.isFinishing) return
        // Màn miễn trừ trách nhiệm / «Ai đang xem?» đang chặn cửa: để lần sau (nhắc chưa lấy ra khỏi danh sách) — không thì
        // «Xem ngay» mở được kênh đè lên màn chọn người xem, qua mặt mã PIN
        if (a is DisclaimerActivity || Disclaimer.needed() || (a is MainActivity && a.pickerShown)) return
        val now = System.currentTimeMillis()
        val list = all()
        val due = list.firstOrNull { it.optLong("at") <= now } ?: return
        // Bỏ nhắc quá cũ (máy tắt lúc tới giờ): chương trình đã qua 30 phút
        val rest = list.filter { it !== due }
        Prefs.iptvSet("iptv_reminders", JSONArray(rest).toString())
        schedule(a)
        if (now / 1000.0 > due.optDouble("s") + 30 * 60) return
        showing = true
        val key = due.optString("k")
        AlertDialog.Builder(a, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(if (due.optBoolean("kick")) "Bóng lăn" else "Sắp phát")
            .setMessage(message(due))
            .setPositiveButton("Xem ngay") { _, _ -> LiveActivity.playKey(a, key) }
            .setNegativeButton("Đóng", null)
            .setOnDismissListener { showing = false }
            .show()
    }

    // ---- Thông báo của máy (app ở nền) ----

    /** Đặt báo thức của máy cho lần nhắc gần nhất (không chính xác từng giây — máy gom báo thức để đỡ tốn pin). */
    fun schedule(c: Context) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(c, 41, Intent(c, ReminderReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val next = all().minOfOrNull { it.optLong("at") }
        if (next == null) {
            am.cancel(pi)
            return
        }
        if (Build.VERSION.SDK_INT >= 23) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi) else am.set(AlarmManager.RTC_WAKEUP, next, pi)
    }

    /** Báo thức tới: app không ở trước màn hình thì hiện thông báo; chạm thông báo → mở kênh. */
    fun fire(c: Context) {
        if (resumed != null) {
            mainHandler.post { check() }
            return
        }
        val now = System.currentTimeMillis()
        val list = all()
        val due = list.filter { it.optLong("at") <= now }
        Prefs.iptvSet("iptv_reminders", JSONArray(list.filter { it.optLong("at") > now }).toString())
        schedule(c)
        val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Nhắc giờ phát (Truyền hình)", NotificationManager.IMPORTANCE_HIGH))
        }
        for ((i, r) in due.withIndex()) {
            if (now / 1000.0 > r.optDouble("s") + 30 * 60) continue
            // Mở qua màn chính: qua màn miễn trừ trách nhiệm / «Ai đang xem?» (mã PIN) rồi mới mở kênh (MainActivity.pendingPlayKey)
            val open = Intent(c, MainActivity::class.java).putExtra("play_key", r.optString("k"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val pi = PendingIntent.getActivity(c, 100 + i, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val b = if (Build.VERSION.SDK_INT >= 26) android.app.Notification.Builder(c, CHANNEL) else
                @Suppress("DEPRECATION") android.app.Notification.Builder(c)
            b.setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(if (r.optBoolean("kick")) "Bóng lăn" else "Sắp phát")
                .setContentText(message(r))
                .setAutoCancel(true)
                .setContentIntent(pi)
            runCatching { nm.notify(4100 + i, b.build()) }
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) = IptvReminders.fire(c)
}
