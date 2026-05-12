package com.example.ranjanasmarthome

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import java.util.*

object BLEController {

    private const val TAG = "BLEController"

    // UUIDs must match your ESP32 NimBLE UUIDs
    private val SERVICE_UUID: UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
    private val CHARACTERISTIC_TX: UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
    private val CHARACTERISTIC_RX: UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var gatt: BluetoothGatt? = null
    private var txCharacteristic: BluetoothGattCharacteristic? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null

    private var context: Context? = null

    fun init(context: Context) {
        this.context = context
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = manager.adapter
    }

    // Connect to a device by address
    fun connect(address: String) {
        val device = bluetoothAdapter?.getRemoteDevice(address)
        if (device == null) {
            Log.e(TAG, "Device not found: $address")
            return
        }
        gatt = device.connectGatt(context, false, gattCallback)
    }

    fun disconnect() {
        gatt?.close()
        gatt = null
        txCharacteristic = null
        rxCharacteristic = null
    }

    fun sendCommand(command: String) {
        if (txCharacteristic == null || gatt == null) {
            Log.e(TAG, "TX characteristic not ready")
            return
        }
        txCharacteristic?.value = command.toByteArray(Charsets.UTF_8)
        gatt?.writeCharacteristic(txCharacteristic)
    }

    // Callbacks for BLE events
    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                Log.d(TAG, "Connected to BLE device")
                gatt.discoverServices()
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected from BLE device")
                txCharacteristic = null
                rxCharacteristic = null
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            val service: BluetoothGattService? = gatt.getService(SERVICE_UUID)
            if (service != null) {
                txCharacteristic = service.getCharacteristic(CHARACTERISTIC_TX)
                rxCharacteristic = service.getCharacteristic(CHARACTERISTIC_RX)
                gatt.setCharacteristicNotification(rxCharacteristic, true)
                Log.d(TAG, "BLE service and characteristics ready")
            } else {
                Log.e(TAG, "BLE service not found")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            if (characteristic.uuid == CHARACTERISTIC_RX) {
                val message = characteristic.value.toString(Charsets.UTF_8)
                Log.d(TAG, "Received BLE message: $message")
                // Update widget state from ESP32
                SmartHomeWidget.updateStateFromESP(message)
            }
        }
    }
}
