package com.example.ranjanasmarthome

import android.util.Log

private const val TAG = "WidgetState"

/**
 * Central state manager for Smart Home devices.
 * Tracks lights (Boolean) and fans (0–100 Int) states.
 * Supports full and partial updates.
 */
object WidgetState {

    /** Callback for UI or widget updates (volatile for thread safety) */
    @Volatile
    var onStateUpdate: ((light1: Boolean, fan1: Int, light2: Boolean, fan2: Int) -> Unit)? = null

    // Internal state
    private var light1: Boolean = false
    private var fan1: Int = 0
    private var light2: Boolean = false
    private var fan2: Int = 0

    /** Update all values at once */
    @Synchronized
    fun update(light1: Boolean, fan1: Int, light2: Boolean, fan2: Int) {
        this.light1 = light1
        this.fan1 = fan1.coerceIn(0..100)
        this.light2 = light2
        this.fan2 = fan2.coerceIn(0..100)
        notifyUI()
    }

    /** Partial update: only non-null values are changed */
    @Synchronized
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

    /** Notify UI / widget / MainActivity of current state */
    @Synchronized
    private fun notifyUI() {
        try {
            onStateUpdate?.invoke(light1, fan1, light2, fan2)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to notify UI: ${e.message}")
        }
        Log.d(TAG, "WidgetState updated: L1=$light1 F1=$fan1 L2=$light2 F2=$fan2")
    }

    /** Return a snapshot of current state */
    @Synchronized
    fun getState(): StateData = StateData(light1, fan1, light2, fan2)

    /** Immutable data class for snapshot */
    data class StateData(
        val light1: Boolean,
        val fan1: Int,
        val light2: Boolean,
        val fan2: Int
    )
}
