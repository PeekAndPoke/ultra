package de.peekandpoke.funktor.cluster.vault

import de.peekandpoke.funktor.cluster.FunktorClusterUi
import de.peekandpoke.kraft.addons.routing.RouterBuilder
import de.peekandpoke.kraft.addons.routing.Static

class VaultRoutes(mount: String) {
    val index = Static(mount)
}

internal fun RouterBuilder.mountFunktorVault(
    ui: FunktorClusterUi,
) {
    mount(ui.routes.vault.index) { ui { VaultIndexPage() } }
}
