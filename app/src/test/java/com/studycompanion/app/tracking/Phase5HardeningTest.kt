package com.studycompanion.app.tracking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.studycompanion.app.core.datastore.UserSessionDataStore
import com.studycompanion.app.core.platform.BatteryOptimizationHelper
import com.studycompanion.app.domain.model.ProfileSettings
import com.studycompanion.app.domain.model.SessionEvent
import com.studycompanion.app.domain.model.StudySession
import com.studycompanion.app.domain.repository.SessionRepository
import com.studycompanion.app.tracking.engine.FocusEngine
import com.studycompanion.app.tracking.engine.FocusEvent
import com.studycompanion.app.tracking.engine.FocusState
import com.studycompanion.app.tracking.recovery.ProcessDeathRecoveryHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

class Phase5FakeSessionRepository : SessionRepository {
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
        if (idx >= 0) recordedSessions[idx] = session else recordedSessions.add(session)
        return Result.success(Unit)
    }
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class Phase5HardeningTest {

    private lateinit var context: Context
    private lateinit var sessionRepository: Phase5FakeSessionRepository
    private lateinit var dataStore: UserSessionDataStore
    private lateinit var focusEngine: FocusEngine
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope

    private val profileId = "profile-test-phase5"
    private val approvedPackage = "com.study.app"

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        sessionRepository = Phase5FakeSessionRepository()
        dataStore = UserSessionDataStore(context)
        focusEngine = FocusEngine(
            sessionRepository = sessionRepository,
            scope = testScope,
            userSessionDataStore = null
        )

        // Initialize engine with test profile
        focusEngine.onEvent(
            FocusEvent.ProfileSwitched(
                profileId = profileId,
                approvedPackages = setOf(approvedPackage),
                settings = ProfileSettings(profileId = profileId),
                timestamp = 1000L
            )
        )
        focusEngine.onEvent(FocusEvent.PermissionChanged(hasUsageAccess = true, timestamp = 1000L))
        focusEngine.onEvent(FocusEvent.UserStart(timestamp = 1000L))
    }

    @After
    fun tearDown() = runBlocking {
        testScope.advanceUntilIdle()
        dataStore.setActiveSessionMarker(null)
    }

    @Test
    fun testProcessDeathRecoveryReconcilesDanglingSession() = runBlocking {
        val startWall = 100_000L
        val lastHeartbeat = 160_000L // 60 seconds of verified studying before crash
        val sessionId = UUID.randomUUID().toString()

        // 1. Simulate active session marker left in DataStore before process died
        val markerJson = JSONObject().apply {
            put("sessionId", sessionId)
            put("profileId", profileId)
            put("packageName", approvedPackage)
            put("startWall", startWall)
            put("lastHeartbeat", lastHeartbeat)
        }.toString()
        dataStore.setActiveSessionMarker(markerJson)

        // 2. Execute process death recovery
        val recoveryHandler = ProcessDeathRecoveryHandler(
            sessionRepository = sessionRepository,
            userSessionDataStore = dataStore,
            focusEngine = focusEngine,
            usageStatsWatcher = null
        )

        recoveryHandler.reconcile(
            profileId = profileId,
            approvedPackages = setOf(approvedPackage),
            settings = ProfileSettings(profileId = profileId)
        )

        // 3. Verify dangling session was reconciled safely without fabricating time
        assertEquals(1, sessionRepository.recordedSessions.size)
        val session = sessionRepository.recordedSessions[0]
        assertEquals(sessionId, session.id)
        assertEquals(startWall, session.startAt)
        assertEquals(lastHeartbeat, session.endAt)
        assertEquals(60L, session.durationSeconds) // Exactly 60s, zero downtime gap added!

        // 4. Verify RECONCILED_AFTER_TERMINATION event was logged
        val recoveryEvent = sessionRepository.recordedEvents.find { it.eventType == "RECONCILED_AFTER_TERMINATION" }
        assertNotNull(recoveryEvent)
        assertEquals(lastHeartbeat, recoveryEvent!!.timestamp)

        // 5. Verify active session marker was cleared
        assertNull(dataStore.getActiveSessionMarker())
    }

    @Test
    fun testBootRecoveryReconcilesDanglingSession() = runBlocking {
        val startWall = 50_000L
        val lastHeartbeat = 80_000L // 30 seconds before device rebooted
        val sessionId = UUID.randomUUID().toString()

        val markerJson = JSONObject().apply {
            put("sessionId", sessionId)
            put("profileId", profileId)
            put("packageName", approvedPackage)
            put("startWall", startWall)
            put("lastHeartbeat", lastHeartbeat)
        }.toString()
        dataStore.setActiveSessionMarker(markerJson)
        dataStore.setTrackingActive(true)

        val recoveryHandler = ProcessDeathRecoveryHandler(
            sessionRepository = sessionRepository,
            userSessionDataStore = dataStore,
            focusEngine = focusEngine,
            usageStatsWatcher = null
        )

        recoveryHandler.reconcile(
            profileId = profileId,
            approvedPackages = setOf(approvedPackage),
            settings = ProfileSettings(profileId = profileId)
        )

        // Verify session was closed at last trustworthy heartbeat before reboot
        val session = sessionRepository.recordedSessions.find { it.id == sessionId }
        assertNotNull(session)
        assertEquals(30L, session!!.durationSeconds)
        assertEquals(lastHeartbeat, session.endAt)

        // Verify marker cleared
        assertNull(dataStore.getActiveSessionMarker())
    }

    @Test
    fun testClockMovesBackwardEnforcesNonNegativeDuration() = runTest(testDispatcher) {
        val t0 = 200_000L
        // Start studying
        focusEngine.onEvent(FocusEvent.AppResumed(approvedPackage, timestamp = t0))
        assertEquals(FocusState.FOCUSING, focusEngine.state.value.state)

        // Clock moves backward by 2 hours (user or network rolled clock back)
        val oldTime = t0 + 60_000L
        val newTime = t0 - 7_200_000L // 2 hours behind start time!

        focusEngine.onEvent(FocusEvent.ClockChanged(oldTimestamp = oldTime, newTimestamp = newTime))
        advanceUntilIdle()

        // Verify that duration recorded is strictly >= 0 (never negative)
        assertTrue(sessionRepository.recordedSessions.isNotEmpty())
        val session = sessionRepository.recordedSessions[0]
        assertTrue("Duration must be non-negative", session.durationSeconds >= 0)

        // Verify CLOCK_CHANGED event recorded with BACKWARD metadata
        val clockEvent = sessionRepository.recordedEvents.find { it.eventType == "CLOCK_CHANGED" }
        assertNotNull(clockEvent)
        assertTrue(clockEvent!!.metadataJson!!.contains("BACKWARD"))

        // Verify new interval starts under new wall timestamp
        assertEquals(FocusState.FOCUSING, focusEngine.state.value.state)
        assertEquals(newTime, focusEngine.state.value.sessionStartWallTime)
    }

    @Test
    fun testClockMovesForwardDoesNotFabricateTime() = runTest(testDispatcher) {
        val t0 = 300_000L
        // Start studying
        focusEngine.onEvent(FocusEvent.AppResumed(approvedPackage, timestamp = t0))
        assertEquals(FocusState.FOCUSING, focusEngine.state.value.state)

        // Clock jumps forward by 24 hours (e.g. user manually changed date ahead)
        val oldTime = t0 + 60_000L
        val newTime = t0 + 86_400_000L

        focusEngine.onEvent(FocusEvent.ClockChanged(oldTimestamp = oldTime, newTimestamp = newTime))
        advanceUntilIdle()

        // Verify that session duration does NOT award the 24-hour wall jump
        assertTrue(sessionRepository.recordedSessions.isNotEmpty())
        val session = sessionRepository.recordedSessions[0]
        assertTrue("Duration must not award 24 hours of fake time", session.durationSeconds < 86_400L)

        // Verify CLOCK_CHANGED event recorded with FORWARD metadata
        val clockEvent = sessionRepository.recordedEvents.find { it.eventType == "CLOCK_CHANGED" }
        assertNotNull(clockEvent)
        assertTrue(clockEvent!!.metadataJson!!.contains("FORWARD"))
    }

    @Test
    fun testTimezoneChangeRecordsEventAndPreservesInstants() = runTest(testDispatcher) {
        val t0 = 400_000L
        focusEngine.onEvent(FocusEvent.AppResumed(approvedPackage, timestamp = t0))

        // Timezone changed from UTC to Asia/Kolkata
        focusEngine.onEvent(
            FocusEvent.TimezoneChanged(
                oldTimezoneId = "UTC",
                newTimezoneId = "Asia/Kolkata",
                timestamp = t0 + 30_000L
            )
        )
        advanceUntilIdle()

        // Verify TIMEZONE_CHANGED event recorded
        val tzEvent = sessionRepository.recordedEvents.find { it.eventType == "TIMEZONE_CHANGED" }
        assertNotNull(tzEvent)
        assertTrue(tzEvent!!.metadataJson!!.contains("Asia/Kolkata"))

        // Active session start wall time remains unmodified UTC instant
        assertEquals(t0, focusEngine.state.value.sessionStartWallTime)
        assertEquals(FocusState.FOCUSING, focusEngine.state.value.state)
    }

    @Test
    fun testMidnightRolloverSplitsDayIntervals() = runTest(testDispatcher) {
        val midnight = 1_700_000_000_000L
        val startDay1 = midnight - 30_000L // 23:59:30

        // Start studying 30s before midnight
        focusEngine.onEvent(FocusEvent.AppResumed(approvedPackage, timestamp = startDay1))
        assertEquals(FocusState.FOCUSING, focusEngine.state.value.state)

        // Midnight arrives
        focusEngine.onEvent(
            FocusEvent.MidnightRolledOver(
                previousDayBoundary = midnight,
                newDayBoundary = midnight,
                timestamp = midnight
            )
        )
        advanceUntilIdle()

        // 1. Day 1 portion must be closed at midnight with duration 30 seconds
        assertEquals(1, sessionRepository.recordedSessions.size)
        val day1Session = sessionRepository.recordedSessions[0]
        assertEquals(startDay1, day1Session.startAt)
        assertEquals(midnight, day1Session.endAt)
        assertEquals(30L, day1Session.durationSeconds)

        // 2. MIDNIGHT_ROLLOVER event recorded
        val rolloverEvent = sessionRepository.recordedEvents.find { it.eventType == "MIDNIGHT_ROLLOVER" }
        assertNotNull(rolloverEvent)

        // 3. Day 2 session started at midnight seamlessly
        assertEquals(FocusState.FOCUSING, focusEngine.state.value.state)
        assertEquals(midnight, focusEngine.state.value.sessionStartWallTime)
    }

    @Test
    fun testUsageAccessRevocationTransitionsToWaiting() = runTest(testDispatcher) {
        val t0 = 500_000L
        focusEngine.onEvent(FocusEvent.AppResumed(approvedPackage, timestamp = t0))
        assertEquals(FocusState.FOCUSING, focusEngine.state.value.state)

        // User revokes Usage Access in Android Settings
        focusEngine.onEvent(FocusEvent.PermissionChanged(hasUsageAccess = false, timestamp = t0 + 10_000L))
        advanceUntilIdle()

        // FocusEngine must immediately transition to WAITING_FOR_PERMISSION
        assertEquals(FocusState.WAITING_FOR_PERMISSION, focusEngine.state.value.state)
        assertFalse(focusEngine.state.value.state.isCounting)

        // Previous session was safely closed
        assertEquals(1, sessionRepository.recordedSessions.size)

        // User re-grants Usage Access in Android Settings
        focusEngine.onEvent(FocusEvent.PermissionChanged(hasUsageAccess = true, timestamp = t0 + 60_000L))
        advanceUntilIdle()

        // Since approved app is still in foreground, tracking resumes with fresh session
        assertEquals(FocusState.FOCUSING, focusEngine.state.value.state)
        assertEquals(t0 + 60_000L, focusEngine.state.value.sessionStartWallTime)
    }

    @Test
    fun testBatteryOptimizationHelperOemDetection() {
        val oem = BatteryOptimizationHelper.detectOem()
        assertNotNull(oem)

        val diagnostic = BatteryOptimizationHelper.getDiagnostic(context)
        assertNotNull(diagnostic)
        assertNotNull(diagnostic.title)
        assertNotNull(diagnostic.explanation)
        assertNotNull(diagnostic.recommendation)
        assertNotNull(diagnostic.settingsIntent)
        assertTrue(diagnostic.explanation.contains("battery manager"))
    }

    @Test
    fun testDiagnosticBundleGenerator() {
        val bundle = com.studycompanion.app.core.diagnostics.DiagnosticBundleGenerator.generate(context)
        assertNotNull(bundle)
        assertNotNull(bundle.deviceModel)
        assertNotNull(bundle.oemType)
        assertNotNull(bundle.timezoneId)
        assertTrue(bundle.jsonString.contains("androidApi"))
        assertTrue(bundle.jsonString.contains("oemType"))
    }
}

