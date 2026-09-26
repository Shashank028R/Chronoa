package com.studycompanion.app.domain.model

enum class TrackingType {
    AUTOMATIC,
    MANUAL,
    AUTOMATIC_MODIFIED
}

enum class VerificationStatus {
    VERIFIED_BY_RULES,
    MANUAL_UNVERIFIED,
    NEEDS_REVIEW
}

enum class FocusDisplayMode {
    MINIMAL,
    CIRCULAR_RING,
    EXPANDED
}

enum class AnimationLevel {
    FULL,
    STANDARD,
    REDUCED,
    OFF
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    AMOLED
}

data class User(
    val id: String,
    val email: String,
    val authProvider: String = "EMAIL",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

data class Profile(
    val id: String,
    val userId: String,
    val name: String,
    val avatarRef: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

data class ProfileSettings(
    val profileId: String,
    val countWhileLocked: Boolean = true,
    val pauseDuringCalls: Boolean = true,
    val blockNotifications: Boolean = false,
    val pauseInMultiWindow: Boolean = true,
    val pauseInFloatingWindow: Boolean = true,
    val defaultDailyTargetSeconds: Long = 10800L, // 3 hours default
    val focusDisplayMode: FocusDisplayMode = FocusDisplayMode.MINIMAL,
    val animationLevel: AnimationLevel = AnimationLevel.STANDARD,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val updatedAt: Long = System.currentTimeMillis()
)

data class DailyTarget(
    val id: String,
    val profileId: String,
    val dateKey: String, // YYYY-MM-DD
    val originalTargetSeconds: Long,
    val adjustedTargetSeconds: Long? = null,
    val carryInSeconds: Long = 0L,
    val carryOutSeconds: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Effective target for today.
     * If user explicitly adjusted today's target, use adjustedTargetSeconds + carryInSeconds.
     * Otherwise originalTargetSeconds + carryInSeconds.
     */
    val effectiveTargetSeconds: Long
        get() = (adjustedTargetSeconds ?: originalTargetSeconds) + carryInSeconds

    /**
     * Calculates remaining study time for today.
     */
    fun calculateRemainingSeconds(studiedSeconds: Long): Long {
        return maxOf(0L, effectiveTargetSeconds - studiedSeconds)
    }

    /**
     * Proposes carry-over time for unfinished targets (optional offer, never auto-applied).
     */
    fun proposeCarryOverSeconds(actualStudiedSeconds: Long): Long {
        return if (actualStudiedSeconds < effectiveTargetSeconds) {
            effectiveTargetSeconds - actualStudiedSeconds
        } else {
            0L
        }
    }
}

data class StudyApp(
    val id: String,
    val profileId: String,
    val packageName: String,
    val appLabel: String,
    val iconRef: String? = null,
    val isEnabled: Boolean = true,
    val addedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val customLabel: String get() = appLabel
}

data class StudySession(
    val id: String,
    val profileId: String,
    val deviceId: String,
    val platform: String = "ANDROID",
    val packageName: String? = null,
    val startAt: Long,
    val endAt: Long,
    val durationSeconds: Long = maxOf(0L, (endAt - startAt) / 1000L),
    val trackingType: TrackingType = TrackingType.AUTOMATIC,
    val verificationStatus: VerificationStatus = VerificationStatus.VERIFIED_BY_RULES,
    val subjectId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncVersion: Long = 1L,
    val syncState: String = "LOCAL"
)

data class DailyStats(
    val id: String,
    val profileId: String,
    val dateKey: String,
    val automaticSeconds: Long = 0L,
    val manualSeconds: Long = 0L,
    val totalSeconds: Long = automaticSeconds + manualSeconds,
    val effectiveTargetSeconds: Long = 0L,
    val sessionCount: Int = 0,
    val longestSessionSeconds: Long = 0L,
    val streakContribution: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

data class SessionEvent(
    val id: String,
    val sessionId: String? = null,
    val profileId: String,
    val deviceId: String,
    val eventType: String,
    val timestamp: Long,
    val packageName: String? = null,
    val engineState: String,
    val reasonCode: String? = null,
    val source: String,
    val metadataJson: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class AuthSession(
    val userId: String,
    val token: String,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null,
    val isAuthenticated: Boolean = true
)
