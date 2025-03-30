package de.peekandpoke.ktorfx.insights

import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

class StopWatch {

    private var startNs: Long = System.nanoTime()
    private var endNs: Long? = null

    fun end() {
        endNs = System.nanoTime()
    }

    fun totalDuration(): Duration {

        val currentEndNs = endNs ?: System.nanoTime()

        val elapsedNs = currentEndNs - startNs

        return elapsedNs.nanoseconds
    }
}
