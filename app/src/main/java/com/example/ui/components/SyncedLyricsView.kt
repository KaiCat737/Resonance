package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AudioTrack
import com.example.data.model.LyricLine
import com.example.util.LyricsParser

@Composable
fun SyncedLyricsView(
    lyrics: List<LyricLine>,
    activeLyricIndex: Int,
    lyricOffsetMs: Long,
    currentTrack: AudioTrack?,
    currentPositionMs: Long,
    onSeekTo: (Long) -> Unit,
    onAdjustOffset: (Long) -> Unit,
    onResetOffset: () -> Unit,
    onSaveLyrics: (String) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var isUserScrolling by remember { mutableStateOf(false) }
    var showLrcEditor by remember { mutableStateOf(false) }

    // Auto scroll to active lyric
    LaunchedEffect(activeLyricIndex) {
        if (activeLyricIndex in lyrics.indices && !isUserScrolling) {
            val targetScroll = (activeLyricIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(targetScroll)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Offset adjustment and Editor Header Bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Sync offset controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Sync Offset",
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )

                    Text(
                        text = "Offset: ${if (lyricOffsetMs >= 0) "+$lyricOffsetMs" else lyricOffsetMs}ms",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(
                        onClick = { onAdjustOffset(-500L) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "-0.5s", modifier = Modifier.size(14.dp))
                    }

                    IconButton(
                        onClick = { onAdjustOffset(500L) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "+0.5s", modifier = Modifier.size(14.dp))
                    }

                    if (lyricOffsetMs != 0L) {
                        IconButton(
                            onClick = onResetOffset,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = "Reset offset", modifier = Modifier.size(14.dp), tint = accentColor)
                        }
                    }
                }

                // Edit LRC Button
                OutlinedButton(
                    onClick = { showLrcEditor = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.EditNote, contentDescription = "Edit LRC", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit LRC", fontSize = 11.sp)
                }
            }
        }

        if (lyrics.isEmpty()) {
            // Empty lyrics placeholder
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatQuote,
                        contentDescription = "No Lyrics",
                        tint = accentColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(54.dp)
                    )
                    Text(
                        text = "No Synced Lyrics Found",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "You can add or create time-synchronized LRC lyrics using the editor below.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Button(
                        onClick = { showLrcEditor = true },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Icon(Icons.Default.EditNote, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add LRC Lyrics")
                    }
                }
            }
        } else {
            // Synced lyrics list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 60.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                itemsIndexed(lyrics) { index, line ->
                    val isActive = index == activeLyricIndex
                    val isPast = index < activeLyricIndex

                    val textColor by animateColorAsState(
                        targetValue = when {
                            isActive -> accentColor
                            isPast -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        },
                        animationSpec = tween(300),
                        label = "lyricColor"
                    )

                    val scale by animateFloatAsState(
                        targetValue = if (isActive) 1.06f else 0.96f,
                        animationSpec = tween(300),
                        label = "lyricScale"
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onSeekTo(line.timestampMs)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .scale(scale),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = line.text,
                            color = textColor,
                            fontSize = if (isActive) 20.sp else 16.sp,
                            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            lineHeight = if (isActive) 28.sp else 24.sp,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (isActive) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(accentColor)
                                )
                                Text(
                                    text = LyricsParser.formatTimestamp(line.timestampMs),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = accentColor.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLrcEditor) {
        LrcEditorDialog(
            initialLrc = currentTrack?.lyricsLrc.orEmpty(),
            currentPositionMs = currentPositionMs,
            onDismiss = { showLrcEditor = false },
            onSave = { updatedLrc ->
                onSaveLyrics(updatedLrc)
                showLrcEditor = false
            },
            accentColor = accentColor
        )
    }
}

@Composable
fun LrcEditorDialog(
    initialLrc: String,
    currentPositionMs: Long,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    accentColor: Color
) {
    var textContent by remember { mutableStateOf(initialLrc) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.EditNote, contentDescription = null, tint = accentColor)
                Text("LRC Lyrics Editor")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Standard LRC format: [mm:ss.xx] Lyric text",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Quick Stamp Button
                Button(
                    onClick = {
                        val stamp = "[${LyricsParser.formatTimestamp(currentPositionMs)}] "
                        textContent = if (textContent.isEmpty()) stamp else "$textContent\n$stamp"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.2f), contentColor = accentColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Stamp Current Playback Time (${LyricsParser.formatTimestamp(currentPositionMs)})")
                }

                OutlinedTextField(
                    value = textContent,
                    onValueChange = { textContent = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    placeholder = { Text("[00:00.00] Song intro\n[00:15.00] First lyric line") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(textContent) },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text("Save Lyrics")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
