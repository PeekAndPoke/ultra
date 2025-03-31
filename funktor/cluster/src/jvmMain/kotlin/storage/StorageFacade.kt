package de.peekandpoke.funktor.cluster.storage

class StorageFacade(
    randomData: Lazy<RandomDataStorage>,
    randomCache: Lazy<RandomCacheStorage>,
) {
    val randomData: RandomDataStorage by randomData
    val randomCache: RandomCacheStorage by randomCache
}
