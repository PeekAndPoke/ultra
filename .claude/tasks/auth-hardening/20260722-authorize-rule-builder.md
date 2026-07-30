# authorize {} builder-accumulator — kill the last-expression-wins footgun

**Status:** DONE (2026-07-22) — review loop terminated on round 4 (zero confirmed findings across
all three reviewers). All 4 rounds' findings fixed; 27 funktor:rest specs + full backend suites green.
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
| 4 | Labeled-receiver escape (`this@authorize.public()` inside a nested block) — not compile-blockable | Neutralized by #5: the whole-chain `validateChain` runs on the final `authRules` at boot, catching any append path |
| 5 | Constant rules as chain members: `public()` in an AND chain is an always-true NO-OP that looks meaningful; `public()` as an OR disjunct is always-allow; `forbidden()` in an OR is a no-op | Constant rules (`public`, `forbidden`) are valid ONLY as the SOLE, BARE rule of the entire route chain. Enforced in TWO places: `RootAuthRuleBuilder.build()` per-block for early feedback, AND `AuthRuleBuilder.validateChain(route.authRules)` at boot (`ValidateRoutesOnAppStarting`) over the WHOLE combined chain — so a constant surviving any path (seed + user block, direct `copy`, `appendRule` of a composite) is a boot error. Requires a BARE constant (`is PublicRule`/`is ForbiddenRule`), so a composite that merely *contains* one is rejected |
| 6 | Empty root chain / empty `forAll {}` / `forAny {}` (OR-of-nothing) | Boot error. Also: empty And/Or nodes anywhere in the tree (an empty AND folds to allow-all) rejected by `containsEmptyComposite` in both `build()` and the boot `validateChain` |
| 7 | Double `authorize` on one route — silently APPENDED before | Exactly ONE user authorize block per route; a second ⇒ build error, keyed off a file-private `UserAuthorizeDeclaredKey` attribute (NOT `authRules` emptiness, so framework paths — floor seed, auto-rules — that pre-populate the chain don't trip it). `authorize` APPENDS to the chain (seed → block → auto-rules) |
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

- [x] Accumulating `AuthRuleBuilder` (sealed; `RootAuthRuleBuilder` + `SubAuthRuleBuilder`) with
      block-style `forAll {}`/`forAny {}`.
- [x] Value-arg composers + infix `AuthRule.or`/`and` removed; `AuthRule.Companion` factories +
      `Or`/`AndAuthRule` node classes remain for programmatic use.
- [x] Two-statement block enforces BOTH rules; a leading-deny/last-grant case proves it through the
      decision path (would GRANT under old last-wins).
- [x] `forAny`/`forAll` block semantics + nesting build the literal tree.
- [x] Empty chain / empty sub-block / empty composite / misplaced constant ⇒ boot failure.
- [x] `public()`/`forbidden()` typed `PublicRule`/`ForbiddenRule`.
- [x] Whole-chain `validateChain` reused by `build()` (per-block) AND `ValidateRoutesOnAppStarting`
      (boot backstop over final `authRules`) — catches seed+block combos and direct `copy`.
- [x] One-per-route guard keyed off a file-private `UserAuthorizeDeclaredKey` attribute (part-2 seed
      compatible); `authorize` appends.
- [x] All existing authorize call sites compile; OperatorApi migrated; full backend suites green.

## Test evidence

- [x] `AuthRuleBuilderSpec` (20 cases): accumulation, footgun-through-decision-path, block
      composition/nesting, all boot failures (#5/#6/#7), whole-chain validator (seed+block, direct
      copy), `appendRule` safety, `forbidden()` legal + rejected.
- [x] `AuthRuleDslCompileSpec` (4 kctfork cases): valid nesting compiles; `public()` inside
      `forAny {}` and `docs {}` inside `authorize {}` fail to compile with the marker-specific
      "implicit receiver" diagnostic; chained-outside control compiles.
- [x] Backend suites green (funktor:rest/auth/all, funktor-demo:server) — the boot validator now
      runs against every real route at app start, so the e2e apps exercise it (no route reddened).

## Review record (review LOOP, 2026-07-22, 3× Opus per round)

**Round 1** — confirmed + fixed: HIGH soleness exception used deep `containsConstant()` (a composite
burying a constant passed as "sole") → require a BARE constant; + empty-composite check; LOWs
(compile-test diagnostic assertion, KDoc ×2, behavioral footgun test, snapshot invariant comment).
Security drift audit: all 82 `authorize` blocks enumerated, OperatorApi the only multi-statement one,
zero drift.

**Round 2** — security: ZERO findings (TypedKey equality is by identity → attribute-guard spoof
impossible; constants final; migration drift-free). impl/style: LOW `forbidden()` untested → added.
domain: MEDIUM the soleness invariant was per-block, not whole-chain (my round-1 "seed+block boots
clean" claim was right, contradicting an earlier note) → hoisted into the reusable `validateChain`
run at boot over the final chain; part 2's "public-seeded group must not also seed restrictive rules"
check now comes for free. Dead infix `Or/AndAuthRule.or/and` removed.

**Round 3** — security + domain: ZERO findings (security independently traced that the boot
validator is a kontainer-registered `OnAppStarting` hook firing inline on real prod boot via
`App.module → setupLifecycle → runHooks`, aborting startup, over the same route set the dispatcher
serves). impl/style: 2 LOWs → fixed: the `AppStartException` header said "converter validation
failed" for auth failures too (→ "Route validation failed"); the boot backstop's failure path was
untested → extracted `validateOrThrow()` (no `Application` dep) + `ValidateRoutesOnAppStartingSpec`
(bad chain aborts, healthy+empty pass, two-bad aggregation). One INFO overload-consistency nit left
as-is (mirrors `AuthRule.Companion`).

**Round 4** — impl/style, domain, security ALL ZERO confirmed findings (fresh reviewers, whole
diff). Verified: `validateOrThrow` extraction behavior-preserving; boot backstop fires on real prod
boot and aborts startup; whole-chain validator covers every DSL-bypass path; constants/nodes final;
TypedKey identity-equal + fail-closed guard; migration AND-identical, zero drift. **Loop terminated
— zero-findings round reached.** One INFO (builder overload asymmetry mirroring `AuthRule.Companion`)
consciously left as-is.

## Cross-references

- `20260720-operator-api-feature.md` (archived) — the cross-realm HIGH that motivated structural
  enforcement; `20260719-cross-realm-authz-and-tests.md` — forUserType primitive this composes with.
- Successor: `20260722-apiroutes-auth-floor.md` builds the mandatory seed ON this builder — it
  INHERITS `validateChain` (whole-chain boot check) as the seam for "public-seeded group must not
  also carry restrictive rules"; the seed pre-populates `authRules` and the user block appends.
