package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class AudioFormat(val displayName: String, val isLossless: Boolean, val badgeColor: Long) {
    FLAC("FLAC", true, 0xFF00E5FF),
    WAV("WAV", true, 0xFFFFB300),
    ALAC("ALAC", true, 0xFF76FF03),
    DSD("DSD", true, 0xFFFF4081),
    AIFF("AIFF", true, 0xFF7C4DFF),
    MP3("MP3", false, 0xFF9E9E9E),
    AAC("AAC", false, 0xFF29B6F6),
    OGG("OGG", false, 0xFF26A69A);

    companion object {
        fun fromExtension(ext: String): AudioFormat {
            return when (ext.lowercase()) {
                "flac" -> FLAC
                "wav", "wave" -> WAV
                "alac", "m4a" -> ALAC
                "dsd", "dsf", "dff" -> DSD
                "aiff", "aif" -> AIFF
                "mp3" -> MP3
                "aac" -> AAC
                "ogg", "oga" -> OGG
                else -> if (ext.contains("flac")) FLAC else if (ext.contains("wav")) WAV else MP3
            }
        }
    }
}

enum class PlaybackMode {
    REPEAT_ALL,
    REPEAT_ONE,
    SHUFFLE,
    OFF
}

enum class VisualizerStyle(val displayName: String) {
    BARS("Neon Spectrum"),
    VINYL_GROOVE("Vinyl Wave"),
    LIQUID("Liquid Flow"),
    OSCILLOSCOPE("Oscilloscope")
}

enum class AppThemeMode(val displayName: String) {
    FROSTED_GLASS("Frosted Glass"),
    ALBUM_DYNAMIC("Album Dynamic"),
    OBSIDIAN_OLED("Obsidian OLED"),
    NEON_CYBER("Cyber Neon"),
    STUDIO_AMBER("Studio Gold"),
    MINIMAL_SLATE("Nordic Slate")
}

data class AudioTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val filePath: String,
    val format: AudioFormat = AudioFormat.FLAC,
    val sampleRateHz: Int = 96000,
    val bitDepth: Int = 24,
    val bitrateKbps: Int = 2304,
    val channels: Int = 2,
    val albumArtUri: String? = null,
    val albumColorHex: Long = 0xFF2B3A67,
    val albumSecondaryColorHex: Long = 0xFF496A81,
    val year: Int? = 2024,
    val genre: String? = "Audiophile Master",
    val trackNumber: Int? = 1,
    val discNumber: Int? = 1,
    val isLossless: Boolean = true,
    val isHiRes: Boolean = true,
    val lyricsLrc: String? = null,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val dateAddedMs: Long = System.currentTimeMillis(),
    val replayGainDb: Float = 0f,
    val fileSizeBytes: Long = 25000000L
) {
    val hiResFormattedTag: String
        get() = "${format.displayName} • ${bitDepth}-bit / ${(sampleRateHz / 1000.0).let { if (it % 1.0 == 0.0) it.toInt().toString() else "%.1f".format(it) }} kHz • $bitrateKbps kbps"

    val durationFormatted: String
        get() {
            val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
}

data class Playlist(
    val id: String,
    val name: String,
    val description: String = "",
    val createdDateMs: Long = System.currentTimeMillis(),
    val accentColorHex: Long = 0xFF00E5FF,
    val iconName: String = "playlist",
    val trackIds: List<String> = emptyList(),
    val isPinned: Boolean = false,
    val isSmartPlaylist: Boolean = false
)

data class LyricLine(
    val timestampMs: Long,
    val text: String,
    val translation: String? = null
)

data class EqualizerState(
    val enabled: Boolean = true,
    val presetName: String = "Audiophile Master",
    val bandGains: List<Float> = listOf(2.5f, 2.0f, 1.0f, 0.5f, 0.0f, 0.5f, 1.5f, 2.5f, 3.0f, 3.5f), // 10 bands
    val bassBoost: Float = 0.25f, // 0.0 to 1.0
    val virtualizer3D: Float = 0.20f, // 0.0 to 1.0
    val preampGainDb: Float = 0.0f // -6 to +6 dB
)

data class CrossPlatformSyncBundle(
    val appVersion: String = "1.0",
    val exportDateMs: Long = System.currentTimeMillis(),
    val deviceName: String = "Resonance Android",
    val totalTracks: Int = 0,
    val playlists: List<PlaylistSyncItem> = emptyList(),
    val favorites: List<String> = emptyList(),
    val customTrackTags: List<CustomTagSyncItem> = emptyList(),
    val customLyrics: List<CustomLyricsSyncItem> = emptyList()
)

data class PlaylistSyncItem(
    val id: String,
    val name: String,
    val description: String,
    val trackTitlesAndArtists: List<Pair<String, String>>,
    val trackIds: List<String>,
    val accentColorHex: Long
)

data class CustomTagSyncItem(
    val trackId: String,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String?,
    val year: Int?
)

data class CustomLyricsSyncItem(
    val trackId: String,
    val title: String,
    val artist: String,
    val lrcContent: String
)
