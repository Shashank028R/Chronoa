package com.studycompanion.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.studycompanion.app.domain.model.DailyTarget
import com.studycompanion.app.domain.model.Profile
import com.studycompanion.app.domain.model.StudyApp
import com.studycompanion.app.domain.model.StudyStatsOverview
import com.studycompanion.app.domain.repository.AuthRepository
import com.studycompanion.app.domain.repository.AuthState
import com.studycompanion.app.domain.repository.ProfileRepository
import com.studycompanion.app.domain.repository.StatsRepository
import com.studycompanion.app.domain.repository.StudyAppRepository
import com.studycompanion.app.domain.repository.TargetRepository
import com.studycompanion.app.sync.SyncEngine
import com.studycompanion.app.sync.model.SyncState
import com.studycompanion.app.tracking.engine.FocusEngine
import com.studycompanion.app.tracking.engine.FocusEvent
import com.studycompanion.app.tracking.engine.FocusSnapshot
import com.studycompanion.app.tracking.engine.FocusState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class HomePresentationState {
    FIRST_TIME,
    NO_TARGET,
    TARGET_CONFIGURED_IDLE,
    FOCUSING,
    PAUSED_UNAPPROVED_APP,
    PAUSED_HOME,
    PAUSED_USER,
    LOCKED_FOCUS,
    PAUSED_CALL,
    PERMISSION_MISSING,
    SYNCING,
    OFFLINE,
    SYNC_ERROR,
    NO_STUDY_APPS
}

data class HomeUiState(
    val presentationState: HomePresentationState = HomePresentationState.TARGET_CONFIGURED_IDLE,
    val activeProfile: Profile? = null,
    val todayTarget: DailyTarget? = null,
    val studyApps: List<StudyApp> = emptyList(),
    val focusSnapshot: FocusSnapshot = FocusSnapshot(),
    val syncState: SyncState = SyncState.Synced(0L),
    val statsOverview: StudyStatsOverview = StudyStatsOverview(),
    val currentAppLabel: String? = null,
    val showTargetDialog: Boolean = false,
    val showProfileDialog: Boolean = false
)

class HomeViewModel(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val targetRepository: TargetRepository,
    private val studyAppRepository: StudyAppRepository,
    private val statsRepository: StatsRepository,
    private val focusEngine: FocusEngine,
    private val syncEngine: SyncEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var lastSyncedProfileId: String? = null
    private var lastSyncedAppConfig: Set<Pair<String, Boolean>>? = null

    private val todayDateKey: String
        get() = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                profileRepository.getActiveProfile(),
                focusEngine.state,
                syncEngine.syncState
            ) { profile, snapshot, syncState ->
                Triple(profile, snapshot, syncState)
            }.collect { (profile, snapshot, syncState) ->
                if (profile != null) {
                    val target = targetRepository.getTargetSync(profile.id, todayDateKey)
                    val apps = studyAppRepository.getStudyAppsSync(profile.id)
                    val stats = statsRepository.getStatsOverview(profile.id, todayDateKey)
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

                    val currentAppConfig = apps.map { it.packageName to it.isEnabled }.toSet()
                    if (profile.id != lastSyncedProfileId || currentAppConfig != lastSyncedAppConfig) {
                        lastSyncedProfileId = profile.id
                        lastSyncedAppConfig = currentAppConfig
                        val approvedPkgs = apps.filter { it.isEnabled }.map { it.packageName }.toSet()
                        focusEngine.onEvent(
                            FocusEvent.ProfileSwitched(
                                profileId = profile.id,
                                approvedPackages = approvedPkgs,
                                settings = com.studycompanion.app.domain.model.ProfileSettings(profileId = profile.id)
                            )
                        )
                    }

                    val presentation = resolvePresentationState(
                        profile = profile,
                        target = target,
                        apps = apps,
                        snapshot = snapshot,
                        syncState = syncState
                    )

                    _uiState.value = _uiState.value.copy(
                        presentationState = presentation,
                        activeProfile = profile,
                        todayTarget = target,
                        studyApps = apps,
                        focusSnapshot = snapshot,
                        syncState = syncState,
                        statsOverview = stats,
                        currentAppLabel = appLabel
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        presentationState = HomePresentationState.FIRST_TIME,
                        activeProfile = null
                    )
                }
            }
        }
    }

    private fun resolvePresentationState(
        profile: Profile?,
        target: DailyTarget?,
        apps: List<StudyApp>,
        snapshot: FocusSnapshot,
        syncState: SyncState
    ): HomePresentationState {
        if (profile == null) return HomePresentationState.FIRST_TIME
        if (apps.isEmpty()) return HomePresentationState.NO_STUDY_APPS
        if (target == null) return HomePresentationState.NO_TARGET

        // Engine focus states take strict authoritative precedence
        return when (snapshot.state) {
            FocusState.FOCUSING -> HomePresentationState.FOCUSING
            FocusState.LOCKED_FOCUS -> HomePresentationState.LOCKED_FOCUS
            FocusState.PAUSED_UNAPPROVED_APP -> HomePresentationState.PAUSED_UNAPPROVED_APP
            FocusState.PAUSED_HOME -> HomePresentationState.PAUSED_HOME
            FocusState.PAUSED_USER -> HomePresentationState.PAUSED_USER
            FocusState.PAUSED_CALL -> HomePresentationState.PAUSED_CALL
            FocusState.WAITING_FOR_PERMISSION -> HomePresentationState.PERMISSION_MISSING
            FocusState.IDLE, FocusState.READY, FocusState.RECOVERING, FocusState.PAUSED_MULTIWINDOW, FocusState.PAUSED_FLOATING -> {
                when {
                    syncState is SyncState.Syncing -> HomePresentationState.SYNCING
                    syncState is SyncState.Offline -> HomePresentationState.OFFLINE
                    syncState is SyncState.Error -> HomePresentationState.SYNC_ERROR
                    else -> HomePresentationState.TARGET_CONFIGURED_IDLE
                }
            }
            FocusState.ERROR -> HomePresentationState.SYNC_ERROR
        }
    }

    fun startFocus() {
        val profile = _uiState.value.activeProfile
        if (profile != null) {
            val apps = _uiState.value.studyApps
            val approvedPkgs = apps.filter { it.isEnabled }.map { it.packageName }.toSet()
            focusEngine.onEvent(
                FocusEvent.ProfileSwitched(
                    profileId = profile.id,
                    approvedPackages = approvedPkgs,
                    settings = com.studycompanion.app.domain.model.ProfileSettings(profileId = profile.id)
                )
            )
        }
        focusEngine.onEvent(FocusEvent.UserStart())
    }

    fun pauseFocus() {
        focusEngine.onEvent(FocusEvent.UserPause())
    }

    fun resumeFocus() {
        focusEngine.onEvent(FocusEvent.UserResume())
    }

    fun stopFocus() {
        focusEngine.onEvent(FocusEvent.UserStop())
    }

    fun setTarget(seconds: Long) {
        val profileId = _uiState.value.activeProfile?.id ?: return
        viewModelScope.launch {
            if (_uiState.value.todayTarget == null) {
                targetRepository.setDailyTarget(profileId, todayDateKey, seconds)
            } else {
                targetRepository.adjustDailyTarget(profileId, todayDateKey, seconds)
            }
            hideTargetDialog()
            refreshData()
        }
    }

    fun showTargetDialog() {
        _uiState.value = _uiState.value.copy(showTargetDialog = true)
    }

    fun hideTargetDialog() {
        _uiState.value = _uiState.value.copy(showTargetDialog = false)
    }

    fun showProfileDialog() {
        _uiState.value = _uiState.value.copy(showProfileDialog = true)
    }

    fun hideProfileDialog() {
        _uiState.value = _uiState.value.copy(showProfileDialog = false)
    }

    fun refreshData() {
        val profile = _uiState.value.activeProfile ?: return
        viewModelScope.launch {
            val target = targetRepository.getTarget(profile.id, todayDateKey).first()
            val apps = studyAppRepository.getStudyApps(profile.id).first()
            val stats = statsRepository.getStatsOverview(profile.id, todayDateKey)
            _uiState.value = _uiState.value.copy(
                todayTarget = target,
                studyApps = apps,
                statsOverview = stats
            )
        }
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val profileRepository: ProfileRepository,
        private val targetRepository: TargetRepository,
        private val studyAppRepository: StudyAppRepository,
        private val statsRepository: StatsRepository,
        private val focusEngine: FocusEngine,
        private val syncEngine: SyncEngine
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(
                authRepository,
                profileRepository,
                targetRepository,
                studyAppRepository,
                statsRepository,
                focusEngine,
                syncEngine
            ) as T
        }
    }
}
