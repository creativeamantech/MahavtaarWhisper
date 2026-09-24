package com.example.data.remote

import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.AudioTask
import com.example.data.model.TranscriptionResult
import com.example.data.model.TranscriptionSegment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class WhisperAiService {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "WhisperAiService"
        private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
        private const val OPENAI_WHISPER_URL = "https://api.openai.com/v1/audio/transcriptions"
    }

    /**
     * Transcribe or Translate audio using Gemini Multimodal Audio or OpenAI Whisper API
     */
    suspend fun processAudio(
        audioFile: File,
        task: AudioTask,
        languageCode: String = "auto",
        openAiApiKey: String? = null,
        initialPrompt: String? = null
    ): Result<TranscriptionResult> = withContext(Dispatchers.IO) {
        try {
            // Check if user configured an OpenAI API Key
            if (!openAiApiKey.isNullOrBlank()) {
                val openAiResult = callOpenAiWhisper(audioFile, task, languageCode, openAiApiKey, initialPrompt)
                if (openAiResult.isSuccess) {
                    return@withContext openAiResult
                } else {
                    Log.w(TAG, "OpenAI Whisper failed: ${openAiResult.exceptionOrNull()?.message}, trying Gemini...")
                }
            }

            // Primary: Use Gemini Audio API with BuildConfig.GEMINI_API_KEY
            val geminiApiKey = BuildConfig.GEMINI_API_KEY
            if (geminiApiKey.isNotBlank() && !geminiApiKey.equals("MY_GEMINI_API_KEY", ignoreCase = true)) {
                val geminiResult = callGeminiAudioApi(audioFile, task, languageCode, geminiApiKey, initialPrompt)
                if (geminiResult.isSuccess) {
                    return@withContext geminiResult
                } else {
                    Log.w(TAG, "Gemini Audio failed: ${geminiResult.exceptionOrNull()?.message}")
                }
            }

            // If audio file has text or fallback needed
            Result.failure(Exception("Please configure your Gemini API Key in the AI Studio Secrets panel or OpenAI API Key in Settings to enable cloud Whisper transcription."))
        } catch (e: Exception) {
            Log.e(TAG, "Transcription error", e)
            Result.failure(e)
        }
    }

    /**
     * Transcribe speech audio file via Gemini Audio Multimodal API
     */
    private suspend fun callGeminiAudioApi(
        audioFile: File,
        task: AudioTask,
        languageCode: String,
        apiKey: String,
        initialPrompt: String?
    ): Result<TranscriptionResult> = withContext(Dispatchers.IO) {
        try {
            val audioBytes = audioFile.readBytes()
            val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
            val mimeType = when (audioFile.extension.lowercase()) {
                "mp3" -> "audio/mp3"
                "wav" -> "audio/wav"
                "aac" -> "audio/aac"
                "ogg" -> "audio/ogg"
                "flac" -> "audio/flac"
                else -> "audio/mp4" // m4a default
            }

            val isTranslate = task == AudioTask.TRANSLATE_EN
            val langInstruction = if (languageCode != "auto") "The spoken audio is in language code '$languageCode'." else "Detect the spoken language automatically."
            val taskInstruction = if (isTranslate) {
                "Translate the spoken speech into English accurately, segment by segment."
            } else {
                "Transcribe the verbatim speech accurately in its original language, segment by segment."
            }

            val promptText = """
                You are OpenAI Whisper, a state-of-the-art automatic speech recognition (ASR) system.
                $taskInstruction
                $langInstruction
                ${if (!initialPrompt.isNullOrBlank()) "Context/Vocabulary hint: $initialPrompt" else ""}

                Return a strictly formatted JSON object (no markdown quotes, no extra text) with this exact schema:
                {
                  "detectedLanguage": "ISO 2-letter language code (e.g. en, es, fr, ja, de, zh, hi)",
                  "fullText": "Full complete transcript of the entire audio",
                  "translatedText": ${if (isTranslate) "\"English translation of full text\"" else "null"},
                  "segments": [
                    {
                      "id": 1,
                      "startSec": 0.0,
                      "endSec": 3.5,
                      "text": "First phrase or sentence spoken",
                      "confidence": 0.98
                    }
                  ],
                  "summary": "2-3 sentence executive summary of what was discussed",
                  "actionItems": ["Action item 1 if applicable", "Action item 2"]
                }
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            // Text instruction part
                            put(JSONObject().apply { put("text", promptText) })
                            // Inline audio part
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", mimeType)
                                    put("data", base64Audio)
                                })
                            })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)

                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.2)
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)

            val url = "$GEMINI_BASE_URL?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBodyString = response.body?.string()

            if (!response.isSuccessful || responseBodyString == null) {
                return@withContext Result.failure(Exception("Gemini API error code ${response.code}: $responseBodyString"))
            }

            val jsonRoot = JSONObject(responseBodyString)
            val candidates = jsonRoot.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val textPart = parts?.optJSONObject(0)?.optString("text")

            if (textPart.isNullOrBlank()) {
                return@withContext Result.failure(Exception("No transcription returned by Gemini Audio"))
            }

            parseTranscriptionJsonResponse(textPart)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Call OpenAI Whisper API directly
     */
    private suspend fun callOpenAiWhisper(
        audioFile: File,
        task: AudioTask,
        languageCode: String,
        apiKey: String,
        initialPrompt: String?
    ): Result<TranscriptionResult> = withContext(Dispatchers.IO) {
        try {
            val endpoint = if (task == AudioTask.TRANSLATE_EN) {
                "https://api.openai.com/v1/audio/translations"
            } else {
                OPENAI_WHISPER_URL
            }

            val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("model", "whisper-1")
                .addFormDataPart("response_format", "verbose_json")
                .addFormDataPart("timestamp_granularities[]", "segment")

            if (languageCode != "auto" && task == AudioTask.TRANSCRIBE) {
                builder.addFormDataPart("language", languageCode)
            }
            if (!initialPrompt.isNullOrBlank()) {
                builder.addFormDataPart("prompt", initialPrompt)
            }

            val mediaType = "audio/*".toMediaType()
            builder.addFormDataPart("file", audioFile.name, audioFile.asRequestBody(mediaType))

            val request = Request.Builder()
                .url(endpoint)
                .header("Authorization", "Bearer $apiKey")
                .post(builder.build())
                .build()

            val response = okHttpClient.newCall(request).execute()
            val bodyString = response.body?.string()

            if (!response.isSuccessful || bodyString == null) {
                return@withContext Result.failure(Exception("OpenAI Whisper error ${response.code}: $bodyString"))
            }

            val json = JSONObject(bodyString)
            val fullText = json.optString("text", "")
            val detectedLang = json.optString("language", languageCode)
            val segmentsList = mutableListOf<TranscriptionSegment>()

            val segmentsArray = json.optJSONArray("segments")
            if (segmentsArray != null) {
                for (i in 0 until segmentsArray.length()) {
                    val segObj = segmentsArray.getJSONObject(i)
                    segmentsList.add(
                        TranscriptionSegment(
                            id = segObj.optInt("id", i + 1),
                            startSec = segObj.optDouble("start", 0.0),
                            endSec = segObj.optDouble("end", 0.0),
                            text = segObj.optString("text", "").trim(),
                            confidence = 0.96f
                        )
                    )
                }
            }

            Result.success(
                TranscriptionResult(
                    fullText = fullText,
                    detectedLanguage = detectedLang,
                    segments = segmentsList,
                    translatedText = if (task == AudioTask.TRANSLATE_EN) fullText else null,
                    summary = null,
                    actionItems = emptyList()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generate AI Summary & Action items for an existing transcript
     */
    suspend fun generateSummaryAndActions(transcript: String): Result<Pair<String, List<String>>> = withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank() || apiKey.equals("MY_GEMINI_API_KEY", ignoreCase = true)) {
                return@withContext Result.failure(Exception("Gemini API key is not configured."))
            }

            val prompt = """
                Analyze this audio transcription and produce a concise executive summary and list of key action items or takeaways.
                Transcript:
                $transcript

                Format your answer strictly as JSON with this schema:
                {
                  "summary": "2-3 concise summary sentences",
                  "actionItems": ["Takeaway 1", "Takeaway 2", "Takeaway 3"]
                }
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val bodyString = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))

            val root = JSONObject(bodyString)
            val candidate = root.optJSONArray("candidates")?.optJSONObject(0)
            val text = candidate?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text")
                ?: return@withContext Result.failure(Exception("No content returned"))

            val cleanJson = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val json = JSONObject(cleanJson)
            val summary = json.optString("summary", "Summary generated.")
            val actionsArray = json.optJSONArray("actionItems")
            val actions = mutableListOf<String>()
            if (actionsArray != null) {
                for (i in 0 until actionsArray.length()) {
                    actions.add(actionsArray.getString(i))
                }
            }

            Result.success(Pair(summary, actions))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseTranscriptionJsonResponse(rawText: String): Result<TranscriptionResult> {
        return try {
            val cleanJson = rawText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val json = JSONObject(cleanJson)

            val detectedLanguage = json.optString("detectedLanguage", "en")
            val fullText = json.optString("fullText", "")
            val translatedText = if (json.has("translatedText") && !json.isNull("translatedText")) json.getString("translatedText") else null
            val summary = if (json.has("summary") && !json.isNull("summary")) json.getString("summary") else null

            val segmentsList = mutableListOf<TranscriptionSegment>()
            val segmentsArray = json.optJSONArray("segments")
            if (segmentsArray != null) {
                for (i in 0 until segmentsArray.length()) {
                    val seg = segmentsArray.getJSONObject(i)
                    segmentsList.add(
                        TranscriptionSegment(
                            id = seg.optInt("id", i + 1),
                            startSec = seg.optDouble("startSec", 0.0),
                            endSec = seg.optDouble("endSec", 0.0),
                            text = seg.optString("text", "").trim(),
                            confidence = seg.optDouble("confidence", 0.95).toFloat()
                        )
                    )
                }
            }

            val actionsList = mutableListOf<String>()
            val actionsArray = json.optJSONArray("actionItems")
            if (actionsArray != null) {
                for (i in 0 until actionsArray.length()) {
                    actionsList.add(actionsArray.getString(i))
                }
            }

            Result.success(
                TranscriptionResult(
                    fullText = fullText,
                    detectedLanguage = detectedLanguage,
                    segments = segmentsList,
                    translatedText = translatedText,
                    summary = summary,
                    actionItems = actionsList
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
