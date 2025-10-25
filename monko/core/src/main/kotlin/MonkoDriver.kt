package de.peekandpoke.monko

import com.mongodb.client.result.InsertOneResult
import com.mongodb.kotlin.client.coroutine.FindFlow
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.log.Log
import de.peekandpoke.ultra.log.NullLog
import de.peekandpoke.ultra.vault.Stored
import de.peekandpoke.ultra.vault.TypedQuery
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
            val jsonWriterSettings = JsonWriterSettings.builder().indent(true).build()

            val filterJson = filter?.toBsonDocument()?.toJson(jsonWriterSettings)
            val sortJson = sort?.toBsonDocument()?.toJson(jsonWriterSettings)

            return buildString {
                appendLine("Filter: $filterJson")
                appendLine("Sort: $sortJson")
                appendLine("Limit: $limit")
                appendLine("Skip: $skip")
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
            _id = "$collection/${insertedId}",
            _key = key ?: insertedId,
            _rev = "",
            value = value,
        )
    }

    fun find(
        collection: String,
        adjust: FindQueryBuilder.() -> Unit = {},
    ): Pair<FindQueryBuilder, FindFlow<Map<String, Any?>>> {
        val coll = database.getCollection<Map<String, Any?>>(collection)

        val queryBuilder = FindQueryBuilder().apply(adjust)

        val flow = coll.find().let { queryBuilder.applyTo(it) }

        return queryBuilder to flow
    }

    suspend fun <T> findStored(
        collection: String,
        type: TypeRef<T>,
        query: FindQueryBuilder.() -> Unit = {},
    ): MonkoCursor<Stored<T>> {
        val found = find(collection, query)

        val mapped = found.second.map {
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

        val storedType = type.wrapWith<Stored<T>>()

        val cursor = MonkoCursor(
            entries = mapped.toList(),
            query = TypedQuery.of(
                type = storedType,
                query = found.first.print(),
                vars = emptyMap(),
            ),
            entityCache = codec.entityCache,
        )

        return cursor
    }
}
