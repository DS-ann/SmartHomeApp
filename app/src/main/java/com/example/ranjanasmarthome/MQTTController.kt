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

    /** Callback for UI updates (widget) */
    var onStateUpdate: ((light1: Boolean, fan1: Int, light2: Boolean, fan2: Int) -> Unit)? = null

    // Local state caches
    private val relayState = BooleanArray(8) { false }
    private val fanSpeed = IntArray(2) { 0 }

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

    /** Decode incoming MQTT messages and update states */
    private fun decodeMessage(msg: String) {
        try {
            when {
                msg.startsWith("a:") && msg.contains("R") -> {
                    val rIndex = msg.indexOf('R')
                    for (i in 0..3) {
                        val c = msg.getOrNull(rIndex + 1 + i) ?: '0'
                        relayState[i] = c == '1'
                    }
                }
                msg.startsWith("b:") && msg.contains("R") -> {
                    val rIndex = msg.indexOf('R')
                    for (i in 0..3) {
                        val c = msg.getOrNull(rIndex + 1 + i) ?: '0'
                        relayState[4 + i] = c == '1'
                    }
                }
                msg.startsWith("F") -> {
                    val parts = msg.substring(1).split(",")
                    if (parts.size == 2) {
                        val fan = parts[0].toIntOrNull()
                        val speed = parts[1].toIntOrNull()
                        if (fan in 0..1 && speed in 0..4) fanSpeed[fan] = speed
                    }
                }
            }

            // Update WidgetState (only first 2 relays, fan ignored)
            WidgetState.update(
                light1 = relayState[0],
                fan1 = 0,
                light2 = relayState[1],
                fan2 = 0
            )

            // Update WebView (all relays + fans)
            MainActivity.updateWebViewFull(relayState, fanSpeed)

            // Optional UI callback
            onStateUpdate?.invoke(
                relayState[0], 0,
                relayState[1], 0
            )

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
