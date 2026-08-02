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

        routes[route.path] = route
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
