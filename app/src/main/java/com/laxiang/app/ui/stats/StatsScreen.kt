package com.laxiang.app.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import com.laxiang.app.data.RecordEntity
import com.laxiang.app.data.SectionPrefs
import com.laxiang.app.ui.common.CollapsibleSection
import com.laxiang.app.ui.common.LaxiangDatePickerDialog
import com.laxiang.app.ui.theme.LxShapes
import com.laxiang.app.ui.theme.LxSpacing
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
            Spacer(modifier = Modifier.height(LxSpacing.xl))
            Text(
                text = "数据大盘",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(LxSpacing.xs))
            Text(
                text = "下面的统计只算选定范围内的记录，默认最近 14 天",
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
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "统计范围",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatsRange.entries.forEach { mode ->
                    FilterChip(
                        selected = rangeMode == mode,
                        onClick = { onSelect(mode) },
                        label = { Text(mode.label) }
                    )
                }
            }
            if (rangeMode == StatsRange.Custom) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onPickStart,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("开始 ${dayFormatter.format(customStart)}")
                    }
                    OutlinedButton(
                        onClick = onPickEnd,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("结束 ${dayFormatter.format(customEnd)}")
                    }
                }
            }
            Text(
                text = spanText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LxSpacing.sm)
        ) {
            StatMetric(
                modifier = Modifier.weight(1f),
                label = "平均时长",
                value = formatDuration(averageDuration)
            )
            StatMetric(modifier = Modifier.weight(1f), label = "健康率", value = "$healthRate%")
        }
        if (total == 0) {
            Text(
                text = "该范围内暂无记录，换个时间范围看看",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
    // 趋势图跟着选定范围走：始终画选定区间的最后 14 天
    val days = (13 downTo 0).map { endDate.minusDays(it.toLong()) }
    val maxCount = days.maxOf { grouped[it]?.size ?: 0 }.coerceAtLeast(1)

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
                text = "14 天趋势",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${dayFormatter.format(days.first())} ~ ${dayFormatter.format(days.last())}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        days.chunked(7).forEach { weekDays ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                weekDays.forEach { day ->
                    val count = grouped[day]?.size ?: 0
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 1.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(count / maxCount.toFloat())
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = type.emoji, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "${type.type}型",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.size(10.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(ratio)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(5.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
    val ratio = count.toFloat() / total
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.width(104.dp),
            maxLines = 1
        )
        Spacer(modifier = Modifier.size(10.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        Spacer(modifier = Modifier.size(10.dp))
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                Text(text = "←", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                text = "${month.year}年${month.monthValue}月",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = { month = month.plusMonths(1) }) {
                Text(text = "→", style = MaterialTheme.typography.titleMedium)
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
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (inRange) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            Color.Transparent
                                        }
                                    )
                                    .clickable(enabled = !date.isAfter(today)) { onSelectDate(date) }
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
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
