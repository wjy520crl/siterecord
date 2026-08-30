package com.jiangxin.siterecord.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.jiangxin.siterecord.data.local.entity.Memo
import java.util.Calendar

object ReminderScheduler {
    fun schedule(context: Context, memo: Memo) {
        val deadline = memo.deadline ?: return
        val now = System.currentTimeMillis()
        var trigger = deadline - memo.remindLeadDays * 24L * 3600 * 1000
        if (trigger <= now) {
            // 提前量算出的时刻已过，但截止日尚未到：退化为截止日当天提醒。
            // 否则「今天下午 3 点提醒我回业主电话」这类同日待办会被静默跳过，一条提醒都不会响。
            if (deadline > now) trigger = deadline else return
        }
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("memoId", memo.id)
            putExtra("title", memo.title)
            putExtra("projectId", memo.projectId)
        }
        val pi = PendingIntent.getBroadcast(
            context, memo.id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (canScheduleExact(am)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, trigger, pi)
            }
        } else {
            // 未取得精确闹钟能力（Android 12+ 未授予，或被 ROM 拦截）：
            // 退化为非精确闹钟，系统可能延后若干分钟，但至少不会完全不响。
            am.set(AlarmManager.RTC_WAKEUP, trigger, pi)
        }
    }

    private fun canScheduleExact(am: AlarmManager): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return try {
            am.canScheduleExactAlarms()
        } catch (e: SecurityException) {
            false
        }
    }

    fun cancel(context: Context, memoId: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context, memoId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(pi)
    }
}
