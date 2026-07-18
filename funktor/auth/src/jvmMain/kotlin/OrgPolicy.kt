package io.peekandpoke.funktor.auth

/**
 * How a realm relates to organisations (tenants).
 *
 * Realms divide user pools / interfaces (admin vs. end-user); organisations are an orthogonal
 * dimension. A realm either ignores orgs entirely or requires exactly one selected per session.
 */
sealed interface OrgPolicy {
    /** The realm ignores orgs entirely (admin / global-management realms). Default. */
    data object None : OrgPolicy

    /** The realm requires an org: login resolves 0/1/n accessible orgs and selects exactly one. */
    data class Required(val onSignup: SignupOrgBehavior = SignupOrgBehavior.None) : OrgPolicy
}

/** What happens to org membership when a user signs up in an [OrgPolicy.Required] realm. */
sealed interface SignupOrgBehavior {
    /** No org is joined at signup — invite-only; the user hits the 0-org gate until assigned. */
    data object None : SignupOrgBehavior

    /** The signing-up user auto-joins the org with this slug (single-tenant / default-org apps). */
    data class AutoJoin(val orgSlug: String) : SignupOrgBehavior

    /** Signup creates a new org owned by the signing-up user (classic "create your workspace"). */
    data object CreateOwnOrg : SignupOrgBehavior
}
