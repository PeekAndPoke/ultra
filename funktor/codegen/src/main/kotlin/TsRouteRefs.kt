package io.peekandpoke.funktor.codegen

import io.peekandpoke.funktor.rest.ApiFeature
import io.peekandpoke.funktor.rest.ApiRoute
import io.peekandpoke.ultra.codegen.sdk.TsSdkRegistry
import io.peekandpoke.ultra.security.user.User

/**
 * Resolves an endpoint of a live [ApiFeature] to the [TsSdkRegistry.ApiRouteRef] a nav entry gates on.
 *
 * Exists so that **no contributor ever hand-copies a URI**. The pair the access matrix is keyed on —
 * `method` and the route PATTERN — is already on the route graph the server built at startup, and it
 * is the same pair `RestApiTsContributor` emits into the generated client. Reading it here rather
 * than restating it turns a renamed route, a changed pattern or a deleted endpoint into a named
 * generation failure.
 *
 * The alternative was a literal `ApiRouteRef("GET", "/…")` in the contributor, and its failure mode is
 * the reason this file exists: the string simply stops matching any matrix row, `ApiAcl` answers
 * `Denied` for it — absence IS how the server transmits denial — and the menu entry disappears for
 * every user with nothing anywhere naming the cause.
 */
object TsRouteRefs {

    /**
     * The route ref for the endpoint of [feature] whose generated member is [member].
     *
     * [member] is the name the ROUTE GRAPH carries — `codeGen { funcName = … }` when the route
     * declares one, otherwise the form [TsClientNames.endpointMember] derives. Both sides call that
     * one function, so the naming rule cannot drift.
     *
     * **It is NOT proof the generated client has such a member.** This walks the unfiltered graph
     * while `RestApiTsContributor` emits from `group.all.filter(include)`, so a PROFILE that drops
     * this route while keeping others in the feature still resolves here — and the page then calls a
     * member the client does not carry. `Route.requiresFiles` catches a missing client FILE, not a
     * missing member. Raised in the 2026-08-24 gate; the cross-check needs the full output plan and
     * is tracked as `.claude/tasks/20260824-nav-requires-endpoint-crosscheck.md`.
     *
     * @param group narrows the search when two route groups of one feature share a member name. Only
     *   needed when the unqualified lookup reports an ambiguity — it says so, and names the groups.
     */
    fun of(feature: ApiFeature, member: String, group: String? = null): TsSdkRegistry.ApiRouteRef {
        val groups = feature.getRouteGroups().filter { group == null || it.name == group }

        val matches = groups.flatMap { g -> g.all.map { g.name to it } }
            .filter { (_, route) -> TsClientNames.endpointMember(route) == member }

        check(matches.isNotEmpty()) {
            val known = feature.getRouteGroups()
                .flatMap { g -> g.all.map { "${g.name}.${TsClientNames.endpointMember(it)}" } }
                .sorted()

            "Feature '${feature.codeGenName}' has no endpoint named '$member'" +
                    (group?.let { " in route group '$it'" } ?: "") +
                    ". A nav entry gates on it, so this would otherwise ship a menu item hidden from " +
                    "every user. Known endpoints: ${known.joinToString()}."
        }

        check(matches.size == 1) {
            val where = matches.joinToString { (g, r) -> "$g (${r.method.value} ${r.pattern.pattern})" }
            val groupNames = matches.map { it.first }.distinct()

            // `group` filters on the NAME, so it cannot separate two ApiRoutes INSTANCES that share
            // one — and this repo has that shape (`funktor:auth` declares `ApiRoutes("login")`
            // twice). Advising it there would send the caller round a loop that reproduces this exact
            // message. Raised in the 2026-08-24 gate.
            val advice = if (groupNames.size == 1) {
                "Both are in a route group named '${groupNames.single()}', so naming the group cannot " +
                        "separate them — they are two ApiRoutes INSTANCES sharing one name. Gate on a " +
                        "member unique to one of them, or rename a group."
            } else {
                "Two route groups may legitimately share a member name, so pass the group to " +
                        "disambiguate: TsRouteRefs.of(feature, \"$member\", group = \"…\")."
            }

            "Feature '${feature.codeGenName}' has ${matches.size} endpoints named '$member': $where. " +
                    advice
        }

        val route = matches.single().second

        return TsSdkRegistry.ApiRouteRef(
            method = route.method.value,
            uri = route.pattern.pattern,
            isPublic = route.isPublicToAnonymous(),
        )
    }
}

/**
 * True when this route's auth rules admit an ANONYMOUS caller.
 *
 * **An evaluation of the real chain, not an enumeration of rule kinds.** `estimateAccess` folds
 * every rule with `and`, so composition, floors and nesting are handled by construction — and a
 * rule kind nobody has written yet is classified correctly without touching this function. That
 * is the whole reason not to mirror `forRole(...)` and friends into TypeScript: a second
 * permission engine would need updating for each new rule, and would ship the authorization
 * model in a public bundle.
 *
 * Evaluable at generation time because `AuthRule.EstimateCtx` holds only a `User` — no params, no
 * body, no live request (`funktor/rest/src/jvmMain/kotlin/auth/AuthRule.kt:144-146`). This is the
 * same evaluator the per-user matrix is built on, called in-process; it is NOT the
 * `getMyApiAccess` endpoint, which needs a running server and a session.
 *
 * Deliberately NOT `ApiRoute.isSoleConstantChain()`: that matches only a single bare
 * `public()`/`forbidden()`, so it misreports `public() and something`.
 *
 * A top-level extension rather than a private helper because two callers need it: the client
 * emitter, which bakes it into every generated member, and [TsRouteRefs], which bakes it into a nav
 * requirement. The two must agree — `ApiAcl.canAccess` short-circuits on it — and one definition is
 * how that is guaranteed rather than hoped for.
 */
internal fun ApiRoute<*>.isPublicToAnonymous(): Boolean =
    !estimateAccess(User.anonymous).isDenied()
