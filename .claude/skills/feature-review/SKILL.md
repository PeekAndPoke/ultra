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

Apply confirmed CRITICAL and MAJOR findings; park anything needing a design decision for the
maintainer. **The scale is CRITICAL / MAJOR / MINOR** — the same one review-loop loops on. Do not
introduce HIGH or MEDIUM here: a gate that loops on CRITICAL/MAJOR while a reviewer reports HIGH drops
it silently, and this repo's most-cited gate did report HIGH, for a live session token on a screen. Re-run the feature's tests after fixes, and mutation-check anything new on a security-critical or persistence path.

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
- A clear verdict: **gate PASS** (no open CRITICAL or MAJOR, e2e present and green) or **gate FAIL**
  with the blocking items named.

## On PASS — finishing the lifecycle

The gate is not done when the verdict is written. CLAUDE.md's lifecycle continues, and these steps are
skipped often enough to be worth naming here:

- [ ] Mark the task **DONE** and move it to `.claude/tasks-archive/<YYYY-MM>/` (the filename is dated).
- [ ] **Create the follow-up DOCS task if the change touched public API** — unless the code is still in
      flux, in which case say so in the archive note. Docs are written against SETTLED code.
- [ ] Create or extend the red-team task if the feature is security-critical.

## Notes

- **Scoped means START from the change set, then follow it into the code it depends on** — it does not
  mean stay inside the diff. In the insights gate the CRITICAL and both worst findings were OUTSIDE the
  diff, in the collector and the drivers the changed files read from; a reviewer who stayed in the `.vue`
  files would have found none of them. It is still not a whole-repo audit.
- **A confirmed finding that is real but out of scope to fix here gets its own disposition:**
  `out-of-scope → follow-up task`. Not "reject", which asserts it is wrong, and not silence. **The gate
  FAILS until the follow-up task exists**, and the task is linked from the Review record.
- `/code-review` and `/security-review` are the generic single-pass tools. Prefer this for features;
  their findings enter the same loop.

## Changelog

- **2026-08-28** — Loop mechanics extracted to `.claude/skills/review-loop/`, adopted from the Klang
  project. This skill was previously a ONE-SHOT gate. The insights review of 2026-08-24 shows the cost:
  it confirmed a CRITICAL it could not fix and had nowhere to put it, left three findings
  confirmed-and-open with no re-review path, and a premise the feature rested on had to be retracted
  mid-session. (An earlier draft of this note claimed the round produced a wrong fix that the maintainer
  caught — that was a misdescription; the round labelled its own output a mitigation and failed the
  gate. Corrected by the self-review.)
