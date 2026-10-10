package com.laxiang.app.sync

import android.net.Uri
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class WebDavClient(
    private val username: String,
    private val password: String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(180, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    fun normalizeBaseUrl(url: String): String {
        val value = url.trim().trimEnd('/')
        return if (value.startsWith("http://", true) || value.startsWith("https://", true)) {
            value
        } else {
            "https://$value"
        }
    }

    fun joinUrl(baseUrl: String, path: String): String {
        val normalizedBase = normalizeBaseUrl(baseUrl)
        val normalizedPath = if (path.startsWith("/")) path else "/$path"
        return normalizedBase + normalizedPath
    }

    fun ensureDirectory(baseUrl: String, remotePath: String) {
        val parts = remotePath.split('/').filter { it.isNotBlank() }
        var currentPath = ""
        parts.forEach { part ->
            currentPath += "/" + Uri.encode(part, "")
            val response = execute("MKCOL", joinUrl(baseUrl, currentPath))
            response.use {
                val code = it.code
                if (code !in 200..299 && code != 301 && code != 405) {
                    throw IOException("创建目录失败：HTTP $code")
                }
            }
        }
    }

    fun getJson(url: String): String? {
        val response = execute("GET", url)
        response.use {
            return when {
                it.code == 404 -> null
                it.isSuccessful -> it.body?.string()
                else -> throw IOException("读取远程数据失败：HTTP ${it.code}")
            }
        }
    }

    fun putJson(url: String, json: String) {
        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
        val response = execute("PUT", url, body)
        response.use {
            if (!it.isSuccessful) {
                throw IOException("写入远程数据失败：HTTP ${it.code}")
            }
        }
    }

    fun uploadPhoto(url: String, file: File) {
        val body = file.asRequestBody("image/jpeg".toMediaType())
        val response = execute("PUT", url, body)
        response.use {
            if (!it.isSuccessful) {
                throw IOException("照片上传失败：HTTP ${it.code}")
            }
        }
    }

    fun downloadPhoto(url: String, target: File) {
        val response = execute("GET", url)
        response.use {
            if (!it.isSuccessful) {
                throw IOException("照片下载失败：HTTP ${it.code}")
            }
            target.parentFile?.mkdirs()
            val body = it.body ?: throw IOException("远程照片为空")
            target.outputStream().use { output -> body.byteStream().copyTo(output) }
        }
    }

    /** 列出目录下的文件名（PROPFIND Depth:1，只取路径最后一段）。服务器不支持时返回空列表。 */
    fun listFileNames(directoryUrl: String): List<String> {
        val response = execute("PROPFIND", directoryUrl, headers = mapOf("Depth" to "1"))
        response.use {
            if (!it.isSuccessful) return emptyList()
            val body = it.body?.string().orEmpty()
            return HREF_PATTERN.findAll(body)
                .map { match -> match.groupValues[1] }
                .map { href -> Uri.decode(href.trim().trimEnd('/').substringAfterLast('/')) }
                .filter { name -> name.isNotBlank() }
                .toList()
        }
    }

    /** 删除远端文件，404 视为已删除。 */
    fun deleteFile(url: String) {
        val response = execute("DELETE", url)
        response.use {
            if (it.code != 404 && !it.isSuccessful) {
                throw IOException("删除远程文件失败：HTTP ${it.code}")
            }
        }
    }

    fun testConnection(baseUrl: String, remotePath: String): Boolean {
        val response = execute("PROPFIND", joinUrl(baseUrl, remotePath), headers = mapOf("Depth" to "0"))
        response.use {
            // 连得通、服务器有响应即可；401/403 属于认证问题，判失败
            return it.code in 200..299 || it.code == 404 || it.code == 405 || it.code == 301
        }
    }

    private fun execute(
        method: String,
        url: String,
        body: RequestBody? = null,
        headers: Map<String, String> = emptyMap()
    ) = client.newCall(
        Request.Builder()
            .url(url)
            .method(method, body)
            .apply {
                if (username.isNotBlank()) {
                    header("Authorization", Credentials.basic(username, password))
                }
                headers.forEach { (name, value) -> header(name, value) }
            }
            .build()
    ).execute()

    private companion object {
        val HREF_PATTERN = Regex("<[^>]*href[^>]*>([^<]+)</[^>]*href>", RegexOption.IGNORE_CASE)
    }
}
