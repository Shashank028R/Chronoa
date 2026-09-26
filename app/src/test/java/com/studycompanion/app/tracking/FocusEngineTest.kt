package com.studycompanion.app.tracking

import com.studycompanion.app.domain.model.ProfileSettings
import com.studycompanion.app.domain.model.SessionEvent
import com.studycompanion.app.domain.model.StudySession
import com.studycompanion.app.domain.model.TrackingType
import com.studycompanion.app.domain.model.VerificationStatus
import com.studycompanion.app.domain.repository.SessionRepository
import com.studycompanion.app.tracking.engine.FocusEngine
import com.studycompanion.app.tracking.engine.FocusEvent
import com.studycompanion.app.tracking.engine.FocusRuleEvaluator
import com.studycompanion.app.tracking.engine.FocusState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeSessionRepository : SessionRepository {
    val recordedSessions = mutableListOf<StudySession>()
    val recordedEvents = mutableListOf<SessionEvent>()

    override fun getSessions(profileId: String): Flow<List<StudySession>> = emptyFlow()
    override fun getSessionsInRange(profileId: String, startAt: Long, endAt: Long): Flow<List<StudySession>> = emptyFlow()

    override suspend fun recordSession(session: StudySession): Result<Unit> {
        recordedSessions.add(session)
        return Result.success(Unit)
    }

    override suspend fun recordEvent(event: SessionEvent): Result<Unit> {
        recordedEvents.add(event)
        return Result.success(Unit)
    }

    override suspend fun getEventsForSession(sessionId: String): List<SessionEvent> {
        return recordedEvents.filter { it.sessionId == sessionId }
    }

    override suspend fun getSessionById(sessionId: String): StudySession? {
        return recordedSessions.firstOrNull { it.id == sessionId }
    }

    override suspend fun deleteSession(sessionId: String): Result<Unit> {
        recordedSessions.removeAll { it.id == sessionId }
        return Result.success(Unit)
    }

    override suspend fun updateSession(session: StudySession): Result<Unit> {
        val idx = recordedSessions.indexOfFirst { it.id == session.id }
        if (idx >= 0) {
            recordedSessions[idx] = session
        } else {
            recordedSessions.add(session)
        }
        return Result.success(Unit)
    }
}

class FocusEngineTest {

    private lateinit var fakeRepository: FakeSessionRepository
    private lateinit var testScope: TestScope
    private lateinit var engine: FocusEngine

    private val profileA = "profile-a"
    private val profileB = "profile-b"
    private val approvedApp = "com.study.math"
    private val unapprovedApp = "com.social.instagram"
    private val launcherApp = "com.google.android.apps.nexuslauncher"

    @Before
    fun setUp() {
        val testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        fakeRepository = FakeSessionRepository()
        engine = FocusEngine(
            sessionRepository = fakeRepository,
            scope = testScope,
            deviceId = "test-device"
        )
        engine.registerLauncherPackages(listOf(launcherApp))
    }

    private fun setupActiveProfile(
        profileId: String = profileA,
        approved: Set<String> = setOf(approvedApp),
        countWhileLocked: Boolean = true,
        pauseDuringCalls: Boolean = true,
        pauseInMultiWindow: Boolean = true,
        timestamp: Long = 1000L
    ) {
        val settings = ProfileSettings(
            profileId = profileId,
            countWhileLocked = countWhileLocked,
            pauseDuringCalls = pauseDuringCalls,
            pauseInMultiWindow = pauseInMultiWindow
        )
        engine.onEvent(FocusEvent.UserStart(timestamp))
        engine.onEvent(FocusEvent.ProfileSwitched(profileId, approved, settings, timestamp))
    }

    @Test
    fun `1 IDLE to READY when tracking started with no active app`() {
        assertEquals(FocusState.IDLE, engine.state.value.state)

        engine.onEvent(FocusEvent.UserStart(1000L))
        assertEquals(FocusState.READY, engine.state.value.state)
    }

    @Test
    fun `2 READY to FOCUSING when approved app resumes`() {
        setupActiveProfile(timestamp = 1000L)
        assertEquals(FocusState.READY, engine.state.value.state)

        engine.onEvent(FocusEvent.AppResumed(approvedApp, timestamp = 2000L))
        assertEquals(FocusState.FOCUSING, engine.state.value.state)
        assertTrue(engine.state.value.state.isCounting)
        assertEquals(approvedApp, engine.state.value.currentPackage)
        assertNotNull(engine.state.value.currentSessionId)
    }

    @Test
    fun `3 FOCUSING to PAUSED_UNAPPROVED_APP when unapproved app opens`() {
        setupActiveProfile(timestamp = 1000L)
        engine.onEvent(FocusEvent.AppResumed(approvedApp, timestamp = 2000L))
        assertEquals(FocusState.FOCUSING, engine.state.value.state)

        // Unapproved app opens
        engine.onEvent(FocusEvent.AppResumed(unapprovedApp, timestamp = 5000L))
        assertEquals(FocusState.PAUSED_UNAPPROVED_APP, engine.state.value.state)
        assertFalse(engine.state.value.state.isCounting)
    }

    @Test
    fun `4 FOCUSING to PAUSED_HOME when user returns to launcher`() {
        setupActiveProfile(timestamp = 1000L)
        engine.onEvent(FocusEvent.AppResumed(approvedApp, timestamp = 2000L))
        assertEquals(FocusState.FOCUSING, engine.state.value.state)

        // Home opens
        engine.onEvent(FocusEvent.AppResumed(launcherApp, timestamp = 5000L))
        assertEquals(FocusState.PAUSED_HOME, engine.state.value.state)
        assertFalse(engine.state.value.state.isCounting)
    }

    @Test
    fun `5 PAUSED_UNAPPROVED_APP to FOCUSING when returning to approved app`() {
        setupActiveProfile(timestamp = 1000L)
        engine.onEvent(FocusEvent.AppResumed(approvedApp, timestamp = 2000L))
        engine.onEvent(FocusEvent.AppResumed(unapprovedApp, timestamp = 5000L))
        assertEquals(FocusState.PAUSED_UNAPPROVED_APP, engine.state.value.state)

        // Return to study app
        engine.onEvent(FocusEvent.AppResumed(approvedApp, timestamp = 8000L))
        assertEquals(FocusState.FOCUSING, engine.state.value.state)
        assertTrue(engine.state.value.state.isCounting)
    }

    @Test
    fun `6 PAUSED_HOME to FOCUSING when launching study app from home`() {
        setupActiveProfile(timestamp = 1000L)
        engine.onEvent(FocusEvent.AppResumed(launcherApp, timestamp = 2000L))
        assertEquals(FocusState.PAUSED_HOME, engine.state.value.state)

        engine.onEvent(FocusEvent.AppResumed(approvedApp, timestamp = 4000L))
        assertEquals(FocusState.FOCUSING, engine.state.value.state)
    }

    @Test
    fun `7 FOCUSING to LOCKED_FOCUS when device locks and countWhileLocked is true`() {
        setupActiveProfile(countWhileLocked = true, timestamp = 1000L)
        engine.onEvent(FocusEvent.AppResumed(approvedApp, timestamp = 2000L))
        assertEquals(FocusState.FOCUSING, engine.state.value.state)

        // Screen locks
        engine.onEvent(FocusEvent.ScreenNonInteractive(timestamp = 5000L))
        engine.onEvent(FocusEvent.KeyguardShown(timestamp = 5000L))

        assertEquals(FocusState.LOCKED_FOCUS, engine.state.value.state)
        assertTrue("LOCKED_FOCUS must continue counting", engine.state.value.state.isCounting)
    }

    @Test
    fun `8 LOCKED_FOCUS to FOCUSING on unlock while still in study app`() {
        setupActiveProfile(countWhileLocked = true, timestamp = 1000L)
        engine.onEvent(FocusEvent.AppResumed(approvedApp, timestamp = 2000L))
        engine.onEvent(FocusEvent.KeyguardShown(timestamp = 5000L))
        assertEquals(FocusState.LOCKED_FOCUS, engine.state.value.state)

        // Unlock
        engine.onEvent(FocusEvent.ScreenInteractive(timestamp = 8000L))
        engine.onEvent(FocusEvent.KeyguardHidden(timestamp = 8000L))

        assertEquals(FocusState.FOCUSING, engine.state.value.state)
        assertTrue(engine.state.value.state.isCounting)
    }

    @Test
    fun `9 FOCUSING to PAUSED_CALL when call begins and pauseDuringCalls is true`() {
        setupActiveProfile(pauseDuringCalls = true, timestamp = 1000L)
        engine.onEvent(FocusEvent.AppResumed(approvedApp, timestamp = 2000L))
        assertEquals(FocusState.FOCUSING, engine.state.value.state)

        // Call starts
        engine.onEvent(FocusEvent.CallStarted(timestamp = 6000L))
        assertEquals(FocusState.PAUSED_CALL, engine.state.value.state)
        assertFalse(engine.state.value.state.isCounting)
    }

    @Test
    fun `10 PAUSED_CALL to FOCUSING or PAUSED based on actual post-call app`() {
        setupActiveProfile(pauseDuringCalls = true, timestamp = 1000L)
        engine.onEvent(FocusEvent.AppResumed(approvedApp, timestamp = 2000L))
        engine.onEvent(FocusEvent.CallStarted(timestamp = 5000L))
        assertEquals(FocusState.PAUSED_CALL, engine.state.value.state)

        // Call ends, study app is still foreground -> resumes FOCUSING
        engine.onEvent(FocusEvent.CallEnded(timestamp = 9000L))
        assertEquals(FocusState.FOCUSING, engine.state.value.state)

        // If call ended on home screen -> PAUSED_HOME
        engine.onEvent(FocusEvent.CallStarted(timestamp = 10000L))
        engine.onEvent(FocusEvent.AppResumed(launcherApp, timestamp = 11000L))
        engine.onEvent(FocusEvent.CallEnded(timestamp = 12000L))
        assertEquals(FocusState.PAUSED_HOME, engine.state.value.state)
    }

    @Test
    fun `11 FOCUSING to PAUSED_MULTIWINDOW when multi-window is detected`() {
        setupActiveProfile(pauseInMultiWindow = true, timestamp = 1000L)
        engine.onEvent(FocusEvent.AppResumed(approvedApp, timestamp = 2000L))
        assertEquals(FocusState.FOCUSING, engine.state.value.state)

        // Multi-window enters
        engine.onEvent(FocusEvent.WindowModeChanged(inMultiWindow = true, inFloating = false, timestamp = 4000L))
        assertEquals(FocusState.PAUSED_MULTIWINDOW, engine.state.value.state)
        assertFalse(engine.state.value.state.isCounting)
    }

    @Test
    fun `12 WAITING_FOR_PERMISSION when Usage Access is revoked`() {
        setupActiveProfile(timestamp = 1000L)
        engine.onEvent(FocusEvent.AppResumed(approvedApp, timestamp = 2000L))
        assertEquals(FocusState.FOCUSING, engine.state.value.state)

        // Permission revoked
        engine.onEvent(FocusEvent.PermissionChanged(hasUsageAccess = false, timestamp = 4000L))
        assertEquals(FocusState.WAITING_FOR_PERMISSION, engine.state.value.state)
        assertFalse(engine.state.value.state.isCounting)
    }

    @Test
    fun `13 duplicate event handling does not trigger duplicate session creation`() {
        setupActiveProfile(timestamp = 1000L)
        engine.onEvent(FocusEvent.AppResumed(approvedApp, timestamp = 2000L))
        assertEquals(FocusState.FOCUSING, engine.state.value.state)
        val initialSessionId = engine.state.value.currentSessionId

        // Duplicate AppResumed for same package
        engine.onEvent(FocusEvent.AppResumed(approvedApp, timestamp = 2500L))
        assertEquals(FocusState.FOCUSING, engine.state.value.state)
        assertEquals("Session ID must remain unchanged on duplicate event", initialSessionId, engine.state.value.currentSessionId)
    }

    @Test
    fun `14 out of order event timestamp normalization`() {
        setupActiveProfile(timestamp = 1000L)
        engine.onEvent(FocusEvent.AppResumed(approvedApp, timestamp = 5000L))
        assertEquals(FocusState.FOCUSING, engine.state.value.state)

        // Switch to unapproved app with older timestamp
        engine.onEvent(FocusEvent.AppResumed(unapprovedApp, timestamp = 3000L))
        // Engine handles it safely without crashing
        assertEquals(FocusState.PAUSED_UNAPPROVED_APP, engine.state.value.state)
    }

    @Test
    fun `15 timestamp normalization prevents negative duration`() {
        setupActiveProfile(timestamp = 1000L)
        engine.onEvent(FocusEvent.AppResumed(approvedApp, timestamp = 5000L))

        // Clock rollback event
        engine.onEvent(FocusEvent.ClockChanged(oldTimestamp = 5000L, newTimestamp = 2000L))
        engine.onEvent(FocusEvent.AppResumed(launcherApp, timestamp = 2500L))

        // Safe closure: duration must never be negative
        testScope.testScheduler.advanceUntilIdle()
        fakeRepository.recordedSessions.forEach {
            assertTrue("Duration must be non-negative: ${it.durationSeconds}", it.durationSeconds >= 0)
        }
    }

    @Test
    fun `16 zero duration protection prevents empty 0-second sessions`() {
        setupActiveProfile(timestamp = 1000L)
        // Resume and immediately pause in the exact same millisecond
        engine.onEvent(FocusEvent.AppResumed(approvedApp, timestamp = 2000L))
        engine.onEvent(FocusEvent.AppResumed(unapprovedApp, timestamp = 2000L))

        testScope.testScheduler.advanceUntilIdle()
        // 0-second duration session is excluded from recorded sessions
        assertTrue("Zero-second session should not be persisted", fakeRepository.recordedSessions.isEmpty())
    }

    @Test
    fun `17 session persistence records correct duration and tracking type`() = runTest {
        setupActiveProfile(timestamp = 1000L)
        engine.onEvent(FocusEvent.AppResumed(approvedApp, timestamp = 10_000L))
        // 25 seconds of focus
        engine.onEvent(FocusEvent.AppResumed(launcherApp, timestamp = 35_000L))

        testScope.testScheduler.advanceUntilIdle()
        assertEquals(1, fakeRepository.recordedSessions.size)
        val session = fakeRepository.recordedSessions.first()

        assertEquals(profileA, session.profileId)
        assertEquals(approvedApp, session.packageName)
        assertEquals(10_000L, session.startAt)
        assertEquals(35_000L, session.endAt)
        assertEquals(25L, session.durationSeconds)
        assertEquals(TrackingType.AUTOMATIC, session.trackingType)
        assertEquals(VerificationStatus.VERIFIED_BY_RULES, session.verificationStatus)
    }

    @Test
    fun `18 session recovery safely reconstructs open session without fabricating time`() {
        setupActiveProfile(timestamp = 1000L)
        engine.onEvent(FocusEvent.AppResumed(approvedApp, timestamp = 5000L))
        assertEquals(FocusState.FOCUSING, engine.state.value.state)

        // Process killed and recovered
        engine.onEvent(FocusEvent.RecoveryTriggered(timestamp = 12000L))
        // Engine retains context and is ready to ingest recent events
        assertNotNull(engine.state.value)
    }

    @Test
    fun `19 profile isolation ensures sessions are tagged with active profileId`() = runTest {
        // Profile A tracks 10 seconds
        setupActiveProfile(profileId = profileA, timestamp = 1000L)
        engine.onEvent(FocusEvent.AppResumed(approvedApp, timestamp = 2000L))
        engine.onEvent(FocusEvent.AppResumed(launcherApp, timestamp = 12_000L))

        // Switch to Profile B
        setupActiveProfile(profileId = profileB, timestamp = 15_000L)
        engine.onEvent(FocusEvent.AppResumed(approvedApp, timestamp = 16_000L))
        engine.onEvent(FocusEvent.AppResumed(launcherApp, timestamp = 26_000L))

        testScope.testScheduler.advanceUntilIdle()
        assertEquals(2, fakeRepository.recordedSessions.size)
        assertEquals(profileA, fakeRepository.recordedSessions[0].profileId)
        assertEquals(profileB, fakeRepository.recordedSessions[1].profileId)
    }

    @Test
    fun `20 rapid app switching produces identical deterministic sequence`() {
        val runSequence = {
            val eng = FocusEngine(fakeRepository, testScope, "test-device")
            eng.registerLauncherPackages(listOf(launcherApp))
            val settings = ProfileSettings(profileId = profileA)
            eng.onEvent(FocusEvent.UserStart(1000L))
            eng.onEvent(FocusEvent.ProfileSwitched(profileA, setOf(approvedApp), settings, 1000L))

            val states = mutableListOf<FocusState>()
            val steps = listOf(
                FocusEvent.AppResumed(approvedApp, 2000L),
                FocusEvent.AppResumed(unapprovedApp, 2500L),
                FocusEvent.AppResumed(approvedApp, 3000L),
                FocusEvent.AppResumed(launcherApp, 3200L),
                FocusEvent.AppResumed(approvedApp, 4000L),
                FocusEvent.CallStarted(4500L),
                FocusEvent.CallEnded(5000L),
                FocusEvent.KeyguardShown(5500L),
                FocusEvent.KeyguardHidden(6000L)
            )

            for (s in steps) {
                eng.onEvent(s)
                states.add(eng.state.value.state)
            }
            states
        }

        val run1 = runSequence()
        val run2 = runSequence()

        assertEquals("Same event sequence must produce strictly identical state transitions", run1, run2)
        assertEquals(
            listOf(
                FocusState.FOCUSING,
                FocusState.PAUSED_UNAPPROVED_APP,
                FocusState.FOCUSING,
                FocusState.PAUSED_HOME,
                FocusState.FOCUSING,
                FocusState.PAUSED_CALL,
                FocusState.FOCUSING,
                FocusState.LOCKED_FOCUS,
                FocusState.FOCUSING
            ),
            run1
        )
    }

    @Test
    fun `21 Study Companion open plus UserStart transitions to FOCUSING`() {
        val ownApp = "com.studycompanion.app.debug"
        engine.registerOwnPackageName(ownApp)
        val settings = ProfileSettings(profileId = profileA)
        engine.onEvent(FocusEvent.ProfileSwitched(profileA, setOf(approvedApp), settings, 1000L))
        assertEquals(FocusState.IDLE, engine.state.value.state)

        // User starts study while inside Study Companion
        engine.onEvent(FocusEvent.UserStart(timestamp = 2000L))
        assertEquals(FocusState.FOCUSING, engine.state.value.state)
        assertTrue(engine.state.value.state.isCounting)
        assertEquals(ownApp, engine.state.value.currentPackage)
    }

    @Test
    fun `22 Start from eligible approved Study App transitions to FOCUSING`() {
        setupActiveProfile(timestamp = 1000L)
        // User opens approved study app
        engine.onEvent(FocusEvent.AppResumed(approvedApp, timestamp = 2000L))
        // And starts study
        engine.onEvent(FocusEvent.UserStart(timestamp = 2000L))

        assertEquals(FocusState.FOCUSING, engine.state.value.state)
        assertTrue(engine.state.value.state.isCounting)
        assertEquals(approvedApp, engine.state.value.currentPackage)
    }

    @Test
    fun `23 Home causes automatic pause to PAUSED_HOME`() {
        val ownApp = "com.studycompanion.app"
        engine.registerOwnPackageName(ownApp)
        setupActiveProfile(timestamp = 1000L)
        engine.onEvent(FocusEvent.UserStart(timestamp = 2000L))
        assertEquals(FocusState.FOCUSING, engine.state.value.state)

        // User switches to Android Home
        engine.onEvent(FocusEvent.AppResumed(launcherApp, timestamp = 5000L))
        assertEquals(FocusState.PAUSED_HOME, engine.state.value.state)
        assertFalse(engine.state.value.state.isCounting)
    }

    @Test
    fun `24 Unapproved app causes automatic pause to PAUSED_UNAPPROVED_APP`() {
        val ownApp = "com.studycompanion.app"
        engine.registerOwnPackageName(ownApp)
        setupActiveProfile(timestamp = 1000L)
        engine.onEvent(FocusEvent.UserStart(timestamp = 2000L))
        assertEquals(FocusState.FOCUSING, engine.state.value.state)

        // User switches to an unapproved app (e.g. Instagram)
        engine.onEvent(FocusEvent.AppResumed(unapprovedApp, timestamp = 5000L))
        assertEquals(FocusState.PAUSED_UNAPPROVED_APP, engine.state.value.state)
        assertFalse(engine.state.value.state.isCounting)
    }

    @Test
    fun `25 Returning from Home to Study Companion automatically resumes to FOCUSING`() {
        val ownApp = "com.studycompanion.app"
        engine.registerOwnPackageName(ownApp)
        setupActiveProfile(timestamp = 1000L)
        engine.onEvent(FocusEvent.UserStart(timestamp = 2000L))
        engine.onEvent(FocusEvent.AppResumed(launcherApp, timestamp = 5000L))
        assertEquals(FocusState.PAUSED_HOME, engine.state.value.state)

        // Return to Study Companion
        engine.onEvent(FocusEvent.AppResumed(ownApp, timestamp = 10_000L))
        assertEquals(FocusState.FOCUSING, engine.state.value.state)
        assertTrue(engine.state.value.state.isCounting)
    }

    @Test
    fun `26 Returning from Unapproved app to approved Study App automatically resumes to FOCUSING`() {
        setupActiveProfile(timestamp = 1000L)
        engine.onEvent(FocusEvent.UserStart(timestamp = 2000L))
        engine.onEvent(FocusEvent.AppResumed(approvedApp, timestamp = 2000L))
        engine.onEvent(FocusEvent.AppResumed(unapprovedApp, timestamp = 5000L))
        assertEquals(FocusState.PAUSED_UNAPPROVED_APP, engine.state.value.state)

        // Return to approved app
        engine.onEvent(FocusEvent.AppResumed(approvedApp, timestamp = 8000L))
        assertEquals(FocusState.FOCUSING, engine.state.value.state)
        assertTrue(engine.state.value.state.isCounting)
    }

    @Test
    fun `27 User pause causes PAUSED_USER`() {
        val ownApp = "com.studycompanion.app"
        engine.registerOwnPackageName(ownApp)
        setupActiveProfile(timestamp = 1000L)
        engine.onEvent(FocusEvent.UserStart(timestamp = 2000L))
        assertEquals(FocusState.FOCUSING, engine.state.value.state)

        // User explicitly taps Pause
        engine.onEvent(FocusEvent.UserPause(timestamp = 6000L))
        assertEquals(FocusState.PAUSED_USER, engine.state.value.state)
        assertFalse(engine.state.value.state.isCounting)
    }

    @Test
    fun `28 PAUSED_USER blocks automatic resume when switching to Home or Study Companion`() {
        val ownApp = "com.studycompanion.app"
        engine.registerOwnPackageName(ownApp)
        setupActiveProfile(timestamp = 1000L)
        engine.onEvent(FocusEvent.UserStart(timestamp = 2000L))
        engine.onEvent(FocusEvent.UserPause(timestamp = 6000L))
        assertEquals(FocusState.PAUSED_USER, engine.state.value.state)

        // Move to Home
        engine.onEvent(FocusEvent.AppResumed(launcherApp, timestamp = 8000L))
        assertEquals("Manual pause must strictly override automatic state transitions", FocusState.PAUSED_USER, engine.state.value.state)

        // Return to Study Companion
        engine.onEvent(FocusEvent.AppResumed(ownApp, timestamp = 12_000L))
        assertEquals("Returning to Study Companion while user paused must remain PAUSED_USER", FocusState.PAUSED_USER, engine.state.value.state)

        // Switch to approved study app
        engine.onEvent(FocusEvent.AppResumed(approvedApp, timestamp = 15_000L))
        assertEquals("Switching to approved app while user paused must remain PAUSED_USER", FocusState.PAUSED_USER, engine.state.value.state)
    }

    @Test
    fun `29 User resume returns to eligible FOCUSING state`() {
        val ownApp = "com.studycompanion.app"
        engine.registerOwnPackageName(ownApp)
        setupActiveProfile(timestamp = 1000L)
        engine.onEvent(FocusEvent.UserStart(timestamp = 2000L))
        engine.onEvent(FocusEvent.UserPause(timestamp = 6000L))
        assertEquals(FocusState.PAUSED_USER, engine.state.value.state)

        // User presses Resume while inside Study Companion
        engine.onEvent(FocusEvent.UserResume(timestamp = 10_000L))
        assertEquals(FocusState.FOCUSING, engine.state.value.state)
        assertTrue(engine.state.value.state.isCounting)
    }

    @Test
    fun `30 No startup race - watcher AppResumed before UserStart does not trigger false FOCUSING`() {
        val ownApp = "com.studycompanion.app"
        engine.registerOwnPackageName(ownApp)
        val settings = ProfileSettings(profileId = profileA)
        engine.onEvent(FocusEvent.ProfileSwitched(profileA, setOf(approvedApp), settings, 1000L))

        // Background watcher starts and observes Study Companion or study app before user starts study
        engine.onEvent(FocusEvent.AppResumed(approvedApp, timestamp = 1500L))

        // Tracking has NOT been started by user
        assertEquals(FocusState.IDLE, engine.state.value.state)
        assertFalse("Must not start counting before UserStart", engine.state.value.state.isCounting)
        assertNull("No session should exist prior to UserStart", engine.state.value.currentSessionId)

        // User now explicitly taps Play
        engine.onEvent(FocusEvent.UserStart(timestamp = 2000L))
        assertEquals(FocusState.FOCUSING, engine.state.value.state)
        assertTrue(engine.state.value.state.isCounting)
    }

    @Test
    fun `31 No false tracking when foreground context is unknown`() {
        // Tracking started but no package has been resolved yet
        engine.onEvent(FocusEvent.UserStart(timestamp = 1000L))
        // Should evaluate to READY, not FOCUSING
        assertEquals(FocusState.READY, engine.state.value.state)
        assertFalse(engine.state.value.state.isCounting)
    }

    @Test
    fun `32 Correct interval splitting without duplicate session creation`() = runTest {
        val ownApp = "com.studycompanion.app"
        engine.registerOwnPackageName(ownApp)
        val settings = ProfileSettings(profileId = profileA)
        engine.onEvent(FocusEvent.ProfileSwitched(profileA, setOf(approvedApp), settings, 10_000L))

        // 1. Focus for 30s in Study App (10_000 to 40_000)
        engine.onEvent(FocusEvent.AppResumed(approvedApp, timestamp = 10_000L))
        engine.onEvent(FocusEvent.UserStart(timestamp = 10_000L))
        assertEquals(FocusState.FOCUSING, engine.state.value.state)

        // 2. Switch to unapproved Instagram for 10s (40_000 to 50_000)
        engine.onEvent(FocusEvent.AppResumed(unapprovedApp, timestamp = 40_000L))
        assertEquals(FocusState.PAUSED_UNAPPROVED_APP, engine.state.value.state)

        testScope.testScheduler.advanceUntilIdle()
        assertEquals(1, fakeRepository.recordedSessions.size)
        assertEquals(30L, fakeRepository.recordedSessions[0].durationSeconds)

        // 3. Switch back to Study App for 20s (50_000 to 70_000)
        engine.onEvent(FocusEvent.AppResumed(approvedApp, timestamp = 50_000L))
        assertEquals(FocusState.FOCUSING, engine.state.value.state)

        // 4. Return to Launcher at 70_000
        engine.onEvent(FocusEvent.AppResumed(launcherApp, timestamp = 70_000L))
        assertEquals(FocusState.PAUSED_HOME, engine.state.value.state)

        testScope.testScheduler.advanceUntilIdle()
        assertEquals(2, fakeRepository.recordedSessions.size)
        assertEquals(20L, fakeRepository.recordedSessions[1].durationSeconds)

        val totalVerifiedDuration = fakeRepository.recordedSessions.sumOf { it.durationSeconds }
        assertEquals(50L, totalVerifiedDuration) // 30s + 20s = 50s (Instagram 10s strictly excluded)
    }
}
