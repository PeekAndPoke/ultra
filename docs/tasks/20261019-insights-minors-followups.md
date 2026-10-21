# Insights: follow-ups from the insights-minors review, and the log-order defect

**Status:** TODO — from review round 1 of ULTRA-00226 (insights vue round2 minors) (clean,
merged `348c66bc`); the maintainer's standing direction is to fix MINORs
**Plan:** ULTRA-00221 (insights auth parallel plan) (insights follow-ups)
**Security-critical:** item 1 yes (what reaches the depot); red-team ULTRA-00161 (redteam insights api)

## Spec

- [ ] 1. **Closed operator set** (`monko/core/src/main/kotlin/MonkoDriver.kt:110-111`): a value-position document is
      walked only if every key is a known Mongo query operator; anything else is bound whole. Kills the `$`-prefixed
      secret key (bcrypt/argon2 hash as a map key), which printing-before-execution now also records for rejected
      queries. Mutation-checked; e2e on Mongo.
- [ ] 2. **Release note** (next CHANGELOG entry): depot records written before 2026-10-19 may hold raw AQL labelled `aql`
      (inlined values included); purge the depot or accept it.
- [ ] 3. Wording: `slices.ts:681-685` (`graphMatch` — not "today's"), `KontainerTab.vue:12` ("fetched for a record of
      this fingerprint").
- [ ] 4. `KontainerTab.vue:86`, `VaultTab.vue:90`: `toLocaleString()` — the date matters on a long-open page.
- [ ] 5. `vue-verify/tests/graphs.spec.ts`: pin `fetchedAt` across a cache hit with fake timers (the re-stamp mutant
      must die).
- [ ] 6. `karango/core/src/main/kotlin/vault/AqlTypedQuery.kt`: `LANGUAGE` / `LANGUAGE_RAW` → `internal`.
- [x] 7. DONE 2026-10-19 (coordinator, on master — it turned master's full suite red: the e2e lost `WARNING`): `UltraLogManager.log` now launches one coroutine PER appender, so a non-suspending appender has the event before `log` returns; pinned by `UltraLogManagerDispatchSpec` "an appender after a suspending one has the event before log returns" (mutant: one coroutine walking all appenders → red at the new assertion, restored + `cmp`). Was: **Log lines out of order or LOST** (pre-existing product defect): `ultra/log/src/jvmMain/kotlin/UltraLogManager.kt:97`
      launches one coroutine per event on `Dispatchers.Unconfined` and walks appenders in order; a suspending appender
      (`KarangoLogAppender.append` → `repo.tryInsert`) delays every later one, so the insights `LogCollector` receives
      lines in arrival order, and a line still in flight when `LogCollector.finish` runs (`getAndClear`) never reaches
      the record. `LogCollector.Data.Entry` has no timestamp to sort by. Options: run the per-request insights appender
      first/synchronously, launch each appender separately, or record `event.ts` and sort in `finish`. Test: the
      `InsightsVaultBindValuesSpec` log-lines case without its sort, run repeatedly.
