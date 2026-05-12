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

    private fun connect() {
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
                    client.subscribe(TOPIC_UPDATE, 0)
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

    fun publishCommand(cmd: String) {
        if (isConnected) {
            try {
                client.publish(TOPIC_CMD, cmd.toByteArray(), 0, false)
            } catch (e: MqttException) {
                Log.e(TAG, "Failed to publish $cmd", e)
            }
        }
    }
private fun decodeMessage(msg: String) {
    try {
        when {
            msg.startsWith("a:") -> {
                // First 4 relays → Light1 (0), Fan1 (1)
                val light1 = msg.getOrNull(3) == '1' // relay 0
                val fan1 = msg.getOrNull(4) == '1'   // relay 1

                WidgetState.onPartialUpdate(
                    light1On = light1,
                    fan1Speed = if (fan1) 1 else 0
                )
            }
            msg.startsWith("b:") -> {
                // Next 4 relays → Light2 (4), Fan2 (5)
                val light2 = msg.getOrNull(3) == '1' // relay 4
                val fan2 = msg.getOrNull(4) == '1'   // relay 5

                WidgetState.onPartialUpdate(
                    light2On = light2,
                    fan2Speed = if (fan2) 1 else 0
                )
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to decode message: $msg", e)
    }
}
