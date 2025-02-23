package de.peekandpoke.ultra.common.cache

import de.peekandpoke.ultra.common.RunSync
import de.peekandpoke.ultra.common.datetime.MpInstant
import kotlin.time.Duration.Companion.milliseconds

// TODO: test me
class TtlCache<K, V>(private val ttlMs: Long) : Cache<K, V> {

    private inner class Entry<V>(
        val data: V,
    ) {
        val expires: MpInstant = MpInstant.now().plus(ttlMs.milliseconds)

        fun check(): Entry<V>? {

            return when (MpInstant.now() > expires) {
                true -> null
                else -> this
            }
        }
    }

    private val map = mutableMapOf<K, Entry<V>>()

    override fun get(key: K): V? {
        return sync {
            map[key]?.check()?.data
        }
    }

    override fun getOrPut(key: K, block: () -> V): V {
        return sync {
            when (val found = get(key)) {
                null -> Entry(block()).also { map[key] = it }.data

                else -> found
            }
        }
    }

    override fun has(key: K): Boolean {
        return get(key) != null
    }

    override fun put(key: K, value: V) {
        sync {
            Entry(value).also { map[key] = it }.data
        }
    }

    private fun <T> sync(block: () -> T): T {
        return RunSync(map, block)
    }
}
