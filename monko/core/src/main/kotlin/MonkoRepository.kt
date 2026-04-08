package io.peekandpoke.monko

import com.mongodb.client.model.Filters.eq
import io.peekandpoke.monko.lang.MongoExpression
import io.peekandpoke.monko.lang.MongoIterableExpr
import io.peekandpoke.monko.lang.MongoNameExpr
import io.peekandpoke.monko.lang.MongoPrinter
import io.peekandpoke.monko.lang.MongoPropertyPath
import io.peekandpoke.monko.lang.dsl.toFieldPath
import io.peekandpoke.ultra.reflection.TypeRef
import io.peekandpoke.ultra.vault.New
import io.peekandpoke.ultra.vault.RemoveResult
import io.peekandpoke.ultra.vault.Repository
import io.peekandpoke.ultra.vault.Repository.Hooks
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.VaultModels
import io.peekandpoke.ultra.vault.ensureKey
import kotlinx.coroutines.flow.firstOrNull
import org.bson.Document

abstract class MonkoRepository<T : Any>(
    override val name: String,
    override val storedType: TypeRef<T>,
    protected val driver: MonkoDriver,
    private val hooks: Hooks<T> = Hooks.empty(),
) : Repository<T>, MongoExpression<List<T>> {

    override val connection: String by lazy {
        driver.getConnectionName()
    }

    override val repo: MonkoRepository<T> get() = this

    val repoExpr: MongoIterableExpr<T> = MongoIterableExpr("repo", this)

    override fun print(p: MongoPrinter) {
        p.append(
            MongoNameExpr(name = name, type = TypeRef.String)
        )
    }

    /** Override this to define indexes using the DSL. */
    protected open fun MonkoIndexBuilder<T>.buildIndexes() {}

    /** Extracts a dot-notation field path string from a KSP-generated property path. */
    fun <R> field(block: (MongoIterableExpr<T>) -> MongoPropertyPath<R, *>): String {
        return block(repoExpr).toFieldPath()
    }

    override suspend fun validateIndexes(): VaultModels.IndexesInfo {
        val builder = MonkoIndexBuilder(this).apply { buildIndexes() }
        return builder.validate(driver, name)
    }

    override suspend fun ensureIndexes() {
        val builder = MonkoIndexBuilder(this).apply { buildIndexes() }
        val results = builder.create(driver, name)

        results.forEach { result ->
            when (result) {
                is MonkoIndexBuilder.EnsureResult.Ensured -> { /* created */
                }

                is MonkoIndexBuilder.EnsureResult.Kept -> { /* already exists */
                }

                is MonkoIndexBuilder.EnsureResult.ReCreated -> { /* recreated with new fields */
                }

                is MonkoIndexBuilder.EnsureResult.Error -> {
                    driver.log.warning("Failed to create index ${result.name} on $name: ${result.error.message}")
                }
            }
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
            val indexName = index["name"].toString()
            // MongoDB forbids dropping the _id_ index
            if (indexName != "_id_") {
                driver.dropIndex(collection = name, indexName = indexName)
            }
        }

        ensureIndexes()
    }

    override suspend fun findAll(): MonkoCursor<Stored<T>> {
        val result = driver.findStored(collection = name, type = storedType)

        return result
    }

    /** Find documents using the type-safe DSL. The [repoExpr] is passed as lambda parameter for property path access. */
    suspend fun find(query: MonkoDriver.FindQueryBuilder.(r: MongoIterableExpr<T>) -> Unit): MonkoCursor<Stored<T>> {
        return driver.findStored(collection = name, type = storedType) {
            query(repoExpr)
        }
    }

    override suspend fun findById(id: String?): Stored<T>? {
        val key = id?.ensureKey ?: return null
        val result = driver.findStored(collection = name, type = storedType) {
            filter(eq("_id", MonkoDriver.toBsonId(key)))
            limit(1)
        }

        return result.asFlow().firstOrNull()
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

    @Suppress("UNCHECKED_CAST")
    override suspend fun <X : T> save(stored: Stored<X>): Stored<X> {
        val beforeHookApplied = hooks.applyOnBeforeSaveHooks(this, stored) as Stored<X>

        driver.replaceOne(collection = name, stored = beforeHookApplied)

        return hooks.applyOnAfterSaveHooks(this, beforeHookApplied)
    }

    override suspend fun remove(idOrKey: String): RemoveResult {
        return driver.deleteOne(collection = name, idOrKey = idOrKey)
    }

    @JvmName("removeStored")
    suspend fun remove(stored: Stored<T>): RemoveResult {
        val result = remove(stored._key)
        hooks.applyOnAfterDeleteHooks(this, stored)
        return result
    }

    suspend fun modifyById(id: String, block: (T) -> T): Stored<T>? {
        val existing = findById(id) ?: return null
        val modified = existing.modify(block)
        return save(modified)
    }

    @JvmName("insertWithKey")
    suspend fun insert(key: String, value: T): Stored<T> {
        return insert(New(_key = key, value = value))
    }

    override suspend fun removeAll(): RemoveResult {
        return driver.removeAll(collection = name)
    }
}
