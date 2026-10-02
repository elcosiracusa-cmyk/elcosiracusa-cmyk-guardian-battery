package com.elco.batteryguardian

data class LeakDiagnosis(
    val severity: LeakSeverity,
    val title: String,
    val explanation: String,
    val likelyCause: String
)

enum class LeakSeverity {
    LEARNING,
    NORMAL,
    WATCH,
    HIGH,
    CRITICAL
}

object BatteryLeakDetector {

    fun diagnose(
        charging: Boolean,
        temperatureC: Float,
        brightnessPercent: Int?,
        instantCurrentMa: Int?,
        observedDrainPerHour: Float?,
        budget: BatteryBudget,
        topApp: AppUsageItem?
    ): LeakDiagnosis {
        if (charging) {
            return LeakDiagnosis(
                severity = LeakSeverity.NORMAL,
                title = "Leak Detector in pausa",
                explanation = "Il telefono è in carica: la scarica reale non è confrontabile.",
                likelyCause = "Nessuna anomalia valutabile durante la ricarica."
            )
        }

        if (observedDrainPerHour == null) {
            return LeakDiagnosis(
                severity = LeakSeverity.LEARNING,
                title = "Leak Detector in apprendimento",
                explanation = "Servono almeno due letture distanziate per misurare la scarica reale.",
                likelyCause = "Apri Battery Guardian più tardi per confrontare i campioni."
            )
        }

        val reasons = mutableListOf<Pair<Int, String>>()

        when {
            temperatureC >= 42f -> reasons += 5 to "temperatura batteria molto alta"
            temperatureC >= 38f -> reasons += 3 to "temperatura batteria elevata"
        }

        if (instantCurrentMa != null) {
            when {
                instantCurrentMa >= 2200 -> reasons += 5 to "assorbimento istantaneo estremamente alto"
                instantCurrentMa >= 1500 -> reasons += 4 to "assorbimento istantaneo alto"
                instantCurrentMa >= 900 -> reasons += 2 to "assorbimento istantaneo sopra la media"
            }
        }

        if (brightnessPercent != null) {
            when {
                brightnessPercent >= 90 -> reasons += 4 to "luminosità schermo quasi al massimo"
                brightnessPercent >= 75 -> reasons += 2 to "luminosità schermo elevata"
            }
        }

        if (budget.status == BudgetStatus.OVER_BUDGET) {
            reasons += 4 to "consumo reale superiore al Battery Budget impostato"
        }

        when {
            observedDrainPerHour >= 12f -> reasons += 5 to "scarica superiore al 12%/ora"
            observedDrainPerHour >= 8f -> reasons += 4 to "scarica superiore all'8%/ora"
            observedDrainPerHour >= 5f -> reasons += 2 to "scarica superiore al 5%/ora"
        }

        if (topApp != null && topApp.foregroundMinutes >= 120) {
            reasons += 2 to (topApp.label + " è stata molto presente in primo piano")
        }

        val best = reasons.maxByOrNull { it.first }
        val maxScore = best?.first ?: 0

        val severity = when {
            maxScore >= 5 -> LeakSeverity.CRITICAL
            maxScore >= 4 -> LeakSeverity.HIGH
            maxScore >= 2 -> LeakSeverity.WATCH
            else -> LeakSeverity.NORMAL
        }

        val title = when (severity) {
            LeakSeverity.CRITICAL -> "Battery Leak critico"
            LeakSeverity.HIGH -> "Battery Leak elevato"
            LeakSeverity.WATCH -> "Consumo da controllare"
            LeakSeverity.NORMAL -> "Nessun Battery Leak evidente"
            LeakSeverity.LEARNING -> "Leak Detector in apprendimento"
        }

        return LeakDiagnosis(
            severity = severity,
            title = title,
            explanation = if (budget.status == BudgetStatus.OVER_BUDGET) {
                "Stai consumando più velocemente di quanto consenta il tuo obiettivo di autonomia."
            } else {
                "Il consumo osservato è confrontato con temperatura, corrente, schermo e utilizzo recente."
            },
            likelyCause = best?.second ?: "Nessuna causa dominante rilevata."
        )
    }
}
