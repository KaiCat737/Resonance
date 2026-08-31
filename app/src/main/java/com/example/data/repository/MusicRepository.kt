package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.database.PlaylistEntity
import com.example.data.database.TrackEntity
import com.example.data.model.AudioFormat
import com.example.data.model.AudioTrack
import com.example.data.model.Playlist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class MusicRepository(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val trackDao = database.trackDao()
    private val playlistDao = database.playlistDao()

    val allTracks: Flow<List<AudioTrack>> = trackDao.getAllTracks().map { list ->
        list.map { it.toAudioTrack() }
    }

    val favoriteTracks: Flow<List<AudioTrack>> = trackDao.getFavoriteTracks().map { list ->
        list.map { it.toAudioTrack() }
    }

    val losslessTracks: Flow<List<AudioTrack>> = trackDao.getLosslessTracks().map { list ->
        list.map { it.toAudioTrack() }
    }

    val allPlaylists: Flow<List<Playlist>> = playlistDao.getAllPlaylists().map { list ->
        list.map { it.toPlaylist() }
    }

    suspend fun initializeIfNeeded() = withContext(Dispatchers.IO) {
        try {
            // Purge any old placeholder demo items from earlier builds
            trackDao.deleteDemoTracks()
            
            // Try automatically scanning device media store for existing tracks
            scanDeviceStorageAudio()
        } catch (e: Exception) {
            Log.e("MusicRepo", "Error during repository initialization", e)
        }
    }

    suspend fun toggleFavorite(trackId: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        trackDao.updateFavorite(trackId, isFavorite)
    }

    suspend fun incrementPlayCount(trackId: String) = withContext(Dispatchers.IO) {
        trackDao.incrementPlayCount(trackId)
    }

    suspend fun updateTrackTags(
        trackId: String,
        title: String,
        artist: String,
        album: String,
        genre: String?,
        year: Int?,
        lyrics: String?
    ) = withContext(Dispatchers.IO) {
        val existing = trackDao.getTrackById(trackId) ?: return@withContext
        val updated = existing.copy(
            title = title,
            artist = artist,
            album = album,
            genre = genre,
            year = year,
            lyricsLrc = lyrics
        )
        trackDao.updateTrack(updated)
    }

    suspend fun updateTrackAlbumArt(trackId: String, artUri: String) = withContext(Dispatchers.IO) {
        trackDao.updateAlbumArt(trackId, artUri)
    }

    suspend fun updateAlbumArtForAlbum(album: String, artUri: String) = withContext(Dispatchers.IO) {
        trackDao.updateAlbumArtForAlbum(album, artUri)
    }

    suspend fun deleteTrack(trackId: String) = withContext(Dispatchers.IO) {
        trackDao.deleteTrackById(trackId)
    }

    suspend fun clearAllLibrary() = withContext(Dispatchers.IO) {
        trackDao.clearAll()
        playlistDao.clearAll()
    }

    suspend fun createPlaylist(name: String, description: String = "", accentColor: Long = 0xFF00E5FF): Playlist = withContext(Dispatchers.IO) {
        val newPlaylist = Playlist(
            id = "pl_" + UUID.randomUUID().toString().take(8),
            name = name,
            description = description,
            createdDateMs = System.currentTimeMillis(),
            accentColorHex = accentColor,
            trackIds = emptyList()
        )
        playlistDao.insertPlaylist(PlaylistEntity.fromPlaylist(newPlaylist))
        newPlaylist
    }

    suspend fun updatePlaylist(playlist: Playlist) = withContext(Dispatchers.IO) {
        playlistDao.updatePlaylist(PlaylistEntity.fromPlaylist(playlist))
    }

    suspend fun deletePlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylistById(playlistId)
    }

    suspend fun addTracksToPlaylist(playlistId: String, newTrackIds: List<String>) = withContext(Dispatchers.IO) {
        val entity = playlistDao.getPlaylistById(playlistId) ?: return@withContext
        val currentPlaylist = entity.toPlaylist()
        val updatedTrackIds = (currentPlaylist.trackIds + newTrackIds).distinct()
        val updated = currentPlaylist.copy(trackIds = updatedTrackIds)
        playlistDao.updatePlaylist(PlaylistEntity.fromPlaylist(updated))
    }

    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) = withContext(Dispatchers.IO) {
        val entity = playlistDao.getPlaylistById(playlistId) ?: return@withContext
        val currentPlaylist = entity.toPlaylist()
        val updatedTrackIds = currentPlaylist.trackIds.filter { it != trackId }
        val updated = currentPlaylist.copy(trackIds = updatedTrackIds)
        playlistDao.updatePlaylist(PlaylistEntity.fromPlaylist(updated))
    }

    suspend fun reorderPlaylistTracks(playlistId: String, fromIndex: Int, toIndex: Int) = withContext(Dispatchers.IO) {
        val entity = playlistDao.getPlaylistById(playlistId) ?: return@withContext
        val currentPlaylist = entity.toPlaylist()
        val list = currentPlaylist.trackIds.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            val updated = currentPlaylist.copy(trackIds = list)
            playlistDao.updatePlaylist(PlaylistEntity.fromPlaylist(updated))
        }
    }

    /**
     * Extracts embedded artwork from an audio file and saves it to the app's cache directory.
     */
    private fun extractAndSaveAlbumArt(retriever: MediaMetadataRetriever, keyId: String): String? {
        return try {
            val pictureBytes = retriever.embeddedPicture ?: return null
            val artFile = File(context.cacheDir, "art_${keyId}.jpg")
            FileOutputStream(artFile).use { out ->
                out.write(pictureBytes)
                out.flush()
            }
            Uri.fromFile(artFile).toString()
        } catch (e: Exception) {
            Log.w("MusicRepo", "Could not extract or save album art: ${e.message}")
            null
        }
    }

    /**
     * Scans Android MediaStore for music on the device and imports them into Room.
     */
    suspend fun scanDeviceStorageAudio(): Int = withContext(Dispatchers.IO) {
        var count = 0
        try {
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.MIME_TYPE,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.YEAR
            )

            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)

                val newTracks = mutableListOf<TrackEntity>()

                while (cursor.moveToNext()) {
                    val mediaId = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaId)
                    val title = cursor.getString(titleCol) ?: "Unknown Title"
                    val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                    val album = cursor.getString(albumCol) ?: "Unknown Album"
                    val durationMs = cursor.getLong(durationCol)
                    val size = cursor.getLong(sizeCol)
                    val dataPath = cursor.getString(dataCol) ?: contentUri.toString()
                    val mimeType = cursor.getString(mimeCol) ?: ""
                    val albumId = cursor.getLong(albumIdCol)
                    val year = cursor.getInt(yearCol)

                    val ext = dataPath.substringAfterLast(".", "")
                    val format = AudioFormat.fromExtension(if (ext.isNotEmpty()) ext else mimeType)
                    val isLossless = format.isLossless || ext.lowercase() in listOf("flac", "wav", "alac", "dsd", "aiff")
                    val sampleRateHz = if (isLossless && (ext.equals("flac", true) || ext.equals("wav", true))) 96000 else 44100
                    val bitDepth = if (isLossless) 24 else 16
                    val bitrateKbps = if (isLossless) 2304 else 320

                    // Extract embedded album art or content album art URI
                    var albumArtUri: String? = null
                    try {
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(context, contentUri)
                        albumArtUri = extractAndSaveAlbumArt(retriever, "media_$mediaId")
                        retriever.release()
                    } catch (ignored: Exception) {}

                    if (albumArtUri == null && albumId > 0) {
                        albumArtUri = ContentUris.withAppendedId(
                            Uri.parse("content://media/external/audio/albumart"),
                            albumId
                        ).toString()
                    }

                    val trackId = "device_$mediaId"
                    val track = AudioTrack(
                        id = trackId,
                        title = title,
                        artist = artist,
                        album = album,
                        durationMs = if (durationMs > 0) durationMs else 180000L,
                        filePath = contentUri.toString(),
                        format = format,
                        sampleRateHz = sampleRateHz,
                        bitDepth = bitDepth,
                        bitrateKbps = bitrateKbps,
                        channels = 2,
                        albumArtUri = albumArtUri,
                        albumColorHex = 0xFF1E293B,
                        albumSecondaryColorHex = 0xFF00E5FF,
                        year = if (year > 0) year else null,
                        genre = "Music",
                        isLossless = isLossless,
                        isHiRes = bitDepth >= 24 || sampleRateHz >= 88200,
                        lyricsLrc = null,
                        fileSizeBytes = if (size > 0) size else 15000000L
                    )

                    newTracks.add(TrackEntity.fromAudioTrack(track))
                }

                if (newTracks.isNotEmpty()) {
                    trackDao.insertTracks(newTracks)
                    count = newTracks.size
                }
            }
        } catch (e: Exception) {
            Log.e("MusicRepo", "Failed to scan device audio: ${e.message}", e)
        }
        count
    }

    // Import audio file via SAF Uri
    suspend fun importAudioUri(uri: Uri): AudioTrack? = withContext(Dispatchers.IO) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)

            var fileName = "Imported Audio"
            var fileSize = 0L

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx != -1) fileName = cursor.getString(nameIdx)
                    if (sizeIdx != -1) fileSize = cursor.getLong(sizeIdx)
                }
            }

            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?: fileName.substringBeforeLast(".")
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown Artist"
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Local Collection"
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 180000L
            val genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE) ?: "Audiophile"
            val yearStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
            val year = yearStr?.toIntOrNull() ?: 2024
            val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: ""
            val ext = fileName.substringAfterLast(".", "")

            val format = AudioFormat.fromExtension(if (ext.isNotEmpty()) ext else mimeType)
            val isLossless = format.isLossless || ext.lowercase() in listOf("flac", "wav", "alac", "dsd", "aiff")
            val sampleRateHz = if (isLossless && (ext.equals("flac", true) || ext.equals("wav", true))) 96000 else 44100
            val bitDepth = if (isLossless) 24 else 16
            val bitrateKbps = if (isLossless) (if (sampleRateHz >= 96000) 2304 else 1411) else 320

            val trackId = "imported_" + UUID.randomUUID().toString().take(8)

            // Extract and cache embedded album artwork if present
            val embeddedArtUri = extractAndSaveAlbumArt(retriever, trackId)

            val track = AudioTrack(
                id = trackId,
                title = title,
                artist = artist,
                album = album,
                durationMs = durationMs,
                filePath = uri.toString(),
                format = format,
                sampleRateHz = sampleRateHz,
                bitDepth = bitDepth,
                bitrateKbps = bitrateKbps,
                channels = 2,
                albumArtUri = embeddedArtUri,
                albumColorHex = 0xFF1E293B,
                albumSecondaryColorHex = 0xFF00E5FF,
                year = year,
                genre = genre,
                isLossless = isLossless,
                isHiRes = bitDepth >= 24 || sampleRateHz >= 88200,
                lyricsLrc = null,
                fileSizeBytes = if (fileSize > 0) fileSize else 15000000L
            )

            trackDao.insertTrack(TrackEntity.fromAudioTrack(track))
            retriever.release()
            track
        } catch (e: Exception) {
            Log.e("MusicRepo", "Failed to import audio URI: $uri", e)
            null
        }
    }

    // --- Cross-Platform Synchronization Engine (JSON & M3U8 Export / Import) ---

    suspend fun exportLibrarySyncBundle(): String = withContext(Dispatchers.IO) {
        val tracks = trackDao.getAllTracks().first().map { it.toAudioTrack() }
        val playlists = playlistDao.getAllPlaylists().first().map { it.toPlaylist() }

        val rootObj = JSONObject()
        rootObj.put("app", "Resonance")
        rootObj.put("version", "1.0")
        rootObj.put("exportTimestamp", System.currentTimeMillis())
        rootObj.put("totalTracks", tracks.size)

        // Playlists
        val plArray = JSONArray()
        playlists.forEach { pl ->
            val plObj = JSONObject()
            plObj.put("id", pl.id)
            plObj.put("name", pl.name)
            plObj.put("description", pl.description)
            plObj.put("accentColorHex", pl.accentColorHex)
            val trackIdsArr = JSONArray()
            pl.trackIds.forEach { trackIdsArr.put(it) }
            plObj.put("trackIds", trackIdsArr)
            plArray.put(plObj)
        }
        rootObj.put("playlists", plArray)

        // Custom tags & lyrics
        val trackArray = JSONArray()
        tracks.forEach { t ->
            val tObj = JSONObject()
            tObj.put("id", t.id)
            tObj.put("title", t.title)
            tObj.put("artist", t.artist)
            tObj.put("album", t.album)
            tObj.put("genre", t.genre ?: "")
            tObj.put("year", t.year ?: 0)
            tObj.put("format", t.format.name)
            tObj.put("sampleRateHz", t.sampleRateHz)
            tObj.put("bitDepth", t.bitDepth)
            tObj.put("bitrateKbps", t.bitrateKbps)
            tObj.put("albumArtUri", t.albumArtUri ?: "")
            tObj.put("isFavorite", t.isFavorite)
            tObj.put("playCount", t.playCount)
            tObj.put("lyricsLrc", t.lyricsLrc ?: "")
            trackArray.put(tObj)
        }
        rootObj.put("tracks", trackArray)

        rootObj.toString(2)
    }

    suspend fun importLibrarySyncBundle(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            if (!root.has("app") && !root.has("playlists") && !root.has("tracks")) {
                return@withContext false
            }

            // Restore/merge tracks
            if (root.has("tracks")) {
                val trackArray = root.getJSONArray("tracks")
                val existingTracks = trackDao.getAllTracks().first().associateBy { it.id }.toMutableMap()

                for (i in 0 until trackArray.length()) {
                    val tObj = trackArray.getJSONObject(i)
                    val id = tObj.getString("id")
                    val title = tObj.getString("title")
                    val artist = tObj.getString("artist")
                    val album = tObj.getString("album")
                    val genre = tObj.optString("genre", "Audiophile")
                    val year = tObj.optInt("year", 2024)
                    val format = tObj.optString("format", "FLAC")
                    val sampleRateHz = tObj.optInt("sampleRateHz", 96000)
                    val bitDepth = tObj.optInt("bitDepth", 24)
                    val bitrateKbps = tObj.optInt("bitrateKbps", 2304)
                    val albumArtUri = tObj.optString("albumArtUri", "").ifEmpty { null }
                    val isFavorite = tObj.optBoolean("isFavorite", false)
                    val playCount = tObj.optInt("playCount", 0)
                    val lyricsLrc = tObj.optString("lyricsLrc", "")

                    val existing = existingTracks[id]
                    if (existing != null) {
                        val updated = existing.copy(
                            title = title,
                            artist = artist,
                            album = album,
                            genre = genre,
                            year = year,
                            albumArtUri = albumArtUri ?: existing.albumArtUri,
                            isFavorite = isFavorite,
                            playCount = playCount,
                            lyricsLrc = lyricsLrc.ifEmpty { existing.lyricsLrc }
                        )
                        trackDao.updateTrack(updated)
                    } else {
                        // Create track entity
                        val newTrack = TrackEntity(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = 200000L,
                            filePath = "content://$id",
                            format = format,
                            sampleRateHz = sampleRateHz,
                            bitDepth = bitDepth,
                            bitrateKbps = bitrateKbps,
                            channels = 2,
                            albumArtUri = albumArtUri,
                            albumColorHex = 0xFF2B3A67,
                            albumSecondaryColorHex = 0xFF00E5FF,
                            year = year,
                            genre = genre,
                            trackNumber = 1,
                            discNumber = 1,
                            isLossless = true,
                            isHiRes = true,
                            lyricsLrc = lyricsLrc,
                            isFavorite = isFavorite,
                            playCount = playCount,
                            dateAddedMs = System.currentTimeMillis(),
                            replayGainDb = 0f,
                            fileSizeBytes = 50000000L
                        )
                        trackDao.insertTrack(newTrack)
                    }
                }
            }

            // Restore/merge playlists
            if (root.has("playlists")) {
                val plArray = root.getJSONArray("playlists")
                for (i in 0 until plArray.length()) {
                    val plObj = plArray.getJSONObject(i)
                    val id = plObj.getString("id")
                    val name = plObj.getString("name")
                    val description = plObj.optString("description", "")
                    val accentColor = plObj.optLong("accentColorHex", 0xFF00E5FF)
                    val trackIdsList = mutableListOf<String>()
                    val trackIdsArr = plObj.optJSONArray("trackIds")
                    if (trackIdsArr != null) {
                        for (k in 0 until trackIdsArr.length()) {
                            trackIdsList.add(trackIdsArr.getString(k))
                        }
                    }

                    val playlistEntity = PlaylistEntity(
                        id = id,
                        name = name,
                        description = description,
                        createdDateMs = System.currentTimeMillis(),
                        accentColorHex = accentColor,
                        iconName = "playlist",
                        trackIdsJson = trackIdsList.joinToString(":::"),
                        isPinned = false,
                        isSmartPlaylist = false
                    )
                    playlistDao.insertPlaylist(playlistEntity)
                }
            }
            true
        } catch (e: Exception) {
            Log.e("MusicRepo", "Error restoring sync bundle", e)
            false
        }
    }

    suspend fun exportPlaylistM3u8(playlist: Playlist): String = withContext(Dispatchers.IO) {
        val tracksMap = trackDao.getAllTracks().first().map { it.toAudioTrack() }.associateBy { it.id }
        val sb = StringBuilder()
        sb.append("#EXTM3U\n")
        sb.append("#PLAYLIST:").append(playlist.name).append("\n")
        playlist.trackIds.forEach { id ->
            val track = tracksMap[id]
            if (track != null) {
                sb.append("#EXTINF:").append(track.durationMs / 1000).append(",")
                    .append(track.artist).append(" - ").append(track.title).append("\n")
                sb.append(track.filePath).append("\n")
            }
        }
        sb.toString()
    }
}
