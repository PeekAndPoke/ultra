# Six Hats Analysis: Library Maturity, Stability & Promotion Strategy

**Date:** 2026-03-24
**Rounds:** 3

---

## White Hat — Facts & Context

| Library          | Source Files             | Test Files | Git Commits     | Recent (2025+) | First Commit | Last Commit | Platform      | TODOs |
|------------------|--------------------------|------------|-----------------|----------------|--------------|-------------|---------------|-------|
| ultra/common     | 210                      | 75         | 49              | 49             | 2025-03      | 2026-03     | JVM+JS+common | 90    |
| ultra/kontainer  | 70                       | 20         | 11              | 11             | 2025-03      | 2026-03     | JVM           | 9     |
| ultra/slumber    | 107                      | 45         | 20              | 20             | 2025-03      | 2026-03     | JVM+common    | 3     |
| ultra/streams    | 42                       | 15         | 10              | 10             | 2025-09      | 2026-03     | JS+common     | 0     |
| ultra/vault      | 42                       | 2          | 21              | 21             | 2025-03      | 2026-03     | JVM+common    | 6     |
| ultra/security   | 33                       | 12         | 10              | 10             | 2025-03      | 2025-11     | JVM+common    | 1     |
| ultra/semanticui | 15                       | 4          | 6               | 6              | 2025-09      | 2026-03     | JS+common     | 0     |
| ultra/html       | 4                        | 0          | 9               | 9              | 2025-09      | 2026-03     | JS+common     | 0     |
| ultra/log        | 13                       | 3          | 2               | 2              | 2025-10      | 2025-10     | JVM+common    | 1     |
| ultra/meta       | 2                        | 1          | 7               | 7              | 2025-03      | 2025-10     | JVM           | 0     |
| mutator          | 43 (core+ksp)            | 18         | 305 (56 recent) | 56             | **2019-08**  | 2026-03     | KSP/common    | 1     |
| karango          | 184 (core+ksp)           | **131**    | 49              | 49             | 2025-03      | 2026-03     | JVM/KSP       | 20    |
| funktor          | **438** across 9 modules | 20         | 75              | 75             | 2025-03      | 2026-03     | JVM+common    | 8     |
| monko            | 15 (core+ksp)            | **0**      | 13              | 13             | 2025-10      | 2026-03     | JVM/KSP       | 5     |

Additional facts:

- Mutator has existing documentation (`mutator/core/docs/`) and examples (`mutator/core/src/examples/`)
- Kontainer has existing examples (`ultra/kontainer/src/examples/`)
- Karango has only 2 `@Deprecated` annotations — minimal API churn
- ultra/common is the foundation — every other library depends on it
- Funktor has a working demo app (`funktor-demo/`) with server + admin UI

---

## Discussion Summary

### Round 1 — Opening Positions

#### Red Hat — Feelings & Intuition

- Mutator's 7-year track record creates emotional trust that no amount of testing in newer libs can match
- Karango's 131 tests feel like love — someone cared deeply about correctness
- Kontainer feels approachable — existing examples, modest scope, low friction
- Slumber gives quiet confidence — low TODOs, good test ratio
- ultra/common's 90 TODOs make a developer nervous about building on shifting ground
- Funktor is intimidating (438 files, 20 tests) — "ambitious but undercooked"
- Monko feels premature — "inviting people to a construction site"

#### Black Hat — Risks & Problems

- No library has the test-coverage + TODO-cleanliness + API-stability trifecta
- ultra/common (90 TODOs) is the weakest foundation — everything downstream inherits its risk
- Funktor's 22:1 source-to-test ratio is a liability
- ultra/security hasn't been touched in 4 months
- ultra/vault has security-adjacent code with only 2 test files
- Monko is critically unready (zero tests, 5 months old)
- Promoting libraries means defending them — can we?

#### Yellow Hat — Benefits & Value

- Mutator + Karango are promotion-ready today
- Slumber's 3 TODOs suggest high polish
- Kontainer's existing examples lower adoption friction
- Funktor's size is a value signal if positioned as "batteries included"
- The cross-library synergy is undervalued — each library's credibility lifts the others

#### Green Hat — Creative Ideas

- Don't rank libraries individually — rank **stories** you can tell with them together
- "Full-stack Kotlin" bundle: Kontainer + Slumber + Funktor + Karango
- Mutator's longevity IS the differentiator — "battle-tested since 2019" badge
- Karango's 131 tests = "131 ways to query ArangoDB" — tests as living cookbook
- Flip TODO counts into contribution invitations
- Announce Monko as "coming soon" to plant a flag in Kotlin+MongoDB

#### Blue Hat — Process & Next Steps

- Ranked: Mutator > Slumber > Karango > Kontainer > Common > Funktor > Monko (hold)
- Phase 1: Mutator + Slumber
- Phase 2: Karango + Kontainer
- Phase 3: Common (stabilized) + Funktor (after test remediation)
- Gate: no promotion without docs + tests + examples trifecta

### Round 2 — Key Debates

The central tension was between **promotion speed and foundation integrity**. Black Hat insisted that ultra/common's 90
TODOs must be triaged before anything is promoted — "building on sand." Yellow Hat countered that TODOs represent
documented awareness, not hidden debt, and that a maintainer who openly tags technical debt signals honesty. Green Hat
proposed reframing TODOs as a "contribution map" — turning a weakness into a community onboarding tool. Black Hat pushed
back hard: "inviting external contributors to resolve architectural debt you haven't resolved yourself is outsourcing
risk to strangers."

Red Hat and Yellow Hat both argued for leading with Mutator + Karango together: "trust meets care" — Mutator's longevity
provides emotional confidence while Karango's test discipline provides intellectual confidence. Blue Hat agreed but
added Slumber between them, noting its polish and low risk.

Green Hat's most impactful idea was the **"gateway drug" architecture**: design Mutator's documentation so that examples
naturally reference Kontainer, Karango, or Slumber. Adoption becomes a journey, not a menu. Blue Hat endorsed this for
Phase 2 (the "bundle" narrative) but insisted individual libraries must clear the promotion gate first.

On Funktor, Red Hat's gut reaction ("intimidating") won the debate. Yellow Hat argued it was "one test sprint from
credibility," but Black Hat pointed out that perception of a 438-file framework with 20 tests cannot be fixed by adding
tests alone — Green Hat agreed, proposing a "persona shift" (simplify the first contact) rather than just a test sprint.

### Round 3 — Final Verdicts

| Hat    | Final Verdict                                                                                                                            |
|--------|------------------------------------------------------------------------------------------------------------------------------------------|
| Red    | Lead with Mutator — it has earned the trust and emotional commitment to survive contact with real users.                                 |
| Black  | No promotion until Mutator has tests and ultra/common has more than one test file — marketing debt compounds on technical debt.          |
| Yellow | Timebox ultra/common triage to 30 days, then release Mutator v1.0 — let a shipped artifact be the gate, not a clean TODO list.           |
| Green  | Ship a Stability Tracker on the docs site (green/yellow/red per library) — transparency about readiness is more compelling than silence. |
| Blue   | Lead with Mutator, stabilize the foundation, bundle the data layer, and let adoption unlock Funktor.                                     |

---

## Synthesis

All five hats converged on **Mutator as the clear #1** — its 7-year track record, compact scope (43 files), existing
documentation, and minimal TODOs (1) make it the lowest-risk, highest-trust library to promote first. The debate was not
whether to lead with Mutator, but what must happen before and alongside it.

The most productive tension was between urgency (Yellow/Red: "ship now, perfection is the enemy") and integrity (
Black: "test what you ship, or don't ship"). The resolution came from Blue and Green: **gate promotion on a lightweight
trifecta (docs + tests + examples)**, but timebox the gate rather than making it open-ended. Yellow's "30-day triage
then ship" proposal was the most actionable compromise.

Green Hat's two strongest contributions will shape the strategy: (1) the **Stability Tracker** — a public page showing
each library's readiness status (green/yellow/red) — turns transparency into a feature, and (2) the **"gateway drug"
architecture** — designing docs so that learning one library naturally pulls developers toward others.

The unresolved tension is Funktor. At 438 files across 9 modules with only 20 tests, it's simultaneously the most
ambitious and most risky library. All hats agree it cannot be promoted as-is. The path forward is a "persona shift" (
make first contact trivially simple) paired with targeted test investment, but this is Phase 3 work.

## Maturity Ranking (corrected by maintainer)

> **NOTE:** The six hats analysis ranked by git metrics. The maintainer corrected this based on
> real-world production usage. Kontainer and Streams are the most battle-tested libraries.
> Mutator is NOT battle-tested despite its git history age.

| Rank | Library            | Maturity          | Readiness              | Rationale                                                                        |
|------|--------------------|-------------------|------------------------|----------------------------------------------------------------------------------|
| 1    | **Kontainer**      | **Battle-tested** | Ready now              | Production-proven DI, 70 files, 20 tests, existing examples                      |
| 2    | **Streams**        | **Battle-tested** | Ready now              | Production-proven, 42 files, 15 tests, 0 TODOs, clean API                        |
| 3    | **Slumber**        | **Battle-tested** | Ready now              | Production-deployed for years, 107 files, 45 tests, only 3 TODOs                 |
| 4    | **Karango**        | High              | Near-ready             | 131 tests (highest!), 184 files, 20 TODOs to triage                              |
| 5    | **Mutator**        | Medium            | Needs test investment  | Old codebase (2019) but NOT battle-tested. Use docs push to write missing tests. |
| 6    | **ultra/common**   | Medium            | Foundation work needed | 210 files, 75 tests, 90 TODOs — foundation must stabilize                        |
| 7    | **ultra/security** | Medium            | Hold                   | 33 files, 12 tests, 4 months since last commit                                   |
| 8    | **Funktor**        | Low-Medium        | Blocked on testing     | 438 files, 20 tests — massive scope, minimal verification                        |
| 9    | **Monko**          | Low               | Incubating             | 15 files, 0 tests, 5 months old — not ready for public                           |

*(ultra/vault, ultra/html, ultra/log, ultra/meta, ultra/semanticui are internal/supporting modules — promote as part of
larger libraries, not standalone)*

## Recommended Actions

### Phase 0: Foundation (weeks 1-2)

1. **Triage ultra/common's 90 TODOs** — categorize as: critical (fix now), enhancement (backlog), or
   contribution-ready (label for community).
2. **Set up a Stability Tracker page** on peekandpoke.io showing per-library status (green/yellow/red).

### Phase 1: Lead with Kontainer + Streams (weeks 2-4)

3. **Write Kontainer docs** — existing examples are the starting point. Production-proven, approachable scope.
4. **Write Streams docs** — clean API, 0 TODOs. Used heavily by Kraft — show standalone value too.
5. **Promotion angle**: "The foundation that powers everything — DI and reactive streams, battle-tested in production."
6. Design examples that naturally reference each other and Kraft (gateway drug).

### Phase 2: Slumber + Karango (weeks 4-8)

7. **Write Slumber docs** — serialization is universal, low-risk, 3 TODOs.
8. **Write Karango docs** — 131 tests repackaged as cookbook ("131 ways to query ArangoDB").
9. **Bundle narrative**: "From DI to serialization to database — one Kotlin ecosystem."

### Phase 3: Mutator + Test Investment (weeks 6-10)

10. **Write missing unit tests for Mutator** — use the documentation push as the forcing function.
11. **Write Mutator docs** — the existing docs+examples are a starting point, but verify against actual behavior with
    new tests.
12. **Promotion angle**: "Mutate deeply nested immutable data without copy() chains — now with comprehensive tests."

### Phase 4: Full Stack (weeks 10+)

13. **Funktor persona shift** — create a "Funktor in 5 minutes" getting-started that's trivially simple.
14. **Targeted Funktor test investment** — prioritize auth + core modules.
15. **Monko** — only promote after test suite exists; label as "experimental" until then.

### Per-Library Promotion Strategy

| Library       | Promotion Angle                                  | Target Audience                             | Content Type                                      |
|---------------|--------------------------------------------------|---------------------------------------------|---------------------------------------------------|
| **Mutator**   | "Mutate the immutable — since 2019"              | Any Kotlin dev using data classes           | Before/after code comparisons, KSP magic reveal   |
| **Slumber**   | "Serialization that just works"                  | Backend Kotlin devs                         | Codec examples, type mapping demos                |
| **Karango**   | "Type-safe ArangoDB — 131 tests prove it"        | ArangoDB users, graph DB enthusiasts        | Query cookbook from test suite, migration guides  |
| **Kontainer** | "DI without reflection, without surprises"       | Devs frustrated with Dagger/Koin complexity | Simplicity comparisons, compile-time safety demos |
| **Funktor**   | "Full-stack Kotlin on Ktor — batteries included" | Full-stack Kotlin teams                     | Demo app walkthrough, module picker guide         |
| **Monko**     | "Kotlin + MongoDB + KSP (experimental)"          | Early adopters, MongoDB+Kotlin users        | "Coming soon" page, contribution invite           |
