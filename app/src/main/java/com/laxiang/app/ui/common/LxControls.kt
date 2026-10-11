package com.laxiang.app.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.laxiang.app.model.StoolOption
import com.laxiang.app.ui.theme.LxShapes
import com.laxiang.app.ui.theme.LxSizes
import com.laxiang.app.ui.theme.LxSpacing

/**
 * 选项标签流式布局（单选 / 多选通用）。
 *
 * 为什么从「横向滚动」改成「自动换行」：
 * 原实现用 horizontalScroll + 固定宽度方块，一屏只能看到 3~4 个选项，
 * 「油腻恶臭」「排便不净」这类排在后面的选项用户根本不知道存在，
 * 属于真实的功能可见性缺陷。FlowRow 自动换行后全部选项一眼可见，
 * 且省掉一次横向滑动操作。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LxOptionFlow(
    options: List<StoolOption>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    accentContainer: Color = MaterialTheme.colorScheme.primaryContainer,
    onAccentContainer: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LxSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(LxSpacing.sm)
    ) {
        options.forEach { option ->
            val selected = option.id in selectedIds
            LxOptionChip(
                label = option.label,
                selected = selected,
                onClick = { onToggle(option.id) },
                accent = accent,
                accentContainer = accentContainer,
                onAccentContainer = onAccentContainer
            )
        }
    }
}

/**
 * 单个选项标签。
 *
 * 可点区域高度固定 40dp 且整块可点；外层 FlowRow 的 8dp 间距
 * 让相邻标签之间留出足够间隔，避免误触。
 * 选中状态同时用「填充色 + 描边 + 字重」三重表达，
 * 不单靠颜色传达含义（色觉障碍用户也能分辨）。
 */
@Composable
fun LxOptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    accentContainer: Color = MaterialTheme.colorScheme.primaryContainer,
    onAccentContainer: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    enabled: Boolean = true
) {
    val container by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.surfaceVariant
            selected -> accentContainer
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(150),
        label = "chipContainer"
    )
    val border by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.outlineVariant
            selected -> accent
            else -> MaterialTheme.colorScheme.outline
        },
        animationSpec = tween(150),
        label = "chipBorder"
    )
    val content by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
            selected -> onAccentContainer
            else -> MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(150),
        label = "chipContent"
    )

    Box(
        modifier = modifier
            .heightIn(min = 40.dp)
            .widthIn(min = LxSizes.optionMinWidth)
            .clip(LxShapes.inner)
            .background(container)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = border,
                shape = LxShapes.inner
            )
            .clickable(enabled = enabled, onClick = onClick)
            .semantics {
                this.selected = selected
                role = Role.Checkbox
            }
            .padding(horizontal = LxSpacing.md, vertical = LxSpacing.sm),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = content,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/**
 * 布里斯托形态选择块：带矢量形态图示。
 *
 * 图示 + 文字 + 序号三重编码，用户不需要记住「4 型」是什么，
 * 看图形就能对上号；同时也照顾了看不懂图示的用户（保留文字）。
 */
@Composable
fun LxBristolTile(
    type: Int,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    accent: Color,
    accentContainer: Color,
    onAccentContainer: Color,
    modifier: Modifier = Modifier
) {
    val container by animateColorAsState(
        targetValue = if (selected) accentContainer else MaterialTheme.colorScheme.surface,
        animationSpec = tween(150),
        label = "tileContainer"
    )
    val border by animateColorAsState(
        targetValue = if (selected) accent else MaterialTheme.colorScheme.outline,
        animationSpec = tween(150),
        label = "tileBorder"
    )
    val glyphTint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .widthIn(min = LxSizes.typeTileWidth)
            .heightIn(min = LxSizes.typeTileHeight)
            .clip(LxShapes.inner)
            .background(container)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = border,
                shape = LxShapes.inner
            )
            .clickable(onClick = onClick)
            .semantics {
                this.selected = selected
                role = Role.RadioButton
            }
            .padding(vertical = LxSpacing.md, horizontal = LxSpacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LxBristolShape(type = type, tint = glyphTint, size = 26.dp)
        Spacer(modifier = Modifier.height(LxSpacing.sm))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (selected) onAccentContainer else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 分区标题 + 可选副标题，用于表单内把字段归组。
 */
@Composable
fun LxFieldLabel(
    text: String,
    modifier: Modifier = Modifier,
    optional: Boolean = false,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (optional) {
            Spacer(modifier = Modifier.size(LxSpacing.sm))
            Box(
                modifier = Modifier
                    .clip(LxShapes.pill)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = LxSpacing.sm, vertical = 2.dp)
            ) {
                Text(
                    text = "选填",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.weight(1f))
            trailing()
        }
    }
}
