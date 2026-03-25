package io.peekandpoke.funktor.cluster.locks.karango

import io.peekandpoke.funktor.cluster.locks.ServerBeaconRepository
import io.peekandpoke.funktor.cluster.locks.domain.ServerBeacon
import io.peekandpoke.funktor.core.fixtures.FixtureLoader
import io.peekandpoke.funktor.core.model.AppInfo
import io.peekandpoke.karango.vault.EntityRepository
import io.peekandpoke.karango.vault.KarangoDriver
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.vault.Stored

class KarangoServerBeaconRepo(driver: KarangoDriver, repoName: String) :
    EntityRepository<ServerBeacon>(name = repoName, kType(), driver), ServerBeaconRepository.Vault.Repo {

    class Fixtures(repo: Lazy<KarangoServerBeaconRepo>) : FixtureLoader {

        private val repo: KarangoServerBeaconRepo by repo

        override suspend fun prepare(result: FixtureLoader.MutableResult) {
            result.info("Clearing repo ${repo.name}")

            repo.removeAll()
        }

        override suspend fun load(result: FixtureLoader.MutableResult) {
            // noop
        }
    }

    override suspend fun update(serverId: String, appInfo: AppInfo): Stored<ServerBeacon> {

        val now = MpInstant.now()
        val version = "${appInfo.version.describeGit()} ${appInfo.version.version}"

        return modifyById(serverId) {
            it.copy(
                serverVersion = version,
                lastPing = now,
            )
        } ?: insert(
            key = serverId,
            ServerBeacon(
                serverId = serverId,
                serverVersion = version,
                lastPing = now,
            )
        )
    }

    override suspend fun list(): List<Stored<ServerBeacon>> {
        return findAll().toList()
    }

    override suspend fun delete(beacon: Stored<ServerBeacon>) {
        remove(beacon)
    }
}
