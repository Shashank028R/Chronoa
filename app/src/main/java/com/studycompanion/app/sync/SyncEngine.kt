package com.studycompanion.app.sync

import com.studycompanion.app.core.database.AppDatabase
import com.studycompanion.app.core.database.entity.DailyTargetEntity
import com.studycompanion.app.core.database.entity.ProfileEntity
import com.studycompanion.app.core.database.entity.ProfileSettingsEntity
import com.studycompanion.app.core.database.entity.StudyAppEntity
import com.studycompanion.app.core.database.entity.StudySessionEntity
import com.studycompanion.app.core.datastore.UserSessionDataStore
import com.studycompanion.app.data.remote.RemoteDataSource
import com.studycompanion.app.sync.conflict.ConflictResolution
import com.studycompanion.app.sync.conflict.ConflictResolver
import com.studycompanion.app.sync.model.SyncMutation
import com.studycompanion.app.sync.model.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * Authoritative Synchronization Engine.
 * Manages two-way sync between Room database and the remote backend,
 * ensuring offline autonomy, idempotent uploads, and deterministic conflict resolution.
 */
class SyncEngine(
    private val database: AppDatabase,
    private val remoteDataSource: RemoteDataSource,
    private val sessionDataStore: UserSessionDataStore,
    private val deviceId: String = "local-device"
) {

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Synced(0L))
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    /**
     * Executes a full synchronization cycle: push pending local mutations, then pull remote changes.
     * Guaranteed to never throw uncaught exceptions or block the Focus Engine.
     */
    suspend fun sync(): Result<Unit> {
        val pendingCount = database.syncMutationDao().getPendingCount()
        _syncState.value = SyncState.Syncing(pendingCount)

        // 1. Push pending local mutations
        val pushResult = pushLocalMutations()
        if (pushResult.isFailure) {
            val ex = pushResult.exceptionOrNull()
            val isOffline = ex is IOException
            val state = if (isOffline) {
                SyncState.Offline(database.syncMutationDao().getPendingCount())
            } else {
                SyncState.Error(ex?.message ?: "Push error", isRetryable = true)
            }
            _syncState.value = state
            return Result.failure(ex ?: Exception("Push failed"))
        }

        // 2. Pull remote changes
        val pullResult = pullRemoteChanges()
        if (pullResult.isFailure) {
            val ex = pullResult.exceptionOrNull()
            val isOffline = ex is IOException
            val state = if (isOffline) {
                SyncState.Offline(database.syncMutationDao().getPendingCount())
            } else {
                SyncState.Error(ex?.message ?: "Pull error", isRetryable = true)
            }
            _syncState.value = state
            return Result.failure(ex ?: Exception("Pull failed"))
        }

        _syncState.value = SyncState.Synced(System.currentTimeMillis())
        return Result.success(Unit)
    }

    /**
     * Uploads pending local mutations to the remote backend.
     */
    suspend fun pushLocalMutations(): Result<Int> {
        val pendingEntities = database.syncMutationDao().getAllPendingMutations()
        if (pendingEntities.isEmpty()) {
            return Result.success(0)
        }

        val mutationsJson = JSONArray()
        for (entity in pendingEntities) {
            val mutation = SyncMutation.fromEntity(entity)
            mutationsJson.put(mutation.toJson())
        }

        val pushResult = remoteDataSource.pushMutations(deviceId, mutationsJson)
        return if (pushResult.isSuccess) {
            val response = pushResult.getOrThrow()
            if (response.acknowledgedMutationIds.isNotEmpty()) {
                database.syncMutationDao().deleteBatch(response.acknowledgedMutationIds)
            }
            Result.success(response.acknowledgedMutationIds.size)
        } else {
            // Update attempt count for retry tracking
            val now = System.currentTimeMillis()
            for (entity in pendingEntities) {
                database.syncMutationDao().update(
                    entity.copy(
                        attemptCount = entity.attemptCount + 1,
                        lastAttemptAt = now,
                        errorCode = pushResult.exceptionOrNull()?.message
                    )
                )
            }
            Result.failure(pushResult.exceptionOrNull() ?: Exception("Push failed"))
        }
    }

    /**
     * Pulls remote changes starting from the last stored cursor and merges them into Room.
     */
    suspend fun pullRemoteChanges(): Result<Int> {
        val currentCursor = sessionDataStore.syncCursorFlow.first()
        val pullResult = remoteDataSource.pullChanges(currentCursor)

        return if (pullResult.isSuccess) {
            val response = pullResult.getOrThrow()
            if (response.changes.isNotEmpty()) {
                applyRemoteChanges(response.changes)
            }
            sessionDataStore.saveSyncCursor(response.nextCursor)
            Result.success(response.changes.size)
        } else {
            Result.failure(pullResult.exceptionOrNull() ?: Exception("Pull failed"))
        }
    }

    private suspend fun applyRemoteChanges(changes: List<com.studycompanion.app.core.network.RemoteChange>) {
        for (change in changes) {
            try {
                when (change.entityType) {
                    "SESSION" -> applySessionChange(change)
                    "TARGET" -> applyTargetChange(change)
                    "STUDY_APP" -> applyStudyAppChange(change)
                    "SETTINGS" -> applySettingsChange(change)
                    "PROFILE" -> applyProfileChange(change)
                }
            } catch (e: Exception) {
                // Ignore malformed individual remote change to avoid blocking remaining updates
            }
        }
    }

    private suspend fun applySessionChange(change: com.studycompanion.app.core.network.RemoteChange) {
        val json = JSONObject(change.payloadJson)
        val remoteSession = StudySessionEntity(
            id = change.entityId,
            profileId = change.profileId ?: json.getString("profileId"),
            deviceId = json.optString("deviceId", "remote-device"),
            platform = json.optString("platform", "ANDROID"),
            packageName = json.optString("packageName").takeIf { it.isNotEmpty() },
            startAt = json.getLong("startAt"),
            endAt = json.getLong("endAt"),
            durationSeconds = json.getLong("durationSeconds"),
            trackingType = json.optString("trackingType", "AUTOMATIC"),
            verificationStatus = json.optString("verificationStatus", "VERIFIED_BY_RULES"),
            subjectId = json.optString("subjectId").takeIf { it.isNotEmpty() },
            createdAt = json.optLong("createdAt", change.serverTimestamp),
            updatedAt = json.optLong("updatedAt", change.serverTimestamp),
            deletedAt = if (change.operation == "DELETE") change.serverTimestamp else null,
            syncVersion = json.optLong("syncVersion", 1L),
            syncState = "SYNCED"
        )

        val local = database.studySessionDao().getSessionById(remoteSession.id)
        when (val res = ConflictResolver.resolveSession(local, remoteSession)) {
            is ConflictResolution.ApplyRemote -> database.studySessionDao().insert(res.entity)
            is ConflictResolution.KeepLocal -> { /* Keep local unchanged */ }
            is ConflictResolution.Delete -> database.studySessionDao().hardDeleteSession(res.id)
        }
    }

    private suspend fun applyTargetChange(change: com.studycompanion.app.core.network.RemoteChange) {
        val json = JSONObject(change.payloadJson)
        val profileId = change.profileId ?: json.getString("profileId")
        val dateKey = json.getString("dateKey")
        val remoteTarget = DailyTargetEntity(
            id = change.entityId,
            profileId = profileId,
            dateKey = dateKey,
            originalTargetSeconds = json.getLong("originalTargetSeconds"),
            adjustedTargetSeconds = if (json.has("adjustedTargetSeconds") && !json.isNull("adjustedTargetSeconds")) json.getLong("adjustedTargetSeconds") else null,
            carryInSeconds = json.optLong("carryInSeconds", 0L),
            carryOutSeconds = json.optLong("carryOutSeconds", 0L),
            createdAt = json.optLong("createdAt", change.serverTimestamp),
            updatedAt = json.optLong("updatedAt", change.serverTimestamp)
        )

        val local = database.dailyTargetDao().getTarget(profileId, dateKey)
        when (val res = ConflictResolver.resolveTarget(local, remoteTarget)) {
            is ConflictResolution.ApplyRemote -> database.dailyTargetDao().insert(res.entity)
            is ConflictResolution.KeepLocal -> { /* Keep local unchanged */ }
            is ConflictResolution.Delete -> database.dailyTargetDao().deleteTarget(res.id)
        }
    }

    private suspend fun applyStudyAppChange(change: com.studycompanion.app.core.network.RemoteChange) {
        val json = JSONObject(change.payloadJson)
        val profileId = change.profileId ?: json.getString("profileId")
        val packageName = json.getString("packageName")
        val remoteApp = StudyAppEntity(
            id = change.entityId,
            profileId = profileId,
            packageName = packageName,
            appLabel = json.optString("appLabel", packageName),
            iconRef = json.optString("iconRef").takeIf { it.isNotEmpty() },
            isEnabled = json.optBoolean("isEnabled", true),
            addedAt = json.optLong("addedAt", change.serverTimestamp),
            updatedAt = json.optLong("updatedAt", change.serverTimestamp)
        )

        val local = database.studyAppDao().getApp(profileId, packageName)
        when (val res = ConflictResolver.resolveStudyApp(local, remoteApp)) {
            is ConflictResolution.ApplyRemote -> database.studyAppDao().insert(res.entity)
            is ConflictResolution.KeepLocal -> { /* Keep local unchanged */ }
            is ConflictResolution.Delete -> database.studyAppDao().deleteApp(profileId, packageName)
        }
    }

    private suspend fun applySettingsChange(change: com.studycompanion.app.core.network.RemoteChange) {
        val json = JSONObject(change.payloadJson)
        val profileId = change.profileId ?: json.getString("profileId")
        val remoteSettings = ProfileSettingsEntity(
            profileId = profileId,
            countWhileLocked = json.optBoolean("countWhileLocked", true),
            pauseDuringCalls = json.optBoolean("pauseDuringCalls", true),
            pauseInMultiWindow = json.optBoolean("pauseInMultiWindow", true),
            pauseInFloatingWindow = json.optBoolean("pauseInFloatingWindow", true),
            themeMode = json.optString("themeMode", "SYSTEM"),
            updatedAt = json.optLong("updatedAt", change.serverTimestamp)
        )

        val local = database.profileSettingsDao().getSettings(profileId)
        when (val res = ConflictResolver.resolveSettings(local, remoteSettings)) {
            is ConflictResolution.ApplyRemote -> database.profileSettingsDao().insert(res.entity)
            is ConflictResolution.KeepLocal -> { /* Keep local unchanged */ }
            is ConflictResolution.Delete -> database.profileSettingsDao().deleteSettings(profileId)
        }
    }

    private suspend fun applyProfileChange(change: com.studycompanion.app.core.network.RemoteChange) {
        val json = JSONObject(change.payloadJson)
        val profile = ProfileEntity(
            id = change.entityId,
            userId = json.getString("userId"),
            name = json.getString("name"),
            avatarRef = json.optString("avatarRef").takeIf { it.isNotEmpty() },
            pinSalt = json.optString("pinSalt", ""),
            pinVerifier = json.optString("pinVerifier", ""),
            pinVersion = json.optInt("pinVersion", 1),
            createdAt = json.optLong("createdAt", change.serverTimestamp),
            updatedAt = json.optLong("updatedAt", change.serverTimestamp),
            deletedAt = if (change.operation == "DELETE") change.serverTimestamp else null
        )
        database.profileDao().insert(profile)
    }
}
