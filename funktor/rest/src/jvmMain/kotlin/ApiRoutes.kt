package io.peekandpoke.funktor.rest

import io.ktor.http.*
import io.ktor.http.HttpMethod.Companion.Delete
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.HttpMethod.Companion.Put
import io.peekandpoke.funktor.core.broker.ConsistentParam
import io.peekandpoke.funktor.core.broker.Routes
import io.peekandpoke.funktor.core.broker.TypedRoute
import io.peekandpoke.funktor.core.broker.UriPattern
import io.peekandpoke.funktor.rest.auth.AuthRule
import io.peekandpoke.funktor.rest.auth.AuthRuleBuilder.Companion.validateChain
import io.peekandpoke.funktor.rest.auth.ConsistentParamRule
import io.peekandpoke.funktor.rest.auth.FloorAuthRuleBuilder
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.remote.ApiResponse
import io.peekandpoke.ultra.remote.TypedApiEndpoint
import kotlin.reflect.KClass
import java.util.Collections

/**
 * The single DSL marker for the route-building DSL receivers: [ApiRoute] (the mount-chain
 * receiver, covering all its variants), [ApiRoutes.RouteBuilder], the docs/codeGen/security
 * `Builder` classes, and the auth-rule builders.
 *
 * NOTE: `@DslMarker` only has an effect when the RECEIVER TYPES are annotated — annotating
 * functions does nothing (the previous `RestDslMarker*` annotations sat on functions and were
 * decorative). With these receivers sharing one marker, implicit access to an OUTER receiver
 * from a nested block is a compile error: `docs {}` inside `authorize {}` fails to compile, as
 * does the root-only `public()` inside `forAny {}` — the two silent-wrong-level traps this DSL
 * had. Deliberately NOT marked: the enclosing [ApiRoutes] feature classes (routes are declared
 * in class bodies, not marked lambdas) and lambdas whose receivers belong to other families
 * (ktor's RoutingContext in `handle {}`, the rule-evaluation contexts in `forCall {}`).
 */
@DslMarker
annotation class RestDsl

/**
 * Base class for creating api routes.
 *
 * Every group MUST declare an [authFloor] — the minimal auth every route in the group
 * inherits as the INITIAL state of its rule chain (structural default-deny). A per-route
 * `authorize {}` can only ADD to the floor (strengthen), never clear it; a genuinely public group
 * declares `authFloor = { public() }`. The floor is caller-only by construction (see
 * [FloorAuthRuleBuilder]) and is materialized + validated once, here, at group construction.
 */
abstract class ApiRoutes(
    val name: String,
    authFloor: FloorAuthRuleBuilder.() -> Unit,
    // A group of API routes has NO mount point and cannot have one — see the KDoc on
    // `Routes.mountPoint`. An `ApiRoutes` route is declared by a `TypedApiEndpoint` the CLIENT owns
    // too, and a server-side prefix is invisible to it.
) : Routes(mountPoint = "") {

    /** list with all registered routes */
    private val allRoutes = mutableListOf<ApiRoute<*>>()

    /**
     * The group's floor rules — the initial auth chain prepended to every route. Materialized and
     * validated (non-empty, constant-soleness, no empty composites) at construction, so a missing
     * or malformed floor aborts app start.
     */
    @PublishedApi
    internal val floorRules: List<AuthRule<Any?, Any?>> =
        FloorAuthRuleBuilder().apply(authFloor).build(name)

    val routeBuilder = RouteBuilder()

    /**
     * All registered routes.
     *
     * An UNMODIFIABLE VIEW, built once — not a `get()` that copies per access, and not the backing
     * list itself. `ApiAccessDescriptor` walks every group's routes on every matrix build, so the old
     * per-access copy allocated once per group per sign-in for no benefit; but handing back
     * `allRoutes` directly is WORSE than the copy was, because Kotlin's `List` is not runtime-
     * immutable and a caller can cast it back and mutate a live group.
     *
     * A view rather than a one-shot copy so it cannot go stale if a route is ever added after first
     * access — routes are only appended during construction today, and that should not be something
     * this property silently depends on.
     */
    val all: List<ApiRoute<*>> = Collections.unmodifiableList(allRoutes)

    /** Registers a route through the single floor-applying choke point [addRoute]. */
    fun <RESULT, ROUTE : ApiRoute<RESULT>> route(block: RouteBuilder.() -> ROUTE): ApiRoute<RESULT> =
        addRoute(routeBuilder.block())

    /**
     * Mounts a typed get api endpoint
     */
    inline fun <reified PARAMS : Any, reified RESPONSE> TypedApiEndpoint.Delete<RESPONSE>.mount(
        @Suppress("UNUSED_PARAMETER", "unused")
        paramsCls: KClass<PARAMS>,
        block: ApiRoute.WithParams<PARAMS, RESPONSE>.() -> ApiRoute.WithParams<PARAMS, RESPONSE>,
    ): ApiRoute.WithParams<PARAMS, RESPONSE> {
        return routeBuilder
            .delete<PARAMS, RESPONSE>(uri)
            .withAttributes(attributes)
            .block()
            .let { addRoute(it) }
    }

    /**
     * Mounts a typed get api endpoint
     */
    inline fun <reified RESPONSE> TypedApiEndpoint.Get<RESPONSE>.mount(
        block: ApiRoute.Plain<RESPONSE>.() -> ApiRoute.Plain<RESPONSE>,
    ): ApiRoute.Plain<RESPONSE> {
        return routeBuilder
            .get<RESPONSE>(uri)
            .withAttributes(attributes)
            .block()
            .let { addRoute(it) }
    }

    /**
     * Mounts a typed get api endpoint
     */
    inline fun <reified PARAMS : Any, reified RESPONSE> TypedApiEndpoint.Get<RESPONSE>.mount(
        @Suppress("UNUSED_PARAMETER", "unused")
        paramsCls: KClass<PARAMS>,
        block: ApiRoute.WithParams<PARAMS, RESPONSE>.() -> ApiRoute.WithParams<PARAMS, RESPONSE>,
    ): ApiRoute.WithParams<PARAMS, RESPONSE> {
        return routeBuilder
            .get<PARAMS, RESPONSE>(uri)
            .withAttributes(attributes)
            .block()
            .let { addRoute(it) }
    }

    /**
     * Mounts a typed get api endpoint
     */
    inline fun <reified PARAMS : Any> TypedApiEndpoint.Sse.mount(
        @Suppress("UNUSED_PARAMETER", "unused")
        paramsCls: KClass<PARAMS>,
        block: ApiRoute.Sse<PARAMS>.() -> ApiRoute.Sse<PARAMS>,
    ): ApiRoute.Sse<PARAMS> {
        return routeBuilder
            .sse<PARAMS>(uri)
            .withAttributes(attributes)
            .block()
            .let { addRoute(it) }
    }

    /**
     * Mounts a typed get api endpoint
     */
    inline fun <reified BODY, reified RESPONSE> TypedApiEndpoint.Post<BODY, RESPONSE>.mount(
        block: ApiRoute.WithBody<BODY, RESPONSE>.() -> ApiRoute.WithBody<BODY, RESPONSE>,
    ): ApiRoute.WithBody<BODY, RESPONSE> {
        return routeBuilder
            .post<BODY, RESPONSE>(uri)
            .withAttributes(attributes)
            .block()
            .let { addRoute(it) }
    }

    /**
     * Mounts a typed get api endpoint
     */
    inline fun <reified PARAMS : Any, reified BODY, reified RESPONSE> TypedApiEndpoint.Post<BODY, RESPONSE>.mount(
        @Suppress("UNUSED_PARAMETER", "unused")
        paramsCls: KClass<PARAMS>,
        block: ApiRoute.WithBodyAndParams<PARAMS, BODY, RESPONSE>.() -> ApiRoute.WithBodyAndParams<PARAMS, BODY, RESPONSE>,
    ): ApiRoute.WithBodyAndParams<PARAMS, BODY, RESPONSE> {
        return routeBuilder
            .post<PARAMS, BODY, RESPONSE>(uri)
            .withAttributes(attributes)
            .block()
            .let { addRoute(it) }
    }

    /**
     * Mounts a typed get api endpoint
     */
    inline fun <reified BODY, reified RESPONSE> TypedApiEndpoint.Put<BODY, RESPONSE>.mount(
        block: ApiRoute.WithBody<BODY, RESPONSE>.() -> ApiRoute.WithBody<BODY, RESPONSE>,
    ): ApiRoute.WithBody<BODY, RESPONSE> {
        return routeBuilder
            .put<BODY, RESPONSE>(uri)
            .withAttributes(attributes)
            .block()
            .let { addRoute(it) }
    }

    /**
     * Mounts a typed get api endpoint
     */
    inline fun <reified PARAMS : Any, reified BODY, reified RESPONSE> TypedApiEndpoint.Put<BODY, RESPONSE>.mount(
        @Suppress("UNUSED_PARAMETER", "unused")
        paramsCls: KClass<PARAMS>,
        block: ApiRoute.WithBodyAndParams<PARAMS, BODY, RESPONSE>.() -> ApiRoute.WithBodyAndParams<PARAMS, BODY, RESPONSE>,
    ): ApiRoute.WithBodyAndParams<PARAMS, BODY, RESPONSE> {
        return routeBuilder
            .put<PARAMS, BODY, RESPONSE>(uri)
            .withAttributes(attributes)
            .block()
            .let { addRoute(it) }
    }

    /**
     * The SINGLE registration choke point. Applies the group [floorRules] itself (so a route can
     * NEVER be registered without its floor — floor-presence is structural, not disciplinary),
     * then validates the whole combined chain:
     * - **non-empty** — an empty chain would serve PUBLIC (the dispatch grants when no rule fails);
     *   post-floor every route has ≥1 rule, so this only ever fires if the floor was somehow
     *   bypassed;
     * - **constant-soleness / no empty composites** (`validateChain`) — a floor/route conflict
     *   (e.g. a public-floored group whose route adds a restrictive rule) fails here at construction.
     *
     * `@PublishedApi internal` so the public inline `mount` overloads can call it; it is NOT public
     * API — external code cannot register a route, so it cannot bypass the floor.
     */
    @PublishedApi
    internal fun <RESPONSE, ROUTE : ApiRoute<RESPONSE>> addRoute(route: ROUTE): ROUTE {
        @Suppress("UNCHECKED_CAST")
        val floored = route.withFloor(floorRules) as ROUTE

        // Append the interface-triggered phase-2 auto-rules (referential consistency + caller-org
        // binding). Structural, never author-declared — they cannot be forgotten.
        val autoRules = paramAutoRules(floored)
        @Suppress("UNCHECKED_CAST")
        val withAuto = (if (autoRules.isEmpty()) floored else floored.withAppendedRules(autoRules)) as ROUTE

        val at = "Route '${withAuto.method.value} ${withAuto.pattern.pattern}'"

        check(withAuto.authRules.isNotEmpty()) {
            "$at ended up with no auth rules even though the group floor is non-empty — internal " +
                    "invariant violation (the floor must be prepended to EVERY route). A normal " +
                    "ApiRoutes group cannot cause this; if you see it, a framework registration path " +
                    "bypassed withFloor — please report it."
        }
        validateChain("$at auth chain", withAuto.authRules)

        allRoutes.add(withAuto)
        return withAuto
    }

    /**
     * The phase-2 auth rules the framework auto-appends to [route] based on its params type —
     * [ConsistentParamRule] when the params opt into [ConsistentParam]. Interface-triggered, so once
     * a params type declares the interface the check can never be forgotten.
     *
     * Skipped for a sole-constant (public/forbidden) chain: a constant must remain the SOLE rule of
     * its chain (`validateChain`). Cross-org isolation is NOT handled here — it is a saas
     * [RouteParamsGuard] run at request time (org semantics live in saas, not the REST core).
     */
    private fun paramAutoRules(route: ApiRoute<*>): List<AuthRule<*, *>> {
        if (route.isSoleConstantChain()) return emptyList()

        val paramsCls = route.typedRoute.reifiedParamsType.cls.java

        return buildList {
            if (ConsistentParam::class.java.isAssignableFrom(paramsCls)) add(ConsistentParamRule())
        }
    }

    @RestDsl
    class RouteBuilder {

        ////  GET  ////////////////////////////////////////////////////////////////////////////////////////////////

        /**
         * Creates a plain [HttpMethod.Get] route without parameters.
         *
         * A handler for this route must return a [ApiResponse] with data of type [RESPONSE].
         */
        inline fun <reified RESPONSE> get(uri: String): ApiRoute.Plain<RESPONSE> {
            return routePlain(Get, uri)
        }

        /**
         * Creates a [HttpMethod.Get] route with request params of type [PARAMS]
         *
         * A handler for this route must return a [ApiResponse] with data of type [RESPONSE].
         *
         * Notice: the param [d1] is a dummy for tricking the Kotlin compiler to accept the overload.
         */
        @Suppress("UNUSED_PARAMETER")
        inline fun <reified PARAMS, reified RESPONSE> get(
            uri: String,
            d1: Nothing? = null,
        ): ApiRoute.WithParams<PARAMS, RESPONSE> {
            return routeParams(Get, uri)
        }

        ////  SSE  ////////////////////////////////////////////////////////////////////////////////////////////////

        /**
         * Creates am SSE route without parameters.
         *
         * A handler for this route must return a [ApiResponse] with params of type [PARAMS].
         */
        inline fun <reified PARAMS> sse(uri: String): ApiRoute.Sse<PARAMS> {
            return routeSse(uri)
        }

        ////  POST  ///////////////////////////////////////////////////////////////////////////////////////////////

        /**
         * Create a plain [HttpMethod.Post] route without request parameters or request body.
         *
         * A handler for this route must return a [ApiResponse] with data of type [RESPONSE].
         */
        inline fun <reified RESPONSE> post(uri: String): ApiRoute.Plain<RESPONSE> {
            return routePlain(Post, uri)
        }

        /**
         * Creates a [HttpMethod.Post] route with request body of type [BODY]
         *
         * A handler for this route must return a [ApiResponse] with data of type [RESPONSE].
         *
         * Notice: the param [d1] is a dummy for tricking the Kotlin compiler to accept the overload.
         */
        @Suppress("UNUSED_PARAMETER")
        inline fun <reified BODY, reified RESPONSE> post(
            uri: String,
            d1: Nothing? = null,
        ): ApiRoute.WithBody<BODY, RESPONSE> {
            return routeBody(Post, uri)
        }

        /**
         * Creates a [HttpMethod.Post] route with request parameters of type [PARAMS] and body of type [BODY]
         *
         * A handler for this route must return a [ApiResponse] with data of type [RESPONSE].
         *
         * Notice: the params [d1] and [d2] are dummies for tricking the Kotlin compiler to accept the overload.
         */
        @Suppress("UNUSED_PARAMETER")
        inline fun <reified PARAMS, reified BODY, reified RESPONSE> post(
            uri: String,
            d1: Nothing? = null,
            d2: Nothing? = null,
        ): ApiRoute.WithBodyAndParams<PARAMS, BODY, RESPONSE> {
            return routeParamsBody(Post, uri)
        }

        ////  PUT  ////////////////////////////////////////////////////////////////////////////////////////////////

        /**
         * Creates a [HttpMethod.Put] route with request body of type [BODY]
         *
         * A handler for this route must return a [ApiResponse] with data of type [RESPONSE].
         *
         * Notice: the param [d1] is a dummy for tricking the Kotlin compiler to accept the overload.
         */
        @Suppress("UNUSED_PARAMETER")
        inline fun <reified BODY, reified RESPONSE> put(
            uri: String,
            d1: Nothing? = null,
        ): ApiRoute.WithBody<BODY, RESPONSE> {
            return routeBody(Put, uri)
        }

        /**
         * Creates a [HttpMethod.Put] route with request params of type [PARAMS] and body of type [BODY]
         *
         * A handler for this route must return a [ApiResponse] with data of type [RESPONSE].
         */
        inline fun <reified PARAMS, reified BODY, reified RESPONSE> put(uri: String): ApiRoute.WithBodyAndParams<PARAMS, BODY, RESPONSE> {
            return routeParamsBody(Put, uri)
        }

        ////  PUT  ////////////////////////////////////////////////////////////////////////////////////////////////

        /**
         * Creates a [HttpMethod.Delete] route with request params of type [PARAMS]
         *
         * A handler for this route must return a [ApiResponse] with data of type [RESPONSE].
         */
        inline fun <reified PARAMS, reified RESPONSE> delete(
            uri: String,
        ): ApiRoute.WithParams<PARAMS, RESPONSE> {
            return routeParams(Delete, uri)
        }

        /**
         * Creates a [HttpMethod.Delete] route with request body of type [BODY]
         *
         * A handler for this route must return a [ApiResponse] with data of type [RESPONSE].
         *
         * Notice: the param [d1] is a dummy for tricking the Kotlin compiler to accept the overload.
         */
        @Suppress("UNUSED_PARAMETER")
        inline fun <reified BODY, reified RESPONSE> delete(
            uri: String, d1: Nothing? = null,
        ): ApiRoute.WithBody<BODY, RESPONSE> {
            return routeBody(Delete, uri)
        }

        /**
         * Creates a [HttpMethod.Delete] route with request params of type [PARAMS] and body of type [BODY]
         *
         * A handler for this route must return a [ApiResponse] with data of type [RESPONSE].
         *
         * Notice: the params [d1] and [d2] is are dummy for tricking the Kotlin compiler to accept the overload.
         */
        @Suppress("UNUSED_PARAMETER")
        inline fun <reified PARAMS, reified BODY, reified RESPONSE> delete(
            uri: String, d1: Nothing? = null, d2: Nothing? = null,
        ): ApiRoute.WithBodyAndParams<PARAMS, BODY, RESPONSE> {
            return routeParamsBody(Delete, uri)
        }

        ////  INTERNAL HELPERS  ///////////////////////////////////////////////////////////////////////////////////////////////

        /** Converts a string into an [UriPattern]. */
        val String.asPattern get() = UriPattern(this)

        /**
         *  Helper for creating a plain route
         */
        inline fun <reified RESPONSE> routePlain(method: HttpMethod, uri: String) =
            ApiRoute.Plain<RESPONSE>(
                method = method,
                route = TypedRoute.Plain(uri.asPattern),
                responseType = kType()
            )

        /**
         *  Helper for creating a plain route
         */
        inline fun <reified PARAMS> routeSse(uri: String) =
            ApiRoute.Sse<PARAMS>(
                route = TypedRoute.Sse(kType(), uri.asPattern),
                responseType = kType()
            )

        /**
         *  Helper for creating a route with input params
         */
        inline fun <reified PARAMS, reified RESPONSE> routeParams(method: HttpMethod, uri: String) =
            ApiRoute.WithParams<PARAMS, RESPONSE>(
                method = method,
                route = TypedRoute.WithParams(kType(), uri.asPattern),
                responseType = kType()
            )

        /**
         *  Helper for creating a route with input body
         */
        inline fun <reified BODY, reified RESPONSE> routeBody(method: HttpMethod, uri: String) =
            ApiRoute.WithBody<BODY, RESPONSE>(
                method = method,
                route = TypedRoute.Plain(uri.asPattern),
                bodyType = kType(),
                responseType = kType()
            )

        /**
         *  Helper for creating a route with input params and input body
         */
        inline fun <reified PARAMS, reified BODY, reified RESPONSE> routeParamsBody(method: HttpMethod, uri: String) =
            ApiRoute.WithBodyAndParams<PARAMS, BODY, RESPONSE>(
                method = method,
                route = TypedRoute.WithParams(kType(), uri.asPattern),
                bodyType = kType(),
                responseType = kType()
            )
    }
}
