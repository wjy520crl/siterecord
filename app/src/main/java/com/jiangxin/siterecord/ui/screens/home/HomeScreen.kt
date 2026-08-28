package com.jiangxin.siterecord.ui.screens.home

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.jiangxin.siterecord.SiteRecordApp
import com.jiangxin.siterecord.data.repository.InspectionRepository
import com.jiangxin.siterecord.data.repository.MemoRepository
import com.jiangxin.siterecord.data.repository.ProjectRepository
import com.jiangxin.siterecord.ui.components.MetricCard
import com.jiangxin.siterecord.ui.components.OverdueLabel
import com.jiangxin.siterecord.ui.components.StatusBadge
import com.jiangxin.siterecord.ui.nav.Screen
import com.jiangxin.siterecord.util.formatDate
import com.jiangxin.siterecord.util.isOverdue
import android.content.Context
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.jiangxin.siterecord.backup.BackupExporter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class FollowUpItem(
    val id: Long,
    val type: String,
    val projectId: Long,
    val projectName: String,
    val title: String,
    val deadline: Long?,
    val importance: String? = null
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx = app as SiteRecordApp
    private val projectRepo = ctx.projectRepository
    private val inspectionRepo = ctx.inspectionRepository
    private val memoRepo = ctx.memoRepository

    val projects = projectRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val unfixedItems = inspectionRepo.observeUnfixedItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val pendingMemos = memoRepo.observePending()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val metricProjectCount = projects.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val metricUnfixed = unfixedItems.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val metricPending = pendingMemos.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val metricOverdue = combine(unfixedItems, pendingMemos) { items, memos ->
        val now = System.currentTimeMillis()
        items.count { it.fixDeadline != null && it.fixDeadline < now } +
            memos.count { it.deadline != null && it.deadline < now }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val followUps: StateFlow<List<FollowUpItem>> = combine(unfixedItems, pendingMemos, projects) { items, memos, projs ->
        val nameOf: (Long) -> String = { id -> projs.firstOrNull { it.id == id }?.name ?: "" }
        val list = mutableListOf<FollowUpItem>()
        items.forEach { it ->
            list.add(
                FollowUpItem(
                    id = it.id,
                    type = "整改",
                    projectId = it.projectId,
                    projectName = nameOf(it.projectId),
                    title = "${it.area} ${it.description}".trim(),
                    deadline = it.fixDeadline,
                    importance = it.severity.name
                )
            )
        }
        memos.forEach { m ->
            list.add(
                FollowUpItem(
                    id = m.id,
                    type = "备案",
                    projectId = m.projectId,
                    projectName = nameOf(m.projectId),
                    title = m.title,
                    deadline = m.deadline,
                    importance = m.importance.name
                )
            )
        }
        list.sortedBy { it.deadline ?: Long.MAX_VALUE }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: androidx.navigation.NavController) {
    val vm: HomeViewModel = viewModel()
    val context = LocalContext.current
    val pc by vm.metricProjectCount.collectAsState()
    val unfixed by vm.metricUnfixed.collectAsState()
    val pending by vm.metricPending.collectAsState()
    val overdue by vm.metricOverdue.collectAsState()
    val followUps by vm.followUps.collectAsState()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("匠心工地记录") },
            actions = {
                TextButton(onClick = { shareBackup(context) }) { Text("备份") }
            }
        )
    }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard(pc, "项目", Modifier.weight(1f))
                    MetricCard(unfixed, "待整改", Modifier.weight(1f))
                    MetricCard(pending, "待办备案", Modifier.weight(1f))
                    MetricCard(overdue, "逾期", Modifier.weight(1f), highlight = overdue > 0)
                }
            }
            item { Text("待跟进", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary) }
            items(followUps) { fu ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        navController.navigate(Screen.ProjectDetail.create(fu.projectId))
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StatusBadge(fu.type, MaterialTheme.colorScheme.primary)
                                if (fu.importance != null) StatusBadge(fu.importance, MaterialTheme.colorScheme.tertiary)
                            }
                            Text(fu.title.ifBlank { "(无标题)" }, style = MaterialTheme.typography.bodyMedium)
                            Text("${fu.projectName} · ${formatDate(fu.deadline)}", style = MaterialTheme.typography.labelSmall)
                        }
                        OverdueLabel(fu.deadline)
                    }
                }
            }
            if (followUps.isEmpty()) {
                item {
                    Surface(tonalElevation = 0.dp, modifier = Modifier.fillMaxWidth()) {
                        Text("暂无待跟进事项", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                    }
                }
            }
        }
    }
}

private fun shareBackup(context: Context) {
    val jsonUri = BackupExporter.exportJsonUri(context) ?: return
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, jsonUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "导出备份（JSON）"))
}
