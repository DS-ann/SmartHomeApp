package com.example.ranjanasmarthome

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class SmartHomeWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_LIGHT1 = "ACTION_LIGHT1"
        const val ACTION_LIGHT2 = "ACTION_LIGHT2"
        const val ACTION_FAN1 = "ACTION_FAN1"
        const val ACTION_FAN2 = "ACTION_FAN2"

        // Maintain toggle states
        val toggleState = mutableMapOf(
            ACTION_LIGHT1 to false,
            ACTION_LIGHT2 to false,
            ACTION_FAN1 to false,
            ACTION_FAN2 to false
        )
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_control)

            // Set click actions
            views.setOnClickPendingIntent(R.id.light1Button, getPendingSelfIntent(context, ACTION_LIGHT1))
            views.setOnClickPendingIntent(R.id.light2Button, getPendingSelfIntent(context, ACTION_LIGHT2))
            views.setOnClickPendingIntent(R.id.fan1Button, getPendingSelfIntent(context, ACTION_FAN1))
            views.setOnClickPendingIntent(R.id.fan2Button, getPendingSelfIntent(context, ACTION_FAN2))

            // Update UI based on toggleState
            updateButtonViews(views)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            context.getPackageName().let { ComponentName(context, SmartHomeWidget::class.java) }
        )

        // Handle widget button clicks
        when (action) {
            ACTION_LIGHT1 -> toggleRelay(context, 0, ACTION_LIGHT1)
            ACTION_LIGHT2 -> toggleRelay(context, 4, ACTION_LIGHT2)
            ACTION_FAN1 -> toggleRelay(context, 1, ACTION_FAN1)
            ACTION_FAN2 -> toggleRelay(context, 5, ACTION_FAN2)
        }

        // Update the widget after action
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_control)
            updateButtonViews(views)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    // Toggle relay
    private fun toggleRelay(context: Context, relayIndex: Int, actionKey: String) {
        val newState = !(toggleState[actionKey] ?: false)
        toggleState[actionKey] = newState
        val cmd = "$relayIndex${if (newState) "1" else "0"}"

        // Send command via BLE or MQTT safely
        if (BLEController.isConnected()) {
            BLEController.sendCommand(cmd)
        } else if (MQTTController.isConnected()) {
            MQTTController.sendCommand(cmd)
        }
    }

    // Update button UI (can replace with images for ON/OFF)
    private fun updateButtonViews(views: RemoteViews) {
        views.setInt(R.id.light1Button, "setBackgroundColor",
            if (toggleState[ACTION_LIGHT1] == true) 0xFFFFA000.toInt() else 0xFF444444.toInt())
        views.setInt(R.id.light2Button, "setBackgroundColor",
            if (toggleState[ACTION_LIGHT2] == true) 0xFFFFA000.toInt() else 0xFF444444.toInt())
        views.setInt(R.id.fan1Button, "setBackgroundColor",
            if (toggleState[ACTION_FAN1] == true) 0xFF00C853.toInt() else 0xFF444444.toInt())
        views.setInt(R.id.fan2Button, "setBackgroundColor",
            if (toggleState[ACTION_FAN2] == true) 0xFF00C853.toInt() else 0xFF444444.toInt())
    }

    private fun getPendingSelfIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, javaClass)
        intent.action = action
        return PendingIntent.getBroadcast(
            context, action.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    // Call this from BLEController/MQTTController when ESP sends updates
    fun updateStateFromESP(light0: Boolean, light4: Boolean, fan1: Boolean, fan5: Boolean, context: Context) {
        toggleState[ACTION_LIGHT1] = light0
        toggleState[ACTION_LIGHT2] = light4
        toggleState[ACTION_FAN1] = fan1
        toggleState[ACTION_FAN2] = fan5

        // Refresh widget
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, SmartHomeWidget::class.java)
        )
        onUpdate(context, appWidgetManager, appWidgetIds)
    }
}
