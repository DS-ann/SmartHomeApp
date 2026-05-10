package com.example.ranjanasmarthome

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class SmartHomeWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_LIGHT1 = "ACTION_LIGHT1"
        const val ACTION_LIGHT2 = "ACTION_LIGHT2"
        const val ACTION_FAN1 = "ACTION_FAN1"
        const val ACTION_FAN2 = "ACTION_FAN2"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val cmd = when (intent.action) {
            ACTION_LIGHT1 -> "LIGHT1_TOGGLE"
            ACTION_LIGHT2 -> "LIGHT2_TOGGLE"
            ACTION_FAN1 -> "FAN1_TOGGLE"
            ACTION_FAN2 -> "FAN2_TOGGLE"
            else -> null
        }

        cmd?.let {
            // Send to BLE via MainActivity
            BLEController.sendCommand(it, context)
        }

        // Refresh widget after action
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, SmartHomeWidget::class.java))
        ids.forEach { updateWidget(context, manager, it) }
    }

    private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_control)

        views.setOnClickPendingIntent(R.id.btnLight1, getPendingIntent(context, ACTION_LIGHT1))
        views.setOnClickPendingIntent(R.id.btnLight2, getPendingIntent(context, ACTION_LIGHT2))
        views.setOnClickPendingIntent(R.id.btnFan1, getPendingIntent(context, ACTION_FAN1))
        views.setOnClickPendingIntent(R.id.btnFan2, getPendingIntent(context, ACTION_FAN2))

        manager.updateAppWidget(widgetId, views)
    }

    private fun getPendingIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, SmartHomeWidget::class.java).apply { this.action = action }
        return PendingIntent.getBroadcast(
            context, action.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
