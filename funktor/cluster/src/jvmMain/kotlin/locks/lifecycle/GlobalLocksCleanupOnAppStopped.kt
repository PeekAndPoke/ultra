package io.peekandpoke.funktor.cluster.locks.lifecycle

import io.ktor.server.application.*
import io.peekandpoke.funktor.cluster.locks.GlobalLocksProvider
import io.peekandpoke.funktor.cluster.locks.GlobalServerId
import io.peekandpoke.funktor.core.lifecycle.AppLifeCycleHooks
import io.peekandpoke.ultra.log.Log

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
