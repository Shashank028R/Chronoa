package com.studycompanion.app.tracking.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.studycompanion.app.StudyCompanionApplication
import com.studycompanion.app.tracking.recovery.ProcessDeathRecoveryHandler
import com.studycompanion.app.tracking.service.FocusTrackingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Handles device boot and package update broadcasts.
 * Reconciles dangling tracking sessions without fabricating time,
 * and restarts tracking when previously active and permitted by the OS.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != "android.intent.action.QUICKBOOT_POWERON" &&
            action != "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }

        Log.i("BootCompletedReceiver", "Received boot/replacement intent: $action")
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val app = context.applicationContext as? StudyCompanionApplication
                val container = app?.container

                if (container != null) {
                    val profile = container.profileRepository.getActiveProfile().firstOrNull()
                    if (profile != null) {
                        val settings = container.profileRepository.getProfileSettings(profile.id).firstOrNull()
                            ?: com.studycompanion.app.domain.model.ProfileSettings(profileId = profile.id)
                        val approvedApps = container.studyAppRepository.getEnabledStudyApps(profile.id).firstOrNull()
                            ?.map { it.packageName }?.toSet() ?: emptySet()

                        val recoveryHandler = ProcessDeathRecoveryHandler(
                            sessionRepository = container.sessionRepository,
                            userSessionDataStore = container.sessionDataStore,
                            focusEngine = container.focusEngine,
                            usageStatsWatcher = null
                        )

                        // 1. Reconcile any session that was dangling before the reboot
                        recoveryHandler.reconcile(
                            profileId = profile.id,
                            approvedPackages = approvedApps,
                            settings = settings
                        )
                    }

                    // 2. Check if tracking was active before device reboot
                    val wasTrackingActive = container.sessionDataStore.isTrackingActive()
                    if (wasTrackingActive) {
                        try {
                            FocusTrackingService.startService(context)
                            Log.i("BootCompletedReceiver", "FocusTrackingService restarted successfully after boot.")
                        } catch (e: Exception) {
                            Log.w("BootCompletedReceiver", "Could not start foreground service directly on boot: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("BootCompletedReceiver", "Error during boot reconciliation", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
