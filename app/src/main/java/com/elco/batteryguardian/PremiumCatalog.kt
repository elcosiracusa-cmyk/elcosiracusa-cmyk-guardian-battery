package com.elco.batteryguardian

data class PremiumItem(
    val feature: PremiumFeature,
    val title: String,
    val description: String
)

object PremiumCatalog {
    fun items(): List<PremiumItem> = listOf(
        PremiumItem(
            PremiumFeature.SMART_LEAK_DETECTOR,
            "Smart Leak Detector",
            "Rileva scariche anomale e indica la causa più probabile."
        ),
        PremiumItem(
            PremiumFeature.BATTERY_BUDGET,
            "Battery Budget",
            "Imposta quante ore deve durare il telefono e quanto % vuoi conservare."
        ),
        PremiumItem(
            PremiumFeature.ADVANCED_WIDGETS,
            "Widget avanzati",
            "Widget con consumo, autonomia, pressione energetica e causa probabile."
        ),
        PremiumItem(
            PremiumFeature.HISTORY_7_DAYS,
            "Storico 7 giorni",
            "Mostra andamento di scarica, temperatura e consumo nel tempo."
        ),
        PremiumItem(
            PremiumFeature.SMART_ALERTS,
            "Smart Alerts",
            "Avvisi intelligenti solo quando il consumo supera davvero la norma."
        ),
        PremiumItem(
            PremiumFeature.CHARGE_GUARD,
            "Charge Guard",
            "Avvisa su surriscaldamento, carica prolungata e soglie personalizzate."
        ),
        PremiumItem(
            PremiumFeature.BATTERY_MODES,
            "Battery Modes",
            "Profili Notte, Viaggio, Emergenza e Daily con suggerimenti dedicati."
        ),
        PremiumItem(
            PremiumFeature.EXPORT_REPORT,
            "Battery Report",
            "Esporta un report tecnico leggibile con salute, consumo e anomalie."
        )
    )
}
