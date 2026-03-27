package io.peekandpoke.funktor.logging.monko

import com.mongodb.client.model.IndexOptions
import io.peekandpoke.funktor.logging.LogsFilter
import io.peekandpoke.funktor.logging.api.LogEntryModel
import io.peekandpoke.funktor.logging.api.LogsRequest
import io.peekandpoke.funktor.logging.karango.KarangoLogEntry
import io.peekandpoke.funktor.logging.karango.createdAt
import io.peekandpoke.funktor.logging.karango.expiresAt
import io.peekandpoke.funktor.logging.karango.loggerName
import io.peekandpoke.funktor.logging.karango.message
import io.peekandpoke.funktor.logging.karango.severity
import io.peekandpoke.funktor.logging.karango.stackTrace
import io.peekandpoke.funktor.logging.karango.state
import io.peekandpoke.monko.MonkoCursor
import io.peekandpoke.monko.MonkoDriver
import io.peekandpoke.monko.MonkoRepository
import io.peekandpoke.monko.lang.dsl.and
import io.peekandpoke.monko.lang.dsl.desc
import io.peekandpoke.monko.lang.dsl.exists
import io.peekandpoke.monko.lang.dsl.gte
import io.peekandpoke.monko.lang.dsl.isIn
import io.peekandpoke.monko.lang.dsl.lt
import io.peekandpoke.monko.lang.dsl.or
import io.peekandpoke.monko.lang.dsl.regex
import io.peekandpoke.monko.lang.dsl.setTo
import io.peekandpoke.ultra.log.NullLog
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.profiling.NullQueryProfiler
import org.bson.Document
import org.bson.conversions.Bson
import java.util.concurrent.TimeUnit

class MonkoLogRepository(
    driver: MonkoDriver,
    repoName: String,
    private val expireAfterSecs: Int = 30 * 24 * 60 * 60,
) : MonkoRepository<KarangoLogEntry>(
    name = repoName,
    storedType = kType(),
    driver = driver.withLog(NullLog).withProfiler(NullQueryProfiler),
) {

    override suspend fun ensureIndexes() {
        // Compound index on createdAt, severity, state
        driver.createIndex(
            collection = name,
            keys = Document(
                mapOf(
                    field { it.createdAt } to 1,
                    field { it.severity } to 1,
                    field { it.state } to 1,
                )
            ),
        )

        // TTL index on expiresAt
        driver.createIndex(
            collection = name,
            keys = Document(field { it.expiresAt }, 1),
            options = IndexOptions().expireAfter(0, TimeUnit.SECONDS),
        )
    }

    suspend fun findBy(filter: LogsFilter): MonkoCursor<Stored<KarangoLogEntry>> = find { r ->
        val filters = mutableListOf<Bson>()

        // Filter by minimum severity
        filters.add(r.severity gte filter.minLevel.severity)

        // Filter by state
        filter.state.takeIf { it.isNotEmpty() }?.let { states ->
            filters.add(
                or(
                    r.state.exists(false),
                    r.state isIn states,
                )
            )
        }

        // Filter by search text
        if (filter.search.isNotBlank()) {
            val parts = filter.search.trim().lowercase().split(" ").filter { it.isNotBlank() }

            val searchFilters = parts.map { part ->
                val pattern = Regex.escape(part)
                or(
                    r.message regex "(?i)$pattern",
                    r.loggerName regex "(?i)$pattern",
                    r.stackTrace regex "(?i)$pattern",
                )
            }

            if (searchFilters.isNotEmpty()) {
                filters.addAll(searchFilters)
            }
        }

        if (filters.isNotEmpty()) {
            filter(and(filters))
        }

        sort(r.createdAt.desc)
        skip(filter.page * filter.epp)
        limit(filter.epp)
    }

    suspend fun bulkSetState(
        filter: LogsRequest.BulkAction.Filter,
        state: LogEntryModel.State,
    ): Long {
        val filterBson = buildBulkFilter(filter)
        val update = repoExpr.state setTo state

        return driver.updateMany(
            collection = name,
            filter = filterBson,
            update = update,
        )
    }

    private fun buildBulkFilter(filter: LogsRequest.BulkAction.Filter): Bson {
        val filters = mutableListOf<Bson>()

        filter.from?.let {
            filters.add(repoExpr.createdAt gte it.toEpochMillis())
        }

        filter.to?.let {
            filters.add(repoExpr.createdAt lt it.toEpochMillis())
        }

        filter.states?.let { states ->
            filters.add(
                or(
                    repoExpr.state.exists(false),
                    repoExpr.state isIn states,
                )
            )
        }

        return if (filters.isNotEmpty()) and(filters) else Document()
    }
}
