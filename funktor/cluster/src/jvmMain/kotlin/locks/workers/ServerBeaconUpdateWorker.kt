package de.peekandpoke.ktorfx.cluster.locks.workers

import de.peekandpoke.ktorfx.cluster.locks.GlobalServerId
import de.peekandpoke.ktorfx.cluster.locks.ServerBeaconRepository
import de.peekandpoke.ktorfx.cluster.workers.StateProvider
import de.peekandpoke.ktorfx.cluster.workers.Worker
import de.peekandpoke.ktorfx.core.model.AppInfo
import de.peekandpoke.ultra.common.datetime.MpInstant

class ServerBeaconUpdateWorker(
    serverId: Lazy<GlobalServerId>,
    serverBeaconRepository: Lazy<ServerBeaconRepository>,
    appInfo: Lazy<AppInfo>,
) : Worker {
    private val serverId by serverId
    private val serverBeaconRepository by serverBeaconRepository
    private val appInfo by appInfo

    override val shouldRun: (lastRun: MpInstant, now: MpInstant) -> Boolean = Worker.Every.seconds(5)

    override suspend fun execute(state: StateProvider) {
        // Update the server beacon of this server
        try {
            serverBeaconRepository.update(serverId = serverId.getId(), appInfo = appInfo)
        } catch (e: Throwable) {
            // noop
        }
    }
}
