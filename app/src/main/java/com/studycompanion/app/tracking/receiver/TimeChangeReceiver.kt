package com.studycompanion.app.tracking.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.studycompanion.app.StudyCompanionApplication
import com.studycompanion.app.tracking.engine.FocusEngine
import com.studycompanion.app.tracking.engine.FocusEvent
import java.util.Calendar
import java.util.TimeZone

/**
 * Handles system clock adjustments, timezone changes, and date/midnight rollover broadcasts.
 */
class TimeChangeReceiver(
    private val focusEngine: FocusEngine? = null
) : BroadcastReceiver() {

    private var lastRecordedTimeMillis: Long = System.currentTimeMillis()
    private var lastRecordedTimezoneId: String = TimeZone.getDefault().id

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val app = context.applicationContext as? StudyCompanionApplication
        val engine = focusEngine ?: app?.container?.focusEngine ?: return

        val now = System.currentTimeMillis()

        when (action) {
            Intent.ACTION_TIME_CHANGED, "android.intent.action.TIME_SET" -> {
                val oldTime = lastRecordedTimeMillis
                lastRecordedTimeMillis = now
                Log.i("TimeChangeReceiver", "Time set event detected: old=$oldTime, new=$now")
                engine.onEvent(FocusEvent.ClockChanged(oldTimestamp = oldTime, newTimestamp = now))
            }
            Intent.ACTION_TIMEZONE_CHANGED -> {
                val oldTz = lastRecordedTimezoneId
                val newTz = intent.getStringExtra("time-zone") ?: TimeZone.getDefault().id
                lastRecordedTimezoneId = newTz
                Log.i("TimeChangeReceiver", "Timezone changed: old=$oldTz, new=$newTz")
                engine.onEvent(FocusEvent.TimezoneChanged(oldTimezoneId = oldTz, newTimezoneId = newTz, timestamp = now))
            }
            Intent.ACTION_DATE_CHANGED -> {
                Log.i("TimeChangeReceiver", "Date/midnight changed: $now")
                val midnight = calculatePreviousMidnight(now)
                engine.onEvent(
                    FocusEvent.MidnightRolledOver(
                        previousDayBoundary = midnight,
                        newDayBoundary = midnight,
                        timestamp = now
                    )
                )
            }
        }
    }

    private fun calculatePreviousMidnight(now: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
