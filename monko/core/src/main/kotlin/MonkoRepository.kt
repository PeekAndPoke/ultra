package de.peekandpoke.monko

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.vault.Cursor
import de.peekandpoke.ultra.vault.New
import de.peekandpoke.ultra.vault.RemoveResult
import de.peekandpoke.ultra.vault.Repository
import de.peekandpoke.ultra.vault.Repository.Hooks
import de.peekandpoke.ultra.vault.Stored
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList

abstract class MonkoRepository<T : Any>(
    override val name: String,
    override val storedType: TypeRef<T>,
    private val driver: MonkoDriver,
    private val hooks: Hooks<T> = Hooks.empty(),
) : Repository<T> {

    override val connection: String by lazy {
        val version = driver.getDatabaseVersion()

        "MongoDB(${version})::${driver.database.name}"
    }

    override suspend fun findAll(): Cursor<Stored<T>> {
        val result = driver.find(collection = name)

        val items = result
            .map {
                val key = "" + it["_id"]
                Stored(
                    _id = "$name/$key",
                    _key = key,
                    _rev = "",
                    value = driver.codec.awake(storedType, it) as T,
                )
            }
            .toList()

        return Cursor.of(items)
    }

    override suspend fun findById(id: String?): Stored<T>? {
        TODO("Not yet implemented")
    }

    override suspend fun <X : T> insert(new: New<X>): Stored<X> {
        val coll = driver.database.getCollection<Map<String, Any?>>(name)

        @Suppress("UNCHECKED_CAST")
        val data = driver.codec.slumber(new) as Map<String, Any?>

        val result = coll.insertOne(data)

        return result.insertedId?.let { key ->
            Stored(
                _id = "$name/$key",
                _key = key.toString(),
                _rev = "",
                value = new.value,
            )
        } ?: throw IllegalStateException("Insert failed")

    }

    override suspend fun <X : T> save(stored: Stored<X>): Stored<X> {
        TODO("Not yet implemented")
    }

    override suspend fun remove(idOrKey: String): RemoveResult {
        TODO("Not yet implemented")
    }

    override suspend fun removeAll(): RemoveResult {
        TODO("Not yet implemented")
    }
}
