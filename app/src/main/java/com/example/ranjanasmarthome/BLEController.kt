package com.example.ranjanasmarthome

object BLEController {

    /**
     * Sends a command to your ESP32 via BLE or MQTT.
     * Currently it just logs the command.
     * Replace this with your actual BLE send implementation.
     */
    fun sendCommand(cmd: String) {
        // TODO: Replace with actual BLE/MQTT send logic
        println("BLEController: Sending command -> $cmd")

        // Example for BLE (pseudo-code, replace with your actual characteristic code):
        /*
        if (bleRunning && deviceConnected && pTxCharacteristic != null) {
            val bytes = cmd.toByteArray(Charsets.UTF_8)
            pTxCharacteristic?.value = bytes
            pTxCharacteristic?.notify()
        }
        */
    }
}
