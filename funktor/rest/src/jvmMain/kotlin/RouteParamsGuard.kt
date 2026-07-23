package io.peekandpoke.funktor.rest

import io.peekandpoke.ultra.security.user.UserPermissions

/**
 * A pluggable phase-2 (post-conversion) request guard. The REST dispatch runs every registered guard
 * against the resolved route params + caller AFTER the route's own auth rules pass; any
 * [GuardVerdict.DenyAsNotFound] answers 404, byte-identical to a genuine not-found.
 *
 * Lets a module (e.g. saas) enforce a cross-cutting, param-data-dependent rule — org-isolation:
 * "the request's org is the caller's selected org, and every entity belongs to it" — with its OWN
 * concrete domain types, instead of the REST core carrying star-projected `Storable<*>` generics.
 */
interface RouteParamsGuard {
    /** Verdict for the resolved [params] and caller [permissions]. Return [GuardVerdict.Pass] to abstain. */
    fun guard(params: Any, permissions: UserPermissions): GuardVerdict
}

/** The outcome of a [RouteParamsGuard]. */
enum class GuardVerdict {
    /** This guard is satisfied, or does not apply to these params. */
    Pass,

    /** Deny — hidden as a not-found (404, byte-identical to a genuine miss), never a distinguishable 403. */
    DenyAsNotFound,
}
