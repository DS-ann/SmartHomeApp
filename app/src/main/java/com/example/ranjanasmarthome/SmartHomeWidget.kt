package com.example.ranjanasmarthome

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.Toast

class SmartHomeWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE = "com.example.ranjanasmarthome.ACTION_TOGGLE"
        const val EXTRA_BUTTON_ID = "extra_button_id"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)

            // Set click listeners
            setClick(context, views, R.id.button_fan1)
            setClick(context, views, R.id.button_fan2)
            setClick(context, views, R.id.button_light1)
            setClick(context, views, R.id.button_light2)

            // Apply current state
            applyStateToViews(context, views)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun setClick(context: Context, views: RemoteViews, buttonId: Int) {
        val intent = Intent(context, SmartHomeWidget::class.java).apply {
            action = ACTION_TOGGLE
            putExtra(EXTRA_BUTTON_ID, buttonId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            buttonId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(buttonId, pendingIntent)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE) {
            val buttonId = intent.getIntExtra(EXTRA_BUTTON_ID, -1)
            if (buttonId != -1) {
                handleToggle(context, buttonId)
            }
        }
    }

    private fun handleToggle(context: Context, buttonId: Int) {
        when (buttonId) {
            R.id.button_fan1 -> {
                WidgetState.fan1 = if (WidgetState.fan1 == 0) 1 else 0
                sendCommand(if (WidgetState.fan1 == 1) "11" else "10") // Fan 1 ON/OFF
            }
            R.id.button_fan2 -> {
                WidgetState.fan2 = if (WidgetState.fan2 == 0) 1 else 0
                sendCommand(if (WidgetState.fan2 == 1) "51" else "50") // Fan 2 ON/OFF
            }
            R.id.button_light1 -> {
                WidgetState.light1 = !WidgetState.light1
                sendCommand(if (WidgetState.light1) "01" else "00") // Light 1 relay 0
            }
            R.id.button_light2 -> {
                WidgetState.light2 = !WidgetState.light2
                sendCommand(if (WidgetState.light2) "41" else "40") // Light 2 relay 4
            }
        }

        // Animate and update widget
        val views = RemoteViews(context.packageName, R.layout.widget_layout)
        applyStateToViews(context, views)
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val ids = appWidgetManager.getAppWidgetIds(
            android.content.ComponentName(context, SmartHomeWidget::class.java)
        )
        for (id in ids) appWidgetManager.updateAppWidget(id, views)
    }

    private fun sendCommand(cmd: String) {
        BLEController.sendCommand(cmd)
        MQTTController.sendCommand(cmd)
    }

    private fun applyStateToViews(context: Context, views: RemoteViews) {
        // Set animation drawable
        views.setImageViewResource(
            R.id.button_fan1,
            if (WidgetState.fan1 == 1) R.drawable.light_on_anim else R.drawable.light_off_anim
        )
        views.setImageViewResource(
            R.id.button_fan2,
            if (WidgetState.fan2 == 1) R.drawable.light_on_anim else R.drawable.light_off_anim
        )
        views.setImageViewResource(
            R.id.button_light1,
            if (WidgetState.light1) R.drawable.light_on_anim else R.drawable.light_off_anim
        )
        views.setImageViewResource(
            R.id.button_light2,
            if (WidgetState.light2) R.drawable.light_on_anim else R.drawable.light_off_anim
        )
    }
}
