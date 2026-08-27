package com.jiangxin.siterecord.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jiangxin.siterecord.data.local.entity.InspectionItem
import kotlinx.coroutines.flow.Flow

@Dao
interface InspectionItemDao {
    @Query("SELECT * FROM inspection_items WHERE inspectionId = :inspectionId ORDER BY id ASC")
    fun observeByInspection(inspectionId: Long): Flow<List<InspectionItem>>

    @Query("SELECT * FROM inspection_items WHERE fixStatus NOT IN ('已整改','已验收')")
    fun observeUnfixed(): Flow<List<InspectionItem>>

    @Query("SELECT * FROM inspection_items")
    fun observeAll(): Flow<List<InspectionItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: InspectionItem): Long

    @Update
    suspend fun update(item: InspectionItem)

    @Delete
    suspend fun delete(item: InspectionItem)
}
