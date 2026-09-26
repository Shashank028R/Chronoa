package com.studycompanion.app.core.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import java.util.Locale

/**
 * Diagnostic helper and recommendation generator for Android battery optimizations and OEM battery killers.
 */
object BatteryOptimizationHelper {

    enum class OemType {
        ONEPLUS,
        OPPO,
        REALME,
        XIAOMI,
        SAMSUNG,
        STOCK_OR_OTHER
    }

    data class BatteryDiagnostic(
        val isIgnoringBatteryOptimizations: Boolean,
        val oemType: OemType,
        val manufacturerName: String,
        val title: String,
        val explanation: String,
        val recommendation: String,
        val actionLabel: String?,
        val settingsIntent: Intent?
    )

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun detectOem(): OemType {
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.ROOT)
        val brand = Build.BRAND.lowercase(Locale.ROOT)

        return when {
            manufacturer.contains("oneplus") || brand.contains("oneplus") -> OemType.ONEPLUS
            manufacturer.contains("oppo") || brand.contains("oppo") -> OemType.OPPO
            manufacturer.contains("realme") || brand.contains("realme") -> OemType.REALME
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || brand.contains("xiaomi") -> OemType.XIAOMI
            manufacturer.contains("samsung") || brand.contains("samsung") -> OemType.SAMSUNG
            else -> OemType.STOCK_OR_OTHER
        }
    }

    fun getDiagnostic(context: Context): BatteryDiagnostic {
        val isIgnored = isIgnoringBatteryOptimizations(context)
        val oem = detectOem()
        val mfgName = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }

        val intent = if (!isIgnored) {
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            createOemBatterySettingsIntent(context, oem)
        }

        val (rec, explanation) = when (oem) {
            OemType.ONEPLUS -> {
                "OxygenOS uses aggressive background freezing." to
                "Allow background activity in Settings > Battery > App Battery Management > Study Companion. Also enable 'Auto-launch' and select 'Don't optimize'."
            }
            OemType.OPPO -> {
                "ColorOS actively freezes background services." to
                "Enable 'Allow background activity' and 'Allow auto-startup' in Settings > Battery > More settings."
            }
            OemType.REALME -> {
                "realme UI enforces background restrictions." to
                "Set Background power consumption to 'Allow background activity' and enable Auto-launch."
            }
            OemType.XIAOMI -> {
                "MIUI / HyperOS kills non-whitelisted apps." to
                "Set Battery Saver to 'No restrictions' and enable 'Autostart' in App info."
            }
            OemType.SAMSUNG -> {
                "One UI limits background activity." to
                "Add Study Companion to 'Never sleeping apps' in Settings > Battery > Background usage limits."
            }
            OemType.STOCK_OR_OTHER -> {
                "Android Battery Optimization" to
                "Set Battery usage for Study Companion to 'Unrestricted' in App info."
            }
        }

        val title = if (isIgnored) "Battery Optimization: Unrestricted" else "Background activity may be limited"
        val actionLabel = if (!isIgnored) "Open Battery Settings" else "Review OEM Settings"

        return BatteryDiagnostic(
            isIgnoringBatteryOptimizations = isIgnored,
            oemType = oem,
            manufacturerName = mfgName,
            title = title,
            explanation = "Your phone's battery manager can restrict background tracking during long study sessions.",
            recommendation = "$rec $explanation Note: Disabling battery optimization improves background survivability, but OEM system managers may still terminate services under critical memory pressure.",
            actionLabel = actionLabel,
            settingsIntent = intent
        )
    }

    private fun createOemBatterySettingsIntent(context: Context, oem: OemType): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }
}
