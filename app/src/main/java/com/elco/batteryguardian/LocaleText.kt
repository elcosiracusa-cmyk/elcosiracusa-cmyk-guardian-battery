package com.elco.batteryguardian

import android.content.Context

object LocaleText {
    fun pick(context: Context, italian: String, english: String): String {
        val language = context.resources.configuration.locales[0]?.language ?: "it"
        return if (language.startsWith("en", ignoreCase = true)) english else italian
    }
}
