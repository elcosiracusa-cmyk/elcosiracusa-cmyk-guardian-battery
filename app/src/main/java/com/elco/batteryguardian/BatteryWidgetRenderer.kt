package com.elco.batteryguardian

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.text.DateFormat
import java.util.Date
import kotlin.math.abs

object BatteryWidgetRenderer {

    const val ACTION_REFRESH_COMPACT =
        "com.elco.batteryguardian.action.REFRESH_COMPACT"
    const val ACTION_REFRESH_ACTIVITY =
        "com.elco.batteryguardian.action.REFRESH_ACTIVITY"

    fun updateCompact(context: Context, manager: AppWidgetManager, ids: IntArray) {
        if (ids.isEmpty()) return

        val snapshot = BatteryAnalyzer.read(context)
        val currentMa = snapshot.currentNowUa
            ?.takeIf { it != Int.MIN_VALUE && it != 0 }
            ?.let { abs(it) / 1000 }

        val powerW = if (currentMa != null && snapshot.voltageMv > 0) {
            (currentMa * snapshot.voltageMv) / 1_000_000f
        } else null

        ids.forEach { id ->
            val views = RemoteViews(
                context.packageName,
                R.layout.widget_battery_compact
            )

            views.setTextViewText(
                R.id.widgetBatteryLevel,
                context.getString(R.string.widget_percent, snapshot.level)
            )
            views.setTextViewText(
                R.id.widgetBatteryScore,
                context.getString(R.string.widget_score, snapshot.score)
            )
            views.setTextViewText(
                R.id.widgetBatteryCurrent,
                currentMa?.let {
                    context.getString(R.string.widget_current, it)
                } ?: context.getString(R.string.widget_current_unknown)
            )
            views.setTextViewText(
                R.id.widgetBatteryPower,
                powerW?.let {
                    context.getString(R.string.widget_power, it)
                } ?: context.getString(R.string.widget_power_unknown)
            )
            views.setTextViewText(
                R.id.widgetBatteryTemperature,
                context.getString(
                    R.string.widget_temperature,
                    snapshot.temperatureC
                )
            )

            views.setOnClickPendingIntent(
                R.id.widgetRefreshButton,
                refreshPendingIntent(
                    context,
                    BatteryCompactWidgetProvider::class.java,
                    ACTION_REFRESH_COMPACT,
                    id
                )
            )
            views.setOnClickPendingIntent(
                R.id.widgetRoot,
                openAppPendingIntent(context, id)
            )

            manager.updateAppWidget(id, views)
        }
    }

    fun updateActivity(context: Context, manager: AppWidgetManager, ids: IntArray) {
        if (ids.isEmpty()) return

        val snapshot = BatteryAnalyzer.read(context)
        val history = BatteryHistoryStore.recordAndEstimate(
            context = context,
            level = snapshot.level,
            charging = snapshot.charging
        )
        val efficiency = PowerEfficiencyEngine.analyze(
            context = context,
            level = snapshot.level,
            charging = snapshot.charging,
            voltageMv = snapshot.voltageMv,
            currentNowUa = snapshot.currentNowUa,
            chargeCounterUah = snapshot.chargeCounterUah
        )

        val topApp = if (UsageAnalyzer.hasUsageAccess(context)) {
            UsageAnalyzer.topApps(context, 1).firstOrNull()
        } else null

        val lastUpdate = DateFormat.getTimeInstance(DateFormat.SHORT)
            .format(Date())

        ids.forEach { id ->
            val views = RemoteViews(
                context.packageName,
                R.layout.widget_battery_activity
            )

            views.setTextViewText(
                R.id.widgetActivityLevel,
                context.getString(R.string.widget_percent, snapshot.level)
            )
            views.setTextViewText(
                R.id.widgetActivityPressure,
                context.getString(
                    R.string.widget_pressure,
                    efficiency.efficiencyLabel
                )
            )
            views.setTextViewText(
                R.id.widgetActivityDrain,
                history.percentPerHour?.let {
                    context.getString(R.string.widget_drain, it)
                } ?: context.getString(R.string.widget_drain_learning)
            )
            views.setTextViewText(
                R.id.widgetActivityTopApp,
                topApp?.let {
                    context.getString(
                        R.string.widget_top_app,
                        it.label,
                        it.foregroundMinutes
                    )
                } ?: context.getString(R.string.widget_top_app_unavailable)
            )
            views.setTextViewText(
                R.id.widgetActivityGuardian,
                context.getString(R.string.widget_zero_wake)
            )
            views.setTextViewText(
                R.id.widgetActivityUpdated,
                context.getString(
                    R.string.widget_last_update,
                    lastUpdate
                )
            )

            views.setOnClickPendingIntent(
                R.id.widgetActivityRefreshButton,
                refreshPendingIntent(
                    context,
                    BatteryActivityWidgetProvider::class.java,
                    ACTION_REFRESH_ACTIVITY,
                    id
                )
            )
            views.setOnClickPendingIntent(
                R.id.widgetActivityRoot,
                openAppPendingIntent(context, id + 10000)
            )

            manager.updateAppWidget(id, views)
        }
    }

    fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)

        val compactIds = manager.getAppWidgetIds(
            ComponentName(context, BatteryCompactWidgetProvider::class.java)
        )
        updateCompact(context, manager, compactIds)

        val activityIds = manager.getAppWidgetIds(
            ComponentName(context, BatteryActivityWidgetProvider::class.java)
        )
        updateActivity(context, manager, activityIds)
    }

    private fun refreshPendingIntent(
        context: Context,
        providerClass: Class<*>,
        action: String,
        widgetId: Int
    ): PendingIntent {
        val intent = Intent(context, providerClass).apply {
            this.action = action
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
        return PendingIntent.getBroadcast(
            context,
            widgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun openAppPendingIntent(
        context: Context,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
