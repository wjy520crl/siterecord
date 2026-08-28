package com.jiangxin.siterecord.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jiangxin.siterecord.data.local.entity.*

class Converters {
    private val gson = Gson()

    @TypeConverter fun fromProjectStage(v: ProjectStage) = v.name
    @TypeConverter fun toProjectStage(v: String?) = ProjectStage.from(v)

    @TypeConverter fun fromProjectStatus(v: ProjectStatus) = v.name
    @TypeConverter fun toProjectStatus(v: String?) = ProjectStatus.from(v)

    @TypeConverter fun fromInspectionSituation(v: InspectionSituation) = v.name
    @TypeConverter fun toInspectionSituation(v: String?) = InspectionSituation.from(v)

    @TypeConverter fun fromSeverity(v: Severity) = v.name
    @TypeConverter fun toSeverity(v: String?) = Severity.from(v)

    @TypeConverter fun fromFixStatus(v: FixStatus) = v.name
    @TypeConverter fun toFixStatus(v: String?) = FixStatus.from(v)

    @TypeConverter fun fromMemoSource(v: MemoSource) = v.name
    @TypeConverter fun toMemoSource(v: String?) = MemoSource.from(v)

    @TypeConverter fun fromImportance(v: Importance) = v.name
    @TypeConverter fun toImportance(v: String?) = Importance.from(v)

    @TypeConverter fun fromMemoStatus(v: MemoStatus) = v.name
    @TypeConverter fun toMemoStatus(v: String?) = MemoStatus.from(v)

    @TypeConverter fun fromStringList(v: List<String>) = gson.toJson(v)
    @TypeConverter fun toStringList(v: String?): List<String> {
        if (v.isNullOrBlank()) return emptyList()
        return try {
            gson.fromJson(v, object : TypeToken<List<String>>() {}.type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
