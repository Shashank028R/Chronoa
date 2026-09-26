package com.studycompanion.app.domain.repository

import com.studycompanion.app.domain.model.StudyApp
import kotlinx.coroutines.flow.Flow

data class SelectableApp(
    val packageName: String,
    val label: String,
    val isSelected: Boolean = false
)

interface StudyAppRepository {
    fun getStudyApps(profileId: String): Flow<List<StudyApp>>
    fun getEnabledStudyApps(profileId: String): Flow<List<StudyApp>>
    suspend fun getStudyAppsSync(profileId: String): List<StudyApp>
    suspend fun addStudyApp(profileId: String, packageName: String, appLabel: String): Result<StudyApp>
    suspend fun removeStudyApp(profileId: String, packageName: String): Result<Unit>
    suspend fun setAppEnabled(profileId: String, packageName: String, enabled: Boolean): Result<Unit>
    suspend fun updateAppLabel(profileId: String, packageName: String, newLabel: String): Result<Unit>
    suspend fun getAvailableLaunchableApps(profileId: String): List<SelectableApp>
}
