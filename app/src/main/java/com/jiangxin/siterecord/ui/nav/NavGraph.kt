package com.jiangxin.siterecord.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jiangxin.siterecord.ui.screens.home.HomeScreen
import com.jiangxin.siterecord.ui.screens.inspection.InspectionFormScreen
import com.jiangxin.siterecord.ui.screens.inspection.InspectionListScreen
import com.jiangxin.siterecord.ui.screens.memo.MemoFormScreen
import com.jiangxin.siterecord.ui.screens.memo.MemoListScreen
import com.jiangxin.siterecord.ui.screens.projects.ProjectDetailScreen
import com.jiangxin.siterecord.ui.screens.projects.ProjectFormScreen
import com.jiangxin.siterecord.ui.screens.projects.ProjectListScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val topRoutes = listOf(Screen.Home.route, Screen.Projects.route, Screen.Memos.route, Screen.Inspections.route)
    val baseRoute = currentRoute?.substringBefore('/')?.substringBefore('?')
    val showBottomBar = baseRoute in topRoutes

    val items = listOf(
        BottomItem(Screen.Home, "首页", Icons.Filled.Home),
        BottomItem(Screen.Projects, "项目", Icons.Filled.Business),
        BottomItem(Screen.Memos, "备案录", Icons.Filled.Bookmark),
        BottomItem(Screen.Inspections, "巡查", Icons.Filled.CameraAlt)
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    val current = navController.currentBackStackEntryAsState().value?.destination
                    items.forEach { item ->
                        NavigationBarItem(
                            selected = baseRoute == item.screen.route,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(Screen.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Home.route) { HomeScreen(navController) }
            composable(Screen.Projects.route) { ProjectListScreen(navController) }
            composable(Screen.ProjectDetail.route, arguments = listOf(navArgument("projectId") { type = NavType.LongType })) {
                val id = it.arguments?.getLong("projectId") ?: 0L
                ProjectDetailScreen(navController, id)
            }
            composable(Screen.ProjectForm.route, arguments = listOf(navArgument("projectId") { type = NavType.LongType; defaultValue = -1L })) {
                // NavType.LongType 不支持可空，用 -1 作哨兵（0 同样视为「新建」）
                val pid = it.arguments?.getLong("projectId")?.takeIf { p -> p != -1L && p != 0L }
                ProjectFormScreen(navController, pid)
            }
            composable(Screen.Memos.route) { MemoListScreen(navController) }
            composable(Screen.MemoForm.route, arguments = listOf(
                navArgument("projectId") { type = NavType.LongType },
                navArgument("memoId") { type = NavType.LongType; defaultValue = -1L }
            )) {
                val pid = it.arguments?.getLong("projectId") ?: 0L
                val mid = it.arguments?.getLong("memoId")?.takeIf { m -> m != -1L }
                MemoFormScreen(navController, pid, mid)
            }
            composable(Screen.Inspections.route) { InspectionListScreen(navController) }
            composable(Screen.InspectionForm.route, arguments = listOf(
                navArgument("projectId") { type = NavType.LongType },
                navArgument("inspectionId") { type = NavType.LongType; defaultValue = -1L }
            )) {
                val pid = it.arguments?.getLong("projectId") ?: 0L
                val iid = it.arguments?.getLong("inspectionId")?.takeIf { i -> i != -1L }
                InspectionFormScreen(navController, pid, iid)
            }
        }
    }
}

private data class BottomItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)
