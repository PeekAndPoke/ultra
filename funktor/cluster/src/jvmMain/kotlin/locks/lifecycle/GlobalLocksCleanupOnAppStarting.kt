package de.peekandpoke.funktor.cluster.locks.lifecycle

import de.peekandpoke.funktor.cluster.locks.GlobalLocksProvider
import de.peekandpoke.funktor.cluster.locks.GlobalServerId
import de.peekandpoke.funktor.core.lifecycle.AppLifeCycleHooks
import de.peekandpoke.ultra.logging.Log
import io.ktor.server.application.*

class GlobalLocksCleanupOnAppStarting(
    private val globalLocksProvider: GlobalLocksProvider,
    private val serverId: GlobalServerId,
    private val log: Log,
) : AppLifeCycleHooks.OnAppStarting {

    override val executionOrder = AppLifeCycleHooks.ExecutionOrder.VeryEarly

    override suspend fun onAppStarting(application: Application) {
        val result = globalLocksProvider.releaseByServerId(serverId)

        if (result.isNotEmpty()) {
            log.warning("Released ${result.size} locks for server ${serverId.getId()}")
        }
    }
}
