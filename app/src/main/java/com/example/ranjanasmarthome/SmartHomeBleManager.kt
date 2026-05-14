package com.example.ranjanasmarthome

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.util.Log
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.data.Data
import java.util.*

private const val TAG = "SmartHomeBleMgr"

class SmartHomeBleManager(context: Context) : BleManager(context) {

    // Nordic UART Service UUIDs
    private val uartServiceUuid = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
    private val txCharacteristicUuid = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e") // device → phone
    private val rxCharacteristicUuid = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e") // phone → device

    private var txCharacteristic: BluetoothGattCharacteristic? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null

    override fun getGattCallback(): BleManagerGattCallback = object : BleManagerGattCallback() {

        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            val service = gatt.getService(uartServiceUuid)
            if (service != null) {
                txCharacteristic = service.getCharacteristic(txCharacteristicUuid)
                rxCharacteristic = service.getCharacteristic(rxCharacteristicUuid)
                return txCharacteristic != null && rxCharacteristic != null
            }
            return false
        }

        override fun initialize() {
            txCharacteristic?.let { characteristic ->
                // Enable notifications for TX characteristic (device → phone)
                setNotificationCallback(characteristic).with { _, data ->
                    onDataReceived(data)
                }
                enableNotifications(characteristic).enqueue()
                Log.d(TAG, "Notifications enabled on TX characteristic")
            } ?: Log.w(TAG, "TX characteristic not found during initialization")
        }

        override fun onDeviceDisconnected() {
            txCharacteristic = null
            rxCharacteristic = null
            Log.d(TAG, "BLE device disconnected")
        }

        override fun onServicesInvalidated() {
            txCharacteristic = null
            rxCharacteristic = null
            Log.d(TAG, "BLE services invalidated")
        }
    }

    private fun onDataReceived(data: Data) {
        val msg = data.value?.toString(Charsets.UTF_8)
        if (!msg.isNullOrEmpty()) {
            Log.d(TAG, "Received BLE message: $msg")
            BLEController.onMessageReceived(msg)
        }
    }

    /** Send a command to the BLE device */
    fun sendCommand(cmd: String) {
        val device = getBluetoothDevice()
        if (device == null || !isConnected) {
            Log.w(TAG, "Cannot send command, device not connected")
            return
        }

        rxCharacteristic?.let { characteristic ->
            writeCharacteristic(characteristic, cmd.toByteArray(Charsets.UTF_8))
                .with { _, _ -> Log.d(TAG, "Sent BLE command: $cmd") }
                .enqueue()
        } ?: Log.w(TAG, "Cannot send command, RX characteristic not initialized")
    }
}
