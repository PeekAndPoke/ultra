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
     */
    fun getOrPut(key: K, producer: () -> V): V
}
