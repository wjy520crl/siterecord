package com.jiangxin.siterecord.data.repository

import com.jiangxin.siterecord.data.local.dao.MemoDao
import com.jiangxin.siterecord.data.local.entity.Memo
import kotlinx.coroutines.flow.Flow

class MemoRepository(private val dao: MemoDao) {
    fun observeAll(): Flow<List<Memo>> = dao.observeAll()
    fun observeByProject(projectId: Long): Flow<List<Memo>> = dao.observeByProject(projectId)
    fun observePending(): Flow<List<Memo>> = dao.observePending()
    suspend fun getById(id: Long): Memo? = dao.getById(id)
    suspend fun insert(memo: Memo): Long = dao.insert(memo)
    suspend fun update(memo: Memo) = dao.update(memo)
    suspend fun delete(memo: Memo) = dao.delete(memo)
}
