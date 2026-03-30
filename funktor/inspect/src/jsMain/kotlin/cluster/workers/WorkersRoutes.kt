package io.peekandpoke.funktor.inspect.cluster.workers

import io.peekandpoke.funktor.inspect.cluster.FunktorInspectClusterUi
import io.peekandpoke.funktor.inspect.cluster.workers.api.WorkerModel
import io.peekandpoke.kraft.routing.Route1
import io.peekandpoke.kraft.routing.RouterBuilder
import io.peekandpoke.kraft.routing.Static

class WorkersRoutes(mount: String) {

    val list = Static(mount)

    val view = Route1("$mount/{id}")
    fun view(worker: WorkerModel) = view.bind(worker.id)
}

internal fun RouterBuilder.mountFunktorWorkers(
    ui: FunktorInspectClusterUi,
) {
    mount(ui.routes.workers.list) { ui { WorkersListPage() } }
    mount(ui.routes.workers.view) { ui { WorkerDetailsPage(it["id"]) } }
}
