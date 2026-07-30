package io.peekandpoke.funktor.rest

import io.peekandpoke.funktor.core.broker.InvalidRouteParamsException
import io.peekandpoke.funktor.core.broker.OutgoingConverter

/**
 * Boot check: every route parameter type must be handled by the [OutgoingConverter], so the route
 * can be rendered / linked. Extracted verbatim from the old inline `ValidateRoutesOnAppStarting`.
 */
class ConverterCompatBootCheck(
    private val converter: OutgoingConverter,
) : RouteBootCheck {
    override fun validate(route: ApiRoute<*>): List<String> =
        try {
            route.typedRoute.validateConverterCompatibility(converter)
            emptyList()
        } catch (e: InvalidRouteParamsException) {
            listOf(e.message ?: "Unknown route validation error")
        }
}
