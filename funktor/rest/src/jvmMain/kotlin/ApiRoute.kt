package de.peekandpoke.funktor.rest

import de.peekandpoke.funktor.core.broker.TypedRoute
import de.peekandpoke.funktor.core.broker.UriPattern
import de.peekandpoke.funktor.rest.auth.AuthResult
import de.peekandpoke.funktor.rest.auth.AuthRule
import de.peekandpoke.funktor.rest.auth.AuthRuleBuilder
import de.peekandpoke.ultra.common.TypedAttributes
import de.peekandpoke.ultra.common.TypedKey
import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.common.remote.ApiAccessLevel
import de.peekandpoke.ultra.common.remote.ApiResponse
import de.peekandpoke.ultra.security.user.UserPermissions
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*

/**
 * Base class for api routes representations
 */
sealed class ApiRoute<RESPONSE> {

    /** The http method of the route */
    abstract val method: HttpMethod

    /** The uri pattern */
    abstract val pattern: UriPattern

    /** The data type of the return [RESPONSE] */
    abstract val responseType: TypeRef<RESPONSE>

    /** The authorization rules */
    abstract val authRules: List<AuthRule<*, *>>

    /** Attributes for this route, e.g. for documentation purposed */
    abstract val attributes: TypedAttributes

    /**
     *  Estimate access level for the given user
     */
    fun estimateAccess(permission: UserPermissions): ApiAccessLevel {
        return estimateAccess(
            user = AuthRule.EstimateCtx(permissions = permission)
        )
    }

    /**
     * Estimate access level for the given [user]
     */
    fun estimateAccess(user: AuthRule.EstimateCtx): ApiAccessLevel {
        return authRules.fold(ApiAccessLevel.Granted) { level, rule ->
            level and rule.estimate(user)
        }
    }

    /**
     * Plain route with input params or input body
     */
    data class Plain<RESPONSE>(
        override val method: HttpMethod,
        val route: TypedRoute.Plain,
        override val responseType: TypeRef<RESPONSE>,
        override val authRules: List<AuthRule<Unit, Unit>> = emptyList(),
        override val attributes: TypedAttributes = TypedAttributes.empty,
        val handler: suspend RoutingContext.() -> Any = {
            ApiResponse.internalServerError<RESPONSE>().withError("Endpoint not implemented")
        },
    ) : ApiRoute<RESPONSE>() {

        override val pattern get() = route.pattern

        /**
         * Check access rules against the given context
         */
        fun checkAccess(ctx: AuthRule.CheckCtx<Unit, Unit>) = AuthResult(
            failedRules = authRules.filter { !it.check(ctx) }
        )

        /**
         * Adds an authorization rules to the route
         */
        @RestDslMarkerConfig
        fun authorize(builder: AuthRuleBuilder<Unit, Unit>.() -> AuthRule<Unit, Unit>) = copy(
            authRules = authRules.plus(
                AuthRuleBuilder<Unit, Unit>(route = this).builder()
            )
        )

        /**
         * Sets a handler that returns a raw response
         */
        @RestDslMarkerConfig
        fun handle(handler: suspend RoutingContext.() -> RESPONSE) = copy(
            handler = {
                call.apiRespond(handler(this) ?: Unit)
            }
        )

        /**
         * Adds the [other] attributes.
         */
        @RestDslMarkerConfig
        fun withAttributes(other: TypedAttributes) = copy(
            attributes = attributes.plus(other)
        )

        /**
         * Adds an entry to the [TypedAttributes]
         */
        @RestDslMarkerConfig
        fun <T : Any> withAttribute(key: TypedKey<T>, value: T) = copy(
            attributes = attributes.plus(key, value)
        )
    }

    /**
     * Plain route with input params or input body
     */
    data class Sse<PARAMS>(
        val route: TypedRoute.Sse<PARAMS>,
        override val responseType: TypeRef<Unit>,
        override val authRules: List<AuthRule<PARAMS, Unit>> = emptyList(),
        override val attributes: TypedAttributes = TypedAttributes.empty,
        val handler: suspend ServerSSESession.(PARAMS) -> Any = {
            error("Sse Endpoint not implemented")
        },
    ) : ApiRoute<Unit>() {

        override val method: HttpMethod = HttpMethod.Get

        override val pattern get() = route.pattern

        /**
         * Check access rules against the given context
         */
        fun checkAccess(ctx: AuthRule.CheckCtx<PARAMS, Unit>) = AuthResult(
            failedRules = authRules.filter { !it.check(ctx) }
        )

        /**
         * Adds an authorization rules to the route
         */
        @RestDslMarkerConfig
        fun authorize(builder: AuthRuleBuilder<PARAMS, Unit>.() -> AuthRule<PARAMS, Unit>) = copy(
            authRules = authRules.plus(
                AuthRuleBuilder<PARAMS, Unit>(route = this).builder()
            )
        )

        /**
         * Sets a handler that returns a raw response
         */
        @RestDslMarkerConfig
        fun handle(handler: suspend ServerSSESession.(PARAMS) -> Unit) = copy(
            handler = handler
        )

        /**
         * Adds the [other] attributes.
         */
        @RestDslMarkerConfig
        fun withAttributes(other: TypedAttributes) = copy(
            attributes = attributes.plus(other)
        )

        /**
         * Adds an entry to the [TypedAttributes]
         */
        @RestDslMarkerConfig
        fun <T : Any> withAttribute(key: TypedKey<T>, value: T) = copy(
            attributes = attributes.plus(key, value)
        )
    }

    /**
     * Route with input params
     */
    data class WithParams<PARAMS, RESPONSE>(
        override val method: HttpMethod,
        val route: TypedRoute.WithParams<PARAMS>,
        override val responseType: TypeRef<RESPONSE>,
        override val authRules: List<AuthRule<PARAMS, Unit>> = emptyList(),
        override val attributes: TypedAttributes = TypedAttributes.empty,
        val handler: suspend RoutingContext.(PARAMS) -> Any = {
            ApiResponse.internalServerError<RESPONSE>().withError("Endpoint not implemented")
        },
    ) : ApiRoute<RESPONSE>() {

        override val pattern get() = route.pattern

        /**
         * Check access rules against the given context
         */
        fun checkAccess(ctx: AuthRule.CheckCtx<PARAMS, Unit>) = AuthResult(
            failedRules = authRules.filter { !it.check(ctx) }
        )

        /**
         * Renders the route by replacing the placeholders with the given [parameters]
         */
        fun render(vararg parameters: Pair<String, String>) = route.render(parameters.toList())

        /**
         * Adds and authorization rule
         */
        @RestDslMarkerConfig
        fun authorize(builder: AuthRuleBuilder<PARAMS, Unit>.() -> AuthRule<PARAMS, Unit>) = copy(
            authRules = authRules.plus(
                AuthRuleBuilder<PARAMS, Unit>(route = this).builder()
            )
        )

        /**
         * Sets a handler that returns a raw response
         */
        @RestDslMarkerConfig
        fun handle(handler: suspend RoutingContext.(PARAMS) -> RESPONSE) = copy(
            handler = { params ->
                call.apiRespond(handler(this, params) ?: Unit)
            }
        )

        /**
         * Adds the [other] attributes.
         */
        @RestDslMarkerConfig
        fun withAttributes(other: TypedAttributes) = copy(
            attributes = attributes.plus(other)
        )

        /**
         * Adds an entry to the [TypedAttributes]
         */
        @RestDslMarkerConfig
        fun <T : Any> withAttribute(key: TypedKey<T>, value: T) = copy(
            attributes = attributes.plus(key, value)
        )
    }

    /**
     * Route with input body
     */
    data class WithBody<BODY, RESPONSE>(
        override val method: HttpMethod,
        val route: TypedRoute.Plain,
        val bodyType: TypeRef<BODY>,
        override val responseType: TypeRef<RESPONSE>,
        override val authRules: List<AuthRule<Unit, BODY>> = emptyList(),
        override val attributes: TypedAttributes = TypedAttributes.empty,
        val handler: suspend RoutingContext.(BODY) -> Any = {
            ApiResponse.internalServerError<RESPONSE>().withError("Endpoint not implemented")
        },
    ) : ApiRoute<RESPONSE>() {

        override val pattern get() = route.pattern

        /**
         * Check access rules against the given context
         */
        fun checkAccess(ctx: AuthRule.CheckCtx<Unit, BODY>) = AuthResult(
            failedRules = authRules.filter { !it.check(ctx) }
        )

        /**
         * Adds an authorization rules to the route
         */
        @RestDslMarkerConfig
        fun authorize(builder: AuthRuleBuilder<Unit, BODY>.() -> AuthRule<Unit, BODY>) = copy(
            authRules = authRules.plus(
                AuthRuleBuilder<Unit, BODY>(route = this).builder()
            )
        )

        /**
         * Sets a handler that returns an ApiResponse with data of type [RESPONSE]
         */
        @RestDslMarkerConfig
        fun handle(handler: suspend RoutingContext.(BODY) -> RESPONSE) = copy(
            handler = { body ->
                call.apiRespond(handler(this, body))
            }
        )

        /**
         * Adds the [other] attributes.
         */
        @RestDslMarkerConfig
        fun withAttributes(other: TypedAttributes) = copy(
            attributes = attributes.plus(other)
        )

        /**
         * Adds an entry to the [TypedAttributes]
         */
        @RestDslMarkerConfig
        fun <T : Any> withAttribute(key: TypedKey<T>, value: T) = copy(
            attributes = attributes.plus(key, value)
        )
    }

    /**
     * Route with input params and input body
     */
    data class WithBodyAndParams<PARAMS, BODY, RESPONSE>(
        override val method: HttpMethod,
        val route: TypedRoute.WithParams<PARAMS>,
        val bodyType: TypeRef<BODY>,
        override val responseType: TypeRef<RESPONSE>,
        override val authRules: List<AuthRule<PARAMS, BODY>> = emptyList(),
        override val attributes: TypedAttributes = TypedAttributes.empty,
        val handler: suspend RoutingContext.(PARAMS, BODY) -> Any = { _, _ ->
            ApiResponse.internalServerError<RESPONSE>().withError("Endpoint not implemented")
        },
    ) : ApiRoute<RESPONSE>() {

        override val pattern get() = route.pattern

        /**
         * Check access rules against the given context
         */
        fun checkAccess(ctx: AuthRule.CheckCtx<PARAMS, BODY>) = AuthResult(
            failedRules = authRules.filter { !it.check(ctx) }
        )

        /**
         * Adds an authorization rules to the route
         */
        @RestDslMarkerConfig
        fun authorize(builder: AuthRuleBuilder<PARAMS, BODY>.() -> AuthRule<PARAMS, BODY>) = copy(
            authRules = authRules.plus(
                AuthRuleBuilder<PARAMS, BODY>(route = this).builder()
            )
        )

        /**
         * Sets a handler that returns an ApiResponse with data of type [RESPONSE]
         */
        @RestDslMarkerConfig
        fun handle(handler: suspend RoutingContext.(PARAMS, BODY) -> RESPONSE) = copy(
            handler = { params, body ->
                call.apiRespond(handler(this, params, body))
            }
        )

        /**
         * Adds the [other] attributes.
         */
        @RestDslMarkerConfig
        fun withAttributes(other: TypedAttributes) = copy(
            attributes = attributes.plus(other)
        )

        /**
         * Adds an entry to the [TypedAttributes]
         */
        @RestDslMarkerConfig
        fun <T : Any> withAttribute(key: TypedKey<T>, value: T) = copy(
            attributes = attributes.plus(key, value)
        )
    }
}
