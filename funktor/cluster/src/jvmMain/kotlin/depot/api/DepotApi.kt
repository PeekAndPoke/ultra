package de.peekandpoke.ktorfx.cluster.depot.api

import de.peekandpoke.ktorfx.cluster.cluster
import de.peekandpoke.ktorfx.core.broker.OutgoingConverter
import de.peekandpoke.ktorfx.rest.ApiRoutes
import de.peekandpoke.ktorfx.rest.docs.codeGen
import de.peekandpoke.ktorfx.rest.docs.docs
import de.peekandpoke.ultra.common.remote.ApiResponse

class DepotApi(converter: OutgoingConverter) : ApiRoutes("depot", converter) {

    data class BrowseParam(
        val repo: String,
        val path: String = "",
    )

    val listRepositories = DepotApiClient.ListRepositories.mount {
        docs {
            name = "List repositories"
        }.codeGen {
            funcName = "listRepositories"
        }.authorize {
            isSuperUser()
        }.handle {
            val result = cluster.depot.getRepos()

            ApiResponse.ok(
                result.map { it.asApiModel() }
            )
        }
    }

    val browse = DepotApiClient.Browse.mount(BrowseParam::class) {
        docs {
            name = "Browse"
        }.codeGen {
            funcName = "browse"
        }.authorize {
            isSuperUser()
        }.handle { params ->

            val repository = cluster.depot.getRepo(params.repo)
                ?: return@handle ApiResponse.notFound<DepotBrowseModel>()
                    .withError("Repo '${params.repo}' not found")

            val item = repository.getItem(params.path)
                ?: return@handle ApiResponse.notFound<DepotBrowseModel>()
                    .withError("Path '${params.repo}/${params.path}' not found")

            val meta = repository.getMeta(params.path)

            val children = repository.listItems(params.path)

            ApiResponse.ok(
                DepotBrowseModel(
                    repo = repository.asApiModel(),
                    item = item.asApiModel(),
                    meta = meta,
                    children = children.sortedBy { it.name }.map { it.asApiModel() }
                )
            )
        }
    }
}
