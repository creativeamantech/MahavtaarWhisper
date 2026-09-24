package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.WhisperDatabase
import com.example.data.model.AudioTask
import com.example.data.model.TranscriptionEntity
import com.example.data.model.TranscriptionSegment
import com.example.data.model.WhisperModel
import com.example.data.remote.WhisperAiService
import com.example.data.repository.TranscriptionRepository
import com.example.util.AudioPlayerManager
import com.example.util.AudioRecorderManager
import com.example.util.LiveSpeechRecognizer
import com.example.util.RecordingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class WhisperViewModel(application: Application) : AndroidViewModel(application) {

    private val db = WhisperDatabase.getDatabase(application)
    private val aiService = WhisperAiService()
    val repository = TranscriptionRepository(db.transcriptionDao(), aiService)

    val recorderManager = AudioRecorderManager(application)
    val playerManager = AudioPlayerManager(application)
    val liveSpeechRecognizer = LiveSpeechRecognizer(application)

    // Search and Filter States
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("All")
    val showFavoritesOnly = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val transcriptionsList: StateFlow<List<TranscriptionEntity>> = combine(
        searchQuery,
        selectedCategory,
        showFavoritesOnly
    ) { query, category, favOnly ->
        Triple(query, category, favOnly)
    }.flatMapLatest { (query, category, favOnly) ->
        if (favOnly) {
            repository.favorites
        } else if (query.isNotBlank()) {
            repository.searchTranscriptions(query)
        } else if (category != "All") {
            repository.getByCategory(category)
        } else {
            repository.allTranscriptions
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Recording configuration
    val selectedLanguage = MutableStateFlow("auto")
    val selectedTask = MutableStateFlow(AudioTask.TRANSCRIBE)
    val selectedModel = MutableStateFlow(WhisperModel.WHISPER_TURBO)
    val recordingCategory = MutableStateFlow("General")
    val recordingTitle = MutableStateFlow("")
    val initialPrompt = MutableStateFlow("")

    // Processing status
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _processingMessage = MutableStateFlow("")
    val processingMessage: StateFlow<String> = _processingMessage.asStateFlow()

    private val _lastCreatedId = MutableStateFlow<Long?>(null)
    val lastCreatedId: StateFlow<Long?> = _lastCreatedId.asStateFlow()

    // Settings
    val openAiApiKey = MutableStateFlow("")
    val isLiveDictationEnabled = MutableStateFlow(true)
    val autoSummarize = MutableStateFlow(true)

    // Current detail screen item
    private val _selectedDetailId = MutableStateFlow<Long?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedTranscription: StateFlow<TranscriptionEntity?> = _selectedDetailId.flatMapLatest { id ->
        if (id != null) {
            repository.getTranscription(id)
        } else {
            MutableStateFlow(null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val playerState = playerManager.playerState

    // Active segment based on current player position
    val activeSegment: StateFlow<TranscriptionSegment?> = combine(
        selectedTranscription,
        playerState
    ) { item, state ->
        if (item == null) return@combine null
        val currentSec = state.currentPositionMs / 1000.0
        item.parseSegments().firstOrNull { seg ->
            currentSec >= seg.startSec && currentSec <= seg.endSec
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Summarize loading state
    private val _isSummarizing = MutableStateFlow(false)
    val isSummarizing: StateFlow<Boolean> = _isSummarizing.asStateFlow()

    private var currentRecordedAudioFile: File? = null

    fun selectTranscription(id: Long) {
        _selectedDetailId.value = id
        viewModelScope.launch {
            val item = repository.getTranscriptionOnce(id)
            if (item?.audioFilePath != null) {
                playerManager.loadAudio(item.audioFilePath)
            }
        }
    }

    fun startRecording() {
        currentRecordedAudioFile = recorderManager.startRecording()
        if (isLiveDictationEnabled.value && liveSpeechRecognizer.isAvailable()) {
            liveSpeechRecognizer.startListening(selectedLanguage.value)
        }
    }

    fun pauseRecording() {
        recorderManager.pauseRecording()
        liveSpeechRecognizer.stopListening()
    }

    fun resumeRecording() {
        recorderManager.resumeRecording()
        if (isLiveDictationEnabled.value && liveSpeechRecognizer.isAvailable()) {
            liveSpeechRecognizer.startListening(selectedLanguage.value)
        }
    }

    fun cancelRecording() {
        recorderManager.cancelRecording()
        liveSpeechRecognizer.reset()
        currentRecordedAudioFile = null
    }

    fun finishRecordingAndProcess(onCompleted: (Long) -> Unit) {
        val audioFile = recorderManager.stopRecording() ?: currentRecordedAudioFile
        val durationMs = recorderManager.durationMillis.value
        val liveTranscript = liveSpeechRecognizer.state.value.finalizedText + " " + liveSpeechRecognizer.state.value.partialText
        liveSpeechRecognizer.reset()

        if (audioFile == null || !audioFile.exists() || audioFile.length() == 0L) {
            _processingMessage.value = "Recording file not found."
            return
        }

        processAudioFile(
            file = audioFile,
            title = recordingTitle.value.ifBlank { "Recording ${java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}" },
            task = selectedTask.value,
            language = selectedLanguage.value,
            model = selectedModel.value,
            durationMs = durationMs,
            category = recordingCategory.value,
            fallbackText = liveTranscript.trim(),
            onCompleted = onCompleted
        )
    }

    fun importAndProcessAudio(
        uri: Uri,
        title: String,
        task: AudioTask,
        language: String,
        model: WhisperModel,
        category: String,
        onCompleted: (Long) -> Unit
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processingMessage.value = "Copying audio file to workspace..."

            val tempFile = withContext(Dispatchers.IO) {
                try {
                    val context = getApplication<Application>()
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val outDir = File(context.filesDir, "imported").apply { mkdirs() }
                    val targetFile = File(outDir, "imported_${System.currentTimeMillis()}.m4a")
                    val outputStream = FileOutputStream(targetFile)
                    inputStream?.use { input ->
                        outputStream.use { output ->
                            input.copyTo(output)
                        }
                    }
                    targetFile
                } catch (e: Exception) {
                    Log.e("WhisperViewModel", "Failed to copy audio URI", e)
                    null
                }
            }

            if (tempFile == null || !tempFile.exists()) {
                _isProcessing.value = false
                _processingMessage.value = "Failed to open imported audio."
                return@launch
            }

            processAudioFile(
                file = tempFile,
                title = title.ifBlank { "Imported Audio ${java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}" },
                task = task,
                language = language,
                model = model,
                durationMs = 0L,
                category = category,
                fallbackText = null,
                onCompleted = onCompleted
            )
        }
    }

    private fun processAudioFile(
        file: File,
        title: String,
        task: AudioTask,
        language: String,
        model: WhisperModel,
        durationMs: Long,
        category: String,
        fallbackText: String?,
        onCompleted: (Long) -> Unit
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processingMessage.value = "Running Whisper AI model transcription..."

            val result = repository.processAndSaveAudio(
                audioFile = file,
                title = title,
                task = task,
                languageCode = language,
                modelName = model.displayName,
                durationMillis = durationMs,
                category = category,
                openAiApiKey = openAiApiKey.value.takeIf { it.isNotBlank() },
                initialPrompt = initialPrompt.value.takeIf { it.isNotBlank() },
                fallbackText = fallbackText
            )

            _isProcessing.value = false
            if (result.isSuccess) {
                val newId = result.getOrThrow()
                _lastCreatedId.value = newId
                onCompleted(newId)
            } else {
                _processingMessage.value = "Error: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun toggleFavorite(item: TranscriptionEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(item.id, !item.isFavorite)
        }
    }

    fun deleteTranscription(id: Long) {
        viewModelScope.launch {
            playerManager.stop()
            repository.delete(id)
            if (_selectedDetailId.value == id) {
                _selectedDetailId.value = null
            }
        }
    }

    fun updateTitle(id: Long, title: String) {
        viewModelScope.launch {
            repository.updateTitle(id, title)
        }
    }

    fun generateSummaryForCurrent(item: TranscriptionEntity) {
        viewModelScope.launch {
            _isSummarizing.value = true
            repository.generateAiSummary(item.id, item.fullText)
            _isSummarizing.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        recorderManager.stopRecording()
        playerManager.stop()
        liveSpeechRecognizer.stopListening()
    }
}
