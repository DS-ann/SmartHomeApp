package com.example.ranjanasmarthome

import android.content.Context

object BLEController {

    private var connected: Boolean = false
    private var context: Context? = null

    fun init(context: Context) {
        this.context = context.applicationContext
    }

    fun setConnected(state: Boolean) {
        connected = state
    }

    fun isConnected() = connected

    fun sendCommand(cmd: String) {
        if (!connected) return
        println("BLE command sent: $cmd")
        // TODO: actual BLE write
    }

    // Call this when ESP sends relay status via BLE
    fun onESPMessage(msg: String) {
        context?.let { ctx ->
            // Example parsing: "R01010000" → relay 0 ON, relay 1 OFF ...
            if (msg.length >= 6) {
                val light0 = msg[1] == '1'
                val fan1 = msg[2] == '1'
                val light4 = msg[5] == '1'
                val fan5 = msg[6] == '1'
                SmartHomeWidget.updateStateFromESP(light0, light4, fan1, fan5, ctx)
            }
        }
    }
}
