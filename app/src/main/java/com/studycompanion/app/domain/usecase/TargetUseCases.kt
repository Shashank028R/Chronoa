package com.studycompanion.app.domain.usecase

import com.studycompanion.app.domain.model.DailyTarget
import com.studycompanion.app.domain.repository.TargetRepository
import kotlinx.coroutines.flow.Flow

class SetDailyTargetUseCase(
    private val targetRepository: TargetRepository
) {
    suspend operator fun invoke(
        profileId: String,
        dateKey: String,
        targetSeconds: Long
    ): Result<DailyTarget> {
        return targetRepository.setDailyTarget(profileId, dateKey, targetSeconds)
    }
}

class AdjustDailyTargetUseCase(
    private val targetRepository: TargetRepository
) {
    suspend operator fun invoke(
        profileId: String,
        dateKey: String,
        adjustedSeconds: Long
    ): Result<DailyTarget> {
        return targetRepository.adjustDailyTarget(profileId, dateKey, adjustedSeconds)
    }
}

class GetDailyTargetUseCase(
    private val targetRepository: TargetRepository
) {
    operator fun invoke(profileId: String, dateKey: String): Flow<DailyTarget?> {
        return targetRepository.getTarget(profileId, dateKey)
    }
}
