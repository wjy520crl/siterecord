package com.jiangxin.siterecord.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 原先是两个共享的 top-level SimpleDateFormat 实例：SimpleDateFormat 不是线程安全的，
// 首页、列表、详情多处并发调用时会输出错乱日期甚至抛 NumberFormatException。
// 这里每次新建实例——创建成本远低于排查一次日期错乱的代价。

fun formatDateTime(ts: Long?): String {
    if (ts == null || ts == 0L) return ""
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(ts))
}

fun formatDate(ts: Long?): String {
    if (ts == null || ts == 0L) return ""
    return SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date(ts))
}

fun isOverdue(ts: Long?): Boolean {
    return ts != null && ts < System.currentTimeMillis()
}
