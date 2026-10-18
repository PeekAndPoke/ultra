package io.peekandpoke.funktor.insights

import io.ktor.server.application.Application
import io.peekandpoke.funktor.core.lifecycle.AppLifeCycleHooks
import io.peekandpoke.funktor.core.lifecycle.AppStartException
import io.peekandpoke.ultra.common.Lookup

/**
 * Aborts the boot when two collectors declare the same [InsightsCollector.key].
 *
 * The envelope is open by design — an application registers its own collectors and they are
 * indistinguishable from the built-ins. That is also the hazard: two slices claim one key, and the
 * frontend tab registry resolves one of them arbitrarily. Nothing else would report it, and the symptom
 * (one collector's tab intermittently showing another's data) points nowhere near the cause.
 *
 * Throws [AppStartException] specifically. `AppLifeCycleBuilder.register` runs `OnAppStarting` hooks with
 * `rethrow = { it is AppStartException }` and **swallows every other throwable into a log line**
 * (`AppLifeCycleBuilder.kt:39,91-97`, pinned by `AppLifeCycleSpec`). This class used `error()` until
 * 2026-07-31, so it never actually stopped a boot — it logged, and the app served with the duplicate.
 */
class CollectorKeyUniquenessCheck(
    private val collectors: Lookup<InsightsCollector>,
) : AppLifeCycleHooks.OnAppStarting {

    override suspend fun onAppStarting(application: Application) {
        val duplicates = collectors.all()
            .groupBy { it.key }
            .filterValues { it.size > 1 }

        if (duplicates.isEmpty()) return

        val detail = duplicates.entries.joinToString("; ") { (key, group) ->
            "'$key' declared by ${group.joinToString(", ") { it::class.simpleName ?: "?" }}"
        }

        throw AppStartException(
            "Insights collector keys must be unique, but $detail. A duplicate key puts two slices " +
                    "under one key in every record and collides in the frontend tab registry. " +
                    "Rename one of them."
        )
    }
}
