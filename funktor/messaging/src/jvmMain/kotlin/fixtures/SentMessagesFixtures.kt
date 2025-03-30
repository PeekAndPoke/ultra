package de.peekandpoke.ktorfx.messaging.fixtures

import de.peekandpoke.ktorfx.core.fixtures.FixtureLoader
import de.peekandpoke.ktorfx.messaging.storage.SentMessagesStorage

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
