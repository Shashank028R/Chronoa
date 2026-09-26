package com.studycompanion.app.tracking.recovery

import android.util.Log
import com.studycompanion.app.core.datastore.UserSessionDataStore
import com.studycompanion.app.domain.model.ProfileSettings
import com.studycompanion.app.domain.model.SessionEvent
import com.studycompanion.app.domain.model.StudySession
import com.studycompanion.app.domain.model.TrackingType
import com.studycompanion.app.domain.model.VerificationStatus
import com.studycompanion.app.domain.repository.SessionRepository
import com.studycompanion.app.tracking.engine.FocusEngine
import com.studycompanion.app.tracking.engine.FocusEvent
import com.studycompanion.app.tracking.usage.UsageStatsWatcher
import org.json.JSONObject
import java.util.UUID

/**
 * Handles process death, application restart, and device reboot reconciliation.
 * Inspects persisted markers, closes dangling open sessions without fabricating study time,
 * and recovers the live tracking state from verified events.
 */
class ProcessDeathRecoveryHandler(
    private val sessionRepository: SessionRepository,
    private val userSessionDataStore: UserSessionDataStore,
    private val focusEngine: FocusEngine,
    private val usageStatsWatcher: UsageStatsWatcher?
) {

    /**
     * Executes safe state reconciliation on application launch or service resurrection.
     */
    suspend fun reconcile(
        profileId: String,
        approvedPackages: Set<String>,
        settings: ProfileSettings
    ) {
        val now = System.currentTimeMillis()

        // 1. Inspect durable active session marker for dangling session from crash or reboot
        try {
            val markerJson = userSessionDataStore.getActiveSessionMarker()
            if (!markerJson.isNullOrBlank()) {
                val json = JSONObject(markerJson)
                val sessionId = json.optString("sessionId", UUID.randomUUID().toString())
                val markerProfileId = json.optString("profileId", profileId)
                val packageName = json.optString("packageName", "").takeIf { it.isNotBlank() }
                val startWall = json.optLong("startWall", now)
                val lastHeartbeat = json.optLong("lastHeartbeat", startWall)

                val durationSeconds = maxOf(0L, (lastHeartbeat - startWall) / 1000L)

                if (durationSeconds > 0 && markerProfileId.isNotBlank()) {
                    val session = StudySession(
                        id = sessionId,
                        profileId = markerProfileId,
                        deviceId = "local-device",
                        platform = "ANDROID",
                        packageName = packageName,
                        startAt = startWall,
                        endAt = lastHeartbeat,
                        durationSeconds = durationSeconds,
                        trackingType = TrackingType.AUTOMATIC,
                        verificationStatus = VerificationStatus.VERIFIED_BY_RULES,
                        syncState = "LOCAL"
                    )
                    sessionRepository.recordSession(session)

                    sessionRepository.recordEvent(
                        SessionEvent(
                            id = UUID.randomUUID().toString(),
                            sessionId = sessionId,
                            profileId = markerProfileId,
                            deviceId = "local-device",
                            eventType = "RECONCILED_AFTER_TERMINATION",
                            timestamp = lastHeartbeat,
                            packageName = packageName,
                            engineState = "PAUSED",
                            reasonCode = "PROCESS_DEATH_OR_REBOOT",
                            source = "RECOVERY_HANDLER",
                            metadataJson = """{"recoveredAt":$now,"gapMs":${now - lastHeartbeat}}"""
                        )
                    )
                    Log.i("ProcessDeathRecovery", "Reconciled dangling session $sessionId: duration=${durationSeconds}s, closed at lastHeartbeat=$lastHeartbeat (gap=${(now - lastHeartbeat)/1000}s excluded).")
                }

                // Clear the marker so it is not processed twice
                userSessionDataStore.setActiveSessionMarker(null)
            }
        } catch (e: Exception) {
            Log.e("ProcessDeathRecovery", "Error inspecting durable session marker", e)
        }

        // 2. Notify FocusEngine that recovery is underway
        focusEngine.onEvent(FocusEvent.RecoveryTriggered(now))

        // 3. Load active profile configuration into engine
        focusEngine.onEvent(
            FocusEvent.ProfileSwitched(
                profileId = profileId,
                approvedPackages = approvedPackages,
                settings = settings,
                timestamp = now
            )
        )

        // 4. Poll recent usage events to discover the actual foreground app right now
        usageStatsWatcher?.pollEvents()
    }
}
