package de.peekandpoke.ultra.common.cache

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

interface Cache<K, V> {

    companion object {
        val defaultCoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    }

    fun has(key: K): Boolean

    fun get(key: K): V?

    fun put(key: K, value: V)

    fun remove(key: K): V?

    fun getOrPut(key: K, producer: () -> V): V
}
