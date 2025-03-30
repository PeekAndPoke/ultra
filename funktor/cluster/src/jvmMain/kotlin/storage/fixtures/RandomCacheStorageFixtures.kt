package de.peekandpoke.ktorfx.cluster.storage.fixtures

import de.peekandpoke.ktorfx.cluster.storage.RandomCacheStorage
import de.peekandpoke.ktorfx.core.fixtures.FixtureLoader

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
