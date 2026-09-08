package com.laxiang.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.laxiang.app.data.SettingsEntity
import com.laxiang.app.model.formatDateTime

@Composable
fun SettingsScreen(
    settings: SettingsEntity?,
    onSaveSettings: (String, String, String, String) -> Unit,
    onOverwriteUpload: (String, String, String, String) -> Unit,
    onOverwriteDownload: (String, String, String, String) -> Unit,
    onTestConnection: () -> Unit
) {
    var webdavUrl by remember { mutableStateOf(settings?.webdavUrl.orEmpty()) }
    var username by remember { mutableStateOf(settings?.username.orEmpty()) }
    var password by remember { mutableStateOf(settings?.password.orEmpty()) }
    var remotePath by remember { mutableStateOf(settings?.remotePath ?: "/laxiang") }
    var showPassword by remember { mutableStateOf(false) }

    LaunchedEffect(settings) {
        settings?.let {
            webdavUrl = it.webdavUrl
            username = it.username
            password = it.password
            remotePath = it.remotePath
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "同步设置",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "本地数据始终可用，云端仅手动同步",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = webdavUrl,
                    onValueChange = { webdavUrl = it },
                    label = { Text("WebDAV 地址") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("用户名") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (showPassword) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        OutlinedButton(onClick = { showPassword = !showPassword }) {
                            Text(if (showPassword) "隐藏" else "显示")
                        }
                    }
                )
                OutlinedTextField(
                    value = remotePath,
                    onValueChange = { remotePath = it },
                    label = { Text("远程目录") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            onSaveSettings(webdavUrl, username, password, remotePath)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("保存配置")
                    }
                    OutlinedButton(
                        onClick = onTestConnection,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("测试连接")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            onOverwriteUpload(webdavUrl, username, password, remotePath)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("覆盖上传")
                    }
                    OutlinedButton(
                        onClick = {
                            onOverwriteDownload(webdavUrl, username, password, remotePath)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("覆盖下载")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "同步状态",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = settings?.lastSyncAt?.let { "上次同步：${formatDateTime(it)}" } ?: "尚未同步",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "覆盖上传会以本机记录和照片覆盖云端；覆盖下载会以远端 records.json 和照片覆盖本机。执行前请确认方向。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "作者：权",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}
