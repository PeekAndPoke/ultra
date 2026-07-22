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
2. **Composition via BLOCK-STYLE SUB-BUILDERS** (refined 2026-07-22), because value-arg
   composers + append-on-call double-register and DISTORT correct code —
   `forAny(isSuperUser(), forRole("org-admin"))` appends both args plus the OR, enforcing
   `AND(super, org-admin)`: the written disjunction becomes a conjunction. Therefore no value
   composition exists in the DSL at all; combinators are blocks and statements are their children:
   ```kotlin
   authorize {
       forAny {
           isSuperUser()
           forAll {
               forUserType(B2bUserModel.USER_TYPE)
               forRole("org-admin")
           }
       }
   }
   ```
   Structure: `@DslMarker`-annotated common interface `AuthRuleBuilder<P, B>` (leaf factories +
   `forAll {}` + `forAny {}`) implemented by `RootAuthRuleBuilder` (AND chain; seed replay;
   `public()` lives ONLY here), `AllAuthRuleBuilder` (AND node), `AnyAuthRuleBuilder` (OR node).
   Statements inside `forAny` are disjuncts; conjunction within a disjunct is an explicit nested
   `forAll`. The rule tree is literally what is written.
   - Members shared by all levels bind to the INNERMOST receiver by Kotlin resolution — correct
     level for free. The `@DslMarker` is load-bearing for level-specific members: without it,
     `public()` inside `forAny {}` silently falls back to the OUTER root receiver (appending an
     always-allow at root level); with it, implicit outer access is a COMPILE ERROR. (Counterpoint
     to the removed KraftForms markers: those scoped nothing; this DSL nests same-family
     receivers with level-specific members — the exact case the marker exists for.)
   - The infix `AuthRule.or`/`and` (`AuthRule.kt:199,210`) are REMOVED (zero production call
     sites). Value-arg `forAny`/`forAll` are removed from the builder; `AuthRule.Companion`
     factories and the `OrAuthRule`/`AndAuthRule` node classes remain for programmatic use.
   - Empty sub-block (`forAny { }` / `forAll { }`) is a mount/boot-time error, same as an empty
     root chain — an OR of nothing must never silently resolve.
3. **Migration is near-free:** every existing block in the repo is single-rule except the one
   `forAll(isSuperUser(), forUserType(...))` in `OperatorApi` (becomes two statements). Any
   multi-statement block that exists today silently enforced only its last rule — under the new
   semantics it becomes stricter, i.e. latent fail-open bugs auto-heal fail-closed. Audit all
   `authorize {` blocks during migration anyway (grep; there are ~30).
4. **Empty chain fails at mount/boot time**: a DECLARED `authorize {}` block that produces zero
   rules (and any empty `forAll {}`/`forAny {}` sub-block) aborts app start with a clear message
   ("declare public() explicitly"). NOTE the precise scope: routes that never call `authorize`
   remain legal (and public) in THIS task — closing that gap repo-wide is exactly the mandatory
   floor of `20260722-apiroutes-auth-floor.md`; flipping it here would front-run part 2's sweep.
5. `public()` becomes a **typed marker rule** so later stages (floor suppression checks, docs)
   can recognize it structurally.

### Factory return type — RESOLVED (2026-07-22, follows from block-style)

Builder factories return **Unit** (append-only). With block-style combinators there is no value
composition anywhere in the DSL, so nothing needs a rule-typed return, and the whole
expression-composition trap class (`a() or b()` inside a block) is unrepresentable. Consequence
accepted: the floor task's "seed must be non-empty" guarantee is a BOOT-TIME check (root chain
and every sub-block), not a compiler-forced return — boot failure is compile-adjacent in
practice.

## Ambiguity inventory — the goal is ZERO (user directive, 2026-07-22)

Every way to write something that reads differently than it enforces must be unrepresentable,
a compile error, or a boot error. Complete inventory; the review loop must re-audit it:

| # | Ambiguity | Killed by |
|---|---|---|
| 1 | Last-expression-wins (`forRule1(); forRule2()` enforces only the last) | Accumulator: statements append, all enforced |
| 2 | Value/infix composition double-registers (written OR enforces AND) | Block-style only; infix `AuthRule.or`/`and` DELETED (zero call sites); no value-arg composers on builders |
| 3 | Level-specific member binds to OUTER receiver (`public()` inside `forAny {}` silently appends always-allow at root) | `@DslMarker` on the receiver TYPES (compile error). **Finding:** the existing `RestDslMarker*`/`RestAuthRuleMarker` annotations are applied to FUNCTIONS, where `@DslMarker` has no scoping effect — they are decorative today. Move to ONE shared marker on all route-DSL receiver types (mount/docs/codeGen/authorize/auth-builders) so CROSS-family implicit access (`docs {}` inside `authorize {}`) errors too; delete the decorative function annotations |
| 4 | Labeled-receiver escape (`this@authorize.public()` inside a nested block) — not compile-blockable | Neutralized by #5: the boot check runs on the final chain, catching any append path |
| 5 | Constant rules as chain members: `public()` in an AND chain is an always-true NO-OP that looks meaningful; `public()` as an OR disjunct is always-allow; `forbidden()` in an OR is a no-op | Constant rules (`public`, `forbidden`) are valid ONLY as the SOLE rule of the entire route chain (public ⇒ the whole floor of a public group; forbidden ⇒ explicit dead-route). Any other position ⇒ boot error |
| 6 | Empty root chain / empty `forAll {}` / `forAny {}` (OR-of-nothing) | Boot error |
| 7 | Double `authorize` on one route — today silently APPENDS (`ApiRoute.kt:106` `authRules.plus`) | Exactly ONE user authorize block per route; a second ⇒ build error. Framework paths (floor seed, interface-triggered auto-rules) append internally, not via `authorize` |
| 8 | Undefined interleaving of seed / route block / auto-rules | Documented stable order: seed → route block → auto-rules (no semantic effect under AND; matters for docs + phase grouping) |
| 9 | Programmatic escape hatch reintroducing traps | `appendRule(rule)` stays public and is SAFE by construction: `AuthRule.Companion` factories never auto-append, so nothing double-registers; the append is the single explicit act |

**Flagged sibling (decide separately, not this task's scope):** the mount chain itself is
fluent-copy — unchained statements (`docs {...}` then `codeGen {...}` on separate lines) silently
discard configuration, the same disease one level up. The route still registers only via the
final returned value, so auth is not affected, but docs/codeGen can vanish silently. Record for
a follow-up decision; review round 1 may pull it in.

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

- [ ] Unit specs in funktor/rest jvmTest: accumulation, block composition (forAny/forAll nesting
      builds the literal tree), boot failures (#5, #6, #7 from the inventory).
- [ ] COMPILE-REJECTION tests via kctfork (precedent: i18n S2): `public()` inside `forAny {}`
      does not compile (marker); cross-family implicit access (`docs {}` inside `authorize {}`)
      does not compile.
- [ ] The existing 401/403 e2e matrix (OperatorApiTest, B2bAuthFlowTest, B2b2cAuthFlowTest,
      AuthApiSpec) green unchanged — proves no semantic drift on single-rule blocks.

## Cross-references

- `20260720-operator-api-feature.md` (archived) — the cross-realm HIGH that motivated structural
  enforcement; `20260719-cross-realm-authz-and-tests.md` — forUserType primitive this composes with.
- Successor: `20260722-apiroutes-auth-floor.md` builds the mandatory seed ON this builder.
