package io.peekandpoke.funktor.core.repair

import io.ktor.server.application.*
import io.peekandpoke.funktor.core.lifecycle.AppLifeCycleHooks

class RepairManOnAppStartingHook(
    private val repairMan: RepairMan,
) : AppLifeCycleHooks.OnAppStarting {

    override val executionOrder = AppLifeCycleHooks.ExecutionOrder.VeryEarly

    override suspend fun onAppStarting(application: Application) {
        repairMan.run()
    }
}
