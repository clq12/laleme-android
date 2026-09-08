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

class WebDavClient(
    private val username: String,
    private val password: String
) {
    private val client = OkHttpClient()

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
            target.outputStream().use { output ->
                it.body?.byteStream()?.copyTo(output) ?: throw IOException("远程照片为空")
            }
        }
    }

    fun testConnection(baseUrl: String, remotePath: String): Boolean {
        val response = execute("PROPFIND", joinUrl(baseUrl, remotePath), headers = mapOf("Depth" to "0"))
        response.use {
            return it.code == 200 || it.code == 207 || it.code == 404
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
}
