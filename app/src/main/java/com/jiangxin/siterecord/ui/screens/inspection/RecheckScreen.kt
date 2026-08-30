package com.jiangxin.siterecord.ui.screens.inspection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.jiangxin.siterecord.SiteRecordApp
import com.jiangxin.siterecord.data.local.entity.InspectionItem
import com.jiangxin.siterecord.ui.components.OverdueLabel
import com.jiangxin.siterecord.ui.components.StatusBadge
import com.jiangxin.siterecord.util.formatDate
import kotlinx.coroutines.launch

/**
 * 待复检清单：把所有「需整改但尚未验收」的问题条目集中到一处，
 * 老板到工地逐条走一遍点验收即可，验收完自动回写对应巡查单的状态为「复检」。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecheckScreen(navController: androidx.navigation.NavController) {
    val app = LocalContext.current.applicationContext as SiteRecordApp
    val repo = app.inspectionRepository
    val pending by repo.observePendingRecheck().collectAsState(initial = emptyList())
    val projects by app.projectRepository.observeAll().collectAsState(initial = emptyList())
    val inspections by repo.observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var verifying by remember { mutableStateOf<InspectionItem?>(null) }
    var note by remember { mutableStateOf("") }

    val nameOf: (Long) -> String = { id -> projects.firstOrNull { it.id == id }?.name ?: "（项目已删除）" }
    val dateOf: (Long) -> String = { id ->
        inspections.firstOrNull { it.id == id }?.dateTime?.let { formatDate(it) } ?: ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("待复检（${pending.size}）") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        if (pending.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("全部复检完毕，暂无待验收条目", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(pending, key = { it.id }) { item ->
                    RecheckCard(
                        item = item,
                        projectName = nameOf(item.projectId),
                        inspectionDate = dateOf(item.inspectionId),
                        onVerify = {
                            note = item.recheckNote
                            verifying = item
                        }
                    )
                }
            }
        }
    }

    verifying?.let { item ->
        AlertDialog(
            onDismissRequest = { verifying = null },
            title = { Text("复检验收") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${item.area}｜${item.description}",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("复检结论（可留空）") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = item
                    val text = note
                    verifying = null
                    scope.launch { repo.markRechecked(target, text) }
                }) { Text("验收通过") }
            },
            dismissButton = {
                TextButton(onClick = { verifying = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun RecheckCard(
    item: InspectionItem,
    projectName: String,
    inspectionDate: String,
    onVerify: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(item.severity.name, MaterialTheme.colorScheme.secondary)
                StatusBadge(item.fixStatus.name, MaterialTheme.colorScheme.tertiary)
                OverdueLabel(item.fixDeadline)
                Spacer(Modifier.weight(1f))
                if (item.fixDeadline != null) {
                    Text(
                        "限 ${formatDate(item.fixDeadline)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            Text(
                "$projectName${if (inspectionDate.isNotBlank()) " · $inspectionDate" else ""}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                "${item.area}｜${item.description}",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (item.owner.isNotBlank()) {
                Text("责任人：${item.owner}", style = MaterialTheme.typography.labelSmall)
            }
            if (item.recheckNote.isNotBlank()) {
                Text("复检结论：${item.recheckNote}", style = MaterialTheme.typography.labelSmall)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                FilledTonalButton(onClick = onVerify) {
                    Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("验收通过")
                }
            }
        }
    }
}
