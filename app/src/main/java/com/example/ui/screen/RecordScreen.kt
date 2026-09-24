package com.example.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AudioTask
import com.example.data.model.TranscriptionSegment
import com.example.data.model.WhisperLanguages
import com.example.data.model.WhisperModel
import com.example.ui.component.LiveRecordingWaveform
import com.example.ui.theme.WhisperCyan
import com.example.ui.theme.WhisperIndigo
import com.example.ui.theme.WhisperIndigoLight
import com.example.ui.theme.WhisperViolet
import com.example.ui.viewmodel.WhisperViewModel
import com.example.util.RecordingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    viewModel: WhisperViewModel,
    onNavigateBack: () -> Unit,
    onTranscriptionReady: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasRecordPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasRecordPermission = granted
    }

    val recordingState by viewModel.recorderManager.recordingState.collectAsStateWithLifecycle()
    val durationMillis by viewModel.recorderManager.durationMillis.collectAsStateWithLifecycle()
    val recentAmplitudes by viewModel.recorderManager.recentAmplitudes.collectAsStateWithLifecycle()
    val liveSpeechState by viewModel.liveSpeechRecognizer.state.collectAsStateWithLifecycle()

    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val selectedTask by viewModel.selectedTask.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()
    val recordingCategory by viewModel.recordingCategory.collectAsStateWithLifecycle()
    val recordingTitle by viewModel.recordingTitle.collectAsStateWithLifecycle()
    val initialPrompt by viewModel.initialPrompt.collectAsStateWithLifecycle()

    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val processingMessage by viewModel.processingMessage.collectAsStateWithLifecycle()

    var showLanguagePicker by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }

    val categories = listOf("General", "Meeting", "Lecture", "Voice Note", "Interview", "Podcast")

    // Blinking recording dot animation
    val infiniteTransition = rememberInfiniteTransition(label = "recordingDot")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Recording Studio", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (recordingState != RecordingState.IDLE) {
                                showDiscardConfirm = true
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
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
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!hasRecordPermission) {
                    PermissionRequestCard(
                        onRequestPermission = {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Studio Visualizer Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // State and Timer Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (recordingState == RecordingState.RECORDING) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444).copy(alpha = dotAlpha))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "REC",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444)
                                )
                            } else if (recordingState == RecordingState.PAUSED) {
                                Text(
                                    text = "PAUSED",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF59E0B)
                                )
                            } else {
                                Text(
                                    text = "READY",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WhisperCyan
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Large Digital Timer
                        Text(
                            text = TranscriptionSegment.formatTime(durationMillis / 1000.0),
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Live Equalizer / Waveform
                        LiveRecordingWaveform(
                            amplitudes = recentAmplitudes,
                            isRecording = recordingState == RecordingState.RECORDING,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Live Dictation Preview Card
                val fullLiveText = (liveSpeechState.finalizedText + " " + liveSpeechState.partialText).trim()
                if (fullLiveText.isNotBlank() || recordingState == RecordingState.RECORDING) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, WhisperIndigo.copy(alpha = 0.25f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Psychology,
                                        contentDescription = null,
                                        tint = WhisperCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Live Dictation Stream",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = WhisperCyan
                                    )
                                }
                                if (liveSpeechState.isListening) {
                                    Text(
                                        text = "● Listening",
                                        fontSize = 11.sp,
                                        color = Color(0xFF10B981)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = fullLiveText.ifBlank { "Speak into the microphone to see real-time speech preview..." },
                                fontSize = 14.sp,
                                color = if (fullLiveText.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                                lineHeight = 20.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Title Input
                OutlinedTextField(
                    value = recordingTitle,
                    onValueChange = { viewModel.recordingTitle.value = it },
                    label = { Text("Recording Title (Optional)") },
                    placeholder = { Text("e.g. Team Sync, Biology Lecture") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recording_title_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Whisper Language & Task Configuration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Language Selector Button
                    val lang = WhisperLanguages.getByCode(selectedLanguage)
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
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Language", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${lang.flag} ${lang.name}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }

                    // Task Mode Selector
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                viewModel.selectedTask.value = if (selectedTask == AudioTask.TRANSCRIBE) {
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
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Task Mode", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = if (selectedTask == AudioTask.TRANSLATE_EN) "➔ English" else "Transcribe",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selectedTask == AudioTask.TRANSLATE_EN) WhisperCyan else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Whisper Model Selector Chips
                Text(
                    text = "AI Speech Model",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
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
                            onClick = { viewModel.selectedModel.value = model },
                            label = { Text(model.displayName, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = WhisperIndigo.copy(alpha = 0.2f),
                                selectedLabelColor = WhisperIndigoLight
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Category Chips
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
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
                            selected = recordingCategory == category,
                            onClick = { viewModel.recordingCategory.value = category },
                            label = { Text(category, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Main Recording Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cancel / Discard
                    if (recordingState != RecordingState.IDLE) {
                        IconButton(
                            onClick = { showDiscardConfirm = true },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Discard",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    // Main Action Button (Record / Pause / Resume)
                    Surface(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .clickable {
                                if (!hasRecordPermission) {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    return@clickable
                                }
                                when (recordingState) {
                                    RecordingState.IDLE -> viewModel.startRecording()
                                    RecordingState.RECORDING -> viewModel.pauseRecording()
                                    RecordingState.PAUSED -> viewModel.resumeRecording()
                                    RecordingState.STOPPED -> viewModel.startRecording()
                                }
                            }
                            .testTag("main_record_toggle_button"),
                        shape = CircleShape,
                        color = if (recordingState == RecordingState.RECORDING) Color(0xFFEF4444) else WhisperIndigo,
                        shadowElevation = 6.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when (recordingState) {
                                    RecordingState.RECORDING -> Icons.Default.Pause
                                    RecordingState.PAUSED -> Icons.Default.PlayArrow
                                    else -> Icons.Default.Mic
                                },
                                contentDescription = "Record Control",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    // Done / Finish & Process Button
                    if (recordingState != RecordingState.IDLE) {
                        IconButton(
                            onClick = {
                                viewModel.finishRecordingAndProcess { id ->
                                    onTranscriptionReady(id)
                                }
                            },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                                .testTag("finish_recording_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Finish and Transcribe",
                                tint = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))
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
                                text = "Transcribing with Whisper",
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
                                    viewModel.selectedLanguage.value = lang.code
                                    showLanguagePicker = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${lang.flag}  ${lang.name}", fontSize = 15.sp)
                            if (selectedLanguage == lang.code) {
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

    // Discard Confirmation Dialog
    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text("Discard Recording?") },
            text = { Text("Are you sure you want to stop and delete this recording?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.cancelRecording()
                        showDiscardConfirm = false
                        onNavigateBack()
                    }
                ) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) {
                    Text("Keep Recording")
                }
            }
        )
    }
}

@Composable
private fun PermissionRequestCard(onRequestPermission: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Microphone Permission Required",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "Required for speech recording and live transcription",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Grant", color = Color.White)
            }
        }
    }
}
