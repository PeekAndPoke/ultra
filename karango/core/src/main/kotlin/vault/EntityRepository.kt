package de.peekandpoke.karango.vault

import com.arangodb.ArangoDBException
import com.arangodb.entity.CollectionType
import com.arangodb.entity.IndexType
import com.arangodb.model.CollectionCreateOptions
import de.peekandpoke.karango.KarangoCursor
import de.peekandpoke.karango.KarangoQueryException
import de.peekandpoke.karango._id
import de.peekandpoke.karango._key
import de.peekandpoke.karango.aql.AS
import de.peekandpoke.karango.aql.AqlBuilder
import de.peekandpoke.karango.aql.COUNT
import de.peekandpoke.karango.aql.DOCUMENT
import de.peekandpoke.karango.aql.FOR
import de.peekandpoke.karango.aql.IN
import de.peekandpoke.karango.aql.INSERT
import de.peekandpoke.karango.aql.INTO
import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.REMOVE
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.UPSERT_REPLACE
import de.peekandpoke.karango.vault.IndexBuilder.Companion.matchesAny
import de.peekandpoke.karango.vault.IndexBuilder.Companion.matchesNone
import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.vault.BatchInsertRepository
import de.peekandpoke.ultra.vault.Cursor
import de.peekandpoke.ultra.vault.New
import de.peekandpoke.ultra.vault.RemoveResult
import de.peekandpoke.ultra.vault.Repository.Hooks
import de.peekandpoke.ultra.vault.Storable
import de.peekandpoke.ultra.vault.Stored
import de.peekandpoke.ultra.vault.TypedQuery
import de.peekandpoke.ultra.vault.VaultModels
import de.peekandpoke.ultra.vault.ensureKey
import de.peekandpoke.ultra.vault.lang.TerminalExpr
import de.peekandpoke.ultra.vault.lang.expr
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlin.time.Duration.Companion.milliseconds

abstract class EntityRepository<T : Any>(
    name: String,
    storedType: TypeRef<T>,
    driver: KarangoDriver,
    private val hooks: Hooks<T> = Hooks.empty(),
) : BaseRepository<T>(name = name, storedType = storedType, driver = driver),
    BatchInsertRepository<T> {

    override suspend fun ensureRepository() {
        driver.ensureEntityCollection(
            name = name,
            options = modifyCollectionCreationOptions(
                CollectionCreateOptions().type(CollectionType.DOCUMENT)
            ),
        )
    }

    override suspend fun getStats(): VaultModels.RepositoryStats {
        val info = getArangoCollection().info.await()

        val figuresResponse = driver.utils.getRepositoryFigures(name)

        @Suppress("UNCHECKED_CAST")
        val figures = figuresResponse["figures"] as? Map<String, *>

        @Suppress("UNCHECKED_CAST")
        val indexes = figures?.get("indexes") as? Map<String, *>

        val docCount = (figuresResponse["count"] as? Number)?.toLong()
        val docTotalSize = (figures?.get("documentsSize") as? Number)?.toLong()

        val docAvgSize: Long? = run {
            if (docTotalSize == null || docCount == null || docCount <= 0L) {
                null
            } else {
                docTotalSize / docCount
            }
        }

        return VaultModels.RepositoryStats(
            type = info.type.name,
            isSystem = info.isSystem,
            status = info.status.name,
            storage = VaultModels.RepositoryStats.Storage(
                count = docCount,
                totalSize = docTotalSize,
                avgSize = docAvgSize,
            ),
            indexes = VaultModels.RepositoryStats.Indexes(
                count = (indexes?.get("count") as? Number)?.toLong(),
                totalSize = (indexes?.get("size") as? Number)?.toLong(),
            ),
            custom = listOf(
                VaultModels.RepositoryStats.Custom.of(
                    name = "Cache",
                    entries = mapOf(
                        "Enabled" to figuresResponse["cacheEnabled"] as? Boolean,
                        "In Use" to figuresResponse["cacheInUse"] as? Boolean,
                        "Size" to (figures?.get("cacheSize") as? Number)?.toInt(),
                        "Usage" to (figures?.get("cacheUsage") as? Number)?.toInt(),
                    )
                ),
                VaultModels.RepositoryStats.Custom.of(
                    name = "Read / Write",
                    entries = mapOf(
                        "Write Concern" to figuresResponse["writeConcern"] as? String,
                        "WaitFor Sync" to info.waitForSync,
                    )
                )
            ),
        )
    }

    override suspend fun validateIndexes(): VaultModels.IndexesInfo {

        val definitions = IndexBuilder(this).apply { buildIndexes() }.getIndexDefinitions()

        val indexes = getArangoCollection().indexes.await().filter { it.type != IndexType.primary }

        val health = definitions.filter { definition -> definition.matchesAny(indexes) }
        val missing = definitions.filter { definition -> definition.matchesNone(indexes) }
        val excess = indexes.filter { index -> index.matchesNone(definitions) }

        if (missing.isEmpty() && excess.isEmpty()) {
            driver.log.info("[OK] Indexes for repo $name are set up properly")
        } else {
            driver.log.warning("[FAILED] Checking repo $name with ${definitions.size} index definitions")

            missing.forEach {
                val name = "$name::${it.getEffectiveName()}"
                driver.log.warning("[MISSING] Index $name with fields ${it.getFieldPaths()} is missing")
            }

            excess.forEach {
                val name = "$name::${it.name}"
                driver.log.warning("[EXCESS] Index $name with fields ${it.fields} is not defined")
            }
        }

        return VaultModels.IndexesInfo(
            healthyIndexes = health.map { it.getIndexDetails() },
            missingIndexes = missing.map { it.getIndexDetails() },
            excessIndexes = excess.map {
                VaultModels.IndexInfo(
                    name = it.name,
                    type = it.type.name,
                    fields = it.fields.toList(),
                )
            }
        )
    }

    override suspend fun ensureIndexes() {
        var attempts = 0
        val maxAttempts = 3

        while (attempts++ < maxAttempts) {
            val results = IndexBuilder(this).apply { buildIndexes() }.create()

            results.forEach { r ->
                when (r) {
                    is IndexBuilder.EnsureResult.Ensured -> {
                        driver.log.info(
                            "[OK] Ensured index '${r.qualifiedName()}' (${r.idx.type}) on fields ${r.fields}"
                        )
                        return
                    }

                    is IndexBuilder.EnsureResult.Kept -> {
                        driver.log.info(
                            "[OK] Kept index '${r.qualifiedName()}' (${r.idx.type}) on fields ${r.fields}"
                        )
                        return
                    }

                    is IndexBuilder.EnsureResult.ReCreated -> {
                        driver.log.info(
                            "[OK] Re-Created index '${r.qualifiedName()}' (${r.idx.type}) on fields ${r.fields}"
                        )
                        return
                    }

                    is IndexBuilder.EnsureResult.Error -> {
                        if (attempts < maxAttempts) {
                            driver.log.warning(
                                "[WARNING] Will retry to crate index '${r.qualifiedName()}' on fields ${r.fields}, " +
                                        "ErrNum: ${r.error.errorNum} ResponseCode: ${r.error.responseCode} " +
                                        "Message: ${r.error.errorMessage}",
                            )

                            // Reload all indexes
                            getArangoCollection().indexes.await()
                            // And wait a bit
                            delay(1000.milliseconds)
                        } else {
                            driver.log.error(
                                "[ERROR] Creating index '${r.qualifiedName()}' on fields ${r.fields} failed, " +
                                        "ErrNum: ${r.error.errorNum} ResponseCode: ${r.error.responseCode} " +
                                        "Message: ${r.error.errorMessage}",
                            )
                        }
                    }
                }
            }
        }
    }

    override suspend fun recreateIndexes() {
        // Delete all indexes
        val coll = getArangoCollection()

        val indexes = coll.indexes.await()
            .filter { it.type != IndexType.primary }

        indexes.forEach {
            val name = "$name::${it.name}"
            try {
                val deleted: String = coll.deleteIndex(it.id).await()
                driver.log.info("[OK] Deleted index '$deleted'")
            } catch (e: ArangoDBException) {
                driver.log.error("[Error] Deleting index '$name' failed: ${e.message}")
            }
        }

        // Create the indexes
        ensureIndexes()
    }

    /**
     * Override this function to ensure indexes on the repo
     */
    protected open fun IndexBuilder<T>.buildIndexes() {}

    // //  Common Queries  ////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Get the number of entries in the collection
     */
    suspend fun count(): Long = queryFirst { RETURN(COUNT(repo)) }!!.toLong()

    /**
     * Inserts the given object with the given key into the database and returns the saved version
     */
    override suspend fun <X : T> insert(key: String?, new: X): Stored<X> = super.insert(key = key?.ensureKey, new = new)

    /**
     * Inserts the given object into the database and returns the saved version
     */
    override suspend fun <X : T> insert(new: New<X>): Stored<X> {
        // Apply before save hooks
        val beforeHookApplied = hooks.applyOnBeforeSaveHooks(this, new)

        // Save
        @Suppress("UNCHECKED_CAST")
        val storedInDb = findFirst {
            INSERT(beforeHookApplied) INTO repo
        }!! as Stored<X>

        // Apply after save hooks
        return hooks.applyOnAfterSaveHooks(this, storedInDb)
    }

    /**
     * Updates the given obj in the database and returns the saved version
     */
    override suspend fun <X : T> save(stored: Stored<X>): Stored<X> {
        // Apply before save hooks
        val beforeHookApplied = hooks.applyOnBeforeSaveHooks(this, stored)

        // Save
        @Suppress("UNCHECKED_CAST")
        val storedInDb = findFirst {
            UPSERT_REPLACE(beforeHookApplied) INTO repo
        }!! as Stored<X>

        // Apply after save hooks
        return hooks.applyOnAfterSaveHooks(this, storedInDb)
    }

    /**
     * Removes the given entity
     */
    override suspend fun <X : T> remove(entity: Stored<X>): RemoveResult {
        return remove(entity._id).also {
            hooks.applyOnAfterDeleteHooks(this, entity)
        }
    }

    /**
     * Removes multiple entities
     */
    suspend fun <X : T> remove(entities: Iterable<Stored<X>>): RemoveResult {
        return query {
            FOR(repo) {
                FILTER(it._id IN entities.map { e -> e._id })

                REMOVE(it).IN(repo) {
                    ignoreErrors = true
                }
            }
        }.let {
            RemoveResult(count = it.count, query = it.query)
        }
    }

    /**
     * Remove the document with the given id or key
     */
    override suspend fun remove(idOrKey: String): RemoveResult = try {
        query {
            REMOVE(idOrKey.ensureKey) IN repo
        }.let {
            RemoveResult(count = it.count, query = it.query)
        }
    } catch (e: KarangoQueryException) {
        RemoveResult(count = 0, query = e.query)
    }

    /**
     * Remove all entries from the collection
     */
    override suspend fun removeAll() = query {
        FOR(repo) { c ->
            REMOVE(c._key) IN repo
        }
    }.let {
        RemoveResult(count = it.count, query = it.query)
    }

    /**
     * Finds all return them as [Stored] entities
     */
    override suspend fun findAll(): Cursor<Stored<T>> = find {
        queryOptions {
            it.count(true).fullCount(true)
        }

        FOR(repo) {
            RETURN(it)
        }
    }

    suspend inline fun <reified C> findPartial(
        crossinline builder: AqlBuilder.() -> TerminalExpr<out C>,
    ): KarangoCursor<Stored<C>> = query {
        builder().AS(kType<C>().wrapWith<Stored<C>>().list)
    }

    /**
     * Returns all results as [Stored] entities
     */
    suspend fun <X : T> find(builder: AqlBuilder.() -> TerminalExpr<out X>): KarangoCursor<Stored<X>> = query {
        builder().wrapAsStored()
    }

    /**
     * Convenience function that returns [Stored] entities and converts to Cursor to a List right away.
     */
    suspend fun <X : T> findList(builder: AqlBuilder.() -> TerminalExpr<out X>): List<Stored<X>> =
        find(builder).toList()

    /**
     * Returns the first result as [Stored] entity
     */
    suspend fun <X : T> findFirst(builder: AqlBuilder.() -> TerminalExpr<out X>): Stored<X>? = queryFirst {
        builder().wrapAsStored()
    }

    /**
     * Find one by id or key and return it as [Stored] entity
     */
    override suspend fun findById(id: String?): Stored<T>? {

        return when {
            id.isNullOrBlank() -> null

            else -> queryFirst {
                RETURN(DOCUMENT(repo, id)).wrapAsStored()
            }
        }
    }

    /**
     * Find by multiple [ids] or keys and return them as [Stored] entities
     */
    suspend fun findByIds(vararg ids: String): Cursor<Stored<T>> = findByIds(ids.toList())

    /**
     * Find by multiple [ids] or keys and returns them as [Stored] entities
     */
    suspend fun findByIds(ids: Collection<String>): KarangoCursor<Stored<T>> = query {
        FOR(DOCUMENT(repo, ids.toList())) { d ->
            RETURN(d)
        }.wrapAsStored()
    }

    /**
     * Find an entity by id and [modify] its data part
     */
    suspend fun modifyById(id: String, modify: suspend (T) -> T): Stored<T>? {

        return when (val loaded = findById(id)) {
            null -> null

            else -> save(loaded, modify)
        }
    }

    /**
     * Find an entity by id and [modify] its data part when the given [condition] is fulfilled
     */
    suspend fun modifyByIdWhen(id: String, condition: (Stored<T>) -> Boolean, modify: suspend (T) -> T): Stored<T>? {

        return when (val loaded = findById(id)) {
            null -> null

            else -> when (condition(loaded)) {
                true -> save(loaded, modify)

                else -> null
            }
        }
    }

    /**
     * Performs the query
     */
    suspend fun <X> query(query: TypedQuery<X>): KarangoCursor<X> = driver.query(query)

    /**
     * Performs a query and returns a cursor of results
     */
    suspend fun <X> query(builder: AqlBuilder.() -> TerminalExpr<X>): KarangoCursor<X> = driver.query(builder)

    /**
     * Performs a query and return a list of results
     *
     * This means that the internal [Cursor] is completely iterated and converted into a [List]
     */
    suspend fun <X> queryList(builder: AqlBuilder.() -> TerminalExpr<X>): List<X> = query(builder).toList()

    /**
     * Performs a query and returns the first result or null
     */
    suspend fun <X> queryFirst(builder: AqlBuilder.() -> TerminalExpr<X>): X? = query(builder).firstOrNull()

    // //  BatchInsert  //////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Inserts multiple elements with a single query.
     *
     * see [BatchInsertRepository.batchInsert]
     */
    override suspend fun <X : T> batchInsert(values: List<New<X>>): List<Stored<X>> {

        val mapped: List<Storable<X>> = values.map {
            hooks.applyOnBeforeSaveHooks(this, it)
        }

        // NOTICE: We need to give the TypeRef explicitly at the moment
        //         Otherwise the inner Type-Variable T (List<Storable<T>>) is not resolved, resulting in an error.
        val type = getType().wrapWith<Storable<X>>().list

        val result: KarangoCursor<Stored<X>> = query {

            val vals = LET("values", mapped.expr(type))

            FOR(vals) { item ->
                INSERT(item) INTO repo
            }.wrapAsStored()
        }

        return result.map {
            hooks.applyOnAfterSaveHooks(this, it)
        }
    }

    // //  HELPERS  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Cast a terminal expr to a [Stored] entity.
     *
     * This is used to tell the deserialization, that we actually want [Stored] entities to be returned
     */
    private fun <U> TerminalExpr<out U>.wrapAsStored(): TerminalExpr<Stored<U>> =
        AS(repo.storedType.wrapWith<Stored<U>>().list)
}
