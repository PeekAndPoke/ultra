package de.peekandpoke.funktor.cluster.vault

import de.peekandpoke.funktor.core.lifecycle.AppLifeCycleHooks
import de.peekandpoke.ultra.vault.Database
import io.ktor.server.application.*

class EnsureRepositoriesOnAppStarting(
    private val database: Database,
) : AppLifeCycleHooks.OnAppStarting {

    override val executionOrder = AppLifeCycleHooks.ExecutionOrder.ExtremelyEarly

    override suspend fun onAppStarting(application: Application) {
        database.ensureRepositories()
    }
}
