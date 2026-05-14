package com.example.ranjanasmarthome

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews

private const val TAG = "SmartHomeWidget"

class SmartHomeWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_LIGHT1 = "ACTION_LIGHT1"
        const val ACTION_LIGHT2 = "ACTION_LIGHT2"
        const val ACTION_FAN1 = "ACTION_FAN1"
        const val ACTION_FAN2 = "ACTION_FAN2"
    }

    // Store states with correct types: Boolean for lights, Int for fans
    private val toggleState = mutableMapOf<String, Any>(
        ACTION_LIGHT1 to false,
        ACTION_FAN1 to 0,
        ACTION_LIGHT2 to false,
        ACTION_FAN2 to 0
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

        // Register BLE & MQTT callbacks
        BLEController.onStateUpdate = { l1, f1, l2, f2 ->
            Log.d(TAG, "BLE update: L1=$l1 F1=$f1 L2=$l2 F2=$f2")
            updateWidgetUI(context, l1, f1, l2, f2)
            syncInternalState(l1, f1, l2, f2)
        }

        MQTTController.onStateUpdate = { l1, f1, l2, f2 ->
            Log.d(TAG, "MQTT update: L1=$l1 F1=$f1 L2=$l2 F2=$f2")
            updateWidgetUI(context, l1, f1, l2, f2)
            syncInternalState(l1, f1, l2, f2)
        }

        MQTTController.connect()
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        when (action) {
            ACTION_LIGHT1 -> toggleRelay(context, ACTION_LIGHT1, isLight = true)
            ACTION_LIGHT2 -> toggleRelay(context, ACTION_LIGHT2, isLight = true)
            ACTION_FAN1 -> toggleRelay(context, ACTION_FAN1, isLight = false)
            ACTION_FAN2 -> toggleRelay(context, ACTION_FAN2, isLight = false)
        }
    }

    private fun toggleRelay(context: Context, key: String, isLight: Boolean) {
        if (isLight) {
            val current = toggleState[key] as? Boolean ?: false
            val newState = !current
            toggleState[key] = newState
            val relayIndex = when (key) {
                ACTION_LIGHT1 -> 0
                ACTION_LIGHT2 -> 4
                else -> 0
            }
            val cmd = "$relayIndex${if (newState) 1 else 0}"
            BLEController.sendCommand(cmd)
            MQTTController.sendCommand(cmd)
        } else {
            // Fan toggle: simple on/off for widget (0 or 100)
            val current = toggleState[key] as? Int ?: 0
            val newState = if (current == 0) 100 else 0
            toggleState[key] = newState
            val relayIndex = when (key) {
                ACTION_FAN1 -> 1
                ACTION_FAN2 -> 5
                else -> 1
            }
            val cmd = "$relayIndex${if (newState > 0) 1 else 0}" // ESP32 expects 0/1
            BLEController.sendCommand(cmd)
            MQTTController.sendCommand(cmd)
        }

        // Update UI
        updateWidgetUI(
            context,
            toggleState[ACTION_LIGHT1] as Boolean,
            toggleState[ACTION_FAN1] as Int,
            toggleState[ACTION_LIGHT2] as Boolean,
            toggleState[ACTION_FAN2] as Int
        )
    }

    private fun syncInternalState(l1: Boolean, f1: Int, l2: Boolean, f2: Int) {
        toggleState[ACTION_LIGHT1] = l1
        toggleState[ACTION_FAN1] = f1
        toggleState[ACTION_LIGHT2] = l2
        toggleState[ACTION_FAN2] = f2
    }

    private fun updateWidgetUI(context: Context, l1: Boolean, f1: Int, l2: Boolean, f2: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_control)
        views.setInt(R.id.light1Button, "setBackgroundColor", if (l1) 0xFFFFAA00.toInt() else 0xFF555555.toInt())
        views.setInt(R.id.light2Button, "setBackgroundColor", if (l2) 0xFFFFAA00.toInt() else 0xFF555555.toInt())
        views.setInt(R.id.fan1Button, "setBackgroundColor", if (f1 > 0) 0xFF00FF00.toInt() else 0xFF555555.toInt())
        views.setInt(R.id.fan2Button, "setBackgroundColor", if (f2 > 0) 0xFF00FF00.toInt() else 0xFF555555.toInt())

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
}
