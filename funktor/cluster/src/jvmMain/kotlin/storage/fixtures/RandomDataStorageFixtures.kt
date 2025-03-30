package de.peekandpoke.ktorfx.cluster.storage.fixtures

import de.peekandpoke.ktorfx.cluster.storage.RandomDataStorage
import de.peekandpoke.ktorfx.core.fixtures.FixtureLoader

class RandomDataStorageFixtures(
    private val randomDataStorage: RandomDataStorage,
) : FixtureLoader {

    override suspend fun prepare(result: FixtureLoader.MutableResult) {
        result.info("Clearing random data storage")

        randomDataStorage.clear()
    }

    override suspend fun load(result: FixtureLoader.MutableResult) {
        // does nothing
    }
}
