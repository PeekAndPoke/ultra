package io.peekandpoke.funktor.messaging.fixtures

import io.peekandpoke.funktor.core.fixtures.FixtureLoader
import io.peekandpoke.funktor.messaging.storage.SentMessagesStorage

class SentMessagesFixtures(
    private val repo: SentMessagesStorage,
) : FixtureLoader {
    override suspend fun prepare(result: FixtureLoader.MutableResult) {
        repo.clear()
    }

    override suspend fun load(result: FixtureLoader.MutableResult) {
        // nothing to do here
    }
}
