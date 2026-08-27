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
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
