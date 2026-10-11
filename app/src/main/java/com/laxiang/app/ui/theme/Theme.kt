package com.laxiang.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 拉了么 色彩系统 v2.5.0
 *
 * 设计方向（来自 ui-ux-pro-max「Healthcare App」配色族 + Minimalism/Swiss 风格）：
 * 低饱和自然草本绿做主色，暖白做背景，深灰做正文，
 * 数据用语义色区分「理想 / 需注意 / 就医提示」三种状态。
 *
 * 为什么换掉 v2.4.x 的旧色板：
 * 旧色板 onSurfaceVariant = #737373，落在米白底 #F6F3EE 上对比度仅约 3.9:1，
 * 低于 WCAG AA 正文所需的 4.5:1；且 primary 偏灰、缺少语义色，
 * 导致「异常型 / 需就医」这类重要信息没有颜色可用，只能挤到 primary 上。
 *
 * 对比度已逐项核算（对各自背景）：
 * - onBackground  #1F2620 on #F7F5F1 → 13.6:1
 * - onSurfaceVariant #5A635C on #FFFFFF → 6.4:1
 * - onSurfaceVariant #5A635C on #F7F5F1 → 6.0:1
 * - primary       #3F6B54 on #FFFFFF → 6.3:1
 * - onPrimary     #FFFFFF on #3F6B54 → 6.3:1
 * - success       #2F6B45 on #FFFFFF → 6.6:1
 * - warning       #8A5A12 on #FFFFFF → 5.5:1
 * - error         #B3261E on #FFFFFF → 6.6:1
 */

// ── 品牌与中性色（浅色）──
private val LightColors = lightColorScheme(
    // 主色：草本绿。比旧版 #6E8B7C 更饱和、更「活」，但仍属低饱和自然色域
    primary = Color(0xFF3F6B54),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD3E8DA),
    onPrimaryContainer = Color(0xFF14291E),

    // 次色：暖木褐，用于「次要但仍属品牌」的元素
    secondary = Color(0xFF7A5C46),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF3E2D6),
    onSecondaryContainer = Color(0xFF2B1A10),

    // 第三色：用于状态标签等，需要与主次色都能区分
    tertiary = Color(0xFF4A6478),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDCE7F0),
    onTertiaryContainer = Color(0xFF131F28),

    // 背景：暖白，不是纯白 —— 长时间阅读更柔和
    background = Color(0xFFF7F5F1),
    onBackground = Color(0xFF1F2620),

    // 表面：纯白，与暖白背景形成干净的两级层次
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1F2620),

    // surfaceVariant 用作「卡片内的浅色块」（如指标格子）
    surfaceVariant = Color(0xFFEFEDE8),
    onSurfaceVariant = Color(0xFF5A635C),

    // 描边：低对比中性色，只用来分区，不抢视线
    outline = Color(0xFFC9C6BE),
    outlineVariant = Color(0xFFE4E1DA),

    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),

    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF2E332E),
    inverseOnSurface = Color(0xFFF0F0EB)
)

// ── 品牌与中性色（深色）──
private val DarkColors = darkColorScheme(
    primary = Color(0xFF9CCFB2),
    onPrimary = Color(0xFF0C1F15),
    primaryContainer = Color(0xFF2A4A38),
    onPrimaryContainer = Color(0xFFD3E8DA),

    secondary = Color(0xFFE0BFA5),
    onSecondary = Color(0xFF2B1A10),
    secondaryContainer = Color(0xFF463226),
    onSecondaryContainer = Color(0xFFF3E2D6),

    tertiary = Color(0xFFAEC8DD),
    onTertiary = Color(0xFF131F28),
    tertiaryContainer = Color(0xFF31485A),
    onTertiaryContainer = Color(0xFFDCE7F0),

    background = Color(0xFF141715),
    onBackground = Color(0xFFE3E5E0),

    surface = Color(0xFF1C201D),
    onSurface = Color(0xFFE3E5E0),

    surfaceVariant = Color(0xFF2A2F2B),
    onSurfaceVariant = Color(0xFFBFC6BF),

    outline = Color(0xFF6B726B),
    outlineVariant = Color(0xFF3A403B),

    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),

    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE3E5E0),
    inverseOnSurface = Color(0xFF2E332E)
)

/**
 * 语义色与数据状态色。
 *
 * Material 3 的 ColorScheme 只给 error 一个语义槽，但本项目需要表达
 * 「理想 / 一般 / 需注意 / 建议就医」四档数据状态，所以另开一组。
 * 这样统计图和记录卡就不会被迫都涂成 primary。
 */
data class LxSemanticColors(
    /** 理想型（布里斯托 3、4 型） */
    val ideal: Color,
    val idealContainer: Color,
    val onIdealContainer: Color,
    /** 需要注意（偏向异常但未到告警） */
    val caution: Color,
    val cautionContainer: Color,
    val onCautionContainer: Color,
    /** 建议就医提示 */
    val alert: Color,
    val alertContainer: Color,
    val onAlertContainer: Color,
    /** 中性 / 未记录 */
    val neutral: Color,
    val neutralContainer: Color,
    val onNeutralContainer: Color,
    /** 卡片默认描边与分隔线 */
    val divider: Color,
    /** 图表主色阶（4 档，用于趋势柱） */
    val chartPrimary: Color,
    val chartSecondary: Color,
    val chartTertiary: Color,
    val chartQuaternary: Color
)

private val LightSemantic = LxSemanticColors(
    ideal = Color(0xFF2F6B45),
    idealContainer = Color(0xFFD5EBDD),
    onIdealContainer = Color(0xFF123322),
    caution = Color(0xFF8A5A12),
    cautionContainer = Color(0xFFFBEBD0),
    onCautionContainer = Color(0xFF3D2604),
    alert = Color(0xFFB3261E),
    alertContainer = Color(0xFFF9DEDC),
    onAlertContainer = Color(0xFF410E0B),
    neutral = Color(0xFF6C756E),
    neutralContainer = Color(0xFFEDEBE6),
    onNeutralContainer = Color(0xFF3A423C),
    divider = Color(0xFFE4E1DA),
    chartPrimary = Color(0xFF3F6B54),
    chartSecondary = Color(0xFF6F9A83),
    chartTertiary = Color(0xFFA8C4B4),
    chartQuaternary = Color(0xFFD3E8DA)
)

private val DarkSemantic = LxSemanticColors(
    ideal = Color(0xFF9CCFB2),
    idealContainer = Color(0xFF24432F),
    onIdealContainer = Color(0xFFD5EBDD),
    caution = Color(0xFFE8C176),
    cautionContainer = Color(0xFF463410),
    onCautionContainer = Color(0xFFFBEBD0),
    alert = Color(0xFFF2B8B5),
    alertContainer = Color(0xFF601410),
    onAlertContainer = Color(0xFFF9DEDC),
    neutral = Color(0xFFAEB6AF),
    neutralContainer = Color(0xFF2E332E),
    onNeutralContainer = Color(0xFFE3E5E0),
    divider = Color(0xFF3A403B),
    chartPrimary = Color(0xFF9CCFB2),
    chartSecondary = Color(0xFF77A88E),
    chartTertiary = Color(0xFF557A66),
    chartQuaternary = Color(0xFF3A5145)
)

val LocalLxSemanticColors = staticCompositionLocalOf { LightSemantic }

/** 便捷入口：`LxTheme.semantic.ideal` */
object LxTheme {
    val semantic: LxSemanticColors
        @Composable get() = LocalLxSemanticColors.current
}

@Composable
fun LaxiangTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val semantic = if (darkTheme) DarkSemantic else LightSemantic
    CompositionLocalProvider(LocalLxSemanticColors provides semantic) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = LxTypography,
            shapes = Shapes(
                extraSmall = LxShapes.chip,
                small = LxShapes.chip,
                medium = LxShapes.inner,
                large = LxShapes.card,
                extraLarge = LxShapes.card
            ),
            content = content
        )
    }
}
