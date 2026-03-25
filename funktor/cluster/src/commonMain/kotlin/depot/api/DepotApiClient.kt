package de.peekandpoke.funktor.cluster.depot.api

import de.peekandpoke.ultra.remote.ApiClient
import de.peekandpoke.ultra.remote.ApiResponse
import de.peekandpoke.ultra.remote.TypedApiEndpoint.Get
import de.peekandpoke.ultra.remote.api
import de.peekandpoke.ultra.remote.apiList
import de.peekandpoke.ultra.remote.call
import depot.api.DepotRepositoryModel
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
