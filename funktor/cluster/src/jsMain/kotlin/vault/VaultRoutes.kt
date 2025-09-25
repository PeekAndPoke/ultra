package de.peekandpoke.funktor.cluster.vault

import de.peekandpoke.funktor.cluster.FunktorClusterUi
import de.peekandpoke.kraft.routing.RouterBuilder
import de.peekandpoke.kraft.routing.Static

class VaultRoutes(mount: String) {
    val index = Static(mount)
}

internal fun RouterBuilder.mountFunktorVault(
    ui: FunktorClusterUi,
) {
    mount(ui.routes.vault.index) { ui { VaultIndexPage() } }
}
