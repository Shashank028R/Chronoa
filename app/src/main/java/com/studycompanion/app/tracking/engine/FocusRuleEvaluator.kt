package com.studycompanion.app.tracking.engine

/**
 * Snapshot of all environmental and configuration parameters required to evaluate focus eligibility.
 */
data class EvaluationContext(
    val hasUsageAccess: Boolean = true,
    val isTrackingStarted: Boolean = false,
    val isUserStopped: Boolean = false,
    val isUserPaused: Boolean = false,
    val isCallActive: Boolean = false,
    val pauseDuringCalls: Boolean = true,
    val inMultiWindow: Boolean = false,
    val pauseInMultiWindow: Boolean = true,
    val inFloatingWindow: Boolean = false,
    val pauseInFloatingWindow: Boolean = true,
    val isKeyguardLocked: Boolean = false,
    val isScreenInteractive: Boolean = true,
    val countWhileLocked: Boolean = true,
    val activeProfileId: String? = null,
    val currentPackage: String? = null,
    val ownPackageName: String? = null,
    val approvedPackages: Set<String> = emptySet(),
    val launcherPackages: Set<String> = emptySet(),
    val previousState: FocusState = FocusState.IDLE
)

/**
 * Pure, deterministic rule evaluator implementing the documented precedence from docs/05_FOCUS_ENGINE.md:
 * 1. Missing critical permission (Usage Access) -> WAITING_FOR_PERMISSION
 * 2. Explicit User Stop / Pause -> IDLE / PAUSED_USER
 * 3. Active Call (when pauseDuringCalls = true) -> PAUSED_CALL
 * 4. Disallowed Multi-Window / Floating Window -> PAUSED_MULTIWINDOW / PAUSED_FLOATING
 * 5. Screen Lock / Non-interactive state -> LOCKED_FOCUS (if countWhileLocked = true) or PAUSED_HOME
 * 6. Foreground Package:
 *    - Launcher / Home -> PAUSED_HOME
 *    - Study Companion (own app) -> FOCUSING
 *    - Approved Study App -> FOCUSING
 *    - Unapproved App -> PAUSED_UNAPPROVED_APP
 * 7. Default fallback -> READY or IDLE
 */
object FocusRuleEvaluator {

    fun evaluate(context: EvaluationContext): FocusState {
        // Rule 1: Permission readiness
        if (!context.hasUsageAccess) {
            return FocusState.WAITING_FOR_PERMISSION
        }

        // Rule 2: Tracking active state & explicit user stop/pause
        if (context.isUserStopped) {
            return FocusState.IDLE
        }
        if (context.isUserPaused) {
            return FocusState.PAUSED_USER
        }
        if (!context.isTrackingStarted) {
            return FocusState.IDLE
        }

        // Rule 3: Active call interruption
        if (context.isCallActive && context.pauseDuringCalls) {
            return FocusState.PAUSED_CALL
        }

        // Rule 4: Multi-window / floating window interruption
        if (context.inMultiWindow && context.pauseInMultiWindow) {
            return FocusState.PAUSED_MULTIWINDOW
        }
        if (context.inFloatingWindow && context.pauseInFloatingWindow) {
            return FocusState.PAUSED_FLOATING
        }

        // Rule 5: Lock screen / non-interactive screen
        val isDeviceLocked = context.isKeyguardLocked || !context.isScreenInteractive
        if (isDeviceLocked) {
            return if (context.countWhileLocked) {
                // Device is locked and user allows counting while locked:
                // If previously focusing or currently in approved app / own app, continue as LOCKED_FOCUS
                val wasFocusing = context.previousState.isCounting
                val isCurrentlyApproved = context.currentPackage != null &&
                        (context.approvedPackages.contains(context.currentPackage) ||
                         context.currentPackage == context.ownPackageName ||
                         context.currentPackage == "com.studycompanion.app" ||
                         context.currentPackage == "com.studycompanion.app.debug")

                if (wasFocusing || isCurrentlyApproved) {
                    FocusState.LOCKED_FOCUS
                } else {
                    FocusState.PAUSED_HOME
                }
            } else {
                // countWhileLocked is disabled -> pause on lock screen
                FocusState.PAUSED_HOME
            }
        }

        // Rule 6: Foreground application resolution
        val pkg = context.currentPackage
        if (pkg == null) {
            return if (context.previousState == FocusState.IDLE || context.previousState.isPaused) FocusState.READY else context.previousState
        }

        // Is launcher / home?
        if (context.launcherPackages.contains(pkg)) {
            return FocusState.PAUSED_HOME
        }

        // Is Study Companion itself (active in-app dashboard / focus view)?
        val isOwnApp = pkg == context.ownPackageName ||
                pkg == "com.studycompanion.app" ||
                pkg == "com.studycompanion.app.debug"
        if (isOwnApp) {
            return FocusState.FOCUSING
        }

        // Is user-approved Study App?
        if (context.approvedPackages.contains(pkg)) {
            return FocusState.FOCUSING
        }

        // Otherwise: unapproved application
        return FocusState.PAUSED_UNAPPROVED_APP
    }
}
