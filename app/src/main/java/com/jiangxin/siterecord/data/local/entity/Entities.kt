package com.jiangxin.siterecord.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val roomNo: String = "",
    val ownerName: String = "",
    val ownerPhone: String = "",
    val stage: ProjectStage = ProjectStage.设计,
    val startDate: Long? = null,
    val expectFinishDate: Long? = null,
    val actualFinishDate: Long? = null,
    val designer: String = "",
    val foreman: String = "",
    val address: String = "",
    val status: ProjectStatus = ProjectStatus.进行中,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "inspections",
    foreignKeys = [ForeignKey(
        entity = Project::class,
        parentColumns = ["id"],
        childColumns = ["projectId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Inspection(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val dateTime: Long = System.currentTimeMillis(),
    val inspector: String = "",
    val stage: ProjectStage = ProjectStage.设计,
    val weather: String = "",
    val situation: InspectionSituation = InspectionSituation.需整改,
    val overallComment: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "inspection_items",
    foreignKeys = [ForeignKey(
        entity = Inspection::class,
        parentColumns = ["id"],
        childColumns = ["inspectionId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class InspectionItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val inspectionId: Long,
    val projectId: Long = 0,
    val area: String = "",
    val description: String = "",
    val severity: Severity = Severity.一般,
    val photos: List<String> = emptyList(),
    val needFix: Boolean = true,
    val fixDeadline: Long? = null,
    val fixStatus: FixStatus = FixStatus.未整改,
    val owner: String = "",
    val recheckNote: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "memos",
    foreignKeys = [ForeignKey(
        entity = Project::class,
        parentColumns = ["id"],
        childColumns = ["projectId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Memo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val source: MemoSource = MemoSource.业主,
    val title: String = "",
    val detail: String = "",
    val importance: Importance = Importance.普通,
    val status: MemoStatus = MemoStatus.待办,
    val deadline: Long? = null,
    val remindLeadDays: Int = 1,
    val photos: List<String> = emptyList(),
    val voicePath: String? = null,
    val photographer: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
