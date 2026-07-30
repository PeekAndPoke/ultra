package io.peekandpoke.funktor.auth

import io.ktor.server.application.Application
import io.peekandpoke.funktor.core.lifecycle.AppLifeCycleHooks

/**
 * Runs [AuthSystem.validateRealms] once while the app is starting — currently a duplicate-realm-id
 * check.
 *
 * At boot rather than lazily, because two realms sharing an id is not a per-request problem: every
 * lookup silently resolves to whichever one happens to be first, so the wrong realm authenticates and
 * nothing looks broken. Failing the deploy is the only place that is cheap to notice.
 */
class AuthSystemAppHooks(
    authSystem: Lazy<AuthSystem>,
) : AppLifeCycleHooks.OnAppStarting {

    private val authSystem by authSystem

    override suspend fun onAppStarting(application: Application) {
        authSystem.validateRealms()
    }
}
