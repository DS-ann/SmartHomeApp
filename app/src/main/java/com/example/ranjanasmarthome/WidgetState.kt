package com.example.ranjanasmarthome

object WidgetState {
    // Callback for UI updates
    var onStateUpdate: ((light1: Boolean, fan1: Boolean, light2: Boolean, fan2: Boolean) -> Unit)? = null

    fun update(light1: Boolean, fan1: Boolean, light2: Boolean, fan2: Boolean) {
        onStateUpdate?.invoke(light1, fan1, light2, fan2)
    }
}
