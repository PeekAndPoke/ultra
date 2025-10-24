package de.peekandpoke.kraft.routing

import de.peekandpoke.ultra.html.renderFn
import kotlinx.html.FlowContent

/**
 * Represent the route currently selected as active route by a [Router]
 */
data class ActiveRoute(
    val uri: String,
    val matchedRoute: Route.Match,
    val mountedRoute: MountedRoute,
) {
    /** Shortcut for getting the pattern of the route */
    val pattern: String = matchedRoute.pattern

    /** Shortcut for getting the route definition */
    val route: Route = matchedRoute.route

    /**
     * Renders the content which is associated with the [mountedRoute]
     */
    fun render(flow: FlowContent) {
        // Get all the layouts that this route is embedded in
        val layouts = mountedRoute.layouts
        var layoutsPtr = layouts.lastIndex

        // Start by pointing to the view of the
        var render = renderFn { mountedRoute.view(this, matchedRoute) }

        // Wrap the view with every layout that was defined as parents of the route
        while (layoutsPtr >= 0) {
            val wrapper = layouts[layoutsPtr--]
            // We need to keep a reference to the current render method
            val current = render
            // Update the render function with the wrapper
            render = renderFn { wrapper.layout(this, current) }
        }

        render(flow)
    }
}
