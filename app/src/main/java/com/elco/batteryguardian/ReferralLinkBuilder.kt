package com.elco.batteryguardian

import android.content.Context
import android.content.Intent
import android.net.Uri

object ReferralLinkBuilder {
    private const val PLAY_BASE =
        "https://play.google.com/store/apps/details?id=com.elco.batteryguardian"

    fun buildInviteUrl(context: Context): String {
        val code = ReferralRepository.referralCode(context)
        val referrer = Uri.encode("referrer_code=$code")
        return "$PLAY_BASE&referrer=$referrer"
    }

    fun shareIntent(context: Context): Intent {
        val link = buildInviteUrl(context)
        val code = ReferralRepository.referralCode(context)
        val text = "Prova Battery Guardian e migliora il controllo della batteria. " +
            "Codice invito: $code\n$link"

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
    }
}
