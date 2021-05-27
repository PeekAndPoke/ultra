package de.peekandpoke.ultra.kontainer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.*
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Keeps track of instantiated kontainers and services
 *
 * TODO: also keep track of services... to find leak, where services are not disposed
 */
class KontainerTracker {

    companion object {
        val scope = CoroutineScope(EmptyCoroutineContext)
    }

    private val lock = Object()

    private val instances = WeakHashMap<Kontainer, Instant>()

    fun track(kontainer: Kontainer) {
        scope.launch {
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
