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
private const val CHANNEL_ID = "SmartHomeServiceChannel"
private const val NOTIFICATION_ID = 101

class SmartHomeService : Service() {

    private lateinit var bleManager: SmartHomeBleManager

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Smart Home Service running"))

        // ================= BLE SETUP =================
        bleManager = SmartHomeBleManager(this)
        BLEController.init(bleManager)

        BLEController.onStateUpdate = { l1, f1, l2, f2 ->
            Log.d(TAG, "BLE update: L1=$l1 F1=$f1 L2=$l2 F2=$f2")
            WidgetState.update(l1, f1, l2, f2)
        }

        // ================= MQTT SETUP =================
        MQTTController.init(applicationContext)
        MQTTController.onStateUpdate = { l1, f1, l2, f2 ->
            Log.d(TAG, "MQTT update: L1=$l1 F1=$f1 L2=$l2 F2=$f2")
            WidgetState.update(l1, f1, l2, f2)
        }

        // ================= START BLE SCAN =================
        startBleScan()
    }

    private fun startBleScan() {
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter

        if (adapter == null || !adapter.isEnabled) {
            Log.e(TAG, "Bluetooth not available or disabled")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN)
                .equals(android.content.pm.PackageManager.PERMISSION_GRANTED)
            ) {
                Log.e(TAG, "BLUETOOTH_SCAN permission not granted")
                return
            }
            if (!checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
                .equals(android.content.pm.PackageManager.PERMISSION_GRANTED)
            ) {
                Log.e(TAG, "BLUETOOTH_CONNECT permission not granted")
                return
            }
        }

        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            Log.e(TAG, "BLE scanner not available")
            return
        }

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

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "BLE scan failed: $errorCode")
            }
        }

        val settings = android.bluetooth.le.ScanSettings.Builder()
            .setScanMode(android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(null, settings, scanCallback)
        Log.d(TAG, "BLE scan started in foreground service")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Smart Home Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Smart Home BLE and MQTT running in background"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Ranjana Smart Home")
            .setContentText(content)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        BLEController.disconnect()
        MQTTController.disconnect()
        Log.d(TAG, "Service destroyed")
    }
}
