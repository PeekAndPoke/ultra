package io.peekandpoke.funktor.cluster.depot.api

import depot.api.DepotRepositoryModel
import io.peekandpoke.ultra.remote.ApiClient
import io.peekandpoke.ultra.remote.ApiResponse
import io.peekandpoke.ultra.remote.TypedApiEndpoint.Get
import io.peekandpoke.ultra.remote.api
import io.peekandpoke.ultra.remote.apiList
import io.peekandpoke.ultra.remote.call
import kotlinx.coroutines.flow.Flow

class DepotApiClient(config: Config) : ApiClient(config) {

    companion object {
        const val base = "/_/funktor/depot"

        val ListRepositories = Get(
            uri = "$base/repositories",
            response = DepotRepositoryModel.serializer().apiList(),
        )

        val Browse = Get(
            uri = "$base/repositories/{repo}/browse",
            response = DepotBrowseModel.serializer().api(),
        )
    }

    fun listRepositories(): Flow<ApiResponse<List<DepotRepositoryModel>>> =
        call(
            ListRepositories(),
        )

    fun browse(repo: String, path: String) =
        call(
            Browse(
                "repo" to repo,
                "path" to path,
            )
        )
}
