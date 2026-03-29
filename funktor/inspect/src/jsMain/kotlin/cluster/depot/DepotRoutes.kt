package io.peekandpoke.funktor.cluster.depot

import io.peekandpoke.funktor.cluster.FunktorClusterUi
import io.peekandpoke.kraft.routing.Route1
import io.peekandpoke.kraft.routing.RouterBuilder
import io.peekandpoke.kraft.routing.Static

class DepotRoutes(mount: String) {
    val listRepositories = Static("$mount/repos")

    val browse = Route1("$mount/repos/{repo}/browse")
    fun browse(repo: String, path: String) = browse.bind(repo)
        .plusQueryParams("path" to path)
}

internal fun RouterBuilder.mountFunktorDepot(
    ui: FunktorClusterUi,
) {
    mount(ui.routes.depot.listRepositories) { ui { DepotRepositoriesListPage() } }
    mount(ui.routes.depot.browse) { ui { DepotBrowsePage(repo = it["repo"], path = it["path"]) } }
}
