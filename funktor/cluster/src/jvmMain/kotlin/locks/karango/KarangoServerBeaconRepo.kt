package de.peekandpoke.ktorfx.cluster.locks.karango

import de.peekandpoke.karango.vault.EntityRepository
import de.peekandpoke.karango.vault.KarangoDriver
import de.peekandpoke.ktorfx.cluster.locks.ServerBeaconRepository
import de.peekandpoke.ktorfx.cluster.locks.domain.ServerBeacon
import de.peekandpoke.ktorfx.core.fixtures.FixtureLoader
import de.peekandpoke.ktorfx.core.model.AppInfo
import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.vault.Stored

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
