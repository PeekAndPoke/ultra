package io.peekandpoke.funktor.cluster.vault.api

import io.peekandpoke.funktor.cluster.database
import io.peekandpoke.funktor.inspect.cluster.vault.api.VaultApiClient
import io.peekandpoke.funktor.rest.ApiRoutes
import io.peekandpoke.funktor.rest.docs.codeGen
import io.peekandpoke.funktor.rest.docs.docs
import io.peekandpoke.ultra.remote.ApiResponse
import io.peekandpoke.ultra.vault.VaultModels

class VaultApi : ApiRoutes("vault") {

    val listRepositories = VaultApiClient.ListRepositories.mount {
        docs {
            name = "List repositories"
        }.codeGen {
            funcName = "listRepositories"
        }.authorize {
            isSuperUser()
        }.handle {
            val repositories = database.getRepositories()
                .sortedWith(compareBy({ it.connection }, { it.name }))

            val results = repositories.map { repo ->
                VaultModels.RepositoryInfo(
                    connection = repo.connection,
                    name = repo.name,
                    stats = repo.getStats(),
                    indexes = repo.validateIndexes(),
                )
            }

            ApiResponse.ok(
                results
            )
        }
    }
}
