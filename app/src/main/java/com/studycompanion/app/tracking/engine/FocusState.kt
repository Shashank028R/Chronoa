package com.studycompanion.app.tracking.engine

/**
 * Deterministic states of the Focus Engine as defined in docs/05_FOCUS_ENGINE.md.
 */
enum class FocusState {
    /** Engine is inactive or unstarted. */
    IDLE,

    /** Engine is active and listening, but no study app or pausing condition is active yet. */
    READY,

    /** Active study tracking! An approved Study App is in the foreground. Time is counting. */
    FOCUSING,

    /** Device is locked while countWhileLocked = true and an approved session was active. Time continues counting. */
    LOCKED_FOCUS,

    /** An unapproved third-party application is in the foreground. Tracking is paused. */
    PAUSED_UNAPPROVED_APP,

    /** The Android home screen or app launcher is in the foreground. Tracking is paused. */
    PAUSED_HOME,

    /** An active phone call is ongoing and pauseDuringCalls = true. Tracking is paused. */
    PAUSED_CALL,

    /** Disallowed multi-window / split-screen mode is detected. Tracking is paused. */
    PAUSED_MULTIWINDOW,

    /** Disallowed floating / freeform window mode is detected. Tracking is paused. */
    PAUSED_FLOATING,

    /** User manually paused tracking. */
    PAUSED_USER,

    /** PACKAGE_USAGE_STATS permission is missing or was revoked by the user. */
    WAITING_FOR_PERMISSION,

    /** Engine is restarting and reconstructing state from persistent storage and usage events. */
    RECOVERING,

    /** An unrecoverable internal error occurred. */
    ERROR;

    /**
     * Returns true if this state represents active study time accumulation.
     */
    val isCounting: Boolean
        get() = this == FOCUSING || this == LOCKED_FOCUS

    /**
     * Returns true if this state is eligible for study tracking.
     */
    val isEligible: Boolean
        get() = this == FOCUSING || this == LOCKED_FOCUS

    /**
     * Returns true if this state represents a paused condition.
     */
    val isPaused: Boolean
        get() = this == PAUSED_UNAPPROVED_APP ||
                this == PAUSED_HOME ||
                this == PAUSED_CALL ||
                this == PAUSED_MULTIWINDOW ||
                this == PAUSED_FLOATING ||
                this == PAUSED_USER
}
