package de.peekandpoke.karango.vault

import com.arangodb.ArangoCollectionAsync
import com.arangodb.ArangoDBException
import com.arangodb.entity.IndexEntity
import com.arangodb.model.PersistentIndexOptions
import com.arangodb.model.TtlIndexOptions
import de.peekandpoke.karango.aql.printQuery
import de.peekandpoke.ultra.vault.Repository
import de.peekandpoke.ultra.vault.VaultModels
import de.peekandpoke.ultra.vault.lang.IterableExpr
import de.peekandpoke.ultra.vault.lang.PropertyPath
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlin.time.Duration.Companion.milliseconds

/**
 * Index builder for [BaseRepository]s
 */
class IndexBuilder<T : Any>(private val repo: BaseRepository<T>) {

    companion object {
        fun <T : Any> BaseBuilder<T>.matchesAny(indexes: Collection<IndexEntity>): Boolean {
            return indexes.toList().any { matches(it) }
        }

        fun <T : Any> BaseBuilder<T>.matchesNone(indexes: Collection<IndexEntity>): Boolean {
            return !matchesAny(indexes)
        }

        fun <T : Any> IndexEntity.matchesAny(definitions: Collection<BaseBuilder<T>>): Boolean {
            return definitions.toList().any { it.matches(this) }
        }

        fun <T : Any> IndexEntity.matchesNone(definitions: Collection<BaseBuilder<T>>): Boolean {
            return !matchesAny(definitions)
        }

        fun <T : Any> BaseBuilder<T>.matches(index: IndexEntity): Boolean {

//            println("--- ${getEffectiveName()} VS ${index.name} -------------------------------------------------------------------")
//            println(this.getFieldPaths().toString())
//            println(index.fields.toList().toString())

            return this.getEffectiveName() == index.name &&
                    this.getFieldPaths() == index.fields.toList()
        }
    }

    sealed class EnsureResult {
        abstract val repo: Repository<*>
        abstract val name: String
        abstract val fields: List<String>

        fun qualifiedName() = "${repo.name}::$name"

        data class Ensured(
            override val repo: Repository<*>,
            val idx: IndexEntity,
        ) : EnsureResult() {
            override val name: String = idx.name
            override val fields: List<String> = idx.fields.toList()
        }

        data class Kept(
            override val repo: Repository<*>,
            val idx: IndexEntity,
        ) : EnsureResult() {
            override val name: String = idx.name
            override val fields: List<String> = idx.fields.toList()
        }

        data class ReCreated(
            override val repo: Repository<*>,
            val idx: IndexEntity,
            val old: IndexEntity,
        ) : EnsureResult() {
            override val name: String = idx.name
            override val fields: List<String> = idx.fields.toList()
        }

        data class Error(
            override val repo: BaseRepository<*>,
            override val name: String,
            override val fields: List<String>,
            val error: ArangoDBException,
        ) : EnsureResult()
    }

    abstract class BaseBuilder<T : Any>(private val repo: BaseRepository<T>) {

        /**
         * The name given to the index. If not set a name will be created
         */
        private var name: String? = null

        /**
         * The fields included in the index.
         */
        private val _fields = mutableListOf<PropertyPath<*, *>>()

        /**
         * Get the effective index name
         */
        abstract fun getEffectiveName(): String

        /**
         * Get details of the built index
         */
        abstract fun getIndexDetails(): VaultModels.IndexInfo

        /**
         * Function that created the index
         */
        internal abstract suspend fun create(): EnsureResult

        /**
         * Sets a custom name for the index
         */
        fun name(name: String) {
            this.name = name
        }

        /**
         * Internal helper for creating indexes
         */
        protected suspend fun createHelper(
            effectiveName: String,
            effectiveFields: List<String>,
            creator: suspend ArangoCollectionAsync.() -> IndexEntity,
        ): EnsureResult {
            val coll = repo.getArangoCollection()

            return try {
                // We try to set the index
                EnsureResult.Ensured(repo = repo, idx = coll.creator())
            } catch (e: ArangoDBException) {
                // When it does not work we create an intermediate error result
                val error = EnsureResult.Error(repo = repo, name = effectiveName, fields = effectiveFields, error = e)

                // Now we try to recover
                when (e.errorNum) {
                    // Is this name or id conflict?
                    1005 -> {
                        val allIndexes = coll.indexes.await()

                        // Yes so we try to remove the index and re-create it
                        when (val existing = allIndexes.firstOrNull { it.name == effectiveName }) {
                            // It does not exist ... we return the error
                            null -> error

                            // Ok lets try to recreate the index
                            else -> {
                                // Does is have the same fields in the same order ?
                                if (existing.fields.toList() == effectiveFields) {
                                    // YES. so we can simply keep the index
                                    EnsureResult.Kept(repo = repo, idx = existing)
                                } else {
                                    // No. Drop the clashing index and try to re-create the index
                                    @Suppress("UNUSED_VARIABLE")
                                    val deleted: String = coll.deleteIndex(existing.id).await()

                                    // Wait for the index actually being deleted on the cluster
                                    delay(1000.milliseconds)

                                    try {
                                        // Recreate it
                                        EnsureResult.ReCreated(repo = repo, idx = coll.creator(), old = existing)
                                    } catch (e: ArangoDBException) {
                                        // Still did not work :(
                                        EnsureResult.Error(
                                            repo = repo,
                                            name = effectiveName,
                                            fields = effectiveFields,
                                            error = e
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Another unknown error
                    else -> error
                }
            }
        }

        /**
         * Add a field to the index
         */
        fun field(block: IterableExpr<T>.() -> PropertyPath<*, *>) {
            _fields.add(
                repo.repoExpr.block()
            )
        }

        /**
         * Returns printed paths of all index fields
         */
        fun getFieldPaths() = _fields.mapNotNull { path -> path.dropRoot() }
            .map { it.printQuery() }
            .map { it.replace("`", "") }

        protected fun getEffectiveName(prefix: String): String {
            val unclean = name ?: "$prefix${getFieldPaths().joinToString("-")}"

            return unclean
                .replace("`", "")
                .replace(".", "_")
                .replace("[*]", "")
                .trim()
        }
    }

    /**
     * Builder for persistent indexes
     */
    class PersistentIndexBuilder<T : Any> internal constructor(repo: BaseRepository<T>) : BaseBuilder<T>(repo) {

        private var options = PersistentIndexOptions()

        override fun getEffectiveName(): String = getEffectiveName("persistent-")

        override fun getIndexDetails() = VaultModels.IndexInfo(
            name = getEffectiveName(),
            type = "persistent",
            fields = getFieldPaths(),
        )

        override suspend fun create(): EnsureResult {

            val name = getEffectiveName()
            val fields = getFieldPaths()

            options.name(name)

            return createHelper(name, fields) {
                ensurePersistentIndex(fields, options).await()
            }
        }

        /**
         * Use this to set index options.
         *
         * See [PersistentIndexOptions]
         */
        fun options(block: PersistentIndexOptions.() -> Unit) {
            options.block()
        }
    }

    /**
     * Builder for persistent indexes
     */
    class TtlIndexBuilder<T : Any> internal constructor(repo: BaseRepository<T>) : BaseBuilder<T>(repo) {

        private var options = TtlIndexOptions()

        override fun getEffectiveName(): String = getEffectiveName("ttl-")

        override fun getIndexDetails() = VaultModels.IndexInfo(
            name = getEffectiveName(),
            type = "ttl",
            fields = getFieldPaths(),
        )

        override suspend fun create(): EnsureResult {

            val name = getEffectiveName()
            val fields = getFieldPaths()

            options.name(name)

            return createHelper(name, fields) {
                ensureTtlIndex(fields, options).await()
            }
        }

        /**
         * Use this to set index options.
         *
         * See [TtlIndexOptions]
         */
        fun options(block: TtlIndexOptions.() -> TtlIndexOptions) {
            options = options.block()
        }
    }

    /** Built indexes */
    private val _indexes = mutableListOf<BaseBuilder<T>>()

    /** Get all index definitions */
    fun getIndexDefinitions(): List<BaseBuilder<T>> = _indexes.toList()

    /**
     * Creates all registered indexes
     */
    internal suspend fun create(): List<EnsureResult> {
        return _indexes.map { it.create() }
    }

    /**
     * Registers a persistent index.
     *
     * See https://www.arangodb.com/docs/stable/indexing-persistent.html
     */
    fun persistentIndex(block: PersistentIndexBuilder<T>.() -> Unit) {
        _indexes.add(
            PersistentIndexBuilder(repo).apply(block)
        )
    }

    /**
     * Ensures a ttl index
     *
     * See https://www.arangodb.com/docs/stable/http/indexes-ttl.html
     */
    fun ttlIndex(block: TtlIndexBuilder<T>.() -> Unit) {
        _indexes.add(
            TtlIndexBuilder(repo)
                .apply {
                    options {
                        expireAfter(0)
                    }
                }.apply(
                    block
                )
        )
    }
}
