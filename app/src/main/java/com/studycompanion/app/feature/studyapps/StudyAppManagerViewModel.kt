package com.studycompanion.app.feature.studyapps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.studycompanion.app.domain.model.StudyApp
import com.studycompanion.app.domain.repository.ProfileRepository
import com.studycompanion.app.domain.repository.SelectableApp
import com.studycompanion.app.domain.repository.StudyAppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class StudyAppManagerUiState(
    val approvedApps: List<StudyApp> = emptyList(),
    val availableApps: List<SelectableApp> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val editingApp: StudyApp? = null
)

class StudyAppManagerViewModel(
    private val profileRepository: ProfileRepository,
    private val studyAppRepository: StudyAppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudyAppManagerUiState(isLoading = true))
    val uiState: StateFlow<StudyAppManagerUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private suspend fun resolveActiveProfile() =
        profileRepository.getActiveProfileSync() ?: profileRepository.getActiveProfile().first()

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val profile = resolveActiveProfile()
            if (profile != null) {
                studyAppRepository.getStudyApps(profile.id).collect { apps ->
                    val available = try {
                        studyAppRepository.getAvailableLaunchableApps(profile.id)
                    } catch (e: Exception) {
                        emptyList()
                    }
                    _uiState.value = _uiState.value.copy(
                        approvedApps = apps,
                        availableApps = available,
                        isLoading = false
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun addApp(packageName: String, label: String) {
        viewModelScope.launch {
            val profile = resolveActiveProfile() ?: return@launch
            studyAppRepository.addStudyApp(profile.id, packageName, label)
            refreshAvailable(profile.id)
        }
    }

    fun removeApp(packageName: String) {
        viewModelScope.launch {
            val profile = resolveActiveProfile() ?: return@launch
            studyAppRepository.removeStudyApp(profile.id, packageName)
            refreshAvailable(profile.id)
        }
    }

    fun addStudyApp(packageName: String, label: String) = addApp(packageName, label)
    fun removeStudyApp(packageName: String) = removeApp(packageName)

    fun setAppEnabled(packageName: String, enabled: Boolean) {
        viewModelScope.launch {
            val profile = resolveActiveProfile() ?: return@launch
            studyAppRepository.setAppEnabled(profile.id, packageName, enabled)
        }
    }

    fun updateAppLabel(packageName: String, newLabel: String) {
        viewModelScope.launch {
            val profile = resolveActiveProfile() ?: return@launch
            studyAppRepository.updateAppLabel(profile.id, packageName, newLabel)
            _uiState.value = _uiState.value.copy(editingApp = null)
        }
    }

    fun saveAppLabel(packageName: String, newLabel: String) = updateAppLabel(packageName, newLabel)

    fun startEditingApp(app: StudyApp) {
        _uiState.value = _uiState.value.copy(editingApp = app)
    }

    fun startEditingLabel(app: StudyApp) = startEditingApp(app)

    fun cancelEditingApp() {
        _uiState.value = _uiState.value.copy(editingApp = null)
    }

    fun cancelEditingLabel() = cancelEditingApp()

    private suspend fun refreshAvailable(profileId: String) {
        try {
            val available = studyAppRepository.getAvailableLaunchableApps(profileId)
            _uiState.value = _uiState.value.copy(availableApps = available)
        } catch (e: Exception) {
            // Ignore in testing or headless environments
        }
    }

    class Factory(
        private val profileRepository: ProfileRepository,
        private val studyAppRepository: StudyAppRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StudyAppManagerViewModel(profileRepository, studyAppRepository) as T
        }
    }
}
