package com.example.ranjanasmarthome

import android.content.Context
import android.content.Intent

object BLEController {
    fun sendCommand(cmd: String, context: Context) {
        // Use broadcast to MainActivity
        val intent = Intent("com.example.ranjanasmarthome.SEND_BLE_CMD")
        intent.putExtra("cmd", cmd)
        context.sendBroadcast(intent)
    }
}
