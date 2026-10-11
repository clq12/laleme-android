package com.laxiang.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.laxiang.app.ui.common.LxIconBook
import com.laxiang.app.ui.common.LxIconHome
import com.laxiang.app.ui.common.LxIconSettings
import com.laxiang.app.ui.common.LxIconStats
import com.laxiang.app.ui.editor.CameraScreen
import com.laxiang.app.ui.editor.EditorScreen
import com.laxiang.app.ui.home.HomeScreen
import com.laxiang.app.ui.knowledge.KnowledgeScreen
import com.laxiang.app.ui.settings.SettingsScreen
import com.laxiang.app.ui.stats.StatsScreen
import com.laxiang.app.ui.theme.LaxiangTheme
import com.laxiang.app.ui.theme.LxSizes

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            LaxiangTheme {
                LaxiangApp()
            }
        }
    }
}

@Composable
private fun navIconTint(selected: Boolean) =
    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

/**
 * 底部导航的一栏。
 *
 * 关于图标尺寸、居中、以及「图标下方留白过多」的三次迭代结论（别再退回旧写法）：
 *
 * 1. M3 的 `NavigationBarItem` 对 icon 槽位是「原样测量 + 在整栏宽度内居中」
 *    （placeIcon: x = (containerWidth - iconWidth) / 2），而 label 走的是
 *    另一条带 `NavigationBarItemHorizontalPadding/2`（=4dp）水平内缩的路径。
 *    不过两者的 X 都基于**同一个** maxWidth，所以「图标相对文字偏移」
 *    只可能来自图形本身的几何中心不在 24 栅格的 (12,12)。
 *    → 已用 `_verify_icons.py` 逐个校验，四个图形中心都是 (12.00, 12.00)。
 *
 * 2. 尺寸必须走令牌、且**只在一处生效**。
 *    之前写成 `Box(Modifier.size(iconNav)) { LxIconXxx(size = iconNav) }`
 *    —— 外层 Box 与外层图标各自按 24dp 约束，等于把同一个尺寸声明了两遍；
 *    由于 icon 槽位是原样测量，这会让实际占位与 M3 期望的 24dp 光学框不一致，
 *    表现为「图标比文字大一号 / 位置不对」。
 *    现在只由内层 Canvas 决定尺寸，外层只负责居中，不再重复声明。
 *
 * 3. v2.5.3：用户反馈「图标下方还有很多空间」。
 *    根因不在图标，而在 **NavigationBar 容器高度 80dp** 与
 *    「图标 24 + 指示器 32 + 文字」的实际内容高度不匹配：
 *    M3 的 80dp 是按「图标 24 + 指示器 32 + 上下 padding」的默认规格定的，
 *    当 label 只有一行 12sp 中文时，内容自然居中后上下都会多出空白，
 *    视觉上就显得图标「吊」在高处、下方空旷。
 *    → 解决方式是把容器压到 64dp（M3 在 3-button/紧凑场景也认可的高度），
 *    并显式收紧 `NavigationBarItemHorizontalPadding`，让四个入口更紧凑。
 *    这样既消掉「下方空一截」，也没有把小字号标签挤掉。
 *
 * 说明：这里**没有**用 modifier.padding 去「推」图标 ——
 * 那样会让图标跑出指示器（activeIndicator）的居中关系，
 * 选中态的胶囊就会和图标错位，是更差的解法。
 */
private object NavBarTokens {
    /**
     * NavigationBar 的内容高度（不含系统导航条 inset）。
     *
     * M3 默认 80dp 是按「图标 24 + 指示器 32 + 标签 + 上下留白」定的，
     * 但那是按大屏/多语言场景取的保守值。本 App 四个入口都是两字中文，
     * 标签只有一行 12sp，80dp 下内容居中后上下各多出一截空白 ——
     * 这正是用户反馈「图标下方还有很多空间」的来源。
     * 压到 60dp 后，24dp 图标 + 32dp 指示器 + 17dp 标签仍能正常排布。
     *
     * 注意：不要再靠「给图标加 padding」去压空间 —— 那会让图标脱离
     * activeIndicator 的居中关系，选中态胶囊就会和图标错位。
     */
    val containerHeight = 60.dp
}

@Composable
private fun RowScope.LxNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: @Composable () -> Unit
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            // 只做居中对齐，尺寸交给图标自己（统一 24dp 令牌），避免二次约束
            Box(contentAlignment = Alignment.Center) { icon() }
        },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        },
        colors = NavigationBarItemDefaults.colors(
            // 图形颜色由 navIconTint 控制，这里把文字色对齐，
            // 避免出现「图标一个色、标签另一个色」的割裂感。
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            indicatorColor = MaterialTheme.colorScheme.primaryContainer
        ),
        // 四个入口始终显示标签：图标语义不够自解释时，标签是唯一兜底
        alwaysShowLabel = true
    )
}

@Composable
fun LaxiangApp() {
    val context = LocalContext.current
    val application = context.applicationContext as LaxiangApplication
    val viewModel: LaxiangViewModel = viewModel(
        factory = viewModelFactory {
            initializer { LaxiangViewModel(application.applicationContext, application.repository) }
        }
    )
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val records by viewModel.records.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val timerStart by viewModel.timerStart.collectAsStateWithLifecycle()
    val editorState by viewModel.editorState.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()
    val syncProgress by viewModel.syncProgress.collectAsStateWithLifecycle()
    val syncRunning by viewModel.syncRunning.collectAsStateWithLifecycle()
    val deletedRecord by viewModel.deletedRecord.collectAsStateWithLifecycle()
    val photoUsage by viewModel.photoUsage.collectAsStateWithLifecycle()
    val backupInfo by viewModel.backupInfo.collectAsStateWithLifecycle()
    val localRecordCount by viewModel.localRecordCount.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    LaunchedEffect(syncMessage) {
        val message = syncMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.dismissSyncMessage()
    }

    // 删除不再是「点了就没」——给一次撤销机会
    LaunchedEffect(deletedRecord) {
        val record = deletedRecord ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = "已删除 1 条记录",
            actionLabel = "撤销",
            duration = SnackbarDuration.Long
        )
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.undoDelete()
        } else {
            viewModel.consumeDeletedRecord()
        }
    }

    Scaffold(
        // ── 系统栏避让（v2.5.3 修复：状态栏遮挡内容）─────────────
        // App 走 enableEdgeToEdge，内容铺满整屏。但原先这里写的是
        //   contentWindowInsets = WindowInsets(0, 0, 0, 0)
        // —— 直接把 inset 全清零了，等于禁止内容避让状态栏，
        // 于是首页头部（App 图标 + 标题）被状态栏压住。
        //
        // 现在改为按 M3 的约定分开处理：
        //   顶部：交给 contentWindowInsets（无 topBar 时它整段给 body 当上内边距）
        //         → 滚动内容的初始位置从状态栏下方开始，静止时不会被遮住。
        //   底部：置 0，因为 bottomBar（NavigationBar）自己会消费导航条 inset，
        //         若这里再给一次就会「导航栏内部留白 + body 再让一次」双重叠加。
        //
        // 这里用 WindowInsets.safeDrawing.only(WindowInsetsSides.Top) 而不是
        // statusBars：safeDrawing 已经帮我们排除掉「被刘海/挖孔/摄像头区域」
        // 的部分，比裸 statusBars 更安全；只取 Top 又不会碰底部。
        contentWindowInsets = WindowInsets.safeDrawing
            .only(WindowInsetsSides.Top),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.height(NavBarTokens.containerHeight),
                // M3 在这里会自己叠加 navigationBars inset —— 不改它，
                // 底部手势条/虚拟键的避让由 NavigationBar 负责，与 Scaffold 无重复。
                tonalElevation = 0.dp
            ) {
                LxNavItem(
                    selected = currentRoute == "home",
                    onClick = { navController.navigate("home") { launchSingleTop = true } },
                    label = "首页"
                ) { LxIconHome(tint = navIconTint(currentRoute == "home"), size = LxSizes.iconNav) }

                LxNavItem(
                    selected = currentRoute == "stats",
                    onClick = { navController.navigate("stats") { launchSingleTop = true } },
                    label = "统计"
                ) { LxIconStats(tint = navIconTint(currentRoute == "stats"), size = LxSizes.iconNav) }

                LxNavItem(
                    selected = currentRoute == "knowledge",
                    onClick = { navController.navigate("knowledge") { launchSingleTop = true } },
                    label = "知识"
                ) { LxIconBook(tint = navIconTint(currentRoute == "knowledge"), size = LxSizes.iconNav) }

                LxNavItem(
                    selected = currentRoute == "settings",
                    onClick = { navController.navigate("settings") { launchSingleTop = true } },
                    label = "设置"
                ) { LxIconSettings(tint = navIconTint(currentRoute == "settings"), size = LxSizes.iconNav) }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(
                    records = records,
                    timerStart = timerStart,
                    onStartTimer = viewModel::startTimer,
                    onStopTimer = {
                        viewModel.stopTimerAndOpenEditor()
                        navController.navigate("editor")
                    },
                    onEditRecord = { record ->
                        viewModel.beginEdit(record)
                        navController.navigate("editor")
                    },
                    onDeleteRecord = viewModel::deleteRecord
                )
            }
            composable("stats") {
                StatsScreen(records = records)
            }
            composable("knowledge") {
                KnowledgeScreen()
            }
            composable("settings") {
                SettingsScreen(
                    settings = settings,
                    syncProgress = syncProgress,
                    syncRunning = syncRunning,
                    photoUsage = photoUsage,
                    backupInfo = backupInfo,
                    localRecordCount = localRecordCount,
                    onSaveSettings = viewModel::saveSettings,
                    onMergeSync = viewModel::mergeSync,
                    onOverwriteUpload = viewModel::overwriteUpload,
                    onOverwriteDownload = viewModel::overwriteDownload,
                    onTestConnection = viewModel::testConnection,
                    onCleanOrphanPhotos = viewModel::cleanOrphanPhotos,
                    onRefreshMaintenance = viewModel::refreshMaintenanceInfo,
                    onExportData = viewModel::exportData
                )
            }
            composable("editor") {
                EditorScreen(
                    editorState = editorState,
                    onUpdate = viewModel::updateEditor,
                    onSave = {
                        viewModel.saveEditor()
                        navController.popBackStack()
                    },
                    onDelete = viewModel::deleteEditor,
                    onCancel = viewModel::clearEditor,
                    onNavigateBack = { navController.popBackStack() },
                    onTakePhoto = { navController.navigate("camera") },
                    onPickPhoto = viewModel::pickPhoto
                )
            }
            composable("camera") {
                CameraScreen(
                    onPhotoCaptured = { photoPath ->
                        viewModel.setEditorPhoto(photoPath)
                        navController.popBackStack()
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
