# ULTRA-00345: Kontainer: an override naming a registered dynamic exactly replaces that one (exact match wins)

> **Status (2026-11-14): DONE.** Merged into master as `f750497f`: strict gate round 1 CLEAN; the close-out was tests, texts and one visibility narrowing. Docs: ULTRA-00338 (kontainer docs). are closed out (see Gate round 1), not merged.
>
> **Part of:** ULTRA-00255 (Epic: Ultra libraries (module scans and defects))
> **Plan:** ULTRA-00316 (close the open epics from the foundation upwards)

**Found by:** the delta review of ULTRA-00344 (eager overrides again), 2026-11-14.

## Why this exists

An override is given by a class; the kontainer finds the service it replaces by asking which registered dynamics
that class is assignable to. With a base and a more specific service both registered, the specific one can never be
overridden:

```kotlin
open class UserProvider
class AdminUserProvider : UserProvider()

val blueprint = kontainer {
    dynamic(UserProvider::class)
    dynamic(AdminUserProvider::class)
}

blueprint.create { with(UserProvider::class) { UserProvider() } }            // works
blueprint.create { with { AdminUserProvider() } }                            // ServiceAmbiguous
blueprint.create { with(AdminUserProvider::class) { AdminUserProvider() } }  // ServiceAmbiguous
```

"Service AdminUserProvider is ambiguous. It has multiple candidates: UserProvider, AdminUserProvider": the lookup
never checks whether one candidate is the class itself. The maintainer: "certainly not wanted".

## Decided (maintainer, 2026-11-14)

"Yes the exact match wins, if not exact match use the current method".

- The class given to `with` is itself a registered dynamic: the override replaces that service and nothing else.
- No exact match: as today. One registered dynamic the class fits is replaced; several is `ServiceAmbiguous`; none
  is refused as unexpected.
- Not changed: injection. A service injecting `UserProvider` in the blueprint above stays a validation error
  ("parameter is ambiguous", ULTRA-00334).

## The work

- The base-type lookup for overrides prefers the exact class; one place, used by the pre-check in `createChecked`
  and by the factory.
- Tests: the three calls above; `with { SuperAdmin() }` with `SuperAdmin : AdminUserProvider` not registered stays
  `ServiceAmbiguous`; the same at `clone`; mutation checks.
- Changelog: `## Unreleased`, new behaviour.

## Acceptance

- The table of the decision holds at `create` and `clone`; kontainer and the suites that build a blueprint are
  green; the gate is closed.

## Built (track D, 2026-11-14)

**Status: built on `wt/kontainer`, commit `5762cddd`, not merged; a strict gate follows.**

- **The rule, in one place.** `KontainerBlueprint.overriddenServiceOf(cls)` (internal) returns `cls` when it is an
  overridable dynamic of the blueprint, otherwise `dynamicsBaseTypeLookUp.getDistinctFor(cls)` as before. Both
  users call it:
  - the pre-check in `createChecked`, so an ambiguous override is still refused before any lambda runs
  - the factory loop that maps each override to the service it replaces
- **Why there, and not in `TypeLookup`:** the lookup is also used elsewhere, and injection must stay as it is. The
  lookup and its cache are unchanged; the exact check sits in front of them.
- **Exact membership** uses `KClass` equality, as the definitions map does. Only the fallback asks Java
  assignability, so a primitive and its wrapper behave no worse than before.
- **Not changed:** injection. A service injecting `UserProvider` with both registered is still "parameter is
  ambiguous" (pinned).

Tests (5 red before the fix: the four exact-match cases and the both-registered case; all green after):
- New `ExactOverrideMatchSpec`, with `UserProvider`, `AdminUserProvider : UserProvider` (both registered) and
  `SuperAdmin : AdminUserProvider` (not registered):
  - `with { AdminUserProvider() }` and `with(AdminUserProvider::class) { ... }` replace `AdminUserProvider` and leave
    `UserProvider` as it is
  - `with(UserProvider::class) { AdminUserProvider() }` replaces `UserProvider`; `AdminUserProvider` keeps its own
    default instance: the key decides, not the instance
  - `with { SuperAdmin() }` stays `ServiceAmbiguous`, with the literal message
  - the same at `clone`; a clone replaces its parent's override by the exact class and keeps the other one
  - injection of the base type stays ambiguous
- `DynamicsAtCreateCharacterizationSpec`:
  - the ambiguity tests now use an unregistered `Leaf : Inner`, since `Inner` itself is now an exact match
  - the two-overrides pin (`with(Base::class)` and `with(Derived::class)` for one registered `Base`) is decided:
    "an edge case, the user should be careful"; the later one wins
  - new: `Base` and `Derived` both registered and both overridden are two overrides
  - the "CURRENT BEHAVIOUR" pin of the public factory constructor goes; ULTRA-00346 makes that constructor internal

Mutation (failed / 574; each restore checked with `cmp`, the generated docs file included):

| # | Mutant | Result |
|---|---|---|
| X1 | Exact match ignored | RED, 5 |
| X2 | Exact taken for an unregistered class too (`else -> cls`) | RED, 20 |
| X3 | First candidate instead of `ServiceAmbiguous` | RED, 5 |
| X4 | Rule in the pre-check only, the factory uses the old lookup | RED, 5 |
| X5 | Rule in the factory only, the pre-check uses the old lookup | RED, 5 |
| G2 | Factory runs the lambda before resolving the base type | survives, equivalent |

G2 is equivalent: the factory is reached only through `createChecked` after the pre-check has resolved every
override, and through `validate`, which instantiates without overrides. With ULTRA-00346 the constructor is
internal, so no public path reaches the factory loop unchecked.

Suites (XML, `--rerun`, 0 failures, both commits of this batch):

| Suite | Tests |
|---|---|
| `:ultra:kontainer:test` | 574 (565 before) |
| `:ultra:log:jvmTest` | 74 |
| `:ultra:vault:jvmTest` | 287 |
| `:funktor:insights:jvmTest` | 118 |
| `:funktor:cluster:jvmTest` | 162 |
| `:funktor:all:jvmTest` | 289 |
| `:funktor:core:jvmTest` | 775 |
| `:funktor:rest:jvmTest` | 124 |
| `:funktor:auth:jvmTest` | 322 |
| `:funktor:messaging:jvmTest` | 67 |
| `:funktor:logging:jvmTest` | 14 |
| `:funktor:codegen:test` | 104 |
| `:funktor:saas:jvmTest` | 50 |
| `:funktor-demo:server:test` | 52 |

Kontainer: 565 + 7 (`ExactOverrideMatchSpec`) + 1 (both registered) + 2 (ULTRA-00346 visibility) - 1 (the public
constructor pin, ULTRA-00346) = 574. The repo-wide compile sweep is green (459 tasks, 0 errors).

The changelog text is under Gate round 1, it replaces the one first written here.

## Gate round 1 (strict, reviewer-fable-xhigh, all three charters, with ULTRA-00346)

**Verdict: CLEAN.** No CRITICAL, no MAJOR, 5 MINOR. Both rules are implemented as decided; refusal order and
messages are unchanged. The reviewer ran nothing; every mutant below was run by track D.

The five MINOR and their outcomes:
1. **The exact set was unpinned.** Widening it to every registered class, or to every dynamic, left all 574 tests
   green. Fixed with two tests, `dynamic(Mailer)` with `singleton(SmtpMailer)` and with `dynamicPrototype(SmtpMailer)`:
   `with { SmtpMailer() }` overrides `Mailer`, and `SmtpMailer` stays a static singleton or a dynamic prototype. W1
   and W2 are RED.
2. **Registration order.** Every fixture registered the base first, so "the last registered candidate" passed for
   "the exact one". Fixed with one case that registers `dynamic(AdminUserProvider)` first. X6 is RED.
3. **Surplus assertions.** The `shouldBeInstanceOf` on a value already of that type is dropped; the provider check and
   the "not the given instance" check remain. The `assertSoftly` around one assertion is unwrapped.
4. **`dynamicsBaseTypeLookUp`** is now `private`. Its only reader was `overriddenServiceOf`; no test or other file
   read it.
5. **The clone flavour** of "two overrides for one service, the later wins" is pinned, with no change (below).

**The clone flavour (5), pinned as decided behaviour:** a parent with `dynamic(Base)` only, created with
`with(Base::class) { a }` and `with(Sub::class) { b }`, then `clone { with(Base::class) { x } }`. The merged order is
`Base` (the clone's lambda) and then `Sub` (the parent's). Both lambdas run, the clone's result is dropped, and
`get(Base::class)` is a new `b`. Test: "A clone's override under the base class runs, but the parent's later override
under a subclass wins". **For the maintainer:** is this still "an edge case, the user should be careful" at `clone`,
where the user's own override is the one dropped?

**Primitives (recorded, pinned, no code change):**
- `dynamic(Int::class)` and `dynamic(Number::class) { 0.5 }`, then `create { with<Int> { 5 } }`:
  - On master, on a fresh blueprint, this replaced `Number`. The reified `Int::class` is `java.lang.Integer` on the
    Java side, so `int` is not assignable from it, but `Number` is.
  - Now it replaces `Int`, because the exact rule uses `KClass` equality.
  - This is the one blueprint shape where an exact match differs from a previous single candidate. The test is red
    under X1 (the old lookup), with `Number` reported as overridden.
- `dynamic(Int::class)` alone with `with { 5 }` is still refused as unexpected on a fresh blueprint, in both versions,
  with "Unexpected dynamics were provided: kotlin.Int". The checker uses Java assignability; it runs before the
  exact rule.

Mutation, round 1 (failed / 580; each restore checked with `cmp`, the generated docs file included):

| # | Mutant | Result |
|---|---|---|
| W1 | Exact set widened to every registered class but the built-ins | RED, 2 (both new "only overridable" tests) |
| W2 | Exact set widened to every dynamic (prototypes included) | RED, 1 (the dynamic prototype test) |
| X6 | The exact match picks the last registered candidate | RED, 2 (registered first; primitives) |
| X1 | Exact match ignored (re-run) | RED, 7 (now also registered first and primitives) |

Also added: in the first test, `get(UserProvider::class)` stays `ServiceAmbiguous` with both registered (the
changelog says so).

Counts after round 1 (XML, `--rerun`, 0 failures): kontainer 580 (574 + 2 exact set + 1 registration order + 2
primitives + 1 clone flavour), log 74, vault 287, insights 118, cluster 162, all 289, core 775, rest 124, auth 322,
messaging 67, logging 14, codegen 104, saas 50, demo server 52. The repo-wide compile sweep is green (459 tasks, 0
errors). The `get` assertion came after the full run; kontainer was re-run with it, 580 green.

**Changelog text (`## Unreleased`, new behaviour; replaces the one written at build):**
- `ultra/kontainer`, behaviour change: an override whose class is itself a registered dynamic replaces exactly that
  service. Before, with `dynamic(UserProvider::class)` and `dynamic(AdminUserProvider::class)` both registered,
  `with { AdminUserProvider() }` failed with `ServiceAmbiguous`. Otherwise as before: the one registered dynamic the
  class fits is replaced, several is `ServiceAmbiguous`. Reads through the base type are unchanged: with both
  registered, `get(UserProvider::class)` stays `ServiceAmbiguous`. Injection is unchanged (ULTRA-00345).

## History

- 2026-11-14: created on the maintainer's decision.
- 2026-11-14: built by track D (`5762cddd`), not merged; a strict gate follows.
- 2026-11-14: strict gate round 1 CLEAN (5 MINOR); close-out by track D, not merged.
- 2026-11-14: DONE.
