package com.elco.batteryguardian

data class BatteryDnaAdvice(
    val title: String,
    val detail: String
)

object BatteryDnaAdvisor {
    fun advice(
        comparison: BatteryDnaComparison,
        efficiency: PowerEfficiencyResult,
        temperatureC: Float,
        topApp: AppUsageItem?
    ): BatteryDnaAdvice {
        if (comparison.status == BatteryDnaStatus.LEARNING) {
            return BatteryDnaAdvice(
                title = "DNA in apprendimento",
                detail = "Battery Guardian creerà una baseline personale confrontando più sessioni reali."
            )
        }

        val likely = when {
            temperatureC >= 40f ->
                "La temperatura elevata è una causa probabile del peggioramento."
            efficiency.brightnessPercent != null &&
                efficiency.brightnessPercent >= 80 ->
                "La luminosità elevata sta incidendo sul consumo."
            efficiency.instantCurrentMa != null &&
                efficiency.instantCurrentMa >= 1400 ->
                "L'assorbimento istantaneo è molto sopra la fascia efficiente."
            topApp != null && topApp.foregroundMinutes >= 90 ->
                topApp.label + " è stata molto presente in primo piano."
            else ->
                "Nessuna singola causa domina: il consumo va confrontato con altri campioni."
        }

        return BatteryDnaAdvice(
            title = when (comparison.status) {
                BatteryDnaStatus.NORMAL -> "DNA stabile"
                BatteryDnaStatus.ABOVE_NORMAL -> "DNA fuori baseline"
                BatteryDnaStatus.HIGH -> "DNA anomalo"
                BatteryDnaStatus.EXTREME -> "DNA critico"
                BatteryDnaStatus.LEARNING -> "DNA in apprendimento"
            },
            detail = likely
        )
    }
}
