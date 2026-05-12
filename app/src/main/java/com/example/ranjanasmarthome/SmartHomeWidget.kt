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

    // Tracks toggle state locally for sending commands
    private val toggleState = mutableMapOf(
        ACTION_LIGHT1 to false,
        ACTION_LIGHT2 to false,
        ACTION_FAN1 to false,
        ACTION_FAN2 to false
    )

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_control)

            views.setOnClickPendingIntent(R.id.light1Button, getPendingIntent(context, ACTION_LIGHT1))
            views.setOnClickPendingIntent(R.id.light2Button, getPendingIntent(context, ACTION_LIGHT2))
            views.setOnClickPendingIntent(R.id.fan1Button, getPendingIntent(context, ACTION_FAN1))
            views.setOnClickPendingIntent(R.id.fan2Button, getPendingIntent(context, ACTION_FAN2))

            appWidgetManager.updateAppWidget(id, views)
        }

        // Register BLE & MQTT update callbacks
        BLEController.onStateUpdate = { l1, f1, l2, f2 -> updateWidgetUI(context, l1, f1, l2, f2) }
        MQTTController.onStateUpdate = { l1, f1, l2, f2 -> updateWidgetUI(context, l1, f1, l2, f2) }

        // Connect MQTT if not connected
        MQTTController.connect()
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return

        when (action) {
            ACTION_LIGHT1 -> toggleRelay(context, 0, ACTION_LIGHT1)
            ACTION_LIGHT2 -> toggleRelay(context, 4, ACTION_LIGHT2)
            ACTION_FAN1   -> toggleRelay(context, 1, ACTION_FAN1)
            ACTION_FAN2   -> toggleRelay(context, 5, ACTION_FAN2)
        }
    }

    private fun toggleRelay(context: Context, relayIndex: Int, actionKey: String) {
        val newState = !toggleState[actionKey]!!
        toggleState[actionKey] = newState
        val cmd = "$relayIndex${if (newState) 1 else 0}"

        // Send commands to both BLE and MQTT
        BLEController.sendCommand(cmd)
        MQTTController.sendCommand(cmd)

        // Immediately update widget UI to reflect new state
        val currentState = getCurrentWidgetState()
        when (relayIndex) {
            0 -> updateWidgetUI(context, newState, currentState.fan1, currentState.light2, currentState.fan2)
            4 -> updateWidgetUI(context, currentState.light1, currentState.fan1, newState, currentState.fan2)
            1 -> updateWidgetUI(context, currentState.light1, newState, currentState.light2, currentState.fan2)
            5 -> updateWidgetUI(context, currentState.light1, currentState.fan1, currentState.light2, newState)
        }
    }

    private fun updateWidgetUI(context: Context, l1: Boolean, f1: Boolean, l2: Boolean, f2: Boolean) {
        val views = RemoteViews(context.packageName, R.layout.widget_control)

        views.setInt(R.id.light1Button, "setBackgroundColor", if (l1) 0xFFFFAA00.toInt() else 0xFF555555.toInt())
        views.setInt(R.id.light2Button, "setBackgroundColor", if (l2) 0xFFFFAA00.toInt() else 0xFF555555.toInt())
        views.setInt(R.id.fan1Button, "setBackgroundColor", if (f1) 0xFF00FF00.toInt() else 0xFF555555.toInt())
        views.setInt(R.id.fan2Button, "setBackgroundColor", if (f2) 0xFF00FF00.toInt() else 0xFF555555.toInt())

        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, SmartHomeWidget::class.java))
        for (id in ids) {
            manager.updateAppWidget(id, views)
        }
    }

    private fun getPendingIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, SmartHomeWidget::class.java)
        intent.action = action
        return PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    // Query current toggleState to use for UI update
    private fun getCurrentWidgetState(): WidgetStateData {
        return WidgetStateData(
            light1 = toggleState[ACTION_LIGHT1] ?: false,
            fan1   = toggleState[ACTION_FAN1] ?: false,
            light2 = toggleState[ACTION_LIGHT2] ?: false,
            fan2   = toggleState[ACTION_FAN2] ?: false
        )
    }

    data class WidgetStateData(
        val light1: Boolean,
        val fan1: Boolean,
        val light2: Boolean,
        val fan2: Boolean
    )
}
