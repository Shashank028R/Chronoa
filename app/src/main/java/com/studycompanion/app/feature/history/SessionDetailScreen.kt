package com.studycompanion.app.feature.history

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studycompanion.app.core.ui.component.TimeFormatter
import com.studycompanion.app.core.ui.theme.StudyTheme
import com.studycompanion.app.domain.model.StudySession
import com.studycompanion.app.domain.model.TrackingType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SessionDetailDialog(
    session: StudySession,
    appLabel: String?,
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit
) {
    val colors = StudyTheme.colors
    val shapes = StudyTheme.shapes
    val typography = StudyTheme.typography

    var showDeleteConfirm by remember { mutableStateOf(false) }

    val isAutomatic = session.trackingType == TrackingType.AUTOMATIC
    val zone = ZoneId.systemDefault()
    val timeFormat = DateTimeFormatter.ofPattern("hh:mm:ss a")

    val startStr = Instant.ofEpochMilli(session.startAt).atZone(zone).format(timeFormat)
    val endStr = Instant.ofEpochMilli(session.endAt).atZone(zone).format(timeFormat)

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = colors.surface,
            shape = shapes.large,
            title = {
                Text(
                    text = "Delete Session?",
                    style = typography.cardTitle,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove this study session? This cannot be undone.",
                    style = typography.bodyMedium,
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(session.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.statusError)
                ) {
                    Text("Delete", color = colors.background)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = shapes.large,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Session Details",
                    style = typography.screenTitle,
                    color = colors.textPrimary
                )
                // Verification Badge
                Box(
                    modifier = Modifier
                        .clip(shapes.pill)
                        .background(if (isAutomatic) colors.accentSubtle else colors.surfaceElevated)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isAutomatic) Icons.Default.Check else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isAutomatic) colors.accent else colors.textSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isAutomatic) "VERIFIED" else "MANUAL",
                            style = typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isAutomatic) colors.accent else colors.textSecondary
                        )
                    }
                }
            }
        },
        text = {
            Column {
                // Large duration
                Text(
                    text = TimeFormatter.formatDurationShort(session.durationSeconds),
                    style = typography.timerLarge,
                    color = colors.textPrimary
                )

                val isOwnApp = session.packageName == "com.studycompanion.app" ||
                    session.packageName == "com.studycompanion.app.debug" ||
                    session.packageName?.contains("studycompanion") == true

                val displayAppName = when {
                    isOwnApp -> "Chronoa"
                    !appLabel.isNullOrBlank() && !appLabel.equals("debug", ignoreCase = true) -> appLabel
                    !session.packageName.isNullOrBlank() -> {
                        val last = session.packageName.substringAfterLast('.')
                        if (last.equals("debug", ignoreCase = true) || last.isBlank()) "Chronoa" else session.packageName
                    }
                    else -> "Unknown"
                }

                Text(
                    text = displayAppName,
                    style = typography.bodyMedium,
                    color = colors.textSecondary
                )

                if (!session.subjectId.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Subject: ${session.subjectId}",
                        style = typography.bodySmall,
                        color = colors.accent
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                DetailRow("Start Time", startStr)
                DetailRow("End Time", endStr)
                DetailRow("Tracking Type", if (isAutomatic) "Automatic Focus Engine" else "Manual Entry")
                DetailRow("Status", if (isAutomatic) "Verified by Rules" else "Not Automatically Verified")
            }
        },
        confirmButton = {
            TextButton(
                onClick = { showDeleteConfirm = true }
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = colors.statusError,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Delete", color = colors.statusError)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = colors.textPrimary)
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    val colors = StudyTheme.colors
    val typography = StudyTheme.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = typography.bodySmall, color = colors.textSecondary)
        Text(text = value, style = typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = colors.textPrimary)
    }
}
