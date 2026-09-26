package com.studycompanion.app.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.studycompanion.app.core.database.AppDatabase
import com.studycompanion.app.core.database.entity.DailyTargetEntity
import com.studycompanion.app.core.database.entity.ProfileEntity
import com.studycompanion.app.core.database.entity.StudySessionEntity
import com.studycompanion.app.core.database.entity.SyncMutationEntity
import com.studycompanion.app.core.database.entity.UserEntity
import com.studycompanion.app.core.datastore.UserSessionDataStore
import com.studycompanion.app.core.network.ApiService
import com.studycompanion.app.core.network.AuthResponse
import com.studycompanion.app.core.network.PullResponse
import com.studycompanion.app.core.network.PushResponse
import com.studycompanion.app.core.network.RemoteChange
import com.studycompanion.app.data.remote.RemoteDataSource
import com.studycompanion.app.data.repository.AuthRepositoryImpl
import com.studycompanion.app.data.repository.SessionRepositoryImpl
import com.studycompanion.app.data.repository.StudyAppRepositoryImpl
import com.studycompanion.app.data.repository.TargetRepositoryImpl
import com.studycompanion.app.domain.model.DailyTarget
import com.studycompanion.app.domain.model.StudySession
import com.studycompanion.app.domain.model.TrackingType
import com.studycompanion.app.domain.model.VerificationStatus
import com.studycompanion.app.domain.repository.AuthState
import com.studycompanion.app.sync.conflict.ConflictResolution
import com.studycompanion.app.sync.conflict.ConflictResolver
import com.studycompanion.app.sync.model.SyncState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
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
import java.io.IOException
import java.util.UUID

/**
 * Fake implementation of ApiService for testing synchronization scenarios,
 * network failures, retries, and cursor progression.
 */
class FakeApiService : ApiService {
    var shouldFailWithNetworkError = false
    var shouldFailWithServerError = false

    val pushedPayloads = mutableListOf<JSONArray>()
    var nextPullResponse: PullResponse = PullResponse(nextCursor = 100L, serverTimestamp = 1000L, changes = emptyList())

    override suspend fun signup(email: String, password: String): Result<AuthResponse> {
        if (shouldFailWithNetworkError) return Result.failure(IOException("No network"))
        return Result.success(
            AuthResponse(
                userId = "remote-user-" + UUID.randomUUID().toString().take(6),
                email = email,
                accessToken = "remote-access-token",
                refreshToken = "remote-refresh-token",
                expiresAt = System.currentTimeMillis() + 3600_000L
            )
        )
    }

    override suspend fun login(email: String, password: String): Result<AuthResponse> {
        if (shouldFailWithNetworkError) return Result.failure(IOException("No network"))
        return Result.success(
            AuthResponse(
                userId = "remote-user-login",
                email = email,
                accessToken = "remote-access-token",
                refreshToken = "remote-refresh-token",
                expiresAt = System.currentTimeMillis() + 3600_000L
            )
        )
    }

    override suspend fun refreshToken(refreshToken: String): Result<AuthResponse> {
        return Result.success(
            AuthResponse(
                userId = "refreshed-user",
                email = "user@example.com",
                accessToken = "new-access-token",
                refreshToken = "new-refresh-token",
                expiresAt = System.currentTimeMillis() + 3600_000L
            )
        )
    }

    override suspend fun logout(accessToken: String): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun deleteAccount(accessToken: String): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun pushMutations(
        accessToken: String,
        deviceId: String,
        mutationsJson: JSONArray
    ): Result<PushResponse> {
        if (shouldFailWithNetworkError) return Result.failure(IOException("Network failure"))
        if (shouldFailWithServerError) return Result.failure(IOException("HTTP 500 Server Error"))

        pushedPayloads.add(mutationsJson)
        val ackIds = mutableListOf<String>()
        for (i in 0 until mutationsJson.length()) {
            val item = mutationsJson.getJSONObject(i)
            ackIds.add(item.getString("id"))
        }

        return Result.success(PushResponse(serverTimestamp = System.currentTimeMillis(), acknowledgedMutationIds = ackIds))
    }

    override suspend fun pullChanges(
        accessToken: String,
        cursor: Long
    ): Result<PullResponse> {
        if (shouldFailWithNetworkError) return Result.failure(IOException("Network failure"))
        return Result.success(nextPullResponse)
    }
}

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class SyncEngineTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var sessionDataStore: UserSessionDataStore
    private lateinit var fakeApi: FakeApiService
    private lateinit var remoteDataSource: RemoteDataSource
    private lateinit var syncEngine: SyncEngine

    private lateinit var sessionRepo: SessionRepositoryImpl
    private lateinit var targetRepo: TargetRepositoryImpl
    private lateinit var studyAppRepo: StudyAppRepositoryImpl
    private lateinit var authRepo: AuthRepositoryImpl

    private val testUserId = "user-123"
    private val testProfileId = "profile-456"
    private val testProfileBId = "profile-789"
    private val testDeviceId = "device-test"

    @Before
    fun setUp() = runTest {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        sessionDataStore = UserSessionDataStore(context)
        sessionDataStore.clearSession()
        sessionDataStore.saveSession(testUserId, "test-access-token", "test-refresh-token")
        sessionDataStore.saveSyncCursor(0L)

        fakeApi = FakeApiService()
        remoteDataSource = RemoteDataSource(fakeApi, sessionDataStore)
        syncEngine = SyncEngine(database, remoteDataSource, sessionDataStore, testDeviceId)

        sessionRepo = SessionRepositoryImpl(database.studySessionDao(), database.sessionEventDao(), database.syncMutationDao())
        targetRepo = TargetRepositoryImpl(database.dailyTargetDao(), database.syncMutationDao(), testDeviceId)
        studyAppRepo = StudyAppRepositoryImpl(database.studyAppDao(), context, database.syncMutationDao(), testDeviceId)
        authRepo = AuthRepositoryImpl(database.userDao(), sessionDataStore, remoteDataSource)

        // Seed basic User and Profile in Room
        val now = System.currentTimeMillis()
        database.userDao().insert(UserEntity(id = testUserId, email = "shashank@example.com", authProvider = "EMAIL", createdAt = now, updatedAt = now))
        database.profileDao().insert(ProfileEntity(id = testProfileId, userId = testUserId, name = "Primary", pinSalt = "salt", pinVerifier = "verifier", createdAt = now, updatedAt = now))
        database.profileDao().insert(ProfileEntity(id = testProfileBId, userId = testUserId, name = "Secondary", pinSalt = "salt", pinVerifier = "verifier", createdAt = now, updatedAt = now))
    }

    @After
    fun tearDown() = runTest {
        sessionDataStore.clearSession()
        sessionDataStore.saveSyncCursor(0L)
        database.close()
    }

    @Test
    fun `1 mutation creation queues pending mutation when session is recorded`() = runTest {
        val session = StudySession(
            id = "session-1",
            profileId = testProfileId,
            deviceId = testDeviceId,
            platform = "ANDROID",
            packageName = "com.study.math",
            startAt = 1000L,
            endAt = 5000L,
            durationSeconds = 4L,
            trackingType = TrackingType.AUTOMATIC,
            verificationStatus = VerificationStatus.VERIFIED_BY_RULES
        )

        sessionRepo.recordSession(session)

        val pending = database.syncMutationDao().getAllPendingMutations()
        assertEquals(1, pending.size)
        assertEquals("SESSION", pending[0].entityType)
        assertEquals("session-1", pending[0].recordId)
        assertEquals("PENDING", pending[0].syncState)
    }

    @Test
    fun `2 mutation deduplication compresses or retains latest state for entity`() = runTest {
        // Repeated adjustments to the same target update the entity
        targetRepo.setDailyTarget(testProfileId, "2026-09-23", 7200L)
        targetRepo.adjustDailyTarget(testProfileId, "2026-09-23", 10800L)

        val pending = database.syncMutationDao().getAllPendingMutations()
        assertTrue("Mutations for target were queued", pending.isNotEmpty())
        val lastMutation = pending.last()
        assertTrue(lastMutation.payloadJson.contains("10800"))
    }

    @Test
    fun `3 successful push uploads mutations and deletes them from local queue`() = runTest {
        sessionRepo.recordSession(
            StudySession(
                id = "session-push",
                profileId = testProfileId,
                deviceId = testDeviceId,
                platform = "ANDROID",
                startAt = 1000L,
                endAt = 6000L,
                durationSeconds = 5L
            )
        )

        assertEquals(1, database.syncMutationDao().getPendingCount())

        val result = syncEngine.pushLocalMutations()
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow())
        assertEquals("Queue should be empty after ACK", 0, database.syncMutationDao().getPendingCount())
    }

    @Test
    fun `4 failed push keeps mutations pending and increments attempt count`() = runTest {
        sessionRepo.recordSession(
            StudySession(
                id = "session-retry",
                profileId = testProfileId,
                deviceId = testDeviceId,
                platform = "ANDROID",
                startAt = 1000L,
                endAt = 6000L,
                durationSeconds = 5L
            )
        )

        fakeApi.shouldFailWithNetworkError = true

        val result = syncEngine.pushLocalMutations()
        assertTrue(result.isFailure)

        val pending = database.syncMutationDao().getAllPendingMutations()
        assertEquals(1, pending.size)
        assertEquals(1, pending[0].attemptCount)
        assertNotNull(pending[0].lastAttemptAt)
    }

    @Test
    fun `5 pull cursor progression saves new nextCursor to datastore`() = runTest {
        fakeApi.nextPullResponse = PullResponse(
            nextCursor = 142L,
            serverTimestamp = 5000L,
            changes = emptyList()
        )

        assertEquals(0L, sessionDataStore.syncCursorFlow.first())

        val result = syncEngine.pullRemoteChanges()
        assertTrue(result.isSuccess)
        assertEquals(142L, sessionDataStore.syncCursorFlow.first())
    }

    @Test
    fun `6 idempotent replay of duplicate remote session does not duplicate records`() = runTest {
        val payload = JSONObject().apply {
            put("id", "session-idempotent")
            put("profileId", testProfileId)
            put("deviceId", "pc-companion")
            put("startAt", 10_000L)
            put("endAt", 15_000L)
            put("durationSeconds", 5L)
        }

        val remoteChange = RemoteChange(
            cursorId = 1L,
            entityType = "SESSION",
            entityId = "session-idempotent",
            profileId = testProfileId,
            operation = "INSERT",
            payloadJson = payload.toString(),
            serverTimestamp = 20_000L
        )

        fakeApi.nextPullResponse = PullResponse(
            nextCursor = 2L,
            serverTimestamp = 20_000L,
            changes = listOf(remoteChange, remoteChange) // duplicate
        )

        syncEngine.pullRemoteChanges()

        val sessions = database.studySessionDao().getSessionsForProfile(testProfileId)
        assertEquals(1, sessions.size)
        assertEquals("session-idempotent", sessions[0].id)
    }

    @Test
    fun `7 conflict resolution preserves original target and protects manual session`() {
        // A: Daily target conflict resolution strictly preserves originalTargetSeconds
        val localTarget = DailyTargetEntity(
            id = "target-1",
            profileId = testProfileId,
            dateKey = "2026-09-23",
            originalTargetSeconds = 10800L,
            adjustedTargetSeconds = null,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val remoteTarget = DailyTargetEntity(
            id = "target-1",
            profileId = testProfileId,
            dateKey = "2026-09-23",
            originalTargetSeconds = 99999L, // Corrupt/tampered remote original
            adjustedTargetSeconds = 14400L,
            createdAt = 1000L,
            updatedAt = 2000L // Newer
        )

        val resolvedTarget = ConflictResolver.resolveTarget(localTarget, remoteTarget)
        assertTrue(resolvedTarget is ConflictResolution.ApplyRemote)
        val appliedTarget = (resolvedTarget as ConflictResolution.ApplyRemote).entity
        assertEquals("Original target must be preserved", 10800L, appliedTarget.originalTargetSeconds)
        assertEquals("Adjusted target updated via LWW", 14400L, appliedTarget.adjustedTargetSeconds)

        // B: Manual session cannot be overwritten by stale automatic data
        val manualSession = StudySessionEntity(
            id = "session-same-id",
            profileId = testProfileId,
            deviceId = testDeviceId,
            platform = "ANDROID",
            packageName = "com.study.math",
            startAt = 1000L,
            endAt = 5000L,
            durationSeconds = 4L,
            trackingType = "MANUAL",
            verificationStatus = "MANUAL_UNVERIFIED",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val staleAutoSession = manualSession.copy(
            trackingType = "AUTOMATIC",
            updatedAt = 2000L
        )
        val sessionResolution = ConflictResolver.resolveSession(manualSession, staleAutoSession)
        assertTrue("Manual session must not be overwritten by automatic", sessionResolution is ConflictResolution.KeepLocal)
    }

    @Test
    fun `8 offline queue accumulates multiple operations across sessions and targets`() = runTest {
        sessionRepo.recordSession(StudySession(id = "s1", profileId = testProfileId, deviceId = testDeviceId, platform = "ANDROID", startAt = 1, endAt = 2, durationSeconds = 1))
        sessionRepo.recordSession(StudySession(id = "s2", profileId = testProfileId, deviceId = testDeviceId, platform = "ANDROID", startAt = 3, endAt = 4, durationSeconds = 1))
        targetRepo.setDailyTarget(testProfileId, "2026-09-23", 3600L)
        studyAppRepo.addStudyApp(testProfileId, "com.study.physics", "Physics")

        val pending = database.syncMutationDao().getAllPendingMutations()
        assertEquals(4, pending.size)
    }

    @Test
    fun `9 online recovery successfully flushes offline queue and sets Synced state`() = runTest {
        sessionRepo.recordSession(StudySession(id = "s-recover", profileId = testProfileId, deviceId = testDeviceId, platform = "ANDROID", startAt = 1, endAt = 2, durationSeconds = 1))
        assertEquals(1, database.syncMutationDao().getPendingCount())

        val result = syncEngine.sync()
        assertTrue(result.isSuccess)
        assertEquals(0, database.syncMutationDao().getPendingCount())
        assertTrue("Engine must report Synced state", syncEngine.syncState.value is SyncState.Synced)
    }

    @Test
    fun `10 profile isolation ensures mutations and changes are tagged with profileId`() = runTest {
        sessionRepo.recordSession(StudySession(id = "s-pA", profileId = testProfileId, deviceId = testDeviceId, platform = "ANDROID", startAt = 1, endAt = 2, durationSeconds = 1))
        sessionRepo.recordSession(StudySession(id = "s-pB", profileId = testProfileBId, deviceId = testDeviceId, platform = "ANDROID", startAt = 3, endAt = 4, durationSeconds = 1))

        val mutationsForA = database.syncMutationDao().getPendingMutationsForProfile(testProfileId)
        val mutationsForB = database.syncMutationDao().getPendingMutationsForProfile(testProfileBId)

        assertEquals(1, mutationsForA.size)
        assertEquals(1, mutationsForB.size)
        assertEquals("s-pA", mutationsForA[0].recordId)
        assertEquals("s-pB", mutationsForB[0].recordId)
    }

    @Test
    fun `11 account isolation ensures tokens and user ids are strictly maintained`() = runTest {
        val currentUserId = sessionDataStore.userIdFlow.first()
        val currentToken = sessionDataStore.sessionTokenFlow.first()
        assertEquals(testUserId, currentUserId)
        assertEquals("test-access-token", currentToken)
    }

    @Test
    fun `12 auth state restoration restores session without requiring re-login`() = runTest {
        val authState = authRepo.authState.first()
        assertTrue("Auth state should be restored as Authenticated", authState is AuthState.Authenticated)
        val user = (authState as AuthState.Authenticated).user
        assertEquals("shashank@example.com", user.email)
    }
}
