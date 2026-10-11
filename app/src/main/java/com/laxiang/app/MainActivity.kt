package com.laxiang.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
 * 为什么把 icon 包一层 Box：M3 的 `NavigationBarItem` 会把 icon 槽位
 * 原样测量后在整栏宽度内居中，而它自带的选中指示器（pill）宽度是 64dp。
 * 我们的 Canvas 图标如果直接铺满 24dp，视觉上会显得比文字「大一号」，
 * 且因为没有指示器留白而看起来偏左。这里显式给出 24dp 的槽位盒、
 * 让指示器和图标共用同一中心，四个栏位的大小/位置就完全一致了。
 */
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
            Box(
                modifier = Modifier.size(LxSizes.iconNav),
                contentAlignment = Alignment.Center
            ) { icon() }
        },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }
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
        // 底部导航：inset 交给 NavigationBar 自己处理，避免被系统导航条盖住
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
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
