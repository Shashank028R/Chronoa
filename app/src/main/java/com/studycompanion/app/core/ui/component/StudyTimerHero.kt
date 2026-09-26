package com.studycompanion.app.core.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.studycompanion.app.core.ui.theme.StudyTheme
import com.studycompanion.app.tracking.engine.FocusState

object TimeFormatter {
    fun formatHms(totalSeconds: Long): String {
        val s = maxOf(0L, totalSeconds)
        val hours = s / 3600
        val minutes = (s % 3600) / 60
        val seconds = s % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun formatDurationShort(totalSeconds: Long): String {
        val s = maxOf(0L, totalSeconds)
        val hours = s / 3600
        val minutes = (s % 3600) / 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            s > 0 -> "${s}s"
            else -> "0m"
        }
    }
}

@Composable
fun StudyTimerHero(
    elapsedSeconds: Long,
    targetSeconds: Long,
    focusState: FocusState,
    appLabel: String? = null,
    modifier: Modifier = Modifier
) {
    val colors = StudyTheme.colors
    val typography = StudyTheme.typography

    val progress = if (targetSeconds > 0L) {
        (elapsedSeconds.toFloat() / targetSeconds.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600),
        label = "targetProgress"
    )

    val remainingSeconds = maxOf(0L, targetSeconds - elapsedSeconds)
    val remainingText = when {
        targetSeconds <= 0L -> "No target set"
        elapsedSeconds >= targetSeconds -> "Target achieved"
        else -> "${TimeFormatter.formatDurationShort(remainingSeconds)} remaining"
    }

    val progressRingColor = when (focusState) {
        FocusState.FOCUSING, FocusState.LOCKED_FOCUS -> colors.accent
        FocusState.PAUSED_UNAPPROVED_APP, FocusState.PAUSED_HOME, FocusState.PAUSED_CALL, FocusState.PAUSED_USER -> colors.statusPaused
        else -> colors.accent.copy(alpha = 0.5f)
    }
    val trackRingColor = colors.borderSubtle

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(232.dp)) {
                val strokeWidth = 8.dp.toPx()
                // Background Track
                drawCircle(
                    color = trackRingColor,
                    style = Stroke(width = strokeWidth)
                )
                // Progress Arc
                if (animatedProgress > 0f) {
                    drawArc(
                        color = progressRingColor,
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = TimeFormatter.formatHms(elapsedSeconds),
                    style = typography.timerHero,
                    color = colors.textPrimary
                )
                Text(
                    text = "focused today",
                    style = typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Target comparison line (e.g. 2h 14m / 3h)
        val targetText = if (targetSeconds > 0L) {
            "${TimeFormatter.formatDurationShort(elapsedSeconds)} / ${TimeFormatter.formatDurationShort(targetSeconds)}"
        } else {
            TimeFormatter.formatDurationShort(elapsedSeconds)
        }

        Text(
            text = targetText,
            style = typography.cardTitle,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = remainingText,
            style = typography.bodyMedium,
            color = colors.textSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        StatusPill(
            state = focusState,
            appLabel = appLabel
        )
    }
}
