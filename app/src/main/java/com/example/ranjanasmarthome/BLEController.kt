package com.example.ranjanasmarthome

import android.bluetooth.BluetoothDevice
import android.util.Log
import no.nordicsemi.android.ble.observer.ConnectionObserver

private const val TAG = "BLEController"

object BLEController {

    private var bleManager: SmartHomeBleManager? = null
    var deviceConnected: Boolean = false
        private set

    // Callback for UI updates
    var onStateUpdate: ((light1: Boolean, fan1: Boolean, light2: Boolean, fan2: Boolean) -> Unit)? = null

    // Current device state
    private var light1State: Boolean = false
    private var fan1State: Boolean = false
    private var light2State: Boolean = false
    private var fan2State: Boolean = false

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
            when {
                msg.startsWith("a:") -> {
                    light1State = msg.getOrNull(3) == '1'
                    fan1State   = msg.getOrNull(4) == '1'
                }
                msg.startsWith("b:") -> {
                    light2State = msg.getOrNull(3) == '1'
                    fan2State   = msg.getOrNull(4) == '1'
                }
            }

            // Update widget state safely (nullable partial update)
            WidgetState.onPartialUpdate(
                light1 = if (msg.startsWith("a:")) light1State else null,
                fan1   = if (msg.startsWith("a:")) fan1State else null,
                light2 = if (msg.startsWith("b:")) light2State else null,
                fan2   = if (msg.startsWith("b:")) fan2State else null
            )

            // Notify listeners
            onStateUpdate?.invoke(light1State, fan1State, light2State, fan2State)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode BLE message: $msg", e)
        }
    }
}
