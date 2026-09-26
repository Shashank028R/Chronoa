package com.studycompanion.app.domain.repository

import com.studycompanion.app.domain.model.SessionEvent
import com.studycompanion.app.domain.model.StudySession
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun getSessions(profileId: String): Flow<List<StudySession>>
    fun getSessionsInRange(profileId: String, startAt: Long, endAt: Long): Flow<List<StudySession>>
    suspend fun recordSession(session: StudySession): Result<Unit>
    suspend fun recordEvent(event: SessionEvent): Result<Unit>
    suspend fun getEventsForSession(sessionId: String): List<SessionEvent>
    suspend fun getSessionById(sessionId: String): StudySession?
    suspend fun deleteSession(sessionId: String): Result<Unit>
    suspend fun updateSession(session: StudySession): Result<Unit>
}
