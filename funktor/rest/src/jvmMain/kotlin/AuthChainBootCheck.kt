package io.peekandpoke.funktor.rest

import io.peekandpoke.funktor.rest.auth.AuthRuleBuilder.Companion.validateChain

/**
 * Boot check: every route's WHOLE auth chain upholds the constant-soleness / no-empty-composite
 * invariants ([validateChain]) and is non-empty. An empty chain would serve PUBLIC — post-floor it
 * can never happen, so a served route with no rules means the floor was bypassed (fail closed).
 *
 * The backstop for chains assembled OUTSIDE the `authorize {}` DSL (a floor seed, auto-rules, or a
 * direct `copy(authRules = …)`), which per-block `build()` validation cannot see. Extracted from the
 * old inline `ValidateRoutesOnAppStarting`.
 */
class AuthChainBootCheck : RouteBootCheck {
    override fun validate(route: ApiRoute<*>): List<String> {
        val at = "Route '${route.method.value} ${route.pattern.pattern}'"
        val errors = mutableListOf<String>()

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
        return errors
    }
}
