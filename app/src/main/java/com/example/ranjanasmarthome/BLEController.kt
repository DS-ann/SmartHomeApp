package com.example.ranjanasmarthome

import android.util.Log
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.data.Data
import no.nordicsemi.android.ble.observer.ConnectionObserver

private const val TAG = "BLEController"

object BLEController {

    private var bleManager: SmartHomeBleManager? = null
    var deviceConnected = false

    fun init(manager: SmartHomeBleManager) {
        bleManager = manager
        bleManager?.setConnectionObserver(object : ConnectionObserver {
            override fun onDeviceConnected(deviceAddress: String) {
                deviceConnected = true
                Log.d(TAG, "BLE connected to $deviceAddress")
            }

            override fun onDeviceDisconnected(deviceAddress: String) {
                deviceConnected = false
                Log.d(TAG, "BLE disconnected from $deviceAddress")
            }
        })
    }

    fun connect(address: String) {
        bleManager?.connect(address)?.enqueue()
    }

    fun disconnect() {
        bleManager?.disconnect()?.enqueue()
    }

    fun sendCommand(cmd: String) {
        if (deviceConnected) {
            bleManager?.sendCommand(cmd)
        }
    }

    // Called whenever BLE receives data
    fun onMessageReceived(msg: String) {
        try {
            when {
                msg.startsWith("a:") -> {
                    val light1 = msg.getOrNull(3) == '1'
                    val fan1 = msg.getOrNull(4) == '1'
                    WidgetState.onPartialUpdate(
                        light1On = light1,
                        fan1Speed = if (fan1) 1 else 0
                    )
                }
                msg.startsWith("b:") -> {
                    val light2 = msg.getOrNull(3) == '1'
                    val fan2 = msg.getOrNull(4) == '1'
                    WidgetState.onPartialUpdate(
                        light2On = light2,
                        fan2Speed = if (fan2) 1 else 0
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode BLE message: $msg", e)
        }
    }
}
