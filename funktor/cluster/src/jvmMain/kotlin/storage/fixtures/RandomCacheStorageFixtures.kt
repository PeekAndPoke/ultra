package io.peekandpoke.funktor.cluster.storage.fixtures

import io.peekandpoke.funktor.cluster.storage.RandomCacheStorage
import io.peekandpoke.funktor.core.fixtures.FixtureLoader

class RandomCacheStorageFixtures(
    private val randomCacheStorage: RandomCacheStorage,
) : FixtureLoader {

    override suspend fun prepare(result: FixtureLoader.MutableResult) {
        result.info("Clearing random cache storage")

        randomCacheStorage.clear()
    }

    override suspend fun load(result: FixtureLoader.MutableResult) {
        // does nothing
    }
}
