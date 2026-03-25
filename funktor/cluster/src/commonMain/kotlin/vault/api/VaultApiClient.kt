package io.peekandpoke.funktor.cluster.vault.api

import io.peekandpoke.ultra.remote.ApiClient
import io.peekandpoke.ultra.remote.ApiResponse
import io.peekandpoke.ultra.remote.TypedApiEndpoint
import io.peekandpoke.ultra.remote.apiList
import io.peekandpoke.ultra.remote.call
import io.peekandpoke.ultra.vault.VaultModels
import kotlinx.coroutines.flow.Flow

class VaultApiClient(config: Config) : ApiClient(config) {

    companion object {
        const val base = "/_/vault"

        val ListRepositories = TypedApiEndpoint.Get(
            "$base/repositories",
            response = VaultModels.RepositoryInfo.serializer().apiList()
        )
    }

    fun listRepositories(): Flow<ApiResponse<List<VaultModels.RepositoryInfo>>> =
        call(ListRepositories())
}
