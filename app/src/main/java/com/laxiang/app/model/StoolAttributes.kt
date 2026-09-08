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
