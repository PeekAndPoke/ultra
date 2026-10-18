# Docs: Kronos after the advanceBy collapse

**Status:** TODO
**Plan:** follow-up of `.claude/tasks-archive/2026-10/20261017-kronos-advanceby-collapse.md` (public API touched)
**Security-critical:** no

## Spec

Rewrite (don't patch) the Kronos sections of `docs-site/src/pages/ultra/datetime/kronos.astro` and the LLM
mirror `docs-site/src/data/llms/datetime.md` against the settled code in
`ultra/datetime/src/commonMain/kotlin/Kronos.kt`:

- [ ] **Pre-existing errors:** `kronos.astro:77-79` claims `mutable.advanceBy(1.hours)` moves the mutable — it
      returns a new Kronos; the idiom is `mutable.mutate { advanceBy(1.hours) }`. `kronos.astro:116-118`
      `KronosDescriptor.AdvancedBy(millis = …)` does not compile (fields: `durationMs`, `inner`).
      `kronos.astro:107` "useful for persisting": lossy for fixed clocks (a fixed clock describes as
      `SystemClock`).
- [ ] `advanceBy { }` vs `advanceBy(Duration)`: the lambda is called once, on the first read, never while
      chaining, and retried after a throw (use case: an offset loaded from a database).
- [ ] Chained advances collapse: any number of `advanceBy` / `mutate { advanceBy(..) }` steps stays one
      layer; `describe()` is one flat `AdvancedBy` per chain; old nested descriptors still load.
- [ ] `Kronos.Mutable.set` / `mutate` throw `IllegalArgumentException` for a kronos that reads the mutable
      itself (seen through mutable and advanceBy layers only); not safe against concurrent writers.
- [ ] Keep the mirror shorter than the page (CLAUDE.md "Keep docs compact").

## History

- 2026-10-18: created on archiving the advanceBy-collapse task.
