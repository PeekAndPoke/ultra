package de.peekandpoke.ktorfx.cluster

import de.peekandpoke.ktorfx.cluster.backgroundjobs.BackgroundJobsApiClient
import de.peekandpoke.ktorfx.cluster.depot.api.DepotApiClient
import de.peekandpoke.ktorfx.cluster.locks.api.GlobalLocksApiClient
import de.peekandpoke.ktorfx.cluster.storage.StorageApiClient
import de.peekandpoke.ktorfx.cluster.vault.api.VaultApiClient
import de.peekandpoke.ktorfx.cluster.workers.api.WorkersApiClient
import de.peekandpoke.ultra.common.remote.ApiClient

class KtorFxClusterApiClient(config: ApiClient.Config) {

    val backgroundJobs = BackgroundJobsApiClient(config)

    val globalLocks = GlobalLocksApiClient(config)

    val depot = DepotApiClient(config)

    val storage = StorageApiClient(config)

    val vault = VaultApiClient(config)

    val workers = WorkersApiClient(config)
}
