package com.jiangxin.siterecord.ui.screens.inspection

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.compose.runtime.collectAsState
import com.jiangxin.siterecord.SiteRecordApp
import com.jiangxin.siterecord.camera.LocationHelper
import com.jiangxin.siterecord.camera.WatermarkCamera
import com.jiangxin.siterecord.data.local.entity.FixStatus
import com.jiangxin.siterecord.data.local.entity.Inspection
import com.jiangxin.siterecord.data.local.entity.InspectionItem
import com.jiangxin.siterecord.data.local.entity.InspectionSituation
import com.jiangxin.siterecord.data.local.entity.ProjectStage
import com.jiangxin.siterecord.data.local.entity.Severity
import com.jiangxin.siterecord.ui.components.PhotoThumb
import com.jiangxin.siterecord.util.formatDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

private data class ItemDraft(
    val id: Long = 0,
    val area: String = "",
    val description: String = "",
    val severity: Severity = Severity.一般,
    val needFix: Boolean = true,
    val fixDeadline: Long? = null,
    val fixStatus: FixStatus = FixStatus.未整改,
    val owner: String = "",
    val recheckNote: String = "",
    val photos: List<String> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionFormScreen(navController: androidx.navigation.NavController, projectId: Long, inspectionId: Long?) {
    val ctx = LocalContext.current.applicationContext as SiteRecordApp
    val repo = ctx.inspectionRepository
    val projects by ctx.projectRepository.observeAll().collectAsState()
    val scope = rememberCoroutineScope()
    var effectiveProjectId by remember { mutableStateOf(projectId) }
    var projectExpanded by remember { mutableStateOf(false) }
    val nameOf: (Long) -> String = { id -> projects.firstOrNull { it.id == id }?.name ?: "" }

    var situation by remember { mutableStateOf(InspectionSituation.需整改) }
    var dateTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var inspector by remember { mutableStateOf("") }
    var stage by remember { mutableStateOf(ProjectStage.设计) }
    var weather by remember { mutableStateOf("") }
    var overallComment by remember { mutableStateOf("") }
    var items by remember { mutableStateOf<List<ItemDraft>>(emptyList()) }
    var captureIndex by remember { mutableStateOf<Int?>(null) }
    var tempFile by remember { mutableStateOf<File?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok && captureIndex != null) {
            val loc = LocationHelper.getLocationText(ctx)
            val out = WatermarkCamera.addWatermark(ctx, tempFile!!, loc, inspector.ifBlank { "小汪" })
            tempFile!!.delete()
            val idx = captureIndex!!
            if (out != null) {
                items = items.mapIndexed { i, d -> if (i == idx) d.copy(photos = d.photos + out.absolutePath) else d }
            }
            captureIndex = null
        }
    }

    LaunchedEffect(inspectionId) {
        if (inspectionId != null) {
            val ins = repo.getById(inspectionId)
            if (ins != null) {
                situation = ins.situation; dateTime = ins.dateTime; inspector = ins.inspector
                stage = ins.stage; weather = ins.weather; overallComment = ins.overallComment
                effectiveProjectId = ins.projectId
            }
            val its = repo.observeItems(inspectionId).first()
            items = its.map {
                ItemDraft(it.id, it.area, it.description, it.severity, it.needFix, it.fixDeadline, it.fixStatus, it.owner, it.recheckNote, it.photos)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (inspectionId == null) "新建巡查" else "编辑巡查") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                },
                actions = {
                    TextButton(onClick = {
                        scope.launch {
                        val inspection = Inspection(
                            id = inspectionId ?: 0,
                            projectId = effectiveProjectId,
                            dateTime = dateTime,
                            inspector = inspector,
                            stage = stage,
                            weather = weather,
                            situation = situation,
                            overallComment = overallComment
                        )
                        val insId = if (inspectionId == null) repo.insert(inspection) else { repo.update(inspection); inspectionId }
                        items.forEach { d ->
                            val entity = InspectionItem(
                                id = d.id, inspectionId = insId, projectId = projectId,
                                area = d.area, description = d.description, severity = d.severity,
                                photos = d.photos, needFix = d.needFix, fixDeadline = d.fixDeadline,
                                fixStatus = d.fixStatus, owner = d.owner, recheckNote = d.recheckNote
                            )
                            if (d.id == 0L) repo.insertItem(entity) else repo.updateItem(entity)
                        }
                        navController.popBackStack()
                        }
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
            Text("巡查情况", style = MaterialTheme.typography.labelSmall)
            EnumChips(InspectionSituation.entries.toList(), situation) { situation = it }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("日期：", style = MaterialTheme.typography.labelSmall)
                Text(formatDate(dateTime), style = MaterialTheme.typography.bodyMedium)
                DateField("选择日期", dateTime) { dateTime = it ?: System.currentTimeMillis() }
            }
            OutlinedTextField(inspector, { inspector = it }, label = { Text("巡查人*") }, modifier = Modifier.fillMaxWidth())
            Text("阶段", style = MaterialTheme.typography.labelSmall)
            EnumChips(ProjectStage.entries.toList(), stage) { stage = it }
            OutlinedTextField(weather, { weather = it }, label = { Text("天气") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(overallComment, { overallComment = it }, label = { Text("总体评价") }, modifier = Modifier.fillMaxWidth().height(90.dp))

            if (situation == InspectionSituation.合格无问题) {
                Text("本次巡查合格，无需填写问题项。", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
            } else {
                Text(if (situation == InspectionSituation.复检) "复检结论（逐项）" else "问题条目", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                items.forEachIndexed { index, d ->
                    ItemCard(d, index,
                        onArea = { items = items.update(index) { copy(area = it) } },
                        onDesc = { items = items.update(index) { copy(description = it) } },
                        onSeverity = { items = items.update(index) { copy(severity = it) } },
                        onNeedFix = { items = items.update(index) { copy(needFix = it) } },
                        onFixDeadline = { items = items.update(index) { copy(fixDeadline = it) } },
                        onOwner = { items = items.update(index) { copy(owner = it) } },
                        onFixStatus = { items = items.update(index) { copy(fixStatus = it) } },
                        onRecheck = { items = items.update(index) { copy(recheckNote = it) } },
                        onCapture = {
                            captureIndex = index
                            val f = File(ctx.cacheDir, "cap_${System.currentTimeMillis()}.jpg")
                            val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", f)
                            tempFile = f
                            launcher.launch(uri)
                        },
                        onDeletePhoto = { p -> items = items.update(index) { copy(photos = photos - p) } },
                        onDelete = {
                            items = items - d
                            if (d.id != 0L) repo.deleteItem(InspectionItem(id = d.id, inspectionId = inspectionId ?: 0, projectId = projectId, area = d.area, description = d.description, severity = d.severity, photos = d.photos, needFix = d.needFix, fixDeadline = d.fixDeadline, fixStatus = d.fixStatus, owner = d.owner, recheckNote = d.recheckNote))
                        }
                    )
                }
                TextButton(onClick = { items = items + ItemDraft() }) { Text("+ 添加问题条目") }
            }
        }
    }
}

private fun <T> List<T>.update(index: Int, transform: T.() -> T): List<T> = mapIndexed { i, t -> if (i == index) t.transform() else t }

@Composable
private fun ItemCard(
    d: ItemDraft,
    index: Int,
    onArea: (String) -> Unit,
    onDesc: (String) -> Unit,
    onSeverity: (Severity) -> Unit,
    onNeedFix: (Boolean) -> Unit,
    onFixDeadline: (Long?) -> Unit,
    onOwner: (String) -> Unit,
    onFixStatus: (FixStatus) -> Unit,
    onRecheck: (String) -> Unit,
    onCapture: () -> Unit,
    onDeletePhoto: (String) -> Unit,
    onDelete: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("问题 ${index + 1}", style = MaterialTheme.typography.labelMedium)
                androidx.compose.material3.IconButton(onClick = onDelete, modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                    Icon(Icons.Filled.Close, "删除条目")
                }
            }
            OutlinedTextField(d.area, onArea, label = { Text("区域") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(d.description, onDesc, label = { Text("问题描述") }, modifier = Modifier.fillMaxWidth())
            Text("严重度", style = MaterialTheme.typography.labelSmall)
            EnumChips(Severity.entries.toList(), d.severity, onSeverity)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = d.needFix, onCheckedChange = onNeedFix)
                Text("需整改")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("整改期限：", style = MaterialTheme.typography.labelSmall)
                Text(formatDate(d.fixDeadline), style = MaterialTheme.typography.bodyMedium)
                DateField("选择", d.fixDeadline) { onFixDeadline(it) }
            }
            OutlinedTextField(d.owner, onOwner, label = { Text("负责人") }, modifier = Modifier.fillMaxWidth())
            Text("整改状态", style = MaterialTheme.typography.labelSmall)
            EnumChips(FixStatus.entries.toList(), d.fixStatus, onFixStatus)
            if (d.recheckNote.isNotEmpty() || true) {
                OutlinedTextField(d.recheckNote, onRecheck, label = { Text("复检结论") }, modifier = Modifier.fillMaxWidth())
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("照片", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                IconButton(onClick = onCapture) { Icon(Icons.Filled.CameraAlt, "拍照") }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(d.photos.size) { i ->
                    Row {
                        PhotoThumb(d.photos[i], size = 64)
                        IconButton(onClick = { onDeletePhoto(d.photos[i]) }) { Icon(Icons.Filled.Close, "删除") }
                    }
                }
            }
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
    TextButton(onClick = { dialog.show() }) { Text(label) }
}
