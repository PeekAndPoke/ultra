# ULTRA-00171: Query bind VALUES in insights: record them, their shape, or nothing?

> **Status (2026-11-10): DONE.** Decided 2026-11-09 (maintainer decision **D4** of ULTRA-00221): values shown, credential repositories withhold them. Gate PASS in two rounds; see the ledger.
>
> **Part of:** ULTRA-00253 (Insights epic)
>
> **DECIDED 2026-11-09 (maintainer): "query values: show them again".** Record the bind values and render them in both GUIs, as the old GUI did. Safety around it: insights is superuser-only and refused in production (ULTRA-00225). Bind names are generated, so masking by name is not possible. What a credential repository (auth records: session ids until ULTRA-00136 hashes them, password hashes on writes) puts into `vars` gets checked when implementing; if it does, that repository opts out wholesale (the "per-repository default" row below), and the tab says so.

**Security-critical:** yes — data exposure, not injection
**Found:** 2026-07-31, while removing Jackson from the query printers

## Implementation (2026-11-10)

- **Opt-out per repository, structural.** `KarangoDriver`/`MonkoDriver.withholdingQueryValues()` is a copy of the
  driver on `ValuesWithheldQueryProfiler` (`ultra/vault`). A repository built on it covers every query it runs:
  each entry is marked `valuesWithheld` before the driver reports into it, `Entry.Impl` drops the values (keeps the
  names) and the plan on the way in, and `explainQueries` is false, so no EXPLAIN runs.
- **Opted out:** `KarangoAuthRecordsRepo`, `MonkoAuthRecordsRepo` (password hashes on writes, token and session
  hashes on lookups), `KarangoSentMessagesRepo`, `MonkoSentMessagesRepo` (mails with activation and reset links).
  **Not opted out, checked:** the log repositories already run on `NullQueryProfiler` (nothing recorded); background
  jobs carry app payloads, no framework job carries a token (messaging and auth do not use jobs); cluster, saas and
  the demo's user repositories hold no secret (the demo seeds passwords through the auth records repo).
- **`VaultCollector.Entry`:** `values` (`{name: JSON text}`, each cut at 4000 chars), `valuesWithheld`, `explain`.
  Ordinary entries keep any text (`aql-raw` too); withheld ones only `aql`/`mongo-bound` text. New keys, so the Vue
  legacy detection (`vars` / `queryExplained`) still only matches pre-collector records, whose text, values and
  plan stay hidden.
- **Vue `VaultTab`:** values as one pretty JSON block under the query, the plan in a collapsed "Explained" (as the
  old GUI), a "Values withheld" notice for withheld entries. Prism highlighting comes with ULTRA-00190.

**Evidence:** vault + insights unit, funktor-all 272/0, karango 1652/0, monko 265/0, vueVerify 80/0. Mutants W1–W12,
W7e, N1 (server) and X1–X7 (Vue) all red for the right reason. Fixtures refreshed from a real run (`refresh.mjs`).

## Review record (ledger)

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 impl | MAJOR | `VaultCollector.kt` `toJson` | A NaN/±Infinity bind value (Double, Float, Decimal128) throws in `Json.encode` inside `finish`, losing the whole record | fix | Confirmed RED first (`JsonEncodingException`). Non-finite numbers become JSON text; mutant N1 red |
| 1 impl | MINOR | `slices.ts` `valuesText` | `JSON.parse` loses Longs above 2^53 | fix | A value is pretty-printed only when re-printing gives the server's text back; otherwise shown verbatim (also `1.0`). Mutant X7 red |
| 1 impl | MINOR | `VaultCollector.kt` `Entry.values` KDoc | `[MAX_VALUE_CHARS]` does not resolve from `Entry` | fix | `[Data.MAX_VALUE_CHARS]` |
| 1 impl | MINOR | `reference/TAB-SPECS.md`, `VaultCollectorKeySpec.kt:10` | Stale: say values are not recorded / the tab withholds them | fix | Both rewritten to the new wire shape |
| 1 sec | MINOR | `funktor/rest/.../apiKeyCaller.kt:27` | The keystore guidance does not mention `withholdingQueryValues()` | fix | One KDoc line; the DOCS follow-up covers app repos (keys, invitations, magic links) |

| 2 impl | MINOR | `MonkoDriver.kt` `version` | `withholdingQueryValues()` copies the driver, and each copy asked `buildInfo` again | fix | A constructor parameter cannot be injected by Kontainer (tried: `KontainerInconsistent`); the version is now cached per `MongoClient` (a singleton), which also drops the one-per-request call that predates this change |
| 2 impl | MINOR | `QueryProfilerSpec.kt:235` | "Wrapping twice is wrapping once" did not test that | fix | `shouldBeSameInstanceAs` |
| 2 impl | MINOR | e2e, withheld entries | Nothing tells "no EXPLAIN ran" from "EXPLAIN ran, plan dropped" | fix | `measureExplain.count == 0` on withheld entries; mutant W7 now also red in the e2e |

Round 2 phase 2: every round-1 fix holds; no rejected CRITICAL/MAJOR. **Verdict: gate PASS** (round 2 clean of
CRITICAL/MAJOR; its MINORs applied in one batch without re-review).

Round 1 CLEAN (security, verified by the reviewer): values, text and plan of the four repositories by every path;
paths around the repo's `driver` (Monko's raw collection calls are never profiled; `withLog` keeps the wrapper);
other credential holders in the framework (none); Vue escaping (no `v-html`), legacy records hidden.
Not adopted: a try/catch net around `valueText` (first applied, then removed: no known value reaches it, so it
would be untested code).

**Red team:** scenario 42 in ULTRA-00161. **Docs follow-up:** none yet. `withholdingQueryValues()` belongs on the
Karango and Monko pages and the insights page; the insights page is stale as a whole (the deleted `InsightsGui`) and
gets rewritten once the Vue and Kraft GUIs and ULTRA-00301 (one switch) settle, which would surprise nobody if they
changed next week. That rewrite covers this.

## Where it stood (2026-10-19)

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
- 2026-11-10: IN PROGRESS.
- 2026-11-10: DONE.
