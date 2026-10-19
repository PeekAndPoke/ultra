# Fixed Karma ports per module (machine-wide browser-test isolation)

**Status:** TODO — maintainer decision 2026-10-19 (relayed by Mission-Control): every module with Kotlin/JS browser tests
gets a fixed Karma port, coordinated through Mission-Control. Fit into a slot.
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
