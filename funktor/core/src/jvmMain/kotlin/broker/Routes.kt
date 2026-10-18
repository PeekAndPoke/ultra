package io.peekandpoke.funktor.core.broker

import io.peekandpoke.ultra.reflection.TypeRef
import io.peekandpoke.ultra.reflection.kType

/**
 * Base class for all route collections
 */
abstract class Routes(
    /**
     * A URL prefix prepended to every route this collection declares.
     *
     * **Never usable for API routes, and `ApiRoutes` therefore does not expose it.** An API route is
     * declared by a `TypedApiEndpoint` that the CLIENT owns too — the same object in `commonMain`
     * builds the server's pattern and the client's request URL. A prefix applied only on the server
     * is invisible to the client, which then calls the unprefixed path and gets a 404, and whose
     * access-matrix lookups miss for the same reason. No value can be correct, so the parameter is
     * simply absent there.
     *
     * It is sound HERE because a plain route collection has no client-side counterpart to diverge
     * from — `InsightsGuiRoutes : Routes("/_")` is the intended use.
     */
    val mountPoint: String = "",
) {
    /**
     * Creates a plain route without params or body
     */
    fun route(uri: String) =
        TypedRoute.Plain(
            pattern = uri.asPattern
        )

    /**
     * Create a route with params
     */
    fun <PARAMS : Any> route(type: TypeRef<PARAMS>, uri: String): TypedRoute.WithParams<PARAMS> =
        TypedRoute.WithParams(
            paramsType = type,
            pattern = uri.asPattern
        )

    /**
     * Creates a route with params and body
     */
    fun <PARAMS : Any, BODY : Any> route(paramsType: TypeRef<PARAMS>, bodyType: TypeRef<BODY>, uri: String) =
        TypedRoute.WithParamsAndBody(
            paramsType = paramsType,
            bodyType = bodyType,
            pattern = uri.asPattern
        )

    /**
     * Reified version for creating a typed route with params
     */
    @Suppress("UNUSED_PARAMETER")
    inline fun <reified PARAMS : Any> route(uri: String, d1: Nothing? = null): TypedRoute.WithParams<PARAMS> =
        route(kType(), uri)

    /**
     * Reified version for creating a typed route with params and body
     */
    inline fun <reified PARAMS : Any, reified BODY : Any> route(uri: String) =
        route(kType<PARAMS>(), kType<BODY>(), uri)

    /**
     * Converts a string into an uri pattern while prepending the mountPoint
     */
    private val String.asPattern get() = UriPattern(mountPoint + this)
}
