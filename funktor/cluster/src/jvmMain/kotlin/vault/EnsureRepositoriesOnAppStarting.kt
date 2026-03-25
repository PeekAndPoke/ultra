package io.peekandpoke.funktor.cluster.vault

import io.ktor.server.application.*
import io.peekandpoke.funktor.core.lifecycle.AppLifeCycleHooks
import io.peekandpoke.ultra.vault.Database

class EnsureRepositoriesOnAppStarting(
    private val database: Database,
) : AppLifeCycleHooks.OnAppStarting {

    override val executionOrder = AppLifeCycleHooks.ExecutionOrder.ExtremelyEarly

    override suspend fun onAppStarting(application: Application) {
        database.ensureRepositories()
    }
}
