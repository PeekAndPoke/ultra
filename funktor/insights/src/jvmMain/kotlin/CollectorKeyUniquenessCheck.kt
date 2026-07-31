package io.peekandpoke.funktor.insights

import io.ktor.server.application.Application
import io.peekandpoke.funktor.core.lifecycle.AppLifeCycleHooks
import io.peekandpoke.ultra.common.Lookup

/**
 * Fails the boot when two collectors declare the same [InsightsCollector.key].
 *
 * The envelope is open by design — an application registers its own collectors and they are
 * indistinguishable from the built-ins. That is also the hazard: a duplicate key is silently
 * last-wins in `InsightsDataLoader`'s summary lookup, and produces two tabs claiming the same slot in
 * the frontend. Nothing else would report it, and the symptom (one collector's data intermittently
 * missing) points nowhere near the cause.
 *
 * Checked at start-up rather than at write time so an app fails immediately and loudly, in the same
 * place `AuthChainBootCheck` reports a bad auth chain.
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

        error(
            "Insights collector keys must be unique, but $detail. A duplicate key silently shadows the " +
                    "other collector's slice in the record summary and collides in the frontend tab " +
                    "registry. Rename one of them."
        )
    }
}
