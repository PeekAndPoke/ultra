# v1.0.0 Roadmap — Single Source of Truth

**Current version:** 0.105.0 → **Target:** 1.0.0
**Last updated:** 2026-04-13

This is the consolidated v1 plan. Replaces the previous v1-parallel-execution, ultra-v1-roadmap,
and funktor-v1-roadmap plans. All open work for v1 lives here.

---

## Gate Status

| Gate                           | Status              | What's left                                                                  |
|--------------------------------|---------------------|------------------------------------------------------------------------------|
| G1 — Zero CRITICAL/HIGH issues | 🟡 Wave 1 DONE      | 3 HIGH items still open in Funktor (see "Open Funktor Issues" below)         |
| G2 — Public API tested (≥25%)  | 🟡 IN PROGRESS      | vault/html/semanticui ✅ hardened (2026-04-07); Funktor modules still thin    |
| G3 — TODOs resolved or tracked | 🟡 IN PROGRESS      | ~15 in Funktor, 5 reflection, 0 vault                                        |
| G4 — KDoc 90%+ public API      | 🟡 IN PROGRESS      | cache ✅, Funktor ✅ (~85% after 2026-04-13 pass); inspect/staticweb remaining |
| G5 — Clean build, no warnings  | ✅ ASSUMED PASSING   | Verify at release gate                                                       |
| G6 — README per library        | ✅ DONE              |                                                                              |
| G7 — ES2015 everywhere         | ✅ DONE (2026-04-06) | All 48 JS modules target ES2015                                              |
| G8 — ApiRoute refactor         | ✅ DONE (2026-04-03) | PRs #40+#41                                                                  |
| G9 — Code audit complete       | 🟡 Wave 1 DONE      | Waves 2-5 (data/network/UI/Monko)                                            |

---

## What's Already Shipped

**Monko Phase 2+3 (DONE 2026-04-06):** save/remove with hooks, cursor fullCount/timeMs, index
management, 23 test files (234 tests). All 10 target repos ported (7 planned + 3 bonus).

**Ultra hardening (DONE 2026-04-07):** vault (~105 tests), html (~26 tests), semanticui (~29 tests)
all ≥35% ratio.

**ApiRoute refactor (DONE 2026-04-03):** OutgoingConverter removed, ValidateRoutesOnAppStarting
added, 16 implementations updated (PRs #40+#41).

**ES2015 rollout (DONE 2026-04-06):** All 48 JS modules.

**ApiAcl + ApiAccessMatrix (DONE 2026-04-06):** User-specific API access on frontend (PR #43).

**Non-blocking DBAL (DONE 2026-04-09):** Flow Cursor, suspend Storable.resolve()/invoke(),
Ref/LazyRef unified (LazyRef deleted), RefCodec zero-runBlocking, EntityCache async.

**Funktor KDoc pass (DONE 2026-04-13):** 8 submodules from ~30-55% to ~80-95% coverage (cluster,
messaging, core, rest, logging, testing, insights, all). inspect and staticweb deferred as internal
tooling.

**Storable API consistency (DONE 2026-04-13):** `modifyAsync` and `transformAsync` lifted to
`Storable` sealed class; implemented on all three subclasses (Stored/Ref/New) with correct
eager/lazy semantics. 17 new tests in StorableSpec cover cast + modify + transform variants.

**Vault API hardening (DONE 2026-04-13):** `Database` API migrated from `Class<T>` to `KClass<T>`
(breaking: `hasRepositoryStoring`, `getRepositoryStoring`, `getRepositoryStoringOrNull`,
`getRepository(cls)`). Fixed latent `SharedRepoClassLookup` NPE on unknown types (sentinel pattern
caches negative lookups). Closed last vault TODO (`DatabaseGraphBuilder`). Added ~23 tests
(StoredSlumberer, StoredAwaker, DatabaseGraphBuilder, Database negative paths,
SharedRepoClassLookup null-provider). Docs-site updated.

**Released:** 0.105.0 with Kraft lifecycle hooks.

---

## Open Work — Organized by Track

### Track A — Code Audit & Hardening (G9 + G2)

1. **Wave 2 audit** — Slumber (re-check), ultra/model, ultra/cache, ultra/datetime
    - Focus: polymorphic type resolution, codec thread safety, untrusted input deserialization,
      cache eviction correctness, TTL boundary conditions, timezone/DST edge cases
2. **Wave 3 audit** — ultra/remote, ultra/log, ultra/html, Streams (re-check)
    - Focus: HTTP client cleanup, timeout handling, log injection, XSS, subscription lifecycle leaks
3. **Wave 4 audit** — Kraft core, Kraft semanticui, 11 addons, Mutator, all KSP processors
    - Focus: component lifecycle, state batching, JS interop safety, mutation tracking correctness,
      generated code edge cases (generics, nullability, sealed hierarchies)
4. **Wave 5 audit** — Monko core + ksp (after current test suite validated)
    - Focus: driver safety, query DSL correctness, index management, connection handling

**Output per wave:** Issues document in `.claude/plans/` with severity ratings. All CRITICAL/HIGH
block v1.

### Track B — Funktor Test Blitz (G2)

Raise Funktor from ~11% to 25%+ test ratio. Focus on the modules with the thinnest coverage.

| Module    | Current | Needed | Focus                                                 |
|-----------|---------|--------|-------------------------------------------------------|
| core      | 5 tests | +20    | App lifecycle, CLI, TypedRoute, converters, fixtures  |
| rest      | 6 tests | +10    | Route security, SSE, codec                            |
| auth      | 9 tests | +15    | AuthSystem, password flows, SSO, both DB backends     |
| cluster   | 7 tests | +30    | Background jobs, locks, workers, depot, both backends |
| messaging | 2 tests | +12    | Email senders, sent storage, both backends            |
| logging   | 0 tests | +6     | Log storage, appender, filtering, both backends       |
| insights  | 0 tests | +8     | Collectors, metrics                                   |
| inspect   | 0 tests | +8     | API clients (commonMain)                              |
| staticweb | 1 test  | +5     | Templates, flash session                              |
| testing   | 0 tests | +2     | AppSpec, test harness                                 |

**Specific test gaps** to address (from funktor-issues.md):

- LogsStorage `update()` and `execBulkAction()` — zero tests
- Pagination — no multi-page tests for logs, jobs, or storage
- Concurrent `claimNextDue` — no race test
- `removeAllButLastSuccessful` — no test for keep-N-delete-rest logic
- TTL cleanup — no test for automatic document expiry
- Index creation — no test verifying indexes exist after `ensureIndexes()`
- Attachments — SentMessages tests always pass `emptyList()`
- Auth `findByToken` unknown token — no test

### Track C — Documentation & KDoc (G4)

1. ✅ **ultra/cache KDoc blitz** — DONE (~90%)
2. ✅ **Funktor KDoc main pass** — DONE 2026-04-13 (8 submodules)
3. **Funktor inspect + staticweb KDoc** — internal tooling, low priority, currently ~40%
4. **Funktor TODO cleanup** — ~15 TODOs, mostly "make configurable":
    - Account activation flow (`AuthSystem.activate()`)
    - Token length + expiration configurable
    - Frontend URL building from app config
    - Depot encryption
    - Fulltext index for log search
    - Dart codegen test TODOs (6)
5. **Funktor docs improvement** — 5 thin pages need real examples:
    - `docs-site/src/pages/ultra/funktor/logging.astro` (STUB → GOOD): Logback appender config,
      level filtering, web UI, REST API query example
    - `docs-site/src/pages/ultra/funktor/auth.astro` (THIN → GOOD): provider config (Google/GitHub),
      JWT generation, kontainer wiring, password policy
    - `docs-site/src/pages/ultra/funktor/messaging.astro` (THIN → GOOD): AWS SES config, dev override,
      email hooks, SentMessages queries
    - `docs-site/src/pages/ultra/funktor/insights.astro` (THIN → GOOD): kontainer config, mounting
      InsightsGui, per-request data flow
    - `docs-site/src/pages/ultra/funktor/staticweb.astro` (THIN → GOOD): web resource config, virtual
      host routing, full template composition
    - Update `docs-site/public/llms.txt` and `llms-full.txt` to match
6. **FunktorConf showcase** — design done (see "FunktorConf Showcase" section below)

### Track D — Release Engineering

1. **Clean build verification** — `./gradlew clean build` across all 79 modules
2. **funktor-demo end-to-end test** — against both ArangoDB and MongoDB
3. **Version bump + release** — 0.105.0 → 1.0.0
4. **CHANGELOG.md** — write release notes for 320+ commits
5. **Root README update** — point at 1.0.0, update badges
6. **Maven Central publish** — artifacts for all 5 library roots
7. **Git tag v1.0.0**

### Track E — Funktor-demo Polish

1. **DB backend decision** — 🟡 DECISION NEEDED. Currently mixes Monko + Karango. Options:
    - (a) all-Karango — proven, ship-ready
    - (b) all-Monko — demonstrates portability story
    - (c) two demo configs — complex but most honest
2. **Admin UI polish pass** — systematic page-by-page human review. Fix layout, spacing,
   consistency, visual quality.
3. **Funktor TODO: AdminUI** — use the funktor-demo admin to validate every feature end-to-end

---

## Open Funktor Issues (from funktor-issues.md)

### HIGH severity (must resolve for G1)

1. ~~**WorkerTracker.lockWorker** — job reference set after completion, cancellation broken.~~
   **FIXED 2026-04-13** — swapped `coroutineScope { async }` for `CoroutineScope(context).async(LAZY)`;
   reference set under `sync`, then `invokeOnCompletion`, then `job.start()`. Regression test in
   `WorkerTrackerSpec`.

2. ~~**WorkerTracker.lastRuns** — not synchronized, data races on `Dispatchers.IO`.~~
   **FIXED 2026-04-13** — both accessors now use the existing `sync { }` helper. Regression test
   in `WorkerTrackerSpec` (1000 concurrent `put` calls).

3. **`runBlocking` in Ktor lifecycle event handlers** — nested blocking risk, potential deadlock.
   `funktor/core/src/jvmMain/kotlin/lifecycle/AppLifeCycleBuilder.kt:37-122`.
   Fix: ensure inner `runBlocking` uses `Dispatchers.IO`.

4. **`queueIfNotPresent` TOCTOU race across JVMs** — duplicate jobs possible in cluster.
   `funktor/cluster/.../backgroundjobs/BackgroundJobs.kt:186-193`.
   Fix: unique compound index on `(type, dataHash, state)` + catch duplicate key exception.

5. **Global lock expiration not checked during acquisition** — crashed servers block cluster
   until cleanup worker runs.
   `funktor/cluster/.../locks/VaultGlobalLocksProvider.kt`.
   Fix: check `expires` field during acquire; treat expired locks as free.

### MEDIUM severity (fix or document deferral)

- `runBlocking` in MonkoDriver.version lazy property (blocks on first access)
- MonkoRepository enum comparison uses `.name` string (fragile)
- WorkersRunner.state — non-volatile shared variable
- TimingInterceptor.children — unsynchronized mutable list
- WorkerHistory.Vault — lastFlush/lastCleanup not synchronized
- WorkerHistory.Vault.putRun — `coroutineScope { launch }` blocks caller (intent unclear)
- ServerBeacon `update()` TOCTOU race on first creation
- Static mutable state in `WorkerHistory.Adapter.Vault` (test isolation leak)
- Attack detection delay creates thread-holding DoS vector
- Lock cleanup can release locks held by alive servers (empty `aliveServerIds` edge case)
- `archiveJob` deletes before archiving — data loss on archive failure
- `dataHash` 32-bit collision risk in `queueIfNotPresent`

### LOW severity (fix or `// TODO(post-v1)`)

- `batchInsert` sequential in MonkoWorkerHistoryRepo
- MonkoSentMessagesRepo mixes raw Filters with DSL
- LogsStorageBaseSpec casts to KarangoLogEntry
- PrimitiveGlobalLocksProvider.Storage static singleton never cleaned up
- LogbackKarangoLogAppender fire-and-forget without error handling
- `page=0` vs `page=1` inconsistency in LogsFilter
- WorkerHistory.Adapter.InMemory not thread-safe
- BackgroundJobRetryPolicy only has `LinearDelay` (no exponential backoff)

---

## FunktorConf Showcase (Track E sub-plan)

A CRUD showcase domain chosen from a six-hats analysis. Maps naturally to Monko repos and gives
funktor-demo a realistic domain that docs can reference 1:1.

**Wave 1 — Domain Models + CRUD (admin-app):** Event, Speaker, Attendee entities. Each gets list +
detail/edit pages with forms + validation. ~6 screens max. Shared models in
`common/src/commonMain/kotlin/funktorconf/`; Monko repos + API routes in
`server/src/main/kotlin/funktorconf/`; admin pages in `adminapp/src/jsMain/kotlin/pages/funktorconf/`.

**Wave 2 — Cross-cutting:** role-based auth (organizer vs. viewer), notifications on confirmation,
logging audit trail.

**Wave 3 — Cluster features:** badge PDF generation (background jobs), reminder emails (workers),
speaker slide uploads (storage/depot), schedule edit locks, cached public schedule.

**Wave 4 — Public user-app:** read-only website consuming the same API. Event landing page,
speaker profiles, live Q&A (SSE), attendee self-registration. Demonstrates common-module sharing.

---

## Shipping Checklist

- [ ] All CRITICAL/HIGH audit findings resolved (Wave 1 ✅, Waves 2-5 pending, 5 Funktor HIGH items)
- [ ] Every shippable module has test coverage ≥25% or documented exception (G2)
- [ ] Zero unresolved TODOs without `// TODO(post-v1)` justification (G3)
- [ ] Every public API has KDoc (G4)
- [ ] `./gradlew clean build` green, no warnings (G5)
- [ ] Every library has a README (G6 ✅)
- [ ] ES2015 target set everywhere (G7 ✅)
- [ ] ApiRoute refactor shipped (G8 ✅)
- [ ] Wave 2-5 audits complete (G9)
- [ ] funktor-demo uses a single DB backend consistently
- [ ] funktor-demo admin UI polish pass complete
- [ ] funktor-demo runs end-to-end on both ArangoDB and MongoDB
- [ ] Maven Central artifacts published for all 5 library roots
- [ ] CHANGELOG.md written
- [ ] Root README points at 1.0.0

---

## Post-v1 Backlog

Explicitly deferred:

1. **Exposed (SQL) backend** — third DB backend for Funktor
2. **CI/CD pipeline** — GitHub Actions for build/test/publish, green badges
3. **Account activation flow** — `AuthSystem.activate()` full email verification
4. **Dart codegen rewrite** — needs proper rewrite; move to `funktor:dart-codegen` package first
5. **Email template editor** — rich template system for messaging
6. **API binary compatibility** — `kotlinx-binary-compatibility-validator`
7. **Line-level code coverage** — metrics and floor beyond API-surface testing
8. **Detekt rule tightening** — stricter static analysis
9. **Device tracking for auth** — device fingerprinting
10. **NPM dependency updates** — version bumps for JS deps
11. **Karakum retry** — alpha-quality, deprioritized. Skill at `.claude/skills/karakum-setup/`
12. **Kraft Tailwind integration** — pluggable styling backend (Six-hats analysis archived)
13. **Kraft React shim PoC** — time-boxed experiment for React component interop
14. **Kraft advanced Tailwind** — build-time DSL generation from `tailwind.config.js`
15. **Mutator Phase 2+3** — deepen Kraft integration, evaluate expansion (server-side dirty-check,
    JSON patch/diff, time-travel plugins)
16. **Docs site live embeds** — `<LiveExample>` Astro component with iframe + postMessage scrubbing
17. **Docs site visual polish** — CSS effects (golden glow on demo frames, glass morphism)
18. **Docs site dogfooding** — evaluate rebuilding docs (or a section) in Kraft itself

---

## Risk Register

| Risk                                               | Likelihood | Impact | Mitigation                                                |
|----------------------------------------------------|------------|--------|-----------------------------------------------------------|
| Audit Waves 2-5 find MANY new issues               | High       | Medium | Triage into fix-now vs v1.x buckets; don't scope-creep    |
| Funktor test blitz underestimated                  | Medium     | High   | Parallelize by module; start with thinnest coverage       |
| Open Funktor HIGH issues cascade into larger fixes | Medium     | Medium | Tackle one at a time; verify each with targeted tests     |
| funktor-demo DB backend decision delayed           | Medium     | Low    | Decision is user-facing; document both options and decide |
| Admin UI polish unbounded                          | High       | Low    | Timebox; polish the main workflows, defer edge screens    |

---

## Active Pilot Status

- **Karakum** (TypeScript→Kotlin externals) — alpha-quality, deprioritized
- **PixiJS addon + Breakout demo** — shipped; template for future heavy-lib addons
- **Kraft Tailwind Phase 1** — deferred post-v1
