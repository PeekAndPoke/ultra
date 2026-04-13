package io.peekandpoke.ultra.vault

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * Entity cache for deduplicating entity lookups during deserialization.
 */
interface EntityCache {
    /** Clear all entries from the cache. */
    fun clear()

    /** Puts the [value] with the given [id] into the cache. */
    fun <T> put(id: String, value: T): T

    /** Gets or puts the entry for the given [id] using the sync [provider]. */
    fun <T> getOrPut(id: String, provider: () -> T?): T?

    /** Suspend variant of [getOrPut] for async resolution paths (e.g. RefCodec). */
    suspend fun <T> getOrPutAsync(id: String, provider: suspend () -> T?): T?
}

/**
 * Entity Cache that does not do any caching.
 */
object NullEntityCache : EntityCache {

    override fun clear() {
        // noop
    }

    override fun <T> put(id: String, value: T): T = value

    override fun <T> getOrPut(id: String, provider: () -> T?): T? = provider()

    override suspend fun <T> getOrPutAsync(id: String, provider: suspend () -> T?): T? = provider()
}

/**
 * A default implementation for [EntityCache].
 *
 * Uses [ConcurrentHashMap] for thread-safe map access. The sync [getOrPut] uses `synchronized`
 * to prevent double provider calls. The suspend [getOrPutAsync] uses a [Mutex] for the same purpose.
 * Both locking mechanisms are safe because the underlying map is concurrent.
 */
class DefaultEntityCache : EntityCache {

    private val syncLock = Any()
    private val asyncMutex = Mutex()
    private val entries = ConcurrentHashMap<String, Any>()

    override fun clear() {
        entries.clear()
    }

    override fun <T> put(id: String, value: T): T {
        if (value != null) entries[id] = value as Any
        return value
    }

    override fun <T> getOrPut(id: String, provider: () -> T?): T? {
        @Suppress("UNCHECKED_CAST")
        (entries[id] as? T)?.let { return it }

        return synchronized(syncLock) {
            @Suppress("UNCHECKED_CAST")
            (entries[id] as? T)?.let { return@synchronized it }

            val value = provider()
            if (value != null) entries[id] = value as Any
            value
        }
    }

    override suspend fun <T> getOrPutAsync(id: String, provider: suspend () -> T?): T? {
        @Suppress("UNCHECKED_CAST")
        (entries[id] as? T)?.let { return it }

        return asyncMutex.withLock {
            @Suppress("UNCHECKED_CAST")
            (entries[id] as? T)?.let { return@withLock it }

            val value = provider()
            if (value != null) entries[id] = value as Any
            value
        }
    }
}

class SharedRepoClassLookup {

    private val typeLookup = ConcurrentHashMap<KClass<*>, Any>()

    private val nameLookup = ConcurrentHashMap<String, Any>()

    fun getOrPut(type: KClass<*>, defaultValue: () -> KClass<out Repository<*>>?): KClass<out Repository<*>>? =
        lookupOrPut(typeLookup, type, defaultValue)

    fun getOrPut(name: String, defaultValue: () -> KClass<out Repository<*>>?): KClass<out Repository<*>>? =
        lookupOrPut(nameLookup, name, defaultValue)

    private fun <K : Any> lookupOrPut(
        map: ConcurrentHashMap<K, Any>,
        key: K,
        defaultValue: () -> KClass<out Repository<*>>?,
    ): KClass<out Repository<*>>? {
        // Sentinel pattern: ConcurrentHashMap forbids null values, so we store MISSING to cache
        // negative lookups and avoid re-walking the repository list on every miss.
        map[key]?.let { return it.decode() }

        val computed = defaultValue()
        // putIfAbsent so the first caller wins; decode whatever is actually stored.
        return (map.putIfAbsent(key, computed ?: MISSING) ?: (computed ?: MISSING)).decode()
    }

    private fun Any.decode(): KClass<out Repository<*>>? {
        if (this === MISSING) {
            return null
        }
        @Suppress("UNCHECKED_CAST")
        return this as KClass<out Repository<*>>
    }

    private companion object {
        private val MISSING = Any()
    }
}
