package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.MainActivity
import com.example.R
import com.example.data.model.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicPlaybackService : Service() {

    inner class LocalBinder : Binder() {
        fun getService(): MusicPlaybackService = this@MusicPlaybackService
    }

    private val binder = LocalBinder()
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private var mediaSession: MediaSession? = null
    private var notificationManager: NotificationManager? = null

    private var currentTrack: AudioTrack? = null
    private var isPlaying: Boolean = false
    private var currentPositionMs: Long = 0L
    private var durationMs: Long = 0L
    private var playbackSpeed: Float = 1.0f
    private var currentArtBitmap: Bitmap? = null
    private var currentArtUri: String? = null

    companion object {
        const val CHANNEL_ID = "resonance_playback_channel"
        const val CHANNEL_NAME = "Music Playback Controls"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY = "com.example.action.PLAY"
        const val ACTION_PAUSE = "com.example.action.PAUSE"
        const val ACTION_TOGGLE_PLAY_PAUSE = "com.example.action.TOGGLE_PLAY_PAUSE"
        const val ACTION_SKIP_NEXT = "com.example.action.SKIP_NEXT"
        const val ACTION_SKIP_PREVIOUS = "com.example.action.SKIP_PREVIOUS"
        const val ACTION_STOP = "com.example.action.STOP"
        const val ACTION_SEEK_TO = "com.example.action.SEEK_TO"
        const val EXTRA_SEEK_POSITION = "extra_seek_position"

        // Event flow for ViewModel to observe control actions from lock screen / notification
        private val _mediaActionsFlow = MutableSharedFlow<MediaServiceAction>(extraBufferCapacity = 16)
        val mediaActionsFlow: SharedFlow<MediaServiceAction> = _mediaActionsFlow.asSharedFlow()

        fun sendAction(action: MediaServiceAction) {
            _mediaActionsFlow.tryEmit(action)
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        setupMediaSession()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows mini player in notification shade and lock screen playback controls"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun setupMediaSession() {
        mediaSession = MediaSession(this, "ResonanceMediaSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    sendAction(MediaServiceAction.Play)
                }

                override fun onPause() {
                    sendAction(MediaServiceAction.Pause)
                }

                override fun onSkipToNext() {
                    sendAction(MediaServiceAction.SkipNext)
                }

                override fun onSkipToPrevious() {
                    sendAction(MediaServiceAction.SkipPrevious)
                }

                override fun onSeekTo(pos: Long) {
                    sendAction(MediaServiceAction.SeekTo(pos))
                }

                override fun onStop() {
                    sendAction(MediaServiceAction.Stop)
                }
            })
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> sendAction(MediaServiceAction.Play)
            ACTION_PAUSE -> sendAction(MediaServiceAction.Pause)
            ACTION_TOGGLE_PLAY_PAUSE -> sendAction(MediaServiceAction.TogglePlayPause)
            ACTION_SKIP_NEXT -> sendAction(MediaServiceAction.SkipNext)
            ACTION_SKIP_PREVIOUS -> sendAction(MediaServiceAction.SkipPrevious)
            ACTION_STOP -> {
                sendAction(MediaServiceAction.Stop)
                stopForegroundService()
            }
            ACTION_SEEK_TO -> {
                val pos = intent.getLongExtra(EXTRA_SEEK_POSITION, 0L)
                sendAction(MediaServiceAction.SeekTo(pos))
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    fun updatePlaybackState(
        track: AudioTrack?,
        playing: Boolean,
        positionMs: Long,
        duration: Long,
        speed: Float = 1.0f
    ) {
        this.isPlaying = playing
        this.currentPositionMs = positionMs
        this.durationMs = duration
        this.playbackSpeed = speed

        val trackChanged = (this.currentTrack?.id != track?.id)
        this.currentTrack = track

        if (track == null) {
            stopForegroundService()
            return
        }

        // Update MediaSession PlaybackState
        updateMediaSessionPlaybackState()

        if (trackChanged || currentArtBitmap == null || currentArtUri != track.albumArtUri) {
            currentArtUri = track.albumArtUri
            loadArtworkAndPublish(track)
        } else {
            publishNotification(track, currentArtBitmap)
        }
    }

    private fun updateMediaSessionPlaybackState() {
        val state = if (isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
        val actions = PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_PLAY_PAUSE or
                PlaybackState.ACTION_SKIP_TO_NEXT or
                PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                PlaybackState.ACTION_SEEK_TO or
                PlaybackState.ACTION_STOP

        val playbackState = PlaybackState.Builder()
            .setActions(actions)
            .setState(state, currentPositionMs, playbackSpeed)
            .build()

        mediaSession?.setPlaybackState(playbackState)
    }

    private fun loadArtworkAndPublish(track: AudioTrack) {
        scope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                loadArtworkBitmap(track)
            }
            currentArtBitmap = bitmap
            updateMediaSessionMetadata(track, bitmap)
            publishNotification(track, bitmap)
        }
    }

    private fun updateMediaSessionMetadata(track: AudioTrack, artBitmap: Bitmap?) {
        val metaBuilder = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, track.title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, track.artist)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, track.album)
            .putString(
                MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE,
                "${track.format.displayName} • ${track.sampleRateHz / 1000}kHz/${track.bitDepth}-bit"
            )
            .putLong(MediaMetadata.METADATA_KEY_DURATION, if (durationMs > 0) durationMs else track.durationMs)

        if (artBitmap != null) {
            metaBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, artBitmap)
            metaBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, artBitmap)
        }

        mediaSession?.setMetadata(metaBuilder.build())
    }

    private suspend fun loadArtworkBitmap(track: AudioTrack): Bitmap {
        if (!track.albumArtUri.isNullOrBlank()) {
            try {
                val imageLoader = ImageLoader(this)
                val request = ImageRequest.Builder(this)
                    .data(track.albumArtUri)
                    .allowHardware(false)
                    .size(512, 512)
                    .build()
                val result = (imageLoader.execute(request) as? SuccessResult)?.drawable
                if (result is BitmapDrawable && result.bitmap != null) {
                    return result.bitmap
                }
            } catch (e: Exception) {
                Log.w("MusicService", "Error loading album art bitmap: ${e.message}")
            }
        }

        // Generate high-resolution gradient fallback bitmap matching album colors
        return generateGradientBitmap(track.albumColorHex, track.albumSecondaryColorHex)
    }

    private fun generateGradientBitmap(primaryHex: Long, secondaryHex: Long): Bitmap {
        val size = 512
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, size.toFloat(), size.toFloat(),
                primaryHex.toInt(),
                secondaryHex.toInt(),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        return bitmap
    }

    private fun publishNotification(track: AudioTrack, artBitmap: Bitmap?) {
        val session = mediaSession ?: return

        // Content intent to reopen app on NowPlaying/Library screen
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Previous Action PendingIntent
        val prevIntent = Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_SKIP_PREVIOUS }
        val prevPendingIntent = PendingIntent.getService(
            this,
            1,
            prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Play/Pause Action PendingIntent
        val playPauseIntent = Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_TOGGLE_PLAY_PAUSE }
        val playPausePendingIntent = PendingIntent.getService(
            this,
            2,
            playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Next Action PendingIntent
        val nextIntent = Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_SKIP_NEXT }
        val nextPendingIntent = PendingIntent.getService(
            this,
            3,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Stop Action PendingIntent (for notification dismiss/stop)
        val stopIntent = Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this,
            4,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIconRes = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
        val playPauseTitle = if (isPlaying) "Pause" else "Play"

        val mediaStyle = Notification.MediaStyle()
            .setMediaSession(session.sessionToken)
            .setShowActionsInCompactView(0, 1, 2) // Mini player in notification shade shows Prev, Play/Pause, Next

        val builder = Notification.Builder(this, CHANNEL_ID)
            .setStyle(mediaStyle)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle(track.title)
            .setContentText("${track.artist} • ${track.album}")
            .setSubText("${track.format.displayName} • ${track.sampleRateHz / 1000}kHz/${track.bitDepth}-bit")
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(stopPendingIntent)
            .setVisibility(Notification.VISIBILITY_PUBLIC) // Displays full player on Lock Screen
            .setOngoing(isPlaying)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, R.drawable.ic_skip_previous),
                    "Previous",
                    prevPendingIntent
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, playPauseIconRes),
                    playPauseTitle,
                    playPausePendingIntent
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, R.drawable.ic_skip_next),
                    "Next",
                    nextPendingIntent
                ).build()
            )

        if (artBitmap != null) {
            builder.setLargeIcon(artBitmap)
        }

        val notification = builder.build()

        try {
            if (isPlaying) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } else {
                // When paused, allow notification to remain dismissible while keeping lockscreen controls active
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
                notificationManager?.notify(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e("MusicService", "Error posting notification: ${e.message}", e)
        }
    }

    private fun stopForegroundService() {
        isPlaying = false
        updateMediaSessionPlaybackState()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        notificationManager?.cancel(NOTIFICATION_ID)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession?.release()
        mediaSession = null
        currentArtBitmap = null
    }
}

sealed class MediaServiceAction {
    object Play : MediaServiceAction()
    object Pause : MediaServiceAction()
    object TogglePlayPause : MediaServiceAction()
    object SkipNext : MediaServiceAction()
    object SkipPrevious : MediaServiceAction()
    object Stop : MediaServiceAction()
    data class SeekTo(val positionMs: Long) : MediaServiceAction()
}
