package com.example.ranjanasmarthome

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.JobIntentService
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.eclipse.paho.client.mqttv3.*
import no.nordicsemi.android.ble.*
import android.os.Handler
import android.os.Looper

class SmartHomeService : Service() {

    companion object {
        private const val TAG = "SmartHomeService"

        // Example: send command to ESP32
        fun sendCommand(cmd: String) {
            // TODO: publish via MQTT or BLE write
            Log.d(TAG, "Sending command: $cmd")
            instance?.sendCmdInternal(cmd)
        }

        private var instance: SmartHomeService? = null
    }

    private lateinit var mqttClient: MqttClient
    private val widgetProvider = SmartHomeWidget()

    override fun onCreate() {
        super.onCreate()
        instance = this
        startForegroundServiceNotification()

        // Connect MQTT
        setupMQTT()
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        mqttClient.disconnect()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /** --------------------- MQTT SETUP --------------------- */
    private fun setupMQTT() {
        try {
            val serverURI = "ssl://5dba91287f8248c1a30195053d3862ed.s1.eu.hivemq.cloud:8883"
            mqttClient = MqttClient(serverURI, "AndroidClient_${System.currentTimeMillis()}", null)
            val options = MqttConnectOptions().apply {
                userName = "Debarghya_Sannigrahi"
                password = "Dsann#5956".toCharArray()
                isCleanSession = true
            }

            mqttClient.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    Log.e(TAG, "MQTT connection lost: ${cause?.message}")
                    reconnectMQTT()
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    message?.let {
                        val payload = it.toString()
                        Log.d(TAG, "MQTT message received: $payload")
                        handleESPUpdate(payload)
                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {}
            })

            mqttClient.connect(options)
            mqttClient.subscribe("home/esp32/update")
        } catch (e: Exception) {
            Log.e(TAG, "MQTT setup failed: ${e.message}")
        }
    }

    private fun reconnectMQTT() {
        Handler(Looper.getMainLooper()).postDelayed({
            if (!mqttClient.isConnected) {
                setupMQTT()
            }
        }, 5000)
    }

    /** --------------------- Handle ESP Messages --------------------- */
    private fun handleESPUpdate(msg: String) {
        // ESP32 sends: a:R1100,T0,0,0,0,D0,0,0,0
        // or b:R0011,T0,0,0,0,D0,0,0,0
        // We'll only handle relay/fan state: Rxxxx

        val relayIndexStart = msg.indexOf("R")
        if (relayIndexStart != -1 && msg.length >= relayIndexStart + 5) {
            val states = msg.substring(relayIndexStart + 1, relayIndexStart + 5)
            // Relay/fan mapping for our widget
            // light0 = relay 0, fan1 = relay 1, light4 = relay 4, fan5 = relay 5
            try {
                val light0 = states[0].digitToInt()
                val fan1 = states[1].digitToInt()
                val light4 = states[4 - 0].digitToInt() // For 'b' message, adjust indexing
                val fan5 = states[4 - 1].digitToInt()   // For 'b' message, adjust indexing

                updateWidget(light0, fan1, light4, fan5)
            } catch (e: Exception) {
                Log.e(TAG, "Failed parsing ESP32 message: ${e.message}")
            }
        }
    }

    /** --------------------- Update Widget --------------------- */
    private fun updateWidget(light0: Int, fan1: Int, light4: Int, fan5: Int) {
        val context: Context = applicationContext
        widgetProvider.updateRelayState(0, light0)
        widgetProvider.updateRelayState(1, fan1)
        widgetProvider.updateRelayState(4, light4)
        widgetProvider.updateRelayState(5, fan5)

        // Refresh all widget instances
        val manager = android.appwidget.AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(android.content.ComponentName(context, SmartHomeWidget::class.java))
        for (id in ids) {
            widgetProvider.onUpdate(context, manager, intArrayOf(id))
        }
    }

    /** --------------------- Send Commands --------------------- */
    private fun sendCmdInternal(cmd: String) {
        // Publish via MQTT
        if (mqttClient.isConnected) {
            try {
                mqttClient.publish("home/esp32/commands", MqttMessage(cmd.toByteArray()))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send MQTT command: ${e.message}")
            }
        }

        // TODO: Add BLE write if connected
    }

    /** --------------------- Foreground Notification --------------------- */
    private fun startForegroundServiceNotification() {
        val notification = NotificationCompat.Builder(this, "SmartHomeChannel")
            .setContentTitle("Ranjana Smart Home")
            .setContentText("Service running...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
        startForeground(1, notification)
    }
}
