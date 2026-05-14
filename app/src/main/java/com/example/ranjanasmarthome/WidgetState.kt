package com.example.ranjanasmarthome

object WidgetState {

    // Callback for UI updates
    var onStateUpdate: ((light1: Boolean, fan1: Int, light2: Boolean, fan2: Int) -> Unit)? = null

    // Internal state
    private var light1: Boolean = false
    private var fan1: Int = 0
    private var light2: Boolean = false
    private var fan2: Int = 0

    /** Update the full state at once */
    fun update(light1: Boolean, fan1: Int, light2: Boolean, fan2: Int) {
        this.light1 = light1
        this.fan1 = fan1.coerceIn(0..100) // Optional: clamp fan speed
        this.light2 = light2
        this.fan2 = fan2.coerceIn(0..100)
        notifyUI()
    }

    /** Partial update: only the fields that are not null will change */
    fun onPartialUpdate(
        light1: Boolean? = null,
        fan1: Int? = null,
        light2: Boolean? = null,
        fan2: Int? = null
    ) {
        light1?.let { this.light1 = it }
        fan1?.let { this.fan1 = it.coerceIn(0..100) }
        light2?.let { this.light2 = it }
        fan2?.let { this.fan2 = it.coerceIn(0..100) }
        notifyUI()
    }

    /** Notify the UI or any listener about current state */
    private fun notifyUI() {
        onStateUpdate?.invoke(light1, fan1, light2, fan2)
    }

    /** Get a snapshot of the current state */
    fun getState(): StateData = StateData(light1, fan1, light2, fan2)

    /** Immutable data class for current state */
    data class StateData(
        val light1: Boolean,
        val fan1: Int,
        val light2: Boolean,
        val fan2: Int
    )
}
