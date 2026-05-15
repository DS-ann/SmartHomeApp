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

    /** Callback for UI updates or widget updates */
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
     * Parses ESP32 messages (a:Rxxxx, b:Rxxxx, F0,2) and updates states.
     */
    fun onMessageReceived(msg: String) {
        try {
            val relayState = BooleanArray(8) { false } // temporary, only relevant relays updated
            val fanSpeed = IntArray(2) { 0 }

            when {
                msg.startsWith("a:") && msg.contains("R") -> {
                    val rIndex = msg.indexOf('R')
                    for (i in 0..3) {
                        val c = msg.getOrNull(rIndex + 1 + i) ?: '0'
                        relayState[i] = c == '1'
                    }
                }
                msg.startsWith("b:") && msg.contains("R") -> {
                    val rIndex = msg.indexOf('R')
                    for (i in 0..3) {
                        val c = msg.getOrNull(rIndex + 1 + i) ?: '0'
                        relayState[4 + i] = c == '1'
                    }
                }
                msg.startsWith("F") -> {
                    val parts = msg.substring(1).split(",")
                    if (parts.size == 2) {
                        val fan = parts[0].toIntOrNull()
                        val speed = parts[1].toIntOrNull()
                        if (fan in 0..1 && speed in 0..4) fanSpeed[fan] = speed
                    }
                }
            }

            // Update WidgetState (only first 2 relays, fan ignored)
            WidgetState.update(
                light1 = relayState[0],
                fan1 = 0, // widget ignores fan
                light2 = relayState[1],
                fan2 = 0
            )

            // Update WebView (all relays + fans)
            MainActivity.updateWebViewFull(relayState, fanSpeed)

            // Optional UI callback
            onStateUpdate?.invoke(
                relayState[0], 0,
                relayState[1], 0
            )

        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse BLE message: $msg", e)
        }
    }
}
