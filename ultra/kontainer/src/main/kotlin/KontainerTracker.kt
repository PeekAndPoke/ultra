package de.peekandpoke.ultra.kontainer

import kotlinx.coroutines.launch
import java.time.Instant
import java.util.*

/**
 * Keeps track of instantiated kontainers and services
 *
 * TODO: also keep track of services... to find leaks, where services are not disposed
 */
interface KontainerTracker {

    companion object {
        fun dummy(): KontainerTracker = NullKontainerTracker()

        fun live(): KontainerTracker = LiveKontainerTracker()
    }

    /** Returns 'true' when the tracker is tracking instances */
    fun isLive(): Boolean

    /** Tracks the given [kontainer] */
    fun track(kontainer: Kontainer)

    /**
     * Gets tracked Kontainer instances and the Instant at which they were created
     */
    fun getAlive(): Map<Kontainer, Instant>

    /**
     * Gets tracked Kontainer instances and the Instant at which they were created that are older than [olderThanSec]
     */
    fun getAlive(olderThanSec: Int): Map<Kontainer, Instant>

    /**
     * Get the number of kontainers that where not yet garbage collected
     */
    fun getNumAlive(): Int

    /**
     * Get the number of kontainers that where not yet garbage collected and that are older than [olderThanSec]
     */
    fun getNumAlive(olderThanSec: Int): Int
}

/**
 * Dummy kontainer tracker, that does not track anything
 */
private class NullKontainerTracker : KontainerTracker {
    override fun isLive(): Boolean = false

    override fun track(kontainer: Kontainer) {
        // noop
    }

    override fun getAlive(): Map<Kontainer, Instant> = emptyMap()

    override fun getAlive(olderThanSec: Int): Map<Kontainer, Instant> = emptyMap()

    override fun getNumAlive(): Int = 0

    override fun getNumAlive(olderThanSec: Int): Int = 0
}

/**
 * Live kontainer tracker, that really keeps track of container instances
 */
internal class LiveKontainerTracker : KontainerTracker {

    private val lock = Object()

    private val instances = WeakHashMap<Kontainer, Instant>()

    override fun isLive(): Boolean = true

    override fun track(kontainer: Kontainer) {
        KontainerCoroutineScope.launch {
            synchronized(lock) {
                instances[kontainer] = Instant.now()
            }
        }
    }

    override fun getAlive(): Map<Kontainer, Instant> = synchronized(lock) {
        instances.toMap()
    }

    override fun getAlive(olderThanSec: Int): Map<Kontainer, Instant> = Instant.now().let { now ->
        getAlive().filterValues { now.epochSecond - it.epochSecond > olderThanSec }
    }

    override fun getNumAlive(): Int = getAlive().size

    override fun getNumAlive(olderThanSec: Int): Int = getAlive(olderThanSec).size
}
