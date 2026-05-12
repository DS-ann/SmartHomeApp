package com.example.ranjanasmarthome

import android.content.Context
import android.util.Log
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*

object MQTTController {

    private const val TAG = "MQTTController"

    // MQTT server settings (update with your credentials)
    private const val MQTT_SERVER = "ssl://5dba91287f8248c1a30195053d3862ed.s1.eu.hivemq.cloud:8883"
    private const val MQTT_USER = "Debarghya_Sannigrahi"
    private const val MQTT_PASSWORD = "Dsann#5956"

    private const val CLIENT_ID = "AndroidSmartHomeApp"

    // Topics
    const val TOPIC_CMD = "home/esp32/commands"
    const val TOPIC_UPDATE = "home/esp32/update"

    private var mqttClient: MqttAndroidClient? = null
    private var isConnected = false

    /**
     * Initialize MQTT client and connect
     */
    fun init(context: Context) {
        if (mqttClient != null) return // Already initialized

        mqttClient = MqttAndroidClient(context.applicationContext, MQTT_SERVER, CLIENT_ID)
        mqttClient?.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable?) {
                Log.w(TAG, "MQTT connection lost: ${cause?.message}")
                isConnected = false
                reconnect()
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                if (topic == null || message == null) return
                val payload = message.toString()
                Log.d(TAG, "MQTT message arrived: $topic -> $payload")

                // Forward to BLEController to update widget state
                BLEController.updateStateFromESP(payload)
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                // optional: log delivery success
            }
        })

        connect()
    }

    private fun connect() {
        val options = MqttConnectOptions().apply {
            userName = MQTT_USER
            password = MQTT_PASSWORD.toCharArray()
            isAutomaticReconnect = true
            isCleanSession = true
        }

        mqttClient?.connect(options, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d(TAG, "MQTT connected")
                isConnected = true
                subscribeToTopics()
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.e(TAG, "MQTT connection failed: ${exception?.message}")
                isConnected = false
            }
        })
    }

    private fun reconnect() {
        mqttClient?.let {
            if (!it.isConnected) {
                Log.d(TAG, "Reconnecting MQTT...")
                connect()
            }
        }
    }

    private fun subscribeToTopics() {
        try {
            mqttClient?.subscribe(TOPIC_UPDATE, 0)
            mqttClient?.subscribe(TOPIC_CMD, 0)
            Log.d(TAG, "Subscribed to topics")
        } catch (e: MqttException) {
            Log.e(TAG, "Failed to subscribe: ${e.message}")
        }
    }

    /**
     * Publish command to ESP32
     */
    fun sendCommand(cmd: String) {
        if (!isConnected) {
            Log.w(TAG, "MQTT not connected, cannot send command")
            return
        }
        try {
            mqttClient?.publish(TOPIC_CMD, cmd.toByteArray(), 0, true)
            Log.d(TAG, "MQTT command sent: $cmd")
        } catch (e: MqttException) {
            Log.e(TAG, "Failed to send MQTT command: ${e.message}")
        }
    }
}
