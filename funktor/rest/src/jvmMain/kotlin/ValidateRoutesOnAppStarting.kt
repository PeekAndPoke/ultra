package io.peekandpoke.funktor.rest

import io.ktor.server.application.*
import io.peekandpoke.funktor.core.broker.InvalidRouteParamsException
import io.peekandpoke.funktor.core.broker.OutgoingConverter
import io.peekandpoke.funktor.core.lifecycle.AppLifeCycleHooks
import io.peekandpoke.funktor.core.lifecycle.AppLifeCycleHooks.ExecutionOrder
import io.peekandpoke.funktor.core.lifecycle.AppStartException
import io.peekandpoke.funktor.rest.auth.AuthRuleBuilder.Companion.validateChain

/**
 * Validates on app startup that:
 * - all route parameter types can be handled by the [OutgoingConverter];
 * - every route's WHOLE auth chain upholds the constant-soleness / no-empty-composite invariants —
 *   the backstop that catches any chain assembled outside the `authorize {}` DSL (a floor seed,
 *   auto-rules, or a direct `copy(authRules = ...)`), which per-block `build()` validation cannot see.
 */
class ValidateRoutesOnAppStarting(
    private val converter: OutgoingConverter,
    private val features: Lazy<List<ApiFeature>>,
) : AppLifeCycleHooks.OnAppStarting {

    override val executionOrder: ExecutionOrder = ExecutionOrder.VeryEarly

    override suspend fun onAppStarting(application: Application) {
        // The Application is not needed — validation is over the feature route registry.
        validateOrThrow()
    }

    /**
     * Runs every route validation and, if anything fails, aborts startup with an aggregated
     * [AppStartException]. Extracted (no [Application] dependency) so the failure path is unit
     * testable — it is the sole boot backstop for auth chains assembled outside the `authorize {}`
     * DSL (a floor seed, auto-rules, or a direct `copy(authRules = ...)`).
     */
    internal fun validateOrThrow() {
        val errors = mutableListOf<String>()

        for (feature in features.value) {
            for (routeGroup in feature.getRouteGroups()) {
                for (route in routeGroup.all) {
                    try {
                        route.typedRoute.validateConverterCompatibility(converter)
                    } catch (e: InvalidRouteParamsException) {
                        errors.add(e.message ?: "Unknown route validation error")
                    }
                    val at = "Route '${route.method.value} ${route.pattern.pattern}'"
                    // An empty chain serves PUBLIC — post-floor it can never happen; a served route
                    // with no rules means the floor was bypassed. Fail closed.
                    if (route.authRules.isEmpty()) {
                        errors.add(
                            "$at has no auth rules even though its group declares a floor — internal " +
                                    "invariant violation (the floor must be prepended to every route); " +
                                    "a normal ApiRoutes group cannot cause this. Please report it."
                        )
                    }
                    try {
                        validateChain("$at auth chain", route.authRules)
                    } catch (e: IllegalStateException) {
                        errors.add(e.message ?: "Unknown auth-rule validation error")
                    }
                }
            }
        }

        if (errors.isNotEmpty()) {
            throw AppStartException(
                "Route validation failed — the app cannot start until every item below is resolved " +
                        "(each line states its fix):\n${errors.joinToString("\n") { "  - $it" }}"
            )
        }
    }
}
