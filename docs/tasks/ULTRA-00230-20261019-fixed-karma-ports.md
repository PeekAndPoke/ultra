# ULTRA-00230: Fixed Karma ports per module (machine-wide browser-test isolation)

> **Status (2026-10-19): NOT STARTED.** maintainer decision 2026-10-19 (relayed by Mission-Control): every module with Kotlin/JS browser tests
> gets a fixed Karma port, coordinated through Mission-Control. Fit into a slot.
>
> **Part of:** ULTRA-00261 (Build, tooling and skills epic)

**Plan:** none — build hygiene; found by Spross-Main-Dev (a spross jsBrowserTest run held two Klang suites: two projects'
Karma on the default 9876/next-free port, a ChromeHeadless joined the other's server — a run can PASS on foreign tests)
**Security-critical:** no

## Ranges (assigned by Mission-Control)

- ultra: **9931–10030** (100 ports); Kraft examples (helloworld, remote, router): **10061–10080**.
- Others, do not use: spross 9911–9930, klang 10031–10060.

## Spec

- [ ] List every module whose browser tests run under Karma (≈29 with `jsTest`/`commonTest` and a browser target — derive
      it from the build, not a directory grep: `./gradlew tasks --all | grep jsBrowserTest`).
- [ ] `<module>/karma.config.d/karma-port.js` with `config.set({ port: NNNN });` (the Kotlin/JS plugin appends every
      file there; `kraft/addons/threejs/karma.config.d/webgl.js` already exists — add beside it). Check the log for
      "server started at http://localhost:NNNN/".
- [ ] **Worktrees share module ports**, so two `ultra-wt/*` worktrees running browser tests at once still cross over
      within ultra (the full-suite flock serializes only full runs). Options: a per-worktree slot (`wt-gradle.sh` exports
      e.g. `ULTRA_KARMA_SLOT`, the port file adds `slot * 33` — 3 slots × 33 ports fit the range), or a machine-wide flock
      around `*BrowserTest` tasks in `wt-gradle.sh`. Decide, implement, document in CLAUDE.md's "Parallel worktrees".
- [ ] Same for the three Kraft example repos (10061–10080).
- [ ] Send Mission-Control the module → port list (ultra and examples).
- [ ] With the adopted test-digest: `--own-package io.peekandpoke` as the safety net (all ultra roots share it).

## History

- 2026-10-21: numbered ULTRA-00230 (was `docs/tasks/20261019-fixed-karma-ports.md`).
- 2026-11-13: built with ULTRA-00316 (wave 0). Decided: a fixed index per module in `console/tools/karma-ports/ports.txt`
  (31 modules with a browser target and test sources), port = 9931 + `ULTRA_KARMA_SLOT` * 33 + index, three blocks
  in ultra's range; `karma-ports.py` writes `<module>/karma.config.d/karma-port.js` and `--check` finds a missing or
  stale file and a module without a port. **A port that is taken fails the run** (Karma would move on to the next
  port, which is another module's): that, not a flock, is what keeps two checkouts apart. `wt-gradle.sh` gives a
  track worktree block 1 or 2; the main worktree has block 0. Verified by real runs: `:ultra:maths` on 9934 and
  `:ultra:model` on 9935 in one build; with 9934 held by another process the run fails naming the port and the
  module; `ULTRA_KARMA_SLOT=1` gives 9967; `ULTRA_KARMA_SLOT=7` is refused. Open: the three Kraft example repos
  (10061-10080) are other repositories; the module to port list for Mission Control; a line in CLAUDE.md (needs the
  maintainer's OK); `--own-package` of the test digest (ULTRA-00227 first).
