package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppThemeConfig
import com.example.data.AppThemePreset
import com.example.data.GitHubUpdateRepository
import com.example.data.MediaStoreVideoRepository
import com.example.data.SortOption
import com.example.data.SortOrder
import com.example.data.VideoItem
import com.example.data.computeResolutionBadge
import com.example.data.formatDuration
import com.example.data.formatFileSize
import com.example.data.sortVideos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VeloPlayCoreTest {

    @Test
    fun `manifest declares scoped update network and installer access`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pkgInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val requested = pkgInfo.requestedPermissions?.toList().orEmpty()
        assertTrue("INTERNET is required for update checks and color sync", requested.contains(Manifest.permission.INTERNET))
        assertTrue(
            "ACCESS_NETWORK_STATE is required for update checks",
            requested.contains(Manifest.permission.ACCESS_NETWORK_STATE)
        )
        assertTrue(
            "REQUEST_INSTALL_PACKAGES is required for APK updates",
            requested.contains(Manifest.permission.REQUEST_INSTALL_PACKAGES)
        )
        assertNotNull(
            "FileProvider must expose the update cache through the app authority",
            context.packageManager.resolveContentProvider(
                "${context.packageName}.fileprovider",
                PackageManager.GET_META_DATA
            )
        )
    }

    @Test
    fun `local theme defaults and numeric version comparison are deterministic`() {
        val theme = AppThemeConfig.parse(AppThemeConfig.DEFAULT_JSON)
        assertEquals("Pine Mint", theme.themeName)
        assertEquals("#070A09", theme.background)
        assertEquals("#0F1614", theme.surface)
        assertEquals("#2EE58F", theme.primaryAccent)
        assertEquals("#11785B", theme.secondaryAccent)
        assertEquals(
            listOf("Pine Mint", "Midnight Indigo", "Obsidian Cyan", "Crimson Noir", "Cinema Gold"),
            AppThemePreset.entries.map { it.displayName }
        )
        assertEquals("#6366F1", AppThemePreset.MIDNIGHT_INDIGO.config.primaryAccent)
        assertEquals("#06B6D4", AppThemePreset.OBSIDIAN_CYAN.config.primaryAccent)
        assertEquals("#F43F5E", AppThemePreset.CRIMSON_NOIR.config.primaryAccent)
        assertEquals("#F59E0B", AppThemePreset.CINEMA_GOLD.config.primaryAccent)
        assertTrue(GitHubUpdateRepository.isVersionNewer("v1.3", "1.2"))
        assertTrue(GitHubUpdateRepository.isVersionNewer("1.10.0", "1.9.9"))
        assertFalse(GitHubUpdateRepository.isVersionNewer("1.2", "1.2.0"))
        assertFalse(GitHubUpdateRepository.isVersionNewer("1.1", "1.2"))
    }

    @Test
    fun `resolution badges and formatters work accurately`() {
        assertEquals("4K", computeResolutionBadge(3840, 2160))
        assertEquals("1080p", computeResolutionBadge(1920, 1080))
        assertEquals("720p", computeResolutionBadge(1280, 720))
        assertEquals("02:05", formatDuration(125_000L))
        assertEquals("1:01:05", formatDuration(3_665_000L))
        assertEquals("1.50 GB", formatFileSize((1.5 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun `supported video formats include mp4 mkv avi and webm`() {
        assertTrue(MediaStoreVideoRepository.isSupportedVideoFormat("mp4", "video/mp4"))
        assertTrue(MediaStoreVideoRepository.isSupportedVideoFormat("mkv", "video/x-matroska"))
        assertTrue(MediaStoreVideoRepository.isSupportedVideoFormat("avi", "video/x-msvideo"))
        assertTrue(MediaStoreVideoRepository.isSupportedVideoFormat("webm", "video/webm"))
        assertFalse(MediaStoreVideoRepository.isSupportedVideoFormat("jpg", "image/jpeg"))
    }

    @Test
    fun `sortVideos sorts by name date and size`() {
        val items = listOf(
            VideoItem(id = 1, uri = Uri.EMPTY, title = "Beta", displayName = "Beta.mp4", bucketId = "1", folderName = "Movies", folderPath = "", durationMs = 1000, sizeBytes = 500, width = 1920, height = 1080, dateModifiedSec = 10, mimeType = "video/mp4", extension = "MP4"),
            VideoItem(id = 2, uri = Uri.EMPTY, title = "Alpha", displayName = "Alpha.mkv", bucketId = "1", folderName = "Movies", folderPath = "", durationMs = 2000, sizeBytes = 900, width = 1920, height = 1080, dateModifiedSec = 30, mimeType = "video/x-matroska", extension = "MKV"),
            VideoItem(id = 3, uri = Uri.EMPTY, title = "Gamma", displayName = "Gamma.avi", bucketId = "1", folderName = "Movies", folderPath = "", durationMs = 3000, sizeBytes = 200, width = 1280, height = 720, dateModifiedSec = 20, mimeType = "video/avi", extension = "AVI")
        )

        assertEquals(
            listOf("Alpha", "Beta", "Gamma"),
            sortVideos(items, SortOption.NAME, SortOrder.ASCENDING).map { it.title }
        )
        assertEquals(
            listOf("Alpha", "Gamma", "Beta"),
            sortVideos(items, SortOption.DATE, SortOrder.DESCENDING).map { it.title }
        )
        assertEquals(
            listOf("Gamma", "Beta", "Alpha"),
            sortVideos(items, SortOption.SIZE, SortOrder.ASCENDING).map { it.title }
        )
    }

    @Test
    fun `private vault PIN hashing verifies matching pins and rejects mismatches`() {
        val stored = com.example.data.PlaybackPreferencesRepository.hashPin("2580")
        assertTrue(com.example.data.PlaybackPreferencesRepository.verifyPin("2580", stored))
        assertFalse(com.example.data.PlaybackPreferencesRepository.verifyPin("1234", stored))
        assertFalse(com.example.data.PlaybackPreferencesRepository.verifyPin("2580", null))
    }
}
