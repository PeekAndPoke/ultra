package de.peekandpoke.funktor.cluster.locks.lifecycle

import de.peekandpoke.funktor.cluster.locks.GlobalLocksProvider
import de.peekandpoke.funktor.cluster.locks.GlobalServerId
import de.peekandpoke.funktor.core.lifecycle.AppLifeCycleHooks
import de.peekandpoke.ultra.logging.Log
import io.ktor.server.application.*

class GlobalLocksCleanupOnAppStopped(
    private val globalLocksProvider: GlobalLocksProvider,
    private val serverId: GlobalServerId,
    private val log: Log,
) : AppLifeCycleHooks.OnAppStopped {

    override val executionOrder = AppLifeCycleHooks.ExecutionOrder.VeryLate

    override suspend fun onAppStopped(application: Application) {
        val result = globalLocksProvider.releaseByServerId(serverId)

        if (result.isNotEmpty()) {
            log.warning("Released ${result.size} locks for server ${serverId.getId()}")
        }
    }
}
