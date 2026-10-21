# Docs: Kronos after the advanceBy collapse

**Status:** DONE (archived 2026-10-18)
**Plan:** follow-up of ULTRA-00205 (kronos advanceby collapse) (public API touched)
**Security-critical:** no

## Spec

Rewrite (don't patch) the Kronos sections of `docs-site/src/pages/ultra/datetime/kronos.astro` and the LLM
mirror `docs-site/src/data/llms/datetime.md` against the settled code in
`ultra/datetime/src/commonMain/kotlin/Kronos.kt`:

- [x] **Pre-existing errors:** `kronos.astro:77-79` claims `mutable.advanceBy(1.hours)` moves the mutable — it
      returns a new Kronos; the idiom is `mutable.mutate { advanceBy(1.hours) }`. `kronos.astro:116-118`
      `KronosDescriptor.AdvancedBy(millis = …)` does not compile (fields: `durationMs`, `inner`).
      `kronos.astro:107` "useful for persisting": lossy for fixed clocks (a fixed clock describes as
      `SystemClock`).
- [x] `advanceBy { }` vs `advanceBy(Duration)`: the lambda is called once, on the first read, never while
      chaining, and retried after a throw (use case: an offset loaded from a database).
- [x] Chained advances collapse: any number of `advanceBy` / `mutate { advanceBy(..) }` steps stays one
      layer; `describe()` is one flat `AdvancedBy` per chain; old nested descriptors still load.
- [x] `Kronos.Mutable.set` / `mutate` throw `IllegalArgumentException` for a kronos that reads the mutable
      itself (seen through mutable and advanceBy layers only); not safe against concurrent writers.
- [x] Keep the mirror shorter than the page (CLAUDE.md "Keep docs compact").

## Done

- `docs-site/src/pages/ultra/datetime/kronos.astro` — Advancing clock (collapse incl. `mutate { advanceBy }`, lazy
  provider: once, never while chaining, retried after a throw), Mutable clock (`mutate`/`set`, `advanceBy` on a
  mutable returns a follower, cycle guard throws + its limits), KronosDescriptor (flat chain, old nested load,
  fixed-clock Aside). Pre-existing errors fixed: `mutable.advanceBy`, `AdvancedBy(millis = …)`, "for persisting",
  `localDateNow()` without a timezone, `println` comments that did not match `MpInstant.toString()`.
- Mirror `docs-site/src/data/llms/datetime.md` "Kronos — Testable Clock" rewritten, shorter than the page.
- Same session, chartjs (from `docs/tasks-archive/2026-10/20261018-kraft-chartjs-live-updates.md`):
  `docs-site/src/pages/ultra/kraft/addons.astro` chartjs section (update contract; removed an unused
  `subscribingTo`), mirror `docs-site/src/data/llms/kraft.md` chartjs section (cut to the page's example + gotchas),
  `kraft/addons/chartjs/README.md` (its example used a DSL that does not exist).
- `pnpm run build` in `docs-site` green; rendered HTML and `dist/llms/*.md` checked for the new text.

## Review record

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | MAJOR | `kronos.astro:89-91`, `datetime.md:283` | Cycle guard documented without its limits (spec item 4): a lambda reading the mutable, custom clocks, concurrent writers get past it | fix | Verified against `Kronos.kt` guard walk; limits added to page + mirror |
| 1 | MINOR | `kronos.astro:67-68` | Collapse via repeated `mutate { advanceBy }` not stated (spec item 3) | fix | Added |
| 1 | MINOR | `addons.astro:232-233` | "keeps its identity" hides index matching | fix | Reworded |
| 1 | MINOR | `addons.astro` vs `kraft.md` | Mirror longer than page, page missing facts, promotional line | fix | Page gained the facts; mirror cut |
| 1 | MINOR | `addons.astro:235`, `kraft.md` | `options.animation = false` does not compile with the typed builder | fix | `asDynamic()` form (refined in round 2) |
| 1 | MINOR | `addons.astro:210` | "just use it" omits registering `chartJs()` | fix | Added |
| 2 | MINOR | `kronos.astro` | `println` comments are not what prints (`MpInstant(…000Z)`) | fix | Comments now annotate the value |
| 2 | MINOR | `kronos.astro:129` | `10.minutes` without import | fix | Import added |
| 2 | MINOR | `addons.astro:237`, `kraft.md` | `options.asDynamic().animation` throws when the config has no options | fix | `options = jsObject { asDynamic().animation = false }` |
| 2 | MINOR | `kraft/addons/chartjs/README.md` | README example uses a non-existent DSL | fix | Rewritten against the real API |

Round 1: one `reviewer-high` (doc-only, scope by risk). Round 2: fresh `reviewer-xhigh`, 0 CRITICAL / 0 MAJOR →
clean; it re-verified the round-1 MAJOR fix (guard limits match the `Mutable` KDoc) in phase 1, so no separate
reconcile phase. **Verdict: PASS.**

## History

- 2026-10-18: created on archiving the advanceBy-collapse task.
- 2026-10-18: done together with the chartjs docs; archived.
