package com.studycompanion.app.ui

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.studycompanion.app.core.database.AppDatabase
import com.studycompanion.app.core.datastore.UserSessionDataStore
import com.studycompanion.app.core.network.ApiService
import com.studycompanion.app.core.network.AuthResponse
import com.studycompanion.app.core.network.PullResponse
import com.studycompanion.app.core.network.PushResponse
import com.studycompanion.app.core.ui.component.FocusStateFormatter
import com.studycompanion.app.core.ui.theme.AppThemeMode
import com.studycompanion.app.data.remote.RemoteDataSource
import com.studycompanion.app.data.repository.AuthRepositoryImpl
import com.studycompanion.app.data.repository.ProfileRepositoryImpl
import com.studycompanion.app.data.repository.SessionRepositoryImpl
import com.studycompanion.app.data.repository.StatsRepositoryImpl
import com.studycompanion.app.data.repository.StudyAppRepositoryImpl
import com.studycompanion.app.data.repository.TargetRepositoryImpl
import com.studycompanion.app.domain.model.StudySession
import com.studycompanion.app.domain.model.ThemeMode
import com.studycompanion.app.domain.model.TrackingType
import com.studycompanion.app.domain.model.VerificationStatus
import com.studycompanion.app.feature.focus.FocusDisplayMode
import com.studycompanion.app.feature.focus.FocusViewModel
import com.studycompanion.app.feature.history.HistoryViewModel
import com.studycompanion.app.feature.home.HomeViewModel
import com.studycompanion.app.feature.settings.SettingsViewModel
import com.studycompanion.app.feature.statistics.StatisticsViewModel
import com.studycompanion.app.feature.studyapps.StudyAppManagerViewModel
import com.studycompanion.app.sync.SyncEngine
import com.studycompanion.app.tracking.engine.FocusEngine
import com.studycompanion.app.tracking.engine.FocusState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.json.JSONArray
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class FakeUiApiService : ApiService {
    override suspend fun signup(email: String, password: String): Result<AuthResponse> =
        Result.success(AuthResponse("u-1", email, "at", "rt", System.currentTimeMillis() + 3600_000L))
    override suspend fun login(email: String, password: String): Result<AuthResponse> =
        Result.success(AuthResponse("u-1", email, "at", "rt", System.currentTimeMillis() + 3600_000L))
    override suspend fun refreshToken(refreshToken: String): Result<AuthResponse> =
        Result.success(AuthResponse("u-1", "test@test.com", "at", "rt", System.currentTimeMillis() + 3600_000L))
    override suspend fun logout(accessToken: String): Result<Unit> = Result.success(Unit)
    override suspend fun pushMutations(accessToken: String, deviceId: String, mutationsJson: JSONArray): Result<PushResponse> =
        Result.success(PushResponse(serverTimestamp = System.currentTimeMillis(), acknowledgedMutationIds = emptyList()))
    override suspend fun pullChanges(accessToken: String, cursor: Long): Result<PullResponse> =
        Result.success(PullResponse(nextCursor = cursor, serverTimestamp = System.currentTimeMillis(), changes = emptyList()))
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Phase4UiTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var sessionDataStore: UserSessionDataStore
    private lateinit var authRepo: AuthRepositoryImpl
    private lateinit var profileRepo: ProfileRepositoryImpl
    private lateinit var targetRepo: TargetRepositoryImpl
    private lateinit var studyAppRepo: StudyAppRepositoryImpl
    private lateinit var sessionRepo: SessionRepositoryImpl
    private lateinit var statsRepo: StatsRepositoryImpl
    private lateinit var focusEngine: FocusEngine
    private lateinit var syncEngine: SyncEngine

    private lateinit var viewModelStore: androidx.lifecycle.ViewModelStore
    private var profileId = ""

    @Before
    fun setUp() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)
        viewModelStore = androidx.lifecycle.ViewModelStore()
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor(testDispatcher.asExecutor())
            .setTransactionExecutor(testDispatcher.asExecutor())
            .build()
        sessionDataStore = UserSessionDataStore(context)
        sessionDataStore.clearSession()
        sessionDataStore.clearActiveProfile()
        advanceUntilIdle()

        val fakeApi = FakeUiApiService()
        val remoteDataSource = RemoteDataSource(fakeApi, sessionDataStore)
        authRepo = AuthRepositoryImpl(db.userDao(), sessionDataStore, remoteDataSource)
        profileRepo = ProfileRepositoryImpl(db.profileDao(), db.profileSettingsDao(), sessionDataStore)
        targetRepo = TargetRepositoryImpl(db.dailyTargetDao(), db.syncMutationDao(), "test-device")
        studyAppRepo = StudyAppRepositoryImpl(db.studyAppDao(), context, db.syncMutationDao(), "test-device")
        sessionRepo = SessionRepositoryImpl(db.studySessionDao(), db.sessionEventDao(), db.syncMutationDao())
        statsRepo = StatsRepositoryImpl(db.studySessionDao(), db.dailyTargetDao(), db.studyAppDao())
        focusEngine = FocusEngine(sessionRepo)
        syncEngine = SyncEngine(db, remoteDataSource, sessionDataStore, "test-device")

        val user = authRepo.signUp("test@studycompanion.app", "Password123!").getOrThrow()
        val profile = profileRepo.createProfile(user.id, "Scholar", "1234").getOrThrow()
        profileId = profile.id
        profileRepo.setActiveProfileDirectly(profileId)
        advanceUntilIdle()
    }

    @After
    fun tearDown() {
        viewModelStore.clear()
        testDispatcher.scheduler.advanceUntilIdle()
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun testFocusStateFriendlyFormatting() {
        assertEquals("Ready to Study", FocusStateFormatter.formatState(FocusState.IDLE))
        assertEquals("Ready to Study", FocusStateFormatter.formatState(FocusState.READY))
        assertEquals("Studying", FocusStateFormatter.formatState(FocusState.FOCUSING))
        assertEquals("Studying · Screen Locked", FocusStateFormatter.formatState(FocusState.LOCKED_FOCUS))
        assertEquals("Paused", FocusStateFormatter.formatState(FocusState.PAUSED_UNAPPROVED_APP))
        assertEquals("Paused", FocusStateFormatter.formatState(FocusState.PAUSED_HOME))
        assertEquals("Paused", FocusStateFormatter.formatState(FocusState.PAUSED_USER))
        assertEquals("Paused · Call", FocusStateFormatter.formatState(FocusState.PAUSED_CALL))
        assertEquals("Paused · Multi-Window", FocusStateFormatter.formatState(FocusState.PAUSED_MULTIWINDOW))
        assertEquals("Paused · Floating", FocusStateFormatter.formatState(FocusState.PAUSED_FLOATING))
        assertEquals("Setup Required", FocusStateFormatter.formatState(FocusState.WAITING_FOR_PERMISSION))
        assertEquals("Restoring Session", FocusStateFormatter.formatState(FocusState.RECOVERING))
        assertEquals("Needs Attention", FocusStateFormatter.formatState(FocusState.ERROR))
    }

    @Test
    fun testHomeViewModelTargetAndSnapshot() = runTest(testDispatcher) {
        val vm = HomeViewModel(
            authRepository = authRepo,
            profileRepository = profileRepo,
            targetRepository = targetRepo,
            studyAppRepository = studyAppRepo,
            statsRepository = statsRepo,
            focusEngine = focusEngine,
            syncEngine = syncEngine
        ).also { viewModelStore.put("home", it) }
        advanceUntilIdle()

        assertEquals("Scholar", vm.uiState.value.activeProfile?.name)
        assertEquals(FocusState.IDLE, vm.uiState.value.focusSnapshot.state)

        vm.setTarget(7200L)
        advanceUntilIdle()

        val todayKey = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val savedTarget = targetRepo.getTarget(profileId, todayKey).first()
        assertNotNull(savedTarget)
        assertEquals(7200L, savedTarget?.originalTargetSeconds)
    }

    @Test
    fun testFocusViewModelDisplayModeAndEngineEvents() = runTest(testDispatcher) {
        val vm = FocusViewModel(
            focusEngine = focusEngine,
            profileRepository = profileRepo,
            targetRepository = targetRepo,
            studyAppRepository = studyAppRepo
        ).also { viewModelStore.put("focus", it) }
        advanceUntilIdle()

        assertEquals(FocusDisplayMode.MINIMAL_AMOLED, vm.uiState.value.displayMode)

        vm.toggleDisplayMode()
        assertEquals(FocusDisplayMode.CLOCK, vm.uiState.value.displayMode)

        vm.toggleDisplayMode()
        assertEquals(FocusDisplayMode.MINIMAL_AMOLED, vm.uiState.value.displayMode)

        vm.pauseFocus()
        advanceUntilIdle()
        assertEquals(FocusState.PAUSED_USER, focusEngine.state.value.state)

        vm.resumeFocus()
        advanceUntilIdle()
        assertEquals(FocusState.READY, focusEngine.state.value.state)
    }

    @Test
    fun testHistoryViewModelNavigationAndManualSession() = runTest(testDispatcher) {
        val vm = HistoryViewModel(
            sessionRepository = sessionRepo,
            profileRepository = profileRepo,
            statsRepository = statsRepo,
            studyAppRepository = studyAppRepo
        ).also { viewModelStore.put("history", it) }
        advanceUntilIdle()

        val today = LocalDate.now()
        assertEquals(today, vm.uiState.value.selectedDate)

        vm.previousDay()
        assertEquals(today.minusDays(1), vm.uiState.value.selectedDate)

        vm.nextDay()
        assertEquals(today, vm.uiState.value.selectedDate)

        val now = System.currentTimeMillis()
        val startAt = now - 1800_000L
        vm.addManualSession(startAt, now, "Mathematics")
        advanceUntilIdle()

        val sessions = sessionRepo.getSessions(profileId).first()
        assertEquals(1, sessions.size)
        val session = sessions.first()
        assertEquals(TrackingType.MANUAL, session.trackingType)
        assertEquals(VerificationStatus.MANUAL_UNVERIFIED, session.verificationStatus)
        assertEquals(1800L, session.durationSeconds)

        vm.openSessionDetail(session)
        assertTrue(vm.uiState.value.showSessionDetailDialog)
        assertEquals(session.id, vm.uiState.value.selectedSession?.id)

        vm.closeSessionDetail()
        assertFalse(vm.uiState.value.showSessionDetailDialog)

        vm.deleteSession(session.id)
        advanceUntilIdle()
        val remaining = sessionRepo.getSessions(profileId).first()
        assertTrue(remaining.isEmpty())
    }

    @Test
    fun testStatsRepositoryCalculations() = runTest(testDispatcher) {
        val now = java.time.LocalDate.now().atTime(12, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val s1 = StudySession(
            id = UUID.randomUUID().toString(),
            profileId = profileId,
            deviceId = "test-device",
            startAt = now - 3600_000L,
            endAt = now - 1800_000L,
            durationSeconds = 1800L,
            packageName = "com.study.math",
            trackingType = TrackingType.AUTOMATIC,
            verificationStatus = VerificationStatus.VERIFIED_BY_RULES
        )
        val s2 = StudySession(
            id = UUID.randomUUID().toString(),
            profileId = profileId,
            deviceId = "test-device",
            startAt = now - 1800_000L,
            endAt = now,
            durationSeconds = 1800L,
            packageName = "com.study.physics",
            trackingType = TrackingType.AUTOMATIC,
            verificationStatus = VerificationStatus.VERIFIED_BY_RULES
        )
        sessionRepo.recordSession(s1)
        sessionRepo.recordSession(s2)

        val todayKey = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        targetRepo.setDailyTarget(profileId, todayKey, 3600L)
        advanceUntilIdle()

        val overview = statsRepo.observeStatsOverview(profileId, todayKey).first()
        assertEquals(3600L, overview.todayTotalSeconds)
        assertEquals(3600L, overview.todayAutomaticSeconds)
        assertEquals(0L, overview.todayManualSeconds)
        assertEquals(2, overview.todaySessionCount)
        assertEquals(1800L, overview.todayLongestSessionSeconds)
        assertEquals(1, overview.currentStreakDays)
    }

    @Test
    fun testCalendarDaySummariesAndNavigation() = runTest(testDispatcher) {
        val now = java.time.LocalDate.now().atTime(10, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val s = StudySession(
            id = UUID.randomUUID().toString(),
            profileId = profileId,
            deviceId = "test-device",
            startAt = now - 1800_000L,
            endAt = now,
            durationSeconds = 1800L,
            packageName = "com.study.math",
            trackingType = TrackingType.AUTOMATIC,
            verificationStatus = VerificationStatus.VERIFIED_BY_RULES
        )
        sessionRepo.recordSession(s)
        advanceUntilIdle()

        val allSummaries = statsRepo.observeAllDaySummaries(profileId).first()
        val todayKey = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        assertTrue(allSummaries.containsKey(todayKey))
        assertEquals(1800L, allSummaries[todayKey]?.totalSeconds)
        assertEquals(1, allSummaries[todayKey]?.sessionCount)

        val vm = StatisticsViewModel(
            profileRepository = profileRepo,
            statsRepository = statsRepo
        ).also { viewModelStore.put("stats", it) }
        advanceUntilIdle()

        val currentMonth = vm.uiState.value.currentMonth
        vm.previousMonth()
        assertEquals(currentMonth.minusMonths(1), vm.uiState.value.currentMonth)
        vm.nextMonth()
        assertEquals(currentMonth, vm.uiState.value.currentMonth)

        vm.selectDate("2026-09-15")
        assertEquals("2026-09-15", vm.uiState.value.selectedDateKey)
    }

    @Test
    fun testStudyAppManagerViewModelOperations() = runTest(testDispatcher) {
        val vm = StudyAppManagerViewModel(
            profileRepository = profileRepo,
            studyAppRepository = studyAppRepo
        ).also { viewModelStore.put("studyApps", it) }
        advanceUntilIdle()

        vm.addStudyApp("com.example.reading", "E-Reader")
        advanceUntilIdle()

        val approved = studyAppRepo.getStudyApps(profileId).first()
        assertEquals(1, approved.size)
        assertEquals("E-Reader", approved.first().customLabel)
        assertTrue(approved.first().isEnabled)

        vm.setAppEnabled("com.example.reading", false)
        advanceUntilIdle()
        val disabled = studyAppRepo.getStudyApps(profileId).first()
        assertFalse(disabled.first().isEnabled)

        vm.saveAppLabel("com.example.reading", "Novel Reader")
        advanceUntilIdle()
        val edited = studyAppRepo.getStudyApps(profileId).first()
        assertEquals("Novel Reader", edited.first().customLabel)

        vm.removeStudyApp("com.example.reading")
        advanceUntilIdle()
        val emptyList = studyAppRepo.getStudyApps(profileId).first()
        assertTrue(emptyList.isEmpty())
    }

    @Test
    fun testSettingsViewModelThemeAndRuleUpdates() = runTest(testDispatcher) {
        val vm = SettingsViewModel(
            authRepository = authRepo,
            profileRepository = profileRepo,
            targetRepository = targetRepo,
            sessionDataStore = sessionDataStore,
            syncEngine = syncEngine
        ).also { viewModelStore.put("settings", it) }
        advanceUntilIdle()

        vm.setThemeMode(AppThemeMode.AMOLED)
        advanceUntilIdle()
        val settings = profileRepo.getProfileSettings(profileId).first()
        assertEquals(ThemeMode.AMOLED, settings.themeMode)

        vm.setCountWhileLocked(true)
        vm.setPauseDuringCalls(false)
        vm.setPauseInMultiWindow(true)
        advanceUntilIdle()

        val updated = profileRepo.getProfileSettings(profileId).first()
        assertTrue(updated.countWhileLocked)
        assertFalse(updated.pauseDuringCalls)
        assertTrue(updated.pauseInMultiWindow)
    }
}
