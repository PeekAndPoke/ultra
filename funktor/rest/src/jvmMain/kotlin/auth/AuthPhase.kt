package io.peekandpoke.funktor.rest.auth

/**
 * Two-phase authorization: the REST dispatch evaluates a route's auth chain in two passes around
 * request-param conversion (which triggers `findById` entity loads — see the REST `routing.kt`):
 *
 * - **Phase 1 — caller-only rules, BEFORE conversion.** Rules whose decision depends only on the
 *   caller (their identity/permissions), never on the request params/body. The mandatory group
 *   floor (part 2) is caller-only by construction, so on every non-public route at least the floor
 *   runs here — an unauthorized caller is rejected with ZERO entity loads (no pre-auth DB reads,
 *   no 404-vs-401 existence oracle).
 * - **Phase 2 — param-dependent rules, AFTER conversion.** Everything else: `forCall {}` closures
 *   and the framework-appended [ConsistentParamRule] / [CallerScopedParamRule], which need the
 *   resolved params to decide.
 *
 * Phase 1 is evaluated through [AuthRule.estimate] (caller-only by contract — it takes an
 * [AuthRule.EstimateCtx], which carries no params). For every rule type classified caller-only
 * below, `estimate` and `check` are defined to agree, so estimating in phase 1 yields exactly the
 * same decision `check` would — this equivalence is the invariant that keeps the split sound, and
 * any new caller-only rule type MUST preserve it.
 */

/**
 * True when this rule's decision can NEVER depend on the request params/body — so it is safe to
 * evaluate in phase 1, before param conversion.
 *
 * Classification law:
 * - [PublicRule] / [ForbiddenRule] — constant, caller-independent → caller-only.
 * - [PermissionsCheck] / [AccessLevelCheck] — evaluate the [AuthRule.EstimateCtx] (caller) only.
 * - [CallCheck] — the `forCall {}` escape hatch reads the full [AuthRule.CheckCtx] → param-dependent.
 * - [AndAuthRule] / [OrAuthRule] — caller-only IFF ALL members are caller-only. A composite mixing
 *   a caller-only and a param-dependent member is param-dependent AS A WHOLE (phase 2) — BUT a
 *   top-level AND is first flattened (see [flattenTopLevelAnds]), so the caller-only conjuncts of a
 *   top-level `forAll { … }` still gate phase 1 (this realizes the "an AND splits its members"
 *   law). Only a mixed OR, or a mixed AND nested inside an OR, is demoted wholesale — an OR cannot
 *   be split, and the floor still guarantees phase-1 gating on the route regardless.
 * - anything else (a custom [AuthRule], the param auto-rules) — conservatively param-dependent: it
 *   runs in phase 2 with the full context available, which is always safe (the floor gates phase 1).
 */
internal fun AuthRule<*, *>.isCallerOnly(): Boolean = when (this) {
    is PublicRule<*, *>, is ForbiddenRule<*, *> -> true
    is PermissionsCheck<*, *>, is AccessLevelCheck<*, *> -> true
    is CallCheck<*, *> -> false
    is AndAuthRule<*, *> -> rules.all { it.isCallerOnly() }
    is OrAuthRule<*, *> -> rules.all { it.isCallerOnly() }
    else -> false
}

/**
 * Flattens top-level [AndAuthRule]s into their members. The whole route chain is an implicit AND
 * (all top-level rules must pass — see `checkAccess`/the dispatch), and a nested `AndAuthRule` is
 * just a nested AND, so re-associating it into the top level preserves the access decision exactly.
 * Flattening lets the phase split classify each conjunct individually — the caller-only conjuncts of
 * a top-level `forAll { … }` gate phase 1 (before conversion) instead of the whole `forAll` being
 * demoted to phase 2. `OrAuthRule`s are NOT flattened (an OR is not associative with the outer AND).
 */
internal fun flattenTopLevelAnds(rules: List<AuthRule<*, *>>): List<AuthRule<*, *>> =
    rules.flatMap { rule ->
        when (rule) {
            is AndAuthRule<*, *> -> flattenTopLevelAnds(rule.rules)
            else -> listOf(rule)
        }
    }

/**
 * Marker for phase-2 rules whose failure must be indistinguishable from a genuine not-found. The
 * dispatch, on a failed rule carrying this marker, throws the SAME `NotFoundException` that param
 * conversion throws on a missing entity, so the response is byte-identical whether the entity does
 * not exist or exists-but-is-not-the-caller's. Only the framework-appended [ConsistentParamRule] /
 * [CallerScopedParamRule] carry it — an ordinary `authorize {}` rule fails as 401.
 */
internal interface HideFailureAsNotFound
