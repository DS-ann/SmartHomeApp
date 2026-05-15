package com.example.ranjanasmarthome

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class SmartHomeWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        // Set current state
        views.setImageViewResource(R.id.button_fan1, if (WidgetState.fan1) R.drawable.button_round_green else R.drawable.button_round_orange)
        views.setImageViewResource(R.id.button_fan2, if (WidgetState.fan2) R.drawable.button_round_green else R.drawable.button_round_orange)
        views.setImageViewResource(R.id.button_light1, if (WidgetState.light1) R.drawable.button_round_green else R.drawable.button_round_orange)
        views.setImageViewResource(R.id.button_light2, if (WidgetState.light2) R.drawable.button_round_green else R.drawable.button_round_orange)

        // Button click intents
        val ids = listOf(R.id.button_fan1, R.id.button_fan2, R.id.button_light1, R.id.button_light2)
        val relays = listOf("F1", "F2", "L1", "L2") // Command identifiers

        ids.forEachIndexed { index, buttonId ->
            val intent = Intent(context, SmartHomeWidget::class.java).apply {
                action = "TOGGLE_BUTTON"
                putExtra("relay", relays[index])
            }
            val pendingIntent = PendingIntent.getBroadcast(context, index, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
            views.setOnClickPendingIntent(buttonId, pendingIntent)
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "TOGGLE_BUTTON") {
            val relay = intent.getStringExtra("relay") ?: return

            when (relay) {
                "F1" -> {
                    WidgetState.fan1 = !WidgetState.fan1
                    SmartHomeService.sendRelayCommand("1", WidgetState.fan1)
                }
                "F2" -> {
                    WidgetState.fan2 = !WidgetState.fan2
                    SmartHomeService.sendRelayCommand("5", WidgetState.fan2)
                }
                "L1" -> {
                    WidgetState.light1 = !WidgetState.light1
                    SmartHomeService.sendRelayCommand("0", WidgetState.light1)
                }
                "L2" -> {
                    WidgetState.light2 = !WidgetState.light2
                    SmartHomeService.sendRelayCommand("4", WidgetState.light2)
                }
            }

            // Refresh widget
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(context.componentName)
            ids.forEach { id ->
                updateWidget(context, manager, id)
            }
        }
    }
}
