package com.laxiang.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.laxiang.app.data.RecordEntity
import com.laxiang.app.model.bristolTypeOf
import com.laxiang.app.model.formatDateTime
import com.laxiang.app.model.formatDuration
import com.laxiang.app.model.formatElapsedSeconds
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.delay

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
    val todayRecords = remember(records) {
        records.filter {
            LocalDate.ofInstant(Instant.ofEpochMilli(it.occurredAt), ZoneId.systemDefault()) == today
        }
    }
    val averageDuration = if (todayRecords.isEmpty()) {
        0
    } else {
        todayRecords.sumOf { it.durationSeconds } / todayRecords.size
    }
    val latestRecord = todayRecords.maxByOrNull { it.occurredAt }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "💩", style = MaterialTheme.typography.headlineMedium)
                }
                Spacer(modifier = Modifier.size(12.dp))
                Column {
                    Text(
                        text = "拉了么",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "认真记录每一次身体信号",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            TimerCard(
                timerStart = timerStart,
                onStartTimer = onStartTimer,
                onStopTimer = onStopTimer
            )
        }

        item {
            TodaySummaryCard(
                todayCount = todayRecords.size,
                averageDuration = averageDuration,
                latestRecord = latestRecord
            )
        }

        item {
            Text(
                text = "最近记录",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (records.isEmpty()) {
            item {
                EmptyStateCard()
            }
        } else {
            items(records.take(20)) { record ->
                RecordCard(
                    record = record,
                    onClick = { onEditRecord(record) },
                    onDelete = { onDeleteRecord(record) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
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
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "马桶计时器",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (timerStart == null) {
                    "00:00"
                } else {
                    formatElapsedSeconds((now - timerStart) / 1000)
                },
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = if (timerStart == null) onStartTimer else onStopTimer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = if (timerStart == null) "开始计时" else "结束并记录",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun TodaySummaryCard(
    todayCount: Int,
    averageDuration: Int,
    latestRecord: RecordEntity?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "今日概览",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryMetric(
                    modifier = Modifier.weight(1f),
                    label = "今日次数",
                    value = todayCount.toString()
                )
                SummaryMetric(
                    modifier = Modifier.weight(1f),
                    label = "平均时长",
                    value = formatDuration(averageDuration)
                )
                SummaryMetric(
                    modifier = Modifier.weight(1f),
                    label = "最近记录",
                    value = latestRecord?.let { formatDateTime(it.occurredAt).takeLast(5) } ?: "--"
                )
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
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyStateCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "🍃", style = MaterialTheme.typography.displayMedium)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "当前空空如也",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "点击上面的按钮开始第一条记录",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = type.emoji,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.size(14.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = type.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${formatDateTime(record.occurredAt)} · ${formatDuration(record.durationSeconds)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (record.notes.isNotBlank() || record.photoPath != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = buildString {
                            append(record.notes)
                            if (record.notes.isNotBlank() && record.photoPath != null) append(" · ")
                            if (record.photoPath != null) append("有照片")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.size(8.dp))
            TextButton(
                onClick = onDelete,
                modifier = Modifier.height(56.dp)
            ) {
                Text(text = "删除")
            }
        }
    }
}
