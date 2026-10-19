# Insights Vue: the round-2 MINORs of T1's gate

**Status:** TODO — 12 MINORs from round 2 of the `/feature-review` gate of `20260802-insights-vue-tabs.md` (gate PASS,
merged `a578ef0a`); the maintainer's standing direction is to fix MINORs. Ledger: that task's "Round 2" table.
**Plan:** `.claude/tasks/20261019-insights-auth-parallel-plan.md` (insights follow-ups; touches T1's area)
**Security-critical:** partly — items 6 and 7 change what reaches the depot; red-team:
`.claude/tasks/20260730-redteam-insights-api.md`

## Spec

Code (`funktor/codegen/src/main/resources/ts/insights/`, `monko/core`, `karango/core`, `funktor/insights`):

- [ ] 1. **Graph cache by fingerprint** (`graphs.ts:298-307`, `InsightsPage.vue:59`): refetch once for a record naming
      another fingerprint; keep that answer if the server still differs. Fixes the false "today's" and "fetched now"
      (`KontainerTab.vue:170-174`, `VaultTab.vue:139-141`). T2's Part A decision.
- [ ] 2. **Pre-D1 Monko entries** (`slices.ts:408-410`): drop the `query === null` clause — every `json` entry has an
      unknown bind-var count.
- [ ] 3. **A failed Mongo query keeps its text** (`MonkoDriver.kt:420-446`): print before executing. Mutation-checked.
- [ ] 6. **Raw AQL is not trusted** (`KarangoDriver.kt:101`, `AqlTypedQuery.kt:15`, `VaultCollector.kt:103-107`): a
      query built by `AqlTypedQuery.of(type, query)` gets its own label (dropped), or the KDoc is corrected and a
      red-team scenario added. Mutation-checked.
- [ ] 7. **Document values are bound whole** (`MonkoDriver.kt:73-79`): at a value position walk only operator
      documents; bind any other as `@pN:object`. Mutation-checked, e2e on Mongo.
- [ ] 8. **Node pin**: bump `use-node-version` in `funktor/codegen/vue-verify/.npmrc` and
      `ultra/codegen/ts-verify/.npmrc` within 22.x — look the newest up online.

Tests:

- [ ] 9. `vue-verify/tests/escaping.spec.ts:41,52`: exact counts (the raw tree repeats `uri`), or assert per position.
- [ ] 10. `vue-verify/tests/graphs.spec.ts:216-221`: count Lazy injections independently; kill the inverted-predicate
      mutant of `graphs.ts:97`.

Text:

- [ ] 4. Rewrite `.claude/tasks/20260731-query-vars-in-insights.md` (four facts stale: Monko values are in `vars`
      now, the allowlist lives in `VaultCollector`, values are no longer written to the depot, D1). Note the coupling
      at `slices.ts:418` (any `vars` key = legacy), which would hide all text if D4 records vars.
- [ ] 5. Stale notes: `.claude/tasks/20260802-insights-vue-tabs.md:548` ("one change" in `refresh.mjs` — it makes
      three), archived graphs task `:307-316`, `:336-338`, `:488`, `:492` (deleted `OLD_AFTER_SECONDS`, four `finish`
      sources, `instanceCountsMeasured`, `graphMismatch`).
- [ ] 11. `InsightsPage.vue:82-83`: vue-verify now compiles a generated SDK with a real toolchain.

## For the maintainer (surplus, not defects)

- `--alias` (`TsSdkGenerateCliCommand.kt:53`) has no user until `@sdk` lands — keep or drop until then?
- The barrel exports `overrideDefinition`, `isInlinedMongoEntry`, `readDatabaseGraph` — make them module-private?
- v1 records' "Kontainers (young / old / total)" row can only say "not measured" — drop it with its helper?

## Red-team scenarios to add to `20260730-redteam-insights-api.md`

- Raw AQL recorded under `aql` (item 6); a document value keyed by a secret (item 7).
- A failed Mongo query whose server error echoes literal values into a logged message (`LogCollector.kt:32-35`).
- `enrichApiResponseWithInsights` (`funktor/rest/src/jvmMain/kotlin/respond.kt:71-84`) adds hostname + full URL to API
  responses, a 401 to a non-superuser included (unverified).

## Review record — the LEDGER (filled by /feature-review)

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|

**Verdict:**
