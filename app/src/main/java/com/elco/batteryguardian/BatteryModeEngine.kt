package com.elco.batteryguardian

enum class BatteryMode {
    DAILY,
    NIGHT,
    TRAVEL,
    EMERGENCY
}

data class BatteryModeProfile(
    val title: String,
    val description: String,
    val targetHours: Int,
    val reservePercent: Int,
    val chargeLimit: Int,
    val recommendation: String
)

object BatteryModeEngine {
    fun profile(mode: BatteryMode): BatteryModeProfile = when (mode) {
        BatteryMode.DAILY -> BatteryModeProfile(
            title = "Daily",
            description = "Equilibrio tra autonomia e prestazioni.",
            targetHours = 8,
            reservePercent = 20,
            chargeLimit = 85,
            recommendation = "Mantieni luminosità adattiva e limita i picchi inutili."
        )
        BatteryMode.NIGHT -> BatteryModeProfile(
            title = "Notte",
            description = "Riduce attività non essenziali durante il riposo.",
            targetHours = 10,
            reservePercent = 25,
            chargeLimit = 80,
            recommendation = "Riduci luminosità e usa il risparmio energetico se non servono prestazioni."
        )
        BatteryMode.TRAVEL -> BatteryModeProfile(
            title = "Viaggio",
            description = "Priorità alla durata durante spostamenti lunghi.",
            targetHours = 14,
            reservePercent = 30,
            chargeLimit = 90,
            recommendation = "Controlla GPS, hotspot, video e luminosità elevata."
        )
        BatteryMode.EMERGENCY -> BatteryModeProfile(
            title = "Emergenza",
            description = "Massimizza la durata residua.",
            targetHours = 24,
            reservePercent = 10,
            chargeLimit = 100,
            recommendation = "Attiva il risparmio energetico Android e limita funzioni non essenziali."
        )
    }
}
