package de.peekandpoke.ktorfx.cluster

import de.peekandpoke.kraft.addons.routing.RouterBuilder
import de.peekandpoke.kraft.addons.routing.Static
import de.peekandpoke.ktorfx.cluster.backgroundjobs.BackgroundJobsRoutes
import de.peekandpoke.ktorfx.cluster.backgroundjobs.mountKtorFxBackgroundJobs
import de.peekandpoke.ktorfx.cluster.depot.DepotRoutes
import de.peekandpoke.ktorfx.cluster.depot.mountKtorFxDepot
import de.peekandpoke.ktorfx.cluster.locks.LocksRoutes
import de.peekandpoke.ktorfx.cluster.locks.mountKtorFxLocks
import de.peekandpoke.ktorfx.cluster.storage.StorageRoutes
import de.peekandpoke.ktorfx.cluster.storage.mountKtorFxStorage
import de.peekandpoke.ktorfx.cluster.vault.VaultRoutes
import de.peekandpoke.ktorfx.cluster.vault.mountKtorFxVault
import de.peekandpoke.ktorfx.cluster.workers.WorkersRoutes
import de.peekandpoke.ktorfx.cluster.workers.mountKtorFxWorkers

class KtorFxClusterRoutes(val mount: String = "/_/ktorfx/cluster") {

    val overview = Static(mount)

    val locks = LocksRoutes("$mount/locks")

    val vault = VaultRoutes("$mount/vault")

    val workers = WorkersRoutes("$mount/workers")

    val backgroundJobs = BackgroundJobsRoutes("$mount/background-jobs")

    val depot = DepotRoutes("$mount/depot")

    val storage = StorageRoutes("$mount/storage")
}

fun RouterBuilder.mountKtorFxCluster(ui: KtorFxClusterUi) {

    mount(ui.routes.overview) { ui { KtorFxClusterOverviewPage() } }

    mountKtorFxLocks(ui)
    mountKtorFxVault(ui)
    mountKtorFxWorkers(ui)
    mountKtorFxBackgroundJobs(ui)
    mountKtorFxDepot(ui)
    mountKtorFxStorage(ui)
}
