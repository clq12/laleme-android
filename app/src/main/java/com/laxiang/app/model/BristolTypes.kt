package com.laxiang.app.model

data class BristolType(
    val type: Int,
    val title: String,
    val emoji: String,
    val subtitle: String,
    val description: String
)

val bristolTypes = listOf(
    BristolType(1, "1型 散状硬球", "🌰", "严重便秘", "像坚果一样的一颗颗硬球，极难排出。通常提示水分和膳食纤维不足。"),
    BristolType(2, "2型 凹凸腊肠", "🪵", "轻微便秘", "表面凹凸不平的腊肠状，提示饮水较少或运动不足。"),
    BristolType(3, "3型 裂纹腊肠", "🥖", "稍微干燥", "表面有裂纹，基本正常，多喝水会更好。"),
    BristolType(4, "4型 光滑香蕉", "🍌", "理想状态", "表面光滑柔软，容易排出，这是最理想的状态。"),
    BristolType(5, "5型 软质断块", "🍞", "纤维偏少", "断块且柔软，提示膳食纤维摄入偏少。"),
    BristolType(6, "6型 糊状泥状", "🥣", "轻微腹泻", "糊状且边缘不规则，可能与饮食、受凉或消化不良有关。"),
    BristolType(7, "7型 水样无固体", "💧", "严重腹泻", "完全水样，可能存在感染或食物中毒，注意补水并考虑就医。")
)

fun bristolTypeOf(type: Int): BristolType = bristolTypes.first { it.type == type }
