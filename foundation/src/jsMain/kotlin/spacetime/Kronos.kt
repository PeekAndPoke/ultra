package de.peekandpoke.ultra.foundation.spacetime

import kotlin.js.Date

interface Kronos {

    companion object {
        val system: Kronos = SystemClock()
    }

    private class SystemClock : Kronos {

        override fun describe(): KronosDescriptor = KronosDescriptor.SystemClock

        override fun millisNow(): Long {
            return Date.now().toLong()
        }
    }

    private class AdvancedBy(private val durationMs: Long, private val inner: Kronos) : Kronos {

        override fun describe(): KronosDescriptor = KronosDescriptor.AdvancedBy(
            durationMs = durationMs,
            inner = inner.describe(),
        )

        override fun millisNow(): Long = inner.millisNow() + durationMs
    }

    fun advanceBy(durationMs: Long): Kronos = AdvancedBy(
        durationMs = durationMs,
        inner = this,
    )

    fun describe(): KronosDescriptor

    fun millisNow(): Long
}
