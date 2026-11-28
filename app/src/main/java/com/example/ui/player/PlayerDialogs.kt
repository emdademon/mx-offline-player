package com.example.ui.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.ClosedCaptionDisabled
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.player.AudioTrackOption
import com.example.player.EqPreset
import com.example.player.EqualizerUiState
import com.example.player.ExternalSubtitleTrack
import com.example.player.PLAYBACK_SPEEDS
import com.example.player.PlayerMath
import com.example.player.SubtitleFontSize
import com.example.player.SubtitleSelection
import com.example.player.SubtitleStyleConfig
import com.example.player.SubtitleTextColor
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.CardOutline
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.DeepNavySurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedNavy
import com.example.ui.theme.GoogleSansFamily
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LuminousMint
import com.example.ui.theme.OnCyanContainer
import com.example.ui.theme.OnVioletContainer
import com.example.ui.theme.TealEmeraldDeep
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletContainer
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackSelectorSheet(
    audioTracks: List<AudioTrackOption>,
    embeddedSubtitles: List<SubtitleSelection.Embedded>,
    externalSubtitles: List<ExternalSubtitleTrack>,
    selectedExternalId: String?,
    subtitlesDisabled: Boolean,
    subtitleOffsetMs: Long,
    subtitleStyle: SubtitleStyleConfig,
    onSelectAudioTrack: (AudioTrackOption) -> Unit,
    onSelectSubtitlesOff: () -> Unit,
    onSelectEmbeddedSubtitle: (SubtitleSelection.Embedded) -> Unit,
    onSelectExternalSubtitle: (ExternalSubtitleTrack) -> Unit,
    onLoadExternalSrtClick: () -> Unit,
    onAdjustSubtitleOffset: (Long) -> Unit,
    onResetSubtitleOffset: () -> Unit,
    onUpdateSubtitleStyle: (SubtitleStyleConfig) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DeepNavySurface,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 580.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.audio_and_subtitles),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )

            // Audio Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Audiotrack,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.audio_tracks_section),
                    style = MaterialTheme.typography.titleSmall,
                    color = ElectricCyan
                )
            }

            if (audioTracks.isEmpty()) {
                Surface(
                    color = ElevatedNavy,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.no_audio_tracks),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    )
                }
            } else {
                audioTracks.forEach { track ->
                    SelectableTrackRow(
                        title = track.label,
                        subtitle = track.detail,
                        selected = track.isSelected,
                        onClick = { onSelectAudioTrack(track) },
                        modifier = Modifier.testTag("audio_track_${track.id}")
                    )
                }
            }

            HorizontalDivider(color = CardOutline, modifier = Modifier.padding(vertical = 4.dp))

            // Subtitles Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ClosedCaption,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.subtitles_section),
                        style = MaterialTheme.typography.titleSmall,
                        color = ElectricCyan
                    )
                }

                Button(
                    onClick = onLoadExternalSrtClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = AmoledBlack
                    ),
                    modifier = Modifier.testTag("load_srt_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FileOpen,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.load_srt_file),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Off option
            val isOffSelected = subtitlesDisabled && selectedExternalId == null
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onSelectSubtitlesOff)
                    .testTag("subtitle_off_option"),
                color = if (isOffSelected) CyanContainer else ElevatedNavy,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ClosedCaptionDisabled,
                            contentDescription = null,
                            tint = if (isOffSelected) ElectricCyan else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.subtitles_off),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isOffSelected) OnCyanContainer else TextPrimary
                        )
                    }
                    if (isOffSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = ElectricCyan
                        )
                    }
                }
            }

            // External SRT tracks
            externalSubtitles.forEach { ext ->
                val isSelected = selectedExternalId == ext.id
                SelectableTrackRow(
                    title = ext.displayName,
                    subtitle = "External SRT • ${ext.cues.size} ${if (ext.cues.size == 1) "cue" else "cues"}",
                    selected = isSelected,
                    onClick = { onSelectExternalSubtitle(ext) },
                    modifier = Modifier.testTag("external_srt_${ext.id}")
                )
            }

            // Embedded subtitle tracks
            embeddedSubtitles.forEach { sub ->
                val isSelected = !subtitlesDisabled && selectedExternalId == null && sub.isSelected
                SelectableTrackRow(
                    title = sub.label,
                    subtitle = sub.language.ifBlank { "Embedded Track" },
                    selected = isSelected,
                    onClick = { onSelectEmbeddedSubtitle(sub) },
                    modifier = Modifier.testTag("embedded_sub_${sub.id}")
                )
            }

            HorizontalDivider(color = CardOutline, modifier = Modifier.padding(vertical = 4.dp))

            // Subtitle Sync Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.subtitle_sync_label),
                    style = MaterialTheme.typography.titleSmall,
                    color = ElectricCyan
                )
            }

            Surface(
                color = ElevatedNavy,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, CardOutline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onAdjustSubtitleOffset(-100L) },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, CardOutline),
                            modifier = Modifier.testTag("subtitle_sync_minus")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Remove,
                                contentDescription = "Delay subtitle -0.1s",
                                modifier = Modifier.size(16.dp),
                                tint = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("0.1s", color = TextPrimary)
                        }

                        Surface(
                            color = CyanContainer,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = PlayerMath.formatSubtitleOffset(subtitleOffsetMs),
                                style = MaterialTheme.typography.labelLarge,
                                color = ElectricCyan,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .testTag("subtitle_sync_value")
                            )
                        }

                        OutlinedButton(
                            onClick = { onAdjustSubtitleOffset(100L) },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, CardOutline),
                            modifier = Modifier.testTag("subtitle_sync_plus")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Delay subtitle +0.1s",
                                modifier = Modifier.size(16.dp),
                                tint = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("0.1s", color = TextPrimary)
                        }
                    }

                    TextButton(
                        onClick = onResetSubtitleOffset,
                        enabled = subtitleOffsetMs != 0L,
                        modifier = Modifier.testTag("subtitle_sync_reset")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = "Reset sync",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset")
                    }
                }
            }

            HorizontalDivider(color = CardOutline, modifier = Modifier.padding(vertical = 4.dp))

            // Subtitle Appearance Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Palette,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.subtitle_appearance_label),
                    style = MaterialTheme.typography.titleSmall,
                    color = ElectricCyan
                )
            }

            Surface(
                color = ElevatedNavy,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, CardOutline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Live Preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AmoledBlack.copy(alpha = 0.7f))
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = AmoledBlack.copy(alpha = subtitleStyle.bgOpacity),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Alt Player Subtitle Preview • বাংলা",
                                fontFamily = GoogleSansFamily,
                                fontSize = subtitleStyle.fontSize.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = subtitleStyle.textColor.color,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Font Size
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Font Size",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SubtitleFontSize.entries.forEach { sizeOption ->
                                val selected = subtitleStyle.fontSize == sizeOption
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            onUpdateSubtitleStyle(subtitleStyle.copy(fontSize = sizeOption))
                                        }
                                        .testTag("sub_size_${sizeOption.name}"),
                                    color = if (selected) CyanContainer else DeepNavySurface,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (selected) ElectricCyan else CardOutline
                                    )
                                ) {
                                    Text(
                                        text = sizeOption.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) OnCyanContainer else TextPrimary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Text Color
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Text Color",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SubtitleTextColor.entries.forEach { colorOption ->
                                val selected = subtitleStyle.textColor == colorOption
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            onUpdateSubtitleStyle(subtitleStyle.copy(textColor = colorOption))
                                        }
                                        .testTag("sub_color_${colorOption.name}"),
                                    color = if (selected) CyanContainer else DeepNavySurface,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (selected) ElectricCyan else CardOutline
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(colorOption.color)
                                                .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = colorOption.label,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (selected) OnCyanContainer else TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Background Opacity
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Background Opacity",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                            Text(
                                text = "${(subtitleStyle.bgOpacity * 100f).roundToInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = ElectricCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = subtitleStyle.bgOpacity,
                            onValueChange = { nextAlpha ->
                                onUpdateSubtitleStyle(subtitleStyle.copy(bgOpacity = nextAlpha.coerceIn(0f, 1f)))
                            },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = ElectricCyan,
                                activeTrackColor = ElectricCyan,
                                inactiveTrackColor = CardOutline
                            ),
                            modifier = Modifier.testTag("sub_bg_opacity_slider")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectableTrackRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = if (selected) CyanContainer else ElevatedNavy,
        shape = RoundedCornerShape(12.dp),
        border = if (selected) BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)) else null
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
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) OnCyanContainer else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) ElectricCyan else TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = ElectricCyan
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedSelectorSheet(
    currentSpeed: Float,
    onSelectSpeed: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DeepNavySurface,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Speed,
                    contentDescription = null,
                    tint = ElectricCyan
                )
                Text(
                    text = stringResource(R.string.playback_speed),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PLAYBACK_SPEEDS.forEach { speed ->
                    val selected = kotlin.math.abs(currentSpeed - speed) < 0.01f
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                onSelectSpeed(speed)
                                onDismiss()
                            }
                            .testTag("speed_option_$speed"),
                        color = if (selected) VioletContainer else ElevatedNavy,
                        shape = RoundedCornerShape(14.dp),
                        border = if (selected) BorderStroke(1.dp, ElectricCyan) else BorderStroke(1.dp, CardOutline)
                    ) {
                        Column(
                            modifier = Modifier.padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "${speed}x",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) OnVioletContainer else TextPrimary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerSheet(
    eqState: EqualizerUiState,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectPreset: (EqPreset) -> Unit,
    onBandLevelChange: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DeepNavySurface,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 580.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = CyanContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(TealEmeraldDeep, LuminousMint)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Equalizer,
                                contentDescription = null,
                                tint = AmoledBlack,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Offline Equalizer",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = if (eqState.enabled) "Active • ${eqState.preset.label}" else "Bypassed (Tap preset or switch on)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (eqState.enabled) LuminousMint else TextMuted
                        )
                    }
                }

                Switch(
                    checked = eqState.enabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AmoledBlack,
                        checkedTrackColor = LuminousMint,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = ElevatedNavy
                    ),
                    modifier = Modifier.testTag("eq_enable_switch")
                )
            }

            // Presets Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EqPreset.selectablePresets.forEach { preset ->
                    val selected = eqState.enabled && eqState.preset == preset
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectPreset(preset) }
                            .testTag("eq_preset_${preset.name}"),
                        color = if (selected) CyanContainer else ElevatedNavy,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            if (selected) LuminousMint else CardOutline
                        )
                    ) {
                        Text(
                            text = preset.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) LuminousMint else TextPrimary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                        )
                    }
                }
            }

            // 5-Band Sliders
            Surface(
                color = ElevatedNavy,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CardOutline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val minMb = minOf(eqState.minLevelMb, eqState.maxLevelMb).toFloat()
                    val maxMb = maxOf(eqState.minLevelMb, eqState.maxLevelMb).toFloat().coerceAtLeast(minMb + 100f)

                    eqState.bandLevelsMb.forEachIndexed { index, levelMb ->
                        val freqHz = eqState.bandCenterFreqsHz.getOrElse(index) {
                            listOf(60, 230, 910, 3600, 14000).getOrElse(index) { 1000 }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = PlayerMath.formatEqFrequency(freqHz),
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary,
                                modifier = Modifier.width(54.dp)
                            )

                            Slider(
                                value = levelMb.toFloat().coerceIn(minMb, maxMb),
                                onValueChange = { nextVal ->
                                    onBandLevelChange(index, nextVal.roundToInt())
                                },
                                valueRange = minMb..maxMb,
                                colors = SliderDefaults.colors(
                                    thumbColor = if (eqState.enabled) LuminousMint else TextSecondary,
                                    activeTrackColor = if (eqState.enabled) LuminousMint else TextMuted,
                                    inactiveTrackColor = CardOutline
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("eq_band_slider_$index")
                            )

                            Surface(
                                color = if (eqState.enabled) CyanContainer.copy(alpha = 0.7f) else DeepNavySurface,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.width(68.dp)
                            ) {
                                Text(
                                    text = PlayerMath.formatEqDb(levelMb),
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (eqState.enabled) LuminousMint else TextMuted,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

