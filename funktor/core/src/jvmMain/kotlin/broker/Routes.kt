package io.peekandpoke.funktor.core.broker

import io.peekandpoke.ultra.reflection.TypeRef
import io.peekandpoke.ultra.reflection.kType

/**
 * Base class for all route collections. A route's pattern is its uri as written: there is no prefix, because an API
 * route's endpoint is shared with the client, and a server-side prefix would be invisible to it.
 */
abstract class Routes {
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

    /** Converts a string into an uri pattern */
    private val String.asPattern get() = UriPattern(this)
}
