package com.laxiang.app

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laxiang.app.data.BackupInfo
import com.laxiang.app.data.LaxiangRepository
import com.laxiang.app.data.PhotoUsage
import com.laxiang.app.data.RecordEntity
import com.laxiang.app.data.SettingsEntity
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EditorState(
    val existingId: String?,
    val occurredAt: Long,
    val durationSeconds: Int,
    val bristolType: Int,
    val stoolColor: String?,
    val stoolAmount: String?,
    val stoolSmell: String?,
    val bowelFeelings: Set<String>,
    val bodyStatuses: Set<String>,
    val notes: String,
    val photoPath: String?,
    val photoRemoteUrl: String?,
    val photoSyncedAt: Long?
)

class LaxiangViewModel(
    private val appContext: Context,
    private val repository: LaxiangRepository
) : ViewModel() {

    val records: StateFlow<List<RecordEntity>> = repository.visibleRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<SettingsEntity?> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _timerStart = MutableStateFlow<Long?>(null)
    val timerStart: StateFlow<Long?> = _timerStart.asStateFlow()

    private val _editorState = MutableStateFlow<EditorState?>(null)
    val editorState: StateFlow<EditorState?> = _editorState.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private val _syncProgress = MutableStateFlow<String?>(null)
    val syncProgress: StateFlow<String?> = _syncProgress.asStateFlow()

    private val _syncRunning = MutableStateFlow(false)
    val syncRunning: StateFlow<Boolean> = _syncRunning.asStateFlow()

    /** 最近一次被软删除的记录，用来撑起 Snackbar 上的「撤销」。 */
    private val _deletedRecord = MutableStateFlow<RecordEntity?>(null)
    val deletedRecord: StateFlow<RecordEntity?> = _deletedRecord.asStateFlow()

    private val _photoUsage = MutableStateFlow<PhotoUsage?>(null)
    val photoUsage: StateFlow<PhotoUsage?> = _photoUsage.asStateFlow()

    private val _backupInfo = MutableStateFlow<BackupInfo?>(null)
    val backupInfo: StateFlow<BackupInfo?> = _backupInfo.asStateFlow()

    private val _localRecordCount = MutableStateFlow(0)
    val localRecordCount: StateFlow<Int> = _localRecordCount.asStateFlow()

    // ------------------------------------------------------------ 计时器

    fun startTimer() {
        _timerStart.value = System.currentTimeMillis()
    }

    fun stopTimerAndOpenEditor() {
        val startedAt = _timerStart.value ?: return
        val durationSeconds = (((System.currentTimeMillis() - startedAt) / 1000L).toInt()).coerceAtLeast(1)
        _timerStart.value = null
        _editorState.value = EditorState(
            existingId = null,
            occurredAt = startedAt,
            durationSeconds = durationSeconds,
            bristolType = 4,
            stoolColor = null,
            stoolAmount = null,
            stoolSmell = null,
            bowelFeelings = emptySet(),
            bodyStatuses = emptySet(),
            notes = "",
            photoPath = null,
            photoRemoteUrl = null,
            photoSyncedAt = null
        )
    }

    fun cancelTimer() {
        _timerStart.value = null
    }

    // ------------------------------------------------------------ 编辑

    fun beginEdit(record: RecordEntity) {
        _editorState.value = EditorState(
            existingId = record.id,
            occurredAt = record.occurredAt,
            durationSeconds = record.durationSeconds,
            bristolType = record.bristolType,
            stoolColor = record.stoolColor,
            stoolAmount = record.stoolAmount,
            stoolSmell = record.stoolSmell,
            bowelFeelings = splitIds(record.bowelFeelings),
            bodyStatuses = splitIds(record.bodyStatuses),
            notes = record.notes,
            photoPath = record.photoPath,
            photoRemoteUrl = record.photoRemoteUrl,
            photoSyncedAt = record.photoSyncedAt
        )
    }

    fun updateEditor(update: EditorState) {
        _editorState.value = update
    }

    fun setEditorPhoto(photoPath: String?) {
        _editorState.value = _editorState.value?.copy(photoPath = photoPath)
    }

    fun pickPhoto(uri: Uri) {
        viewModelScope.launch {
            try {
                val photoPath = repository.copyPhotoToAppStorage(uri)
                setEditorPhoto(photoPath)
            } catch (error: Exception) {
                _syncMessage.value = "照片读取失败：${error.message}"
            }
        }
    }

    fun saveEditor() {
        val state = _editorState.value ?: return
        viewModelScope.launch {
            val existing = state.existingId?.let { repository.getRecord(it) }
            val record = RecordEntity(
                id = state.existingId ?: UUID.randomUUID().toString(),
                occurredAt = state.occurredAt,
                durationSeconds = state.durationSeconds,
                bristolType = state.bristolType,
                stoolColor = state.stoolColor,
                stoolAmount = state.stoolAmount,
                stoolSmell = state.stoolSmell,
                bowelFeelings = state.bowelFeelings.joinToString(",").ifBlank { null },
                bodyStatuses = state.bodyStatuses.joinToString(",").ifBlank { null },
                notes = state.notes,
                photoPath = state.photoPath,
                photoRemoteUrl = existing?.photoRemoteUrl ?: state.photoRemoteUrl,
                photoSyncedAt = existing?.photoSyncedAt ?: state.photoSyncedAt,
                deleted = false,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveRecord(record)
            clearEditor()
        }
    }

    fun clearEditor() {
        _editorState.value = null
    }

    // ------------------------------------------------------------ 删除与撤销

    fun deleteRecord(record: RecordEntity) {
        viewModelScope.launch {
            repository.deleteRecord(record)
            _deletedRecord.value = record
        }
    }

    fun deleteEditor() {
        val state = _editorState.value ?: return
        val existingId = state.existingId ?: return clearEditor()
        viewModelScope.launch {
            val existing = repository.getRecord(existingId) ?: return@launch
            repository.deleteRecord(existing)
            _deletedRecord.value = existing
            clearEditor()
        }
    }

    fun undoDelete() {
        val record = _deletedRecord.value ?: return
        _deletedRecord.value = null
        viewModelScope.launch { repository.restoreRecord(record) }
    }

    fun consumeDeletedRecord() {
        _deletedRecord.value = null
    }

    // ------------------------------------------------------------ 设置与同步

    fun saveSettings(
        webdavUrl: String,
        username: String,
        password: String,
        remotePath: String
    ) {
        viewModelScope.launch {
            repository.saveSettings(buildSettings(webdavUrl, username, password, remotePath))
            _syncMessage.value = "WebDAV 配置已保存"
        }
    }

    fun mergeSync(webdavUrl: String, username: String, password: String, remotePath: String) =
        runSync("合并同步") { progress ->
            repository.mergeSync(buildSettings(webdavUrl, username, password, remotePath), progress)
        }

    fun overwriteUpload(webdavUrl: String, username: String, password: String, remotePath: String) =
        runSync("覆盖上传") { progress ->
            repository.overwriteUpload(buildSettings(webdavUrl, username, password, remotePath), progress)
        }

    fun overwriteDownload(webdavUrl: String, username: String, password: String, remotePath: String) =
        runSync("覆盖下载") { progress ->
            repository.overwriteDownload(buildSettings(webdavUrl, username, password, remotePath), progress)
        }

    fun testConnection() {
        viewModelScope.launch {
            _syncMessage.value = "正在测试连接…"
            try {
                val result = repository.testConnection()
                _syncMessage.value = if (result) "连接成功" else "连接失败"
            } catch (error: Exception) {
                _syncMessage.value = "连接失败：${error.message}"
            }
        }
    }

    /** 同步串行执行，避免同时点两个按钮把远端写乱。 */
    private fun runSync(action: String, block: suspend ((String) -> Unit) -> String) {
        if (_syncRunning.value) {
            _syncMessage.value = "已有同步任务在进行，请稍候"
            return
        }
        viewModelScope.launch {
            _syncRunning.value = true
            _syncProgress.value = "准备中…"
            try {
                _syncMessage.value = block { text -> _syncProgress.value = text }
            } catch (error: Exception) {
                _syncMessage.value = "$action 失败：${error.message ?: "未知错误"}"
            } finally {
                _syncProgress.value = null
                _syncRunning.value = false
                refreshMaintenanceInfo()
            }
        }
    }

    // ------------------------------------------------------------ 数据维护与导出

    fun refreshMaintenanceInfo() {
        viewModelScope.launch {
            _photoUsage.value = runCatching { repository.photoUsage() }.getOrNull()
            _backupInfo.value = runCatching { repository.backupInfo() }.getOrNull()
            _localRecordCount.value = runCatching { repository.countLocalRecords() }.getOrDefault(0)
        }
    }

    fun cleanOrphanPhotos() {
        viewModelScope.launch {
            try {
                val deleted = repository.cleanOrphanPhotos()
                _photoUsage.value = repository.photoUsage()
                _syncMessage.value = if (deleted > 0) {
                    "已清理 $deleted 张无用照片"
                } else {
                    "没有需要清理的照片"
                }
            } catch (error: Exception) {
                _syncMessage.value = "清理失败：${error.message}"
            }
        }
    }

    fun exportData(uri: Uri) {
        viewModelScope.launch {
            _syncProgress.value = "正在打包数据…"
            try {
                val summary = appContext.contentResolver.openOutputStream(uri)?.use { output ->
                    repository.exportTo(output)
                } ?: throw IllegalStateException("无法写入所选位置")
                _syncMessage.value = "导出完成：${summary.recordCount} 条记录、${summary.photoCount} 张照片"
            } catch (error: Exception) {
                _syncMessage.value = "导出失败：${error.message}"
            } finally {
                _syncProgress.value = null
            }
        }
    }

    fun dismissSyncMessage() {
        _syncMessage.value = null
    }

    /** 多选字段在库里存成逗号分隔的 id 串，读出来转成集合。 */
    private fun splitIds(value: String?): Set<String> =
        value?.split(',')?.filter { it.isNotBlank() }?.toSet() ?: emptySet()

    private fun buildSettings(
        webdavUrl: String,
        username: String,
        password: String,
        remotePath: String
    ) = SettingsEntity(
        id = 1,
        webdavUrl = webdavUrl.trim(),
        username = username.trim(),
        password = password,
        remotePath = remotePath.ifBlank { "/laxiang" },
        lastSyncAt = settings.value?.lastSyncAt
    )
}
