package de.peekandpoke.monko

import com.mongodb.ExplainVerbosity
import com.mongodb.client.model.DropIndexOptions
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.result.InsertOneResult
import com.mongodb.kotlin.client.coroutine.FindFlow
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import de.peekandpoke.monko.vault.vault.MongoTypedQuery
import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.log.Log
import de.peekandpoke.ultra.log.NullLog
import de.peekandpoke.ultra.slumber.awake
import de.peekandpoke.ultra.slumber.slumber
import de.peekandpoke.ultra.vault.RemoveResult
import de.peekandpoke.ultra.vault.Stored
import de.peekandpoke.ultra.vault.profiling.NullQueryProfiler
import de.peekandpoke.ultra.vault.profiling.QueryProfiler
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.bson.Document
import org.bson.conversions.Bson
import org.bson.json.JsonWriterSettings

class MonkoDriver(
    private val lazyCodec: Lazy<MonkoCodec>,
    private val lazyClient: Lazy<MongoClient>,
    private val lazyDatabase: Lazy<MongoDatabase>,
    private val lazyProfiler: Lazy<QueryProfiler> = lazy { NullQueryProfiler },
    private val log: Log = NullLog,
) {
    companion object {
        private val prettyJsonWriterSettings = JsonWriterSettings.builder().indent(true).build()

        private fun Document.toPrettyJson(): String {
            return toBsonDocument().toJson(prettyJsonWriterSettings)
        }
    }

    class FindQueryBuilder internal constructor() {
        private var filter: Bson? = null
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

        fun <T : Any> applyTo(flow: FindFlow<T>): FindFlow<T> {
            return flow
                .apply { filter?.let { filter(it) } ?: this }
                .apply { sort?.let { flow.sort(it) } ?: this }
                .apply { limit?.let { flow.limit(it) } ?: this }
                .apply { skip?.let { flow.skip(it) } ?: this }
        }

        fun print(): String {
            val doc = Document()
            doc["filter"] = filter?.toBsonDocument()
            doc["sort"] = sort?.toBsonDocument()
            doc["limit"] = limit
            doc["skip"] = skip

            return doc.toPrettyJson()
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
    )

    fun withProfiler(newProfiler: QueryProfiler) = MonkoDriver(
        lazyCodec = lazyCodec,
        lazyClient = lazyClient,
        lazyDatabase = lazyDatabase,
        lazyProfiler = lazy { newProfiler },
        log = log,
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

        val result = insertOne(collection, document)

        val insertedId = result.insertedId?.toString()
            ?: throw IllegalStateException("Insert failed")

        return Stored(
            _id = "$collection/$insertedId",
            _key = key ?: insertedId,
            _rev = "",
            value = value,
        )
    }

    suspend fun <T> findStored(
        collection: String,
        type: TypeRef<T>,
        query: FindQueryBuilder.() -> Unit = {},
    ): MonkoCursor<Stored<T>> {
        return profile {
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

    private suspend fun <R> profile(block: suspend QueryProfiler.Entry.() -> R): R = profiler.profile(
        connection = getConnectionName(),
        queryLanguage = "json",
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

        val entries = profile.measureQuery.async {
            mapped.toList()
        }

        val cursor = MonkoCursor(
            entries = entries,
            query = MongoTypedQuery.of(
                type = storedType,
                query = builder.print(),
                vars = emptyMap(),
            ),
            entityCache = codec.entityCache,
        )

        profile.count = cursor.count
        profile.query = cursor.query.query
        profile.vars = cursor.query.vars

        if (profiler.explainQueries) {
            profile.measureExplain.async {
                val explained = flow.explain(ExplainVerbosity.QUERY_PLANNER)
                profile.queryExplained = explained.toPrettyJson()
            }
        }

        return cursor
    }
}
