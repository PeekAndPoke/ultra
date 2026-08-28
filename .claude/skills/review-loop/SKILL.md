---
name: review-loop
description: Use when reviewing code changes, running a code review, applying review findings, or checking that a test can actually fail (mutation check). Codifies the project review standard — reviews loop until a clean round, fixes get re-reviewed, and every new test is mutation-checked.
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

The second standard, **mutation-checked tests**, is here because a review-loop fix almost always ships
a test, and a green test proves nothing until it has been RED for the right reason.

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
   - *Phase 1 — review.* The full current diff, the constraints, and the fix delta marked as the
     primary target — and deliberately NOT the previous round's findings. Fresh eyes on the current
     state.
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
3. **Triage every finding into the LEDGER** — one line per finding, carried verbatim into every later
   round: severity, `path:line`, the claim, and its disposition. The ledger is what phase 2 reconciles
   against, and it is the artifact that makes "settled" mean something. Each entry is exactly one of:
   - **fix** — apply it. **Only CRITICAL and MAJOR feed the loop.** MINORs are collected and either
     applied once as a single batch with no re-review, or handed to the maintainer as a list;
   - **reject** — with a stated reason. A rejection on project philosophy must name the rule;
   - **maintainer-decision** — park it (design fork, tradeoff, anything needing product knowledge).
4. **Verify each finding against the code before acting on it.** Reviewers here have been confidently
   wrong, and so has the coordinator — say so when one does not survive. Where a one-command
   experiment settles it, run it.
5. **Apply the fixes**, run the affected tests, mutation-check anything new (Standard 2).
6. **If a CRITICAL/MAJOR fix was applied → go to 2.** MINOR-only rounds do not loop.

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
- **Never silently drop a finding.** Every one ends as fix / reject+reason / maintainer-decision.
- **A finding about a MISSING e2e test is a gate failure, not a debatable nit** — see CLAUDE.md.
- **Final report** lists rounds run, each round's findings and outcomes, parked decisions on top.

### The three charters

Reviewers run `opus` at high effort — correctness-critical verification, the tier
`.claude/skills/agent-fleet/` assigns to hard verification. Do not downgrade them to save tokens.

Give every reviewer: the change set, the task file, the plan link, its charter, and these constraints —
findings must cite `path/File.kt:line` and give a concrete failure scenario; "NO FINDINGS" is a valid
answer; do not pad; do NOT spawn sub-agents.

1. **Implementation & code style** — correctness vs the task spec, edge cases, reuse and
   simplification, test quality, and `.claude/skills/code-style/` (explicit imports, no FQCN, no
   wildcards, resolvable KDoc links, branding, pnpm).
2. **Domain expert** — is it correct *for the domain*? Auth/tenancy/org invariants, data-model
   soundness, API contract fit, consistency with existing funktor patterns. Judge design fit, not lint.
3. **Security** — authn/authz gaps, tenant isolation (can org A read org B?), injection, secrets,
   token/session handling, unsafe deserialization, privilege escalation, input validation, error
   leakage. Assume a hostile authenticated user of another org.

---

## Standard 2 — Mutation-check every new test

A green test proves nothing until it has been RED for the right reason.

### Protocol (per new test)

1. **GREEN** — write it, run it, confirm it passes.
2. **MUTATE** — introduce ONE targeted mutation that should break the behaviour under test.
   **Prefer mutating the code under test** (flip an operator, off-by-one a constant, drop a term,
   swap a branch); mutating the test's inputs is the fallback.
3. **RED** — run it. It MUST fail. Still green → the test is toothless; fix it and repeat from 1.
4. **RESTORE** — revert exactly, run again, green. **Back up with `cp` first, and NEVER restore with
   `git checkout`** — the file usually carries other uncommitted work, and on this worktree possibly
   another agent's. Verify with `git diff` that only the intended change remains.
5. **REPORT** one line per test: `mutation-checked: <what was mutated> → red`.

**A surviving mutant is not always a missing test.** It can mean the FIX was wrong, the fixture cannot
express the difference, or a comment claimed something false. Work out which before adding an assertion
to make it die.

### Scope

Mandatory for new specs, regression guards, and tests written as review-loop fixes. Not a retrofit
mandate — mutation-check older tests opportunistically when a change touches them.

### Why this exists

This repo has shipped toothless guards repeatedly:

- A "single-use token" e2e that a cooldown also satisfied.
- A `validate()` test that built the healthy object, so it passed whether or not validation ran.
- A coverage guard comparing two hardcoded lists in the same file — a seventh type would appear in
  neither. Two reviewers found it independently.
- `JwtSignatureGateSpec` was flaky 1-in-4 because it built alternates from a char range instead of the
  base64 alphabet. It passed a three-reviewer gate and most runs.

**And the newest one, which is the sharpest.** `VaultCollector.Data` held a type Slumber had no codec
for, so the slice threw inside a post-response coroutine and silently dropped the WHOLE record — across
1193 depot records, `vault.entries` was `[]` in every one. The collector had tests. The API had tests.
The one operation the data actually undergoes in production had none. **If a type is serialized in
production, a test must serialize it.**

---

## Gotchas — this repo specifically

- **Take `.claude/BUILD-LOCK.md` before building.** Reviewers are read-only and need no lock; applying
  fixes and running tests do. Read the lock as its OWN step and act on what it says — chaining the read
  into the command with `&&` is not a check.
- **`git diff <path>` immediately before committing that path.** `git commit -- <path>` commits the
  WORKING TREE, not the index, so it takes another agent's edits to that file too. Checking
  `git diff --cached` does not cover this — that mistake swept a lock claim on 2026-08-24.
- **kotest ignores `--tests`.** Confirm a spec ran via `build/test-results/**/TEST-*.xml`, never by
  trusting a filtered gradle invocation. MPP modules use `:jvmTest`; `funktor-demo:server` uses `:test`.
- **…but that XML mis-attributes WHICH test failed.** Counts are reliable; the `<testcase name>` a
  failure nests under is not. Use the XML for counts, the console for which case broke.
- **Module test tasks do not compile everything.** Before claiming a cross-module change is contained:
  `./gradlew compileKotlinJvm compileTestKotlinJvm compileKotlinJs compileTestKotlinJs compileKotlin
  compileTestKotlin --continue` and grep `^e:`.
- **A green compile sweep can be measured against STALE test classes**, most likely after a concurrent
  build. Recovery: `rm -rf <module>/build/classes/kotlin/**/test` and re-run.
- **`shouldBe` is untyped**, so `valueClass shouldBe "literal"` rots silently.
- **A green FIRST run on a new code path is the signal to go check the fixture**, not to move on. It has
  meant a vacuous test four times on the codegen work alone.
- **`vue-tsc` cannot catch a missing `.vue` file** — `shims-vue.d.ts` declares `module '*.vue'`, which
  resolves any specifier whether the file exists or not. Only `vite build` catches it.
- Local DBs: `docker start mongodb arangodb`.

## Changelog

- **2026-08-28** — Adopted from `klang/.claude/skills/review-loop/`, which had outgrown this repo's
  one-shot gate. Kept wholesale: the loop, the two-phase later rounds, CRITICAL/MAJOR-only looping,
  the prose-churn rule, the 2-round safety valve, and the mutation protocol. Replaced: klang's two
  reviewers with this repo's three charters, and its gotchas with ours.

  **The local evidence that this was needed** is the insights gate of 2026-08-24. It ran ONE round and
  found a real live secret disclosure — but the fix that round produced was a language allowlist in the
  frontend, which was *itself wrong*, and the maintainer caught it, not a review. A second round is
  exactly what that case wanted. The same gate also shows the loop's failure mode already present here:
  two of my own claims across the session were confidently wrong and had to be withdrawn, which is why
  step 4 (verify before acting) is not optional.
