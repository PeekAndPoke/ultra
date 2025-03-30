package de.peekandpoke.ktorfx.core.broker

import de.peekandpoke.ultra.common.toUri
import de.peekandpoke.ultra.security.csrf.CsrfProtection
import io.ktor.http.*
import kotlin.reflect.full.declaredMemberProperties

/**
 * Renders uri from instances of [TypedRoute]
 */
class TypedRouteRenderer(
    private val converter: OutgoingConverter,
    private val csrf: CsrfProtection,
) {
    /**
     * Renders the uri for the given bound [route]
     */
    operator fun <T : Any> invoke(route: TypedRoute.Bound<T>): String = render(route.route, route.obj)

    /**
     * Renders the uri for the given bound [route]
     */
    fun <T : Any> render(route: TypedRoute.Bound<T>): String = render(route.route, route.obj)

    /**
     * Renders the uri for the given [route] and the given params [obj]
     */
    fun <T : Any> render(route: TypedRoute<T>, obj: T): String {

        var result = route.pattern.pattern

        val queryParams = mutableMapOf<String, String>()

        // Special handling for csrf tokens
        if (result.contains("{csrf}")) {
            result = result.replace("{csrf}", csrf.createToken(obj.toString()))
        }

        // replace route params or build up query parameters
        route.reifiedParamsType.cls.declaredMemberProperties.forEach {

            val converted =
                converter.convert(it.get(obj) ?: "", it.returnType).encodeURLQueryComponent(encodeFull = true)

            // replace the route param
            if (route.parsedUriParams.contains(it.name)) {
                result = result.replace("{${it.name}}", converted)
            } else {

                val itVal = it.get(obj)

                // only add to the query params if the value is non null or not an empty string
                if (itVal != null && itVal.toString().isNotEmpty()) {
                    queryParams[it.name] = converted
                }
            }
        }

        // finally append query params if there are any
        return result.toUri(queryParams)
    }
}
