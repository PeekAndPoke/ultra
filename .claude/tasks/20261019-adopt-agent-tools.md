# Adopt the shared agent tools into `console/tools/`

**Status:** TODO — the maintainer approved the shared first drafts (relayed by Mission-Control, 2026-10-19); fit in when
a slot allows, never displacing a running track
**Plan:** none — coordinator tooling. Source drafts: `/opt/dev/peekandpoke/agent-tools/` (README there)
**Security-critical:** no — but `mutate` edits and restores source files, so its restore path is the risk

## Why

Mechanical work done by inference today, with recorded traps: confirming a spec ran (kotest ignores `--tests`, the
XML mis-attributes failures, FROM-CACHE looks fresh), the hand mutation protocol (path-mirrored backups, cp + cmp
restore, compile-error "kills"), and task-doc consistency (statuses vs folder, links broken by `git mv`).

## Spec

- [ ] Read each draft in full before running it (they were never run inside this build).
- [ ] `console/tools/test-digest/` — copy and adapt; one real run against a module build and against the full suite.
      A spec counts as RAN only if its `<testsuite timestamp>` falls inside the run.
- [ ] `console/tools/mutate/` — copy and adapt (test command = `console/wt-gradle.sh`, `--rerun` on the test task,
      never `--rerun-tasks`). One real run: a known-KILLED mutant and a known-SURVIVED one, and a Ctrl-C mid-run that
      must leave the file byte-identical.
- [ ] `console/tools/docs-lint/` — copy the generic core; put this repo's rules in `docs_lint_local.py`: status vs
      folder, `Security-critical: yes` → a red-team file exists, ledger rows have Disposition + Reason, a `Verdict:`
      before archiving, `.claude/tasks/…` links resolve. Fix the 5 broken links it already found in
      `.claude/tasks-archive/2026-03`/`2026-04` (`funktor-v1-roadmap.md`, `monko-query-dsl.md`,
      `mutator-quality-improvements.md`).
- [ ] `console/tools/review-pack/` — per review round: `full.diff` from the merge base (untracked included) and
      `delta.diff` since the previous round, round states under `refs/review-pack/<label>/round-<n>`; warns when the merge
      base moved (T3's wrong-diff-base slip). Would have replaced the hand-made `change.diff` / ledger-free task copy of
      T1's round 2. One real run on the next gate.
- [ ] Copy the CURRENT versions (test-digest fixed both first-run defects; mutate now re-runs the restored code after a
      campaign so the last mutant's failing XML does not linger as fresh).
- [ ] Point the review-loop skill and CLAUDE.md's verification traps at the tools (one line each), on master.
- [ ] Skill `.claude/skills/console-tools/SKILL.md` from the draft `/opt/dev/peekandpoke/agent-tools/skill/console-tools/
      SKILL.md` (requested via Mission-Control 2026-10-19), adapted: `console/wt-gradle.sh` as the test command, the
      BUILD-LOCK and one-full-suite rules; changes recorded under "Repo notes"; per the skill-builder conventions.
- [ ] One CLAUDE.md line pointing at that skill — **approved by the maintainer directly 2026-10-19** ("yes add the line").
- [ ] No dependency on the shared folder after the copy.
- [ ] **Final step — condense the always-loaded files** (maintainer, directly, 2026-10-19: "yes condense it as you
      proposed"; still show the before/after summary with line counts BEFORE committing):
      - KEEP in CLAUDE.md one line per tool: its name, why it exists, the trap it guards against.
      - MOVE tool-only detail (commands, verdicts, flags, `wt-gradle.sh` in mutate specs) to the console-tools skill.
      - REMOVE hand procedures the tools enforce (the cp / path-mirrored backup / never-`git checkout` protocol,
        reading `TEST-*.xml` by hand) → "use <tool>".
      - DON'T remove traps no tool covers: compile sweep, stale test classes after an ABI change, greps missing
        receiver-less calls and anonymous objects, untyped `shouldBe`, `\uXXXX` escapes, "a surviving mutant is not
        always a missing test". When unsure, keep.
      - Same pass over MEMORY.md and skills that repeat the protocol (review-loop's mutation steps).

## Findings from the first real run (2026-10-19, test-digest on master's full suite after the T1 merge)

Digest mode against `clean build` (15 864 tests): the expected specs it named RAN correctly, and a wrong spec name was
reported MISSING (it was `KarangoGlobalLocksE2eSpec`/`MonkoGlobalLocksE2eSpec`). Two draft defects to fix in our copy
(and report upstream):
- The summary claims "45 test task(s) not run (cache/up-to-date)" — all 45 are `clean*Test` tasks; the test-task regex
  `[Tt]est$` matches them. Exclude `^clean` (and `:clean`).
- vitest's JUnit XML (`funktor/codegen/build/test-results/vueVerify/TEST-vue-verify.xml`) has a `<testsuites>` root with
  no timestamp, the timestamps sit on the child `<testsuite>`s — so its 66 tests count as stale (15 798 instead of
  15 864). Read child suites when the root is `<testsuites>`.

## Not here

`stack-merge` (shared, 2026-10-19): not adopted for now — tracks merge one at a time with `merge --no-ff` and a full
suite each, and two parallel tracks rarely need stacking; revisit if merges start to queue. Merge preflight (#4) is undrafted; the repo-facts snapshot (#5) is Mission-Control's own.
