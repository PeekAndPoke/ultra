package io.peekandpoke.ultra.vault

import io.peekandpoke.ultra.cache.NullableCache
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
 * A default implementation for [EntityCache], backed by a [NullableCache].
 *
 * Ids that resolve to nothing are remembered too, so a reference to a missing document costs one
 * lookup per cache rather than one per resolution.
 *
 * Nothing is ever evicted — the cache is registered per request (see [Ultra_Vault]) and dropped with
 * it, so it is only bounded by how much one request reads.
 */
class DefaultEntityCache : EntityCache {

    private val entries = NullableCache<String, Any>()

    override fun clear() {
        entries.clear()
    }

    override fun <T> put(id: String, value: T): T {
        entries.put(id, value)

        return value
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getOrPut(id: String, provider: () -> T?): T? {
        return entries.getOrPut(id) { provider() } as T?
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> getOrPutAsync(id: String, provider: suspend () -> T?): T? {
        return entries.getOrPutAsync(id) { provider() } as T?
    }
}

/**
 * Caches which [Repository] class stores a given entity type, or answers to a given repository name.
 *
 * Registered as a kontainer singleton while [Database] is dynamic (see [Ultra_Vault]), so a
 * resolution outlives the per-request [Database] that triggered it. Only [KClass] values are cached,
 * never repository instances — that is what makes sharing across requests safe; each caller turns
 * the class back into an instance through its own [Database].
 *
 * Misses are cached as well, through a `MISSING` sentinel ([ConcurrentHashMap] forbids null values),
 * so a type or name that resolved to nothing keeps resolving to nothing for the lifetime of this
 * instance. The two overloads of [getOrPut] use independent maps.
 */
class SharedRepoClassLookup {

    private val typeLookup = NullableCache<KClass<*>, KClass<out Repository<*>>>()

    private val nameLookup = NullableCache<String, KClass<out Repository<*>>>()

    /** Gets or computes the class of the repository storing the entity [type]. */
    fun getOrPut(type: KClass<*>, defaultValue: () -> KClass<out Repository<*>>?): KClass<out Repository<*>>? =
        typeLookup.getOrPut(type, defaultValue)

    /** Gets or computes the class of the repository with the given [Repository.name]. */
    fun getOrPut(name: String, defaultValue: () -> KClass<out Repository<*>>?): KClass<out Repository<*>>? =
        nameLookup.getOrPut(name, defaultValue)
}
