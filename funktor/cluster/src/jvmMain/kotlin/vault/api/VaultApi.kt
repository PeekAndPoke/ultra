package de.peekandpoke.ktorfx.cluster.vault.api

import de.peekandpoke.ktorfx.cluster.database
import de.peekandpoke.ktorfx.core.broker.OutgoingConverter
import de.peekandpoke.ktorfx.rest.ApiRoutes
import de.peekandpoke.ktorfx.rest.docs.codeGen
import de.peekandpoke.ktorfx.rest.docs.docs
import de.peekandpoke.ultra.common.remote.ApiResponse
import de.peekandpoke.ultra.vault.VaultModels

class VaultApi(converter: OutgoingConverter) : ApiRoutes("vault", converter) {

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
                    figures = repo.getStats(),
                    indexes = repo.validateIndexes(),
                )
            }

            ApiResponse.ok(
                results
            )
        }
    }
}
