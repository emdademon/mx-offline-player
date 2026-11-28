package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.player.SubtitleFontSize
import com.example.player.SubtitleStyleConfig
import com.example.player.SubtitleTextColor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.altPlayerDataStore: DataStore<Preferences> by preferencesDataStore(name = "altplayer_prefs")

data class PlaybackPreferencesSnapshot(
    val sortOption: SortOption = SortOption.DATE,
    val sortOrder: SortOrder = SortOrder.DESCENDING,
    val isGridView: Boolean = false,
    val appThemePreset: AppThemePreset = AppThemePreset.PINE_MINT,
    val resumeBehavior: ResumeBehavior = ResumeBehavior.ASK,
    val defaultOrientation: DefaultOrientation = DefaultOrientation.AUTO,
    val autoLoadSrt: Boolean = true,
    val showHiddenFolders: Boolean = false,
    val hiddenBucketIds: Set<String> = emptySet(),
    val privatePinHash: String? = null,
    val subtitleStyle: SubtitleStyleConfig = SubtitleStyleConfig(),
    val proLicense: ProLicenseState = ProLicenseState(),
    val lastPlayedUri: String? = null,
    val positionsByUri: Map<String, Long> = emptyMap(),
    val watchedUris: Set<String> = emptySet()
)

class PlaybackPreferencesRepository(private val context: Context) {

    private object Keys {
        val SORT_OPTION = stringPreferencesKey("sort_option")
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val IS_GRID_VIEW = booleanPreferencesKey("is_grid_view")
        val APP_THEME_PRESET = stringPreferencesKey("app_theme_preset")
        val RESUME_BEHAVIOR = stringPreferencesKey("resume_behavior")
        val DEFAULT_ORIENTATION = stringPreferencesKey("default_orientation")
        val AUTO_LOAD_SRT = booleanPreferencesKey("auto_load_srt")
        val SHOW_HIDDEN_FOLDERS = booleanPreferencesKey("show_hidden_folders")
        val HIDDEN_BUCKET_IDS = stringSetPreferencesKey("hidden_bucket_ids")
        val PRIVATE_PIN_HASH = stringPreferencesKey("private_pin_hash")
        val SUBTITLE_FONT_SIZE = stringPreferencesKey("sub_font_size")
        val SUBTITLE_TEXT_COLOR = stringPreferencesKey("sub_text_color")
        val SUBTITLE_BG_OPACITY = floatPreferencesKey("sub_bg_opacity")
        // PRO_UNLOCK_TYPE is retained as a legacy source for existing installations.
        val PRO_UNLOCK_TYPE = stringPreferencesKey("pro_unlock_type")
        val PRO_LICENSE_TIER = stringPreferencesKey("pro_license_tier")
        val PRO_IS_UNLOCKED = booleanPreferencesKey("pro_is_unlocked")
        val PRO_ACTIVATED_CODE = stringPreferencesKey("pro_activated_code")
        val TRIAL_STARTED_AT_MS = longPreferencesKey("trial_started_at_ms")
        val HAS_SEEN_ACTIVATION = booleanPreferencesKey("has_seen_activation")
        val LAST_PLAYED_URI = stringPreferencesKey("last_played_uri")
    }

    val preferencesFlow: Flow<PlaybackPreferencesSnapshot> = context.altPlayerDataStore.data.map { prefs ->
        val sortOption = prefs[Keys.SORT_OPTION]?.let {
            runCatching { SortOption.valueOf(it) }.getOrNull()
        } ?: SortOption.DATE
        val sortOrder = prefs[Keys.SORT_ORDER]?.let {
            runCatching { SortOrder.valueOf(it) }.getOrNull()
        } ?: SortOrder.DESCENDING
        val resumeBehavior = prefs[Keys.RESUME_BEHAVIOR]?.let {
            runCatching { ResumeBehavior.valueOf(it) }.getOrNull()
        } ?: ResumeBehavior.ASK
        val defaultOrientation = prefs[Keys.DEFAULT_ORIENTATION]?.let {
            runCatching { DefaultOrientation.valueOf(it) }.getOrNull()
        } ?: DefaultOrientation.AUTO
        val appThemePreset = AppThemePreset.fromStoredValue(prefs[Keys.APP_THEME_PRESET])

        val subSize = prefs[Keys.SUBTITLE_FONT_SIZE]?.let {
            runCatching { SubtitleFontSize.valueOf(it) }.getOrNull()
        } ?: SubtitleFontSize.MEDIUM
        val subColor = prefs[Keys.SUBTITLE_TEXT_COLOR]?.let {
            runCatching { SubtitleTextColor.valueOf(it) }.getOrNull()
        } ?: SubtitleTextColor.WHITE
        val subOpacity = prefs[Keys.SUBTITLE_BG_OPACITY] ?: 0.72f

        val positions = mutableMapOf<String, Long>()
        val watched = mutableSetOf<String>()
        prefs.asMap().forEach { (key, value) ->
            val keyName = key.name
            if (keyName.startsWith(POS_PREFIX) && value is Long) {
                val uriKey = keyName.removePrefix(POS_PREFIX)
                positions[uriKey] = value
                watched.add(uriKey)
            }
        }

        val legacyUnlockType = prefs[Keys.PRO_UNLOCK_TYPE]?.let {
            runCatching { ProUnlockType.valueOf(it) }.getOrNull()
        } ?: ProUnlockType.NONE
        val activatedCode = prefs[Keys.PRO_ACTIVATED_CODE].orEmpty()
        val storedTier = LicenseTier.fromStoredValue(prefs[Keys.PRO_LICENSE_TIER])?.value
        // Older builds stored FAMILY_VIP/PRO_CODE/TRIAL in PRO_UNLOCK_TYPE only. Resolve those
        // records into the new canonical tier before exposing the state to the UI.
        val resolvedTier = storedTier
            ?: legacyUnlockType.licenseTier
            ?: ProLicenseManager.licenseTierForActivatedCode(activatedCode)
        val resolvedUnlockType = when (resolvedTier) {
            LicenseTier.FAMILY_VIP.value -> ProUnlockType.FAMILY_VIP
            LicenseTier.PRO_BUYER.value -> ProUnlockType.PRO_CODE
            LicenseTier.TRIAL.value -> ProUnlockType.TRIAL
            else -> legacyUnlockType
        }
        val isUnlocked = prefs[Keys.PRO_IS_UNLOCKED] ?: (resolvedUnlockType != ProUnlockType.NONE)
        val proLicense = ProLicenseState(
            unlockType = resolvedUnlockType,
            activatedCode = activatedCode,
            trialStartedAtMs = prefs[Keys.TRIAL_STARTED_AT_MS] ?: 0L,
            hasSeenActivation = prefs[Keys.HAS_SEEN_ACTIVATION] ?: isUnlocked,
            licenseTier = resolvedTier,
            isUnlocked = isUnlocked
        )

        PlaybackPreferencesSnapshot(
            sortOption = sortOption,
            sortOrder = sortOrder,
            isGridView = prefs[Keys.IS_GRID_VIEW] ?: false,
            appThemePreset = appThemePreset,
            resumeBehavior = resumeBehavior,
            defaultOrientation = defaultOrientation,
            autoLoadSrt = prefs[Keys.AUTO_LOAD_SRT] ?: true,
            showHiddenFolders = prefs[Keys.SHOW_HIDDEN_FOLDERS] ?: false,
            hiddenBucketIds = prefs[Keys.HIDDEN_BUCKET_IDS] ?: emptySet(),
            privatePinHash = prefs[Keys.PRIVATE_PIN_HASH],
            subtitleStyle = SubtitleStyleConfig(
                fontSize = subSize,
                textColor = subColor,
                bgOpacity = subOpacity
            ),
            proLicense = proLicense,
            lastPlayedUri = prefs[Keys.LAST_PLAYED_URI],
            positionsByUri = positions,
            watchedUris = watched
        )
    }

    suspend fun saveSortPreference(option: SortOption, order: SortOrder) {
        context.altPlayerDataStore.edit { prefs ->
            prefs[Keys.SORT_OPTION] = option.name
            prefs[Keys.SORT_ORDER] = order.name
        }
    }

    suspend fun saveIsGridView(isGrid: Boolean) {
        context.altPlayerDataStore.edit { prefs ->
            prefs[Keys.IS_GRID_VIEW] = isGrid
        }
    }

    suspend fun saveResumeBehavior(behavior: ResumeBehavior) {
        context.altPlayerDataStore.edit { prefs ->
            prefs[Keys.RESUME_BEHAVIOR] = behavior.name
        }
    }

    suspend fun saveAppThemePreset(preset: AppThemePreset) {
        context.altPlayerDataStore.edit { prefs ->
            prefs[Keys.APP_THEME_PRESET] = preset.name
        }
    }

    suspend fun saveDefaultOrientation(orientation: DefaultOrientation) {
        context.altPlayerDataStore.edit { prefs ->
            prefs[Keys.DEFAULT_ORIENTATION] = orientation.name
        }
    }

    suspend fun saveAutoLoadSrt(enabled: Boolean) {
        context.altPlayerDataStore.edit { prefs ->
            prefs[Keys.AUTO_LOAD_SRT] = enabled
        }
    }

    suspend fun saveShowHiddenFolders(show: Boolean) {
        context.altPlayerDataStore.edit { prefs ->
            prefs[Keys.SHOW_HIDDEN_FOLDERS] = show
        }
    }

    suspend fun setFolderHidden(bucketId: String, hidden: Boolean) {
        context.altPlayerDataStore.edit { prefs ->
            val current = prefs[Keys.HIDDEN_BUCKET_IDS]?.toMutableSet() ?: mutableSetOf()
            if (hidden) current.add(bucketId) else current.remove(bucketId)
            prefs[Keys.HIDDEN_BUCKET_IDS] = current
        }
    }

    suspend fun saveSubtitleStyle(style: SubtitleStyleConfig) {
        context.altPlayerDataStore.edit { prefs ->
            prefs[Keys.SUBTITLE_FONT_SIZE] = style.fontSize.name
            prefs[Keys.SUBTITLE_TEXT_COLOR] = style.textColor.name
            prefs[Keys.SUBTITLE_BG_OPACITY] = style.bgOpacity
        }
    }

    suspend fun activateProWithCode(rawCode: String): ProActivationResult {
        val result = ProLicenseManager.validateCode(rawCode)
        if (result is ProActivationResult.Success) {
            context.altPlayerDataStore.edit { prefs ->
                // Keep the old enum for compatibility, but make the exact tier and unlock state
                // explicit for all new activations.
                prefs[Keys.PRO_UNLOCK_TYPE] = result.unlockType.name
                prefs[Keys.PRO_LICENSE_TIER] = result.licenseTier
                prefs[Keys.PRO_IS_UNLOCKED] = true
                prefs[Keys.PRO_ACTIVATED_CODE] = result.normalizedCode
                prefs[Keys.HAS_SEEN_ACTIVATION] = true
            }
        }
        return result
    }

    suspend fun startThreeDayTrial(nowMs: Long = System.currentTimeMillis()) {
        context.altPlayerDataStore.edit { prefs ->
            val existingStart = prefs[Keys.TRIAL_STARTED_AT_MS] ?: 0L
            if (existingStart <= 0L) {
                prefs[Keys.TRIAL_STARTED_AT_MS] = nowMs
            }
            prefs[Keys.PRO_UNLOCK_TYPE] = ProUnlockType.TRIAL.name
            prefs[Keys.PRO_LICENSE_TIER] = LicenseTier.TRIAL.value
            prefs[Keys.PRO_IS_UNLOCKED] = true
            prefs[Keys.HAS_SEEN_ACTIVATION] = true
        }
    }

    suspend fun setPrivatePin(pin: String) {
        context.altPlayerDataStore.edit { prefs ->
            prefs[Keys.PRIVATE_PIN_HASH] = hashPin(pin)
        }
    }

    suspend fun clearPrivatePin() {
        context.altPlayerDataStore.edit { prefs ->
            prefs.remove(Keys.PRIVATE_PIN_HASH)
        }
    }

    suspend fun savePlaybackProgress(uriString: String, positionMs: Long, durationMs: Long) {
        val safeUriKey = uriKey(uriString)
        context.altPlayerDataStore.edit { prefs ->
            prefs[Keys.LAST_PLAYED_URI] = uriString
            val nearEnd = durationMs > 0 && positionMs >= (durationMs - 3_000L).coerceAtLeast(0L)
            prefs[longPreferencesKey(POS_PREFIX + safeUriKey)] = if (nearEnd) 0L else positionMs.coerceAtLeast(0L)
        }
    }

    companion object {
        private const val POS_PREFIX = "pos_"
        private const val PIN_SALT = "altplayer_local_vault_v1"

        fun uriKey(uriString: String): String = uriString.hashCode().toUInt().toString(16)

        fun hashPin(pin: String): String {
            val bytes = MessageDigest.getInstance("SHA-256")
                .digest("$PIN_SALT:$pin".toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }

        fun verifyPin(pin: String, storedHash: String?): Boolean {
            if (storedHash.isNullOrBlank()) return false
            return hashPin(pin) == storedHash
        }
    }
}
