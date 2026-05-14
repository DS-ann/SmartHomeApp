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
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat

private const val TAG = "SmartHomeService"
private const val CHANNEL_ID = "SmartHomeServiceChannel"
private const val NOTIFICATION_ID = 101

class SmartHomeService : Service() {

    private lateinit var bleManager: SmartHomeBleManager
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Service starting..."))

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
        mainHandler.post { startBleScan() }
    }

    private fun startBleScan() {
        try {
            val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = bluetoothManager.adapter
            if (adapter == null || !adapter.isEnabled) {
                Log.w(TAG, "Bluetooth not available or disabled")
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    Log.w(TAG, "BLUETOOTH_SCAN permission missing")
                    return
                }
            }

            val scanner = adapter.bluetoothLeScanner ?: run {
                Log.w(TAG, "BLE scanner not available")
                return
            }

            val scanCallback = object : android.bluetooth.le.ScanCallback() {
                override fun onScanResult(callbackType: Int, result: android.bluetooth.le.ScanResult?) {
                    try {
                        val device = result?.device ?: return
                        val name = device.name ?: return
                        if (name.startsWith("RanjanaSmartHome")) {
                            scanner.stopScan(this)
                            BLEController.connect(device)
                            mainHandler.post {
                                Log.d(TAG, "BLE Device Found: $name")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "BLE scan callback error: ${e.message}")
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
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start BLE scan: ${e.message}", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Smart Home Background Service",
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
            .setSmallIcon(R.mipmap.ic_launcher) // your launcher icon
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
