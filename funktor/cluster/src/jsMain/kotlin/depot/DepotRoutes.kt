package de.peekandpoke.ktorfx.cluster.depot

import de.peekandpoke.kraft.addons.routing.Route1
import de.peekandpoke.kraft.addons.routing.RouterBuilder
import de.peekandpoke.kraft.addons.routing.Static
import de.peekandpoke.ktorfx.cluster.KtorFxClusterUi
import js.uri.encodeURI

class DepotRoutes(mount: String) {
    val listRepositories = Static("$mount/repos")

    val browse = Route1("$mount/repos/{repo}/browse")
    fun browse(repo: String, path: String) = browse.buildUri(repo) + "?path=${encodeURI(path)}"
}

internal fun RouterBuilder.mountKtorFxDepot(
    ui: KtorFxClusterUi,
) {
    mount(ui.routes.depot.listRepositories) { ui { DepotRepositoriesListPage() } }
    mount(ui.routes.depot.browse) { ui { DepotBrowsePage(repo = it["repo"], path = it["path"]) } }
}
