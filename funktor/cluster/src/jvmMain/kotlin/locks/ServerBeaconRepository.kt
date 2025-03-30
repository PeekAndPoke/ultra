package de.peekandpoke.ktorfx.cluster.locks

import de.peekandpoke.ktorfx.cluster.locks.domain.ServerBeacon
import de.peekandpoke.ktorfx.core.model.AppInfo
import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.vault.New
import de.peekandpoke.ultra.vault.Stored

interface ServerBeaconRepository {

    object Null : ServerBeaconRepository {
        override suspend fun update(serverId: String, appInfo: AppInfo): Stored<ServerBeacon> {
            val version = "${appInfo.version.describeGit()} ${appInfo.version.version}"

            return New(
                ServerBeacon(
                    serverId = serverId,
                    serverVersion = version,
                    lastPing = MpInstant.now()
                ),
            ).asStored
        }

        override suspend fun list(): List<Stored<ServerBeacon>> {
            return emptyList()
        }

        override suspend fun remove(beacon: Stored<ServerBeacon>) {
            // noop
        }
    }

    class Vault(val inner: Repo) : ServerBeaconRepository {
        interface Repo {
            suspend fun update(serverId: String, appInfo: AppInfo): Stored<ServerBeacon>

            suspend fun list(): List<Stored<ServerBeacon>>

            suspend fun delete(beacon: Stored<ServerBeacon>)
        }

        override suspend fun update(serverId: String, appInfo: AppInfo): Stored<ServerBeacon> {
            return inner.update(serverId = serverId, appInfo = appInfo)
        }

        override suspend fun list(): List<Stored<ServerBeacon>> {
            return inner.list()
        }

        override suspend fun remove(beacon: Stored<ServerBeacon>) {
            return inner.delete(beacon)
        }
    }

    /**
     * Updates the Beacon for the given [serverId].
     */
    suspend fun update(serverId: String, appInfo: AppInfo): Stored<ServerBeacon>

    /**
     * Lists all Beacons.
     */
    suspend fun list(): List<Stored<ServerBeacon>>

    /**
     * Removes a server beacon
     */
    suspend fun remove(beacon: Stored<ServerBeacon>)
}
