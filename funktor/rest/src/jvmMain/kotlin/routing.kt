@file:Suppress("MoveVariableDeclarationIntoWhen")

package io.peekandpoke.funktor.rest

import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.ktor.util.AttributeKey
import io.peekandpoke.funktor.core.broker.convertIncomingParameters
import io.peekandpoke.funktor.core.kontainer
import io.peekandpoke.funktor.core.user
import io.peekandpoke.funktor.rest.auth.AuthRule
import io.peekandpoke.funktor.rest.auth.HideFailureAsNotFound
import io.peekandpoke.ultra.common.TypedAttributes
import io.peekandpoke.ultra.common.TypedKey
import io.peekandpoke.ultra.security.user.UserPermissions

/**
 * Two-phase authorization (part 3 of the auth-hardening quartet).
 *
 * Every route is evaluated in two passes AROUND param conversion — because conversion runs
 * `IncomingVaultConverter.findById(...)`, a DB read, and its miss throws `NotFoundException`:
 *
 * 1. **Phase 1 — caller-only rules** ([ApiRoute.phase1Denials]) run FIRST, before any param/body is
 *    read. The mandatory group floor is caller-only, so an unauthorized caller is rejected here with
 *    ZERO entity loads — no pre-auth DB reads, and (crucially) a 401 that is identical whether the
 *    requested id exists or not, closing the 404-vs-401 existence oracle.
 * 2. **Conversion** — params/body materialized. Only reachable by callers who passed phase 1.
 * 3. **Phase 2 — param-dependent rules** ([ApiRoute.checkParamPhase]) run with the resolved params.
 *    A failure of a [HideFailureAsNotFound] rule (the framework consistency / caller-binding rules)
 *    throws the SAME `NotFoundException` a missing entity throws — a byte-identical 404.
 */

/**
 * Carries an [ApiRoute]'s [TypedAttributes] onto the ktor route it was mounted as.
 *
 * `ApiRoute` is a build-time object captured in a handler closure — nothing about it is reachable from
 * an [ApplicationCall]. ktor's own `Route.attributes` is public and lives for the application's
 * lifetime, so one entry there makes the whole bag readable at request time via
 * `(call as? RoutingPipelineCall)?.route?.attributes`.
 *
 * The bag is carried WHOLE under a single key rather than translated entry by entry: funktor's
 * [TypedKey] and ktor's [AttributeKey] are unrelated types with no shared registry, so a per-key
 * mapping would need a translation table. This way every future request-time attribute is free.
 */
val FunktorRouteAttributes = AttributeKey<TypedAttributes>("funktor.route.attributes")

/**
 * Dispatches to the correct handler based on the [ApiRoute] variant (plain, params, body, SSE).
 *
 * Returns the mounted ktor route — the method-selector node that request resolution lands on, and the
 * one carrying [FunktorRouteAttributes]. Returned so a caller (and a test) can assert against the exact
 * node `insightsOptions()` reads, rather than against the subtree it sits in.
 */
fun <RESPONSE> Route.handle(route: ApiRoute<RESPONSE>): Route {
    val mounted = when (route) {
        is ApiRoute.Plain<*> -> handlePlain(route)
        is ApiRoute.WithParams<*, *> -> handleWithParams(route)
        is ApiRoute.WithBody<*, *> -> handleWithBody(route)
        is ApiRoute.WithBodyAndParams<*, *, *> -> handleWithBodyAndParams(route)
        is ApiRoute.Sse<*> -> handleSse(route)
    }

    mounted.attributes.put(FunktorRouteAttributes, route.attributes)

    return mounted
}

/**
 * Phase-1 gate: evaluates the route's caller-only rules BEFORE any param/body is materialized.
 * Returns true when access is still granted; on denial it has already responded 401 and the caller
 * must stop — no entity is ever loaded for a caller rejected here.
 */
@PublishedApi
internal suspend fun RoutingContext.passesPhase1(route: ApiRoute<*>, uri: String): Boolean {
    val denials = route.phase1Denials(AuthRule.EstimateCtx.of(call.user))
    if (denials.isEmpty()) return true
    call.apiRespondUnauthorized<Any?>(route.method, uri, denials)
    return false
}

/**
 * Phase-2 outcome: runs [onGranted] when no param-dependent rule failed; otherwise fails closed. A
 * failed rule marked [HideFailureAsNotFound] throws the same `NotFoundException` a missing entity
 * would — a byte-identical 404 that leaks nothing about existence or ownership. Any other failure
 * responds 401.
 */
/**
 * Runs every registered phase-2 [RouteParamsGuard] (from the request kontainer) against the resolved
 * [params] + [permissions]. Returns true if any guard denies — the caller then throws the shared
 * `NotFoundException` (404, byte-identical to not-found). Guards run only on param-bearing routes;
 * `Unit`/null params abstain. Not a [RoutingContext] extension so the SSE path (a `ServerSSESession`)
 * can reuse it — hence the explicit [call].
 */
private fun deniedByGuards(call: ApplicationCall, params: Any?, permissions: UserPermissions): Boolean {
    if (params == null || params == Unit) return false
    return call.kontainer.getAll(RouteParamsGuard::class).any {
        it.guard(params, permissions) == GuardVerdict.DenyAsNotFound
    }
}

@PublishedApi
internal suspend fun RoutingContext.dispatchPhase2(
    route: ApiRoute<*>,
    uri: String,
    failed: List<AuthRule<*, *>>,
    onGranted: suspend RoutingContext.() -> Unit,
) {
    when {
        failed.isEmpty() -> onGranted()
        failed.any { it is HideFailureAsNotFound } -> throw NotFoundException()
        else -> call.apiRespondUnauthorized<Any?>(route.method, uri, failed)
    }
}

/**
 * Registers a handler for a [ApiRoute.Plain] that returns an ApiResponse
 */
fun <RESPONSE> Route.handlePlain(route: ApiRoute.Plain<RESPONSE>): Route {

    val uri = route.route.pattern.pattern

    return route(uri, route.method) {
        handle {
            // Phase 1 — caller-only rules
            if (!passesPhase1(route, uri)) return@handle
            // Phase 2 — param-dependent rules (no params here, but forCall {} rules may exist)
            val ctx: AuthRule.CheckCtx<Unit, Unit> = AuthRule.CheckCtx.plain(call)
            dispatchPhase2(route, uri, route.checkParamPhase(ctx).failedRules) {
                route.handler(this)
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
        // Same bridge as the dispatching `handle` above — without it a route mounted through this
        // overload silently ignores `noInsights()` and records in full.
        attributes.put(FunktorRouteAttributes, route.attributes)

        handle {
            // Phase 1 — caller-only rules
            if (!passesPhase1(route, uri)) return@handle
            // Phase 2 — param-dependent rules
            val ctx: AuthRule.CheckCtx<Unit, Unit> = AuthRule.CheckCtx.plain(call)
            dispatchPhase2(route, uri, route.checkParamPhase(ctx).failedRules) {
                call.respond(body())
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
        handle {
            // Phase 1 — caller-only rules, BEFORE param conversion (no findById yet)
            if (!passesPhase1(route, uri)) return@handle
            // Param conversion — reachable only past the floor
            val params: PARAMS = call.convertIncomingParameters(route.route)
            // Phase 2 — param-dependent rules
            val ctx: AuthRule.CheckCtx<PARAMS, Unit> = AuthRule.CheckCtx.paramsOnly(call = call, params = params)
            dispatchPhase2(route, uri, route.checkParamPhase(ctx).failedRules) {
                if (deniedByGuards(call, params, ctx.permissions)) throw NotFoundException()
                route.handler(this, params)
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
        handle {
            // Phase 1 — caller-only rules, BEFORE receiving/deserializing the body
            if (!passesPhase1(route, uri)) return@handle
            // Receive the request body — reachable only past the floor
            val bodyContent = call.receive<ByteArray>()
            // Awake the request body
            @Suppress("UNCHECKED_CAST")
            val bodyAwoken = restCodec
                .deserialize(route.bodyType.type, String(bodyContent)) as BODY
            // Phase 2 — body-dependent rules
            val ctx: AuthRule.CheckCtx<Unit, BODY> = AuthRule.CheckCtx.bodyOnly(call = call, body = bodyAwoken)
            dispatchPhase2(route, uri, route.checkParamPhase(ctx).failedRules) {
                route.handler(this, bodyAwoken)
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
        handle {
            // Phase 1 — caller-only rules, BEFORE param conversion / body receive
            if (!passesPhase1(route, uri)) return@handle
            // Param conversion + body receive — reachable only past the floor
            val params: PARAMS = call.convertIncomingParameters(route.route)
            val bodyContent = call.receive<ByteArray>()
            // Awake the request body
            @Suppress("UNCHECKED_CAST")
            val bodyAwoken = restCodec.deserialize(route.bodyType.type, String(bodyContent)) as BODY
            // Phase 2 — param/body-dependent rules
            val ctx: AuthRule.CheckCtx<PARAMS, BODY> =
                AuthRule.CheckCtx.paramsAndBody(call = call, params = params, body = bodyAwoken)
            dispatchPhase2(route, uri, route.checkParamPhase(ctx).failedRules) {
                if (deniedByGuards(call, params, ctx.permissions)) throw NotFoundException()
                route.handler(this, params, bodyAwoken)
            }
        }
    }
}

/**
 * Registers a handler for a [ApiRoute.Sse] starting a server sse session.
 *
 * Uses the same two-phase logic as the HTTP variants ([ApiRoute.phase1Denials] /
 * [ApiRoute.checkParamPhase]) but inlined, because the SSE session (`this`) is not a
 * [io.ktor.server.routing.RoutingContext] and cannot use the [passesPhase1]/[dispatchPhase2]
 * helpers. NOTE: the 401/404 here are emitted from INSIDE `sse { }`; delivering them cleanly assumes
 * the SSE response is not yet committed. SSE routes are dormant today — the ktor SSE plugin is not
 * installed in any funktor app, so they are unmountable — so this within-session delivery is
 * unexercised; validate it when SSE is actually adopted (rejecting before the session opens would be
 * the robust approach).
 */
fun <PARAMS> Route.handleSse(route: ApiRoute.Sse<PARAMS>): Route {

    val uri = route.route.pattern.pattern

    return sse(uri) {
        // SSE session
        val session = this
        // Phase 1 — caller-only rules, BEFORE param conversion
        val denials = route.phase1Denials(AuthRule.EstimateCtx.of(call.user))
        if (denials.isNotEmpty()) {
            call.apiRespondUnauthorized<Unit>(route.method, uri, denials)
            return@sse
        }
        // Param conversion — reachable only past the floor
        val params = call.convertIncomingParameters(route.route)
        // Phase 2 — param-dependent rules
        val ctx: AuthRule.CheckCtx<PARAMS, Unit> = AuthRule.CheckCtx.paramsOnly(call, params)
        val failed = route.checkParamPhase(ctx).failedRules
        when {
            failed.isEmpty() && deniedByGuards(call, params, ctx.permissions) -> throw NotFoundException()
            failed.isEmpty() -> route.handler(session, params)
            failed.any { it is HideFailureAsNotFound } -> throw NotFoundException()
            else -> call.apiRespondUnauthorized<Unit>(route.method, uri, failed)
        }
    }
}
