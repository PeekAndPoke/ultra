# ULTRA-00357: Kraft: lazy addons load on first subscribe, not at app start

> **Status (2026-11-25): DESIGNED.** Maintainer chose to fix the code; implementation next.

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

## Acceptance

- A lazy addon's loader runs on the first subscribe or value read, never in `loadEager()`; non-lazy addons unchanged.

## History

- 2026-11-25: created on the maintainer's decision.
