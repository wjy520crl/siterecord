package com.jiangxin.siterecord.ui.screens.memo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.jiangxin.siterecord.SiteRecordApp
import com.jiangxin.siterecord.data.local.entity.Memo
import com.jiangxin.siterecord.data.local.entity.Project
import com.jiangxin.siterecord.ui.components.OverdueLabel
import com.jiangxin.siterecord.ui.components.StatusBadge
import com.jiangxin.siterecord.ui.nav.Screen
import com.jiangxin.siterecord.util.formatDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoListScreen(navController: androidx.navigation.NavController) {
    val application = LocalContext.current.applicationContext as SiteRecordApp
    val memos by application.memoRepository.observeAll().collectAsState()
    val projects by application.projectRepository.observeAll().collectAsState()

    var selProject by remember { mutableStateOf<Long?>(null) }
    var query by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val nameOf: (Long) -> String = { id -> projects.firstOrNull { it.id == id }?.name ?: "" }
    val filtered = memos.filter {
        (selProject == null || it.projectId == selProject) &&
            (query.isBlank() || it.title.contains(query) || it.detail.contains(query))
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("备案录") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Screen.MemoForm.create(0)) }) {
                Icon(Icons.Filled.Add, "新建备案")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    OutlinedTextField(
                        value = nameOf(selProject ?: 0).ifEmpty { "全部项目" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("项目") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { androidx.compose.material3.IconButton(onClick = { expanded = !expanded }) { Icon(Icons.Filled.ArrowDropDown, null) } }
                    )
                    DropdownMenu(expanded, { expanded = false }) {
                        DropdownMenuItem(onClick = { selProject = null; expanded = false }, text = { Text("全部项目") })
                        projects.forEach { p ->
                            DropdownMenuItem(onClick = { selProject = p.id; expanded = false }, text = { Text(p.name) })
                        }
                    }
                }
                OutlinedTextField(query, { query = it }, placeholder = { Text("搜索") }, modifier = Modifier.weight(1f))
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.id }) { m ->
                    MemoCard(m, nameOf(m.projectId)) {
                        navController.navigate(Screen.MemoForm.create(m.projectId, m.id))
                    }
                }
                if (filtered.isEmpty()) item { Text("暂无备案", Modifier.padding(16.dp)) }
            }
        }
    }
}

@Composable
private fun MemoCard(memo: Memo, projectName: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusBadge(memo.status.name, MaterialTheme.colorScheme.primary)
                    StatusBadge(memo.importance.name, MaterialTheme.colorScheme.tertiary)
                }
                Text(memo.title.ifBlank { "(无标题)" }, style = MaterialTheme.typography.bodyLarge)
                Text("$projectName · ${memo.source.name} · ${formatDate(memo.deadline)}", style = MaterialTheme.typography.labelSmall)
                if (memo.photos.isNotEmpty()) Text("含照片 ${memo.photos.size}", style = MaterialTheme.typography.labelSmall)
            }
            OverdueLabel(memo.deadline)
        }
    }
}
