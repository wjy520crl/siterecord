package com.jiangxin.siterecord.ui.screens.memo

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.runtime.collectAsState
import com.jiangxin.siterecord.SiteRecordApp
import com.jiangxin.siterecord.audio.VoiceRecorder
import com.jiangxin.siterecord.camera.LocationHelper
import com.jiangxin.siterecord.camera.WatermarkCamera
import com.jiangxin.siterecord.data.local.entity.Importance
import com.jiangxin.siterecord.data.local.entity.Memo
import com.jiangxin.siterecord.data.local.entity.MemoSource
import com.jiangxin.siterecord.data.local.entity.MemoStatus
import com.jiangxin.siterecord.reminder.ReminderScheduler
import com.jiangxin.siterecord.ui.components.ConfirmDeleteDialog
import com.jiangxin.siterecord.ui.components.PhotoThumb
import com.jiangxin.siterecord.util.formatDate
import java.io.File
import java.util.Calendar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoFormScreen(navController: androidx.navigation.NavController, projectId: Long, memoId: Long?) {
    val ctx = LocalContext.current.applicationContext as SiteRecordApp
    val repo = ctx.memoRepository
    val projects by ctx.projectRepository.observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var effectiveProjectId by remember { mutableStateOf(projectId) }
    var projectExpanded by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
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
    var voicePath by remember { mutableStateOf<String?>(null) }
    var recording by remember { mutableStateOf(false) }
    val recorder = remember { VoiceRecorder(ctx) }

    // 录音权限按需申请：启动时就弹会打扰，只有真要点录音时才要
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            if (recorder.start() != null) recording = true
            else Toast.makeText(ctx, "录音启动失败，请检查麦克风", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(ctx, "需要麦克风权限才能录语音备忘", Toast.LENGTH_SHORT).show()
        }
    }

    // 离开页面时必须停掉录音，否则 MediaRecorder 一直占着麦克风
    DisposableEffect(Unit) {
        onDispose {
            recorder.cancel()
            recorder.stopPlay()
        }
    }

    val leadOptions = listOf(0 to "当天", 1 to "提前1天", 3 to "提前3天", 7 to "提前7天")

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok && tempFile != null) {
            val src = tempFile!!
            val loc = LocationHelper.getLocationText(ctx)
            val out = WatermarkCamera.addWatermark(ctx, src, loc, photographer.ifBlank { "小汪" })
            if (out != null) {
                photos = photos + out.absolutePath
                if (out.absolutePath != src.absolutePath) src.delete()
            } else {
                // 水印失败就退回原图，绝不静默丢照片：
                // 老板在工地拍完发现照片没了，只能再跑一趟现场
                photos = photos + src.absolutePath
                Toast.makeText(ctx, "水印烧录失败，已保存原图", Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(memoId) {
        if (memoId != null) {
            val m = repo.getById(memoId)
            if (m != null) {
                source = m.source; title = m.title; detail = m.detail; importance = m.importance
                status = m.status; deadline = m.deadline; lead = m.remindLeadDays
                photos = m.photos; photographer = m.photographer
                voicePath = m.voicePath
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
                        // 不校验的话 projectId 会是 0，而 projects 表 id 从不为 0，
                        // Room 外键约束直接抛异常；原先异常没人接，会闪退且刚填的内容全丢
                        if (effectiveProjectId == 0L) {
                            Toast.makeText(ctx, "请先选择关联项目", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        scope.launch {
                            try {
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
                                    voicePath = voicePath,
                                    photographer = photographer
                                )
                                val savedId = if (memoId == null) repo.insert(memo) else { repo.update(memo); memoId }
                                // 先撤销旧闹钟：改期或清空截止日后，旧提醒不应再触发
                                ReminderScheduler.cancel(ctx, savedId)
                                // 已完成/已反馈业主的备案不该再提醒，否则办完了还天天弹
                                if (deadline != null && status != MemoStatus.已完成 && status != MemoStatus.已反馈业主) {
                                    ReminderScheduler.schedule(ctx, memo.copy(id = savedId))
                                }
                                navController.popBackStack()
                            } catch (t: Throwable) {
                                Toast.makeText(ctx, "保存失败：${t.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }) { Text("保存") }
                    if (memoId != null) {
                        TextButton(onClick = { showDelete = true }) { Text("删除") }
                    }
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
            Text("语音备忘", style = MaterialTheme.typography.labelSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (recording) {
                    Text("● 录音中…", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    FilledTonalButton(onClick = {
                        val f = recorder.stop()
                        recording = false
                        if (f != null) voicePath = f.absolutePath
                        else Toast.makeText(ctx, "录音太短，未保存", Toast.LENGTH_SHORT).show()
                    }) { Text("停止") }
                } else if (voicePath != null) {
                    IconButton(onClick = { recorder.play(voicePath!!) }) {
                        Icon(Icons.Filled.PlayArrow, "播放")
                    }
                    Text("已录制，点 ▶ 试听", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = {
                        recorder.stopPlay()
                        File(voicePath!!).delete()
                        voicePath = null
                    }) { Text("删除") }
                } else {
                    OutlinedButton(onClick = {
                        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            if (recorder.start() != null) recording = true
                            else Toast.makeText(ctx, "录音启动失败，请检查麦克风", Toast.LENGTH_SHORT).show()
                        } else {
                            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }) { Text("开始录音") }
                }
            }
        }
    }

    if (showDelete) {
        ConfirmDeleteDialog(
            title = "删除备案",
            message = "将删除本条备案录并撤销其待办提醒，此操作不可恢复。确定继续吗？",
            onConfirm = {
                showDelete = false
                val id = memoId ?: return@ConfirmDeleteDialog
                scope.launch {
                    repo.getById(id)?.let { repo.delete(it) }
                    ReminderScheduler.cancel(ctx, id)
                    navController.popBackStack()
                }
            },
            onDismiss = { showDelete = false }
        )
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
