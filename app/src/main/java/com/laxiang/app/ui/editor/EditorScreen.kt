package com.laxiang.app.ui.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.laxiang.app.EditorState
import com.laxiang.app.model.bristolTypes
import com.laxiang.app.model.formatDate
import com.laxiang.app.model.formatDuration
import com.laxiang.app.model.formatTime
import com.laxiang.app.model.StoolOption
import com.laxiang.app.model.stoolAmounts
import com.laxiang.app.model.stoolColors
import com.laxiang.app.model.stoolSmells
import com.laxiang.app.model.bowelFeelings
import com.laxiang.app.model.bodyStatuses
import com.laxiang.app.model.toggleStatus
import com.laxiang.app.ui.common.LaxiangDatePickerDialog
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    editorState: EditorState?,
    onUpdate: (EditorState) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    onNavigateBack: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickPhoto: (Uri) -> Unit
) {
    if (editorState == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "没有正在编辑的记录")
        }
        return
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let(onPickPhoto)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = {
                    // 顺手清掉编辑态，避免下次进来看到上次的残留草稿
                    onCancel()
                    onNavigateBack()
                }
            ) {
                Text(text = "返回")
            }
            Text(
                text = if (editorState.existingId == null) "新记录" else "编辑记录",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        DateTimeCard(
            editorState = editorState,
            onUpdate = onUpdate,
            onShowDatePicker = { showDatePicker = true },
            onShowTimePicker = { showTimePicker = true }
        )

        Spacer(modifier = Modifier.height(12.dp))
        DurationCard(
            editorState = editorState,
            onUpdate = onUpdate
        )

        Spacer(modifier = Modifier.height(12.dp))
        BristolTypeCard(
            editorState = editorState,
            onUpdate = onUpdate
        )

        Spacer(modifier = Modifier.height(12.dp))
        StoolAttributeCard(
            title = "大便颜色",
            options = stoolColors,
            selectedId = editorState.stoolColor,
            onSelect = { stoolColor -> onUpdate(editorState.copy(stoolColor = stoolColor)) }
        )

        Spacer(modifier = Modifier.height(12.dp))
        StoolAttributeCard(
            title = "分量",
            options = stoolAmounts,
            selectedId = editorState.stoolAmount,
            onSelect = { stoolAmount -> onUpdate(editorState.copy(stoolAmount = stoolAmount)) }
        )

        Spacer(modifier = Modifier.height(12.dp))
        StoolAttributeCard(
            title = "气味",
            options = stoolSmells,
            selectedId = editorState.stoolSmell,
            onSelect = { stoolSmell -> onUpdate(editorState.copy(stoolSmell = stoolSmell)) }
        )

        Spacer(modifier = Modifier.height(12.dp))
        MultiSelectCard(
            title = "排便感受",
            options = bowelFeelings,
            selectedIds = editorState.bowelFeelings,
            onToggle = { id ->
                val next = if (id in editorState.bowelFeelings) {
                    editorState.bowelFeelings - id
                } else {
                    editorState.bowelFeelings + id
                }
                onUpdate(editorState.copy(bowelFeelings = next))
            }
        )

        Spacer(modifier = Modifier.height(12.dp))
        MultiSelectCard(
            title = "当前状态（选填）",
            hint = "记录当时的状态或可能的原因，可多选；不选也能直接保存，表示这次没找到明确原因。",
            emptyHint = "未选择 —— 选填项，留空直接保存即可。",
            options = bodyStatuses,
            selectedIds = editorState.bodyStatuses,
            onClear = { onUpdate(editorState.copy(bodyStatuses = emptySet())) },
            onToggle = { id ->
                onUpdate(
                    editorState.copy(bodyStatuses = toggleStatus(editorState.bodyStatuses, id))
                )
            }
        )

        Spacer(modifier = Modifier.height(12.dp))
        NotesCard(
            editorState = editorState,
            onUpdate = onUpdate
        )

        Spacer(modifier = Modifier.height(12.dp))
        PhotoCard(
            editorState = editorState,
            onTakePhoto = onTakePhoto,
            onPickPhoto = {
                photoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onRemovePhoto = { onUpdate(editorState.copy(photoPath = null)) }
        )

        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(text = "保存记录")
        }

        if (editorState.existingId != null) {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = {
                    onDelete()
                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(text = "删除记录")
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }

    if (showDatePicker) {
        val initialDate = Instant.ofEpochMilli(editorState.occurredAt)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        LaxiangDatePickerDialog(
            initialDate = initialDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { selectedDate ->
                val existingTime = Instant.ofEpochMilli(editorState.occurredAt)
                    .atZone(ZoneId.systemDefault())
                    .toLocalTime()
                val newMillis = selectedDate
                    .atTime(existingTime)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                onUpdate(editorState.copy(occurredAt = newMillis))
                showDatePicker = false
            }
        )
    }

    if (showTimePicker) {
        val currentTime = Instant.ofEpochMilli(editorState.occurredAt)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
        val timePickerState = rememberTimePickerState(
            initialHour = currentTime.hour,
            initialMinute = currentTime.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(text = "选择时间") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val existingDate = Instant.ofEpochMilli(editorState.occurredAt)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        val newTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        val newMillis = existingDate
                            .atTime(newTime)
                            .atZone(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()
                        onUpdate(editorState.copy(occurredAt = newMillis))
                        showTimePicker = false
                    }
                ) {
                    Text(text = "确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(text = "取消")
                }
            }
        )
    }
}

@Composable
private fun DateTimeCard(
    editorState: EditorState,
    onUpdate: (EditorState) -> Unit,
    onShowDatePicker: () -> Unit,
    onShowTimePicker: () -> Unit
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
                text = "日期和时间",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onShowDatePicker,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = formatDate(editorState.occurredAt))
                }
                OutlinedButton(
                    onClick = onShowTimePicker,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = formatTime(editorState.occurredAt))
                }
            }
        }
    }
}

@Composable
private fun DurationCard(
    editorState: EditorState,
    onUpdate: (EditorState) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "排便时长",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = formatDuration(editorState.durationSeconds),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        onUpdate(
                            editorState.copy(
                                durationSeconds = (editorState.durationSeconds - 30).coerceAtLeast(1)
                            )
                        )
                    }
                ) {
                    Text(text = "-30秒")
                }
                OutlinedButton(
                    onClick = {
                        onUpdate(
                            editorState.copy(
                                durationSeconds = editorState.durationSeconds + 30
                            )
                        )
                    }
                ) {
                    Text(text = "+30秒")
                }
            }
        }
    }
}

@Composable
private fun BristolTypeCard(
    editorState: EditorState,
    onUpdate: (EditorState) -> Unit
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
                text = "布里斯托类型",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                bristolTypes.forEach { type ->
                    val selected = editorState.bristolType == type.type
                    Column(
                        modifier = Modifier
                            .width(86.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                            .border(
                                width = if (selected) 2.dp else 0.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                onUpdate(editorState.copy(bristolType = type.type))
                            }
                            .padding(vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = type.emoji,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${type.type}型",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            val selectedType = bristolTypes.firstOrNull { it.type == editorState.bristolType }
                ?: bristolTypes[3]
            Text(
                text = "${selectedType.title} · ${selectedType.subtitle}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = selectedType.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StoolAttributeCard(
    title: String,
    options: List<StoolOption>,
    selectedId: String?,
    onSelect: (String?) -> Unit
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
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                options.forEach { option ->
                    val selected = option.id == selectedId
                    Column(
                        modifier = Modifier
                            .width(92.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                            .border(
                                width = if (selected) 2.dp else 0.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onSelect(if (selected) null else option.id) }
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
            val selectedOption = options.firstOrNull { it.id == selectedId }
            if (selectedOption == null) {
                Text(
                    text = "未选择，可保持为空。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = selectedOption.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (selectedOption.warning) {
                    Text(
                        text = "如持续出现建议就医",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun MultiSelectCard(
    title: String,
    options: List<StoolOption>,
    selectedIds: Set<String>,
    hint: String? = null,
    emptyHint: String = "未选择，可保持为空。",
    onClear: (() -> Unit)? = null,
    onToggle: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (onClear != null && selectedIds.isNotEmpty()) {
                    TextButton(onClick = onClear) {
                        Text(text = "清空")
                    }
                }
            }
            if (hint != null) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                options.forEach { option ->
                    val selected = option.id in selectedIds
                    Column(
                        modifier = Modifier
                            .width(92.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                            .border(
                                width = if (selected) 2.dp else 0.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onToggle(option.id) }
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
            val selectedOptions = options.filter { it.id in selectedIds }
            if (selectedOptions.isEmpty()) {
                Text(
                    text = emptyHint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                selectedOptions.forEach { option ->
                    Text(
                        text = "${option.label}：${option.description}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (selectedOptions.any { it.warning }) {
                    Text(
                        text = "如持续出现建议就医",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun NotesCard(
    editorState: EditorState,
    onUpdate: (EditorState) -> Unit
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
                text = "备注",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = editorState.notes,
                onValueChange = { notes ->
                    onUpdate(editorState.copy(notes = notes))
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(text = "记录饮食、感受或其他信息") },
                minLines = 3
            )
        }
    }
}

@Composable
private fun PhotoCard(
    editorState: EditorState,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit
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
                text = "照片",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (editorState.photoPath != null) {
                AsyncImage(
                    model = File(editorState.photoPath!!),
                    contentDescription = "排便照片",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无照片",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onTakePhoto,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "拍照")
                }
                OutlinedButton(
                    onClick = onPickPhoto,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "相册")
                }
                if (editorState.photoPath != null) {
                    OutlinedButton(
                        onClick = onRemovePhoto,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "移除")
                    }
                }
            }
        }
    }
}
