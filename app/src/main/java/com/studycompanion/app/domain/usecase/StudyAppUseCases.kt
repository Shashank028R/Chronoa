package com.studycompanion.app.domain.usecase

import com.studycompanion.app.domain.model.StudyApp
import com.studycompanion.app.domain.repository.SelectableApp
import com.studycompanion.app.domain.repository.StudyAppRepository
import kotlinx.coroutines.flow.Flow

class ManageStudyAppsUseCase(
    private val studyAppRepository: StudyAppRepository
) {
    fun getStudyApps(profileId: String): Flow<List<StudyApp>> {
        return studyAppRepository.getStudyApps(profileId)
    }

    suspend fun addStudyApp(profileId: String, packageName: String, appLabel: String): Result<StudyApp> {
        return studyAppRepository.addStudyApp(profileId, packageName, appLabel)
    }

    suspend fun removeStudyApp(profileId: String, packageName: String): Result<Unit> {
        return studyAppRepository.removeStudyApp(profileId, packageName)
    }

    suspend fun setAppEnabled(profileId: String, packageName: String, enabled: Boolean): Result<Unit> {
        return studyAppRepository.setAppEnabled(profileId, packageName, enabled)
    }

    suspend fun getAvailableLaunchableApps(profileId: String): List<SelectableApp> {
        return studyAppRepository.getAvailableLaunchableApps(profileId)
    }
}
