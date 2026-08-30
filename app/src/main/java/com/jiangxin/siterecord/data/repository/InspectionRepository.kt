package com.jiangxin.siterecord.data.repository

import com.jiangxin.siterecord.data.local.dao.InspectionDao
import com.jiangxin.siterecord.data.local.dao.InspectionItemDao
import com.jiangxin.siterecord.data.local.entity.FixStatus
import com.jiangxin.siterecord.data.local.entity.Inspection
import com.jiangxin.siterecord.data.local.entity.InspectionItem
import com.jiangxin.siterecord.data.local.entity.InspectionSituation
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
    fun observePendingRecheck(): Flow<List<InspectionItem>> = itemDao.observePendingRecheck()
    fun observeAllItems(): Flow<List<InspectionItem>> = itemDao.observeAll()
    suspend fun insertItem(item: InspectionItem): Long = itemDao.insert(item)
    suspend fun updateItem(item: InspectionItem) = itemDao.update(item)
    suspend fun deleteItem(item: InspectionItem) = itemDao.delete(item)

    /**
     * 复检验收：条目标记为已验收并写入复检结论。
     * 该巡查下所有需整改条目都验收通过后，自动把巡查状态回写为「复检」——
     * 否则老板得手动回去改巡查单，实际一定会忘。
     */
    suspend fun markRechecked(item: InspectionItem, note: String) {
        itemDao.update(item.copy(fixStatus = FixStatus.已验收, recheckNote = note))
        val siblings = itemDao.getByInspection(item.inspectionId)
        val allVerified = siblings.filter { it.needFix }.all { it.fixStatus == FixStatus.已验收 }
        if (allVerified) {
            dao.getById(item.inspectionId)?.let { ins ->
                if (ins.situation != InspectionSituation.复检) {
                    dao.update(ins.copy(situation = InspectionSituation.复检))
                }
            }
        }
    }
}
