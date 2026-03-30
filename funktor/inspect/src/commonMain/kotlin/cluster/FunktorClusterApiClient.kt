package io.peekandpoke.funktor.inspect.cluster

import io.peekandpoke.funktor.inspect.cluster.backgroundjobs.BackgroundJobsApiClient
import io.peekandpoke.funktor.inspect.cluster.depot.api.DepotApiClient
import io.peekandpoke.funktor.inspect.cluster.locks.api.GlobalLocksApiClient
import io.peekandpoke.funktor.inspect.cluster.storage.StorageApiClient
import io.peekandpoke.funktor.inspect.cluster.vault.api.VaultApiClient
import io.peekandpoke.funktor.inspect.cluster.workers.api.WorkersApiClient
import io.peekandpoke.ultra.remote.ApiClient

class FunktorClusterApiClient(config: ApiClient.Config) {

    val backgroundJobs = BackgroundJobsApiClient(config)

    val globalLocks = GlobalLocksApiClient(config)

    val depot = DepotApiClient(config)

    val storage = StorageApiClient(config)

    val vault = VaultApiClient(config)

    val workers = WorkersApiClient(config)
}
