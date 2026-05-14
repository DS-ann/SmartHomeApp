package com.example.ranjanasmarthome

import android.content.Context
import android.util.Log
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*

private const val TAG = "MQTTController"

object MQTTController {

    private const val SERVER_URI = "ssl://5dba91287f8248c1a30195053d3862ed.s1.eu.hivemq.cloud:8883"
    private const val USER = "Debarghya_Sannigrahi"
    private const val PASSWORD = "Dsann#5956"
    private const val CLIENT_ID = "AndroidSmartHome"
    private const val TOPIC_CMD = "home/esp32/commands"
    private const val TOPIC_UPDATE = "home/esp32/update"

    private lateinit var client: MqttAndroidClient

    // Mutable connection state
    var isConnected: Boolean = false
        private set

    // Callback for UI updates
    var onStateUpdate: ((light1: Boolean, fan1: Int, light2: Boolean, fan2: Int) -> Unit)? = null

    // Local device state (mutable)
    private var light1State: Boolean = false
    private var fan1State: Int = 0
    private var light2State: Boolean = false
    private var fan2State: Int = 0

    /** Initialize MQTT client */
    fun init(context: Context) {
        client = MqttAndroidClient(context, SERVER_URI, CLIENT_ID).apply {
            setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    Log.e(TAG, "MQTT lost connection: $cause")
                    isConnected = false
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    message?.toString()?.let { decodeMessage(it) }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {}
            })
        }
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
            Log.e(TAG, "MQTT connect error", e)
        }
    }

    /** Subscribe to the ESP32 update topic */
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

    /** Decode incoming MQTT messages and update state */
    private fun decodeMessage(msg: String) {
        try {
            when {
                msg.startsWith("a:") -> {
                    light1State = msg.getOrNull(3) == '1'
                    fan1State = if (msg.getOrNull(4) == '1') 1 else 0
                }
                msg.startsWith("b:") -> {
                    light2State = msg.getOrNull(3) == '1'
                    fan2State = if (msg.getOrNull(4) == '1') 1 else 0
                }
            }
            // Notify listeners
            onStateUpdate?.invoke(light1State, fan1State, light2State, fan2State)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode MQTT message: $msg", e)
        }
    }

    /** Disconnect cleanly */
    fun disconnect() {
        try {
            if (::client.isInitialized && isConnected) {
                client.disconnect()
                isConnected = false
                Log.d(TAG, "MQTT disconnected")
            }
        } catch (e: MqttException) {
            Log.e(TAG, "Failed to disconnect MQTT", e)
        }
    }
}
