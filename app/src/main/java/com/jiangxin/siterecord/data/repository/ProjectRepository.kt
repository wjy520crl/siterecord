package com.jiangxin.siterecord.data.repository

import com.jiangxin.siterecord.data.local.dao.ProjectDao
import com.jiangxin.siterecord.data.local.entity.Project
import kotlinx.coroutines.flow.Flow

class ProjectRepository(private val dao: ProjectDao) {
    fun observeAll(): Flow<List<Project>> = dao.observeAll()
    fun observeById(id: Long): Flow<Project?> = dao.observeById(id)
    suspend fun getById(id: Long): Project? = dao.getById(id)
    suspend fun insert(project: Project): Long = dao.insert(project)
    suspend fun update(project: Project) = dao.update(project)
    suspend fun delete(project: Project) = dao.delete(project)
}
