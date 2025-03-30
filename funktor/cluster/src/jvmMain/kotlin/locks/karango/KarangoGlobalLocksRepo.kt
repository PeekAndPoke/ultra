package de.peekandpoke.ktorfx.cluster.locks.karango

import de.peekandpoke.karango.vault.EntityRepository
import de.peekandpoke.karango.vault.KarangoDriver
import de.peekandpoke.ktorfx.cluster.locks.domain.GlobalLockEntry
import de.peekandpoke.ktorfx.core.fixtures.FixtureLoader
import de.peekandpoke.ultra.common.reflection.kType

class KarangoGlobalLocksRepo(driver: KarangoDriver, repoName: String) :
    EntityRepository<GlobalLockEntry>(name = repoName, kType(), driver) {

    class Fixtures(repo: Lazy<KarangoGlobalLocksRepo>) : FixtureLoader {

        private val repo: KarangoGlobalLocksRepo by repo

        override suspend fun prepare(result: FixtureLoader.MutableResult) {
            result.info("Clearing repo ${repo.name}")
            repo.removeAll()
        }

        override suspend fun load(result: FixtureLoader.MutableResult) {
            // noop
        }
    }
}
