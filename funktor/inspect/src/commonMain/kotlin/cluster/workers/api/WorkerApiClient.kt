package io.peekandpoke.funktor.inspect.cluster.workers.api

import io.peekandpoke.ultra.remote.ApiClient
import io.peekandpoke.ultra.remote.ApiResponse
import io.peekandpoke.ultra.remote.TypedApiEndpoint
import io.peekandpoke.ultra.remote.api
import io.peekandpoke.ultra.remote.apiList
import io.peekandpoke.ultra.remote.call
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
