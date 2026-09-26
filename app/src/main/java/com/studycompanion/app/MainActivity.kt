package com.studycompanion.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.studycompanion.app.core.ui.component.MainDestination
import com.studycompanion.app.core.ui.component.StudyBottomNavigation
import com.studycompanion.app.core.ui.theme.StudyCompanionTheme
import com.studycompanion.app.core.ui.theme.StudyTheme
import com.studycompanion.app.domain.repository.AuthState
import com.studycompanion.app.feature.focus.FocusScreen
import com.studycompanion.app.feature.focus.FocusViewModel
import com.studycompanion.app.feature.health.PermissionHealthScreen
import com.studycompanion.app.feature.health.PermissionHealthViewModel
import com.studycompanion.app.feature.history.HistoryScreen
import com.studycompanion.app.feature.history.HistoryViewModel
import com.studycompanion.app.feature.home.HomeScreen
import com.studycompanion.app.feature.home.HomeViewModel
import com.studycompanion.app.feature.onboarding.OnboardingScreen
import com.studycompanion.app.feature.onboarding.OnboardingViewModel
import com.studycompanion.app.feature.profile.ProfileSwitchDialog
import com.studycompanion.app.feature.settings.SettingsScreen
import com.studycompanion.app.feature.settings.SettingsViewModel
import com.studycompanion.app.feature.statistics.StatisticsScreen
import com.studycompanion.app.feature.statistics.StatisticsViewModel
import com.studycompanion.app.feature.studyapps.StudyAppManagerScreen
import com.studycompanion.app.feature.studyapps.StudyAppManagerViewModel
import com.studycompanion.app.tracking.service.FocusTrackingService

enum class SubDestination {
    NONE,
    FOCUS,
    STUDY_APPS,
    HEALTH_CENTER
}

class MainActivity : ComponentActivity() {

    private val onboardingViewModel: OnboardingViewModel by viewModels {
        val app = application as StudyCompanionApplication
        val c = app.container
        OnboardingViewModel.Factory(
            c.authRepository,
            c.profileRepository,
            c.targetRepository,
            c.studyAppRepository
        )
    }

    private val homeViewModel: HomeViewModel by viewModels {
        val app = application as StudyCompanionApplication
        val c = app.container
        HomeViewModel.Factory(
            c.authRepository,
            c.profileRepository,
            c.targetRepository,
            c.studyAppRepository,
            c.statsRepository,
            c.focusEngine,
            c.syncEngine
        )
    }

    private val focusViewModel: FocusViewModel by viewModels {
        val app = application as StudyCompanionApplication
        val c = app.container
        FocusViewModel.Factory(
            c.focusEngine,
            c.profileRepository,
            c.targetRepository,
            c.studyAppRepository,
            c.statsRepository
        )
    }

    private val historyViewModel: HistoryViewModel by viewModels {
        val app = application as StudyCompanionApplication
        val c = app.container
        HistoryViewModel.Factory(
            c.sessionRepository,
            c.profileRepository,
            c.statsRepository,
            c.studyAppRepository
        )
    }

    private val statisticsViewModel: StatisticsViewModel by viewModels {
        val app = application as StudyCompanionApplication
        val c = app.container
        StatisticsViewModel.Factory(
            c.profileRepository,
            c.statsRepository
        )
    }

    private val studyAppManagerViewModel: StudyAppManagerViewModel by viewModels {
        val app = application as StudyCompanionApplication
        val c = app.container
        StudyAppManagerViewModel.Factory(
            c.profileRepository,
            c.studyAppRepository
        )
    }

    private val settingsViewModel: SettingsViewModel by viewModels {
        val app = application as StudyCompanionApplication
        val c = app.container
        SettingsViewModel.Factory(
            c.authRepository,
            c.profileRepository,
            c.targetRepository,
            c.sessionDataStore,
            c.syncEngine
        )
    }

    private val permissionHealthViewModel: PermissionHealthViewModel by viewModels {
        val app = application as StudyCompanionApplication
        val c = app.container
        PermissionHealthViewModel.Factory(
            applicationContext,
            c.syncEngine
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val onboardingState by onboardingViewModel.uiState.collectAsState()
            val settingsState by settingsViewModel.uiState.collectAsState()

            StudyCompanionTheme(themeMode = settingsState.currentTheme) {
                val isAuthenticated = onboardingState.authState is AuthState.Authenticated &&
                        onboardingState.activeProfile != null

                LaunchedEffect(isAuthenticated) {
                    if (isAuthenticated) {
                        try {
                            FocusTrackingService.startService(applicationContext)
                        } catch (e: Exception) {
                            // Service start may be deferred if permission pending
                        }
                    }
                }

                if (!isAuthenticated) {
                    OnboardingScreen(
                        state = onboardingState,
                        onSignUp = { email, pass -> onboardingViewModel.signUp(email, pass) },
                        onLogin = { email, pass -> onboardingViewModel.login(email, pass) },
                        onCreateProfile = { name, pin -> onboardingViewModel.createProfile(name, pin) },
                        onSetDailyTarget = { seconds -> onboardingViewModel.setDailyTarget(seconds) },
                        onAdjustDailyTarget = { seconds -> onboardingViewModel.adjustDailyTarget(seconds) },
                        onAddStudyApp = { pkg, label -> onboardingViewModel.addStudyApp(pkg, label) },
                        onRemoveStudyApp = { pkg -> onboardingViewModel.removeStudyApp(pkg) },
                        onToggleStudyApp = { pkg, enabled -> onboardingViewModel.setAppEnabled(pkg, enabled) },
                        onSwitchProfile = { profId, pin -> onboardingViewModel.switchProfile(profId, pin) },
                        onLockProfile = { onboardingViewModel.lockProfile() },
                        onLogout = { onboardingViewModel.logout() },
                        onNavigateStep = { step -> onboardingViewModel.navigateToStep(step) },
                        onLaunchPoc = { /* POC deprecated in favor of full Phase 4 app */ }
                    )
                } else {
                    StudyCompanionAppContent()
                }
            }
        }
    }

    @Composable
    private fun StudyCompanionAppContent() {
        var currentDestination by rememberSaveable { mutableStateOf(MainDestination.HOME) }
        var currentSubDestination by rememberSaveable { mutableStateOf(SubDestination.NONE) }
        var showProfileSwitchDialog by remember { mutableStateOf(false) }

        val onboardingState by onboardingViewModel.uiState.collectAsState()
        val homeState by homeViewModel.uiState.collectAsState()
        val focusState by focusViewModel.uiState.collectAsState()
        val historyState by historyViewModel.uiState.collectAsState()
        val statisticsState by statisticsViewModel.uiState.collectAsState()
        val studyAppsState by studyAppManagerViewModel.uiState.collectAsState()
        val settingsState by settingsViewModel.uiState.collectAsState()
        val healthState by permissionHealthViewModel.uiState.collectAsState()

        BackHandler(enabled = true) {
            when {
                showProfileSwitchDialog -> showProfileSwitchDialog = false
                currentSubDestination != SubDestination.NONE -> currentSubDestination = SubDestination.NONE
                currentDestination != MainDestination.HOME -> currentDestination = MainDestination.HOME
                else -> finish()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(StudyTheme.colors.background)
        ) {
            when (currentSubDestination) {
                SubDestination.FOCUS -> {
                    FocusScreen(
                        state = focusState,
                        onPause = { focusViewModel.pauseFocus() },
                        onResume = { focusViewModel.resumeFocus() },
                        onExit = { currentSubDestination = SubDestination.NONE },
                        onToggleDisplayMode = { focusViewModel.toggleDisplayMode() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                SubDestination.STUDY_APPS -> {
                    StudyAppManagerScreen(
                        state = studyAppsState,
                        onBack = { currentSubDestination = SubDestination.NONE },
                        onSearchQueryChange = { q -> studyAppManagerViewModel.setSearchQuery(q) },
                        onAddApp = { pkg, label -> studyAppManagerViewModel.addStudyApp(pkg, label) },
                        onRemoveApp = { pkg -> studyAppManagerViewModel.removeStudyApp(pkg) },
                        onSetAppEnabled = { pkg, en -> studyAppManagerViewModel.setAppEnabled(pkg, en) },
                        onStartEditingApp = { app -> studyAppManagerViewModel.startEditingLabel(app) },
                        onCancelEditingApp = { studyAppManagerViewModel.cancelEditingLabel() },
                        onSaveAppLabel = { pkg, label -> studyAppManagerViewModel.saveAppLabel(pkg, label) },
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                    )
                }
                SubDestination.HEALTH_CENTER -> {
                    PermissionHealthScreen(
                        state = healthState,
                        onBack = { currentSubDestination = SubDestination.NONE },
                        onTriggerSync = { permissionHealthViewModel.triggerSync() },
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                    )
                }
                SubDestination.NONE -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            AnimatedContent(
                                targetState = currentDestination,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "MainTabTransition"
                            ) { destination ->
                                when (destination) {
                                    MainDestination.HOME -> {
                                        HomeScreen(
                                            state = homeState,
                                            onStartFocus = {
                                                homeViewModel.startFocus()
                                                try {
                                                    FocusTrackingService.startService(applicationContext)
                                                } catch (e: Exception) {}
                                            },
                                            onPauseFocus = {
                                                homeViewModel.pauseFocus()
                                            },
                                            onResumeFocus = {
                                                homeViewModel.resumeFocus()
                                            },
                                            onOpenFocusView = {
                                                currentSubDestination = SubDestination.FOCUS
                                            },
                                            onOpenStudyApps = {
                                                currentSubDestination = SubDestination.STUDY_APPS
                                            },
                                            onOpenHealthCenter = {
                                                currentSubDestination = SubDestination.HEALTH_CENTER
                                            },
                                            onOpenProfileSwitch = {
                                                showProfileSwitchDialog = true
                                            },
                                            onSetTarget = { sec ->
                                                homeViewModel.setTarget(sec)
                                            }
                                        )
                                    }
                                    MainDestination.HISTORY -> {
                                        HistoryScreen(
                                            state = historyState,
                                            onSelectDate = { date -> historyViewModel.selectDate(date) },
                                            onPreviousDay = { historyViewModel.previousDay() },
                                            onNextDay = { historyViewModel.nextDay() },
                                            onOpenSessionDetail = { session -> historyViewModel.openSessionDetail(session) },
                                            onCloseSessionDetail = { historyViewModel.closeSessionDetail() },
                                            onShowAddManual = { historyViewModel.showAddManualDialog() },
                                            onHideAddManual = { historyViewModel.hideAddManualDialog() },
                                            onAddManualSession = { start, end, subj ->
                                                historyViewModel.addManualSession(start, end, subj)
                                            },
                                            onDeleteSession = { sessionId ->
                                                historyViewModel.deleteSession(sessionId)
                                            }
                                        )
                                    }
                                    MainDestination.STATISTICS -> {
                                        StatisticsScreen(
                                            state = statisticsState,
                                            onPreviousMonth = { statisticsViewModel.previousMonth() },
                                            onNextMonth = { statisticsViewModel.nextMonth() },
                                            onSelectDate = { dateKey -> statisticsViewModel.selectDate(dateKey) }
                                        )
                                    }
                                    MainDestination.SETTINGS -> {
                                        SettingsScreen(
                                            state = settingsState,
                                            onNavigateStudyApps = {
                                                currentSubDestination = SubDestination.STUDY_APPS
                                            },
                                            onNavigateHealthCenter = {
                                                currentSubDestination = SubDestination.HEALTH_CENTER
                                            },
                                            onSelectTheme = { mode ->
                                                settingsViewModel.setThemeMode(mode)
                                            },
                                            onToggleCountWhileLocked = { en ->
                                                settingsViewModel.setCountWhileLocked(en)
                                            },
                                            onTogglePauseDuringCalls = { en ->
                                                settingsViewModel.setPauseDuringCalls(en)
                                            },
                                            onTogglePauseInMultiWindow = { en ->
                                                settingsViewModel.setPauseInMultiWindow(en)
                                            },
                                            onTriggerSync = {
                                                settingsViewModel.triggerSync()
                                            },
                                            onSwitchProfile = {
                                                showProfileSwitchDialog = true
                                            },
                                            onLockProfile = {
                                                settingsViewModel.lockProfile()
                                            },
                                            onLogout = {
                                                settingsViewModel.logout()
                                            },
                                            onDeleteAccount = {
                                                settingsViewModel.deleteAccount()
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        StudyBottomNavigation(
                            currentDestination = currentDestination,
                            onNavigate = { destination -> currentDestination = destination },
                            modifier = Modifier.navigationBarsPadding()
                        )
                    }
                }
            }

            if (showProfileSwitchDialog) {
                ProfileSwitchDialog(
                    profiles = onboardingState.userProfiles,
                    activeProfileId = onboardingState.activeProfile?.id,
                    onDismiss = { showProfileSwitchDialog = false },
                    onSwitchProfile = { profileId, pin ->
                        onboardingViewModel.switchProfile(profileId, pin)
                        showProfileSwitchDialog = false
                    },
                    onCreateProfile = { name, pin ->
                        onboardingViewModel.createProfile(name, pin)
                        showProfileSwitchDialog = false
                    }
                )
            }
        }
    }
}
