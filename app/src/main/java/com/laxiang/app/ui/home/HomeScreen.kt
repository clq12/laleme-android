package com.laxiang.app.ui.home

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.laxiang.app.R
import com.laxiang.app.data.RecordEntity
import com.laxiang.app.data.SectionPrefs
import com.laxiang.app.model.bristolTypeOf
import com.laxiang.app.model.formatDateTime
import com.laxiang.app.model.formatDuration
import com.laxiang.app.model.formatElapsedSeconds
import com.laxiang.app.model.stoolAttributeSummary
import com.laxiang.app.model.statusLabels
import com.laxiang.app.model.statusSummary
import com.laxiang.app.ui.common.CollapsibleSection
import com.laxiang.app.ui.common.LxBristolShape
import com.laxiang.app.ui.common.LxDivider
import com.laxiang.app.ui.common.LxIconAdd
import com.laxiang.app.ui.common.LxIconChevronRight
import com.laxiang.app.ui.common.LxIconClock
import com.laxiang.app.ui.common.LxIconDelete
import com.laxiang.app.ui.common.LxIconDuration
import com.laxiang.app.ui.common.LxIconLeaf
import com.laxiang.app.ui.common.LxIconSearch
import com.laxiang.app.ui.common.LxIconTimer
import com.laxiang.app.ui.common.LxIconWarning
import com.laxiang.app.ui.common.LxOptionChip
import com.laxiang.app.ui.theme.LxShapes
import com.laxiang.app.ui.theme.LxSizes
import com.laxiang.app.ui.theme.LxSpacing
import com.laxiang.app.ui.theme.LxTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.delay

/** 历史列表每页条数 */
private const val PAGE_SIZE = 30

private enum class RecordFilter(val label: String) {
    All("全部"),
    Ideal("理想型"),
    Abnormal("异常型"),
    WithPhoto("带照片"),
    WithNotes("有备注"),
    WithStatus("有状态")
}

private fun RecordEntity.matchesFilter(filter: RecordFilter): Boolean = when (filter) {
    RecordFilter.All -> true
    RecordFilter.Ideal -> bristolType == 3 || bristolType == 4
    RecordFilter.Abnormal -> bristolType != 3 && bristolType != 4
    RecordFilter.WithPhoto -> photoPath != null
    RecordFilter.WithNotes -> notes.isNotBlank()
    RecordFilter.WithStatus -> !bodyStatuses.isNullOrBlank()
}

private fun RecordEntity.matchesQuery(query: String): Boolean {
    val keyword = query.trim()
    if (keyword.isEmpty()) return true
    val haystack = buildString {
        append(notes)
        append(' ')
        append(bristolTypeOf(bristolType).title)
        append(' ')
        append(stoolAttributeSummary(stoolColor, stoolAmount, stoolSmell, bowelFeelings))
        append(' ')
        append(statusSummary(bodyStatuses))
    }
    return haystack.contains(keyword, ignoreCase = true)
}

/** 布里斯托 3/4 型为「理想型」，其余为「需留意」，用于记录卡左侧的状态标记 */
private fun isIdealType(type: Int) = type == 3 || type == 4

@Composable
fun HomeScreen(
    records: List<RecordEntity>,
    timerStart: Long?,
    onStartTimer: () -> Unit,
    onStopTimer: () -> Unit,
    onEditRecord: (RecordEntity) -> Unit,
    onDeleteRecord: (RecordEntity) -> Unit
) {
    val today = LocalDate.now()
    val todayRecords = remember(records, today) {
        records.filter {
            LocalDate.ofInstant(Instant.ofEpochMilli(it.occurredAt), ZoneId.systemDefault()) == today
        }
    }
    val averageDuration = if (todayRecords.isEmpty()) {
        0
    } else {
        todayRecords.sumOf { it.durationSeconds } / todayRecords.size
    }
    val latestRecord = records.maxByOrNull { it.occurredAt }
    val latestTodayRecord = todayRecords.maxByOrNull { it.occurredAt }
    val idealCountToday = todayRecords.count { isIdealType(it.bristolType) }

    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(RecordFilter.All) }
    var visibleCount by remember { mutableStateOf(PAGE_SIZE) }

    // 今日概览折叠状态：默认展开，用户收起后记住
    val context = LocalContext.current
    val sectionPrefs = remember { SectionPrefs(context) }
    var todayExpanded by remember {
        mutableStateOf(sectionPrefs.isExpanded(SectionPrefs.HOME_TODAY, true))
    }

    val filtered = remember(records, query, filter) {
        records.filter { it.matchesFilter(filter) && it.matchesQuery(query) }
    }
    // 搜索词或筛选一变，分页从头开始，避免停在「第 5 页却只剩 3 条结果」
    LaunchedEffect(query, filter) {
        visibleCount = PAGE_SIZE
    }
    val paged = filtered.take(visibleCount)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = LxSpacing.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(LxSpacing.md)
    ) {
        item { HomeHeader(todayCount = todayRecords.size) }

        // 主入口：新增记录。放在最显眼位置，是第一屏唯一的实心主按钮。
        item {
            PrimaryAddRecordCard(
                onClick = {
                    // 走「计时→结束并记录」这条既有链路，保证与旧行为一致：
                    // 空计时器点开就是一条新记录，且不覆盖已有草稿。
                    onStopTimer()
                }
            )
        }

        item {
            TimerCard(
                timerStart = timerStart,
                onStartTimer = onStartTimer,
                onStopTimer = onStopTimer
            )
        }

        // 今日概览：第一屏只看「今天」的关键数字，其余统计都在统计页
        item {
            CollapsibleSection(
                title = "今日概览",
                subtitle = buildTodaySubtitle(
                    todayCount = todayRecords.size,
                    averageDuration = averageDuration,
                    idealCount = idealCountToday
                ),
                expanded = todayExpanded,
                onToggle = {
                    todayExpanded = !todayExpanded
                    sectionPrefs.setExpanded(SectionPrefs.HOME_TODAY, todayExpanded)
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LxSpacing.sm)
                ) {
                    SummaryMetric(
                        modifier = Modifier.weight(1f),
                        label = "今日次数",
                        value = todayRecords.size.toString()
                    )
                    SummaryMetric(
                        modifier = Modifier.weight(1f),
                        label = "平均时长",
                        value = formatDuration(averageDuration)
                    )
                    SummaryMetric(
                        modifier = Modifier.weight(1f),
                        label = "最近一次",
                        value = latestTodayRecord
                            ?.let { formatDateTime(it.occurredAt).takeLast(5) }
                            ?: "--"
                    )
                }
            }
        }

        item { HistoryToolbar(
            query = query,
            onQueryChange = { query = it },
            filter = filter,
            onFilterChange = { filter = it },
            filteredCount = filtered.size,
            totalCount = records.size
        ) }

        if (filtered.isEmpty()) {
            item {
                EmptyStateCard(
                    hasAnyRecord = records.isNotEmpty(),
                    onAddRecord = onStopTimer
                )
            }
        } else {
            items(paged, key = { it.id }) { record ->
                RecordCard(
                    record = record,
                    onClick = { onEditRecord(record) },
                    onDelete = { onDeleteRecord(record) }
                )
            }
            if (paged.size < filtered.size) {
                item {
                    LoadMoreRow(
                        shown = paged.size,
                        total = filtered.size,
                        onLoadMore = { visibleCount += PAGE_SIZE }
                    )
                }
            } else if (filtered.size > PAGE_SIZE) {
                item {
                    Text(
                        text = "已显示全部 ${filtered.size} 条",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(LxSpacing.xl))
        }
    }
}

private fun buildTodaySubtitle(
    todayCount: Int,
    averageDuration: Int,
    idealCount: Int
): String = when {
    todayCount == 0 -> "今天还没有记录，点上面的按钮开始第一条"
    else -> "今日 $todayCount 次 · 平均 ${formatDuration(averageDuration)}" +
        " · 其中理想型 $idealCount 次"
}

@Composable
private fun HomeHeader(todayCount: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 顶部让开状态栏：Scaffold 已把 safeDrawing 顶边给了 body，
        // 这里只补一段视觉呼吸留白（不再是「拿 24dp 硬凑状态栏高度」）。
        Spacer(modifier = Modifier.height(LxSpacing.md))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppLogoBadge(size = LxSizes.iconBadge)
            Spacer(modifier = Modifier.size(LxSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "拉了么",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "认真记录每一次身体信号",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // 今日次数做成紧凑徽标，取代原先压在概览卡里的大数字
            if (todayCount > 0) {
                Box(
                    modifier = Modifier
                        .clip(LxShapes.pill)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = LxSpacing.md, vertical = LxSpacing.sm)
                ) {
                    Text(
                        text = "今日 $todayCount 次",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 品牌标识：直接复用 App 的启动图标（自适应图标前景层）。
 *
 * 为什么不用文字占位：首页是用户每天看最多的页，
 * 这里出现一个「拉」字会让人以为是没做完的占位符，
 * 直接用真实图标能和桌面图标对上，建立品牌一致性。
 */
@Composable
private fun AppLogoBadge(size: Dp) {
    val ctx = LocalContext.current
    // 优先取自适应图标的前景层（已含品牌背景圆角方块），
    // 若某个密度下缺失则回退到合成图标，不会白屏。
    // 注意：painterResource 是 @Composable，必须在组合作用域里调用，
    // 不能塞进 remember 的 lambda（那里不是 Composable）。
    val logoResId = remember(ctx) {
        ctx.resources.getIdentifier("ic_launcher_foreground", "mipmap", ctx.packageName)
            .takeIf { it != 0 } ?: R.mipmap.ic_launcher
    }
    val painter = painterResource(logoResId)
    Box(
        modifier = Modifier
            .size(size)
            .clip(LxShapes.inner)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        // ic_launcher_foreground 在 108dp 画布中只占中间约 72dp，
        // 直接 fillMaxSize 会有大量留白，故放大后 Crop 掉外围。
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .scale(1.42f)
        )
    }
}

/**
 * 首页主操作：新增记录。
 *
 * 设计取舍：这是首页唯一使用 primary 实心填充的大块，
 * 视觉权重明显高于其余卡片，用户第一眼就能识别出「要记录点这里」。
 * 文案同时写清「点一下会发生什么」，避免只有一个图标造成理解成本。
 */
@Composable
private fun PrimaryAddRecordCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(LxShapes.card)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick)
            .semantics { role = Role.Button }
            .padding(horizontal = LxSpacing.cardPadding, vertical = LxSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(LxSizes.iconBadge)
                .clip(LxShapes.inner)
                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            LxIconAdd(tint = MaterialTheme.colorScheme.onPrimary, size = 24.dp)
        }
        Spacer(modifier = Modifier.size(LxSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "新增排便记录",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                text = "记录类型、颜色、分量与当时状态",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f)
            )
        }
        LxIconChevronRight(
            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
            size = 20.dp
        )
    }
}

@Composable
private fun TimerCard(
    timerStart: Long?,
    onStartTimer: () -> Unit,
    onStopTimer: () -> Unit
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(timerStart) {
        // 没在计时就别每秒刷一次首页，白耗电
        if (timerStart == null) return@LaunchedEffect
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

    val running = timerStart != null

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(LxSpacing.cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LxIconTimer(
                    tint = if (running) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    size = 20.dp
                )
                Spacer(modifier = Modifier.size(LxSpacing.sm))
                Text(
                    text = "马桶计时器",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(LxSpacing.md))
            Text(
                text = if (running) {
                    formatElapsedSeconds((now - timerStart) / 1000)
                } else {
                    "00:00"
                },
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Medium,
                color = if (running) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Spacer(modifier = Modifier.height(LxSpacing.md))
            // 计时中把主按钮让给「结束并记录」，未计时时用次级按钮，避免与新增记录抢焦点
            if (running) {
                androidx.compose.material3.Button(
                    onClick = onStopTimer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(LxSizes.buttonLarge)
                ) {
                    Text(text = "结束并记录", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                OutlinedButton(
                    onClick = onStartTimer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(LxSizes.touchTarget)
                ) {
                    Text(text = "开始计时")
                }
            }
        }
    }
}

@Composable
private fun SummaryMetric(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Column(
        modifier = modifier
            .clip(LxShapes.inner)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = LxSpacing.md, horizontal = LxSpacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(LxSpacing.xs))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 历史记录工具条：标题 + 计数 + 搜索 + 筛选。
 * 从原来塞在「今日概览」折叠区里独立出来 —— 搜索是高频动作，
 * 折起来会让用户每次都得先展开一次。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryToolbar(
    query: String,
    onQueryChange: (String) -> Unit,
    filter: RecordFilter,
    onFilterChange: (RecordFilter) -> Unit,
    filteredCount: Int,
    totalCount: Int
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "历史记录",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (filteredCount == totalCount) {
                    "共 $totalCount 条"
                } else {
                    "命中 $filteredCount / $totalCount"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(LxSpacing.sm))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("搜备注、类型、颜色分量或状态") },
            leadingIcon = {
                LxIconSearch(tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp)
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    TextButton(onClick = { onQueryChange("") }) {
                        Text("清空")
                    }
                }
            },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(LxSpacing.sm))
        // 6 个筛选项改为自动换行，窄屏也不会被挤出屏幕外
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LxSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(LxSpacing.sm)
        ) {
            RecordFilter.entries.forEach { item ->
                LxOptionChip(
                    label = item.label,
                    selected = filter == item,
                    onClick = { onFilterChange(item) },
                    onAccentContainer = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun LoadMoreRow(
    shown: Int,
    total: Int,
    onLoadMore: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LxSpacing.xs)
    ) {
        OutlinedButton(
            onClick = onLoadMore,
            modifier = Modifier
                .fillMaxWidth()
                .height(LxSizes.touchTarget)
        ) {
            Text("加载更多")
        }
        Text(
            text = "已显示 $shown / $total 条",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyStateCard(hasAnyRecord: Boolean, onAddRecord: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = LxSpacing.xxl, horizontal = LxSpacing.cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(LxShapes.pill)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (hasAnyRecord) {
                    LxIconSearch(tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 26.dp)
                } else {
                    LxIconLeaf(tint = MaterialTheme.colorScheme.primary, size = 26.dp)
                }
            }
            Spacer(modifier = Modifier.height(LxSpacing.md))
            Text(
                text = if (hasAnyRecord) "没有匹配的记录" else "还没有任何记录",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(LxSpacing.xs))
            Text(
                text = if (hasAnyRecord) {
                    "换个关键词或筛选项试试"
                } else {
                    "点下方按钮记录第一次，数据只存在你的手机上"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!hasAnyRecord) {
                Spacer(modifier = Modifier.height(LxSpacing.lg))
                androidx.compose.material3.Button(
                    onClick = onAddRecord,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(LxSizes.buttonLarge)
                ) {
                    LxIconAdd(tint = MaterialTheme.colorScheme.onPrimary, size = 20.dp)
                    Spacer(modifier = Modifier.size(LxSpacing.sm))
                    Text(text = "新增第一条记录", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun RecordCard(
    record: RecordEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val type = bristolTypeOf(record.bristolType)
    val ideal = isIdealType(record.bristolType)
    val semantic = LxTheme.semantic

    // 理想型 / 需留意：用「有色标记条 + 文字标签」双重表达，不单靠颜色
    val (accentColor: Color, accentLabel: String) = if (ideal) {
        semantic.ideal to "理想型"
    } else {
        semantic.caution to "需留意"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // 左侧状态色条：颜色 + 屏读器可读的文字标签共同表达
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )
            // ── 排版重构（v2.5.3）──────────────────────────────
            // 用户反馈的三个排版问题，根因是「所有内容都塞在一个 weight(1f)
            // 的 Column 里、且与删除按钮同处一个 Row」：
            //   1. 形态标题 + 「需留意」胶囊并排 → 标题长一点，胶囊就被挤出
            //      可用宽度，「需留意」被压成「需留 / 意」两行；
            //   2. 时间与时长用 " · " 拼成一行 → 窄屏必折行，且折点不可控，
            //      出现「2026-10-10 19:20 · 4 / 分38秒」这种把数字切开的断法；
            //   3. 删除按钮在最右侧吃掉宽度，进一步压缩上面两处的可用空间。
            // 解法：把内容列拆成「标题行 / 时间行 / 时长行」三条独立行，
            // 时间与时长都改为单行不折（nowrap），删除按钮移到卡片右上角
            // 作为图标按钮，不再和文字抢宽度。
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(LxSpacing.cardPadding),
                verticalAlignment = Alignment.Top
            ) {
                // 形态图示替代 emoji：不依赖系统字体，且能真实传达形态差异
                Box(
                    modifier = Modifier
                        .size(LxSizes.iconBadge)
                        .clip(LxShapes.inner)
                        .background(if (ideal) semantic.idealContainer else semantic.cautionContainer),
                    contentAlignment = Alignment.Center
                ) {
                    LxBristolShape(
                        type = record.bristolType,
                        tint = if (ideal) semantic.onIdealContainer else semantic.onCautionContainer,
                        size = LxSizes.iconShape
                    )
                }
                Spacer(modifier = Modifier.size(LxSpacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    // 第 1 行：形态标题 + 状态胶囊。
                    // 标题给 weight(1f) 并允许最多 2 行，胶囊用 wrapContentWidth
                    // 保证「需留意」「理想型」永远是完整的三个字一行，
                    // 不会再被压成两行（这是本次要修的核心问题）。
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = type.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.size(LxSpacing.sm))
                        StatusPill(
                            label = accentLabel,
                            container = if (ideal) {
                                semantic.idealContainer
                            } else {
                                semantic.cautionContainer
                            },
                            content = if (ideal) {
                                semantic.onIdealContainer
                            } else {
                                semantic.onCautionContainer
                            }
                        )
                    }
                    // 第 2 行：记录时间，独占一行，不折行。
                    Spacer(modifier = Modifier.height(LxSpacing.xs))
                    MetaLine(
                        icon = {
                            LxIconClock(
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                size = LxSizes.iconXs
                            )
                        },
                        text = formatDateTime(record.occurredAt)
                    )
                    // 第 3 行：持续时间，另起一行（用户明确要求）。
                    Spacer(modifier = Modifier.height(2.dp))
                    MetaLine(
                        icon = {
                            LxIconDuration(
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                size = LxSizes.iconXs
                            )
                        },
                        text = "持续时间 ${formatDuration(record.durationSeconds)}"
                    )
                    val attributes = stoolAttributeSummary(
                        record.stoolColor,
                        record.stoolAmount,
                        record.stoolSmell,
                        record.bowelFeelings
                    )
                    if (attributes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(LxSpacing.xs))
                        Text(
                            text = attributes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    val statuses = statusLabels(record.bodyStatuses)
                    if (statuses.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(LxSpacing.xs))
                        Row(verticalAlignment = Alignment.Top) {
                            LxIconWarning(
                                tint = semantic.caution,
                                size = LxSizes.iconSm,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.size(LxSpacing.xs))
                            Text(
                                text = statuses.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = semantic.caution,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (record.notes.isNotBlank() || record.photoPath != null) {
                        Spacer(modifier = Modifier.height(LxSpacing.xs))
                        Text(
                            text = buildString {
                                if (record.notes.isNotBlank()) append(record.notes)
                                if (record.notes.isNotBlank() && record.photoPath != null) append(" · ")
                                if (record.photoPath != null) append("附照片")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            // 删除按钮：移到卡片右上角做图标按钮。
            // 原来是带「删除」二字的 TextButton，横着占掉约 56dp 宽度，
            // 正是把标题/时间挤出折行的那部分。换成 44dp 图标按钮后
            // 内容列拿到完整宽度；触控区仍保持 48dp（外层 Box 撑满）。
            Box(
                modifier = Modifier
                    .align(Alignment.Top)
                    .padding(top = LxSpacing.sm, end = LxSpacing.xs)
                    .size(LxSizes.touchTarget)
                    .clip(LxShapes.pill)
                    .clickable(onClick = onDelete)
                    .semantics { role = Role.Button },
                contentAlignment = Alignment.Center
            ) {
                LxIconDelete(
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    size = LxSizes.iconSm
                )
            }
        }
    }
}

/**
 * 状态胶囊：「理想型」/「需留意」。
 *
 * 独立成组件的原因：这两个标签是本次反复出问题的位置 ——
 * 必须保证「三个字永远在一行内完整显示」，
 * 所以这里显式 `maxLines = 1` + `softWrap = false`，
 * 并且不设 maxWidth 约束（让它按内容撑开），
 * 由调用方用 `weight(1f, fill = false)` 把标题让出空间给它。
 */
@Composable
private fun StatusPill(
    label: String,
    container: Color,
    content: Color
) {
    Box(
        modifier = Modifier
            .clip(LxShapes.pill)
            .background(container)
            .padding(horizontal = LxSpacing.sm, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = content,
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * 记录卡里「图标 + 一行元信息」的统一排版。
 *
 * 时间行与时长行共用：图标固定 12dp、垂直按首行基线对齐，
 * 文字单行不折（`softWrap = false`），避免窄屏把
 * 「2026-10-10 19:20」切成两行、或把「4分38秒」从数字中间断开。
 */
@Composable
private fun MetaLine(
    icon: @Composable () -> Unit,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon()
        Spacer(modifier = Modifier.size(LxSpacing.xs))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}
