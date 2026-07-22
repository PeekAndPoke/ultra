# authorize {} builder-accumulator — kill the last-expression-wins footgun

**Status:** TODO (designed + agreed 2026-07-22)
**Plan:** part 1 of the auth-hardening quartet: this → `20260722-apiroutes-auth-floor.md` →
`20260722-two-phase-auth-consistent-params.md` → `20260722-stored-param-migration.md`
**Security-critical:** YES — this changes how every API auth rule is declared.

## Review protocol (user directive, 2026-07-22 — applies to all four quartet tasks)

Deep review LOOP, not a single pass:

1. Full 3-agent gate (impl+style, domain, security — all `opus`) on the complete diff; every
   finding adversarially verified by the coordinator before acceptance.
2. Fix ALL confirmed findings — no severity threshold; confirmed LOWs get fixed too.
3. Re-run the FULL gate on the updated diff (fresh reviewers, whole diff — not just the fix delta).
4. Repeat 2–3 until a round produces ZERO confirmed findings. **One pass is not enough.**
5. **ESCALATION:** if a round surfaces something fundamentally wrong with the agreed direction —
   not a fixable finding but "the design judgement itself was wrong" — STOP the loop and consult
   the user on direction before continuing. Design issues missed in the discussion are *expected*
   to surface here; that is the loop's job.

## Problem

`ApiRoute.authorize(builder: AuthRuleBuilder<P,B>.() -> AuthRule<P,B>)` uses the lambda's last
expression as THE rule. This compiles and silently enforces only `forRule2`:

```kotlin
authorize {
    forRule1()   // discarded — never enforced
    forRule2()
}
```

It *reads* as conjunction and *fails open*. `forAll(...)` exists only to work around this.

## Design (DECIDED 2026-07-22)

1. **Accumulator receiver.** Every rule-factory call on the builder appends to an internal rule
   chain; the route enforces the AND of the chain ("implicit forAll"). No re-parenting, no
   reordering, no removal — **the rules are what is written** (explicitly decided against
   composer re-parenting: silent removal is magic).
2. **Composition via lambda branches**, because value-arg composers + append-on-call
   double-register and DISTORT correct code — `forAny(isSuperUser(), forRole("org-admin"))`
   appends both args plus the OR, enforcing `AND(super, org-admin)`: the written disjunction
   becomes a conjunction (fails closed, but breaks both intended audiences). Therefore:
   ```kotlin
   authorize {
       forAny(
           { isSuperUser() },
           { forRole("org-admin") },
       )
   }
   ```
   Each branch runs in its own CHILD builder: statements AND within a branch, OR across branches,
   the composite appends to the parent. Uniform law: **a rule call registers with the block it
   syntactically sits in.** Value-arg `forAny`/`forAll` are REMOVED from the builder (kept on
   `AuthRule.Companion` for programmatic, non-DSL use). Top-level `forAll` becomes redundant
   (the block is the forAll); keep a lambda form only if nesting inside `forAny` needs it.
3. **Migration is near-free:** every existing block in the repo is single-rule except the one
   `forAll(isSuperUser(), forUserType(...))` in `OperatorApi` (becomes two statements). Any
   multi-statement block that exists today silently enforced only its last rule — under the new
   semantics it becomes stricter, i.e. latent fail-open bugs auto-heal fail-closed. Audit all
   `authorize {` blocks during migration anyway (grep; there are ~30).
4. **Empty chain fails at mount/boot time** (interim, until the mandatory floor of
   `20260722-apiroutes-auth-floor.md` makes emptiness impossible): a route whose chain is empty
   aborts app start with a clear message ("declare public() explicitly").
5. `public()` becomes a **typed marker rule** so later stages (floor suppression checks, docs)
   can recognize it structurally.

### OPEN design point (resolve at implementation start or in review round 1)

**Factory return type.** (a) Builder factories return `Unit` (append-only): value/infix
composition inside blocks becomes *unrepresentable* — also neutralizes the same trap via the
`AuthRule.or/and` infix operators — but the "seed must be non-empty" guarantee of the floor task
falls back to a boot-time check instead of a compiler-forced return. (b) Factories return the
rule: enables the compiler-forced non-empty seed initializer (user preference), but leaves
`isSuperUser() or forRole("x")` inside a block as a writable trap (both operands append, the OR
does not → enforced AND). Recommendation: **(a)**, boot-failure ≈ compile-failure in practice and
it kills the whole expression-composition trap class; the infix `or`/`and` on `AuthRule` then
either stay programmatic-only or get deprecated. Decide with the user if review disagrees.

## Blast radius

- `funktor/rest/src/jvmMain/kotlin/auth/AuthRuleBuilder.kt` (accumulator + child builders),
  `AuthRule.kt` (companion untouched; infix operators per open point),
  `ApiRoute.kt` (5 `authorize` overload signatures), route mount/validation.
- All `authorize {}` call sites (funktor/auth, funktor/saas, funktor/insights, funktor/inspect,
  funktor-demo) — expected source-compatible except OperatorApi's `forAll`.
- Docs/access-estimation: chain renders via the existing `AndAuthRule` description composition.

## Spec

- [ ] Accumulating `AuthRuleBuilder` with child-builder lambda branches for `forAny`.
- [ ] Value-arg composers removed from the builder; companion variants remain.
- [ ] Two-statement block enforces BOTH rules (regression test for the footgun).
- [ ] `forAny` branch semantics: AND within branch, OR across; nesting works.
- [ ] Empty-chain route ⇒ boot failure with actionable message.
- [ ] `public()` typed marker.
- [ ] All existing authorize call sites compile; OperatorApi migrated; full backend e2e suites
      green unchanged (funktor:auth, funktor:all, funktor-demo:server, saas, rest).

## Test evidence

- [ ] Unit specs in funktor/rest jvmTest: accumulation, branch composition, empty-chain boot
      failure, marker rule.
- [ ] The existing 401/403 e2e matrix (OperatorApiTest, B2bAuthFlowTest, B2b2cAuthFlowTest,
      AuthApiSpec) green unchanged — proves no semantic drift on single-rule blocks.

## Cross-references

- `20260720-operator-api-feature.md` (archived) — the cross-realm HIGH that motivated structural
  enforcement; `20260719-cross-realm-authz-and-tests.md` — forUserType primitive this composes with.
- Successor: `20260722-apiroutes-auth-floor.md` builds the mandatory seed ON this builder.
