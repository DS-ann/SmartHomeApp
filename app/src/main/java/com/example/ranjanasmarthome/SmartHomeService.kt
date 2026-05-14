package com.example.ranjanasmarthome

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

private const val TAG = "SmartHomeService"
private const val CHANNEL_ID = "SmartHomeChannel"

class SmartHomeService : Service() {

    private lateinit var bleManager: SmartHomeBleManager

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        bleManager = SmartHomeBleManager(this)
        BLEController.init(bleManager)

        MQTTController.init(applicationContext)

        startForeground(1, createNotification("Smart Home service running"))

        // Optionally start scanning right away
        scanBLE()
    }

    private fun scanBLE() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter ?: return
        if (!adapter.isEnabled) return

        val scanner = adapter.bluetoothLeScanner ?: return

        val scanCallback = object : android.bluetooth.le.ScanCallback() {
            override fun onScanResult(callbackType: Int, result: android.bluetooth.le.ScanResult?) {
                val device = result?.device ?: return
                val name = device.name ?: return
                if (name.startsWith("RanjanaSmartHome")) {
                    scanner.stopScan(this)
                    BLEController.connect(device)
                    Log.d(TAG, "BLE Device Found: $name")
                }
            }
        }

        val settings = android.bluetooth.le.ScanSettings.Builder()
            .setScanMode(android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(null, settings, scanCallback)
    }

    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Ranjana Smart Home")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Smart Home Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        BLEController.disconnect()
        MQTTController.disconnect()
        Log.d(TAG, "Smart Home service destroyed")
    }
}
