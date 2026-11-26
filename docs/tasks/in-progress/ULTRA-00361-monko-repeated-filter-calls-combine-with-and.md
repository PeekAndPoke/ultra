# ULTRA-00361: Monko: repeated filter() calls combine with AND

> **Status (2026-11-26): IN PROGRESS.** Implemented and mutation-checked; review gate next.

**Security-critical:** no (query building: mutation-checked per the testing rule)

## Why this exists

The docs truth audit (ULTRA-00356) found that a second `filter()` call in `MonkoRepository.find { }` replaced the
first. The docs were made to say so. Maintainer, 2026-11-26: "I think they should combine". Karango already behaves
this way: several AQL `FILTER` statements AND together.

## Decide before implementing

Settled with the maintainer on 2026-11-26 ("Plan ok, please proceed"):

- Repeated `filter()` calls combine with AND. One call is sent as given; several become one flat `$and`, in call order.
- `sort`, `limit` and `skip` keep last-wins.
- Behaviour change: listed under Breaking in the CHANGELOG.

## The work

- `monko/core/src/main/kotlin/MonkoDriver.kt:199-217`: `FindQueryBuilder` collects the filters in a list; the
  internal `filter` property derives null / the single filter / `Filters.and(list)`. `applyTo`, `print` and
  `printQuery` read that property unchanged.
- Docs: `docs-site/src/pages/ultra/monko/query-dsl.astro:27-29` and `docs-site/src/data/llms/monko.md:267-268`.
- Callers checked: no find block outside the new spec calls `filter()` more than once (scan of all 31 files using
  `MonkoRepository` or `findStored`), so no production query changes meaning.

## Acceptance

- [x] Two or more `filter()` calls return only documents matching all of them (real MongoDB).
- [x] One call keeps its filter as given, with no `$and` around it (query text of existing callers unchanged).
- [x] Several calls give one flat `$and` in call order.
- [x] `printQuery` binds every value of a combined filter.
- [x] Docs page and LLM mirror say so; docs site builds.

## Test evidence

- New tests:
  - `MonkoDriverFindQueryBuilderSpec`: single call, two calls, three calls (flat, ordered).
  - `MonkoDriverPrintQuerySpec`: two calls give one `$and`, values bound, token absent from the text.
  - `MonkoRepositoryFilterCombineSpec` (new, real MongoDB): two calls in both orders; one call alone.
- RED before the fix: `:monko:core:test` gave 275 tests, 5 failed, all 5 new multi-call tests, each because only the
  last filter applied (e.g. `["red-small", "blue-small"]` instead of `["red-small"]`). The single-call tests passed.
- Green after: 275 tests, 0 failures, 0 errors, 28 XML files, none stale (counted from the test-results XML).
- Mutants (each restored from a cp backup, `cmp` clean afterwards):
  - M1, last filter wins (the old behaviour): 5 failed.
  - M2, `Filters.or` instead of `and`: 5 failed.
  - M3, nested `and` by reduce instead of flat: 1 failed (the three-call test).
  - M4, a single filter wrapped in `$and`: 6 failed.
- Docs: `pnpm astro build`, 118 pages, complete.

## Review record (LEDGER)

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|

## History

- 2026-11-26: created.
- 2026-11-26: implemented on master (sequential, main checkout); RED, green and four mutants recorded above.
