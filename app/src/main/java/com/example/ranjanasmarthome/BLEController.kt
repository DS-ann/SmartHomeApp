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
            // Local state variables
            var light1: Boolean? = null
            var fan1: Int? = null
            var light2: Boolean? = null
            var fan2: Int? = null

            // Parse message format: "a:XY" or "b:XY"
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

            // Update WidgetState with partial values
            WidgetState.onPartialUpdate(
                light1 = light1,
                fan1   = fan1,
                light2 = light2,
                fan2   = fan2
            )

            // Notify UI callbacks
            onStateUpdate?.invoke(
                WidgetState.getState().light1,
                WidgetState.getState().fan1,
                WidgetState.getState().light2,
                WidgetState.getState().fan2
            )

        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode BLE message: $msg", e)
        }
    }
}
