package com.example.ranjanasmarthome

object WidgetState {

    // Fans: 0 = OFF, 1 = ON
    var fan1: Int = 0
    var fan2: Int = 0

    // Lights: false = OFF, true = ON
    var light1: Boolean = false
    var light2: Boolean = false

    // Optional callback for UI updates (MainActivity)
    var onStateUpdate: ((light1: Boolean, fan1: Int, light2: Boolean, fan2: Int) -> Unit)? = null

    // Call this whenever state changes to notify UI
    fun notifyUpdate() {
        onStateUpdate?.invoke(light1, fan1, light2, fan2)
    }
}
