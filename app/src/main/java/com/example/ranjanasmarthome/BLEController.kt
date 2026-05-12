package com.example.ranjanasmarthome

import android.bluetooth.BluetoothDevice
import android.util.Log
import no.nordicsemi.android.ble.observer.ConnectionObserver

private const val TAG = "BLEController"

object BLEController {

    private var bleManager: SmartHomeBleManager? = null
    var deviceConnected: Boolean = false
        private set

    fun init(manager: SmartHomeBleManager) {
        bleManager = manager

        bleManager?.setConnectionObserver(object : ConnectionObserver {
            override fun onDeviceConnecting(device: BluetoothDevice) {
                Log.d(TAG, "Connecting to ${device.address}")
            }

            override fun onDeviceConnected(device: BluetoothDevice) {
                deviceConnected = true
                Log.d(TAG, "Connected to ${device.address}")
            }

            override fun onDeviceDisconnecting(device: BluetoothDevice) {
                Log.d(TAG, "Disconnecting from ${device.address}")
            }

            override fun onDeviceDisconnected(device: BluetoothDevice, reason: Int) {
                deviceConnected = false
                Log.d(TAG, "Disconnected from ${device.address}, reason=$reason")
            }
        })
    }

    fun connect(device: BluetoothDevice) {
        bleManager?.connect(device)?.enqueue()
    }

    fun disconnect() {
        bleManager?.disconnect()?.enqueue()
    }

    fun sendCommand(cmd: String) {
        if (deviceConnected) {
            bleManager?.sendCommand(cmd)
        } else {
            Log.w(TAG, "Cannot send command, device not connected")
        }
    }

    fun onMessageReceived(msg: String) {
        try {
            when {
                msg.startsWith("a:") -> {
                    val light1 = msg.getOrNull(3) == '1'
                    val fan1 = msg.getOrNull(4) == '1'
                    WidgetState.update(light1 = light1, fan1Speed = if (fan1) 1 else 0)
                }
                msg.startsWith("b:") -> {
                    val light2 = msg.getOrNull(3) == '1'
                    val fan2 = msg.getOrNull(4) == '1'
                    WidgetState.update(light2 = light2, fan2Speed = if (fan2) 1 else 0)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode BLE message: $msg", e)
        }
    }
}
