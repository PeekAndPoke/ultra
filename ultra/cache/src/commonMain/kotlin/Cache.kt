package io.peekandpoke.ultra.cache

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * A generic key-value cache interface.
 *
 * Implementations provide thread-safe storage with optional eviction behaviours
 * such as TTL-based expiration, max-entries limits, and memory-usage caps.
 *
 * @param K the type of cache keys
 * @param V the type of cache values
 */
interface Cache<K, V> {

    /**
     * Shared defaults plus the [isEmpty] / [isNotEmpty] extensions.
     *
     * Being members of the companion, the extensions need `Cache.Companion` in scope to be callable.
     */
    companion object {
        /** Default [CoroutineScope] used by cache implementations for background processing. */
        val defaultCoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

        /** Checks if the cache is empty. */
        fun <K, V> Cache<K, V>.isEmpty() = size == 0

        /** Checks if the cache is not empty. */
        fun <K, V> Cache<K, V>.isNotEmpty() = !isEmpty()
    }

    /** All current keys. */
    val keys: Set<K>

    /** All current values. */
    val values: List<V>

    /** All current entries as an immutable snapshot. */
    val entries: Map<K, V>

    /** Number of entries in the cache. */
    val size: Int

    /** Removes all entries from the cache. */
    fun clear()

    /** Returns `true` if the cache contains the given [key]. */
    fun has(key: K): Boolean

    // TODO(scan): V is unbounded, so a stored null is indistinguishable from an absent key here.
    /** Returns the value for the given [key], or `null` if not present. */
    fun get(key: K): V?

    /** Associates the given [value] with the given [key] in the cache. */
    fun put(key: K, value: V)

    /** Removes and returns the value for the given [key], or `null` if not present. */
    fun remove(key: K): V?

    /**
     * Returns the value for the given [key] if present.
     *
     * Otherwise calls [producer] to compute the value, stores it, and returns it.
     *
     * Not atomic: under contention an implementation may run [producer] more than once for one key.
     * [FastCache] does, and a caller that loses the race returns the value already stored instead of
     * its own. Where the producer must run once per key, use [NullableCache].
     */
    fun getOrPut(key: K, producer: () -> V): V
}
