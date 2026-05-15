package com.example.ranjanasmarthome

object WidgetState {

    // Only 4 devices for the widget
    var light1: Boolean = false
    var fan1: Int = 0
    var light2: Boolean = false
    var fan2: Int = 0

    // Callback for widget updates
    var onStateUpdate: ((l1: Boolean, f1: Int, l2: Boolean, f2: Int) -> Unit)? = null

    // Update the 4 tracked devices
    fun update(l1: Boolean, f1: Int, l2: Boolean, f2: Int) {
        light1 = l1
        fan1 = f1
        light2 = l2
        fan2 = f2
        onStateUpdate?.invoke(l1, f1, l2, f2)
    }
}
