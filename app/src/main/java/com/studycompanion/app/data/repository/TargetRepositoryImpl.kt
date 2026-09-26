package com.studycompanion.app.data.repository

import com.studycompanion.app.core.database.dao.DailyTargetDao
import com.studycompanion.app.core.database.entity.DailyTargetEntity
import com.studycompanion.app.domain.model.DailyTarget
import com.studycompanion.app.domain.repository.TargetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class TargetRepositoryImpl(
    private val dailyTargetDao: DailyTargetDao,
    private val syncMutationDao: com.studycompanion.app.core.database.dao.SyncMutationDao? = null,
    private val deviceId: String = "local-device"
) : TargetRepository {

    override fun getTarget(profileId: String, dateKey: String): Flow<DailyTarget?> {
        return flow {
            emit(dailyTargetDao.getTarget(profileId, dateKey)?.toDomain())
            emitAll(dailyTargetDao.getTargetFlow(profileId, dateKey).map { it?.toDomain() })
        }
    }

    override suspend fun getTargetSync(profileId: String, dateKey: String): DailyTarget? {
        return dailyTargetDao.getTarget(profileId, dateKey)?.toDomain()
    }

    override fun getTargetsForProfile(profileId: String): Flow<List<DailyTarget>> {
        return dailyTargetDao.getTargetsFlowForProfile(profileId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun setDailyTarget(
        profileId: String,
        dateKey: String,
        targetSeconds: Long
    ): Result<DailyTarget> {
        if (targetSeconds <= 0) {
            return Result.failure(IllegalArgumentException("Target must be greater than 0 seconds"))
        }

        val now = System.currentTimeMillis()
        val existing = dailyTargetDao.getTarget(profileId, dateKey)

        val updatedEntity = if (existing != null) {
            // CRITICAL: Preserve originalTargetSeconds!
            // When updating a target that was already set, store change in adjustedTargetSeconds.
            existing.copy(
                adjustedTargetSeconds = targetSeconds,
                updatedAt = now
            )
        } else {
            DailyTargetEntity(
                id = UUID.randomUUID().toString(),
                profileId = profileId,
                dateKey = dateKey,
                originalTargetSeconds = targetSeconds,
                adjustedTargetSeconds = null,
                carryInSeconds = 0L,
                carryOutSeconds = 0L,
                createdAt = now,
                updatedAt = now
            )
        }

        dailyTargetDao.insert(updatedEntity)
        enqueueTargetMutation(updatedEntity)
        return Result.success(updatedEntity.toDomain())
    }

    override suspend fun adjustDailyTarget(
        profileId: String,
        dateKey: String,
        adjustedSeconds: Long
    ): Result<DailyTarget> {
        if (adjustedSeconds <= 0) {
            return Result.failure(IllegalArgumentException("Adjusted target must be greater than 0 seconds"))
        }

        val existing = dailyTargetDao.getTarget(profileId, dateKey)
            ?: return Result.failure(IllegalStateException("Cannot adjust non-existent target for $dateKey"))

        val now = System.currentTimeMillis()
        val updatedEntity = existing.copy(
            adjustedTargetSeconds = adjustedSeconds,
            updatedAt = now
        )

        dailyTargetDao.update(updatedEntity)
        enqueueTargetMutation(updatedEntity)
        return Result.success(updatedEntity.toDomain())
    }

    private suspend fun enqueueTargetMutation(entity: DailyTargetEntity) {
        val dao = syncMutationDao ?: return
        val payload = org.json.JSONObject().apply {
            put("id", entity.id)
            put("profileId", entity.profileId)
            put("dateKey", entity.dateKey)
            put("originalTargetSeconds", entity.originalTargetSeconds)
            if (entity.adjustedTargetSeconds != null) {
                put("adjustedTargetSeconds", entity.adjustedTargetSeconds)
            }
            put("carryInSeconds", entity.carryInSeconds)
            put("carryOutSeconds", entity.carryOutSeconds)
            put("createdAt", entity.createdAt)
            put("updatedAt", entity.updatedAt)
        }
        val mutation = com.studycompanion.app.core.database.entity.SyncMutationEntity(
            id = UUID.randomUUID().toString(),
            profileId = entity.profileId,
            deviceId = deviceId,
            entityType = "TARGET",
            recordId = entity.id,
            operation = "UPDATE",
            payloadJson = payload.toString(),
            createdAt = entity.updatedAt,
            syncState = "PENDING"
        )
        dao.insert(mutation)
    }

    override suspend fun recordCarryIn(
        profileId: String,
        dateKey: String,
        carryInSeconds: Long
    ): Result<DailyTarget> {
        val existing = dailyTargetDao.getTarget(profileId, dateKey)
            ?: return Result.failure(IllegalStateException("Cannot record carry-in for non-existent target for $dateKey"))

        val now = System.currentTimeMillis()
        val updatedEntity = existing.copy(
            carryInSeconds = carryInSeconds,
            updatedAt = now
        )

        dailyTargetDao.update(updatedEntity)
        return Result.success(updatedEntity.toDomain())
    }

    private fun DailyTargetEntity.toDomain(): DailyTarget {
        return DailyTarget(
            id = id,
            profileId = profileId,
            dateKey = dateKey,
            originalTargetSeconds = originalTargetSeconds,
            adjustedTargetSeconds = adjustedTargetSeconds,
            carryInSeconds = carryInSeconds,
            carryOutSeconds = carryOutSeconds,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
