# v1.0.0 Parallel Execution Plan

**Date:** 2026-04-05
**Current version:** 0.104.2 → **Target:** 1.0.0
**Approach:** Three independent tracks running in parallel, with one hard sequential bottleneck.

---

## Gate Status

| Gate                           | Status              | What's left                                                               |
|--------------------------------|---------------------|---------------------------------------------------------------------------|
| G1 — Zero CRITICAL/HIGH issues | ✅ DONE              | Wave 1 resolved all                                                       |
| G2 — Public API tested (≥25%)  | 🟡 IN PROGRESS      | vault/html/semanticui ✅ hardened (2026-04-07); Funktor remaining          |
| G3 — TODOs resolved or tracked | 🟡 IN PROGRESS      | ~15 in Funktor, 5 reflection, 3 vault                                     |
| G4 — KDoc 90%+ public API      | 🟡 IN PROGRESS      | cache ✅ ~90%, Funktor (~50%) remaining                                    |
| G5 — Clean build, no warnings  | ✅ ASSUMED PASSING   | Verify at release gate                                                    |
| G6 — README per library        | ✅ DONE              |                                                                           |
| G7 — ES2015 everywhere         | ✅ DONE (2026-04-06) | All 48 JS modules target ES2015; jsJar + jsTest green                     |
| G8 — ApiRoute refactor         | ✅ DONE (2026-04-03) | PRs #40+#41: OutgoingConverter removed, ValidateRoutesOnAppStarting added |
| G9 — Code audit complete       | 🟡 Wave 1 DONE      | Waves 2-5 (data/network/UI/Monko)                                         |

---

## Critical Path — UPDATED 2026-04-05

**Monko Phase 2 and Phase 3 porting are effectively DONE.** The critical path dissolved once we looked
at the actual code — Track B is ahead of where the plan assumed.

### What's actually shipped

**Monko Phase 2 (core API):** DONE

- `save()` / `update()` with hooks ✓
- `remove()` with hooks ✓
- `Repository.Hooks<T>` infrastructure (onBeforeSave, onAfterSave, onAfterDelete) ✓
- `MonkoCursor.fullCount` and `MonkoCursor.timeMs` ✓
- Index management basics (ensure/validate/recreate, TTL indexes) ✓

**Monko Phase 2 (open items):**

- `insertMany()` optimization in MonkoDriver — workaround (loop insert) exists, not blocking
- Standalone Monko CRUD test suite — only 1 driver test today, target 15+

**Portability Phase 3 (7 repos):** DONE — and then some
| Repo | Monko impl |
|------|-----------|
| BackgroundJobsQueueRepo | ✓ |
| BackgroundJobsArchiveRepo | ✓ |
| GlobalLocksRepo | ✓ |
| ServerBeaconRepo | ✓ |
| LogRepository + LogsStorage | ✓ |
| SentMessagesRepo | ✓ |
| (3 bonus: RandomData, RandomCache, WorkerHistory) | ✓ |

Each ported repo has at least one Monko-backed integration spec.

### What Track B still needs for v1

1. **Monko core test expansion** — 1 → 15+ test files covering CRUD, cursor, hooks, index management
2. **Optional:** `insertMany()` optimization (quality-of-life, not blocking)

**No more critical path.** All four tracks can run fully in parallel.

---

## Parallel Tracks

### Track A — Code Audit & Hardening

**Goal:** Close G9 (audit) and G2 (test coverage) for Ultra modules.

**Sub-tasks (all parallelizable at module granularity):**

1. **Wave 2 audit** — Slumber (re-check), ultra/model, ultra/cache, ultra/datetime
2. **Wave 3 audit** — ultra/remote, ultra/log, ultra/html, Streams (re-check)
3. **Wave 4 audit** — Kraft core, Kraft semanticui, 11 addons, Mutator, all KSP processors
4. **Wave 5 audit** — Monko (after Phase 2 ships)
5. ~~**Hardening**~~ — ✅ DONE (2026-04-07). vault (~105 tests), html (~26 tests), semanticui (~29 tests) all hardened to
   35%+; remote/log/maths already above target

**Dependencies:** None. Each wave and each module is independent.

**Deliverables:** Audit findings documents in `.claude/plans/`, test files added, bugs fixed.

---

### Track B — Database Portability (Mostly Done)

**Status (2026-04-05):** Monko Phase 2 core API complete. All 7 target repos ported + 3 bonus repos.
See "Critical Path" section above for the full shipped inventory.

**Remaining work:**

1. ~~**Monko core test expansion**~~ — ✅ DONE (2026-04-06). 23 test files, 234 tests covering driver, codec, cursor,
   hooks, indexes, filter/sort/update DSL, property paths

2. **`insertMany()` optimization** in MonkoDriver — replace the loop-insert workaround with a bulk write.
   Owner: quick task, ~0.5 day. Not blocking for v1.

3. **Funktor test blitz for ported repos** — depth coverage beyond the current 1 integration spec per
   module. Joins Track C as test work rather than Track B as critical path.

**Deliverables:** Comprehensive Monko CRUD test suite, bulk insert, validated dual-backend architecture.

---

### Track C — Documentation & KDoc

**Goal:** Close G4 (KDoc 90%+) and ship polished docs.

**Sub-tasks (all parallelizable):**

1. **ultra/cache KDoc blitz** — 24% → 90% (largest single gap)
2. **Funktor KDoc pass** — ~50% → 90% across ~600 files (parallelizable per submodule)
3. **Funktor TODO cleanup** — ~15 TODOs, mostly "make configurable"
4. **KDoc for new Monko code** — as Track B ships
5. **FunktorConf showcase** — design done, code pending (Event/Speaker/Attendee CRUD)
6. **Funktor docs improvement Wave 1** — blocked on FunktorConf; adds real examples to thin pages

**Dependencies:** None. FunktorConf blocks Funktor docs improvement, but both are internal to this track.

**Deliverables:** KDoc coverage at 90%+, FunktorConf demo deployed, 5 thin Funktor pages filled out.

---

### Track D — Release Engineering (Late-phase)

**Goal:** Close G8 (ApiRoute refactor), finish ES2015 rollout, ship v1.0.0.

**Sub-tasks:**

1. ~~**ApiRoute refactor**~~ — ✅ DONE (2026-04-03). PRs #40+#41: OutgoingConverter removed, ValidateRoutesOnAppStarting
   added, 16 implementations updated
2. ~~**ES2015 rollout**~~ — ✅ DONE (2026-04-06). All 48 JS modules target ES2015; jsJar + jsTest pass
3. **User-specific API access matrix (ApiAcl)** — 7 files, design done (another agent in-flight)
4. ~~**Non-blocking DBAL**~~ — ✅ DONE (2026-04-09). Flow Cursor, suspend Storable.resolve()/invoke(),
   Ref/LazyRef unified (LazyRef deleted), RefCodec zero-runBlocking, EntityCache async. See archived plan.
6. **Clean build verification** — `./gradlew clean build` across all 79 modules
7. **funktor-demo end-to-end test** — against both ArangoDB and MongoDB
8. **Version bump + release** — 0.104.2 → 1.0.0, CHANGELOG, Maven Central publish, v1.0.0 tag

**Dependencies:** Step 5 depends on Track B being done. Steps 1-3 are independent.

**Deliverables:** Clean `1.0.0` release artifacts on Maven Central.

---

## Agent Assignment Strategy — Updated 2026-04-05

With Track B mostly shipped, the remaining work is pure parallelism — no critical path.

### 3-agent parallel mode

| Agent       | Primary track                | Weeks 1-2                          | Weeks 3-4                          | Weeks 5+                    |
|-------------|------------------------------|------------------------------------|------------------------------------|-----------------------------|
| **Agent 1** | Track A (audit + hardening)  | Wave 2 + Wave 3 audits             | Wave 4 audit + Ultra hardening     | Wave 5 audit (Monko)        |
| **Agent 2** | Track B (Monko tests) + C    | Monko CRUD test suite + cache KDoc | Funktor KDoc pass                  | Funktor test blitz          |
| **Agent 3** | Track C + D (docs + release) | FunktorConf showcase               | ApiRoute refactor + ES2015 rollout | Funktor docs + release prep |

### 2-agent parallel mode

- **Agent 1:** Track A (audits → findings-driven fixes) + Track D (release prep)
- **Agent 2:** Track B (Monko tests) + Track C (KDoc + FunktorConf)

### Single-agent sequential mode (recommended priority)

1. **Monko core test expansion** (~1 week) — closes the last real Track B gap
2. **Wave 2 audit** of ultra/model, ultra/cache, ultra/datetime — findings feed Track A hardening
3. **ultra/cache KDoc blitz** (24% → 90%) — largest single KDoc gap
4. **ApiRoute refactor** (G8) — clean architecture before Funktor test blitz
5. **Funktor test blitz** — raise coverage to 25%+ across auth/cluster/logging/messaging
6. **ES2015 rollout** to remaining executable modules
7. **Release prep** — clean build, funktor-demo E2E, version bump, publish

---

## What Can Be Deferred to post-v1

- **Kraft Tailwind integration** — strategic but non-blocking
- **Kraft React shim PoC** — time-boxed experiment
- **Exposed (SQL) backend** — entirely new, high complexity
- **CI/CD pipeline** — ship locally-verified first
- **Dart codegen rewrite** — works today
- **Account activation flow** — auth enhancement
- **Line-level code coverage metrics** — post-v1 polish

---

## Shipping Checklist (final gate before `git tag v1.0.0`)

- [ ] All CRITICAL/HIGH audit findings resolved (G1 ✅, Waves 2-5 pending)
- [ ] Every shippable module has test coverage ≥25% or documented exception (G2)
- [ ] Zero unresolved TODOs without a `// TODO(v1.x)` justification (G3)
- [ ] Every public API has KDoc (G4)
- [ ] `./gradlew clean build` green, no warnings (G5)
- [ ] Every library has a README (G6 ✅)
- [ ] ES2015 target set in all executable modules OR documented exception (G7)
- [ ] ApiRoute refactor shipped, all 17 implementations updated (G8)
- [ ] Wave 2-5 audits complete (G9)
- [ ] funktor-demo runs end-to-end on both ArangoDB and MongoDB
- [ ] Maven Central artifacts published for all 5 library roots
- [ ] CHANGELOG.md written
- [ ] Root README points at 1.0.0

---

## Risk Register

| Risk                                                | Likelihood | Impact | Mitigation                                                  |
|-----------------------------------------------------|------------|--------|-------------------------------------------------------------|
| Monko Phase 2 hits unforeseen MongoDB driver issues | Medium     | High   | Start with simplest repo (logging), iterate                 |
| Audit Waves find MANY new issues                    | High       | Medium | Triage into "fix now" vs "v1.x" buckets, don't scope-creep  |
| ES2015 rollout surfaces new bridge bugs             | Low        | Medium | Bridge proven in 4 apps; enable modules in waves, test each |
| ApiRoute refactor breaks existing Funktor demo      | Medium     | Medium | Refactor on branch, validate funktor-demo before merge      |
| KDoc work underestimated (cache 24%→90%)            | Medium     | Low    | Parallelizable; hire it out if needed                       |

---

## Status of in-flight pilots (not on critical path)

- **Karakum** (TypeScript→Kotlin externals) — alpha-quality, deprioritized. Skill captured at
  `.claude/skills/karakum-setup/`. Retry for simpler libraries post-v1.
- **Kraft Tailwind Phase 1** — not started. Explicitly deferred to post-v1.
- **PixiJS addon + Breakout demo** — shipped; template for future heavy-lib addons.
