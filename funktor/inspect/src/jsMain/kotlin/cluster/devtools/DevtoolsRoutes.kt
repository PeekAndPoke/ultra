package io.peekandpoke.funktor.inspect.cluster.devtools

import io.peekandpoke.funktor.inspect.cluster.FunktorInspectClusterUi
import io.peekandpoke.kraft.routing.RouterBuilder
import io.peekandpoke.kraft.routing.Static

class DevtoolsRoutes(mount: String) {
    val requestHistory = Static("$mount/request-history")
}

internal fun RouterBuilder.mountFunktorDevtools(
    ui: FunktorInspectClusterUi,
) {
    mount(ui.routes.devtools.requestHistory) { ui { DevtoolsRequestHistoryPage() } }
}
