package io.peekandpoke.funktor.cluster

import io.peekandpoke.funktor.cluster.backgroundjobs.BackgroundJobsApiClient
import io.peekandpoke.funktor.cluster.depot.api.DepotApiClient
import io.peekandpoke.funktor.cluster.locks.api.GlobalLocksApiClient
import io.peekandpoke.funktor.cluster.storage.StorageApiClient
import io.peekandpoke.funktor.cluster.vault.api.VaultApiClient
import io.peekandpoke.funktor.cluster.workers.api.WorkersApiClient
import io.peekandpoke.ultra.remote.ApiClient

class FunktorClusterApiClient(config: ApiClient.Config) {

    val backgroundJobs = BackgroundJobsApiClient(config)

    val globalLocks = GlobalLocksApiClient(config)

    val depot = DepotApiClient(config)

    val storage = StorageApiClient(config)

    val vault = VaultApiClient(config)

    val workers = WorkersApiClient(config)
}
