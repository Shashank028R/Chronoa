package com.studycompanion.app.core.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.studycompanion.app.core.ui.theme.StudyTheme
import com.studycompanion.app.tracking.engine.FocusState

/**
 * Human-friendly status presentation mapping for authoritative FocusEngine states.
 */
object FocusStateFormatter {
    fun toDisplayName(state: FocusState, appLabel: String? = null): String {
        val cleanLabel = appLabel?.takeIf {
            it.isNotBlank() &&
            !it.equals("debug", ignoreCase = true) &&
            !it.equals("chronoa", ignoreCase = true) &&
            !it.contains("studycompanion", ignoreCase = true)
        }
        val labelSuffix = if (cleanLabel != null) " · $cleanLabel" else ""
        return when (state) {
            FocusState.FOCUSING -> "Studying$labelSuffix"
            FocusState.LOCKED_FOCUS -> "Studying · Screen Locked"
            FocusState.PAUSED_UNAPPROVED_APP -> "Paused"
            FocusState.PAUSED_HOME -> "Paused"
            FocusState.PAUSED_CALL -> "Paused · Call"
            FocusState.PAUSED_MULTIWINDOW -> "Paused · Multi-Window"
            FocusState.PAUSED_FLOATING -> "Paused · Floating"
            FocusState.PAUSED_USER -> "Paused"
            FocusState.WAITING_FOR_PERMISSION -> "Setup Required"
            FocusState.RECOVERING -> "Restoring Session"
            FocusState.READY -> "Ready to Study"
            FocusState.IDLE -> "Ready to Study"
            FocusState.ERROR -> "Needs Attention"
        }
    }

    fun formatState(state: FocusState, appLabel: String? = null): String = toDisplayName(state, appLabel)
}

@Composable
fun StatusPill(
    state: FocusState,
    appLabel: String? = null,
    modifier: Modifier = Modifier
) {
    val colors = StudyTheme.colors
    val shapes = StudyTheme.shapes
    val typography = StudyTheme.typography

    val dotColor by animateColorAsState(
        targetValue = when (state) {
            FocusState.FOCUSING, FocusState.LOCKED_FOCUS -> colors.statusStudying
            FocusState.PAUSED_UNAPPROVED_APP, FocusState.PAUSED_HOME, FocusState.PAUSED_CALL, FocusState.PAUSED_USER, FocusState.PAUSED_MULTIWINDOW, FocusState.PAUSED_FLOATING -> colors.statusPaused
            FocusState.WAITING_FOR_PERMISSION, FocusState.ERROR -> colors.statusWarning
            FocusState.IDLE, FocusState.READY, FocusState.RECOVERING -> colors.statusIdle
        },
        label = "statusDotColor"
    )

    val isPulsing = state == FocusState.FOCUSING || state == FocusState.LOCKED_FOCUS
    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulseAlpha by if (isPulsing) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0.35f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1100),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )
    } else {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1000)),
            label = "staticAlpha"
        )
    }

    Box(
        modifier = modifier
            .clip(shapes.pill)
            .background(colors.surfaceElevated)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(pulseAlpha)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = FocusStateFormatter.toDisplayName(state, appLabel),
                style = typography.statusPill,
                color = colors.textPrimary
            )
        }
    }
}
