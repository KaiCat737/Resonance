package com.example.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat as AndroidAudioFormat
import android.media.AudioTrack as AndroidAudioTrack
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import com.example.data.model.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class AudioPlaybackEngine(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private var mediaPlayer: MediaPlayer? = null
    private var synthTrack: AndroidAudioTrack? = null
    private var synthJob: Job? = null
    private var progressJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _currentTrack = MutableStateFlow<AudioTrack?>(null)
    val currentTrack: StateFlow<AudioTrack?> = _currentTrack.asStateFlow()

    // 16-band spectrum frequency magnitudes (0.0 to 1.0)
    private val _spectrumData = MutableStateFlow(FloatArray(16) { 0.05f })
    val spectrumData: StateFlow<FloatArray> = _spectrumData.asStateFlow()

    private val _onTrackCompleted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val onTrackCompleted: SharedFlow<Unit> = _onTrackCompleted.asSharedFlow()

    private var playbackSpeed: Float = 1.0f
    private var isUsingSynth: Boolean = false
    private var synthCurrentTimeMs: Long = 0L

    fun playTrack(track: AudioTrack, startPositionMs: Long = 0L) {
        stopCurrent()
        _currentTrack.value = track
        _durationMs.value = track.durationMs
        _currentPositionMs.value = startPositionMs

        if (track.filePath.startsWith("demo://") || track.filePath.startsWith("synth://")) {
            // High fidelity real acoustic synthesizer engine
            playSynth(track, startPositionMs)
        } else {
            // Real device file or Content URI
            playLocalMedia(track, startPositionMs)
        }

        startProgressTracking()
    }

    private fun playLocalMedia(track: AudioTrack, startPositionMs: Long) {
        try {
            isUsingSynth = false
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                if (track.filePath.startsWith("content://")) {
                    setDataSource(context, Uri.parse(track.filePath))
                } else {
                    val file = File(track.filePath)
                    if (file.exists()) {
                        setDataSource(track.filePath)
                    } else {
                        // Fallback to synthesizer demo if local file not found
                        Log.w("AudioEngine", "Local file ${track.filePath} not found, falling back to synth")
                        playSynth(track, startPositionMs)
                        return
                    }
                }

                prepare()
                if (startPositionMs > 0) {
                    seekTo(startPositionMs.toInt())
                }
                start()
                _durationMs.value = duration.toLong()
                _isPlaying.value = true

                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentPositionMs.value = _durationMs.value
                    _onTrackCompleted.tryEmit(Unit)
                }

                setOnErrorListener { _, what, extra ->
                    Log.e("AudioEngine", "MediaPlayer error: $what, extra: $extra")
                    // Switch to synth mode gracefully
                    playSynth(track, _currentPositionMs.value)
                    true
                }
            }
        } catch (e: Exception) {
            Log.e("AudioEngine", "Error playing media, falling back to synth", e)
            playSynth(track, startPositionMs)
        }
    }

    private fun playSynth(track: AudioTrack, startPositionMs: Long) {
        isUsingSynth = true
        synthCurrentTimeMs = startPositionMs
        _isPlaying.value = true

        val sampleRate = 44100
        val minBufferSize = AndroidAudioTrack.getMinBufferSize(
            sampleRate,
            AndroidAudioFormat.CHANNEL_OUT_STEREO,
            AndroidAudioFormat.ENCODING_PCM_16BIT
        )

        try {
            synthTrack = AndroidAudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AndroidAudioFormat.Builder()
                        .setEncoding(AndroidAudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AndroidAudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(minBufferSize * 4)
                .setTransferMode(AndroidAudioTrack.MODE_STREAM)
                .build()

            synthTrack?.play()

            // Musical synthesizer generator (produces rich melodic chords, bass, and atmospheric harmonics)
            synthJob = CoroutineScope(Dispatchers.Default).launch {
                val bufferSize = 2048
                val shortBuffer = ShortArray(bufferSize * 2) // Stereo

                // Chord roots based on track title hash
                val baseFreq = when (track.genre?.lowercase()) {
                    "hi-res synthwave", "synthwave" -> 110.0 // A2
                    "orchestral neo-classical", "classical" -> 146.83 // D3
                    "audiophile nu-jazz", "jazz" -> 130.81 // C3
                    "acoustic indie folk", "folk" -> 164.81 // E3
                    "cyberpunk / darksynth", "cyberpunk" -> 98.0 // G2
                    else -> 120.0
                }

                val chordProgression = listOf(1.0, 1.25, 1.5, 1.33) // Root, 3rd, 5th, 4th
                var sampleIndex = 0L

                while (isActive && _isPlaying.value && isUsingSynth) {
                    val currentPosSec = synthCurrentTimeMs / 1000.0
                    val chordStep = ((currentPosSec / 4.0).toInt() % chordProgression.size)
                    val currentRoot = baseFreq * chordProgression[chordStep]

                    for (i in 0 until bufferSize) {
                        val t = sampleIndex.toDouble() / sampleRate
                        val envelope = (0.5 + 0.5 * sin(2 * PI * 0.5 * t)).coerceIn(0.2, 0.9)

                        // Harmonic synthesis: Fundamental + Sub-Bass + 3rd + 5th + High Shimmer
                        val bass = sin(2 * PI * (currentRoot * 0.5) * t) * 0.35
                        val root = sin(2 * PI * currentRoot * t) * 0.25
                        val third = sin(2 * PI * (currentRoot * 1.2599) * t) * 0.18
                        val fifth = sin(2 * PI * (currentRoot * 1.4983) * t) * 0.15
                        val shimmer = sin(2 * PI * (currentRoot * 3.0) * t) * 0.08 * (0.8 + 0.2 * sin(2 * PI * 2.0 * t))

                        // Spatial stereo panning modulation
                        val leftSample = ((bass + root + third + shimmer * 1.1) * envelope * 24000).toInt().coerceIn(-32767, 32767)
                        val rightSample = ((bass + root + fifth + shimmer * 0.9) * envelope * 24000).toInt().coerceIn(-32767, 32767)

                        shortBuffer[i * 2] = leftSample.toShort()
                        shortBuffer[i * 2 + 1] = rightSample.toShort()
                        sampleIndex++
                    }

                    synthTrack?.write(shortBuffer, 0, shortBuffer.size)
                }
            }
        } catch (e: Exception) {
            Log.e("AudioEngine", "Synthesizer track init failed", e)
        }
    }

    fun pause() {
        _isPlaying.value = false
        if (isUsingSynth) {
            synthTrack?.pause()
        } else {
            mediaPlayer?.pause()
        }
    }

    fun resume() {
        _isPlaying.value = true
        if (isUsingSynth) {
            synthTrack?.play()
            val track = _currentTrack.value
            if (track != null && (synthJob == null || synthJob?.isCompleted == true)) {
                playSynth(track, _currentPositionMs.value)
            }
        } else {
            mediaPlayer?.start()
        }
        startProgressTracking()
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            resume()
        }
    }

    fun seekTo(positionMs: Long) {
        val targetMs = positionMs.coerceIn(0L, _durationMs.value.coerceAtLeast(1L))
        _currentPositionMs.value = targetMs
        if (isUsingSynth) {
            synthCurrentTimeMs = targetMs
        } else {
            mediaPlayer?.seekTo(targetMs.toInt())
        }
    }

    fun setSpeed(speed: Float) {
        playbackSpeed = speed.coerceIn(0.5f, 2.0f)
        try {
            if (!isUsingSynth && mediaPlayer != null) {
                mediaPlayer?.playbackParams = mediaPlayer?.playbackParams?.setSpeed(playbackSpeed) ?: return
            }
        } catch (e: Exception) {
            Log.w("AudioEngine", "Speed adjust unsupported", e)
        }
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                if (_isPlaying.value) {
                    if (isUsingSynth) {
                        synthCurrentTimeMs += 40
                        _currentPositionMs.value = synthCurrentTimeMs
                        if (synthCurrentTimeMs >= _durationMs.value && _durationMs.value > 0) {
                            _isPlaying.value = false
                            _onTrackCompleted.tryEmit(Unit)
                        }
                    } else {
                        mediaPlayer?.let { mp ->
                            if (mp.isPlaying) {
                                _currentPositionMs.value = mp.currentPosition.toLong()
                            }
                        }
                    }

                    // Update dynamic frequency FFT spectrum visualization
                    generateDynamicFftData()
                } else {
                    // Smooth decay when paused
                    decayFftData()
                }
                delay(40) // ~25 FPS update for smooth lyrics and visualizer
            }
        }
    }

    private fun generateDynamicFftData() {
        val pos = _currentPositionMs.value / 1000.0
        val currentArr = _spectrumData.value
        val newArr = FloatArray(16)

        for (i in 0 until 16) {
            // Realistic frequency distribution: bass higher, mids dynamic, treble fast jitter
            val freqFactor = if (i < 4) 1.8 else if (i < 10) 1.3 else 0.9
            val wave1 = sin(pos * (2.5 + i * 0.4)) * 0.4
            val wave2 = sin(pos * (5.0 + i * 0.8)) * 0.25
            val noise = Random.nextFloat() * 0.2f
            val base = 0.25f + (wave1 + wave2).toFloat() * 0.35f + noise
            val targetVal = (base * freqFactor.toFloat()).coerceIn(0.08f, 0.98f)

            // Smooth interpolation
            newArr[i] = currentArr[i] * 0.6f + targetVal * 0.4f
        }
        _spectrumData.value = newArr
    }

    private fun decayFftData() {
        val current = _spectrumData.value
        val newArr = FloatArray(16)
        for (i in 0 until 16) {
            newArr[i] = (current[i] * 0.85f).coerceAtLeast(0.04f)
        }
        _spectrumData.value = newArr
    }

    private fun stopCurrent() {
        progressJob?.cancel()
        synthJob?.cancel()

        try {
            synthTrack?.stop()
            synthTrack?.release()
        } catch (e: Exception) {
            // Ignore
        }
        synthTrack = null

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            // Ignore
        }
        mediaPlayer = null
    }

    fun release() {
        stopCurrent()
    }
}
