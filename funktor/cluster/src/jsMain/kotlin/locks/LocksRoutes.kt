package de.peekandpoke.ktorfx.cluster.locks

import de.peekandpoke.kraft.addons.routing.RouterBuilder
import de.peekandpoke.kraft.addons.routing.Static
import de.peekandpoke.ktorfx.cluster.KtorFxClusterUi

class LocksRoutes(mount: String) {

    val listServerBeacons = Static("$mount/server-beacons")

    val listGlobalLocks = Static("$mount/global-locks")
}

internal fun RouterBuilder.mountKtorFxLocks(
    ui: KtorFxClusterUi,
) {
    mount(ui.routes.locks.listServerBeacons) { ui { ServerBeaconsListPage() } }
    mount(ui.routes.locks.listGlobalLocks) { ui { GlobalLocksListPage() } }
}
