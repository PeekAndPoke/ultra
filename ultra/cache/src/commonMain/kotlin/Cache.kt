package de.peekandpoke.ultra.cache

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

interface Cache<K, V> {

    companion object {
        val defaultCoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

        /** Checks if the cache is empty */
        fun <K, V> Cache<K, V>.isEmpty() = size == 0

        /** Checks if the cache is not empty */
        fun <K, V> Cache<K, V>.isNotEmpty() = !isEmpty()
    }

    /** All current keys */
    val keys: Set<K>

    /** All current values */
    val values: List<V>

    /** All current entries */
    val entries: Map<K, V>

    /** Number of entries in the cache */
    val size: Int

    /** Clears the cache */
    fun clear()

    fun has(key: K): Boolean

    fun get(key: K): V?

    fun put(key: K, value: V)

    fun remove(key: K): V?

    fun getOrPut(key: K, producer: () -> V): V
}
