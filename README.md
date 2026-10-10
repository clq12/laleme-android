# 拉了么 · 原生 Android App

Kotlin + Jetpack Compose 原生应用，目标 Android 16 / API 36，最低支持 Android 7.0 / API 24。

## 功能

- 布里斯托 1-7 型记录
- 大便颜色、分量、气味、排便感受记录
- 当前状态 / 诱因记录（着凉、吃了重口、吃坏肚子、吃了不新鲜、饮酒、熬夜、压力大、乳制品等，可多选，**选填**）
- 马桶计时器：开始 / 结束后自动计算时长
- 日期、时间、时长、备注、照片
- 保存新记录或编辑记录后自动返回首页
- CameraX 拍照、系统相册选图（入库前自动压缩到长边 1600 / JPEG 85）
- 历史记录支持搜索 + 筛选（全部 / 理想型 / 异常型 / 带照片 / 有备注 / 有状态），每页 30 条分页加载
- 统计页可选时间范围：近 14 天（默认）/ 全部历史 / 自定义起止，所有分布与趋势都按所选范围计算；月历点某天即把范围切到那天
- 今日概览、14 天趋势、类型分布、颜色分布、分量分布、气味分布、状态分布、健康率、月历
- 知识栏：布里斯托类型、颜色原因、气味原因、状态诱因
- Room 本地存储，断网可继续记录
- 删除带撤销（底部提示条），不再一点就没
- 照片回收：查看本机占用、清理无主照片（本地与云端都会清）
- 数据导出：全部记录 + 照片打包成 zip，存到任意位置，不依赖 WebDAV
- WebDAV 同步：合并同步（推荐）/ 覆盖上传 / 覆盖下载，均带实时进度

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

## 自动构建与发布（GitHub Actions）

推送到 `main` 分支后会自动构建 release APK 并发布到 GitHub Releases；也可以在仓库的 Actions 页面手动触发一次。

- Release 标题：`拉了么 v<versionName>`，版本号取自 `app/build.gradle` 的 `versionName`
- Release 标签：`v<versionName>`
- APK 文件名：`拉了么-v<versionName>.apk`
- 同一版本号重复推送会**覆盖**同一个 Release 里的 APK；改完 `versionName` 再推，才会生成新的 Release
- 构建产物同时能在 Actions 页面该次运行的 Artifacts 里下载

CI 使用仓库 Secrets 里的正式证书签名，四个 Secret：

| Secret | 说明 |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | `laxiang-release.keystore` 的 base64（`base64 -w 0 laxiang-release.keystore`） |
| `ANDROID_KEYSTORE_PASSWORD` | keystore 口令 |
| `ANDROID_KEY_ALIAS` | 别名（`laxiang`） |
| `ANDROID_KEY_PASSWORD` | 别名口令 |

没配 Secrets 时 workflow 会退回 debug 签名，仍能出包，但签名与正式包不同，无法覆盖升级。

## 环境要求（不用 Android Studio 也能构建）

- JDK 17（本机路径：`C:\Android\jdk-17`）
- Android SDK：`platform-tools`、`platforms;android-36`、`build-tools;36.0.0`（本机路径：`C:\Android\Sdk`）
- Gradle 8.14.3 由 wrapper 自动下载，无需手工安装
- `build-local.ps1` 会自动探测 `JAVA_HOME` / `ANDROID_HOME`；换机后如果装在别处，先设置这两个环境变量即可
- `local.properties` 里的 `sdk.dir` 是本机路径，不入库，换机后重新生成或直接依赖 `ANDROID_HOME`

## WebDAV 配置

1. 打开 App 的「设置」页
2. 填写 WebDAV 地址、用户名、密码、远程目录
3. 点击「测试连接」
4. 日常备份与多端同步点「合并同步」：按记录逐条比对更新时间，两边都保留、谁也不覆盖谁，照片双向补齐
5. 「覆盖上传」「覆盖下载」是单向整体替换，只能二选一，点击后还需再确认一次；覆盖下载执行前会自动把本机数据备份到应用私有目录（保留最近 5 份）

示例：

```text
服务器地址：https://cloud.example.com/remote.php/dav/files/username
远程目录：/laxiang
```

数据会保存为 `records.json`，照片会保存到 `photos/` 目录。同步时还会清理云端 photos 里没有对应记录的照片，避免只增不减。

## 说明

- 本版本只在手动触发时访问网络，不做后台自动同步
- 合并同步：以记录 id 为单位、`updatedAt` 新者胜；删除会留下墓碑标记，因此「在一端删除」能正确传播到另一端
- 覆盖上传会用本机数据整体替换云端；覆盖下载会用云端数据整体替换本机
- 覆盖下载前会自动快照本机记录与照片到 `files/backups/<时间戳>/`，保留最近 5 份
- 删除记录时不立刻删照片，方便误删后撤销；确认不需要了在设置页点「清理无主照片」回收
- 数据库版本 3：新增 `bodyStatuses` 字段（当前状态/诱因，逗号分隔的 id），升级时自动迁移，老记录显示为「未记录」
- 「当前状态」是**选填**项：一个都不勾也能直接保存，此时按「未记录」统计
- 「无明显诱因」与其他状态互斥，不会出现自相矛盾的组合
- 支持自签名或局域网 HTTP WebDAV
- 密码保存在本机数据库中，未加密
