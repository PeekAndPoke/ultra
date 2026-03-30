package io.peekandpoke.funktor.logging.karango

import io.peekandpoke.funktor.inspect.logging.LogsFilter
import io.peekandpoke.funktor.inspect.logging.api.LogEntryModel
import io.peekandpoke.funktor.inspect.logging.api.LogsRequest
import io.peekandpoke.funktor.logging.LogsStorage
import io.peekandpoke.karango.aql.FOR
import io.peekandpoke.karango.aql.RETURN_COUNT
import io.peekandpoke.karango.aql.UPDATE
import io.peekandpoke.karango.aql.aql
import io.peekandpoke.ultra.model.Paged

class KarangoLogsStorage(
    private val repo: KarangoLogRepository,
) : LogsStorage {
    override suspend fun getById(id: String): LogEntryModel? {
        return repo.findById(id)?.asApiModel()
    }

    override suspend fun update(entry: LogEntryModel): LogEntryModel {
        val logEntry = repo.findById(entry.id) ?: return entry

        return repo.save(logEntry) { it.update(entry) }.asApiModel()
    }

    override suspend fun find(filter: LogsFilter): Paged<LogEntryModel> {
        val found = repo.findBy(filter)

        return Paged(
            items = found.map { it.asApiModel() },
            page = filter.page,
            epp = filter.epp,
            fullItemCount = found.fullCount,
        )
    }

    override suspend fun execBulkAction(action: LogsRequest.BulkAction): LogsRequest.BulkResponse {
        val result = when (val a = action.action) {
            is LogsRequest.Action.SetState -> {
                val numChanged = repo.query {
                    FOR(repo) { entry ->
                        with(repo) {
                            filter(entry, action.filter)
                        }

                        UPDATE(entry, repo) {
                            put({ state }) {
                                a.state.aql
                            }
                        }

                        RETURN_COUNT()
                    }
                }.first()

                LogsRequest.BulkResponse(numChanged = numChanged)
            }
        }

        return result
    }
}
