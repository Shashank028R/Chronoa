package com.studycompanion.app.domain.repository

import com.studycompanion.app.domain.model.DailyTarget
import kotlinx.coroutines.flow.Flow

interface TargetRepository {
    fun getTarget(profileId: String, dateKey: String): Flow<DailyTarget?>
    suspend fun getTargetSync(profileId: String, dateKey: String): DailyTarget?
    fun getTargetsForProfile(profileId: String): Flow<List<DailyTarget>>
    suspend fun setDailyTarget(profileId: String, dateKey: String, targetSeconds: Long): Result<DailyTarget>
    suspend fun adjustDailyTarget(profileId: String, dateKey: String, adjustedSeconds: Long): Result<DailyTarget>
    suspend fun recordCarryIn(profileId: String, dateKey: String, carryInSeconds: Long): Result<DailyTarget>
}
