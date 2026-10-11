package com.laxiang.app.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import com.laxiang.app.data.BackupInfo
import com.laxiang.app.data.PhotoUsage
import com.laxiang.app.data.SectionPrefs
import com.laxiang.app.data.SettingsEntity
import com.laxiang.app.model.formatDateTime
import com.laxiang.app.ui.common.CollapsibleSection
import com.laxiang.app.ui.common.LxDivider
import com.laxiang.app.ui.common.LxFieldLabel
import com.laxiang.app.ui.common.LxIconDelete
import com.laxiang.app.ui.common.LxIconDownload
import com.laxiang.app.ui.common.LxIconStorage
import com.laxiang.app.ui.common.LxIconSync
import com.laxiang.app.ui.common.LxIconWarning
import com.laxiang.app.ui.theme.LxShapes
import com.laxiang.app.ui.theme.LxSizes
import com.laxiang.app.ui.theme.LxSpacing
import com.laxiang.app.ui.theme.LxTheme

private enum class PendingSyncAction(val title: String, val confirmLabel: String, val description: String) {
    Upload(
        title = "确认覆盖上传？",
        confirmLabel = "确定覆盖上传",
        description = "将以【本机】的记录和照片覆盖云端，云端原有内容会被替换，此操作不可撤销。"
    ),
    Download(
        title = "确认覆盖下载？",
        confirmLabel = "确定覆盖下载",
        description = "将以【云端】的 records.json 和照片覆盖本机，本机现有记录会被替换。" +
            "执行前会自动把本机数据备份到应用私有目录（保留最近 5 份）。"
    )
}

/**
 * 同步方式的风险等级。
 * 用「安全 / 需谨慎 / 不可撤销」三档把三种操作分开呈现，
 * 避免用户把「合并同步」和「覆盖上传」当成同一件事随便点。
 */
private enum class RiskLevel(val label: String, val dotColor: @Composable () -> Color) {
    Safe("安全", { LxTheme.semantic.ideal }),
    Careful("需谨慎", { LxTheme.semantic.caution }),
    Destructive("不可撤销", { LxTheme.semantic.alert })
}

@Composable
fun SettingsScreen(
    settings: SettingsEntity?,
    syncProgress: String?,
    syncRunning: Boolean,
    photoUsage: PhotoUsage?,
    backupInfo: BackupInfo?,
    localRecordCount: Int,
    onSaveSettings: (String, String, String, String) -> Unit,
    onMergeSync: (String, String, String, String) -> Unit,
    onOverwriteUpload: (String, String, String, String) -> Unit,
    onOverwriteDownload: (String, String, String, String) -> Unit,
    onTestConnection: () -> Unit,
    onCleanOrphanPhotos: () -> Unit,
    onRefreshMaintenance: () -> Unit,
    onExportData: (Uri) -> Unit
) {
    var webdavUrl by remember { mutableStateOf(settings?.webdavUrl.orEmpty()) }
    var username by remember { mutableStateOf(settings?.username.orEmpty()) }
    var password by remember { mutableStateOf(settings?.password.orEmpty()) }
    var remotePath by remember { mutableStateOf(settings?.remotePath ?: "/laxiang") }
    var showPassword by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<PendingSyncAction?>(null) }

    // 折叠状态：读一次记住，之后由用户点击驱动并落盘
    val context = LocalContext.current
    val sectionPrefs = remember { SectionPrefs(context) }
    var syncExpanded by remember {
        mutableStateOf(sectionPrefs.isExpanded(SectionPrefs.SETTINGS_SYNC, true))
    }
    var storageExpanded by remember {
        mutableStateOf(sectionPrefs.isExpanded(SectionPrefs.SETTINGS_STORAGE, false))
    }
    var backupExpanded by remember {
        mutableStateOf(sectionPrefs.isExpanded(SectionPrefs.SETTINGS_BACKUP, false))
    }

    LaunchedEffect(settings) {
        settings?.let {
            webdavUrl = it.webdavUrl
            username = it.username
            password = it.password
            remotePath = it.remotePath
        }
    }

    LaunchedEffect(Unit) {
        onRefreshMaintenance()
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let(onExportData)
    }

    pendingAction?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LxIconWarning(tint = MaterialTheme.colorScheme.error, size = 20.dp)
                    Spacer(modifier = Modifier.size(LxSpacing.sm))
                    Text(action.title, color = MaterialTheme.colorScheme.error)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(LxSpacing.md)) {
                    Text(action.description)
                    Text(
                        text = "目标目录：${remotePath.ifBlank { "/laxiang" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "本机当前有 $localRecordCount 条记录。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingAction = null
                        when (action) {
                            PendingSyncAction.Upload ->
                                onOverwriteUpload(webdavUrl, username, password, remotePath)
                            PendingSyncAction.Download ->
                                onOverwriteDownload(webdavUrl, username, password, remotePath)
                        }
                    }
                ) {
                    Text(
                        text = action.confirmLabel,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingAction = null }) { Text("取消") }
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
            // 顶部让开状态栏：Scaffold 已把 safeDrawing 顶边给了 body，
            // 这里只补一段呼吸留白，避免与 Scaffold 的 inset 双重叠加。
            Spacer(modifier = Modifier.height(LxSpacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(LxShapes.inner)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    LxIconStorage(tint = MaterialTheme.colorScheme.onPrimaryContainer, size = 24.dp)
                }
                Spacer(modifier = Modifier.size(LxSpacing.md))
                Column {
                    Text(
                        text = "设置",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "WebDAV 备份、数据维护与导出",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ── 分组一：数据同步（服务器配置 + 三种同步方式）──
        item {
            CollapsibleSection(
                title = "数据同步",
                subtitle = when {
                    syncRunning && syncProgress != null -> syncProgress
                    settings?.lastSyncAt != null -> "最近同步 ${formatDateTime(settings.lastSyncAt)}"
                    else -> "配置 WebDAV 服务器并同步记录"
                },
                expanded = syncExpanded,
                onToggle = {
                    syncExpanded = !syncExpanded
                    sectionPrefs.setExpanded(SectionPrefs.SETTINGS_SYNC, syncExpanded)
                },
                trailing = if (syncRunning) {
                    {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    null
                }
            ) {
                LxFieldLabel(text = "WebDAV 服务器")
                OutlinedTextField(
                    value = webdavUrl,
                    onValueChange = { webdavUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("服务器地址") },
                    placeholder = { Text("https://dav.example.com/dav") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("用户名") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("密码") },
                    singleLine = true,
                    visualTransformation = if (showPassword) {
                        androidx.compose.ui.text.input.VisualTransformation.None
                    } else {
                        androidx.compose.ui.text.input.PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        // 明文/隐藏切换做成图标按钮，触控区仍保持 48dp
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Text(
                                text = if (showPassword) "隐藏" else "显示",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
                OutlinedTextField(
                    value = remotePath,
                    onValueChange = { remotePath = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("远端目录") },
                    singleLine = true,
                    supportingText = { Text("云端会把记录和照片放在这个目录下") }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(LxSpacing.sm)) {
                    Button(
                        onClick = { onSaveSettings(webdavUrl, username, password, remotePath) },
                        modifier = Modifier
                            .weight(1f)
                            .height(LxSizes.touchTarget),
                        enabled = !syncRunning
                    ) {
                        Text("保存配置")
                    }
                    OutlinedButton(
                        onClick = onTestConnection,
                        modifier = Modifier
                            .weight(1f)
                            .height(LxSizes.touchTarget),
                        enabled = !syncRunning
                    ) {
                        LxIconSync(tint = MaterialTheme.colorScheme.onSurface, size = 18.dp)
                        Spacer(modifier = Modifier.size(LxSpacing.sm))
                        Text("测试连接")
                    }
                }

                if (syncProgress != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(LxShapes.inner)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(LxSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(LxSpacing.sm)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(
                            text = syncProgress,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                LxDivider()

                // ── 推荐路径：合并同步 ──
                LxFieldLabel(text = "推荐 · 合并同步")
                SyncModeCard(
                    risk = RiskLevel.Safe,
                    title = "合并同步",
                    summary = "两边都保留，谁也不覆盖谁",
                    detail = "按记录逐条比对更新时间，照片双向补齐。" +
                        "手机和电脑交替使用时用这个，不会丢数据。",
                    actionLabel = "合并同步",
                    onAction = { onMergeSync(webdavUrl, username, password, remotePath) },
                    enabled = !syncRunning
                )

                // ── 危险区：两种覆盖操作 ──
                LxDivider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LxIconWarning(tint = LxTheme.semantic.alert, size = 18.dp)
                    Spacer(modifier = Modifier.size(LxSpacing.sm))
                    Text(
                        text = "危险操作",
                        style = MaterialTheme.typography.titleSmall,
                        color = LxTheme.semantic.alert
                    )
                }
                Text(
                    text = "下面两个操作是单向整体替换，不做合并。点击后还会再确认一次，覆盖下载会先自动备份本机数据。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SyncModeCard(
                    risk = RiskLevel.Destructive,
                    title = "覆盖上传",
                    summary = "用本机数据替换云端",
                    detail = "云端原有的记录和照片会被删除，此操作不可撤销。",
                    actionLabel = "覆盖上传",
                    onAction = { pendingAction = PendingSyncAction.Upload },
                    enabled = !syncRunning
                )
                SyncModeCard(
                    risk = RiskLevel.Destructive,
                    title = "覆盖下载",
                    summary = "用云端数据替换本机",
                    detail = "本机现有记录会被替换；执行前会自动备份本机数据，出问题还能回滚。",
                    actionLabel = "覆盖下载",
                    onAction = { pendingAction = PendingSyncAction.Download },
                    enabled = !syncRunning
                )
            }
        }

        // ── 分组二：本地存储 ──
        item {
            CollapsibleSection(
                title = "本地存储",
                subtitle = photoUsage?.let { "${it.fileCount} 张照片 · ${formatBytes(it.totalBytes)}" }
                    ?: "统计中…",
                expanded = storageExpanded,
                onToggle = {
                    storageExpanded = !storageExpanded
                    sectionPrefs.setExpanded(SectionPrefs.SETTINGS_STORAGE, storageExpanded)
                }
            ) {
                InfoRow("记录条数", "$localRecordCount 条")
                LxDivider()
                InfoRow(
                    label = "照片",
                    value = photoUsage?.let { "${it.fileCount} 张 · ${formatBytes(it.totalBytes)}" }
                        ?: "统计中…"
                )
                InfoRow(
                    label = "无主照片",
                    value = photoUsage?.let {
                        if (it.orphanCount == 0) {
                            "无"
                        } else {
                            "${it.orphanCount} 张 · ${formatBytes(it.orphanBytes)}"
                        }
                    } ?: "统计中…"
                )
                InfoRow(
                    label = "自动备份",
                    value = backupInfo?.let {
                        if (it.count == 0) {
                            "暂无"
                        } else {
                            "${it.count} 份 · ${formatBytes(it.totalBytes)}"
                        }
                    } ?: "统计中…"
                )
                backupInfo?.latestAt?.let { latest ->
                    Text(
                        text = "最近备份：${formatDateTime(latest)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(LxSpacing.sm)) {
                    OutlinedButton(
                        onClick = onCleanOrphanPhotos,
                        modifier = Modifier
                            .weight(1f)
                            .height(LxSizes.touchTarget),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    ) {
                        LxIconDelete(tint = MaterialTheme.colorScheme.error, size = 18.dp)
                        Spacer(modifier = Modifier.size(LxSpacing.sm))
                        Text("清理无主照片")
                    }
                    OutlinedButton(
                        onClick = onRefreshMaintenance,
                        modifier = Modifier
                            .weight(1f)
                            .height(LxSizes.touchTarget)
                    ) {
                        Text("刷新")
                    }
                }
                Text(
                    text = "删除记录时照片会暂时保留，方便误删后撤销；确认不再需要就点「清理无主照片」回收空间。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ── 分组三：备份导出 ──
        item {
            CollapsibleSection(
                title = "备份与导出",
                subtitle = "打包全部记录与照片为 zip",
                expanded = backupExpanded,
                onToggle = {
                    backupExpanded = !backupExpanded
                    sectionPrefs.setExpanded(SectionPrefs.SETTINGS_BACKUP, backupExpanded)
                }
            ) {
                Text(
                    text = "把全部记录和照片打包成一个 zip，存到手机任意位置。不依赖 WebDAV，可以随时拿走自己的数据。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = { exportLauncher.launch(exportFileName()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(LxSizes.touchTarget)
                ) {
                    LxIconDownload(tint = MaterialTheme.colorScheme.onSurface, size = 18.dp)
                    Spacer(modifier = Modifier.size(LxSpacing.sm))
                    Text("导出数据（zip）")
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(LxSpacing.xl))
        }
    }
}

/**
 * 同步方式卡片。
 * 风险等级用「色点 + 文字」双重表达（不单靠颜色），
 * 操作按钮放在卡片内部，和说明文字绑定，避免按钮与说明错位。
 */
@Composable
private fun SyncModeCard(
    risk: RiskLevel,
    title: String,
    summary: String,
    detail: String,
    actionLabel: String,
    onAction: () -> Unit,
    enabled: Boolean
) {
    val dotColor = risk.dotColor()
    val destructive = risk == RiskLevel.Destructive

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LxShapes.inner)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(LxSpacing.md),
        verticalArrangement = Arrangement.spacedBy(LxSpacing.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(LxShapes.pill)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.size(LxSpacing.sm))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.size(LxSpacing.sm))
            Box(
                modifier = Modifier
                    .clip(LxShapes.pill)
                    .background(dotColor.copy(alpha = 0.16f))
                    .padding(horizontal = LxSpacing.sm, vertical = 2.dp)
            ) {
                Text(
                    text = risk.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = dotColor
                )
            }
        }
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (destructive) {
            OutlinedButton(
                onClick = onAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LxSizes.touchTarget),
                enabled = enabled,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
            ) {
                Text(actionLabel)
            }
        } else {
            Button(
                onClick = onAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LxSizes.touchTarget),
                enabled = enabled
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun exportFileName(): String {
    val stamp = java.time.LocalDateTime.now()
        .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
    return "laxiang-export-$stamp.zip"
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024 * 1024 -> "%.1f GB".format(bytes / 1024.0 / 1024 / 1024)
    bytes >= 1024L * 1024 -> "%.1f MB".format(bytes / 1024.0 / 1024)
    bytes >= 1024L -> "%.0f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}
