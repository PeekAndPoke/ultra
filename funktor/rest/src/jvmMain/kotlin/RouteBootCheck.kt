package io.peekandpoke.funktor.rest

/**
 * A pluggable route validation run at app-start. [ValidateRoutesOnAppStarting] injects every
 * registered check and runs each over every route, aggregating their messages into one actionable
 * `AppStartException`.
 *
 * Each check owns a single concern (converter compatibility, auth-chain soundness, org-isolation, …)
 * and its own actionable messages, so the REST core stays a thin runner and a module (e.g. saas)
 * contributes a domain check — with its OWN concrete types — without the core hard-coding them.
 */
interface RouteBootCheck {
    /** Returns actionable error messages for [route], or an empty list when the route is fine. */
    fun validate(route: ApiRoute<*>): List<String>
}
