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
- [ ] Point the review-loop skill and CLAUDE.md's verification traps at the tools (one line each), on master.
- [ ] No dependency on the shared folder after the copy.

## Not here

Merge preflight (#4) is undrafted; the repo-facts snapshot (#5) is Mission-Control's own.
