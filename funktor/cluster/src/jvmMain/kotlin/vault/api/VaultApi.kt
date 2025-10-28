package de.peekandpoke.funktor.cluster.vault.api

import de.peekandpoke.funktor.cluster.database
import de.peekandpoke.funktor.core.broker.OutgoingConverter
import de.peekandpoke.funktor.rest.ApiRoutes
import de.peekandpoke.funktor.rest.docs.codeGen
import de.peekandpoke.funktor.rest.docs.docs
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
