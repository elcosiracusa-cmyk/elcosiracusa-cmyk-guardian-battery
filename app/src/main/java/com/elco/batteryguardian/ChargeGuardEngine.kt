package com.elco.batteryguardian

data class ChargeGuardState(
    val active: Boolean,
    val severity: Int,
    val title: String,
    val message: String
)

object ChargeGuardEngine {
    fun evaluate(
        charging: Boolean,
        level: Int,
        temperatureC: Float,
        chargeLimit: Int
    ): ChargeGuardState {
        if (!charging) {
            return ChargeGuardState(
                active = false,
                severity = 0,
                title = "Charge Guard",
                message = "Nessuna ricarica in corso."
            )
        }

        return when {
            temperatureC >= 42f -> ChargeGuardState(
                active = true,
                severity = 3,
                title = "Charge Guard · Calore critico",
                message = "La batteria è molto calda durante la ricarica. Riduci il carico e scollega se possibile."
            )
            temperatureC >= 38f -> ChargeGuardState(
                active = true,
                severity = 2,
                title = "Charge Guard · Calore elevato",
                message = "Evita giochi, video pesanti e cover molto isolanti durante la ricarica."
            )
            level >= chargeLimit -> ChargeGuardState(
                active = true,
                severity = 1,
                title = "Charge Guard · Soglia raggiunta",
                message = "Hai raggiunto la soglia impostata del " + chargeLimit + "%."
            )
            else -> ChargeGuardState(
                active = true,
                severity = 0,
                title = "Charge Guard · OK",
                message = "Ricarica nei parametri impostati."
            )
        }
    }
}
