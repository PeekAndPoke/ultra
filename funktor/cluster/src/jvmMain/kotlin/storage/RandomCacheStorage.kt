package io.peekandpoke.funktor.cluster.storage

import io.peekandpoke.funktor.cluster.storage.domain.RawCacheData
import io.peekandpoke.funktor.cluster.storage.domain.TypedCacheData
import io.peekandpoke.funktor.inspect.cluster.storage.RawCacheDataModel
import io.peekandpoke.ultra.datetime.Kronos
import io.peekandpoke.ultra.reflection.TypeRef
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.vault.Cursor
import io.peekandpoke.ultra.vault.New
import io.peekandpoke.ultra.vault.Repository
import io.peekandpoke.ultra.vault.Storable
import io.peekandpoke.ultra.vault.Stored
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retry
import kotlin.time.Duration.Companion.seconds

/** Typed key-value cache with TTL and serve-stale-while-refresh semantics. */
class RandomCacheStorage(
    private val kronos: Kronos,
    private val adapter: Adapter,
) {
    companion object {
        inline fun <reified T : Any> category(
            id: String,
            policy: RawCacheDataModel.Policy,
        ): CategoryKey<T> = category(
            id = id,
            policy = policy,
            type = kType(),
        )

        fun <T> category(
            id: String,
            policy: RawCacheDataModel.Policy,
            type: TypeRef<T>,
        ): CategoryKey<T> = CategoryKey(
            category = id,
            policy = policy,
            type = type,
        )
    }

    /** Typed key identifying a cache category, its eviction policy, and the data type. */
    class CategoryKey<T> internal constructor(
        val category: String,
        val policy: RawCacheDataModel.Policy,
        val type: TypeRef<T>,
    ) {

        /** A [CategoryKey] bound to a specific data ID. */
        class Bound<T> internal constructor(val category: CategoryKey<T>, val dataId: String)

        fun bind(dataId: String): Bound<T> = Bound(category = this, dataId = dataId)

        fun <X> bind(storable: Storable<X>): Bound<T> = bind(storable._id)
    }

    /** Backend adapter for persisting raw cache data entries. */
    interface Adapter {

        suspend fun list(search: String, page: Int, epp: Int): Cursor<Stored<RawCacheData>>

        suspend fun getById(id: String): Stored<RawCacheData>?

        suspend fun <T> get(key: CategoryKey<T>, dataId: String): TypedCacheData<T>?

        suspend fun <T> put(key: CategoryKey<T>, dataId: String, data: T): TypedCacheData<T>?

        suspend fun clear()

        object Null : Adapter {

            override suspend fun list(search: String, page: Int, epp: Int): Cursor<Stored<RawCacheData>> {
                return Cursor.empty()
            }

            override suspend fun getById(id: String): Stored<RawCacheData>? {
                return null
            }

            override suspend fun <T> get(key: CategoryKey<T>, dataId: String): TypedCacheData<T>? {
                return null
            }

            override suspend fun <T> put(key: CategoryKey<T>, dataId: String, data: T): TypedCacheData<T>? {
                return null
            }

            override suspend fun clear() {
                // noop
            }
        }

        class Vault(private val inner: Repo, private val kronos: Kronos) : Adapter {

            interface Repo : Repository<RawCacheData> {
                suspend fun find(search: String, page: Int, epp: Int): Cursor<Stored<RawCacheData>>

                suspend fun <T> findOneBy(key: CategoryKey<T>, dataId: String): Stored<RawCacheData>?

                suspend fun <T> findOneWithoutData(key: CategoryKey<T>, dataId: String): Stored<RawCacheData>?

                fun <T> encode(type: TypeRef<T>, raw: RawCacheData): TypedCacheData<T>?
            }

            override suspend fun list(search: String, page: Int, epp: Int): Cursor<Stored<RawCacheData>> {
                return inner.find(search = search, page = page, epp = epp)
            }

            override suspend fun getById(id: String): Stored<RawCacheData>? {
                return inner.findById(id)
            }

            override suspend fun <T> get(key: CategoryKey<T>, dataId: String): TypedCacheData<T>? {
                return try {
                    val raw = inner.findOneBy(key = key, dataId = dataId) ?: return null
                    val rawValue = raw.resolve()

                    if (rawValue.expiresAt < kronos.millisNow() / 1_000) return null

                    inner.encode(key.type, rawValue)
                } catch (e: Throwable) {
                    e.printStackTrace()
                    null
                }
            }

            override suspend fun <T> put(key: CategoryKey<T>, dataId: String, data: T): TypedCacheData<T>? {
                // We retry saving the value 3 times
                val result = flow {
                    val existing = inner.findOneWithoutData(key, dataId)

                    val expiresAt = kronos.instantNow().plus(key.policy.ttl.seconds).toEpochSeconds()

                    val entry = when (existing) {
                        null -> New(
                            RawCacheData(
                                category = key.category,
                                dataId = dataId,
                                data = data,
                                policy = key.policy,
                                expiresAt = expiresAt,
                            )
                        )

                        else -> existing.modify {
                            it.copy(
                                data = data,
                                expiresAt = expiresAt,
                            )
                        }
                    }

                    emit(inner.save(entry))
                }.retry(retries = 3) {
                    delay(20)
                    // return
                    true
                }.firstOrNull()

                if (result == null) return null

                return inner.encode(key.type, result.resolve())
            }

            override suspend fun clear() {
                inner.removeAll()
            }
        }
    }

    suspend fun list(search: String, page: Int, epp: Int): Cursor<Stored<RawCacheData>> {
        return adapter.list(search = search, page = page, epp = epp)
    }

    suspend fun get(id: String): Stored<RawCacheData>? {
        return adapter.getById(id)
    }

    suspend fun <T> put(bound: CategoryKey.Bound<T>, data: T) {
        adapter.put(
            key = bound.category,
            dataId = bound.dataId,
            data = data,
        )
    }

    suspend fun <T> getOrPut(bound: CategoryKey.Bound<T>, produce: suspend () -> T?): T? {

        val result = when (val policy = bound.category.policy) {
            is RawCacheDataModel.Policy.ServeCacheAndRefresh -> {
                handleGetOrPut(policy = policy, key = bound.category, dataId = bound.dataId, produce = produce)
            }
        }

        return result
    }

    suspend fun clear() {
        adapter.clear()
    }

    private suspend fun <T> handleGetOrPut(
        policy: RawCacheDataModel.Policy.ServeCacheAndRefresh,
        key: CategoryKey<T>,
        dataId: String,
        produce: suspend () -> T?,
    ): T? {
        val exising: TypedCacheData<T>? = adapter.get(key = key, dataId = dataId)

        suspend fun produceAndPut(): T? = produce()?.also { data ->
            adapter.put(key = key, dataId = dataId, data = data)
        }

        val result = when (exising) {
            null -> {
                produceAndPut()
            }

            else -> {
                if (exising.updatedAt.plus(policy.refreshAfter.seconds) <= kronos.instantNow()) {
                    // first we save the entry once more to update the "updatedAt" field
                    adapter.put(key = key, dataId = dataId, data = exising.data)
                    // Then we launch a coroutine to refresh in the background
                    @Suppress("DeferredResultUnused")
                    coroutineScope {
                        async(Dispatchers.IO) { produceAndPut() }
                    }
                }

                exising.data
            }
        }

        return result
    }
}
