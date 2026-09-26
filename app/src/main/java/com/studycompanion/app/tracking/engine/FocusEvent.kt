package com.studycompanion.app.tracking.engine

import com.studycompanion.app.domain.model.ProfileSettings

/**
 * System and user events fed into the FocusEngine state machine.
 */
sealed interface FocusEvent {
    val timestamp: Long

    data class AppResumed(
        val packageName: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : FocusEvent

    data class AppPaused(
        val packageName: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : FocusEvent

    data class KeyguardShown(
        override val timestamp: Long = System.currentTimeMillis()
    ) : FocusEvent

    data class KeyguardHidden(
        override val timestamp: Long = System.currentTimeMillis()
    ) : FocusEvent

    data class ScreenInteractive(
        override val timestamp: Long = System.currentTimeMillis()
    ) : FocusEvent

    data class ScreenNonInteractive(
        override val timestamp: Long = System.currentTimeMillis()
    ) : FocusEvent

    data class CallStarted(
        override val timestamp: Long = System.currentTimeMillis()
    ) : FocusEvent

    data class CallEnded(
        override val timestamp: Long = System.currentTimeMillis()
    ) : FocusEvent

    data class WindowModeChanged(
        val inMultiWindow: Boolean,
        val inFloating: Boolean,
        override val timestamp: Long = System.currentTimeMillis()
    ) : FocusEvent

    data class ProfileSwitched(
        val profileId: String,
        val approvedPackages: Set<String>,
        val settings: ProfileSettings,
        override val timestamp: Long = System.currentTimeMillis()
    ) : FocusEvent

    data class StudyAppsUpdated(
        val approvedPackages: Set<String>,
        override val timestamp: Long = System.currentTimeMillis()
    ) : FocusEvent

    data class SettingsUpdated(
        val settings: ProfileSettings,
        override val timestamp: Long = System.currentTimeMillis()
    ) : FocusEvent

    data class PermissionChanged(
        val hasUsageAccess: Boolean,
        override val timestamp: Long = System.currentTimeMillis()
    ) : FocusEvent

    data class UserStart(
        override val timestamp: Long = System.currentTimeMillis()
    ) : FocusEvent

    data class UserPause(
        override val timestamp: Long = System.currentTimeMillis()
    ) : FocusEvent

    data class UserResume(
        override val timestamp: Long = System.currentTimeMillis()
    ) : FocusEvent

    data class UserStop(
        override val timestamp: Long = System.currentTimeMillis()
    ) : FocusEvent

    data class ClockChanged(
        val oldTimestamp: Long,
        val newTimestamp: Long,
        override val timestamp: Long = newTimestamp
    ) : FocusEvent

    data class TimezoneChanged(
        val oldTimezoneId: String,
        val newTimezoneId: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : FocusEvent

    data class MidnightRolledOver(
        val previousDayBoundary: Long,
        val newDayBoundary: Long,
        override val timestamp: Long = newDayBoundary
    ) : FocusEvent

    data class RecoveryTriggered(
        override val timestamp: Long = System.currentTimeMillis()
    ) : FocusEvent
}
