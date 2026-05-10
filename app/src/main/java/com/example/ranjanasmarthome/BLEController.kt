package com.example.ranjanasmarthome

import android.util.Log

object BLEController {

    private val deviceStates = mutableMapOf(
        "L1" to false,
        "L2" to false,
        "F1" to false,
        "F2" to false
    )

    fun toggleDevice(device: String) {
        val newState = !(deviceStates[device] ?: false)
        deviceStates[device] = newState
        sendCommandToBLE(device, newState)
        Log.d("BLEController", "$device -> $newState")
    }

    fun isDeviceOn(device: String): Boolean {
        return deviceStates[device] ?: false
    }

    private fun sendCommandToBLE(device: String, state: Boolean) {
        // Format your BLE command here, e.g., "L1:ON"
        val cmd = "$device:${if (state) "ON" else "OFF"}"
        MainActivity.sendCommand(cmd)  // Call your existing BLE sending function
    }
}
