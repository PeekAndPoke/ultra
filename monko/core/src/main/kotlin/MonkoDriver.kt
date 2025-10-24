package de.peekandpoke.monko

import com.mongodb.kotlin.client.coroutine.FindFlow
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import de.peekandpoke.ultra.log.Log
import de.peekandpoke.ultra.log.NullLog
import de.peekandpoke.ultra.vault.profiling.NullQueryProfiler
import de.peekandpoke.ultra.vault.profiling.QueryProfiler
import kotlinx.coroutines.runBlocking
import org.bson.Document

class MonkoDriver(
    private val lazyCodec: Lazy<MonkoCodec>,
    private val lazyClient: Lazy<MongoClient>,
    private val lazyDatabase: Lazy<MongoDatabase>,
    private val lazyProfiler: Lazy<QueryProfiler> = lazy { NullQueryProfiler },
    private val log: Log = NullLog,
) {
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

    fun find(collection: String): FindFlow<Map<String, Any?>> {
        val coll = database.getCollection<Map<String, Any?>>(collection)

        return coll.find()
    }
}
