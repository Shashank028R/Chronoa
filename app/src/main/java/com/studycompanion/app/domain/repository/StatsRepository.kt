package com.studycompanion.app.domain.repository

import com.studycompanion.app.domain.model.DaySummary
import com.studycompanion.app.domain.model.StudyStatsOverview
import kotlinx.coroutines.flow.Flow

interface StatsRepository {
    fun observeStatsOverview(profileId: String, todayDateKey: String): Flow<StudyStatsOverview>
    fun observeDaySummary(profileId: String, dateKey: String): Flow<DaySummary>
    fun observeAllDaySummaries(profileId: String): Flow<Map<String, DaySummary>>
    suspend fun getStatsOverview(profileId: String, todayDateKey: String): StudyStatsOverview
}
