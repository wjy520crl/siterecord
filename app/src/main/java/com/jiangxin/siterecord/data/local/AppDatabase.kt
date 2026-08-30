package com.jiangxin.siterecord.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.jiangxin.siterecord.data.local.dao.InspectionDao
import com.jiangxin.siterecord.data.local.dao.InspectionItemDao
import com.jiangxin.siterecord.data.local.dao.MemoDao
import com.jiangxin.siterecord.data.local.dao.ProjectDao
import com.jiangxin.siterecord.data.local.entity.Inspection
import com.jiangxin.siterecord.data.local.entity.InspectionItem
import com.jiangxin.siterecord.data.local.entity.Memo
import com.jiangxin.siterecord.data.local.entity.Project

@Database(
    entities = [Project::class, Inspection::class, InspectionItem::class, Memo::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun inspectionDao(): InspectionDao
    abstract fun inspectionItemDao(): InspectionItemDao
    abstract fun memoDao(): MemoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "siterecord.db"
                )
                    // 故意不启用 fallbackToDestructiveMigration：
                    // 一旦将来改了表结构、version 升到 2 却没写 Migration，
                    // 破坏性重建会把老板在工地录的全部项目/巡查/备案/照片一次性清空且毫无提示。
                    // 宁可让 App 启动当场崩（立刻能发现），也不要静默丢数据。
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
