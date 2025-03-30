package de.peekandpoke.ktorfx.cluster

import de.peekandpoke.ktorfx.cluster.backgroundjobs.BackgroundJobs
import de.peekandpoke.ktorfx.cluster.depot.DepotFacade
import de.peekandpoke.ktorfx.cluster.locks.GlobalServerId
import de.peekandpoke.ktorfx.cluster.locks.GlobalServerList
import de.peekandpoke.ktorfx.cluster.locks.LocksFacade
import de.peekandpoke.ktorfx.cluster.storage.StorageFacade
import de.peekandpoke.ktorfx.cluster.workers.WorkersFacade

class KtorFxClusterFacade(
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
