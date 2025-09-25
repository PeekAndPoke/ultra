package de.peekandpoke.funktor.cluster

import de.peekandpoke.funktor.cluster.backgroundjobs.BackgroundJobsRoutes
import de.peekandpoke.funktor.cluster.backgroundjobs.mountFunktorBackgroundJobs
import de.peekandpoke.funktor.cluster.depot.DepotRoutes
import de.peekandpoke.funktor.cluster.depot.mountFunktorDepot
import de.peekandpoke.funktor.cluster.locks.LocksRoutes
import de.peekandpoke.funktor.cluster.locks.mountFunktorLocks
import de.peekandpoke.funktor.cluster.storage.StorageRoutes
import de.peekandpoke.funktor.cluster.storage.mountFunktorStorage
import de.peekandpoke.funktor.cluster.vault.VaultRoutes
import de.peekandpoke.funktor.cluster.vault.mountFunktorVault
import de.peekandpoke.funktor.cluster.workers.WorkersRoutes
import de.peekandpoke.funktor.cluster.workers.mountFunktorWorkers
import de.peekandpoke.kraft.routing.RouterBuilder
import de.peekandpoke.kraft.routing.Static

class FunktorClusterRoutes(val mount: String = "/_/funktor/cluster") {

    val overview = Static(mount)

    val locks = LocksRoutes("$mount/locks")

    val vault = VaultRoutes("$mount/vault")

    val workers = WorkersRoutes("$mount/workers")

    val backgroundJobs = BackgroundJobsRoutes("$mount/background-jobs")

    val depot = DepotRoutes("$mount/depot")

    val storage = StorageRoutes("$mount/storage")
}

fun RouterBuilder.mountFunktorCluster(ui: FunktorClusterUi) {

    mount(ui.routes.overview) { ui { FunktorClusterOverviewPage() } }

    mountFunktorLocks(ui)
    mountFunktorVault(ui)
    mountFunktorWorkers(ui)
    mountFunktorBackgroundJobs(ui)
    mountFunktorDepot(ui)
    mountFunktorStorage(ui)
}
