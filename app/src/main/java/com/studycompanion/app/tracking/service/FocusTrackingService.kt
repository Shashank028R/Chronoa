package com.studycompanion.app.tracking.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.studycompanion.app.MainActivity
import com.studycompanion.app.StudyCompanionApplication
import com.studycompanion.app.tracking.calls.CallStateWatcher
import com.studycompanion.app.tracking.engine.FocusEngine
import com.studycompanion.app.tracking.engine.FocusEvent
import com.studycompanion.app.tracking.engine.FocusState
import com.studycompanion.app.tracking.lockstate.LockStateReceiver
import com.studycompanion.app.tracking.usage.UsageStatsWatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground Service running the background watchers and maintaining the active FocusEngine session.
 */
class FocusTrackingService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private lateinit var focusEngine: FocusEngine
    private lateinit var usageStatsWatcher: UsageStatsWatcher
    private lateinit var lockStateReceiver: LockStateReceiver
    private lateinit var callStateWatcher: CallStateWatcher
    private var timeChangeReceiver: com.studycompanion.app.tracking.receiver.TimeChangeReceiver? = null
    private var isTimeReceiverRegistered = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val app = application as StudyCompanionApplication
        val container = app.container

        // Use application container's shared FocusEngine singleton
        focusEngine = container.focusEngine
        focusEngine.registerOwnPackageName(packageName)

        usageStatsWatcher = UsageStatsWatcher(this, focusEngine)
        lockStateReceiver = LockStateReceiver(this, focusEngine)
        callStateWatcher = CallStateWatcher(this, focusEngine)
        timeChangeReceiver = com.studycompanion.app.tracking.receiver.TimeChangeReceiver(focusEngine)

        // Observe FocusEngine state changes to update notification
        serviceScope.launch {
            focusEngine.state.collect { snapshot ->
                updateNotification(snapshot.state, snapshot.currentPackage)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as? StudyCompanionApplication
        val sessionDataStore = app?.container?.sessionDataStore

        try {
            android.util.Log.i("FocusTrackingService", "onStartCommand: action=${intent?.action}")
        } catch (t: Throwable) {}

        when (intent?.action) {
            ACTION_STOP -> {
                focusEngine.onEvent(FocusEvent.UserStop())
                serviceScope.launch {
                    sessionDataStore?.setTrackingActive(false)
                }
                stopWatchers()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START, null -> {
                startForegroundWithNotification()
                startWatchers()
                // Tracking activation is strictly governed by explicit user action or recovery handler
                serviceScope.launch {
                    sessionDataStore?.setTrackingActive(true)
                }
            }
        }
        return START_STICKY
    }

    private fun startWatchers() {
        usageStatsWatcher.startWatching(serviceScope, intervalMs = 750L)
        lockStateReceiver.startWatching()
        callStateWatcher.startWatching()

        if (!isTimeReceiverRegistered && timeChangeReceiver != null) {
            val filter = android.content.IntentFilter().apply {
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction("android.intent.action.TIME_SET")
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
                addAction(Intent.ACTION_DATE_CHANGED)
            }
            registerReceiver(timeChangeReceiver, filter)
            isTimeReceiverRegistered = true
        }
    }

    private fun stopWatchers() {
        usageStatsWatcher.stopWatching()
        lockStateReceiver.stopWatching()
        callStateWatcher.stopWatching()

        if (isTimeReceiverRegistered && timeChangeReceiver != null) {
            try {
                unregisterReceiver(timeChangeReceiver)
            } catch (e: Exception) {
                // Ignore if already unregistered
            }
            isTimeReceiverRegistered = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopWatchers()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Study Tracking",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Ongoing study tracking notification"
            setShowBadge(false)
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification(FocusState.READY, null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                }
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(state: FocusState, currentPackage: String?) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = buildNotification(state, currentPackage)
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(state: FocusState, currentPackage: String?): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, FocusTrackingService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = when (state) {
            FocusState.FOCUSING -> "Focusing: ${currentPackage ?: "Study App"}"
            FocusState.LOCKED_FOCUS -> "Focusing (Locked)"
            FocusState.PAUSED_HOME -> "Paused (Home screen)"
            FocusState.PAUSED_UNAPPROVED_APP -> "Paused (Unapproved app)"
            FocusState.PAUSED_CALL -> "Paused (Call in progress)"
            FocusState.PAUSED_USER -> "Paused by user"
            FocusState.WAITING_FOR_PERMISSION -> "Paused (Usage permission required)"
            else -> "Tracking Ready"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Chronoa")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(contentIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "study_tracking_channel"
        const val NOTIFICATION_ID = 2001
        const val ACTION_START = "com.studycompanion.app.tracking.ACTION_START"
        const val ACTION_STOP = "com.studycompanion.app.tracking.ACTION_STOP"

        fun startService(context: Context) {
            val intent = Intent(context, FocusTrackingService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FocusTrackingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
