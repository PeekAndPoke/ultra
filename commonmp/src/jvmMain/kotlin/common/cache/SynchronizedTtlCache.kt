package de.peekandpoke.ultra.common.cache

import java.time.Clock
import java.time.Instant

class SynchronizedTtlCache<K, V>(private val ttlMs: Long) : Cache<K, V> {

    private val clock: Clock = Clock.systemUTC()

    private object Lock

    private data class Entry<V>(
        val value: V,
        val createdAt: Instant,
        val expiresAt: Instant,
    )

    private val entries = mutableMapOf<K, Entry<V>>()

    override fun has(key: K): Boolean = synchronized(Lock) {
        return when (entries.contains(key)) {
            false -> false

            else -> {
                val entry = entries[key]

                // Did the entry expire
                when (entry != null && entry.hasNotExpired()) {
                    true -> true
                    else -> false.also { entries.remove(key) }
                }
            }
        }
    }

    override fun get(key: K): V? = synchronized(Lock) {
        return when (entries.contains(key)) {
            false -> null

            else -> {
                val entry = entries[key]

                // Did the entry expire
                when (entry != null && entry.hasNotExpired()) {
                    true -> entry.value
                    else -> null.also { entries.remove(key) }
                }
            }
        }
    }

    override fun put(key: K, value: V) {
        entries[key] = createEntry(value = value)
    }

    override fun getOrPut(key: K, block: () -> V): V = synchronized(Lock) {

        return when (entries.contains(key)) {
            false -> createEntry(value = block()).also { entries[key] = it }.value

            else -> {
                val entry = entries[key]

                // Did the entry expire
                when (entry != null && entry.hasNotExpired()) {
                    true -> entry.value
                    else -> block().also { put(key, it) }
                }
            }
        }
    }

    private fun createEntry(value: V): Entry<V> = Entry(
        value = value,
        createdAt = clock.instant(),
        expiresAt = clock.instant().plusMillis(ttlMs),
    )

    private fun Entry<V>.hasNotExpired(): Boolean {
        return expiresAt > clock.instant()
    }
}
