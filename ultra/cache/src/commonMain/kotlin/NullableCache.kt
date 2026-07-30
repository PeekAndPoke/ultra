package io.peekandpoke.ultra.cache

import io.peekandpoke.ultra.common.RunSync
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Memoizes lookups that can legitimately resolve to nothing, so a miss is only paid for once.
 *
 * A key that resolved to null stays resolved to null — use [has] to tell "cached as null" from
 * "not cached". This is what separates it from [FastCache], along with the single-flight guarantee
 * below; [FastCache] may run its producer more than once under contention and offers eviction, which
 * this one deliberately does not. Nothing is ever evicted here, so use it where the key space is
 * bounded or the instance is short-lived.
 *
 * [getOrPut] and [getOrPutAsync] each run the provider at most once per key among concurrent callers
 * of that same method. They lock independently, so a blocking and a suspending caller racing for one
 * key can both run their provider.
 */
class NullableCache<K : Any, V : Any> {

    private val lock = Any()

    private val mutex = Mutex()

    private val entries = mutableMapOf<K, Any>()

    /** Number of cached keys, counting those cached as null. */
    val size: Int get() = RunSync(lock) { entries.size }

    /** Drops every entry. */
    fun clear() {
        // TODO(scan): an in-flight getOrPutAsync re-inserts its key after this returns.
        RunSync(lock) { entries.clear() }
    }

    /** True when [key] has a cached result, including when that result is null. */
    fun has(key: K): Boolean = RunSync(lock) { entries.containsKey(key) }

    /** The value cached for [key], or null when it is absent OR cached as null — see [has]. */
    fun get(key: K): V? = read(key)?.decode()

    /** Caches [value] for [key], nulls included, and returns it. */
    fun put(key: K, value: V?): V? {
        RunSync(lock) { entries[key] = value.encode() }

        return value
    }

    /** Returns the cached result for [key], or computes it with [provider] and caches it. */
    fun getOrPut(key: K, provider: () -> V?): V? {
        read(key)?.let { return it.decode() }

        return RunSync(lock) {
            // Branch on PRESENCE, never on the decoded value: MISSING decodes to null, so an elvis
            // here would re-run the provider for every key already cached as a miss.
            entries[key]?.let { return@RunSync it.decode() }

            // TODO(scan): provider runs under the lock - on native that is the process-wide RunSync spin lock.
            provider().also { entries[key] = it.encode() }
        }
    }

    /** Suspending [getOrPut], for providers that need to await, such as a database round trip. */
    suspend fun getOrPutAsync(key: K, provider: suspend () -> V?): V? {
        read(key)?.let { return it.decode() }

        // TODO(scan): the Mutex is not re-entrant and spans all keys - a nested call deadlocks, others serialise.
        return mutex.withLock {
            read(key)?.let { return@withLock it.decode() }

            provider().also { put(key, it) }
        }
    }

    private fun read(key: K): Any? = RunSync(lock) { entries[key] }

    private fun V?.encode(): Any = this ?: MISSING

    @Suppress("UNCHECKED_CAST")
    private fun Any.decode(): V? = if (this === MISSING) null else this as V

    private companion object {
        /** Stands in for a cached null, so an absent key stays distinguishable from a null one. */
        private val MISSING = Any()
    }
}
