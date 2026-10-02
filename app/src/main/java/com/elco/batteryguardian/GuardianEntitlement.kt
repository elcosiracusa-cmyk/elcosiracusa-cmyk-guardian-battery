package com.elco.batteryguardian

import android.content.Context

enum class PlusSource {
    NONE,
    PURCHASE,
    REFERRAL_REWARD
}

data class GuardianEntitlementState(
    val active: Boolean,
    val source: PlusSource,
    val expiresAt: Long?
)

object GuardianEntitlement {
    fun state(context: Context): GuardianEntitlementState {
        if (EntitlementRepository.isPremium(context)) {
            return GuardianEntitlementState(
                active = true,
                source = PlusSource.PURCHASE,
                expiresAt = null
            )
        }

        val referral = ReferralRepository.progress(context)
        if (referral.rewardActive) {
            return GuardianEntitlementState(
                active = true,
                source = PlusSource.REFERRAL_REWARD,
                expiresAt = referral.rewardExpiresAt
            )
        }

        return GuardianEntitlementState(
            active = false,
            source = PlusSource.NONE,
            expiresAt = null
        )
    }

    fun isPlus(context: Context): Boolean = state(context).active
}
