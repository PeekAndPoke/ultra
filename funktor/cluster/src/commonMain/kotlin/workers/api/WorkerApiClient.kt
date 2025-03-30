package de.peekandpoke.ktorfx.cluster.workers.api

import de.peekandpoke.ultra.common.remote.ApiClient
import de.peekandpoke.ultra.common.remote.ApiResponse
import de.peekandpoke.ultra.common.remote.TypedApiEndpoint
import de.peekandpoke.ultra.common.remote.api
import de.peekandpoke.ultra.common.remote.apiList
import de.peekandpoke.ultra.common.remote.call
import kotlinx.coroutines.flow.Flow

class WorkersApiClient(config: Config) : ApiClient(config) {

    companion object {
        const val base = "/_/workers"

        val List = TypedApiEndpoint.Get(
            base,
            response = WorkerModel.serializer().apiList()
        )

        val Get = TypedApiEndpoint.Get(
            "$base/{worker}",
            response = WorkerModel.serializer().api()
        )
    }

    fun list(): Flow<ApiResponse<List<WorkerModel>>> =
        call(List())

    fun get(workerId: String): Flow<ApiResponse<WorkerModel>> =
        call(Get("worker" to workerId))
}
