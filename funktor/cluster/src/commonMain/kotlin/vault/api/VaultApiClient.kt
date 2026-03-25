package de.peekandpoke.funktor.cluster.vault.api

import de.peekandpoke.ultra.remote.ApiClient
import de.peekandpoke.ultra.remote.ApiResponse
import de.peekandpoke.ultra.remote.TypedApiEndpoint
import de.peekandpoke.ultra.remote.apiList
import de.peekandpoke.ultra.remote.call
import de.peekandpoke.ultra.vault.VaultModels
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
