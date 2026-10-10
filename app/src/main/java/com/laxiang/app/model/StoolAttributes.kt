package com.laxiang.app.model

data class StoolOption(
    val id: String,
    val label: String,
    val description: String,
    val warning: Boolean = false
)

val stoolColors = listOf(
    StoolOption("brown", "棕色", "常见正常颜色。"),
    StoolOption("dark_brown", "深棕色", "常见正常颜色。"),
    StoolOption("yellow_brown", "黄褐色", "常见正常颜色。"),
    StoolOption("yellow", "黄色", "可能与脂肪消化、胆汁变化或饮食有关。"),
    StoolOption("green", "绿色", "可能与绿叶蔬菜、胆色素变化或肠道蠕动过快有关。"),
    StoolOption(
        "black",
        "黑色",
        "可能提示上消化道出血，也可能与补铁剂、蓝莓等食物有关。",
        warning = true
    ),
    StoolOption(
        "red",
        "红色",
        "可能提示痔疮或下消化道出血，也可能与番茄、西瓜等食物有关。",
        warning = true
    ),
    StoolOption(
        "gray_white",
        "灰白色",
        "可能提示胆道梗阻或胆汁分泌不足。",
        warning = true
    )
)

val stoolAmounts = listOf(
    StoolOption("very_small", "非常少", "碎屑或小块，少于20g，大约2颗葡萄。"),
    StoolOption("small", "偏少", "短条或少量颗粒，约20-50g。"),
    StoolOption("normal", "正常", "成形长条，约100-200g，大约一根香蕉。"),
    StoolOption("large", "偏多", "成形长条或多段，约200-300g。"),
    StoolOption("very_large", "大量", "多段或明显堆叠，超过300g，大约拳头大小。")
)

val stoolSmells = listOf(
    StoolOption("none", "无明显异味", "正常，或与清淡饮食有关。"),
    StoolOption("normal", "普通臭味", "正常肠道菌群代谢产物。"),
    StoolOption("sour", "酸味", "可能与碳水化合物发酵、乳糖或淀粉消化较快有关。"),
    StoolOption(
        "durian",
        "榴莲味",
        "强烈发酵气味，常见于高蛋白、高硫食物或发酵较旺盛。"
    ),
    StoolOption("sulfur", "臭鸡蛋味", "可能与鸡蛋、肉类、洋葱、大蒜等高硫食物有关。"),
    StoolOption("putrid", "腐败味", "可能与蛋白质腐败、便秘或消化不良有关。"),
    StoolOption(
        "fishy",
        "鱼腥味",
        "可能提示感染或炎症。",
        warning = true
    ),
    StoolOption(
        "oily",
        "油腻恶臭",
        "可能与脂肪消化吸收不良有关。",
        warning = true
    ),
    StoolOption("other", "其他", "可在备注中补充具体气味。")
)

val bowelFeelings = listOf(
    StoolOption("smooth", "顺畅", "排便轻松，无需过度用力。"),
    StoolOption("normal", "正常", "整体感受平稳。"),
    StoolOption("strained", "费力", "可能提示粪便偏干或腹压不足。"),
    StoolOption("painful", "疼痛", "可能与肛裂、痔疮或炎症有关。"),
    StoolOption("bloated", "腹胀", "可能与气体积聚、菌群发酵或便秘有关。"),
    StoolOption("urgent", "急迫", "可能提示肠道刺激或蠕动过快。"),
    StoolOption("incomplete", "排便不净", "可能提示直肠刺激、炎症或粪便滞留。")
)

/**
 * 记录当时的「当前状态 / 诱因」，可多选。
 * 目的是事后能回溯：到底是哪些原因让人闹肚子。
 */
val bodyStatuses = listOf(
    StoolOption("cold", "着凉", "腹部受凉后肠道蠕动容易加快，可能腹痛或稀便。"),
    StoolOption("heavy_flavor", "吃了重口", "高油、高辣、重咸的饮食容易刺激肠道。"),
    StoolOption("bad_food", "吃坏肚子", "怀疑食物不洁，常伴随腹痛、腹泻或恶心。"),
    StoolOption("stale_food", "吃了不新鲜", "食物放置过久或已变质，吃完容易肠胃不适。"),
    StoolOption("alcohol", "饮酒", "酒精刺激肠道，可能让大便变稀、次数变多。"),
    StoolOption("late_night", "熬夜", "作息紊乱会影响肠道节律与消化功能。"),
    StoolOption("stress", "压力大", "紧张焦虑容易引起肠道功能紊乱。"),
    StoolOption("dairy", "吃了乳制品", "乳糖不耐受常见腹胀、肠鸣和稀便。"),
    StoolOption("none", "无明显诱因", "没找到明显原因，正常记录即可。")
)

/** 「无明显诱因」与其他状态互斥，避免选出「着凉 + 无明显诱因」这种自相矛盾的组合。 */
const val NO_OBVIOUS_CAUSE_ID = "none"

fun toggleStatus(selected: Set<String>, id: String): Set<String> = when {
    id == NO_OBVIOUS_CAUSE_ID -> if (id in selected) selected - id else setOf(id)
    id in selected -> selected - id
    else -> selected - NO_OBVIOUS_CAUSE_ID + id
}

fun statusLabels(statuses: String?): List<String> =
    statuses
        ?.split(',')
        ?.filter { it.isNotBlank() }
        ?.mapNotNull { statusId -> bodyStatuses.firstOrNull { it.id == statusId }?.label }
        ?: emptyList()

fun statusSummary(statuses: String?): String {
    val labels = statusLabels(statuses)
    return if (labels.isEmpty()) "未记录" else labels.joinToString(" · ")
}

fun stoolOptionOf(options: List<StoolOption>, id: String?): StoolOption? =
    options.firstOrNull { it.id == id }

fun bowelFeelingLabels(feelings: String?): List<String> =
    feelings
        ?.split(',')
        ?.filter { it.isNotBlank() }
        ?.mapNotNull { feelingId -> bowelFeelings.firstOrNull { it.id == feelingId }?.label }
        ?: emptyList()

fun stoolAttributeSummary(
    color: String?,
    amount: String?,
    smell: String?,
    feelings: String?
): String {
    val labels = listOfNotNull(
        stoolOptionOf(stoolColors, color)?.label,
        stoolOptionOf(stoolAmounts, amount)?.label,
        stoolOptionOf(stoolSmells, smell)?.label
    ) + bowelFeelingLabels(feelings)
    return if (labels.isEmpty()) "未记录" else labels.joinToString(" · ")
}
