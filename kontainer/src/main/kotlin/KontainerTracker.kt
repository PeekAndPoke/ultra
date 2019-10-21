package de.peekandpoke.ultra.kontainer

import java.time.Instant
import java.util.*

/**
 * Keeps track of instantiated kontainers and services
 *
 * TODO: also keep track of services... to find leak, where services are not disposed
 */
class KontainerTracker {

    private val instances = WeakHashMap<Kontainer, Instant>()

    fun track(kontainer: Kontainer) {
        instances[kontainer] = Instant.now()
    }

    fun getAlive(): Map<Kontainer, Instant> = instances.toMap()

    fun getAlive(olderThanSec: Int): Map<Kontainer, Instant> = Instant.now().let { now ->
        instances.filterValues { now.epochSecond - it.epochSecond > olderThanSec }
    }

    fun getNumAlive(): Int = instances.toMap().size

    fun getNumAlive(olderThanSec: Int): Int = getAlive(olderThanSec).size
}
