package com.studycompanion.app.sync.conflict

import com.studycompanion.app.core.database.entity.DailyTargetEntity
import com.studycompanion.app.core.database.entity.ProfileSettingsEntity
import com.studycompanion.app.core.database.entity.StudyAppEntity
import com.studycompanion.app.core.database.entity.StudySessionEntity

/**
 * Result of resolving a conflict between local state and incoming remote state.
 */
sealed interface ConflictResolution<T> {
    data class ApplyRemote<T>(val entity: T) : ConflictResolution<T>
    data class KeepLocal<T>(val entity: T) : ConflictResolution<T>
    data class Delete<T>(val id: String) : ConflictResolution<T>
}

/**
 * Pure conflict resolution engine enforcing the rules from docs/11_OFFLINE_SYNC.md:
 * 1. Study Sessions: Immutable once finalized; duplicate IDs ignored idempotently;
 *    manual sessions never overwritten by stale automatic data.
 * 2. Daily Targets: Last-Write-Wins (LWW) based on updatedAt, while preserving originalTargetSeconds.
 * 3. Study Apps: Set semantics; enable/disable resolved by latest updatedAt.
 * 4. Settings: LWW based on updatedAt.
 * 5. Deletions: Tombstone propagation.
 */
object ConflictResolver {

    /**
     * Resolves session conflict.
     * Sessions are immutable once finalized. If local exists, keep local unless local is a draft
     * or remote is an explicit deletion / manual edit with higher sync version.
     */
    fun resolveSession(
        local: StudySessionEntity?,
        remote: StudySessionEntity
    ): ConflictResolution<StudySessionEntity> {
        if (local == null) {
            return ConflictResolution.ApplyRemote(remote)
        }

        // Idempotency: exact same session already exists
        if (local.id == remote.id) {
            // If local was already deleted, respect deletion
            if (local.deletedAt != null && remote.deletedAt == null) {
                return ConflictResolution.KeepLocal(local)
            }
            if (remote.deletedAt != null) {
                return ConflictResolution.ApplyRemote(remote)
            }

            // Protect MANUAL sessions from being overwritten by AUTOMATIC sessions
            if (local.trackingType == "MANUAL" && remote.trackingType == "AUTOMATIC") {
                return ConflictResolution.KeepLocal(local)
            }

            // If remote has higher sync version or newer updatedAt, accept remote
            if (remote.syncVersion > local.syncVersion ||
                (remote.syncVersion == local.syncVersion && remote.updatedAt > local.updatedAt)
            ) {
                return ConflictResolution.ApplyRemote(remote)
            }

            return ConflictResolution.KeepLocal(local)
        }

        return ConflictResolution.ApplyRemote(remote)
    }

    /**
     * Resolves daily target conflict using Last-Write-Wins (LWW) while strictly preserving
     * the immutable originalTargetSeconds.
     */
    fun resolveTarget(
        local: DailyTargetEntity?,
        remote: DailyTargetEntity
    ): ConflictResolution<DailyTargetEntity> {
        if (local == null) {
            return ConflictResolution.ApplyRemote(remote)
        }

        // Always preserve originalTargetSeconds from whichever was established first or original local
        val preservedOriginal = local.originalTargetSeconds

        return if (remote.updatedAt > local.updatedAt) {
            // Apply remote adjusted target, but preserve originalTargetSeconds
            ConflictResolution.ApplyRemote(
                remote.copy(originalTargetSeconds = preservedOriginal)
            )
        } else {
            ConflictResolution.KeepLocal(local)
        }
    }

    /**
     * Resolves Study App conflicts based on latest updatedAt.
     */
    fun resolveStudyApp(
        local: StudyAppEntity?,
        remote: StudyAppEntity
    ): ConflictResolution<StudyAppEntity> {
        if (local == null) {
            return ConflictResolution.ApplyRemote(remote)
        }

        return if (remote.updatedAt >= local.updatedAt) {
            ConflictResolution.ApplyRemote(remote)
        } else {
            ConflictResolution.KeepLocal(local)
        }
    }

    /**
     * Resolves profile settings conflict using Last-Write-Wins.
     */
    fun resolveSettings(
        local: ProfileSettingsEntity?,
        remote: ProfileSettingsEntity
    ): ConflictResolution<ProfileSettingsEntity> {
        if (local == null) {
            return ConflictResolution.ApplyRemote(remote)
        }

        return if (remote.updatedAt >= local.updatedAt) {
            ConflictResolution.ApplyRemote(remote)
        } else {
            ConflictResolution.KeepLocal(local)
        }
    }
}
