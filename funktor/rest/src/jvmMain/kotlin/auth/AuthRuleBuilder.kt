package de.peekandpoke.funktor.rest.auth

import de.peekandpoke.funktor.rest.ApiRoute
import de.peekandpoke.funktor.rest.RestAuthRuleMarker
import de.peekandpoke.ultra.common.remote.ApiAccessLevel
import de.peekandpoke.ultra.security.user.UserPermissions
import io.ktor.server.application.*

/**
 * Helper class for creating auth rules
 *
 * The generated auth rules are applicable to:
 * - Request Parameters of type [PARAMS]
 * - Request Body of type [BODY]
 */
class AuthRuleBuilder<PARAMS, BODY>(
    val route: ApiRoute<*>,
) {
    /**
     * Auth rule that always succeeds, for documentation purposes.
     */
    @RestAuthRuleMarker
    fun forbidden(): AuthRule<PARAMS, BODY> =
        forCall(
            description = "Forbidden to everyone",
            estimateFn = { ApiAccessLevel.Denied },
        ) { false }

    /**
     * Auth rule that always succeeds, for documentation purposes.
     */
    @RestAuthRuleMarker
    fun public(): AuthRule<PARAMS, BODY> =
        forCall(
            description = "Public to everyone",
            estimateFn = { ApiAccessLevel.Granted },
        ) { true }

    /**
     * Combines all the given [rules] with a logic OR.
     *
     * If at least ONE of the given [rules] is accepted the combination is accepted.
     */
    @RestAuthRuleMarker
    fun forAny(vararg rules: AuthRule<in PARAMS, in BODY>): AuthRule<PARAMS, BODY> {
        @Suppress("UNCHECKED_CAST")
        return OrAuthRule(rules.toList() as List<AuthRule<PARAMS, BODY>>)
    }

    /**
     * Combines all the given [rules] with a logic AND.
     *
     * If all the given [rules] are accepted the combination is accepted.
     */
    @RestAuthRuleMarker
    fun forAll(vararg rules: AuthRule<in PARAMS, in BODY>): AuthRule<PARAMS, BODY> {
        @Suppress("UNCHECKED_CAST")
        return AndAuthRule(rules.toList() as List<AuthRule<PARAMS, BODY>>)
    }

    /**
     * Combines all the given [rules] with a logic AND.
     *
     * If all the given [rules] are accepted the combination is accepted.
     */
    @RestAuthRuleMarker
    fun forAll(rules: List<AuthRule<in PARAMS, in BODY>>): AuthRule<PARAMS, BODY> {
        @Suppress("UNCHECKED_CAST")
        return AndAuthRule(rules as List<AuthRule<PARAMS, BODY>>)
    }

    /**
     * Creates an auth rule for the current [ApplicationCall].
     */
    @RestAuthRuleMarker
    fun forCall(
        description: String,
        estimateFn: AuthRule.EstimateCtx.() -> ApiAccessLevel = { ApiAccessLevel.Granted },
        checkFn: AuthRule.CheckCtx<PARAMS, BODY>.() -> Boolean,
    ): AuthRule<PARAMS, BODY> =
        AuthRule.forCall(
            description = description,
            checkFn = checkFn,
            estimateFn = estimateFn,
        )

    /**
     * Creates an auth rule that checks if the current user is a super-user
     */
    @RestAuthRuleMarker
    fun isSuperUser(): AuthRule<PARAMS, BODY> = AuthRule.isSuperUser()

    /**
     * Creates an auth rule that checks if the current user has the given [group] in the [UserPermissions]
     */
    @RestAuthRuleMarker
    fun forGroup(group: String): AuthRule<PARAMS, BODY> = AuthRule.forGroup(group)

    /**
     * Creates an auth rule that checks if the current user has at least one of the given [groups] in the [UserPermissions]
     */
    @RestAuthRuleMarker
    fun forAnyGroup(vararg groups: String): AuthRule<PARAMS, BODY> = AuthRule.forAnyGroup(groups.toList())

    /**
     * Creates an auth rule that checks if the current user has the given [role] in the [UserPermissions]
     */
    @RestAuthRuleMarker
    fun forRole(role: String): AuthRule<PARAMS, BODY> = AuthRule.forRole(role)

    /**
     * Creates an auth rule that checks if the current user has at least one of the given [roles] in the [UserPermissions]
     */
    @RestAuthRuleMarker
    fun forAnyRole(vararg roles: String): AuthRule<PARAMS, BODY> = forAnyRole(roles.toList())

    /**
     * Creates an auth rule that checks if the current user has at least one of the given [roles] in the [UserPermissions]
     */
    @RestAuthRuleMarker
    fun forAnyRole(roles: Collection<String>): AuthRule<PARAMS, BODY> = AuthRule.forAnyRole(roles.toList())

    /**
     * Creates an auth rule that checks if the current user has the given [permission] in the [UserPermissions]
     */
    @RestAuthRuleMarker
    fun forPermission(permission: String): AuthRule<PARAMS, BODY> = AuthRule.forPermission(permission)

    /**
     * Creates an auth rule that checks if the current user has at least one of the given [permissions] in the [UserPermissions]
     */
    @RestAuthRuleMarker
    fun forAnyPermission(vararg permissions: String): AuthRule<PARAMS, BODY> =
        AuthRule.forAnyPermission(permissions.toList())
}
