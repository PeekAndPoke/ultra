package de.peekandpoke.ktorfx.cluster.storage

import de.peekandpoke.ktorfx.cluster.storage.domain.RawRandomData
import de.peekandpoke.ktorfx.cluster.storage.domain.TypedRandomData
import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.slumber.AwakerException
import de.peekandpoke.ultra.vault.Cursor
import de.peekandpoke.ultra.vault.Repository
import de.peekandpoke.ultra.vault.Stored

class RandomDataStorage(
    private val adapter: Adapter,
) {
    companion object {
        inline fun <reified T : Any> category(id: String): CategoryKey<T> = category(id = id, type = kType())

        fun <T> category(id: String, type: TypeRef<T>): CategoryKey<T> = CategoryKey(category = id, type = type)
    }

    class CategoryKey<T> internal constructor(val category: String, val type: TypeRef<T>) {

        class Bound<T> internal constructor(val category: CategoryKey<T>, val dataId: String)

        fun bind(dataId: String): Bound<T> = Bound(category = this, dataId = dataId)
    }

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

                fun <T> encode(type: TypeRef<T>, raw: Stored<RawRandomData>): T?
            }

            override suspend fun list(search: String, page: Int, epp: Int): Cursor<Stored<RawRandomData>> {
                return inner.find(search = search, page = page, epp = epp)
            }

            override suspend fun getById(id: String): Stored<RawRandomData>? {
                return inner.findById(id)
            }

            override suspend fun <T> load(category: CategoryKey<T>, dataId: String): TypedRandomData<T>? {
                return try {
                    inner.findOneBy(category = category.category, dataId = dataId)?.let { raw ->
                        inner.encode(category.type, raw)?.let { encoded ->
                            TypedRandomData(
                                category = raw.value.category,
                                dataId = raw.value.dataId,
                                data = encoded,
                                createdAt = raw.value.createdAt,
                                updatedAt = raw.value.updatedAt,
                            )
                        }
                    }
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

                return TypedRandomData(
                    category = saved.value.category,
                    dataId = saved.value.dataId,
                    data = data,
                    createdAt = saved.value.createdAt,
                    updatedAt = saved.value.updatedAt,
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
