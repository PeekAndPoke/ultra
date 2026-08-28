---
name: review-loop
description: Use when reviewing code changes, running a code review, applying review findings, or checking that a test can actually fail (mutation check). Codifies the project review standard — reviews loop until a clean round without flip-flopping on settled findings, and tests on security-critical or persistence paths are mutation-checked.
---

## What This Skill Does

**Adopted from the Klang project 2026-08-28.** The loop mechanics and the two-phase shape are theirs;
the charters, gotchas and evidence are this repo's.

### The problem it solves

A fix produced by a review is itself an unreviewed change, so one round is never enough. But naive
looping is worse than no looping, because it **flip-flops**: round 3 re-raises what round 2 rejected,
round 4 critiques the prose round 3's fix just wrote, and the loop feeds on its own output. Klang ran
five rounds on one change and rounds 3-5 were almost entirely churn.

**So the loop is built around one property: a settled finding stays settled unless someone can say what
is factually wrong with the reason it was settled.** Everything below serves that —

| Mechanism | Stops |
|---|---|
| **Two-phase rounds** — review blind, THEN reconcile against the ledger | Re-litigating a rejection while still getting fresh eyes |
| **A findings ledger** carried between rounds, every entry dispositioned | Silently dropping, or silently re-raising |
| **Reopening requires naming the factual error in the rejection** | "I still think so" restarting a settled argument |
| **Only CRITICAL/MAJOR loop** | MINOR churn triggering another round |
| **Comment findings only when factually wrong** | Prose churn — the loop's documented failure mode |
| **Safety valve at 2 rounds** | Fix-churn feeding itself indefinitely |

The second standard, **mutation-checked tests on security-critical and persistence paths**, is here because a
review-loop fix almost always ships a test, and a green test proves nothing until it has been RED for
the right reason. **Scope is narrower here than in klang** — see Standard 2.

Applies whenever you review changes or write tests, including when `/code-review` or `/simplify`
produce findings — their output enters this loop at step 3. `/feature-review` is this loop run with
the mandatory three-charter gate; see that skill for the gate-specific parts (e2e precondition,
red-team follow-up, task-file record).

---

## Standard 1 — The Review Loop

### The loop

1. **Collect the change set.** State the base explicitly — usually the branch point off `master`.
   On a shared worktree, scope by FILE SET rather than a commit range: another agent's commits will
   be interleaved with yours.
2. **Review round** — spawn **FRESH** reviewer agents in parallel, per `.claude/skills/agent-fleet/`.
   **Round 1 is BLIND**: the task, the change set, the constraints — nothing else.
   **Every later round runs in TWO PHASES with the same fresh agent** — review FIRST, previous
   results AFTER:
   - *Phase 1 — review.* The full current diff and the constraints, WITHOUT the previous round's
     findings. Do not flag which hunks are new: the fix delta is an answer key to the last round, and
     pointing at it both anchors the agent on what it is meant not to see and pulls attention into
     re-critiquing freshly written lines — the prose-churn mode below.
   - *Phase 2 — reconcile.* Send that SAME agent the previous findings verbatim plus each one's
     triage. For any phase-1 finding that overlaps a settled one, it must either WITHDRAW it or STICK
     TO IT by naming what is factually wrong in the rejection reason. It also states, per
     previously-FIXED finding, whether the fix actually holds in the current diff.

   Fresh-eyes value and settled-stays-settled, without anchoring the review itself.

   **The reconcile rule, stated exactly, because it is the whole point:** a finding that overlaps a
   settled one may be reopened ONLY by naming what is factually wrong in the rejection reason —
   a mis-read line, a since-changed file, a wrong claim about behaviour. Re-asserting the finding, or
   disagreeing with the judgement, is a WITHDRAWAL. Reviewers do not get a second vote on a call the
   coordinator already made on the facts; they get a chance to show the facts were wrong.
3. **Triage every finding into the LEDGER**, and **write the ledger to the task file BEFORE starting
   the next round** — it is an input to round N+1, not a report written afterwards. One row per finding:

   | Round | Sev | `path:line` | Claim | Disposition | Reason |

   **The Reason column is the mechanism, not bookkeeping.** Phase 2 asks a reviewer to name what is
   factually wrong in a rejection; a reviewer who cannot see the reason cannot do that, and per the
   reconcile rule its re-raise counts as a withdrawal — so an empty ledger silences correct findings
   automatically. `.claude/tasks/TEMPLATE.md` carries this table. Each entry is exactly one of:
   - **fix** — apply it. **Only CRITICAL and MAJOR feed the loop.** MINORs are collected and either
     applied once as a single batch with no re-review, or handed to the maintainer as a list;
   - **reject** — with a reason that is CHECKABLE: a named project rule, or a fact someone can verify
     (`X.kt:42 already scopes by org`). **An unfalsifiable rejection is not a rejection.** "Acceptable
     in practice", "fine for an internal tool", "unlikely" assert nothing a reviewer can refute, and the
     vaguer the reason the more unassailable it becomes — the incentive runs exactly backwards. A
     rejection you cannot make checkable is a **maintainer-decision**;
   - **maintainer-decision** — park it (design fork, tradeoff, anything needing product knowledge).

   **You may not reject a CRITICAL/MAJOR raised against code you wrote yourself.** It goes to
   maintainer-decision. Self-rejection is the cheapest way to make a correct finding disappear.
4. **Verify each finding against the code before acting on it.** Reviewers here have been confidently
   wrong, and so has the coordinator — say so when one does not survive. Where a one-command
   experiment settles it, run it.
5. **Apply the fixes**, run the affected tests, and mutation-check anything new that touches a
   security-critical **or persistence** path (Standard 2).
6. **If a CRITICAL/MAJOR was FIXED *or REJECTED* → go to 2.** Both, and the rejection case is the one
   that matters: if only fixes looped, a rejection would end the review, and the reconcile phase — the
   sole mechanism by which a reviewer can show a rejection was wrong — would never run against it.
   Rejecting would be strictly cheaper than fixing, and a coordinator could close a gate in one round on
   their own say-so. When a round produced only rejections, round 2 may be **reconcile-only**: a fresh
   agent receives each finding with its rejection reason and must accept or refute it. MINOR-only rounds
   do not loop.

### Termination — the loop stops ONLY on

- **Clean round** — zero CRITICAL/MAJOR → done. Remaining MINORs go to the maintainer as a batch.
- **Maintainer decision needed** — STOP, present the parked decisions crisply, wait. Do not guess.
- **Wall** — a finding oscillates, reviewers contradict each other, or a fix is impossible without
  breaking something else → STOP and present the state honestly.
- **Safety valve — 2 rounds** without a clean round: STOP and consult the maintainer with the open
  findings. (Klang cut this from 5 to 2 on 2026-08-28: five rounds let fix-churn feed itself.)

### Rules

- **Fresh agents every round.** A reviewer that saw round N is anchored for N+1. Fresh AGENT, informed
  PROMPT — the round context of step 2 travels to the new agent.
- **Comment/KDoc findings only when the text is factually WRONG** — never for completeness or style.
  Prose churn is this loop's documented failure mode: each round's fixes write new text, the next
  round critiques it, and the loop feeds itself.
- **Scope by risk.** The full charter set is for production code. Test-only or doc-only portions get
  one reviewer or none — mutation checks guard tests harder than a reviewer can.
- **One severity scale, everywhere: CRITICAL / MAJOR / MINOR.** Not HIGH, not MEDIUM. A gate that
  loops on "CRITICAL/MAJOR" while a reviewer reports "HIGH" silently drops it — and the repo's own
  most-cited gate did report HIGH, for a live session token on screen.
- **Never silently drop a finding.** Every one ends as fix / reject+reason / maintainer-decision.
- **A finding about a MISSING e2e test is a gate failure, not a debatable nit** — see CLAUDE.md.
- **Final report** lists rounds run, each round's findings and outcomes, parked decisions on top.

### The reviewer charters

**Defined in `.claude/skills/feature-review/`, and only there.** They were duplicated here at first and
the two copies had already drifted apart within a day — this repo's own "Docs vs skills" rule warns
about exactly that, and the review skills are not exempt from it.

Reviewers run `opus` at high effort per `.claude/skills/agent-fleet/` — correctness-critical
verification. Do not downgrade them to save tokens.

Give every reviewer: the change set, the task file, the plan link, its charter, and these constraints —
findings must use the severity scale **CRITICAL / MAJOR / MINOR**; cite `path/File.kt:line`; give a
concrete failure scenario; "NO FINDINGS" is a valid answer; do not pad; do NOT spawn sub-agents.

---

## Standard 2 — Mutation-check tests on security-critical and persistence paths

A green test proves nothing until it has been RED for the right reason.

**Scope, and it differs from klang deliberately** (maintainer, 2026-08-28). Klang mutation-checks every
new test; here it is mandatory for two families:

1. **Security-critical** — auth, sessions, tokens, permissions, tenancy, anything guarding a data
   boundary.
2. **Persistence** — storage, serialization and codecs, migrations, indexes, query building. Anything
   whose output ends up on disk or comes back off it.

Plus regression guards and review-loop fixes on either. Everywhere else it is encouraged, not required.
Do not quietly widen this back to "every test": the narrower scope is a maintainer decision, not an
oversight in the adoption.

**Why persistence sits beside security rather than below it:** both fail the same way — silently, with
the damage already done by the time anyone looks. A wrong authz check leaks data that was already
readable; a wrong codec loses data that is already gone. Neither announces itself, which is exactly the
condition under which a green test is worth nothing.

### Protocol (per new test)

1. **GREEN** — write it, run it, confirm it passes.
2. **MUTATE** — introduce ONE targeted mutation that should break the behaviour under test.
   **Prefer mutating the code under test** (flip an operator, off-by-one a constant, drop a term,
   swap a branch); mutating the test's inputs is the fallback.
3. **RED** — run it. It MUST fail, **and you must confirm the failure is the test you mutated for.**
   This step is the loop's strongest instrument and the easiest to fool: kotest ignores `--tests`, so a
   filtered run may execute something else entirely, and the XML mis-attributes which case failed. Read
   the CONSOLE for which case broke and the XML counts to confirm the spec ran at all. A pre-existing
   flake counts as a false RED — `JwtSignatureGateSpec` was flaky 1-in-4 and would certify a toothless
   test as checked. Still green → the test is toothless; fix it and repeat from 1.
4. **RESTORE** — revert exactly, run again, green. **Back up with `cp` first, and NEVER restore with
   `git checkout`** — the file usually carries other uncommitted work, and on this worktree possibly
   another agent's. Verify with `git diff` that only the intended change remains.
5. **REPORT** one line per test: `mutation-checked: <what was mutated> → red`.

**A surviving mutant is not always a missing test.** It can mean the FIX was wrong, the fixture cannot
express the difference, or a comment claimed something false. Work out which before adding an assertion
to make it die.

### Not a retrofit mandate

Mutation-check older tests opportunistically, when a change touches them.

### Why this exists

This repo has shipped toothless guards repeatedly:

- A "single-use token" e2e that a cooldown also satisfied.
- A `validate()` test that built the healthy object, so it passed whether or not validation ran.
- A coverage guard comparing two hardcoded lists in the same file — a seventh type would appear in
  neither. Two reviewers found it independently.
- `JwtSignatureGateSpec` was flaky 1-in-4 because it built alternates from a char range instead of the
  base64 alphabet. It passed a three-reviewer gate and most runs.

**And a related one that mutation checking would NOT have caught**, kept here because it is the
neighbouring trap: `VaultCollector.Data` held a type Slumber had no codec for, so the slice threw in a
post-response coroutine and silently dropped the WHOLE record — `vault.entries` was `[]` across all 1193
depot records. No mutation would have found that, because no test exercised the path at all. The rule it
produced is a coverage rule and lives in CLAUDE.md: **if a type is serialized in production, a test must
serialize it.**

---

## Gotchas — the ones specific to reviewing

**CLAUDE.md's "Verification traps" section is canonical and auto-loaded; it is not repeated here.** Read
it. The two below are about the review process itself rather than about testing, which is why they live
in this file:

- **Take `.claude/BUILD-LOCK.md` before building.** Reviewers are read-only and need no lock; applying
  fixes and running tests do. Read the lock as its OWN step and act on what it says — chaining the read
  into the command with `&&` is not a check.
- **`git diff <path>` immediately before committing that path.** `git commit -- <path>` commits the
  WORKING TREE, not the index, so it takes another agent's edits to that file too. Checking
  `git diff --cached` does not cover this — that mistake swept a lock claim on 2026-08-24.

## Changelog

- **2026-08-28** — Adopted from `klang/.claude/skills/review-loop/`, which had outgrown this repo's
  one-shot gate. Kept wholesale: the loop, the two-phase later rounds, CRITICAL/MAJOR-only looping,
  the prose-churn rule, the 2-round safety valve, and the mutation protocol. Replaced: klang's two
  reviewers with this repo's three charters, and its gotchas with ours.

  **The local evidence that this was needed, restated 2026-08-28 after the self-review caught me
  misdescribing it.** I first wrote that the insights gate of 2026-08-24 produced a wrong fix that "the
  maintainer caught, not a review". **That is false, and the record says so.** The round produced a
  mitigation, *labelled it* "a mitigation, not the fix", named the real backend fix, put it on the
  blocking list and FAILED the gate
  (`.claude/tasks/20260802-insights-vue-tabs.md:487-500`). The maintainer's contribution was a better
  DESIGN — enforce the invariant at the writer instead of the reader — not a catch the review missed.

  What that gate actually shows, which argues for the loop without needing the misattribution:

  - It confirmed a **CRITICAL it could not fix** (`VaultCollector.Data` unslumberable, dropping every
    record) and had **nowhere to put it**: no fix, so nothing looped; "reject" would have been wrong.
    That gap is now finding #11's disposition below.
  - Three findings finished CONFIRMED-and-open with no re-review path.
  - A premise the whole feature rested on had to be **retracted mid-session** — the tab's `vars`
    suppression was believed to contain the exposure and did not. A second round is what tests a claim
    like that.

  **The lesson I nearly taught instead is the sharper one:** a process document that cites a false
  precedent teaches reviewers to distrust an accurate record. Verify the evidence in your own changelog
  against the record, exactly as you would a reviewer's finding.
