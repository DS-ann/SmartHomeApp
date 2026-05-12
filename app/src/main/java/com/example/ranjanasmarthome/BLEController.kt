package com.example.ranjanasmarthome

import android.util.Log
import java.util.concurrent.ConcurrentLinkedQueue

object BLEController {

    private const val TAG = "BLEController"

    // This queue stores commands sent before BLE is ready
    private val commandQueue = ConcurrentLinkedQueue<String>()

    // BLE ready flag
    @Volatile
    var isBleReady = false
        private set

    // This should be called when BLE is initialized
    fun setBleReady(ready: Boolean) {
        isBleReady = ready
        if (ready) {
            flushQueue()
        }
    }

    // Send a command safely
    fun sendCommand(cmd: String) {
        if (isBleReady) {
            try {
                // Replace this with your actual BLE write logic
                writeToBle(cmd)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send BLE command: $cmd", e)
                // Optionally queue failed command
                commandQueue.offer(cmd)
            }
        } else {
            // Queue command until BLE is ready
            Log.w(TAG, "BLE not ready, queuing command: $cmd")
            commandQueue.offer(cmd)
        }
    }

    // Flush queued commands
    private fun flushQueue() {
        while (commandQueue.isNotEmpty()) {
            val cmd = commandQueue.poll() ?: continue
            try {
                writeToBle(cmd)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send queued BLE command: $cmd", e)
                // Re-queue if needed
                commandQueue.offer(cmd)
                break
            }
        }
    }

    // Mock BLE write — replace with actual BLE logic
    private fun writeToBle(cmd: String) {
        // Example: write to characteristic
        // pTxCharacteristic?.setValue(cmd.toByteArray())
        // pTxCharacteristic?.notify()
        Log.d(TAG, "Sending BLE command: $cmd")
    }
}
