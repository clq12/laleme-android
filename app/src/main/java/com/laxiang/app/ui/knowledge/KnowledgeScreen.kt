package com.laxiang.app.ui.knowledge

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.laxiang.app.model.StoolOption
import com.laxiang.app.model.bristolTypes
import com.laxiang.app.model.bodyStatuses
import com.laxiang.app.model.stoolAmounts
import com.laxiang.app.model.stoolColors
import com.laxiang.app.model.stoolSmells
import com.laxiang.app.ui.common.LxBristolShape
import com.laxiang.app.ui.common.LxIconCaution
import com.laxiang.app.ui.common.LxIconIdeal
import com.laxiang.app.ui.common.LxIconLeaf
import com.laxiang.app.ui.common.LxIconWarning
import com.laxiang.app.ui.theme.LxShapes
import com.laxiang.app.ui.theme.LxSpacing
import com.laxiang.app.ui.theme.LxTheme

/**
 * 健康知识页 v2.5.0
 *
 * 定位从「一长串同构卡片」改成「可以顺手翻阅的指南」：
 * - 每个大节开头有一段导读，先讲清楚「这一节在讲什么、怎么看」；
 * - 布里斯托 7 型用矢量形态图 + 理想/留意分组色，形态差异一眼可比；
 * - 颜色/气味/状态按「常见 → 需要留意 → 建议就医」排序，
 *   并把 warning 项统一挂上语义色标记，扫一眼就知道哪些要当回事。
 *
 * 注意：本页只做「呈现方式」改版，所有医学描述文本均沿用原有数据源，
 * 未新增、未修改任何医学结论。
 */

/** 颜色色板对应的实际色值，仅用于展示，不改动任何知识文案 */
private fun colorSwatchOf(id: String): Color = when (id) {
    "brown" -> Color(0xFF7B5137)
    "dark_brown" -> Color(0xFF452B1E)
    "yellow" -> Color(0xFFC9A227)
    "green" -> Color(0xFF4E7A46)
    "black" -> Color(0xFF2B2B2B)
    "red" -> Color(0xFF9E2B25)
    "pale" -> Color(0xFFE0D8C3)
    "white" -> Color(0xFFF2F0EA)
    else -> Color(0xFF9A9A94)
}

@Composable
fun KnowledgeScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = LxSpacing.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(LxSpacing.sm)
    ) {
        item { KnowledgeHeader() }

        // ── 第一节：布里斯托分型 ──
        item {
            SectionIntro(
                title = "布里斯托大便分类",
                lead = "国际通用的 7 型分类法。对照形态图看今天的记录属于哪一型，" +
                    "3 型和 4 型属于理想状态，其余偏硬或偏稀都提示需要留意。"
            )
        }
        item { BristolLegend() }
        items(bristolTypes) { type ->
            BristolTypeRow(
                type = type.type,
                title = type.title,
                subtitle = type.subtitle,
                description = type.description
            )
        }

        // ── 第二节：颜色 ──
        item {
            SectionIntro(
                title = "颜色可能的原因",
                lead = "颜色主要受饮食、消化速度和胆汁影响。深褐到黄褐属常见范围，" +
                    "出现黑、红、白这三种需要优先排除出血或胆道问题。"
            )
        }
        items(stoolColors) { option ->
            StoolKnowledgeRow(option = option, swatch = colorSwatchOf(option.id))
        }

        // ── 第三节：气味 ──
        item {
            SectionIntro(
                title = "气味可能的原因",
                lead = "气味与食物成分、菌群发酵和消化吸收有关。" +
                    "多数气味改变通过调整饮食即可缓解，持续恶臭才需要考虑就医。"
            )
        }
        items(stoolSmells) { option ->
            StoolKnowledgeRow(option = option, swatch = null)
        }

        // ── 第四节：分量（原页面未单列，与数据源保持一致地补上索引）──
        item {
            SectionIntro(
                title = "分量参考",
                lead = "分量是相对主观的指标，适合纵向对比自己的变化，而不必和他人比较。"
            )
        }
        items(stoolAmounts) { option ->
            StoolKnowledgeRow(option = option, swatch = null)
        }

        // ── 第五节：当前状态 ──
        item {
            SectionIntro(
                title = "哪些原因在影响你",
                lead = "记录时勾选的「当前状态」都在这里。回头看一看统计页的状态分布，" +
                    "就能知道哪些诱因最容易让你闹肚子。"
            )
        }
        items(bodyStatuses) { option ->
            StoolKnowledgeRow(option = option, swatch = null)
        }

        item {
            MedicalDisclaimer()
        }

        item {
            Spacer(modifier = Modifier.height(LxSpacing.listBottomInset))
        }
    }
}

@Composable
private fun KnowledgeHeader() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(LxSpacing.xl))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(LxShapes.inner)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                LxIconLeaf(tint = MaterialTheme.colorScheme.onPrimaryContainer, size = 24.dp)
            }
            Spacer(modifier = Modifier.size(LxSpacing.md))
            Column {
                Text(
                    text = "健康知识",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "读懂身体给出的信号",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 小节导读。
 * 用一张带左侧色条的浅色卡，把「这节讲什么」和下面的条目区分开，
 * 避免整页同构卡片导致阅读节奏完全平掉。
 */
@Composable
private fun SectionIntro(title: String, lead: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = LxSpacing.lg)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(18.dp)
                    .clip(LxShapes.pill)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.size(LxSpacing.sm))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(modifier = Modifier.height(LxSpacing.sm))
        Text(
            text = lead,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** 布里斯托图例：先给出「哪种算理想」的判断标准，再看逐条明细 */
@Composable
private fun BristolLegend() {
    val semantic = LxTheme.semantic
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LxShapes.inner)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(LxSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(LxSpacing.lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(color = semantic.ideal, label = "3 / 4 型 · 理想")
        LegendItem(color = semantic.caution, label = "其余 · 需留意")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(LxShapes.pill)
                .background(color)
        )
        Spacer(modifier = Modifier.size(LxSpacing.sm))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BristolTypeRow(
    type: Int,
    title: String,
    subtitle: String,
    description: String
) {
    val semantic = LxTheme.semantic
    val ideal = type == 3 || type == 4
    val accent = if (ideal) semantic.ideal else semantic.caution
    val container = if (ideal) semantic.idealContainer else semantic.cautionContainer
    val onContainer = if (ideal) semantic.onIdealContainer else semantic.onCautionContainer

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(LxSpacing.cardPadding)) {
            Column(
                modifier = Modifier.width(56.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(LxShapes.inner)
                        .background(container),
                    contentAlignment = Alignment.Center
                ) {
                    LxBristolShape(type = type, tint = onContainer, size = 30.dp)
                }
                Spacer(modifier = Modifier.height(LxSpacing.xs))
                Text(
                    text = "$type 型",
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.size(LxSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (ideal) {
                        LxIconIdeal(tint = semantic.ideal, size = 16.dp)
                        Spacer(modifier = Modifier.size(LxSpacing.xs))
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = accent
                )
                Spacer(modifier = Modifier.height(LxSpacing.sm))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StoolKnowledgeRow(option: StoolOption, swatch: Color?) {
    val semantic = LxTheme.semantic

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(LxSpacing.cardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 颜色类条目左侧放色板；其余条目放状态图标，保持行首视觉节奏一致
                if (swatch != null) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(LxShapes.chip)
                            .background(swatch)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = LxShapes.chip
                            )
                    )
                } else {
                    LxIconCaution(
                        tint = if (option.warning) semantic.caution else semantic.neutral,
                        size = 20.dp
                    )
                }
                Spacer(modifier = Modifier.size(LxSpacing.md))
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (option.warning) {
                    Box(
                        modifier = Modifier
                            .clip(LxShapes.pill)
                            .background(semantic.alertContainer)
                            .padding(horizontal = LxSpacing.sm, vertical = 2.dp)
                    ) {
                        Text(
                            text = "需留意",
                            style = MaterialTheme.typography.bodySmall,
                            color = semantic.onAlertContainer
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(LxSpacing.sm))
            Text(
                text = option.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (option.warning) {
                Spacer(modifier = Modifier.height(LxSpacing.sm))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(LxShapes.inner)
                        .background(semantic.alertContainer)
                        .padding(LxSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LxIconWarning(tint = semantic.alert, size = 16.dp)
                    Spacer(modifier = Modifier.size(LxSpacing.sm))
                    Text(
                        text = "如持续出现建议就医",
                        style = MaterialTheme.typography.bodySmall,
                        color = semantic.onAlertContainer
                    )
                }
            }
        }
    }
}

/**
 * 免责提示。
 * 属于「呈现层」补充，只重复既有的「建议就医」口径，不新增任何医学判断，
 * 目的是让用户清晰知道这个 App 的定位是记录工具而非诊断工具。
 */
@Composable
private fun MedicalDisclaimer() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = LxSpacing.lg)
            .clip(LxShapes.inner)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(LxSpacing.cardPadding),
        verticalAlignment = Alignment.Top
    ) {
        LxIconWarning(tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 18.dp)
        Spacer(modifier = Modifier.size(LxSpacing.md))
        Text(
            text = "以上内容仅用于帮助你理解记录里的各项含义，不能替代医生诊断。" +
                "如果出现持续不适，请及时就医。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
