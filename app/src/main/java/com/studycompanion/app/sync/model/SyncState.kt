package com.studycompanion.app.sync.model

/**
 * Observable synchronization state exposed to the UI and system monitoring.
 */
sealed interface SyncState {
    data class Synced(val lastSyncTimestamp: Long) : SyncState
    data class Syncing(val inFlightCount: Int) : SyncState
    data class Offline(val pendingCount: Int) : SyncState
    data class Error(val message: String, val isRetryable: Boolean) : SyncState
}
