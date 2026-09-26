package com.studycompanion.app.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studycompanion.app.core.ui.component.StudyTimerHero
import com.studycompanion.app.core.ui.component.SyncStatusIndicator
import com.studycompanion.app.core.ui.component.TimeFormatter
import com.studycompanion.app.core.ui.theme.StudyTheme
import com.studycompanion.app.feature.focus.PauseIcon
import com.studycompanion.app.tracking.engine.FocusState
import kotlinx.coroutines.delay
import java.time.LocalTime

@Composable
fun HomeScreen(
    state: HomeUiState,
    onStartFocus: () -> Unit,
    onPauseFocus: () -> Unit = {},
    onResumeFocus: () -> Unit = {},
    onOpenFocusView: () -> Unit,
    onOpenStudyApps: () -> Unit,
    onOpenHealthCenter: () -> Unit,
    onOpenProfileSwitch: () -> Unit,
    onSetTarget: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudyTheme.colors
    val shapes = StudyTheme.shapes
    val typography = StudyTheme.typography

    // Ticking display state for active session: strictly UI display offset,
    // reflecting FocusEngine monotonic nano elapsed time without altering business logic.
    var currentSessionSeconds by remember { mutableLongStateOf(0L) }
    val isFocusing = state.focusSnapshot.state == FocusState.FOCUSING || state.focusSnapshot.state == FocusState.LOCKED_FOCUS

    LaunchedEffect(isFocusing, state.focusSnapshot.sessionStartMonotonicNanos) {
        if (isFocusing && state.focusSnapshot.sessionStartMonotonicNanos != null) {
            while (true) {
                currentSessionSeconds = state.focusSnapshot.calculateCurrentSessionElapsedSeconds()
                delay(500L)
            }
        } else {
            currentSessionSeconds = 0L
        }
    }

    val totalDisplaySeconds = state.statsOverview.todayAutomaticSeconds + currentSessionSeconds
    val targetSeconds = state.todayTarget?.adjustedTargetSeconds
        ?: state.todayTarget?.originalTargetSeconds
        ?: 0L

    var showEditTargetDialog by remember { mutableStateOf(false) }

    val greeting = remember {
        val hour = LocalTime.now().hour
        when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header: Chronoa Logo + Greeting, Profile Avatar, Sync Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.studycompanion.app.R.drawable.ic_chronoa_logo),
                    contentDescription = "Chronoa Logo",
                    modifier = Modifier
                        .size(44.dp)
                        .clip(shapes.medium)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = greeting,
                        style = typography.greeting,
                        color = colors.textSecondary
                    )
                    Text(
                        text = state.activeProfile?.name ?: "Student",
                        style = typography.profileName,
                        color = colors.textPrimary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                SyncStatusIndicator(
                    syncState = state.syncState,
                    onClick = null
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceElevated)
                        .clickable(onClick = onOpenProfileSwitch),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Switch Profile",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Hero Timer
        StudyTimerHero(
            elapsedSeconds = totalDisplaySeconds,
            targetSeconds = targetSeconds,
            focusState = state.focusSnapshot.state,
            appLabel = state.currentAppLabel
        )

        Spacer(modifier = Modifier.height(28.dp))

        // State Banners (Permission Missing, No Study Apps, etc.)
        when (state.presentationState) {
            HomePresentationState.PERMISSION_MISSING -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.medium)
                        .background(colors.statusWarning.copy(alpha = 0.12f))
                        .border(1.dp, colors.statusWarning.copy(alpha = 0.3f), shapes.medium)
                        .clickable(onClick = onOpenHealthCenter)
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = colors.statusWarning,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Setup Required",
                                style = typography.cardTitle,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Usage Access is required for automatic tracking.",
                                style = typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = colors.textSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            HomePresentationState.NO_STUDY_APPS -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.medium)
                        .background(colors.surfaceElevated)
                        .clickable(onClick = onOpenStudyApps)
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Choose Study Apps",
                                style = typography.cardTitle,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Select apps that should count as study time.",
                                style = typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = colors.textSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            HomePresentationState.OFFLINE -> {
                Text(
                    text = "Offline · Study tracking continues normally",
                    style = typography.bodySmall,
                    color = colors.textTertiary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            else -> {}
        }

        // Primary Hero Action Button: Strictly authoritative based on FocusEngine state
        val isCounting = state.focusSnapshot.state == FocusState.FOCUSING ||
                state.focusSnapshot.state == FocusState.LOCKED_FOCUS
        val isUserPaused = state.focusSnapshot.state == FocusState.PAUSED_USER

        when {
            isCounting -> {
                Button(
                    onClick = onPauseFocus,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = shapes.pill,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.surfaceElevated,
                        contentColor = colors.textPrimary
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(colors.borderSubtle)
                    )
                ) {
                    Icon(
                        imageVector = PauseIcon,
                        contentDescription = "Pause",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pause",
                        style = typography.cardTitle.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
            isUserPaused -> {
                Button(
                    onClick = onResumeFocus,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = shapes.pill,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.background
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Resume Study",
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Resume Study",
                        style = typography.cardTitle.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
            else -> {
                Button(
                    onClick = onStartFocus,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = shapes.pill,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.background
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start Today's Study",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Start Today's Study",
                        style = typography.cardTitle.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }

        if (state.focusSnapshot.state.isCounting || state.focusSnapshot.state.isPaused) {
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(
                onClick = onOpenFocusView,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Open Fullscreen Focus View",
                    style = typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Secondary Action: Edit Target
        OutlinedButton(
            onClick = { showEditTargetDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = shapes.pill,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = colors.textPrimary
            ),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(colors.borderSubtle)
            )
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = colors.textSecondary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (targetSeconds > 0L) "Edit Today's Target" else "Set Today's Target",
                style = typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Thin Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(colors.borderSubtle)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Quick 3-Metric Summary (Today, Sessions, Streak)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            MetricItem(
                label = "Today",
                value = TimeFormatter.formatDurationShort(totalDisplaySeconds)
            )
            MetricItem(
                label = "Sessions",
                value = state.statsOverview.todaySessionCount.toString()
            )
            MetricItem(
                label = "Streak",
                value = "${state.statsOverview.currentStreakDays}d"
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(colors.borderSubtle)
        )
    }

    // Edit Target Dialog
    if (showEditTargetDialog) {
        EditTargetDialog(
            currentTargetSeconds = targetSeconds,
            originalTargetSeconds = state.todayTarget?.originalTargetSeconds ?: targetSeconds,
            onDismiss = { showEditTargetDialog = false },
            onSave = { newSeconds ->
                onSetTarget(newSeconds)
                showEditTargetDialog = false
            }
        )
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String
) {
    val colors = StudyTheme.colors
    val typography = StudyTheme.typography

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = typography.bodySmall,
            color = colors.textSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = typography.cardTitle.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textPrimary
        )
    }
}

@Composable
fun EditTargetDialog(
    currentTargetSeconds: Long,
    originalTargetSeconds: Long,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit
) {
    val colors = StudyTheme.colors
    val shapes = StudyTheme.shapes
    val typography = StudyTheme.typography

    var selectedSeconds by remember { mutableLongStateOf(if (currentTargetSeconds > 0) currentTargetSeconds else 7200L) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = shapes.large,
        title = {
            Text(
                text = "Today's Target",
                style = typography.screenTitle,
                color = colors.textPrimary
            )
        },
        text = {
            Column {
                if (originalTargetSeconds > 0) {
                    Text(
                        text = "Original target: ${TimeFormatter.formatDurationShort(originalTargetSeconds)}",
                        style = typography.bodySmall,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Text(
                    text = TimeFormatter.formatDurationShort(selectedSeconds),
                    style = typography.timerLarge,
                    color = colors.accent,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Quick preset buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PresetChip("1h", 3600L, selectedSeconds) { selectedSeconds = it }
                    PresetChip("2h", 7200L, selectedSeconds) { selectedSeconds = it }
                    PresetChip("3h", 10800L, selectedSeconds) { selectedSeconds = it }
                    PresetChip("4h", 14400L, selectedSeconds) { selectedSeconds = it }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    OutlinedButton(
                        onClick = { selectedSeconds = maxOf(900L, selectedSeconds - 900L) },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("-15m")
                    }
                    OutlinedButton(
                        onClick = { selectedSeconds += 900L }
                    ) {
                        Text("+15m")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedSeconds) },
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
            ) {
                Text("Save Target", color = colors.background)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        }
    )
}

@Composable
private fun PresetChip(
    label: String,
    seconds: Long,
    currentSelected: Long,
    onSelect: (Long) -> Unit
) {
    val colors = StudyTheme.colors
    val shapes = StudyTheme.shapes
    val isSelected = currentSelected == seconds

    Box(
        modifier = Modifier
            .clip(shapes.pill)
            .background(if (isSelected) colors.accent else colors.surfaceElevated)
            .clickable { onSelect(seconds) }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) colors.background else colors.textPrimary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp
        )
    }
}
