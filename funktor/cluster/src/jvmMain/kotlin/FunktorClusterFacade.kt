package de.peekandpoke.funktor.cluster

import de.peekandpoke.funktor.cluster.backgroundjobs.BackgroundJobs
import de.peekandpoke.funktor.cluster.depot.DepotFacade
import de.peekandpoke.funktor.cluster.locks.GlobalServerId
import de.peekandpoke.funktor.cluster.locks.GlobalServerList
import de.peekandpoke.funktor.cluster.locks.LocksFacade
import de.peekandpoke.funktor.cluster.storage.StorageFacade
import de.peekandpoke.funktor.cluster.workers.WorkersFacade

class FunktorClusterFacade(
    serverId: Lazy<GlobalServerId>,
    servers: Lazy<GlobalServerList>,
    locks: Lazy<LocksFacade>,
    workers: Lazy<WorkersFacade>,
    backgroundJobs: Lazy<BackgroundJobs>,
    depot: Lazy<DepotFacade>,
    storage: Lazy<StorageFacade>,
) {
    val serverId: GlobalServerId by serverId
    val servers: GlobalServerList by servers
    val locks: LocksFacade by locks
    val backgroundJobs: BackgroundJobs by backgroundJobs
    val workers: WorkersFacade by workers
    val depot: DepotFacade by depot
    val storage: StorageFacade by storage
}
