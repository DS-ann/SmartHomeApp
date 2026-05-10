// SmartHomeWidget.kt
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
        const val ACTION_LIGHT1 = "com.example.ranjanasmarthome.LIGHT1"
        const val ACTION_LIGHT2 = "com.example.ranjanasmarthome.LIGHT2"
        const val ACTION_FAN1 = "com.example.ranjanasmarthome.FAN1"
        const val ACTION_FAN2 = "com.example.ranjanasmarthome.FAN2"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_control)

            // Setup button intents
            views.setOnClickPendingIntent(R.id.btnLight1, getPendingIntent(context, ACTION_LIGHT1))
            views.setOnClickPendingIntent(R.id.btnLight2, getPendingIntent(context, ACTION_LIGHT2))
            views.setOnClickPendingIntent(R.id.btnFan1, getPendingIntent(context, ACTION_FAN1))
            views.setOnClickPendingIntent(R.id.btnFan2, getPendingIntent(context, ACTION_FAN2))

            appWidgetManager.updateAppWidget(widgetId, views)
        }
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

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return

        // Send BLE command using your BLEController singleton
        when (action) {
            ACTION_LIGHT1 -> BLEController.sendCommand("LIGHT1_TOGGLE")
            ACTION_LIGHT2 -> BLEController.sendCommand("LIGHT2_TOGGLE")
            ACTION_FAN1 -> BLEController.sendCommand("FAN1_TOGGLE")
            ACTION_FAN2 -> BLEController.sendCommand("FAN2_TOGGLE")
        }

        // Update widget appearance after command
        updateWidgetUI(context)
    }

    private fun updateWidgetUI(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, SmartHomeWidget::class.java))
        val views = RemoteViews(context.packageName, R.layout.widget_control)

        // For simplicity, just reset to default colors
        views.setTextColor(R.id.btnLight1, 0xFFFFFFFF.toInt())
        views.setTextColor(R.id.btnLight2, 0xFFFFFFFF.toInt())
        views.setTextColor(R.id.btnFan1, 0xFFFFFFFF.toInt())
        views.setTextColor(R.id.btnFan2, 0xFFFFFFFF.toInt())

        manager.updateAppWidget(ids, views)
    }
}
