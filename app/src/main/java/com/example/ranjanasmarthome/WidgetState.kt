package com.example.ranjanasmarthome

object WidgetState {

    // Callback for UI updates
    var onStateUpdate: ((light1: Boolean, fan1: Boolean, light2: Boolean, fan2: Boolean) -> Unit)? = null

    // Internal state
    private var light1: Boolean = false
    private var fan1: Boolean = false
    private var light2: Boolean = false
    private var fan2: Boolean = false

    // Update full state
    fun update(light1: Boolean, fan1: Boolean, light2: Boolean, fan2: Boolean) {
        this.light1 = light1
        this.fan1 = fan1
        this.light2 = light2
        this.fan2 = fan2
        notifyUI()
    }

    // Partial update: only fields that are not null will change
    fun onPartialUpdate(
        light1: Boolean? = null,
        fan1: Boolean? = null,
        light2: Boolean? = null,
        fan2: Boolean? = null
    ) {
        light1?.let { this.light1 = it }
        fan1?.let { this.fan1 = it }
        light2?.let { this.light2 = it }
        fan2?.let { this.fan2 = it }
        notifyUI()
    }

    // Internal method to call the UI callback
    private fun notifyUI() {
        onStateUpdate?.invoke(light1, fan1, light2, fan2)
    }

    // Get current state
    fun getState(): StateData {
        return StateData(light1, fan1, light2, fan2)
    }

    data class StateData(
        val light1: Boolean,
        val fan1: Boolean,
        val light2: Boolean,
        val fan2: Boolean
    )
}
