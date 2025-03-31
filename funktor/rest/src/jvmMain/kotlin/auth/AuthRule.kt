package de.peekandpoke.funktor.rest.auth

import de.peekandpoke.funktor.core.kontainer
import de.peekandpoke.funktor.rest.auth.AuthRule.CheckCtx
import de.peekandpoke.funktor.rest.auth.AuthRule.EstimateCtx
import de.peekandpoke.ultra.common.remote.ApiAccessLevel
import de.peekandpoke.ultra.kontainer.KontainerAware
import de.peekandpoke.ultra.security.user.User
import de.peekandpoke.ultra.security.user.UserPermissions
import de.peekandpoke.ultra.security.user.UserProvider
import de.peekandpoke.ultra.security.user.UserRecord
import io.ktor.server.application.*

/**
 * Definition for all auth rules.
 *
 * An auth rule is applicable to:
 * - Request Parameters of type [PARAMS]
 * - Request Body of type [BODY]
 *
 * Auth rules have access to more information via the [CheckCtx].
 * See [CheckCtx] and the [check] method.
 */
interface AuthRule<PARAMS, BODY> {
    /**
     * Human-readable description of the auth rule, which can be used for generating docs.
     */
    val description: String

    /**
     * The authorization check returns 'true' when access should be granted.
     */
    fun check(ctx: CheckCtx<PARAMS, BODY>): Boolean

    /**
     * Estimates that [ApiAccessLevel].
     */
    fun estimate(ctx: EstimateCtx): ApiAccessLevel

    companion object {

        /**
         * Creates a generic auth rule.
         *
         * For the given
         * - Request Parameters of type [P]
         * - Request Body of type [B]
         */
        fun <P, B> forCall(
            description: String,
            estimateFn: EstimateCtx.() -> ApiAccessLevel = { ApiAccessLevel.Granted },
            checkFn: CheckCtx<P, B>.() -> Boolean,
        ): AuthRule<P, B> =
            CallCheck(description = description, checkFn = checkFn, estimateFn = estimateFn)

        /**
         * Creates a Rule that returns true when the current user is a SuperUser
         */
        fun <P, B> isSuperUser(): AuthRule<P, B> =
            PermissionsCheck("Is SuperUser") { permissions.isSuperUser }

        /**
         * Creates a Rule that returns true when the current user has given [organisation]
         */
        fun <P, B> forOrganisation(organisation: String): AuthRule<P, B> =
            PermissionsCheck("Is part of organisation $organisation") {
                permissions.hasOrganisation(organisation)
            }

        /**
         * Creates a Rule that returns true when the current user has any of the given [organisations]
         */
        fun <P, B> forAnyOrganisation(organisations: Collection<String>): AuthRule<P, B> =
            PermissionsCheck("Is part of any organisation $organisations") {
                permissions.hasAnyOrganisation(organisations)
            }

        /**
         * Creates a Rule that returns true when the current user has given [group]
         */
        fun <P, B> forGroup(group: String): AuthRule<P, B> =
            PermissionsCheck("Matches group $group") { permissions.hasGroup(group) }

        /**
         * Creates a Rule that returns true when the current user has any of the given [groups]
         */
        fun <P, B> forAnyGroup(groups: Collection<String>): AuthRule<P, B> =
            PermissionsCheck("Matches any group in $groups") { permissions.hasAnyGroup(groups) }

        /**
         * Creates a Rule that returns true when the current user has the given [role]
         */
        fun <P, B> forRole(role: String): AuthRule<P, B> =
            PermissionsCheck("Matches role $role") { permissions.hasRole(role) }

        /**
         * Creates a Rule that returns true when the current user has any of the given [roles]
         */
        fun <P, B> forAnyRole(roles: Collection<String>): AuthRule<P, B> =
            PermissionsCheck("Matches any role in $roles") { permissions.hasAnyRole(roles) }

        /**
         * Creates a Rule that returns true when the current user has the given [permission]
         */
        fun <P, B> forPermission(permission: String): AuthRule<P, B> =
            PermissionsCheck("Matches permission $permission") { permissions.hasRole(permission) }

        /**
         * Creates a Rules that returns true when the current user has any of the given [permissions]
         */
        fun <P, B> forAnyPermission(permissions: Collection<String>): AuthRule<P, B> =
            PermissionsCheck("Matches any permission in $permissions") {
                this.permissions.hasAnyPermission(permissions)
            }
    }

    /**
     * The context used to estimate the [ApiAccessLevel] of a rule.
     */
    class EstimateCtx(
        val permissions: UserPermissions,
    ) {
        companion object {
            fun of(user: User) = EstimateCtx(user.permissions)
        }
    }

    /**
     * The context in which all auth rules are called.
     *
     * It provides information needed to make an authorization decision, like:
     *
     * - the [ApplicationCall]
     * - the Request Parameters of type [PARAMS]
     * - the Request Body of type [BODY]
     * - the [UserRecord] of the user doing the call
     * - the [UserPermissions] of the user doing the call
     */
    class CheckCtx<PARAMS, BODY>(
        val call: ApplicationCall,
        val params: PARAMS,
        val body: BODY,
    ) : KontainerAware {
        companion object {
            /**
             * Creates the context for empty Request Params and empty Request Body
             */
            fun plain(call: ApplicationCall) = CheckCtx(call = call, params = Unit, body = Unit)

            /**
             * Creates the context for Request Params of type [P] and empty Request Body
             */
            fun <P> paramsOnly(call: ApplicationCall, params: P) = CheckCtx(call = call, params = params, body = Unit)

            /**
             * Creates the context for Request Body of type [B] and empty Request Parameters
             */
            fun <B> bodyOnly(call: ApplicationCall, body: B) = CheckCtx(call = call, params = Unit, body = body)

            /**
             * Creates the context for Request Body of type [B] and empty Request Parameters
             */
            fun <P, B> paramsAndBody(call: ApplicationCall, params: P, body: B) =
                CheckCtx(call = call, params = params, body = body)
        }

        override val kontainer get() = call.kontainer

        val user by lazy { call.kontainer.get(UserProvider::class).invoke() }
        val permissions by lazy { user.permissions }

        val estimateCtx by lazy { EstimateCtx.of(user) }
    }
}

/**
 * Helper for creating a logic OR combination of two auth rules
 */
infix fun <PARAMS, BODY> AuthRule<in PARAMS, in BODY>.or(other: AuthRule<in PARAMS, in BODY>): AuthRule<PARAMS, BODY> {

    val rules = listOf(this, other)

    @Suppress("UNCHECKED_CAST")
    return OrAuthRule(rules as List<AuthRule<PARAMS, BODY>>)
}

/**
 * Helper for creating a logic AND combination of two auth rules
 */
infix fun <PARAMS, BODY> AuthRule<in PARAMS, in BODY>.and(other: AuthRule<in PARAMS, in BODY>): AuthRule<PARAMS, BODY> {

    val rules = listOf(this, other)

    @Suppress("UNCHECKED_CAST")
    return AndAuthRule(rules as List<AuthRule<PARAMS, BODY>>)
}

class CallCheck<PARAMS, BODY>(
    override val description: String,
    private val checkFn: CheckCtx<PARAMS, BODY>.() -> Boolean,
    private val estimateFn: EstimateCtx.() -> ApiAccessLevel,
) : AuthRule<PARAMS, BODY> {

    override fun check(ctx: CheckCtx<PARAMS, BODY>): Boolean {
        return checkFn(ctx)
    }

    override fun estimate(ctx: EstimateCtx): ApiAccessLevel {
        return estimateFn(ctx)
    }
}

/**
 * Simple auth rules implementation
 */
class PermissionsCheck<PARAMS, BODY>(
    override val description: String,
    private val rule: EstimateCtx.() -> Boolean,
) : AuthRule<PARAMS, BODY> {

    override fun check(ctx: CheckCtx<PARAMS, BODY>): Boolean = rule(ctx.estimateCtx)

    override fun estimate(ctx: EstimateCtx): ApiAccessLevel = when (rule(ctx)) {
        true -> ApiAccessLevel.Granted
        else -> ApiAccessLevel.Denied
    }
}

/**
 * Implements the logic OR for the given [rules]
 */
class OrAuthRule<PARAMS, BODY>(internal val rules: List<AuthRule<PARAMS, BODY>>) : AuthRule<PARAMS, BODY> {

    override val description = "Fulfills any of: ${rules.joinToString(" OR ") { "(${it.description})" }}"

    override fun check(ctx: CheckCtx<PARAMS, BODY>) = rules.any {
        it.run { check(ctx) }
    }

    override fun estimate(ctx: EstimateCtx): ApiAccessLevel {
        return rules.fold(ApiAccessLevel.Denied) { level, next ->
            level or next.estimate(ctx)
        }
    }

    infix fun or(other: AuthRule<PARAMS, BODY>): AuthRule<PARAMS, BODY> = OrAuthRule(rules.plus(other))
}

/**
 * Implements the logic ANY for the given [rules]
 */
class AndAuthRule<PARAMS, BODY>(internal val rules: List<AuthRule<PARAMS, BODY>>) : AuthRule<PARAMS, BODY> {

    override val description = "Fulfills all of: ${rules.joinToString(" AND ") { "(${it.description})" }}"

    override fun check(ctx: CheckCtx<PARAMS, BODY>) = rules.all {
        it.run { check(ctx) }
    }

    override fun estimate(ctx: EstimateCtx): ApiAccessLevel {
        return rules.fold(ApiAccessLevel.Granted) { level, next ->
            level and next.estimate(ctx)
        }
    }

    infix fun and(other: AuthRule<PARAMS, BODY>): AuthRule<PARAMS, BODY> = AndAuthRule(rules.plus(other))
}

