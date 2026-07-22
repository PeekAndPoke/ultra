package io.peekandpoke.funktor.rest.auth

import io.peekandpoke.funktor.rest.ApiRoute
import io.peekandpoke.funktor.rest.RestDsl
import io.peekandpoke.ultra.remote.ApiAccessLevel

/**
 * Accumulating builder for a route's auth rules — the receiver of `authorize {}` and of the
 * `forAll {}` / `forAny {}` combinator blocks.
 *
 * SEMANTICS (see `.claude/tasks/20260722-authorize-rule-builder.md`, ambiguity inventory):
 * - Every rule call APPENDS to the enclosing block's chain; nothing returns a rule, so value
 *   composition (`forAny(a, b)`, `a or b`) is unrepresentable — statements are the only syntax.
 * - Statements in `authorize {}` and `forAll {}` are conjuncts (ALL must pass); statements in
 *   `forAny {}` are disjuncts (ONE must pass). The rule tree is literally what is written.
 * - The class family shares one [RestDsl] marker, so a nested block hides the outer receivers:
 *   root-only members (`public()`, `forbidden()`) inside a sub-block are a COMPILE error instead
 *   of silently registering at the wrong level.
 * - Empty blocks and misplaced constant rules fail at route-construction (= boot) time.
 */
@RestDsl
sealed class AuthRuleBuilder<PARAMS, BODY> {

    private val rules = mutableListOf<AuthRule<PARAMS, BODY>>()

    /**
     * Snapshot of the rules collected so far (for build + validation).
     *
     * INVARIANT: this MUST return a COPY. Routes must be immutable once built — a builder
     * reference can leak into request-time lambdas (e.g. a nonsensical `isSuperUser()` call
     * inside a `forCall {}` body compiles, because those lambdas' receivers are unmarked), and
     * only this snapshot guarantees such appends land on an orphaned list instead of mutating a
     * served route's rule chain.
     */
    protected fun collected(): List<AuthRule<PARAMS, BODY>> = rules.toList()

    /**
     * The explicit programmatic bridge: append a pre-built rule (e.g. composed via the
     * [AuthRule] companion factories, which never touch a builder — so nothing double-registers).
     */
    fun appendRule(rule: AuthRule<PARAMS, BODY>) {
        rules += rule
    }

    //  Leaf factories — append-only, nothing returns a rule  /////////////////////////////////////

    /** Requires any authenticated (non-anonymous) user. */
    fun authenticated() = appendRule(AuthRule.authenticated())

    /** Requires the super-user permission. */
    fun isSuperUser() = appendRule(AuthRule.isSuperUser())

    /** Requires the user-type claim to equal [type] — the realm-boundary check, see [AuthRule.forUserType]. */
    fun forUserType(type: String) = appendRule(AuthRule.forUserType(type))

    /** Requires the given [group]. */
    fun forGroup(group: String) = appendRule(AuthRule.forGroup(group))

    /** Requires at least one of the given [groups]. */
    fun forAnyGroup(vararg groups: String) = forAnyGroup(groups.toList())

    /** Requires at least one of the given [groups]. */
    fun forAnyGroup(groups: Collection<String>) = appendRule(AuthRule.forAnyGroup(groups.toList()))

    /** Requires the given [role]. */
    fun forRole(role: String) = appendRule(AuthRule.forRole(role))

    /** Requires at least one of the given [roles]. */
    fun forAnyRole(vararg roles: String) = forAnyRole(roles.toList())

    /** Requires at least one of the given [roles]. */
    fun forAnyRole(roles: Collection<String>) = appendRule(AuthRule.forAnyRole(roles.toList()))

    /** Requires the given [permission]. */
    fun forPermission(permission: String) = appendRule(AuthRule.forPermission(permission))

    /** Requires at least one of the given [permissions]. */
    fun forAnyPermission(vararg permissions: String) =
        appendRule(AuthRule.forAnyPermission(permissions.toList()))

    /** Requires membership in the given [organisation]. */
    fun forOrganisation(organisation: String) = appendRule(AuthRule.forOrganisation(organisation))

    /** Requires membership in at least one of the given [organisations]. */
    fun forAnyOrganisation(organisations: Collection<String>) =
        appendRule(AuthRule.forAnyOrganisation(organisations))

    /** Appends a custom rule evaluated against the full call context — the escape hatch. */
    fun forCall(
        description: String,
        estimateFn: AuthRule.EstimateCtx.() -> ApiAccessLevel = { ApiAccessLevel.Granted },
        checkFn: AuthRule.CheckCtx<PARAMS, BODY>.() -> Boolean,
    ) = appendRule(AuthRule.forCall(description = description, estimateFn = estimateFn, checkFn = checkFn))

    //  Combinators — block-style only  ///////////////////////////////////////////////////////////

    /** ALL statements in the block must pass. Redundant at the top level (the chain already ANDs); useful inside [forAny]. */
    fun forAll(block: SubAuthRuleBuilder<PARAMS, BODY>.() -> Unit) {
        appendRule(SubAuthRuleBuilder<PARAMS, BODY>().apply(block).buildAnd())
    }

    /** At least ONE statement in the block must pass. */
    fun forAny(block: SubAuthRuleBuilder<PARAMS, BODY>.() -> Unit) {
        appendRule(SubAuthRuleBuilder<PARAMS, BODY>().apply(block).buildOr())
    }

    companion object {
        /**
         * True when the rule IS or CONTAINS a constant ([PublicRule]/[ForbiddenRule]) anywhere in
         * its And/Or tree. Accepted hole (cannot be closed structurally): a custom [AuthRule] or
         * [CallCheck] closure wrapping constant behavior is indistinguishable from
         * `forCall { true }` — this check is a footgun guard for the DECLARED tree, not a
         * semantic-analysis boundary.
         */
        internal fun AuthRule<*, *>.containsConstant(): Boolean = when (this) {
            is PublicRule<*, *>, is ForbiddenRule<*, *> -> true
            is AndAuthRule<*, *> -> rules.any { it.containsConstant() }
            is OrAuthRule<*, *> -> rules.any { it.containsConstant() }
            else -> false
        }

        /**
         * True when the tree contains an EMPTY And/Or node. An empty AND folds to Granted
         * (allow-all!) and an empty OR to Denied (silent dead route) — both must be boot errors.
         * Unreachable via the DSL (sub-builders reject empty blocks); guards the [appendRule] path.
         */
        internal fun AuthRule<*, *>.containsEmptyComposite(): Boolean = when (this) {
            is AndAuthRule<*, *> -> rules.isEmpty() || rules.any { it.containsEmptyComposite() }
            is OrAuthRule<*, *> -> rules.isEmpty() || rules.any { it.containsEmptyComposite() }
            else -> false
        }

        /**
         * Validates the invariants that must hold over a route's WHOLE auth chain, regardless of
         * how the rules got there — the `authorize {}` DSL, a part-2 floor seed, part-3 auto-rules,
         * or a direct `copy(authRules = ...)`. This is the single source of truth reused by
         * [RootAuthRuleBuilder.build] (per-block early feedback) AND the boot-time route validator
         * (whole-chain backstop). Deliberately does NOT check non-emptiness: a route that never
         * declares `authorize` has an empty chain and is legally public (closed by the part-2 floor).
         */
        internal fun validateChain(where: String, rules: List<AuthRule<*, *>>) {
            // A constant may appear ONLY as the single, BARE rule of the whole chain — a composite
            // that merely contains one (e.g. OrAuthRule(role, PublicRule)) reads restrictive but is
            // always-allow; a constant beside other rules is a silent no-op or an always-allow.
            val soleBareConstant = rules.size == 1 &&
                    (rules[0] is PublicRule<*, *> || rules[0] is ForbiddenRule<*, *>)
            check(rules.none { it.containsConstant() } || soleBareConstant) {
                "$where: public()/forbidden() must be the SOLE rule of the chain — combined with " +
                        "(or nested inside) other rules they silently become a no-op or an " +
                        "always-allow. Fix: make public()/forbidden() the only rule; to serve BOTH a " +
                        "public and a protected audience, split them into SEPARATE ApiRoutes groups, " +
                        "each with its own floor (an append-only floor cannot mix public() with a " +
                        "restrictive rule)."
            }
            check(rules.none { it.containsEmptyComposite() }) {
                "$where: contains an empty forAll{}/forAny{} — an empty AND allows everyone, an " +
                        "empty OR denies everyone. Fix: remove the empty combinator, or add rules to it."
            }
        }
    }
}

/**
 * The receiver of a route's `authorize {}` block. Additionally carries the CONSTANT rules, which
 * are only meaningful as the SOLE rule of the whole chain:
 * - `public()` as an AND-member is an always-true no-op that looks meaningful;
 * - `public()` as an OR-disjunct would allow everyone;
 * - `forbidden()` as an OR-member is a no-op.
 *
 * [build] enforces this per-block for early feedback; the same invariant is re-checked over the
 * route's WHOLE chain at boot (see [validateChain]), so it holds no matter how a constant got
 * appended — the DSL, a part-2 floor seed, or a direct `copy`.
 */
class RootAuthRuleBuilder<PARAMS, BODY> internal constructor(
    private val route: ApiRoute<*>,
) : AuthRuleBuilder<PARAMS, BODY>() {

    /** Declares the route public. Must be the ONLY rule of the route. */
    fun public() = appendRule(PublicRule())

    /** Declares the route dead (deny-all). Must be the ONLY rule of the route. */
    fun forbidden() = appendRule(ForbiddenRule())

    internal fun build(): List<AuthRule<PARAMS, BODY>> {
        val at = "authorize {} at '${route.method.value} ${route.pattern.pattern}'"
        val all = collected()

        check(all.isNotEmpty()) {
            "$at declared no rules. Fix: add at least one rule (e.g. isSuperUser()), or declare " +
                    "public() explicitly for a public route."
        }
        validateChain(at, all)

        return all
    }
}

/**
 * The receiver of `forAll {}` / `forAny {}` blocks. Deliberately has NO constant rules
 * ([RootAuthRuleBuilder.public] / `forbidden`) — inside a combinator they are no-ops or
 * always-allows, and the shared [RestDsl] marker turns an attempt into a compile error instead
 * of a silent outer-receiver append.
 */
class SubAuthRuleBuilder<PARAMS, BODY> internal constructor() : AuthRuleBuilder<PARAMS, BODY>() {

    internal fun buildAnd(): AuthRule<PARAMS, BODY> = AndAuthRule(validated("forAll"))

    internal fun buildOr(): AuthRule<PARAMS, BODY> = OrAuthRule(validated("forAny"))

    private fun validated(where: String): List<AuthRule<PARAMS, BODY>> {
        val all = collected()

        check(all.isNotEmpty()) {
            "$where {} declared no rules — a combinator of nothing must not silently resolve. " +
                    "Fix: add rules to the $where {} block, or remove it."
        }
        check(all.none { it.containsConstant() }) {
            "$where {} must not contain the constant rules public()/forbidden() — they make the " +
                    "combinator meaningless (an always-allow/deny). Fix: remove the constant; use it " +
                    "as the sole rule of the route/floor instead."
        }
        check(all.none { it.containsEmptyComposite() }) {
            "$where {} contains an empty And/Or composite — an empty AND allows everyone, an empty " +
                    "OR denies everyone. Fix: remove the empty combinator, or add rules to it."
        }

        return all
    }
}
