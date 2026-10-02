package com.elco.batteryguardian

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent

class BatteryActivityWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        BatteryWidgetRenderer.updateActivity(
            context,
            appWidgetManager,
            appWidgetIds
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == BatteryWidgetRenderer.ACTION_REFRESH_ACTIVITY) {
            val widgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                BatteryWidgetRenderer.updateActivity(
                    context,
                    AppWidgetManager.getInstance(context),
                    intArrayOf(widgetId)
                )
            }
        }
    }
}
