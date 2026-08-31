package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks ORDER BY title ASC")
    fun getAllTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavoriteTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE isLossless = 1 ORDER BY title ASC")
    fun getLosslessTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE format = :format ORDER BY title ASC")
    fun getTracksByFormat(format: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE id = :id LIMIT 1")
    suspend fun getTrackById(id: String): TrackEntity?

    @Query("SELECT * FROM tracks WHERE album = :album ORDER BY trackNumber ASC, title ASC")
    fun getTracksByAlbum(album: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE artist = :artist ORDER BY album ASC, trackNumber ASC, title ASC")
    fun getTracksByArtist(artist: String): Flow<List<TrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<TrackEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackEntity)

    @Update
    suspend fun updateTrack(track: TrackEntity)

    @Query("UPDATE tracks SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean)

    @Query("UPDATE tracks SET playCount = playCount + 1 WHERE id = :id")
    suspend fun incrementPlayCount(id: String)

    @Query("UPDATE tracks SET lyricsLrc = :lyrics WHERE id = :id")
    suspend fun updateLyrics(id: String, lyrics: String)

    @Query("UPDATE tracks SET albumArtUri = :artUri WHERE id = :id")
    suspend fun updateAlbumArt(id: String, artUri: String)

    @Query("UPDATE tracks SET albumArtUri = :artUri WHERE album = :album")
    suspend fun updateAlbumArtForAlbum(album: String, artUri: String)

    @Query("DELETE FROM tracks WHERE filePath LIKE 'demo://%'")
    suspend fun deleteDemoTracks()

    @Query("DELETE FROM tracks WHERE id = :id")
    suspend fun deleteTrackById(id: String)

    @Query("DELETE FROM tracks")
    suspend fun clearAll()
}
