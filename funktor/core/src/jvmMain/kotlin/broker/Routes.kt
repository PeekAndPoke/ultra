package de.peekandpoke.funktor.core.broker

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.common.reflection.kType

/**
 * Base class for all route collections
 */
abstract class Routes(
    val converter: OutgoingConverter,
    val mountPoint: String = "",
) {
    /**
     * Creates a plain route without params or body
     */
    fun route(uri: String) =
        TypedRoute.Plain(
            converter = converter,
            pattern = uri.asPattern
        )

    /**
     * Create a route with params
     */
    fun <PARAMS : Any> route(type: TypeRef<PARAMS>, uri: String): TypedRoute.WithParams<PARAMS> =
        TypedRoute.WithParams(
            converter = converter,
            paramsType = type,
            pattern = uri.asPattern
        )

    /**
     * Creates a route with params and body
     */
    fun <PARAMS : Any, BODY : Any> route(paramsType: TypeRef<PARAMS>, bodyType: TypeRef<BODY>, uri: String) =
        TypedRoute.WithParamsAndBody(
            converter = converter,
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
