package de.peekandpoke.ktorfx.cluster.vault

import de.peekandpoke.kraft.addons.routing.RouterBuilder
import de.peekandpoke.kraft.addons.routing.Static
import de.peekandpoke.ktorfx.cluster.KtorFxClusterUi

class VaultRoutes(mount: String) {
    val index = Static(mount)
}

internal fun RouterBuilder.mountKtorFxVault(
    ui: KtorFxClusterUi,
) {
    mount(ui.routes.vault.index) { ui { VaultIndexPage() } }
}
