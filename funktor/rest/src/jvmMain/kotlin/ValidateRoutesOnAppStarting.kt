package io.peekandpoke.funktor.rest

import io.ktor.server.application.*
import io.peekandpoke.funktor.core.lifecycle.AppLifeCycleHooks
import io.peekandpoke.funktor.core.lifecycle.AppLifeCycleHooks.ExecutionOrder
import io.peekandpoke.funktor.core.lifecycle.AppStartException

/**
 * Runs every registered [RouteBootCheck] over every route at app-start and, if anything fails,
 * aborts startup with one aggregated [AppStartException].
 *
 * A thin runner: each concern (converter compatibility, auth-chain soundness, org-isolation, …)
 * lives in its own injected [RouteBootCheck], so a module contributes a domain check without this
 * class knowing its types.
 */
class ValidateRoutesOnAppStarting(
    private val checks: Lazy<List<RouteBootCheck>>,
    private val features: Lazy<List<ApiFeature>>,
) : AppLifeCycleHooks.OnAppStarting {

    override val executionOrder: ExecutionOrder = ExecutionOrder.VeryEarly

    override suspend fun onAppStarting(application: Application) {
        // The Application is not needed — validation is over the feature route registry.
        validateOrThrow()
    }

    /**
     * Runs every [RouteBootCheck] over every route and, if anything fails, aborts startup with an
     * aggregated [AppStartException]. Public + no [Application] dependency so a module's boot check
     * can be exercised through the real runner in a test.
     */
    fun validateOrThrow() {
        val errors = mutableListOf<String>()

        for (feature in features.value) {
            for (routeGroup in feature.getRouteGroups()) {
                for (route in routeGroup.all) {
                    for (check in checks.value) {
                        errors.addAll(check.validate(route))
                    }
                }
            }
        }

        if (errors.isNotEmpty()) {
            throw AppStartException(
                "Route validation failed — the app cannot start until every item below is resolved " +
                        "(each line states its fix):\n" + errors.joinToString("\n") { indentEntry(it) }
            )
        }
    }

    /**
     * Prefixes the FIRST line of an entry with `"  - "` and indents every CONTINUATION line under the
     * text, so a multi-line message keeps the bullet grouping (the recorded aggregation-indent fix).
     */
    private fun indentEntry(entry: String): String =
        entry.split("\n").mapIndexed { index, line ->
            if (index == 0) "  - $line" else "    $line"
        }.joinToString("\n")
}
