package io.peekandpoke.funktor.inspect.cluster

import io.peekandpoke.funktor.inspect.cluster.backgroundjobs.BackgroundJobsRoutes
import io.peekandpoke.funktor.inspect.cluster.backgroundjobs.mountFunktorBackgroundJobs
import io.peekandpoke.funktor.inspect.cluster.depot.DepotRoutes
import io.peekandpoke.funktor.inspect.cluster.depot.mountFunktorDepot
import io.peekandpoke.funktor.inspect.cluster.devtools.DevtoolsRoutes
import io.peekandpoke.funktor.inspect.cluster.devtools.mountFunktorDevtools
import io.peekandpoke.funktor.inspect.cluster.locks.LocksRoutes
import io.peekandpoke.funktor.inspect.cluster.locks.mountFunktorLocks
import io.peekandpoke.funktor.inspect.cluster.storage.StorageRoutes
import io.peekandpoke.funktor.inspect.cluster.storage.mountFunktorStorage
import io.peekandpoke.funktor.inspect.cluster.vault.VaultRoutes
import io.peekandpoke.funktor.inspect.cluster.vault.mountFunktorVault
import io.peekandpoke.funktor.inspect.cluster.workers.WorkersRoutes
import io.peekandpoke.funktor.inspect.cluster.workers.mountFunktorWorkers
import io.peekandpoke.kraft.routing.RouterBuilder
import io.peekandpoke.kraft.routing.Static

class FunktorClusterRoutes(val mount: String = "/_/funktor/cluster") {

    val overview = Static(mount)

    val locks = LocksRoutes("$mount/locks")

    val vault = VaultRoutes("$mount/vault")

    val workers = WorkersRoutes("$mount/workers")

    val backgroundJobs = BackgroundJobsRoutes("$mount/background-jobs")

    val depot = DepotRoutes("$mount/depot")

    val storage = StorageRoutes("$mount/storage")

    val devtools = DevtoolsRoutes("$mount/devtools")
}

fun RouterBuilder.mountFunktorCluster(ui: FunktorInspectClusterUi) {

    mount(ui.routes.overview) { ui { FunktorClusterOverviewPage() } }

    mountFunktorLocks(ui)
    mountFunktorVault(ui)
    mountFunktorWorkers(ui)
    mountFunktorBackgroundJobs(ui)
    mountFunktorDepot(ui)
    mountFunktorStorage(ui)
    mountFunktorDevtools(ui)
}
