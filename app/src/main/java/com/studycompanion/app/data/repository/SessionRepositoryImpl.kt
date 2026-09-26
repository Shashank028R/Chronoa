package com.studycompanion.app.data.repository

import com.studycompanion.app.core.database.dao.SessionEventDao
import com.studycompanion.app.core.database.dao.StudySessionDao
import com.studycompanion.app.core.database.entity.SessionEventEntity
import com.studycompanion.app.core.database.entity.StudySessionEntity
import com.studycompanion.app.domain.model.SessionEvent
import com.studycompanion.app.domain.model.StudySession
import com.studycompanion.app.domain.model.TrackingType
import com.studycompanion.app.domain.model.VerificationStatus
import com.studycompanion.app.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class SessionRepositoryImpl(
    private val studySessionDao: StudySessionDao,
    private val sessionEventDao: SessionEventDao,
    private val syncMutationDao: com.studycompanion.app.core.database.dao.SyncMutationDao? = null
) : SessionRepository {

    override fun getSessions(profileId: String): Flow<List<StudySession>> {
        return flow {
            emit(studySessionDao.getSessionsForProfile(profileId).map { it.toDomain() })
            emitAll(studySessionDao.getSessionsFlowForProfile(profileId).map { list ->
                list.map { it.toDomain() }
            })
        }
    }

    override fun getSessionsInRange(
        profileId: String,
        startAt: Long,
        endAt: Long
    ): Flow<List<StudySession>> {
        return studySessionDao.getSessionsFlowForProfileInRange(profileId, startAt, endAt).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun recordSession(session: StudySession): Result<Unit> {
        val entity = StudySessionEntity(
            id = session.id,
            profileId = session.profileId,
            deviceId = session.deviceId,
            platform = session.platform,
            packageName = session.packageName,
            startAt = session.startAt,
            endAt = session.endAt,
            durationSeconds = session.durationSeconds,
            trackingType = session.trackingType.name,
            verificationStatus = session.verificationStatus.name,
            subjectId = session.subjectId,
            createdAt = session.createdAt,
            updatedAt = session.updatedAt,
            deletedAt = session.deletedAt,
            syncVersion = session.syncVersion,
            syncState = session.syncState
        )
        studySessionDao.insert(entity)

        if (syncMutationDao != null) {
            val payload = org.json.JSONObject().apply {
                put("id", entity.id)
                put("profileId", entity.profileId)
                put("deviceId", entity.deviceId)
                put("platform", entity.platform)
                put("packageName", entity.packageName ?: "")
                put("startAt", entity.startAt)
                put("endAt", entity.endAt)
                put("durationSeconds", entity.durationSeconds)
                put("trackingType", entity.trackingType)
                put("verificationStatus", entity.verificationStatus)
                put("subjectId", entity.subjectId ?: "")
                put("createdAt", entity.createdAt)
                put("updatedAt", entity.updatedAt)
                put("syncVersion", entity.syncVersion)
            }
            val mutation = com.studycompanion.app.core.database.entity.SyncMutationEntity(
                id = java.util.UUID.randomUUID().toString(),
                profileId = entity.profileId,
                deviceId = entity.deviceId,
                entityType = "SESSION",
                recordId = entity.id,
                operation = "INSERT",
                payloadJson = payload.toString(),
                createdAt = entity.updatedAt,
                syncState = "PENDING"
            )
            syncMutationDao.insert(mutation)
        }

        return Result.success(Unit)
    }

    override suspend fun recordEvent(event: SessionEvent): Result<Unit> {
        val entity = SessionEventEntity(
            id = event.id,
            sessionId = event.sessionId,
            profileId = event.profileId,
            deviceId = event.deviceId,
            eventType = event.eventType,
            timestamp = event.timestamp,
            packageName = event.packageName,
            engineState = event.engineState,
            reasonCode = event.reasonCode,
            source = event.source,
            metadataJson = event.metadataJson,
            createdAt = event.createdAt
        )
        sessionEventDao.insert(entity)
        return Result.success(Unit)
    }

    override suspend fun getEventsForSession(sessionId: String): List<SessionEvent> {
        return sessionEventDao.getEventsForSession(sessionId).map { it.toDomain() }
    }

    override suspend fun getSessionById(sessionId: String): StudySession? {
        return studySessionDao.getSessionById(sessionId)?.toDomain()
    }

    override suspend fun deleteSession(sessionId: String): Result<Unit> {
        val now = System.currentTimeMillis()
        val existing = studySessionDao.getSessionById(sessionId) ?: return Result.failure(Exception("Session not found"))
        studySessionDao.softDeleteSession(sessionId, now)

        if (syncMutationDao != null) {
            val mutation = com.studycompanion.app.core.database.entity.SyncMutationEntity(
                id = java.util.UUID.randomUUID().toString(),
                profileId = existing.profileId,
                deviceId = existing.deviceId,
                entityType = "SESSION",
                recordId = sessionId,
                operation = "DELETE",
                payloadJson = org.json.JSONObject().apply {
                    put("id", sessionId)
                    put("deletedAt", now)
                }.toString(),
                createdAt = now,
                syncState = "PENDING"
            )
            syncMutationDao.insert(mutation)
        }
        return Result.success(Unit)
    }

    override suspend fun updateSession(session: StudySession): Result<Unit> {
        val now = System.currentTimeMillis()
        val entity = StudySessionEntity(
            id = session.id,
            profileId = session.profileId,
            deviceId = session.deviceId,
            platform = session.platform,
            packageName = session.packageName,
            startAt = session.startAt,
            endAt = session.endAt,
            durationSeconds = session.durationSeconds,
            trackingType = session.trackingType.name,
            verificationStatus = session.verificationStatus.name,
            subjectId = session.subjectId,
            createdAt = session.createdAt,
            updatedAt = now,
            deletedAt = session.deletedAt,
            syncVersion = session.syncVersion + 1,
            syncState = "PENDING"
        )
        studySessionDao.update(entity)

        if (syncMutationDao != null) {
            val payload = org.json.JSONObject().apply {
                put("id", entity.id)
                put("profileId", entity.profileId)
                put("deviceId", entity.deviceId)
                put("platform", entity.platform)
                put("packageName", entity.packageName ?: "")
                put("startAt", entity.startAt)
                put("endAt", entity.endAt)
                put("durationSeconds", entity.durationSeconds)
                put("trackingType", entity.trackingType)
                put("verificationStatus", entity.verificationStatus)
                put("subjectId", entity.subjectId ?: "")
                put("createdAt", entity.createdAt)
                put("updatedAt", entity.updatedAt)
                put("syncVersion", entity.syncVersion)
            }
            val mutation = com.studycompanion.app.core.database.entity.SyncMutationEntity(
                id = java.util.UUID.randomUUID().toString(),
                profileId = entity.profileId,
                deviceId = entity.deviceId,
                entityType = "SESSION",
                recordId = entity.id,
                operation = "UPDATE",
                payloadJson = payload.toString(),
                createdAt = now,
                syncState = "PENDING"
            )
            syncMutationDao.insert(mutation)
        }
        return Result.success(Unit)
    }

    private fun StudySessionEntity.toDomain(): StudySession {
        val tracking = try { TrackingType.valueOf(trackingType) } catch (e: Exception) { TrackingType.AUTOMATIC }
        val verification = try { VerificationStatus.valueOf(verificationStatus) } catch (e: Exception) { VerificationStatus.VERIFIED_BY_RULES }

        return StudySession(
            id = id,
            profileId = profileId,
            deviceId = deviceId,
            platform = platform,
            packageName = packageName,
            startAt = startAt,
            endAt = endAt,
            durationSeconds = durationSeconds,
            trackingType = tracking,
            verificationStatus = verification,
            subjectId = subjectId,
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
            syncVersion = syncVersion,
            syncState = syncState
        )
    }

    private fun SessionEventEntity.toDomain(): SessionEvent {
        return SessionEvent(
            id = id,
            sessionId = sessionId,
            profileId = profileId,
            deviceId = deviceId,
            eventType = eventType,
            timestamp = timestamp,
            packageName = packageName,
            engineState = engineState,
            reasonCode = reasonCode,
            source = source,
            metadataJson = metadataJson,
            createdAt = createdAt
        )
    }
}
