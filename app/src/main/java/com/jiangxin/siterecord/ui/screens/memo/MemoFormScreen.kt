package com.jiangxin.siterecord.ui.screens.memo

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jiangxin.siterecord.SiteRecordApp
import com.jiangxin.siterecord.camera.LocationHelper
import com.jiangxin.siterecord.camera.WatermarkCamera
import com.jiangxin.siterecord.data.local.entity.Importance
import com.jiangxin.siterecord.data.local.entity.Memo
import com.jiangxin.siterecord.data.local.entity.MemoSource
import com.jiangxin.siterecord.data.local.entity.MemoStatus
import com.jiangxin.siterecord.reminder.ReminderScheduler
import com.jiangxin.siterecord.ui.components.PhotoThumb
import com.jiangxin.siterecord.util.formatDate
import java.io.File
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoFormScreen(navController: androidx.navigation.NavController, projectId: Long, memoId: Long?) {
    val ctx = LocalContext.current.applicationContext as SiteRecordApp
    val repo = ctx.memoRepository
    val projects by ctx.projectRepository.observeAll().collectAsStateWithLifecycle()
    var effectiveProjectId by remember { mutableStateOf(projectId) }
    var projectExpanded by remember { mutableStateOf(false) }
    val nameOf: (Long) -> String = { id -> projects.firstOrNull { it.id == id }?.name ?: "" }

    var source by remember { mutableStateOf(MemoSource.业主) }
    var title by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }
    var importance by remember { mutableStateOf(Importance.普通) }
    var status by remember { mutableStateOf(MemoStatus.待办) }
    var deadline by remember { mutableStateOf<Long?>(null) }
    var lead by remember { mutableStateOf(1) }
    var photos by remember { mutableStateOf<List<String>>(emptyList()) }
    var photographer by remember { mutableStateOf("小汪") }
    var tempFile by remember { mutableStateOf<File?>(null) }

    val leadOptions = listOf(0 to "当天", 1 to "提前1天", 3 to "提前3天", 7 to "提前7天")

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok && tempFile != null) {
            val loc = LocationHelper.getLocationText(ctx)
            val out = WatermarkCamera.addWatermark(ctx, tempFile!!, loc, photographer.ifBlank { "小汪" })
            tempFile!!.delete()
            if (out != null) photos = photos + out.absolutePath
        }
    }

    LaunchedEffect(memoId) {
        if (memoId != null) {
            val m = repo.getById(memoId)
            if (m != null) {
                source = m.source; title = m.title; detail = m.detail; importance = m.importance
                status = m.status; deadline = m.deadline; lead = m.remindLeadDays
                photos = m.photos; photographer = m.photographer
                effectiveProjectId = m.projectId
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (memoId == null) "新建备案" else "编辑备案") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                },
                actions = {
                    TextButton(onClick = {
                        val memo = Memo(
                            id = memoId ?: 0,
                            projectId = effectiveProjectId,
                            source = source,
                            title = title,
                            detail = detail,
                            importance = importance,
                            status = status,
                            deadline = deadline,
                            remindLeadDays = lead,
                            photos = photos,
                            photographer = photographer
                        )
                        val savedId = if (memoId == null) repo.insert(memo) else { repo.update(memo); memoId }
                        if (deadline != null) {
                            ReminderScheduler.schedule(LocalContext.current, memo.copy(id = savedId))
                        }
                        navController.popBackStack()
                    }) { Text("保存") }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(12.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (projectId == 0L) {
                Text("关联项目*", style = MaterialTheme.typography.labelSmall)
                Box {
                    OutlinedTextField(
                        value = nameOf(effectiveProjectId).ifEmpty { "请选择项目" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("项目") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { IconButton(onClick = { projectExpanded = !projectExpanded }) { Icon(Icons.Filled.ArrowDropDown, null) } }
                    )
                    DropdownMenu(projectExpanded, { projectExpanded = false }) {
                        projects.forEach { p ->
                            DropdownMenuItem(onClick = { effectiveProjectId = p.id; projectExpanded = false }, text = { Text(p.name) })
                        }
                    }
                }
            }
            OutlinedTextField(title, { title = it }, label = { Text("主题*") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(detail, { detail = it }, label = { Text("详情") }, modifier = Modifier.fillMaxWidth().height(120.dp))
            Text("来源", style = MaterialTheme.typography.labelSmall)
            EnumChips(MemoSource.entries.toList(), source) { source = it }
            Text("重要度", style = MaterialTheme.typography.labelSmall)
            EnumChips(Importance.entries.toList(), importance) { importance = it }
            Text("状态", style = MaterialTheme.typography.labelSmall)
            EnumChips(MemoStatus.entries.toList(), status) { status = it }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("截止：", style = MaterialTheme.typography.labelSmall)
                Text(formatDate(deadline), style = MaterialTheme.typography.bodyMedium)
                DateField("选择截止日", deadline) { deadline = it }
            }
            Text("提醒提前", style = MaterialTheme.typography.labelSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(leadOptions.size) { i ->
                    val (v, l) = leadOptions[i]
                    FilterChip(selected = lead == v, onClick = { lead = v }, label = { Text(l) })
                }
            }
            OutlinedTextField(photographer, { photographer = it }, label = { Text("拍照人") }, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("照片", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    val f = File(ctx.cacheDir, "cap_${System.currentTimeMillis()}.jpg")
                    val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", f)
                    tempFile = f
                    launcher.launch(uri)
                }) { Icon(Icons.Filled.CameraAlt, "拍照") }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(photos.size) { i ->
                    Row {
                        PhotoThumb(photos[i], size = 72)
                        androidx.compose.material3.IconButton(onClick = { photos = photos - photos[i] }) {
                            Icon(Icons.Filled.Close, "删除")
                        }
                    }
                }
            }
            Text("语音备忘（占位，后续版本实现）", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun <T : Enum<T>> EnumChips(options: List<T>, selected: T, onSelect: (T) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(options.size) { idx ->
            val opt = options[idx]
            FilterChip(selected = opt == selected, onClick = { onSelect(opt) }, label = { Text(opt.name) })
        }
    }
}

@Composable
private fun DateField(label: String, value: Long?, onPick: (Long?) -> Unit) {
    val context = LocalContext.current
    val cal = Calendar.getInstance().apply { timeInMillis = value ?: System.currentTimeMillis() }
    val dialog = android.app.DatePickerDialog(
        context,
        { _: android.widget.DatePicker, y, m, d -> cal.set(y, m, d); onPick(cal.timeInMillis) },
        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
    )
    androidx.compose.material3.TextButton(onClick = { dialog.show() }) { Text(label) }
}
