package com.example.ranjanasmarthome

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import android.util.Log

class SmartHomeWidget : AppWidgetProvider() {

    companion object {
        private const val TAG = "SmartHomeWidget"

        const val ACTION_LIGHT1 = "ACTION_LIGHT1"
        const val ACTION_LIGHT2 = "ACTION_LIGHT2"
        const val ACTION_FAN1 = "ACTION_FAN1"
        const val ACTION_FAN2 = "ACTION_FAN2"
    }

    // Keep widget toggle state
    private val toggleState = mutableMapOf(
        ACTION_LIGHT1 to false,
        ACTION_LIGHT2 to false,
        ACTION_FAN1 to false,
        ACTION_FAN2 to false
    )

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_control)

            // Set PendingIntents for buttons
            views.setOnClickPendingIntent(R.id.light1Button, getPendingSelfIntent(context, ACTION_LIGHT1))
            views.setOnClickPendingIntent(R.id.light2Button, getPendingSelfIntent(context, ACTION_LIGHT2))
            views.setOnClickPendingIntent(R.id.fan1Button, getPendingSelfIntent(context, ACTION_FAN1))
            views.setOnClickPendingIntent(R.id.fan2Button, getPendingSelfIntent(context, ACTION_FAN2))

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return

        try {
            when (action) {
                ACTION_LIGHT1 -> toggleRelay(context, 0, ACTION_LIGHT1)
                ACTION_LIGHT2 -> toggleRelay(context, 4, ACTION_LIGHT2)
                ACTION_FAN1 -> toggleRelay(context, 1, ACTION_FAN1)
                ACTION_FAN2 -> toggleRelay(context, 5, ACTION_FAN2)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle widget action: $action", e)
        }
    }

    private fun toggleRelay(context: Context, relayIndex: Int, actionKey: String) {
        // Toggle local state
        val state = !toggleState[actionKey]!!
        toggleState[actionKey] = state

        val cmd = "${relayIndex}${if (state) "1" else "0"}"

        // Send command safely
        try {
            BLEController.sendCommand(cmd)
        } catch (e: Exception) {
            Log.e(TAG, "BLE not ready, command not sent: $cmd", e)
        }
    }

    private fun getPendingSelfIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, javaClass)
        intent.action = action
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        else
            PendingIntent.FLAG_UPDATE_CURRENT
        return PendingIntent.getBroadcast(context, action.hashCode(), intent, flags)
    }
}
