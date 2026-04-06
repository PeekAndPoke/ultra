package io.peekandpoke.funktor.rest

import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.peekandpoke.funktor.core.broker.TypedRoute
import io.peekandpoke.funktor.core.broker.UriPattern
import io.peekandpoke.funktor.rest.auth.AuthResult
import io.peekandpoke.funktor.rest.auth.AuthRule
import io.peekandpoke.funktor.rest.auth.AuthRuleBuilder
import io.peekandpoke.ultra.common.TypedAttributes
import io.peekandpoke.ultra.common.TypedKey
import io.peekandpoke.ultra.reflection.TypeRef
import io.peekandpoke.ultra.remote.ApiAccessLevel
import io.peekandpoke.ultra.remote.ApiResponse
import io.peekandpoke.ultra.security.user.User
import io.peekandpoke.ultra.security.user.UserPermissions
import io.peekandpoke.ultra.security.user.UserRecord

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

    /** The typed route */
    abstract val typedRoute: TypedRoute<*>

    /** The authorization rules */
    abstract val authRules: List<AuthRule<*, *>>

    /** Attributes for this route, e.g. for documentation purposed */
    abstract val attributes: TypedAttributes

    /**
     * Estimate access level for the given [permission] set.
     *
     * Synthesizes a non-anonymous [User] (userId "role-eval") from the permissions — "evaluate what
     * a user WITH THESE PERMISSIONS would have access to" semantically implies an authenticated user.
     * Used by the per-role access matrix (admin dashboards).
     */
    fun estimateAccess(permission: UserPermissions): ApiAccessLevel {
        val synthetic = User(
            record = UserRecord(userId = "role-eval"),
            permissions = permission,
        )
        return estimateAccess(user = AuthRule.EstimateCtx(user = synthetic))
    }

    /**
     * Estimate access level for the given [user] (full User object, preferred).
     *
     * Used by the per-user access matrix (ApiAcl) which needs to distinguish anonymous from
     * authenticated users.
     */
    fun estimateAccess(user: User): ApiAccessLevel {
        return estimateAccess(user = AuthRule.EstimateCtx(user = user))
    }

    /**
     * Estimate access level for the given estimation context.
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

        override val typedRoute get() = route
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

        override val typedRoute get() = route
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

        override val typedRoute get() = route
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

        override val typedRoute get() = route
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

        override val typedRoute get() = route
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
