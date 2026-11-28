package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.SortOption
import com.example.data.SortOrder
import com.example.data.VideoFolder
import com.example.data.VideoItem
import com.example.data.VideoMediaInfo
import com.example.data.VideoThumbnailLoader
import com.example.data.formatDuration
import com.example.data.formatFileSize
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.CardOutline
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.DeepNavySurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedNavy
import com.example.ui.theme.EmeraldOffline
import com.example.ui.theme.ForestContainer
import com.example.ui.theme.OnCyanContainer
import com.example.ui.theme.OnVioletContainer
import com.example.ui.theme.SurfaceHighlight
import com.example.ui.theme.TealEmeraldDeep
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletAccent
import com.example.ui.theme.VioletContainer

@Composable
fun AltPlayerLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.alt_player_logo),
        contentDescription = stringResource(R.string.app_name),
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}

@Composable
fun VideoThumbnailImage(
    video: VideoItem,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(video.uri) {
        mutableStateOf<ImageBitmap?>(VideoThumbnailLoader.getCached(video.uri))
    }

    LaunchedEffect(video.uri) {
        if (bitmap == null) {
            bitmap = VideoThumbnailLoader.loadThumbnail(context, video)
        }
    }

    Box(
        modifier = modifier
            .background(
                if (video.isAudio) {
                    Brush.linearGradient(
                        colors = listOf(TealEmeraldDeep.copy(alpha = 0.55f), ForestContainer)
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(ElevatedNavy, SurfaceHighlight)
                    )
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        val loaded = bitmap
        if (loaded != null) {
            Image(
                bitmap = loaded,
                contentDescription = video.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else if (video.isAudio) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = null,
                    tint = ElectricCyan.copy(alpha = 0.9f),
                    modifier = Modifier.size(28.dp)
                )
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = VioletAccent.copy(alpha = 0.85f),
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            Icon(
                imageVector = Icons.Rounded.Movie,
                contentDescription = null,
                tint = TextMuted.copy(alpha = 0.5f),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun BadgePill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            maxLines = 1
        )
    }
}

@Composable
fun FolderCard(
    folder: VideoFolder,
    hasUnwatchedVideos: Boolean,
    isGrid: Boolean,
    onClick: () -> Unit,
    onPlayFirst: () -> Unit,
    onToggleHide: () -> Unit,
    onDeleteFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    if (isGrid) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onClick)
                .testTag("folder_card_${folder.bucketId}"),
            color = DeepNavySurface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, CardOutline.copy(alpha = 0.7f)),
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 10f)
                        .background(ElevatedNavy)
                ) {
                    val leadVideo = folder.videos.firstOrNull()
                    if (leadVideo != null) {
                        VideoThumbnailImage(video = leadVideo, modifier = Modifier.fillMaxSize())
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, AmoledBlack.copy(alpha = 0.78f))
                                    )
                                )
                        )
                    }
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Folder,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${folder.videoCount}",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (folder.isHidden) {
                        BadgePill(
                            text = stringResource(R.string.hidden_badge),
                            containerColor = AmberWarm.copy(alpha = 0.22f),
                            contentColor = AmberWarm,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                        )
                    } else if (hasUnwatchedVideos) {
                        BadgePill(
                            text = stringResource(R.string.new_badge),
                            containerColor = CyanContainer,
                            contentColor = ElectricCyan,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                        )
                    }
                    Box(modifier = Modifier.align(Alignment.TopEnd)) {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.testTag("folder_menu_${folder.bucketId}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = stringResource(R.string.more_options),
                                tint = TextPrimary
                            )
                        }
                        FolderOverflowMenu(
                            expanded = menuExpanded,
                            isHidden = folder.isHidden,
                            onDismiss = { menuExpanded = false },
                            onPlay = onPlayFirst,
                            onToggleHide = onToggleHide,
                            onDelete = onDeleteFolder
                        )
                    }
                }
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${formatFileSize(folder.totalSizeBytes)} • ${formatDuration(folder.totalDurationMs)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
            }
        }
    } else {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onClick)
                .testTag("folder_card_${folder.bucketId}"),
            color = DeepNavySurface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, CardOutline.copy(alpha = 0.7f)),
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 88.dp, height = 64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElevatedNavy)
                        .border(1.dp, CardOutline, RoundedCornerShape(12.dp))
                ) {
                    val leadVideo = folder.videos.firstOrNull()
                    if (leadVideo != null) {
                        VideoThumbnailImage(
                            video = leadVideo,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, AmoledBlack.copy(alpha = 0.72f))
                                    )
                                )
                        )
                    }
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Folder,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${folder.videoCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = folder.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (folder.isHidden) {
                            BadgePill(
                                text = stringResource(R.string.hidden_badge),
                                containerColor = AmberWarm.copy(alpha = 0.2f),
                                contentColor = AmberWarm
                            )
                        } else if (hasUnwatchedVideos) {
                            BadgePill(
                                text = stringResource(R.string.new_badge),
                                containerColor = CyanContainer,
                                contentColor = ElectricCyan
                            )
                        }
                    }

                    Text(
                        text = folder.path.ifBlank { folder.name },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val countLabel = if (folder.videoCount == 1) "1 video" else "${folder.videoCount} videos"
                        Text(
                            text = countLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricCyan
                        )
                        Text(text = "•", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                        Text(
                            text = formatFileSize(folder.totalSizeBytes),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.testTag("folder_menu_${folder.bucketId}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = stringResource(R.string.more_options),
                            tint = TextSecondary
                        )
                    }
                    FolderOverflowMenu(
                        expanded = menuExpanded,
                        isHidden = folder.isHidden,
                        onDismiss = { menuExpanded = false },
                        onPlay = onPlayFirst,
                        onToggleHide = onToggleHide,
                        onDelete = onDeleteFolder
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderOverflowMenu(
    expanded: Boolean,
    isHidden: Boolean,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onToggleHide: () -> Unit,
    onDelete: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = ElevatedNavy
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_play_resume), color = TextPrimary) },
            leadingIcon = { Icon(Icons.Rounded.PlayArrow, null, tint = ElectricCyan) },
            onClick = {
                onDismiss()
                onPlay()
            }
        )
        DropdownMenuItem(
            text = {
                Text(
                    stringResource(if (isHidden) R.string.menu_unhide_folder else R.string.menu_hide_folder),
                    color = TextPrimary
                )
            },
            leadingIcon = {
                Icon(
                    if (isHidden) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                    null,
                    tint = AmberWarm
                )
            },
            onClick = {
                onDismiss()
                onToggleHide()
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_delete), color = CoralError) },
            leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null, tint = CoralError) },
            onClick = {
                onDismiss()
                onDelete()
            }
        )
    }
}

@Composable
fun VideoCard(
    video: VideoItem,
    resumePositionMs: Long,
    isWatched: Boolean,
    isGrid: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onMoveToVault: (() -> Unit)?,
    onProperties: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    if (isGrid) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onClick)
                .testTag("video_card_${video.id}"),
            color = DeepNavySurface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, CardOutline.copy(alpha = 0.65f))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                ) {
                    VideoThumbnailImage(video = video, modifier = Modifier.fillMaxSize())
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, AmoledBlack.copy(alpha = 0.85f))
                                )
                            )
                    )
                    BadgePill(
                        text = video.resolutionLabel,
                        containerColor = AmoledBlack.copy(alpha = 0.78f),
                        contentColor = ElectricCyan,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    )
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp),
                        color = AmoledBlack.copy(alpha = 0.82f),
                        shape = RoundedCornerShape(5.dp)
                    ) {
                        Text(
                            text = formatDuration(video.durationMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                    if (resumePositionMs > 3_000L && video.durationMs > 0L) {
                        val progress = (resumePositionMs.toFloat() / video.durationMs.toFloat()).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .align(Alignment.BottomCenter)
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .fillMaxHeight()
                                    .background(ElectricCyan)
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, top = 8.dp, bottom = 10.dp, end = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = video.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${video.extension} • ${formatFileSize(video.sizeBytes)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.testTag("video_menu_${video.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = stringResource(R.string.more_options),
                                tint = TextSecondary
                            )
                        }
                        VideoOverflowMenu(
                            expanded = menuExpanded,
                            onDismiss = { menuExpanded = false },
                            onPlay = onClick,
                            onRename = onRename,
                            onShare = onShare,
                            onMoveToVault = onMoveToVault,
                            onDelete = onDelete,
                            onProperties = onProperties
                        )
                    }
                }
            }
        }
    } else {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onClick)
                .testTag("video_card_${video.id}"),
            color = DeepNavySurface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, CardOutline.copy(alpha = 0.65f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, top = 10.dp, bottom = 10.dp, end = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(124.dp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, CardOutline, RoundedCornerShape(10.dp))
                ) {
                    VideoThumbnailImage(video = video, modifier = Modifier.fillMaxSize())
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, AmoledBlack.copy(alpha = 0.85f))
                                )
                            )
                    )
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp),
                        color = AmoledBlack.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(5.dp)
                    ) {
                        Text(
                            text = formatDuration(video.durationMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                    if (resumePositionMs > 3_000L && video.durationMs > 0L) {
                        val progress = (resumePositionMs.toFloat() / video.durationMs.toFloat()).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .align(Alignment.BottomCenter)
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .fillMaxHeight()
                                    .background(ElectricCyan)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        val isHighRes = !video.isAudio && (
                            video.resolutionLabel == "4K" ||
                                video.resolutionLabel == "2K" ||
                                video.resolutionLabel == "1080p"
                            )

                        if (!video.isAudio) {
                            BadgePill(
                                text = video.resolutionLabel,
                                containerColor = if (isHighRes) CyanContainer else SurfaceHighlight,
                                contentColor = if (isHighRes) ElectricCyan else TextSecondary
                            )
                        }

                        BadgePill(
                            text = video.extension,
                            containerColor = VioletContainer,
                            contentColor = OnVioletContainer
                        )

                        if (resumePositionMs > 3_000L) {
                            BadgePill(
                                text = stringResource(R.string.resume_label),
                                containerColor = ElectricCyan.copy(alpha = 0.16f),
                                contentColor = ElectricCyan
                            )
                        } else if (!isWatched) {
                            BadgePill(
                                text = stringResource(R.string.new_badge),
                                containerColor = EmeraldOffline.copy(alpha = 0.18f),
                                contentColor = EmeraldOffline
                            )
                        }
                    }

                    Text(
                        text = buildString {
                            if (video.isAudio && video.artist.isNotBlank()) {
                                append(video.artist)
                                append(" • ")
                            }
                            append(formatFileSize(video.sizeBytes))
                            if (!video.isAudio && video.dimensionsText.isNotEmpty()) {
                                append(" • ")
                                append(video.dimensionsText)
                            }
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.testTag("video_menu_${video.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = stringResource(R.string.more_options),
                            tint = TextSecondary
                        )
                    }
                    VideoOverflowMenu(
                        expanded = menuExpanded,
                        onDismiss = { menuExpanded = false },
                        onPlay = onClick,
                        onRename = onRename,
                        onShare = onShare,
                        onMoveToVault = onMoveToVault,
                        onDelete = onDelete,
                        onProperties = onProperties
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoOverflowMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onMoveToVault: (() -> Unit)?,
    onDelete: () -> Unit,
    onProperties: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = ElevatedNavy
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_play_resume), color = TextPrimary) },
            leadingIcon = { Icon(Icons.Rounded.PlayArrow, null, tint = ElectricCyan) },
            onClick = { onDismiss(); onPlay() }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_rename), color = TextPrimary) },
            leadingIcon = { Icon(Icons.Rounded.DriveFileRenameOutline, null, tint = TextSecondary) },
            onClick = { onDismiss(); onRename() }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_share), color = TextPrimary) },
            leadingIcon = { Icon(Icons.Rounded.Share, null, tint = TextSecondary) },
            onClick = { onDismiss(); onShare() }
        )
        if (onMoveToVault != null) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_move_to_vault), color = TextPrimary) },
                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.DriveFileMove, null, tint = VioletAccent) },
                onClick = { onDismiss(); onMoveToVault() }
            )
        }
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_properties), color = TextPrimary) },
            leadingIcon = { Icon(Icons.Rounded.Info, null, tint = ElectricCyan) },
            onClick = { onDismiss(); onProperties() }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_delete), color = CoralError) },
            leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null, tint = CoralError) },
            onClick = { onDismiss(); onDelete() }
        )
    }
}

@Composable
fun RenameVideoDialog(
    initialTitle: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(initialTitle) { mutableStateOf(initialTitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepNavySurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = { Text(stringResource(R.string.menu_rename), style = MaterialTheme.typography.titleLarge) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("Video name") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = CardOutline,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedLabelColor = ElectricCyan
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rename_video_input")
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = AmoledBlack)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
fun DeleteConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepNavySurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = CoralError, contentColor = TextPrimary)
            ) {
                Text(stringResource(R.string.menu_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
fun MediaInfoDialog(
    isLoading: Boolean,
    info: VideoMediaInfo?,
    onDismiss: () -> Unit
) {
    if (!isLoading && info == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepNavySurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.Info, contentDescription = null, tint = ElectricCyan)
                Text(stringResource(R.string.menu_properties), style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            if (isLoading || info == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ElectricCyan)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MediaInfoRow("File Name", info.title)
                    MediaInfoRow("Full Path", info.filePath)
                    MediaInfoRow("Size", formatFileSize(info.sizeBytes))
                    MediaInfoRow("Duration", formatDuration(info.durationMs))
                    MediaInfoRow("Resolution", info.resolution)
                    MediaInfoRow("Container", info.containerFormat)
                    MediaInfoRow("Video Codec", info.videoCodec)
                    MediaInfoRow("Frame Rate", info.frameRate)
                    MediaInfoRow("Audio Codec", info.audioCodec)
                    MediaInfoRow("Audio Channels", info.audioChannels)
                    if (info.bitrateKbps > 0) {
                        MediaInfoRow("Bitrate", "${info.bitrateKbps} kbps")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = ElectricCyan)
            }
        }
    )
}

@Composable
private fun MediaInfoRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(text = value.ifBlank { "—" }, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
    }
}

@Composable
fun ContinueWatchingBanner(
    video: VideoItem,
    positionMs: Long,
    onResumeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onResumeClick)
            .testTag("continue_watching_card"),
        color = ElevatedNavy,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(CyanContainer.copy(alpha = 0.45f), ElevatedNavy)
                    )
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                VideoThumbnailImage(video = video, modifier = Modifier.fillMaxSize())
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(AmoledBlack.copy(alpha = 0.72f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.continue_watching).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatDuration(positionMs)} / ${formatDuration(video.durationMs)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortOptionsSheet(
    currentOption: SortOption,
    currentOrder: SortOrder,
    onSelectSort: (SortOption, SortOrder) -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.sort_by),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            val options = listOf(
                SortOption.NAME to stringResource(R.string.sort_name),
                SortOption.DATE to stringResource(R.string.sort_date),
                SortOption.SIZE to stringResource(R.string.sort_size)
            )

            options.forEach { (option, label) ->
                val selected = currentOption == option
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            onSelectSort(option, currentOrder)
                            onDismiss()
                        }
                        .testTag("sort_option_${option.name.lowercase()}"),
                    color = if (selected) CyanContainer else ElevatedNavy,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (selected) OnCyanContainer else TextPrimary
                        )
                        if (selected) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = ElectricCyan)
                        }
                    }
                }
            }

            HorizontalDivider(color = CardOutline, modifier = Modifier.padding(vertical = 4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val ascSelected = currentOrder == SortOrder.ASCENDING
                val descSelected = currentOrder == SortOrder.DESCENDING

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            onSelectSort(currentOption, SortOrder.ASCENDING)
                            onDismiss()
                        }
                        .testTag("sort_order_asc"),
                    color = if (ascSelected) VioletContainer else ElevatedNavy,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 13.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.ArrowUpward,
                            contentDescription = null,
                            tint = if (ascSelected) OnVioletContainer else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.sort_ascending),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (ascSelected) OnVioletContainer else TextSecondary
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            onSelectSort(currentOption, SortOrder.DESCENDING)
                            onDismiss()
                        }
                        .testTag("sort_order_desc"),
                    color = if (descSelected) VioletContainer else ElevatedNavy,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 13.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.ArrowDownward,
                            contentDescription = null,
                            tint = if (descSelected) OnVioletContainer else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.sort_descending),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (descSelected) OnVioletContainer else TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyShieldSheet(
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
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = EmeraldOffline.copy(alpha = 0.16f),
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.VerifiedUser,
                            contentDescription = null,
                            tint = EmeraldOffline,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = stringResource(R.string.privacy_sheet_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = stringResource(R.string.offline_privacy_badge),
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldOffline
                    )
                }
            }

            Text(
                text = stringResource(R.string.privacy_sheet_body),
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )

            Surface(
                color = ElevatedNavy,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, CardOutline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Rounded.Shield, contentDescription = null, tint = ElectricCyan)
                    Text(
                        text = "Network is limited to app updates and theme colors",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}
