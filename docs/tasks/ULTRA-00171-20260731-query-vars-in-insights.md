# ULTRA-00171: Query bind VALUES in insights: record them, their shape, or nothing?

> **Status (2026-10-19): NOT STARTED.** OPEN — maintainer decision **D4** of ULTRA-00221 (insights auth parallel plan), not blocking. Rewritten
> 2026-10-19 (round-2 MINORs): the earlier text predated D1 and T1 and was wrong on four facts.
>
> **Part of:** ULTRA-00253 (Insights epic)

**Security-critical:** yes — data exposure, not injection
**Found:** 2026-07-31, while removing Jackson from the query printers

## Where it stands (2026-10-19)

- **No bind value reaches a record or the depot.** Both drivers still hand values to the profiler
  (`KarangoDriver.kt:109`, `MonkoDriver.kt:454`), but `VaultCollector.Data.of` keeps only their COUNT
  (`VaultCollector.kt:124`) and never the EXPLAIN plan, which the database fills with the literals. Since D1
  (`e448c64d`, 2026-08-24) the slice is an explicit DTO; before it, a profiled request's record could not be
  written at all.
- **Monko's values are in `vars` now**, not its text: `FindQueryBuilder.printQuery` writes typed `@pN:<type>`
  placeholders under the label `mongo-bound` (T1). Monko's old label `json` meant values inline.
- **The allowlist lives in `VaultCollector`** (`PLACEHOLDER_SAFE_LANGUAGES`, `VaultCollector.kt:108`), not in the
  Vue tab: only `aql` and `mongo-bound` keep their text. Raw AQL from `AqlTypedQuery.of` is `aql-raw`
  (`AqlTypedQuery.kt:30`) and withheld.
- **Old records still hold values on disk**: those written while Jackson serialised the profiler entry whole.
  The Vue tab marks them legacy and hides their text; it renders no `vars` and has no raw-slice dump, for
  any record. The same constraint stands for the Kraft port (ULTRA-00198 (insights kraft gui)).
- Not injection: values travel separately to the database. `AqlPrinter.raw` (`printer.kt:70`), which inlines
  them, has no production caller.

## The decision (D4)

Recording nothing costs debugging value: a slow query without what it was slow *for*. Redacting by parameter
name fails (names are generated), and `Redacted<T>` has no declaration site. Options, none decided:

| Shape | Note |
|---|---|
| Per-repository default | The repository knows "this one holds credentials"; auth/session repos opt out wholesale |
| Per-query opt-out | Precise, but per-call-site controls get forgotten (cf. `.noInsights()`) |
| Both, repo as the floor | Mirrors `ApiRoutes.authFloor`: a query can only tighten it |
| Record only the SHAPE | Type and arity, no content. Half exists: Monko's placeholders carry the type, Karango's do not |

## Coupling to know before recording anything

The Vue tab detects a pre-collector entry by the presence of a `vars` KEY (`slices.ts:421`): such an entry
is legacy and its text is hidden. Recording values, or their shape, under `vars` would mark every new entry
legacy and hide ALL query text. Use another key, or change the detection in the same change.

## Related

- ULTRA-00160 (insights rest api) — the API that made records remotely readable
- ULTRA-00167 (config secrets in insights) — the config half of the same problem
- ULTRA-00161 (redteam insights api) — attack scenarios against the recorded slices

## History

- 2026-10-21: numbered ULTRA-00171 (was `docs/tasks/20260731-query-vars-in-insights.md`).
