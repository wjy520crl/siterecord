package com.jiangxin.siterecord.ui.screens.projects

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.jiangxin.siterecord.SiteRecordApp
import com.jiangxin.siterecord.data.local.entity.Memo
import com.jiangxin.siterecord.data.local.entity.Project
import com.jiangxin.siterecord.data.local.entity.Inspection
import com.jiangxin.siterecord.data.repository.InspectionRepository
import com.jiangxin.siterecord.data.repository.MemoRepository
import com.jiangxin.siterecord.data.repository.ProjectRepository
import com.jiangxin.siterecord.ui.components.ConfirmDeleteDialog
import com.jiangxin.siterecord.ui.components.StatusBadge
import com.jiangxin.siterecord.ui.nav.Screen
import com.jiangxin.siterecord.util.formatDateTime
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class TimelineEntry(
    val kind: String,
    val time: Long,
    val title: String,
    val status: String,
    val refId: Long
)

class ProjectDetailViewModel(app: Application, private val projectId: Long) : AndroidViewModel(app) {
    private val ctx = app as SiteRecordApp
    val project: StateFlow<Project?> = ctx.projectRepository.observeById(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val inspections: StateFlow<List<Inspection>> = ctx.inspectionRepository.observeByProject(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val memos: StateFlow<List<Memo>> = ctx.memoRepository.observeByProject(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val timeline: StateFlow<List<TimelineEntry>> = combine(inspections, memos) { ins, ms ->
        val items = mutableListOf<TimelineEntry>()
        ins.forEach { i ->
            items.add(TimelineEntry("巡查", i.dateTime, "${i.situation.name} ${i.overallComment}".trim(), i.situation.name, i.id))
        }
        ms.forEach { m ->
            items.add(TimelineEntry("备案", m.createdAt, m.title, m.status.name, m.id))
        }
        items.sortedByDescending { it.time }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(navController: androidx.navigation.NavController, projectId: Long) {
    val application = LocalContext.current.applicationContext as Application
    val vm: ProjectDetailViewModel = viewModel(
        key = projectId.toString(),
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = ProjectDetailViewModel(application, projectId) as T
        }
    )
    val project by vm.project.collectAsState()
    val timeline by vm.timeline.collectAsState()

    val app = LocalContext.current.applicationContext as SiteRecordApp
    val scope = rememberCoroutineScope()
    var showDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(project?.name ?: "项目详情") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    TextButton(onClick = { navController.navigate(Screen.ProjectForm.create(projectId)) }) { Text("编辑") }
                    TextButton(onClick = { showDelete = true }) { Text("删除") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            project?.let {
                Card(Modifier.fillMaxWidth().padding(12.dp)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        InfoRow("房号", it.roomNo)
                        InfoRow("业主", "${it.ownerName} ${it.ownerPhone}")
                        InfoRow("阶段", it.stage.name)
                        InfoRow("状态", it.status.name)
                        InfoRow("设计师/工长", "${it.designer} / ${it.foreman}")
                        InfoRow("地址", it.address)
                    }
                }
            }
            Text("时间线", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(12.dp, 4.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(timeline) { e ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    StatusBadge(e.kind, MaterialTheme.colorScheme.primary)
                                    StatusBadge(e.status, MaterialTheme.colorScheme.tertiary)
                                }
                                Text(e.title.ifBlank { "(无标题)" }, style = MaterialTheme.typography.bodyMedium)
                                Text(formatDateTime(e.time), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { navController.navigate(Screen.InspectionForm.create(projectId)) }, modifier = Modifier.weight(1f)) { Text("+ 巡查报告") }
                Button(onClick = { navController.navigate(Screen.MemoForm.create(projectId)) }, modifier = Modifier.weight(1f)) { Text("+ 备案录") }
            }
        }
    }

    if (showDelete) {
        ConfirmDeleteDialog(
            title = "删除项目",
            message = "将删除「${project?.name ?: ""}」及其下全部巡查报告与备案录，此操作不可恢复。确定继续吗？",
            onConfirm = {
                showDelete = false
                project?.let { p ->
                    scope.launch {
                        app.projectRepository.delete(p)
                        navController.popBackStack()
                    }
                }
            },
            onDismiss = { showDelete = false }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text("$label：", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Text(value.ifBlank { "—" }, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun TextButton(onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick, content = content)
}
