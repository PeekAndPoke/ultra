# ULTRA-00316: Plan: close the open epics from the foundation upwards, in parallel tracks

> **Status (2026-11-13): IN PROGRESS.**

## Why this exists

The maintainer (2026-11-13): "I would like to close open topics by epic starting from the components at the root of
all dependencies ... finish open epics from the foundation upwards ... with multiple agents in parallel working on
different epics where they would not interfere." Decisions of 2026-11-13: triage first ("yes"), three tracks
("three tracks"), the triage proposes do / drop / park for ideas ("yes lets review the tickets after triage"),
red-team sessions are run as each area closes ("run it"), this plan is stored ("store the plan"). And: "Once the
three tracks are done i would like to do a deep review on slumber: correctness, performance, bugfixes, eventually
missing features."

## The rules that keep tracks apart

- **A track owns a set of module directories and nothing else.** Two tracks never share a module.
- **At most three tracks**, each in a worktree made with `console/worktree-new.sh <track>` and built only through
  `console/wt-gradle.sh` (its own Arango and Mongo databases). One full suite at a time, machine-wide.
- **Only the coordinator, on master, touches** `CHANGELOG.md`, `CLAUDE.md`, skills, the roadmap (ULTRA-00028),
  `settings.gradle`, `buildSrc`, and ticket numbers (`docs/tasks/_next-ticket.txt`). A track writes its changelog
  text into its ticket; a track that needs a new ticket asks the coordinator for the number.
- **A ticket that changes a public API of a lower layer runs first** and is merged before a track above it starts.
  After every merge the other tracks rebase.
- **Every ticket passes the usual gate** (tests, mutation checks, the three-charter review; standard tier, strict
  for a ticket marked security-critical). The coordinator merges after a clean gate.
- **An epic closes** when every child is done, or the maintainer parked it (ideas go to a post-v1 list).
- **Budget:** cheap work only from 85 % of the week, stop at 90 %; a new wave starts only below 60 %.

## Step 0: triage (read-only, running since 2026-11-13)

One agent per epic group checks every open ticket against today's code: done already, ready, needs a decision,
drop or park. Reports: one decision list per epic for the maintainer. Results are recorded below as they arrive.

## The waves (by the module dependency graph)

| Wave | Track | Owns | Epic and tickets |
|---|---|---|---|
| 0 | coordinator | build, tooling | ULTRA-00230 Karma ports (browser tests of two worktrees collide without it) |
| 1 | A: datetime | `ultra/datetime` | ULTRA-00210 first (API change), then 00211, 00215, 00217 |
| 1 | B: common, reflection, log, cache | those four modules | ULTRA-00143, 00148, 00149, 00158, 00159, 00071 |
| 1 | C: mutator | `mutator/*` | ULTRA-00144, 00234 |
| 2 | A: slumber, a deep review | `ultra/slumber` | correctness, performance, bugfixes, missing features (maintainer), with ULTRA-00147, 00154, 00163, 00180 |
| 2 | B: persistence | `ultra/vault`, `karango`, `monko` | ULTRA-00060, 00140, 00141, 00142, 00173, 00313, 00300 |
| 2 | C: Kraft | `kraft/*` | ULTRA-00165, 00203, 00239, and the ideas the maintainer keeps |
| 3 | A: rest and core | `funktor/core`, `funktor/rest` | epic ULTRA-00250, ULTRA-00299, 00092, 00151 |
| 3 | B: messaging, cluster, depot | those modules | ULTRA-00124, 00224, 00164 |
| 3 | C: codegen | `ultra/codegen`, `funktor/codegen` | epic ULTRA-00252, the parts that need no auth |
| 4 | A: auth | `funktor/auth` | epic ULTRA-00248; the value-class ids (ULTRA-00109, 00110) first and alone |
| 4 | B: i18n | `ultra/i18n`, tooling | epic ULTRA-00256 |
| 5 | A: saas | `funktor/saas` | epic ULTRA-00249, after auth is merged |
| 5 | B: insights and SDK auth | `funktor/insights`, `funktor/inspect`, codegen | ULTRA-00190, 00166, 00168, 00186 |

Across the waves:
- **Docs backlog (ULTRA-00260):** a docs ticket runs when its area's wave is merged, by a track that owns only
  `docs-site`.
- **Red team (ULTRA-00259):** each session is run, read-only, when its area closes; findings become tickets in
  that area's epic.
- **Build and tooling (ULTRA-00261):** ULTRA-00238 (Gradle version catalog) touches every build file: alone,
  between two waves.

## Progress

- 2026-11-13: plan stored; triage started (nine agents); ULTRA-00230 started on master.
- 2026-11-13, wave 1:
  - **Track A (datetime), batch 1 MERGED** (`87ad3489`): ULTRA-00215, ULTRA-00217, ULTRA-00317 DONE, the `recurse`
    item of ULTRA-00143 done. Gate: round 2 CLEAN, delta round 3 CLEAN. Batch 2 (ULTRA-00211 with ULTRA-00210,
    shipped together) is next; `completeDay` waits for the 24:00 decision.
  - **Track B (cache and log), ULTRA-00159: STOPPED, waiting for the maintainer.** Round 2 CLEAN from
    implementation and domain, NOT CLEAN from security: a hot key outlives its write TTL and the hard TTL
    (CONFIRMED by probe, pinned as a known-defect spec on `wt/cache-log`). Two rounds without a clean one.
  - **Track C (mutator): DONE and closed.** Batch 1 MERGED (`ae0a23cc`, ULTRA-00144 DONE); ULTRA-00234 MERGED
    (`d2a19b0c`), round 3 CLEAN on the top level, master green (JVM 241, JS 241). The maintainer (2026-11-14):
    Mutator is an experiment with no user; open points go to the future mutator epic ULTRA-00322 (ULTRA-00318,
    ULTRA-00320, ULTRA-00323 design for element tracking, ULTRA-00324, ULTRA-00325). No further mutator work in this
    plan.
  - **On master:** ULTRA-00230 (Karma ports), and the MAJOR of ULTRA-00158 (database log lost stack traces) fixed
    (`c218d1a6`); its gate continues after the open log decisions.

- 2026-11-14, the maintainer's order after wave 1 ("my idea is to harden everything we have here, starting from the
  foundational libs"): (1) finish tracks A and B, including the five real defects left open in the cache; (2) the
  **Slumber deep review** next, planned and shown to him before it starts; (3) then **red-team the modules just
  touched** (datetime, cache, log; not Mutator, which is parked in ULTRA-00322). A repo-wide **module review table**
  (which module was reviewed when, which charters, which model) is kept from now on: "we need to review things
  frequently, ideally always with the sharpest models we have available."

## History

- 2026-11-13: created on the maintainer's word.
