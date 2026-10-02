package com.elco.batteryguardian

import android.content.Context

/**
 * Entitlement gateway.
 *
 * Important: this class does not fake purchases.
 * Until Google Play Billing is connected, premium remains locked.
 */
object EntitlementRepository {
    private const val PREFS = "guardian_entitlement"
    private const val KEY_PREMIUM = "premium_unlocked"

    fun isPremium(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_PREMIUM, false)

    fun isUnlocked(context: Context, feature: PremiumFeature): Boolean {
        return when (feature) {
            PremiumFeature.SMART_LEAK_DETECTOR,
            PremiumFeature.BATTERY_BUDGET,
            PremiumFeature.ADVANCED_WIDGETS,
            PremiumFeature.HISTORY_7_DAYS,
            PremiumFeature.SMART_ALERTS,
            PremiumFeature.CHARGE_GUARD,
            PremiumFeature.BATTERY_MODES,
            PremiumFeature.EXPORT_REPORT -> isPremium(context)
        }
    }

    /**
     * To be called only by verified Play Billing purchase restore / acknowledgement.
     */
    fun setPremiumVerified(context: Context, unlocked: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PREMIUM, unlocked)
            .apply()
    }
}
