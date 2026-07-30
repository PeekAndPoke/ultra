package io.peekandpoke.funktor.auth

/**
 * How a realm relates to organisations (tenants).
 *
 * Realms divide user pools / interfaces (admin vs. end-user); organisations are an orthogonal
 * dimension. A realm either ignores orgs entirely or requires exactly one selected per session.
 *
 * ## Why this lives in `auth` and not in `saas`
 *
 * It reads like a saas concept, but it is realm CONFIGURATION and `AuthRealm` is its only consumer.
 * `funktor:auth` and `funktor:saas` are siblings over `core`/`rest` — auth does not depend on saas —
 * so moving it would invert that, pointing the general module at the specific one. The org types auth
 * genuinely shares with saas (`OrgId`, `OrgMembership`, `SelectedOrg`) already live below both, in
 * `ultra:security`.
 */
sealed interface OrgPolicy {
    /** The realm ignores orgs entirely (admin / global-management realms). Default. */
    data object None : OrgPolicy

    /** The realm requires an org: login resolves 0/1/n accessible orgs and selects exactly one. */
    data object Required : OrgPolicy
}
