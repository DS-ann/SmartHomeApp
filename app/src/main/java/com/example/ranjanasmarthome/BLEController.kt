package com.example.ranjanasmarthome

import android.bluetooth.BluetoothDevice
import android.util.Log
import no.nordicsemi.android.ble.observer.ConnectionObserver

private const val TAG = "BLEController"

object BLEController {

    private var bleManager: SmartHomeBleManager? = null
    var deviceConnected = false

    /** Initialize BLEController with your BLE manager */
    fun init(manager: SmartHomeBleManager) {
        bleManager = manager

        // Observe connection events
        bleManager?.observeConnection(object : ConnectionObserver {
            override fun onDeviceConnected(device: BluetoothDevice) {
                deviceConnected = true
                Log.d(TAG, "BLE connected to ${device.address}")
            }

            override fun onDeviceDisconnected(device: BluetoothDevice, reason: Int) {
                deviceConnected = false
                Log.d(TAG, "BLE disconnected from ${device.address}, reason $reason")
            }
        })
    }

    /** Connect to a BLE device by address */
    fun connect(address: String) {
        val device = bleManager?.getBluetoothDevice(address)
        device?.let {
            bleManager?.connect(it)?.enqueue()
        } ?: Log.e(TAG, "Cannot connect, device not found: $address")
    }

    /** Disconnect from BLE device */
    fun disconnect() {
        bleManager?.disconnect()?.enqueue()
    }

    /** Send command string over BLE */
    fun sendCommand(cmd: String) {
        if (deviceConnected) {
            bleManager?.sendCommand(cmd)
        } else {
            Log.w(TAG, "Cannot send, device not connected")
        }
    }

    /** Called whenever BLE receives data from the device */
    fun onMessageReceived(msg: String) {
        try {
            when {
                msg.startsWith("a:") -> {
                    val light1 = msg.getOrNull(3) == '1'
                    val fan1 = msg.getOrNull(4) == '1'
                    WidgetState.onPartialUpdate(
                        light1 = light1,
                        fan1 = fan1
                    )
                }
                msg.startsWith("b:") -> {
                    val light2 = msg.getOrNull(3) == '1'
                    val fan2 = msg.getOrNull(4) == '1'
                    WidgetState.onPartialUpdate(
                        light2 = light2,
                        fan2 = fan2
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode BLE message: $msg", e)
        }
    }
}
