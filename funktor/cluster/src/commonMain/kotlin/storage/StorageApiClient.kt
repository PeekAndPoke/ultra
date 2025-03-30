package de.peekandpoke.ktorfx.cluster.storage

import de.peekandpoke.ultra.common.remote.ApiClient

class StorageApiClient(config: Config) : ApiClient(config) {

    val randomData = RandomDataStorageApiClient(config)
    val randomCache = RandomCacheStorageApiClient(config)
}
