package com.studycompanion.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.studycompanion.app.domain.model.DailyTarget
import com.studycompanion.app.domain.model.Profile
import com.studycompanion.app.domain.model.StudyApp
import com.studycompanion.app.domain.model.User
import com.studycompanion.app.domain.repository.AuthRepository
import com.studycompanion.app.domain.repository.AuthState
import com.studycompanion.app.domain.repository.ProfileRepository
import com.studycompanion.app.domain.repository.SelectableApp
import com.studycompanion.app.domain.repository.StudyAppRepository
import com.studycompanion.app.domain.repository.TargetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class OnboardingStep {
    AUTH,
    PROFILE_CREATE,
    TARGET_SETUP,
    STUDY_APPS_SETUP,
    DASHBOARD
}

data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.AUTH,
    val authState: AuthState = AuthState.Unauthenticated,
    val activeProfile: Profile? = null,
    val userProfiles: List<Profile> = emptyList(),
    val todayTarget: DailyTarget? = null,
    val studyApps: List<StudyApp> = emptyList(),
    val availableApps: List<SelectableApp> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class OnboardingViewModel(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val targetRepository: TargetRepository,
    private val studyAppRepository: StudyAppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val todayDateKey: String
        get() = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    init {
        observeAuthState()
        observeActiveProfile()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.authState.collect { auth ->
                _uiState.value = _uiState.value.copy(authState = auth)
                if (auth is AuthState.Authenticated) {
                    observeProfilesForUser(auth.user.id)
                } else {
                    _uiState.value = _uiState.value.copy(
                        currentStep = OnboardingStep.AUTH,
                        activeProfile = null,
                        userProfiles = emptyList(),
                        successMessage = null,
                        errorMessage = null
                    )
                }
            }
        }
    }

    private fun observeProfilesForUser(userId: String) {
        viewModelScope.launch {
            profileRepository.getProfiles(userId).collect { profiles ->
                _uiState.value = _uiState.value.copy(userProfiles = profiles)
                if (profiles.isEmpty()) {
                    _uiState.value = _uiState.value.copy(currentStep = OnboardingStep.PROFILE_CREATE)
                }
            }
        }
    }

    private fun observeActiveProfile() {
        viewModelScope.launch {
            profileRepository.getActiveProfile().collect { activeProfile ->
                _uiState.value = _uiState.value.copy(activeProfile = activeProfile)
                if (activeProfile != null) {
                    observeTarget(activeProfile.id)
                    observeStudyApps(activeProfile.id)
                    loadAvailableApps(activeProfile.id)
                }
            }
        }
    }

    private fun observeTarget(profileId: String) {
        viewModelScope.launch {
            targetRepository.getTarget(profileId, todayDateKey).collect { target ->
                _uiState.value = _uiState.value.copy(todayTarget = target)
            }
        }
    }

    private fun observeStudyApps(profileId: String) {
        viewModelScope.launch {
            studyAppRepository.getStudyApps(profileId).collect { apps ->
                _uiState.value = _uiState.value.copy(studyApps = apps)
            }
        }
    }

    fun loadAvailableApps(profileId: String) {
        viewModelScope.launch {
            try {
                val available = studyAppRepository.getAvailableLaunchableApps(profileId)
                _uiState.value = _uiState.value.copy(availableApps = available)
            } catch (e: Exception) {
                // Ignore in testing or environments without launchable apps
            }
        }
    }

    fun signUp(email: String, pass: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            val result = authRepository.signUp(email, pass)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        currentStep = OnboardingStep.PROFILE_CREATE,
                        successMessage = "Account created successfully",
                        errorMessage = null
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = err.message ?: "Sign up failed",
                        successMessage = null
                    )
                }
            )
        }
    }

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            val result = authRepository.login(email, pass)
            result.fold(
                onSuccess = { user ->
                    val profiles = profileRepository.getProfiles(user.id).firstOrNull() ?: emptyList()
                    if (profiles.isNotEmpty()) {
                        val active = profiles.first()
                        profileRepository.activateProfile(active.id)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            activeProfile = active,
                            userProfiles = profiles,
                            currentStep = OnboardingStep.DASHBOARD,
                            successMessage = "Logged in successfully",
                            errorMessage = null
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            userProfiles = emptyList(),
                            currentStep = OnboardingStep.PROFILE_CREATE,
                            successMessage = "Logged in successfully",
                            errorMessage = null
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = err.message ?: "Invalid email or password",
                        successMessage = null
                    )
                }
            )
        }
    }

    fun createProfile(name: String, pin: String) {
        val auth = _uiState.value.authState
        if (auth !is AuthState.Authenticated) {
            _uiState.value = _uiState.value.copy(errorMessage = "Must be logged in to create profile")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = profileRepository.createProfile(auth.user.id, name, pin)
            result.fold(
                onSuccess = { createdProfile ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        currentStep = OnboardingStep.TARGET_SETUP,
                        successMessage = "Profile '${createdProfile.name}' created"
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = err.message ?: "Failed to create profile"
                    )
                }
            )
        }
    }

    fun setDailyTarget(targetSeconds: Long) {
        val profileId = _uiState.value.activeProfile?.id ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = targetRepository.setDailyTarget(profileId, todayDateKey, targetSeconds)
            result.fold(
                onSuccess = { target ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        todayTarget = target,
                        currentStep = OnboardingStep.STUDY_APPS_SETUP,
                        successMessage = "Target set"
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = err.message ?: "Failed to set target"
                    )
                }
            )
        }
    }

    fun adjustDailyTarget(newSeconds: Long) {
        val profileId = _uiState.value.activeProfile?.id ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = targetRepository.adjustDailyTarget(profileId, todayDateKey, newSeconds)
            result.fold(
                onSuccess = { target ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        todayTarget = target,
                        successMessage = "Target adjusted (Original target preserved)"
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = err.message ?: "Failed to adjust target"
                    )
                }
            )
        }
    }

    fun addStudyApp(packageName: String, label: String) {
        val profileId = _uiState.value.activeProfile?.id ?: return
        viewModelScope.launch {
            studyAppRepository.addStudyApp(profileId, packageName, label)
            loadAvailableApps(profileId)
        }
    }

    fun removeStudyApp(packageName: String) {
        val profileId = _uiState.value.activeProfile?.id ?: return
        viewModelScope.launch {
            studyAppRepository.removeStudyApp(profileId, packageName)
            loadAvailableApps(profileId)
        }
    }

    fun setAppEnabled(packageName: String, enabled: Boolean) {
        val profileId = _uiState.value.activeProfile?.id ?: return
        viewModelScope.launch {
            studyAppRepository.setAppEnabled(profileId, packageName, enabled)
        }
    }

    fun switchProfile(profileId: String, pin: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = profileRepository.switchProfile(profileId, pin)
            result.fold(
                onSuccess = { profile ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        currentStep = OnboardingStep.DASHBOARD,
                        successMessage = "Switched to profile: ${profile.name}"
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = err.message ?: "PIN verification failed"
                    )
                }
            )
        }
    }

    fun lockProfile() {
        viewModelScope.launch {
            profileRepository.lockActiveProfile()
        }
    }

    fun logout() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    fun navigateToStep(step: OnboardingStep) {
        _uiState.value = _uiState.value.copy(currentStep = step)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val profileRepository: ProfileRepository,
        private val targetRepository: TargetRepository,
        private val studyAppRepository: StudyAppRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OnboardingViewModel(
                authRepository,
                profileRepository,
                targetRepository,
                studyAppRepository
            ) as T
        }
    }
}
