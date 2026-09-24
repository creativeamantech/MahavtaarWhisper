package com.example.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TranscriptionEntity
import com.example.ui.theme.WhisperCyan
import com.example.ui.theme.WhisperIndigo
import com.example.ui.theme.WhisperIndigoLight
import com.example.ui.theme.WhisperViolet
import com.example.util.ExportFormat
import com.example.util.ExportUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportBottomSheet(
    transcription: TranscriptionEntity,
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var previewFormat by remember { mutableStateOf<ExportFormat?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Export Transcription",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Choose format to share, copy or generate subtitles",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Export Options
            ExportOptionRow(
                icon = Icons.Default.Description,
                title = "Plain Text (.txt)",
                subtitle = "Complete transcript with timestamps & summary",
                onShare = {
                    ExportUtils.shareExportFile(context, transcription, ExportFormat.TXT)
                    onDismiss()
                },
                onCopy = {
                    val text = ExportUtils.getExportContent(transcription, ExportFormat.TXT)
                    ExportUtils.copyToClipboard(context, text)
                    onDismiss()
                },
                onPreview = { previewFormat = ExportFormat.TXT }
            )

            Spacer(modifier = Modifier.height(12.dp))

            ExportOptionRow(
                icon = Icons.Default.Subtitles,
                title = "SubRip Subtitles (.srt)",
                subtitle = "Standard subtitle format for video editors & YouTube",
                onShare = {
                    ExportUtils.shareExportFile(context, transcription, ExportFormat.SRT)
                    onDismiss()
                },
                onCopy = {
                    val srt = ExportUtils.getExportContent(transcription, ExportFormat.SRT)
                    ExportUtils.copyToClipboard(context, srt)
                    onDismiss()
                },
                onPreview = { previewFormat = ExportFormat.SRT }
            )

            Spacer(modifier = Modifier.height(12.dp))

            ExportOptionRow(
                icon = Icons.Default.Subtitles,
                title = "WebVTT Subtitles (.vtt)",
                subtitle = "Modern web video subtitle format with timestamps",
                onShare = {
                    ExportUtils.shareExportFile(context, transcription, ExportFormat.VTT)
                    onDismiss()
                },
                onCopy = {
                    val vtt = ExportUtils.getExportContent(transcription, ExportFormat.VTT)
                    ExportUtils.copyToClipboard(context, vtt)
                    onDismiss()
                },
                onPreview = { previewFormat = ExportFormat.VTT }
            )

            Spacer(modifier = Modifier.height(12.dp))

            ExportOptionRow(
                icon = Icons.Default.Code,
                title = "JSON Structure (.json)",
                subtitle = "Raw segments array with startSec, endSec and metadata",
                onShare = {
                    ExportUtils.shareExportFile(context, transcription, ExportFormat.JSON)
                    onDismiss()
                },
                onCopy = {
                    val json = ExportUtils.getExportContent(transcription, ExportFormat.JSON)
                    ExportUtils.copyToClipboard(context, json)
                    onDismiss()
                },
                onPreview = { previewFormat = ExportFormat.JSON }
            )
        }
    }

    previewFormat?.let { format ->
        val content = ExportUtils.getExportContent(transcription, format)
        AlertDialog(
            onDismissRequest = { previewFormat = null },
            title = {
                Text(text = "${format.displayName} Preview")
            },
            text = {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = content,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        ExportUtils.copyToClipboard(context, content)
                        previewFormat = null
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy")
                }
            },
            dismissButton = {
                TextButton(onClick = { previewFormat = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun ExportOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onPreview: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = WhisperIndigo.copy(alpha = 0.15f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = WhisperIndigoLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onPreview,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Preview", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(6.dp))

                OutlinedButton(
                    onClick = onCopy,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(6.dp))

                Button(
                    onClick = onShare,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share", fontSize = 12.sp)
                }
            }
        }
    }
}
