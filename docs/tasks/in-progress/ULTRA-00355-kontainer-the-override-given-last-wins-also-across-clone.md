# ULTRA-00355: Kontainer: the override given last wins, also across clone

> **Status (2026-11-25): DONE.** Gate PASS in round 1; merged into master (143328c3).

## Why this exists

Found by the ULTRA-00338 docs review and confirmed by a probe: when two overrides hit one dynamic service under
different classes, a class given again keeps its FIRST place, so it can lose to an override given before it.

- `create { with(Base){A}; with(Derived){B} }.clone { with(Base){C} }` yields B: the clone's new override is
  silently not used.
- `create { with(Base){A}; with(Derived){B}; with(Base){C} }` yields B.

Cause: `DynamicOverrides.merge` is `this.overrides + other.overrides` and the builder writes `overrides[srv] = ...`;
both keep an existing key's position in the LinkedHashMap. `ServiceProviderFactory` builds the override map with
`associate`, so the last entry in iteration order wins per overridden service.

## Decision (maintainer, 2026-11-25)

"Fix: last given wins". A class given again moves to the end: the override given last wins, and a clone's overrides
take precedence over the parent's. This makes the docs' "the later one wins" and "new overrides take precedence" true.

## The work

- Tests first, RED against the current code: both examples above, plus the plain cases that must stay (one override,
  two classes in order, clone overriding the same class as the parent).
- Builder: remove then put. `merge`: `(this.overrides - other.overrides.keys) + other.overrides`.
- Mutation check of both spots; kontainer suite green.
- CHANGELOG (coordinator): Fixes.

## Acceptance

- The override given last wins in one `create`, and across `create` plus `clone`; every other case unchanged.

## Implementation notes

- Builder: `DynamicOverrides.Builder.with` (`ultra/kontainer/src/main/kotlin/DynamicOverrides.kt:29`) removes the
  class, then puts it: a class given again moves to the end.
- Merge: `DynamicOverrides.merge` (`DynamicOverrides.kt:36`) is `(this.overrides - other.overrides.keys) +
  other.overrides`: the clone's classes move to the end, the parent's others keep their order.
- Other paths that build or merge overrides: none. `build()` copies with `toMap()` (order kept);
  `ServiceProviderFactory.newKontainer` is the only caller of `merge` (clone); `ServiceProviderFactory` turns the map
  into providers with `associate`, last entry per overridden service wins (unchanged); `KontainerBlueprint`
  constructs only empty overrides. funktor (`funktor/core/src/jvmMain/kotlin/app.kt:202`) only passes a builder block.
- Side effect of the decision: the override lambdas run in map order, so their run order follows the order given too.
  A class given again now runs at its new place, and at clone the parent's remaining overrides run first, then the
  clone's in the order given. Two existing pins in `DynamicsAtCreateCharacterizationSpec` changed with it:
  - `:422` (ULTRA-00344): `A1, B, A2` ran `A2, B`, now `B, A2`; the clone adding `C, A3` ran `A3, B, C`, now
    `B, C, A3`.
  - `:510` (ULTRA-00345, "decided to stay" then): the clone's `with(Base)` now wins over the parent's `with(Sub)`;
    lambdas `b, x` instead of `x, b`.
- Tests: new `ultra/kontainer/src/test/kotlin/e2e/OverrideOrderSpec.kt` (9 tests). Changed: both ticket examples
  (`:25`, `:41`) and a clone of a clone (`:60`). Unchanged: no override and one override (`:78`), two classes in
  either order (`:86`), the same class twice in one create (`:104`), a clone overriding the parent's class (`:119`), a
  clone without overrides (`:130`), a clone adding a new class (`:148`).

## Test evidence

- RED before the fix (XML): `OverrideOrderSpec` 9 tests, 3 failed, all for the right reason. Both examples yield
  `B` instead of `C` and run the lambdas `C, B`; the clone chain yields `B, B, D, D` instead of `B, C, D, E`. The 6
  unchanged cases passed. (A first run failed all 9 because the fixture `OrderBase(name: String)` had no factory; that
  was fixed before this count.)
- Kontainer suite after the fix (XML, `-x generateDocs`): 661 tests, 0 failures, 0 errors (652 + 9 new).
- Mutants (each from a `cp` backup, restore checked with `cmp` and `git status`; failures from XML):

| # | Mutant | Result |
|---|--------|--------|
| N1 | builder without `remove` (old put in place) | RED, 2 (example 2, lambda order pin) |
| N2 | merge back to `this.overrides + other.overrides` | RED, 4 (example 1, clone chain, both clone pins) |
| N3 | merge with the parent taking precedence for a shared class | RED, 6 (also the same-class clone case) |
| N4 | merge drops the parent's overrides | RED, 9 |

- Full suite not run (coordinator: only if something outside kontainer breaks; nothing outside kontainer changed).

## Review ledger

Round 1, standard gate, three charters (implementation and code style, domain, security), rung 1 reviewer-opus-high (Opus high while the Fable credits are out), for ULTRA-00354 and
ULTRA-00355 together. Zero CRITICAL, zero MAJOR: gate PASS in round 1. This ticket's rows:

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | MINOR (domain) | `CHANGELOG.md` | The CHANGELOG must name the run-order change of the override lambdas | coordinator | CHANGELOG names the run order |
| 1 | MINOR (impl) | `ultra/kontainer/src/main/kotlin/DynamicOverrides.kt:29`, `:36` | The lambdas run in a new order, not covered by "every other case unchanged" | coordinator | CHANGELOG names the run order |
| 1 | security | | NO FINDINGS | | |

CLEAN in round 1, one line each:
- Bypass paths: none; every registration form and `with` at create and clone reach the checks.
- Allowed types are not refused: `Number`, `CharSequence`, arrays, value classes of one's own, a factory returning a
  value, `instance(Number::class, 5)`.
- Request isolation: overrides stay per kontainer; a clone does not change its parent.
- Concurrency of `merge`: it builds new immutable maps and shares no mutable state.
- Messages carry class names only, no values.
- Pins changed by the decisions: none weaker; each changed pin asserts the new behaviour as strictly as before.

## History

- 2026-11-25: created on the maintainer's decision.
- 2026-11-25: IN PROGRESS.
- 2026-11-25: implemented on `wt/kontainer-values`; spec RED before, 4 mutants RED; status IN PROGRESS, review gate next.
- 2026-11-25: gate round 1 PASS (0 CRITICAL, 0 MAJOR); both MINORs go to the CHANGELOG (coordinator: names the
  run order). Kontainer 661 / 0 / 0 (XML). Ready to merge.
- 2026-11-25: MINOR batch hand-checked by the coordinator (message hint, literal names); merged into master
  (143328c3); CHANGELOG written. Docs: the services page and the mirror carry the value-type sentence.
  No DOCS follow-up needed.
