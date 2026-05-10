package com.example.ranjanasmarthome

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews

class SmartHomeWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_LIGHT1 = "ACTION_LIGHT1"
        const val ACTION_LIGHT2 = "ACTION_LIGHT2"
        const val ACTION_FAN1 = "ACTION_FAN1"
        const val ACTION_FAN2 = "ACTION_FAN2"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_LIGHT1 -> BLEController.toggleDevice("L1")
            ACTION_LIGHT2 -> BLEController.toggleDevice("L2")
            ACTION_FAN1 -> BLEController.toggleDevice("F1")
            ACTION_FAN2 -> BLEController.toggleDevice("F2")
        }

        // Update all widgets after a change
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(
            context.packageManager.getReceiverInfo(
                context.packageManager.getReceiverComponentName(this)
            )
        )
        for (id in ids) {
            updateWidget(context, manager, id)
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_control)

        // Set up intents for each button
        views.setOnClickPendingIntent(R.id.btnLight1, getPendingIntent(context, ACTION_LIGHT1))
        views.setOnClickPendingIntent(R.id.btnLight2, getPendingIntent(context, ACTION_LIGHT2))
        views.setOnClickPendingIntent(R.id.btnFan1, getPendingIntent(context, ACTION_FAN1))
        views.setOnClickPendingIntent(R.id.btnFan2, getPendingIntent(context, ACTION_FAN2))

        // Update button colors based on BLEController state
        views.setInt(
            R.id.btnLight1, "setBackgroundColor",
            if (BLEController.isDeviceOn("L1")) Color.YELLOW else Color.DKGRAY
        )
        views.setInt(
            R.id.btnLight2, "setBackgroundColor",
            if (BLEController.isDeviceOn("L2")) Color.YELLOW else Color.DKGRAY
        )
        views.setInt(
            R.id.btnFan1, "setBackgroundColor",
            if (BLEController.isDeviceOn("F1")) Color.CYAN else Color.DKGRAY
        )
        views.setInt(
            R.id.btnFan2, "setBackgroundColor",
            if (BLEController.isDeviceOn("F2")) Color.CYAN else Color.DKGRAY
        )

        appWidgetManager.updateAppWidget(widgetId, views)
    }

    private fun getPendingIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, SmartHomeWidget::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
