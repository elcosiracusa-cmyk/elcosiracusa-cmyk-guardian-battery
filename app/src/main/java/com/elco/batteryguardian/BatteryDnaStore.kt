package com.elco.batteryguardian

import android.content.Context
import kotlin.math.abs

data class BatteryDnaProfile(
    val samples: Int,
    val averageDrainPerHour: Float?,
    val averageTemperatureC: Float?,
    val averageCurrentMa: Float?,
    val confidence: Int
)

data class BatteryDnaComparison(
    val deviationPercent: Float?,
    val status: BatteryDnaStatus,
    val message: String
)

enum class BatteryDnaStatus {
    LEARNING,
    NORMAL,
    ABOVE_NORMAL,
    HIGH,
    EXTREME
}

object BatteryDnaStore {
    private const val PREFS = "battery_dna"
    private const val KEY_SAMPLES = "samples"
    private const val KEY_DRAIN_SUM = "drain_sum"
    private const val KEY_TEMP_SUM = "temp_sum"
    private const val KEY_CURRENT_SUM = "current_sum"

    fun addSample(
        context: Context,
        drainPerHour: Float?,
        temperatureC: Float,
        currentMa: Int?
    ) {
        if (drainPerHour == null || drainPerHour < 0f || drainPerHour > 100f) return

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val samples = prefs.getInt(KEY_SAMPLES, 0)
        val drainSum = prefs.getFloat(KEY_DRAIN_SUM, 0f)
        val tempSum = prefs.getFloat(KEY_TEMP_SUM, 0f)
        val currentSum = prefs.getFloat(KEY_CURRENT_SUM, 0f)

        prefs.edit()
            .putInt(KEY_SAMPLES, (samples + 1).coerceAtMost(500))
            .putFloat(KEY_DRAIN_SUM, drainSum + drainPerHour)
            .putFloat(KEY_TEMP_SUM, tempSum + temperatureC)
            .putFloat(KEY_CURRENT_SUM, currentSum + (currentMa ?: 0).toFloat())
            .apply()
    }

    fun profile(context: Context): BatteryDnaProfile {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val samples = prefs.getInt(KEY_SAMPLES, 0)
        if (samples <= 0) {
            return BatteryDnaProfile(0, null, null, null, 0)
        }

        val drain = prefs.getFloat(KEY_DRAIN_SUM, 0f) / samples
        val temp = prefs.getFloat(KEY_TEMP_SUM, 0f) / samples
        val current = prefs.getFloat(KEY_CURRENT_SUM, 0f) / samples
        val confidence = ((samples / 20f) * 100f).toInt().coerceIn(0, 100)

        return BatteryDnaProfile(
            samples = samples,
            averageDrainPerHour = drain,
            averageTemperatureC = temp,
            averageCurrentMa = current.takeIf { it > 0f },
            confidence = confidence
        )
    }

    fun compare(
        context: Context,
        currentDrainPerHour: Float?
    ): BatteryDnaComparison {
        val profile = profile(context)
        val baseline = profile.averageDrainPerHour

        if (currentDrainPerHour == null || baseline == null || profile.samples < 3) {
            return BatteryDnaComparison(
                deviationPercent = null,
                status = BatteryDnaStatus.LEARNING,
                message = "Battery DNA sta imparando il comportamento normale del dispositivo."
            )
        }

        if (baseline <= 0.01f) {
            return BatteryDnaComparison(
                deviationPercent = 0f,
                status = BatteryDnaStatus.NORMAL,
                message = "Consumo nella norma per questo dispositivo."
            )
        }

        val deviation = ((currentDrainPerHour - baseline) / baseline) * 100f
        val absDeviation = abs(deviation)

        val status = when {
            deviation >= 100f -> BatteryDnaStatus.EXTREME
            deviation >= 60f -> BatteryDnaStatus.HIGH
            deviation >= 25f -> BatteryDnaStatus.ABOVE_NORMAL
            else -> BatteryDnaStatus.NORMAL
        }

        val message = when (status) {
            BatteryDnaStatus.LEARNING ->
                "Battery DNA sta imparando."
            BatteryDnaStatus.NORMAL ->
                "Consumo coerente con il profilo abituale del telefono."
            BatteryDnaStatus.ABOVE_NORMAL ->
                "Consumo sopra la tua baseline personale."
            BatteryDnaStatus.HIGH ->
                "Consumo molto più alto del normale per questo dispositivo."
            BatteryDnaStatus.EXTREME ->
                "Anomalia forte: consumo oltre il doppio della baseline personale."
        }

        return BatteryDnaComparison(
            deviationPercent = absDeviation,
            status = status,
            message = message
        )
    }
}
