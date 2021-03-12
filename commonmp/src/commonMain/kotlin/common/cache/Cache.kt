package de.peekandpoke.ultra.common.cache

interface Cache<K, V> {

    fun has(key: K): Boolean

    fun get(key: K): V?

    fun put(key: K, value: V)

    fun getOrPut(key: K, block: () -> V): V
}
