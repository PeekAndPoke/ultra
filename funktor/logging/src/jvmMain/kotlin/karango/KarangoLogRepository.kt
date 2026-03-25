package io.peekandpoke.funktor.logging.karango

import io.peekandpoke.funktor.logging.LogsFilter
import io.peekandpoke.funktor.logging.api.LogsRequest
import io.peekandpoke.karango.KarangoCursor
import io.peekandpoke.karango.aql.AqlForLoop
import io.peekandpoke.karango.aql.AqlIterableExpr
import io.peekandpoke.karango.aql.CONTAINS
import io.peekandpoke.karango.aql.DESC
import io.peekandpoke.karango.aql.FOR
import io.peekandpoke.karango.aql.GTE
import io.peekandpoke.karango.aql.IN
import io.peekandpoke.karango.aql.IS_NULL
import io.peekandpoke.karango.aql.LOWER
import io.peekandpoke.karango.aql.LT
import io.peekandpoke.karango.aql.OR
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.aql.anyOrTrueIfEmpty
import io.peekandpoke.karango.aql.aql
import io.peekandpoke.karango.vault.EntityRepository
import io.peekandpoke.karango.vault.IndexBuilder
import io.peekandpoke.karango.vault.KarangoDriver
import io.peekandpoke.ultra.log.NullLog
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.profiling.NullQueryProfiler

class KarangoLogRepository(
    driver: KarangoDriver,
    repoName: String,
    private val expireAfterSecs: Int = 30 * 24 * 60 * 60,
) : EntityRepository<KarangoLogEntry>(
    name = repoName,
    storedType = kType(),
    driver = driver.withLog(NullLog).withProfiler(NullQueryProfiler),
) {

    override fun IndexBuilder<KarangoLogEntry>.buildIndexes() {
        persistentIndex {
            field { createdAt }
            field { severity }
            field { state }
        }

        persistentIndex {
            field { createdAt }
        }

        persistentIndex {
            field { severity }
        }

        persistentIndex {
            field { state }
        }

        ttlIndex {
            field { expiresAt }
            options {
                expireAfter(expireAfterSecs)
            }
        }
    }

    suspend fun findBy(filter: LogsFilter): KarangoCursor<Stored<KarangoLogEntry>> = find {
//        queryOptions {
//            it.count(true).fullCount(true)
//        }

        FOR(repo) { entry ->
            FILTER(entry.severity GTE filter.minLevel.severity)

            filter.state.takeIf { it.isNotEmpty() }?.let { state ->
                FILTER(IS_NULL(entry.state) OR (entry.state IN state))
            }

            if (filter.search.isNotBlank()) {
                val parts = filter.search.trim().lowercase().split(" ").filter { it.isNotBlank() }

                // TODO: use fulltext index

                FILTER(
                    parts.map { part ->
                        listOf(
                            CONTAINS(LOWER(entry.message), part.aql),
                            CONTAINS(LOWER(entry.loggerName), part.aql),
                            CONTAINS(LOWER(entry.stackTrace), part.aql),
                        ).anyOrTrueIfEmpty
                    }.anyOrTrueIfEmpty
                )
            }

            SORT(entry.createdAt.DESC)

            PAGE(page = filter.page, epp = filter.epp)

            RETURN(entry)
        }
    }

    fun AqlForLoop.filter(entry: AqlIterableExpr<KarangoLogEntry>, filter: LogsRequest.BulkAction.Filter) {
        filter.from?.let {
            FILTER(entry.createdAt GTE it.toEpochMillis())
        }

        filter.to?.let {
            FILTER(entry.createdAt LT it.toEpochMillis())
        }

        filter.states?.let {
            FILTER(IS_NULL(entry.state) OR (entry.state IN it))
        }

        SORT(entry.createdAt.DESC)
    }
}
