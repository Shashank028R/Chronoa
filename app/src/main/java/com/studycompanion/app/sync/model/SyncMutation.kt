package com.studycompanion.app.sync.model

import com.studycompanion.app.core.database.entity.SyncMutationEntity
import org.json.JSONObject
import java.util.UUID

/**
 * Domain representation of an offline mutation waiting to be synced to the cloud.
 */
data class SyncMutation(
    val id: String = UUID.randomUUID().toString(),
    val profileId: String,
    val deviceId: String,
    val entityType: String, // PROFILE, TARGET, STUDY_APP, SESSION, SETTINGS
    val recordId: String,
    val operation: String,  // INSERT, UPDATE, DELETE
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val syncState: String = "PENDING",
    val attemptCount: Int = 0,
    val lastAttemptAt: Long? = null,
    val errorCode: String? = null
) {
    fun toEntity(): SyncMutationEntity {
        return SyncMutationEntity(
            id = id,
            profileId = profileId,
            deviceId = deviceId,
            entityType = entityType,
            recordId = recordId,
            operation = operation,
            payloadJson = payloadJson,
            createdAt = createdAt,
            syncState = syncState,
            attemptCount = attemptCount,
            lastAttemptAt = lastAttemptAt,
            errorCode = errorCode
        )
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("profileId", profileId)
            put("deviceId", deviceId)
            put("entityType", entityType)
            put("recordId", recordId)
            put("operation", operation)
            put("payload", JSONObject(payloadJson))
            put("createdAt", createdAt)
        }
    }

    companion object {
        fun fromEntity(entity: SyncMutationEntity): SyncMutation {
            return SyncMutation(
                id = entity.id,
                profileId = entity.profileId,
                deviceId = entity.deviceId,
                entityType = entity.entityType,
                recordId = entity.recordId,
                operation = entity.operation,
                payloadJson = entity.payloadJson,
                createdAt = entity.createdAt,
                syncState = entity.syncState,
                attemptCount = entity.attemptCount,
                lastAttemptAt = entity.lastAttemptAt,
                errorCode = entity.errorCode
            )
        }
    }
}
