package com.laxiang.app

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laxiang.app.data.LaxiangRepository
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
    val notes: String,
    val photoPath: String?,
    val photoRemoteUrl: String?,
    val photoSyncedAt: Long?
)

class LaxiangViewModel(
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
            notes = "",
            photoPath = null,
            photoRemoteUrl = null,
            photoSyncedAt = null
        )
    }

    fun beginEdit(record: RecordEntity) {
        _editorState.value = EditorState(
            existingId = record.id,
            occurredAt = record.occurredAt,
            durationSeconds = record.durationSeconds,
            bristolType = record.bristolType,
            stoolColor = record.stoolColor,
            stoolAmount = record.stoolAmount,
            stoolSmell = record.stoolSmell,
            bowelFeelings = record.bowelFeelings
                ?.split(',')
                ?.filter { it.isNotBlank() }
                ?.toSet()
                ?: emptySet(),
            notes = record.notes,
            photoPath = record.photoPath,
            photoRemoteUrl = record.photoRemoteUrl,
            photoSyncedAt = record.photoSyncedAt
        )
    }

    fun deleteRecord(record: RecordEntity) {
        viewModelScope.launch {
            repository.deleteRecord(record)
        }
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
                bowelFeelings = state.bowelFeelings.joinToString(","),
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

    fun deleteEditor() {
        val state = _editorState.value ?: return
        val existingId = state.existingId ?: return clearEditor()
        viewModelScope.launch {
            val existing = repository.getRecord(existingId) ?: return@launch
            repository.deleteRecord(existing)
            clearEditor()
        }
    }

    fun clearEditor() {
        _editorState.value = null
    }

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

    fun overwriteUpload(
        webdavUrl: String,
        username: String,
        password: String,
        remotePath: String
    ) {
        viewModelScope.launch {
            _syncMessage.value = "正在覆盖上传…"
            try {
                repository.overwriteUpload(
                    buildSettings(webdavUrl, username, password, remotePath)
                )
                _syncMessage.value = "覆盖上传成功"
            } catch (error: Exception) {
                _syncMessage.value = "覆盖上传失败：${error.message}"
            }
        }
    }

    fun overwriteDownload(
        webdavUrl: String,
        username: String,
        password: String,
        remotePath: String
    ) {
        viewModelScope.launch {
            _syncMessage.value = "正在覆盖下载…"
            try {
                repository.overwriteDownload(
                    buildSettings(webdavUrl, username, password, remotePath)
                )
                _syncMessage.value = "覆盖下载成功"
            } catch (error: Exception) {
                _syncMessage.value = "覆盖下载失败：${error.message}"
            }
        }
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

    fun dismissSyncMessage() {
        _syncMessage.value = null
    }

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
