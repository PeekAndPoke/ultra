package io.peekandpoke.funktor.cluster.storage

import io.peekandpoke.funktor.cluster.storage.domain.RawRandomData
import io.peekandpoke.funktor.cluster.storage.domain.TypedRandomData
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.reflection.TypeRef
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.slumber.AwakerException
import io.peekandpoke.ultra.vault.Cursor
import io.peekandpoke.ultra.vault.Repository
import io.peekandpoke.ultra.vault.Stored

/** Typed key-value storage for persistent random data, organized by category. */
class RandomDataStorage(
    private val adapter: Adapter,
) {
    companion object {
        inline fun <reified T : Any> category(id: String): CategoryKey<T> = category(id = id, type = kType())

        fun <T> category(id: String, type: TypeRef<T>): CategoryKey<T> = CategoryKey(category = id, type = type)
    }

    /** Typed key identifying a storage category and the data type it holds. */
    class CategoryKey<T> internal constructor(val category: String, val type: TypeRef<T>) {

        /** A [CategoryKey] bound to a specific data ID. */
        class Bound<T> internal constructor(val category: CategoryKey<T>, val dataId: String)

        fun bind(dataId: String): Bound<T> = Bound(category = this, dataId = dataId)
    }

    /** Backend adapter for persisting raw random data entries. */
    interface Adapter {

        suspend fun list(search: String, page: Int, epp: Int): Cursor<Stored<RawRandomData>>

        suspend fun getById(id: String): Stored<RawRandomData>?

        suspend fun <T> load(category: CategoryKey<T>, dataId: String): TypedRandomData<T>?

        suspend fun <T> save(category: CategoryKey<T>, dataId: String, data: T): TypedRandomData<T>

        suspend fun clear()

        object Null : Adapter {
            override suspend fun list(search: String, page: Int, epp: Int): Cursor<Stored<RawRandomData>> {
                return Cursor.empty()
            }

            override suspend fun getById(id: String): Stored<RawRandomData>? {
                return null
            }

            override suspend fun <T> load(category: CategoryKey<T>, dataId: String): TypedRandomData<T>? {
                return null
            }

            override suspend fun <T> save(category: CategoryKey<T>, dataId: String, data: T): TypedRandomData<T> {
                return TypedRandomData(
                    category = category.category,
                    dataId = dataId,
                    data = data,
                    createdAt = MpInstant.Epoch,
                    updatedAt = MpInstant.Epoch,
                )
            }

            override suspend fun clear() {
                // noop
            }
        }

        class Vault(private val inner: Repo) : Adapter {

            interface Repo : Repository<RawRandomData> {
                suspend fun find(search: String, page: Int, epp: Int): Cursor<Stored<RawRandomData>>

                suspend fun findOneBy(category: String, dataId: String): Stored<RawRandomData>?

                suspend fun <T> encode(type: TypeRef<T>, raw: Stored<RawRandomData>): T?
            }

            override suspend fun list(search: String, page: Int, epp: Int): Cursor<Stored<RawRandomData>> {
                return inner.find(search = search, page = page, epp = epp)
            }

            override suspend fun getById(id: String): Stored<RawRandomData>? {
                return inner.findById(id)
            }

            override suspend fun <T> load(category: CategoryKey<T>, dataId: String): TypedRandomData<T>? {
                return try {
                    val raw = inner.findOneBy(category = category.category, dataId = dataId) ?: return null
                    val rawValue = raw.resolve()
                    val encoded = inner.encode(category.type, raw) ?: return null

                    TypedRandomData(
                        category = rawValue.category,
                        dataId = rawValue.dataId,
                        data = encoded,
                        createdAt = rawValue.createdAt,
                        updatedAt = rawValue.updatedAt,
                    )
                } catch (e: AwakerException) {
                    e.printStackTrace()
                    null
                }
            }

            override suspend fun <T> save(category: CategoryKey<T>, dataId: String, data: T): TypedRandomData<T> {

                val saved = when (val found = inner.findOneBy(category.category, dataId)) {
                    null -> inner.insert(
                        new = RawRandomData(
                            category = category.category,
                            dataId = dataId,
                            data = data,
                        )
                    )

                    else -> inner.save(found) { it.copy(data = data) }
                }

                val savedValue = saved.resolve()

                return TypedRandomData(
                    category = savedValue.category,
                    dataId = savedValue.dataId,
                    data = data,
                    createdAt = savedValue.createdAt,
                    updatedAt = savedValue.updatedAt,
                )
            }

            override suspend fun clear() {
                inner.removeAll()
            }
        }
    }

    suspend fun list(search: String, page: Int, epp: Int): Cursor<Stored<RawRandomData>> {
        return adapter.list(search = search, page = page, epp = epp)
    }

    suspend fun get(id: String): Stored<RawRandomData>? {
        return adapter.getById(id)
    }

    suspend fun <T> load(category: CategoryKey<T>, dataId: String): TypedRandomData<T>? {
        return adapter.load(category = category, dataId = dataId)
    }

    suspend fun <T> load(bound: CategoryKey.Bound<T>): TypedRandomData<T>? {
        return load(category = bound.category, dataId = bound.dataId)
    }

    suspend fun <T> save(category: CategoryKey<T>, dataId: String, data: T): TypedRandomData<T> {
        return adapter.save(category = category, dataId = dataId, data = data)
    }

    suspend fun <T> save(bound: CategoryKey.Bound<T>, data: T): TypedRandomData<T> {
        return save(category = bound.category, dataId = bound.dataId, data)
    }

    suspend fun clear() {
        adapter.clear()
    }
}
