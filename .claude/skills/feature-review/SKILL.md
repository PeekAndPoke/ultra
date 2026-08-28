---
name: feature-review
description: Use after implementing a feature to run the mandatory review gate — the review loop with three charters (implementation & code style, domain expert, security). Also use when someone says "feature-review", "review this feature", "run the review gate", or finishes a task file in .claude/tasks/.
argument-hint: [task-file-or-feature-description]
---

## What This Skill Does

The mandatory gate a feature passes before it can be DONE — the `→ review →` step of the CLAUDE.md
workflow.

**The mechanics live in `.claude/skills/review-loop/`. Read it first; this skill is the gate wrapped
around it.** In particular the gate LOOPS: a fix produced by a review is itself an unreviewed change, so
rounds repeat until a clean one, and a settled finding stays settled unless a reviewer can name what is
factually wrong in the reason it was settled.

This skill adds only what is specific to gating a feature:

| | |
|---|---|
| Preconditions | including the e2e requirement, which is a gate failure and not a finding |
| The three charters | implementation & code style, domain expert, security |
| Red-team follow-up | for security-critical features |
| The record | the task file's Review record table, and the DONE/archive decision |

> **Status: PROVISIONAL.** Adjust as we learn what catches real issues. Log accepted changes in the
> changelog at the bottom.

## When to run

After implementing any feature with a task file in `.claude/tasks/`, before marking it DONE. The change
should be committed or at least staged so the change set is well defined.

## Preconditions — check these first

1. **Identify the change set and state the base explicitly** — usually the branch point off `master`.
   On this shared worktree, prefer scoping by FILE SET: other agents' commits interleave with yours, so
   a commit range will not say what you mean.
2. **Confirm the feature's own tests pass and are in the change set.** If backend code has **no
   end-to-end tests** (`AppSpec`/`AppUnderTest`, both DB backends where storage is involved), STOP and
   flag it — per CLAUDE.md that is a gate failure, not a finding to debate.
3. **Load the task file and its linked plan** for the domain context the reviewers need.
4. **Read `.claude/BUILD-LOCK.md`.** Reviewers are read-only and need no lock; applying fixes and
   running tests do.

## Running it

Run the loop from `.claude/skills/review-loop/` with the three charters below — round 1 blind, later
rounds two-phase, findings carried in a ledger, only CRITICAL/MAJOR looping, safety valve at 2 rounds.

All three run `opus` at high effort, in parallel, per `.claude/skills/agent-fleet/`. This is
correctness-critical verification; do not downgrade them to save tokens. Tell each: cite
`path/File.kt:line`, give a concrete failure scenario, "NO FINDINGS" is valid, do not pad, and do NOT
spawn sub-agents.

| Reviewer | Charter |
|---|---|
| **1. Implementation & code style** | Correctness vs the task spec; edge cases; reuse and simplification; test quality — do the e2e tests exercise the real flow, both DB backends? Plus `.claude/skills/code-style/`: explicit imports, no FQCN, no wildcards, resolvable KDoc links, branding, pnpm |
| **2. Domain expert** | Is it correct *for the domain*? Auth/tenancy/org invariants, data-model soundness, API contract fit, consistency with existing funktor patterns. Migration expectations — this repo requires none, so verify nothing silently depends on that. Judge design fit, not lint |
| **3. Security** | Authn/authz gaps, tenant isolation (can org A read org B?), injection, secrets, token/session handling, unsafe deserialization, privilege escalation (`isSuperUser` bypass), input validation, error-message leakage. Assume a hostile authenticated user of another org |

**Scope by risk** (review-loop rule): a doc-only or test-only feature does not need all three.

## Coordinator synthesis — you, in the main loop

Per review-loop's triage, plus one rule this gate leans on hardest:

**Adversarially verify every finding against the code before acting on it, and say when one does not
survive.** Reviewers here have been confidently wrong; so has the coordinator. Where a one-command
experiment settles it, run it. Record what was probed and stayed CLEAN as well as what was found — it
stops the next session re-treading the same ground.

Apply confirmed CRITICAL/HIGH and clear MEDIUM findings; park anything needing a design decision for the
maintainer. Re-run the feature's tests after fixes, and mutation-check anything new on a security-critical or persistence path.

## Security-critical → red-team follow-up

If the feature touches auth, sessions, tenancy, tokens, permissions or any data boundary, create
`.claude/tasks/YYYYMMDD-redteam-<slug>.md` capturing concrete attack scenarios — cross-org access, token
forgery/replay, privilege escalation, IDOR on org/branch ids, session fixation. **Collect only; never
execute them here.** Note the created task in the feature's task file. Extend an existing red-team task
rather than opening a second one for the same surface.

## Output

- Confirmed findings, grouped and severity-ranked, each with `file:line` and its fix.
- Fixes applied, tests re-run, mutations reported.
- The task file's **Review record** updated — including findings that were REJECTED and why, and any
  claim of yours that did not survive. That record is the ledger the next round reconciles against.
- A clear verdict: **gate PASS** (no open CRITICAL/HIGH, e2e present and green) or **gate FAIL** with
  the blocking items named.

## Notes

- This gate is about *this feature's* change set — not a whole-repo audit. Keep reviewers scoped.
- `/code-review` and `/security-review` are the generic single-pass tools. Prefer this for features;
  their findings enter the same loop.

## Changelog

- **2026-08-28** — Loop mechanics extracted to `.claude/skills/review-loop/`, adopted from the Klang
  project. This skill was previously a ONE-SHOT gate, and the insights review of 2026-08-24 showed the
  cost: it found a real live secret disclosure, but the fix that round produced was itself wrong and was
  caught by the maintainer rather than by a second round. It also had no ledger, so a rejected finding
  had nowhere to be recorded as settled.
