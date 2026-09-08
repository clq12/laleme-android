package com.laxiang.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.laxiang.app.ui.editor.CameraScreen
import com.laxiang.app.ui.editor.EditorScreen
import com.laxiang.app.ui.home.HomeScreen
import com.laxiang.app.ui.knowledge.KnowledgeScreen
import com.laxiang.app.ui.settings.SettingsScreen
import com.laxiang.app.ui.stats.StatsScreen
import com.laxiang.app.ui.theme.LaxiangTheme

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
fun LaxiangApp() {
    val context = LocalContext.current
    val application = context.applicationContext as LaxiangApplication
    val viewModel: LaxiangViewModel = viewModel(
        factory = viewModelFactory {
            initializer { LaxiangViewModel(application.repository) }
        }
    )
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val records by viewModel.records.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val timerStart by viewModel.timerStart.collectAsStateWithLifecycle()
    val editorState by viewModel.editorState.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    LaunchedEffect(syncMessage) {
        if (syncMessage != null) {
            snackbarHostState.showSnackbar(syncMessage!!)
            viewModel.dismissSyncMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == "home",
                    onClick = { navController.navigate("home") { launchSingleTop = true } },
                    icon = { Text("🏠") },
                    label = { Text("首页") }
                )
                NavigationBarItem(
                    selected = currentRoute == "stats",
                    onClick = { navController.navigate("stats") { launchSingleTop = true } },
                    icon = { Text("📊") },
                    label = { Text("统计") }
                )
                NavigationBarItem(
                    selected = currentRoute == "knowledge",
                    onClick = { navController.navigate("knowledge") { launchSingleTop = true } },
                    icon = { Text("📖") },
                    label = { Text("知识") }
                )
                NavigationBarItem(
                    selected = currentRoute == "settings",
                    onClick = { navController.navigate("settings") { launchSingleTop = true } },
                    icon = { Text("⚙️") },
                    label = { Text("设置") }
                )
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
                    onSaveSettings = viewModel::saveSettings,
                    onOverwriteUpload = viewModel::overwriteUpload,
                    onOverwriteDownload = viewModel::overwriteDownload,
                    onTestConnection = viewModel::testConnection
                )
            }
            composable("editor") {
                EditorScreen(
                    editorState = editorState,
                    onUpdate = viewModel::updateEditor,
                    onSave = viewModel::saveEditor,
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
