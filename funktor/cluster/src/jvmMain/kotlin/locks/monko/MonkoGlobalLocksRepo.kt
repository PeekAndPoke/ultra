package io.peekandpoke.funktor.cluster.locks.monko

import io.peekandpoke.funktor.cluster.locks.domain.GlobalLockEntry
import io.peekandpoke.funktor.core.fixtures.FixtureLoader
import io.peekandpoke.monko.MonkoDriver
import io.peekandpoke.monko.MonkoRepository
import io.peekandpoke.ultra.reflection.kType

class MonkoGlobalLocksRepo(driver: MonkoDriver, repoName: String) :
    MonkoRepository<GlobalLockEntry>(name = repoName, kType(), driver) {

    class Fixtures(repo: Lazy<MonkoGlobalLocksRepo>) : FixtureLoader {

        private val repo: MonkoGlobalLocksRepo by repo

        override suspend fun prepare(result: FixtureLoader.MutableResult) {
            result.info("Clearing repo ${repo.name}")
            repo.removeAll()
        }

        override suspend fun load(result: FixtureLoader.MutableResult) {
            // noop
        }
    }
}
