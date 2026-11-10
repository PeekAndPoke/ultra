package io.peekandpoke.funktor.insights

import io.ktor.server.application.Application
import io.peekandpoke.funktor.core.config.AppConfig
import io.peekandpoke.funktor.core.lifecycle.AppLifeCycleHooks

/**
 * Aborts the boot when the app's one insights switch is on outside a development environment (ULTRA-00225,
 * ULTRA-00301), whether or not any route calls `instrumentWithInsights`.
 *
 * Throws `AppStartException`, the one throwable an `OnAppStarting` hook rethrows (see [CollectorKeyUniquenessCheck]).
 */
class InsightsEnvironmentCheck(private val appConfig: AppConfig) : AppLifeCycleHooks.OnAppStarting {

    override suspend fun onAppStarting(application: Application) = check()

    internal fun check() = refuseInsightsOutsideDevelopment(appConfig.funktor.insights, appConfig.ktor)
}
