package com.jiangxin.siterecord.ui.nav

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Projects : Screen("projects")
    object ProjectDetail : Screen("project/{projectId}") {
        fun create(projectId: Long) = "project/$projectId"
    }
    // 注意：路由里不能出现字面量 "null"。NavType.LongType 无法解析 "null" 会直接抛异常，
    // 统一用 -1 作为「无此参数」的哨兵值，由 NavGraph 侧 takeIf 还原成 Kotlin 的 null。
    object ProjectForm : Screen("project_form?projectId={projectId}") {
        fun create(projectId: Long?) = "project_form?projectId=${projectId ?: -1L}"
    }
    object Memos : Screen("memos")
    object MemoForm : Screen("memo_form?projectId={projectId}&memoId={memoId}") {
        fun create(projectId: Long, memoId: Long? = null) = "memo_form?projectId=$projectId&memoId=${memoId ?: -1L}"
    }
    object Inspections : Screen("inspections")
    object InspectionForm : Screen("inspection_form?projectId={projectId}&inspectionId={inspectionId}") {
        fun create(projectId: Long, inspectionId: Long? = null) = "inspection_form?projectId=$projectId&inspectionId=${inspectionId ?: -1L}"
    }
}
