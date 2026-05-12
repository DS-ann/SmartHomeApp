package com.example.ranjanasmarthome

import android.util.Log

object BLEController {

    private val listeners = mutableListOf<(DeviceState) -> Unit>()

    fun registerListener(listener: (DeviceState) -> Unit) {
        if (!listeners.contains(listener)) listeners.add(listener)
    }

    fun unregisterListener(listener: (DeviceState) -> Unit) {
        listeners.remove(listener)
    }

    // Call this when BLE message arrives
    fun onMessageReceived(msg: String) {
        try {
            val state = decodeMessage(msg)
            listeners.forEach { it(state) }
        } catch (e: Exception) {
            Log.e("BLEController", "Failed to decode BLE message: $msg", e)
        }
    }

    private fun decodeMessage(msg: String): DeviceState {
        // Format: "a:R0101,T...,D..." or "b:R0101..."
        val rIndex = msg.indexOf('R')
        if (rIndex < 0) return DeviceState()

        val rStr = msg.substring(rIndex + 1, rIndex + 5) // first 4 relays
        // Mapping: Light1=0, Fan1=1, Light2=4, Fan2=5
        val light1 = rStr.getOrNull(0)?.toString() == "1"
        val fan1 = rStr.getOrNull(1)?.toString() == "1"
        val light2 = rStr.getOrNull(2)?.toString() == "1"
        val fan2 = rStr.getOrNull(3)?.toString() == "1"

        return DeviceState(
            light1 = light1,
            light2 = light2,
            fan1 = fan1,
            fan2 = fan2
        )
    }
}

// Data class to hold only the 4 relevant devices
data class DeviceState(
    val light1: Boolean = false,
    val light2: Boolean = false,
    val fan1: Boolean = false,
    val fan2: Boolean = false
)
