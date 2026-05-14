package com.example.ranjanasmarthome

import android.bluetooth.BluetoothDevice
import android.util.Log
import no.nordicsemi.android.ble.observer.ConnectionObserver

private const val TAG = "BLEController"

object BLEController {

    private var bleManager: SmartHomeBleManager? = null
    var deviceConnected: Boolean = false
        private set

    // Add proper onStateUpdate callback
    var onStateUpdate: ((light1: Boolean, fan1: Int, light2: Boolean, fan2: Int) -> Unit)? = null

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

            override fun onDeviceReady(device: BluetoothDevice) {
                Log.d(TAG, "Device is ready: ${device.address}")
            }

            override fun onDeviceDisconnecting(device: BluetoothDevice) {
                Log.d(TAG, "Disconnecting from ${device.address}")
            }

            override fun onDeviceDisconnected(device: BluetoothDevice, reason: Int) {
                deviceConnected = false
                Log.d(TAG, "Disconnected from ${device.address}, reason=$reason")
            }

            override fun onDeviceFailedToConnect(device: BluetoothDevice, reason: Int) {
                Log.e(TAG, "Failed to connect to ${device.address}, reason=$reason")
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
            var light1 = false
            var fan1 = 0
            var light2 = false
            var fan2 = 0

            when {
                msg.startsWith("a:") -> {
                    light1 = msg.getOrNull(3) == '1'
                    fan1 = if (msg.getOrNull(4) == '1') 1 else 0
                }
                msg.startsWith("b:") -> {
                    light2 = msg.getOrNull(3) == '1'
                    fan2 = if (msg.getOrNull(4) == '1') 1 else 0
                }
            }

            // Update widget and notify subscribers
            WidgetState.onPartialUpdate(light1 = if (msg.startsWith("a:")) light1 else null,
                                        fan1   = if (msg.startsWith("a:")) fan1 else null,
                                        light2 = if (msg.startsWith("b:")) light2 else null,
                                        fan2   = if (msg.startsWith("b:")) fan2 else null)

            onStateUpdate?.invoke(light1, fan1, light2, fan2)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode BLE message: $msg", e)
        }
    }
}
