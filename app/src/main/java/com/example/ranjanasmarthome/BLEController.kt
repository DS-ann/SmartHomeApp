package com.example.ranjanasmarthome

import android.bluetooth.BluetoothDevice
import android.util.Log
import no.nordicsemi.android.ble.observer.ConnectionObserver

private const val TAG = "BLEController"

/**
 * Central BLE controller for Smart Home device.
 * Handles connection, command sending, and incoming messages.
 */
object BLEController {

    private var bleManager: SmartHomeBleManager? = null

    /** Current device connection state */
    var deviceConnected: Boolean = false
        private set

    /**
     * Callback for UI updates or widget updates.
     * Emits: light1 (Boolean), fan1 (Int), light2 (Boolean), fan2 (Int)
     */
    var onStateUpdate: ((light1: Boolean, fan1: Int, light2: Boolean, fan2: Int) -> Unit)? = null

    /** Initialize BLE controller with SmartHomeBleManager */
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
                Log.d(TAG, "Device ready: ${device.address}")
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

    /** Connect to a BLE device */
    fun connect(device: BluetoothDevice) {
        bleManager?.connect(device)?.enqueue()
    }

    /** Disconnect from BLE device */
    fun disconnect() {
        bleManager?.disconnect()?.enqueue()
    }

    /** Send a command string to BLE device */
    fun sendCommand(cmd: String) {
        if (deviceConnected) {
            bleManager?.sendCommand(cmd)
        } else {
            Log.w(TAG, "Cannot send command, device not connected")
        }
    }

    /**
     * Called by BLE manager when a message is received.
     * Parses and updates WidgetState.
     */
    fun onMessageReceived(msg: String) {
        try {
            // Default values: null = no change
            var light1: Boolean? = null
            var fan1: Int? = null
            var light2: Boolean? = null
            var fan2: Int? = null

            // Example message formats:
            // "a:L1F1" => light1, fan1
            // "b:L2F2" => light2, fan2
            when {
                msg.startsWith("a:") && msg.length >= 5 -> {
                    light1 = msg[2] == '1'
                    fan1 = msg[3].digitToIntOrNull() ?: 0
                }
                msg.startsWith("b:") && msg.length >= 5 -> {
                    light2 = msg[2] == '1'
                    fan2 = msg[3].digitToIntOrNull() ?: 0
                }
            }

            // Update WidgetState partially
            WidgetState.onPartialUpdate(
                light1 = light1,
                fan1 = fan1,
                light2 = light2,
                fan2 = fan2
            )

            // Notify UI / MainActivity callbacks
            val state = WidgetState.getState()
            onStateUpdate?.invoke(state.light1, state.fan1, state.light2, state.fan2)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse BLE message: $msg", e)
        }
    }
}
