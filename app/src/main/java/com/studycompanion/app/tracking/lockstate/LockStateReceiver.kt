package com.studycompanion.app.tracking.lockstate

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import com.studycompanion.app.tracking.engine.FocusEngine
import com.studycompanion.app.tracking.engine.FocusEvent

/**
 * Observes screen interactive state and keyguard (lock screen) changes.
 */
class LockStateReceiver(
    private val context: Context,
    private val focusEngine: FocusEngine
) : BroadcastReceiver() {

    private val keyguardManager: KeyguardManager? =
        context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager

    private val powerManager: PowerManager? =
        context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    private var isRegistered = false

    override fun onReceive(ctx: Context?, intent: Intent?) {
        val now = System.currentTimeMillis()
        when (intent?.action) {
            Intent.ACTION_SCREEN_OFF -> {
                focusEngine.onEvent(FocusEvent.ScreenNonInteractive(now))
                val isLocked = keyguardManager?.isKeyguardLocked ?: true
                if (isLocked) {
                    focusEngine.onEvent(FocusEvent.KeyguardShown(now))
                }
            }
            Intent.ACTION_SCREEN_ON -> {
                focusEngine.onEvent(FocusEvent.ScreenInteractive(now))
                val isLocked = keyguardManager?.isKeyguardLocked ?: false
                if (isLocked) {
                    focusEngine.onEvent(FocusEvent.KeyguardShown(now))
                } else {
                    focusEngine.onEvent(FocusEvent.KeyguardHidden(now))
                }
            }
            Intent.ACTION_USER_PRESENT -> {
                // Device successfully unlocked by user
                focusEngine.onEvent(FocusEvent.ScreenInteractive(now))
                focusEngine.onEvent(FocusEvent.KeyguardHidden(now))
            }
        }
    }

    fun isDeviceLocked(): Boolean {
        return keyguardManager?.isKeyguardLocked ?: false
    }

    fun isScreenInteractive(): Boolean {
        return powerManager?.isInteractive ?: true
    }

    fun startWatching() {
        if (!isRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            context.registerReceiver(this, filter)
            isRegistered = true

            // Send initial state
            val now = System.currentTimeMillis()
            if (!isScreenInteractive()) {
                focusEngine.onEvent(FocusEvent.ScreenNonInteractive(now))
            }
            if (isDeviceLocked()) {
                focusEngine.onEvent(FocusEvent.KeyguardShown(now))
            }
        }
    }

    fun stopWatching() {
        if (isRegistered) {
            try {
                context.unregisterReceiver(this)
            } catch (e: Exception) {
                // Ignore if already unregistered
            }
            isRegistered = false
        }
    }
}
