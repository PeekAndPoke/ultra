package de.peekandpoke.funktor.logging.karango

import de.peekandpoke.funktor.logging.LogsFilter
import de.peekandpoke.funktor.logging.LogsStorage
import de.peekandpoke.funktor.logging.api.LogEntryModel
import de.peekandpoke.funktor.logging.api.LogsRequest
import de.peekandpoke.karango.aql.FOR
import de.peekandpoke.karango.aql.RETURN_COUNT
import de.peekandpoke.karango.aql.UPDATE
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.ultra.common.model.Paged

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
