@file:Suppress("MoveVariableDeclarationIntoWhen")

package de.peekandpoke.funktor.rest

import de.peekandpoke.funktor.core.broker.convertIncomingParameters
import de.peekandpoke.funktor.core.broker.handle
import de.peekandpoke.funktor.rest.auth.AuthRule
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*

fun <RESPONSE> Route.handle(route: ApiRoute<RESPONSE>) {
    when (route) {
        is ApiRoute.Plain<*> -> handlePlain(route)
        is ApiRoute.WithParams<*, *> -> handleWithParams(route)
        is ApiRoute.WithBody<*, *> -> handleWithBody(route)
        is ApiRoute.WithBodyAndParams<*, *, *> -> handleWithBodyAndParams(route)
        is ApiRoute.Sse<*> -> handleSse(route)
    }
}

/**
 * Registers a handler for a [ApiRoute.Plain] that returns an ApiResponse
 */
fun <RESPONSE> Route.handlePlain(route: ApiRoute.Plain<RESPONSE>): Route {

    val uri = route.route.pattern.pattern

    return route(uri, route.method) {
        handle(route.route) {
            // Create the authorization context
            val ctx: AuthRule.CheckCtx<Unit, Unit> = AuthRule.CheckCtx.plain(call)
            // Check authorization rules
            val authResult = route.checkAccess(ctx)
            // Respond accordingly
            when (authResult.isSuccess()) {
                true -> route.handler(this)
                false -> call.apiRespondUnauthorized<RESPONSE>(route.method, uri, authResult.failedRules)
            }
        }
    }
}

/**
 * Registers a handler for a [ApiRoute.Plain] that returns a raw response
 */
inline fun <reified RESULT : Any> Route.handle(
    route: ApiRoute.Plain<RESULT>,
    noinline body: suspend RoutingContext.() -> RESULT,
): Route {

    val uri = route.route.pattern.pattern

    return route(uri, route.method) {
        handle(route.route) {
            // Create the authorization context
            val ctx: AuthRule.CheckCtx<Unit, Unit> = AuthRule.CheckCtx.plain(call)
            // Check authorization rules
            val authResult = route.checkAccess(ctx)
            // Respond accordingly
            when (authResult.isSuccess()) {
                true -> call.respond(body())
                false -> call.apiRespondUnauthorized<RESULT>(route.method, uri, authResult.failedRules)
            }
        }
    }
}

/**
 * Registers a handler for a [ApiRoute.WithParams]
 */
fun <PARAMS, RESPONSE> Route.handleWithParams(
    route: ApiRoute.WithParams<PARAMS, RESPONSE>,
): Route {

    val uri = route.route.pattern.pattern

    return route(uri, route.method) {
        handle(route.route) { params: PARAMS ->
            // Create the authorization context
            val ctx: AuthRule.CheckCtx<PARAMS, Unit> = AuthRule.CheckCtx.paramsOnly(call = call, params = params)
            // Check authorization rules
            val authResult = route.checkAccess(ctx)
            // Respond accordingly
            when (authResult.isSuccess()) {
                true -> route.handler(this, params)
                false -> call.apiRespondUnauthorized<RESPONSE>(route.method, uri, authResult.failedRules)
            }
        }
    }
}

/**
 * Registers a handler for a [ApiRoute.WithBody]
 */
fun <BODY, RESPONSE> Route.handleWithBody(
    route: ApiRoute.WithBody<BODY, RESPONSE>,
): Route {

    val uri = route.route.pattern.pattern

    return route(uri, route.method) {

        handle(route.route) {
            // Receive the request body
            val bodyContent = call.receive<ByteArray>()
            // Awake the request body
            @Suppress("UNCHECKED_CAST")
            val bodyAwoken = restCodec
                .deserialize(route.bodyType.type, String(bodyContent)) as BODY
            // Create the authorization context
            val ctx: AuthRule.CheckCtx<Unit, BODY> = AuthRule.CheckCtx.bodyOnly(call = call, body = bodyAwoken)
            // Check authorization rules
            val authResult = route.checkAccess(ctx)
            // Respond accordingly
            when (authResult.isSuccess()) {
                true -> route.handler(this, bodyAwoken)
                false -> call.apiRespondUnauthorized<RESPONSE>(route.method, uri, authResult.failedRules)
            }
        }
    }
}

/**
 * Registers a handler for a [ApiRoute.WithBodyAndParams]
 */
fun <PARAMS, BODY, RESPONSE> Route.handleWithBodyAndParams(
    route: ApiRoute.WithBodyAndParams<PARAMS, BODY, RESPONSE>,
): Route {

    val uri = route.route.pattern.pattern

    return route(uri, route.method) {

        handle(route.route) { params: PARAMS ->
            // Receive the request body
            val bodyContent = call.receive<ByteArray>()

            // Awake the request body
            @Suppress("UNCHECKED_CAST")
            val bodyAwoken = restCodec.deserialize(route.bodyType.type, String(bodyContent)) as BODY

            // Create the authorization context
            val ctx: AuthRule.CheckCtx<PARAMS, BODY> =
                AuthRule.CheckCtx.paramsAndBody(call = call, params = params, body = bodyAwoken)

            // Check authorization rules
            val authResult = route.checkAccess(ctx)

            // Respond accordingly
            when (authResult.isSuccess()) {
                true -> route.handler(this, params, bodyAwoken)
                false -> call.apiRespondUnauthorized<RESPONSE>(route.method, uri, authResult.failedRules)
            }
        }
    }
}

/**
 * Registers a handler for a [ApiRoute.Sse] starting a server sse session
 */
fun <PARAMS> Route.handleSse(route: ApiRoute.Sse<PARAMS>) {

    val uri = route.route.pattern.pattern

    return sse(uri) {
        // SSE session
        val session = this
        // Incoming parameters
        val params = call.convertIncomingParameters(route.route)
        // Create the authorization context
        val ctx: AuthRule.CheckCtx<PARAMS, Unit> = AuthRule.CheckCtx.paramsOnly(call, params)
        // Check authorization rules
        val authResult = route.checkAccess(ctx)

        // Respond accordingly
        when (authResult.isSuccess()) {
            true -> route.handler(session, params)
            false -> call.apiRespondUnauthorized<Unit>(route.method, uri, authResult.failedRules)
        }
    }
}
