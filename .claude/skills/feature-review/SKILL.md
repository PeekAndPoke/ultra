---
name: feature-review
description: Use after implementing a feature to run the mandatory multi-agent review gate — implementation & code style, domain expert, and security. Also use when someone says "feature-review", "review this feature", "run the review gate", or finishes a task file in .claude/tasks/.
argument-hint: [task-file-or-feature-description]
---

## What This Skill Does

Runs the project's mandatory review gate on a freshly implemented feature: three independent
sub-agent reviews (implementation & code style, domain expert, security), then coordinator
synthesis of confirmed findings. It is the `→ review →` step of the development workflow in
`CLAUDE.md`. Run it before a task can move to DONE.

> **Status: PROVISIONAL.** Adjust reviewer prompts/models as we learn what catches real issues.

## When to run

- After implementing any feature (a task file in `.claude/tasks/`), before marking it DONE.
- The change should be committed or at least staged so the diff is well-defined.

## Preconditions (check first)

1. Identify the diff under review: `git diff <base>...HEAD` or the working-tree diff. State the
   base explicitly (usually the branch point off `master`).
2. Confirm the feature's own tests pass and are included in the diff. If backend code has **no
   end-to-end tests** (`AppSpec`/`AppUnderTest`, both DB backends where storage is involved), stop
   and flag it — that is a gate failure per `CLAUDE.md`, not a review finding to debate.
3. Load the task file and its linked plan for domain context to hand the reviewers.

## The three reviews

Launch as a fleet per `.claude/skills/agent-fleet/`. All three are read-only analysis over the same
diff and run **in parallel**. Give each reviewer: the diff, the task file, the plan link, and its
specific charter. Require every finding to cite `path/File.kt:line` and include a concrete
failure/impact scenario — reject vague findings.

Model/effort per the agent-fleet skill:

| Reviewer | agentType/model | effort | Charter |
|---|---|---|---|
| 1. Implementation & code style | general-purpose, `opus` | high | Correctness vs. the task spec; edge cases; the repo's Kotlin style (`.claude/skills/code-style/`: explicit imports, no FQCN, no wildcards, branding, pnpm); reuse/simplification; test quality — do the e2e tests actually exercise the real flow, both DB backends? |
| 2. Domain expert | general-purpose, `opus` | high | Is this correct **for the domain**? Auth/tenancy/orgs invariants, data-model soundness, API contract fit, consistency with existing funktor patterns, migration/back-compat expectations (this repo: none required — verify nothing silently depends on that). Judge design fit, not lint. |
| 3. Security | general-purpose, `opus` | high | AuthZ/authN gaps, tenant/org isolation (can org A read org B?), injection, secrets, token/session handling, unsafe deserialization, privilege escalation (`isSuperUser` bypass), input validation, error-message leakage. Assume a hostile authenticated user of another org. |

Use `opus` for all three: this is correctness-critical verification, the tier the agent-fleet skill
assigns to hard verification. Do not downgrade reviewers to save tokens.

Prompt each reviewer to return structured findings: `severity` (CRITICAL/HIGH/MEDIUM/LOW), `file:line`,
one-line claim, failure scenario, and suggested fix direction. Tell them to return an empty list
rather than invent low-value nits.

## Coordinator synthesis (you, in the main loop)

1. Collect all three reports. **Adversarially verify** each finding against the actual code before
   accepting it — do not forward a reviewer's claim you haven't confirmed. Drop the unconfirmed.
2. Deduplicate across reviewers; keep the most severe framing.
3. Present confirmed findings grouped by review, most-severe first, each with file:line + fix.
4. Apply fixes for confirmed CRITICAL/HIGH (and clear MEDIUM) findings, or list them for the user
   if they involve a design decision. Re-run the feature's tests after fixes.
5. Record the outcome in the task file's **Review record** table.

## Security-critical → red-team follow-up

If the feature is security-critical (auth, sessions, tenancy/org isolation, tokens, permissions,
anything guarding data boundaries), create a follow-up task
`.claude/tasks/YYYYMMDD-redteam-<slug>.md` capturing concrete attack scenarios to attempt later
(cross-org data access, token forgery/replay, privilege escalation, IDOR on org/branch ids,
session fixation, etc.). **Collect only — do not execute attacks here.** Dedicated penetration-test
sessions run these later. Note the created red-team task in the feature task file.

## Output

- Confirmed-findings summary (grouped, severity-ranked, file:line + fix).
- Fixes applied + test result.
- Task file updated (Review record table; red-team task linked if created).
- A clear verdict: **gate PASS** (no open CRITICAL/HIGH, e2e tests present & green) or **gate FAIL**
  with the blocking items.

## Notes

- This gate is about *this feature's* diff — not a whole-repo audit. Keep reviewers scoped.
- Missing e2e backend tests fail the gate outright; don't let a reviewer "note" it as optional.
- `/code-review` and `/security-review` are the generic single-pass tools; this skill is the
  structured 3-reviewer gate with domain context and the red-team hand-off. Prefer this for features.
