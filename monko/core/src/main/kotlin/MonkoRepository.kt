package de.peekandpoke.monko

import com.mongodb.client.model.Filters.eq
import de.peekandpoke.monko.lang.printQuery
import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.vault.New
import de.peekandpoke.ultra.vault.RemoveResult
import de.peekandpoke.ultra.vault.Repository
import de.peekandpoke.ultra.vault.Repository.Hooks
import de.peekandpoke.ultra.vault.Stored
import de.peekandpoke.ultra.vault.VaultModels
import de.peekandpoke.ultra.vault.ensureKey
import de.peekandpoke.ultra.vault.lang.Expression
import de.peekandpoke.ultra.vault.lang.PropertyPath
import org.bson.Document

abstract class MonkoRepository<T : Any>(
    override val name: String,
    override val storedType: TypeRef<T>,
    protected val driver: MonkoDriver,
    private val hooks: Hooks<T> = Hooks.empty(),
) : Repository<T> {

    override val connection: String by lazy {
        driver.getConnectionName()
    }

    fun <R> field(block: (Expression<T>) -> PropertyPath<R, *>): String {
        val path = block(repo.asIterableExpr())

        val dropped = path.dropRoot() ?: return ""

        return dropped.getAsList().joinToString(".") {
            it.printQuery().replace("`", "")
        }
    }

    override suspend fun getStats(): VaultModels.RepositoryStats {
        val coll = driver.database.getCollection<Document>(name)
        val stats = driver.database.runCommand(Document("collStats", name))

        val docCount = stats.getNumberOrNull("count")?.toLong()
        val docAvgSize = stats.getNumberOrNull("avgObjSize")?.toLong()
        val docTotalSize = run {
            if (docCount == null || docAvgSize == null) {
                null
            } else {
                docCount * docAvgSize
            }
        }

        return VaultModels.RepositoryStats(
            type = null,
            isSystem = null,
            status = null,
            storage = VaultModels.RepositoryStats.Storage(
                count = docCount,
                totalSize = docTotalSize,
                avgSize = docAvgSize,
            ),
            indexes = VaultModels.RepositoryStats.Indexes(
                count = stats.getNumberOrNull("nindexes")?.toLong(),
                totalSize = stats.getNumberOrNull("totalIndexSize")?.toLong(),
            ),
            custom = listOf(
                VaultModels.RepositoryStats.Custom.of(
                    name = "Read / Write",
                    entries = mapOf(
                        "Read Concern" to coll.readConcern.toString(),
                        "Read Preference" to coll.readPreference.name,
                        "Write Concern" to coll.writeConcern.toString(),
                    )
                )
            ),
        )
    }

    override suspend fun recreateIndexes() {
        val indexes = driver.listIndexes(collection = name)

        indexes.forEach { index ->
            driver.dropIndex(collection = name, indexName = index["name"].toString())
        }

        ensureIndexes()
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
        // Apply before save hooks
        val beforeHookApplied = hooks.applyOnBeforeSaveHooks(this, new)

        val storedInDb = driver.insertOne(
            collection = name,
            value = beforeHookApplied.value,
            key = new._key.takeIf { it.isNotBlank() },
        )

        // Apply after save hooks
        return hooks.applyOnAfterSaveHooks(this, storedInDb)
    }

    override suspend fun <X : T> save(stored: Stored<X>): Stored<X> {
        TODO("Not yet implemented")
    }

    override suspend fun remove(idOrKey: String): RemoveResult {
        TODO("Not yet implemented")
    }

    override suspend fun removeAll(): RemoveResult {
        return driver.removeAll(collection = name)
    }
}
