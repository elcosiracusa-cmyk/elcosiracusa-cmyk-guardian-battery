package com.elco.batteryguardian

import android.content.Context

object GuardianPreferences {
    private const val PREFS = "guardian_preferences"
    private const val KEY_TARGET_HOURS = "target_hours"
    private const val KEY_RESERVE_PERCENT = "reserve_percent"
    private const val KEY_CHARGE_LIMIT = "charge_limit"
    private const val KEY_MODE = "battery_mode"

    fun targetHours(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_TARGET_HOURS, 8)
            .coerceIn(1, 48)

    fun reservePercent(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_RESERVE_PERCENT, 20)
            .coerceIn(0, 80)

    fun chargeLimit(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_CHARGE_LIMIT, 85)
            .coerceIn(70, 100)

    fun batteryMode(context: Context): BatteryMode {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_MODE, BatteryMode.DAILY.name)
        return runCatching { BatteryMode.valueOf(raw ?: BatteryMode.DAILY.name) }
            .getOrDefault(BatteryMode.DAILY)
    }

    fun saveBudget(context: Context, hours: Int, reserve: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_TARGET_HOURS, hours.coerceIn(1, 48))
            .putInt(KEY_RESERVE_PERCENT, reserve.coerceIn(0, 80))
            .apply()
    }

    fun saveChargeLimit(context: Context, limit: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_CHARGE_LIMIT, limit.coerceIn(70, 100))
            .apply()
    }

    fun saveMode(context: Context, mode: BatteryMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODE, mode.name)
            .apply()
    }
}
