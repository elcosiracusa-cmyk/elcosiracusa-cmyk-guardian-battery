package com.elco.batteryguardian

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import kotlin.math.abs

data class PowerEfficiencyResult(
    val powerSaverActive: Boolean,
    val thermalStatusLabel: String,
    val brightnessPercent: Int?,
    val instantCurrentMa: Int?,
    val instantPowerW: Float?,
    val estimatedHoursRemaining: Float?,
    val efficiencyLabel: String,
    val recommendation: String
)

object PowerEfficiencyEngine {

    fun analyze(
        context: Context,
        level: Int,
        charging: Boolean,
        voltageMv: Int,
        currentNowUa: Int?,
        chargeCounterUah: Int?
    ): PowerEfficiencyResult {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val saver = powerManager.isPowerSaveMode

        val thermal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            when (powerManager.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE -> LocaleText.pick(context, "Normale", "Normal")
                PowerManager.THERMAL_STATUS_LIGHT -> LocaleText.pick(context, "Leggero", "Light")
                PowerManager.THERMAL_STATUS_MODERATE -> LocaleText.pick(context, "Moderato", "Moderate")
                PowerManager.THERMAL_STATUS_SEVERE -> LocaleText.pick(context, "Alto", "High")
                PowerManager.THERMAL_STATUS_CRITICAL -> LocaleText.pick(context, "Critico", "Critical")
                PowerManager.THERMAL_STATUS_EMERGENCY -> LocaleText.pick(context, "Emergenza", "Emergency")
                PowerManager.THERMAL_STATUS_SHUTDOWN -> LocaleText.pick(context, "Spegnimento", "Shutdown")
                else -> LocaleText.pick(context, "Non disponibile", "Unavailable")
            }
        } else {
            LocaleText.pick(context, "Non disponibile", "Unavailable")
        }

        val brightness = try {
            val raw = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS
            )
            ((raw / 255f) * 100f).toInt().coerceIn(0, 100)
        } catch (_: Exception) {
            null
        }

        val currentMa = currentNowUa
            ?.takeIf { it != Int.MIN_VALUE && it != 0 }
            ?.let { abs(it) / 1000 }

        val powerW = if (currentMa != null && voltageMv > 0) {
            (currentMa * voltageMv) / 1_000_000f
        } else null

        val hours = if (
            !charging &&
            currentNowUa != null &&
            chargeCounterUah != null &&
            currentNowUa != 0 &&
            currentNowUa != Int.MIN_VALUE &&
            chargeCounterUah > 0
        ) {
            (chargeCounterUah.toFloat() / abs(currentNowUa).toFloat())
                .takeIf { it in 0.05f..72f }
        } else null

        val pressure = when {
            charging -> LocaleText.pick(context, "Ricarica", "Charging")
            currentMa == null -> LocaleText.pick(context, "Non misurabile", "Unavailable")
            currentMa < 250 -> LocaleText.pick(context, "Eccellente", "Excellent")
            currentMa < 600 -> LocaleText.pick(context, "Buono", "Good")
            currentMa < 1200 -> LocaleText.pick(context, "Medio", "Medium")
            currentMa < 2000 -> LocaleText.pick(context, "Alto", "High")
            else -> LocaleText.pick(context, "Molto alto", "Very high")
        }

        val recommendation = when {
            charging && level >= 90 ->
                LocaleText.pick(context, "Carica oltre il 90%: evita di lasciarla collegata inutilmente.", "Charge above 90%: avoid leaving the phone plugged in unnecessarily.")
            thermal == LocaleText.pick(context, "Critico", "Critical") || thermal == LocaleText.pick(context, "Emergenza", "Emergency") ->
                LocaleText.pick(context, "Carico termico critico: interrompi attività pesanti e ricarica.", "Critical thermal load: stop heavy activity and charging.")
            brightness != null && brightness >= 80 && !charging ->
                LocaleText.pick(context, "Luminosità molto alta: ridurla è uno degli interventi più efficaci.", "Very high brightness: lowering it is one of the most effective actions.")
            currentMa != null && currentMa >= 1500 && !charging ->
                LocaleText.pick(context, "Assorbimento istantaneo elevato: controlla schermo, GPS, video e app pesanti.", "High instantaneous drain: check display, GPS, video and heavy apps.")
            level <= 20 && !saver && !charging ->
                LocaleText.pick(context, "Attiva il risparmio energetico Android: batteria sotto il 20%.", "Enable Android Battery Saver: battery below 20%.")
            saver ->
                LocaleText.pick(context, "Risparmio energetico attivo. Battery Guardian resta in modalità Zero-Wake.", "Battery Saver is active. Battery Guardian remains in Zero-Wake mode.")
            else ->
                LocaleText.pick(context, "Nessun intervento urgente: mantieni attiva la modalità Zero-Wake.", "No urgent action required: keep Zero-Wake mode active.")
        }

        return PowerEfficiencyResult(
            powerSaverActive = saver,
            thermalStatusLabel = thermal,
            brightnessPercent = brightness,
            instantCurrentMa = currentMa,
            instantPowerW = powerW,
            estimatedHoursRemaining = hours,
            efficiencyLabel = pressure,
            recommendation = recommendation
        )
    }
}
