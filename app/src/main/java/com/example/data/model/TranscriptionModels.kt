package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

@Entity(tableName = "transcriptions")
data class TranscriptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val durationMillis: Long = 0,
    val audioFilePath: String? = null,
    val fullText: String,
    val translatedText: String? = null,
    val detectedLanguage: String = "en",
    val targetLanguage: String? = null,
    val modelUsed: String = "Whisper v3 Turbo",
    val taskType: String = "transcribe", // "transcribe" or "translate"
    val segmentsJson: String = "[]",
    val summary: String? = null,
    val actionItemsJson: String = "[]",
    val category: String = "General",
    val isFavorite: Boolean = false,
    val audioFileSize: Long = 0L,
    val wordCount: Int = 0
) {
    fun parseSegments(): List<TranscriptionSegment> {
        val list = mutableListOf<TranscriptionSegment>()
        try {
            val array = JSONArray(segmentsJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    TranscriptionSegment(
                        id = obj.optInt("id", i + 1),
                        startSec = obj.optDouble("startSec", 0.0),
                        endSec = obj.optDouble("endSec", 0.0),
                        text = obj.optString("text", ""),
                        confidence = obj.optDouble("confidence", 0.95).toFloat()
                    )
                )
            }
        } catch (_: Exception) {
            if (fullText.isNotBlank()) {
                list.add(
                    TranscriptionSegment(
                        id = 1,
                        startSec = 0.0,
                        endSec = (durationMillis / 1000.0).coerceAtLeast(1.0),
                        text = fullText,
                        confidence = 0.95f
                    )
                )
            }
        }
        return list
    }

    fun parseActionItems(): List<String> {
        val list = mutableListOf<String>()
        try {
            val array = JSONArray(actionItemsJson)
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
        } catch (_: Exception) {}
        return list
    }
}

data class TranscriptionSegment(
    val id: Int,
    val startSec: Double,
    val endSec: Double,
    val text: String,
    val confidence: Float = 0.95f
) {
    fun formatStartTimestamp(): String = formatTime(startSec)
    fun formatEndTimestamp(): String = formatTime(endSec)
    fun formatSrtStart(): String = formatSrtTime(startSec)
    fun formatSrtEnd(): String = formatSrtTime(endSec)
    fun formatVttStart(): String = formatVttTime(startSec)
    fun formatVttEnd(): String = formatVttTime(endSec)

    companion object {
        fun formatTime(seconds: Double): String {
            val totalSec = seconds.toLong()
            val hours = totalSec / 3600
            val minutes = (totalSec % 3600) / 60
            val secs = totalSec % 60
            return if (hours > 0) {
                String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, secs)
            } else {
                String.format(Locale.US, "%02d:%02d", minutes, secs)
            }
        }

        fun formatSrtTime(seconds: Double): String {
            val totalMillis = (seconds * 1000).toLong()
            val hours = totalMillis / 3600000
            val minutes = (totalMillis % 3600000) / 60000
            val secs = (totalMillis % 60000) / 1000
            val millis = totalMillis % 1000
            return String.format(Locale.US, "%02d:%02d:%02d,%03d", hours, minutes, secs, millis)
        }

        fun formatVttTime(seconds: Double): String {
            val totalMillis = (seconds * 1000).toLong()
            val hours = totalMillis / 3600000
            val minutes = (totalMillis % 3600000) / 60000
            val secs = (totalMillis % 60000) / 1000
            val millis = totalMillis % 1000
            return String.format(Locale.US, "%02d:%02d:%02d.%03d", hours, minutes, secs, millis)
        }
    }
}

data class WhisperLanguage(
    val code: String,
    val name: String,
    val flag: String
)

object WhisperLanguages {
    val ALL = listOf(
        WhisperLanguage("auto", "Auto Detect", "🌐"),
        WhisperLanguage("en", "English", "🇺🇸"),
        WhisperLanguage("es", "Spanish (Español)", "🇪🇸"),
        WhisperLanguage("fr", "French (Français)", "🇫🇷"),
        WhisperLanguage("de", "German (Deutsch)", "🇩🇪"),
        WhisperLanguage("zh", "Chinese (中文)", "🇨🇳"),
        WhisperLanguage("ja", "Japanese (日本語)", "🇯🇵"),
        WhisperLanguage("ko", "Korean (한국어)", "🇰🇷"),
        WhisperLanguage("hi", "Hindi (हिन्दी)", "🇮🇳"),
        WhisperLanguage("ar", "Arabic (العربية)", "🇸🇦"),
        WhisperLanguage("pt", "Portuguese (Português)", "🇧🇷"),
        WhisperLanguage("it", "Italian (Italiano)", "🇮🇹"),
        WhisperLanguage("ru", "Russian (Русский)", "🇷🇺"),
        WhisperLanguage("nl", "Dutch (Nederlands)", "🇳🇱"),
        WhisperLanguage("tr", "Turkish (Türkçe)", "🇹🇷"),
        WhisperLanguage("pl", "Polish (Polski)", "🇵🇱"),
        WhisperLanguage("sv", "Swedish (Svenska)", "🇸🇪"),
        WhisperLanguage("id", "Indonesian (Bahasa)", "🇮🇩"),
        WhisperLanguage("uk", "Ukrainian (Українська)", "🇺🇦"),
        WhisperLanguage("vi", "Vietnamese (Tiếng Việt)", "🇻🇳"),
        WhisperLanguage("th", "Thai (ไทย)", "🇹🇭"),
        WhisperLanguage("el", "Greek (Ελληνικά)", "🇬🇷"),
        WhisperLanguage("he", "Hebrew (עברית)", "🇮🇱"),
        WhisperLanguage("bn", "Bengali (বাংলা)", "🇧🇩"),
        WhisperLanguage("ur", "Urdu (اردو)", "🇵🇰")
    )

    fun getByCode(code: String): WhisperLanguage {
        return ALL.firstOrNull { it.code.equals(code, ignoreCase = true) }
            ?: WhisperLanguage(code, code.uppercase(Locale.ROOT), "🌐")
    }
}

enum class WhisperModel(val displayName: String, val description: String, val isCloud: Boolean) {
    WHISPER_LARGE_V3("Whisper Large-v3", "Highest accuracy multilingual ASR & translation", true),
    WHISPER_TURBO("Whisper Turbo", "Blazing fast speech recognition with high accuracy", true),
    GEMINI_AUDIO("Gemini 2.5 Audio", "Multimodal audio understanding, timestamps & summaries", true),
    ON_DEVICE("On-Device Voice Engine", "Real-time speech dictation, offline capable", false)
}

enum class AudioTask(val title: String, val description: String) {
    TRANSCRIBE("Transcribe", "Transcribe speech directly in spoken language"),
    TRANSLATE_EN("Translate to English", "Translate spoken audio directly into English")
}

data class TranscriptionResult(
    val fullText: String,
    val detectedLanguage: String,
    val segments: List<TranscriptionSegment>,
    val translatedText: String? = null,
    val summary: String? = null,
    val actionItems: List<String> = emptyList()
)
