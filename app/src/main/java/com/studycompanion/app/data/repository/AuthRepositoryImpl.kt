package com.studycompanion.app.data.repository

import com.studycompanion.app.core.database.dao.UserDao
import com.studycompanion.app.core.database.entity.UserEntity
import com.studycompanion.app.core.datastore.UserSessionDataStore
import com.studycompanion.app.core.security.PinVerifier
import com.studycompanion.app.domain.model.AuthSession
import com.studycompanion.app.domain.model.User
import com.studycompanion.app.domain.repository.AuthRepository
import com.studycompanion.app.domain.repository.AuthState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryImpl(
    private val userDao: UserDao,
    private val sessionDataStore: UserSessionDataStore,
    private val remoteDataSource: com.studycompanion.app.data.remote.RemoteDataSource? = null
) : AuthRepository {

    override val authState: Flow<AuthState> = combine(
        sessionDataStore.userIdFlow,
        sessionDataStore.sessionTokenFlow
    ) { userId, token ->
        Pair(userId, token)
    }.flatMapLatest { (userId, token) ->
        if (userId != null && token != null) {
            userDao.getUserFlow(userId).flatMapLatest { userEntity ->
                if (userEntity != null) {
                    val user = userEntity.toDomain()
                    val session = AuthSession(
                        userId = user.id,
                        token = token,
                        createdAt = userEntity.createdAt,
                        expiresAt = null
                    )
                    flowOf(AuthState.Authenticated(user, session))
                } else {
                    flowOf(AuthState.Unauthenticated)
                }
            }
        } else {
            flowOf(AuthState.Unauthenticated)
        }
    }

    override suspend fun getCurrentUser(): User? {
        val userId = sessionDataStore.userIdFlow.firstOrNull() ?: return null
        return userDao.getUserById(userId)?.toDomain()
    }

    override suspend fun getActiveSession(): AuthSession? {
        val userId = sessionDataStore.userIdFlow.firstOrNull() ?: return null
        val token = sessionDataStore.sessionTokenFlow.firstOrNull() ?: return null
        val userEntity = userDao.getUserById(userId) ?: return null
        return AuthSession(
            userId = userEntity.id,
            token = token,
            createdAt = userEntity.createdAt,
            expiresAt = null
        )
    }

    override suspend fun signUp(email: String, password: String): Result<User> {
        val trimmedEmail = email.trim().lowercase()
        if (trimmedEmail.isBlank() || !trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
            return Result.failure(IllegalArgumentException("Invalid email address format"))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters"))
        }

        // 1. Enforce uniqueness: Duplicate email MUST fail immediately
        val existingLocal = userDao.getUserByEmail(trimmedEmail)
        if (existingLocal != null) {
            return Result.failure(IllegalArgumentException("An account with this email already exists."))
        }

        // 2. Attempt remote signup if configured
        var remoteUserId: String? = null
        var remoteToken: String? = null
        var remoteRefreshToken: String? = null

        if (remoteDataSource != null) {
            val remoteResult = remoteDataSource.signup(trimmedEmail, password)
            if (remoteResult.isSuccess) {
                val auth = remoteResult.getOrThrow()
                remoteUserId = auth.userId
                remoteToken = auth.accessToken
                remoteRefreshToken = auth.refreshToken
            } else {
                val exception = remoteResult.exceptionOrNull()
                if (exception is IOException && !isNetworkOrHostError(exception)) {
                    // Backend explicitly rejected (e.g. 400 user already exists on server)
                    return Result.failure(exception)
                }
            }
        }

        // 3. Cryptographic salt and PBKDF2 hash (Zero plaintext password storage)
        val (salt, hash) = PinVerifier.hashPin(password)
        val userId = remoteUserId ?: UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val userEntity = UserEntity(
            id = userId,
            email = trimmedEmail,
            authProvider = "EMAIL",
            passwordSalt = salt,
            passwordHash = hash,
            createdAt = now,
            updatedAt = now,
            deletedAt = null
        )

        try {
            userDao.insert(userEntity)
        } catch (e: Exception) {
            return Result.failure(IllegalArgumentException("An account with this email already exists."))
        }

        val token = remoteToken ?: UUID.randomUUID().toString()
        sessionDataStore.saveSession(userId, token, remoteRefreshToken)

        return Result.success(userEntity.toDomain())
    }

    override suspend fun login(email: String, password: String): Result<User> {
        val trimmedEmail = email.trim().lowercase()
        if (trimmedEmail.isBlank()) {
            return Result.failure(IllegalArgumentException("Email cannot be blank"))
        }
        if (password.isBlank()) {
            return Result.failure(IllegalArgumentException("Password cannot be blank"))
        }

        // 1. Attempt remote login if remoteDataSource is configured
        if (remoteDataSource != null) {
            val remoteResult = remoteDataSource.login(trimmedEmail, password)
            if (remoteResult.isSuccess) {
                val auth = remoteResult.getOrThrow()
                val now = System.currentTimeMillis()
                val existingLocal = userDao.getUserByEmail(trimmedEmail)
                val (salt, hash) = PinVerifier.hashPin(password)
                val userEntity = UserEntity(
                    id = auth.userId,
                    email = trimmedEmail,
                    authProvider = "EMAIL",
                    passwordSalt = salt,
                    passwordHash = hash,
                    createdAt = existingLocal?.createdAt ?: now,
                    updatedAt = now,
                    deletedAt = null
                )
                if (existingLocal != null) {
                    userDao.update(userEntity)
                } else {
                    userDao.insert(userEntity)
                }
                sessionDataStore.saveSession(auth.userId, auth.accessToken, auth.refreshToken)
                return Result.success(userEntity.toDomain())
            } else {
                val exception = remoteResult.exceptionOrNull()
                if (exception is IOException && !isNetworkOrHostError(exception)) {
                    // Backend explicitly rejected (e.g. 401 Unauthorized / wrong credentials)
                    return Result.failure(IllegalArgumentException("Invalid email or password"))
                }
            }
        }

        // 2. Local-first cryptographic verification
        val userEntity = userDao.getUserByEmail(trimmedEmail)
        if (userEntity == null) {
            return Result.failure(IllegalArgumentException("Invalid email or password"))
        }

        val passwordValid = if (userEntity.passwordSalt.isNotBlank() && userEntity.passwordHash.isNotBlank()) {
            PinVerifier.verifyPin(password, userEntity.passwordSalt, userEntity.passwordHash)
        } else {
            false
        }

        if (!passwordValid) {
            return Result.failure(IllegalArgumentException("Invalid email or password"))
        }

        val token = UUID.randomUUID().toString()
        sessionDataStore.saveSession(userEntity.id, token)

        return Result.success(userEntity.toDomain())
    }

    override suspend fun logout(): Result<Unit> {
        try {
            remoteDataSource?.logout()
        } catch (e: Exception) {
            // Ignore remote logout failure when offline
        }
        sessionDataStore.clearSession()
        return Result.success(Unit)
    }

    private fun isNetworkOrHostError(throwable: Throwable?): Boolean {
        var current: Throwable? = throwable
        while (current != null) {
            if (current is UnknownHostException ||
                current is ConnectException ||
                current is SocketTimeoutException ||
                current is NoRouteToHostException
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }

    private fun UserEntity.toDomain(): User {
        return User(
            id = id,
            email = email,
            authProvider = authProvider,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
