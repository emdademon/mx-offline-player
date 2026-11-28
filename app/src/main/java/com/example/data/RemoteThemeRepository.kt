package com.example.data

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** A validated color palette that is safe to keep separate from local media state. */
data class AppThemeConfig(
    val themeName: String,
    val background: String,
    val surface: String,
    val border: String,
    val primaryAccent: String,
    val secondaryAccent: String,
    val textPrimary: String,
    val textSecondary: String
) {
    companion object {
        const val DEFAULT_JSON = """
{
  "themeName": "Pine Mint",
  "background": "#070A09",
  "surface": "#0F1614",
  "border": "#1C3329",
  "primaryAccent": "#2EE58F",
  "secondaryAccent": "#11785B",
  "textPrimary": "#F2FFF8",
  "textSecondary": "#9DB7AA"
}
"""

        val DEFAULT = AppThemeConfig(
            themeName = "Pine Mint",
            background = "#070A09",
            surface = "#0F1614",
            border = "#1C3329",
            primaryAccent = "#2EE58F",
            secondaryAccent = "#11785B",
            textPrimary = "#F2FFF8",
            textSecondary = "#9DB7AA"
        )

        private val hexColorPattern = Regex("^#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?$")

        /** Returns the built-in palette if a downloaded document is malformed. */
        fun fromJson(rawJson: String?): AppThemeConfig =
            runCatching { parse(rawJson.orEmpty()) }.getOrElse { DEFAULT }

        /** Strict parser used before a downloaded document is persisted. */
        fun parse(rawJson: String): AppThemeConfig {
            val json = JSONObject(rawJson)
            fun requiredName(key: String): String {
                val value = json.optString(key).trim()
                require(value.isNotEmpty()) { "Missing theme field: $key" }
                return value
            }

            fun requiredColor(key: String): String {
                val value = requiredName(key).uppercase()
                require(hexColorPattern.matches(value)) { "Invalid theme color: $key" }
                return value
            }

            return AppThemeConfig(
                themeName = requiredName("themeName"),
                background = requiredColor("background"),
                surface = requiredColor("surface"),
                border = requiredColor("border"),
                primaryAccent = requiredColor("primaryAccent"),
                secondaryAccent = requiredColor("secondaryAccent"),
                textPrimary = requiredColor("textPrimary"),
                textSecondary = requiredColor("textSecondary")
            )
        }
    }
}

enum class AppThemePreset(
    val id: String,
    val displayName: String,
    val config: AppThemeConfig
) {
    PINE_MINT(
        id = "pine_mint",
        displayName = "Pine Mint",
        config = AppThemeConfig.DEFAULT
    ),
    MIDNIGHT_INDIGO(
        id = "midnight_indigo",
        displayName = "Midnight Indigo",
        config = AppThemeConfig(
            themeName = "Midnight Indigo",
            background = "#090A0F",
            surface = "#12151E",
            border = "#1E2433",
            primaryAccent = "#6366F1",
            secondaryAccent = "#4338CA",
            textPrimary = "#F8FAFC",
            textSecondary = "#A5B4FC"
        )
    ),
    OBSIDIAN_CYAN(
        id = "obsidian_cyan",
        displayName = "Obsidian Cyan",
        config = AppThemeConfig(
            themeName = "Obsidian Cyan",
            background = "#070B0D",
            surface = "#0D171B",
            border = "#17313A",
            primaryAccent = "#06B6D4",
            secondaryAccent = "#0E7490",
            textPrimary = "#ECFEFF",
            textSecondary = "#A5F3FC"
        )
    ),
    CRIMSON_NOIR(
        id = "crimson_noir",
        displayName = "Crimson Noir",
        config = AppThemeConfig(
            themeName = "Crimson Noir",
            background = "#0E080A",
            surface = "#1A0F12",
            border = "#3A1A22",
            primaryAccent = "#F43F5E",
            secondaryAccent = "#9F1239",
            textPrimary = "#FFF1F2",
            textSecondary = "#FDA4AF"
        )
    ),
    CINEMA_GOLD(
        id = "cinema_gold",
        displayName = "Cinema Gold",
        config = AppThemeConfig(
            themeName = "Cinema Gold",
            background = "#100D06",
            surface = "#1A160C",
            border = "#3B2C12",
            primaryAccent = "#F59E0B",
            secondaryAccent = "#B45309",
            textPrimary = "#FFFBEB",
            textSecondary = "#FCD34D"
        )
    );

    companion object {
        fun fromStoredValue(value: String?): AppThemePreset =
            entries.firstOrNull { it.name == value || it.id == value } ?: PINE_MINT
    }
}

data class ThemePreferences(
    /** Kept for migration compatibility with older builds; local presets are always authoritative. */
    val useRemoteTheme: Boolean = false,
    val themeJson: String = AppThemeConfig.DEFAULT_JSON,
    val lastError: String? = null
) {
    val activeTheme: AppThemeConfig
        get() = AppThemeConfig.DEFAULT
}

private val Context.altPlayerThemeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "altplayer_theme_prefs"
)

/**
 * Stores the optional color document separately from local media repositories. The app's active
 * palette is selected from local presets, so a downloaded document can never replace that choice.
 */
class RemoteThemeRepository(private val context: Context) {
    private object Keys {
        val USE_REMOTE_THEME = booleanPreferencesKey("use_remote_theme")
        val THEME_JSON = stringPreferencesKey("theme_json")
        val LAST_ERROR = stringPreferencesKey("theme_last_error")
    }

    val preferencesFlow: Flow<ThemePreferences> = context.altPlayerThemeDataStore.data.map { prefs ->
        ThemePreferences(
            useRemoteTheme = prefs[Keys.USE_REMOTE_THEME] ?: false,
            themeJson = prefs[Keys.THEME_JSON] ?: AppThemeConfig.DEFAULT_JSON,
            lastError = prefs[Keys.LAST_ERROR]
        )
    }

    suspend fun setRemoteThemeEnabled(enabled: Boolean) {
        context.altPlayerThemeDataStore.edit { prefs ->
            prefs[Keys.USE_REMOTE_THEME] = enabled
            if (!enabled) prefs.remove(Keys.LAST_ERROR)
        }
    }

    suspend fun fetchAndStoreRemoteTheme(): Result<AppThemeConfig> {
        return try {
            val rawJson = withContext(Dispatchers.IO) { downloadThemeDocument() }
            val theme = AppThemeConfig.parse(rawJson)
            context.altPlayerThemeDataStore.edit { prefs ->
                prefs[Keys.THEME_JSON] = rawJson
                prefs.remove(Keys.LAST_ERROR)
            }
            Result.success(theme)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            val message = error.message?.takeIf { it.isNotBlank() } ?: "Theme sync failed"
            context.altPlayerThemeDataStore.edit { prefs ->
                prefs[Keys.LAST_ERROR] = message.take(160)
            }
            Result.failure(error)
        }
    }

    private fun downloadThemeDocument(): String {
        val connection = (URL(THEME_API_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = NETWORK_TIMEOUT_MS
            readTimeout = NETWORK_TIMEOUT_MS
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Alt-Player-Theme-Sync")
        }

        return try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IOException("Theme request failed ($responseCode)")
            }
            require(connection.url.protocol == "https" && connection.url.host == "api.github.com") {
                "Theme request redirected outside the expected host"
            }
            val apiResponse = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val encodedContent = JSONObject(apiResponse).optString("content").replace("\\s".toRegex(), "")
            require(encodedContent.isNotBlank()) { "Theme response contained no content" }
            String(Base64.decode(encodedContent, Base64.DEFAULT), Charsets.UTF_8)
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val THEME_API_URL =
            "https://api.github.com/repos/emdademon/Alt-Player/contents/theme.json?ref=main"
        private const val NETWORK_TIMEOUT_MS = 8_000
    }
}
