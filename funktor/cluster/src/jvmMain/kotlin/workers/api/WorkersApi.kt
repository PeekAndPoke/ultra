package io.peekandpoke.funktor.cluster.workers.api

import io.peekandpoke.funktor.cluster.cluster
import io.peekandpoke.funktor.inspect.cluster.workers.api.WorkersApiClient
import io.peekandpoke.funktor.rest.ApiRoutes
import io.peekandpoke.funktor.rest.docs.codeGen
import io.peekandpoke.funktor.rest.docs.docs
import io.peekandpoke.ultra.remote.ApiResponse

class WorkersApi : ApiRoutes("workers") {

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
