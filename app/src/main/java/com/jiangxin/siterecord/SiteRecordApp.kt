package com.jiangxin.siterecord

import android.app.Application
import com.jiangxin.siterecord.data.local.AppDatabase
import com.jiangxin.siterecord.data.repository.InspectionRepository
import com.jiangxin.siterecord.data.repository.MemoRepository
import com.jiangxin.siterecord.data.repository.ProjectRepository

class SiteRecordApp : Application() {
    val database by lazy { AppDatabase.get(this) }
    val projectRepository by lazy { ProjectRepository(database.projectDao()) }
    val inspectionRepository by lazy {
        InspectionRepository(database.inspectionDao(), database.inspectionItemDao())
    }
    val memoRepository by lazy { MemoRepository(database.memoDao()) }
}
