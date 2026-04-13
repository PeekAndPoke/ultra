package io.peekandpoke.funktor.logging.monko

import io.peekandpoke.funktor.inspect.logging.LogsFilter
import io.peekandpoke.funktor.inspect.logging.api.LogEntryModel
import io.peekandpoke.funktor.inspect.logging.api.LogsRequest
import io.peekandpoke.funktor.logging.LogsStorage
import io.peekandpoke.funktor.logging.karango.asApiModel
import io.peekandpoke.ultra.model.Paged
import io.peekandpoke.ultra.vault.map

class MonkoLogsStorage(
    private val repo: MonkoLogRepository,
) : LogsStorage {

    override suspend fun getById(id: String): LogEntryModel? {
        return repo.findById(id)?.asApiModel()
    }

    override suspend fun update(entry: LogEntryModel): LogEntryModel {
        val logEntry = repo.findById(entry.id) ?: return entry

        return repo.save(logEntry.modify { it.update(entry) }).asApiModel()
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
                val numChanged = repo.bulkSetState(
                    filter = action.filter,
                    state = a.state,
                )

                LogsRequest.BulkResponse(numChanged = numChanged.toInt())
            }
        }

        return result
    }
}
