# 拉了么 · 原生 Android App

Kotlin + Jetpack Compose 原生应用，目标 Android 16 / API 36，最低支持 Android 7.0 / API 24。

## 功能

- 布里斯托 1-7 型记录
- 马桶计时器：开始 / 结束后自动计算时长
- 日期、时间、时长、备注、照片
- CameraX 拍照、系统相册选图
- 今日概览、14 天趋势、类型分布、健康率、月历
- Room 本地存储，断网可继续记录
- WebDAV 手动覆盖上传 / 覆盖下载

## 技术栈

- Kotlin + Jetpack Compose + Material 3
- Room 数据库
- CameraX
- OkHttp
- Coil
- Navigation Compose

## 构建

```powershell
.\build-local.ps1
```

生成 debug APK：

```text
lale-debug.apk
```

生成正式签名 release APK：

```powershell
.\build-local.ps1 -Release
```

生成：

```text
lale-release.apk
```

签名文件不会提交到 Git。请在本地保留 `laxiang-release.keystore` 和 `signing.properties`，丢失后无法给同一应用发布可升级的正式版本。

## WebDAV 配置

1. 打开 App 的「设置」页
2. 填写 WebDAV 地址、用户名、密码、远程目录
3. 点击「测试连接」
4. 需要云端备份时点击「覆盖上传」；需要恢复云端数据时点击「覆盖下载」

示例：

```text
服务器地址：https://cloud.example.com/remote.php/dav/files/username
远程目录：/laxiang
```

数据会保存为 `records.json`，照片会保存到 `photos/` 目录。

## 说明

- 本版本仅手动覆盖上传 / 覆盖下载，不会后台自动访问网络
- 覆盖上传会用本机数据覆盖云端；覆盖下载会用云端数据覆盖本机
- 支持自签名或局域网 HTTP WebDAV
- 密码仅保存在本机
- 删除记录会生成同步墓碑，避免多端误恢复
