package com.jiangxin.siterecord.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jiangxin.siterecord.data.local.entity.Inspection
import kotlinx.coroutines.flow.Flow

@Dao
interface InspectionDao {
    @Query("SELECT * FROM inspections WHERE projectId = :projectId ORDER BY dateTime DESC")
    fun observeByProject(projectId: Long): Flow<List<Inspection>>

    @Query("SELECT * FROM inspections ORDER BY dateTime DESC")
    fun observeAll(): Flow<List<Inspection>>

    @Query("SELECT * FROM inspections WHERE id = :id")
    suspend fun getById(id: Long): Inspection?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(inspection: Inspection): Long

    @Update
    suspend fun update(inspection: Inspection)

    @Delete
    suspend fun delete(inspection: Inspection)
}
