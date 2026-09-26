package com.studycompanion.app.core.diagnostics

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.Process
import com.studycompanion.app.core.platform.BatteryOptimizationHelper
import org.json.JSONObject
import java.util.TimeZone

/**
 * Generates an exportable, privacy-safe diagnostic report detailing device state,
 * OEM power policies, permission statuses, and tracking subsystem health.
 */
object DiagnosticBundleGenerator {

    data class DiagnosticBundle(
        val deviceModel: String,
        val manufacturer: String,
        val androidVersion: Int,
        val oemType: String,
        val isIgnoringBatteryOptimizations: Boolean,
        val hasUsageAccess: Boolean,
        val timezoneId: String,
        val generatedAtMillis: Long,
        val jsonString: String
    )

    fun generate(context: Context): DiagnosticBundle {
        val now = System.currentTimeMillis()
        val oem = BatteryOptimizationHelper.detectOem()
        val isBatteryExempt = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
        val hasUsage = checkUsageAccess(context)
        val tzId = TimeZone.getDefault().id

        val json = JSONObject().apply {
            put("timestamp", now)
            put("timezone", tzId)
            put("androidApi", Build.VERSION.SDK_INT)
            put("deviceModel", Build.MODEL)
            put("manufacturer", Build.MANUFACTURER)
            put("brand", Build.BRAND)
            put("oemType", oem.name)
            put("isIgnoringBatteryOptimizations", isBatteryExempt)
            put("hasUsageAccess", hasUsage)
        }

        return DiagnosticBundle(
            deviceModel = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            androidVersion = Build.VERSION.SDK_INT,
            oemType = oem.name,
            isIgnoringBatteryOptimizations = isBatteryExempt,
            hasUsageAccess = hasUsage,
            timezoneId = tzId,
            generatedAtMillis = now,
            jsonString = json.toString(2)
        )
    }

    private fun checkUsageAccess(context: Context): Boolean {
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
}
