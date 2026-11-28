package com.example

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.MediaAccessState
import com.example.player.PlaybackEngine
import com.example.player.PlaybackService
import com.example.ui.browser.ActivationScreen
import com.example.ui.browser.FolderDetailScreen
import com.example.ui.browser.LibraryScreen
import com.example.ui.browser.LibraryViewModel
import com.example.ui.browser.PinPadDialog
import com.example.ui.browser.PrivateVaultScreen
import com.example.ui.browser.SettingsScreen
import com.example.ui.player.PlayerScreen
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.VeloPlayTheme
import kotlinx.coroutines.flow.MutableStateFlow

object Routes {
    const val LIBRARY = "library"
    const val FOLDER_DETAIL = "folder/{bucketId}"
    const val SETTINGS = "settings"
    const val PRIVATE_VAULT = "private_vault"
    const val PLAYER = "player"

    fun folderDetail(bucketId: String): String = "folder/${Uri.encode(bucketId)}"
}

class MainActivity : ComponentActivity() {

    private val libraryViewModel: LibraryViewModel by viewModels()
    private var pendingViewUri by mutableStateOf<Uri?>(null)
    private var pendingOpenPlayerFromNotification by mutableStateOf(false)
    private val isInPipModeFlow = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        handleIncomingIntent(intent)

        setContent {
            val isInPipMode by isInPipModeFlow.collectAsStateWithLifecycle()
            val themeState by libraryViewModel.uiState.collectAsStateWithLifecycle()
            VeloPlayTheme(theme = themeState.appThemePreset.config) {
                AltPlayerApp(
                    libraryViewModel = libraryViewModel,
                    pendingViewUri = pendingViewUri,
                    pendingOpenPlayerFromNotification = pendingOpenPlayerFromNotification,
                    isInPipMode = isInPipMode,
                    onConsumePendingUri = { pendingViewUri = null },
                    onConsumeOpenPlayer = { pendingOpenPlayerFromNotification = false }
                )
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipModeFlow.value = isInPictureInPictureMode
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                pendingViewUri = uri
            }
        }
        if (intent?.getBooleanExtra(PlaybackService.EXTRA_OPEN_PLAYER, false) == true) {
            pendingOpenPlayerFromNotification = true
        }
    }
}

@Composable
fun AltPlayerApp(
    libraryViewModel: LibraryViewModel,
    pendingViewUri: Uri?,
    pendingOpenPlayerFromNotification: Boolean,
    isInPipMode: Boolean,
    onConsumePendingUri: () -> Unit,
    onConsumeOpenPlayer: () -> Unit
) {
    val navController = rememberNavController()
    val uiState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val playbackState by PlaybackEngine.sessionState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val view = LocalView.current
    val activity = remember(context) { context.findActivity() }

    var showPinDialog by remember { mutableStateOf(false) }
    var forceCreatePin by remember { mutableStateOf(false) }
    var navigateToVaultOnUnlock by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && uiState.accessState == MediaAccessState.DENIED) {
                libraryViewModel.refreshPermissionAndScan(false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val gateVisible = uiState.isPrefsLoaded && uiState.proLicense.shouldShowActivationGate()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Keep system Status Bar & Navigation Bar visible on every non-player screen
    LaunchedEffect(currentRoute, gateVisible) {
        if (gateVisible || currentRoute != Routes.PLAYER) {
            val window = activity?.window ?: return@LaunchedEffect
            WindowCompat.getInsetsController(window, view)
                .show(WindowInsetsCompat.Type.systemBars())
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    LaunchedEffect(pendingViewUri, uiState.isPrefsLoaded, gateVisible) {
        if (!uiState.isPrefsLoaded || gateVisible) return@LaunchedEffect
        val uri = pendingViewUri ?: return@LaunchedEffect
        onConsumePendingUri()
        libraryViewModel.openExternalVideo(uri) {
            navController.navigate(Routes.PLAYER) {
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(pendingOpenPlayerFromNotification, uiState.isPrefsLoaded, gateVisible, playbackState.hasActiveSession) {
        if (!uiState.isPrefsLoaded || gateVisible || !pendingOpenPlayerFromNotification) return@LaunchedEffect
        onConsumeOpenPlayer()
        if (playbackState.hasActiveSession) {
            navController.navigate(Routes.PLAYER) {
                launchSingleTop = true
            }
        }
    }

    if (!uiState.isPrefsLoaded) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBlack)
        )
        return
    }

    if (gateVisible) {
        ActivationScreen(
            proLicense = uiState.proLicense,
            onActivateCode = libraryViewModel::activateProWithCode,
            onContinueTrial = libraryViewModel::startThreeDayTrial
        )
        return
    }

    NavHost(
        navController = navController,
        startDestination = Routes.LIBRARY,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() }
    ) {
        composable(Routes.LIBRARY) {
            LibraryScreen(
                uiState = uiState,
                onSearchQueryChange = libraryViewModel::onSearchQueryChange,
                onSelectTab = libraryViewModel::onSelectTab,
                onSelectSort = libraryViewModel::onSelectSort,
                onToggleGridView = libraryViewModel::toggleGridView,
                onRefreshScan = libraryViewModel::refreshPermissionAndScan,
                onOpenFolder = { folder ->
                    navController.navigate(Routes.folderDetail(folder.bucketId))
                },
                onOpenSettings = {
                    navController.navigate(Routes.SETTINGS)
                },
                onOpenPrivateVault = {
                    forceCreatePin = !uiState.hasPrivatePin
                    navigateToVaultOnUnlock = true
                    showPinDialog = true
                },
                onPlayVideo = { video, playlist ->
                    libraryViewModel.startPlayback(video, playlist)
                    navController.navigate(Routes.PLAYER)
                },
                onOpenActivePlayer = {
                    if (PlaybackEngine.sessionState.value.hasActiveSession) {
                        navController.navigate(Routes.PLAYER) {
                            launchSingleTop = true
                        }
                    }
                },
                onOpenExternalUri = { uri ->
                    libraryViewModel.openExternalVideo(uri) {
                        navController.navigate(Routes.PLAYER)
                    }
                },
                onSetFolderHidden = libraryViewModel::setFolderHidden,
                onInspectMediaInfo = libraryViewModel::inspectMediaInfo,
                onDismissMediaInfo = libraryViewModel::dismissMediaInfo,
                createDeleteIntentSender = libraryViewModel::createDeleteIntentSender,
                createWriteIntentSender = libraryViewModel::createWriteIntentSender,
                onDeleteVideosLegacy = libraryViewModel::deleteVideosLegacy,
                onPerformRename = libraryViewModel::performRename,
                onMoveVideoToVault = libraryViewModel::moveVideoToVault,
                getShareableUri = libraryViewModel::getShareableUri,
                onClearStatusMessage = libraryViewModel::clearStatusMessage
            )
        }

        composable(
            route = Routes.FOLDER_DETAIL,
            arguments = listOf(navArgument("bucketId") { type = NavType.StringType })
        ) { backStackEntry ->
            val rawBucketId = backStackEntry.arguments?.getString("bucketId").orEmpty()
            val bucketId = Uri.decode(rawBucketId)
            val folder = libraryViewModel.getFolderById(bucketId)

            FolderDetailScreen(
                folder = folder,
                uiState = uiState,
                onSearchQueryChange = libraryViewModel::onSearchQueryChange,
                onSelectSort = libraryViewModel::onSelectSort,
                onToggleGridView = libraryViewModel::toggleGridView,
                onRefreshScan = libraryViewModel::refreshPermissionAndScan,
                onBack = { navController.popBackStack() },
                onPlayVideo = { video, playlist ->
                    libraryViewModel.startPlayback(video, playlist)
                    navController.navigate(Routes.PLAYER)
                },
                onInspectMediaInfo = libraryViewModel::inspectMediaInfo,
                onDismissMediaInfo = libraryViewModel::dismissMediaInfo,
                createDeleteIntentSender = libraryViewModel::createDeleteIntentSender,
                createWriteIntentSender = libraryViewModel::createWriteIntentSender,
                onDeleteVideosLegacy = libraryViewModel::deleteVideosLegacy,
                onPerformRename = libraryViewModel::performRename,
                onMoveVideoToVault = libraryViewModel::moveVideoToVault,
                getShareableUri = libraryViewModel::getShareableUri,
                onClearStatusMessage = libraryViewModel::clearStatusMessage
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                uiState = uiState,
                onSelectResumeBehavior = libraryViewModel::setResumeBehavior,
                onSelectDefaultOrientation = libraryViewModel::setDefaultOrientation,
                onToggleAutoLoadSrt = libraryViewModel::setAutoLoadSrt,
                onToggleShowHiddenFolders = libraryViewModel::setShowHiddenFolders,
                onOpenChangePin = {
                    forceCreatePin = true
                    navigateToVaultOnUnlock = false
                    showPinDialog = true
                },
                onClearPin = libraryViewModel::clearPrivatePin,
                onActivateProCode = libraryViewModel::activateProWithCode,
                onGenerateBuyerCode = libraryViewModel::generateOfflineBuyerCode,
                onSelectAppTheme = libraryViewModel::setAppThemePreset,
                onCheckForUpdates = { libraryViewModel.checkForUpdates() },
                onDownloadAndInstallUpdate = libraryViewModel::downloadAndInstallUpdate,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PRIVATE_VAULT) {
            PrivateVaultScreen(
                uiState = uiState,
                onToggleGridView = libraryViewModel::toggleGridView,
                onImportUri = libraryViewModel::importDocumentToVault,
                onPlayVideo = { video, playlist ->
                    libraryViewModel.startPlayback(video, playlist)
                    navController.navigate(Routes.PLAYER)
                },
                onRenameVaultVideo = { video, newTitle ->
                    libraryViewModel.performRename(video, newTitle) { }
                },
                onDeleteVaultVideo = libraryViewModel::deleteVaultVideo,
                onInspectMediaInfo = libraryViewModel::inspectMediaInfo,
                onDismissMediaInfo = libraryViewModel::dismissMediaInfo,
                getShareableUri = libraryViewModel::getShareableUri,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PLAYER) {
            val effectiveSessionKey = if (uiState.activePlaylist.isEmpty() && playbackState.hasActiveSession) {
                PlaybackEngine.activeSessionKey
            } else {
                uiState.playerSessionKey
            }
            PlayerScreen(
                sessionKey = effectiveSessionKey,
                playlist = uiState.activePlaylist,
                initialIndex = uiState.activeVideoIndex,
                resumeBehavior = uiState.resumeBehavior,
                defaultOrientation = uiState.defaultOrientation,
                autoLoadSrt = uiState.autoLoadSrt,
                subtitleStyle = uiState.subtitleStyle,
                isInPipMode = isInPipMode,
                getResumePositionMs = libraryViewModel::getResumePositionMs,
                findMatchingSrtUri = libraryViewModel::findMatchingSrtUri,
                onUpdateSubtitleStyle = libraryViewModel::setSubtitleStyle,
                onSaveProgress = libraryViewModel::savePlaybackProgress,
                onBack = { navController.popBackStack() }
            )
        }
    }

    if (showPinDialog) {
        PinPadDialog(
            isCreatingNewPin = forceCreatePin,
            onVerifyPin = libraryViewModel::verifyPrivatePin,
            onCreatePin = libraryViewModel::setPrivatePin,
            onSuccess = {
                showPinDialog = false
                if (navigateToVaultOnUnlock) {
                    navController.navigate(Routes.PRIVATE_VAULT)
                }
            },
            onDismiss = { showPinDialog = false }
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
