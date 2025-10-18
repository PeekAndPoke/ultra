package de.peekandpoke.funktor.logging.api

import de.peekandpoke.funktor.core.broker.OutgoingConverter
import de.peekandpoke.funktor.logging.LogsFilter
import de.peekandpoke.funktor.logging.logging
import de.peekandpoke.funktor.rest.ApiRoutes
import de.peekandpoke.funktor.rest.docs.codeGen
import de.peekandpoke.funktor.rest.docs.docs
import de.peekandpoke.ultra.common.remote.ApiResponse
import de.peekandpoke.ultra.log.LogLevel

class LoggingApi(converter: OutgoingConverter) : ApiRoutes("logging", converter) {

    data class ListParam(
        val search: String = "",
        val minLevel: LogLevel = LogLevel.WARNING,
        val state: String = "",
        val page: Int = 1,
        val epp: Int = 100,
    ) {
        fun toFilter() = LogsFilter.of(
            search = search,
            minLevel = minLevel.name,
            state = state,
            page = page,
            epp = epp,
        )
    }

    data class GetParam(val id: String)

    val list = LoggingApiClient.List.mount(ListParam::class) {
        docs {
            name = "List log entries"
        }.codeGen {
            funcName = "list"
        }.authorize {
            isSuperUser()
        }.handle { params ->

            val filter = params.toFilter()
            val result = logging.logsStorage.find(filter)

            ApiResponse.ok(result)
        }
    }

    val get = LoggingApiClient.Get.mount(GetParam::class) {
        docs {
            name = "Get log entry"
        }.codeGen {
            funcName = "get"
        }.authorize {
            isSuperUser()
        }.handle {
            val result = logging.logsStorage.getById(it.id)

            ApiResponse.okOrNotFound(result)
        }
    }

    val execBulkAction = LoggingApiClient.ExexBulkAction.mount {
        docs {
            name = "Exec bulk action"
        }.codeGen {
            funcName = "execBulkAction"
        }.authorize {
            isSuperUser()
        }.handle { body ->

            val result: List<LogEntryModel> = logging.logsStorage.execBulkAction(body)

            ApiResponse.okOrNotFound(
                LogsRequest.BulkResponse(
                    numChanged = result.size,
                )
            )
        }
    }

    val execAction = LoggingApiClient.ExecAction.mount(GetParam::class) {
        docs {
            name = "Exec action"
        }.codeGen {
            funcName = "execAction"
        }.authorize {
            isSuperUser()
        }.handle { params, body ->
            val result = logging.logsStorage.execAction(params.id, body)

            ApiResponse.okOrNotFound(result)
        }
    }
}
