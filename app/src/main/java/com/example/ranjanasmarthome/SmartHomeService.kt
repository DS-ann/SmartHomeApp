package com.example.ranjanasmarthome

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

private const val TAG = "SmartHomeService"
private const val CHANNEL_ID = "smart_home_service_channel"
private const val NOTIF_ID = 101

class SmartHomeService : Service() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification("Smart Home Service running"))

        // Initialize BLE
        BLEController.initialize(this)

        // Initialize MQTT
        MQTTController.initialize(this)

        Log.d(TAG, "SmartHomeService created and initialized")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "SmartHomeService started")
        // You can handle intent actions here if needed
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        BLEController.disconnect()
        MQTTController.disconnect()
        Log.d(TAG, "SmartHomeService destroyed, disconnected BLE and MQTT")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ------------------- NOTIFICATION -------------------
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Smart Home Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Ranjana Smart Home")
            .setContentText(content)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
    }

    // ------------------- CALLBACKS FROM BLE -------------------
    // Called by BLEController when a message arrives
    fun onBLEMessageReceived(msg: String) {
        Log.d(TAG, "BLE message: $msg")
        decodeRelayMessage(msg)
    }

    // ------------------- CALLBACKS FROM MQTT -------------------
    // Called by MQTTController when a message arrives
    fun onMQTTMessageReceived(msg: String) {
        Log.d(TAG, "MQTT message: $msg")
        decodeRelayMessage(msg)
    }

    // ------------------- DECODE RELAY STATE -------------------
    private fun decodeRelayMessage(msg: String) {
        // Fan 1 → 10 / 11
        when (msg.trim()) {
            "10" -> WidgetState.fan1 = 1
            "11" -> WidgetState.fan1 = 0
            "50" -> WidgetState.fan2 = 1
            "51" -> WidgetState.fan2 = 0
            "01" -> WidgetState.light1 = true
            "00" -> WidgetState.light1 = false
            "41" -> WidgetState.light2 = true
            "40" -> WidgetState.light2 = false
        }

        // Notify Widget + WebView
        WidgetState.notifyUpdate()
    }
}
