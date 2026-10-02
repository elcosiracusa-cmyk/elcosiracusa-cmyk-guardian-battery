package com.elco.batteryguardian

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Process

data class AppUsageItem(
    val label: String,
    val packageName: String,
    val foregroundMinutes: Long
)

object UsageAnalyzer {

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun topApps(context: Context, limit: Int = 5): List<AppUsageItem> {
        if (!hasUsageAccess(context)) return emptyList()

        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val from = now - 24L * 60L * 60L * 1000L
        val pm = context.packageManager

        return manager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            from,
            now
        )
            .asSequence()
            .filter { it.totalTimeInForeground > 0 }
            .sortedByDescending { it.totalTimeInForeground }
            .take(limit)
            .map {
                val label = try {
                    val info: ApplicationInfo = pm.getApplicationInfo(it.packageName, 0)
                    pm.getApplicationLabel(info).toString()
                } catch (_: Exception) {
                    it.packageName
                }
                AppUsageItem(
                    label = label,
                    packageName = it.packageName,
                    foregroundMinutes = it.totalTimeInForeground / 60000L
                )
            }
            .toList()
    }
}
