package com.elco.batteryguardian

data class BatterySnapshot(
    val level: Int,
    val charging: Boolean,
    val temperatureC: Float,
    val voltageMv: Int,
    val currentNowUa: Int?,
    val chargeCounterUah: Int?,
    val healthLabel: String,
    val score: Int,
    val statusMessage: String
)
