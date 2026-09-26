package com.studycompanion.app.poc

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.LinkedHashSet

/**
 * Isolated proof-of-concept engine for validating UsageStatsManager / UsageEvents.
 * This is an experimental probe for Phase 0.2, NOT the final production Focus Engine.
 */
class UsageEventsPocEngine(private val context: Context) {

    private val TAG = "UsageEventsPocEngine"

    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    private val _state = MutableStateFlow(PocEngineState())
    val state: StateFlow<PocEngineState> = _state.asStateFlow()

    private var pollingJob: Job? = null

    // Deduplication tracker: bounded set of event signatures
    private val processedEventKeys = LinkedHashSet<String>()
    private val maxProcessedKeys = 1000

    // High watermark for queried events
    private var lastProcessedTimestamp = 0L

    init {
        // Initial permission and launcher check
        val granted = checkUsageAccessPermission()
        val launchers = resolveLauncherPackages()
        _state.update {
            it.copy(
                isUsageAccessGranted = granted,
                launcherPackages = launchers
            )
        }
    }

    /**
     * Checks whether PACKAGE_USAGE_STATS is granted via AppOps.
     */
    fun checkUsageAccessPermission(): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            Log.e(TAG, "Error checking usage access permission", e)
            false
        }
    }

    /**
     * Dynamically determines current home/launcher packages on the device.
     */
    fun resolveLauncherPackages(): Set<String> {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            }
            val launchers = resolveInfos.mapNotNull { it.activityInfo?.packageName }.toSet()
            Log.d(TAG, "Resolved launcher packages: $launchers")
            launchers
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving launcher packages", e)
            emptySet()
        }
    }

    /**
     * Add a package to approved list.
     */
    fun addApprovedPackage(pkg: String) {
        val trimmed = pkg.trim()
        if (trimmed.isNotEmpty()) {
            _state.update { current ->
                val updated = current.approvedPackages + trimmed
                val newState = recomputeState(current.currentForegroundPackage, updated, current.launcherPackages)
                current.copy(approvedPackages = updated, currentFocusState = newState)
            }
        }
    }

    /**
     * Remove a package from approved list.
     */
    fun removeApprovedPackage(pkg: String) {
        _state.update { current ->
            val updated = current.approvedPackages - pkg
            val newState = recomputeState(current.currentForegroundPackage, updated, current.launcherPackages)
            current.copy(approvedPackages = updated, currentFocusState = newState)
        }
    }

    /**
     * Start continuous polling loop.
     */
    fun startPolling(scope: CoroutineScope, pollIntervalMs: Long = 800L) {
        if (pollingJob?.isActive == true) return

        pollingJob = scope.launch(Dispatchers.Default) {
            Log.i(TAG, "Starting UsageEvents polling loop (interval=${pollIntervalMs}ms)")
            while (isActive) {
                try {
                    pollOnce()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in polling iteration", e)
                }
                delay(pollIntervalMs)
            }
        }
    }

    /**
     * Stop continuous polling loop.
     */
    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        Log.i(TAG, "Stopped UsageEvents polling loop")
    }

    /**
     * Single polling step: checks permission, queries events, processes new transitions.
     */
    fun pollOnce() {
        val isGranted = checkUsageAccessPermission()
        val launchers = resolveLauncherPackages()

        if (!isGranted) {
            _state.update {
                it.copy(
                    isUsageAccessGranted = false,
                    currentFocusState = PocFocusState.UNKNOWN,
                    launcherPackages = launchers,
                    lastPollTimeMillis = System.currentTimeMillis()
                )
            }
            return
        }

        if (usageStatsManager == null) {
            Log.e(TAG, "UsageStatsManager is null")
            return
        }

        val now = System.currentTimeMillis()
        // Query recent window: look back at least 30s or from last processed timestamp minus 5s safety margin
        val startTime = if (lastProcessedTimestamp > 0L) {
            maxOf(lastProcessedTimestamp - 5_000L, now - 60_000L)
        } else {
            now - 30_000L
        }

        val usageEvents = try {
            usageStatsManager.queryEvents(startTime, now)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query usage events", e)
            null
        }

        if (usageEvents == null) {
            _state.update { it.copy(isUsageAccessGranted = true, lastPollTimeMillis = now) }
            return
        }

        val newEvents = mutableListOf<PocUsageEvent>()
        val outEvent = UsageEvents.Event()

        var latestForegroundPkg = _state.value.currentForegroundPackage
        var latestFocusState = _state.value.currentFocusState
        var maxEventTimestamp = lastProcessedTimestamp
        var latestDelay = _state.value.lastEventDelayMillis

        val approvedPkgs = _state.value.approvedPackages

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(outEvent)

            val typeCode = outEvent.eventType
            val eventTypeStr = eventTypeToString(typeCode)
            val pkg = outEvent.packageName ?: continue
            val eventTs = outEvent.timeStamp

            // Only track relevant foreground/screen lifecycle events
            if (!isRelevantEvent(typeCode)) {
                continue
            }

            val eventKey = "${eventTs}_${pkg}_${typeCode}"
            if (processedEventKeys.contains(eventKey)) {
                continue // Deduplicate
            }

            // Record as processed
            processedEventKeys.add(eventKey)
            if (processedEventKeys.size > maxProcessedKeys) {
                val iterator = processedEventKeys.iterator()
                if (iterator.hasNext()) {
                    iterator.next()
                    iterator.remove()
                }
            }

            if (eventTs > maxEventTimestamp) {
                maxEventTimestamp = eventTs
            }

            val detectedAt = System.currentTimeMillis()
            val delayMs = maxOf(0L, detectedAt - eventTs)
            latestDelay = delayMs

            // If an activity was resumed, this indicates a new foreground app!
            if (typeCode == UsageEvents.Event.ACTIVITY_RESUMED) {
                latestForegroundPkg = pkg
                latestFocusState = recomputeState(pkg, approvedPkgs, launchers)
            } else if (typeCode == UsageEvents.Event.KEYGUARD_SHOWN) {
                // Informational lock screen indication in timeline
            }

            val pocEvent = PocUsageEvent(
                id = eventKey,
                eventType = eventTypeStr,
                eventTypeCode = typeCode,
                packageName = pkg,
                className = outEvent.className,
                timestampMillis = eventTs,
                detectedAtMillis = detectedAt,
                delayMillis = delayMs,
                resultingState = latestFocusState
            )
            newEvents.add(pocEvent)
        }

        if (maxEventTimestamp > lastProcessedTimestamp) {
            lastProcessedTimestamp = maxEventTimestamp
        }

        _state.update { current ->
            val updatedTimeline = if (newEvents.isNotEmpty()) {
                // Newest first, capped at 60 entries
                (newEvents.reversed() + current.recentTimeline).take(60)
            } else {
                current.recentTimeline
            }

            current.copy(
                isUsageAccessGranted = true,
                currentForegroundPackage = latestForegroundPkg,
                currentFocusState = latestFocusState,
                launcherPackages = launchers,
                recentTimeline = updatedTimeline,
                lastPollTimeMillis = now,
                totalEventsProcessed = current.totalEventsProcessed + newEvents.size,
                lastEventDelayMillis = latestDelay
            )
        }
    }

    /**
     * Map package to temporary POC Focus State.
     */
    private fun recomputeState(
        pkg: String?,
        approved: Set<String>,
        launchers: Set<String>
    ): PocFocusState {
        if (pkg == null) return PocFocusState.UNKNOWN
        return when {
            approved.contains(pkg) -> PocFocusState.FOCUSING
            launchers.contains(pkg) -> PocFocusState.PAUSED_HOME
            else -> PocFocusState.PAUSED_UNAPPROVED
        }
    }

    private fun isRelevantEvent(type: Int): Boolean {
        return when (type) {
            UsageEvents.Event.ACTIVITY_RESUMED,
            UsageEvents.Event.ACTIVITY_PAUSED,
            UsageEvents.Event.KEYGUARD_SHOWN,
            UsageEvents.Event.KEYGUARD_HIDDEN,
            UsageEvents.Event.SCREEN_INTERACTIVE,
            UsageEvents.Event.SCREEN_NON_INTERACTIVE -> true
            else -> false
        }
    }

    private fun eventTypeToString(type: Int): String {
        return when (type) {
            UsageEvents.Event.ACTIVITY_RESUMED -> "ACTIVITY_RESUMED"
            UsageEvents.Event.ACTIVITY_PAUSED -> "ACTIVITY_PAUSED"
            UsageEvents.Event.KEYGUARD_SHOWN -> "KEYGUARD_SHOWN"
            UsageEvents.Event.KEYGUARD_HIDDEN -> "KEYGUARD_HIDDEN"
            UsageEvents.Event.SCREEN_INTERACTIVE -> "SCREEN_INTERACTIVE"
            UsageEvents.Event.SCREEN_NON_INTERACTIVE -> "SCREEN_NON_INTERACTIVE"
            UsageEvents.Event.USER_INTERACTION -> "USER_INTERACTION"
            UsageEvents.Event.STANDBY_BUCKET_CHANGED -> "STANDBY_BUCKET_CHANGED"
            else -> "EVENT_$type"
        }
    }
}
