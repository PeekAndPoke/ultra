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
     * [member] is the name as it appears in the generated client — `codeGen { funcName = … }` when the
     * route declares one, otherwise the derived form. That is deliberate: it is the name a frontend
     * developer already reads in `api/…Client.ts`, so the declaration and the call site agree.
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

            "Feature '${feature.codeGenName}' has ${matches.size} endpoints named '$member': $where. " +
                    "Two route groups may legitimately share a member name, so pass the group to " +
                    "disambiguate: TsRouteRefs.of(feature, \"$member\", group = \"…\")."
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
