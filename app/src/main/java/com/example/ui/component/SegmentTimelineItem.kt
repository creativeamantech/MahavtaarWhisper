package com.example.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TranscriptionSegment
import com.example.ui.theme.WhisperCyan
import com.example.ui.theme.WhisperIndigo
import com.example.ui.theme.WhisperIndigoLight
import com.example.util.ExportUtils

@Composable
fun SegmentTimelineItem(
    segment: TranscriptionSegment,
    isActive: Boolean,
    onJumpToTime: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val backgroundColor by animateColorAsState(
        targetValue = if (isActive) {
            WhisperIndigo.copy(alpha = 0.18f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "segmentBg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isActive) {
            WhisperCyan
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        },
        label = "segmentBorder"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onJumpToTime(segment.startSec) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(if (isActive) 1.5.dp else 1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 3.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Timestamp Badge & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Start-End Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isActive) WhisperCyan.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(0.5.dp, if (isActive) WhisperCyan else Color.Transparent)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isActive) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Playing",
                                    tint = WhisperCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = "${segment.formatStartTimestamp()}  ➔  ${segment.formatEndTimestamp()}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isActive) WhisperCyan else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Confidence Pill
                    val confidencePercent = (segment.confidence * 100).toInt()
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "$confidencePercent%",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Quick Play & Copy
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onJumpToTime(segment.startSec) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(if (isActive) WhisperCyan else MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play from ${segment.formatStartTimestamp()}",
                                tint = if (isActive) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            ExportUtils.copyToClipboard(context, segment.text, "Segment text")
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy segment text",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Transcript text
            Text(
                text = segment.text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 22.sp,
                    fontSize = 15.sp
                ),
                color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}
