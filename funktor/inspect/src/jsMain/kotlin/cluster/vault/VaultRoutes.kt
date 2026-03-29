package io.peekandpoke.funktor.cluster.vault

import io.peekandpoke.funktor.cluster.FunktorClusterUi
import io.peekandpoke.kraft.routing.RouterBuilder
import io.peekandpoke.kraft.routing.Static

class VaultRoutes(mount: String) {
    val index = Static(mount)
}

internal fun RouterBuilder.mountFunktorVault(
    ui: FunktorClusterUi,
) {
    mount(ui.routes.vault.index) { ui { VaultIndexPage() } }
}
