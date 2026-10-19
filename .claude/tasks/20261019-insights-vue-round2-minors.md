# Insights Vue: the round-2 MINORs of T1's gate

**Status:** DONE (archived 2026-10-19) — items 1–11 implemented on `wt/insights-minors`; review round 1 (`reviewer-high`
×2: implementation + domain fit, security) CLEAN — 0 CRITICAL / 0 MAJOR; its 7 MINORs and one pre-existing product defect
go to `.claude/tasks/20261019-insights-minors-followups.md`. Originally: 12 MINORs from round 2 of the gate of
`20260802-insights-vue-tabs.md` (gate PASS, merged `a578ef0a`); the maintainer's standing direction is to fix MINORs.
**Plan:** `.claude/tasks/20261019-insights-auth-parallel-plan.md` (insights follow-ups; touches T1's area)
**Security-critical:** partly — items 3, 6 and 7 change what reaches the depot; red-team:
`.claude/tasks/20260730-redteam-insights-api.md` (scenarios 32–35 already added on master, `:109-117`)

## Spec

Code (`funktor/codegen/src/main/resources/ts/insights/`, `monko/core`, `karango/core`, `funktor/insights`):

- [x] 1. **Graph cache by fingerprint** — `graphs.ts:325` `byFingerprint`: answers cached by the fingerprint the
      SERVER sent; a record naming an uncached one triggers one fetch, kept for that fingerprint even when the
      server answers another; a record naming none takes the latest answer. `InsightsGraphSource.kontainer`
      takes the fingerprint (`graphs.ts:291`); `GraphResult` carries `fetchedAt` (`graphs.ts:265`, stamped at
      `:304`). Wording made true: `KontainerTab.vue:179` ("Asked for it at <time>, the server answered …",
      true because a mismatch can only come from a fetch made for that fingerprint), `:185`;
      `VaultTab.vue:150` ("the server's schema as fetched at <time> … fetched once per page"). Tests:
      `graphs.spec.ts` — once per fingerprint, one refetch for another (kept, third record asks nothing),
      cached under the server's fingerprint + null-fingerprint record takes it, redeploy brings the new graph.
- [x] 2. **Pre-D1 Monko entries** — `slices.ts:410`: every `json` entry has an unknown bind-var count. Test:
      `slices.spec.ts` "…and so has a PRE-collector one".
- [x] 3. **A failed Mongo query keeps its text** — `MonkoDriver.kt:452-454`: printed and handed to the profiler
      before executing. E2e: `InsightsVaultBindValuesSpec` "a FAILED Mongo query keeps its placeholder text"
      (probe runs `$noSuchOperator`, server rejects it). Mutation-checked.
- [x] 6. **Raw AQL is not trusted** — code fix, not KDoc: `AqlTypedQuery.kt:19` `queryLanguage` is `aql` only
      when the text equals what its DSL root printed (`base_expr.kt:161` `AqlRootExpression.printed`, used by
      `query.kt:16`), else `aql-raw` (`AqlTypedQuery.kt:30`) — `of(type, query)` and a `copy(query = …)` alike.
      `KarangoDriver.kt:101` records under it; `VaultCollector.kt:98` KDoc corrected (its allowlist is unchanged,
      so `aql-raw` text is dropped); `VaultTab.vue:116` says why. API: two added public constants, an internal
      property; no shape change, no caller touched. Tests: `AqlTypedQueryLanguageSpec`,
      `VaultCollectorSlumberSpec`, e2e `InsightsVaultBindValuesSpec` "raw AQL is recorded under its own label",
      `tabs.spec.ts` "a raw AQL entry says why". Mutation-checked.
- [x] 7. **Document values are bound whole** — `MonkoDriver.kt:77` `placeholderiseQuery` (keys kept; `$and` /
      `$or` / `$nor` operands walked as queries), `:97` `placeholderiseValue` (only an operator document is
      walked, `$elemMatch` operand as a query; anything else — embedded document, any array — one `@pN`),
      `:110` `isOperatorDocument`. All 10 existing `MonkoDriverPrintQuerySpec` cases unchanged and green; 3
      added (embedded-document equality, under `$ne` and in an `$in` list, `$not` / operator-`$elemMatch`
      stay structure). E2e on Mongo: probe looks up `{token: {<token>: 1}}`. Mutation-checked.
      **Residual:** a document whose keys ALL start with `$` is walked as operators, so a `$`-prefixed secret
      used as a key stays in the text (the brief's rule; a known-operator allowlist would close it).
- [x] 8. **Node pin** — `use-node-version=22.23.3` (newest 22.x on nodejs.org/dist/index.json, 2026-09-23) in
      `funktor/codegen/vue-verify/.npmrc:3` and `ultra/codegen/ts-verify/.npmrc:3`; pnpm fetched it, both
      harnesses green on it (`pnpm exec node --version` → v22.23.3).

Tests:

- [x] 9. `escaping.spec.ts:44` asserts the EXACT count; the request case is 5 (`:56`, the raw tree shows `uri`).
      Every other case already matched exactly.
- [x] 10. `graphs.spec.ts` "dashes and lightens exactly the Lazy injections": dashed and solid edges counted
      against drawable Lazy / non-Lazy injections computed from the data.

Text:

- [x] 4. `.claude/tasks/20260731-query-vars-in-insights.md` rewritten: values not in the depot (count only),
      Monko values in `vars`, allowlist in `VaultCollector`, D1, `aql-raw`; the `vars`-key coupling stated
      there and at `slices.ts:419`.
- [x] 5. Stale notes rewritten: `20260802-insights-vue-tabs.md:549-551` (refresh.mjs makes three rewrites) and
      `:611-613` (cache per fingerprint); archived graphs task `:308-313` (pre-T1 tab check, `OLD_AFTER_SECONDS`
      gone), `:333-336` (two sources, current lines), `:486-491` (`instanceCountsMeasured`, `graphMatch`),
      `:497-503` (graph source).
- [x] 11. `InsightsPage.vue:81-84`: vue-verify compiles a generated SDK with the real toolchain on every
      `:funktor:codegen:check`. Also `InsightsPage.vue:57-58` and `InsightsDetailPage.vue:10-14` (cache wording).

Found on the way, fixed: `InsightsVaultBindValuesSpec` "the probe's log lines are recorded" flaked twice in five
runs (`[WARNING, INFO]`). `UltraLogManager` launches each event on `Dispatchers.Unconfined` (`UltraLogManager.kt:97`),
so a suspending appender lets the next event overtake; the assertion now sorts.

## Mutation checks

Backups by `cp` to a path-mirrored name outside the worktree, restored by `cp`, verified by `cmp`. Failures read
from the console; XML counts confirm the spec ran.

| # | Mutant | Test run | Result |
|---|---|---|---|
| 3 | `profile.query` set AFTER executing (`MonkoDriver.kt:453` moved below `profile.count`) | `:funktor:all:jvmTest` | RED: only "a FAILED Mongo query keeps its placeholder text" (188 tests, 1 failure) |
| 6 | `KarangoDriver.kt:101` back to `"aql"` | `:funktor:all:jvmTest` | RED: "token appears NOWHERE", "raw AQL is recorded under its own label" (2 failures) |
| 6 | `queryLanguage` = `root is AqlRootExpression` (no text comparison) | `:karango:core:test` | RED: "a printed query copied with other text is aql-raw too" (1652 tests, 1 failure) |
| 7 | `isOperatorDocument` → `isDocument` (walk any document) | `:monko:core:test` | RED: both embedded-document cases (13 tests, 2 failures) |
| 7 | value position walks arrays of documents again | `:monko:core:test` | RED: "…under an operator is ONE value too, and so is a list of them" |
| 7 | `isOperatorDocument` → `isDocument` | `:funktor:all:jvmTest` | RED: "token appears NOWHERE", "embedded document binds it whole" (+ the log flake, since fixed) |
| 9 | `RequestTab.vue` drops the query-parameter table | vitest, old + new spec | old `>=` spec GREEN (survived), new exact spec RED |
| 10 | `graphs.ts:97` predicate inverted (`!== 'Lazy'`) | vitest, old + new spec | old Lazy test GREEN (survived), new one RED |
| 1 | cache ignores the fingerprint (`known = latest`) | vitest | RED: "asks once more", "after a redeploy" |
| 1 | answers not stored by the server's fingerprint | vitest | RED: "cached under ITS fingerprint" (survived the first test set; that test was added for it) |
| 1 | `latest` never set | vitest | RED: "cached under ITS fingerprint" |
| 2 | `query === null` clause restored | vitest | RED: "…and so has a PRE-collector one" |

Vue mutants were applied to the harness copy (`vue-verify/src/funktorsdk/insights/`, regenerated from resources on
every `vueVerify`), not to the resources.

## Test counts (fresh XML, 2026-10-19)

One run, `console/wt-gradle.sh :monko:core:test :karango:core:test :funktor:insights:jvmTest :funktor:all:jvmTest
:funktor:codegen:check :ultra:codegen:check`, every test task executed (none UP-TO-DATE), all green:

| Module | Specs | Tests |
|---|---|---|
| `monko:core` | 26 | 262 (`MonkoDriverPrintQuerySpec` 13) |
| `karango:core` | 144 | 1652 (`AqlTypedQueryLanguageSpec` 3) |
| `funktor:insights` | 13 | 94 (`VaultCollectorSlumberSpec` 4) |
| `funktor:all` | 18 | 188 (`InsightsVaultBindValuesSpec` 8) |
| `funktor:codegen` (JVM + vue-verify) | 9 | 103 (vitest 71 of them) |
| `ultra:codegen` (JVM + ts-verify) | 21 | 325 (ts-verify 11 fixtures) |

No compile sweep: the only public API change is two added constants on `AqlTypedQuery`; no full suite (coordinator).

## For the maintainer (surplus, not defects)

- `--alias` (`TsSdkGenerateCliCommand.kt:53`) has no user until `@sdk` lands — keep or drop until then?
- The barrel exports `overrideDefinition`, `isInlinedMongoEntry`, `readDatabaseGraph` — make them module-private?
- v1 records' "Kontainers (young / old / total)" row can only say "not measured" — drop it with its helper?

## Review record — the LEDGER (filled by /feature-review)

Round 1 (2026-10-19), blind, base `0062fa47`, head `55bb2549`. Two charters (implementation + domain fit; security) —
scoped by risk: a MINOR fix batch, the domain questions (Karango API fit, cache semantics) folded into the first.
Coordinator checks: `UltraLogManager.kt:31,97` (one coroutine per event on `Dispatchers.Unconfined`, appenders walked
in order) — as claimed; the `$`-prefix rule — the implementer's own recorded residual.

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | MINOR | `MonkoDriver.kt:110-111` + `:452-454` (security) | A value-position document whose keys all start with `$` is walked as operators, so a `$`-prefixed secret key (a bcrypt/argon2 hash as a map key) stays in the text; printing before execution now records it for server-rejected queries too | follow-up | Closed set of Mongo query operators; anything else bound whole |
| 1 | MINOR | `VaultCollector.kt:123`, `slices.ts:428` (security) | Depot records written before this change may hold raw AQL labelled `aql`; nothing at read time can tell | follow-up | Not fixable at read time — a release note (purge or accept) |
| 1 | MINOR | `slices.ts:681-685` (implementation) | `graphMatch` KDoc still says "today's" | follow-up | Reword |
| 1 | MINOR | `KontainerTab.vue:12` (implementation) | KDoc "fetched for this record" — the fetch is per fingerprint | follow-up | Reword |
| 1 | MINOR | `vue-verify/tests/graphs.spec.ts` (implementation) | No test pins `fetchedAt` across a cache hit; re-stamping on a hit survives | follow-up | Fake timers: fetch at T1, advance, assert T1 |
| 1 | MINOR | `KontainerTab.vue:86`, `VaultTab.vue:90` (implementation) | `toLocaleTimeString()` drops the date; an overnight tab reads as today | follow-up | `toLocaleString()` |
| 1 | MINOR (surplus) | `AqlTypedQuery.kt` `LANGUAGE`/`LANGUAGE_RAW` (implementation) | New public constants with no reader outside karango; Monko's equivalent is internal | follow-up | `internal` |
| 1 | MINOR | `InsightsVaultBindValuesSpec.kt:373-375`, `UltraLogManager.kt:97`, `LogCollector.kt:47-50` (implementation) | The sort fixes the test, but the product defect stays: a suspending appender (Karango) delays later appenders, so the Log tab shows arrival order, and a line still in flight at `finish` is LOST (the e2e can still flake as size 1) | out-of-scope → follow-up | Pre-existing, `ultra/log` + `funktor/insights`; same follow-up task |

Red-team (security, collected): 36 raw AQL that fails — `KarangoQueryException` embeds `query.query`
(`KarangoDriver.kt:130-137`); 37 secrets as field names (both printers keep names by design); 33a — addendum to 33.

**Verdict:** PASS — round 1 clean (0 CRITICAL / 0 MAJOR); module tests green; mutation table above.
