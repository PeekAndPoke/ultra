package io.peekandpoke.funktor.inspect.cluster.vault

import io.peekandpoke.funktor.inspect.cluster.FunktorInspectClusterUi
import io.peekandpoke.kraft.routing.RouterBuilder
import io.peekandpoke.kraft.routing.Static

class VaultRoutes(mount: String) {
    val index = Static(mount)
}

internal fun RouterBuilder.mountFunktorVault(
    ui: FunktorInspectClusterUi,
) {
    mount(ui.routes.vault.index) { ui { VaultIndexPage() } }
}
