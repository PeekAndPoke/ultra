package io.peekandpoke.funktor.logging.api

import io.peekandpoke.funktor.inspect.logging.LogsFilter
import io.peekandpoke.funktor.inspect.logging.api.LoggingApiClient
import io.peekandpoke.funktor.inspect.logging.api.LogsRequest
import io.peekandpoke.funktor.logging.logging
import io.peekandpoke.funktor.rest.ApiRoutes
import io.peekandpoke.funktor.rest.docs.codeGen
import io.peekandpoke.funktor.rest.docs.docs
import io.peekandpoke.ultra.log.LogLevel
import io.peekandpoke.ultra.remote.ApiResponse

class LoggingApi : ApiRoutes("logging") {

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

            val result: LogsRequest.BulkResponse = logging.logsStorage.execBulkAction(body)

            ApiResponse.ok(
                result
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
