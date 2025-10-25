package de.peekandpoke.monko

import com.mongodb.client.model.Filters.eq
import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.vault.New
import de.peekandpoke.ultra.vault.RemoveResult
import de.peekandpoke.ultra.vault.Repository
import de.peekandpoke.ultra.vault.Repository.Hooks
import de.peekandpoke.ultra.vault.Stored
import de.peekandpoke.ultra.vault.ensureKey

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

    override suspend fun findAll(): MonkoCursor<Stored<T>> {
        val result = driver.findStored(collection = name, type = storedType)

        return result
    }

    suspend fun find(query: MonkoDriver.FindQueryBuilder.() -> Unit): MonkoCursor<Stored<T>> {
        val result = driver.findStored(collection = name, type = storedType, query = query)

        return result
    }

    override suspend fun findById(id: String?): Stored<T>? {
        val result = driver.findStored(collection = name, type = storedType) {
            filter(eq("_id", id?.ensureKey))
            limit(1)
        }

        return result.firstOrNull()
    }

    override suspend fun <X : T> insert(new: New<X>): Stored<X> {
        val result = driver.insertOne(
            collection = name,
            value = new.value,
            key = new._key.takeIf { it.isNotBlank() },
        )

        return result
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
