package com.jiangxin.siterecord.ui.screens.projects

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.DateRange
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jiangxin.siterecord.SiteRecordApp
import com.jiangxin.siterecord.data.local.entity.Project
import com.jiangxin.siterecord.data.local.entity.ProjectStage
import com.jiangxin.siterecord.data.local.entity.ProjectStatus
import com.jiangxin.siterecord.ui.nav.Screen
import com.jiangxin.siterecord.util.formatDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectFormScreen(navController: androidx.navigation.NavController, projectId: Long?) {
    val application = LocalContext.current.applicationContext as SiteRecordApp
    val repo = application.projectRepository
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var roomNo by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var ownerPhone by remember { mutableStateOf("") }
    var stage by remember { mutableStateOf(ProjectStage.设计) }
    var status by remember { mutableStateOf(ProjectStatus.进行中) }
    var designer by remember { mutableStateOf("") }
    var foreman by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf<Long?>(null) }
    var expectFinish by remember { mutableStateOf<Long?>(null) }
    var actualFinish by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(projectId) {
        if (projectId != null) {
            val p = repo.getById(projectId)
            if (p != null) {
                name = p.name; roomNo = p.roomNo; ownerName = p.ownerName; ownerPhone = p.ownerPhone
                stage = p.stage; status = p.status; designer = p.designer; foreman = p.foreman
                address = p.address; startDate = p.startDate; expectFinish = p.expectFinishDate; actualFinish = p.actualFinishDate
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (projectId == null) "新建项目" else "编辑项目") },
                navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        scope.launch {
                        val project = Project(
                            id = projectId ?: 0, name = name, roomNo = roomNo, ownerName = ownerName,
                            ownerPhone = ownerPhone, stage = stage, status = status, designer = designer,
                            foreman = foreman, address = address, startDate = startDate,
                            expectFinishDate = expectFinish, actualFinishDate = actualFinish
                        )
                        if (projectId == null) repo.insert(project) else repo.update(project)
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
            OutlinedTextField(name, { name = it }, label = { Text("项目名称 / 小区*") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(roomNo, { roomNo = it }, label = { Text("房号") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(ownerName, { ownerName = it }, label = { Text("业主姓名") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(ownerPhone, { ownerPhone = it }, label = { Text("业主电话") }, modifier = Modifier.fillMaxWidth())
            Text("阶段", style = MaterialTheme.typography.labelSmall)
            EnumChips(ProjectStage.entries.toList(), stage) { stage = it }
            Text("状态", style = MaterialTheme.typography.labelSmall)
            EnumChips(ProjectStatus.entries.toList(), status) { status = it }
            OutlinedTextField(designer, { designer = it }, label = { Text("设计师") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(foreman, { foreman = it }, label = { Text("工长") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(address, { address = it }, label = { Text("详细地址") }, modifier = Modifier.fillMaxWidth())
            DateField("开工日期", startDate) { startDate = it }
            DateField("预计完工", expectFinish) { expectFinish = it }
            DateField("实际完工", actualFinish) { actualFinish = it }
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
    val dialog = DatePickerDialog(
        context,
        { _: android.widget.DatePicker, y, m, d -> cal.set(y, m, d); onPick(cal.timeInMillis) },
        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
    )
    OutlinedTextField(
        value = formatDate(value),
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = { IconButton(onClick = { dialog.show() }) { Icon(Icons.Filled.DateRange, null) } },
        modifier = Modifier.fillMaxWidth()
    )
}
