package com.jiangxin.siterecord.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.jiangxin.siterecord.data.local.entity.InspectionItem
import kotlinx.coroutines.flow.Flow

@Dao
interface InspectionItemDao {
    @Query("SELECT * FROM inspection_items WHERE inspectionId = :inspectionId ORDER BY id ASC")
    fun observeByInspection(inspectionId: Long): Flow<List<InspectionItem>>

    @Query("SELECT * FROM inspection_items WHERE fixStatus NOT IN ('已整改','已验收')")
    fun observeUnfixed(): Flow<List<InspectionItem>>

    /** 待复检：需整改、但尚未验收通过的条目 */
    @Query("SELECT * FROM inspection_items WHERE needFix = 1 AND fixStatus != '已验收' ORDER BY id DESC")
    fun observePendingRecheck(): Flow<List<InspectionItem>>

    @Query("SELECT * FROM inspection_items")
    fun observeAll(): Flow<List<InspectionItem>>

    @Query("SELECT * FROM inspection_items WHERE inspectionId = :inspectionId")
    suspend fun getByInspection(inspectionId: Long): List<InspectionItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: InspectionItem): Long

    @Update
    suspend fun update(item: InspectionItem)

    @Delete
    suspend fun delete(item: InspectionItem)

    @Query("DELETE FROM inspection_items WHERE inspectionId = :inspectionId")
    suspend fun deleteByInspection(inspectionId: Long)

    /**
     * 整单替换问题条目：先删旧、再全量插入（统一重置 id，否则删完再 update 会全部落空）。
     * 放在同一事务里——原先是逐条 insert/update，中途任何一条失败都会留下
     * 「有巡查单但没有问题项」的残单，老板回头看是空的。
     */
    @Transaction
    suspend fun replaceItems(inspectionId: Long, items: List<InspectionItem>) {
        deleteByInspection(inspectionId)
        items.forEach { insert(it.copy(id = 0L)) }
    }
}
