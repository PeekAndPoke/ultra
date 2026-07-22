package io.peekandpoke.funktor.rest.auth

import io.peekandpoke.funktor.core.broker.ConsistentParam
import io.peekandpoke.funktor.core.broker.OrgScopedParam
import io.peekandpoke.ultra.remote.ApiAccessLevel

/**
 * Phase-2 auth rule auto-appended by the framework to every non-public route whose params object
 * implements [ConsistentParam] (see the REST `ApiRoutes.addRoute`). Never declared by an author —
 * so the referential-consistency check can NEVER be forgotten.
 *
 * It runs in phase 2 (after conversion): [isCallerOnly] returns false for it (the `else` branch),
 * so it is evaluated with the resolved params in hand. On failure it hides as not-found
 * ([HideFailureAsNotFound]) — a mismatched entity pair is answered 404, identical to a genuine miss.
 *
 * [estimate] returns [ApiAccessLevel.Granted]: the per-role/-user access matrix estimates without
 * params, and consistency is a request-shape guard, not a permission the matrix should reason about.
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

/**
 * Phase-2 auth rule auto-appended by the framework to every non-public route whose params object
 * implements [OrgScopedParam]. Binds the request's addressed org ([OrgScopedParam.orgId]) to the
 * caller's SELECTED session org. Never declared by an author.
 *
 * Binds against the selected org ([io.peekandpoke.ultra.security.user.UserPermissions.hasOrganisation]
 * — `isSuperUser || org == orgId`), NOT `canAccessOrg`. Under the one-active-org-per-session model
 * the session's roles/branches/permissions are the SELECTED org's slice only; `accessibleOrgs` is
 * merely the login picker's candidate set (documented "Non-authz"). Authorizing against
 * `accessibleOrgs` would let a multi-org user who selected org A act on org B's data under org A's
 * role slice — a cross-tenant escalation. This matches the pre-existing `forOrganisation` rule.
 *
 * Like [ConsistentParamRule] it is phase-2 and hides its failure as not-found — a foreign-org
 * request is answered 404, never a distinguishable 403 that would leak "this org/entity exists".
 */
internal class CallerScopedParamRule : AuthRule<OrgScopedParam, Any?>, HideFailureAsNotFound {
    override val description: String = "Request org matches the caller's selected org (OrgScopedParam)"

    // No cast here: the rule is TYPED on [OrgScopedParam] — see the note on [ConsistentParamRule].
    override fun check(ctx: AuthRule.CheckCtx<OrgScopedParam, Any?>): Boolean =
        ctx.permissions.hasOrganisation(ctx.params.orgId)

    override fun estimate(ctx: AuthRule.EstimateCtx): ApiAccessLevel = ApiAccessLevel.Granted
}
