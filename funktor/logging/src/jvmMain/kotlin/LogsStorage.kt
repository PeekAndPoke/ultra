package de.peekandpoke.funktor.logging

import de.peekandpoke.funktor.logging.api.LogEntryModel
import de.peekandpoke.funktor.logging.api.LogsRequest
import de.peekandpoke.ultra.common.model.Paged

interface LogsStorage {

    /** Find a log entry by its id */
    suspend fun getById(id: String): LogEntryModel?

    /** Updates an existing log entry */
    suspend fun update(entry: LogEntryModel): LogEntryModel

    /** Finds log entries */
    suspend fun find(filter: LogsFilter): Paged<LogEntryModel>

    /** Finds log entries */
    suspend fun find(filter: LogsRequest.BulkAction.Filter): List<LogEntryModel>

    /** Modifies a log entry */
    suspend fun modifyById(id: String, block: (LogEntryModel) -> LogEntryModel): LogEntryModel? {
        return getById(id)
            ?.let(block)
            ?.let { update(it) }
    }

    /** Executes an action on a log entry */
    suspend fun execAction(id: String, action: LogsRequest.Action): LogEntryModel? {
        return modifyById(id) { applyAction(it, action) }
    }

    /** Executes a bulk action */
    suspend fun execBulkAction(body: LogsRequest.BulkAction): List<LogEntryModel> {
        return find(body.filter)
            .map { applyAction(it, body.action) }
            .map { update(it) }
    }

    /**
     * Applies the given action to the given entry.
     *
     * This only returns a modified copy but does not save the changes.
     */
    private fun applyAction(entry: LogEntryModel, action: LogsRequest.Action): LogEntryModel {
        return when (action) {
            is LogsRequest.Action.SetState -> entry.copy(state = action.state)
        }
    }

    class Null : LogsStorage {
        override suspend fun getById(id: String): LogEntryModel? {
            return null
        }

        override suspend fun update(entry: LogEntryModel): LogEntryModel {
            return entry
        }

        override suspend fun find(filter: LogsFilter): Paged<LogEntryModel> {
            return Paged.empty()
        }

        override suspend fun find(filter: LogsRequest.BulkAction.Filter): List<LogEntryModel> {
            return emptyList()
        }
    }
}
