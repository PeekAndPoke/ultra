package de.peekandpoke.funktor.auth

import de.peekandpoke.funktor.core.lifecycle.AppLifeCycleHooks
import io.ktor.server.application.*

class AuthSystemAppHooks(
    authSystem: Lazy<AuthSystem>,
) : AppLifeCycleHooks.OnAppStarting {

    private val authSystem by authSystem

    override suspend fun onAppStarting(application: Application) {
        authSystem.validateRealms()
    }
}
