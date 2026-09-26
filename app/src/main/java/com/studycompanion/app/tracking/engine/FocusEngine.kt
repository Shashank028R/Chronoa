package com.studycompanion.app.tracking.engine

import com.studycompanion.app.domain.model.ProfileSettings
import com.studycompanion.app.domain.model.SessionEvent
import com.studycompanion.app.domain.model.StudySession
import com.studycompanion.app.domain.model.TrackingType
import com.studycompanion.app.domain.model.VerificationStatus
import com.studycompanion.app.domain.repository.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Immutable snapshot of the Focus Engine's current state and active timing context.
 */
data class FocusSnapshot(
    val state: FocusState = FocusState.IDLE,
    val activeProfileId: String? = null,
    val currentPackage: String? = null,
    val currentSessionId: String? = null,
    val sessionStartWallTime: Long? = null,
    val sessionStartMonotonicNanos: Long? = null,
    val totalActiveElapsedMillis: Long = 0L,
    val lastTransitionTimestamp: Long = System.currentTimeMillis(),
    val reasonCode: String? = null
) {
    /**
     * Calculates the continuous elapsed seconds of the currently open session using monotonic clock.
     */
    fun calculateCurrentSessionElapsedSeconds(nowMonotonicNanos: Long = System.nanoTime()): Long {
        if (!state.isCounting || sessionStartMonotonicNanos == null) return 0L
        val elapsedNanos = maxOf(0L, nowMonotonicNanos - sessionStartMonotonicNanos)
        return elapsedNanos / 1_000_000_000L
    }
}

/**
 * Authoritative Focus Engine state machine.
 * Single source of truth for automatic study tracking.
 */
class FocusEngine(
    private val sessionRepository: SessionRepository? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val deviceId: String = "local-device",
    private val userSessionDataStore: com.studycompanion.app.core.datastore.UserSessionDataStore? = null
) {

    private val _state = MutableStateFlow(FocusSnapshot())
    val state: StateFlow<FocusSnapshot> = _state.asStateFlow()

    // Internal working context
    @Volatile
    private var context = EvaluationContext()

    // Set of known launcher packages
    private val launcherPackages = mutableSetOf(
        "com.android.launcher",
        "com.android.launcher3",
        "com.google.android.apps.nexuslauncher",
        "com.oneplus.launcher",
        "com.miui.home",
        "com.sec.android.app.launcher",
        "com.huawei.android.launcher",
        "com.oppo.launcher",
        "net.oneplus.launcher"
    )

    fun registerLauncherPackages(packages: Collection<String>) {
        launcherPackages.addAll(packages)
        context = context.copy(launcherPackages = launcherPackages)
    }

    fun registerOwnPackageName(packageName: String) {
        context = context.copy(ownPackageName = packageName)
    }

    private fun logDiagnostic(msg: String) {
        try {
            android.util.Log.i("FocusEngine", msg)
        } catch (t: Throwable) {
            // Ignore in headless JVM unit tests
        }
    }

    /**
     * Ingests a system or user event and executes the state machine transition.
     */
    @Synchronized
    fun onEvent(event: FocusEvent) {
        val oldSnapshot = _state.value
        val oldState = oldSnapshot.state

        // 1. Update internal evaluation context based on incoming event
        context = updateContextWithEvent(context, event, oldState)

        // 2. Evaluate new state via deterministic rule evaluator
        val newState = FocusRuleEvaluator.evaluate(context)

        // 3. Handle state transitions and session boundaries
        handleTransition(oldSnapshot, newState, event)
    }

    private fun updateContextWithEvent(
        current: EvaluationContext,
        event: FocusEvent,
        oldState: FocusState
    ): EvaluationContext {
        val base = current.copy(
            previousState = oldState,
            launcherPackages = launcherPackages
        )

        return when (event) {
            is FocusEvent.AppResumed -> base.copy(currentPackage = event.packageName)
            is FocusEvent.AppPaused -> {
                // If the paused app was the current foreground package, keep it until a new app resumes,
                // or clear if needed.
                base
            }
            is FocusEvent.KeyguardShown -> base.copy(isKeyguardLocked = true)
            is FocusEvent.KeyguardHidden -> base.copy(isKeyguardLocked = false)
            is FocusEvent.ScreenInteractive -> base.copy(isScreenInteractive = true)
            is FocusEvent.ScreenNonInteractive -> base.copy(isScreenInteractive = false)
            is FocusEvent.CallStarted -> base.copy(isCallActive = true)
            is FocusEvent.CallEnded -> base.copy(isCallActive = false)
            is FocusEvent.WindowModeChanged -> base.copy(
                inMultiWindow = event.inMultiWindow,
                inFloatingWindow = event.inFloating
            )
            is FocusEvent.ProfileSwitched -> base.copy(
                activeProfileId = event.profileId,
                approvedPackages = event.approvedPackages,
                countWhileLocked = event.settings.countWhileLocked,
                pauseDuringCalls = event.settings.pauseDuringCalls,
                pauseInMultiWindow = event.settings.pauseInMultiWindow,
                pauseInFloatingWindow = event.settings.pauseInFloatingWindow
            )
            is FocusEvent.StudyAppsUpdated -> base.copy(approvedPackages = event.approvedPackages)
            is FocusEvent.SettingsUpdated -> base.copy(
                countWhileLocked = event.settings.countWhileLocked,
                pauseDuringCalls = event.settings.pauseDuringCalls,
                pauseInMultiWindow = event.settings.pauseInMultiWindow,
                pauseInFloatingWindow = event.settings.pauseInFloatingWindow
            )
            is FocusEvent.PermissionChanged -> base.copy(hasUsageAccess = event.hasUsageAccess)
            is FocusEvent.UserStart -> {
                val pkg = base.currentPackage ?: base.ownPackageName
                base.copy(isTrackingStarted = true, isUserStopped = false, isUserPaused = false, currentPackage = pkg)
            }
            is FocusEvent.UserPause -> base.copy(isUserPaused = true)
            is FocusEvent.UserResume -> base.copy(isTrackingStarted = true, isUserPaused = false)
            is FocusEvent.UserStop -> base.copy(isTrackingStarted = false, isUserStopped = true)
            is FocusEvent.ClockChanged -> base
            is FocusEvent.TimezoneChanged -> base
            is FocusEvent.MidnightRolledOver -> base
            is FocusEvent.RecoveryTriggered -> base
        }
    }

    private fun handleTransition(
        oldSnapshot: FocusSnapshot,
        newState: FocusState,
        event: FocusEvent
    ) {
        val oldState = oldSnapshot.state
        val timestamp = event.timestamp
        val profileId = (event as? FocusEvent.ProfileSwitched)?.profileId
            ?: context.activeProfileId
            ?: oldSnapshot.activeProfileId

        logDiagnostic("Transition: event=${event::class.simpleName}, oldState=$oldState -> newState=$newState, pkg=${context.currentPackage}, profile=$profileId, trackingStarted=${context.isTrackingStarted}, userPaused=${context.isUserPaused}")

        // 3.1 Special platform event: Clock change
        if (event is FocusEvent.ClockChanged) {
            val isBackward = event.newTimestamp < event.oldTimestamp
            val direction = if (isBackward) "BACKWARD" else "FORWARD"

            if (oldState.isCounting) {
                val startWall = oldSnapshot.sessionStartWallTime ?: event.oldTimestamp
                val wallElapsedSeconds = maxOf(0L, (event.oldTimestamp - startWall) / 1000L)
                val monotonicSeconds = oldSnapshot.calculateCurrentSessionElapsedSeconds()
                val durationSeconds = if (monotonicSeconds > 0 && monotonicSeconds < wallElapsedSeconds) {
                    monotonicSeconds
                } else {
                    wallElapsedSeconds
                }
                val endWall = if (isBackward) maxOf(startWall, event.oldTimestamp) else (startWall + durationSeconds * 1000L)
                val sessionId = oldSnapshot.currentSessionId ?: UUID.randomUUID().toString()

                if (durationSeconds > 0 && profileId != null) {
                    persistCompletedSession(
                        sessionId = sessionId,
                        profileId = profileId,
                        packageName = oldSnapshot.currentPackage ?: context.currentPackage,
                        startAt = startWall,
                        endAt = endWall,
                        durationSeconds = durationSeconds
                    )
                }

                persistSessionEvent(
                    sessionId = sessionId,
                    profileId = profileId,
                    eventType = "CLOCK_CHANGED",
                    engineState = oldState.name,
                    packageName = oldSnapshot.currentPackage ?: context.currentPackage,
                    metadataJson = """{"direction":"$direction","old":${event.oldTimestamp},"new":${event.newTimestamp},"duration":$durationSeconds}""",
                    timestamp = event.newTimestamp
                )

                if (newState.isCounting) {
                    val nextSessionId = UUID.randomUUID().toString()
                    val nextMono = System.nanoTime()
                    _state.value = oldSnapshot.copy(
                        state = newState,
                        activeProfileId = profileId,
                        currentPackage = context.currentPackage,
                        currentSessionId = nextSessionId,
                        sessionStartWallTime = event.newTimestamp,
                        sessionStartMonotonicNanos = nextMono,
                        lastTransitionTimestamp = event.newTimestamp,
                        reasonCode = null
                    )
                    persistSessionEvent(
                        sessionId = nextSessionId,
                        profileId = profileId,
                        eventType = "FOCUS_STARTED",
                        engineState = newState.name,
                        packageName = context.currentPackage,
                        timestamp = event.newTimestamp
                    )
                    updateDurableSessionMarker(nextSessionId, profileId, context.currentPackage, event.newTimestamp)
                    return
                } else {
                    clearDurableSessionMarker()
                    _state.value = oldSnapshot.copy(
                        state = newState,
                        activeProfileId = profileId,
                        currentPackage = context.currentPackage,
                        currentSessionId = null,
                        sessionStartWallTime = null,
                        sessionStartMonotonicNanos = null,
                        lastTransitionTimestamp = event.newTimestamp,
                        reasonCode = newState.name
                    )
                    return
                }
            } else {
                persistSessionEvent(
                    sessionId = null,
                    profileId = profileId,
                    eventType = "CLOCK_CHANGED",
                    engineState = newState.name,
                    packageName = context.currentPackage,
                    metadataJson = """{"direction":"$direction","old":${event.oldTimestamp},"new":${event.newTimestamp}}""",
                    timestamp = event.newTimestamp
                )
                _state.value = oldSnapshot.copy(
                    state = newState,
                    activeProfileId = profileId,
                    currentPackage = context.currentPackage,
                    lastTransitionTimestamp = event.newTimestamp
                )
                return
            }
        }

        // 3.2 Special platform event: Timezone change
        if (event is FocusEvent.TimezoneChanged) {
            persistSessionEvent(
                sessionId = oldSnapshot.currentSessionId,
                profileId = profileId,
                eventType = "TIMEZONE_CHANGED",
                engineState = newState.name,
                packageName = context.currentPackage,
                metadataJson = """{"old":"${event.oldTimezoneId}","new":"${event.newTimezoneId}"}""",
                timestamp = event.timestamp
            )
            _state.value = oldSnapshot.copy(
                state = newState,
                activeProfileId = profileId,
                currentPackage = context.currentPackage,
                lastTransitionTimestamp = event.timestamp
            )
            return
        }

        // 3.3 Special platform event: Midnight rollover
        if (event is FocusEvent.MidnightRolledOver) {
            if (oldState.isCounting) {
                val startWall = oldSnapshot.sessionStartWallTime ?: event.previousDayBoundary
                val endWall = event.previousDayBoundary
                val durationDay1 = maxOf(0L, (endWall - startWall) / 1000L)
                val sessionDay1 = oldSnapshot.currentSessionId ?: UUID.randomUUID().toString()

                if (durationDay1 > 0 && profileId != null) {
                    persistCompletedSession(
                        sessionId = sessionDay1,
                        profileId = profileId,
                        packageName = oldSnapshot.currentPackage ?: context.currentPackage,
                        startAt = startWall,
                        endAt = endWall,
                        durationSeconds = durationDay1
                    )
                }

                persistSessionEvent(
                    sessionId = sessionDay1,
                    profileId = profileId,
                    eventType = "MIDNIGHT_ROLLOVER",
                    engineState = oldState.name,
                    packageName = oldSnapshot.currentPackage ?: context.currentPackage,
                    metadataJson = """{"boundary":${event.previousDayBoundary},"day1Duration":$durationDay1}""",
                    timestamp = event.previousDayBoundary
                )

                if (newState.isCounting) {
                    val sessionDay2 = UUID.randomUUID().toString()
                    val startMono = System.nanoTime()
                    _state.value = oldSnapshot.copy(
                        state = newState,
                        activeProfileId = profileId,
                        currentPackage = context.currentPackage,
                        currentSessionId = sessionDay2,
                        sessionStartWallTime = event.newDayBoundary,
                        sessionStartMonotonicNanos = startMono,
                        lastTransitionTimestamp = event.newDayBoundary,
                        reasonCode = null
                    )
                    persistSessionEvent(
                        sessionId = sessionDay2,
                        profileId = profileId,
                        eventType = "FOCUS_STARTED",
                        engineState = newState.name,
                        packageName = context.currentPackage,
                        timestamp = event.newDayBoundary
                    )
                    updateDurableSessionMarker(sessionDay2, profileId, context.currentPackage, event.newDayBoundary)
                    return
                } else {
                    clearDurableSessionMarker()
                    _state.value = oldSnapshot.copy(
                        state = newState,
                        activeProfileId = profileId,
                        currentPackage = context.currentPackage,
                        currentSessionId = null,
                        sessionStartWallTime = null,
                        sessionStartMonotonicNanos = null,
                        lastTransitionTimestamp = event.newDayBoundary,
                        reasonCode = newState.name
                    )
                    return
                }
            } else {
                persistSessionEvent(
                    sessionId = null,
                    profileId = profileId,
                    eventType = "MIDNIGHT_ROLLOVER",
                    engineState = newState.name,
                    packageName = context.currentPackage,
                    metadataJson = """{"boundary":${event.previousDayBoundary}}""",
                    timestamp = event.previousDayBoundary
                )
                _state.value = oldSnapshot.copy(
                    state = newState,
                    activeProfileId = profileId,
                    currentPackage = context.currentPackage,
                    lastTransitionTimestamp = event.timestamp
                )
                return
            }
        }

        // Transition: Ineligible -> Eligible (e.g. READY/PAUSED -> FOCUSING or LOCKED_FOCUS)
        if (!oldState.isCounting && newState.isCounting) {
            val newSessionId = UUID.randomUUID().toString()
            val startWall = timestamp
            val startMono = System.nanoTime()

            logDiagnostic("Session started: id=$newSessionId, state=$newState, pkg=${context.currentPackage}, startWall=$startWall")

            _state.value = oldSnapshot.copy(
                state = newState,
                activeProfileId = profileId,
                currentPackage = context.currentPackage,
                currentSessionId = newSessionId,
                sessionStartWallTime = startWall,
                sessionStartMonotonicNanos = startMono,
                lastTransitionTimestamp = timestamp,
                reasonCode = null
            )

            // Persist transition start event
            persistSessionEvent(
                sessionId = newSessionId,
                profileId = profileId,
                eventType = "FOCUS_STARTED",
                engineState = newState.name,
                packageName = context.currentPackage,
                timestamp = timestamp
            )
            updateDurableSessionMarker(newSessionId, profileId, context.currentPackage, timestamp)
            return
        }

        // Transition: Eligible -> Ineligible (e.g. FOCUSING -> PAUSED_HOME / PAUSED_UNAPPROVED_APP)
        if (oldState.isCounting && !newState.isCounting) {
            val startWall = oldSnapshot.sessionStartWallTime ?: timestamp
            val endWall = maxOf(startWall, timestamp)
            val durationSeconds = maxOf(0L, (endWall - startWall) / 1000L)
            val sessionId = oldSnapshot.currentSessionId ?: UUID.randomUUID().toString()

            logDiagnostic("Session closed: id=$sessionId, duration=${durationSeconds}s, reason=${newState.name}, totalElapsed=${oldSnapshot.totalActiveElapsedMillis + (endWall - startWall)}ms")

            // Close session interval and record to repository
            if (durationSeconds > 0 && profileId != null) {
                persistCompletedSession(
                    sessionId = sessionId,
                    profileId = profileId,
                    packageName = oldSnapshot.currentPackage ?: context.currentPackage,
                    startAt = startWall,
                    endAt = endWall,
                    durationSeconds = durationSeconds
                )
            }

            // Persist transition pause/stop event
            persistSessionEvent(
                sessionId = sessionId,
                profileId = profileId,
                eventType = if (newState == FocusState.IDLE) "FOCUS_STOPPED" else "FOCUS_PAUSED",
                engineState = newState.name,
                packageName = context.currentPackage,
                reasonCode = newState.name,
                timestamp = timestamp
            )

            clearDurableSessionMarker()

            _state.value = oldSnapshot.copy(
                state = newState,
                activeProfileId = profileId,
                currentPackage = context.currentPackage,
                currentSessionId = null,
                sessionStartWallTime = null,
                sessionStartMonotonicNanos = null,
                totalActiveElapsedMillis = oldSnapshot.totalActiveElapsedMillis + (endWall - startWall),
                lastTransitionTimestamp = timestamp,
                reasonCode = newState.name
            )
            return
        }

        // Transition: Eligible -> Eligible (e.g. FOCUSING -> LOCKED_FOCUS or vice-versa)
        if (oldState.isCounting && newState.isCounting) {
            val eventType = if (newState == FocusState.LOCKED_FOCUS) "LOCKED" else "UNLOCKED"
            _state.value = oldSnapshot.copy(
                state = newState,
                activeProfileId = profileId,
                lastTransitionTimestamp = timestamp,
                reasonCode = null
            )

            persistSessionEvent(
                sessionId = oldSnapshot.currentSessionId,
                profileId = profileId,
                eventType = eventType,
                engineState = newState.name,
                packageName = context.currentPackage,
                timestamp = timestamp
            )
            return
        }

        _state.value = oldSnapshot.copy(
            state = newState,
            activeProfileId = profileId,
            currentPackage = context.currentPackage,
            lastTransitionTimestamp = timestamp,
            reasonCode = if (newState.isPaused) newState.name else oldSnapshot.reasonCode
        )
    }

    private fun persistCompletedSession(
        sessionId: String,
        profileId: String,
        packageName: String?,
        startAt: Long,
        endAt: Long,
        durationSeconds: Long
    ) {
        val repo = sessionRepository ?: return
        val session = StudySession(
            id = sessionId,
            profileId = profileId,
            deviceId = deviceId,
            platform = "ANDROID",
            packageName = packageName,
            startAt = startAt,
            endAt = endAt,
            durationSeconds = durationSeconds,
            trackingType = TrackingType.AUTOMATIC,
            verificationStatus = VerificationStatus.VERIFIED_BY_RULES,
            syncState = "LOCAL"
        )

        scope.launch {
            try {
                repo.recordSession(session)
            } catch (e: Exception) {
                // Ignore in uninitialized test environments
            }
        }
    }

    private fun persistSessionEvent(
        sessionId: String?,
        profileId: String?,
        eventType: String,
        engineState: String,
        packageName: String?,
        reasonCode: String? = null,
        metadataJson: String? = null,
        timestamp: Long
    ) {
        val repo = sessionRepository ?: return
        val profId = profileId ?: return

        val event = SessionEvent(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            profileId = profId,
            deviceId = deviceId,
            eventType = eventType,
            timestamp = timestamp,
            packageName = packageName,
            engineState = engineState,
            reasonCode = reasonCode,
            source = "FOCUS_ENGINE",
            metadataJson = metadataJson
        )

        scope.launch {
            try {
                repo.recordEvent(event)
            } catch (e: Exception) {
                // Ignore in uninitialized test environments
            }
        }
    }

    private fun updateDurableSessionMarker(
        sessionId: String,
        profileId: String?,
        packageName: String?,
        startWall: Long
    ) {
        val store = userSessionDataStore ?: return
        val prof = profileId ?: return
        scope.launch {
            try {
                val marker = org.json.JSONObject().apply {
                    put("sessionId", sessionId)
                    put("profileId", prof)
                    put("packageName", packageName ?: "")
                    put("startWall", startWall)
                    put("lastHeartbeat", startWall)
                }
                store.setActiveSessionMarker(marker.toString())
            } catch (e: Exception) {
                // Non-critical persistence failure
            }
        }
    }

    private fun clearDurableSessionMarker() {
        val store = userSessionDataStore ?: return
        scope.launch {
            try {
                store.setActiveSessionMarker(null)
            } catch (e: Exception) {
                // Non-critical persistence failure
            }
        }
    }

    fun updateHeartbeat(timestamp: Long = System.currentTimeMillis()) {
        val snapshot = _state.value
        if (!snapshot.state.isCounting) return
        val sessionId = snapshot.currentSessionId ?: return
        val profileId = snapshot.activeProfileId ?: return
        val startWall = snapshot.sessionStartWallTime ?: return
        val store = userSessionDataStore ?: return
        scope.launch {
            try {
                val marker = org.json.JSONObject().apply {
                    put("sessionId", sessionId)
                    put("profileId", profileId)
                    put("packageName", snapshot.currentPackage ?: "")
                    put("startWall", startWall)
                    put("lastHeartbeat", timestamp)
                }
                store.setActiveSessionMarker(marker.toString())
            } catch (e: Exception) {
                // Non-critical persistence failure
            }
        }
    }
}
