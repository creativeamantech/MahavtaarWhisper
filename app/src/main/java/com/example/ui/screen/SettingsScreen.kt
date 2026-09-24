package com.example.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.ui.theme.WhisperCyan
import com.example.ui.theme.WhisperIndigo
import com.example.ui.theme.WhisperIndigoLight
import com.example.ui.viewmodel.WhisperViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: WhisperViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val openAiApiKey by viewModel.openAiApiKey.collectAsStateWithLifecycle()
    val isLiveDictationEnabled by viewModel.isLiveDictationEnabled.collectAsStateWithLifecycle()
    val autoSummarize by viewModel.autoSummarize.collectAsStateWithLifecycle()
    val initialPrompt by viewModel.initialPrompt.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Settings & Models", fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // API Credentials Section
            Text(
                text = "AI API Credentials",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = WhisperIndigoLight
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Gemini Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = WhisperCyan, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Gemini 2.5 Audio Backend", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            val isGeminiConfigured = BuildConfig.GEMINI_API_KEY.isNotBlank() && !BuildConfig.GEMINI_API_KEY.equals("MY_GEMINI_API_KEY", ignoreCase = true)
                            Text(
                                text = if (isGeminiConfigured) "Active & Connected (AI Studio Secrets)" else "Configured via AI Studio Secrets Panel",
                                fontSize = 12.sp,
                                color = if (isGeminiConfigured) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // OpenAI Whisper API Key input
                    OutlinedTextField(
                        value = openAiApiKey,
                        onValueChange = { viewModel.openAiApiKey.value = it },
                        label = { Text("OpenAI API Key (Optional)") },
                        placeholder = { Text("sk-...") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = WhisperIndigoLight) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("openai_api_key_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Optional: If provided, app will directly call OpenAI's Whisper API endpoint (model: whisper-1).",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Whisper Vocabulary & Context Prompt
            Text(
                text = "Whisper Prompt Context",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = WhisperIndigoLight
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = initialPrompt,
                        onValueChange = { viewModel.initialPrompt.value = it },
                        label = { Text("Initial Vocabulary / Acronyms Hint") },
                        placeholder = { Text("e.g. Kotlin, Jetpack Compose, GraphQL, AI Studio") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Whisper uses this prompt to improve spelling of specific technical terms, names, or brand keywords.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Transcription Preferences
            Text(
                text = "Transcription & Dictation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = WhisperIndigoLight
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Live dictation switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Live Speech Dictation", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Show real-time words on screen while recording", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isLiveDictationEnabled,
                            onCheckedChange = { viewModel.isLiveDictationEnabled.value = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = WhisperIndigo)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Auto-summarize switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AI Summaries & Takeaways", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Automatically extract key action items and executive summaries", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = autoSummarize,
                            onCheckedChange = { viewModel.autoSummarize.value = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = WhisperIndigo)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // About Whisper Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = WhisperIndigo.copy(alpha = 0.12f)),
                border = BorderStroke(1.dp, WhisperIndigoLight.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = WhisperCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("About OpenAI Whisper", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = WhisperIndigoLight)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Whisper is an automatic speech recognition (ASR) system trained on 680,000 hours of multilingual and multitask supervised data. It supports 90+ languages, robust accents handling, background noise resistance, timestamped subtitle generation (SRT/VTT), and direct translation to English.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
