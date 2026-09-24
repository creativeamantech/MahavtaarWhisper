package com.example.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.ui.theme.WhisperCyan
import com.example.ui.theme.WhisperIndigo
import com.example.ui.theme.WhisperIndigoLight
import com.example.ui.theme.WhisperViolet
import kotlin.math.sin

@Composable
fun LiveRecordingWaveform(
    amplitudes: List<Float>,
    isRecording: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 36
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val idlePulse by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idlePulse"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f
            val spacing = width / barCount
            val barWidth = spacing * 0.55f

            // Fill or sample amplitudes
            val sampled = if (amplitudes.isEmpty() || !isRecording) {
                List(barCount) { index ->
                    (sin((index * 0.35f) + idlePulse * 5f) * 0.15f + 0.1f).coerceIn(0.06f, 0.4f)
                }
            } else {
                List(barCount) { i ->
                    val sourceIndex = (i * amplitudes.size / barCount).coerceIn(0, amplitudes.size - 1)
                    amplitudes.getOrNull(sourceIndex) ?: 0.08f
                }
            }

            sampled.forEachIndexed { index, amp ->
                val x = index * spacing + (spacing - barWidth) / 2f
                val barHeight = (height * amp * 0.85f).coerceAtLeast(6f)
                val top = centerY - barHeight / 2f

                val brush = Brush.verticalGradient(
                    colors = listOf(
                        WhisperCyan,
                        WhisperIndigoLight,
                        WhisperViolet
                    ),
                    startY = top,
                    endY = top + barHeight
                )

                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(x, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }
    }
}

@Composable
fun InteractivePlaybackWaveform(
    progressRatio: Float, // 0.0 to 1.0
    durationMs: Long,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    barCount: Int = 48
) {
    // Generate deterministic pseudo-random heights based on seed for aesthetic audio waveform look
    val bars = remember(durationMs) {
        List(barCount) { index ->
            val v1 = sin(index * 0.4f) * 0.35f + 0.45f
            val v2 = sin(index * 0.9f) * 0.2f
            (v1 + v2).toFloat().coerceIn(0.15f, 0.95f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek(ratio)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f
            val spacing = width / barCount
            val barWidth = spacing * 0.58f

            val playedCutoffX = width * progressRatio.coerceIn(0f, 1f)

            bars.forEachIndexed { index, amp ->
                val x = index * spacing + (spacing - barWidth) / 2f
                val barHeight = height * amp * 0.8f
                val top = centerY - barHeight / 2f

                val isPlayed = x <= playedCutoffX
                val color = if (isPlayed) WhisperIndigoLight else Color.Gray.copy(alpha = 0.35f)

                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }

            // Draw current playhead indicator line
            if (playedCutoffX > 0) {
                drawLine(
                    color = WhisperCyan,
                    start = Offset(playedCutoffX, 4f),
                    end = Offset(playedCutoffX, height - 4f),
                    strokeWidth = 3.dp.toPx()
                )
            }
        }
    }
}
