package com.laxiang.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 间距令牌：统一全 App 的留白节奏。
 * 只允许使用这一套值，避免各处 10/14/18 这类随手数字导致节奏散乱。
 */
object LxSpacing {
    /** 4dp —— 图标与紧邻文字之间 */
    val xs = 4.dp
    /** 8dp —— 同一组内的行间距 */
    val sm = 8.dp
    /** 12dp —— 卡片内部元素间距 */
    val md = 12.dp
    /** 16dp —— 卡片之间的间距、屏幕左右边距 */
    val lg = 16.dp
    /** 24dp —— 分组区块之间的大间距 */
    val xl = 24.dp
    /** 32dp —— 页面首尾留白 */
    val xxl = 32.dp

    /** 屏幕内容统一左右边距 */
    val screenHorizontal = lg
}

/**
 * 圆角令牌：卡片与内层元素两级，不再混用 8/12/16/20。
 */
object LxShapes {
    /** 外层卡片 16dp */
    val card = RoundedCornerShape(16.dp)
    /** 内层元素（输入框、小块、chip）12dp */
    val inner = RoundedCornerShape(12.dp)
    /** 小标记（徽标、色块）8dp */
    val chip = RoundedCornerShape(8.dp)
    /** 整条圆角（进度条、分隔块） */
    val pill = RoundedCornerShape(50)
}

/**
 * 排版令牌：建立清晰的字号梯度，避免到处都是 14sp / 16sp 混用。
 * 全部基于 MaterialTheme.typography 扩展语义，不硬编码到组件里。
 */
val LxTypography = Typography().let { base ->
    base.copy(
        // 页面大标题
        headlineSmall = base.headlineSmall.copy(
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Medium
        ),
        // 卡片标题
        titleMedium = base.titleMedium.copy(
            fontSize = 16.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Medium
        ),
        // 分组标题（折叠区头部）
        titleSmall = base.titleSmall.copy(
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium
        ),
        bodyLarge = base.bodyLarge.copy(
            fontSize = 15.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Normal
        ),
        bodyMedium = base.bodyMedium.copy(
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Normal
        ),
        // 辅助说明、单位、时间
        bodySmall = base.bodySmall.copy(
            fontSize = 12.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Normal
        ),
        // 指标大数字
        displaySmall = TextStyle(
            fontSize = 28.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Medium
        )
    )
}
