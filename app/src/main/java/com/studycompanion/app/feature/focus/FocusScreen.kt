package com.studycompanion.app.feature.focus

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studycompanion.app.core.ui.component.FocusStateFormatter
import com.studycompanion.app.core.ui.component.StatusPill
import com.studycompanion.app.core.ui.component.TimeFormatter
import com.studycompanion.app.core.ui.theme.AmoledStudyColors
import com.studycompanion.app.core.ui.theme.StudyCompanionTheme
import com.studycompanion.app.core.ui.theme.StudyTheme
import com.studycompanion.app.tracking.engine.FocusState
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun FocusScreen(
    state: FocusUiState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onExit: () -> Unit,
    onToggleDisplayMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Pure AMOLED true black background
    val amoledBg = Color(0xFF000000)

    // Elapsed timer display state: strictly UI display offset,
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

    // Current clock time state for Clock Mode
    var currentClockTime by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentClockTime = LocalDateTime.now()
            delay(1000L)
        }
    }

    // OLED Anti-burn-in shift: periodically drifts clock position slightly in X and Y
    // so no phosphor or OLED subpixel is continuously activated in the same coordinates.
    // Transitions smoothly using a gentle 2.5-second easing so it does not distract the student.
    val burnInOffsets = remember {
        listOf(
            0.dp to 0.dp,
            (-12).dp to (-28).dp,
            14.dp to 22.dp,
            (-16).dp to 18.dp,
            12.dp to (-24).dp,
            (-10).dp to 30.dp,
            16.dp to (-16).dp,
            0.dp to 26.dp,
            (-14).dp to (-12).dp,
            10.dp to 16.dp
        )
    }
    var offsetIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(40_000L) // Drifts every 40 seconds
            offsetIndex = (offsetIndex + 1) % burnInOffsets.size
        }
    }

    val currentOffset = burnInOffsets[offsetIndex]
    val animatedOffsetX by animateDpAsState(
        targetValue = currentOffset.first,
        animationSpec = tween(
            durationMillis = 2500,
            easing = FastOutSlowInEasing
        ),
        label = "burnInShiftX"
    )
    val animatedOffsetY by animateDpAsState(
        targetValue = currentOffset.second,
        animationSpec = tween(
            durationMillis = 2500,
            easing = FastOutSlowInEasing
        ),
        label = "burnInShiftY"
    )

    // Single-tap controls toggle to keep screen ultra-clean and prevent static element burn-in
    var showControls by remember { mutableStateOf(false) }

    // Auto-hide controls after 5 seconds of inactivity
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(5000L)
            showControls = false
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? android.app.Activity

    // Immersive Fullscreen: Hide status bar and system bars while in full-screen view mode
    androidx.compose.runtime.DisposableEffect(activity) {
        val window = activity?.window
        if (window != null) {
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior =
                androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            onDispose {
                insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        } else {
            onDispose {}
        }
    }

    val targetSeconds = state.todayTarget?.adjustedTargetSeconds
        ?: state.todayTarget?.originalTargetSeconds
        ?: 0L

    val isPaused = state.focusSnapshot.state == FocusState.PAUSED_UNAPPROVED_APP ||
            state.focusSnapshot.state == FocusState.PAUSED_HOME ||
            state.focusSnapshot.state == FocusState.PAUSED_CALL ||
            state.focusSnapshot.state == FocusState.PAUSED_USER

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(amoledBg)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) {
                showControls = !showControls
            }
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        // Top Bar Buttons: Exit button, OLED Burn-in indicator, Mode toggle (revealed on single tap)
        androidx.compose.animation.AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF141414))
                        .clickable(onClick = onExit),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Exit Focus",
                        tint = Color(0xFFA1A1AA),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // OLED Burn-in Protection Pill
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF141414))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "OLED Shift Active",
                            color = Color(0xFF71717A),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF141414))
                        .clickable(onClick = onToggleDisplayMode)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color(0xFFA1A1AA),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (state.displayMode == FocusDisplayMode.MINIMAL_AMOLED) "Clock Mode" else "AMOLED Mode",
                            color = Color(0xFFA1A1AA),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Center Content: Middle details remain visible and drift together with anti-burn-in offset
        AnimatedContent(
            targetState = state.displayMode,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = animatedOffsetX, y = animatedOffsetY),
            label = "focusModeTransition"
        ) { mode ->
            when (mode) {
                FocusDisplayMode.MINIMAL_AMOLED -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = TimeFormatter.formatHms(totalDisplaySeconds),
                            style = StudyTheme.typography.timerHero.copy(
                                fontSize = 64.sp,
                                lineHeight = 72.sp,
                                fontWeight = FontWeight.ExtraLight
                            ),
                            color = Color(0xFFFAFAFA)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        StatusPill(
                            state = state.focusSnapshot.state,
                            appLabel = state.currentAppLabel
                        )

                        if (targetSeconds > 0L) {
                            Spacer(modifier = Modifier.height(16.dp))
                            val remaining = maxOf(0L, targetSeconds - totalDisplaySeconds)
                            Text(
                                text = if (remaining > 0L) "${TimeFormatter.formatDurationShort(remaining)} remaining" else "Daily Target Reached",
                                style = StudyTheme.typography.bodySmall,
                                color = Color(0xFF71717A)
                            )
                        }
                    }
                }

                FocusDisplayMode.CLOCK -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Current Real-world Clock
                        Text(
                            text = currentClockTime.format(DateTimeFormatter.ofPattern("hh:mm a")),
                            style = StudyTheme.typography.timerHero.copy(
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            color = Color(0xFFFAFAFA)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = currentClockTime.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                            style = StudyTheme.typography.bodyMedium,
                            color = Color(0xFF71717A)
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // Focused study time indicator
                        Text(
                            text = "Focused Study Time",
                            style = StudyTheme.typography.labelMedium,
                            color = Color(0xFF71717A)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = TimeFormatter.formatHms(totalDisplaySeconds),
                            style = StudyTheme.typography.timerLarge,
                            color = AmoledStudyColors.accent
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        StatusPill(
                            state = state.focusSnapshot.state,
                            appLabel = state.currentAppLabel
                        )
                    }
                }
            }
        }

        // Bottom Controls: Pause / Resume buttons (revealed on single tap)
        androidx.compose.animation.AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isPaused) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                            .clickable(onClick = onResume),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Resume Tracking",
                            tint = Color(0xFF000000),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1C1C1E))
                            .clickable(onClick = onPause),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = PauseIcon,
                            contentDescription = "Pause Tracking",
                            tint = Color(0xFFE4E4E7),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

internal val PauseIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Pause",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(6f, 19f)
            horizontalLineToRelative(4f)
            verticalLineTo(5f)
            horizontalLineTo(6f)
            verticalLineToRelative(14f)
            close()
            moveTo(14f, 5f)
            verticalLineToRelative(14f)
            horizontalLineToRelative(4f)
            verticalLineTo(5f)
            horizontalLineToRelative(-4f)
            close()
        }
    }.build()
}
