package io.peekandpoke.ultra.codegen.sdk

import io.peekandpoke.ultra.codegen.ts.TsClientSpec

/**
 * Where contributors declare entries that the BUILDER renders into one shared file.
 *
 * The emit-phase analogue of `TsTypeClaims`. `TsSdkOutput.file` is exclusive by design — one
 * contributor owns one path — which is right for a client module and wrong for an aggregate: the
 * router table needs a line from every contributor that ships a page, and none of them owns it.
 *
 * **Every insert is KEYED**, so contributor order is structurally irrelevant rather than merely
 * conventional. That matters because contributors arrive from a DI container, which guarantees no
 * ordering at all. Two contributors claiming the same key is a hard error naming both, never a
 * silent last-wins.
 *
 * The registry only COLLECTS. Rendering belongs to whoever owns the aggregate file, so the shape of
 * the emitted TypeScript stays in one place instead of being smeared across contributors.
 */
class TsSdkRegistry {

    /** A page route the app can mount. */
    data class Route(
        /** The URL path, e.g. `/insights`. Unique across the SDK — it is the key. */
        val path: String,
        /**
         * The component's path relative to the SDK root, e.g. `pages/insights/InsightsPage.vue`.
         *
         * The contributor must ALSO emit this file. Nothing here checks that: the registry has no
         * view of the output plan, and a missing component surfaces as a module-resolution error in
         * the consuming app's build. [TsSdkBuilder] cross-checks it instead, where both are visible.
         */
        val component: String,
        /**
         * Whether a session is required to reach this page.
         *
         * **Declared, not derived.** A contributor knows whether its page needs a session; deriving it
         * would mean asking the access matrix, which a logged-out visitor cannot fetch. That is the
         * whole reason page-route gating and API-route publicness are separate mechanisms.
         */
        val requiresAuth: Boolean,
        /** Optional navigation entry. `null` means the route exists but is not in any menu. */
        val nav: Nav?,
        /**
         * Other emitted files [component] imports and cannot work without, relative to the SDK root.
         *
         * **For files a DIFFERENT contributor emits.** A contributor already guarantees its own
         * output; what it cannot guarantee is a generated API client, because whether one exists
         * depends on a profile it never sees. `RestApiTsContributor` emits no client for a feature
         * whose routes were all filtered out, so a page importing that client shipped into an SDK
         * without it — TypeScript that cannot resolve its own imports, invisible to `vue-tsc` (the
         * app's `shims-vue.d.ts` wildcard resolves any `.vue`) and surfacing only as a `vite build`
         * failure in someone's frontend. Found by `/feature-review`, 2026-08-09.
         *
         * Checked by [TsSdkBuilder] after every contributor has run, which is the only point where
         * the full plan is visible — contributor order is undefined, so no contributor can check it
         * itself.
         *
         * **Named `requiresFiles`, not `requires`**, because [Nav.requires] is a different thing four
         * lines away in the same `route(...)` call: this is emitted FILES, that is API ROUTES. Both
         * reviewers in the 2026-08-24 gate read the pair as one concept that had moved. Renamed on
         * this side because it has no counterpart in the emitted TypeScript at all, so `Nav.requires`
         * and `SdkNavItem.requires` stay in step.
         */
        val requiresFiles: List<String> = emptyList(),
        val declaredBy: String,
    )

    /** A navigation entry pointing at its route. */
    data class Nav(
        val label: String,
        /** Icon name, interpreted by the app's icon set. `null` for none. */
        val icon: String? = null,
        /** Ascending. Ties break on path, so the emitted order is total and stable. */
        val order: Int = 0,
        /**
         * API routes the page cannot function without. **ALL must be accessible** for the entry to
         * appear; empty means the entry is never hidden on access grounds.
         *
         * **Declare the MINIMUM.** With one entry ALL and ANY coincide, which is the common case and
         * the one to aim for. A route the page merely DEGRADES without does not belong here — listing
         * it hides the whole page over a panel the user could have done without.
         *
         * **On the nav, not on [Route], deliberately.** This decides what a menu RENDERS and nothing
         * more: the router guard does not consult it, so a user who types the URL still reaches the
         * page and it 403s honestly. A redirect driven by a client-side ADVISORY matrix is the worse
         * failure mode — a matrix that failed to load, or a stale baked [ApiRouteRef.isPublic], would
         * lock a legitimate user out of a page the server would happily serve (maintainer, 2026-08-24).
         *
         * Not derivable, which is why it is declared: nothing in the emitted route table says which
         * endpoints a `.vue` file calls.
         */
        val requires: List<ApiRouteRef> = emptyList(),
    )

    /**
     * An API route a page needs, resolved at GENERATION time.
     *
     * Structurally the `RouteRef` in `ts/runtime/route.ts`, so `ApiAcl.canAccess` accepts one
     * unchanged — but `TsMountEmitter` re-declares the shape rather than importing it. Runtime
     * modules ship only when something imports them, and `mount.ts` is emitted unconditionally, so an
     * import would break every SDK that reaches no client at all.
     *
     * [method] and [uri] must be the pair the ACCESS MATRIX is keyed on. Whoever builds this owns
     * that; on the funktor side `TsRouteRefs` reads both off the live route graph, so no URI is ever
     * copied by hand into a contributor.
     */
    data class ApiRouteRef(
        /** Upper-case, and one of the methods `HttpMethod` in `ts/runtime/route.ts` lists. */
        val method: String,
        /**
         * The route PATTERN, placeholders included — `/users/{id}`, never a filled-in URL.
         *
         * That is the form the server puts in the matrix, so it is the only form that matches.
         */
        val uri: String,
        /**
         * True when the server's auth rules admit an ANONYMOUS caller.
         *
         * Carried because `ApiAcl.canAccess` SHORT-CIRCUITS on it: the matrix endpoint is itself
         * authenticated, so a logged-out visitor has no matrix and every requirement would otherwise
         * read as denied.
         *
         * **DERIVE it; never assert it.** This is the one field here that fails OPEN, and it cannot
         * be validated on this side — `ultra:codegen` has no route graph to check against. A wrong
         * `true` short-circuits `canAccess` before the matrix is consulted at all, so the entry
         * renders for every visitor including anonymous ones, and nothing anywhere notices. [method]
         * and [uri] are the opposite: wrong values match no matrix row, `getAccessLevel` falls back
         * to `Denied`, and the entry hides.
         *
         * On the funktor side `TsRouteRefs` evaluates the route's real rule chain. Anywhere else,
         * hand-writing `isPublic = true` is a decision to disable the gate.
         */
        val isPublic: Boolean,
    )

    /**
     * A stylesheet the app must load, and where it sits in the cascade.
     *
     * Separate from [Route] because the collision rule is the opposite one — see [addStyle].
     */
    data class Style(
        /** The sheet's path relative to the SDK root, e.g. `ui/theme.css`. Unique — it is the key. */
        val path: String,
        /**
         * Cascade position, ascending. Ties break on path, so the emitted order is total and stable.
         *
         * **This is semantic, not cosmetic.** A theme defines the custom properties a feature sheet
         * consumes, so loading them the other way round does not error — it silently drops the
         * overrides. Contributors arrive from a DI container in no defined order, so the order has to
         * be declared somewhere, and this is it.
         *
         * Convention: base/theme near 0, feature sheets in the hundreds.
         */
        val order: Int,
        val declaredBy: String,
    )

    private val routes = LinkedHashMap<String, Route>()
    private val styles = LinkedHashMap<String, Style>()

    /** Every route, ordered by path so the emitted file is stable across runs. */
    fun allRoutes(): List<Route> = routes.values.sortedBy { it.path }

    /** Every route carrying a nav entry, in menu order. */
    fun navRoutes(): List<Route> = routes.values
        .filter { it.nav != null }
        .sortedWith(compareBy({ it.nav!!.order }, { it.path }))

    /** Every stylesheet, in cascade order. */
    fun allStyles(): List<Style> = styles.values.sortedWith(compareBy({ it.order }, { it.path }))

    /** The API handed to a contributor during the emit phase. */
    fun scopeFor(contributor: String): Scope = Scope(contributor)

    private fun add(route: Route) {
        val existing = routes[route.path]

        check(existing == null) {
            "Route '${route.path}' is registered twice: by '${existing!!.declaredBy}' (as " +
                    "'${existing.component}') and by '${route.declaredBy}' (as '${route.component}'). " +
                    "A path may only be registered once — otherwise which component mounts depends on " +
                    "contributor order, which a DI container does not define. Fix: change one path, or " +
                    "register only one of the two contributors."
        }

        require(route.path.startsWith("/")) {
            "Route path '${route.path}' from '${route.declaredBy}' must start with '/'. A relative " +
                    "path resolves against whatever the app happens to be showing."
        }

        require(!route.component.startsWith("/") && !route.component.contains("..")) {
            "Route '${route.path}' from '${route.declaredBy}' points at component " +
                    "'${route.component}', which must be relative to the SDK root and must not " +
                    "escape it. The generator owns that directory and writes nothing outside it."
        }

        route.nav?.requires?.forEach { ref -> validateNavRequirement(route, ref) }

        routes[route.path] = route
    }

    /**
     * Rejects a nav requirement that could never match a matrix row.
     *
     * **Both checks fail SILENTLY at runtime if skipped**, which is why they are here rather than
     * left to review. `ApiAcl` keys on `"$method|$uri"` and falls back to `Denied` for anything it
     * has no row for — the mechanism by which the server transmits denial by OMISSION — so a
     * requirement that cannot match reads as denied for every user, and the menu entry disappears
     * for everyone with nothing anywhere naming the cause.
     *
     * **[ApiRouteRef.isPublic] is deliberately NOT checked, and it is the field that matters most.**
     * These two fail closed; that one fails open, and it is unverifiable here — this module has no
     * route graph. Whoever builds the ref owns it; see [ApiRouteRef.isPublic]. Raised in the
     * 2026-08-24 review gate.
     */
    private fun validateNavRequirement(route: Route, ref: ApiRouteRef) {
        // The method lands in a CLOSED union literal in `mount.ts`. Reusing the client emitter's set
        // rather than restating it: a second copy is exactly how the union and its guard drift apart.
        require(ref.method in TsClientSpec.Endpoint.KNOWN_HTTP_METHODS) {
            "Nav entry for route '${route.path}' (from '${route.declaredBy}') requires HTTP method " +
                    "'${ref.method}', which `HttpMethod` in runtime/route.ts does not list. It is " +
                    "emitted as a literal of that union, so it must be one of " +
                    "${TsClientSpec.Endpoint.KNOWN_HTTP_METHODS.joinToString()}."
        }

        require(ref.uri.startsWith("/")) {
            "Nav entry for route '${route.path}' (from '${route.declaredBy}') requires uri " +
                    "'${ref.uri}', which does not start with '/'. The access matrix is keyed on the " +
                    "route PATTERN as the server writes it, so a uri in any other form matches no " +
                    "row and hides the entry from every user."
        }
    }

    /**
     * Registers a stylesheet, or accepts an identical re-registration.
     *
     * **Deliberately NOT the collision rule [add] uses.** A route is owned by one contributor, so a
     * second claim on the same path is a bug. A stylesheet is the opposite: `ui/theme.css` is a
     * shared dependency, and every module shipping components that depend on it should be free to
     * say so — otherwise the app loads the theme only if the ONE contributor blessed to register it
     * happens to be in the profile. Identical registration therefore dedupes, exactly as
     * `TsSdkOutput.shared` dedupes identical content.
     *
     * A real disagreement — same sheet, different cascade position — still fails, because there is
     * no answer that is not arbitrary and the wrong one is silent.
     */
    private fun addStyle(style: Style) {
        require(!style.path.startsWith("/") && !style.path.contains("..")) {
            "Stylesheet '${style.path}' from '${style.declaredBy}' must be relative to the SDK root " +
                    "and must not escape it. The generator owns that directory and writes nothing " +
                    "outside it."
        }

        val existing = styles[style.path]

        if (existing != null) {
            check(existing.order == style.order) {
                "Stylesheet '${style.path}' is registered at conflicting cascade positions: order " +
                        "${existing.order} by '${existing.declaredBy}' and ${style.order} by " +
                        "'${style.declaredBy}'. Loading order decides which rules win, so there is no " +
                        "safe default to pick here. Fix: agree on one order, or split the sheet."
            }

            return
        }

        styles[style.path] = style
    }

    inner class Scope internal constructor(private val contributor: String) {

        /**
         * Registers a page route.
         *
         * The contributor is responsible for emitting [component] itself — typically with
         * `out.resource(...)` in the same `emit` call.
         */
        fun route(
            path: String,
            component: String,
            requiresAuth: Boolean,
            nav: Nav? = null,
            requiresFiles: List<String> = emptyList(),
        ) {
            add(
                Route(
                    path = path,
                    component = component,
                    requiresAuth = requiresAuth,
                    nav = nav,
                    requiresFiles = requiresFiles,
                    declaredBy = contributor,
                )
            )
        }

        /**
         * Registers a stylesheet the app must load.
         *
         * The contributor emits the file itself — typically `out.sharedResource(...)` for anything
         * another module might also want, `out.resource(...)` for its own.
         *
         * Registering the same sheet at the same [order] twice is FINE and deduplicates, so a module
         * may declare its dependency on a shared theme without coordinating over who owns the
         * registration.
         */
        fun style(path: String, order: Int) {
            addStyle(Style(path = path, order = order, declaredBy = contributor))
        }
    }
}
