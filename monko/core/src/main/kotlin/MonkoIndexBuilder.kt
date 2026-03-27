package io.peekandpoke.monko

import com.mongodb.MongoCommandException
import com.mongodb.client.model.IndexOptions
import io.peekandpoke.monko.lang.MongoIterableExpr
import io.peekandpoke.monko.lang.MongoPropertyPath
import io.peekandpoke.monko.lang.dsl.toFieldPath
import io.peekandpoke.ultra.vault.VaultModels
import org.bson.Document
import java.util.concurrent.TimeUnit

/**
 * DSL builder for defining MongoDB indexes on a [MonkoRepository].
 *
 * Usage:
 * ```
 * override fun MonkoIndexBuilder<MyEntity>.buildIndexes() {
 *     persistentIndex {
 *         field { it.createdAt }
 *         field { it.status }
 *     }
 *
 *     ttlIndex {
 *         field { it.expiresAt }
 *     }
 *
 *     uniqueIndex {
 *         field { it.category }
 *         field { it.dataId }
 *     }
 * }
 * ```
 */
class MonkoIndexBuilder<T : Any>(private val repo: MonkoRepository<T>) {

    /** Base builder for all index types. */
    abstract class BaseBuilder<T : Any>(private val repo: MonkoRepository<T>) {

        private val fields = mutableListOf<String>()
        private var customName: String? = null

        /** The type prefix for auto-generated index names. */
        protected abstract val typePrefix: String

        /** Add a field to this index using a type-safe property path. */
        fun field(block: (MongoIterableExpr<T>) -> MongoPropertyPath<*, *>) {
            fields.add(block(repo.repoExpr).toFieldPath())
        }

        /** Set a custom name for this index. */
        fun name(name: String) {
            customName = name
        }

        /** Get the field paths for this index. */
        fun getFieldPaths(): List<String> = fields.toList()

        /** Get the effective index name (custom or auto-generated). */
        fun getEffectiveName(): String {
            val raw = customName ?: "$typePrefix${fields.joinToString("-")}"
            return raw.replace(".", "_").trim()
        }

        /** Get details for validation reporting. */
        fun getIndexDetails(): VaultModels.IndexInfo = VaultModels.IndexInfo(
            name = getEffectiveName(),
            type = typePrefix.trimEnd('-'),
            fields = fields,
        )

        /** Build the MongoDB key document (all fields ascending). */
        protected fun buildKeys(): Document {
            val doc = Document()
            fields.forEach { doc[it] = 1 }
            return doc
        }

        /** Build the index options with the effective name. */
        protected open fun buildOptions(): IndexOptions {
            return IndexOptions().name(getEffectiveName())
        }

        /** Check if this definition matches an existing MongoDB index. */
        fun matches(existing: Document): Boolean {
            val existingName = existing["name"]?.toString() ?: return false
            return existingName == getEffectiveName()
        }

        /** Create this index on the given driver/collection. */
        internal suspend fun create(driver: MonkoDriver, collection: String): EnsureResult {
            return try {
                driver.createIndex(collection, buildKeys(), buildOptions())
                EnsureResult.Ensured(repo, getEffectiveName(), getFieldPaths())
            } catch (e: MongoCommandException) {
                // Index already exists with same name — check if fields match
                val indexes = driver.listIndexes(collection)
                val existing = indexes.firstOrNull { it["name"] == getEffectiveName() }

                if (existing != null) {
                    val existingKeys = (existing["key"] as? Document)?.keys?.toList() ?: emptyList()
                    if (existingKeys == fields) {
                        EnsureResult.Kept(repo, getEffectiveName(), getFieldPaths())
                    } else {
                        // Fields differ — drop and recreate
                        driver.dropIndex(collection, getEffectiveName())
                        try {
                            driver.createIndex(collection, buildKeys(), buildOptions())
                            EnsureResult.ReCreated(repo, getEffectiveName(), getFieldPaths())
                        } catch (e2: Exception) {
                            EnsureResult.Error(repo, getEffectiveName(), getFieldPaths(), e2)
                        }
                    }
                } else {
                    EnsureResult.Error(repo, getEffectiveName(), getFieldPaths(), e)
                }
            } catch (e: Exception) {
                EnsureResult.Error(repo, getEffectiveName(), getFieldPaths(), e)
            }
        }
    }

    /** Result of an index ensure operation. */
    sealed class EnsureResult {
        abstract val repo: MonkoRepository<*>
        abstract val name: String
        abstract val fields: List<String>

        data class Ensured(
            override val repo: MonkoRepository<*>,
            override val name: String,
            override val fields: List<String>,
        ) : EnsureResult()

        data class Kept(
            override val repo: MonkoRepository<*>,
            override val name: String,
            override val fields: List<String>,
        ) : EnsureResult()

        data class ReCreated(
            override val repo: MonkoRepository<*>,
            override val name: String,
            override val fields: List<String>,
        ) : EnsureResult()

        data class Error(
            override val repo: MonkoRepository<*>,
            override val name: String,
            override val fields: List<String>,
            val error: Exception,
        ) : EnsureResult()
    }

    /** Builder for standard persistent indexes. */
    class PersistentIndexBuilder<T : Any> internal constructor(repo: MonkoRepository<T>) : BaseBuilder<T>(repo) {
        override val typePrefix = "persistent-"
    }

    /** Builder for unique indexes. */
    class UniqueIndexBuilder<T : Any> internal constructor(repo: MonkoRepository<T>) : BaseBuilder<T>(repo) {
        override val typePrefix = "unique-"

        override fun buildOptions(): IndexOptions {
            return super.buildOptions().unique(true)
        }
    }

    /** Builder for TTL indexes. */
    class TtlIndexBuilder<T : Any> internal constructor(repo: MonkoRepository<T>) : BaseBuilder<T>(repo) {
        override val typePrefix = "ttl-"
        private var expireAfterSeconds: Long = 0

        /** Set the TTL expiration. 0 means expire at the exact date stored in the field. */
        fun expireAfter(seconds: Long) {
            expireAfterSeconds = seconds
        }

        override fun buildOptions(): IndexOptions {
            return super.buildOptions().expireAfter(expireAfterSeconds, TimeUnit.SECONDS)
        }
    }

    private val indexes = mutableListOf<BaseBuilder<T>>()

    /** Get all index definitions. */
    fun getIndexDefinitions(): List<BaseBuilder<T>> = indexes.toList()

    /** Define a persistent (standard) index. */
    fun persistentIndex(block: PersistentIndexBuilder<T>.() -> Unit) {
        indexes.add(PersistentIndexBuilder(repo).apply(block))
    }

    /** Define a unique index. */
    fun uniqueIndex(block: UniqueIndexBuilder<T>.() -> Unit) {
        indexes.add(UniqueIndexBuilder(repo).apply(block))
    }

    /** Define a TTL index. */
    fun ttlIndex(block: TtlIndexBuilder<T>.() -> Unit) {
        indexes.add(TtlIndexBuilder(repo).apply(block))
    }

    /** Create all defined indexes. */
    internal suspend fun create(driver: MonkoDriver, collection: String): List<EnsureResult> {
        return indexes.map { it.create(driver, collection) }
    }

    /** Validate defined indexes against actual indexes in the database. */
    internal suspend fun validate(driver: MonkoDriver, collection: String): VaultModels.IndexesInfo {
        val definitions = indexes
        val actual = driver.listIndexes(collection).filter { it["name"] != "_id_" }

        val healthy = definitions.filter { def -> actual.any { def.matches(it) } }
        val missing = definitions.filter { def -> actual.none { def.matches(it) } }
        val excess = actual.filter { idx -> definitions.none { it.matches(idx) } }

        return VaultModels.IndexesInfo(
            healthyIndexes = healthy.map { it.getIndexDetails() },
            missingIndexes = missing.map { it.getIndexDetails() },
            excessIndexes = excess.map {
                VaultModels.IndexInfo(
                    name = it["name"]?.toString() ?: "unknown",
                    type = "unknown",
                    fields = (it["key"] as? Document)?.keys?.toList() ?: emptyList(),
                )
            },
        )
    }
}
