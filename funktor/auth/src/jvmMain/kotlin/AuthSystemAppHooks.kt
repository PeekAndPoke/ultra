package io.peekandpoke.funktor.auth

import io.ktor.server.application.*
import io.peekandpoke.funktor.core.lifecycle.AppLifeCycleHooks

class AuthSystemAppHooks(
    authSystem: Lazy<AuthSystem>,
) : AppLifeCycleHooks.OnAppStarting {

    private val authSystem by authSystem

    override suspend fun onAppStarting(application: Application) {
        authSystem.validateRealms()
    }
}
