package de.peekandpoke.funktor.cluster.locks

import de.peekandpoke.funktor.cluster.FunktorClusterUi
import de.peekandpoke.kraft.addons.routing.RouterBuilder
import de.peekandpoke.kraft.routing.Static

class LocksRoutes(mount: String) {

    val listServerBeacons = Static("$mount/server-beacons")

    val listGlobalLocks = Static("$mount/global-locks")
}

internal fun RouterBuilder.mountFunktorLocks(
    ui: FunktorClusterUi,
) {
    mount(ui.routes.locks.listServerBeacons) { ui { ServerBeaconsListPage() } }
    mount(ui.routes.locks.listGlobalLocks) { ui { GlobalLocksListPage() } }
}
