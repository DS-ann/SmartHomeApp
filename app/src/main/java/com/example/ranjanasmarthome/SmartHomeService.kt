package com.example.ranjanasmarthome

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log

private const val TAG = "SmartHomeService"

class SmartHomeService : Service() {

    private val binder = LocalBinder()

    companion object {
        private var bleManager: SmartHomeBleManager? = null
        private var mqttController: MQTTController? = null

        /** Call this from Widget to send relay commands */
        fun sendRelayCommand(relayNumber: String, state: Boolean) {
            val cmd = "$relayNumber${if (state) 1 else 0}"
            Log.d(TAG, "Sending command: $cmd")

            // Send via BLE if connected
            bleManager?.let {
                if (it.isConnectedExternal) it.sendCommand(cmd)
            }

            // Send via MQTT if connected
            mqttController?.let {
                if (it.isConnected()) it.publishRelay(cmd)
            }
        }

        /** Call this from MainActivity to set BLE manager */
        fun setBleManager(manager: SmartHomeBleManager) {
            bleManager = manager
        }

        /** Call this from MainActivity to set MQTT controller */
        fun setMqttController(controller: MQTTController) {
            mqttController = controller
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "SmartHomeService started")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    inner class LocalBinder : Binder() {
        fun getService(): SmartHomeService = this@SmartHomeService
    }

    override fun onDestroy() {
        Log.d(TAG, "SmartHomeService destroyed")
        super.onDestroy()
    }
}
