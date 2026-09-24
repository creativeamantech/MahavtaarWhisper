package com.example.ui.screen

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AudioTask
import com.example.data.model.WhisperLanguages
import com.example.data.model.WhisperModel
import com.example.ui.theme.WhisperCyan
import com.example.ui.theme.WhisperIndigo
import com.example.ui.theme.WhisperIndigoLight
import com.example.ui.viewmodel.WhisperViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportAudioScreen(
    viewModel: WhisperViewModel,
    onNavigateBack: () -> Unit,
    onTranscriptionReady: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedAudioUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var selectedFileSize by remember { mutableStateOf(0L) }

    var importTitle by remember { mutableStateOf("") }
    var selectedLang by remember { mutableStateOf("auto") }
    var selectedTask by remember { mutableStateOf(AudioTask.TRANSCRIBE) }
    var selectedModel by remember { mutableStateOf(WhisperModel.WHISPER_TURBO) }
    var selectedCategory by remember { mutableStateOf("Meeting") }

    var showLanguagePicker by remember { mutableStateOf(false) }

    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val processingMessage by viewModel.processingMessage.collectAsStateWithLifecycle()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedAudioUri = uri
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex >= 0) selectedFileName = cursor.getString(nameIndex)
                        if (sizeIndex >= 0) selectedFileSize = cursor.getLong(sizeIndex)
                    }
                }
            } catch (_: Exception) {
                selectedFileName = "Selected Audio File"
            }
            if (importTitle.isBlank()) {
                importTitle = selectedFileName.substringBeforeLast(".")
            }
        }
    }

    val categories = listOf("Meeting", "Lecture", "Voice Note", "Interview", "Podcast", "General")

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Import Audio File", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Dropzone Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { filePickerLauncher.launch("audio/*") }
                        .testTag("audio_file_dropzone"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedAudioUri != null) WhisperIndigo.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(
                        1.5.dp,
                        if (selectedAudioUri != null) WhisperCyan else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (selectedAudioUri != null) WhisperCyan.copy(alpha = 0.2f) else WhisperIndigo.copy(alpha = 0.15f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (selectedAudioUri != null) Icons.Default.AudioFile else Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = if (selectedAudioUri != null) WhisperCyan else WhisperIndigoLight,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (selectedAudioUri != null) {
                            Text(
                                text = selectedFileName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val sizeKb = selectedFileSize / 1024
                            Text(
                                text = "File Size: ${if (sizeKb > 1024) "${sizeKb / 1024} MB" else "$sizeKb KB"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = WhisperCyan
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Tap to choose a different file",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "Select Audio File",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Supports MP3, M4A, WAV, AAC, OGG, FLAC",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { filePickerLauncher.launch("audio/*") },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Browse Files")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Title Input
                OutlinedTextField(
                    value = importTitle,
                    onValueChange = { importTitle = it },
                    label = { Text("Transcription Title") },
                    placeholder = { Text("e.g. Q3 Earnings Call, Interview Audio") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Language & Task
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val lang = WhisperLanguages.getByCode(selectedLang)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showLanguagePicker = true },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Spoken Language", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${lang.flag} ${lang.name}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedTask = if (selectedTask == AudioTask.TRANSCRIBE) {
                                    AudioTask.TRANSLATE_EN
                                } else {
                                    AudioTask.TRANSCRIBE
                                }
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedTask == AudioTask.TRANSLATE_EN) WhisperCyan.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (selectedTask == AudioTask.TRANSLATE_EN) WhisperCyan else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Task Type", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = if (selectedTask == AudioTask.TRANSLATE_EN) "➔ Translate EN" else "Transcribe",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selectedTask == AudioTask.TRANSLATE_EN) WhisperCyan else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Whisper Model Selector
                Text(
                    text = "Transcription Model",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WhisperModel.values().forEach { model ->
                        FilterChip(
                            selected = selectedModel == model,
                            onClick = { selectedModel = model },
                            label = { Text(model.displayName, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = WhisperIndigo.copy(alpha = 0.2f),
                                selectedLabelColor = WhisperIndigoLight
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Category Selector
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Start Transcription Action
                Button(
                    onClick = {
                        val uri = selectedAudioUri ?: return@Button
                        viewModel.importAndProcessAudio(
                            uri = uri,
                            title = importTitle,
                            task = selectedTask,
                            language = selectedLang,
                            model = selectedModel,
                            category = selectedCategory,
                            onCompleted = onTranscriptionReady
                        )
                    },
                    enabled = selectedAudioUri != null && !isProcessing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("start_file_transcription_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Transcribe Audio File", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Processing Overlay
            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.75f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.padding(32.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = WhisperIndigoLight,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                text = "Transcribing Imported File",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = processingMessage.ifBlank { "Processing audio timestamps & multilingual speech recognition..." },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    // Language Picker Dialog
    if (showLanguagePicker) {
        AlertDialog(
            onDismissRequest = { showLanguagePicker = false },
            title = { Text("Select Audio Language") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    WhisperLanguages.ALL.forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedLang = lang.code
                                    showLanguagePicker = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${lang.flag}  ${lang.name}", fontSize = 15.sp)
                            if (selectedLang == lang.code) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = WhisperCyan)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguagePicker = false }) {
                    Text("Close")
                }
            }
        )
    }
}
