package com.jiangxin.siterecord.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jiangxin.siterecord.SiteRecordApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 开机后重建全部待办提醒。
 *
 * AlarmManager 的闹钟在设备重启后会被系统清空。不重建的话，
 * 老板重启一次手机，此前设的所有备案提醒就全部静默消失——
 * 这恰恰是本 App 最不能容忍的失效方式，所以必须补上。
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as SiteRecordApp
                // 只重建未完成的：已完成/已反馈业主的备案不该再响，
                // 否则老板重启一次手机，早就办完的事又全部冒出来
                val memos = app.memoRepository.observePending().first()
                memos.forEach { memo -> ReminderScheduler.schedule(context, memo) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
