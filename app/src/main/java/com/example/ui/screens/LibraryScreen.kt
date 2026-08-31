package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppThemeMode
import com.example.data.model.AudioFormat
import com.example.data.model.AudioTrack
import com.example.data.model.Playlist
import com.example.ui.components.AlbumArtImage
import com.example.ui.components.HiResBadge
import com.example.ui.components.LosslessPill
import com.example.ui.components.MiniPlayer
import com.example.ui.components.PulsingDot
import com.example.ui.components.ReorderableTrackList
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
import com.example.viewmodel.SortOption
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    tracks: List<AudioTrack>,
    allTracksList: List<AudioTrack>,
    playlists: List<Playlist>,
    currentTrack: AudioTrack?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    searchQuery: String,
    selectedFormatFilter: AudioFormat?,
    onlyLosslessFilter: Boolean,
    sortOption: SortOption,
    appThemeMode: AppThemeMode,
    onSearchQueryChange: (String) -> Unit,
    onFormatFilterSelect: (AudioFormat?) -> Unit,
    onToggleLosslessOnly: () -> Unit,
    onSortOptionSelect: (SortOption) -> Unit,
    onTrackClick: (AudioTrack, List<AudioTrack>) -> Unit,
    onToggleFavorite: (AudioTrack) -> Unit,
    onReorderQueue: (Int, Int) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onOpenMiniPlayerFullscreen: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSync: () -> Unit,
    onCycleTheme: () -> Unit,
    onOpenTagEditor: (AudioTrack) -> Unit,
    onOpenInspector: (AudioTrack) -> Unit,
    onCreatePlaylist: (name: String, desc: String, color: Long) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onAddTrackToPlaylist: (String, String) -> Unit,
    onRemoveTrackFromPlaylist: (String, String) -> Unit,
    onReorderPlaylist: (String, Int, Int) -> Unit,
    onImportAudioUri: (Uri) -> Unit,
    onExportM3u8: suspend (Playlist) -> String,
    onOpenCreatePlaylistDialog: () -> Unit,
    onOpenAddToPlaylistDialog: (AudioTrack) -> Unit,
    accentColor: Color = FrostedNeonCyan,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var isSearchActive by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    // Detail view state for Playlists & Albums
    var activeDetailPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var activeDetailAlbum by remember { mutableStateOf<String?>(null) }

    // SAF Document Picker for local FLAC / WAV audio files
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportAudioUri(uri)
        }
    }

    val tabs = listOf("Tracks", "Playlists", "Albums", "Artists", "Hi-Res Vault")

    FrostedGlassAmbientBackground(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxSize(),
            topBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Main Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(FrostedPurple, FrostedNeonCyan)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Resonance",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = FrostedTextPrimary
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(0x66000000))
                                            .border(1.dp, FrostedGlassBorder, RoundedCornerShape(20.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            PulsingDot(color = FrostedNeonCyan, size = 5)
                                            Text(
                                                text = "LOSSLESS",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = FrostedNeonCyan,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "${allTracksList.size} Lossless Master Tracks",
                                    fontSize = 11.sp,
                                    color = FrostedTextSecondary
                                )
                            }
                        }

                        // Top Action Icons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(FrostedGlassWhite)
                                    .border(1.dp, FrostedGlassBorder, CircleShape)
                                    .clickable { isSearchActive = !isSearchActive },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isSearchActive) Icons.Default.Clear else Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = if (isSearchActive) FrostedNeonCyan else FrostedTextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(FrostedGlassWhite)
                                    .border(1.dp, FrostedGlassBorder, CircleShape)
                                    .clickable { audioPickerLauncher.launch(arrayOf("audio/*", "application/octet-stream")) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileOpen,
                                    contentDescription = "Import FLAC/WAV",
                                    tint = FrostedNeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(FrostedGlassWhite)
                                    .border(1.dp, FrostedGlassBorder, CircleShape)
                                    .clickable { onOpenSync() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Cross-Platform Sync",
                                    tint = FrostedTextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(FrostedGlassWhite)
                                    .border(1.dp, FrostedGlassBorder, CircleShape)
                                    .clickable { onOpenEqualizer() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = "Equalizer",
                                    tint = FrostedTextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(FrostedGlassWhite)
                                    .border(1.dp, FrostedGlassBorder, CircleShape)
                                    .clickable { onCycleTheme() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Theme",
                                    tint = FrostedTextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Search Bar Expandable
                    AnimatedVisibility(visible = isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = { Text("Search songs, artists, albums, or lyrics...", color = FrostedTextMuted) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = FrostedNeonCyan) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchQueryChange("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = FrostedTextSecondary)
                                    }
                                }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0x22FFFFFF),
                                unfocusedContainerColor = FrostedGlassWhite,
                                focusedBorderColor = FrostedNeonCyan,
                                unfocusedBorderColor = FrostedGlassBorder,
                                focusedTextColor = FrostedTextPrimary,
                                unfocusedTextColor = FrostedTextPrimary
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }

                    // Main Tabs Bar (Frosted Pill Segment)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x0CFFFFFF))
                            .border(1.dp, FrostedGlassBorder, RoundedCornerShape(14.dp))
                            .padding(3.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            tabs.forEachIndexed { index, title ->
                                val isSelected = selectedTab == index
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) FrostedGlassWhiteHeavy else Color.Transparent)
                                        .border(
                                            1.dp,
                                            if (isSelected) FrostedGlassBorderHeavy else Color.Transparent,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedTab = index }
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = title,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else FrostedTextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            },
            floatingActionButton = {
                if (selectedTab == 1) { // Playlists tab
                    FloatingActionButton(
                        onClick = onOpenCreatePlaylistDialog,
                        containerColor = FrostedNeonCyan,
                        contentColor = Color.Black,
                        shape = CircleShape,
                        modifier = Modifier.shadow(12.dp, CircleShape, spotColor = FrostedNeonCyan)
                    ) {
                        Icon(Icons.Default.PlaylistAdd, contentDescription = "New Playlist")
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Tab Contents
                    when (selectedTab) {
                        0 -> {
                            // Tracks Tab with Format Filter Chips & Sort
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Filter & Sort Bar
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                        .horizontalScroll(rememberScrollState()),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Sort Dropdown Chip
                                    Box {
                                        FrostedFilterChip(
                                            label = sortOption.displayName,
                                            isSelected = true,
                                            icon = Icons.Default.Sort,
                                            onClick = { showSortMenu = true }
                                        )

                                        DropdownMenu(
                                            expanded = showSortMenu,
                                            onDismissRequest = { showSortMenu = false }
                                        ) {
                                            SortOption.values().forEach { option ->
                                                DropdownMenuItem(
                                                    text = { Text(option.displayName) },
                                                    onClick = {
                                                        onSortOptionSelect(option)
                                                        showSortMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // Format Filters
                                    FrostedFilterChip(
                                        label = "All (${allTracksList.size})",
                                        isSelected = selectedFormatFilter == null && !onlyLosslessFilter,
                                        onClick = { onFormatFilterSelect(null) }
                                    )

                                    FrostedFilterChip(
                                        label = "FLAC",
                                        isSelected = selectedFormatFilter == AudioFormat.FLAC,
                                        onClick = { onFormatFilterSelect(if (selectedFormatFilter == AudioFormat.FLAC) null else AudioFormat.FLAC) }
                                    )

                                    FrostedFilterChip(
                                        label = "WAV",
                                        isSelected = selectedFormatFilter == AudioFormat.WAV,
                                        onClick = { onFormatFilterSelect(if (selectedFormatFilter == AudioFormat.WAV) null else AudioFormat.WAV) }
                                    )

                                    FrostedFilterChip(
                                        label = "24-bit Hi-Res Only",
                                        isSelected = onlyLosslessFilter,
                                        onClick = onToggleLosslessOnly
                                    )
                                }

                                // Reorderable Track List
                                ReorderableTrackList(
                                    tracks = tracks,
                                    currentPlayingTrackId = currentTrack?.id,
                                    isPlaying = isPlaying,
                                    onTrackClick = { onTrackClick(it, tracks) },
                                    onToggleFavorite = onToggleFavorite,
                                    onReorder = onReorderQueue,
                                    onAddToPlaylist = onOpenAddToPlaylistDialog,
                                    onEditTags = onOpenTagEditor,
                                    onViewMetadata = onOpenInspector,
                                    enableDragReorder = false,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                )
                            }
                        }

                        1 -> {
                            // Playlists Tab
                            PlaylistsView(
                                playlists = playlists,
                                allTracks = allTracksList,
                                onPlaylistClick = { activeDetailPlaylist = it },
                                onCreatePlaylist = onOpenCreatePlaylistDialog,
                                onDeletePlaylist = onDeletePlaylist,
                                accentColor = accentColor,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        2 -> {
                            // Albums Grid Tab
                            val albums = allTracksList.groupBy { it.album }
                            AlbumsGridView(
                                albums = albums,
                                onAlbumClick = { albumName -> activeDetailAlbum = albumName },
                                accentColor = accentColor,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        3 -> {
                            // Artists Tab
                            val artists = allTracksList.groupBy { it.artist }
                            ArtistsListView(
                                artists = artists,
                                onTrackClick = { onTrackClick(it, allTracksList) },
                                accentColor = accentColor,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        4 -> {
                            // Hi-Res Audio Vault / Storage Manager
                            HiResVaultView(
                                tracks = allTracksList,
                                onImportFile = { audioPickerLauncher.launch(arrayOf("audio/*", "application/octet-stream")) },
                                onOpenSync = onOpenSync,
                                accentColor = accentColor,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Mini Player docked cleanly at the bottom
                    MiniPlayer(
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        currentPositionMs = currentPositionMs,
                        durationMs = durationMs,
                        onTogglePlayPause = onTogglePlayPause,
                        onSkipNext = onSkipNext,
                        onClick = onOpenMiniPlayerFullscreen
                    )
                }
            }
        }
    }

    // Playlist Detail Bottom Sheet
    if (activeDetailPlaylist != null) {
        val pl = activeDetailPlaylist!!
        val plTracks = pl.trackIds.mapNotNull { id -> allTracksList.find { it.id == id } }
        val scope = rememberCoroutineScope()

        ModalBottomSheet(
            onDismissRequest = { activeDetailPlaylist = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xF2121418)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = pl.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(pl.accentColorHex)
                        )
                        Text(
                            text = "${plTracks.size} tracks • Drag handle to reorder songs",
                            fontSize = 12.sp,
                            color = FrostedTextSecondary
                        )
                    }

                    Row {
                        IconButton(onClick = {
                            scope.launch {
                                onExportM3u8(pl)
                            }
                        }) {
                            Icon(Icons.Default.IosShare, contentDescription = "Export M3U8", tint = Color(pl.accentColorHex))
                        }

                        IconButton(onClick = {
                            onDeletePlaylist(pl.id)
                            activeDetailPlaylist = null
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Playlist", tint = Color(0xFFFF5252))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (plTracks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No tracks in this playlist yet. Add songs from the Tracks tab.", color = FrostedTextMuted)
                    }
                } else {
                    ReorderableTrackList(
                        tracks = plTracks,
                        currentPlayingTrackId = currentTrack?.id,
                        isPlaying = isPlaying,
                        onTrackClick = { onTrackClick(it, plTracks) },
                        onToggleFavorite = onToggleFavorite,
                        onReorder = { from, to ->
                            onReorderPlaylist(pl.id, from, to)
                        },
                        onAddToPlaylist = onOpenAddToPlaylistDialog,
                        onEditTags = onOpenTagEditor,
                        onViewMetadata = onOpenInspector,
                        onRemove = { onRemoveTrackFromPlaylist(pl.id, it.id) },
                        enableDragReorder = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                    )
                }
            }
        }
    }

    // Album Detail Sheet
    if (activeDetailAlbum != null) {
        val albumName = activeDetailAlbum!!
        val albumTracks = allTracksList.filter { it.album == albumName }

        ModalBottomSheet(
            onDismissRequest = { activeDetailAlbum = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xF2121418)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = albumName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = FrostedTextPrimary
                )
                Text(
                    text = "${albumTracks.firstOrNull()?.artist.orEmpty()} • ${albumTracks.size} Tracks",
                    fontSize = 12.sp,
                    color = FrostedTextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                ReorderableTrackList(
                    tracks = albumTracks,
                    currentPlayingTrackId = currentTrack?.id,
                    isPlaying = isPlaying,
                    onTrackClick = { onTrackClick(it, albumTracks) },
                    onToggleFavorite = onToggleFavorite,
                    onReorder = { _, _ -> },
                    onAddToPlaylist = onOpenAddToPlaylistDialog,
                    onEditTags = onOpenTagEditor,
                    onViewMetadata = onOpenInspector,
                    enableDragReorder = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                )
            }
        }
    }
}

@Composable
fun FrostedFilterChip(
    label: String,
    isSelected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) FrostedGlassWhiteHeavy else Color(0x0CFFFFFF))
            .border(
                1.dp,
                if (isSelected) FrostedNeonCyan else FrostedGlassBorder,
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) FrostedNeonCyan else FrostedTextMuted,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else FrostedTextSecondary
            )
        }
    }
}

@Composable
fun PlaylistsView(
    playlists: List<Playlist>,
    allTracks: List<AudioTrack>,
    onPlaylistClick: (Playlist) -> Unit,
    onCreatePlaylist: () -> Unit,
    onDeletePlaylist: (String) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Smart Playlist: Favorites
        item {
            val favTracks = allTracks.filter { it.isFavorite }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(FrostedGlassWhite)
                    .border(1.dp, Color(0x40FF4081), RoundedCornerShape(16.dp))
                    .clickable {
                        onPlaylistClick(
                            Playlist(
                                id = "smart_favorites",
                                name = "Favorite Lossless Tracks",
                                description = "Smart collection of favorited audiophile songs",
                                createdDateMs = System.currentTimeMillis(),
                                accentColorHex = 0xFFFF4081,
                                trackIds = favTracks.map { it.id }
                            )
                        )
                    }
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x33FF4081)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Hd, contentDescription = null, tint = Color(0xFFFF4081), modifier = Modifier.size(26.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Favorite Lossless Tracks", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FrostedTextPrimary)
                        Text("${favTracks.size} Tracks", fontSize = 12.sp, color = FrostedTextSecondary)
                    }
                }
            }
        }

        // Custom User Playlists
        items(playlists) { playlist ->
            val plColor = Color(playlist.accentColorHex)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(FrostedGlassWhite)
                    .border(1.dp, plColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .clickable { onPlaylistClick(playlist) }
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(plColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.QueueMusic, contentDescription = null, tint = plColor, modifier = Modifier.size(26.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(playlist.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FrostedTextPrimary)
                        Text("${playlist.trackIds.size} Tracks • ${playlist.description.ifEmpty { "Custom Playlist" }}", fontSize = 12.sp, color = FrostedTextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumsGridView(
    albums: Map<String, List<AudioTrack>>,
    onAlbumClick: (String) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(albums.entries.toList()) { entry ->
            val albumName = entry.key
            val trackList = entry.value
            val firstTrack = trackList.firstOrNull()

            val albumColor = if (firstTrack != null) Color(firstTrack.albumColorHex) else Color(0xFF1E293B)
            val secColor = if (firstTrack != null) Color(firstTrack.albumSecondaryColorHex) else accentColor

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(FrostedGlassWhite)
                    .border(1.dp, FrostedGlassBorder, RoundedCornerShape(18.dp))
                    .clickable { onAlbumClick(albumName) }
                    .padding(10.dp)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AlbumArtImage(
                            artUri = firstTrack?.albumArtUri,
                            fallbackColorHex = firstTrack?.albumColorHex ?: 0xFF1E293B,
                            fallbackSecondaryHex = firstTrack?.albumSecondaryColorHex ?: 0xFF00E5FF,
                            iconSize = 48.dp,
                            contentDescription = "Cover for $albumName",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = albumName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = FrostedTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "${firstTrack?.artist.orEmpty()} • ${trackList.size} tracks",
                        fontSize = 12.sp,
                        color = FrostedTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun ArtistsListView(
    artists: Map<String, List<AudioTrack>>,
    onTrackClick: (AudioTrack) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(artists.entries.toList()) { entry ->
            val artistName = entry.key
            val trackList = entry.value

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(FrostedGlassWhite)
                    .border(1.dp, FrostedGlassBorder, RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(artistName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FrostedTextPrimary)
                        Text("${trackList.size} Master tracks in library", fontSize = 12.sp, color = FrostedTextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun HiResVaultView(
    tracks: List<AudioTrack>,
    onImportFile: () -> Unit,
    onOpenSync: () -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val totalSizeMb = tracks.sumOf { it.fileSizeBytes } / (1024.0 * 1024.0)
    val flacCount = tracks.count { it.format == AudioFormat.FLAC }
    val wavCount = tracks.count { it.format == AudioFormat.WAV }
    val hiResCount = tracks.count { it.isHiRes }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Vault Header Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(FrostedGlassWhite)
                .border(1.dp, FrostedGlassBorder, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Storage, contentDescription = null, tint = FrostedNeonCyan)
                    Text("Offline Lossless Vault", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = FrostedTextPrimary)
                }

                Text(
                    text = "High-Resolution audio engine with bit-perfect direct output pipeline. Supports native uncompressed 24-bit/96kHz and 32-bit/192kHz audio streams.",
                    fontSize = 12.sp,
                    color = FrostedTextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    VaultStatBox(label = "Total Vault Size", value = "%.1f MB".format(totalSizeMb), color = FrostedNeonCyan)
                    VaultStatBox(label = "FLAC Lossless", value = "$flacCount tracks", color = Color(AudioFormat.FLAC.badgeColor))
                    VaultStatBox(label = "WAV Studio", value = "$wavCount tracks", color = Color(AudioFormat.WAV.badgeColor))
                }
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(FrostedNeonCyan)
                    .clickable { onImportFile() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Text("Import FLAC / WAV", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(FrostedGlassWhite)
                    .border(1.dp, FrostedGlassBorder, RoundedCornerShape(14.dp))
                    .clickable { onOpenSync() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, tint = FrostedTextPrimary, modifier = Modifier.size(16.dp))
                    Text("Cross-Platform Sync", color = FrostedTextPrimary, fontWeight = FontWeight.Medium, fontSize = 12.5.sp)
                }
            }
        }
    }
}

@Composable
fun VaultStatBox(label: String, value: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x22000000))
            .border(1.dp, FrostedGlassBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, fontSize = 10.sp, color = FrostedTextSecondary)
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color, fontFamily = FontFamily.Monospace)
        }
    }
}
