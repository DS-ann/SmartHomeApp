package com.example.ranjanasmarthome

import android.bluetooth.BluetoothDevice
import android.util.Log
import no.nordicsemi.android.ble.observer.ConnectionObserverAdapter

private const val TAG = "BLEController"

object BLEController {

    private var bleManager: SmartHomeBleManager? = null
    var deviceConnected = false

    /** Initialize BLEController with BLE manager */
    fun init(manager: SmartHomeBleManager) {
        bleManager = manager

        // Subscribe to connection events using adapter
        bleManager?.setConnectionObserver(object : ConnectionObserverAdapter() {
            override fun onDeviceConnecting(device: BluetoothDevice) {
                Log.d(TAG, "Connecting to ${device.address}")
            }

            override fun onDeviceConnected(device: BluetoothDevice) {
                deviceConnected = true
                Log.d(TAG, "BLE connected to ${device.address}")
            }

            override fun onDeviceDisconnecting(device: BluetoothDevice) {
                Log.d(TAG, "Disconnecting from ${device.address}")
            }

            override fun onDeviceDisconnected(device: BluetoothDevice, reason: Int) {
                deviceConnected = false
                Log.d(TAG, "BLE disconnected from ${device.address}, reason: $reason")
            }
        })
    }

    /** Connect to BLE device */
    fun connect(device: BluetoothDevice) {
        bleManager?.connect(device)?.enqueue()
    }

    /** Disconnect from BLE device */
    fun disconnect() {
        bleManager?.disconnect()?.enqueue()
    }

    /** Send command over BLE */
    fun sendCommand(cmd: String) {
        if (deviceConnected) {
            bleManager?.sendCommand(cmd)
        } else {
            Log.w(TAG, "Cannot send, device not connected")
        }
    }

    /** Called when BLE receives data */
    fun onMessageReceived(msg: String) {
        try {
            when {
                msg.startsWith("a:") -> {
                    val light1 = msg.getOrNull(3) == '1'
                    val fan1 = msg.getOrNull(4) == '1'
                    WidgetState.update(light1, if (fan1) 1 else 0, null, null)
                }
                msg.startsWith("b:") -> {
                    val light2 = msg.getOrNull(3) == '1'
                    val fan2 = msg.getOrNull(4) == '1'
                    WidgetState.update(null, null, light2, if (fan2) 1 else 0)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode BLE message: $msg", e)
        }
    }
}
