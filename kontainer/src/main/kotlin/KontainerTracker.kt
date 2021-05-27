package de.peekandpoke.ultra.kontainer

import kotlinx.coroutines.launch
import java.time.Instant
import java.util.*

/**
 * Keeps track of instantiated kontainers and services
 *
 * TODO: also keep track of services... to find leaks, where services are not disposed
 */
class KontainerTracker {

    private val lock = Object()

    private val instances = WeakHashMap<Kontainer, Instant>()

    fun track(kontainer: Kontainer) {
        KontainerCoroutineScope.launch {
            synchronized(lock) {
                instances[kontainer] = Instant.now()
            }
        }
    }

    fun getAlive(): Map<Kontainer, Instant> = synchronized(lock) {
        instances.toMap()
    }

    fun getAlive(olderThanSec: Int): Map<Kontainer, Instant> = Instant.now().let { now ->
        getAlive().filterValues { now.epochSecond - it.epochSecond > olderThanSec }
    }

    fun getNumAlive(): Int = getAlive().size

    fun getNumAlive(olderThanSec: Int): Int = getAlive(olderThanSec).size
}
