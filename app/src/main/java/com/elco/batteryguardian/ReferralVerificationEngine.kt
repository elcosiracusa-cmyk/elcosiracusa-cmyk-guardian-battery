package com.elco.batteryguardian

import android.content.Context

data class ReferralVerificationResult(
    val verifiedFriends: Int,
    val rewardGranted: Boolean
)

object ReferralVerificationEngine {
    fun applyVerifiedReferralCount(
        context: Context,
        verifiedFriendsFromServer: Int
    ): ReferralVerificationResult {
        val before = ReferralRewardManager.status(context)
        val safeCount = verifiedFriendsFromServer.coerceIn(0, 1000)
        ReferralRewardManager.setVerifiedFriendsFromTrustedSource(context, safeCount)
        val after = ReferralRewardManager.status(context)

        val crossedThreshold =
            before.verifiedFriends < before.requiredFriends &&
            after.verifiedFriends >= after.requiredFriends

        if (crossedThreshold) {
            EntitlementRepository.grantReferralRewardVerified(
                context,
                after.rewardDays
            )
        }
        return ReferralVerificationResult(after.verifiedFriends, crossedThreshold)
    }
}
