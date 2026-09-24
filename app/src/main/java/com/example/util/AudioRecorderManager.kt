package com.example.util

import android.content.Context
import android.media.MediaRecorder
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class RecordingState {
    IDLE,
    RECORDING,
    PAUSED,
    STOPPED
}

class AudioRecorderManager(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var recordingStartTime: Long = 0L
    private var pausedDuration: Long = 0L
    private var pauseStartTime: Long = 0L

    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _amplitude = MutableStateFlow(0f) // 0.0 to 1.0
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _durationMillis = MutableStateFlow(0L)
    val durationMillis: StateFlow<Long> = _durationMillis.asStateFlow()

    private val _recentAmplitudes = MutableStateFlow<List<Float>>(emptyList())
    val recentAmplitudes: StateFlow<List<Float>> = _recentAmplitudes.asStateFlow()

    private var pollingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun startRecording(): File? {
        try {
            stopRecordingInternal()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val audioDir = File(context.filesDir, "recordings").apply { mkdirs() }
            val outputFile = File(audioDir, "whisper_rec_$timestamp.m4a")
            currentOutputFile = outputFile

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            recordingStartTime = System.currentTimeMillis()
            pausedDuration = 0L
            _durationMillis.value = 0L
            _recentAmplitudes.value = emptyList()
            _recordingState.value = RecordingState.RECORDING

            startPolling()
            return outputFile
        } catch (e: Exception) {
            Log.e("AudioRecorderManager", "Failed to start recording", e)
            stopRecordingInternal()
            _recordingState.value = RecordingState.IDLE
            return null
        }
    }

    fun pauseRecording() {
        if (_recordingState.value == RecordingState.RECORDING && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                mediaRecorder?.pause()
                pauseStartTime = System.currentTimeMillis()
                _recordingState.value = RecordingState.PAUSED
            } catch (e: Exception) {
                Log.e("AudioRecorderManager", "Error pausing recorder", e)
            }
        }
    }

    fun resumeRecording() {
        if (_recordingState.value == RecordingState.PAUSED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                mediaRecorder?.resume()
                if (pauseStartTime > 0) {
                    pausedDuration += (System.currentTimeMillis() - pauseStartTime)
                    pauseStartTime = 0L
                }
                _recordingState.value = RecordingState.RECORDING
            } catch (e: Exception) {
                Log.e("AudioRecorderManager", "Error resuming recorder", e)
            }
        }
    }

    fun stopRecording(): File? {
        val file = currentOutputFile
        stopRecordingInternal()
        _recordingState.value = RecordingState.IDLE
        return file
    }

    fun cancelRecording() {
        val file = currentOutputFile
        stopRecordingInternal()
        file?.delete()
        currentOutputFile = null
        _recordingState.value = RecordingState.IDLE
        _durationMillis.value = 0L
        _recentAmplitudes.value = emptyList()
    }

    private fun stopRecordingInternal() {
        pollingJob?.cancel()
        pollingJob = null
        try {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
        } catch (e: Exception) {
            Log.w("AudioRecorderManager", "Recorder stop exception: ${e.message}")
        } finally {
            mediaRecorder = null
        }
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            val maxHistory = 40
            while (isActive && _recordingState.value != RecordingState.IDLE) {
                if (_recordingState.value == RecordingState.RECORDING) {
                    val currentElapsed = (System.currentTimeMillis() - recordingStartTime) - pausedDuration
                    _durationMillis.value = currentElapsed.coerceAtLeast(0L)

                    val maxAmp = try {
                        mediaRecorder?.maxAmplitude ?: 0
                    } catch (_: Exception) { 0 }

                    // Normalize amplitude from 0..32767 to 0..1
                    val normalizedAmp = (maxAmp / 32767f).coerceIn(0.05f, 1.0f)
                    _amplitude.value = normalizedAmp

                    val currentList = _recentAmplitudes.value.toMutableList()
                    currentList.add(normalizedAmp)
                    if (currentList.size > maxHistory) {
                        currentList.removeAt(0)
                    }
                    _recentAmplitudes.value = currentList
                }
                delay(60) // Update UI at ~16fps
            }
        }
    }
}
