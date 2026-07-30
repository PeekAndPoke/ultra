package io.peekandpoke.funktor.rest

import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.peekandpoke.funktor.core.broker.TypedRoute
import io.peekandpoke.funktor.core.broker.UriPattern
import io.peekandpoke.funktor.rest.auth.AuthResult
import io.peekandpoke.funktor.rest.auth.AuthRule
import io.peekandpoke.funktor.rest.auth.ForbiddenRule
import io.peekandpoke.funktor.rest.auth.PublicRule
import io.peekandpoke.funktor.rest.auth.RootAuthRuleBuilder
import io.peekandpoke.funktor.rest.auth.flattenTopLevelAnds
import io.peekandpoke.funktor.rest.auth.isCallerOnly
import io.peekandpoke.ultra.common.TypedAttributes
import io.peekandpoke.ultra.common.TypedKey
import io.peekandpoke.ultra.reflection.TypeRef
import io.peekandpoke.ultra.remote.ApiAccessLevel
import io.peekandpoke.ultra.remote.ApiResponse
import io.peekandpoke.ultra.security.user.User
import io.peekandpoke.ultra.security.user.UserId
import io.peekandpoke.ultra.security.user.UserPermissions
import io.peekandpoke.ultra.security.user.UserRecord

/**
 * Marks that the USER declared an `authorize {}` block on the route. The once-per-route guard
 * keys off THIS attribute — not off `authRules` being non-empty — so framework paths (the
 * mandatory floor seed of part 2, interface-triggered auto-rules of part 3) can pre-populate or
 * extend the chain without tripping it.
 */
private val UserAuthorizeDeclaredKey = TypedKey<Boolean>("UserAuthorizeDeclared")

/**
 * Base class for api routes representations
 */
@RestDsl
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
     * Prepends the group's floor rules as the INITIAL chain state, before the per-route
     * `authorize {}` (if any) appends. Called by the [io.peekandpoke.funktor.rest.ApiRoutes] mount
     * machinery with the group's materialized [FloorAuthRuleBuilder] output. The floor rules are
     * caller-only (they ignore request params/body), so the unchecked cast to this route's rule
     * type in each override is behaviour-safe.
     */
    abstract fun withFloor(floor: List<AuthRule<*, *>>): ApiRoute<RESPONSE>

    /**
     * Estimate access level for the given [permission] set.
     *
     * Synthesizes a non-anonymous [User] (userId "role-eval") from the permissions — "evaluate what
     * a user WITH THESE PERMISSIONS would have access to" semantically implies an authenticated user.
     * Used by the per-role access matrix (admin dashboards).
     */
    fun estimateAccess(permission: UserPermissions): ApiAccessLevel {
        val synthetic = User(
            record = UserRecord.LoggedIn(userId = UserId("role-eval")),
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
     * Phase-1 evaluation (two-phase auth — see [io.peekandpoke.funktor.rest.auth.isCallerOnly]): the
     * caller-only rules that DENY for this caller, evaluated via [AuthRule.estimate] (which needs no
     * params). A non-empty result means the request is rejected BEFORE any param conversion runs —
     * no pre-auth entity loads, no 404-vs-401 existence oracle. For every caller-only rule type,
     * `estimate` and `check` agree, so this yields exactly the decision `check` would.
     */
    fun phase1Denials(ctx: AuthRule.EstimateCtx): List<AuthRule<*, *>> =
        flattenTopLevelAnds(authRules).filter { it.isCallerOnly() && it.estimate(ctx).isDenied() }

    /**
     * True when the whole chain is a single bare constant rule ([PublicRule] / [ForbiddenRule]) —
     * i.e. the route is intentionally public (or dead). The floor-applying choke point skips
     * appending param auto-rules to such routes: a public route has no caller/org boundary to bind,
     * and a constant must stay the SOLE rule of its chain (see the auth-rule `validateChain`).
     *
     * TRADE-OFF: because a public route gets no param auto-rule and phase 1 never denies it,
     * conversion (a `findById`) runs for anonymous callers, so a public entity-param route DOES
     * expose an existence oracle (200 vs 404) on its own entities. That is acceptable for genuinely
     * public data — but declare `public()` on an entity-param group deliberately, never by accident.
     */
    fun isSoleConstantChain(): Boolean =
        authRules.size == 1 && (authRules[0] is PublicRule<*, *> || authRules[0] is ForbiddenRule<*, *>)

    /**
     * Appends framework-computed phase-2 auto-rules after the existing chain. Called ONLY by the
     * [io.peekandpoke.funktor.rest.ApiRoutes.addRoute] choke point, and ONLY with rules typed on the
     * param interface they require (`ConsistentParamRule : AuthRule<ConsistentParam, *>`) after
     * `addRoute` has verified this route's PARAMS implements that interface. So the unchecked cast to
     * this route's rule type in each override is the single place the "PARAMS is a ConsistentParam"
     * invariant is asserted, co-located with the detection — the rules themselves need no cast. Same
     * unchecked-cast rationale as [withFloor].
     */
    abstract fun withAppendedRules(rules: List<AuthRule<*, *>>): ApiRoute<RESPONSE>

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
         * Phase-2 evaluation: the param-dependent rules ([isCallerOnly] false) that fail for this
         * call. Caller-only rules are handled earlier by [phase1Denials], so each rule is evaluated
         * exactly once, in its phase.
         */
        fun checkParamPhase(ctx: AuthRule.CheckCtx<Unit, Unit>): AuthResult<Unit, Unit> {
            @Suppress("UNCHECKED_CAST")
            val flat = flattenTopLevelAnds(authRules) as List<AuthRule<Unit, Unit>>
            return AuthResult(failedRules = flat.filter { !it.isCallerOnly() && !it.check(ctx) })
        }

        /**
         * Declares the route's auth rules. May be declared ONCE per route; every appended rule
         * must pass (statements are conjuncts). See [RootAuthRuleBuilder].
         */
        fun authorize(builder: RootAuthRuleBuilder<Unit, Unit>.() -> Unit): Plain<RESPONSE> {
            check(attributes[UserAuthorizeDeclaredKey] != true) {
                "authorize {} may only be declared once per route ('${method.value} ${pattern.pattern}'). Fix: merge the rules into a single authorize { } block (statements are ANDed)."
            }
            // Append (not replace): framework paths may have pre-populated the chain (floor seed).
            return copy(authRules = authRules + RootAuthRuleBuilder<Unit, Unit>(route = this).apply(builder).build())
                .withAttribute(UserAuthorizeDeclaredKey, true)
        }

        @Suppress("UNCHECKED_CAST")
        override fun withFloor(floor: List<AuthRule<*, *>>): Plain<RESPONSE> =
            copy(authRules = (floor as List<AuthRule<Unit, Unit>>) + authRules)

        @Suppress("UNCHECKED_CAST")
        override fun withAppendedRules(rules: List<AuthRule<*, *>>): Plain<RESPONSE> =
            copy(authRules = authRules + (rules as List<AuthRule<Unit, Unit>>))

        /**
         * Sets a handler that returns a raw response
         */
        fun handle(handler: suspend RoutingContext.() -> RESPONSE) = copy(
            handler = {
                call.apiRespond(handler(this) ?: Unit)
            }
        )

        /**
         * Adds the [other] attributes.
         */
        fun withAttributes(other: TypedAttributes) = copy(
            attributes = attributes.plus(other)
        )

        /**
         * Adds an entry to the [TypedAttributes]
         */
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
         * Phase-2 evaluation: the param-dependent rules ([isCallerOnly] false) that fail for this
         * call. Caller-only rules are handled earlier by [phase1Denials], so each rule is evaluated
         * exactly once, in its phase.
         */
        fun checkParamPhase(ctx: AuthRule.CheckCtx<PARAMS, Unit>): AuthResult<PARAMS, Unit> {
            @Suppress("UNCHECKED_CAST")
            val flat = flattenTopLevelAnds(authRules) as List<AuthRule<PARAMS, Unit>>
            return AuthResult(failedRules = flat.filter { !it.isCallerOnly() && !it.check(ctx) })
        }

        /**
         * Declares the route's auth rules. May be declared ONCE per route; every appended rule
         * must pass (statements are conjuncts). See [RootAuthRuleBuilder].
         */
        fun authorize(builder: RootAuthRuleBuilder<PARAMS, Unit>.() -> Unit): Sse<PARAMS> {
            check(attributes[UserAuthorizeDeclaredKey] != true) {
                "authorize {} may only be declared once per route ('${method.value} ${pattern.pattern}'). Fix: merge the rules into a single authorize { } block (statements are ANDed)."
            }
            // Append (not replace): framework paths may have pre-populated the chain (floor seed).
            return copy(authRules = authRules + RootAuthRuleBuilder<PARAMS, Unit>(route = this).apply(builder).build())
                .withAttribute(UserAuthorizeDeclaredKey, true)
        }

        @Suppress("UNCHECKED_CAST")
        override fun withFloor(floor: List<AuthRule<*, *>>): Sse<PARAMS> =
            copy(authRules = (floor as List<AuthRule<PARAMS, Unit>>) + authRules)

        @Suppress("UNCHECKED_CAST")
        override fun withAppendedRules(rules: List<AuthRule<*, *>>): Sse<PARAMS> =
            copy(authRules = authRules + (rules as List<AuthRule<PARAMS, Unit>>))

        /**
         * Sets a handler that returns a raw response
         */
        fun handle(handler: suspend ServerSSESession.(PARAMS) -> Unit) = copy(
            handler = handler
        )

        /**
         * Adds the [other] attributes.
         */
        fun withAttributes(other: TypedAttributes) = copy(
            attributes = attributes.plus(other)
        )

        /**
         * Adds an entry to the [TypedAttributes]
         */
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
         * Phase-2 evaluation: the param-dependent rules ([isCallerOnly] false) that fail for this
         * call. Caller-only rules are handled earlier by [phase1Denials], so each rule is evaluated
         * exactly once, in its phase.
         */
        fun checkParamPhase(ctx: AuthRule.CheckCtx<PARAMS, Unit>): AuthResult<PARAMS, Unit> {
            @Suppress("UNCHECKED_CAST")
            val flat = flattenTopLevelAnds(authRules) as List<AuthRule<PARAMS, Unit>>
            return AuthResult(failedRules = flat.filter { !it.isCallerOnly() && !it.check(ctx) })
        }

        /**
         * Renders the route by replacing the placeholders with the given [parameters]
         */
        fun render(vararg parameters: Pair<String, String>) = route.render(parameters.toList())

        /**
         * Declares the route's auth rules. May be declared ONCE per route; every appended rule
         * must pass (statements are conjuncts). See [RootAuthRuleBuilder].
         */
        fun authorize(builder: RootAuthRuleBuilder<PARAMS, Unit>.() -> Unit): WithParams<PARAMS, RESPONSE> {
            check(attributes[UserAuthorizeDeclaredKey] != true) {
                "authorize {} may only be declared once per route ('${method.value} ${pattern.pattern}'). Fix: merge the rules into a single authorize { } block (statements are ANDed)."
            }
            // Append (not replace): framework paths may have pre-populated the chain (floor seed).
            return copy(authRules = authRules + RootAuthRuleBuilder<PARAMS, Unit>(route = this).apply(builder).build())
                .withAttribute(UserAuthorizeDeclaredKey, true)
        }

        @Suppress("UNCHECKED_CAST")
        override fun withFloor(floor: List<AuthRule<*, *>>): WithParams<PARAMS, RESPONSE> =
            copy(authRules = (floor as List<AuthRule<PARAMS, Unit>>) + authRules)

        @Suppress("UNCHECKED_CAST")
        override fun withAppendedRules(rules: List<AuthRule<*, *>>): WithParams<PARAMS, RESPONSE> =
            copy(authRules = authRules + (rules as List<AuthRule<PARAMS, Unit>>))

        /**
         * Sets a handler that returns a raw response
         */
        fun handle(handler: suspend RoutingContext.(PARAMS) -> RESPONSE) = copy(
            handler = { params ->
                call.apiRespond(handler(this, params) ?: Unit)
            }
        )

        /**
         * Adds the [other] attributes.
         */
        fun withAttributes(other: TypedAttributes) = copy(
            attributes = attributes.plus(other)
        )

        /**
         * Adds an entry to the [TypedAttributes]
         */
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
         * Phase-2 evaluation: the param-dependent rules ([isCallerOnly] false) that fail for this
         * call. Caller-only rules are handled earlier by [phase1Denials], so each rule is evaluated
         * exactly once, in its phase.
         */
        fun checkParamPhase(ctx: AuthRule.CheckCtx<Unit, BODY>): AuthResult<Unit, BODY> {
            @Suppress("UNCHECKED_CAST")
            val flat = flattenTopLevelAnds(authRules) as List<AuthRule<Unit, BODY>>
            return AuthResult(failedRules = flat.filter { !it.isCallerOnly() && !it.check(ctx) })
        }

        /**
         * Declares the route's auth rules. May be declared ONCE per route; every appended rule
         * must pass (statements are conjuncts). See [RootAuthRuleBuilder].
         */
        fun authorize(builder: RootAuthRuleBuilder<Unit, BODY>.() -> Unit): WithBody<BODY, RESPONSE> {
            check(attributes[UserAuthorizeDeclaredKey] != true) {
                "authorize {} may only be declared once per route ('${method.value} ${pattern.pattern}'). Fix: merge the rules into a single authorize { } block (statements are ANDed)."
            }
            // Append (not replace): framework paths may have pre-populated the chain (floor seed).
            return copy(authRules = authRules + RootAuthRuleBuilder<Unit, BODY>(route = this).apply(builder).build())
                .withAttribute(UserAuthorizeDeclaredKey, true)
        }

        @Suppress("UNCHECKED_CAST")
        override fun withFloor(floor: List<AuthRule<*, *>>): WithBody<BODY, RESPONSE> =
            copy(authRules = (floor as List<AuthRule<Unit, BODY>>) + authRules)

        @Suppress("UNCHECKED_CAST")
        override fun withAppendedRules(rules: List<AuthRule<*, *>>): WithBody<BODY, RESPONSE> =
            copy(authRules = authRules + (rules as List<AuthRule<Unit, BODY>>))

        /**
         * Sets a handler that returns an ApiResponse with data of type [RESPONSE]
         */
        fun handle(handler: suspend RoutingContext.(BODY) -> RESPONSE) = copy(
            handler = { body ->
                call.apiRespond(handler(this, body))
            }
        )

        /**
         * Adds the [other] attributes.
         */
        fun withAttributes(other: TypedAttributes) = copy(
            attributes = attributes.plus(other)
        )

        /**
         * Adds an entry to the [TypedAttributes]
         */
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
         * Phase-2 evaluation: the param-dependent rules ([isCallerOnly] false) that fail for this
         * call. Caller-only rules are handled earlier by [phase1Denials], so each rule is evaluated
         * exactly once, in its phase.
         */
        fun checkParamPhase(ctx: AuthRule.CheckCtx<PARAMS, BODY>): AuthResult<PARAMS, BODY> {
            @Suppress("UNCHECKED_CAST")
            val flat = flattenTopLevelAnds(authRules) as List<AuthRule<PARAMS, BODY>>
            return AuthResult(failedRules = flat.filter { !it.isCallerOnly() && !it.check(ctx) })
        }

        /**
         * Declares the route's auth rules. May be declared ONCE per route; every appended rule
         * must pass (statements are conjuncts). See [RootAuthRuleBuilder].
         */
        fun authorize(builder: RootAuthRuleBuilder<PARAMS, BODY>.() -> Unit): WithBodyAndParams<PARAMS, BODY, RESPONSE> {
            check(attributes[UserAuthorizeDeclaredKey] != true) {
                "authorize {} may only be declared once per route ('${method.value} ${pattern.pattern}'). Fix: merge the rules into a single authorize { } block (statements are ANDed)."
            }
            // Append (not replace): framework paths may have pre-populated the chain (floor seed).
            return copy(authRules = authRules + RootAuthRuleBuilder<PARAMS, BODY>(route = this).apply(builder).build())
                .withAttribute(UserAuthorizeDeclaredKey, true)
        }

        @Suppress("UNCHECKED_CAST")
        override fun withFloor(floor: List<AuthRule<*, *>>): WithBodyAndParams<PARAMS, BODY, RESPONSE> =
            copy(authRules = (floor as List<AuthRule<PARAMS, BODY>>) + authRules)

        @Suppress("UNCHECKED_CAST")
        override fun withAppendedRules(rules: List<AuthRule<*, *>>): WithBodyAndParams<PARAMS, BODY, RESPONSE> =
            copy(authRules = authRules + (rules as List<AuthRule<PARAMS, BODY>>))

        /**
         * Sets a handler that returns an ApiResponse with data of type [RESPONSE]
         */
        fun handle(handler: suspend RoutingContext.(PARAMS, BODY) -> RESPONSE) = copy(
            handler = { params, body ->
                call.apiRespond(handler(this, params, body))
            }
        )

        /**
         * Adds the [other] attributes.
         */
        fun withAttributes(other: TypedAttributes) = copy(
            attributes = attributes.plus(other)
        )

        /**
         * Adds an entry to the [TypedAttributes]
         */
        fun <T : Any> withAttribute(key: TypedKey<T>, value: T) = copy(
            attributes = attributes.plus(key, value)
        )
    }
}
