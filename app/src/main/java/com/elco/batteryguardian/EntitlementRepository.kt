package com.elco.batteryguardian

import android.content.Context

object EntitlementRepository {
    private const val PREFS = "guardian_entitlement"
    private const val KEY_PREMIUM = "premium_unlocked"
    private const val KEY_REWARD_UNTIL = "reward_premium_until"

    fun isPurchasedPremium(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_PREMIUM, false)

    fun rewardPremiumUntil(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_REWARD_UNTIL, 0L)

    fun isRewardPremium(context: Context, now: Long = System.currentTimeMillis()): Boolean =
        rewardPremiumUntil(context) > now

    fun isPremium(context: Context): Boolean =
        isPurchasedPremium(context) || isRewardPremium(context)

    fun isUnlocked(context: Context, feature: PremiumFeature): Boolean = isPremium(context)

    fun premiumSource(context: Context): String = when {
        isPurchasedPremium(context) -> "Plus permanente"
        isRewardPremium(context) -> "Plus premio inviti"
        else -> "Free"
    }

    fun rewardDaysRemaining(context: Context, now: Long = System.currentTimeMillis()): Int {
        val remaining = rewardPremiumUntil(context) - now
        if (remaining <= 0L) return 0
        val day = 24L * 60L * 60L * 1000L
        return ((remaining + day - 1L) / day).toInt()
    }

    fun setPremiumVerified(context: Context, unlocked: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_PREMIUM, unlocked).apply()
    }

    fun grantReferralRewardVerified(context: Context, durationDays: Int = 30) {
        val now = System.currentTimeMillis()
        val existing = rewardPremiumUntil(context).coerceAtLeast(now)
        val duration = durationDays.coerceIn(1, 365).toLong() * 24L * 60L * 60L * 1000L
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putLong(KEY_REWARD_UNTIL, existing + duration).apply()
    }
}
