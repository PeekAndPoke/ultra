package io.peekandpoke.funktor.cluster.locks.workers

import io.peekandpoke.funktor.cluster.locks.GlobalServerId
import io.peekandpoke.funktor.cluster.locks.ServerBeaconRepository
import io.peekandpoke.funktor.cluster.workers.StateProvider
import io.peekandpoke.funktor.cluster.workers.Worker
import io.peekandpoke.funktor.core.model.AppInfo
import io.peekandpoke.ultra.datetime.MpInstant

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
