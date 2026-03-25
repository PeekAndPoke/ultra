package de.peekandpoke.funktor.logging.api

import de.peekandpoke.funktor.logging.LogsFilter
import de.peekandpoke.ultra.model.Paged
import de.peekandpoke.ultra.remote.ApiClient
import de.peekandpoke.ultra.remote.ApiResponse
import de.peekandpoke.ultra.remote.TypedApiEndpoint.Get
import de.peekandpoke.ultra.remote.TypedApiEndpoint.Put
import de.peekandpoke.ultra.remote.api
import de.peekandpoke.ultra.remote.apiPaged
import de.peekandpoke.ultra.remote.call
import kotlinx.coroutines.flow.Flow

class LoggingApiClient(config: Config) : ApiClient(config) {

    companion object {
        const val base = "/_/funktor/logs"

        val List = Get(
            uri = base,
            response = LogEntryModel.serializer().apiPaged()
        )

        val Get = Get(
            uri = "$base/{id}",
            response = LogEntryModel.serializer().api()
        )

        val ExexBulkAction = Put(
            uri = "$base/bulk-action",
            body = LogsRequest.BulkAction.serializer(),
            response = LogsRequest.BulkResponse.serializer().api(),
        )

        val ExecAction = Put(
            uri = "$base/{id}/action",
            body = LogsRequest.Action.serializer(),
            response = LogEntryModel.serializer().api()
        )
    }

    fun list(filter: LogsFilter): Flow<ApiResponse<Paged<LogEntryModel>>> = call(
        List(
            *filter.toRequestParams(),
        )
    )

    fun get(id: String): Flow<ApiResponse<LogEntryModel>> = call(
        Get("id" to id)
    )

    fun execBulkAction(
        request: LogsRequest.BulkAction,
    ) = call(
        ExexBulkAction(body = request)
    )

    fun execAction(
        id: String,
        request: LogsRequest.Action,
    ): Flow<ApiResponse<LogEntryModel>> = call(
        ExecAction("id" to id, body = request)
    )
}
