package com.laxiang.app.ui.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.laxiang.app.ui.common.LxBristolTile
import com.laxiang.app.ui.common.LxFieldLabel
import com.laxiang.app.ui.common.LxIconBack
import com.laxiang.app.ui.common.LxIconCalendar
import com.laxiang.app.ui.common.LxIconCamera
import com.laxiang.app.ui.common.LxIconCheck
import com.laxiang.app.ui.common.LxIconClock
import com.laxiang.app.ui.common.LxIconDelete
import com.laxiang.app.ui.common.LxIconGallery
import com.laxiang.app.ui.common.LxIconWarning
import com.laxiang.app.ui.common.LxOptionFlow
import com.laxiang.app.ui.theme.LxShapes
import com.laxiang.app.ui.theme.LxSizes
import com.laxiang.app.ui.theme.LxSpacing
import com.laxiang.app.ui.theme.LxTheme
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
            .padding(horizontal = LxSpacing.screenHorizontal)
    ) {
        Spacer(modifier = Modifier.height(LxSpacing.lg))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(LxSizes.touchTarget)
                    .clip(LxShapes.pill)
                    .clickable {
                        // 顺手清掉编辑态，避免下次进来看到上次的残留草稿
                        onCancel()
                        onNavigateBack()
                    },
                contentAlignment = Alignment.Center
            ) {
                LxIconBack(tint = MaterialTheme.colorScheme.onSurface, size = 22.dp)
            }
            Spacer(modifier = Modifier.size(LxSpacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (editorState.existingId == null) "新记录" else "编辑记录",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "按顺序填写即可，带「选填」的项可以跳过",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(LxSpacing.md))
        DateTimeCard(
            editorState = editorState,
            onUpdate = onUpdate,
            onShowDatePicker = { showDatePicker = true },
            onShowTimePicker = { showTimePicker = true }
        )

        Spacer(modifier = Modifier.height(LxSpacing.md))
        DurationCard(
            editorState = editorState,
            onUpdate = onUpdate
        )

        Spacer(modifier = Modifier.height(LxSpacing.md))
        BristolTypeCard(
            editorState = editorState,
            onUpdate = onUpdate
        )

        Spacer(modifier = Modifier.height(LxSpacing.md))
        StoolAttributeCard(
            title = "大便颜色",
            options = stoolColors,
            selectedId = editorState.stoolColor,
            onSelect = { stoolColor -> onUpdate(editorState.copy(stoolColor = stoolColor)) }
        )

        Spacer(modifier = Modifier.height(LxSpacing.md))
        StoolAttributeCard(
            title = "分量",
            options = stoolAmounts,
            selectedId = editorState.stoolAmount,
            onSelect = { stoolAmount -> onUpdate(editorState.copy(stoolAmount = stoolAmount)) }
        )

        Spacer(modifier = Modifier.height(LxSpacing.md))
        StoolAttributeCard(
            title = "气味",
            options = stoolSmells,
            selectedId = editorState.stoolSmell,
            onSelect = { stoolSmell -> onUpdate(editorState.copy(stoolSmell = stoolSmell)) }
        )

        Spacer(modifier = Modifier.height(LxSpacing.md))
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

        Spacer(modifier = Modifier.height(LxSpacing.md))
        MultiSelectCard(
            title = "当前状态",
            optional = true,
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

        Spacer(modifier = Modifier.height(LxSpacing.md))
        NotesCard(
            editorState = editorState,
            onUpdate = onUpdate
        )

        Spacer(modifier = Modifier.height(LxSpacing.md))
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

        Spacer(modifier = Modifier.height(LxSpacing.lg))
        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(LxSizes.buttonLarge)
        ) {
            LxIconCheck(tint = MaterialTheme.colorScheme.onPrimary, size = 20.dp)
            Spacer(modifier = Modifier.size(LxSpacing.sm))
            Text(text = "保存记录", style = MaterialTheme.typography.titleMedium)
        }

        if (editorState.existingId != null) {
            Spacer(modifier = Modifier.height(LxSpacing.md))
            // 删除是不可逆操作：用 error 色描边 + 图标强化，与保存形成明确层级差
            OutlinedButton(
                onClick = {
                    onDelete()
                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LxSizes.buttonLarge),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
            ) {
                LxIconDelete(tint = MaterialTheme.colorScheme.error, size = 20.dp)
                Spacer(modifier = Modifier.size(LxSpacing.sm))
                Text(text = "删除这条记录", style = MaterialTheme.typography.titleMedium)
            }
        }

        Spacer(modifier = Modifier.height(LxSpacing.listBottomInset))
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

/**
 * 表单卡片统一容器。
 * 所有表单区块走同一个 Card + 间距 + 标题规范，
 * 避免各卡自己写 padding / 圆角 / 标题字重导致视觉不一致。
 */
@Composable
private fun FormCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LxSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(LxSpacing.md),
            content = content
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
    FormCard {
        LxFieldLabel(text = "日期和时间")
        Text(
            text = "默认是当前时刻，事后补记可以改",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(LxSpacing.sm)) {
            OutlinedButton(
                onClick = onShowDatePicker,
                modifier = Modifier
                    .weight(1f)
                    .height(LxSizes.touchTarget)
            ) {
                LxIconCalendar(tint = MaterialTheme.colorScheme.onSurface, size = 18.dp)
                Spacer(modifier = Modifier.size(LxSpacing.sm))
                Text(text = formatDate(editorState.occurredAt))
            }
            OutlinedButton(
                onClick = onShowTimePicker,
                modifier = Modifier
                    .weight(1f)
                    .height(LxSizes.touchTarget)
            ) {
                LxIconClock(tint = MaterialTheme.colorScheme.onSurface, size = 18.dp)
                Spacer(modifier = Modifier.size(LxSpacing.sm))
                Text(text = formatTime(editorState.occurredAt))
            }
        }
    }
}

@Composable
private fun DurationCard(
    editorState: EditorState,
    onUpdate: (EditorState) -> Unit
) {
    FormCard {
        LxFieldLabel(
            text = "排便时长",
            trailing = {
                Text(
                    text = formatDuration(editorState.durationSeconds),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(LxSpacing.sm)) {
            OutlinedButton(
                onClick = {
                    onUpdate(
                        editorState.copy(
                            durationSeconds = (editorState.durationSeconds - 30).coerceAtLeast(1)
                        )
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(LxSizes.touchTarget)
            ) {
                Text(text = "-30 秒")
            }
            OutlinedButton(
                onClick = {
                    onUpdate(
                        editorState.copy(
                            durationSeconds = editorState.durationSeconds + 30
                        )
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(LxSizes.touchTarget)
            ) {
                Text(text = "+30 秒")
            }
        }
    }
}

/**
 * 布里斯托类型选择。
 *
 * 改动要点：
 * 1. 横向滚动 + 固定 86dp 方块 → FlowRow 自动换行，7 种类型一屏全部可见；
 *    原实现第 5~7 型被挤出屏幕外，用户不知道还有这些选项。
 * 2. emoji 图示 → LxBristolShape 矢量形态图，不依赖系统字体、跨设备一致。
 * 3. 选中态的说明文字用「标题 + 描述」两行固定结构，避免切换选项时卡片高度跳动。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BristolTypeCard(
    editorState: EditorState,
    onUpdate: (EditorState) -> Unit
) {
    val semantic = LxTheme.semantic
    val selectedType = bristolTypes.firstOrNull { it.type == editorState.bristolType }

    FormCard {
        LxFieldLabel(text = "布里斯托类型", trailing = {
            Text(
                text = "必填",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        })
        Text(
            text = "对着形态图选最接近的一种就行",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LxSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(LxSpacing.sm)
        ) {
            bristolTypes.forEach { type ->
                LxBristolTile(
                    type = type.type,
                    label = "${type.type} 型",
                    selected = editorState.bristolType == type.type,
                    onClick = { onUpdate(editorState.copy(bristolType = type.type)) },
                    accent = MaterialTheme.colorScheme.primary,
                    accentContainer = MaterialTheme.colorScheme.primaryContainer,
                    onAccentContainer = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        if (selectedType != null) {
            val ideal = selectedType.type == 3 || selectedType.type == 4
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(LxShapes.inner)
                    .background(
                        if (ideal) semantic.idealContainer else semantic.cautionContainer
                    )
                    .padding(LxSpacing.md)
            ) {
                Text(
                    text = "${selectedType.title} · ${selectedType.subtitle}",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (ideal) semantic.onIdealContainer else semantic.onCautionContainer
                )
                Spacer(modifier = Modifier.height(LxSpacing.xs))
                Text(
                    text = selectedType.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (ideal) semantic.onIdealContainer else semantic.onCautionContainer
                )
            }
        }
    }
}

/**
 * 单选属性卡（颜色 / 分量 / 气味）。
 * 选项改为自动换行 chip，撤销一次选择即取消选择，行为与原先一致。
 */
@Composable
private fun StoolAttributeCard(
    title: String,
    options: List<StoolOption>,
    selectedId: String?,
    onSelect: (String?) -> Unit
) {
    val semantic = LxTheme.semantic
    FormCard {
        LxFieldLabel(
            text = title,
            optional = true,
            trailing = {
                if (selectedId != null) {
                    TextButton(onClick = { onSelect(null) }) {
                        Text("清除")
                    }
                }
            }
        )
        LxOptionFlow(
            options = options,
            selectedIds = setOfNotNull(selectedId),
            onToggle = { id -> onSelect(if (id == selectedId) null else id) }
        )
        val selectedOption = options.firstOrNull { it.id == selectedId }
        if (selectedOption != null) {
            Spacer(modifier = Modifier.height(LxSpacing.xs))
            Text(
                text = selectedOption.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (selectedOption.warning) {
                Spacer(modifier = Modifier.height(LxSpacing.xs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LxIconWarning(tint = semantic.caution, size = 16.dp)
                    Spacer(modifier = Modifier.size(LxSpacing.xs))
                    Text(
                        text = "如持续出现建议就医",
                        style = MaterialTheme.typography.bodySmall,
                        color = semantic.caution
                    )
                }
            }
        }
    }
}

/**
 * 多选卡（排便感受 / 当前状态）。
 * 选中项的说明改为逐条列表展示，信息量不变但不挤在标签里。
 */
@Composable
private fun MultiSelectCard(
    title: String,
    options: List<StoolOption>,
    selectedIds: Set<String>,
    hint: String? = null,
    emptyHint: String = "未选择，可保持为空。",
    optional: Boolean = false,
    onClear: (() -> Unit)? = null,
    onToggle: (String) -> Unit
) {
    val semantic = LxTheme.semantic
    FormCard {
        LxFieldLabel(
            text = title,
            optional = optional,
            trailing = {
                if (onClear != null && selectedIds.isNotEmpty()) {
                    TextButton(onClick = onClear) {
                        Text("清空")
                    }
                }
            }
        )
        if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LxOptionFlow(
            options = options,
            selectedIds = selectedIds,
            onToggle = onToggle
        )
        val selectedOptions = options.filter { it.id in selectedIds }
        if (selectedOptions.isEmpty()) {
            Text(
                text = emptyHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(LxShapes.inner)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(LxSpacing.md),
                verticalArrangement = Arrangement.spacedBy(LxSpacing.xs)
            ) {
                selectedOptions.forEach { option ->
                    Text(
                        text = "${option.label}：${option.description}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (selectedOptions.any { it.warning }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LxIconWarning(tint = semantic.caution, size = 16.dp)
                        Spacer(modifier = Modifier.size(LxSpacing.xs))
                        Text(
                            text = "如持续出现建议就医",
                            style = MaterialTheme.typography.bodySmall,
                            color = semantic.caution
                        )
                    }
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
    FormCard {
        LxFieldLabel(text = "备注", optional = true)
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

@Composable
private fun PhotoCard(
    editorState: EditorState,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit
) {
    FormCard {
        LxFieldLabel(text = "照片", optional = true)
        if (editorState.photoPath != null) {
            AsyncImage(
                model = File(editorState.photoPath!!),
                contentDescription = "排便照片",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(LxShapes.inner),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(LxShapes.inner)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LxIconCamera(tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 26.dp)
                    Spacer(modifier = Modifier.height(LxSpacing.sm))
                    Text(
                        text = "暂未添加照片",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(LxSpacing.sm)) {
            Button(
                onClick = onTakePhoto,
                modifier = Modifier
                    .weight(1f)
                    .height(LxSizes.touchTarget)
            ) {
                LxIconCamera(tint = MaterialTheme.colorScheme.onPrimary, size = 18.dp)
                Spacer(modifier = Modifier.size(LxSpacing.sm))
                Text(text = "拍照")
            }
            OutlinedButton(
                onClick = onPickPhoto,
                modifier = Modifier
                    .weight(1f)
                    .height(LxSizes.touchTarget)
            ) {
                LxIconGallery(tint = MaterialTheme.colorScheme.onSurface, size = 18.dp)
                Spacer(modifier = Modifier.size(LxSpacing.sm))
                Text(text = "相册")
            }
            if (editorState.photoPath != null) {
                OutlinedButton(
                    onClick = onRemovePhoto,
                    modifier = Modifier
                        .weight(1f)
                        .height(LxSizes.touchTarget),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(text = "移除")
                }
            }
        }
    }
}
