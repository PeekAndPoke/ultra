package io.peekandpoke.funktor.rest

import io.ktor.server.application.*
import io.peekandpoke.funktor.core.broker.InvalidRouteParamsException
import io.peekandpoke.funktor.core.broker.OutgoingConverter
import io.peekandpoke.funktor.core.lifecycle.AppLifeCycleHooks
import io.peekandpoke.funktor.core.lifecycle.AppLifeCycleHooks.ExecutionOrder
import io.peekandpoke.funktor.core.lifecycle.AppStartException

/**
 * Validates that all route parameter types can be handled by the [OutgoingConverter] on app startup.
 */
class ValidateRoutesOnAppStarting(
    private val converter: OutgoingConverter,
    private val features: Lazy<List<ApiFeature>>,
) : AppLifeCycleHooks.OnAppStarting {

    override val executionOrder: ExecutionOrder = ExecutionOrder.VeryEarly

    override suspend fun onAppStarting(application: Application) {
        val errors = mutableListOf<String>()

        for (feature in features.value) {
            for (routeGroup in feature.getRouteGroups()) {
                for (route in routeGroup.all) {
                    try {
                        route.typedRoute.validateConverterCompatibility(converter)
                    } catch (e: InvalidRouteParamsException) {
                        errors.add(e.message ?: "Unknown route validation error")
                    }
                }
            }
        }

        if (errors.isNotEmpty()) {
            throw AppStartException(
                "Route converter validation failed:\n${errors.joinToString("\n") { "  - $it" }}"
            )
        }
    }
}
