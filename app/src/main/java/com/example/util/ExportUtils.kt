package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.TranscriptionEntity
import com.example.data.model.TranscriptionSegment
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

enum class ExportFormat(val extension: String, val displayName: String, val mimeType: String) {
    TXT("txt", "Plain Text (.txt)", "text/plain"),
    SRT("srt", "SubRip Subtitles (.srt)", "application/x-subrip"),
    VTT("vtt", "WebVTT Subtitles (.vtt)", "text/vtt"),
    JSON("json", "JSON Segments (.json)", "application/json")
}

object ExportUtils {

    fun generatePlainText(item: TranscriptionEntity, includeTimestamps: Boolean = false): String {
        val sb = StringBuilder()
        sb.append(item.title).append("\n")
        sb.append("Language: ").append(item.detectedLanguage.uppercase()).append(" | ")
        sb.append("Date: ").append(java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(item.createdAt))).append("\n")
        sb.append("----------------------------------------\n\n")

        if (includeTimestamps) {
            val segments = item.parseSegments()
            if (segments.isNotEmpty()) {
                segments.forEach { seg ->
                    sb.append("[${seg.formatStartTimestamp()} -> ${seg.formatEndTimestamp()}] ")
                    sb.append(seg.text).append("\n\n")
                }
            } else {
                sb.append(item.fullText).append("\n")
            }
        } else {
            sb.append(item.fullText).append("\n")
        }

        if (!item.translatedText.isNullOrBlank()) {
            sb.append("\n========================================\n")
            sb.append("ENGLISH TRANSLATION\n")
            sb.append("========================================\n")
            sb.append(item.translatedText).append("\n")
        }

        if (!item.summary.isNullOrBlank()) {
            sb.append("\n========================================\n")
            sb.append("EXECUTIVE SUMMARY\n")
            sb.append("========================================\n")
            sb.append(item.summary).append("\n")
        }

        val actions = item.parseActionItems()
        if (actions.isNotEmpty()) {
            sb.append("\nKEY ACTION ITEMS / TAKEAWAYS:\n")
            actions.forEachIndexed { i, act ->
                sb.append("${i + 1}. $act\n")
            }
        }

        return sb.toString()
    }

    fun generateSrt(item: TranscriptionEntity): String {
        val segments = item.parseSegments()
        val sb = StringBuilder()
        segments.forEachIndexed { index, seg ->
            sb.append("${index + 1}\n")
            sb.append("${seg.formatSrtStart()} --> ${seg.formatSrtEnd()}\n")
            sb.append(seg.text.trim()).append("\n\n")
        }
        return sb.toString().trim()
    }

    fun generateVtt(item: TranscriptionEntity): String {
        val segments = item.parseSegments()
        val sb = StringBuilder()
        sb.append("WEBVTT - ").append(item.title).append("\n\n")
        segments.forEachIndexed { index, seg ->
            sb.append("${index + 1}\n")
            sb.append("${seg.formatVttStart()} --> ${seg.formatVttEnd()}\n")
            sb.append(seg.text.trim()).append("\n\n")
        }
        return sb.toString().trim()
    }

    fun generateJson(item: TranscriptionEntity): String {
        val root = JSONObject()
        root.put("id", item.id)
        root.put("title", item.title)
        root.put("createdAt", item.createdAt)
        root.put("durationMillis", item.durationMillis)
        root.put("detectedLanguage", item.detectedLanguage)
        root.put("modelUsed", item.modelUsed)
        root.put("fullText", item.fullText)
        root.put("translatedText", item.translatedText ?: JSONObject.NULL)
        root.put("summary", item.summary ?: JSONObject.NULL)

        val segmentsArray = JSONArray()
        item.parseSegments().forEach { seg ->
            val sObj = JSONObject()
            sObj.put("id", seg.id)
            sObj.put("startSec", seg.startSec)
            sObj.put("endSec", seg.endSec)
            sObj.put("text", seg.text)
            sObj.put("confidence", seg.confidence)
            segmentsArray.put(sObj)
        }
        root.put("segments", segmentsArray)
        return root.toString(2)
    }

    fun getExportContent(item: TranscriptionEntity, format: ExportFormat): String {
        return when (format) {
            ExportFormat.TXT -> generatePlainText(item, includeTimestamps = true)
            ExportFormat.SRT -> generateSrt(item)
            ExportFormat.VTT -> generateVtt(item)
            ExportFormat.JSON -> generateJson(item)
        }
    }

    fun copyToClipboard(context: Context, text: String, label: String = "Whisper Transcript") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun shareText(context: Context, content: String, title: String = "Share Transcription") {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, content)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, title)
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }

    fun shareExportFile(context: Context, item: TranscriptionEntity, format: ExportFormat) {
        try {
            val content = getExportContent(item, format)
            val cleanTitle = item.title.replace("[^a-zA-Z0-9_]".toRegex(), "_")
            val fileName = "whisper_${cleanTitle}_${item.id}.${format.extension}"
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(exportDir, fileName)
            file.writeText(content)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_STREAM, uri)
                type = format.mimeType
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Export ${format.displayName}"))
        } catch (e: Exception) {
            Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
