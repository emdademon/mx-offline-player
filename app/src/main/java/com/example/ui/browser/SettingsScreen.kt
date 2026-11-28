package com.example.ui.browser

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.data.AppThemePreset
import com.example.data.DefaultOrientation
import com.example.data.LicenseTier
import com.example.data.ProActivationResult
import com.example.data.ProLicenseManager
import com.example.data.ProUnlockType
import com.example.data.ResumeBehavior
import com.example.data.UpdateStatus
import com.example.ui.components.AltPlayerLogo
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.CardOutline
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.DeepNavySurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedNavy
import com.example.ui.theme.EmeraldOffline
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.OnCyanContainer
import com.example.ui.theme.OnVioletContainer
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletAccent
import com.example.ui.theme.VioletContainer


private fun presetSurfaceColor(preset: AppThemePreset): Color =
    Color(AndroidColor.parseColor(preset.config.surface))

@Composable
fun SettingsScreen(
    uiState: LibraryUiState,
    onSelectResumeBehavior: (ResumeBehavior) -> Unit,
    onSelectDefaultOrientation: (DefaultOrientation) -> Unit,
    onToggleAutoLoadSrt: (Boolean) -> Unit,
    onToggleShowHiddenFolders: (Boolean) -> Unit,
    onOpenChangePin: () -> Unit,
    onClearPin: () -> Unit,
    onActivateProCode: (String, (ProActivationResult) -> Unit) -> Unit,
    onGenerateBuyerCode: () -> String,
    onSelectAppTheme: (AppThemePreset) -> Unit,
    onCheckForUpdates: () -> Unit,
    onDownloadAndInstallUpdate: () -> Unit,
    onBack: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val canUseDeveloperGenerator =
        uiState.proLicense.isUnlocked &&
            uiState.proLicense.licenseTier == LicenseTier.FAMILY_VIP.value
    var aboutTapCount by remember { mutableIntStateOf(0) }
    var showDevCodeGenerator by remember { mutableStateOf(false) }
    var generatedCode by remember { mutableStateOf("") }
    var copiedNotice by remember { mutableStateOf(false) }
    var showEnterCodeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(canUseDeveloperGenerator) {
        if (!canUseDeveloperGenerator) {
            // Do not retain a developer panel, tap count, or generated code after the tier changes.
            aboutTapCount = 0
            showDevCodeGenerator = false
            generatedCode = ""
            copiedNotice = false
        }
    }

    fun onAboutTap() {
        if (!canUseDeveloperGenerator) return
        aboutTapCount++
        if (aboutTapCount >= 5) {
            showDevCodeGenerator = true
            if (generatedCode.isBlank()) {
                generatedCode = onGenerateBuyerCode()
            }
        }
    }

    Scaffold(
        containerColor = AmoledBlack,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(ElevatedNavy, AmoledBlack, AmoledBlack)
                    )
                )
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = TextPrimary
                    )
                }
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Playback Preferences: Resume Behavior
                SettingsSectionHeader(
                    icon = Icons.Rounded.PlayCircleOutline,
                    title = "Default Resume Behavior"
                )
                Surface(
                    color = DeepNavySurface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, CardOutline)
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ResumeBehavior.entries.forEach { behavior ->
                            val selected = uiState.resumeBehavior == behavior
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelectResumeBehavior(behavior) }
                                    .testTag("resume_behavior_${behavior.name.lowercase()}"),
                                color = if (selected) CyanContainer else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = behavior.label,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (selected) OnCyanContainer else TextPrimary
                                        )
                                        Text(
                                            text = behavior.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (selected) ElectricCyan else TextMuted
                                        )
                                    }
                                    if (selected) {
                                        Icon(Icons.Rounded.Check, contentDescription = null, tint = ElectricCyan)
                                    }
                                }
                            }
                        }
                    }
                }

                // Default Screen Orientation
                SettingsSectionHeader(
                    icon = Icons.Rounded.ScreenRotation,
                    title = "Default Screen Orientation"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DefaultOrientation.entries.forEach { orientation ->
                        val selected = uiState.defaultOrientation == orientation
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectDefaultOrientation(orientation) }
                                .testTag("default_orientation_${orientation.name.lowercase()}"),
                            color = if (selected) VioletContainer else DeepNavySurface,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, if (selected) ElectricCyan else CardOutline)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = orientation.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) OnVioletContainer else TextSecondary,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Subtitles & Folders Toggles
                SettingsSectionHeader(
                    icon = Icons.Rounded.Subtitles,
                    title = "Subtitles & Folders"
                )
                Surface(
                    color = DeepNavySurface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, CardOutline)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto-load matching .srt subtitles",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Automatically attach same-named .srt files when available",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = uiState.autoLoadSrt,
                                onCheckedChange = onToggleAutoLoadSrt,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AmoledBlack,
                                    checkedTrackColor = ElectricCyan
                                ),
                                modifier = Modifier.testTag("switch_auto_load_srt")
                            )
                        }

                        HorizontalDivider(color = CardOutline)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Show Hidden Folders",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                val hiddenCount = uiState.hiddenBucketIds.size
                                Text(
                                    text = if (hiddenCount == 0) {
                                        "No folders are currently hidden"
                                    } else {
                                        "$hiddenCount hidden folder(s) on this device"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = uiState.showHiddenFolders,
                                onCheckedChange = onToggleShowHiddenFolders,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AmoledBlack,
                                    checkedTrackColor = ElectricCyan
                                ),
                                modifier = Modifier.testTag("switch_show_hidden_folders")
                            )
                        }
                    }
                }

                // Private Vault PIN Security
                SettingsSectionHeader(
                    icon = Icons.Rounded.Lock,
                    title = "Private Vault Security"
                )
                Surface(
                    color = DeepNavySurface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, CardOutline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (uiState.hasPrivatePin) "4-Digit Vault PIN Active" else "Set Private Vault PIN",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Stored locally via SHA-256 hash",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                        Row {
                            TextButton(onClick = onOpenChangePin) {
                                Text(
                                    text = if (uiState.hasPrivatePin) "Change" else "Set PIN",
                                    color = ElectricCyan
                                )
                            }
                            if (uiState.hasPrivatePin && uiState.vaultVideos.isEmpty()) {
                                TextButton(onClick = onClearPin) {
                                    Text("Clear", color = TextSecondary)
                                }
                            }
                        }
                    }
                }

                // Pro License Status
                SettingsSectionHeader(
                    icon = Icons.Rounded.Key,
                    title = "Alt Player Pro License"
                )
                Surface(
                    color = DeepNavySurface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, CardOutline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val statusTitle = when (uiState.proLicense.unlockType) {
                                ProUnlockType.FAMILY_VIP -> "Family VIP Lifetime Active"
                                ProUnlockType.PRO_CODE -> "Pro Lifetime Licensed"
                                ProUnlockType.TRIAL -> {
                                    val rem = ProLicenseManager.formatRemainingTrial(
                                        uiState.proLicense.remainingTrialMs()
                                    )
                                    "3-Day Free Trial ($rem)"
                                }
                                ProUnlockType.NONE -> "Not Activated"
                            }
                            Text(
                                text = statusTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = if (uiState.proLicense.isLifetimePro()) {
                                    "Verified offline • Code ${uiState.proLicense.activatedCode}"
                                } else {
                                    "Enter a buyer or family code to unlock forever"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                        if (!uiState.proLicense.isLifetimePro()) {
                            TextButton(
                                onClick = { showEnterCodeDialog = true },
                                modifier = Modifier.testTag("settings_enter_pro_code")
                            ) {
                                Text("Activate", color = ElectricCyan)
                            }
                        }
                    }
                }

                // App Theme
                SettingsSectionHeader(
                    icon = Icons.Rounded.Refresh,
                    title = "App Theme"
                )
                Surface(
                    color = DeepNavySurface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, CardOutline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Choose a color style for the player",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                        AppThemePreset.entries.forEach { preset ->
                            val selected = uiState.appThemePreset == preset
                            val accent = Color(AndroidColor.parseColor(preset.config.primaryAccent))
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onSelectAppTheme(preset) }
                                    .testTag("app_theme_${preset.id}"),
                                color = if (selected) presetSurfaceColor(preset) else Color.Transparent,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) accent else CardOutline
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.size(18.dp),
                                        color = accent,
                                        shape = RoundedCornerShape(9.dp)
                                    ) {}
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = preset.displayName,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = preset.config.primaryAccent,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (selected) accent else TextMuted
                                        )
                                    }
                                    if (selected) {
                                        Icon(Icons.Rounded.Check, contentDescription = null, tint = accent)
                                    }
                                }
                            }
                        }
                    }
                }

                // App Updates
                SettingsSectionHeader(
                    icon = Icons.Rounded.VerifiedUser,
                    title = "Check for Updates"
                )
                Surface(
                    color = DeepNavySurface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, CardOutline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Check for Updates",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Check for the latest version and improvements",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                        uiState.appUpdate.message?.let { message ->
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (uiState.appUpdate.status == UpdateStatus.ERROR) {
                                    CoralError
                                } else {
                                    TextMuted
                                }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (uiState.appUpdate.status == UpdateStatus.DOWNLOADING) {
                                CircularProgressIndicator(
                                    color = ElectricCyan,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                            }
                            TextButton(
                                onClick = onCheckForUpdates,
                                enabled = uiState.appUpdate.status != UpdateStatus.CHECKING &&
                                    uiState.appUpdate.status != UpdateStatus.DOWNLOADING,
                                modifier = Modifier.testTag("check_for_updates")
                            ) {
                                Text("Check", color = ElectricCyan)
                            }
                            if (uiState.appUpdate.availableUpdate != null) {
                                Button(
                                    onClick = onDownloadAndInstallUpdate,
                                    enabled = uiState.appUpdate.status != UpdateStatus.DOWNLOADING,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ElectricCyan,
                                        contentColor = AmoledBlack
                                    ),
                                    modifier = Modifier.testTag("download_update_button")
                                ) {
                                    Text(
                                        text = if (uiState.appUpdate.status == UpdateStatus.DOWNLOADING) {
                                            "Downloading ${uiState.appUpdate.downloadProgress}%"
                                        } else {
                                            "Download & Install"
                                        },
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // About Card (tap version 5x to reveal offline Buyer Code Generator)
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("about_card"),
                    color = ElevatedNavy,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.28f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AltPlayerLogo(modifier = Modifier.size(44.dp))
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .then(
                                        if (canUseDeveloperGenerator) {
                                            Modifier.clickable { onAboutTap() }
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .testTag("about_version_tap_target")
                            ) {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.VerifiedUser,
                                        contentDescription = null,
                                        tint = EmeraldOffline,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "v${BuildConfig.VERSION_NAME} • ${stringResource(R.string.offline_privacy_badge)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldOffline
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = CardOutline)

                        Text(
                            text = stringResource(R.string.about_footer_text),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium,
                            modifier = if (canUseDeveloperGenerator) {
                                Modifier.clickable { onAboutTap() }
                            } else {
                                Modifier
                            }
                        )

                        if (canUseDeveloperGenerator && showDevCodeGenerator) {
                            HorizontalDivider(color = CardOutline)
                            Surface(
                                color = DeepNavySurface,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, VioletAccent.copy(alpha = 0.45f)),
                                modifier = Modifier.testTag("dev_code_generator_panel")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Offline Buyer Code Generator (Developer)",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = VioletAccent
                                    )
                                    Surface(
                                        color = AmoledBlack,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, CardOutline)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = generatedCode,
                                                fontFamily = JetBrainsMonoFamily,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = TextPrimary,
                                                modifier = Modifier.testTag("generated_buyer_code_text")
                                            )
                                            IconButton(
                                                onClick = {
                                                    clipboard.setText(AnnotatedString(generatedCode))
                                                    copiedNotice = true
                                                },
                                                modifier = Modifier.testTag("copy_generated_code_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.ContentCopy,
                                                    contentDescription = "Copy code",
                                                    tint = ElectricCyan,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (copiedNotice) "Copied to clipboard" else "Family VIP: ${ProLicenseManager.MASTER_FAMILY_CODE}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (copiedNotice) ElectricCyan else TextMuted
                                        )
                                        OutlinedButton(
                                            onClick = {
                                                generatedCode = onGenerateBuyerCode()
                                                copiedNotice = false
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, CardOutline),
                                            modifier = Modifier.testTag("generate_new_buyer_code_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Refresh,
                                                contentDescription = null,
                                                tint = ElectricCyan,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("New Code", color = TextPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showEnterCodeDialog) {
        var codeInput by remember { mutableStateOf("") }
        var errorText by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showEnterCodeDialog = false },
            containerColor = DeepNavySurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text(stringResource(R.string.activate_pro_title), style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = {
                            codeInput = it.uppercase()
                            errorText = null
                        },
                        singleLine = true,
                        label = { Text(stringResource(R.string.enter_activation_code)) },
                        placeholder = { Text("ALT-PRO-XXXX") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = CardOutline,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedLabelColor = ElectricCyan
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorText != null) {
                        Text(text = errorText!!, color = CoralError, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onActivateProCode(codeInput) { res ->
                            if (res is ProActivationResult.Success) {
                                showEnterCodeDialog = false
                            } else {
                                errorText = "Invalid activation code."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = AmoledBlack)
                ) {
                    Text("Activate", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEnterCodeDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = ElectricCyan
        )
    }
}
