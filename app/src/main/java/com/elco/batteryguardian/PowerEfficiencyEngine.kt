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
                PowerManager.THERMAL_STATUS_NONE -> "Normale"
                PowerManager.THERMAL_STATUS_LIGHT -> "Leggero"
                PowerManager.THERMAL_STATUS_MODERATE -> "Moderato"
                PowerManager.THERMAL_STATUS_SEVERE -> "Alto"
                PowerManager.THERMAL_STATUS_CRITICAL -> "Critico"
                PowerManager.THERMAL_STATUS_EMERGENCY -> "Emergenza"
                PowerManager.THERMAL_STATUS_SHUTDOWN -> "Spegnimento"
                else -> "Non disponibile"
            }
        } else {
            "Non disponibile"
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
            charging -> "Ricarica"
            currentMa == null -> "Non misurabile"
            currentMa < 250 -> "Eccellente"
            currentMa < 600 -> "Buono"
            currentMa < 1200 -> "Medio"
            currentMa < 2000 -> "Alto"
            else -> "Molto alto"
        }

        val recommendation = when {
            charging && level >= 90 ->
                "Carica oltre il 90%: evita di lasciarla collegata inutilmente."
            thermal == "Critico" || thermal == "Emergenza" ->
                "Carico termico critico: interrompi attività pesanti e ricarica."
            brightness != null && brightness >= 80 && !charging ->
                "Luminosità molto alta: ridurla è uno degli interventi più efficaci."
            currentMa != null && currentMa >= 1500 && !charging ->
                "Assorbimento istantaneo elevato: controlla schermo, GPS, video e app pesanti."
            level <= 20 && !saver && !charging ->
                "Attiva il risparmio energetico Android: batteria sotto il 20%."
            saver ->
                "Risparmio energetico attivo. Battery Guardian resta in modalità Zero-Wake."
            else ->
                "Nessun intervento urgente: mantieni attiva la modalità Zero-Wake."
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
