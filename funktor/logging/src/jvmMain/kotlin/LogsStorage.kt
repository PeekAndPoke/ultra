package io.peekandpoke.funktor.logging

import io.peekandpoke.funktor.inspect.logging.LogsFilter
import io.peekandpoke.funktor.inspect.logging.api.LogEntryModel
import io.peekandpoke.funktor.inspect.logging.api.LogsRequest
import io.peekandpoke.ultra.model.Paged

interface LogsStorage {

    /** Find a log entry by its id */
    suspend fun getById(id: String): LogEntryModel?

    /** Updates an existing log entry */
    suspend fun update(entry: LogEntryModel): LogEntryModel

    /** Finds log entries */
    suspend fun find(filter: LogsFilter): Paged<LogEntryModel>

    /** Executes a bulk action */
    suspend fun execBulkAction(action: LogsRequest.BulkAction): LogsRequest.BulkResponse

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

    /**
     * A no-op implementation of [LogsStorage]
     */
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

        override suspend fun execBulkAction(action: LogsRequest.BulkAction): LogsRequest.BulkResponse {
            return LogsRequest.BulkResponse(
                numChanged = 0,
            )
        }
    }
}
