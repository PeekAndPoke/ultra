package de.peekandpoke.ultra.vault

import java.lang.reflect.Type

/**
 * Entity cache
 */
interface EntityCache {
    /**
     * Clear all entries from the cache
     */
    fun clear()

    /**
     * Puts the [value] with the given [id] into the cache
     */
    fun <T> put(id: String, value: T): T

    /**
     * Gets or puts the entry for the given [id] by using the [provider] if the [id] is not yet present.
     */
    fun <T> getOrPut(id: String, provider: () -> T?): T?
}

/**
 * Entity Cache that does not do any caching.
 */
object NullEntityCache : EntityCache {

    /**
     * @see EntityCache.clear
     */
    override fun clear() {
        // noop
    }

    /**
     * @see EntityCache.put
     */
    override fun <T> put(id: String, value: T): T = value

    /**
     * @see EntityCache.getOrPut
     */
    override fun <T> getOrPut(id: String, provider: () -> T?): T? = provider()
}

/**
 * A default implementation for [EntityCache]
 *
 * The cache will hold as many [entries] as it is given.
 * There is no mechanism like TTL or LRU in place here.
 */
class DefaultEntityCache : EntityCache {

    private val lock = Any()
    private val entries = mutableMapOf<String, Any?>()

    /**
     * @see EntityCache.clear
     */
    override fun clear(): Unit = synchronized(lock) {
        entries.clear()
    }

    /**
     * @see EntityCache.put
     */
    override fun <T> put(id: String, value: T): T {
        return synchronized(lock) {
            value.also {
                entries[id] = value
            }
        }
    }

    /**
     * @see EntityCache.getOrPut
     */
    override fun <T> getOrPut(id: String, provider: () -> T?): T? {
        @Suppress("UNCHECKED_CAST")
        val exists = entries[id] as T

        if (exists != null) {
            return exists
        }

        return provider().apply {
            put(id, this)
        }
    }
}

class SharedRepoClassLookup {

    private val typeLookup = mutableMapOf<Type, Class<out Repository<*>>?>()

    private val nameLookup = mutableMapOf<String, Class<out Repository<*>>?>()

    fun getOrPut(type: Type, defaultValue: () -> Class<out Repository<*>>?) =
        typeLookup.getOrPut(type, defaultValue)

    fun getOrPut(name: String, defaultValue: () -> Class<out Repository<*>>?) =
        nameLookup.getOrPut(name, defaultValue)
}
