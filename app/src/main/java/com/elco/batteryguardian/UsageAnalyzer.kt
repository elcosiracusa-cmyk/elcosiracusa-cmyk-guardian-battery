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

    fun hasUsageAccess(context: Context): Boolean = runCatching {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            ?: return@runCatching false
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        mode == AppOpsManager.MODE_ALLOWED
    }.getOrDefault(false)

    fun topApps(context: Context, limit: Int = 5): List<AppUsageItem> = runCatching {
        if (!hasUsageAccess(context)) return@runCatching emptyList()

        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return@runCatching emptyList()

        val now = System.currentTimeMillis()
        val from = now - 24L * 60L * 60L * 1000L
        val pm = context.packageManager

        val stats = manager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            from,
            now
        ) ?: emptyList()

        stats.asSequence()
            .filter { it.totalTimeInForeground > 0 }
            .sortedByDescending { it.totalTimeInForeground }
            .take(limit.coerceAtLeast(1))
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
    }.getOrDefault(emptyList())
}
