package de.peekandpoke.kraft.addons.routing

import kotlinx.html.FlowContent

/**
 * Helper class for building a [Router]
 */
class RouterBuilder {

    private var enabled = true

    private val mounted = mutableListOf<MountedRoute>()

    private val middlewares = mutableListOf<RouterMiddleware>()

    fun build() = Router(mounted.toList(), enabled)

    /**
     * Creates a router that is enabled from the beginning
     */
    fun enable() {
        enabled = true
    }

    /**
     * Creates a router that is disabled from the beginning
     */
    fun disabled() {
        enabled = false
    }

    /**
     * Mounts a [route] and associates it with the given [view].
     *
     * The route is mounted with all [middlewares] that are defined in the current scope.
     */
    fun mount(route: Route, view: FlowContent.(Route.Match) -> Unit) {
        mounted.add(
            MountedRoute(route, middlewares.toList(), view)
        )
    }

    /**
     * Uses one or multiple [RouterMiddleware]s.
     *
     * All routes that are mounted inside the [scope] will be using the given [wares].
     *
     * The [using] function can be nested
     */
    fun using(vararg wares: RouterMiddleware, scope: RouterBuilder.() -> Unit) {
        // add them all to the scope
        middlewares.addAll(wares)
        // run the scope
        scope()
        // remove all middlewares from the scope
        repeat(wares.size) { middlewares.removeLast() }
    }

    /**
     * Mounts a fallback route that matches any pattern.
     */
    fun catchAll(view: FlowContent.(Route.Match) -> Unit) {
        mounted.add(
            MountedRoute(Pattern.CatchAll, middlewares.toList(), view)
        )
    }
}
