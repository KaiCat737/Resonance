package com.example.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppThemeMode
import com.example.data.model.AudioFormat
import com.example.data.model.AudioTrack
import com.example.data.model.EqualizerState
import com.example.data.model.LyricLine
import com.example.data.model.PlaybackMode
import com.example.data.model.Playlist
import com.example.data.model.VisualizerStyle
import com.example.data.repository.MusicRepository
import com.example.player.AudioPlaybackEngine
import com.example.service.MediaServiceAction
import com.example.service.MusicPlaybackService
import com.example.util.LyricsParser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOption(val displayName: String) {
    TITLE("Title (A-Z)"),
    ARTIST("Artist (A-Z)"),
    SAMPLE_RATE("Hi-Res Sample Rate"),
    BITRATE("Bitrate (Kbps)"),
    DURATION("Duration"),
    DATE_ADDED("Recently Added")
}

enum class NowPlayingDisplayMode {
    VINYL,
    COVER_ART,
    LYRICS,
    QUEUE
}

class MusicPlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MusicRepository(application)
    private val engine = AudioPlaybackEngine(application)

    private var playbackService: MusicPlaybackService? = null
    private var isServiceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (binder is MusicPlaybackService.LocalBinder) {
                playbackService = binder.getService()
                isServiceBound = true
                syncServicePlaybackState()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playbackService = null
            isServiceBound = false
        }
    }

    val allTracks: StateFlow<List<AudioTrack>> = repository.allTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlaylists: StateFlow<List<Playlist>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Playback state
    val isPlaying: StateFlow<Boolean> = engine.isPlaying
    val currentPositionMs: StateFlow<Long> = engine.currentPositionMs
    val durationMs: StateFlow<Long> = engine.durationMs
    val currentTrack: StateFlow<AudioTrack?> = engine.currentTrack
    val spectrumData: StateFlow<FloatArray> = engine.spectrumData

    // Queue & Modes
    private val _queue = MutableStateFlow<List<AudioTrack>>(emptyList())
    val queue: StateFlow<List<AudioTrack>> = _queue.asStateFlow()

    private val _queueIndex = MutableStateFlow(0)
    val queueIndex: StateFlow<Int> = _queueIndex.asStateFlow()

    private val _playbackMode = MutableStateFlow(PlaybackMode.REPEAT_ALL)
    val playbackMode: StateFlow<PlaybackMode> = _playbackMode.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _nowPlayingMode = MutableStateFlow(NowPlayingDisplayMode.VINYL)
    val nowPlayingMode: StateFlow<NowPlayingDisplayMode> = _nowPlayingMode.asStateFlow()

    private val _visualizerStyle = MutableStateFlow(VisualizerStyle.BARS)
    val visualizerStyle: StateFlow<VisualizerStyle> = _visualizerStyle.asStateFlow()

    private val _appThemeMode = MutableStateFlow(AppThemeMode.FROSTED_GLASS)
    val appThemeMode: StateFlow<AppThemeMode> = _appThemeMode.asStateFlow()

    // Lyrics State
    private val _parsedLyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val parsedLyrics: StateFlow<List<LyricLine>> = _parsedLyrics.asStateFlow()

    private val _activeLyricIndex = MutableStateFlow(-1)
    val activeLyricIndex: StateFlow<Int> = _activeLyricIndex.asStateFlow()

    private val _lyricOffsetMs = MutableStateFlow(0L)
    val lyricOffsetMs: StateFlow<Long> = _lyricOffsetMs.asStateFlow()

    // Equalizer State
    private val _equalizerState = MutableStateFlow(EqualizerState())
    val equalizerState: StateFlow<EqualizerState> = _equalizerState.asStateFlow()

    // Library Filtering & Search
    val searchQuery = MutableStateFlow("")
    val selectedFormatFilter = MutableStateFlow<AudioFormat?>(null)
    val onlyLosslessFilter = MutableStateFlow(false)
    val sortOption = MutableStateFlow(SortOption.TITLE)

    val filteredTracks: StateFlow<List<AudioTrack>> = combine(
        allTracks,
        searchQuery,
        selectedFormatFilter,
        onlyLosslessFilter,
        sortOption
    ) { tracks, query, formatFilter, losslessOnly, sort ->
        var list = tracks

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.title.lowercase().contains(q) ||
                it.artist.lowercase().contains(q) ||
                it.album.lowercase().contains(q) ||
                it.genre?.lowercase()?.contains(q) == true ||
                it.lyricsLrc?.lowercase()?.contains(q) == true
            }
        }

        if (formatFilter != null) {
            list = list.filter { it.format == formatFilter }
        }

        if (losslessOnly) {
            list = list.filter { it.isLossless }
        }

        when (sort) {
            SortOption.TITLE -> list.sortedBy { it.title.lowercase() }
            SortOption.ARTIST -> list.sortedBy { it.artist.lowercase() }
            SortOption.SAMPLE_RATE -> list.sortedByDescending { it.sampleRateHz }
            SortOption.BITRATE -> list.sortedByDescending { it.bitrateKbps }
            SortOption.DURATION -> list.sortedByDescending { it.durationMs }
            SortOption.DATE_ADDED -> list.sortedByDescending { it.dateAddedMs }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Sleep Timer
    private val _sleepTimerMinutes = MutableStateFlow<Int?>(null)
    val sleepTimerMinutes: StateFlow<Int?> = _sleepTimerMinutes.asStateFlow()
    private var sleepTimerJob: Job? = null

    // UI Sheets / Dialogs
    val showEqualizerSheet = MutableStateFlow(false)
    val showTagEditorTrack = MutableStateFlow<AudioTrack?>(null)
    val showMetadataInspectorTrack = MutableStateFlow<AudioTrack?>(null)
    val showSyncDialog = MutableStateFlow(false)
    val showCreatePlaylistDialog = MutableStateFlow(false)
    val showAddToPlaylistTrack = MutableStateFlow<AudioTrack?>(null)
    val showFullscreenPlayer = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            repository.initializeIfNeeded()
        }

        // Listen for track completions to auto-advance
        viewModelScope.launch {
            engine.onTrackCompleted.collect {
                handleTrackCompletion()
            }
        }

        // Parse lyrics whenever current track changes
        viewModelScope.launch {
            currentTrack.collect { track ->
                _lyricOffsetMs.value = 0L
                if (track != null) {
                    _parsedLyrics.value = LyricsParser.parse(track.lyricsLrc)
                    repository.incrementPlayCount(track.id)
                } else {
                    _parsedLyrics.value = emptyList()
                }
            }
        }

        // Bind and start playback service for lock screen and notification mini player
        try {
            val serviceIntent = Intent(application, MusicPlaybackService::class.java)
            application.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Listen for lock screen / notification actions
        viewModelScope.launch {
            MusicPlaybackService.mediaActionsFlow.collect { action ->
                when (action) {
                    is MediaServiceAction.Play -> resumePlayback()
                    is MediaServiceAction.Pause -> engine.pause()
                    is MediaServiceAction.TogglePlayPause -> togglePlayPause()
                    is MediaServiceAction.SkipNext -> skipNext()
                    is MediaServiceAction.SkipPrevious -> skipPrevious()
                    is MediaServiceAction.SeekTo -> seekTo(action.positionMs)
                    is MediaServiceAction.Stop -> engine.pause()
                }
            }
        }

        // Synchronize playback state to MusicPlaybackService
        viewModelScope.launch {
            combine(
                currentTrack,
                isPlaying,
                durationMs,
                playbackSpeed
            ) { track, playing, duration, speed ->
                Triple(track, playing, duration) to speed
            }.collect { (tuple, speed) ->
                val (track, playing, duration) = tuple
                if (playing) {
                    startPlaybackService()
                }
                playbackService?.updatePlaybackState(
                    track = track,
                    playing = playing,
                    positionMs = currentPositionMs.value,
                    duration = duration,
                    speed = speed
                )
            }
        }

        // Update active lyric index in sync with playback position
        viewModelScope.launch {
            currentPositionMs.collect { pos ->
                val lyrics = _parsedLyrics.value
                if (lyrics.isNotEmpty()) {
                    _activeLyricIndex.value = LyricsParser.findActiveIndex(lyrics, pos, _lyricOffsetMs.value)
                }
            }
        }
    }

    private fun startPlaybackService() {
        try {
            val intent = Intent(getApplication(), MusicPlaybackService::class.java)
            val context = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun syncServicePlaybackState() {
        playbackService?.updatePlaybackState(
            track = currentTrack.value,
            playing = isPlaying.value,
            positionMs = currentPositionMs.value,
            duration = durationMs.value,
            speed = playbackSpeed.value
        )
    }

    private fun resumePlayback() {
        if (currentTrack.value == null && allTracks.value.isNotEmpty()) {
            playTrack(allTracks.value.first(), allTracks.value)
        } else {
            engine.resume()
        }
    }

    fun playTrack(track: AudioTrack, sourceList: List<AudioTrack> = emptyList()) {
        val newQueue = if (sourceList.isNotEmpty()) sourceList else if (_queue.value.isEmpty()) allTracks.value else _queue.value
        _queue.value = newQueue
        val idx = newQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        _queueIndex.value = idx
        engine.playTrack(track)
    }

    fun togglePlayPause() {
        if (currentTrack.value == null && allTracks.value.isNotEmpty()) {
            playTrack(allTracks.value.first(), allTracks.value)
        } else {
            engine.togglePlayPause()
        }
    }

    fun seekTo(positionMs: Long) {
        engine.seekTo(positionMs)
    }

    fun skipNext() {
        val currentQ = _queue.value
        if (currentQ.isEmpty()) return

        val nextIndex = when (_playbackMode.value) {
            PlaybackMode.SHUFFLE -> (currentQ.indices - _queueIndex.value).randomOrNull() ?: 0
            else -> (_queueIndex.value + 1) % currentQ.size
        }
        _queueIndex.value = nextIndex
        val nextTrack = currentQ[nextIndex]
        engine.playTrack(nextTrack)
    }

    fun skipPrevious() {
        val currentQ = _queue.value
        if (currentQ.isEmpty()) return

        if (currentPositionMs.value > 3000L) {
            seekTo(0L)
            return
        }

        val prevIndex = if (_queueIndex.value - 1 < 0) currentQ.lastIndex else _queueIndex.value - 1
        _queueIndex.value = prevIndex
        val prevTrack = currentQ[prevIndex]
        engine.playTrack(prevTrack)
    }

    private fun handleTrackCompletion() {
        val currentQ = _queue.value
        if (currentQ.isEmpty()) return

        when (_playbackMode.value) {
            PlaybackMode.REPEAT_ONE -> {
                currentTrack.value?.let { engine.playTrack(it, 0L) }
            }
            PlaybackMode.REPEAT_ALL -> {
                skipNext()
            }
            PlaybackMode.SHUFFLE -> {
                skipNext()
            }
            PlaybackMode.OFF -> {
                if (_queueIndex.value < currentQ.lastIndex) {
                    skipNext()
                } else {
                    engine.pause()
                }
            }
        }
    }

    fun togglePlaybackMode() {
        _playbackMode.value = when (_playbackMode.value) {
            PlaybackMode.REPEAT_ALL -> PlaybackMode.REPEAT_ONE
            PlaybackMode.REPEAT_ONE -> PlaybackMode.SHUFFLE
            PlaybackMode.SHUFFLE -> PlaybackMode.OFF
            PlaybackMode.OFF -> PlaybackMode.REPEAT_ALL
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        engine.setSpeed(speed)
    }

    fun setNowPlayingMode(mode: NowPlayingDisplayMode) {
        _nowPlayingMode.value = mode
    }

    fun setVisualizerStyle(style: VisualizerStyle) {
        _visualizerStyle.value = style
    }

    fun setAppThemeMode(theme: AppThemeMode) {
        _appThemeMode.value = theme
    }

    fun adjustLyricOffset(deltaMs: Long) {
        _lyricOffsetMs.value += deltaMs
    }

    fun resetLyricOffset() {
        _lyricOffsetMs.value = 0L
    }

    fun toggleFavorite(track: AudioTrack) {
        viewModelScope.launch {
            repository.toggleFavorite(track.id, !track.isFavorite)
        }
    }

    fun setSleepTimer(minutes: Int?) {
        _sleepTimerMinutes.value = minutes
        sleepTimerJob?.cancel()
        if (minutes != null && minutes > 0) {
            sleepTimerJob = viewModelScope.launch {
                delay(minutes * 60 * 1000L)
                engine.pause()
                _sleepTimerMinutes.value = null
            }
        }
    }

    // Drag-and-drop Queue reordering
    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        val list = _queue.value.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            _queue.value = list
            // Update queueIndex if the active track moved
            currentTrack.value?.let { active ->
                _queueIndex.value = list.indexOfFirst { it.id == active.id }.coerceAtLeast(0)
            }
        }
    }

    // Drag-and-drop Playlist reordering
    fun reorderPlaylist(playlistId: String, fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            repository.reorderPlaylistTracks(playlistId, fromIndex, toIndex)
        }
    }

    fun createPlaylist(name: String, description: String = "", accentColor: Long = 0xFF00E5FF) {
        viewModelScope.launch {
            repository.createPlaylist(name, description, accentColor)
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    fun addTrackToPlaylist(playlistId: String, trackId: String) {
        viewModelScope.launch {
            repository.addTracksToPlaylist(playlistId, listOf(trackId))
        }
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(playlistId, trackId)
        }
    }

    fun updateTrackTags(
        trackId: String,
        title: String,
        artist: String,
        album: String,
        genre: String?,
        year: Int?,
        lyrics: String?
    ) {
        viewModelScope.launch {
            repository.updateTrackTags(trackId, title, artist, album, genre, year, lyrics)
            // If current playing track was updated, re-parse lyrics
            if (currentTrack.value?.id == trackId) {
                _parsedLyrics.value = LyricsParser.parse(lyrics)
            }
        }
    }

    fun importAudioFile(uri: Uri) {
        viewModelScope.launch {
            val imported = repository.importAudioUri(uri)
            if (imported != null) {
                // Auto play or add to queue
                playTrack(imported, allTracks.value + imported)
            }
        }
    }

    // Equalizer controls
    fun updateEqualizerBand(bandIndex: Int, gainDb: Float) {
        val currentGains = _equalizerState.value.bandGains.toMutableList()
        if (bandIndex in currentGains.indices) {
            currentGains[bandIndex] = gainDb.coerceIn(-10f, 10f)
            _equalizerState.value = _equalizerState.value.copy(
                bandGains = currentGains,
                presetName = "Custom"
            )
        }
    }

    fun setEqualizerPreset(presetName: String) {
        val gains = when (presetName) {
            "Audiophile Master" -> listOf(2.5f, 2.0f, 1.0f, 0.5f, 0.0f, 0.5f, 1.5f, 2.5f, 3.0f, 3.5f)
            "Bass Punch" -> listOf(6.0f, 5.0f, 3.5f, 1.5f, 0.0f, 0.0f, 0.0f, 1.0f, 2.0f, 2.5f)
            "Vocal Clarity" -> listOf(-1.0f, -0.5f, 1.0f, 2.5f, 4.0f, 3.5f, 2.0f, 1.0f, 0.5f, 0.0f)
            "Hi-Res Treble" -> listOf(0.0f, 0.0f, 0.0f, 0.5f, 1.0f, 2.0f, 3.5f, 5.0f, 6.0f, 7.0f)
            "Electronic" -> listOf(5.0f, 4.0f, 2.0f, 0.0f, -1.0f, 1.0f, 2.5f, 4.0f, 4.5f, 5.0f)
            "Acoustic" -> listOf(3.0f, 2.5f, 1.5f, 1.0f, 1.5f, 2.0f, 2.5f, 3.0f, 3.5f, 2.5f)
            "Flat" -> listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
            else -> listOf(2.5f, 2.0f, 1.0f, 0.5f, 0.0f, 0.5f, 1.5f, 2.5f, 3.0f, 3.5f)
        }
        _equalizerState.value = _equalizerState.value.copy(
            presetName = presetName,
            bandGains = gains
        )
    }

    fun setBassBoost(level: Float) {
        _equalizerState.value = _equalizerState.value.copy(bassBoost = level.coerceIn(0f, 1f))
    }

    fun setVirtualizer(level: Float) {
        _equalizerState.value = _equalizerState.value.copy(virtualizer3D = level.coerceIn(0f, 1f))
    }

    fun setPreampGain(gainDb: Float) {
        _equalizerState.value = _equalizerState.value.copy(preampGainDb = gainDb.coerceIn(-6f, 6f))
    }

    fun toggleEqualizerEnabled() {
        _equalizerState.value = _equalizerState.value.copy(enabled = !_equalizerState.value.enabled)
    }

    // Cross-Platform Synchronization
    suspend fun exportLibraryJson(): String {
        return repository.exportLibrarySyncBundle()
    }

    suspend fun importLibraryJson(json: String): Boolean {
        return repository.importLibrarySyncBundle(json)
    }

    suspend fun exportPlaylistM3u8(playlist: Playlist): String {
        return repository.exportPlaylistM3u8(playlist)
    }

    override fun onCleared() {
        super.onCleared()
        if (isServiceBound) {
            try {
                getApplication<Application>().unbindService(serviceConnection)
            } catch (e: Exception) {
                // Ignore unbind exceptions
            }
            isServiceBound = false
        }
        engine.release()
    }
}
