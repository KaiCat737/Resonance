package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.model.AudioTrack
import com.example.ui.theme.FrostedGlassBorder
import com.example.ui.theme.FrostedGlassBorderHeavy
import com.example.ui.theme.FrostedGlassWhite
import com.example.ui.theme.FrostedGlassWhiteHeavy
import com.example.ui.theme.FrostedNeonCyan
import com.example.ui.theme.FrostedPurple
import com.example.ui.theme.FrostedTextMuted
import com.example.ui.theme.FrostedTextPrimary
import com.example.ui.theme.FrostedTextSecondary

@Composable
fun ReorderableTrackList(
    tracks: List<AudioTrack>,
    currentPlayingTrackId: String?,
    isPlaying: Boolean,
    onTrackClick: (AudioTrack) -> Unit,
    onToggleFavorite: (AudioTrack) -> Unit,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    onAddToPlaylist: (AudioTrack) -> Unit,
    onEditTags: (AudioTrack) -> Unit,
    onViewMetadata: (AudioTrack) -> Unit,
    onRemove: ((AudioTrack) -> Unit)? = null,
    modifier: Modifier = Modifier,
    enableDragReorder: Boolean = true
) {
    if (tracks.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(FrostedGlassWhite)
                        .border(1.dp, FrostedGlassBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Album,
                        contentDescription = null,
                        tint = FrostedNeonCyan,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = "No Audio Files Found",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = FrostedTextPrimary
                )

                Text(
                    text = "Import FLAC, WAV or audio files from your device storage to begin listening in pure high resolution.",
                    fontSize = 13.sp,
                    color = FrostedTextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }
        return
    }

    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(tracks, key = { _, item -> item.id }) { index, track ->
            val isCurrentPlaying = track.id == currentPlayingTrackId
            val isBeingDragged = index == draggedIndex

            val elevation by animateDpAsState(
                targetValue = if (isBeingDragged) 14.dp else 0.dp,
                label = "dragElevation"
            )

            val scale by animateFloatAsState(
                targetValue = if (isBeingDragged) 1.03f else 1.0f,
                label = "dragScale"
            )

            var showItemMenu by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(if (isBeingDragged) 10f else 1f)
                    .scale(scale)
                    .then(if (isBeingDragged) Modifier.shadow(elevation, RoundedCornerShape(16.dp), spotColor = FrostedNeonCyan) else Modifier)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isCurrentPlaying) {
                            Color(0x226200EE)
                        } else {
                            FrostedGlassWhite
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = if (isCurrentPlaying) {
                            FrostedNeonCyan
                        } else {
                            FrostedGlassBorder
                        },
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onTrackClick(track) }
                    .testTag("track_item_${track.id}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Drag Handle
                    if (enableDragReorder) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .pointerInput(tracks) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggedIndex = index
                                            dragOffsetY = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffsetY += dragAmount.y
                                            val threshold = 70f
                                            if (dragOffsetY > threshold && index < tracks.size - 1) {
                                                onReorder(index, index + 1)
                                                draggedIndex = index + 1
                                                dragOffsetY = 0f
                                            } else if (dragOffsetY < -threshold && index > 0) {
                                                onReorder(index, index - 1)
                                                draggedIndex = index - 1
                                                dragOffsetY = 0f
                                            }
                                        },
                                        onDragEnd = {
                                            draggedIndex = -1
                                            dragOffsetY = 0f
                                        },
                                        onDragCancel = {
                                            draggedIndex = -1
                                            dragOffsetY = 0f
                                        }
                                    )
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = "Drag to reorder",
                                tint = if (isBeingDragged) FrostedNeonCyan else FrostedTextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Album Art Mini
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, FrostedGlassBorder, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AlbumArtImage(
                            artUri = track.albumArtUri,
                            fallbackColorHex = track.albumColorHex,
                            fallbackSecondaryHex = track.albumSecondaryColorHex,
                            iconSize = 22.dp,
                            contentDescription = "Cover for ${track.album}",
                            modifier = Modifier.fillMaxSize()
                        )

                        if (isCurrentPlaying && isPlaying) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0x88000000)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Equalizer,
                                    contentDescription = "Playing",
                                    tint = FrostedNeonCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Track Metadata
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = track.title,
                                fontSize = 14.sp,
                                fontWeight = if (isCurrentPlaying) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isCurrentPlaying) FrostedNeonCyan else FrostedTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            LosslessPill(format = track.format, isHiRes = track.isHiRes)
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${track.artist} • ${track.album}",
                                fontSize = 12.sp,
                                color = FrostedTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            Text(
                                text = track.durationFormatted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = FrostedTextMuted
                            )
                        }
                    }

                    // Up / Down Quick Shift for accessibility & precision
                    if (enableDragReorder) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (index > 0) {
                                IconButton(
                                    onClick = { onReorder(index, index - 1) },
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Icon(
                                        Icons.Default.KeyboardArrowUp,
                                        contentDescription = "Move Up",
                                        modifier = Modifier.size(16.dp),
                                        tint = FrostedTextMuted
                                    )
                                }
                            }
                            if (index < tracks.size - 1) {
                                IconButton(
                                    onClick = { onReorder(index, index + 1) },
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Icon(
                                        Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Move Down",
                                        modifier = Modifier.size(16.dp),
                                        tint = FrostedTextMuted
                                    )
                                }
                            }
                        }
                    }

                    // Favorite Button
                    IconButton(
                        onClick = { onToggleFavorite(track) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (track.isFavorite) Color(0xFFFF4081) else FrostedTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Options Menu
                    Box {
                        IconButton(
                            onClick = { showItemMenu = true },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = FrostedTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showItemMenu,
                            onDismissRequest = { showItemMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Hi-Res Metadata Inspector") },
                                onClick = {
                                    showItemMenu = false
                                    onViewMetadata(track)
                                },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Edit ID3 / Vorbis Tags") },
                                onClick = {
                                    showItemMenu = false
                                    onEditTags(track)
                                },
                                leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Add to Playlist") },
                                onClick = {
                                    showItemMenu = false
                                    onAddToPlaylist(track)
                                },
                                leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) }
                            )
                            if (onRemove != null) {
                                DropdownMenuItem(
                                    text = { Text("Remove from Playlist") },
                                    onClick = {
                                        showItemMenu = false
                                        onRemove(track)
                                    },
                                    leadingIcon = { Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, tint = Color(0xFFFF5252)) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
