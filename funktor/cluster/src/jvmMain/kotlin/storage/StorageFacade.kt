package io.peekandpoke.funktor.cluster.storage

/** Facade providing access to random-data and random-cache storage subsystems. */
class StorageFacade(
    randomData: Lazy<RandomDataStorage>,
    randomCache: Lazy<RandomCacheStorage>,
) {
    val randomData: RandomDataStorage by randomData
    val randomCache: RandomCacheStorage by randomCache
}
