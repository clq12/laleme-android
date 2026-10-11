package com.laxiang.app.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.laxiang.app.data.RecordEntity
import com.laxiang.app.data.SectionPrefs
import com.laxiang.app.ui.common.CollapsibleSection
import com.laxiang.app.ui.common.LxBristolShape
import com.laxiang.app.ui.common.LxIconCalendar
import com.laxiang.app.ui.common.LxIconChevronLeft
import com.laxiang.app.ui.common.LxIconChevronRight
import com.laxiang.app.ui.common.LxIconClock
import com.laxiang.app.ui.common.LxIconIdeal
import com.laxiang.app.ui.common.LxOptionChip
import com.laxiang.app.ui.common.LaxiangDatePickerDialog
import com.laxiang.app.ui.theme.LxShapes
import com.laxiang.app.ui.theme.LxSizes
import com.laxiang.app.ui.theme.LxSpacing
import com.laxiang.app.ui.theme.LxTheme
import com.laxiang.app.model.bristolTypes
import com.laxiang.app.model.formatDuration
import com.laxiang.app.model.StoolOption
import com.laxiang.app.model.bodyStatuses
import com.laxiang.app.model.stoolAmounts
import com.laxiang.app.model.stoolColors
import com.laxiang.app.model.stoolSmells
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** 统计范围：默认只看最近 14 天，避免历史数据把当前状态冲淡。 */
private enum class StatsRange(val label: String) {
    Last14Days("近 14 天"),
    AllTime("全部历史"),
    Custom("自定义")
}

private val dayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(records: List<RecordEntity>) {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)

    var rangeMode by remember { mutableStateOf(StatsRange.Last14Days) }
    var customStart by remember { mutableStateOf(today.minusDays(13)) }
    var customEnd by remember { mutableStateOf(today) }
    var showDatePicker by remember { mutableStateOf(false) }
    var pickingStart by remember { mutableStateOf(true) }

    // 折叠状态：首次进入展开「概览与趋势」，收起其余两组，保持首屏清爽
    val context = LocalContext.current
    val sectionPrefs = remember { SectionPrefs(context) }
    var overviewExpanded by remember {
        mutableStateOf(sectionPrefs.isExpanded(SectionPrefs.STATS_OVERVIEW, true))
    }
    var distributionExpanded by remember {
        mutableStateOf(sectionPrefs.isExpanded(SectionPrefs.STATS_DISTRIBUTION, false))
    }
    var calendarExpanded by remember {
        mutableStateOf(sectionPrefs.isExpanded(SectionPrefs.STATS_CALENDAR, false))
    }

    val recordDates = remember(records, zone) {
        records.map { LocalDate.ofInstant(Instant.ofEpochMilli(it.occurredAt), zone) }
    }
    val firstDate = recordDates.minOrNull()

    val rangeStart: LocalDate
    val rangeEnd: LocalDate
    when (rangeMode) {
        StatsRange.Last14Days -> {
            rangeStart = today.minusDays(13)
            rangeEnd = today
        }
        StatsRange.AllTime -> {
            rangeStart = firstDate ?: today
            rangeEnd = today
        }
        StatsRange.Custom -> {
            // 起止日期选反了也自动纠正，不用用户自己发现
            rangeStart = if (customStart.isAfter(customEnd)) customEnd else customStart
            rangeEnd = if (customStart.isAfter(customEnd)) customStart else customEnd
        }
    }

    val rangeRecords = remember(records, rangeStart, rangeEnd, zone) {
        records.filter {
            val date = LocalDate.ofInstant(Instant.ofEpochMilli(it.occurredAt), zone)
            !date.isBefore(rangeStart) && !date.isAfter(rangeEnd)
        }
    }

    val grouped = remember(records, zone) {
        records.groupBy { LocalDate.ofInstant(Instant.ofEpochMilli(it.occurredAt), zone) }
    }

    val total = rangeRecords.size
    val healthyCount = rangeRecords.count { it.bristolType == 3 || it.bristolType == 4 }
    val healthRate = if (total == 0) 0 else healthyCount * 100 / total
    val averageDuration = if (total == 0) 0 else rangeRecords.sumOf { it.durationSeconds } / total
    val spanDays = (ChronoUnit.DAYS.between(rangeStart, rangeEnd).toInt() + 1).coerceAtLeast(1)
    val perDay = total.toDouble() / spanDays
    val spanText = "${dayFormatter.format(rangeStart)} ~ ${dayFormatter.format(rangeEnd)} · 共 $spanDays 天"

    if (showDatePicker) {
        val initialDate = if (pickingStart) rangeStart else rangeEnd
        LaxiangDatePickerDialog(
            initialDate = initialDate,
            maxDate = today, // 未来的日期没有意义，直接禁掉
            onDismiss = { showDatePicker = false },
            onConfirm = { picked ->
                if (pickingStart) {
                    customStart = picked
                    if (customEnd.isBefore(picked)) customEnd = picked
                } else {
                    customEnd = picked
                    if (customStart.isAfter(picked)) customStart = picked
                }
                rangeMode = StatsRange.Custom
                showDatePicker = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = LxSpacing.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(LxSpacing.md)
    ) {
        item {
            // 顶部让开状态栏：Scaffold 已经把 safeDrawing 顶边作为 body 上内边距，
            // 这里只留一段视觉呼吸间距（原先用 xl=24dp 硬凑状态栏高度，会双重留白）。
            Spacer(modifier = Modifier.height(LxSpacing.sm))
            Text(
                text = "数据大盘",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(LxSpacing.xs))
            Text(
                text = "下面的统计都在同一范围内计算，默认最近 14 天",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            RangeSelectorCard(
                rangeMode = rangeMode,
                onSelect = { rangeMode = it },
                customStart = customStart,
                customEnd = customEnd,
                onPickStart = {
                    pickingStart = true
                    showDatePicker = true
                },
                onPickEnd = {
                    pickingStart = false
                    showDatePicker = true
                },
                spanText = spanText
            )
        }

        // ── 分组一：概览与趋势 ──
        item {
            CollapsibleSection(
                title = "概览与趋势",
                subtitle = "$spanText",
                expanded = overviewExpanded,
                onToggle = {
                    overviewExpanded = !overviewExpanded
                    sectionPrefs.setExpanded(SectionPrefs.STATS_OVERVIEW, overviewExpanded)
                }
            ) {
                OverviewCard(
                    rangeLabel = rangeMode.label,
                    spanText = spanText,
                    total = total,
                    perDay = perDay,
                    averageDuration = averageDuration,
                    healthRate = healthRate
                )
                TrendCard(grouped = grouped, endDate = rangeEnd)
            }
        }

        // ── 分组二：分布分析（类型 / 属性 / 状态）──
        item {
            CollapsibleSection(
                title = "分布分析",
                subtitle = "类型 · 颜色 · 分量 · 气味 · 状态",
                expanded = distributionExpanded,
                onToggle = {
                    distributionExpanded = !distributionExpanded
                    sectionPrefs.setExpanded(SectionPrefs.STATS_DISTRIBUTION, distributionExpanded)
                }
            ) {
                TypeDistributionCard(records = rangeRecords, rangeLabel = rangeMode.label)

                AttributeDistributionCard(
                    title = "颜色分布",
                    options = stoolColors,
                    records = rangeRecords,
                    rangeLabel = rangeMode.label,
                    selector = { it.stoolColor }
                )

                AttributeDistributionCard(
                    title = "分量分布",
                    options = stoolAmounts,
                    records = rangeRecords,
                    rangeLabel = rangeMode.label,
                    selector = { it.stoolAmount }
                )

                AttributeDistributionCard(
                    title = "气味分布",
                    options = stoolSmells,
                    records = rangeRecords,
                    rangeLabel = rangeMode.label,
                    selector = { it.stoolSmell }
                )

                // v2.4.2 补齐：此卡片代码早已存在，但此前从未被渲染，
                // 导致「当前状态」的分布统计在页面上看不到。
                StatusDistributionCard(records = rangeRecords, rangeLabel = rangeMode.label)
            }
        }

        // ── 分组三：日历 ──
        item {
            CollapsibleSection(
                title = "日历",
                subtitle = "按月查看记录分布，点某天可只看那天",
                expanded = calendarExpanded,
                onToggle = {
                    calendarExpanded = !calendarExpanded
                    sectionPrefs.setExpanded(SectionPrefs.STATS_CALENDAR, calendarExpanded)
                }
            ) {
                CalendarCard(
                    grouped = grouped,
                    rangeStart = rangeStart,
                    rangeEnd = rangeEnd,
                    onSelectDate = { date ->
                        // 点某天 = 把统计范围切到那一天，顺手回答「这天到底怎么样」
                        customStart = date
                        customEnd = date
                        rangeMode = StatsRange.Custom
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(LxSpacing.xl))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RangeSelectorCard(
    rangeMode: StatsRange,
    onSelect: (StatsRange) -> Unit,
    customStart: LocalDate,
    customEnd: LocalDate,
    onPickStart: () -> Unit,
    onPickEnd: () -> Unit,
    spanText: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(LxSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(LxSpacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LxIconCalendar(tint = MaterialTheme.colorScheme.primary, size = 20.dp)
                Spacer(modifier = Modifier.size(LxSpacing.sm))
                Text(
                    text = "统计范围",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(LxShapes.pill)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = LxSpacing.md, vertical = LxSpacing.xs)
                ) {
                    Text(
                        text = rangeMode.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            // 三个范围选项自动换行，窄屏不会被截断
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LxSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(LxSpacing.sm)
            ) {
                StatsRange.entries.forEach { mode ->
                    LxOptionChip(
                        label = mode.label,
                        selected = rangeMode == mode,
                        onClick = { onSelect(mode) },
                        onAccentContainer = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            if (rangeMode == StatsRange.Custom) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LxSpacing.sm)
                ) {
                    OutlinedButton(
                        onClick = onPickStart,
                        modifier = Modifier
                            .weight(1f)
                            .height(LxSizes.touchTarget)
                    ) {
                        Text("开始 ${dayFormatter.format(customStart)}")
                    }
                    OutlinedButton(
                        onClick = onPickEnd,
                        modifier = Modifier
                            .weight(1f)
                            .height(LxSizes.touchTarget)
                    ) {
                        Text("结束 ${dayFormatter.format(customEnd)}")
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(LxShapes.inner)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(LxSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LxIconClock(tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 16.dp)
                Spacer(modifier = Modifier.size(LxSpacing.sm))
                Text(
                    text = spanText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun OverviewCard(
    rangeLabel: String,
    spanText: String,
    total: Int,
    perDay: Double,
    averageDuration: Int,
    healthRate: Int
) {
    val semantic = LxTheme.semantic
    // 健康率是这个 App 最该被看到的结论，单独提级到一个大数字块，
    // 其余三个指标退成次级小格 —— 避免四个格子权重完全相同、看不出重点。
    val rateTone = when {
        total == 0 -> semantic.neutral
        healthRate >= 70 -> semantic.ideal
        healthRate >= 50 -> semantic.caution
        else -> semantic.alert
    }
    val rateContainer = when {
        total == 0 -> semantic.neutralContainer
        healthRate >= 70 -> semantic.idealContainer
        healthRate >= 50 -> semantic.cautionContainer
        else -> semantic.alertContainer
    }
    val onRateContainer = when {
        total == 0 -> semantic.onNeutralContainer
        healthRate >= 70 -> semantic.onIdealContainer
        healthRate >= 50 -> semantic.onCautionContainer
        else -> semantic.onAlertContainer
    }
    val rateCaption = when {
        total == 0 -> "该范围内暂无记录"
        healthRate >= 70 -> "理想型占比良好"
        healthRate >= 50 -> "理想型占比一般"
        else -> "理想型占比偏低，可留意饮食与作息"
    }

    // 区块化：外层已由 CollapsibleSection 提供卡片，这里不再套 Card
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LxSpacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "总览",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = rangeLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = spanText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 主指标：健康率
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(LxShapes.inner)
                .background(rateContainer)
                .padding(LxSpacing.cardPadding)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LxIconIdeal(tint = rateTone, size = 18.dp)
                Spacer(modifier = Modifier.size(LxSpacing.sm))
                Text(
                    text = "健康率（3/4 型占比）",
                    style = MaterialTheme.typography.labelMedium,
                    color = onRateContainer
                )
            }
            Spacer(modifier = Modifier.height(LxSpacing.sm))
            Text(
                text = if (total == 0) "--" else "$healthRate%",
                style = MaterialTheme.typography.displaySmall,
                color = rateTone
            )
            Spacer(modifier = Modifier.height(LxSpacing.xs))
            Text(
                text = rateCaption,
                style = MaterialTheme.typography.bodySmall,
                color = onRateContainer
            )
        }

        // 次级指标：三格等宽小卡
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LxSpacing.sm)
        ) {
            StatMetric(modifier = Modifier.weight(1f), label = "次数", value = total.toString())
            StatMetric(
                modifier = Modifier.weight(1f),
                label = "日均次数",
                value = "%.1f".format(perDay)
            )
            StatMetric(
                modifier = Modifier.weight(1f),
                label = "平均时长",
                value = formatDuration(averageDuration)
            )
        }
    }
}

@Composable
private fun StatMetric(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Column(
        modifier = modifier
            .clip(LxShapes.inner)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = LxSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TrendCard(grouped: Map<LocalDate, List<RecordEntity>>, endDate: LocalDate) {
    val semantic = LxTheme.semantic
    // 趋势图跟着选定范围走：始终画选定区间的最后 14 天
    val days = (13 downTo 0).map { endDate.minusDays(it.toLong()) }
    val maxCount = days.maxOf { grouped[it]?.size ?: 0 }.coerceAtLeast(1)
    val totalInWindow = days.sumOf { grouped[it]?.size ?: 0 }

    // 区块化：外层已由 CollapsibleSection 提供卡片，这里不再套 Card
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LxSpacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "最近 14 天走势",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "峰值 $maxCount 次 / 天",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = "${dayFormatter.format(days.first())} ~ ${dayFormatter.format(days.last())}" +
                " · 合计 $totalInWindow 次",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        days.chunked(7).forEach { weekDays ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                horizontalArrangement = Arrangement.spacedBy(LxSpacing.xs)
            ) {
                weekDays.forEach { day ->
                    val count = grouped[day]?.size ?: 0
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 数值只在有记录时显示，避免 0 排成一行干扰视线
                        Text(
                            text = if (count > 0) count.toString() else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight((count / maxCount.toFloat()).coerceIn(0f, 1f))
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        // 越高越深，形成自然的「热度」梯度
                                        when {
                                            count == 0 -> Color.Transparent
                                            count.toFloat() / maxCount >= 0.75f -> semantic.chartPrimary
                                            count.toFloat() / maxCount >= 0.4f -> semantic.chartSecondary
                                            else -> semantic.chartTertiary
                                        }
                                    )
                            )
                        }
                        Spacer(modifier = Modifier.height(LxSpacing.xs))
                        Text(
                            text = "${day.monthValue}/${day.dayOfMonth}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeDistributionCard(
    records: List<RecordEntity>,
    rangeLabel: String
) {
    val semantic = LxTheme.semantic
    val total = records.size.coerceAtLeast(1)

    // 区块化：外层已由 CollapsibleSection 提供卡片
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LxSpacing.md)
    ) {
        DistributionTitle(title = "类型分布", rangeLabel = rangeLabel)
        bristolTypes.forEach { type ->
            val count = records.count { it.bristolType == type.type }
            val ratio = count.toFloat() / total
            val ideal = type.type == 3 || type.type == 4
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 与首页记录卡一致：矢量形态图示 + 理想/留意区分色
                LxBristolShape(
                    type = type.type,
                    tint = if (ideal) semantic.ideal else semantic.caution,
                    size = 20.dp
                )
                Spacer(modifier = Modifier.size(LxSpacing.sm))
                Text(
                    text = "${type.type} 型",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(42.dp),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.size(LxSpacing.sm))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(LxSizes.trendBar)
                        .clip(LxShapes.pill)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(ratio.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(LxShapes.pill)
                            .background(if (ideal) semantic.ideal else semantic.caution)
                    )
                }
                Spacer(modifier = Modifier.size(LxSpacing.sm))
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(24.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun DistributionTitle(title: String, rangeLabel: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = rangeLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun AttributeDistributionCard(
    title: String,
    options: List<StoolOption>,
    records: List<RecordEntity>,
    rangeLabel: String,
    selector: (RecordEntity) -> String?
) {
    val total = records.size.coerceAtLeast(1)

    // 区块化：外层已由 CollapsibleSection 提供卡片
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LxSpacing.md)
    ) {
        DistributionTitle(title = title, rangeLabel = rangeLabel)
        options.forEach { option ->
            DistributionRow(
                label = option.label,
                count = records.count { selector(it) == option.id },
                total = total
            )
        }
        DistributionRow(
            label = "未记录",
            count = records.count { selector(it).isNullOrBlank() },
            total = total
        )
    }
}

/** 「当前状态」是多选，所以按选项统计，一条记录会在多个选项上各计一次。 */
@Composable
private fun StatusDistributionCard(
    records: List<RecordEntity>,
    rangeLabel: String
) {
    val total = records.size.coerceAtLeast(1)
    val selectedIds = records.flatMap { record ->
        // 不能写成 bodyStatuses?.split(...).orEmpty()，那样整个链条会变成可空类型
        (record.bodyStatuses ?: "").split(',').filter { it.isNotBlank() }
    }

    // 区块化：外层已由 CollapsibleSection 提供卡片
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LxSpacing.md)
    ) {
        DistributionTitle(title = "状态分布", rangeLabel = rangeLabel)
        bodyStatuses.forEach { option ->
            DistributionRow(
                label = option.label,
                count = selectedIds.count { it == option.id },
                total = total
            )
        }
        DistributionRow(
            label = "未记录",
            count = records.count { it.bodyStatuses.isNullOrBlank() },
            total = total
        )
        Text(
            text = "一条记录可选多个状态，所以各项相加可能多于记录条数",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DistributionRow(
    label: String,
    count: Int,
    total: Int
) {
    val ratio = (count.toFloat() / total).coerceIn(0f, 1f)
    // 0 次的分项不画条，只有底槽，避免一整片实心色看不出差异
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(88.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.size(LxSpacing.sm))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(LxSizes.trendBar)
                .clip(LxShapes.pill)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (count > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(ratio)
                        .fillMaxHeight()
                        .clip(LxShapes.pill)
                        .background(LxTheme.semantic.chartPrimary)
                )
            }
        }
        Spacer(modifier = Modifier.size(LxSpacing.sm))
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(24.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun CalendarCard(
    grouped: Map<LocalDate, List<RecordEntity>>,
    rangeStart: LocalDate,
    rangeEnd: LocalDate,
    onSelectDate: (LocalDate) -> Unit
) {
    var month by remember { mutableStateOf(YearMonth.now()) }
    val today = LocalDate.now()
    val firstDay = month.atDay(1)
    val daysInMonth = month.lengthOfMonth()
    val leadingBlanks = firstDay.dayOfWeek.value % 7
    val weekDays = listOf("日", "一", "二", "三", "四", "五", "六")

    // 区块化：外层已由 CollapsibleSection 提供卡片
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LxSpacing.sm)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { month = month.minusMonths(1) }) {
                LxIconChevronLeft(tint = MaterialTheme.colorScheme.onSurface, size = 22.dp)
            }
            Text(
                text = "${month.year}年${month.monthValue}月",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = { month = month.plusMonths(1) }) {
                LxIconChevronRight(tint = MaterialTheme.colorScheme.onSurface, size = 22.dp)
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            weekDays.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        repeat((daysInMonth + leadingBlanks + 6) / 7) { weekIndex ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { dayIndex ->
                    val cellIndex = weekIndex * 7 + dayIndex
                    val dayNumber = cellIndex - leadingBlanks + 1
                    val date = if (dayNumber in 1..daysInMonth) {
                        month.atDay(dayNumber)
                    } else {
                        null
                    }
                    val count = date?.let { grouped[it]?.size } ?: 0
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (date != null) {
                            val inRange = !date.isBefore(rangeStart) && !date.isAfter(rangeEnd)
                            val isToday = date == today
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 40.dp)
                                    .clip(LxShapes.chip)
                                    .background(
                                        when {
                                            inRange -> MaterialTheme.colorScheme.primaryContainer
                                            else -> Color.Transparent
                                        }
                                    )
                                    .border(
                                        width = if (isToday) 1.5.dp else 0.dp,
                                        color = if (isToday) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            Color.Transparent
                                        },
                                        shape = LxShapes.chip
                                    )
                                    .clickable(enabled = !date.isAfter(today)) { onSelectDate(date) }
                                    .padding(horizontal = 4.dp, vertical = LxSpacing.xs),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = dayNumber.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (count > 0) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 2.dp)
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = count.toString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Text(
            text = "点某一天，统计范围会直接切到那天",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
