package io.peekandpoke.funktor.cluster.depot.api

import io.peekandpoke.funktor.cluster.cluster
import io.peekandpoke.funktor.inspect.cluster.depot.api.DepotApiClient
import io.peekandpoke.funktor.inspect.cluster.depot.api.DepotBrowseModel
import io.peekandpoke.funktor.rest.ApiRoutes
import io.peekandpoke.funktor.rest.docs.codeGen
import io.peekandpoke.funktor.rest.docs.docs
import io.peekandpoke.ultra.remote.ApiResponse

class DepotApi : ApiRoutes("depot") {

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
