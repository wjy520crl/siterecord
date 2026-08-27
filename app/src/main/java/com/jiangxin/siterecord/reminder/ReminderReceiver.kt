package com.jiangxin.siterecord.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val memoId = intent.getLongExtra("memoId", 0L)
        val title = intent.getStringExtra("title") ?: "备案提醒"
        val projectId = intent.getLongExtra("projectId", 0L)
        NotificationHelper.sendReminder(context, memoId, title, projectId)
    }
}
