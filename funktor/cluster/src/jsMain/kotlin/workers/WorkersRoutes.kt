package de.peekandpoke.ktorfx.cluster.workers

import de.peekandpoke.kraft.addons.routing.Route1
import de.peekandpoke.kraft.addons.routing.RouterBuilder
import de.peekandpoke.kraft.addons.routing.Static
import de.peekandpoke.ktorfx.cluster.KtorFxClusterUi
import de.peekandpoke.ktorfx.cluster.workers.api.WorkerModel

class WorkersRoutes(mount: String) {

    val list = Static(mount)

    val view = Route1("$mount/{id}")

    fun view(worker: WorkerModel) = view.build(worker.id)
}

internal fun RouterBuilder.mountKtorFxWorkers(
    ui: KtorFxClusterUi,
) {
    mount(ui.routes.workers.list) { ui { WorkersListPage() } }
    mount(ui.routes.workers.view) { ui { WorkerDetailsPage(it["id"]) } }
}
