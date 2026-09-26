package com.studycompanion.app.poc

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Temporary state mapping for Phase 0.2 POC.
 * Note: This is an experimental mapping for platform verification,
 * not the complete production Focus Engine state machine.
 */
enum class PocFocusState(val displayTitle: String) {
    FOCUSING("APPROVED / FOCUSING"),
    PAUSED_UNAPPROVED("UNAPPROVED / PAUSED"),
    PAUSED_HOME("HOME / PAUSED"),
    UNKNOWN("UNKNOWN / WAITING")
}

/**
 * Normalized representation of an observed Android UsageEvent.
 */
data class PocUsageEvent(
    val id: String,
    val eventType: String,
    val eventTypeCode: Int,
    val packageName: String,
    val className: String?,
    val timestampMillis: Long,
    val detectedAtMillis: Long,
    val delayMillis: Long = detectedAtMillis - timestampMillis,
    val resultingState: PocFocusState
) {
    val formattedEventTime: String by lazy {
        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        sdf.format(Date(timestampMillis))
    }

    val formattedDetectedTime: String by lazy {
        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        sdf.format(Date(detectedAtMillis))
    }
}

/**
 * Snapshot of current POC engine state exposed to UI.
 */
data class PocEngineState(
    val isUsageAccessGranted: Boolean = false,
    val currentForegroundPackage: String? = null,
    val currentFocusState: PocFocusState = PocFocusState.UNKNOWN,
    val approvedPackages: Set<String> = setOf(
        "com.android.chrome",
        "com.google.android.youtube",
        "com.adobe.reader"
    ),
    val launcherPackages: Set<String> = emptySet(),
    val recentTimeline: List<PocUsageEvent> = emptyList(),
    val lastPollTimeMillis: Long = 0L,
    val totalEventsProcessed: Int = 0,
    val lastEventDelayMillis: Long = 0L
)
