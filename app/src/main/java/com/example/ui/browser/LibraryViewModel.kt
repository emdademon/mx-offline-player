package com.example.ui.browser

import android.app.Application
import android.app.RecoverableSecurityException
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.AppThemePreset
import com.example.data.AppUpdateUiState
import com.example.data.ApkInstallResult
import com.example.data.DefaultOrientation
import com.example.data.GitHubUpdateRepository
import com.example.data.LibraryTab
import com.example.data.LicenseTier
import com.example.data.MediaAccessState
import com.example.data.MediaStoreVideoRepository
import com.example.data.PlaybackPreferencesRepository
import com.example.data.PlaybackPreferencesSnapshot
import com.example.data.PrivateVaultRepository
import com.example.data.ProActivationResult
import com.example.data.ProLicenseManager
import com.example.data.ProLicenseState
import com.example.data.ResumeBehavior
import com.example.data.UpdateStatus
import com.example.data.SortOption
import com.example.data.SortOrder
import com.example.data.VideoFolder
import com.example.data.VideoItem
import com.example.data.VideoMediaInfo
import com.example.data.sortFolders
import com.example.data.sortVideos
import com.example.player.SubtitleStyleConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LibraryUiState(
    val isLoading: Boolean = true,
    val isPrefsLoaded: Boolean = false,
    val isRefreshing: Boolean = false,
    val accessState: MediaAccessState = MediaAccessState.UNKNOWN,
    val allVideos: List<VideoItem> = emptyList(),
    val allAudio: List<VideoItem> = emptyList(),
    val folders: List<VideoFolder> = emptyList(),
    val filteredFolders: List<VideoFolder> = emptyList(),
    val filteredVideos: List<VideoItem> = emptyList(),
    val filteredAudio: List<VideoItem> = emptyList(),
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.DATE,
    val sortOrder: SortOrder = SortOrder.DESCENDING,
    val isGridView: Boolean = false,
    val activeTab: LibraryTab = LibraryTab.FOLDERS,
    val resumeBehavior: ResumeBehavior = ResumeBehavior.ASK,
    val defaultOrientation: DefaultOrientation = DefaultOrientation.AUTO,
    val autoLoadSrt: Boolean = true,
    val showHiddenFolders: Boolean = false,
    val hiddenBucketIds: Set<String> = emptySet(),
    val privatePinHash: String? = null,
    val subtitleStyle: SubtitleStyleConfig = SubtitleStyleConfig(),
    val proLicense: ProLicenseState = ProLicenseState(),
    val appThemePreset: AppThemePreset = AppThemePreset.PINE_MINT,
    val appUpdate: AppUpdateUiState = AppUpdateUiState(),
    val vaultVideos: List<VideoItem> = emptyList(),
    val positionsByUri: Map<String, Long> = emptyMap(),
    val watchedUris: Set<String> = emptySet(),
    val continueWatchingVideo: VideoItem? = null,
    val continueWatchingPositionMs: Long = 0L,
    val activePlaylist: List<VideoItem> = emptyList(),
    val activeVideoIndex: Int = 0,
    val playerSessionKey: Int = 0,
    val inspectedMediaInfo: VideoMediaInfo? = null,
    val isLoadingMediaInfo: Boolean = false,
    val statusMessage: String? = null
) {
    val hasPrivatePin: Boolean get() = !privatePinHash.isNullOrBlank()
}

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val videoRepo = MediaStoreVideoRepository(application)
    private val prefsRepo = PlaybackPreferencesRepository(application)
    private val vaultRepo = PrivateVaultRepository(application)
    private val updateRepo = GitHubUpdateRepository(application)

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private var latestPrefs = PlaybackPreferencesSnapshot()
    private var updateCheckJob: Job? = null

    init {
        viewModelScope.launch {
            prefsRepo.preferencesFlow.collect { snapshot ->
                latestPrefs = snapshot
                _uiState.update { current ->
                    val updatedFolders = videoRepo.groupVideosByFolder(
                        current.allVideos,
                        snapshot.hiddenBucketIds
                    )
                    recomputeDerivedLists(
                        current.copy(
                            sortOption = snapshot.sortOption,
                            sortOrder = snapshot.sortOrder,
                            isGridView = snapshot.isGridView,
                            resumeBehavior = snapshot.resumeBehavior,
                            defaultOrientation = snapshot.defaultOrientation,
                            autoLoadSrt = snapshot.autoLoadSrt,
                            showHiddenFolders = snapshot.showHiddenFolders,
                            hiddenBucketIds = snapshot.hiddenBucketIds,
                            privatePinHash = snapshot.privatePinHash,
                            subtitleStyle = snapshot.subtitleStyle,
                            proLicense = snapshot.proLicense,
                            appThemePreset = snapshot.appThemePreset,
                            isPrefsLoaded = true,
                            positionsByUri = snapshot.positionsByUri,
                            watchedUris = snapshot.watchedUris,
                            folders = updatedFolders
                        )
                    )
                }
            }
        }
        checkForUpdates(silent = true)
        refreshPermissionAndScan()
        refreshVault()
    }

    fun refreshPermissionAndScan(pullToRefresh: Boolean = false) {
        val access = videoRepo.checkMediaAccessState()
        _uiState.update { it.copy(accessState = access) }

        if (access == MediaAccessState.GRANTED_ALL || access == MediaAccessState.GRANTED_PARTIAL) {
            scanLibrary(pullToRefresh = pullToRefresh)
        } else {
            _uiState.update {
                recomputeDerivedLists(
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        allVideos = emptyList(),
                        allAudio = emptyList(),
                        folders = emptyList()
                    )
                )
            }
        }
    }

    fun scanLibrary(pullToRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                if (pullToRefresh) it.copy(isRefreshing = true)
                else it.copy(
                    isLoading = it.allVideos.isEmpty() && it.allAudio.isEmpty(),
                    isRefreshing = it.allVideos.isNotEmpty() || it.allAudio.isNotEmpty()
                )
            }
            val access = videoRepo.checkMediaAccessState()
            val scannedVideos = if (access == MediaAccessState.GRANTED_ALL || access == MediaAccessState.GRANTED_PARTIAL) {
                videoRepo.scanAllVideos()
            } else {
                emptyList()
            }
            val scannedAudio = if (access == MediaAccessState.GRANTED_ALL) {
                videoRepo.scanAllAudio()
            } else {
                emptyList()
            }
            val grouped = videoRepo.groupVideosByFolder(scannedVideos, latestPrefs.hiddenBucketIds)
            _uiState.update { current ->
                recomputeDerivedLists(
                    current.copy(
                        isLoading = false,
                        isRefreshing = false,
                        accessState = access,
                        allVideos = scannedVideos,
                        allAudio = scannedAudio,
                        folders = grouped
                    )
                )
            }
        }
    }

    fun refreshVault() {
        viewModelScope.launch {
            val items = vaultRepo.listVaultVideos()
            _uiState.update { it.copy(vaultVideos = items) }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { current ->
            recomputeDerivedLists(current.copy(searchQuery = query))
        }
    }

    fun onSelectTab(tab: LibraryTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun onSelectSort(option: SortOption, order: SortOrder) {
        viewModelScope.launch {
            prefsRepo.saveSortPreference(option, order)
        }
        _uiState.update { current ->
            recomputeDerivedLists(current.copy(sortOption = option, sortOrder = order))
        }
    }

    fun toggleGridView() {
        val next = !_uiState.value.isGridView
        viewModelScope.launch { prefsRepo.saveIsGridView(next) }
        _uiState.update { it.copy(isGridView = next) }
    }

    fun setResumeBehavior(behavior: ResumeBehavior) {
        viewModelScope.launch { prefsRepo.saveResumeBehavior(behavior) }
    }

    fun setDefaultOrientation(orientation: DefaultOrientation) {
        viewModelScope.launch { prefsRepo.saveDefaultOrientation(orientation) }
    }

    fun setAutoLoadSrt(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.saveAutoLoadSrt(enabled) }
    }

    fun setShowHiddenFolders(show: Boolean) {
        viewModelScope.launch { prefsRepo.saveShowHiddenFolders(show) }
    }

    fun setSubtitleStyle(style: SubtitleStyleConfig) {
        viewModelScope.launch { prefsRepo.saveSubtitleStyle(style) }
    }

    fun setAppThemePreset(preset: AppThemePreset) {
        _uiState.update { it.copy(appThemePreset = preset) }
        viewModelScope.launch { prefsRepo.saveAppThemePreset(preset) }
    }

    fun checkForUpdates(silent: Boolean = false) {
        updateCheckJob?.cancel()
        if (!silent) {
            _uiState.update {
                it.copy(
                    appUpdate = AppUpdateUiState(
                        status = UpdateStatus.CHECKING,
                        message = "Checking for updates…"
                    )
                )
            }
        }
        updateCheckJob = viewModelScope.launch {
            if (silent) {
                _uiState.update {
                    it.copy(appUpdate = it.appUpdate.copy(status = UpdateStatus.CHECKING))
                }
            }
            val result = updateRepo.checkForUpdate(BuildConfig.VERSION_NAME)
            result.onSuccess { update ->
                _uiState.update {
                    it.copy(
                        appUpdate = if (update == null) {
                            AppUpdateUiState(
                                status = UpdateStatus.UP_TO_DATE,
                                message = "You are running the latest version."
                            )
                        } else {
                            AppUpdateUiState(
                                status = UpdateStatus.AVAILABLE,
                                availableUpdate = update,
                                message = "Version ${update.versionName} is ready to download."
                            )
                        }
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        appUpdate = AppUpdateUiState(
                            status = UpdateStatus.ERROR,
                            message = "Could not check for updates."
                        )
                    )
                }
            }
        }
    }

    fun downloadAndInstallUpdate() {
        val update = _uiState.value.appUpdate.availableUpdate ?: return
        _uiState.update {
            it.copy(
                appUpdate = it.appUpdate.copy(
                    status = UpdateStatus.DOWNLOADING,
                    downloadProgress = 0,
                    message = "Downloading ${update.assetName}…"
                )
            )
        }
        viewModelScope.launch {
            val result = updateRepo.downloadApk(update) { progress ->
                _uiState.update {
                    it.copy(appUpdate = it.appUpdate.copy(downloadProgress = progress))
                }
            }
            result.onSuccess { apkFile ->
                runCatching { updateRepo.startInstaller(apkFile) }
                    .onSuccess { installResult ->
                        val message = when (installResult) {
                            ApkInstallResult.STARTED -> "Android installer opened."
                            ApkInstallResult.PERMISSION_REQUIRED ->
                                "Allow installs from Alt Player, then tap Download & Install again."
                        }
                        _uiState.update {
                            it.copy(
                                appUpdate = it.appUpdate.copy(
                                    status = UpdateStatus.AVAILABLE,
                                    message = message,
                                    downloadProgress = 100
                                )
                            )
                        }
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                appUpdate = it.appUpdate.copy(
                                    status = UpdateStatus.ERROR,
                                    message = "Could not open the installer."
                                )
                            )
                        }
                    }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        appUpdate = it.appUpdate.copy(
                            status = UpdateStatus.ERROR,
                            message = "Update download failed."
                        )
                    )
                }
            }
        }
    }

    fun startThreeDayTrial() {
        viewModelScope.launch {
            prefsRepo.startThreeDayTrial()
        }
    }

    fun activateProWithCode(code: String, onResult: (ProActivationResult) -> Unit) {
        viewModelScope.launch {
            val result = prefsRepo.activateProWithCode(code)
            if (result is ProActivationResult.Success) {
                _uiState.update {
                    it.copy(statusMessage = "Alt Player Pro unlocked (${result.unlockType.label})")
                }
            }
            onResult(result)
        }
    }

    /**
     * The developer generator is an additional defense-in-depth boundary. The Settings screen
     * does not expose this callback to non-family users, and the ViewModel refuses generation
     * unless the DataStore-backed tier is FAMILY_VIP as well.
     */
    fun generateOfflineBuyerCode(): String {
        val license = _uiState.value.proLicense
        if (!license.isUnlocked || license.licenseTier != LicenseTier.FAMILY_VIP.value) {
            return ""
        }
        return ProLicenseManager.generateBuyerCode()
    }

    fun setFolderHidden(bucketId: String, hidden: Boolean) {
        viewModelScope.launch {
            prefsRepo.setFolderHidden(bucketId, hidden)
            val msg = if (hidden) "Folder hidden. Manage in Settings." else "Folder restored."
            _uiState.update { it.copy(statusMessage = msg) }
        }
    }

    fun setPrivatePin(pin: String) {
        viewModelScope.launch {
            prefsRepo.setPrivatePin(pin)
        }
    }

    fun verifyPrivatePin(pin: String): Boolean {
        return PlaybackPreferencesRepository.verifyPin(pin, _uiState.value.privatePinHash)
    }

    fun clearPrivatePin() {
        viewModelScope.launch { prefsRepo.clearPrivatePin() }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun getFolderById(bucketId: String): VideoFolder? {
        val state = _uiState.value
        val folder = state.folders.find { it.bucketId == bucketId } ?: return null
        val q = state.searchQuery.trim().lowercase()
        val matching = if (q.isEmpty()) {
            folder.videos
        } else {
            folder.videos.filter {
                it.title.lowercase().contains(q) || it.displayName.lowercase().contains(q)
            }
        }
        return folder.copy(videos = sortVideos(matching, state.sortOption, state.sortOrder))
    }

    fun startPlayback(video: VideoItem, playlist: List<VideoItem>) {
        val index = playlist.indexOfFirst { it.uri == video.uri }.let { if (it >= 0) it else 0 }
        _uiState.update { state ->
            val nextKey = maxOf(state.playerSessionKey, com.example.player.PlaybackEngine.activeSessionKey, 0) + 1
            state.copy(
                activePlaylist = playlist.ifEmpty { listOf(video) },
                activeVideoIndex = index,
                playerSessionKey = nextKey
            )
        }
    }

    fun openExternalVideo(uri: Uri, onReady: () -> Unit) {
        viewModelScope.launch {
            val existingVideo = _uiState.value.allVideos.find { it.uri == uri }
            val existingAudio = _uiState.value.allAudio.find { it.uri == uri }
            val resolved = existingVideo ?: existingAudio ?: videoRepo.resolveDocumentVideo(uri)
            val playlist = when {
                existingVideo != null -> {
                    val folderVideos = _uiState.value.folders
                        .find { it.bucketId == existingVideo.bucketId }?.videos ?: listOf(existingVideo)
                    sortVideos(folderVideos, _uiState.value.sortOption, _uiState.value.sortOrder)
                }
                existingAudio != null -> {
                    sortVideos(_uiState.value.allAudio, _uiState.value.sortOption, _uiState.value.sortOrder)
                }
                else -> listOf(resolved)
            }
            startPlayback(resolved, playlist)
            onReady()
        }
    }

    fun getResumePositionMs(uri: Uri): Long {
        val key = PlaybackPreferencesRepository.uriKey(uri.toString())
        return _uiState.value.positionsByUri[key] ?: 0L
    }

    fun savePlaybackProgress(uri: Uri, positionMs: Long, durationMs: Long) {
        viewModelScope.launch {
            prefsRepo.savePlaybackProgress(uri.toString(), positionMs, durationMs)
        }
    }

    suspend fun findMatchingSrtUri(video: VideoItem): Uri? {
        return videoRepo.findMatchingSrtUri(video)
    }

    fun inspectMediaInfo(video: VideoItem) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMediaInfo = true, inspectedMediaInfo = null) }
            val info = videoRepo.loadMediaInfo(video)
            _uiState.update { it.copy(isLoadingMediaInfo = false, inspectedMediaInfo = info) }
        }
    }

    fun dismissMediaInfo() {
        _uiState.update { it.copy(isLoadingMediaInfo = false, inspectedMediaInfo = null) }
    }

    fun getShareableUri(video: VideoItem): Uri = vaultRepo.getShareableUri(video)

    fun createDeleteIntentSender(uris: List<Uri>): IntentSender? {
        if (uris.isEmpty()) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching {
                MediaStore.createDeleteRequest(getApplication<Application>().contentResolver, uris).intentSender
            }.getOrNull()
        } else {
            null
        }
    }

    fun createWriteIntentSender(uris: List<Uri>): IntentSender? {
        if (uris.isEmpty()) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching {
                MediaStore.createWriteRequest(getApplication<Application>().contentResolver, uris).intentSender
            }.getOrNull()
        } else {
            null
        }
    }

    fun deleteVideosLegacy(
        videos: List<VideoItem>,
        onRecoverableSecurity: (IntentSender) -> Unit
    ) {
        viewModelScope.launch {
            var deletedAny = false
            for (video in videos) {
                if (video.isPrivateVault) {
                    if (vaultRepo.deleteFromVault(video)) deletedAny = true
                } else {
                    try {
                        if (videoRepo.deleteMediaStoreVideoLegacy(video.uri)) {
                            deletedAny = true
                        }
                    } catch (e: SecurityException) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
                            onRecoverableSecurity(e.userAction.actionIntent.intentSender)
                            return@launch
                        }
                    }
                }
            }
            if (deletedAny) {
                refreshVault()
                scanLibrary()
                _uiState.update { it.copy(statusMessage = "Deleted successfully") }
            }
        }
    }

    fun performRename(
        video: VideoItem,
        newTitle: String,
        onRecoverableSecurity: (IntentSender) -> Unit
    ) {
        viewModelScope.launch {
            if (video.isPrivateVault) {
                val ok = vaultRepo.renameInVault(video, newTitle)
                refreshVault()
                _uiState.update {
                    it.copy(statusMessage = if (ok) "Renamed in Private Vault" else "Unable to rename")
                }
                return@launch
            }
            try {
                val ok = videoRepo.renameMediaStoreVideo(video.uri, newTitle, video.displayName)
                if (ok) {
                    scanLibrary()
                    _uiState.update { it.copy(statusMessage = "Video renamed") }
                } else {
                    _uiState.update { it.copy(statusMessage = "Unable to rename video") }
                }
            } catch (e: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
                    onRecoverableSecurity(e.userAction.actionIntent.intentSender)
                } else {
                    _uiState.update { it.copy(statusMessage = "Permission required to rename") }
                }
            }
        }
    }

    fun importDocumentToVault(uri: Uri) {
        viewModelScope.launch {
            val added = vaultRepo.importToVault(uri)
            refreshVault()
            _uiState.update {
                it.copy(
                    statusMessage = if (added != null) {
                        "Added \"${added.title}\" to Private Vault"
                    } else {
                        "Failed to import video"
                    }
                )
            }
        }
    }

    fun moveVideoToVault(
        video: VideoItem,
        onRequestDeleteOriginal: (VideoItem) -> Unit
    ) {
        viewModelScope.launch {
            val imported = vaultRepo.importToVault(video.uri, video.displayName)
            if (imported != null) {
                refreshVault()
                _uiState.update { it.copy(statusMessage = "Secured \"${video.title}\" in Private Vault") }
                if (!video.isPrivateVault && !video.isExternalPick) {
                    onRequestDeleteOriginal(video)
                }
            } else {
                _uiState.update { it.copy(statusMessage = "Could not copy video to Private Vault") }
            }
        }
    }

    fun deleteVaultVideo(video: VideoItem) {
        viewModelScope.launch {
            if (vaultRepo.deleteFromVault(video)) {
                refreshVault()
                _uiState.update { it.copy(statusMessage = "Removed from Private Vault") }
            }
        }
    }

    private fun recomputeDerivedLists(state: LibraryUiState): LibraryUiState {
        val q = state.searchQuery.trim().lowercase()
        val hiddenBuckets = if (state.showHiddenFolders) emptySet() else state.hiddenBucketIds

        val visibleVideos = state.allVideos.filter { it.bucketId !in hiddenBuckets }
        val sortedAll = sortVideos(visibleVideos, state.sortOption, state.sortOrder)
        val filteredAll = if (q.isEmpty()) {
            sortedAll
        } else {
            sortedAll.filter {
                it.title.lowercase().contains(q) ||
                    it.displayName.lowercase().contains(q) ||
                    it.folderName.lowercase().contains(q)
            }
        }

        val visibleFolders = state.folders.filter { it.bucketId !in hiddenBuckets }
        val sortedFolders = sortFolders(
            visibleFolders.map { folder ->
                folder.copy(videos = sortVideos(folder.videos, state.sortOption, state.sortOrder))
            },
            state.sortOption,
            state.sortOrder
        )

        val filteredFolders = if (q.isEmpty()) {
            sortedFolders
        } else {
            sortedFolders.filter { folder ->
                folder.name.lowercase().contains(q) ||
                    folder.videos.any {
                        it.title.lowercase().contains(q) || it.displayName.lowercase().contains(q)
                    }
            }
        }

        val sortedAudio = sortVideos(state.allAudio, state.sortOption, state.sortOrder)
        val filteredAudio = if (q.isEmpty()) {
            sortedAudio
        } else {
            sortedAudio.filter {
                it.title.lowercase().contains(q) ||
                    it.displayName.lowercase().contains(q) ||
                    it.artist.lowercase().contains(q) ||
                    it.album.lowercase().contains(q)
            }
        }

        val lastUri = latestPrefs.lastPlayedUri
        val continueVideo = if (lastUri != null) {
            visibleVideos.find { it.uri.toString() == lastUri }
                ?: state.allAudio.find { it.uri.toString() == lastUri }
        } else {
            null
        }
        val continuePos = if (continueVideo != null) {
            state.positionsByUri[PlaybackPreferencesRepository.uriKey(continueVideo.uri.toString())] ?: 0L
        } else {
            0L
        }

        return state.copy(
            filteredVideos = filteredAll,
            filteredFolders = filteredFolders,
            filteredAudio = filteredAudio,
            continueWatchingVideo = if (continuePos > 3_000L) continueVideo else null,
            continueWatchingPositionMs = continuePos
        )
    }
}
