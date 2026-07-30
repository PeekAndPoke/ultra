package io.peekandpoke.funktor.saas.isolation

import io.peekandpoke.funktor.core.broker.entityRefParams
import io.peekandpoke.funktor.rest.ApiRoute
import io.peekandpoke.funktor.rest.RouteBootCheck
import kotlin.reflect.KClass

/**
 * Boot check (a [RouteBootCheck]): a route whose params resolve an [OrgAware] entity from the URL
 * MUST declare its addressed org via [OrgAwareParam] — otherwise the framework cannot bind the
 * request to the caller's org and `OrgIsolationGuard` never runs, leaving a cross-org IDOR.
 *
 * Whether an ENTITY is `OrgAware` is the developer's opt-in; this only forces the param side once an
 * org-owned entity is actually resolved by the route.
 *
 * LIMITATION: detection is by the DECLARED `Stored<X>` type argument. A polymorphic/base-typed param
 * (`Stored<Base>` where `Base` is not itself `OrgAware` but the concrete rows are) is NOT forced here
 * — boot has only types, not instances. The runtime `OrgIsolationGuard` still checks such an entity
 * by its runtime value IF the params are `OrgAwareParam`; but a base-typed param that also skips
 * `OrgAwareParam` would go unguarded. Base-typed entity params are unusual; the entity-`OrgAware`
 * linter (follow-up) is the intended backstop.
 */
class OrgIsolationBootCheck : RouteBootCheck {

    override fun validate(route: ApiRoute<*>): List<String> {
        val paramsType = route.typedRoute.reifiedParamsType

        val orgAwareEntityFields = paramsType.entityRefParams().filter { (_, type) ->
            val inner = type.arguments.firstOrNull()?.type?.classifier as? KClass<*>
            inner != null && OrgAware::class.java.isAssignableFrom(inner.java)
        }
        if (orgAwareEntityFields.isEmpty()) return emptyList()

        if (OrgAwareParam::class.java.isAssignableFrom(paramsType.cls.java)) return emptyList()

        val name = paramsType.cls.simpleName ?: paramsType.cls.toString()
        val fields = orgAwareEntityFields.joinToString(", ") { it.first.name ?: "?" }
        val at = "Route '${route.method.value} ${route.pattern.pattern}'"

        return listOf(
            "$at: params '$name' resolve org-owned (OrgAware) entities ($fields) but '$name' does " +
                    "not implement OrgAwareParam — the framework cannot bind the request to the " +
                    "caller's org, a cross-org IDOR. Fix: add an '{org}' path param and implement " +
                    "OrgAwareParam on '$name' (`override val org: Stored<Organisation>`), so " +
                    "OrgIsolationGuard enforces caller-binding + that every entity belongs to that org."
        )
    }
}
