package de.peekandpoke.funktor.cluster

import de.peekandpoke.funktor.cluster.backgroundjobs.BackgroundJobsApiClient
import de.peekandpoke.funktor.cluster.depot.api.DepotApiClient
import de.peekandpoke.funktor.cluster.locks.api.GlobalLocksApiClient
import de.peekandpoke.funktor.cluster.storage.StorageApiClient
import de.peekandpoke.funktor.cluster.vault.api.VaultApiClient
import de.peekandpoke.funktor.cluster.workers.api.WorkersApiClient
import de.peekandpoke.ultra.common.remote.ApiClient

class FunktorClusterApiClient(config: ApiClient.Config) {

    val backgroundJobs = BackgroundJobsApiClient(config)

    val globalLocks = GlobalLocksApiClient(config)

    val depot = DepotApiClient(config)

    val storage = StorageApiClient(config)

    val vault = VaultApiClient(config)

    val workers = WorkersApiClient(config)
}
