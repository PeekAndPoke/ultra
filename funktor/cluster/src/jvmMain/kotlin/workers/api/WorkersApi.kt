package de.peekandpoke.ktorfx.cluster.workers.api

import de.peekandpoke.ktorfx.cluster.cluster
import de.peekandpoke.ktorfx.core.broker.OutgoingConverter
import de.peekandpoke.ktorfx.rest.ApiRoutes
import de.peekandpoke.ktorfx.rest.docs.codeGen
import de.peekandpoke.ktorfx.rest.docs.docs
import de.peekandpoke.ultra.common.remote.ApiResponse

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
