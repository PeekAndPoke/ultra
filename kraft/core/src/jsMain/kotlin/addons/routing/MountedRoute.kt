package de.peekandpoke.kraft.addons.routing

import de.peekandpoke.kraft.routing.Route
import kotlinx.html.FlowContent

/**
 * Represents a [de.peekandpoke.kraft.routing.Route] that is mounted by a [Router]
 *
 * The [middlewares] are called in order once this [route] is matched and is about to become the [ActiveRoute].
 *
 * The [view] is the displayable content associated with the [route]
 */
data class MountedRoute(
    val route: Route,
    val middlewares: List<RouterMiddleware>,
    val view: FlowContent.(Route.Match) -> Unit,
) {
    companion object {
        val default = MountedRoute(Route.default, emptyList()) {}
    }
}
