package com.example.ranjanasmarthome

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.JobIntentService
import androidx.core.content.ContextCompat
import android.util.Log

// Action constants for toggles
private const val ACTION_TOGGLE_LIGHT0 = "ACTION_TOGGLE_LIGHT0"
private const val ACTION_TOGGLE_FAN1 = "ACTION_TOGGLE_FAN1"
private const val ACTION_TOGGLE_LIGHT4 = "ACTION_TOGGLE_LIGHT4"
private const val ACTION_TOGGLE_FAN5 = "ACTION_TOGGLE_FAN5"

class SmartHomeWidget : AppWidgetProvider() {

    // Track state locally (0 = OFF, 1 = ON)
    private var stateLight0 = 0
    private var stateFan1 = 0
    private var stateLight4 = 0
    private var stateFan5 = 0

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_TOGGLE_LIGHT0 -> stateLight0 = toggleRelay(context, 0, stateLight0)
            ACTION_TOGGLE_FAN1 -> stateFan1 = toggleRelay(context, 1, stateFan1)
            ACTION_TOGGLE_LIGHT4 -> stateLight4 = toggleRelay(context, 4, stateLight4)
            ACTION_TOGGLE_FAN5 -> stateFan5 = toggleRelay(context, 5, stateFan5)
        }
    }

    private fun toggleRelay(context: Context, relayId: Int, currentState: Int): Int {
        val newState = if (currentState == 0) 1 else 0

        // Send command to ESP32 via MQTT or BLE
        SmartHomeService.sendCommand("${relayId}${newState}")  // Example: "01" → relay 0 ON

        // Update widget UI
        updateAllWidgets(context)

        return newState
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        // Set toggle visuals
        views.setBoolean(R.id.toggleLight0, "setChecked", stateLight0 == 1)
        views.setBoolean(R.id.toggleFan1, "setChecked", stateFan1 == 1)
        views.setBoolean(R.id.toggleLight4, "setChecked", stateLight4 == 1)
        views.setBoolean(R.id.toggleFan5, "setChecked", stateFan5 == 1)

        // Set pending intents
        views.setOnClickPendingIntent(R.id.toggleLight0, getPendingIntent(context, ACTION_TOGGLE_LIGHT0))
        views.setOnClickPendingIntent(R.id.toggleFan1, getPendingIntent(context, ACTION_TOGGLE_FAN1))
        views.setOnClickPendingIntent(R.id.toggleLight4, getPendingIntent(context, ACTION_TOGGLE_LIGHT4))
        views.setOnClickPendingIntent(R.id.toggleFan5, getPendingIntent(context, ACTION_TOGGLE_FAN5))

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun getPendingIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, SmartHomeWidget::class.java).apply { this.action = action }
        return PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
        )
    }

    private fun updateAllWidgets(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, SmartHomeWidget::class.java))
        for (id in ids) {
            updateWidget(context, manager, id)
        }
    }

    // Call this externally from Service when MQTT/BLE updates arrive
    fun updateRelayState(relayId: Int, state: Int) {
        when (relayId) {
            0 -> stateLight0 = state
            1 -> stateFan1 = state
            4 -> stateLight4 = state
            5 -> stateFan5 = state
        }
    }
}
