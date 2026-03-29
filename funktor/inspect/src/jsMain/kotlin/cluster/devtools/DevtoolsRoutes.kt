package io.peekandpoke.funktor.cluster.devtools

import io.peekandpoke.funktor.cluster.FunktorClusterUi
import io.peekandpoke.kraft.routing.RouterBuilder
import io.peekandpoke.kraft.routing.Static

class DevtoolsRoutes(mount: String) {
    val requestHistory = Static("$mount/request-history")
}

internal fun RouterBuilder.mountFunktorDevtools(
    ui: FunktorClusterUi,
) {
    mount(ui.routes.devtools.requestHistory) { ui { DevtoolsRequestHistoryPage() } }
}
