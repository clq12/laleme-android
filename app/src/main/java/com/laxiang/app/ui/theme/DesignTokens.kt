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

    /** 卡片内部统一内边距 */
    val cardPadding = lg

    /** 列表底部留白，避免最后一项贴着导航栏 */
    val listBottomInset = 28.dp
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
 * 尺寸令牌：触控区域、图标、图形元素。
 * 48dp 是 Material 规范的最小触控目标，关键可点元素都得达到。
 */
object LxSizes {
    /** 最小触控高度（Material 规范） */
    val touchTarget = 48.dp
    /** 主操作按钮高度 */
    val buttonLarge = 52.dp
    /** 普通按钮高度 */
    val button = 44.dp

    // ── 图标尺寸梯度 ────────────────────────────────
    // 规范要求：图标尺寸必须走令牌，不要到处散落 20/24/28 这类随意值，
    // 否则同一个界面里图标的视觉重量会飘。这里定三档够用：
    //   iconSm 20dp —— 行内/紧凑场景（列表项右侧、chip 内）
    //   iconMd 24dp —— 标准控件（导航栏、图标按钮、标题左侧）
    //   iconLg 32dp —— 空状态/功能锚点这类需要视觉重量的地方
    /** 小图标 */
    val iconSm = 20.dp
    /**
     * 标准图标：与 M3 NavigationBarTokens.IconSize 一致（24dp）。
     *
     * 注意：M3 的 `NavigationBarItem` 会原样测量 icon 槽位并在整栏宽度内居中，
     * 而 label 走的是另一条（带 4dp 水平内缩的）排版路径。
     * 所以手绘图标必须保证「24dp 画布内的图形光学中心 = (12,12)」，
     * 否则会肉眼可见地相对文字偏左/偏上。校验见 _verify_icons.py。
     */
    val iconMd = 24.dp
    /** 大图标 */
    val iconLg = 32.dp
    /** 导航图标（语义别名，值同 iconMd） */
    val iconNav = iconMd
    /** 卡片标题左侧的图标容器 */
    val iconBadge = 44.dp
    /** 布里斯托选择块 */
    val typeTileWidth = 76.dp
    val typeTileHeight = 84.dp
    /** 选项标签最小宽度 */
    val optionMinWidth = 72.dp
    /** 色板圆点 */
    val swatch = 10.dp
    /** 趋势柱条宽度 */
    val trendBar = 10.dp
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
        // 分组标题（折叠区头部）/ 小节标题
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
