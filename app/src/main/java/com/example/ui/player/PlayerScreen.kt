package com.example.ui.player

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.Icon as AndroidIcon
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.util.Rational
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.ContentFrame
import com.example.R
import com.example.data.DefaultOrientation
import com.example.data.ResumeBehavior
import com.example.data.VideoItem
import com.example.data.formatDuration
import com.example.player.AspectMode
import com.example.player.DecoderMode
import com.example.player.ExternalSubtitleTrack
import com.example.player.GestureFeedback
import com.example.player.PlaybackEngine
import com.example.player.PlayerMath
import com.example.player.SleepTimerOption
import com.example.player.SrtSubtitleParser
import com.example.player.SubtitleStyleConfig
import com.example.player.VideoScreenshotHelper
import com.example.ui.components.VideoThumbnailImage
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.DeepNavySurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GoogleSansFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletAccent
import com.example.ui.theme.VioletContainer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class DragMode {
    NONE,
    PINCH_ZOOM,
    HORIZONTAL_SEEK,
    BRIGHTNESS,
    VOLUME
}

private const val ACTION_PIP_CONTROL = "com.example.player.PIP_CONTROL"
private const val EXTRA_PIP_COMMAND = "pip_command"
private const val PIP_CMD_PLAY_PAUSE = 1
private const val PIP_CMD_REWIND = 2
private const val PIP_CMD_FORWARD = 3

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    sessionKey: Int,
    playlist: List<VideoItem>,
    initialIndex: Int,
    resumeBehavior: ResumeBehavior,
    defaultOrientation: DefaultOrientation,
    autoLoadSrt: Boolean,
    subtitleStyle: SubtitleStyleConfig,
    isInPipMode: Boolean,
    getResumePositionMs: (Uri) -> Long,
    findMatchingSrtUri: suspend (VideoItem) -> Uri?,
    onUpdateSubtitleStyle: (SubtitleStyleConfig) -> Unit,
    onSaveProgress: (Uri, Long, Long) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    val coroutineScope = rememberCoroutineScope()

    val playbackState by PlaybackEngine.sessionState.collectAsStateWithLifecycle()
    val exoPlayer by PlaybackEngine.exoPlayerFlow.collectAsStateWithLifecycle()

    val activePlaylist = playbackState.playlist.takeIf {
        playbackState.hasActiveSession && PlaybackEngine.activeSessionKey == sessionKey && it.isNotEmpty()
    } ?: playlist

    val activeIndex = if (playbackState.hasActiveSession && PlaybackEngine.activeSessionKey == sessionKey) {
        playbackState.currentIndex.coerceIn(0, (activePlaylist.size - 1).coerceAtLeast(0))
    } else {
        initialIndex.coerceIn(0, (activePlaylist.size - 1).coerceAtLeast(0))
    }

    val currentVideo = activePlaylist.getOrNull(activeIndex) ?: run {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableLongStateOf(0L) }

    var controlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var lockedHintVisible by remember { mutableStateOf(false) }
    var aspectMode by remember { mutableStateOf(AspectMode.FIT) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var quickToolsExpanded by remember { mutableStateOf(false) }

    val externalSubtitles = remember { mutableStateListOf<ExternalSubtitleTrack>() }
    var activeExternalSubId by remember { mutableStateOf<String?>(null) }
    var subtitlesDisabled by remember { mutableStateOf(false) }
    var subtitleOffsetMs by remember { mutableLongStateOf(0L) }

    var showTrackSheet by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showEqualizerSheet by remember { mutableStateOf(false) }
    var pendingAskResumeMs by remember { mutableStateOf<Long?>(null) }
    var askedNotificationOnce by remember { mutableStateOf(false) }

    var gestureFeedback by remember { mutableStateOf<GestureFeedback?>(null) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // Notification permission launcher for Android 13+ Media Playback Controls
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        PlaybackEngine.setBackgroundPlayEnabled(true)
        PlaybackEngine.showToastLabel(
            if (granted) "Background Play Enabled" else "Background Audio On (Notification Hidden)"
        )
    }

    // Request POST_NOTIFICATIONS unobtrusively once when opening an audio track on Android 13+
    LaunchedEffect(currentVideo.uri, currentVideo.isAudio) {
        if (currentVideo.isAudio &&
            !askedNotificationOnce &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            askedNotificationOnce = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Start or reattach MediaSessionService playback session
    LaunchedEffect(sessionKey) {
        PlaybackEngine.ensureServiceConnected(context)
        if (sessionKey != PlaybackEngine.activeSessionKey && playlist.isNotEmpty()) {
            val safeIdx = initialIndex.coerceIn(0, playlist.lastIndex)
            val target = playlist[safeIdx]
            val resumeMs = getResumePositionMs(target.uri)
            val hasValidResume = resumeMs > 3_000L &&
                (target.durationMs <= 0L || resumeMs < target.durationMs - 5_000L)

            val startPos = when {
                !hasValidResume -> 0L
                resumeBehavior == ResumeBehavior.ALWAYS_RESUME -> resumeMs
                resumeBehavior == ResumeBehavior.START_OVER -> 0L
                else -> {
                    pendingAskResumeMs = resumeMs
                    0L
                }
            }
            val autoPlay = pendingAskResumeMs == null
            PlaybackEngine.requestPlaylistPlayback(
                context = context,
                sessionKey = sessionKey,
                playlist = playlist,
                startIndex = safeIdx,
                startPositionMs = startPos,
                autoPlay = autoPlay
            )
        }
    }

    // Reset per-item subtitle state & auto-load matching .srt when track changes
    LaunchedEffect(currentVideo.uri, autoLoadSrt) {
        activeExternalSubId = null
        subtitlesDisabled = false
        subtitleOffsetMs = 0L

        if (autoLoadSrt && !currentVideo.isAudio) {
            val matchingSrt = findMatchingSrtUri(currentVideo)
            if (matchingSrt != null) {
                val parsed = SrtSubtitleParser.loadFromUri(context, matchingSrt)
                if (parsed != null && parsed.cues.isNotEmpty()) {
                    if (externalSubtitles.none { it.id == parsed.id }) {
                        externalSubtitles.add(parsed)
                    }
                    activeExternalSubId = parsed.id
                    subtitlesDisabled = false
                    PlaybackEngine.disableEmbeddedSubtitles()
                    PlaybackEngine.showToastLabel("Auto-loaded ${parsed.displayName}")
                }
            }
        }
    }

    // Brightness & Volume state
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var brightnessFraction by remember {
        val initial = activity?.window?.attributes?.screenBrightness?.takeIf { it in 0f..1f } ?: 0.5f
        mutableFloatStateOf(initial)
    }

    // Track double-tap accumulation
    var lastDoubleTapTimeMs by remember { mutableLongStateOf(0L) }
    var accumulatedTapDeltaSec by remember { mutableIntStateOf(0) }
    var lastTapForward by remember { mutableStateOf(true) }

    val pipSupported = remember(activity) {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            activity?.packageManager?.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) == true
    }

    fun updatePipParams(playing: Boolean, dimensions: Pair<Int, Int>) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || activity == null || !pipSupported) return
        val (num, den) = PlayerMath.clampPipAspectRatio(dimensions.first, dimensions.second)
        val rewindAction = createPipRemoteAction(
            context = context,
            requestCode = 201,
            iconRes = android.R.drawable.ic_media_rew,
            title = "Rewind 10s",
            command = PIP_CMD_REWIND
        )
        val playPauseAction = createPipRemoteAction(
            context = context,
            requestCode = 202,
            iconRes = if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
            title = if (playing) "Pause" else "Play",
            command = PIP_CMD_PLAY_PAUSE
        )
        val forwardAction = createPipRemoteAction(
            context = context,
            requestCode = 203,
            iconRes = android.R.drawable.ic_media_ff,
            title = "Forward 10s",
            command = PIP_CMD_FORWARD
        )
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(num, den))
            .setActions(listOf(rewindAction, playPauseAction, forwardAction))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(false)
        }
        runCatching { activity.setPictureInPictureParams(builder.build()) }
    }

    // BroadcastReceiver for PiP remote action buttons
    DisposableEffect(pipSupported) {
        if (!pipSupported) {
            onDispose { }
        } else {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    if (intent?.action != ACTION_PIP_CONTROL) return
                    val state = PlaybackEngine.sessionState.value
                    when (intent.getIntExtra(EXTRA_PIP_COMMAND, 0)) {
                        PIP_CMD_PLAY_PAUSE -> PlaybackEngine.togglePlayPause()
                        PIP_CMD_REWIND -> {
                            PlaybackEngine.seekTo((state.positionMs - 10_000L).coerceAtLeast(0L))
                        }
                        PIP_CMD_FORWARD -> {
                            val maxDur = state.durationMs.takeIf { it > 0L } ?: Long.MAX_VALUE
                            PlaybackEngine.seekTo((state.positionMs + 10_000L).coerceAtMost(maxDur))
                        }
                    }
                }
            }
            ContextCompat.registerReceiver(
                context,
                receiver,
                IntentFilter(ACTION_PIP_CONTROL),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            onDispose {
                runCatching { context.unregisterReceiver(receiver) }
            }
        }
    }

    // Keep PiP actions synced with Play/Pause state and video aspect ratio
    LaunchedEffect(playbackState.isPlaying, playbackState.videoDimensions, pipSupported) {
        updatePipParams(playbackState.isPlaying, playbackState.videoDimensions)
    }

    // Hide system bars ONLY during active full-screen video playback; keep visible for audio & always restore on exit
    DisposableEffect(activity, view, currentVideo.isAudio) {
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        if (currentVideo.isAudio) {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            controller?.show(WindowInsetsCompat.Type.systemBars())
        } else {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            controller?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            window?.attributes = window?.attributes?.apply {
                screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
            controller?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Exit helper: persist progress, keep service running if audio or Background Play is enabled
    fun exitPlayerScreen() {
        PlaybackEngine.saveCurrentProgress()
        playbackState.currentItem?.let { item ->
            onSaveProgress(item.uri, playbackState.positionMs, playbackState.durationMs)
        }
        val keepPlayingInBackground = playbackState.backgroundPlayEnabled || currentVideo.isAudio
        if (!keepPlayingInBackground) {
            PlaybackEngine.closePlayback(context)
        }
        onBack()
    }

    // Block accidental back press when locked; navigate cleanly when unlocked
    BackHandler {
        if (isLocked) {
            lockedHintVisible = true
        } else {
            exitPlayerScreen()
        }
    }

    // Apply default orientation preference for video playback
    LaunchedEffect(defaultOrientation, currentVideo.isAudio) {
        if (currentVideo.isAudio) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            when (defaultOrientation) {
                DefaultOrientation.LANDSCAPE ->
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                DefaultOrientation.PORTRAIT ->
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                DefaultOrientation.AUTO ->
                    // Explicitly hand rotation back to the physical sensors. UNSPECIFIED can
                    // inherit a stale lock from a previous manual Rotate action.
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
            }
        }
    }

    // Lifecycle handling: NEVER pause background/audio playback on ON_PAUSE or ON_STOP
    val updatedInPip by rememberUpdatedState(isInPipMode)
    val updatedBgPlay by rememberUpdatedState(playbackState.backgroundPlayEnabled)
    val updatedIsAudio by rememberUpdatedState(currentVideo.isAudio)
    val updatedVideo by rememberUpdatedState(currentVideo)
    val updatedPosMs by rememberUpdatedState(playbackState.positionMs)
    val updatedDurMs by rememberUpdatedState(playbackState.durationMs)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    PlaybackEngine.saveCurrentProgress()
                    onSaveProgress(updatedVideo.uri, updatedPosMs, updatedDurMs)
                }
                Lifecycle.Event.ON_STOP -> {
                    if (!updatedInPip && !updatedBgPlay && !updatedIsAudio) {
                        PlaybackEngine.pauseForForegroundExit()
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Auto-hide controls after 3.5 seconds while playing video (keep audio stage controls responsive)
    LaunchedEffect(
        controlsVisible,
        playbackState.isPlaying,
        isLocked,
        showTrackSheet,
        showSpeedSheet,
        showEqualizerSheet,
        quickToolsExpanded
    ) {
        if (controlsVisible && playbackState.isPlaying && !isLocked && !showTrackSheet && !showSpeedSheet && !showEqualizerSheet && !quickToolsExpanded) {
            delay(3500L)
            controlsVisible = false
        }
    }

    // Auto-hide locked hint
    LaunchedEffect(lockedHintVisible) {
        if (lockedHintVisible) {
            delay(2200L)
            lockedHintVisible = false
        }
    }

    // Auto-hide centered status toast
    LaunchedEffect(playbackState.statusToastLabel) {
        if (playbackState.statusToastLabel != null) {
            delay(1500L)
            PlaybackEngine.clearToastLabel()
        }
    }

    // Auto-hide double-tap feedback
    LaunchedEffect(gestureFeedback) {
        if (gestureFeedback is GestureFeedback.DoubleTapSeek) {
            delay(900L)
            gestureFeedback = null
        }
    }

    // SRT document picker
    val srtPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val parsed = SrtSubtitleParser.loadFromUri(context, uri)
                if (parsed != null) {
                    val existingIdx = externalSubtitles.indexOfFirst { it.id == parsed.id }
                    if (existingIdx >= 0) {
                        externalSubtitles[existingIdx] = parsed
                    } else {
                        externalSubtitles.add(parsed)
                    }
                    activeExternalSubId = parsed.id
                    subtitlesDisabled = false
                    PlaybackEngine.disableEmbeddedSubtitles()
                }
            }
        }
    }

    // Compute active subtitle text (with subtitleOffsetMs sync applied)
    val activeExternalTrack = externalSubtitles.find { it.id == activeExternalSubId }
    val syncedPositionMs = (playbackState.positionMs - subtitleOffsetMs).coerceAtLeast(0L)
    val activeSubtitleText = when {
        subtitlesDisabled -> ""
        activeExternalTrack != null -> SrtSubtitleParser.findActiveCues(
            activeExternalTrack.cues,
            syncedPositionMs
        )
        else -> playbackState.embeddedCueText
    }

    val updatedIsLocked by rememberUpdatedState(isLocked)
    val updatedDuration by rememberUpdatedState(playbackState.durationMs)
    val updatedPosition by rememberUpdatedState(playbackState.positionMs)
    val viewConfig = LocalViewConfiguration.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .onSizeChanged { containerSize = it }
            // Tap & Double-tap gesture detector
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (isInPipMode) return@detectTapGestures
                        if (updatedIsLocked) {
                            lockedHintVisible = !lockedHintVisible
                        } else {
                            controlsVisible = !controlsVisible
                        }
                    },
                    onDoubleTap = { offset ->
                        if (isInPipMode) return@detectTapGestures
                        if (updatedIsLocked) {
                            lockedHintVisible = true
                            return@detectTapGestures
                        }
                        val width = containerSize.width.toFloat().coerceAtLeast(1f)
                        val isLeft = offset.x < width * 0.42f
                        val isRight = offset.x > width * 0.58f

                        if (!isLeft && !isRight) {
                            PlaybackEngine.togglePlayPause()
                            return@detectTapGestures
                        }

                        val isForward = isRight
                        val now = System.currentTimeMillis()
                        if (now - lastDoubleTapTimeMs < 950L && lastTapForward == isForward) {
                            accumulatedTapDeltaSec += 10
                        } else {
                            accumulatedTapDeltaSec = 10
                        }
                        lastDoubleTapTimeMs = now
                        lastTapForward = isForward

                        val deltaMs = if (isForward) 10_000L else -10_000L
                        val maxDur = updatedDuration.takeIf { it > 0L } ?: Long.MAX_VALUE
                        val basePos = exoPlayer?.currentPosition ?: updatedPosition
                        val newPos = (basePos + deltaMs).coerceIn(0L, maxDur)
                        PlaybackEngine.seekTo(newPos)
                        gestureFeedback = GestureFeedback.DoubleTapSeek(
                            isForward = isForward,
                            totalDeltaSeconds = accumulatedTapDeltaSec
                        )
                    }
                )
            }
            // Pinch-to-Zoom & Pan + Single-finger MX Vertical/Horizontal swipe detector
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (updatedIsLocked || isInPipMode) return@awaitEachGesture

                    val startX = down.position.x
                    var dragMode = DragMode.NONE
                    var cumulativeDx = 0f
                    var cumulativeDy = 0f

                    val initialPosMs = (exoPlayer?.currentPosition ?: updatedPosition).coerceAtLeast(0L)
                    var seekTargetMs = initialPosMs

                    val currentBrightness = activity?.window?.attributes?.screenBrightness
                        ?.takeIf { it in 0f..1f } ?: brightnessFraction
                    var activeBrightness = currentBrightness

                    val currentVolStep = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    var activeVol = currentVolStep.toFloat() / maxVolume.toFloat()

                    val slop = viewConfig.touchSlop

                    while (true) {
                        val event = awaitPointerEvent()
                        val pressedChanges = event.changes.filter { it.pressed }

                        if (pressedChanges.isEmpty()) {
                            if (dragMode == DragMode.HORIZONTAL_SEEK) {
                                PlaybackEngine.seekTo(seekTargetMs)
                            }
                            if (dragMode != DragMode.NONE) {
                                gestureFeedback = null
                            }
                            break
                        }

                        if (pressedChanges.size >= 2) {
                            dragMode = DragMode.PINCH_ZOOM
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            val nextScale = PlayerMath.clampZoomScale(zoomScale * zoomChange)
                            zoomScale = nextScale

                            val maxOffsetX = (containerSize.width * (nextScale - 1f).coerceAtLeast(0f)) / 2f + 160f
                            val maxOffsetY = (containerSize.height * (nextScale - 1f).coerceAtLeast(0f)) / 2f + 160f
                            val nextPan = panOffset + panChange
                            panOffset = Offset(
                                x = nextPan.x.coerceIn(-maxOffsetX, maxOffsetX),
                                y = nextPan.y.coerceIn(-maxOffsetY, maxOffsetY)
                            )

                            gestureFeedback = GestureFeedback.PinchZoom((nextScale * 100f).roundToInt())
                            event.changes.forEach { it.consume() }
                            continue
                        }

                        if (dragMode == DragMode.PINCH_ZOOM) {
                            continue
                        }

                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            if (dragMode == DragMode.HORIZONTAL_SEEK) {
                                PlaybackEngine.seekTo(seekTargetMs)
                            }
                            if (dragMode != DragMode.NONE) {
                                gestureFeedback = null
                            }
                            break
                        }

                        val delta = change.positionChange()
                        cumulativeDx += delta.x
                        cumulativeDy += delta.y

                        if (dragMode == DragMode.NONE) {
                            val absX = abs(cumulativeDx)
                            val absY = abs(cumulativeDy)
                            if (absX > slop || absY > slop) {
                                dragMode = if (absX > absY) {
                                    DragMode.HORIZONTAL_SEEK
                                } else if (startX < containerSize.width / 2f) {
                                    DragMode.BRIGHTNESS
                                } else {
                                    DragMode.VOLUME
                                }
                            }
                        }

                        when (dragMode) {
                            DragMode.HORIZONTAL_SEEK -> {
                                change.consume()
                                val widthPx = containerSize.width.toFloat().coerceAtLeast(1f)
                                val totalSeekSpanMs = if (updatedDuration > 0L) {
                                    minOf(120_000L, maxOf(45_000L, updatedDuration / 2L))
                                } else {
                                    60_000L
                                }
                                val offsetFromStart = change.position.x - startX
                                val deltaMs = ((offsetFromStart / widthPx) * totalSeekSpanMs).toLong()
                                val maxDur = updatedDuration.takeIf { it > 0L } ?: Long.MAX_VALUE
                                seekTargetMs = (initialPosMs + deltaMs).coerceIn(0L, maxDur)
                                gestureFeedback = GestureFeedback.HorizontalSeek(
                                    targetMs = seekTargetMs,
                                    deltaMs = seekTargetMs - initialPosMs,
                                    durationMs = updatedDuration
                                )
                            }

                            DragMode.BRIGHTNESS -> {
                                change.consume()
                                val heightPx = (containerSize.height * 0.65f).coerceAtLeast(1f)
                                val deltaNorm = -delta.y / heightPx
                                activeBrightness = (activeBrightness + deltaNorm).coerceIn(0.01f, 1.0f)
                                brightnessFraction = activeBrightness
                                activity?.window?.let { w ->
                                    val attrs = w.attributes
                                    attrs.screenBrightness = activeBrightness
                                    w.attributes = attrs
                                }
                                gestureFeedback = GestureFeedback.Brightness(
                                    percent = (activeBrightness * 100f).roundToInt()
                                )
                            }

                            DragMode.VOLUME -> {
                                change.consume()
                                val heightPx = (containerSize.height * 0.65f).coerceAtLeast(1f)
                                val deltaNorm = -delta.y / heightPx
                                activeVol = (activeVol + deltaNorm).coerceIn(0f, 1f)
                                val targetStep = (activeVol * maxVolume).roundToInt().coerceIn(0, maxVolume)
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetStep, 0)
                                gestureFeedback = GestureFeedback.Volume(
                                    percent = (activeVol * 100f).roundToInt(),
                                    isMuted = targetStep == 0
                                )
                            }

                            DragMode.PINCH_ZOOM, DragMode.NONE -> Unit
                        }
                    }
                }
            }
    ) {
        // Video Surface via Media3 ContentFrame with Pinch-to-Zoom & Pan, or Emerald Audio Stage
        if (currentVideo.isAudio) {
            AudioPlayerStage(
                track = currentVideo,
                isPlaying = playbackState.isPlaying,
                controlsVisible = controlsVisible && !isLocked,
                isInPipMode = isInPipMode,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            ContentFrame(
                player = exoPlayer,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoomScale
                        scaleY = zoomScale
                        translationX = panOffset.x
                        translationY = panOffset.y
                    },
                contentScale = aspectMode.contentScale
            )
        }

        // Subtitles Overlay (Google Sans default for Bangla & English legibility)
        if (activeSubtitleText.isNotEmpty() && !isInPipMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                    )
                    .padding(
                        start = 24.dp,
                        end = 24.dp,
                        bottom = if (controlsVisible && !isLocked) 132.dp else 36.dp
                    )
            ) {
                Surface(
                    color = AmoledBlack.copy(alpha = subtitleStyle.bgOpacity),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = activeSubtitleText,
                        fontFamily = GoogleSansFamily,
                        fontSize = subtitleStyle.fontSize.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = subtitleStyle.textColor.color,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("subtitle_overlay_text")
                    )
                }
            }
        }

        // Gesture Feedback HUD (Brightness / Volume / Seek / Double-Tap / PinchZoom / Toast)
        if (!isInPipMode) {
            GestureFeedbackHud(
                feedback = gestureFeedback,
                statusToastLabel = playbackState.statusToastLabel,
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            )
        }

        // Controls Overlay (when unlocked and not in PiP)
        AnimatedVisibility(
            visible = controlsVisible && !isLocked && !isInPipMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            val subtitleMeta = if (currentVideo.isAudio) {
                listOf(currentVideo.artist, currentVideo.album, currentVideo.extension)
                    .filter { it.isNotBlank() }
                    .joinToString(" • ")
            } else {
                "${currentVideo.resolutionLabel} • ${currentVideo.extension}"
            }
            PlayerControlsOverlay(
                title = currentVideo.title,
                subtitleLabel = subtitleMeta,
                isAudio = currentVideo.isAudio,
                isPlaying = playbackState.isPlaying,
                currentPositionMs = if (isScrubbing) scrubPositionMs else playbackState.positionMs,
                durationMs = playbackState.durationMs,
                playbackSpeed = playbackState.playbackSpeed,
                aspectMode = aspectMode,
                decoderMode = playbackState.decoderMode,
                zoomScale = zoomScale,
                pipSupported = pipSupported,
                backgroundPlayEnabled = playbackState.backgroundPlayEnabled,
                quickToolsExpanded = quickToolsExpanded,
                audioBoostPercent = playbackState.audioBoostPercent,
                eqState = playbackState.equalizer,
                sleepTimerOption = playbackState.sleepTimerOption,
                sleepRemainingSeconds = playbackState.sleepRemainingSeconds,
                loopMode = playbackState.loopMode,
                abStartMs = playbackState.abStartMs,
                abEndMs = playbackState.abEndMs,
                hasPrevious = activeIndex > 0,
                hasNext = activeIndex < activePlaylist.lastIndex,
                onBack = { exitPlayerScreen() },
                onToggleDecoder = {
                    val nextMode = if (playbackState.decoderMode == DecoderMode.HW) DecoderMode.SW else DecoderMode.HW
                    PlaybackEngine.switchDecoderMode(nextMode)
                },
                onToggleBackgroundPlay = {
                    if (playbackState.backgroundPlayEnabled) {
                        PlaybackEngine.setBackgroundPlayEnabled(false)
                        PlaybackEngine.showToastLabel("Background Play Off")
                    } else if (
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        PlaybackEngine.setBackgroundPlayEnabled(true)
                        PlaybackEngine.showToastLabel("Background Play Enabled")
                    }
                },
                onEnterPip = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null && pipSupported) {
                        controlsVisible = false
                        quickToolsExpanded = false
                        updatePipParams(playbackState.isPlaying, playbackState.videoDimensions)
                        val (num, den) = PlayerMath.clampPipAspectRatio(
                            playbackState.videoDimensions.first,
                            playbackState.videoDimensions.second
                        )
                        val params = PictureInPictureParams.Builder()
                            .setAspectRatio(Rational(num, den))
                            .build()
                        activity.enterPictureInPictureMode(params)
                    }
                },
                onToggleQuickTools = {
                    quickToolsExpanded = !quickToolsExpanded
                },
                onOpenTracks = { showTrackSheet = true },
                onOpenEqualizer = { showEqualizerSheet = true },
                onCaptureScreenshot = {
                    val targetVideo = currentVideo
                    val capturePosMs = exoPlayer?.currentPosition ?: playbackState.positionMs
                    val dims = playbackState.videoDimensions
                    coroutineScope.launch {
                        val saved = VideoScreenshotHelper.captureAndSaveFrame(
                            context = context,
                            rootView = view,
                            videoUri = targetVideo.uri,
                            videoTitle = targetVideo.title,
                            positionMs = capturePosMs,
                            videoDimensions = dims
                        )
                        PlaybackEngine.showToastLabel(
                            if (saved) "Screenshot saved to Pictures/AltPlayer"
                            else "Unable to capture video frame"
                        )
                    }
                },
                onOpenSpeed = { showSpeedSheet = true },
                onCycleAspect = {
                    aspectMode = aspectMode.next()
                    zoomScale = 1.0f
                    panOffset = Offset.Zero
                    PlaybackEngine.showToastLabel(aspectMode.label)
                },
                onResetZoom = {
                    zoomScale = 1.0f
                    panOffset = Offset.Zero
                    PlaybackEngine.showToastLabel("Zoom Reset (100%)")
                },
                onToggleRotation = {
                    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                    activity?.requestedOrientation = if (isLandscape) {
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    }
                },
                onLock = {
                    isLocked = true
                    controlsVisible = false
                    quickToolsExpanded = false
                    lockedHintVisible = true
                },
                onPrevious = { PlaybackEngine.skipPrevious() },
                onNext = { PlaybackEngine.skipNext() },
                onRewind10 = {
                    val basePos = exoPlayer?.currentPosition ?: playbackState.positionMs
                    PlaybackEngine.seekTo((basePos - 10_000L).coerceAtLeast(0L))
                },
                onForward10 = {
                    val maxDur = playbackState.durationMs.takeIf { it > 0L } ?: Long.MAX_VALUE
                    val basePos = exoPlayer?.currentPosition ?: playbackState.positionMs
                    PlaybackEngine.seekTo((basePos + 10_000L).coerceAtMost(maxDur))
                },
                onTogglePlayPause = { PlaybackEngine.togglePlayPause() },
                onScrubChange = { pos ->
                    isScrubbing = true
                    scrubPositionMs = pos
                },
                onScrubFinished = {
                    PlaybackEngine.seekTo(scrubPositionMs)
                    isScrubbing = false
                },
                onSelectAudioBoost = { pct -> PlaybackEngine.setAudioBoostPercent(pct) },
                onSelectSleepTimer = { option -> PlaybackEngine.setSleepTimerOption(option) },
                onCycleLoopMode = { PlaybackEngine.cycleLoopMode() },
                onToggleAbRepeat = { PlaybackEngine.toggleAbRepeat() },
                onClearAbRepeat = { PlaybackEngine.clearAbRepeat() }
            )
        }

        // Locked State Unlock Pill
        AnimatedVisibility(
            visible = isLocked && lockedHintVisible && !isInPipMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(18.dp)
        ) {
            Surface(
                onClick = {
                    isLocked = false
                    lockedHintVisible = false
                    controlsVisible = true
                },
                color = AmoledBlack.copy(alpha = 0.82f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("unlock_screen_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = stringResource(R.string.unlock_screen),
                        tint = ElectricCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.screen_locked_hint),
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                }
            }
        }
    }

    if (showTrackSheet) {
        TrackSelectorSheet(
            audioTracks = playbackState.audioTracks,
            embeddedSubtitles = playbackState.embeddedSubtitles,
            externalSubtitles = externalSubtitles,
            selectedExternalId = activeExternalSubId,
            subtitlesDisabled = subtitlesDisabled,
            subtitleOffsetMs = subtitleOffsetMs,
            subtitleStyle = subtitleStyle,
            onSelectAudioTrack = { option ->
                PlaybackEngine.selectAudioTrack(option)
                showTrackSheet = false
            },
            onSelectSubtitlesOff = {
                activeExternalSubId = null
                subtitlesDisabled = true
                PlaybackEngine.disableEmbeddedSubtitles()
                showTrackSheet = false
            },
            onSelectEmbeddedSubtitle = { sub ->
                activeExternalSubId = null
                subtitlesDisabled = false
                PlaybackEngine.selectEmbeddedSubtitle(sub)
                showTrackSheet = false
            },
            onSelectExternalSubtitle = { ext ->
                activeExternalSubId = ext.id
                subtitlesDisabled = false
                PlaybackEngine.disableEmbeddedSubtitles()
                showTrackSheet = false
            },
            onLoadExternalSrtClick = {
                srtPickerLauncher.launch(
                    arrayOf(
                        "application/x-subrip",
                        "text/plain",
                        "application/octet-stream",
                        "*/*"
                    )
                )
            },
            onAdjustSubtitleOffset = { deltaMs ->
                subtitleOffsetMs = PlayerMath.clampSubtitleOffsetMs(subtitleOffsetMs + deltaMs)
            },
            onResetSubtitleOffset = {
                subtitleOffsetMs = 0L
            },
            onUpdateSubtitleStyle = onUpdateSubtitleStyle,
            onDismiss = { showTrackSheet = false }
        )
    }

    if (showSpeedSheet) {
        SpeedSelectorSheet(
            currentSpeed = playbackState.playbackSpeed,
            onSelectSpeed = { speed -> PlaybackEngine.setPlaybackSpeed(speed) },
            onDismiss = { showSpeedSheet = false }
        )
    }

    if (showEqualizerSheet) {
        EqualizerSheet(
            eqState = playbackState.equalizer,
            onToggleEnabled = { enabled -> PlaybackEngine.setEqualizerEnabled(enabled) },
            onSelectPreset = { preset -> PlaybackEngine.selectEqualizerPreset(preset) },
            onBandLevelChange = { band, level -> PlaybackEngine.setEqualizerBandLevel(band, level) },
            onDismiss = { showEqualizerSheet = false }
        )
    }

    pendingAskResumeMs?.let { savedMs ->
        AlertDialog(
            onDismissRequest = {
                pendingAskResumeMs = null
                exoPlayer?.let {
                    it.playWhenReady = true
                    it.play()
                }
            },
            containerColor = DeepNavySurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = {
                Text(
                    text = "Resume Playback?",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = "Resume \"${currentVideo.title}\" from ${formatDuration(savedMs)} or start over?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        PlaybackEngine.seekTo(savedMs)
                        pendingAskResumeMs = null
                        exoPlayer?.let {
                            it.playWhenReady = true
                            it.play()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = AmoledBlack
                    ),
                    modifier = Modifier.testTag("resume_prompt_confirm")
                ) {
                    Text(
                        text = "Resume from ${formatDuration(savedMs)}",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        PlaybackEngine.seekTo(0L)
                        pendingAskResumeMs = null
                        exoPlayer?.let {
                            it.playWhenReady = true
                            it.play()
                        }
                    },
                    modifier = Modifier.testTag("resume_prompt_start_over")
                ) {
                    Text("Start Over", color = TextSecondary)
                }
            }
        )
    }
}

private fun createPipRemoteAction(
    context: Context,
    requestCode: Int,
    iconRes: Int,
    title: String,
    command: Int
): RemoteAction {
    val intent = Intent(ACTION_PIP_CONTROL).apply {
        setPackage(context.packageName)
        putExtra(EXTRA_PIP_COMMAND, command)
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    return RemoteAction(
        AndroidIcon.createWithResource(context, iconRes),
        title,
        title,
        pendingIntent
    )
}

@Composable
private fun AudioPlayerStage(
    track: VideoItem,
    isPlaying: Boolean,
    controlsVisible: Boolean,
    isInPipMode: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(DeepNavySurface, AmoledBlack, AmoledBlack)
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.45f)),
                shadowElevation = 16.dp,
                color = DeepNavySurface,
                modifier = Modifier
                    .size(if (isInPipMode) 110.dp else 210.dp)
                    .testTag("audio_player_artwork")
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    VideoThumbnailImage(
                        video = track,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (controlsVisible) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(AmoledBlack.copy(alpha = 0.5f))
                        )
                    }
                }
            }

            if (!controlsVisible && !isInPipMode) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    val artistAlbum = listOf(track.artist, track.album)
                        .filter { it.isNotBlank() }
                        .joinToString(" • ")
                    if (artistAlbum.isNotBlank()) {
                        Text(
                            text = artistAlbum,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                    Surface(
                        color = if (isPlaying) CyanContainer else VioletContainer,
                        shape = CircleShape,
                        border = BorderStroke(
                            1.dp,
                            if (isPlaying) ElectricCyan.copy(alpha = 0.45f) else VioletAccent.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${track.extension} • ${if (isPlaying) "Playing" else "Paused"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
