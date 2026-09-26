package com.studycompanion.app.feature.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.studycompanion.app.domain.model.DailyTarget
import com.studycompanion.app.domain.model.StudyApp
import com.studycompanion.app.domain.model.StudyStatsOverview
import com.studycompanion.app.domain.repository.ProfileRepository
import com.studycompanion.app.domain.repository.StatsRepository
import com.studycompanion.app.domain.repository.StudyAppRepository
import com.studycompanion.app.domain.repository.TargetRepository
import com.studycompanion.app.tracking.engine.FocusEngine
import com.studycompanion.app.tracking.engine.FocusEvent
import com.studycompanion.app.tracking.engine.FocusSnapshot
import com.studycompanion.app.tracking.engine.FocusState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class FocusDisplayMode {
    MINIMAL_AMOLED,
    CLOCK
}

data class FocusUiState(
    val displayMode: FocusDisplayMode = FocusDisplayMode.MINIMAL_AMOLED,
    val focusSnapshot: FocusSnapshot = FocusSnapshot(),
    val currentAppLabel: String? = null,
    val todayTarget: DailyTarget? = null,
    val statsOverview: StudyStatsOverview = StudyStatsOverview()
)

class FocusViewModel(
    private val focusEngine: FocusEngine,
    private val profileRepository: ProfileRepository,
    private val targetRepository: TargetRepository,
    private val studyAppRepository: StudyAppRepository,
    private val statsRepository: StatsRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(FocusUiState())
    val uiState: StateFlow<FocusUiState> = _uiState.asStateFlow()

    private val todayDateKey: String
        get() = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    init {
        observeEngine()
    }

    private fun observeEngine() {
        viewModelScope.launch {
            combine(
                profileRepository.getActiveProfile(),
                focusEngine.state
            ) { profile, snapshot ->
                Pair(profile, snapshot)
            }.collect { (profile, snapshot) ->
                if (profile != null) {
                    val target = targetRepository.getTargetSync(profile.id, todayDateKey)
                    val apps = studyAppRepository.getStudyAppsSync(profile.id)
                    val stats = statsRepository?.getStatsOverview(profile.id, todayDateKey) ?: StudyStatsOverview()
                    val isOwnApp = snapshot.currentPackage == "com.studycompanion.app" ||
                        snapshot.currentPackage == "com.studycompanion.app.debug" ||
                        snapshot.currentPackage?.contains("studycompanion") == true

                    val appLabel = if (isOwnApp) {
                        null
                    } else {
                        apps.find { it.packageName == snapshot.currentPackage }?.customLabel
                            ?: apps.find { it.packageName == snapshot.currentPackage }?.appLabel
                            ?: snapshot.currentPackage?.substringAfterLast('.')?.takeIf { !it.equals("debug", ignoreCase = true) }
                    }

                    _uiState.value = _uiState.value.copy(
                        focusSnapshot = snapshot,
                        currentAppLabel = appLabel,
                        todayTarget = target,
                        statsOverview = stats
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        focusSnapshot = snapshot,
                        currentAppLabel = null,
                        todayTarget = null,
                        statsOverview = StudyStatsOverview()
                    )
                }
            }
        }
    }

    fun setDisplayMode(mode: FocusDisplayMode) {
        _uiState.value = _uiState.value.copy(displayMode = mode)
    }

    fun toggleDisplayMode() {
        val next = if (_uiState.value.displayMode == FocusDisplayMode.MINIMAL_AMOLED) {
            FocusDisplayMode.CLOCK
        } else {
            FocusDisplayMode.MINIMAL_AMOLED
        }
        _uiState.value = _uiState.value.copy(displayMode = next)
    }

    fun pause() {
        focusEngine.onEvent(FocusEvent.UserPause())
    }

    fun pauseFocus() = pause()

    fun resume() {
        focusEngine.onEvent(FocusEvent.UserResume())
    }

    fun resumeFocus() = resume()

    fun stop() {
        focusEngine.onEvent(FocusEvent.UserStop())
    }

    class Factory(
        private val focusEngine: FocusEngine,
        private val profileRepository: ProfileRepository,
        private val targetRepository: TargetRepository,
        private val studyAppRepository: StudyAppRepository,
        private val statsRepository: StatsRepository? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FocusViewModel(
                focusEngine,
                profileRepository,
                targetRepository,
                studyAppRepository,
                statsRepository
            ) as T
        }
    }
}
