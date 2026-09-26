package com.studycompanion.app.domain.usecase

import com.studycompanion.app.domain.model.Profile
import com.studycompanion.app.domain.repository.ProfileRepository

class CreateProfileUseCase(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(userId: String, name: String, pin: String): Result<Profile> {
        return profileRepository.createProfile(userId, name, pin)
    }
}

class SwitchProfileUseCase(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(profileId: String, pin: String): Result<Profile> {
        return profileRepository.switchProfile(profileId, pin)
    }
}
