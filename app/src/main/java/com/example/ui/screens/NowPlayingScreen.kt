package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppThemeMode
import com.example.data.model.AudioTrack
import com.example.data.model.LyricLine
import com.example.data.model.PlaybackMode
import com.example.data.model.VisualizerStyle
import com.example.ui.components.AlbumArtImage
import com.example.ui.components.HiResBadge
import com.example.ui.components.PulsingDot
import com.example.ui.components.ReorderableTrackList
import com.example.ui.components.SpectrumVisualizer
import com.example.ui.components.SyncedLyricsView
import com.example.ui.theme.FrostedBackground
import com.example.ui.theme.FrostedCyan
import com.example.ui.theme.FrostedGlassAmbientBackground
import com.example.ui.theme.FrostedGlassBorder
import com.example.ui.theme.FrostedGlassBorderHeavy
import com.example.ui.theme.FrostedGlassWhite
import com.example.ui.theme.FrostedGlassWhiteHeavy
import com.example.ui.theme.FrostedNeonCyan
import com.example.ui.theme.FrostedPurple
import com.example.ui.theme.FrostedTextMuted
import com.example.ui.theme.FrostedTextPrimary
import com.example.ui.theme.FrostedTextSecondary
import com.example.viewmodel.NowPlayingDisplayMode

@Composable
fun NowPlayingScreen(
    currentTrack: AudioTrack?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    queue: List<AudioTrack>,
    playbackMode: PlaybackMode,
    playbackSpeed: Float,
    displayMode: NowPlayingDisplayMode,
    visualizerStyle: VisualizerStyle,
    spectrumData: FloatArray,
    lyrics: List<LyricLine>,
    activeLyricIndex: Int,
    lyricOffsetMs: Long,
    appThemeMode: AppThemeMode,
    sleepTimerMinutes: Int?,
    onCollapse: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onTogglePlaybackMode: () -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onSetDisplayMode: (NowPlayingDisplayMode) -> Unit,
    onCycleVisualizerStyle: () -> Unit,
    onCycleTheme: () -> Unit,
    onToggleFavorite: (AudioTrack) -> Unit,
    onAdjustLyricOffset: (Long) -> Unit,
    onResetLyricOffset: () -> Unit,
    onSaveLyrics: (String) -> Unit,
    onReorderQueue: (Int, Int) -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenTagEditor: (AudioTrack) -> Unit,
    onOpenInspector: (AudioTrack) -> Unit,
    onOpenSync: () -> Unit,
    onSetSleepTimer: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (currentTrack == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No track selected", color = FrostedTextPrimary)
        }
        return
    }

    val primaryAccent = if (appThemeMode == AppThemeMode.FROSTED_GLASS) FrostedNeonCyan else Color(currentTrack.albumSecondaryColorHex)
    val albumColor = Color(currentTrack.albumColorHex)
    var showMoreMenu by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showSleepMenu by remember { mutableStateOf(false) }

    FrostedGlassAmbientBackground(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        topGlowColor = if (appThemeMode == AppThemeMode.FROSTED_GLASS) FrostedPurple else albumColor,
        bottomGlowColor = if (appThemeMode == AppThemeMode.FROSTED_GLASS) FrostedCyan else primaryAccent
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Collapse circular button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(FrostedGlassWhite)
                        .border(1.dp, FrostedGlassBorder, CircleShape)
                        .clickable { onCollapse() }
                        .testTag("now_playing_collapse_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = FrostedTextPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Centered Title & Subtitle
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NOW PLAYING",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currentTrack.album,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = FrostedTextPrimary.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Header Action Buttons (Equalizer + More Menu)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(FrostedGlassWhite)
                            .border(1.dp, FrostedGlassBorder, CircleShape)
                            .clickable { onOpenEqualizer() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Equalizer",
                            tint = primaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(FrostedGlassWhite)
                                .border(1.dp, FrostedGlassBorder, CircleShape)
                                .clickable { showMoreMenu = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = FrostedTextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Hi-Res Audio Inspector") },
                                onClick = {
                                    showMoreMenu = false
                                    onOpenInspector(currentTrack)
                                },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Edit Audio Tags & Lyrics") },
                                onClick = {
                                    showMoreMenu = false
                                    onOpenTagEditor(currentTrack)
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Playback Speed (${playbackSpeed}x)") },
                                onClick = {
                                    showMoreMenu = false
                                    showSpeedMenu = true
                                },
                                leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Sleep Timer ${if (sleepTimerMinutes != null) "(${sleepTimerMinutes}m)" else ""}") },
                                onClick = {
                                    showMoreMenu = false
                                    showSleepMenu = true
                                },
                                leadingIcon = { Icon(Icons.Default.AvTimer, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Switch Visualizer Style (${visualizerStyle.displayName})") },
                                onClick = {
                                    showMoreMenu = false
                                    onCycleVisualizerStyle()
                                },
                                leadingIcon = { Icon(Icons.Default.Equalizer, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Dynamic Theme (${appThemeMode.displayName})") },
                                onClick = {
                                    showMoreMenu = false
                                    onCycleTheme()
                                },
                                leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Cross-Platform Sync & Backup") },
                                onClick = {
                                    showMoreMenu = false
                                    onOpenSync()
                                },
                                leadingIcon = { Icon(Icons.Default.Sync, contentDescription = null) }
                            )
                        }
                    }
                }
            }

            // Display Mode Segmented Selector (Vinyl / Cover / Lyrics / Queue)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(FrostedGlassWhite)
                    .border(1.dp, FrostedGlassBorder, RoundedCornerShape(16.dp))
                    .padding(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ModeButton(
                        icon = Icons.Default.MusicNote,
                        label = "Cover",
                        isSelected = displayMode == NowPlayingDisplayMode.COVER_ART,
                        accentColor = primaryAccent,
                        onClick = { onSetDisplayMode(NowPlayingDisplayMode.COVER_ART) }
                    )
                    ModeButton(
                        icon = Icons.Default.Album,
                        label = "Vinyl",
                        isSelected = displayMode == NowPlayingDisplayMode.VINYL,
                        accentColor = primaryAccent,
                        onClick = { onSetDisplayMode(NowPlayingDisplayMode.VINYL) }
                    )
                    ModeButton(
                        icon = Icons.Default.FormatQuote,
                        label = "Lyrics",
                        isSelected = displayMode == NowPlayingDisplayMode.LYRICS,
                        accentColor = primaryAccent,
                        onClick = { onSetDisplayMode(NowPlayingDisplayMode.LYRICS) }
                    )
                    ModeButton(
                        icon = Icons.Default.QueueMusic,
                        label = "Queue (${queue.size})",
                        isSelected = displayMode == NowPlayingDisplayMode.QUEUE,
                        accentColor = primaryAccent,
                        onClick = { onSetDisplayMode(NowPlayingDisplayMode.QUEUE) }
                    )
                }
            }

            // Main Visual Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                when (displayMode) {
                    NowPlayingDisplayMode.COVER_ART -> {
                        FrostedCoverArtView(
                            track = currentTrack,
                            accentColor = primaryAccent,
                            lyrics = lyrics,
                            activeLyricIndex = activeLyricIndex,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    NowPlayingDisplayMode.VINYL -> {
                        VinylTurntableDisc(
                            track = currentTrack,
                            isPlaying = isPlaying,
                            accentColor = primaryAccent,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    NowPlayingDisplayMode.LYRICS -> {
                        SyncedLyricsView(
                            lyrics = lyrics,
                            activeLyricIndex = activeLyricIndex,
                            lyricOffsetMs = lyricOffsetMs,
                            currentTrack = currentTrack,
                            currentPositionMs = currentPositionMs,
                            onSeekTo = onSeekTo,
                            onAdjustOffset = onAdjustLyricOffset,
                            onResetOffset = onResetLyricOffset,
                            onSaveLyrics = onSaveLyrics,
                            accentColor = primaryAccent,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    NowPlayingDisplayMode.QUEUE -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "NOW PLAYING QUEUE (DRAG TO REORDER)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryAccent,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                            )
                            ReorderableTrackList(
                                tracks = queue,
                                currentPlayingTrackId = currentTrack.id,
                                isPlaying = isPlaying,
                                onTrackClick = { onSeekTo(0L) },
                                onToggleFavorite = onToggleFavorite,
                                onReorder = onReorderQueue,
                                onAddToPlaylist = {},
                                onEditTags = onOpenTagEditor,
                                onViewMetadata = onOpenInspector,
                                enableDragReorder = true,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // Real-Time Animated Spectrum Visualizer
            SpectrumVisualizer(
                spectrumData = spectrumData,
                style = visualizerStyle,
                primaryColor = primaryAccent,
                secondaryColor = FrostedPurple,
                height = 32.dp,
                isPlaying = isPlaying,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x08FFFFFF))
                    .border(0.75.dp, FrostedGlassBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .clickable { onCycleVisualizerStyle() }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Track Info & Audio Resolution Readout Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = currentTrack.title,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = FrostedTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        Text(
                            text = "${currentTrack.bitDepth}-bit / ${(currentTrack.sampleRateHz / 1000)}kHz",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = FrostedTextMuted,
                            modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${currentTrack.artist} • ${currentTrack.album}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = FrostedTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(FrostedGlassWhite)
                        .border(1.dp, FrostedGlassBorder, CircleShape)
                        .clickable { onToggleFavorite(currentTrack) }
                        .testTag("now_playing_favorite_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (currentTrack.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (currentTrack.isFavorite) Color(0xFFFF4081) else FrostedTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Seekbar Progress & Time Labels
            var isUserDraggingSlider by remember { mutableStateOf(false) }
            var sliderDragPositionMs by remember { mutableFloatStateOf(0f) }

            val displayPosMs = if (isUserDraggingSlider) sliderDragPositionMs.toLong() else currentPositionMs
            val progressFraction = if (durationMs > 0) (displayPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = progressFraction,
                    onValueChange = { fraction ->
                        isUserDraggingSlider = true
                        sliderDragPositionMs = fraction * durationMs
                    },
                    onValueChangeFinished = {
                        isUserDraggingSlider = false
                        onSeekTo(sliderDragPositionMs.toLong())
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = FrostedGlassWhiteHeavy
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(displayPosMs),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = FrostedTextMuted
                    )

                    Text(
                        text = formatTime(durationMs),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = FrostedTextMuted
                    )
                }
            }

            // Main Playback Controls Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp, top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Shuffle Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (playbackMode == PlaybackMode.SHUFFLE) primaryAccent.copy(alpha = 0.25f) else FrostedGlassWhite)
                        .border(1.dp, if (playbackMode == PlaybackMode.SHUFFLE) primaryAccent else FrostedGlassBorder, CircleShape)
                        .clickable { onTogglePlaybackMode() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (playbackMode == PlaybackMode.SHUFFLE) primaryAccent else FrostedTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Controls cluster: Skip Prev, Center Play/Pause, Skip Next
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Skip Previous
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(FrostedGlassWhite)
                            .border(1.dp, FrostedGlassBorder, CircleShape)
                            .clickable { onSkipPrevious() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    // Pure Solid White Play / Pause Button with Drop Shadow
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .shadow(20.dp, CircleShape, spotColor = Color.White)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable { onTogglePlayPause() }
                            .testTag("now_playing_play_pause_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = FrostedBackground,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    // Skip Next
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(FrostedGlassWhite)
                            .border(1.dp, FrostedGlassBorder, CircleShape)
                            .clickable { onSkipNext() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                // Repeat Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (playbackMode == PlaybackMode.REPEAT_ALL || playbackMode == PlaybackMode.REPEAT_ONE) primaryAccent.copy(alpha = 0.25f) else FrostedGlassWhite)
                        .border(1.dp, if (playbackMode == PlaybackMode.REPEAT_ALL || playbackMode == PlaybackMode.REPEAT_ONE) primaryAccent else FrostedGlassBorder, CircleShape)
                        .clickable { onTogglePlaybackMode() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (playbackMode == PlaybackMode.REPEAT_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = if (playbackMode == PlaybackMode.REPEAT_ALL || playbackMode == PlaybackMode.REPEAT_ONE) primaryAccent else FrostedTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Speed Menu Dialog
        if (showSpeedMenu) {
            SpeedSelectorDialog(
                currentSpeed = playbackSpeed,
                onSelectSpeed = {
                    onSetPlaybackSpeed(it)
                    showSpeedMenu = false
                },
                onDismiss = { showSpeedMenu = false },
                accentColor = primaryAccent
            )
        }

        // Sleep Timer Dialog
        if (showSleepMenu) {
            SleepTimerDialog(
                currentMinutes = sleepTimerMinutes,
                onSelectMinutes = {
                    onSetSleepTimer(it)
                    showSleepMenu = false
                },
                onDismiss = { showSleepMenu = false },
                accentColor = primaryAccent
            )
        }
    }
}

@Composable
fun ModeButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) FrostedGlassWhiteHeavy else Color.Transparent)
            .border(
                1.dp,
                if (isSelected) FrostedGlassBorderHeavy else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color.White else FrostedTextMuted,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else FrostedTextMuted
            )
        }
    }
}

@Composable
fun FrostedCoverArtView(
    track: AudioTrack,
    accentColor: Color,
    lyrics: List<LyricLine>,
    activeLyricIndex: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Frosted Album Artwork Card with Atmospheric Aura
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            // Ambient Aura behind the album card
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .scale(1.05f)
                    .clip(RoundedCornerShape(40.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                FrostedPurple.copy(alpha = 0.35f),
                                FrostedCyan.copy(alpha = 0.30f)
                            )
                        )
                    )
            )

            // Main Album Glass Container
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .shadow(24.dp, RoundedCornerShape(36.dp), spotColor = accentColor.copy(alpha = 0.4f))
                    .clip(RoundedCornerShape(36.dp))
                    .border(1.dp, FrostedGlassBorder, RoundedCornerShape(36.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Real Album Art Image or fallback
                AlbumArtImage(
                    artUri = track.albumArtUri,
                    fallbackColorHex = track.albumColorHex,
                    fallbackSecondaryHex = track.albumSecondaryColorHex,
                    iconSize = 72.dp,
                    contentDescription = "Cover for ${track.album}",
                    modifier = Modifier.fillMaxSize()
                )

                // If no custom art, show stylish frosted title overlay
                if (track.albumArtUri.isNullOrBlank()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = track.album,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.95f),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Top Right Frosted Hi-Res FLAC Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x88000000))
                        .border(1.dp, FrostedGlassBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        PulsingDot(color = FrostedNeonCyan, size = 6)
                        Text(
                            text = "HI-RES ${track.format.displayName}",
                            color = Color(0xFFD0F8FF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Frosted Synced Lyrics Preview Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(FrostedGlassWhite)
                .border(1.dp, FrostedGlassBorder, RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (lyrics.isNotEmpty() && activeLyricIndex in lyrics.indices) {
                val prevLyric = if (activeLyricIndex > 0) lyrics[activeLyricIndex - 1].text else ""
                val currentLyric = lyrics[activeLyricIndex].text
                val nextLyric = if (activeLyricIndex < lyrics.size - 1) lyrics[activeLyricIndex + 1].text else ""

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (prevLyric.isNotEmpty()) {
                        Text(
                            text = prevLyric,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.35f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = currentLyric,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    if (nextLyric.isNotEmpty()) {
                        Text(
                            text = nextLyric,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.35f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatQuote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Pure Lossless Audiophile Master",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun VinylTurntableDisc(
    track: AudioTrack,
    isPlaying: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinylRotation"
    )

    val currentRotation = if (isPlaying) rotation else 0f

    Box(
        modifier = modifier.padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Turntable frosted back-plate
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(CircleShape)
                .background(FrostedGlassWhite)
                .border(1.dp, FrostedGlassBorder, CircleShape)
        )

        // Vinyl Record Disc
        Box(
            modifier = Modifier
                .size(220.dp)
                .rotate(currentRotation)
                .shadow(24.dp, CircleShape, spotColor = accentColor),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.width / 2f

                // Outer Vinyl Base
                drawCircle(
                    color = Color(0xFF0D0D11),
                    radius = radius,
                    center = center
                )

                // Vinyl Micro-grooves
                for (r in 4..16) {
                    val grooveRadius = (radius * 0.42f) + (radius * 0.55f * (r / 16f))
                    drawCircle(
                        color = Color(0xFF1E2028),
                        radius = grooveRadius,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // Radial Light Sheen
                drawCircle(
                    brush = Brush.sweepGradient(
                        listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.08f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = center
                    ),
                    radius = radius,
                    center = center
                )
            }

            // Center Label Artwork
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color(0xFFD4AF37), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AlbumArtImage(
                    artUri = track.albumArtUri,
                    fallbackColorHex = track.albumColorHex,
                    fallbackSecondaryHex = track.albumSecondaryColorHex,
                    iconSize = 32.dp,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )

                // Center Spindle Hole
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0D0D11))
                        .border(1.5.dp, Color(0xFFD4AF37), CircleShape)
                )
            }
        }
    }
}

@Composable
fun SpeedSelectorDialog(
    currentSpeed: Float,
    onSelectSpeed: (Float) -> Unit,
    onDismiss: () -> Unit,
    accentColor: Color
) {
    val speeds = listOf(0.5f, 0.75f, 0.9f, 1.0f, 1.1f, 1.25f, 1.5f, 2.0f)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Playback Speed", color = FrostedTextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                speeds.forEach { speed ->
                    val isSelected = currentSpeed == speed
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) FrostedGlassWhiteHeavy else FrostedGlassWhite)
                            .border(1.dp, if (isSelected) FrostedNeonCyan else FrostedGlassBorder, RoundedCornerShape(12.dp))
                            .clickable { onSelectSpeed(speed) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${speed}x", color = FrostedTextPrimary, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            if (isSelected) {
                                Text("Active", color = FrostedNeonCyan, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = FrostedNeonCyan)
            }
        },
        containerColor = Color(0xF2121418)
    )
}

@Composable
fun SleepTimerDialog(
    currentMinutes: Int?,
    onSelectMinutes: (Int?) -> Unit,
    onDismiss: () -> Unit,
    accentColor: Color
) {
    val options = listOf(
        Pair("Off (Cancel Timer)", null),
        Pair("15 minutes", 15),
        Pair("30 minutes", 30),
        Pair("45 minutes", 45),
        Pair("60 minutes (1 hour)", 60),
        Pair("90 minutes", 90)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep Timer", color = FrostedTextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                options.forEach { (label, min) ->
                    val isSelected = currentMinutes == min
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) FrostedGlassWhiteHeavy else FrostedGlassWhite)
                            .border(1.dp, if (isSelected) FrostedNeonCyan else FrostedGlassBorder, RoundedCornerShape(12.dp))
                            .clickable { onSelectMinutes(min) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, color = FrostedTextPrimary, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            if (isSelected) {
                                Text("Active", color = FrostedNeonCyan, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = FrostedNeonCyan)
            }
        },
        containerColor = Color(0xF2121418)
    )
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
