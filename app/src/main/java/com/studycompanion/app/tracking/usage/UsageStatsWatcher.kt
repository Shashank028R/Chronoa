package com.studycompanion.app.tracking.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import com.studycompanion.app.tracking.engine.FocusEngine
import com.studycompanion.app.tracking.engine.FocusEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Production watcher using Android UsageStatsManager.queryEvents() to detect
 * foreground application transitions chronologically without fabricating timestamps.
 */
class UsageStatsWatcher(
    private val context: Context,
    private val focusEngine: FocusEngine,
    private val usageStatsManager: UsageStatsManager? = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
) {

    private var pollingJob: Job? = null
    private var lastProcessedEventTimestamp: Long = System.currentTimeMillis() - 60_000L
    private val processedEventSignatures = mutableSetOf<String>()
    private var lastKnownPermissionState: Boolean? = null

    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? android.app.AppOpsManager ?: return false
        val mode = appOps.unsafeCheckOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    /**
     * Dynamically queries the system PackageManager for all declared launcher/home activities.
     */
    fun resolveLauncherPackages(): Set<String> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val pm = context.packageManager
        val resolveInfos = pm.queryIntentActivities(launcherIntent, 0)
        val homePackages = resolveInfos.mapNotNull { it.activityInfo?.packageName }.toMutableSet()

        // Also query default fallback homes if any
        val secondaryIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
        }
        val defResolves = pm.queryIntentActivities(secondaryIntent, 0)
        homePackages.addAll(defResolves.mapNotNull { it.activityInfo?.packageName }.filter {
            it.contains("launcher", ignoreCase = true) || it.contains("home", ignoreCase = true)
        })

        homePackages.remove("com.android.settings")
        return homePackages
    }

    /**
     * Initializes launcher package detection and registers them with the FocusEngine.
     */
    fun initLauncherDetection() {
        val homes = resolveLauncherPackages()
        focusEngine.registerLauncherPackages(homes)
    }

    /**
     * Polls recent UsageEvents since lastProcessedEventTimestamp and feeds new transitions to FocusEngine.
     */
    @Synchronized
    fun pollEvents(): Int {
        val now = System.currentTimeMillis()

        // 1. Verify Usage Access permission state dynamically
        val hasAccess = hasUsageAccess()
        if (lastKnownPermissionState != hasAccess) {
            lastKnownPermissionState = hasAccess
            focusEngine.onEvent(FocusEvent.PermissionChanged(hasUsageAccess = hasAccess, timestamp = now))
        }

        if (!hasAccess) {
            return 0
        }

        // 2. Update engine heartbeat for process recovery
        focusEngine.updateHeartbeat(now)

        val manager = usageStatsManager ?: return 0
        val queryStart = maxOf(0L, lastProcessedEventTimestamp - 1000L) // 1s overlap for safety

        val events = manager.queryEvents(queryStart, now)
        val eventList = mutableListOf<UsageEvents.Event>()

        while (events.hasNextEvent()) {
            val event = UsageEvents.Event()
            events.getNextEvent(event)

            // Focus on ACTIVITY_RESUMED (1) and ACTIVITY_PAUSED (2)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                event.eventType == UsageEvents.Event.ACTIVITY_PAUSED
            ) {
                eventList.add(event)
            }
        }

        // Chronological sorting is required to prevent out-of-order state confusion
        eventList.sortBy { it.timeStamp }

        var dispatchedCount = 0
        for (event in eventList) {
            val signature = "${event.timeStamp}_${event.packageName}_${event.eventType}"
            if (processedEventSignatures.add(signature)) {
                // Keep signature set bounded
                if (processedEventSignatures.size > 500) {
                    val toRemove = processedEventSignatures.take(100)
                    processedEventSignatures.removeAll(toRemove.toSet())
                }

                if (event.timeStamp > lastProcessedEventTimestamp) {
                    lastProcessedEventTimestamp = event.timeStamp
                }

                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> {
                        focusEngine.onEvent(
                            FocusEvent.AppResumed(
                                packageName = event.packageName,
                                timestamp = event.timeStamp
                            )
                        )
                        dispatchedCount++
                    }
                    UsageEvents.Event.ACTIVITY_PAUSED -> {
                        focusEngine.onEvent(
                            FocusEvent.AppPaused(
                                packageName = event.packageName,
                                timestamp = event.timeStamp
                            )
                        )
                    }
                }
            }
        }

        return dispatchedCount
    }

    /**
     * Inspects recent UsageEvents to identify the most recent legitimately resumed package.
     * Returns null if no verified recent foreground event exists.
     */
    fun resolveLatestForegroundPackage(windowMs: Long = 15_000L): String? {
        val manager = usageStatsManager ?: return null
        if (!hasUsageAccess()) return null
        val now = System.currentTimeMillis()
        val events = manager.queryEvents(now - windowMs, now)
        var latestPkg: String? = null
        var latestTimestamp = 0L

        while (events.hasNextEvent()) {
            val event = UsageEvents.Event()
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED && event.timeStamp >= latestTimestamp) {
                latestPkg = event.packageName
                latestTimestamp = event.timeStamp
            }
        }
        return latestPkg
    }

    /**
     * Starts continuous polling loop at the specified cadence on the given coroutine scope.
     */
    fun startWatching(scope: CoroutineScope, intervalMs: Long = 750L) {
        stopWatching()
        initLauncherDetection()

        // Prime lastProcessedEventTimestamp to current time
        lastProcessedEventTimestamp = System.currentTimeMillis()

        // Safely resolve the real latest foreground package without assuming own app
        val initialPackage = resolveLatestForegroundPackage()
        if (initialPackage != null) {
            focusEngine.onEvent(
                FocusEvent.AppResumed(
                    packageName = initialPackage,
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        try {
            android.util.Log.i("UsageStatsWatcher", "startWatching: initialPackage=$initialPackage")
        } catch (t: Throwable) {}

        pollingJob = scope.launch {
            while (isActive) {
                try {
                    pollEvents()
                } catch (e: Exception) {
                    // Suppress and retry next interval
                }
                delay(intervalMs)
            }
        }
    }

    /**
     * Stops continuous polling.
     */
    fun stopWatching() {
        pollingJob?.cancel()
        pollingJob = null
    }
}
