package io.peekandpoke.funktor.rest.auth

import io.peekandpoke.funktor.core.broker.ConsistentParam
import io.peekandpoke.ultra.remote.ApiAccessLevel

/**
 * Phase-2 auth rule auto-appended by the framework to every non-public route whose params object
 * implements [ConsistentParam] (see the REST `ApiRoutes.addRoute`). Opt-in — a params type declares
 * [ConsistentParam] when it wants a referential-consistency guard the framework then runs on every
 * request, so it can never be forgotten once opted in.
 *
 * It runs in phase 2 (after conversion): [isCallerOnly] returns false for it (the `else` branch),
 * so it is evaluated with the resolved params in hand. On failure it hides as not-found
 * ([HideFailureAsNotFound]) — a mismatched entity pair is answered 404, identical to a genuine miss.
 *
 * [estimate] returns [ApiAccessLevel.Granted]: the per-role/-user access matrix estimates without
 * params, and consistency is a request-shape guard, not a permission the matrix should reason about.
 *
 * NOTE: [ConsistentParam] is NOT a tenant boundary — it checks the entity graph against itself,
 * never the caller. Cross-org isolation is enforced separately by the saas `OrgIsolationGuard`
 * (a [io.peekandpoke.funktor.rest.RouteParamsGuard]).
 */
internal class ConsistentParamRule : AuthRule<ConsistentParam, Any?>, HideFailureAsNotFound {
    override val description: String = "Route params are referentially consistent (ConsistentParam)"

    // No cast here: the rule is TYPED on [ConsistentParam]. It is only ever appended to a route whose
    // PARAMS implements the interface (ApiRoutes.paramAutoRules gates on isAssignableFrom), and that
    // single, co-located generic cast in ApiRoutes.withAppendedRules carries the invariant.
    override fun check(ctx: AuthRule.CheckCtx<ConsistentParam, Any?>): Boolean =
        ctx.params.isConsistent()

    override fun estimate(ctx: AuthRule.EstimateCtx): ApiAccessLevel = ApiAccessLevel.Granted
}
