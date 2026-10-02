package com.elco.batteryguardian

import android.content.Context
import java.util.UUID

data class ReferralProgress(
    val referralCode: String,
    val verifiedInvites: Int,
    val requiredInvites: Int,
    val rewardActive: Boolean,
    val rewardExpiresAt: Long?
)

object ReferralRepository {
    private const val PREFS = "guardian_referrals"
    private const val KEY_CODE = "referral_code"
    private const val KEY_VERIFIED = "verified_invites"
    private const val KEY_REWARD_EXPIRES = "reward_expires_at"
    const val REQUIRED_INVITES = 5
    const val REWARD_DAYS = 30

    fun referralCode(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_CODE, null)
        if (!existing.isNullOrBlank()) return existing

        val code = "BG-" + UUID.randomUUID()
            .toString()
            .replace("-", "")
            .take(8)
            .uppercase()

        prefs.edit().putString(KEY_CODE, code).apply()
        return code
    }

    fun progress(context: Context): ReferralProgress {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val verified = prefs.getInt(KEY_VERIFIED, 0).coerceAtLeast(0)
        val expiresAt = prefs.getLong(KEY_REWARD_EXPIRES, 0L).takeIf { it > 0L }
        val active = expiresAt?.let { it > System.currentTimeMillis() } ?: false

        return ReferralProgress(
            referralCode = referralCode(context),
            verifiedInvites = verified,
            requiredInvites = REQUIRED_INVITES,
            rewardActive = active,
            rewardExpiresAt = expiresAt
        )
    }

    /**
     * Must only be called after a trusted backend verifies a referred install.
     */
    fun setVerifiedInviteCount(context: Context, count: Int) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val safeCount = count.coerceAtLeast(0)
        prefs.edit().putInt(KEY_VERIFIED, safeCount).apply()

        if (safeCount >= REQUIRED_INVITES && !isRewardActive(context)) {
            activateThirtyDayReward(context)
        }
    }

    fun isRewardActive(context: Context): Boolean {
        val expires = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_REWARD_EXPIRES, 0L)
        return expires > System.currentTimeMillis()
    }

    private fun activateThirtyDayReward(context: Context) {
        val expires = System.currentTimeMillis() +
            REWARD_DAYS * 24L * 60L * 60L * 1000L

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_REWARD_EXPIRES, expires)
            .apply()
    }
}
