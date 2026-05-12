package com.example.ranjanasmarthome

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.util.Log
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.BleManager.BleManagerGattCallback
import no.nordicsemi.android.ble.data.Data
import java.util.*

private const val TAG = "SmartHomeBleMgr"

class SmartHomeBleManager(context: Context) : BleManager(context) {

    // Nordic UART Service UUID
    private val uartServiceUuid = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
    private val txCharacteristicUuid = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
    private val rxCharacteristicUuid = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")

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
            // Enable notifications on TX characteristic
            txCharacteristic?.let { characteristic ->
                setNotificationCallback(characteristic).with { _, data -> onDataReceived(data) }
                enableNotifications(characteristic).enqueue()
            }
        }

        override fun onDeviceDisconnected() {
            txCharacteristic = null
            rxCharacteristic = null
            Log.d(TAG, "BLE device disconnected")
        }
    }

    private fun onDataReceived(data: Data) {
        val msg = data.value?.toString(Charsets.UTF_8) ?: return
        BLEController.onMessageReceived(msg)
    }

    fun sendCommand(cmd: String) {
        rxCharacteristic?.let { characteristic ->
            writeCharacteristic(characteristic, cmd.toByteArray(Charsets.UTF_8)).enqueue()
        } ?: Log.w(TAG, "Cannot send command, RX characteristic not initialized")
    }
}
