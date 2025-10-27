package de.peekandpoke.karango.vault

import com.arangodb.ArangoCursorAsync
import com.arangodb.ArangoDBException
import com.arangodb.ArangoDatabaseAsync
import com.arangodb.entity.ArangoDBVersion
import com.arangodb.entity.CollectionType
import com.arangodb.model.AqlQueryOptions
import com.arangodb.model.CollectionCreateOptions
import de.peekandpoke.karango.AqlQueryOptionProvider
import de.peekandpoke.karango.KarangoCursor
import de.peekandpoke.karango.KarangoQueryException
import de.peekandpoke.karango.aql.AqlBuilder
import de.peekandpoke.karango.aql.RootExpression
import de.peekandpoke.karango.buildQuery
import de.peekandpoke.karango.slumber.KarangoCodec
import de.peekandpoke.karango.utils.ArangoDbRequestUtils
import de.peekandpoke.ultra.common.reflection.kMapType
import de.peekandpoke.ultra.log.Log
import de.peekandpoke.ultra.log.NullLog
import de.peekandpoke.ultra.vault.TypedQuery
import de.peekandpoke.ultra.vault.lang.TerminalExpr
import de.peekandpoke.ultra.vault.profiling.NullQueryProfiler
import de.peekandpoke.ultra.vault.profiling.QueryProfiler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking

class KarangoDriver(
    private val lazyCodec: Lazy<KarangoCodec>,
    private val lazyArangoDb: Lazy<ArangoDatabaseAsync>,
    private val lazyProfiler: Lazy<QueryProfiler> = lazy { NullQueryProfiler },
    val log: Log = NullLog,
) {
    val codec: KarangoCodec by lazyCodec
    val arangoDb: ArangoDatabaseAsync by lazyArangoDb
    val utils = ArangoDbRequestUtils(arangoDb)
    private val profiler: QueryProfiler by lazyProfiler

    fun withLog(newLog: Log) = KarangoDriver(
        lazyCodec = lazyCodec,
        lazyArangoDb = lazyArangoDb,
        lazyProfiler = lazyProfiler,
        log = newLog,
    )

    fun withProfiler(newProfiler: QueryProfiler) = KarangoDriver(
        lazyCodec = lazyCodec,
        lazyArangoDb = lazyArangoDb,
        lazyProfiler = lazy { newProfiler },
        log = log,
    )

    private val version: ArangoDBVersion by lazy {
        runBlocking { arangoDb.version.await() }
    }

    fun getDatabaseVersion(): ArangoDBVersion {
        return version
    }

    suspend fun ensureEntityCollection(
        name: String,
        options: CollectionCreateOptions = CollectionCreateOptions().type(CollectionType.DOCUMENT),
    ) {
        val arangoColl = arangoDb.collection(name)

        if (!arangoColl.exists().await()) {
            arangoDb.createCollection(name, options)
        }
    }

    /**
     * Performs a query and returns a cursor of results
     */
    suspend fun <X> query(builder: AqlBuilder.() -> TerminalExpr<X>): KarangoCursor<X> = query(
        query = buildQuery(builder)
    )

    /**
     * Performs the query and returns a cursor of the results
     */
    suspend fun <T> query(query: TypedQuery<T>): KarangoCursor<T> {

        return profiler.profile(
            connection = "ArangoDB::${arangoDb.name()}",
            queryLanguage = "aql",
            query = query.query
        ) { profilerEntry ->

            // IMPORTANT: We specifically define the values in the vars as nullable, since they can in fact be null.
            @Suppress("UNCHECKED_CAST")
            val vars = codec.slumber(kMapType<String, Any?>().type, query.vars) as Map<String, Any?>

            profilerEntry.vars = vars

//            log.trace("Arango query:\n${query.query}\nVars:\n${vars}\n")
//            println(query.query)
//            println(query.vars)

            // Get the options configured on the query
            val optionsProvider: AqlQueryOptionProvider? = (query.root as? RootExpression<T>)?.builder?.queryOptions

            // Apply the options
            val options = AqlQueryOptions().count(true).let {
                optionsProvider?.invoke(it)
            }

            val result = coroutineScope {
                val queryDeferred = async(Dispatchers.IO) {
                    profilerEntry.measureQuery.async {

//                        println(query.query)
//                        println(query.vars)

                        try {
                            arangoDb.query(
                                /* query = */ query.query,
                                /* type = */ Object::class.java,
                                /* bindVars = */ vars,
                                /* options = */ options,
                            ).await()
                        } catch (e: ArangoDBException) {
                            throw KarangoQueryException(
                                query = query,
                                message = "Error while querying '${e.message}':\n\n${query.query}\nwith params\n\n$vars",
                                cause = e,
                            )
                        }
                    }
                }

                val explainDeferred = profiler.explainQueries.takeIf { it }?.let {
                    async(Dispatchers.IO) {
                        profilerEntry.measureExplain.async {
                            profilerEntry.queryExplained = utils.explainQuery(query.query, vars)
                        }
                    }
                }

                val results = listOfNotNull(queryDeferred, explainDeferred).awaitAll()

                @Suppress("UNCHECKED_CAST")
                results.first() as ArangoCursorAsync<Any?>
            }

            profilerEntry.count = result.count?.toLong()
            profilerEntry.totalCount = result.extra?.stats?.fullCount

            // return the cursor
            KarangoCursor(
                arangoCursor = result,
                query = query,
                codec = codec,
                profiler = profilerEntry,
            )
        }
    }
}
