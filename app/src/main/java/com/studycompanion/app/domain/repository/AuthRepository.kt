package com.studycompanion.app.domain.repository

import com.studycompanion.app.domain.model.AuthSession
import com.studycompanion.app.domain.model.User
import kotlinx.coroutines.flow.Flow

sealed interface AuthState {
    data object Unauthenticated : AuthState
    data class Authenticated(val user: User, val session: AuthSession) : AuthState
}

interface AuthRepository {
    val authState: Flow<AuthState>
    suspend fun getCurrentUser(): User?
    suspend fun getActiveSession(): AuthSession?
    suspend fun signUp(email: String, password: String): Result<User>
    suspend fun login(email: String, password: String): Result<User>
    suspend fun logout(): Result<Unit>
}
