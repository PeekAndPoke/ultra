package de.peekandpoke.kraft.addons.routing

/**
 * A [RouterMiddleware] is just a function with [RouterMiddlewareContext] as the receiver
 */
typealias RouterMiddleware = RouterMiddlewareContext.() -> Unit

/**
 * Helper for defining a router middleware
 */
fun routerMiddleware(func: RouterMiddleware): RouterMiddleware = func

/**
 * The context for all middle wares
 */
class RouterMiddlewareContext(
    val router: Router,
    val uri: String,
    val match: Route.Match,
) {
    var redirectToUri: String? = null
        private set

    fun redirectTo(uri: String) {
        redirectToUri = uri
    }
}
