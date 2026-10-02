package com.elco.batteryguardian

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlin.math.roundToInt

object BatteryAnalyzer {

    fun read(context: Context): BatterySnapshot {
        val intent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        val levelRaw = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val level = if (levelRaw >= 0 && scale > 0) {
            (levelRaw * 100f / scale).roundToInt().coerceIn(0, 100)
        } else 0

        val status = intent?.getIntExtra(
            BatteryManager.EXTRA_STATUS,
            BatteryManager.BATTERY_STATUS_UNKNOWN
        ) ?: BatteryManager.BATTERY_STATUS_UNKNOWN

        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val temperature = (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        val voltage = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0

        val currentNowUa = batteryManager
            .getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            .takeUnless { it == Int.MIN_VALUE }

        val chargeCounterUah = batteryManager
            .getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            .takeUnless { it == Int.MIN_VALUE }

        val health = intent?.getIntExtra(
            BatteryManager.EXTRA_HEALTH,
            BatteryManager.BATTERY_HEALTH_UNKNOWN
        ) ?: BatteryManager.BATTERY_HEALTH_UNKNOWN

        val healthLabel = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> LocaleText.pick(context, "Buona", "Good")
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> LocaleText.pick(context, "Surriscaldata", "Overheating")
            BatteryManager.BATTERY_HEALTH_DEAD -> LocaleText.pick(context, "Critica", "Critical")
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> LocaleText.pick(context, "Sovratensione", "Over voltage")
            BatteryManager.BATTERY_HEALTH_COLD -> LocaleText.pick(context, "Troppo fredda", "Too cold")
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> LocaleText.pick(context, "Anomalia", "Anomaly")
            else -> LocaleText.pick(context, "Non disponibile", "Unavailable")
        }

        var score = 100
        when {
            temperature >= 42f -> score -= 35
            temperature >= 38f -> score -= 18
            temperature >= 35f -> score -= 8
        }

        if (
            health != BatteryManager.BATTERY_HEALTH_GOOD &&
            health != BatteryManager.BATTERY_HEALTH_UNKNOWN
        ) {
            score -= 30
        }

        if (level <= 10) score -= 15
        else if (level <= 20) score -= 6

        val message = when {
            temperature >= 42f -> LocaleText.pick(context, "Temperatura molto alta: riduci carico e ricarica.", "Very high temperature: reduce load and charging.")
            temperature >= 38f -> LocaleText.pick(context, "Temperatura elevata: evita giochi, GPS e ricarica rapida.", "High temperature: avoid gaming, GPS and fast charging.")
            level <= 15 && !charging -> LocaleText.pick(context, "Batteria bassa: attiva il risparmio energetico.", "Low battery: enable Battery Saver.")
            charging && level >= 90 -> LocaleText.pick(context, "Carica alta: scollega quando non serve.", "High charge level: unplug when charging is no longer needed.")
            else -> LocaleText.pick(context, "Parametri nella norma. Continua il monitoraggio.", "Parameters are normal. Keep monitoring.")
        }

        return BatterySnapshot(
            level = level,
            charging = charging,
            temperatureC = temperature,
            voltageMv = voltage,
            currentNowUa = currentNowUa,
            chargeCounterUah = chargeCounterUah,
            healthLabel = healthLabel,
            score = score.coerceIn(0, 100),
            statusMessage = message
        )
    }
}
