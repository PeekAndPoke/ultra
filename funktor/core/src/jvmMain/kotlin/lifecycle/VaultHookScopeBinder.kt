package io.peekandpoke.funktor.core.lifecycle

import io.ktor.server.application.Application
import io.peekandpoke.ultra.log.Log
import io.peekandpoke.ultra.vault.DeferredVaultHookScope

/**
 * Roots vault's after-save / after-delete hooks in the [Application]'s coroutine scope.
 *
 * The vault module cannot do this itself: it sits below funktor and has no access to the Ktor
 * [Application], which does not exist yet when the kontainer is built. Until this runs, the scope
 * executes hooks inline.
 *
 * Binding to the [Application] means in-flight hooks are cancelled when the app shuts down,
 * instead of surviving in a detached scope.
 */
class VaultHookScopeBinder(
    private val hookScope: DeferredVaultHookScope,
    private val log: Log,
) : AppLifeCycleHooks.OnAppStarting {

    override val executionOrder = AppLifeCycleHooks.ExecutionOrder.ExtremelyEarly

    override suspend fun onAppStarting(application: Application) {
        hookScope.bind(scope = application, log = log)
    }
}
