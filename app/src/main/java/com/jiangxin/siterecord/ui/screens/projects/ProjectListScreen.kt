package com.jiangxin.siterecord.ui.screens.projects

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.jiangxin.siterecord.SiteRecordApp
import com.jiangxin.siterecord.data.local.entity.Project
import com.jiangxin.siterecord.data.repository.ProjectRepository
import com.jiangxin.siterecord.ui.components.StatusBadge
import com.jiangxin.siterecord.ui.nav.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class ProjectListViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as SiteRecordApp).projectRepository
    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()
    fun setQuery(q: String) { _query.value = q }

    val filtered = repo.observeAll().combine(_query) { list, q ->
        if (q.isBlank()) list else list.filter {
            it.name.contains(q) || it.ownerName.contains(q) || it.roomNo.contains(q)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(navController: androidx.navigation.NavController) {
    val vm: ProjectListViewModel = viewModel()
    val list by vm.filtered.collectAsState()
    val query by vm.query.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("项目") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Screen.ProjectForm.create(null)) }) {
                Icon(Icons.Filled.Add, "新建项目")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { vm.setQuery(it) },
                placeholder = { Text("搜索项目 / 业主 / 房号") },
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(list, key = { it.id }) { p ->
                    ProjectCard(p) { navController.navigate(Screen.ProjectDetail.create(p.id)) }
                }
                if (list.isEmpty()) {
                    item { Text("暂无项目，点击右下角新建", modifier = Modifier.padding(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ProjectCard(project: Project, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(project.name.firstOrNull()?.toString() ?: "?", color = MaterialTheme.colorScheme.onPrimary)
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(project.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${project.roomNo} · ${project.ownerName}".ifBlank { "未填" },
                    style = MaterialTheme.typography.labelSmall
                )
            }
            StatusBadge(project.stage.name, MaterialTheme.colorScheme.primary)
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}
