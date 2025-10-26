package de.peekandpoke.funktor.cluster.devtools

import de.peekandpoke.funktor.cluster.FunktorClusterUi
import de.peekandpoke.kraft.routing.RouterBuilder
import de.peekandpoke.kraft.routing.Static

class DevtoolsRoutes(mount: String) {
    val requestHistory = Static("$mount/request-history")
}

internal fun RouterBuilder.mountFunktorDevtools(
    ui: FunktorClusterUi,
) {
    mount(ui.routes.devtools.requestHistory) { ui { DevtoolsRequestHistoryPage() } }
}
