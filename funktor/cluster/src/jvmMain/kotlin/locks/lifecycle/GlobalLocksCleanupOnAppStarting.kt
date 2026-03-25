package io.peekandpoke.funktor.cluster.locks.lifecycle

import io.ktor.server.application.*
import io.peekandpoke.funktor.cluster.locks.GlobalLocksProvider
import io.peekandpoke.funktor.cluster.locks.GlobalServerId
import io.peekandpoke.funktor.core.lifecycle.AppLifeCycleHooks
import io.peekandpoke.ultra.log.Log

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
