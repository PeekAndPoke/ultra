package de.peekandpoke.funktor.logging.karango

import de.peekandpoke.funktor.logging.LogsFilter
import de.peekandpoke.funktor.logging.LogsStorage
import de.peekandpoke.funktor.logging.api.LogEntryModel
import de.peekandpoke.funktor.logging.api.LogsRequest
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

    override suspend fun find(filter: LogsRequest.BulkAction.Filter): List<LogEntryModel> {
        return repo.findBy(filter).map { it.asApiModel() }
    }
}
