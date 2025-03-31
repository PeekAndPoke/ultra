package de.peekandpoke.funktor.messaging.fixtures

import de.peekandpoke.funktor.core.fixtures.FixtureLoader
import de.peekandpoke.funktor.messaging.storage.SentMessagesStorage

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
