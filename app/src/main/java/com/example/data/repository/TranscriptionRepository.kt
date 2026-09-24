package com.example.data.repository

import android.content.Context
import com.example.data.local.TranscriptionDao
import com.example.data.model.AudioTask
import com.example.data.model.TranscriptionEntity
import com.example.data.model.TranscriptionResult
import com.example.data.remote.WhisperAiService
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class TranscriptionRepository(
    private val dao: TranscriptionDao,
    private val aiService: WhisperAiService
) {
    val allTranscriptions: Flow<List<TranscriptionEntity>> = dao.getAllTranscriptions()
    val favorites: Flow<List<TranscriptionEntity>> = dao.getFavorites()

    fun getTranscription(id: Long): Flow<TranscriptionEntity?> = dao.getTranscriptionById(id)

    suspend fun getTranscriptionOnce(id: Long): TranscriptionEntity? = dao.getTranscriptionByIdOnce(id)

    fun searchTranscriptions(query: String): Flow<List<TranscriptionEntity>> = dao.searchTranscriptions(query)

    fun getByCategory(category: String): Flow<List<TranscriptionEntity>> = dao.getByCategory(category)

    suspend fun insert(transcription: TranscriptionEntity): Long = dao.insert(transcription)

    suspend fun update(transcription: TranscriptionEntity) = dao.update(transcription)

    suspend fun delete(id: Long) = dao.deleteById(id)

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) = dao.setFavorite(id, isFavorite)

    suspend fun updateTitle(id: Long, title: String) = dao.updateTitle(id, title)

    suspend fun updateContent(id: Long, text: String, segments: List<com.example.data.model.TranscriptionSegment>) {
        val array = JSONArray()
        segments.forEach { s ->
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("startSec", s.startSec)
            obj.put("endSec", s.endSec)
            obj.put("text", s.text)
            obj.put("confidence", s.confidence)
            array.put(obj)
        }
        dao.updateContent(id, text, array.toString())
    }

    suspend fun updateSummary(id: Long, summary: String, actionItems: List<String>) {
        val array = JSONArray()
        actionItems.forEach { array.put(it) }
        dao.updateSummary(id, summary, array.toString())
    }

    suspend fun processAndSaveAudio(
        audioFile: File,
        title: String,
        task: AudioTask,
        languageCode: String,
        modelName: String,
        durationMillis: Long,
        category: String,
        openAiApiKey: String? = null,
        initialPrompt: String? = null,
        fallbackText: String? = null
    ): Result<Long> {
        val aiResult = aiService.processAudio(
            audioFile = audioFile,
            task = task,
            languageCode = languageCode,
            openAiApiKey = openAiApiKey,
            initialPrompt = initialPrompt
        )

        val transcriptionResult = if (aiResult.isSuccess) {
            aiResult.getOrThrow()
        } else if (!fallbackText.isNullOrBlank()) {
            // Use on-device speech recognizer text fallback
            val durationSec = (durationMillis / 1000.0).coerceAtLeast(1.0)
            val segments = createFallbackSegments(fallbackText, durationSec)
            TranscriptionResult(
                fullText = fallbackText,
                detectedLanguage = if (languageCode == "auto") "en" else languageCode,
                segments = segments,
                translatedText = if (task == AudioTask.TRANSLATE_EN && languageCode != "en") fallbackText else null,
                summary = "Live voice transcription captured on-device.",
                actionItems = emptyList()
            )
        } else {
            return Result.failure(aiResult.exceptionOrNull() ?: Exception("Transcription failed"))
        }

        val segmentsJsonArray = JSONArray()
        transcriptionResult.segments.forEach { seg ->
            val obj = JSONObject()
            obj.put("id", seg.id)
            obj.put("startSec", seg.startSec)
            obj.put("endSec", seg.endSec)
            obj.put("text", seg.text)
            obj.put("confidence", seg.confidence)
            segmentsJsonArray.put(obj)
        }

        val actionsJsonArray = JSONArray()
        transcriptionResult.actionItems.forEach { actionsJsonArray.put(it) }

        val wordCount = transcriptionResult.fullText.split("\\s+".toRegex()).count { it.isNotBlank() }

        val entity = TranscriptionEntity(
            title = title.ifBlank { "Voice Note ${java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}" },
            createdAt = System.currentTimeMillis(),
            durationMillis = durationMillis,
            audioFilePath = audioFile.absolutePath,
            fullText = transcriptionResult.fullText,
            translatedText = transcriptionResult.translatedText,
            detectedLanguage = transcriptionResult.detectedLanguage,
            targetLanguage = if (task == AudioTask.TRANSLATE_EN) "en" else null,
            modelUsed = modelName,
            taskType = if (task == AudioTask.TRANSLATE_EN) "translate" else "transcribe",
            segmentsJson = segmentsJsonArray.toString(),
            summary = transcriptionResult.summary,
            actionItemsJson = actionsJsonArray.toString(),
            category = category,
            audioFileSize = audioFile.length(),
            wordCount = wordCount
        )

        val newId = dao.insert(entity)
        return Result.success(newId)
    }

    suspend fun generateAiSummary(id: Long, transcript: String): Result<Pair<String, List<String>>> {
        val result = aiService.generateSummaryAndActions(transcript)
        if (result.isSuccess) {
            val (summary, actions) = result.getOrThrow()
            updateSummary(id, summary, actions)
        }
        return result
    }

    private fun createFallbackSegments(text: String, totalDurationSec: Double): List<com.example.data.model.TranscriptionSegment> {
        val sentences = text.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }
        if (sentences.isEmpty()) {
            return listOf(
                com.example.data.model.TranscriptionSegment(1, 0.0, totalDurationSec, text, 0.95f)
            )
        }

        val step = totalDurationSec / sentences.size
        return sentences.mapIndexed { index, s ->
            val start = index * step
            val end = (index + 1) * step
            com.example.data.model.TranscriptionSegment(
                id = index + 1,
                startSec = start,
                endSec = end,
                text = s.trim(),
                confidence = 0.95f
            )
        }
    }
}
