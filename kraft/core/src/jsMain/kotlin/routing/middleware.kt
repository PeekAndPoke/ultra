package io.peekandpoke.kraft.routing

/**
 * A router middleware function
 */
typealias RouterMiddlewareFn = (RouterMiddlewareContext) -> RouterMiddlewareResult

/**
 * Helper for defining a router middleware
 */
fun routerMiddleware(func: RouterMiddlewareFn): RouterMiddlewareFn = func

/**
 * Context passed to a [RouterMiddlewareFn]
 */
data class RouterMiddlewareContext(
    val uri: String,
) {
    /** Returns a result that redirects the router to the given [uri]. */
    fun redirect(uri: String) = RouterMiddlewareResult.Redirect(uri)

    /** Returns a result that allows routing to continue normally. */
    fun proceed() = RouterMiddlewareResult.Proceed
}

/**
 * The result of a [RouterMiddlewareFn]
 */
sealed interface RouterMiddlewareResult {
    /**
     * When [Proceed] is returned, the routing will continue normally
     */
    object Proceed : RouterMiddlewareResult

    /**
     * When [Redirect] is returned, the current routing be stepped and a redirect to [uri] will happen
     */
    data class Redirect(
        val uri: String,
    ) : RouterMiddlewareResult
}
