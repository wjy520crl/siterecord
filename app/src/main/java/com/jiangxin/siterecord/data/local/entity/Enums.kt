package com.jiangxin.siterecord.data.local.entity

/** 装修阶段 */
enum class ProjectStage {
    设计, 拆改, 水电, 泥木, 油漆, 安装, 验收;

    companion object {
        fun from(name: String?) = entries.firstOrNull { it.name == name } ?: 设计
    }
}

/** 项目状态 */
enum class ProjectStatus {
    进行中, 已完工, 已验收;

    companion object {
        fun from(name: String?) = entries.firstOrNull { it.name == name } ?: 进行中
    }
}

/** 巡查情况：合格无问题 / 需整改 / 复检 */
enum class InspectionSituation {
    合格无问题, 需整改, 复检;

    companion object {
        fun from(name: String?) = entries.firstOrNull { it.name == name } ?: 需整改
    }
}

/** 问题严重度 */
enum class Severity {
    一般, 重要, 紧急;

    companion object {
        fun from(name: String?) = entries.firstOrNull { it.name == name } ?: 一般
    }
}

/** 整改状态 */
enum class FixStatus {
    未整改, 整改中, 已整改, 已验收;

    companion object {
        fun from(name: String?) = entries.firstOrNull { it.name == name } ?: 未整改
    }
}

/** 备案录来源 */
enum class MemoSource {
    业主, 设计师, 工长, 自记;

    companion object {
        fun from(name: String?) = entries.firstOrNull { it.name == name } ?: 业主
    }
}

/** 备案录重要度 */
enum class Importance {
    普通, 重要, 关键;

    companion object {
        fun from(name: String?) = entries.firstOrNull { it.name == name } ?: 普通
    }
}

/** 备案录状态机 */
enum class MemoStatus {
    待办, 进行中, 已完成, 已反馈业主;

    companion object {
        fun from(name: String?) = entries.firstOrNull { it.name == name } ?: 待办
    }
}
