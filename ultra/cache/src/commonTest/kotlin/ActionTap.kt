package io.peekandpoke.ultra.cache

import io.peekandpoke.ultra.common.RunSync
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * Records every action a cache's loop hands to its behaviours.
 *
 * A test feeds the recording to a behaviour it drives by hand, so that behaviour sees exactly what the
 * real cache produced, and the test decides what it does NOT see.
 */
class ActionTap<K, V> : FastCache.Behaviour<K, V> {
    private val lock = Any()
    private val recorded = mutableListOf<FastCache.Action<K, V>>()
    private var taken = 0

    /** Every action recorded so far, in order. */
    val actions: List<FastCache.Action<K, V>> get() = RunSync(lock) { recorded.toList() }

    /** The actions recorded since the last call, in order: the next batch to feed a hand-driven behaviour. */
    fun takeNew(): List<FastCache.Action<K, V>> = RunSync(lock) {
        recorded.subList(taken, recorded.size).toList().also { taken = recorded.size }
    }

    override fun process(cache: FastCache<K, V>, updates: FastCache.ActionUpdates<K, V>) {
        RunSync(lock) { recorded.addAll(updates.actions) }
    }
}

/**
 * Whether [refresh] (refresh TTL 100 ms) still holds a write time for "a", after it was fed what the
 * cache recorded.
 *
 * "a" is put back into [cache] without [refresh] seeing that put, and the refresh TTL is let pass: only
 * a write time it failed to drop makes it load "a" now. The loader must count its calls into [loads].
 */
suspend fun refreshStillTracksA(
    cache: FastCache<String, String>,
    refresh: FastCache.RefreshAfterWriteBehaviour<String, String>,
    loads: () -> Int,
): Boolean {
    cache.put("a", "back")
    delay(150.milliseconds)
    refresh.process(cache, FastCache.ActionUpdates(emptyList()))

    // a launched loader runs on another coroutine
    delay(100.milliseconds)

    return loads() > 0
}
