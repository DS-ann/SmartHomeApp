
package com.example.ranjanasmarthome

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

object WidgetState {
    var onStateUpdate: ((Boolean, Int, Boolean, Int) -> Unit)? = null
    private var l1 = false
    private var f1 = 0
    private var l2 = false
    private var f2 = 0

    fun update(light1: Boolean, fan1: Int, light2: Boolean, fan2: Int) {
        l1 = light1; f1 = fan1; l2 = light2; f2 = fan2
        onStateUpdate?.invoke(l1, f1, l2, f2)
    }
}

class SmartHomeService : Service() {

    private val TAG = "SmartHomeService"

    // Total 8 relays
    private val relayState = BooleanArray(8)
    private val fanSpeed = IntArray(2)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        // Start BLE
        BLEController.startBLE { msg -> handleIncoming(msg) }

        // Connect MQTT
        MQTTController.connect { topic, msg -> handleIncoming(msg) }
    }

    /** Handle incoming BLE or MQTT messages */
    private fun handleIncoming(msg: String) {
        Log.d(TAG, "Message received: $msg")

        when {
            msg.startsWith("a:") || msg.startsWith("b:") -> decodeRelayMessage(msg)
            msg.startsWith("F") -> decodeFanMessage(msg)
        }

        // Update WidgetState: only first 4 devices (relay/fan ignored)
        WidgetState.update(
            relayState[0], 0,
            relayState[1], 0
        )

        // Update WebView: all relays + fan speed
        MainActivity.updateWebViewFull(relayState, fanSpeed)
    }

    /** Decode relay message a/b:R1010,T0,0,0,0,D0,0,0,0 */
    private fun decodeRelayMessage(msg: String) {
        val base = if (msg.startsWith("a")) 0 else 4
        val rIndex = msg.indexOf('R')
        if (rIndex < 0) return
        for (i in 0..3) {
            val c = msg.getOrNull(rIndex + 1 + i) ?: '0'
            relayState[base + i] = c == '1'
        }
    }

    /** Decode fan message F0,2 → fan0 speed2 */
    private fun decodeFanMessage(msg: String) {
        val parts = msg.substring(1).split(",")
        if (parts.size != 2) return
        val fan = parts[0].toIntOrNull() ?: return
        val speed = parts[1].toIntOrNull() ?: return
        if (fan in 0..1 && speed in 0..4) fanSpeed[fan] = speed
    }

    /** Send command to both BLE and MQTT */
    fun sendCommand(cmd: String) {
        Log.d(TAG, "Sending command: $cmd")
        BLEController.sendCommand(cmd)
        MQTTController.sendCommand(cmd)
    }

    override fun onDestroy() {
        super.onDestroy()
        BLEController.disconnect()
        MQTTController.disconnect()
    }
}
