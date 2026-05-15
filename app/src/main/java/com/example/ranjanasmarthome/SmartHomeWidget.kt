package com.example.ranjanasmarthome

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import android.view.animation.AnimationUtils
import android.util.Log

private const val TAG = "SmartHomeWidget"

class SmartHomeWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE_FAN1 = "ACTION_TOGGLE_FAN1"
        const val ACTION_TOGGLE_FAN2 = "ACTION_TOGGLE_FAN2"
        const val ACTION_TOGGLE_LIGHT1 = "ACTION_TOGGLE_LIGHT1"
        const val ACTION_TOGGLE_LIGHT2 = "ACTION_TOGGLE_LIGHT2"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_TOGGLE_FAN1 -> toggleFan1(context)
            ACTION_TOGGLE_FAN2 -> toggleFan2(context)
            ACTION_TOGGLE_LIGHT1 -> toggleLight1(context)
            ACTION_TOGGLE_LIGHT2 -> toggleLight2(context)
        }
    }

    private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        // Set up button click actions
        val pendingIntentFan1 = getPendingIntent(context, ACTION_TOGGLE_FAN1)
        val pendingIntentFan2 = getPendingIntent(context, ACTION_TOGGLE_FAN2)
        val pendingIntentLight1 = getPendingIntent(context, ACTION_TOGGLE_LIGHT1)
        val pendingIntentLight2 = getPendingIntent(context, ACTION_TOGGLE_LIGHT2)

        views.setOnClickPendingIntent(R.id.button_fan1, pendingIntentFan1)
        views.setOnClickPendingIntent(R.id.button_fan2, pendingIntentFan2)
        views.setOnClickPendingIntent(R.id.button_light1, pendingIntentLight1)
        views.setOnClickPendingIntent(R.id.button_light2, pendingIntentLight2)

        // Set images based on current state
        views.setImageViewResource(
            R.id.button_fan1,
            if (WidgetState.fan1 == 1) R.drawable.button_round_orange else R.drawable.button_round_green
        )
        views.setImageViewResource(
            R.id.button_fan2,
            if (WidgetState.fan2 == 1) R.drawable.button_round_orange else R.drawable.button_round_green
        )
        views.setImageViewResource(
            R.id.button_light1,
            if (WidgetState.light1) R.drawable.button_round_orange else R.drawable.button_round_green
        )
        views.setImageViewResource(
            R.id.button_light2,
            if (WidgetState.light2) R.drawable.button_round_orange else R.drawable.button_round_green
        )

        manager.updateAppWidget(widgetId, views)
    }

    private fun getPendingIntent(context: Context, action: String) =
        android.app.PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            Intent(context, SmartHomeWidget::class.java).setAction(action),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_MUTABLE
            else
                android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

    // ---------------- TOGGLE METHODS ----------------
    private fun toggleFan1(context: Context) {
        WidgetState.fan1 = if (WidgetState.fan1 == 1) 0 else 1
        sendFanCommand(WidgetState.fan1, 10, 11)
        WidgetState.notifyUpdate()
        animateWidgetButton(context, R.id.button_fan1)
    }

    private fun toggleFan2(context: Context) {
        WidgetState.fan2 = if (WidgetState.fan2 == 1) 0 else 1
        sendFanCommand(WidgetState.fan2, 50, 51)
        WidgetState.notifyUpdate()
        animateWidgetButton(context, R.id.button_fan2)
    }

    private fun toggleLight1(context: Context) {
        WidgetState.light1 = !WidgetState.light1
        sendLightCommand(WidgetState.light1, 0)
        WidgetState.notifyUpdate()
        animateWidgetButton(context, R.id.button_light1)
    }

    private fun toggleLight2(context: Context) {
        WidgetState.light2 = !WidgetState.light2
        sendLightCommand(WidgetState.light2, 4)
        WidgetState.notifyUpdate()
        animateWidgetButton(context, R.id.button_light2)
    }

    // ---------------- SEND COMMANDS ----------------
    private fun sendFanCommand(state: Int, onRelay: Int, offRelay: Int) {
        val cmd = if (state == 1) "$onRelay" else "$offRelay"
        BLEController.sendCommand(cmd)
        MQTTController.sendCommand(cmd)
        Log.d(TAG, "Fan command sent: $cmd")
    }

    private fun sendLightCommand(state: Boolean, relay: Int) {
        val cmd = if (state) "${relay}1" else "${relay}0"
        BLEController.sendCommand(cmd)
        MQTTController.sendCommand(cmd)
        Log.d(TAG, "Light command sent: $cmd")
    }

    // ---------------- ANIMATION ----------------
    private fun animateWidgetButton(context: Context, buttonId: Int) {
        val manager = AppWidgetManager.getInstance(context)
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        views.setInt(buttonId, "setBackgroundResource", R.drawable.toggle_light)
        manager.updateAppWidget(android.appwidget.AppWidgetManager.getInstance(context).getAppWidgetIds(
            android.content.ComponentName(context, SmartHomeWidget::class.java)
        ), views)
    }
}
