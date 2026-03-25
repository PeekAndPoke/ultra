package io.peekandpoke.funktor.cluster.locks.karango

import io.peekandpoke.funktor.cluster.locks.domain.GlobalLockEntry
import io.peekandpoke.funktor.core.fixtures.FixtureLoader
import io.peekandpoke.karango.vault.EntityRepository
import io.peekandpoke.karango.vault.KarangoDriver
import io.peekandpoke.ultra.reflection.kType

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
