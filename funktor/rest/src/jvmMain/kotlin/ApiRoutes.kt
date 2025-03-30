package de.peekandpoke.ktorfx.rest

import de.peekandpoke.ktorfx.core.broker.OutgoingConverter
import de.peekandpoke.ktorfx.core.broker.Routes
import de.peekandpoke.ktorfx.core.broker.TypedRoute
import de.peekandpoke.ktorfx.core.broker.UriPattern
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.common.remote.ApiResponse
import de.peekandpoke.ultra.common.remote.TypedApiEndpoint
import io.ktor.http.*
import io.ktor.http.HttpMethod.Companion.Delete
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.HttpMethod.Companion.Put
import kotlin.reflect.KClass

@DslMarker
annotation class RestDslMarkerRoute

@DslMarker
annotation class RestDslMarkerConfig

@DslMarker
annotation class RestAuthRuleMarker

@DslMarker
annotation class RestSecurityRuleMarker

/**
 * Base class for creating api routes
 */
abstract class ApiRoutes(val name: String, converter: OutgoingConverter, mountPoint: String = "") :
    Routes(converter, mountPoint) {

    /** list with all registered routes */
    private val allRoutes = mutableListOf<ApiRoute<*>>()

    val routeBuilder = RouteBuilder(converter, mountPoint)

    /** A list with all registered routes */
    val all get(): List<ApiRoute<*>> = allRoutes

    /** Registers a route */
    @RestDslMarkerRoute
    fun <RESULT, ROUTE : ApiRoute<RESULT>> route(block: RouteBuilder.() -> ROUTE) =
        routeBuilder.block().apply { allRoutes.add(this) }

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
            .apply { addRoute(this) }
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
            .apply { addRoute(this) }
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
            .apply { addRoute(this) }
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
            .apply { addRoute(this) }
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
            .apply { addRoute(this) }
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
            .apply { addRoute(this) }
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
            .apply { addRoute(this) }
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
            .apply { addRoute(this) }
    }


    fun <RESPONSE> addRoute(route: ApiRoute<RESPONSE>) {
        allRoutes.add(route)
    }

    class RouteBuilder(val converter: OutgoingConverter, private val mountPoint: String) {

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

        /**
         * Converts a string into an [UriPattern] while prepending the [mountPoint]
         */
        val String.asPattern get() = UriPattern(mountPoint + this)

        /**
         *  Helper for creating a plain route
         */
        inline fun <reified RESPONSE> routePlain(method: HttpMethod, uri: String) =
            ApiRoute.Plain<RESPONSE>(
                method = method,
                route = TypedRoute.Plain(converter, uri.asPattern),
                responseType = kType()
            )

        /**
         *  Helper for creating a plain route
         */
        inline fun <reified PARAMS> routeSse(uri: String) =
            ApiRoute.Sse<PARAMS>(
                route = TypedRoute.Sse(converter, kType(), uri.asPattern),
                responseType = kType()
            )

        /**
         *  Helper for creating a route with input params
         */
        inline fun <reified PARAMS, reified RESPONSE> routeParams(method: HttpMethod, uri: String) =
            ApiRoute.WithParams<PARAMS, RESPONSE>(
                method = method,
                route = TypedRoute.WithParams(converter, kType(), uri.asPattern),
                responseType = kType()
            )

        /**
         *  Helper for creating a route with input body
         */
        inline fun <reified BODY, reified RESPONSE> routeBody(method: HttpMethod, uri: String) =
            ApiRoute.WithBody<BODY, RESPONSE>(
                method = method,
                route = TypedRoute.Plain(converter, uri.asPattern),
                bodyType = kType(),
                responseType = kType()
            )

        /**
         *  Helper for creating a route with input params and input body
         */
        inline fun <reified PARAMS, reified BODY, reified RESPONSE> routeParamsBody(method: HttpMethod, uri: String) =
            ApiRoute.WithBodyAndParams<PARAMS, BODY, RESPONSE>(
                method = method,
                route = TypedRoute.WithParams(converter, kType(), uri.asPattern),
                bodyType = kType(),
                responseType = kType()
            )
    }
}
