package com.studycompanion.app.data.remote

import com.studycompanion.app.core.datastore.UserSessionDataStore
import com.studycompanion.app.core.network.ApiService
import com.studycompanion.app.core.network.AuthResponse
import com.studycompanion.app.core.network.PullResponse
import com.studycompanion.app.core.network.PushResponse
import kotlinx.coroutines.flow.firstOrNull
import org.json.JSONArray
import java.io.IOException

/**
 * Remote data source coordinating network requests and authorization session tokens.
 */
class RemoteDataSource(
    private val apiService: ApiService,
    private val sessionDataStore: UserSessionDataStore
) {

    suspend fun signup(email: String, password: String): Result<AuthResponse> {
        val result = apiService.signup(email, password)
        result.onSuccess { auth ->
            sessionDataStore.saveSession(auth.userId, auth.accessToken, auth.refreshToken)
        }
        return result
    }

    suspend fun login(email: String, password: String): Result<AuthResponse> {
        val result = apiService.login(email, password)
        result.onSuccess { auth ->
            sessionDataStore.saveSession(auth.userId, auth.accessToken, auth.refreshToken)
        }
        return result
    }

    suspend fun logout(): Result<Unit> {
        val token = sessionDataStore.sessionTokenFlow.firstOrNull() ?: ""
        val result = if (token.isNotEmpty()) {
            apiService.logout(token)
        } else {
            Result.success(Unit)
        }
        sessionDataStore.clearSession()
        return result
    }

    suspend fun pushMutations(deviceId: String, mutationsJson: JSONArray): Result<PushResponse> {
        val token = getValidAccessToken() ?: return Result.failure(IOException("Not authenticated"))
        return apiService.pushMutations(token, deviceId, mutationsJson)
    }

    suspend fun pullChanges(cursor: Long): Result<PullResponse> {
        val token = getValidAccessToken() ?: return Result.failure(IOException("Not authenticated"))
        return apiService.pullChanges(token, cursor)
    }

    private suspend fun getValidAccessToken(): String? {
        val currentToken = sessionDataStore.sessionTokenFlow.firstOrNull()
        if (!currentToken.isNullOrEmpty()) {
            return currentToken
        }
        // Attempt refresh
        val refreshToken = sessionDataStore.refreshTokenFlow.firstOrNull() ?: return null
        val refreshResult = apiService.refreshToken(refreshToken)
        return refreshResult.getOrNull()?.let { auth ->
            sessionDataStore.saveSession(auth.userId, auth.accessToken, auth.refreshToken)
            auth.accessToken
        }
    }
}
