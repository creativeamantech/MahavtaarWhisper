package com.example.util

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class PlayerState(
    val isPlaying: Boolean = false,
    val isPrepared: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val filePath: String? = null
)

class AudioPlayerManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    fun loadAudio(filePath: String, autoPlay: Boolean = false) {
        try {
            stop()
            val file = File(filePath)
            if (!file.exists()) {
                Log.w("AudioPlayerManager", "File does not exist: $filePath")
                return
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, Uri.fromFile(file))
                setOnPreparedListener { mp ->
                    val duration = mp.duration.toLong().coerceAtLeast(0L)
                    _playerState.value = _playerState.value.copy(
                        isPrepared = true,
                        durationMs = duration,
                        filePath = filePath
                    )
                    if (autoPlay) {
                        play()
                    }
                }
                setOnCompletionListener {
                    _playerState.value = _playerState.value.copy(
                        isPlaying = false,
                        currentPositionMs = 0L
                    )
                    progressJob?.cancel()
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Failed to load audio: $filePath", e)
        }
    }

    fun togglePlayPause() {
        if (_playerState.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun play() {
        val mp = mediaPlayer ?: return
        if (_playerState.value.isPrepared) {
            try {
                applyPlaybackSpeed(_playerState.value.playbackSpeed)
                mp.start()
                _playerState.value = _playerState.value.copy(isPlaying = true)
                startProgressPolling()
            } catch (e: Exception) {
                Log.e("AudioPlayerManager", "Play error", e)
            }
        }
    }

    fun pause() {
        val mp = mediaPlayer ?: return
        if (mp.isPlaying) {
            try {
                mp.pause()
                _playerState.value = _playerState.value.copy(isPlaying = false)
                progressJob?.cancel()
            } catch (e: Exception) {
                Log.e("AudioPlayerManager", "Pause error", e)
            }
        }
    }

    fun seekTo(positionMs: Long) {
        val mp = mediaPlayer ?: return
        if (_playerState.value.isPrepared) {
            val safePos = positionMs.coerceIn(0L, _playerState.value.durationMs).toInt()
            mp.seekTo(safePos)
            _playerState.value = _playerState.value.copy(currentPositionMs = safePos.toLong())
        }
    }

    fun skipForward(millis: Long = 5000L) {
        val target = _playerState.value.currentPositionMs + millis
        seekTo(target)
    }

    fun skipBackward(millis: Long = 5000L) {
        val target = _playerState.value.currentPositionMs - millis
        seekTo(target)
    }

    fun setSpeed(speed: Float) {
        _playerState.value = _playerState.value.copy(playbackSpeed = speed)
        applyPlaybackSpeed(speed)
    }

    private fun applyPlaybackSpeed(speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mediaPlayer?.let { mp ->
                    val params = PlaybackParams().apply { this.speed = speed }
                    mp.playbackParams = params
                }
            } catch (e: Exception) {
                Log.w("AudioPlayerManager", "Speed change error", e)
            }
        }
    }

    fun stop() {
        progressJob?.cancel()
        progressJob = null
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                reset()
                release()
            }
        } catch (e: Exception) {
            Log.w("AudioPlayerManager", "Stop error", e)
        } finally {
            mediaPlayer = null
            _playerState.value = PlayerState()
        }
    }

    private fun startProgressPolling() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _playerState.value.isPlaying) {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        _playerState.value = _playerState.value.copy(
                            currentPositionMs = mp.currentPosition.toLong()
                        )
                    }
                }
                delay(100)
            }
        }
    }
}
