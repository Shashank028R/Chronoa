package com.studycompanion.app.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studycompanion.app.core.ui.component.TimeFormatter
import com.studycompanion.app.core.ui.theme.StudyTheme
import com.studycompanion.app.domain.model.StudySession
import com.studycompanion.app.domain.model.TrackingType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onSelectDate: (LocalDate) -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onOpenSessionDetail: (StudySession) -> Unit,
    onCloseSessionDetail: () -> Unit,
    onShowAddManual: () -> Unit,
    onHideAddManual: () -> Unit,
    onAddManualSession: (startAt: Long, endAt: Long, subject: String?) -> Unit,
    onDeleteSession: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudyTheme.colors
    val shapes = StudyTheme.shapes
    val typography = StudyTheme.typography

    val isToday = state.selectedDate == LocalDate.now()
    val canGoForward = !state.selectedDate.isEqual(LocalDate.now()) && state.selectedDate.isBefore(LocalDate.now())

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "History",
                style = typography.screenTitle,
                color = colors.textPrimary
            )

            Button(
                onClick = onShowAddManual,
                shape = shapes.pill,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.surfaceElevated,
                    contentColor = colors.textPrimary
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = colors.accent
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Add Session", style = typography.bodySmall.copy(fontWeight = FontWeight.Medium))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Date Ribbon Navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shapes.medium)
                .background(colors.surfaceElevated)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousDay) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Previous Day",
                    tint = colors.textPrimary
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isToday) "Today" else state.selectedDate.format(DateTimeFormatter.ofPattern("EEEE")),
                    style = typography.cardTitle,
                    color = colors.textPrimary
                )
                Text(
                    text = state.selectedDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
                    style = typography.bodySmall,
                    color = colors.textSecondary
                )
            }

            IconButton(
                onClick = onNextDay,
                enabled = canGoForward
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Next Day",
                    tint = if (canGoForward) colors.textPrimary else colors.textTertiary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Daily Summary Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shapes.medium)
                .background(colors.surface)
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "Total Focused Time",
                            style = typography.bodySmall,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = TimeFormatter.formatDurationShort(state.daySummary.totalSeconds),
                            style = typography.timerLarge,
                            color = colors.textPrimary
                        )
                    }

                    if (state.daySummary.targetSeconds > 0L) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Target",
                                style = typography.bodySmall,
                                color = colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = TimeFormatter.formatDurationShort(state.daySummary.targetSeconds),
                                style = typography.cardTitle,
                                color = if (state.daySummary.isTargetMet) colors.statusStudying else colors.textPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Breakdown: Automatic vs Manual
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${TimeFormatter.formatDurationShort(state.daySummary.automaticSeconds)} verified automatic",
                        style = typography.bodySmall,
                        color = colors.accent
                    )
                    if (state.daySummary.manualSeconds > 0L) {
                        Text(
                            text = "+${TimeFormatter.formatDurationShort(state.daySummary.manualSeconds)} manual",
                            style = typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "SESSIONS (${state.sessions.size})",
            style = typography.sectionHeader,
            color = colors.textTertiary
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (state.sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No study sessions recorded on this day.",
                    style = typography.bodyMedium,
                    color = colors.textTertiary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.sessions, key = { it.id }) { session ->
                    SessionRowItem(
                        session = session,
                        appLabel = state.appLabels[session.packageName],
                        onClick = { onOpenSessionDetail(session) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Dialogs
    if (state.showAddManualDialog) {
        ManualSessionDialog(
            selectedDate = state.selectedDate,
            onDismiss = onHideAddManual,
            onSave = onAddManualSession
        )
    }

    if (state.showSessionDetailDialog && state.selectedSession != null) {
        val selectedPkg = state.selectedSession.packageName
        val isSelectedOwnApp = selectedPkg == "com.studycompanion.app" ||
            selectedPkg == "com.studycompanion.app.debug" ||
            selectedPkg?.contains("studycompanion") == true
        val resolvedDialogLabel = if (isSelectedOwnApp) {
            "Chronoa"
        } else {
            state.appLabels[selectedPkg]?.takeIf { !it.equals("debug", ignoreCase = true) }
                ?: selectedPkg?.substringAfterLast('.')?.takeIf { !it.equals("debug", ignoreCase = true) }
                ?: "Unknown"
        }

        SessionDetailDialog(
            session = state.selectedSession,
            appLabel = resolvedDialogLabel,
            onDismiss = onCloseSessionDetail,
            onDelete = onDeleteSession
        )
    }
}

@Composable
private fun SessionRowItem(
    session: StudySession,
    appLabel: String?,
    onClick: () -> Unit
) {
    val colors = StudyTheme.colors
    val shapes = StudyTheme.shapes
    val typography = StudyTheme.typography

    val isAuto = session.trackingType == TrackingType.AUTOMATIC
    val zone = ZoneId.systemDefault()
    val startTime = Instant.ofEpochMilli(session.startAt).atZone(zone).format(DateTimeFormatter.ofPattern("hh:mm a"))
    val endTime = Instant.ofEpochMilli(session.endAt).atZone(zone).format(DateTimeFormatter.ofPattern("hh:mm a"))

    val isOwnApp = session.packageName == "com.studycompanion.app" ||
        session.packageName == "com.studycompanion.app.debug" ||
        session.packageName?.contains("studycompanion") == true

    val displayTitle = when {
        isOwnApp -> "Chronoa"
        !appLabel.isNullOrBlank() && !appLabel.equals("debug", ignoreCase = true) -> appLabel
        !session.packageName.isNullOrBlank() -> {
            val lastPart = session.packageName.substringAfterLast('.')
            if (lastPart.equals("debug", ignoreCase = true) || lastPart.isBlank()) "Chronoa" else lastPart
        }
        !session.subjectId.isNullOrBlank() -> session.subjectId
        else -> "Unknown"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.medium)
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = displayTitle,
                        style = typography.cardTitle,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Subtle verification indicator
                    Icon(
                        imageVector = if (isAuto) Icons.Default.Check else Icons.Default.Info,
                        contentDescription = if (isAuto) "Verified Automatic" else "Manual",
                        tint = if (isAuto) colors.accent else colors.textTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "$startTime — $endTime",
                    style = typography.bodySmall,
                    color = colors.textSecondary
                )
            }

            Text(
                text = TimeFormatter.formatDurationShort(session.durationSeconds),
                style = typography.cardTitle.copy(fontWeight = FontWeight.SemiBold),
                color = if (isAuto) colors.textPrimary else colors.textSecondary
            )
        }
    }
}
