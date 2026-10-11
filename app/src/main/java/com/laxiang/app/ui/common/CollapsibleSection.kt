package com.laxiang.app.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.laxiang.app.ui.theme.LxShapes
import com.laxiang.app.ui.theme.LxSpacing

/**
 * 可折叠分组卡：用于把长页面按主题分组收起。
 *
 * 设计要点（对齐 ui-ux-pro-max 规范）：
 * - 头部整行可点，点击区域高度不低于 48dp，满足 44×44dp 最小触控要求
 * - 展开/收起带纵向动画，时长 200ms 且有淡入淡出，不突兀
 * - 用旋转箭头表示状态，同时给整行加了 contentDescription，读屏可播报
 * - 收起时不渲染任何内容，首屏可见条目显著增加
 *
 * @param title 分组标题
 * @param subtitle 标题下的一行摘要，收起时也能让用户知道组里有什么
 * @param expanded 当前是否展开（由调用方持有状态，便于记忆）
 * @param onToggle 点击头部回调
 * @param trailing 标题右侧、箭头左侧的附加内容（如状态标签）
 */
@Composable
fun CollapsibleSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "sectionArrow"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = LxShapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(onClick = onToggle)
                    .semantics {
                        contentDescription =
                            "$title，${if (expanded) "已展开，点击收起" else "已收起，点击展开"}"
                    }
                    .padding(horizontal = LxSpacing.lg, vertical = LxSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (trailing != null) {
                    trailing()
                }

                ChevronIndicator(
                    rotation = arrowRotation,
                    modifier = Modifier.padding(start = LxSpacing.sm)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(
                    animationSpec = tween(durationMillis = 200)
                ) + fadeIn(animationSpec = tween(durationMillis = 160)),
                exit = shrinkVertically(
                    animationSpec = tween(durationMillis = 180)
                ) + fadeOut(animationSpec = tween(durationMillis = 120))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = LxSpacing.lg,
                            end = LxSpacing.lg,
                            bottom = LxSpacing.lg
                        ),
                    verticalArrangement = Arrangement.spacedBy(LxSpacing.md),
                    content = content
                )
            }
        }
    }
}

/**
 * 折叠指示箭头：纯矢量绘制，不依赖图标库、也不用 emoji。
 * 画一个向下的折线箭头，展开时旋转 180 度。
 */
@Composable
private fun ChevronIndicator(
    rotation: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(20.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .size(12.dp)
                .rotate(rotation)
        ) {
            val w = size.width
            val h = size.height
            val stroke = 1.8.dp.toPx()
            // 向下箭头：三点折线
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.15f, h * 0.35f)
                lineTo(w * 0.5f, h * 0.68f)
                lineTo(w * 0.85f, h * 0.35f)
            }
            drawPath(
                path = path,
                color = Color(0xFF8A8A8A),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = stroke,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )
        }
    }
}

/**
 * 细分隔线：用于折叠组内部把不同小节分开。
 *
 * 手绘而不是用 Material3 的 Divider/HorizontalDivider，原因是这两个 API
 * 在不同 Material3 版本里名字变过，跨版本编译不稳定；一条 1px 线自己画最省事。
 */
@Composable
fun LxDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = LxSpacing.sm)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    )
}

/**
 * 分区小标题：用于折叠组之外的独立区块，统一视觉层级。
 */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = LxSpacing.xs, bottom = LxSpacing.sm)
    )
}

/**
 * 小圆点标记：给分组标题做视觉点缀，替代 emoji。
 */
@Composable
fun DotMarker(
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(8.dp)
            .background(color, CircleShape)
    )
}
