package com.studycompanion.app.feature.history

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@Composable
fun ManualSessionDialog(
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (startAt: Long, endAt: Long, subject: String?) -> Unit
) {
    val colors = StudyTheme.colors
    val shapes = StudyTheme.shapes
    val typography = StudyTheme.typography

    var durationMinutes by remember { mutableIntStateOf(30) }
    var subjectText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = shapes.large,
        title = {
            Text(
                text = "Add Manual Session",
                style = typography.screenTitle,
                color = colors.textPrimary
            )
        },
        text = {
            Column {
                // Honest Verification Disclaimer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.small)
                        .background(colors.surfaceElevated)
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(16.dp).padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Manual time is tagged as not automatically verified to maintain study integrity.",
                            style = typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Duration: ${TimeFormatter.formatDurationShort(durationMinutes * 60L)}",
                    style = typography.cardTitle,
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Duration presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(15, 30, 45, 60, 90).forEach { mins ->
                        val isSelected = durationMinutes == mins
                        Box(
                            modifier = Modifier
                                .clip(shapes.pill)
                                .background(if (isSelected) colors.accent else colors.surfaceElevated)
                                .clickable { durationMinutes = mins }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${mins}m",
                                color = if (isSelected) colors.background else colors.textPrimary,
                                style = typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = subjectText,
                    onValueChange = { subjectText = it },
                    label = { Text("Subject or Topic (Optional)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.borderSubtle,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val zone = ZoneId.systemDefault()
                    val endAt = System.currentTimeMillis()
                    val startAt = endAt - (durationMinutes * 60 * 1000L)
                    onSave(startAt, endAt, subjectText)
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
            ) {
                Text("Add Session", color = colors.background)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        }
    )
}
