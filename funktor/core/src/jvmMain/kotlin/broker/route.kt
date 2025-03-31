package de.peekandpoke.funktor.core.broker

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*

suspend fun <PARAMS> ApplicationCall.convertIncomingParameters(route: TypedRoute<PARAMS>): PARAMS {
    @Suppress("UNCHECKED_CAST")
    return incomingConverter.convert(
        parameters,
        request.queryParameters,
        route.reifiedParamsType
    ) as PARAMS
}

/**
 * Handles a [TypedRoute]
 */
fun <T> Route.handle(route: TypedRoute<T>, body: suspend RoutingContext.(T) -> Unit) {
    handle {
        @Suppress("UNCHECKED_CAST")
        body(
            call.convertIncomingParameters(route)
        )
    }
}

/**
 * Registers a [TypedRoute] with [HttpMethod.Get]
 */
fun <T : Any> Route.get(route: TypedRoute<T>, body: suspend RoutingContext.(T) -> Unit): Route {
    return route(route.pattern.pattern, HttpMethod.Get) { handle(route, body) }
}

/**
 * Registers a [TypedRoute] with [HttpMethod.Get]
 */
fun <T : Any> Route.post(route: TypedRoute<T>, body: suspend RoutingContext.(T) -> Unit): Route {
    return route(route.pattern.pattern, HttpMethod.Post) { handle(route, body) }
}

/**
 * Registers a [TypedRoute] with [HttpMethod.Get] and [HttpMethod.Post]
 */
fun <T : Any> Route.getOrPost(route: TypedRoute<T>, body: suspend RoutingContext.(T) -> Unit): Route {
    return route(route.pattern.pattern) {
        method(HttpMethod.Get) { handle(route, body) }
        method(HttpMethod.Post) { handle(route, body) }
    }
}

/**
 * Registers a fallback route for mostly all http methods
 *
 * Methods that will NOT be registered:
 * - [HttpMethod.Head]
 *
 * Methods that will be registered:
 * - [HttpMethod.Get]
 * - [HttpMethod.Post]
 * - [HttpMethod.Put]
 * - [HttpMethod.Patch]
 * - [HttpMethod.Delete]
 * - [HttpMethod.Options]
 */
fun Route.fallback(body: RoutingHandler) {

    val uri = "/{...}"

    get(uri, body)
    post(uri, body)
    put(uri, body)
    patch(uri, body)
    delete(uri, body)
    options(uri, body)
}

