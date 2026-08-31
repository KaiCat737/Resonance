package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.AudioFormat
import com.example.data.model.AudioTrack
import com.example.data.model.Playlist

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val filePath: String,
    val format: String,
    val sampleRateHz: Int,
    val bitDepth: Int,
    val bitrateKbps: Int,
    val channels: Int,
    val albumArtUri: String?,
    val albumColorHex: Long,
    val albumSecondaryColorHex: Long,
    val year: Int?,
    val genre: String?,
    val trackNumber: Int?,
    val discNumber: Int?,
    val isLossless: Boolean,
    val isHiRes: Boolean,
    val lyricsLrc: String?,
    val isFavorite: Boolean,
    val playCount: Int,
    val dateAddedMs: Long,
    val replayGainDb: Float,
    val fileSizeBytes: Long
) {
    fun toAudioTrack(): AudioTrack = AudioTrack(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        filePath = filePath,
        format = AudioFormat.valueOf(format.ifEmpty { "FLAC" }),
        sampleRateHz = sampleRateHz,
        bitDepth = bitDepth,
        bitrateKbps = bitrateKbps,
        channels = channels,
        albumArtUri = albumArtUri,
        albumColorHex = albumColorHex,
        albumSecondaryColorHex = albumSecondaryColorHex,
        year = year,
        genre = genre,
        trackNumber = trackNumber,
        discNumber = discNumber,
        isLossless = isLossless,
        isHiRes = isHiRes,
        lyricsLrc = lyricsLrc,
        isFavorite = isFavorite,
        playCount = playCount,
        dateAddedMs = dateAddedMs,
        replayGainDb = replayGainDb,
        fileSizeBytes = fileSizeBytes
    )

    companion object {
        fun fromAudioTrack(track: AudioTrack): TrackEntity = TrackEntity(
            id = track.id,
            title = track.title,
            artist = track.artist,
            album = track.album,
            durationMs = track.durationMs,
            filePath = track.filePath,
            format = track.format.name,
            sampleRateHz = track.sampleRateHz,
            bitDepth = track.bitDepth,
            bitrateKbps = track.bitrateKbps,
            channels = track.channels,
            albumArtUri = track.albumArtUri,
            albumColorHex = track.albumColorHex,
            albumSecondaryColorHex = track.albumSecondaryColorHex,
            year = track.year,
            genre = track.genre,
            trackNumber = track.trackNumber,
            discNumber = track.discNumber,
            isLossless = track.isLossless,
            isHiRes = track.isHiRes,
            lyricsLrc = track.lyricsLrc,
            isFavorite = track.isFavorite,
            playCount = track.playCount,
            dateAddedMs = track.dateAddedMs,
            replayGainDb = track.replayGainDb,
            fileSizeBytes = track.fileSizeBytes
        )
    }
}

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val createdDateMs: Long,
    val accentColorHex: Long,
    val iconName: String,
    val trackIdsJson: String, // Stored as comma-separated or json string
    val isPinned: Boolean,
    val isSmartPlaylist: Boolean
) {
    fun toPlaylist(): Playlist {
        val ids = if (trackIdsJson.isBlank()) emptyList() else trackIdsJson.split(":::").filter { it.isNotBlank() }
        return Playlist(
            id = id,
            name = name,
            description = description,
            createdDateMs = createdDateMs,
            accentColorHex = accentColorHex,
            iconName = iconName,
            trackIds = ids,
            isPinned = isPinned,
            isSmartPlaylist = isSmartPlaylist
        )
    }

    companion object {
        fun fromPlaylist(playlist: Playlist): PlaylistEntity = PlaylistEntity(
            id = playlist.id,
            name = playlist.name,
            description = playlist.description,
            createdDateMs = playlist.createdDateMs,
            accentColorHex = playlist.accentColorHex,
            iconName = playlist.iconName,
            trackIdsJson = playlist.trackIds.joinToString(":::"),
            isPinned = playlist.isPinned,
            isSmartPlaylist = playlist.isSmartPlaylist
        )
    }
}
