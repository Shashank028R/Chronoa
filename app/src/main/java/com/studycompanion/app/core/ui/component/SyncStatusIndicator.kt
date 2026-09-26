package com.studycompanion.app.core.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.studycompanion.app.core.ui.theme.StudyTheme
import com.studycompanion.app.sync.model.SyncState

@Composable
fun SyncStatusIndicator(
    syncState: SyncState,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val colors = StudyTheme.colors
    val shapes = StudyTheme.shapes
    val typography = StudyTheme.typography

    val dotColor = when (syncState) {
        is SyncState.Synced -> colors.statusStudying
        is SyncState.Syncing -> colors.accent
        is SyncState.Offline -> colors.textTertiary
        is SyncState.Error -> colors.statusWarning
    }

    val labelText = when (syncState) {
        is SyncState.Synced -> "Synced"
        is SyncState.Syncing -> "Syncing"
        is SyncState.Offline -> "Offline"
        is SyncState.Error -> "Sync Error"
    }

    Box(
        modifier = modifier
            .clip(shapes.pill)
            .background(colors.surfaceElevated.copy(alpha = 0.6f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = labelText,
                style = typography.bodySmall,
                color = colors.textSecondary
            )
        }
    }
}
