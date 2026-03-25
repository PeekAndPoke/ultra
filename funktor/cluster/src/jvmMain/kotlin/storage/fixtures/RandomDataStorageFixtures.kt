package io.peekandpoke.funktor.cluster.storage.fixtures

import io.peekandpoke.funktor.cluster.storage.RandomDataStorage
import io.peekandpoke.funktor.core.fixtures.FixtureLoader

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
