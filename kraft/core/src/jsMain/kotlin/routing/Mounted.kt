package de.peekandpoke.kraft.routing

import kotlinx.html.FlowContent

/**
 * Marker interface for all types that can be mounted by a [Router]
 */
sealed interface Mounted

/**
 * Represents a [Route] that is mounted by a [Router]
 *
 * The [view] is the displayable content associated with the [route]
 */
data class MountedRoute(
    val route: Route,
    val parents: List<Mounted>,
    val view: FlowContent.(Route.Match) -> Unit,
) : Mounted {
    companion object {
        val default = MountedRoute(Route.default, emptyList()) {}
    }

    /** All middlewares wrapping this route */
    val middlewares: List<MountedMiddleware> = parents.filterIsInstance<MountedMiddleware>()

    /** All layouts wrapping this route */
    val layouts: List<MountedLayout> = parents.filterIsInstance<MountedLayout>()
}

/**
 * Represents a [RouterMiddlewareFn] that is mounted by a [Router]
 */
data class MountedMiddleware(
    val middleware: RouterMiddlewareFn,
) : Mounted

/**
 * Represents a [LayoutFn] that is mounted by a [Router]
 */
data class MountedLayout(
    val layout: LayoutFn,
) : Mounted
