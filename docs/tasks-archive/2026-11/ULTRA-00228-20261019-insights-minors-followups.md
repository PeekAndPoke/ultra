# ULTRA-00228: Insights: follow-ups from the insights-minors review, and the log-order defect

> **Status (2026-11-09): DONE.** Items 1–6 built 2026-11-09 (maintainer's order: after ULTRA-00225). Gate PASS in round 1 (two charters, both CLEAN); the MINOR/NIT batch applied and mutation-checked. One question for the maintainer below.
>
> **Part of:** ULTRA-00253 (Insights epic)

**Plan:** ULTRA-00221 (insights auth parallel plan) (insights follow-ups)
**Security-critical:** item 1 yes (what reaches the depot); red-team ULTRA-00161 (redteam insights api)

## Spec

- [x] 1. **Closed operator set** (`monko/core/src/main/kotlin/MonkoDriver.kt:110-111`): a value-position document is
      walked only if every key is a known Mongo query operator; anything else is bound whole. Kills the `$`-prefixed
      secret key (bcrypt/argon2 hash as a map key), which printing-before-execution now also records for rejected
      queries. Mutation-checked; e2e on Mongo.
- [x] 2. **Release note** (next CHANGELOG entry): depot records written before 2026-10-19 may hold raw AQL labelled `aql`
      (inlined values included); purge the depot or accept it.
- [x] 3. Wording: `slices.ts:681-685` (`graphMatch` — not "today's"), `KontainerTab.vue:12` ("fetched for a record of
      this fingerprint").
- [x] 4. `KontainerTab.vue:86`, `VaultTab.vue:90`: `toLocaleString()` — the date matters on a long-open page.
- [x] 5. `vue-verify/tests/graphs.spec.ts`: pin `fetchedAt` across a cache hit with fake timers (the re-stamp mutant
      must die).
- [x] 6. `karango/core/src/main/kotlin/vault/AqlTypedQuery.kt`: `LANGUAGE` / `LANGUAGE_RAW` → `internal`.
- [x] 7. DONE 2026-10-19 (coordinator, on master — it turned master's full suite red: the e2e lost `WARNING`): `UltraLogManager.log` now launches one coroutine PER appender, so a non-suspending appender has the event before `log` returns; pinned by `UltraLogManagerDispatchSpec` "an appender after a suspending one has the event before log returns" (mutant: one coroutine walking all appenders → red at the new assertion, restored + `cmp`). Was: **Log lines out of order or LOST** (pre-existing product defect): `ultra/log/src/jvmMain/kotlin/UltraLogManager.kt:97`
      launches one coroutine per event on `Dispatchers.Unconfined` and walks appenders in order; a suspending appender
      (`KarangoLogAppender.append` → `repo.tryInsert`) delays every later one, so the insights `LogCollector` receives
      lines in arrival order, and a line still in flight when `LogCollector.finish` runs (`getAndClear`) never reaches
      the record. `LogCollector.Data.Entry` has no timestamp to sort by. Options: run the per-request insights appender
      first/synchronously, launch each appender separately, or record `event.ts` and sort in `finish`. Test: the
      `InsightsVaultBindValuesSpec` log-lines case without its sort, run repeatedly.

## Implementation notes (items 1–6, 2026-11-09)

1. `monko/core/src/main/kotlin/MonkoDriver.kt`: `VALUE_OPERATORS`, the closed set of query operators that may stand at a
   value position; `isOperatorDocument` asks it instead of `startsWith("$")`. Consequence, accepted by the item's own
   words ("anything else is bound whole"): a query with an unknown operator (a typo) records that operand as one
   `@p1:object`, so the record no longer shows the misspelt operator. The old e2e case asserting `$noSuchOperator` in
   the text was restated (`InsightsVaultBindValuesSpec`, "a FAILED Mongo query is recorded too").
2. CHANGELOG (`## Unreleased`, the ULTRA-00225 entry): older depot records are not rewritten; those before 2026-10-19
   may hold raw AQL labelled `aql`; clear the depot on upgrade. Plus an entry for item 1 and item 6.
3. `slices.ts` (`graphMatch` KDoc: "the server's at the time the graph was fetched"), `KontainerTab.vue:12` ("fetched
   for a record of this fingerprint").
4. `KontainerTab.vue:86`, `VaultTab.vue:90`: `toLocaleString()`.
5. `vue-verify/tests/graphs.spec.ts`, "a cached graph keeps the time it was fetched": fake `Date` only; a second tab
   three days later, served from the cache, says the first fetch time. Kontainer (a record naming no fingerprint, the
   case whose text shows the time) and database.
6. `AqlTypedQuery.LANGUAGE` / `LANGUAGE_RAW` are `internal` (only karango's own spec used them).

## Test evidence

- `MonkoDriverPrintQuerySpec`: "a document keyed by a hash that starts with $ is ONE value" (bcrypt key, a known
  operator next to it, an argon2 key).
- `InsightsVaultBindValuesSpec` (e2e, real Mongo): the probe sends a hash-keyed lookup the server rejects; the record
  carries no trace of the hash, and three lookups are recorded as one bound document each.
- Runs: `:monko:core:test` 263/0, `:funktor:all:jvmTest` 269/0, `:karango:core:test` 1652/0, `:funktor:codegen:vueVerify` 73/0.
- Mutants: I1 (any `$` key counts as an operator again) red in the printer spec and both e2e cases.
- Vue mutants (content restored and `cmp`-verified each time), each red in its own case of "a cached graph keeps the
  time it was fetched": V1 the kontainer tab stamps the click time; V2 the vault tab does; V3 the database cache
  re-stamps on every hit (`once`); V4 the kontainer cache re-stamps on every hit (`byFingerprint`).

## Review record — the LEDGER

Round 1, level 1 (`reviewer-high`), two charters by risk: implementation & code style, security. Both **CLEAN**
(0 CRITICAL, 0 MAJOR). The batch applied without re-review, per the loop; mutation-checked.

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | MINOR (both) | `MonkoDriver.kt:90-101,116` | The closed set covers value positions only: a `$`-key at a QUERY position (top-level filter, a `$and` element, an `$elemMatch` operand) is still printed, also for a rejected query | fix | Verified by reading. `placeholderiseQuery` binds a document whole when a `$` key is none of `QUERY_OPERATORS` (logical + `$expr`, `$where`, `$text`, `$comment`, `$jsonSchema`); an `$elemMatch` operand of operators goes through `placeholderiseValue`. Printer case for all three positions; mutants J1, J2 red |
| 1 | MINOR (sec) | `error_logging.kt:72`, `LogCollector.kt:35` | Mongo's error for the rejected query (`unknown operator: $2b$10$…`) reaches the log slice when the exception escapes the handler; the e2e catches it, so it proves the vault slice only | maintainer-decision | Verified: the e2e wraps the lookup in `runCatching`. Out of this item's scope (the vault slice). Asked 2026-11-09 whether driver exception messages in the log slice get a ticket |
| 1 | NIT (impl) | `MonkoDriver.kt:78-84` | `$text` lost its structure (`$search` not in the set) | fix | `$search`, `$language`, `$caseSensitive`, `$diacriticSensitive` added; `$expr`'s aggregation operators stay closed on purpose. Printer case; mutant J3 red |
| 1 | NIT (impl) | `MonkoDriver.kt:83` | `$comment` is top-level only, so it does not belong to the value set | fix | Moved to `QUERY_OPERATORS` |
| 1 | NIT (impl) | `InsightsVaultBindValuesSpec.kt:153` | The probe does not prove the server rejects the hash lookup | fix | `check(lookupByHashAsKey(hash).isFailure)` |
| 1 | NIT (impl) | `InsightsVaultBindValuesSpec.kt:338,379,149` | A duplicated count assertion; a stale probe comment | fix | The hash case keeps only its own assertion; the comment says the three lookups are told apart by counting |
| 1 | NIT (impl) | `CHANGELOG.md:17-18` | No blank line; the Karango change inside the Monko entry; old records may also hold a hash key | fix | Separate entries; the depot note names both |

Probed and CLEAN (both reviewers): every value goes through a bind; the 33 operator names checked against MongoDB's
reference; every operator Monko's DSL emits is in a set; `all`, not `any`; empty documents bound; operators whose
operand is a list or geometry are bound whole; `typeLabel` reads the type only; the record keeps `varsCount`, not the
values, and never the explain output; the AQL side needs no change (a map is one bind var, object keys are bound);
item 6 breaks no caller; items 3–4 as specified; the Vue fake timers fake `Date` only, are restored in `afterEach`, and
pin the time for the right reason (one request proves the cache hit); no `v-html`.

### Mutation table, review batch

| # | Mutant | Red in |
|---|---|---|
| J1 | no query-position check | `MonkoDriverPrintQuerySpec` query-position case |
| J2 | the query-position check lets every `$` key pass | the same case |
| J3 | `$text`'s options not in the set | `MonkoDriverPrintQuerySpec` query-level operators case |

Runs after the batch: `:monko:core:test` 265/0, `:funktor:all:jvmTest` 269/0. (`:karango:core:test` 1652/0 and
`:funktor:codegen:vueVerify` 73/0 before it; the batch touched neither.)

**Verdict:** gate PASS (round 1).

**Red team:** ULTRA-00161 scenarios 33 and 37 cover this ground; scenario 41 added for the query-position case.

**Docs follow-up:** none; no public API beyond two constants made `internal`.

## Open for the maintainer

- A database driver's exception message can carry query input (here a hash used as an operator) into the log slice
  and the app's log when the exception escapes. A ticket for that?

## History

- 2026-10-21: numbered ULTRA-00228 (was `docs/tasks/20261019-insights-minors-followups.md`).
- 2026-11-09: IN PROGRESS.
- 2026-11-09: DONE.
