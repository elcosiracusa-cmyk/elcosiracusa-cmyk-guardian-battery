package com.elco.batteryguardian

import android.content.Context
import java.util.UUID

data class ReferralStatus(
    val referralCode: String,
    val verifiedFriends: Int,
    val requiredFriends: Int = 5,
    val rewardDays: Int = 30
)

object ReferralRewardManager {
    private const val PREFS = "guardian_referrals"
    private const val KEY_CODE = "referral_code"
    private const val KEY_VERIFIED = "verified_friends"

    fun status(context: Context): ReferralStatus {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        var code = prefs.getString(KEY_CODE, null)
        if (code.isNullOrBlank()) {
            code = "BG-" + UUID.randomUUID().toString()
                .replace("-", "").take(8).uppercase()
            prefs.edit().putString(KEY_CODE, code).apply()
        }
        return ReferralStatus(
            referralCode = code,
            verifiedFriends = prefs.getInt(KEY_VERIFIED, 0).coerceAtLeast(0)
        )
    }

    fun setVerifiedFriendsFromTrustedSource(context: Context, count: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_VERIFIED, count.coerceAtLeast(0)).apply()
    }

    fun shareText(context: Context): String {
        val code = status(context).referralCode
        return "Prova Battery Guardian. Usa il mio codice invito $code. " +
            "Dopo 5 amici verificati ottengo Plus gratis per 30 giorni."
    }
}
