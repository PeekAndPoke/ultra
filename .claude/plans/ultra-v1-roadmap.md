# Ultra v1.0.0 Roadmap

**Date:** 2026-03-30
**Scope:** All modules in the Ultra mono-repo
**Current version:** 0.104.2
**Target:** 1.0.0 (mono-version — all modules ship together)

---

## Key Decisions

| Decision       | Choice                   | Rationale                                                           |
|----------------|--------------------------|---------------------------------------------------------------------|
| Versioning     | Mono-version             | All modules share one version number. Simpler for users.            |
| DB portability | Karango + Monko required | Funktor v1 must support ArangoDB and MongoDB. Exposed/SQL deferred. |
| Test bar       | All public API tested    | Every public function/class must have at least one test.            |
| CI/CD          | Nice-to-have             | Can follow shortly after v1. Tests must pass locally.               |

---

## Hard v1.0.0 Gate Criteria

Every gate must pass before tagging 1.0.0. No exceptions.

### G1. Zero CRITICAL / HIGH issues

~~The current backlog has 1 CRITICAL and 8 HIGH issues.~~ **Updated 2026-03-31:** Wave 1 audit resolved all 5 CRITICAL
and 14 HIGH issues across Karango, Kontainer, Vault, and Security. Funktor issues (1C + 8H) were addressed in a prior
session. Remaining HIGH issues: funktor items only (verify status).

### G2. Public-API test coverage

Every public class and function (anything without `internal`/`private` in a `*Main` source set) must have at least one
test. Quick sanity check: every module must have >= 25% test-file-to-source-file ratio.

### G3. No unresolved TODOs in shipping code

TODOs representing incomplete functionality must be resolved. Enhancement notes for post-v1 work must be tagged
`// TODO(post-v1): ...`.

### G4. KDoc on every public declaration

Every public class, interface, object, function, and property in `*Main` source sets must have KDoc. Minimum:
one-sentence summary. Target: 90%+ coverage per module.

### G5. Clean compilation

`./gradlew build` produces zero warnings in non-test source sets.

### G6. README per module family

Each family (Ultra, Kraft, Funktor, Karango, Monko, Mutator) must have a README covering purpose, getting started, and
links to docs.

### G7. ES2015 for all Kotlin/JS

All ~40 Kotlin/JS modules must compile with ES2015 target. Implementation: single-file change in root
`build.gradle.kts` (plan at `.claude/plans/es2015-migration.md`). Verification: generated JS uses `class X extends Y`
syntax, all tests pass, browser smoke test passes.

### G8. ApiRoute refactor — remove OutgoingConverter parameter

All `ApiRoutes` implementations must NOT receive `converter: OutgoingConverter` as a constructor parameter. Route
validation (checking that the OutgoingConverter can handle all parameter types) must happen via an `OnAppStarting`
lifecycle hook instead.

**Current state:** 17 `ApiRoutes` implementations across funktor and funktor-demo all receive `converter` through
constructor chaining: `ApiRoutes -> Routes -> RouteBuilder -> TypedRoute.validateParams()`.

**Required change:**

1. Remove `converter` from `Routes`, `ApiRoutes`, `RouteBuilder`, all `TypedRoute` constructors
2. Create `RouteValidationOnAppStartingHook : OnAppStarting` that collects all routes from all `ApiFeature` instances,
   validates each against the `OutgoingConverter`, and throws on failure
3. Update all 17 `ApiRoutes` implementations and their `ApiFeature` wrappers

**Key files:**

- `funktor/rest/src/jvmMain/kotlin/ApiRoutes.kt`
- `funktor/core/src/jvmMain/kotlin/broker/Routes.kt`
- `funktor/core/src/jvmMain/kotlin/broker/TypedRoute.kt`
- `funktor/core/src/jvmMain/kotlin/lifecycle/AppLifeCycleHooks.kt`
- 17 ApiRoutes implementations across funktor/* and funktor-demo/*

### G9. Comprehensive code audit for every module

Every module must pass a structured code review covering **five dimensions** before v1. The funktor-issues review (which
uncovered 1 CRITICAL, 8 HIGH, 10 MEDIUM, 8 LOW issues) is the model — that same rigor must be applied to every module
family.

**Audit dimensions:**

1. **Logic bugs** — incorrect algorithms, off-by-one errors, wrong return values, edge cases that produce wrong results,
   missing null checks on external input, broken control flow
2. **Implementation bugs** — race conditions, resource leaks, memory leaks, incorrect coroutine usage (blocking in
   suspend context, missing cancellation, shared mutable state without synchronization), broken error handling,
   swallowed exceptions
3. **Security** — injection vulnerabilities, authentication/authorization bypasses, token handling (generation, storage,
   invalidation, expiration), data exposure through error messages or logs, unsafe defaults, missing input validation at
   system boundaries
4. **Code style** — consistent naming conventions, explicit imports (no wildcards), no fully-qualified class names,
   proper use of Kotlin idioms, consistent error handling patterns, no dead code, no commented-out code blocks
5. **API design** — consistent naming across modules, sensible defaults, minimal surface area (don't expose internals),
   proper use of sealed/value classes, consistent parameter ordering, builder DSL consistency, extension function
   placement

**Audit output per module:**

- Issues list with severity (CRITICAL / HIGH / MEDIUM / LOW)
- Every CRITICAL and HIGH must be fixed before v1
- MEDIUM issues: fix or document rationale for deferral
- LOW issues: fix or tag `// TODO(post-v1): ...`

**Audit scope:**

| Family  | Modules to Audit                                                                                                                              | Audit Status                                                                                                  |
|---------|-----------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------|
| Ultra   | common, cache, datetime, fixture, html, kontainer, log, maths, meta, model, reflection, remote, security, semanticui, slumber, streams, vault | **kontainer, security, vault: Wave 1 DONE.** Slumber, Streams, ultra/common had partial audits. Rest: pending |
| Kraft   | core, semanticui, testing, 11 addons                                                                                                          | KDoc audit done, no deep code review                                                                          |
| Funktor | core, rest, auth, cluster, inspect, insights, logging, messaging, staticweb, testing                                                          | **Done** — funktor-issues.md (deep review March 2026)                                                         |
| Karango | core, ksp                                                                                                                                     | **Wave 1 DONE** — full deep audit with 65 issues found and resolved                                           |
| Monko   | core, ksp                                                                                                                                     | **Not done** — needs full audit after Phase 1 completion                                                      |
| Mutator | core, ksp                                                                                                                                     | Quality pass done, but no formal security/logic review                                                        |

**Excluded from audit (deferred to post-v1 rewrite):**

- Dart codegen in `funktor:rest` (~28 files in `dart/` package) — needs a proper rewrite, not worth auditing current
  state

**Modules already audited (verify findings are addressed):**

- Funktor: `funktor-issues.md` — verify all CRITICAL/HIGH fixes, review MEDIUM/LOW status

**Modules requiring full audit (no prior deep review):**

- All Kraft modules (core + semanticui + testing + 11 addons)
- ultra: cache, datetime, fixture, html, log, maths, meta, model, reflection, remote, security, semanticui, vault
- Karango: core, ksp (deeper than prior partial audit)
- Monko: core, ksp (after Phase 1 completion)
- Mutator: core, ksp (deeper than prior quality pass)

### Module-Specific Gates

**G-DB.** Database-dependent modules (auth, cluster, logging, messaging):

- Integration tests must run against real ArangoDB and MongoDB instances
- Monko `save()`, `remove()`, and all repository hooks must be complete
- `funktor:testing` must provide test harness for both DB backends

**G-SEC.** Security-sensitive modules (auth, security, cluster/locks):

- `setPassword` must require authentication
- Sign-up race condition must be resolved (unique index on email)
- Lock expiration and TOCTOU races must be addressed

**G-KSP.** Code-generation modules (karango:ksp, monko:ksp, mutator:ksp):

- Compile-testing for each KSP processor
- At least 3 representative input-output pairs per processor

**G-UI.** JS/Browser modules (Kraft, funktor:inspect):

- Manual smoke test plan documented
- All public component APIs must have at least one test or demo page

---

## Phased Roadmap

### Phase 0: Foundation Fixes

**Goal:** Resolve all CRITICAL and HIGH issues. Apply ES2015 migration.

| #  | Task                                                                  | Severity | Key File(s)                                                           |
|----|-----------------------------------------------------------------------|----------|-----------------------------------------------------------------------|
| 1  | ~~Fix VaultScope `runBlocking`~~ **DONE (Wave 1)**                    | CRITICAL | `ultra/vault/src/jvmMain/kotlin/vault_module.kt:62-73`                |
| 2  | Fix WorkerTracker cancellation — set job reference before awaiting    | HIGH     | `funktor/cluster/.../workers/services/WorkerTracker.kt:98-108`        |
| 3  | Fix `setPassword` — require current password or caller identity check | HIGH     | `funktor/auth/.../provider/EmailAndPasswordAuth.kt:253-278`           |
| 4  | Fix sign-up race — add unique index on email                          | HIGH     | `funktor/auth/.../provider/EmailAndPasswordAuth.kt:222-248`           |
| 5  | Fix lock expiration — check `expires` field during acquire            | HIGH     | `funktor/cluster/.../locks/VaultGlobalLocksProvider.kt`               |
| 6  | Fix archive data loss — archive before delete                         | HIGH     | `funktor/cluster/.../backgroundjobs/BackgroundJobs.kt:572-582`        |
| 7  | Fix `queueIfNotPresent` TOCTOU — unique compound index                | HIGH     | `funktor/cluster/.../backgroundjobs/BackgroundJobs.kt:186-193`        |
| 8  | Fix lock cleanup — skip if `aliveServerIds` is empty                  | HIGH     | `funktor/cluster/.../locks/workers/GlobalLocksCleanupWorker.kt:28-48` |
| 9  | Fix attack detection DoS — remove delay, return 404 immediately       | HIGH     | `funktor/rest/.../ApiStatusPages.kt:98-105`                           |
| 10 | Apply ES2015 migration (single-file change)                           | GATE G7  | `build.gradle.kts` (root)                                             |

**Parallelism:** Items 1, 2, 3-4, 5-9, 10 can be worked in parallel (vault, workers, auth, cluster, build).

### Phase 1: Comprehensive Code Audit

**Goal:** Deep review of every module across all 5 dimensions (G9). Findings feed into all subsequent phases.

This phase runs early so that discovered bugs are fixed alongside the test blitz and hardening work. Funktor is already
audited (`funktor-issues.md`). The remaining modules are grouped by priority.

**Wave 1 — Security-critical and foundational — ✅ COMPLETE (2026-03-31):**

| Module         | Issues Found | Fixed  | By Design/Won't Fix | Deferred | Key Fixes                                                           |
|----------------|--------------|--------|---------------------|----------|---------------------------------------------------------------------|
| ultra:vault    | 15           | 11     | 1                   | 3        | Cache thread safety, profiler sync, VaultException, safe casts      |
| ultra:security | 16           | 14     | 1                   | 1        | CSRF timing attack, token delimiters, secret redaction, TTL→Long    |
| Karango core   | 17           | 9      | 5                   | 3        | AQL injection fix, ensureIndexes, query options, KSP imports        |
| Kontainer      | 17           | 5      | 9                   | 0        | AtomicBoolean, ConcurrentHashMap, circular dep detection, @Volatile |
| **Total**      | **65**       | **39** | **16**              | **7**    |                                                                     |

Archived to `.claude/plans-archive/2026-03-31-wave1-audit-*.md`.

**Wave 2 — Data and serialization:**

| Module         | Audit Focus                                                                                                                                                                                 |
|----------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Slumber        | Polymorphic type resolution edge cases, codec caching thread safety, untrusted input deserialization (can a crafted payload cause DoS or type confusion?), error messages leaking internals |
| ultra:common   | Collection mutation safety, hashing collision handling, WeakReference/WeakSet correctness across platforms, encoding edge cases                                                             |
| ultra:model    | Data class correctness, equals/hashCode consistency                                                                                                                                         |
| ultra:cache    | Cache eviction correctness, TTL boundary conditions, concurrent access                                                                                                                      |
| ultra:datetime | Timezone edge cases, DST transitions, platform divergence (JVM vs JS), overflow on extreme dates                                                                                            |

**Wave 3 — Network and I/O:**

| Module       | Audit Focus                                                                                                                                         |
|--------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| ultra:remote | HTTP client resource cleanup, timeout handling, redirect following, header injection, error response handling                                       |
| ultra:log    | Log injection (can user input appear unsanitized in structured logs?), file handle leaks, concurrent appender safety                                |
| ultra:html   | XSS through unescaped content, attribute injection, proper HTML entity encoding                                                                     |
| Streams      | Subscription lifecycle (leaks on unsubscribe?), backpressure handling, error propagation in composed streams, platform-specific behavior divergence |

**Wave 4 — UI and code generation:**

| Module                                | Audit Focus                                                                                                                                          |
|---------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------|
| Kraft core                            | Component lifecycle correctness, state update batching, event handler cleanup, memory leaks from detached components, virtual DOM diffing edge cases |
| Kraft semanticui                      | Prop forwarding correctness, CSS class composition                                                                                                   |
| Kraft addons (11)                     | JS interop safety (`js("...")` calls), external library version compatibility, resource cleanup                                                      |
| Mutator core                          | Mutation tracking correctness (isModified/reset/commit), collection mutator contract compliance (MutableList/Set/Map), snapshot integrity            |
| Mutator ksp + Karango ksp + Monko ksp | Generated code correctness, edge cases (generics, nullability, nested types, sealed hierarchies), annotation processing ordering                     |

**Wave 5 — Monko + remaining:**

| Module           | Audit Focus                                                                                                                       |
|------------------|-----------------------------------------------------------------------------------------------------------------------------------|
| Monko core       | Full audit after Phase 2 completion — driver safety, query DSL correctness, index management, connection handling, hook lifecycle |
| Monko ksp        | Generated code correctness after completion                                                                                       |
| ultra:reflection | @Experimental usage, platform compatibility                                                                                       |
| ultra:maths      | Floating-point precision, overflow, division by zero                                                                              |
| ultra:fixture    | Randomness quality, deterministic seeding for tests                                                                               |
| ultra:meta       | KSP processor correctness, annotation resolution                                                                                  |
| ultra:semanticui | CSS class correctness, component rendering                                                                                        |

**Audit output:** Each wave produces an issues document (following `funktor-issues.md` format) filed at
`.claude/plans/{family}-audit-issues.md`. All CRITICAL/HIGH findings become blockers for subsequent phases.

**Parallelism:** Waves 1-4 can overlap. Wave 5 (Monko) must wait for Phase 2 Monko completion.

### Phase 2: ApiRoute Refactor + Monko Completion

**Goal:** Remove OutgoingConverter from ApiRoutes (G8). Bring Monko to feature-complete. Runs in parallel with Phase 1
audit.

**ApiRoute refactor:**

1. Remove `converter` param from `Routes`, `ApiRoutes`, `RouteBuilder`, all `TypedRoute` constructors
2. Create `RouteValidationOnAppStartingHook` implementing `OnAppStarting`
3. Hook collects all routes from kontainer-provided `ApiFeature` instances
4. Hook validates each route's params against `OutgoingConverter`
5. Update all 17 `ApiRoutes` implementations
6. Update all `ApiFeature` wrappers to stop passing converter
7. Add tests for the validation hook (valid routes pass, invalid routes throw)

**Monko completion:**

1. Implement `MonkoRepository.save()` and `remove()`
2. Complete repository hooks (onBeforeSave, onAfterSave in insert-many/update-many, onAfterDelete)
3. Complete query DSL — verify filters, sorts, updates are comprehensive
4. Add aggregation support (count, distinct at minimum)
5. Implement batch insert (`insertMany`)
6. Complete cursor features (fullCount, timeMs)
7. Write 15+ test files covering every public API
8. Add compile-test edge cases for monko:ksp

**Blocks:** Phase 3 (Funktor tests) depends on Monko being complete for dual-backend testing.

### Phase 3: Funktor Dual-Backend Porting + Test Blitz

**Goal:** Port all Karango-only repos to Monko. Raise Funktor from ~6% to 25%+ test ratio.

**Porting work (7 repositories):**

| Module    | Repository                | Karango | Monko         |
|-----------|---------------------------|---------|---------------|
| auth      | AuthRecordsRepo           | Done    | Done (verify) |
| cluster   | BackgroundJobsQueueRepo   | Done    | **Port**      |
| cluster   | BackgroundJobsArchiveRepo | Done    | **Port**      |
| cluster   | Lock/Beacon repos         | Done    | **Port**      |
| cluster   | RandomData/Cache repos    | Done    | **Port**      |
| logging   | LogRepository + Storage   | Done    | **Port**      |
| messaging | SentMessagesRepo          | Done    | **Port**      |

**Test blitz targets (estimated new test files needed):**

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

**Total: ~115+ new test files**

**Parallelism:** Porting and testing can proceed by module. core + rest (no DB) can start immediately. DB-dependent
modules start after Phase 2.

### Phase 4: Ultra Module Hardening

**Goal:** Bring all Yellow/Red ultra modules to Green. Runs in parallel with Phase 3. Incorporates findings from Phase 1
audit.

| Module     | Current Ratio | Key Work                                                                       |
|------------|---------------|--------------------------------------------------------------------------------|
| vault      | 18% (8 tests) | Fix 6 TODOs, add 10+ tests for VaultScope, Hooks, Database, Repository, Cursor |
| remote     | 21% (7 tests) | Add tests for URI builders, HTTP client wrappers                               |
| cache      | 35% (6 tests) | KDoc from 24% to 90%, add edge-case tests                                      |
| html       | 15% (2 tests) | Add 4+ test files for HTML builders, KDoc                                      |
| semanticui | 27% (4 tests) | Add 4+ test files, KDoc                                                        |
| reflection | 43% (6 tests) | Resolve 5 TODOs                                                                |
| maths      | 33% (4 tests) | Add edge-case precision tests                                                  |
| log        | 29% (4 tests) | Add tests for all log implementations                                          |
| fixture    | 33% (3 tests) | Add tests for all fixture helpers                                              |

### Phase 5: KDoc + TODO Cleanup + Docs

**Goal:** Meet G4 (KDoc) and G3 (TODO cleanup). Runs in parallel with Phases 3-4.

1. **KDoc sweep** (largest gaps first):
    - ultra:cache (24% KDoc)
    - Funktor submodules (~50% average across ~600 source files)
    - Monko (new code from Phase 1)
    - ultra:common remaining gaps (WeakRef/WeakSet, numbers, enums)

2. **TODO cleanup:**
    - Implement or defer every TODO in the codebase
    - All deferred items tagged `// TODO(post-v1): ...`
    - Key items requiring resolution: `AuthSystem.activate()`, various "make configurable" TODOs

3. **Docs-site: all major features documented** (matching current GREEN tier quality: 8-16 pages per library):

   | Library | Current Pages | Target | Gap |
      |---------|--------------|--------|-----|
   | Slumber | 8 pages | OK | - |
   | Streams | 8 pages | OK | - |
   | Karango | 8 pages | OK | - |
   | Kontainer | 7 pages | OK | - |
   | Kraft | 16 pages | OK | - |
   | Mutator | 6 pages | OK (verify completeness) | Minor |
   | Funktor | ~11 sparse pages | 12-16 pages with depth | Major |
   | Monko | ? | 6-8 pages | Major |
   | Ultra (common, vault, etc.) | 0 | Overview + key modules | Medium |

    - Every library's core features get their own page with examples
    - Minor utilities and addons get brief coverage
    - Update `llms.txt` and `llms-full.txt` to reflect v1 content

### Phase 6: Integration + Release

**Goal:** Validate everything works together. Publish v1.0.0.

1. `./gradlew clean build` — zero failures across all 79 modules, all platforms
2. Run funktor-demo end-to-end against both ArangoDB and MongoDB:
    - Auth flows (sign-up, sign-in, password reset)
    - Background jobs (queue, execute, archive, retry)
    - Global locks (acquire, release, timeout, expiry)
    - Workers (start, run, stop, cancellation)
    - Messaging (send, store)
    - Insights/Inspect UI loads
3. Verify generated JS uses ES2015 class syntax
4. Browser smoke test for Kraft examples
5. Version bump: `VERSION_NAME=1.0.0` in `gradle.properties`
6. Create `CHANGELOG.md`
7. Update root `README.MD` with v1.0.0 branding
8. Publish to Maven Central
9. Git tag `v1.0.0`

---

## Module Status Matrix

### Current vs Required

| Module            | Tests Now  | Tests Needed | KDoc Now | KDoc Needed | Issues            | v1 Ready? |
|-------------------|------------|--------------|----------|-------------|-------------------|-----------|
| **Slumber**       | 60 (49%)   | OK           | ~98%     | OK          | 0                 | YES       |
| **Streams**       | 21 (41%)   | OK           | 100%     | OK          | 0                 | YES       |
| **Karango**       | 145 (79%)  | OK           | 67%      | +23%        | 0 (Wave 1 done)   | ALMOST    |
| **Kontainer**     | 24 (35%)   | OK           | ~98%     | OK          | 0 (Wave 1 done)   | YES       |
| **ultra/common**  | 28 (65%)   | OK           | ~67%     | +23%        | 0                 | ALMOST    |
| **ultra/model**   | 9 (45%)    | OK           | ?        | Audit       | 0                 | ALMOST    |
| **Kraft**         | 319 (166%) | OK           | ~80%     | +10%        | 0                 | ALMOST    |
| **Mutator**       | 28 (65%)   | OK           | 30%      | +60%        | 0                 | KDoc gap  |
| ultra/security    | 14 (42%)   | OK           | ~90%     | OK          | 0 (Wave 1 done)   | YES       |
| ultra/cache       | 6 (35%)    | +3           | 24%      | +66%        | 0                 | KDoc gap  |
| ultra/datetime    | 28 (44%)   | OK           | ?        | Audit       | 1                 | ALMOST    |
| ultra/vault       | 8 (18%)    | +10          | ?        | Audit       | 3 TODOs (Wave 1)  | NO        |
| ultra/remote      | 7 (21%)    | +5           | ?        | Audit       | 0                 | TEST gap  |
| ultra/reflection  | 6 (43%)    | OK           | 63%      | +27%        | 5 TODOs           | TODO gap  |
| ultra/html        | 2 (15%)    | +4           | ?        | Audit       | 0                 | NO        |
| ultra/semanticui  | 4 (27%)    | +2           | ?        | Audit       | 0                 | NO        |
| ultra/log         | 4 (29%)    | +3           | ?        | Audit       | 0                 | TEST gap  |
| ultra/maths       | 4 (33%)    | +2           | ?        | Audit       | 0                 | TEST gap  |
| **Funktor** (all) | 34 (~6%)   | +115         | ~50%     | +40%        | 1C+8H (pre-Wave1) | NO        |
| **Monko**         | 3 (16%)    | +15          | ?        | Full        | save/remove TODO  | NO        |

---

## Post-v1.0.0 Backlog

Explicitly deferred to after 1.0.0:

1. **Exposed (SQL) backend** — third DB backend for Funktor
2. **CI/CD pipeline** — GitHub Actions for build/test/publish
3. **Account activation flow** — `AuthSystem.activate()` full email verification
4. **Dart codegen rewrite** — needs a proper rewrite post-v1; excluded from audit/test/KDoc scope. Before v1: move Dart
   codegen out of `funktor:rest` into its own separate package (e.g. `funktor:dart-codegen`) to isolate it from the
   stable API surface
5. **Email template editor** — rich template system for messaging
6. **API binary compatibility** — `kotlinx-binary-compatibility-validator`
7. **Line-level code coverage** — metrics and floor beyond API-surface testing
8. **Detekt rule tightening** — stricter static analysis
9. **Device tracking for auth** — device fingerprinting
10. **Vault Cursor truly async** — make Cursor suspend-based
11. **NPM dependency updates** — version bumps for JS deps
