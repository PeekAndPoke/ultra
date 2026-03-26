# Library Maturity, Quality & Promotion Assessment

**Date:** 2026-03-26 (updated)
**Previous assessment:** 2026-03-25
**Reviewers:** Senior Software Engineer, QA Engineer, Developer Relations Engineer

---

## What Changed Since Last Assessment (March 26)

- **Kraft docs overhaul** — 8 → 16 doc pages. Added: Data Loading, Messaging, Overlays, Drag & Drop, Utilities, Testing,
  DOM Events, plus major rewrites of Forms & Validation
- **Kraft KDoc** — Full public API documentation across all modules: core (535 KDoc blocks), semanticui (193), addons (
  186), testing (98). Coverage went from ~10% to ~80%+
- **Kraft tests** — 6 test files, 92 test cases (was ~25). New: DomEventsSpec (47 tests), KQueryDomSpec (28 tests)
- **Kraft testing module** — KQuery expanded with 50+ DOM helpers: event dispatch (mouse, keyboard, pointer, touch,
  drag, clipboard, animation), form state (values, checked, disabled), traversal (parents, children, nth), attributes,
  classes, innerHTML, awaitCss
- **DOM events** — dom_events.kt expanded from 28 to 56 event handlers. Added: pointer events (8), touch events (4),
  clipboard (3), animation/transition (4), drag (4), scroll, load, beforeInput
- **Form demos** — All 12 form demo pages restructured with CodeBlocks extraction (source code shown alongside live
  forms)
- **StyleSheet autoMount** — StyleSheet, RawStyleSheet, StyleSheetTag now auto-mount by default. StyleSheet uses
  requestAnimationFrame to defer mount after rule initialization
- **Kraft READMEs** — 18 new README.md files across all sub-modules (core, semanticui, testing, 11 addons, 3 examples)
- **Docs site embeds** — 7 pages now have "See it in action" iframes linking to live fomanticui/addons examples
- **Addons page** — All JS libraries linked to their npm packages

## What Changed (March 25)

- **ultra/common split** — 90 TODOs → 2. Extracted into 8 focused modules (cache, datetime, fixture, maths, model,
  reflection, remote)
- **Slumber audit** — 15 new test files (45→60), KDoc on all public API, typo fixed, dead code removed
- **Karango audit** — dead entity.kt deleted, debug code removed, KDoc on public API
- **Kontainer + Streams** — full documentation added by another agent
- **KDoc upgrade** — Slumber 23%→55% (16 files), Kontainer 43%→85% (4 files), Streams 57%→96% (1 file)
- **Docs site** — 41 pages across 5 libraries, llms.txt with per-library split, local fonts
- **Kraft examples** — 3 repos upgraded to Kotlin 2.3.10 / Ultra 0.102.0
- **ultra/datetime** — inherited 45 TODOs from ultra/common split (new tech debt hotspot)

---

## Quality Ranking (March 2026)

### Tier 1: GREEN — Promotion Ready

| Rank | Module           | Score | Src | Tests | Ratio | TODOs | KDoc | Docs         | Assessment                                                                                                         |
|------|------------------|-------|-----|-------|-------|-------|------|--------------|--------------------------------------------------------------------------------------------------------------------|
| 1    | **Slumber**      | 4.9   | 122 | 60    | 49%   | 3     | ~98% | 8 pages      | Gold standard. Full KDoc on public API + all built-in codecs, primitives, datetime.                                |
| 2    | **Streams**      | 4.8   | 51  | 21    | 41%   | 0     | 100% | 8 pages      | Zero TODOs, 100% KDoc. Battle-tested. Compact and clean.                                                           |
| 3    | **Karango**      | 4.3   | 183 | 131   | 72%   | 20    | 67%  | 8 pages      | Best raw test count in ecosystem. Battle-tested. TODOs are mostly unimplemented AQL functions (roadmap, not debt). |
| 4    | **Kontainer**    | 4.3   | 69  | 20    | 29%   | 6     | ~98% | 7 pages      | Battle-tested DI. Full docs. KDoc complete on all public API.                                                      |
| 5    | **ultra/common** | 4.0   | 71  | 28    | 39%   | 2     | 24%  | (foundation) | Massive improvement: 90→2 TODOs. The split was the right call.                                                     |
| 6    | **ultra/model**  | 4.0   | 20  | 9     | 45%   | 0     | -    | -            | Zero TODOs, strong ratio. Small focused module post-split.                                                         |

### Tier 2: YELLOW — Solid, Promote with Context

| Rank | Module               | Score | Src  | Tests | Ratio | TODOs | KDoc | Docs                  | Assessment                                                                                                        |
|------|----------------------|-------|------|-------|-------|-------|------|-----------------------|-------------------------------------------------------------------------------------------------------------------|
| 7    | **Mutator**          | 3.7   | 43   | 18    | 42%   | 1     | 12%  | -                     | Good tests but NOT battle-tested (maintainer confirmed). Needs docs and more tests before promotion.              |
| 8    | **ultra/security**   | 3.8   | 33   | 12    | 36%   | 1     | -    | -                     | Clean, active. Security-sensitive code warrants higher test coverage.                                             |
| 9    | **ultra/cache**      | 3.5   | 17   | 6     | 35%   | 1     | 24%  | -                     | Clean post-split. Caching correctness needs more tests.                                                           |
| 10   | **ultra/reflection** | 3.4   | 14   | 6     | 43%   | 5     | 63%  | -                     | Good ratio and KDoc but 5 TODOs in 14 files is high density.                                                      |
| 11   | **ultra/maths**      | 3.2   | 12   | 4     | 33%   | 1     | -    | -                     | Small, clean. Needs precision edge-case tests.                                                                    |
| 12   | **ultra/log**        | 3.3   | 14   | 4     | 29%   | 1     | -    | -                     | Small, adequate.                                                                                                  |
| 13   | **ultra/remote**     | 3.0   | 33   | 7     | 21%   | 1     | -    | -                     | Network code at 21% test ratio is thin.                                                                           |
| 14   | **Kraft**            | 3.8   | ~192 | 92    | 48%   | -     | ~80% | 16 pages + 3 examples | Major upgrade: 16 doc pages, 92 tests, KDoc on all public API, 18 READMEs, live embeds. Test ratio 48% (was 10%). |

### Tier 3: RED — Not Ready for Promotion

| Rank | Module               | Score | Src | Tests | Ratio  | TODOs  | Assessment                                                                                         |
|------|----------------------|-------|-----|-------|--------|--------|----------------------------------------------------------------------------------------------------|
| 15   | **ultra/datetime**   | 2.8   | 64  | 28    | 44%    | **45** | Inherited tech debt from common split. Good test ratio but 45 TODOs is the worst in the ecosystem. |
| 16   | **ultra/vault**      | 2.5   | 45  | 5     | 11%    | 6      | **Critical gap.** Persistence/entity module at 11% test coverage. Used by Karango and Funktor.     |
| 17   | **ultra/html**       | 2.7   | 13  | 2     | 15%    | 0      | Near-zero coverage.                                                                                |
| 18   | **ultra/semanticui** | 2.6   | 15  | 4     | 27%    | 0      | Stale (last commit March 10). UI module, lower blast radius.                                       |
| 19   | **Funktor**          | 2.0   | 438 | 20    | **5%** | ~15    | **Blocker.** Largest module, lowest test ratio. Cannot be promoted.                                |
| 20   | **Monko**            | 1.5   | 15  | **0** | 0%     | 5      | **Blocker.** Zero tests. Incubating.                                                               |

---

## Key Changes from Previous Assessment

| Item                 | Before (March 24)                   | After (March 25)                                         | Change                |
|----------------------|-------------------------------------|----------------------------------------------------------|-----------------------|
| ultra/common TODOs   | 90                                  | 2                                                        | Split into 8 modules  |
| ultra/datetime TODOs | (didn't exist)                      | 45                                                       | New tech debt hotspot |
| Slumber tests        | 45                                  | 60                                                       | +15 new test files    |
| Slumber KDoc         | Partial                             | All public API                                           | Full audit complete   |
| Karango dead code    | entity.kt, debug lines              | Removed                                                  | Clean                 |
| Karango KDoc         | Minimal                             | Public API documented                                    | Audit complete        |
| Slumber KDoc         | 23%                                 | ~98% (+45 files)                                         | All codecs documented |
| Kontainer KDoc       | 43%                                 | ~98% (+5 files)                                          | Full public API       |
| Streams KDoc         | 57%                                 | 100% (+1 file)                                           | Complete              |
| Documented libraries | 1 (Kraft)                           | 6 (Kraft, Kontainer, Slumber, Streams, Karango, Mutator) | +5 libraries          |
| Docs site pages      | 10                                  | 56                                                       | +46 pages             |
| llms.txt             | Basic placeholder                   | Full index + per-library split                           | Complete              |
| Example repos        | Stale (Kotlin 2.2.20, Ultra 0.96.2) | Updated (Kotlin 2.3.10, Ultra 0.102.0)                   | All 3 compiling       |

---

## Promotion Readiness Summary

### Ready NOW (5 libraries)

1. **Kontainer** — battle-tested DI, 7 doc pages
2. **Slumber** — audited serialization, 8 doc pages, 60 tests
3. **Streams** — zero TODOs, 8 doc pages, battle-tested
4. **Karango** — 131 tests, 8 doc pages, battle-tested
5. **Kraft** — 16 doc pages, 92 tests, 18 READMEs, KDoc on all public API, 3 example repos, live embeds

### Blockers for Public Launch (PR engineer findings)

1. ~~**No LICENSE file**~~ — **DONE.** Apache 2.0, copyright 2019-2026 PeekAndPoke
2. ~~**Root README.MD is stale**~~ — **DONE.** Mentions peekandpoke.io, Kotlin 2.3.10, all 5 libs, llms.txt
3. **No CI/CD visible** — no GitHub Actions, no green badges
4. **No CHANGELOG** — 320 commits in 2025 with no release notes

### Not Ready

- **Mutator** — needs tests + docs
- **Funktor** — 5% test ratio, needs major test investment
- **Monko** — zero tests, incubating

---

## Top 5 Priorities (updated)

### Immediate (before any public announcement)

1. ~~**Add LICENSE file**~~ — **DONE.** Apache 2.0
2. ~~**Rewrite root README.MD**~~ — **DONE.**
3. **Add GitHub Actions CI** — run tests for the GREEN modules, show badges

### Short-term (next sprint)

4. **Triage ultra/datetime's 45 TODOs** — the new tech debt hotspot from the common split
5. **Invest in ultra/vault tests** — 11% coverage on the entity/persistence layer is a risk for Karango

### Medium-term

6. ~~**Add Kraft tests**~~ — **DONE.** 92 tests (was ~25), 48% ratio. DomEventsSpec, KQueryDomSpec added.
7. **Write Mutator docs + tests** — next library in the promotion pipeline
8. **Funktor test sprint** — start with core + rest modules
9. **Expand Kraft tests further** — component lifecycle tests, routing tests, form validation tests
