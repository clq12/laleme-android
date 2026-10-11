package com.laxiang.app.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.laxiang.app.sync.WebDavClient
import java.io.BufferedOutputStream
import java.io.File
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class PhotoUsage(
    val fileCount: Int,
    val totalBytes: Long,
    val orphanCount: Int,
    val orphanBytes: Long
)

data class BackupInfo(
    val count: Int,
    val latestAt: Long?,
    val totalBytes: Long
)

data class ExportSummary(
    val recordCount: Int,
    val photoCount: Int
)

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

    private val backupDirectory: File
        get() = File(context.filesDir, "backups").apply { mkdirs() }

    // ---------------------------------------------------------------- 记录

    suspend fun saveRecord(record: RecordEntity) = withContext(Dispatchers.IO) {
        recordDao.upsertRecord(record)
    }

    /**
     * 软删除：只打墓碑标记，**照片文件保留**。
     * 这样「撤销」能完整恢复，照片也不会在误删的瞬间就没了。
     */
    suspend fun deleteRecord(record: RecordEntity) = withContext(Dispatchers.IO) {
        recordDao.upsertRecord(record.copy(deleted = true, updatedAt = System.currentTimeMillis()))
    }

    suspend fun restoreRecord(record: RecordEntity) = withContext(Dispatchers.IO) {
        // 撤销时照片可能已被「清理无主照片」收走，那就别留一个指向空气的路径
        val photoMissing = record.photoPath?.let { !File(it).isFile } ?: false
        recordDao.upsertRecord(
            record.copy(
                deleted = false,
                photoPath = if (photoMissing) null else record.photoPath,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getRecord(id: String): RecordEntity? = withContext(Dispatchers.IO) {
        recordDao.getRecord(id)
    }

    suspend fun countLocalRecords(): Int = withContext(Dispatchers.IO) {
        recordDao.countVisibleRecords()
    }

    // ---------------------------------------------------------------- 设置

    suspend fun saveSettings(newSettings: SettingsEntity) = withContext(Dispatchers.IO) {
        settingsDao.upsertSettings(newSettings.copy(id = 1))
    }

    // ---------------------------------------------------------------- 照片

    /** 把外部图片复制进 App 私有目录，并顺手压缩（长边 1600 / JPEG 85）。 */
    suspend fun copyPhotoToAppStorage(source: Uri): String = withContext(Dispatchers.IO) {
        val target = File(photoDirectory, "${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(source)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IllegalStateException("无法读取照片")
        ImageCompressor.compressInPlace(target)
        target.absolutePath
    }

    suspend fun photoUsage(): PhotoUsage = withContext(Dispatchers.IO) {
        val referenced = referencedPhotoPaths(recordDao.getAllRecords())
        val files = photoDirectory.listFiles()?.filter { it.isFile }.orEmpty()
        val orphans = files.filter { it.absolutePath !in referenced }
        PhotoUsage(
            fileCount = files.size,
            totalBytes = files.sumOf { it.length() },
            orphanCount = orphans.size,
            orphanBytes = orphans.sumOf { it.length() }
        )
    }

    /** 删掉没有被任何有效记录引用的照片，并清掉指向已丢失文件的悬空路径。返回删除的文件数。 */
    suspend fun cleanOrphanPhotos(): Int = withContext(Dispatchers.IO) {
        val records = recordDao.getAllRecords()
        val referenced = referencedPhotoPaths(records)

        var deleted = 0
        photoDirectory.listFiles()?.forEach { file ->
            if (file.isFile && file.absolutePath !in referenced && file.delete()) deleted++
        }

        val dangling = records.filter { record ->
            val path = record.photoPath ?: return@filter false
            !File(path).isFile
        }
        if (dangling.isNotEmpty()) {
            recordDao.upsertRecords(dangling.map { it.copy(photoPath = null) })
        }
        deleted
    }

    private fun referencedPhotoPaths(records: List<RecordEntity>): Set<String> =
        records
            .filter { !it.deleted }
            .mapNotNull { it.photoPath }
            .map(::File)
            .filter { it.isFile }
            .map { it.absolutePath }
            .toSet()

    // ---------------------------------------------------------------- 备份

    /**
     * 覆盖下载前把本机数据整体快照一份到 `files/backups/<时间戳>/`。
     * 覆盖下载会清空本地表并顺带删掉本地照片，没有这个快照就真的救不回来了。
     */
    private suspend fun createBackupSnapshot(): File? {
        val records = recordDao.getAllRecords()
        if (records.isEmpty()) return null

        val directory = File(backupDirectory, BACKUP_STAMP.format(LocalDateTime.now())).apply { mkdirs() }
        File(directory, "records.json").writeText(serializeRecords(records))

        val photosDirectory = File(directory, "photos").apply { mkdirs() }
        records
            .mapNotNull { it.photoPath }
            .map(::File)
            .filter { it.isFile }
            .forEach { source ->
                runCatching { source.copyTo(File(photosDirectory, source.name), overwrite = true) }
            }

        pruneBackups(KEEP_BACKUPS)
        return directory
    }

    private fun pruneBackups(keep: Int) {
        val directories = backupDirectory
            .listFiles { file -> file.isDirectory }
            ?.sortedByDescending { it.name }
            ?: return
        directories.drop(keep).forEach { runCatching { it.deleteRecursively() } }
    }

    suspend fun backupInfo(): BackupInfo = withContext(Dispatchers.IO) {
        val directories = backupDirectory
            .listFiles { file -> file.isDirectory }
            ?.sortedByDescending { it.name }
            .orEmpty()
        BackupInfo(
            count = directories.size,
            latestAt = directories.firstOrNull()?.let { dir ->
                dir.walkBottomUp().filter { it.isFile }.maxOfOrNull { it.lastModified() }
            },
            totalBytes = directories.sumOf { dir ->
                dir.walkBottomUp().filter { it.isFile }.sumOf { it.length() }
            }
        )
    }

    // ---------------------------------------------------------------- 同步

    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        val currentSettings = settingsDao.getSettings() ?: return@withContext false
        if (currentSettings.webdavUrl.isBlank() || currentSettings.username.isBlank()) {
            return@withContext false
        }
        val client = WebDavClient(currentSettings.username, currentSettings.password)
        client.testConnection(currentSettings.webdavUrl, currentSettings.remotePath)
    }

    suspend fun overwriteUpload(
        newSettings: SettingsEntity,
        onProgress: ((String) -> Unit)? = null
    ): String = withContext(Dispatchers.IO) {
        val settings = validateSettings(newSettings)
        settingsDao.upsertSettings(settings)

        val client = WebDavClient(settings.username, settings.password)
        val baseUrl = settings.webdavUrl
        val basePath = settings.remotePath
        client.ensureDirectory(baseUrl, basePath)
        client.ensureDirectory(baseUrl, "$basePath/photos")

        val localRecords = recordDao.getAllRecords().toMutableList()
        val uploadable = localRecords.filter { record ->
            !record.deleted && record.photoPath?.let { path -> File(path).isFile } == true
        }

        uploadable.forEachIndexed { index, record ->
            onProgress?.invoke("上传照片 ${index + 1}/${uploadable.size}")
            val photoUrl = remotePhotoUrl(client, baseUrl, basePath, record.id)
            client.uploadPhoto(photoUrl, File(record.photoPath!!))
            val position = localRecords.indexOfFirst { it.id == record.id }
            if (position >= 0) {
                localRecords[position] = record.copy(
                    photoRemoteUrl = photoUrl,
                    photoSyncedAt = System.currentTimeMillis()
                )
            }
        }

        onProgress?.invoke("写入远端 records.json…")
        client.putJson(client.joinUrl(baseUrl, "$basePath/records.json"), serializeRecords(localRecords))

        onProgress?.invoke("清理云端无用照片…")
        runCatching { cleanRemoteOrphanPhotos(client, baseUrl, basePath, localRecords) }

        database.withTransaction {
            recordDao.deleteAllRecords()
            recordDao.upsertRecords(localRecords)
        }
        settingsDao.upsertSettings(settings.copy(lastSyncAt = System.currentTimeMillis()))

        "覆盖上传完成：${localRecords.size} 条记录、${uploadable.size} 张照片"
    }

    suspend fun overwriteDownload(
        newSettings: SettingsEntity,
        onProgress: ((String) -> Unit)? = null
    ): String = withContext(Dispatchers.IO) {
        val settings = validateSettings(newSettings)
        settingsDao.upsertSettings(settings)

        val client = WebDavClient(settings.username, settings.password)
        val baseUrl = settings.webdavUrl
        val basePath = settings.remotePath
        val recordsJsonUrl = client.joinUrl(baseUrl, "$basePath/records.json")

        onProgress?.invoke("备份本机数据…")
        val backup = createBackupSnapshot()

        onProgress?.invoke("读取远端数据…")
        val remoteJson = client.getJson(recordsJsonUrl)
            ?: throw IllegalStateException("远端没有 records.json，无法覆盖下载")
        val remoteRecords = parseRecords(remoteJson).toMutableList()

        val downloadable = remoteRecords.filter { !it.deleted && !it.photoRemoteUrl.isNullOrBlank() }
        downloadable.forEachIndexed { index, record ->
            onProgress?.invoke("下载照片 ${index + 1}/${downloadable.size}")
            val target = File(photoDirectory, "${record.id}.jpg")
            client.downloadPhoto(record.photoRemoteUrl!!, target)
            val position = remoteRecords.indexOfFirst { it.id == record.id }
            if (position >= 0) {
                remoteRecords[position] = record.copy(
                    photoPath = target.absolutePath,
                    photoSyncedAt = target.lastModified()
                )
            }
        }

        onProgress?.invoke("写入本地数据库…")
        database.withTransaction {
            recordDao.deleteAllRecords()
            recordDao.upsertRecords(remoteRecords)
        }
        cleanOrphanPhotos()
        settingsDao.upsertSettings(settings.copy(lastSyncAt = System.currentTimeMillis()))

        val backupNote = backup?.let { "；原数据已备份到 backups/${it.name}" }.orEmpty()
        "覆盖下载完成：${remoteRecords.size} 条记录、${downloadable.size} 张照片$backupNote"
    }

    /**
     * 增量合并同步（推荐日常使用）：
     * 以记录 id 为单位、`updatedAt` 新者胜，`deleted` 作为墓碑参与比较，
     * 两端的照片互相补齐。不会像覆盖下载那样抹掉另一边的新数据。
     */
    suspend fun mergeSync(
        newSettings: SettingsEntity,
        onProgress: ((String) -> Unit)? = null
    ): String = withContext(Dispatchers.IO) {
        val settings = validateSettings(newSettings)
        settingsDao.upsertSettings(settings)

        val client = WebDavClient(settings.username, settings.password)
        val baseUrl = settings.webdavUrl
        val basePath = settings.remotePath
        client.ensureDirectory(baseUrl, basePath)
        client.ensureDirectory(baseUrl, "$basePath/photos")
        val recordsJsonUrl = client.joinUrl(baseUrl, "$basePath/records.json")

        onProgress?.invoke("读取远端数据…")
        val remoteJson = client.getJson(recordsJsonUrl)
        val remoteRecords = remoteJson?.let(::parseRecords).orEmpty()
        val localRecords = recordDao.getAllRecords()

        val merged = LinkedHashMap<String, RecordEntity>()
        localRecords.forEach { merged[it.id] = it }
        var adoptedFromRemote = 0
        remoteRecords.forEach { remote ->
            val local = merged[remote.id]
            when {
                local == null -> {
                    merged[remote.id] = remote
                    adoptedFromRemote++
                }
                remote.updatedAt > local.updatedAt -> {
                    merged[remote.id] = remote.copy(
                        photoPath = local.photoPath,
                        photoRemoteUrl = remote.photoRemoteUrl ?: local.photoRemoteUrl,
                        photoSyncedAt = local.photoSyncedAt ?: remote.photoSyncedAt
                    )
                    adoptedFromRemote++
                }
            }
        }

        val active = merged.values.filter { !it.deleted }
        var uploaded = 0
        var downloaded = 0
        active.forEachIndexed { index, record ->
            val localFile = record.photoPath?.let(::File)
            when {
                localFile != null && localFile.isFile -> {
                    val needsUpload = record.photoRemoteUrl.isNullOrBlank() ||
                        (record.photoSyncedAt ?: 0L) < localFile.lastModified()
                    if (needsUpload) {
                        onProgress?.invoke("上传照片 ${index + 1}/${active.size}")
                        val url = remotePhotoUrl(client, baseUrl, basePath, record.id)
                        client.uploadPhoto(url, localFile)
                        merged[record.id] = record.copy(
                            photoRemoteUrl = url,
                            photoSyncedAt = System.currentTimeMillis()
                        )
                        uploaded++
                    }
                }
                !record.photoRemoteUrl.isNullOrBlank() -> {
                    onProgress?.invoke("下载照片 ${index + 1}/${active.size}")
                    val target = File(photoDirectory, "${record.id}.jpg")
                    client.downloadPhoto(record.photoRemoteUrl!!, target)
                    merged[record.id] = record.copy(
                        photoPath = target.absolutePath,
                        photoSyncedAt = target.lastModified()
                    )
                    downloaded++
                }
            }
        }

        val finalRecords = merged.values.toList()
        onProgress?.invoke("写入本地数据库…")
        database.withTransaction {
            recordDao.deleteAllRecords()
            recordDao.upsertRecords(finalRecords)
        }

        onProgress?.invoke("上传合并结果…")
        client.putJson(recordsJsonUrl, serializeRecords(finalRecords))

        onProgress?.invoke("清理两端无用照片…")
        runCatching { cleanRemoteOrphanPhotos(client, baseUrl, basePath, finalRecords) }
        cleanOrphanPhotos()

        settingsDao.upsertSettings(settings.copy(lastSyncAt = System.currentTimeMillis()))

        val visible = finalRecords.count { !it.deleted }
        "合并同步完成：共 $visible 条记录；远端补入 $adoptedFromRemote 条；上传 $uploaded 张、下载 $downloaded 张照片"
    }

    // ---------------------------------------------------------------- 导出

    /** 打包「记录 JSON + 全部照片」到给定输出流（配合 SAF 由用户选保存位置）。 */
    suspend fun exportTo(target: OutputStream): ExportSummary = withContext(Dispatchers.IO) {
        val records = recordDao.getVisibleRecords()
        var photoCount = 0

        ZipOutputStream(BufferedOutputStream(target)).use { zip ->
            zip.putNextEntry(ZipEntry("laxiang-records.json"))
            zip.write(serializeRecords(records).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            records.forEach { record ->
                val file = record.photoPath?.let(::File)
                if (file != null && file.isFile) {
                    zip.putNextEntry(ZipEntry("photos/${record.id}.jpg"))
                    file.inputStream().use { input -> input.copyTo(zip) }
                    zip.closeEntry()
                    photoCount++
                }
            }

            zip.putNextEntry(ZipEntry("README.txt"))
            zip.write(exportReadme(records.size, photoCount).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        ExportSummary(records.size, photoCount)
    }

    private fun exportReadme(recordCount: Int, photoCount: Int): String = buildString {
        appendLine("拉了么 数据导出")
        appendLine("导出时间：${LocalDateTime.now()}")
        appendLine("记录条数：$recordCount")
        appendLine("照片张数：$photoCount")
        appendLine()
        appendLine("laxiang-records.json 是全部记录，photos/ 是对应照片（文件名就是记录 id）。")
        appendLine("这份导出是纯本机备份，不依赖 WebDAV，用文本编辑器就能看懂。")
    }

    // ---------------------------------------------------------------- 内部工具

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
        "$basePath/photos/${Uri.encode("${recordId}.jpg", "")}"
    )

    /** 云端 photos 目录里，凡是不对应任何有效记录的都删掉，免得只增不减。 */
    private fun cleanRemoteOrphanPhotos(
        client: WebDavClient,
        baseUrl: String,
        basePath: String,
        records: List<RecordEntity>
    ) {
        val keep = records.filter { !it.deleted }.map { "${it.id}.jpg" }.toSet()
        val photosUrl = client.joinUrl(baseUrl, "$basePath/photos")
        val remoteNames = runCatching { client.listFileNames(photosUrl) }.getOrDefault(emptyList())
        remoteNames
            .filter { it.endsWith(".jpg", ignoreCase = true) && it !in keep }
            .forEach { name ->
                runCatching {
                    client.deleteFile(client.joinUrl(photosUrl, "/${Uri.encode(name, "")}"))
                }
            }
    }

    private fun parseRecords(json: String): List<RecordEntity> {
        val array = runCatching { JSONArray(json) }.getOrNull() ?: return emptyList()
        val records = mutableListOf<RecordEntity>()
        for (index in 0 until array.length()) {
            val obj = array.optJSONObject(index) ?: continue
            val id = obj.optString("id")
            if (id.isBlank()) continue
            records += RecordEntity(
                id = id,
                occurredAt = obj.optLong("occurredAt", System.currentTimeMillis()),
                durationSeconds = obj.optInt("durationSeconds", 0).coerceAtLeast(0),
                // 远端数据可能被改坏过，钳到合法区间，避免 UI 里 first{} 抛异常
                bristolType = obj.optInt("bristolType", 4).coerceIn(1, 7),
                stoolColor = obj.optString("color").ifBlank { null },
                stoolAmount = obj.optString("amount").ifBlank { null },
                stoolSmell = obj.optString("smell").ifBlank { null },
                bowelFeelings = obj.optString("feelings").ifBlank { null },
                bodyStatuses = obj.optString("bodyStatuses").ifBlank { null },
                notes = obj.optString("notes"),
                photoPath = null,
                photoRemoteUrl = obj.optString("photoRemoteUrl").ifBlank { null },
                photoSyncedAt = if (obj.has("photoSyncedAt") && !obj.isNull("photoSyncedAt")) {
                    obj.optLong("photoSyncedAt").takeIf { it > 0L }
                } else {
                    null
                },
                deleted = obj.optBoolean("deleted", false),
                updatedAt = obj.optLong("updatedAt", 0L)
            )
        }
        return records
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
                    .put("color", record.stoolColor ?: "")
                    .put("amount", record.stoolAmount ?: "")
                    .put("smell", record.stoolSmell ?: "")
                    .put("feelings", record.bowelFeelings ?: "")
                    .put("bodyStatuses", record.bodyStatuses ?: "")
                    .put("notes", record.notes)
                    .put("photoRemoteUrl", record.photoRemoteUrl ?: "")
                    .put("photoSyncedAt", record.photoSyncedAt ?: JSONObject.NULL)
                    .put("deleted", record.deleted)
                    .put("updatedAt", record.updatedAt)
            )
        }
        return array.toString()
    }

    private companion object {
        const val KEEP_BACKUPS = 5
        val BACKUP_STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
    }
}
