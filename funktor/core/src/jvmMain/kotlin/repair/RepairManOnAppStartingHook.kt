package de.peekandpoke.funktor.core.repair

import de.peekandpoke.funktor.core.lifecycle.AppLifeCycleHooks
import io.ktor.server.application.*

class RepairManOnAppStartingHook(
    private val repairMan: RepairMan,
) : AppLifeCycleHooks.OnAppStarting {

    override val executionOrder = AppLifeCycleHooks.ExecutionOrder.VeryEarly

    override suspend fun onAppStarting(application: Application) {
        repairMan.run()
    }
}
