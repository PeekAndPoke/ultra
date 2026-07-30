package io.peekandpoke.funktor.rest.auth

import io.peekandpoke.funktor.rest.RestDsl
import io.peekandpoke.funktor.rest.auth.AuthRuleBuilder.Companion.validateChain

/**
 * Restricted builder for an [io.peekandpoke.funktor.rest.ApiRoutes] floor seed
 * (`ApiRoutes(authFloor = { ... })`).
 *
 * It exposes ONLY caller-independent rule factories — rules that read the CALLER (permissions,
 * user-type, authentication state), never the request params or body. This restriction is
 * deliberate and structural, and buys two properties:
 *
 * - **Type-agnostic replay.** The floor is declared once, before any route type is known, then
 *   applied to every route of the group regardless of its params/body types. Caller-only rules
 *   ignore the request payload, so a single materialized floor is safe to prepend to any route.
 * - **Phase-1 by construction** (see `20260722-two-phase-auth-consistent-params.md`). Because the
 *   floor cannot read request data, it can be evaluated BEFORE param conversion / DB loads — an
 *   unauthenticated or wrong-realm caller is rejected without touching storage.
 *
 * There is intentionally NO `forCall {}` (request-data access) and NO `appendRule` (which could
 * inject a param-dependent rule) here — those belong to the per-route `authorize {}`, not the floor.
 *
 * The floor obeys the same tree rules as `authorize {}` (see [AuthRuleBuilder]): statements are
 * conjuncts, `forAny {}` disjuncts; constants ([public]/[forbidden]) may only be the sole rule of
 * the floor; empty blocks/composites are boot errors. A floor MUST declare at least one rule —
 * use `{ public() }` for a deliberately public group.
 */
@RestDsl
class FloorAuthRuleBuilder internal constructor() {

    private val rules = mutableListOf<AuthRule<Any?, Any?>>()

    private fun add(rule: AuthRule<Any?, Any?>) {
        rules += rule
    }

    //  Caller-only leaf factories  ////////////////////////////////////////////////////////////////

    /** Requires any authenticated (non-anonymous) user — the cross-realm "any logged-in user" floor. */
    fun authenticated() = add(AuthRule.authenticated())

    /** Requires the super-user permission. */
    fun isSuperUser() = add(AuthRule.isSuperUser())

    /** Requires the JWT user-type claim to equal [type] — the realm-boundary floor. */
    fun forUserType(type: String) = add(AuthRule.forUserType(type))

    /** Requires the given [group]. */
    fun forGroup(group: String) = add(AuthRule.forGroup(group))

    /** Requires at least one of the given [groups]. */
    fun forAnyGroup(vararg groups: String) = add(AuthRule.forAnyGroup(groups.toList()))

    /** Requires the given [role]. */
    fun forRole(role: String) = add(AuthRule.forRole(role))

    /** Requires at least one of the given [roles]. */
    fun forAnyRole(vararg roles: String) = add(AuthRule.forAnyRole(roles.toList()))

    /** Requires the given [permission]. */
    fun forPermission(permission: String) = add(AuthRule.forPermission(permission))

    /** Requires at least one of the given [permissions]. */
    fun forAnyPermission(vararg permissions: String) = add(AuthRule.forAnyPermission(permissions.toList()))

    //  Constants — sole-rule only  ////////////////////////////////////////////////////////////////

    /** Declares the whole group public. Must be the ONLY rule of the floor. */
    fun public() = add(PublicRule())

    /** Declares the whole group dead (deny-all). Must be the ONLY rule of the floor. */
    fun forbidden() = add(ForbiddenRule())

    //  Combinators — block-style only  ///////////////////////////////////////////////////////////

    /** ALL statements in the block must pass. */
    fun forAll(block: FloorAuthRuleBuilder.() -> Unit) =
        add(AndAuthRule(FloorAuthRuleBuilder().apply(block).rules.toList()))

    /** At least ONE statement in the block must pass. */
    fun forAny(block: FloorAuthRuleBuilder.() -> Unit) =
        add(OrAuthRule(FloorAuthRuleBuilder().apply(block).rules.toList()))

    /**
     * Materializes the floor as caller-only rules, validated over the whole floor chain. Throws if
     * the floor is empty (every group must declare its minimal auth) or violates constant-soleness
     * / empty-composite (the same whole-chain invariants as `authorize {}` — [validateChain]
     * recurses the tree, so a constant nested in a `forAny`/`forAll` and an empty combinator are
     * both boot errors here, not just at the top level).
     */
    internal fun build(groupName: String): List<AuthRule<Any?, Any?>> {
        val where = "ApiRoutes '$groupName' authFloor"

        check(rules.isNotEmpty()) {
            "$where declared no rules — every ApiRoutes group must declare its minimal auth floor. " +
                    "Fix: authFloor = { isSuperUser() } for an admin group, " +
                    "authFloor = { authenticated() } for any-logged-in-user, or " +
                    "authFloor = { public() } for a deliberately public group."
        }
        validateChain(where, rules)

        return rules.toList()
    }
}
