package de.peekandpoke.kraft.routing

import kotlinx.html.FlowContent

@RouterDsl
interface RouterBuilder {
    /**
     * Mounts a [route] and associates it with the given [view].
     */
    fun mount(route: Route, view: FlowContent.(Route.Match) -> Unit)

    /**
     * Mounts a fallback route that matches any pattern.
     */
    fun catchAll(view: FlowContent.(Route.Match) -> Unit)

    /**
     * Uses a [RouterMiddlewareFn].
     */
    fun middleware(middleware: RouterMiddlewareFn, scope: RouterBuilder.() -> Unit)

    /**
     * Uses a layout.
     */
    fun layout(layout: LayoutFn, scope: RouterBuilder.() -> Unit)
}

/**
 * Helper class for building a [Router]
 */
@RouterDsl
class RootRouterBuilder : RouterBuilder {

    private class RouterBuilderImpl(
        private val router: RootRouterBuilder,
        private val parents: List<Mounted>,
    ) : RouterBuilder {
        /**
         * Mounts a [route] and associates it with the given [view].
         */
        override fun mount(route: Route, view: FlowContent.(Route.Match) -> Unit) {
            router.mounted.add(
                MountedRoute(route = route, parents = parents, view = view)
            )
        }

        /**
         * Mounts a fallback route that matches any pattern.
         */
        override fun catchAll(view: FlowContent.(Route.Match) -> Unit) {
            router.mounted.add(
                MountedRoute(route = RoutePattern.CatchAll, parents = parents, view = view)
            )
        }

        /**
         * Uses a [RouterMiddlewareFn].
         */
        override fun middleware(middleware: RouterMiddlewareFn, scope: RouterBuilder.() -> Unit) {
            val builder = RouterBuilderImpl(
                router = router,
                parents = parents.plus(MountedMiddleware(middleware)),
            )

            builder.scope()
        }

        override fun layout(layout: LayoutFn, scope: RouterBuilder.() -> Unit) {
            val builder = RouterBuilderImpl(
                router = router,
                parents = parents.plus(MountedLayout(layout)),
            )

            builder.scope()
        }
    }

    private var enabled = true
    private var strategy: (Router) -> Router.RouterStrategy = { Router.HashRoutingStrategy(it) }

    private val mounted = mutableListOf<MountedRoute>()

    private val root = RouterBuilderImpl(router = this, parents = emptyList())

    fun build() = Router(
        mountedRoutes = mounted.toList(),
        strategyProvider = strategy,
        enabled = enabled,
    )

    /**
     * Creates a router that is enabled from the beginning
     */
    fun enabled() {
        enabled = true
    }

    /**
     * Creates a router that is disabled from the beginning
     */
    fun disabled() {
        enabled = false
    }

    /**
     * Sets the strategy for the router.
     */
    fun strategy(strategy: (Router) -> Router.RouterStrategy) {
        this.strategy = strategy
    }

    /**
     * Uses the path strategy for the router.
     */
    fun usePathStrategy() {
        strategy { Router.PathRoutingStrategy(it) }
    }

    /**
     * Uses the hash strategy for the router.
     */
    fun useHashStrategy() {
        strategy { Router.HashRoutingStrategy(it) }
    }

    /**
     * Mounts a [route] and associates it with the given [view].
     */
    override fun mount(route: Route, view: FlowContent.(Route.Match) -> Unit) {
        root.mount(route, view)
    }

    /**
     * Mounts a fallback route that matches any pattern.
     */
    override fun catchAll(view: FlowContent.(Route.Match) -> Unit) {
        root.catchAll(view)
    }

    /**
     * Uses a [RouterMiddlewareFn].
     */
    override fun middleware(middleware: RouterMiddlewareFn, scope: RouterBuilder.() -> Unit) {
        root.middleware(middleware = middleware, scope = scope)
    }

    /**
     * Uses a layout.
     */
    override fun layout(layout: LayoutFn, scope: RouterBuilder.() -> Unit) {
        root.layout(layout, scope)
    }
}
