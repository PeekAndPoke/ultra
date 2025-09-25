package de.peekandpoke.kraft.addons.routing

import de.peekandpoke.kraft.routing.Route
import kotlinx.html.FlowContent

/**
 * Represent the route that is currently selected as active route by a [Router]
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
    fun render(flow: FlowContent) = mountedRoute.view(flow, matchedRoute)
}
