package io.peekandpoke.funktor.inspect.cluster.locks

import io.peekandpoke.funktor.inspect.cluster.FunktorInspectClusterUi
import io.peekandpoke.kraft.routing.RouterBuilder
import io.peekandpoke.kraft.routing.Static

class LocksRoutes(mount: String) {

    val listServerBeacons = Static("$mount/server-beacons")

    val listGlobalLocks = Static("$mount/global-locks")
}

internal fun RouterBuilder.mountFunktorLocks(
    ui: FunktorInspectClusterUi,
) {
    mount(ui.routes.locks.listServerBeacons) { ui { ServerBeaconsListPage() } }
    mount(ui.routes.locks.listGlobalLocks) { ui { GlobalLocksListPage() } }
}
