package io.peekandpoke.funktor.rest

import io.ktor.server.application.*
import io.peekandpoke.funktor.core.broker.ConsistentParam
import io.peekandpoke.funktor.core.broker.InvalidRouteParamsException
import io.peekandpoke.funktor.core.broker.OutgoingConverter
import io.peekandpoke.funktor.core.broker.entityRefParams
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
                    consistencyForcingError(route, at)?.let { errors.add(it) }
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

    /**
     * Forces referential-consistency declaration where the route resolves TWO OR MORE entities from
     * the URL — the shape where one id can be spoofed to point at a foreign org's entity while the
     * request still looks internally consistent (cross-org IDOR). Such a params type MUST implement
     * [ConsistentParam]; otherwise the [io.peekandpoke.funktor.rest.auth.ConsistentParamRule] can't
     * be auto-appended and the hole stays open. Returns the actionable error, or null when fine.
     *
     * Skipped for a sole-constant (public/forbidden) route — it has no caller/org boundary, so
     * [io.peekandpoke.funktor.rest.ApiRoutes.addRoute] does not auto-append the rule there either.
     */
    private fun consistencyForcingError(route: ApiRoute<*>, at: String): String? {
        if (route.isSoleConstantChain()) return null

        val paramsType = route.typedRoute.reifiedParamsType
        val entityRefs = paramsType.entityRefParams()

        if (entityRefs.size < 2) return null
        if (ConsistentParam::class.java.isAssignableFrom(paramsType.cls.java)) return null

        val name = paramsType.cls.simpleName ?: paramsType.cls.toString()
        val refs = entityRefs.joinToString(", ") { "${it.first.name}: ${it.second}" }
        val firstKey = entityRefs[0].first.name
        val secondKey = entityRefs[1].first.name

        return "$at: its params '$name' resolve ${entityRefs.size} entities from the URL ($refs) but " +
                "'$name' does not implement ConsistentParam. Two entity ids in one route can be " +
                "spoofed into an inconsistent pair (e.g. an entity that belongs to a different org " +
                "than the '$firstKey' in the same URL) — a cross-org IDOR. Fix: implement " +
                "ConsistentParam on '$name' and assert the references belong together, e.g. " +
                "`override fun isConsistent() = $secondKey.value.orgId == $firstKey._key`. NOTE: " +
                "ConsistentParam is NOT a tenant boundary on its own (it never sees the caller) — if " +
                "'$firstKey' is an organisation, ALSO implement OrgScopedParam (`override val orgId " +
                "get() = $firstKey._key`) so the caller is bound to their selected org, otherwise a " +
                "caller can read another org's internally-consistent pair."
    }
}
