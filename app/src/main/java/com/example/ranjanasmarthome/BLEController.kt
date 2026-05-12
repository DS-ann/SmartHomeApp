package com.example.ranjanasmarthome

import android.util.Log
import java.util.*

object BLEController {

    private const val TAG = "BLEController"

    var onStateUpdate: ((light1: Boolean, fan1: Boolean, light2: Boolean, fan2: Boolean) -> Unit)? = null

    // Send command to ESP32 over BLE
    fun sendCommand(cmd: String) {
        Log.d(TAG, "Sending BLE command: $cmd")
        // TODO: implement actual BLE write using pRxCharacteristic.setValue(cmd.toByteArray()) + notify
    }

    // Call this from BLE callback when ESP32 sends update
    fun receiveESPMessage(msg: String) {
        // Parse messages like a:R1010,... b:R0101,...
        val light1 = msg.getOrNull(msg.indexOf("R") + 0 + 0) == '1' // relay0
        val fan1   = msg.getOrNull(msg.indexOf("R") + 0 + 1) == '1' // relay1
        val light2 = msg.getOrNull(msg.indexOf("R", 1) + 0 + 0) == '1' // relay4
        val fan2   = msg.getOrNull(msg.indexOf("R", 1) + 0 + 1) == '1' // relay5

        onStateUpdate?.invoke(light1, fan1, light2, fan2)
    }
}
