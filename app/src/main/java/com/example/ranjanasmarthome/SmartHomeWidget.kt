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
    }

    // Track toggle state for only 4 devices
    private val toggleState = mutableMapOf(
        ACTION_LIGHT1 to false,
        ACTION_FAN1 to false,
        ACTION_LIGHT2 to false,
        ACTION_FAN2 to false
    )

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_control)

            // Light1
            views.setOnClickPendingIntent(
                R.id.light1Button,
                getPendingSelfIntent(context, ACTION_LIGHT1)
            )
            // Fan1
            views.setOnClickPendingIntent(
                R.id.fan1Button,
                getPendingSelfIntent(context, ACTION_FAN1)
            )
            // Light2
            views.setOnClickPendingIntent(
                R.id.light2Button,
                getPendingSelfIntent(context, ACTION_LIGHT2)
            )
            // Fan2
            views.setOnClickPendingIntent(
                R.id.fan2Button,
                getPendingSelfIntent(context, ACTION_FAN2)
            )

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return

        when (action) {
            ACTION_LIGHT1 -> toggleRelay(context, ACTION_LIGHT1, 0)
            ACTION_FAN1 -> toggleRelay(context, ACTION_FAN1, 1)
            ACTION_LIGHT2 -> toggleRelay(context, ACTION_LIGHT2, 4)
            ACTION_FAN2 -> toggleRelay(context, ACTION_FAN2, 5)
        }
    }

    private fun toggleRelay(context: Context, actionKey: String, relayIndex: Int) {
        val state = !toggleState[actionKey]!! // toggle
        toggleState[actionKey] = state

        // Send 01/00 style command to ESP32
        val cmd = "${relayIndex}${if (state) "1" else "0"}"
        sendCommand(context, cmd)
    }

    private fun sendCommand(context: Context, cmd: String) {
        // Sends command via your BLE/MQTT logic
        BLEController.sendCommand(cmd)
    }

    private fun getPendingSelfIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, javaClass)
        intent.action = action
        return PendingIntent.getBroadcast(
            context, action.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }
}
