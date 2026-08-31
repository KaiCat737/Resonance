package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.example.data.model.AppThemeMode
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.AudioMetadataInspectorDialog
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.CrossPlatformSyncDialog
import com.example.ui.components.EqualizerBottomSheet
import com.example.ui.components.TagEditorDialog
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.NowPlayingScreen
import com.example.ui.theme.ResonanceTheme
import com.example.viewmodel.MusicPlayerViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MusicPlayerViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission result handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestNotificationPermissionIfNeeded()

        setContent {
            val currentTrack by viewModel.currentTrack.collectAsState()
            val appThemeMode by viewModel.appThemeMode.collectAsState()

            ResonanceTheme(
                themeMode = appThemeMode,
                currentTrack = currentTrack
            ) {
                ResonanceApp(viewModel = viewModel)
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun ResonanceApp(viewModel: MusicPlayerViewModel) {
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPositionMs by viewModel.currentPositionMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val playbackMode by viewModel.playbackMode.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val nowPlayingMode by viewModel.nowPlayingMode.collectAsState()
    val visualizerStyle by viewModel.visualizerStyle.collectAsState()
    val spectrumData by viewModel.spectrumData.collectAsState()
    val lyrics by viewModel.parsedLyrics.collectAsState()
    val activeLyricIndex by viewModel.activeLyricIndex.collectAsState()
    val lyricOffsetMs by viewModel.lyricOffsetMs.collectAsState()
    val appThemeMode by viewModel.appThemeMode.collectAsState()
    val sleepTimerMinutes by viewModel.sleepTimerMinutes.collectAsState()

    val allTracks by viewModel.allTracks.collectAsState()
    val filteredTracks by viewModel.filteredTracks.collectAsState()
    val allPlaylists by viewModel.allPlaylists.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFormatFilter by viewModel.selectedFormatFilter.collectAsState()
    val onlyLosslessFilter by viewModel.onlyLosslessFilter.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()

    val showFullscreenPlayer by viewModel.showFullscreenPlayer.collectAsState()
    val showEqualizerSheet by viewModel.showEqualizerSheet.collectAsState()
    val showTagEditorTrack by viewModel.showTagEditorTrack.collectAsState()
    val showMetadataInspectorTrack by viewModel.showMetadataInspectorTrack.collectAsState()
    val showSyncDialog by viewModel.showSyncDialog.collectAsState()
    val showCreatePlaylistDialog by viewModel.showCreatePlaylistDialog.collectAsState()
    val showAddToPlaylistTrack by viewModel.showAddToPlaylistTrack.collectAsState()
    val equalizerState by viewModel.equalizerState.collectAsState()

    val primaryAccent = if (currentTrack != null) Color(currentTrack!!.albumSecondaryColorHex) else Color(0xFF00E5FF)

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = showFullscreenPlayer,
            transitionSpec = {
                if (targetState) {
                    (slideInVertically(initialOffsetY = { it }) + fadeIn()) togetherWith (fadeOut())
                } else {
                    (fadeIn()) togetherWith (slideOutVertically(targetOffsetY = { it }) + fadeOut())
                }
            },
            label = "playerTransition"
        ) { isFullscreen ->
            if (isFullscreen) {
                NowPlayingScreen(
                    currentTrack = currentTrack,
                    isPlaying = isPlaying,
                    currentPositionMs = currentPositionMs,
                    durationMs = durationMs,
                    queue = queue,
                    playbackMode = playbackMode,
                    playbackSpeed = playbackSpeed,
                    displayMode = nowPlayingMode,
                    visualizerStyle = visualizerStyle,
                    spectrumData = spectrumData,
                    lyrics = lyrics,
                    activeLyricIndex = activeLyricIndex,
                    lyricOffsetMs = lyricOffsetMs,
                    appThemeMode = appThemeMode,
                    sleepTimerMinutes = sleepTimerMinutes,
                    onCollapse = { viewModel.showFullscreenPlayer.value = false },
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onSeekTo = { viewModel.seekTo(it) },
                    onSkipNext = { viewModel.skipNext() },
                    onSkipPrevious = { viewModel.skipPrevious() },
                    onTogglePlaybackMode = { viewModel.togglePlaybackMode() },
                    onSetPlaybackSpeed = { viewModel.setPlaybackSpeed(it) },
                    onSetDisplayMode = { viewModel.setNowPlayingMode(it) },
                    onCycleVisualizerStyle = {
                        val next = when (visualizerStyle) {
                            com.example.data.model.VisualizerStyle.BARS -> com.example.data.model.VisualizerStyle.LIQUID
                            com.example.data.model.VisualizerStyle.LIQUID -> com.example.data.model.VisualizerStyle.OSCILLOSCOPE
                            com.example.data.model.VisualizerStyle.OSCILLOSCOPE -> com.example.data.model.VisualizerStyle.VINYL_GROOVE
                            com.example.data.model.VisualizerStyle.VINYL_GROOVE -> com.example.data.model.VisualizerStyle.BARS
                        }
                        viewModel.setVisualizerStyle(next)
                    },
                    onCycleTheme = {
                        val next = when (appThemeMode) {
                            AppThemeMode.FROSTED_GLASS -> AppThemeMode.ALBUM_DYNAMIC
                            AppThemeMode.ALBUM_DYNAMIC -> AppThemeMode.OBSIDIAN_OLED
                            AppThemeMode.OBSIDIAN_OLED -> AppThemeMode.NEON_CYBER
                            AppThemeMode.NEON_CYBER -> AppThemeMode.STUDIO_AMBER
                            AppThemeMode.STUDIO_AMBER -> AppThemeMode.MINIMAL_SLATE
                            AppThemeMode.MINIMAL_SLATE -> AppThemeMode.FROSTED_GLASS
                        }
                        viewModel.setAppThemeMode(next)
                    },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onAdjustLyricOffset = { viewModel.adjustLyricOffset(it) },
                    onResetLyricOffset = { viewModel.resetLyricOffset() },
                    onSaveLyrics = { updatedLrc ->
                        currentTrack?.let {
                            viewModel.updateTrackTags(it.id, it.title, it.artist, it.album, it.genre, it.year, updatedLrc)
                        }
                    },
                    onReorderQueue = { from, to -> viewModel.reorderQueue(from, to) },
                    onOpenEqualizer = { viewModel.showEqualizerSheet.value = true },
                    onOpenTagEditor = { viewModel.showTagEditorTrack.value = it },
                    onOpenInspector = { viewModel.showMetadataInspectorTrack.value = it },
                    onOpenSync = { viewModel.showSyncDialog.value = true },
                    onSetSleepTimer = { viewModel.setSleepTimer(it) }
                )
            } else {
                LibraryScreen(
                    tracks = filteredTracks,
                    allTracksList = allTracks,
                    playlists = allPlaylists,
                    currentTrack = currentTrack,
                    isPlaying = isPlaying,
                    currentPositionMs = currentPositionMs,
                    durationMs = durationMs,
                    searchQuery = searchQuery,
                    selectedFormatFilter = selectedFormatFilter,
                    onlyLosslessFilter = onlyLosslessFilter,
                    sortOption = sortOption,
                    appThemeMode = appThemeMode,
                    onSearchQueryChange = { viewModel.searchQuery.value = it },
                    onFormatFilterSelect = { viewModel.selectedFormatFilter.value = it },
                    onToggleLosslessOnly = { viewModel.onlyLosslessFilter.value = !viewModel.onlyLosslessFilter.value },
                    onSortOptionSelect = { viewModel.sortOption.value = it },
                    onTrackClick = { track, sourceList ->
                        viewModel.playTrack(track, sourceList)
                    },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onReorderQueue = { from, to -> viewModel.reorderQueue(from, to) },
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onSkipNext = { viewModel.skipNext() },
                    onOpenMiniPlayerFullscreen = { viewModel.showFullscreenPlayer.value = true },
                    onOpenEqualizer = { viewModel.showEqualizerSheet.value = true },
                    onOpenSync = { viewModel.showSyncDialog.value = true },
                    onCycleTheme = {
                        val next = when (appThemeMode) {
                            AppThemeMode.FROSTED_GLASS -> AppThemeMode.ALBUM_DYNAMIC
                            AppThemeMode.ALBUM_DYNAMIC -> AppThemeMode.OBSIDIAN_OLED
                            AppThemeMode.OBSIDIAN_OLED -> AppThemeMode.NEON_CYBER
                            AppThemeMode.NEON_CYBER -> AppThemeMode.STUDIO_AMBER
                            AppThemeMode.STUDIO_AMBER -> AppThemeMode.MINIMAL_SLATE
                            AppThemeMode.MINIMAL_SLATE -> AppThemeMode.FROSTED_GLASS
                        }
                        viewModel.setAppThemeMode(next)
                    },
                    onOpenTagEditor = { viewModel.showTagEditorTrack.value = it },
                    onOpenInspector = { viewModel.showMetadataInspectorTrack.value = it },
                    onCreatePlaylist = { name, desc, color -> viewModel.createPlaylist(name, desc, color) },
                    onDeletePlaylist = { viewModel.deletePlaylist(it) },
                    onAddTrackToPlaylist = { plId, trackId -> viewModel.addTrackToPlaylist(plId, trackId) },
                    onRemoveTrackFromPlaylist = { plId, trackId -> viewModel.removeTrackFromPlaylist(plId, trackId) },
                    onReorderPlaylist = { plId, from, to -> viewModel.reorderPlaylist(plId, from, to) },
                    onImportAudioUri = { viewModel.importAudioFile(it) },
                    onExportM3u8 = { pl -> viewModel.exportPlaylistM3u8(pl) },
                    onOpenCreatePlaylistDialog = { viewModel.showCreatePlaylistDialog.value = true },
                    onOpenAddToPlaylistDialog = { viewModel.showAddToPlaylistTrack.value = it },
                    accentColor = primaryAccent
                )
            }
        }

        // Equalizer Bottom Sheet
        if (showEqualizerSheet) {
            EqualizerBottomSheet(
                equalizerState = equalizerState,
                onToggleEnabled = { viewModel.toggleEqualizerEnabled() },
                onSelectPreset = { viewModel.setEqualizerPreset(it) },
                onBandGainChange = { band, gain -> viewModel.updateEqualizerBand(band, gain) },
                onBassBoostChange = { viewModel.setBassBoost(it) },
                onVirtualizerChange = { viewModel.setVirtualizer(it) },
                onPreampChange = { viewModel.setPreampGain(it) },
                onDismiss = { viewModel.showEqualizerSheet.value = false },
                accentColor = primaryAccent
            )
        }

        // Metadata Inspector Dialog
        showMetadataInspectorTrack?.let { track ->
            AudioMetadataInspectorDialog(
                track = track,
                onDismiss = { viewModel.showMetadataInspectorTrack.value = null },
                onOpenTagEditor = {
                    viewModel.showMetadataInspectorTrack.value = null
                    viewModel.showTagEditorTrack.value = track
                }
            )
        }

        // Tag Editor Dialog
        showTagEditorTrack?.let { track ->
            TagEditorDialog(
                track = track,
                onDismiss = { viewModel.showTagEditorTrack.value = null },
                onSave = { title, artist, album, genre, year, lrc ->
                    viewModel.updateTrackTags(track.id, title, artist, album, genre, year, lrc)
                    viewModel.showTagEditorTrack.value = null
                }
            )
        }

        // Cross-Platform Sync Dialog
        if (showSyncDialog) {
            CrossPlatformSyncDialog(
                onDismiss = { viewModel.showSyncDialog.value = false },
                onExportJson = { viewModel.exportLibraryJson() },
                onImportJson = { viewModel.importLibraryJson(it) },
                accentColor = primaryAccent
            )
        }

        // Create Playlist Dialog
        if (showCreatePlaylistDialog) {
            CreatePlaylistDialog(
                onDismiss = { viewModel.showCreatePlaylistDialog.value = false },
                onCreate = { name, desc, color ->
                    viewModel.createPlaylist(name, desc, color)
                    viewModel.showCreatePlaylistDialog.value = false
                },
                defaultAccentColor = primaryAccent
            )
        }

        // Add to Playlist Selection Dialog
        showAddToPlaylistTrack?.let { track ->
            AddToPlaylistDialog(
                track = track,
                playlists = allPlaylists,
                onDismiss = { viewModel.showAddToPlaylistTrack.value = null },
                onAddToPlaylist = { playlistId ->
                    viewModel.addTrackToPlaylist(playlistId, track.id)
                    viewModel.showAddToPlaylistTrack.value = null
                },
                onCreateNewPlaylist = {
                    viewModel.showAddToPlaylistTrack.value = null
                    viewModel.showCreatePlaylistDialog.value = true
                },
                accentColor = primaryAccent
            )
        }
    }
}
