package de.peekandpoke.funktor.cluster.workers

import de.peekandpoke.funktor.cluster.FunktorClusterUi
import de.peekandpoke.funktor.cluster.workers.api.WorkerModel
import de.peekandpoke.kraft.routing.Route1
import de.peekandpoke.kraft.routing.RouterBuilder
import de.peekandpoke.kraft.routing.Static

class WorkersRoutes(mount: String) {

    val list = Static(mount)

    val view = Route1("$mount/{id}")

    fun view(worker: WorkerModel) = view.build(worker.id)
}

internal fun RouterBuilder.mountFunktorWorkers(
    ui: FunktorClusterUi,
) {
    mount(ui.routes.workers.list) { ui { WorkersListPage() } }
    mount(ui.routes.workers.view) { ui { WorkerDetailsPage(it["id"]) } }
}
