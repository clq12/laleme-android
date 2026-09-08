package com.laxiang.app.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.laxiang.app.sync.WebDavClient
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class LaxiangRepository(
    private val context: Context,
    private val database: LaxiangDatabase,
    private val recordDao: RecordDao,
    private val settingsDao: SettingsDao
) {
    val visibleRecords: Flow<List<RecordEntity>> = recordDao.observeVisibleRecords()
    val settings: Flow<SettingsEntity?> = settingsDao.observeSettings()

    private val photoDirectory: File
        get() = File(context.filesDir, "photos").apply { mkdirs() }

    suspend fun saveRecord(record: RecordEntity) = withContext(Dispatchers.IO) {
        recordDao.upsertRecord(record)
    }

    suspend fun deleteRecord(record: RecordEntity) = withContext(Dispatchers.IO) {
        recordDao.upsertRecord(record.copy(deleted = true, updatedAt = System.currentTimeMillis()))
    }

    suspend fun getRecord(id: String): RecordEntity? = withContext(Dispatchers.IO) {
        recordDao.getRecord(id)
    }

    suspend fun saveSettings(newSettings: SettingsEntity) = withContext(Dispatchers.IO) {
        settingsDao.upsertSettings(newSettings.copy(id = 1))
    }

    suspend fun copyPhotoToAppStorage(source: Uri): String = withContext(Dispatchers.IO) {
        val target = File(photoDirectory, "${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(source)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IllegalStateException("无法读取照片")
        target.absolutePath
    }

    suspend fun overwriteUpload(newSettings: SettingsEntity) = withContext(Dispatchers.IO) {
        val settings = validateSettings(newSettings)
        settingsDao.upsertSettings(settings)

        val client = WebDavClient(settings.username, settings.password)
        val baseUrl = settings.webdavUrl
        val basePath = settings.remotePath
        client.ensureDirectory(baseUrl, basePath)
        client.ensureDirectory(baseUrl, "$basePath/photos")

        val localRecords = recordDao.getAllRecords().toMutableList()
        localRecords.forEachIndexed { index, record ->
            val localPhoto = record.photoPath?.let(::File)
            if (!record.deleted && localPhoto != null && localPhoto.exists()) {
                val photoUrl = remotePhotoUrl(client, baseUrl, basePath, record.id)
                client.uploadPhoto(photoUrl, localPhoto)
                localRecords[index] = record.copy(
                    photoRemoteUrl = photoUrl,
                    photoSyncedAt = System.currentTimeMillis()
                )
            }
        }

        val recordsJsonUrl = client.joinUrl(baseUrl, "$basePath/records.json")
        client.putJson(recordsJsonUrl, serializeRecords(localRecords))

        database.withTransaction {
            recordDao.deleteAllRecords()
            recordDao.upsertRecords(localRecords)
        }
        settingsDao.upsertSettings(
            settings.copy(lastSyncAt = System.currentTimeMillis())
        )
    }

    suspend fun overwriteDownload(newSettings: SettingsEntity) = withContext(Dispatchers.IO) {
        val settings = validateSettings(newSettings)
        settingsDao.upsertSettings(settings)

        val client = WebDavClient(settings.username, settings.password)
        val baseUrl = settings.webdavUrl
        val basePath = settings.remotePath
        val recordsJsonUrl = client.joinUrl(baseUrl, "$basePath/records.json")
        val remoteJson = client.getJson(recordsJsonUrl)
            ?: throw IllegalStateException("远端没有 records.json，无法覆盖下载")
        val remoteRecords = parseRecords(remoteJson).toMutableList()

        remoteRecords.forEachIndexed { index, record ->
            if (!record.deleted && !record.photoRemoteUrl.isNullOrBlank()) {
                val target = File(photoDirectory, "${record.id}.jpg")
                client.downloadPhoto(record.photoRemoteUrl!!, target)
                remoteRecords[index] = record.copy(
                    photoPath = target.absolutePath,
                    photoSyncedAt = target.lastModified()
                )
            }
        }

        database.withTransaction {
            recordDao.deleteAllRecords()
            recordDao.upsertRecords(remoteRecords)
        }
        removeOrphanPhotos(remoteRecords)
        settingsDao.upsertSettings(
            settings.copy(lastSyncAt = System.currentTimeMillis())
        )
    }

    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        val currentSettings = settingsDao.getSettings() ?: return@withContext false
        if (currentSettings.webdavUrl.isBlank() || currentSettings.username.isBlank()) {
            return@withContext false
        }
        val client = WebDavClient(currentSettings.username, currentSettings.password)
        client.testConnection(currentSettings.webdavUrl, currentSettings.remotePath)
    }

    private fun validateSettings(settings: SettingsEntity): SettingsEntity {
        if (settings.webdavUrl.isBlank() || settings.username.isBlank()) {
            throw IllegalStateException("请先配置 WebDAV 服务器和用户名")
        }
        return settings.copy(
            id = 1,
            webdavUrl = settings.webdavUrl.trim().trimEnd('/'),
            username = settings.username.trim(),
            remotePath = settings.remotePath.ifBlank { "/laxiang" }
        )
    }

    private fun remotePhotoUrl(
        client: WebDavClient,
        baseUrl: String,
        basePath: String,
        recordId: String
    ): String = client.joinUrl(
        baseUrl,
        "$basePath/photos/${Uri.encode("$recordId.jpg", "")}"
    )

    private fun removeOrphanPhotos(records: List<RecordEntity>) {
        val referencedPhotos = records
            .mapNotNull { it.photoPath }
            .map(::File)
            .filter { it.exists() }
            .map { it.absolutePath }
            .toSet()
        photoDirectory.listFiles()?.forEach { file ->
            if (file.isFile && file.absolutePath !in referencedPhotos) {
                file.delete()
            }
        }
    }

    private fun parseRecords(json: String): List<RecordEntity> {
        val array = JSONArray(json)
        return (0 until array.length()).map { index ->
            val obj = array.getJSONObject(index)
            RecordEntity(
                id = obj.getString("id"),
                occurredAt = obj.getLong("occurredAt"),
                durationSeconds = obj.getInt("durationSeconds"),
                bristolType = obj.getInt("bristolType"),
                notes = obj.optString("notes"),
                photoPath = null,
                photoRemoteUrl = obj.optString("photoRemoteUrl").ifBlank { null },
                photoSyncedAt = if (obj.has("photoSyncedAt") && !obj.isNull("photoSyncedAt")) {
                    obj.getLong("photoSyncedAt")
                } else {
                    null
                },
                deleted = obj.optBoolean("deleted", false),
                updatedAt = obj.getLong("updatedAt")
            )
        }
    }

    private fun serializeRecords(records: List<RecordEntity>): String {
        val array = JSONArray()
        records.forEach { record ->
            array.put(
                JSONObject()
                    .put("id", record.id)
                    .put("occurredAt", record.occurredAt)
                    .put("durationSeconds", record.durationSeconds)
                    .put("bristolType", record.bristolType)
                    .put("notes", record.notes)
                    .put("photoRemoteUrl", record.photoRemoteUrl ?: "")
                    .put("photoSyncedAt", record.photoSyncedAt ?: JSONObject.NULL)
                    .put("deleted", record.deleted)
                    .put("updatedAt", record.updatedAt)
            )
        }
        return array.toString()
    }
}
