# v1.0.0 Parallel Execution Plan

**Date:** 2026-04-05
**Current version:** 0.104.2 → **Target:** 1.0.0
**Approach:** Three independent tracks running in parallel, with one hard sequential bottleneck.

---

## Gate Status

| Gate                           | Status              | What's left                                                  |
|--------------------------------|---------------------|--------------------------------------------------------------|
| G1 — Zero CRITICAL/HIGH issues | ✅ DONE              | Wave 1 resolved all                                          |
| G2 — Public API tested (≥25%)  | 🟡 IN PROGRESS      | Funktor, ultra/vault, ultra/html, ultra/semanticui           |
| G3 — TODOs resolved or tracked | 🟡 IN PROGRESS      | ~15 in Funktor, 5 reflection, 3 vault                        |
| G4 — KDoc 90%+ public API      | 🟡 IN PROGRESS      | cache (24%→90%), Funktor (~50%)                              |
| G5 — Clean build, no warnings  | ✅ ASSUMED PASSING   | Verify at release gate                                       |
| G6 — README per library        | ✅ DONE              |                                                              |
| G7 — ES2015 everywhere         | ✅ SOLVED, ⚠ ROLLOUT | Bridge fix works; 36 modules still ES5                       |
| G8 — ApiRoute refactor         | 🟡 DESIGN DONE      | Implement `OutgoingConverter` removal + 17 ApiRoutes updates |
| G9 — Code audit complete       | 🟡 Wave 1 DONE      | Waves 2-5 (data/network/UI/Monko)                            |

---

## Critical Path (Sequential Bottleneck)

**Monko Phase 2 → Phase 3 portability → Phase 3 tests**

Nothing else blocks this chain. All other work runs in parallel around it.

```
Monko: save() + remove() + cursor + hooks
  ↓
Port 7 repos to Monko (cluster × 5, logging × 1, messaging × 1)
  ↓
Funktor test blitz for those repos (Monko + Karango variants)
```

**Owner recommendation:** One dedicated agent/person. This is ~3-4 weeks of focused work.

---

## Parallel Tracks

### Track A — Code Audit & Hardening

**Goal:** Close G9 (audit) and G2 (test coverage) for Ultra modules.

**Sub-tasks (all parallelizable at module granularity):**

1. **Wave 2 audit** — Slumber (re-check), ultra/model, ultra/cache, ultra/datetime
2. **Wave 3 audit** — ultra/remote, ultra/log, ultra/html, Streams (re-check)
3. **Wave 4 audit** — Kraft core, Kraft semanticui, 11 addons, Mutator, all KSP processors
4. **Wave 5 audit** — Monko (after Phase 2 ships)
5. **Hardening**: ultra/vault (18%→35%), ultra/html (15%→35%), ultra/semanticui (27%→35%), ultra/remote (21%→35%),
   ultra/log (29%→35%), ultra/maths (33%→35%)

**Dependencies:** None. Each wave and each module is independent.

**Deliverables:** Audit findings documents in `.claude/plans/`, test files added, bugs fixed.

---

### Track B — Database Portability (Critical Path)

**Goal:** Ship Monko + port 7 funktor repos to enable database-agnostic architecture.

**Sub-tasks (sequential within track):**

1. **Monko Phase 2 completion** (~2 weeks)
    - `save()` / `update()`: TODO
    - `remove()`: TODO
    - Repository hooks: onBeforeSave, onAfterSave, onAfterDelete
    - Cursor: fullCount, timeMs
    - Batch insert, index management
    - Tests: 3 → 15+ files

2. **Portability Phase 3** (~1-2 weeks, parallel across 7 repos once DSL stable)
    - cluster/BackgroundJobsQueueRepo
    - cluster/BackgroundJobsArchiveRepo
    - cluster/Lock repos (3 repos)
    - logging/LogRepository + Storage
    - messaging/SentMessagesRepo

**Dependencies:** Phase 3 is **HARD BLOCKED** on Phase 2.

**Deliverables:** Monko feature-complete, 7 new `*MonkoRepo.kt` files, dual-backend auth pattern extended.

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

1. **ApiRoute refactor** — remove `OutgoingConverter` param, add `RouteValidationOnAppStartingHook`, update 17 ApiRoutes
   implementations
2. **ES2015 rollout** — enable `target.set("es2015")` in remaining 36 Kotlin/JS modules, verify each
3. **User-specific API access matrix (ApiAcl)** — 7 files, design done
4. **Clean build verification** — `./gradlew clean build` across all 79 modules
5. **funktor-demo end-to-end test** — against both ArangoDB and MongoDB
6. **Version bump + release** — 0.104.2 → 1.0.0, CHANGELOG, Maven Central publish, v1.0.0 tag

**Dependencies:** Step 5 depends on Track B being done. Steps 1-3 are independent.

**Deliverables:** Clean `1.0.0` release artifacts on Maven Central.

---

## Agent Assignment Strategy

### 3-agent parallel mode

| Agent       | Primary track                | Weeks 1-2                              | Weeks 3-4                        | Weeks 5+                      |
|-------------|------------------------------|----------------------------------------|----------------------------------|-------------------------------|
| **Agent 1** | Track B (DB portability)     | Monko save/remove/cursor/hooks + tests | Port 7 repos to Monko            | —                             |
| **Agent 2** | Track A (audit + hardening)  | Wave 2 + Wave 3 audits                 | Wave 4 audit + Ultra hardening   | Wave 5 audit (Monko)          |
| **Agent 3** | Track C + D (docs + release) | cache KDoc + FunktorConf showcase      | Funktor KDoc + ApiRoute refactor | ES2015 rollout + release prep |

### 2-agent parallel mode

- **Agent 1:** Track B (critical path) + Track D release prep
- **Agent 2:** Track A + Track C

### Single-agent sequential mode

Priority order: Monko save/remove → port 2-3 critical repos → Wave 2 audit → cache KDoc → ApiRoute refactor → release

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
