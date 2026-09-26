package com.studycompanion.app.tracking.calls

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.TelecomManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.studycompanion.app.tracking.engine.FocusEngine
import com.studycompanion.app.tracking.engine.FocusEvent

/**
 * Observes active cellular and ConnectionService-integrated phone calls using Android TelecomManager
 * and TelephonyManager.
 *
 * Platform Limitation Note:
 * Android system APIs detect standard phone calls and third-party VoIP apps that integrate with Android's
 * Telecom ConnectionService. Proprietary VoIP apps (e.g. WhatsApp, Signal) that do not declare ConnectionService
 * are not visible to TelecomManager.isInCall() through public system APIs.
 */
class CallStateWatcher(
    private val context: Context,
    private val focusEngine: FocusEngine
) {

    private val telecomManager: TelecomManager? =
        context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager

    private val telephonyManager: TelephonyManager? =
        context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

    private var isCallActive: Boolean = false

    /**
     * Checks if a phone call is currently in progress via TelecomManager.
     */
    fun isCallInProgress(): Boolean {
        val tm = telecomManager ?: return false
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        return if (hasPermission) {
            try {
                tm.isInCall
            } catch (e: SecurityException) {
                false
            }
        } else {
            false
        }
    }

    /**
     * Checks the call state and dispatches CallStarted or CallEnded if state changed.
     */
    fun checkAndUpdateCallState() {
        val inCall = isCallInProgress()
        if (inCall != isCallActive) {
            isCallActive = inCall
            val now = System.currentTimeMillis()
            if (inCall) {
                focusEngine.onEvent(FocusEvent.CallStarted(now))
            } else {
                focusEngine.onEvent(FocusEvent.CallEnded(now))
            }
        }
    }

    /**
     * Starts call state monitoring.
     */
    fun startWatching() {
        checkAndUpdateCallState()

        // Register TelephonyManager callback on Android 12+ if permission granted
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission && telephonyManager != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val executor = ContextCompat.getMainExecutor(context)
                    telephonyManager.registerTelephonyCallback(
                        executor,
                        object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                            override fun onCallStateChanged(state: Int) {
                                val inCall = state == TelephonyManager.CALL_STATE_OFFHOOK ||
                                        state == TelephonyManager.CALL_STATE_RINGING ||
                                        isCallInProgress()
                                updateCallState(inCall)
                            }
                        }
                    )
                } else {
                    @Suppress("DEPRECATION")
                    telephonyManager.listen(
                        object : PhoneStateListener() {
                            @Deprecated("Deprecated in Java")
                            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                                val inCall = state == TelephonyManager.CALL_STATE_OFFHOOK ||
                                        state == TelephonyManager.CALL_STATE_RINGING ||
                                        isCallInProgress()
                                updateCallState(inCall)
                            }
                        },
                        PhoneStateListener.LISTEN_CALL_STATE
                    )
                }
            } catch (e: Exception) {
                // Telephony listener not permitted or failed
            }
        }
    }

    private fun updateCallState(inCall: Boolean) {
        if (inCall != isCallActive) {
            isCallActive = inCall
            val now = System.currentTimeMillis()
            if (inCall) {
                focusEngine.onEvent(FocusEvent.CallStarted(now))
            } else {
                focusEngine.onEvent(FocusEvent.CallEnded(now))
            }
        }
    }

    fun stopWatching() {
        // Telephony listener unregistration
    }
}
