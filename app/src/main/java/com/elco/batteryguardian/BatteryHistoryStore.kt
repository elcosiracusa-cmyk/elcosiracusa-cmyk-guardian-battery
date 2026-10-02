package com.elco.batteryguardian

import android.content.Context

object BatteryHistoryStore {
    private const val PREFS = "battery_guardian_history"
    private const val KEY_TIME = "last_time"
    private const val KEY_LEVEL = "last_level"
    private const val KEY_CHARGING = "last_charging"

    data class DrainObservation(
        val percentPerHour: Float?,
        val minutesObserved: Long
    )

    fun recordAndEstimate(
        context: Context,
        level: Int,
        charging: Boolean,
        nowMs: Long = System.currentTimeMillis()
    ): DrainObservation {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val previousTime = prefs.getLong(KEY_TIME, 0L)
        val previousLevel = prefs.getInt(KEY_LEVEL, -1)
        val previousCharging = prefs.getBoolean(KEY_CHARGING, false)

        var result = DrainObservation(null, 0L)

        if (previousTime > 0L && previousLevel >= 0) {
            val elapsedMs = nowMs - previousTime
            val elapsedMinutes = elapsedMs / 60_000L

            if (
                elapsedMinutes in 10..(24 * 60) &&
                !previousCharging &&
                !charging &&
                previousLevel >= level
            ) {
                val drop = previousLevel - level
                val hours = elapsedMs / 3_600_000f
                val rate = if (drop > 0 && hours > 0f) drop / hours else 0f
                result = DrainObservation(
                    percentPerHour = rate.coerceIn(0f, 100f),
                    minutesObserved = elapsedMinutes
                )
            }
        }

        prefs.edit()
            .putLong(KEY_TIME, nowMs)
            .putInt(KEY_LEVEL, level)
            .putBoolean(KEY_CHARGING, charging)
            .apply()

        return result
    }
}
