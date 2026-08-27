package com.jiangxin.siterecord.backup

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.jiangxin.siterecord.SiteRecordApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupExporter {
    fun exportJsonUri(context: Context): Uri? {
        val app = context.applicationContext as SiteRecordApp
        val projects = runBlocking { app.projectRepository.observeAll().first() }
        val inspections = runBlocking { app.inspectionRepository.observeAll().first() }
        val items = runBlocking { app.inspectionRepository.observeAllItems().first() }
        val memos = runBlocking { app.memoRepository.observeAll().first() }
        val payload = mapOf(
            "projects" to projects,
            "inspections" to inspections,
            "inspectionItems" to items,
            "memos" to memos
        )
        val json = Gson().toJson(payload)
        val outDir = File(context.filesDir, "backups")
        outDir.mkdirs()
        val file = File(outDir, "siterecord_${stamp()}.json")
        file.writeText(json)
        return FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    }

    fun exportCsvUri(context: Context): Uri? {
        val app = context.applicationContext as SiteRecordApp
        val memos = runBlocking { app.memoRepository.observeAll().first() }
        val sb = StringBuilder()
        sb.appendLine("id,projectId,source,title,detail,importance,status,deadline,remindLeadDays,photographer")
        memos.forEach { m ->
            sb.appendLine(
                "${m.id},${m.projectId},${m.source},${csv(m.title)},${csv(m.detail)},${m.importance},${m.status},${m.deadline ?: ""},${m.remindLeadDays},${csv(m.photographer)}"
            )
        }
        val outDir = File(context.filesDir, "backups")
        outDir.mkdirs()
        val file = File(outDir, "memos_${stamp()}.csv")
        file.writeText(sb.toString())
        return FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    }

    private fun csv(s: String) = s.replace("\"", "\"\"").let { "\"$it\"" }
    private fun stamp() = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(Date())
}
