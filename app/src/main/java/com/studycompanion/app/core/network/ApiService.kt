package com.studycompanion.app.core.network

import org.json.JSONArray
import org.json.JSONObject

/**
 * Data transfer models for remote API communication.
 */
data class AuthResponse(
    val userId: String,
    val email: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long
)

data class RemoteChange(
    val cursorId: Long,
    val entityType: String,
    val entityId: String,
    val profileId: String?,
    val operation: String, // INSERT, UPDATE, DELETE
    val payloadJson: String,
    val serverTimestamp: Long
)

data class PullResponse(
    val nextCursor: Long,
    val serverTimestamp: Long,
    val changes: List<RemoteChange>
)

data class PushResponse(
    val serverTimestamp: Long,
    val acknowledgedMutationIds: List<String>,
    val rejectedMutationIds: Map<String, String> = emptyMap() // mutationId -> reason
)

/**
 * Contract for the remote HTTP API service.
 */
interface ApiService {
    suspend fun signup(email: String, password: String):Result<AuthResponse>
    suspend fun login(email: String, password: String): Result<AuthResponse>
    suspend fun refreshToken(refreshToken: String): Result<AuthResponse>
    suspend fun logout(accessToken: String): Result<Unit>

    suspend fun pushMutations(
        accessToken: String,
        deviceId: String,
        mutationsJson: JSONArray
    ): Result<PushResponse>

    suspend fun pullChanges(
        accessToken: String,
        cursor: Long
    ): Result<PullResponse>
}
