package io.peekandpoke.funktor.cluster.storage

import io.peekandpoke.ultra.remote.ApiClient

class StorageApiClient(config: Config) : ApiClient(config) {

    val randomData = RandomDataStorageApiClient(config)
    val randomCache = RandomCacheStorageApiClient(config)
}
