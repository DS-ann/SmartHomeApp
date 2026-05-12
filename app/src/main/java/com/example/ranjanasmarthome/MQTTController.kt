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
    var isConnected = false
        private set

    // Callback for widget/other components
    var onStateUpdate: ((light1: Boolean, fan1: Boolean, light2: Boolean, fan2: Boolean) -> Unit)? = null

    // Local state of relays
    private var light1State = false
    private var fan1State = false
    private var light2State = false
    private var fan2State = false

    fun init(context: Context) {
        client = MqttAndroidClient(context, SERVER_URI, CLIENT_ID)
        client.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable?) {
                Log.e(TAG, "MQTT lost connection: $cause")
                isConnected = false
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                message?.let { decodeMessage(it.toString()) }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {}
        })
        connect()
    }

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
                    try {
                        client.subscribe(TOPIC_UPDATE, 0)
                    } catch (e: MqttException) {
                        Log.e(TAG, "Failed to subscribe", e)
                    }
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

    fun sendCommand(cmd: String) {
        if (!isConnected) return
        try {
            client.publish(TOPIC_CMD, cmd.toByteArray(), 0, false)
        } catch (e: MqttException) {
            Log.e(TAG, "Failed to send command: $cmd", e)
        }
    }

    private fun decodeMessage(msg: String) {
        try {
            when {
                msg.startsWith("a:") -> {
                    // First 4 relays: Light1 (0), Fan1 (1)
                    light1State = msg.getOrNull(3) == '1'
                    fan1State = msg.getOrNull(4) == '1'
                }
                msg.startsWith("b:") -> {
                    // Next 4 relays: Light2 (4), Fan2 (5)
                    light2State = msg.getOrNull(3) == '1'
                    fan2State = msg.getOrNull(4) == '1'
                }
            }

            // Send combined state to widget/other listeners
            onStateUpdate?.invoke(light1State, fan1State, light2State, fan2State)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode message: $msg", e)
        }
    }
}
