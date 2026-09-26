package com.studycompanion.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.studycompanion.app.core.datastore.UserSessionDataStore
import com.studycompanion.app.core.ui.theme.AppThemeMode
import com.studycompanion.app.domain.model.Profile
import com.studycompanion.app.domain.model.ProfileSettings
import com.studycompanion.app.domain.model.User
import com.studycompanion.app.domain.repository.AuthRepository
import com.studycompanion.app.domain.repository.AuthState
import com.studycompanion.app.domain.repository.ProfileRepository
import com.studycompanion.app.domain.repository.TargetRepository
import com.studycompanion.app.sync.SyncEngine
import com.studycompanion.app.sync.model.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class SettingsUiState(
    val currentUser: User? = null,
    val activeProfile: Profile? = null,
    val settings: ProfileSettings = ProfileSettings(""),
    val currentTheme: AppThemeMode = AppThemeMode.AMOLED,
    val syncState: SyncState = SyncState.Synced(0L),
    val todayTargetSeconds: Long = 0L,
    val isSyncing: Boolean = false,
    val message: String? = null
)

class SettingsViewModel(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val targetRepository: TargetRepository,
    private val sessionDataStore: UserSessionDataStore,
    private val syncEngine: SyncEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val todayDateKey: String
        get() = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            authRepository.authState.collect { auth ->
                val user = if (auth is AuthState.Authenticated) auth.user else null
                _uiState.value = _uiState.value.copy(currentUser = user)
            }
        }

        viewModelScope.launch {
            profileRepository.getActiveProfile().collect { profile ->
                _uiState.value = _uiState.value.copy(activeProfile = profile)
                if (profile != null) {
                    val s = profileRepository.getProfileSettings(profile.id).first()
                    val target = targetRepository.getTarget(profile.id, todayDateKey).first()
                    val theme = when (s.themeMode) {
                        com.studycompanion.app.domain.model.ThemeMode.LIGHT -> AppThemeMode.LIGHT
                        com.studycompanion.app.domain.model.ThemeMode.DARK -> AppThemeMode.DARK
                        com.studycompanion.app.domain.model.ThemeMode.AMOLED -> AppThemeMode.AMOLED
                        com.studycompanion.app.domain.model.ThemeMode.SYSTEM -> AppThemeMode.SYSTEM
                    }
                    _uiState.value = _uiState.value.copy(
                        settings = s,
                        currentTheme = theme,
                        todayTargetSeconds = target?.adjustedTargetSeconds ?: target?.originalTargetSeconds ?: 0L
                    )
                }
            }
        }

        viewModelScope.launch {
            syncEngine.syncState.collect { sync ->
                _uiState.value = _uiState.value.copy(
                    syncState = sync,
                    isSyncing = sync is SyncState.Syncing
                )
            }
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        _uiState.value = _uiState.value.copy(currentTheme = mode)
        val domainTheme = when (mode) {
            AppThemeMode.LIGHT -> com.studycompanion.app.domain.model.ThemeMode.LIGHT
            AppThemeMode.DARK -> com.studycompanion.app.domain.model.ThemeMode.DARK
            AppThemeMode.AMOLED -> com.studycompanion.app.domain.model.ThemeMode.AMOLED
            AppThemeMode.SYSTEM -> com.studycompanion.app.domain.model.ThemeMode.SYSTEM
        }
        updateSettings(_uiState.value.settings.copy(themeMode = domainTheme))
    }

    fun setCountWhileLocked(enabled: Boolean) {
        val updated = _uiState.value.settings.copy(countWhileLocked = enabled)
        updateSettings(updated)
    }

    fun setPauseDuringCalls(enabled: Boolean) {
        val updated = _uiState.value.settings.copy(pauseDuringCalls = enabled)
        updateSettings(updated)
    }

    fun setPauseInMultiWindow(enabled: Boolean) {
        val updated = _uiState.value.settings.copy(pauseInMultiWindow = enabled)
        updateSettings(updated)
    }

    private fun updateSettings(newSettings: ProfileSettings) {
        _uiState.value = _uiState.value.copy(settings = newSettings)
        viewModelScope.launch {
            val profId = newSettings.profileId.ifBlank {
                _uiState.value.activeProfile?.id
                    ?: profileRepository.getActiveProfileSync()?.id
                    ?: ""
            }
            if (profId.isNotBlank()) {
                profileRepository.updateProfileSettings(newSettings.copy(profileId = profId))
            }
        }
    }

    fun triggerSync() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true)
            syncEngine.sync()
            _uiState.value = _uiState.value.copy(isSyncing = false)
        }
    }

    fun lockProfile() {
        viewModelScope.launch {
            profileRepository.lockActiveProfile()
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    fun deleteAccount(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            authRepository.deleteAccount()
            onComplete()
        }
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val profileRepository: ProfileRepository,
        private val targetRepository: TargetRepository,
        private val sessionDataStore: UserSessionDataStore,
        private val syncEngine: SyncEngine
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(
                authRepository,
                profileRepository,
                targetRepository,
                sessionDataStore,
                syncEngine
            ) as T
        }
    }
}
