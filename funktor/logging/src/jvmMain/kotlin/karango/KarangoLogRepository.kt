package de.peekandpoke.funktor.logging.karango

import de.peekandpoke.funktor.logging.LogsFilter
import de.peekandpoke.funktor.logging.api.LogsRequest
import de.peekandpoke.karango.KarangoCursor
import de.peekandpoke.karango.aql.AqlForLoop
import de.peekandpoke.karango.aql.CONTAINS
import de.peekandpoke.karango.aql.DESC
import de.peekandpoke.karango.aql.FOR
import de.peekandpoke.karango.aql.GTE
import de.peekandpoke.karango.aql.IN
import de.peekandpoke.karango.aql.IS_NULL
import de.peekandpoke.karango.aql.LOWER
import de.peekandpoke.karango.aql.LT
import de.peekandpoke.karango.aql.OR
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.any
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.vault.EntityRepository
import de.peekandpoke.karango.vault.IndexBuilder
import de.peekandpoke.karango.vault.KarangoDriver
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.log.NullLog
import de.peekandpoke.ultra.vault.Stored
import de.peekandpoke.ultra.vault.profiling.NullQueryProfiler

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
        this.persistentIndex {
            field { createdAt }
            field { severity }
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
                        ).any
                    }.any
                )
            }

            SORT(entry.createdAt.DESC)

            PAGE(page = filter.page, epp = filter.epp)

            RETURN(entry)
        }
    }

    fun AqlForLoop.filter(entry: IterableExpr<KarangoLogEntry>, filter: LogsRequest.BulkAction.Filter) {
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
