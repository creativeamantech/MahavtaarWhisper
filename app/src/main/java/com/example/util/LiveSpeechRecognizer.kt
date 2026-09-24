package com.example.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class LiveSpeechState(
    val isListening: Boolean = false,
    val partialText: String = "",
    val finalizedText: String = "",
    val rmsDb: Float = 0f,
    val errorMessage: String? = null
)

class LiveSpeechRecognizer(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private val _state = MutableStateFlow(LiveSpeechState())
    val state: StateFlow<LiveSpeechState> = _state.asStateFlow()

    private var currentAccumulatedText = StringBuilder()
    private var isIntentionalStop = false

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening(languageCode: String = "en-US") {
        if (!isAvailable()) {
            _state.value = _state.value.copy(errorMessage = "Speech recognition is not available on this device")
            return
        }

        stopListening()
        isIntentionalStop = false

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _state.value = _state.value.copy(isListening = true, errorMessage = null)
                    }

                    override fun onBeginningOfSpeech() {}

                    override fun onRmsChanged(rmsdB: Float) {
                        _state.value = _state.value.copy(rmsDb = rmsdB.coerceIn(0f, 10f))
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {}

                    override fun onError(error: Int) {
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                            SpeechRecognizer.ERROR_NETWORK -> "Network connection error"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                            SpeechRecognizer.ERROR_SERVER -> "Server error"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                            else -> "Recognition error $error"
                        }
                        Log.w("LiveSpeechRecognizer", "Error: $message")
                        if (!isIntentionalStop) {
                            // If it's a transient timeout, we keep listening if still active
                            _state.value = _state.value.copy(isListening = false, errorMessage = if (error == SpeechRecognizer.ERROR_NO_MATCH) null else message)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            if (currentAccumulatedText.isNotEmpty()) {
                                currentAccumulatedText.append(" ")
                            }
                            currentAccumulatedText.append(text)
                        }
                        _state.value = _state.value.copy(
                            finalizedText = currentAccumulatedText.toString(),
                            partialText = "",
                            isListening = false
                        )
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull() ?: ""
                        _state.value = _state.value.copy(partialText = partial)
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                val targetLang = if (languageCode == "auto" || languageCode.isBlank()) Locale.getDefault().toLanguageTag() else languageCode
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, targetLang)
            }

            speechRecognizer?.startListening(intent)
            _state.value = _state.value.copy(isListening = true, errorMessage = null)
        } catch (e: Exception) {
            Log.e("LiveSpeechRecognizer", "Failed to start speech recognition", e)
            _state.value = _state.value.copy(isListening = false, errorMessage = e.message)
        }
    }

    fun stopListening() {
        isIntentionalStop = true
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w("LiveSpeechRecognizer", "Error stopping: ${e.message}")
        } finally {
            speechRecognizer = null
            _state.value = _state.value.copy(isListening = false, partialText = "")
        }
    }

    fun reset() {
        stopListening()
        currentAccumulatedText.clear()
        _state.value = LiveSpeechState()
    }
}
