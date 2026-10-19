package io.peekandpoke.monko

import com.mongodb.ExplainVerbosity
import com.mongodb.client.model.DropIndexOptions
import com.mongodb.client.model.Filters
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.result.InsertOneResult
import com.mongodb.kotlin.client.coroutine.FindFlow
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.peekandpoke.monko.vault.vault.MongoTypedQuery
import io.peekandpoke.ultra.log.Log
import io.peekandpoke.ultra.log.NullLog
import io.peekandpoke.ultra.reflection.TypeRef
import io.peekandpoke.ultra.slumber.awake
import io.peekandpoke.ultra.slumber.slumber
import io.peekandpoke.ultra.vault.RemoveResult
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.VaultHookScope
import io.peekandpoke.ultra.vault.ensureKey
import io.peekandpoke.ultra.vault.profiling.NullQueryProfiler
import io.peekandpoke.ultra.vault.profiling.QueryProfiler
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.bson.BsonArray
import org.bson.BsonDocument
import org.bson.BsonDocumentReader
import org.bson.BsonInt32
import org.bson.BsonNull
import org.bson.BsonString
import org.bson.BsonType
import org.bson.BsonValue
import org.bson.Document
import org.bson.codecs.DecoderContext
import org.bson.codecs.DocumentCodec
import org.bson.conversions.Bson
import org.bson.json.JsonWriterSettings
import org.bson.types.ObjectId

class MonkoDriver(
    private val lazyCodec: Lazy<MonkoCodec>,
    private val lazyClient: Lazy<MongoClient>,
    private val lazyDatabase: Lazy<MongoDatabase>,
    private val lazyProfiler: Lazy<QueryProfiler> = lazy { NullQueryProfiler },
    val log: Log = NullLog,
    val hookScope: VaultHookScope = VaultHookScope.Inline(),
) {
    companion object {
        private val prettyJsonWriterSettings = JsonWriterSettings.builder().indent(true).build()

        private fun Document.toPrettyJson(): String {
            return toBsonDocument().toJson(prettyJsonWriterSettings)
        }

        /**
         * Converts a string key to the appropriate BSON id type.
         *
         * When MongoDB auto-generates an _id, it uses ObjectId. The key stored in [Stored._key]
         * is the hex string representation. This method converts it back to an ObjectId when
         * appropriate, so that filters match the original document.
         */
        internal fun toBsonId(key: String): Any {
            return if (ObjectId.isValid(key)) ObjectId(key) else key
        }

        private val documentCodec = DocumentCodec()

        /** The logical operators whose operand is a list of query documents. */
        private val LOGICAL_OPERATORS = setOf("\$and", "\$or", "\$nor")

        /**
         * Placeholder-ises a query document, recording each value in [vars]. Its keys are field names and
         * operators, so they are kept; each value goes through [placeholderiseValue].
         */
        private fun placeholderiseQuery(query: BsonDocument, vars: MutableMap<String, Any?>): BsonDocument =
            BsonDocument().also { out ->
                query.forEach { (key, child) ->
                    out[key] = when {
                        key in LOGICAL_OPERATORS && child.isArray -> BsonArray(
                            child.asArray().map { if (it.isDocument) placeholderiseQuery(it.asDocument(), vars) else bind(it, vars) }
                        )

                        else -> placeholderiseValue(child, vars)
                    }
                }
            }

        /**
         * Placeholder-ises what a field or an operator is compared with.
         *
         * Fail-closed: only an operator document (every key starts with `$`) is walked, and an `$elemMatch`
         * operand is walked as a query. Anything else is ONE value -- a whole `$in` list, and an embedded
         * document, whose keys may be data (`{profile: {<secret>: 1}}`).
         */
        private fun placeholderiseValue(value: BsonValue, vars: MutableMap<String, Any?>): BsonValue {
            if (!value.isOperatorDocument()) return bind(value, vars)

            return BsonDocument().also { out ->
                value.asDocument().forEach { (operator, operand) ->
                    out[operator] = when {
                        operator == "\$elemMatch" && operand.isDocument -> placeholderiseQuery(operand.asDocument(), vars)
                        else -> placeholderiseValue(operand, vars)
                    }
                }
            }
        }

        private fun BsonValue.isOperatorDocument(): Boolean =
            isDocument && asDocument().isNotEmpty() && asDocument().keys.all { it.startsWith("$") }

        /** Records [value] in [vars] and returns its `@pN:<type>` placeholder. */
        private fun bind(value: BsonValue, vars: MutableMap<String, Any?>): BsonString {
            val name = "p${vars.size + 1}"
            vars[name] = value.toPlain()
            return BsonString("@$name:${typeLabel(value)}")
        }

        /**
         * Mongo's `$type` alias of [value]'s BSON type, and an array's length: what the value IS, never the value.
         *
         * Read from the type alone, so nothing of the value can travel in it. It is what tells an `ObjectId`
         * `_id` from a string one -- the classic wrong-query bug -- which an untyped `@p1` hides.
         */
        private fun typeLabel(value: BsonValue): String = when (value.bsonType) {
            BsonType.ARRAY -> "array[${value.asArray().size}]"
            BsonType.DOUBLE -> "double"
            BsonType.STRING -> "string"
            BsonType.DOCUMENT -> "object"
            BsonType.BINARY -> "binData"
            BsonType.UNDEFINED -> "undefined"
            BsonType.OBJECT_ID -> "objectId"
            BsonType.BOOLEAN -> "bool"
            BsonType.DATE_TIME -> "date"
            BsonType.NULL -> "null"
            BsonType.REGULAR_EXPRESSION -> "regex"
            BsonType.DB_POINTER -> "dbPointer"
            BsonType.JAVASCRIPT -> "javascript"
            BsonType.SYMBOL -> "symbol"
            BsonType.JAVASCRIPT_WITH_SCOPE -> "javascriptWithScope"
            BsonType.INT32 -> "int"
            BsonType.TIMESTAMP -> "timestamp"
            BsonType.INT64 -> "long"
            BsonType.DECIMAL128 -> "decimal"
            BsonType.MIN_KEY -> "minKey"
            BsonType.MAX_KEY -> "maxKey"
            BsonType.END_OF_DOCUMENT, null -> "unknown"
        }

        /** The plain Java value a [Document] would hold, e.g. a `String` rather than a `BsonString`. */
        private fun BsonValue.toPlain(): Any? =
            documentCodec.decode(BsonDocumentReader(BsonDocument("v", this)), DecoderContext.builder().build())["v"]
    }

    class FindQueryBuilder internal constructor() {
        internal var filter: Bson? = null
            private set
        private var sort: Bson? = null
        private var limit: Int? = null
        private var skip: Int? = null

        fun filter(filter: Bson) {
            this.filter = filter
        }

        fun sort(sort: Bson) {
            this.sort = sort
        }

        fun limit(limit: Int) {
            this.limit = limit
        }

        fun skip(skip: Int) {
            this.skip = skip
        }

        fun page(page: Int = 1, epp: Int = 20) {
            skip(maxOf(0, page - 1) * epp)
            limit(epp)
        }

        fun <T : Any> applyTo(flow: FindFlow<T>): FindFlow<T> {
            return flow
                .apply { filter?.let { filter(it) } ?: this }
                .apply { sort?.let { flow.sort(it) } ?: this }
                .apply { limit?.let { flow.limit(it) } ?: this }
                .apply { skip?.let { flow.skip(it) } ?: this }
        }

        /** The query with every value inline. A debugging aid: never record it, see [printQuery]. */
        fun print(): String {
            val doc = Document()
            doc["filter"] = filter?.toBsonDocument()
            doc["sort"] = sort?.toBsonDocument()
            doc["limit"] = limit
            doc["skip"] = skip

            return doc.toPrettyJson()
        }

        /**
         * The query as the profiler records it: every filter VALUE is an `@pN:<type>` placeholder, held in
         * [Printed.vars].
         *
         * The [collection], field names, `sort`, `limit` and `skip` stay in the text, as AQL keeps its collection,
         * attribute names and `LIMIT`. A sort value is a direction or a `{$meta: …}` expression, never a bind value.
         */
        internal fun printQuery(collection: String): Printed {
            val vars = linkedMapOf<String, Any?>()

            val doc = BsonDocument()
            doc["collection"] = BsonString(collection)
            doc["filter"] = filter?.let { placeholderiseQuery(it.toBsonDocument(), vars) } ?: BsonNull.VALUE
            doc["sort"] = sort?.toBsonDocument() ?: BsonNull.VALUE
            doc["limit"] = limit?.let { BsonInt32(it) } ?: BsonNull.VALUE
            doc["skip"] = skip?.let { BsonInt32(it) } ?: BsonNull.VALUE

            return Printed(query = doc.toJson(prettyJsonWriterSettings), vars = vars)
        }

        /** A placeholder-ised query and the values its placeholders stand for. */
        internal data class Printed(val query: String, val vars: Map<String, Any?>) {
            companion object {
                /**
                 * The query language this text is recorded under -- and ONLY text from [printQuery] may be.
                 *
                 * `VaultCollector` keeps a query's text by this label alone, so a path recording raw `print()`
                 * under it would put bind values into insights records. Before 2026-10-19 Monko recorded raw
                 * text as `json`; that label is no longer trusted.
                 */
                const val LANGUAGE: String = "mongo-bound"
            }
        }
    }

    val codec by lazyCodec
    val client by lazyClient
    val database by lazyDatabase
    val profiler by lazyProfiler

    fun withLog(newLog: Log) = MonkoDriver(
        lazyCodec = lazyCodec,
        lazyClient = lazyClient,
        lazyDatabase = lazyDatabase,
        lazyProfiler = lazyProfiler,
        log = newLog,
        hookScope = hookScope,
    )

    fun withProfiler(newProfiler: QueryProfiler) = MonkoDriver(
        lazyCodec = lazyCodec,
        lazyClient = lazyClient,
        lazyDatabase = lazyDatabase,
        lazyProfiler = lazy { newProfiler },
        log = log,
        hookScope = hookScope,
    )

    private val version: String by lazy {
        runBlocking {
            client
                .getDatabase("admin")
                .runCommand(Document("buildInfo", 1))
                .getString("version")
        }
    }

    fun getDatabaseVersion(): String {
        return version
    }

    fun getConnectionName(): String {
        val version = getDatabaseVersion()

        return "MongoDB($version)::${database.name}"
    }

    suspend fun listIndexes(collection: String): List<Document> {
        val coll = database.getCollection<Document>(collection)

        val result = coll.listIndexes()

        return result.toList()
    }

    suspend fun dropIndex(collection: String, indexName: String, options: DropIndexOptions = DropIndexOptions()) {
        val coll = database.getCollection<Document>(collection)

        coll.dropIndex(indexName = indexName, options = options)
    }

    suspend fun createIndex(collection: String, keys: Document, options: IndexOptions = IndexOptions()): String {
        val coll = database.getCollection<Document>(collection)

        val result = coll.createIndex(keys, options)

        return result
    }

    suspend fun insertOne(collection: String, document: Document): InsertOneResult {
        val coll = database.getCollection<Document>(collection)

        return coll.insertOne(document)
    }

    suspend fun <T : Any> insertOne(collection: String, value: T, key: String?): Stored<T> {
        @Suppress("UNCHECKED_CAST")
        val slumbered = codec.slumber(value) as Map<String, Any?>

        val document = Document(slumbered)
        key?.let { document["_id"] = it }

        val result: InsertOneResult = insertOne(collection, document)

        val insertedId = result.insertedId ?: throw IllegalStateException("Insert failed")
        val stringId: String = when {
            insertedId.isString -> insertedId.asString().value
            insertedId.isObjectId -> insertedId.asObjectId().value.toHexString()
            else -> insertedId.toString()
        }

        return Stored(
            _id = "$collection/$stringId",
            _key = stringId,
            _rev = "",
            value = value,
        )
    }

    suspend fun <T : Any> replaceOne(collection: String, stored: Stored<T>): Stored<T> {
        @Suppress("UNCHECKED_CAST")
        val slumbered = codec.slumber(stored.resolve()) as Map<String, Any?>

        val bsonId = toBsonId(stored._key)

        val document = Document(slumbered)
        document["_id"] = bsonId

        val coll = database.getCollection<Document>(collection)

        coll.replaceOne(
            Filters.eq("_id", bsonId),
            document,
            ReplaceOptions().upsert(true),
        )

        return stored
    }

    suspend fun deleteOne(collection: String, idOrKey: String): RemoveResult {
        val coll = database.getCollection<Document>(collection)
        val result = coll.deleteOne(Filters.eq("_id", toBsonId(idOrKey.ensureKey)))

        return RemoveResult(
            count = result.deletedCount,
            query = null,
        )
    }

    suspend fun deleteMany(collection: String, filter: Bson): RemoveResult {
        val coll = database.getCollection<Document>(collection)
        val result = coll.deleteMany(filter)

        return RemoveResult(
            count = result.deletedCount,
            query = null,
        )
    }

    suspend fun updateMany(collection: String, filter: Bson, update: Bson): Long {
        val coll = database.getCollection<Document>(collection)
        val result = coll.updateMany(filter, update)

        return result.modifiedCount
    }

    suspend fun <T> findStored(
        collection: String,
        type: TypeRef<T>,
        query: FindQueryBuilder.() -> Unit = {},
    ): MonkoCursor<Stored<T>> {
        return profile(queryLanguage = FindQueryBuilder.Printed.LANGUAGE) {
            findStored(collection, type, query)
        }
    }

    suspend fun removeAll(collection: String): RemoveResult {
        val coll = database.getCollection<Document>(collection)
        val result = coll.deleteMany(Document())

        return RemoveResult(
            count = result.deletedCount,
            query = null,
        )
    }

    /** [queryLanguage] is required: each call site states what kind of text it records. */
    private suspend fun <R> profile(queryLanguage: String, block: suspend QueryProfiler.Entry.() -> R): R = profiler.profile(
        connection = getConnectionName(),
        queryLanguage = queryLanguage,
        query = "",
        block = block,
    )

    private fun find(
        collection: String,
        adjust: FindQueryBuilder.() -> Unit = {},
    ): Pair<FindQueryBuilder, FindFlow<Map<String, Any?>>> {
        val coll = database.getCollection<Map<String, Any?>>(collection)

        val queryBuilder = FindQueryBuilder().apply(adjust)

        val flow = coll.find().let { queryBuilder.applyTo(it) }

        return queryBuilder to flow
    }

    private suspend fun <T> QueryProfiler.Entry.findStored(
        collection: String,
        type: TypeRef<T>,
        query: FindQueryBuilder.() -> Unit = {},
    ): MonkoCursor<Stored<T>> {
        val profile = this

        val found = find(collection, query)
        val builder = found.first
        val flow = found.second

        val mapped = flow.map {
            profile.measureDeserializer {
                val key = "" + it["_id"]

                @Suppress("UNCHECKED_CAST")
                val value = codec.awake(type, it) as T

                Stored(
                    _id = "$collection/$key",
                    _key = key,
                    _rev = "",
                    value = value,
                )
            }
        }

        val storedType = type.wrapWith<Stored<T>>()

        // Placeholders, not `print()`: this text reaches the profiler and so the insights record, and a filter
        // is how a session token or an activation code is looked up. Recorded BEFORE executing, so a failed
        // query keeps its text, as Karango's does.
        val printed = builder.printQuery(collection)
        profile.query = printed.query
        profile.vars = printed.vars

        val entries = profile.measureQuery.async {
            mapped.toList()
        }

        // Count total matching documents (for pagination) when skip/limit are used
        val fullCount: Long? = builder.filter?.let { filter ->
            database.getCollection<Document>(collection).countDocuments(filter)
        }

        val cursor = MonkoCursor(
            entries = entries,
            query = MongoTypedQuery.of(
                type = storedType,
                query = printed.query,
                vars = printed.vars,
            ),
            entityCache = codec.entityCache,
            _fullCount = fullCount,
        )

        profile.count = cursor.count

        if (profiler.explainQueries) {
            profile.measureExplain.async {
                val explained = flow.explain(ExplainVerbosity.QUERY_PLANNER)
                profile.queryExplained = explained.toPrettyJson()
            }
        }

        return cursor
    }
}
