package com.example.ranjanasmarthome

import android.content.Context
import android.util.Log
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*

private const val TAG = "MQTTController"

/**
 * Central MQTT controller for Smart Home device.
 * Handles connection, publishing commands, and receiving updates.
 */
object MQTTController {

    private const val SERVER_URI = "ssl://5dba91287f8248c1a30195053d3862ed.s1.eu.hivemq.cloud:8883"
    private const val USER = "Debarghya_Sannigrahi"
    private const val PASSWORD = "Dsann#5956"
    private const val CLIENT_ID = "AndroidSmartHome"
    private const val TOPIC_CMD = "home/esp32/commands"
    private const val TOPIC_UPDATE = "home/esp32/update"

    private lateinit var client: MqttAndroidClient

    var isConnected: Boolean = false
        private set

    // Callback for UI updates: light1, fan1 (Int), light2, fan2 (Int)
    var onStateUpdate: ((light1: Boolean, fan1: Int, light2: Boolean, fan2: Int) -> Unit)? = null

    // Local device state
    private var light1State: Boolean = false
    private var fan1State: Int = 0
    private var light2State: Boolean = false
    private var fan2State: Int = 0

    /** Initialize MQTT client */
    fun init(context: Context) {
        client = MqttAndroidClient(context.applicationContext, SERVER_URI, CLIENT_ID)
        client.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable?) {
                Log.e(TAG, "MQTT connection lost: $cause")
                isConnected = false
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                message?.toString()?.let { decodeMessage(it) }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {}
        })

        connect()
    }

    /** Connect to MQTT broker */
    fun connect() {
        if (!::client.isInitialized) return

        val options = MqttConnectOptions().apply {
            userName = USER
            password = PASSWORD.toCharArray()
            isAutomaticReconnect = true
            isCleanSession = true
        }

        try {
            client.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "MQTT connected")
                    isConnected = true
                    subscribeToUpdates()
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(TAG, "MQTT connect failed: $exception")
                    isConnected = false
                }
            })
        } catch (e: MqttException) {
            Log.e(TAG, "MQTT connection error", e)
        }
    }

    /** Subscribe to device state updates */
    private fun subscribeToUpdates() {
        try {
            client.subscribe(TOPIC_UPDATE, 0) { _, message ->
                decodeMessage(message.toString())
            }
        } catch (e: MqttException) {
            Log.e(TAG, "Failed to subscribe to $TOPIC_UPDATE", e)
        }
    }

    /** Send a command to ESP32 */
    fun sendCommand(cmd: String) {
        if (!isConnected) {
            Log.w(TAG, "Cannot send command, MQTT not connected")
            return
        }
        try {
            client.publish(TOPIC_CMD, cmd.toByteArray(), 0, false)
        } catch (e: MqttException) {
            Log.e(TAG, "Failed to send command: $cmd", e)
        }
    }

    /** Decode incoming MQTT messages and update WidgetState */
    private fun decodeMessage(msg: String) {
        try {
            when {
                msg.startsWith("a:") && msg.length >= 5 -> {
                    light1State = msg[2] == '1'
                    fan1State = msg[3].digitToIntOrNull() ?: 0
                }
                msg.startsWith("b:") && msg.length >= 5 -> {
                    light2State = msg[2] == '1'
                    fan2State = msg[3].digitToIntOrNull() ?: 0
                }
            }

            // Update WidgetState
            WidgetState.onPartialUpdate(
                light1 = light1State,
                fan1 = fan1State,
                light2 = light2State,
                fan2 = fan2State
            )

            // Notify UI callbacks
            onStateUpdate?.invoke(light1State, fan1State, light2State, fan2State)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode MQTT message: $msg", e)
        }
    }

    /** Disconnect MQTT client */
    fun disconnect() {
        if (!::client.isInitialized) return
        try {
            if (isConnected) {
                client.disconnect()
                isConnected = false
                Log.d(TAG, "MQTT disconnected")
            }
        } catch (e: MqttException) {
            Log.e(TAG, "Failed to disconnect MQTT", e)
        }
    }
}
