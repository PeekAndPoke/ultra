package io.peekandpoke.ultra.codegen.sdk

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
        val declaredBy: String,
    )

    /** A navigation entry pointing at its route. */
    data class Nav(
        val label: String,
        /** Icon name, interpreted by the app's icon set. `null` for none. */
        val icon: String? = null,
        /** Ascending. Ties break on path, so the emitted order is total and stable. */
        val order: Int = 0,
    )

    private val routes = LinkedHashMap<String, Route>()

    /** Every route, ordered by path so the emitted file is stable across runs. */
    fun allRoutes(): List<Route> = routes.values.sortedBy { it.path }

    /** Every route carrying a nav entry, in menu order. */
    fun navRoutes(): List<Route> = routes.values
        .filter { it.nav != null }
        .sortedWith(compareBy({ it.nav!!.order }, { it.path }))

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

        routes[route.path] = route
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
        ) {
            add(
                Route(
                    path = path,
                    component = component,
                    requiresAuth = requiresAuth,
                    nav = nav,
                    declaredBy = contributor,
                )
            )
        }
    }
}
