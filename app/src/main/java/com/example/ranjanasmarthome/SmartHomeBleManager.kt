package com.example.ranjanasmarthome

import android.bluetooth.BluetoothGatt
import android.content.Context
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.data.Data

class SmartHomeBleManager(context: Context) : BleManager(context) {

    private var txCharacteristicUuid = java.util.UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
    private var rxCharacteristicUuid = java.util.UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")

    override fun getGattCallback(): BleManagerGattCallback = object : BleManagerGattCallback() {

        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            return gatt.getService(txCharacteristicUuid) != null &&
                   gatt.getService(rxCharacteristicUuid) != null
        }

        override fun initialize() {
            // Enable notifications on TX
            setNotificationCallback(txCharacteristicUuid).with { _, data -> onDataReceived(data) }
            enableNotifications(txCharacteristicUuid).enqueue()
        }

        override fun onDeviceDisconnected() {}
    }

    private fun onDataReceived(data: Data) {
        val msg = data.value?.toString(Charsets.UTF_8) ?: return
        BLEController.onMessageReceived(msg)
    }

    fun sendCommand(cmd: String) {
        writeCharacteristic(rxCharacteristicUuid, cmd.toByteArray(Charsets.UTF_8)).enqueue()
    }
}
