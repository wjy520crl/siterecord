package com.jiangxin.siterecord.ui.nav

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Projects : Screen("projects")
    object ProjectDetail : Screen("project/{projectId}") {
        fun create(projectId: Long) = "project/$projectId"
    }
    object ProjectForm : Screen("project_form?projectId={projectId}") {
        fun create(projectId: Long?) = "project_form?projectId=$projectId"
    }
    object Memos : Screen("memos")
    object MemoForm : Screen("memo_form?projectId={projectId}&memoId={memoId}") {
        fun create(projectId: Long, memoId: Long? = null) = "memo_form?projectId=$projectId&memoId=$memoId"
    }
    object Inspections : Screen("inspections")
    object InspectionForm : Screen("inspection_form?projectId={projectId}&inspectionId={inspectionId}") {
        fun create(projectId: Long, inspectionId: Long? = null) = "inspection_form?projectId=$projectId&inspectionId=$inspectionId"
    }
}
