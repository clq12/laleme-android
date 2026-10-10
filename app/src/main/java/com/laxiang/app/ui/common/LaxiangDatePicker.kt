package com.laxiang.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth

/**
 * 项目自带的月历选择器。
 *
 * 周标题在这里写死成「一 二 三 四 五 六 日」，不走系统 DatePicker —— 系统组件按
 * locale 取周名（中文下是「星期一」这样的三字名），格子一窄就被裁成「星」。
 *
 * @param maxDate 传入后，该日期之后的天不可选（例如统计页不允许选未来）。
 */
@Composable
internal fun LaxiangDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
    title: String = "选择日期",
    maxDate: LocalDate? = null
) {
    var displayedMonth by remember { mutableStateOf(YearMonth.from(initialDate)) }
    var selectedDate by remember { mutableStateOf(initialDate) }
    val weekLabels = listOf("一", "二", "三", "四", "五", "六", "日")
    val leadingBlanks = displayedMonth.atDay(1).dayOfWeek.value - 1
    val daysInMonth = displayedMonth.lengthOfMonth()
    val canGoNext = maxDate == null || displayedMonth.isBefore(YearMonth.from(maxDate))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { displayedMonth = displayedMonth.minusMonths(1) }) {
                        Text(text = "←")
                    }
                    Text(
                        text = "${displayedMonth.year}年${displayedMonth.monthValue}月",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    TextButton(
                        onClick = { displayedMonth = displayedMonth.plusMonths(1) },
                        enabled = canGoNext
                    ) {
                        Text(text = "→")
                    }
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    weekLabels.forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
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
                                displayedMonth.atDay(dayNumber)
                            } else {
                                null
                            }
                            val selectable = date != null && (maxDate == null || !date.isAfter(maxDate))
                            val isSelected = date != null && date == selectedDate
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 2.dp)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            selectable -> MaterialTheme.colorScheme.surfaceVariant
                                            else -> MaterialTheme.colorScheme.surface
                                        }
                                    )
                                    .clickable(enabled = selectable) { date?.let { selectedDate = it } },
                                contentAlignment = Alignment.Center
                            ) {
                                if (date != null) {
                                    Text(
                                        text = dayNumber.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                            selectable -> MaterialTheme.colorScheme.onSurface
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedDate) }) {
                Text(text = "确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消")
            }
        }
    )
}
