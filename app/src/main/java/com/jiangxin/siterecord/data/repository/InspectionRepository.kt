package com.jiangxin.siterecord.data.repository

import com.jiangxin.siterecord.data.local.dao.InspectionDao
import com.jiangxin.siterecord.data.local.dao.InspectionItemDao
import com.jiangxin.siterecord.data.local.entity.Inspection
import com.jiangxin.siterecord.data.local.entity.InspectionItem
import kotlinx.coroutines.flow.Flow

class InspectionRepository(
    private val dao: InspectionDao,
    private val itemDao: InspectionItemDao
) {
    fun observeByProject(projectId: Long): Flow<List<Inspection>> = dao.observeByProject(projectId)
    fun observeAll(): Flow<List<Inspection>> = dao.observeAll()
    suspend fun getById(id: Long): Inspection? = dao.getById(id)
    suspend fun insert(inspection: Inspection): Long = dao.insert(inspection)
    suspend fun update(inspection: Inspection) = dao.update(inspection)
    suspend fun delete(inspection: Inspection) = dao.delete(inspection)

    fun observeItems(inspectionId: Long): Flow<List<InspectionItem>> = itemDao.observeByInspection(inspectionId)
    fun observeUnfixedItems(): Flow<List<InspectionItem>> = itemDao.observeUnfixed()
    fun observeAllItems(): Flow<List<InspectionItem>> = itemDao.observeAll()
    suspend fun insertItem(item: InspectionItem): Long = itemDao.insert(item)
    suspend fun updateItem(item: InspectionItem) = itemDao.update(item)
    suspend fun deleteItem(item: InspectionItem) = itemDao.delete(item)
}
