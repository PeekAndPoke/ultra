package io.peekandpoke.funktor.inspect.cluster.storage

import io.peekandpoke.ultra.remote.ApiClient

class StorageApiClient(config: Config) : ApiClient(config) {

    val randomData = RandomDataStorageApiClient(config)
    val randomCache = RandomCacheStorageApiClient(config)
}
