package io.peekandpoke.karango.vault

import com.arangodb.ArangoCursorAsync
import com.arangodb.ArangoDBException
import com.arangodb.ArangoDatabaseAsync
import com.arangodb.entity.ArangoDBVersion
import com.arangodb.entity.CollectionType
import com.arangodb.model.AqlQueryOptions
import com.arangodb.model.CollectionCreateOptions
import io.peekandpoke.karango.AqlQueryOptionProvider
import io.peekandpoke.karango.KarangoCursor
import io.peekandpoke.karango.KarangoQueryException
import io.peekandpoke.karango.aql.AqlRootExpression
import io.peekandpoke.karango.aql.AqlStatementBuilderImpl
import io.peekandpoke.karango.aql.AqlTerminalExpr
import io.peekandpoke.karango.buildAqlQuery
import io.peekandpoke.karango.slumber.KarangoCodec
import io.peekandpoke.karango.utils.ArangoDbRequestUtils
import io.peekandpoke.ultra.log.Log
import io.peekandpoke.ultra.log.NullLog
import io.peekandpoke.ultra.reflection.kMapType
import io.peekandpoke.ultra.vault.profiling.NullQueryProfiler
import io.peekandpoke.ultra.vault.profiling.QueryProfiler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking

/**
 * Executes AQL queries against ArangoDB and returns typed [KarangoCursor] results.
 *
 * Uses [KarangoCodec] for serializing query variables and deserializing results.
 * Supports query profiling via [QueryProfiler] and logging via [Log].
 */
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
    suspend fun <X> query(builder: AqlStatementBuilderImpl.() -> AqlTerminalExpr<X>): KarangoCursor<X> =
        query(query = buildAqlQuery(builder))

    /**
     * Performs the query and returns a cursor of the results
     */
    suspend fun <T> query(query: AqlTypedQuery<T>): KarangoCursor<T> {

        return profiler.profile(
            connection = "ArangoDB::${arangoDb.name()}",
            queryLanguage = "aql",
            query = query.query
        ) { profilerEntry ->

            // IMPORTANT: We specifically define the values in the vars as nullable, since they can in fact be null.
            @Suppress("UNCHECKED_CAST")
            val vars = codec.slumber(kMapType<String, Any?>().type, query.vars) as Map<String, Any?>

            profilerEntry.vars = vars

            // Get the options configured on the query
            val optionsProvider: AqlQueryOptionProvider? = (query.root as? AqlRootExpression<T>)?.builder?.queryOptions

            // Apply the options (preserve count(true) default when no provider is set)
            val options = AqlQueryOptions().count(true).let {
                optionsProvider?.invoke(it) ?: it
            }

            val result = coroutineScope {
                val queryDeferred = async(Dispatchers.IO) {
                    profilerEntry.measureQuery.async {
                        try {
                            arangoDb.query(
                                /* query = */ query.query,
                                /* type = */ Any::class.java,
                                /* bindVars = */ vars,
                                /* options = */ options,
                            ).await()
                        } catch (e: ArangoDBException) {
                            throw KarangoQueryException(
                                query = query,
                                message = "Error while querying '${e.message}':\n\n${query.query}\nwith params [${
                                    vars.keys.joinToString(
                                        ", "
                                    )
                                }]",
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
