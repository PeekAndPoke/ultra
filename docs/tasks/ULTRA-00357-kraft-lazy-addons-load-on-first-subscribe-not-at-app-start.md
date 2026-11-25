# ULTRA-00357: Kraft: lazy addons load on first subscribe, not at app start

> **Status (2026-11-25): DONE.** Gate PASS in round 1; merged into master.

## Why this exists

Found by the ULTRA-00356 truth audit (Kraft part 3): an addon registered with `lazy = true` (e.g. `pixiJs(lazy = true)`)
still loads at app start. `AddonRegistry.loadEager()` (kraft/core/src/jsMain/kotlin/addons/registry/AddonRegistry.kt)
calls `load()` on every addon, and `Addon.load()` calls `ensureLoading()` with no check, although the KDoc of
`loadEager` says "Triggers loading of all non-lazy addons". The docs (addons page, LLM mirror, pdfjs page and README)
promise that a lazy addon loads only when the first component subscribes. The tests in `AddonRegistrySpec` pass anyway
because the fake load is delayed by 100 ms, so `addon() shouldBe null` holds either way.

Maintainer (2026-11-25): "Fix the code".

## The work

- Tests first, RED against the current code: after `loadEager()`, a lazy addon has not started loading (its loader
  was not called); a non-lazy one has; subscribing to the lazy addon starts the load once.
- Fix: `loadEager()` loads only the non-lazy addons.
- Mutation check; kraft core-tests green.
- CHANGELOG (coordinator): Fixes.

## Implementation notes

- `Addon.load()` became `loadIfEager()`: `if (!lazy) ensureLoading()` (kraft/core/src/jsMain/kotlin/addons/registry/Addon.kt).
  `lazy` stays private; `AddonRegistry.loadEager()` calls `loadIfEager()`.
- Two new tests in `AddonRegistrySpec` count loader calls instead of reading the addon (a read starts a lazy load):
  after `loadEager()` a lazy loader has run 0 times, and once after a subscribe, a second subscribe and a read; a
  non-lazy loader runs once over two `loadEager()` calls.

## Test evidence

- RED before the fix: "loadEager does not call the loader of a lazy addon" failed, expected 0 but was 1.
- Mutant `if (lazy) ensureLoading()` in `loadIfEager`: 6 tests RED, both new ones among them. Restored (cmp).
- `:kraft:core-tests:jsTest` green: 593 tests, 0 failures (XML).

## Review ledger

Round 1, standard gate, one reviewer covering the three charters (implementation and code style, domain, security),
rung 1 reviewer-opus-high (Opus high while the Fable credits are out). Verdict: PASS, 0 CRITICAL, 0 MAJOR.

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | MINOR | AddonRegistrySpec.kt | no test shows a read alone starts a lazy load | fix | new test "a read alone starts loading a lazy addon"; mutant without the read trigger RED |
| 1 | MINOR | AddonRegistry.kt:12 | KDoc example `addons.marked.ready` does not compile | fix | now `subscribingTo(addons.marked)` |

CLEAN: no in-repo caller relies on eager loading (all consumers subscribe); no path where a lazy addon never loads
(Stream has only invoke and subscribeToStream, both trigger); re-entrancy safe (single thread, guard before launch);
no security surface. Not part of this ticket: a loader that throws is never retried (older, applies to eager addons).

## Acceptance

- A lazy addon's loader runs on the first subscribe or value read, never in `loadEager()`; non-lazy addons unchanged.

## History

- 2026-11-25: created on the maintainer's decision.
- 2026-11-25: built in wt/kraft-lazy, tests first (RED before, mutant RED), core-tests 593 / 0.
- 2026-11-25: gate PASS round 1; MINORs applied by the coordinator (read-trigger test, KDoc example, KDocs name
  the read); read-trigger mutant RED; core-tests 594 / 0 (XML).
