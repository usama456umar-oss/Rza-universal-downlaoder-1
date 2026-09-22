package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.components.AppLoadingSplashScreen
import com.example.ui.components.RzaHeader
import com.example.ui.downloader.DownloaderScreen
import com.example.ui.history.HistoryScreen
import com.example.ui.search.SearchScreen
import com.example.ui.theme.*
import com.example.ui.transcribe.TranscribeScreen
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                var isAppStarting by remember { mutableStateOf(true) }

                // Splash loading timer
                LaunchedEffect(Unit) {
                    delay(1600)
                    isAppStarting = false
                }

                // Request Notification Permission on Android 13+
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { /* Permission handled */ }
                )

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    RzaAppRoot(viewModel = viewModel)

                    AnimatedVisibility(
                        visible = isAppStarting,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        AppLoadingSplashScreen(
                            onFinished = { isAppStarting = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RzaAppRoot(viewModel: MainViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val historyList by viewModel.historyList.collectAsStateWithLifecycle()

    // Downloader states
    val urlInput by viewModel.urlInput.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val resultData by viewModel.resultData.collectAsStateWithLifecycle()
    val bulkItems by viewModel.bulkItems.collectAsStateWithLifecycle()
    val isBulkActive by viewModel.isBulkActive.collectAsStateWithLifecycle()
    val isBulkQueueRunning by viewModel.isBulkQueueRunning.collectAsStateWithLifecycle()
    val isLoadingNext by viewModel.isLoadingNext.collectAsStateWithLifecycle()
    val isTranscribing by viewModel.isTranscribing.collectAsStateWithLifecycle()
    val transcriptResult by viewModel.transcriptResult.collectAsStateWithLifecycle()

    // Search states
    val searchKeyword by viewModel.searchKeyword.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val searchError by viewModel.searchError.collectAsStateWithLifecycle()
    val isLoadingMore by viewModel.isLoadingMore.collectAsStateWithLifecycle()

    // Transcribe states
    val transcribeError by viewModel.transcribeError.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(RzaBackground),
        topBar = {
            RzaHeader(
                historyCount = historyList.size,
                onOpenHistory = { viewModel.setTab("history") },
                onNavigateHome = { viewModel.setTab("downloader") }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = RzaSurface.copy(alpha = 0.98f),
                tonalElevation = 8.dp,
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .border(1.dp, RzaBorderSubtle, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            ) {
                // Tab 1: Downloader
                NavigationBarItem(
                    selected = currentTab == "downloader",
                    onClick = { viewModel.setTab("downloader") },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == "downloader") Icons.Filled.Download else Icons.Outlined.Download,
                            contentDescription = "Downloader"
                        )
                    },
                    label = {
                        Text(
                            text = "Download",
                            fontSize = 11.sp,
                            fontWeight = if (currentTab == "downloader") FontWeight.ExtraBold else FontWeight.Medium
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = RzaPrimaryGlow,
                        indicatorColor = RzaPrimaryDark,
                        unselectedIconColor = RzaTextSecondary,
                        unselectedTextColor = RzaTextSecondary
                    )
                )

                // Tab 2: Search
                NavigationBarItem(
                    selected = currentTab == "search",
                    onClick = { viewModel.setTab("search") },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == "search") Icons.Filled.Search else Icons.Outlined.Search,
                            contentDescription = "Search"
                        )
                    },
                    label = {
                        Text(
                            text = "Search",
                            fontSize = 11.sp,
                            fontWeight = if (currentTab == "search") FontWeight.ExtraBold else FontWeight.Medium
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = RzaPrimaryGlow,
                        indicatorColor = RzaPrimaryDark,
                        unselectedIconColor = RzaTextSecondary,
                        unselectedTextColor = RzaTextSecondary
                    )
                )

                // Tab 3: AI Transcribe
                NavigationBarItem(
                    selected = currentTab == "transcribe",
                    onClick = { viewModel.setTab("transcribe") },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == "transcribe") Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                            contentDescription = "AI Transcribe"
                        )
                    },
                    label = {
                        Text(
                            text = "AI Tools",
                            fontSize = 11.sp,
                            fontWeight = if (currentTab == "transcribe") FontWeight.ExtraBold else FontWeight.Medium
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = RzaPrimaryGlow,
                        indicatorColor = RzaPrimaryDark,
                        unselectedIconColor = RzaTextSecondary,
                        unselectedTextColor = RzaTextSecondary
                    )
                )

                // Tab 4: History
                NavigationBarItem(
                    selected = currentTab == "history",
                    onClick = { viewModel.setTab("history") },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (historyList.isNotEmpty()) {
                                    Badge(
                                        containerColor = RzaPrimaryDark,
                                        contentColor = Color.White
                                    ) {
                                        Text(
                                            text = if (historyList.size > 99) "99+" else historyList.size.toString(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (currentTab == "history") Icons.Filled.History else Icons.Outlined.History,
                                contentDescription = "History"
                            )
                        }
                    },
                    label = {
                        Text(
                            text = "History",
                            fontSize = 11.sp,
                            fontWeight = if (currentTab == "history") FontWeight.ExtraBold else FontWeight.Medium
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = RzaPrimaryGlow,
                        indicatorColor = RzaPrimaryDark,
                        unselectedIconColor = RzaTextSecondary,
                        unselectedTextColor = RzaTextSecondary
                    )
                )
            }
        },
        containerColor = RzaBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        listOf(RzaBackground, Color(0xFF090412), Color(0xFF05020B))
                    )
                )
        ) {
            Crossfade(targetState = currentTab, label = "TabSwitch") { tab ->
                when (tab) {
                    "downloader" -> {
                        DownloaderScreen(
                            urlInput = urlInput,
                            onUrlChange = viewModel::onUrlChange,
                            onSubmitSingle = viewModel::submitSingle,
                            onSubmitBatch = viewModel::submitBatch,
                            onClear = viewModel::clearDownloader,
                            isLoading = isLoading,
                            errorMessage = errorMessage,
                            resultData = resultData,
                            bulkItems = bulkItems,
                            isBulkActive = isBulkActive,
                            isBulkQueueRunning = isBulkQueueRunning,
                            onToggleBulkSelect = viewModel::toggleBulkSelect,
                            onSelectAllBulk = viewModel::selectAllBulk,
                            onSelectNoneBulk = viewModel::selectNoneBulk,
                            onStartBulkDownload = viewModel::startBulkDownload,
                            onRemoveBulkItem = viewModel::removeBulkItem,
                            onDownloadVideo = viewModel::downloadVideo,
                            onDownloadAudio = viewModel::downloadAudio,
                            onDownloadCover = viewModel::downloadCover,
                            onDownloadPhoto = viewModel::downloadPhoto,
                            onLoadNextVideo = viewModel::loadNextVideo,
                            isLoadingNext = isLoadingNext,
                            onTranscribe = viewModel::transcribeCurrentMedia,
                            isTranscribing = isTranscribing,
                            transcriptResult = transcriptResult,
                            onCloseTranscript = viewModel::closeTranscript,
                            onLoadManualMedia = viewModel::loadManualMedia
                        )
                    }

                    "search" -> {
                        SearchScreen(
                            keywordInput = searchKeyword,
                            onKeywordChange = viewModel::onKeywordChange,
                            onSearch = viewModel::search,
                            searchResults = searchResults,
                            isSearching = isSearching,
                            searchError = searchError,
                            onDownloadItem = viewModel::downloadSearchItem,
                            onSelectForDownloader = viewModel::selectSearchItemForDownloader,
                            onLoadMore = viewModel::loadMoreSearch,
                            isLoadingMore = isLoadingMore
                        )
                    }

                    "transcribe" -> {
                        TranscribeScreen(
                            onTranscribe = viewModel::runTranscription,
                            isTranscribing = isTranscribing,
                            transcriptResult = transcriptResult,
                            errorMessage = transcribeError,
                            onClearResult = viewModel::closeTranscript
                        )
                    }

                    "history" -> {
                        HistoryScreen(
                            historyList = historyList,
                            onDeleteHistoryItem = viewModel::deleteHistoryItem,
                            onClearAllHistory = viewModel::clearAllHistory,
                            onDownloadAgain = viewModel::redownloadHistory
                        )
                    }
                }
            }
        }
    }
}
