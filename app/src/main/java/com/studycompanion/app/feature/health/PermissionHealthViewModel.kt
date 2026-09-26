package com.studycompanion.app.feature.health

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.os.Process
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.studycompanion.app.sync.SyncEngine
import com.studycompanion.app.sync.model.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class HealthStatus {
    READY,
    ACTION_REQUIRED,
    LIMITED,
    NOT_AVAILABLE
}

data class HealthItem(
    val title: String,
    val description: String,
    val status: HealthStatus,
    val statusText: String,
    val actionLabel: String?,
    val actionType: HealthActionType?
)

enum class HealthActionType {
    OPEN_USAGE_ACCESS,
    OPEN_NOTIFICATION_SETTINGS,
    REQUEST_BATTERY_OPTIMIZATION,
    TRIGGER_SYNC
}

data class PermissionHealthUiState(
    val items: List<HealthItem> = emptyList(),
    val overallReady: Boolean = false
)

class PermissionHealthViewModel(
    private val context: Context,
    private val syncEngine: SyncEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(PermissionHealthUiState())
    val uiState: StateFlow<PermissionHealthUiState> = _uiState.asStateFlow()

    init {
        checkHealth()
    }

    fun checkHealth() {
        val items = mutableListOf<HealthItem>()

        // 1. Usage Access
        val isUsageAccessGranted = checkUsageAccess()
        items.add(
            HealthItem(
                title = "Usage Access",
                description = "Enables automatic detection when your approved study apps are active in the foreground.",
                status = if (isUsageAccessGranted) HealthStatus.READY else HealthStatus.ACTION_REQUIRED,
                statusText = if (isUsageAccessGranted) "Active" else "Action Required",
                actionLabel = if (!isUsageAccessGranted) "Enable Access" else null,
                actionType = if (!isUsageAccessGranted) HealthActionType.OPEN_USAGE_ACCESS else null
            )
        )

        // 2. Notifications
        val isNotificationsGranted = checkNotifications()
        items.add(
            HealthItem(
                title = "Ongoing Tracking Alert",
                description = "Shows an ongoing silent notification so the Android system keeps the study tracker alive.",
                status = if (isNotificationsGranted) HealthStatus.READY else HealthStatus.LIMITED,
                statusText = if (isNotificationsGranted) "Enabled" else "Limited",
                actionLabel = if (!isNotificationsGranted) "Allow Notifications" else null,
                actionType = if (!isNotificationsGranted) HealthActionType.OPEN_NOTIFICATION_SETTINGS else null
            )
        )

        // 3. Battery Optimization
        val diagnostic = com.studycompanion.app.core.platform.BatteryOptimizationHelper.getDiagnostic(context)
        items.add(
            HealthItem(
                title = "Background Battery Policy",
                description = diagnostic.recommendation,
                status = if (diagnostic.isIgnoringBatteryOptimizations) HealthStatus.READY else HealthStatus.LIMITED,
                statusText = if (diagnostic.isIgnoringBatteryOptimizations) "Unrestricted" else "Standard (May be limited)",
                actionLabel = diagnostic.actionLabel,
                actionType = HealthActionType.REQUEST_BATTERY_OPTIMIZATION
            )
        )

        // 4. Cloud Synchronization
        val sync = syncEngine.syncState.value
        val syncStatus = when (sync) {
            is SyncState.Synced -> HealthStatus.READY to "Up to date"
            is SyncState.Syncing -> HealthStatus.READY to "Syncing now"
            is SyncState.Offline -> HealthStatus.LIMITED to "Offline (Safe locally)"
            is SyncState.Error -> HealthStatus.ACTION_REQUIRED to "Sync Error"
        }
        items.add(
            HealthItem(
                title = "Cloud Synchronization",
                description = "Backs up study sessions and profile targets. All tracking remains 100% autonomous offline.",
                status = syncStatus.first,
                statusText = syncStatus.second,
                actionLabel = if (sync is SyncState.Error) "Retry Sync" else null,
                actionType = if (sync is SyncState.Error) HealthActionType.TRIGGER_SYNC else null
            )
        )

        _uiState.value = PermissionHealthUiState(
            items = items,
            overallReady = isUsageAccessGranted
        )
    }

    fun triggerSync() {
        viewModelScope.launch {
            syncEngine.sync()
            checkHealth()
        }
    }

    private fun checkUsageAccess(): Boolean {
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
            false
        }
    }

    private fun checkNotifications(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun checkBatteryExemption(): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }

    class Factory(
        private val context: Context,
        private val syncEngine: SyncEngine
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PermissionHealthViewModel(context, syncEngine) as T
        }
    }
}
