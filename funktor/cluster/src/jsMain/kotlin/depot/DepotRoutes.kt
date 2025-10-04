package de.peekandpoke.funktor.cluster.depot

import de.peekandpoke.funktor.cluster.FunktorClusterUi
import de.peekandpoke.kraft.routing.Route1
import de.peekandpoke.kraft.routing.RouterBuilder
import de.peekandpoke.kraft.routing.Static

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
