package com.example.ranjanasmarthome

import android.util.Log
import org.eclipse.paho.client.mqttv3.*

object MQTTController {

    private const val TAG = "MQTTController"
    private const val BROKER = "tcp://5dba91287f8248c1a30195053d3862ed.s1.eu.hivemq.cloud:8883"
    private const val CLIENT_ID = "AndroidWidgetClient"
    private const val TOPIC_UPDATE = "home/esp32/update"
    private const val TOPIC_FAN = "home/esp32/fan_status"

    var onStateUpdate: ((light1: Boolean, fan1: Boolean, light2: Boolean, fan2: Boolean) -> Unit)? = null

    private var client: MqttClient? = null

    fun connect() {
        try {
            client = MqttClient(BROKER, CLIENT_ID, null)
            val options = MqttConnectOptions()
            options.isCleanSession = true
            options.userName = "Debarghya_Sannigrahi"
            options.password = "Dsann#5956".toCharArray()
            client?.setCallback(object: MqttCallback {
                override fun connectionLost(cause: Throwable?) {}
                override fun deliveryComplete(token: IMqttDeliveryToken?) {}
                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    message?.let {
                        parseESPMessage(String(it.payload))
                    }
                }
            })
            client?.connect(options)
            client?.subscribe(TOPIC_UPDATE)
            client?.subscribe(TOPIC_FAN)
        } catch(e: Exception) {
            Log.e(TAG, "MQTT connect failed: ${e.message}")
        }
    }

    fun sendCommand(cmd: String) {
        try { client?.publish("home/esp32/commands", MqttMessage(cmd.toByteArray())) } catch(_: Exception) {}
    }

    private fun parseESPMessage(msg: String) {
        val light1 = msg.getOrNull(msg.indexOf("R") + 0 + 0) == '1' // relay0
        val fan1   = msg.getOrNull(msg.indexOf("R") + 0 + 1) == '1' // relay1
        val light2 = msg.getOrNull(msg.indexOf("R", 1) + 0 + 0) == '1' // relay4
        val fan2   = msg.getOrNull(msg.indexOf("R", 1) + 0 + 1) == '1' // relay5

        onStateUpdate?.invoke(light1, fan1, light2, fan2)
    }
}
