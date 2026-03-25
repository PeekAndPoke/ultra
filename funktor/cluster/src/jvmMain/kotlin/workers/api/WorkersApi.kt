package io.peekandpoke.funktor.cluster.workers.api

import io.peekandpoke.funktor.cluster.cluster
import io.peekandpoke.funktor.core.broker.OutgoingConverter
import io.peekandpoke.funktor.rest.ApiRoutes
import io.peekandpoke.funktor.rest.docs.codeGen
import io.peekandpoke.funktor.rest.docs.docs
import io.peekandpoke.ultra.remote.ApiResponse

class WorkersApi(converter: OutgoingConverter) : ApiRoutes("workers", converter) {

    data class WorkerParam(
        val worker: String,
    )

    val list = WorkersApiClient.List.mount {
        docs {
            name = "List workers"
        }.codeGen {
            funcName = "list"
        }.authorize {
            isSuperUser()
        }.handle {
            ApiResponse.ok(
                cluster.workers.stats()
            )
        }
    }

    val get = WorkersApiClient.Get.mount(WorkerParam::class) {
        docs {
            name = "Get worker"
        }.codeGen {
            funcName = "get"
        }.authorize {
            isSuperUser()
        }.handle { params ->
            ApiResponse.ok(
                cluster.workers.stats(params.worker)
            )
        }
    }
}
