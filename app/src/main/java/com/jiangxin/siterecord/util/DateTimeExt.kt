package com.jiangxin.siterecord.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dtFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
private val dFormat = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)

fun formatDateTime(ts: Long?): String {
    if (ts == null || ts == 0L) return ""
    return dtFormat.format(Date(ts))
}

fun formatDate(ts: Long?): String {
    if (ts == null || ts == 0L) return ""
    return dFormat.format(Date(ts))
}

fun isOverdue(ts: Long?): Boolean {
    return ts != null && ts < System.currentTimeMillis()
}
