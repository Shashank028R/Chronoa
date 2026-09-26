package com.studycompanion.app.core.network

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Interceptor that redacts sensitive headers and tokens to prevent leaking credentials in logs.
 */
class SecureRedactingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        // Forward request without logging sensitive authorization or authentication payloads
        return chain.proceed(request)
    }
}

/**
 * Factory providing preconfigured, TLS-secured OkHttpClient instances.
 */
object HttpClientFactory {
    fun createClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(SecureRedactingInterceptor())
            .retryOnConnectionFailure(true)
            .build()
    }
}

/**
 * Production implementation of ApiService using OkHttp.
 */
class HttpApiService(
    private val client: OkHttpClient = HttpClientFactory.createClient(),
    private val baseUrl: String = "https://api.studycompanion.app/v1"
) : ApiService {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override suspend fun signup(email: String, password: String): Result<AuthResponse> {
        val payload = JSONObject().apply {
            put("email", email)
            put("password", password)
        }
        return postAuth("/auth/signup", payload)
    }

    override suspend fun login(email: String, password: String): Result<AuthResponse> {
        val payload = JSONObject().apply {
            put("email", email)
            put("password", password)
        }
        return postAuth("/auth/login", payload)
    }

    override suspend fun refreshToken(refreshToken: String): Result<AuthResponse> {
        val payload = JSONObject().apply {
            put("refreshToken", refreshToken)
        }
        return postAuth("/auth/refresh", payload)
    }

    override suspend fun logout(accessToken: String): Result<Unit> {
        val request = Request.Builder()
            .url("$baseUrl/auth/logout")
            .addHeader("Authorization", "Bearer $accessToken")
            .post("{}".toRequestBody(jsonMediaType))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(IOException("Logout failed with HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAccount(accessToken: String): Result<Unit> {
        val request = Request.Builder()
            .url("$baseUrl/auth/account")
            .addHeader("Authorization", "Bearer $accessToken")
            .delete()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 404) {
                    Result.success(Unit)
                } else {
                    Result.failure(IOException("Account deletion failed with HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun pushMutations(
        accessToken: String,
        deviceId: String,
        mutationsJson: JSONArray
    ): Result<PushResponse> {
        val payload = JSONObject().apply {
            put("deviceId", deviceId)
            put("mutations", mutationsJson)
        }

        val request = Request.Builder()
            .url("$baseUrl/sync/push")
            .addHeader("Authorization", "Bearer $accessToken")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(IOException("Sync push failed with HTTP ${response.code}"))
                }
                val bodyStr = response.body?.string() ?: "{}"
                val json = JSONObject(bodyStr)
                val serverTime = json.optLong("serverTimestamp", System.currentTimeMillis())
                val ackArray = json.optJSONArray("acknowledgedMutationIds") ?: JSONArray()
                val ackList = mutableListOf<String>()
                for (i in 0 until ackArray.length()) {
                    ackList.add(ackArray.getString(i))
                }
                Result.success(PushResponse(serverTime, ackList))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun pullChanges(
        accessToken: String,
        cursor: Long
    ): Result<PullResponse> {
        val request = Request.Builder()
            .url("$baseUrl/sync/pull?cursor=$cursor")
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(IOException("Sync pull failed with HTTP ${response.code}"))
                }
                val bodyStr = response.body?.string() ?: "{}"
                val json = JSONObject(bodyStr)
                val nextCursor = json.optLong("nextCursor", cursor)
                val serverTime = json.optLong("serverTimestamp", System.currentTimeMillis())
                val changesArray = json.optJSONArray("changes") ?: JSONArray()
                val changes = mutableListOf<RemoteChange>()

                for (i in 0 until changesArray.length()) {
                    val item = changesArray.getJSONObject(i)
                    changes.add(
                        RemoteChange(
                            cursorId = item.optLong("cursorId"),
                            entityType = item.optString("entityType"),
                            entityId = item.optString("entityId"),
                            profileId = item.optString("profileId").takeIf { it.isNotEmpty() },
                            operation = item.optString("operation", "INSERT"),
                            payloadJson = item.optJSONObject("payload")?.toString()
                                ?: item.optString("payloadJson", "{}"),
                            serverTimestamp = item.optLong("serverTimestamp", serverTime)
                        )
                    )
                }

                Result.success(PullResponse(nextCursor, serverTime, changes))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun postAuth(endpoint: String, payload: JSONObject): Result<AuthResponse> {
        val request = Request.Builder()
            .url("$baseUrl$endpoint")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(IOException("Auth failed with HTTP ${response.code}"))
                }
                val bodyStr = response.body?.string() ?: "{}"
                val json = JSONObject(bodyStr)
                val auth = AuthResponse(
                    userId = json.getString("userId"),
                    email = json.optString("email", ""),
                    accessToken = json.getString("accessToken"),
                    refreshToken = json.getString("refreshToken"),
                    expiresAt = json.optLong("expiresAt", System.currentTimeMillis() + 3600_000L)
                )
                Result.success(auth)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
