---
name: agent-fleet
description: Use when spawning sub-agents, launching multiple agents in parallel, fanning out tasks with the Agent tool, or writing Workflow scripts. Defines how to pick model and effort per sub-agent to balance result quality against token spend.
---

## What This Skill Does

> **Status: PROVISIONAL.** This policy is new and expected to evolve. When a fleet run shows a
> mapping is wrong (a tier too weak for the task, or wastefully strong), propose a concrete
> adjustment to this file and apply it once the user approves. Log accepted changes in the
> changelog at the bottom. **This copy is the upstream of klang's**
> (`/opt/dev/peekandpoke/klang/.claude/skills/agent-fleet/SKILL.md`), which has since grown its own
> rules — when a general rule changes on either side, carry it across.

Defines how to assign models and effort levels when distributing work across sub-agents. Applies
to every Agent tool call and every Workflow `agent()` call. Invoke it (mentally or via
`/agent-fleet`) before launching any multi-agent fan-out.

**Skills that fan out and are governed by this one:** `/review-loop` and `/feature-review` (up to
three fresh reviewers per round, looped), `/six-hats` (concurrent perspective agents).

## Core principle

The main loop is the **coordinator**. It runs on the user-selected model and effort — never
downgrade it, and keep final synthesis and judgment in the coordinator rather than delegating
them to a cheap agent. Sub-agents are **workers**: each gets the cheapest model/effort tier that
can do its task well.

## Model tiers

| Task type | Model | Examples |
|---|---|---|
| Coding, implementation, debugging, hard verification | `opus` | write/refactor code, fix a failing test, adversarially verify a subtle correctness claim |
| Info retrieval, exploration, research, summarization | `sonnet` | find usages, map a subsystem, web research, summarize a diff or doc |
| Trivial mechanical work | `haiku` | extract/list/count, format conversion, high-volume simple scans |
| Frontier reasoning | inherit (omit `model`) | architecture judgment, deep cross-cutting analysis the coordinator can't decompose further |

**Review rounds do not use this table** — they follow the effort ladder in `/review-loop`
(`reviewer-high`, then `reviewer-xhigh`, then `reviewer-max`). Security and domain review is
correctness-critical: never `sonnet`.

## Effort tiers

| Stage                         | Effort                                                        |
|-------------------------------|---------------------------------------------------------------|
| Mechanical / bulk stages      | `low`                                                         |
| Standard work                 | omit (inherit session effort)                                 |
| Hardest verify / judge stages | `high` or `xhigh`                                             |
| Review rounds 1, 2, 3+        | `high`, `xhigh`, `max` via the `reviewer-*` agent definitions |

## Where the dials live

- **Agent tool**: set the `model` parameter per call. There is no per-call effort override —
  effort comes from the agent definition. `fork`-type agents always inherit the parent model;
  don't set `model` on them. Effort for reviewers is pinned in `.claude/agents/reviewer-high.md`,
  `reviewer-xhigh.md` and `reviewer-max.md`.
- **Workflow `agent()`**: set both `model` and `effort` in the opts, per stage.
- **Custom agents** (`.claude/agents/*.md`): can pin model/effort in frontmatter; prefer that
  for agents whose task type never varies.
- **Agent type**: prefer `Explore` for read-only search fan-out (it reads excerpts, not whole
  files — cheaper, and it cannot edit). Use `general-purpose` when the worker must run commands or
  write files.

## Decision procedure

1. Before launching, classify each sub-task into a tier using the tables above.
2. Set `model` (and `effort`, where available) explicitly on every call — don't let a whole
   fleet silently inherit the session model.
3. When launching a fleet, state the mapping in one line (e.g. "3 Sonnet finders + 1 Opus
   verifier") so the user can correct it — that feedback is how this provisional policy improves.
4. Decide **who owns the build** before launching (below), and say it in the same line.

## The build is a single-writer resource

Two failure modes, and the second is the expensive one:

1. **Concurrent Gradle runs corrupt incremental state.** CLAUDE.md's verification traps record the
   ultra case: a compile sweep reported clean against stale test classes while another agent built
   the same worktree, and it surfaced only as an `AbstractMethodError` at runtime.
2. **A build that races an edit produces a wrong answer.** If one agent restores a mutation while
   another runs a test, the second agent's verdict is about code it never chose — and a green that
   should have been red looks exactly like a toothless test. No error is raised.

The lock here is **advisory**: `.claude/BUILD-LOCK.md`, read as its own step (never chained into the
build with `&&`). Klang found advisory alone was not enough for mutation-heavy fan-outs and added a
real `flock` wrapper (`klang/console/with-build-lock.sh`); port it if ultra starts running those.

- **Default: the coordinator owns the build.** Workers read, analyse and propose; the coordinator
  builds once, afterwards. This is the right shape for review and analysis fan-outs.
- **Say it in the worker prompt**: *"Do NOT run Gradle or any build command. Report what should be
  run; the coordinator runs it."*
- **Only ONE owner mutates production code, ever.** Two workers mutating the same module read each
  other's edits and both draw wrong verdicts. For a mutation check the critical section is
  `mutate → build → restore`, not just the build.
- If a worker genuinely must build, give exactly one worker that permission, and have it take the
  lock.

## Rules of thumb

- **Fan-out multiplies cost.** For large sweeps (~10+ agents), use cheap finders
  (`sonnet`/`haiku`) feeding a narrow, expensive verify stage (`opus`, high effort) — not an
  expensive model on every item.
- **When unsure between tiers:** tier up for correctness-critical work, tier down for
  volume/coverage work.
- **Escalate, don't accept.** If a cheap agent returns a weak or suspect result, re-run that one
  task a tier up instead of patching around bad output.
- **Don't use `haiku`** for anything whose output the coordinator can't cheaply sanity-check.
- **Give workers the constraints, not just the task.** This repo carries deliberate decisions a
  fresh agent would "fix": the narrower mutation scope, no migrations required, Kontainer scoping,
  Mutator not being battle-tested, the CLAUDE.md verification traps. A reviewer without them files
  findings that make things worse. Paste the relevant ones into the prompt, and the charter from
  `/feature-review` for reviewers.

## Concurrency & fan-out safety

Cost isn't the only failure mode — in one run, several heavy agents launched at once **stalled or
disconnected mid-response**, and a failed agent writes nothing. **Causation is not proven:** the
observed run also happened over a flaky network connection, which could equally explain the
stalls. So treat the limits below as prudent defaults, not hard evidence-backed ceilings — they
cost little and remove one variable. If future runs show heavy concurrency is reliable, **broaden
it back in small increments** (e.g. 2–3 → 4 → 5 heavy agents) and note what held. Two rules:

- **Workers must not fan out.** A sub-agent that spawns its own sub-fleet compounds load
  invisibly and, if it dies, orphans its children — their finished results are discarded with the
  parent. When a worker's task might tempt it to delegate, tell it explicitly in the prompt:
  *"Do NOT spawn sub-agents. Work sequentially yourself; read files in small batches."* Keep
  fan-out one level deep: the coordinator fans out, workers do not.
- **Cap concurrent heavy-tier agents at ~2–3.** `opus`/inherit-tier agents are the ones that
  stall under simultaneous load. `sonnet`/`haiku` workers parallelise fine (run 5–6+). If a phase
  needs many heavy agents, batch them or run the heaviest synchronously
  (`run_in_background: false`) so a stall surfaces immediately instead of after a 10-min watchdog.
- **On failure, retry the one agent** — synchronously, sub-fan-out forbidden — rather than
  relaunching the whole fleet. Check what already landed on disk first; partial work may survive.
- **No two workers edit the same file.** Partition by file, or use `isolation: "worktree"`.

## Every brief opens with the bar (adopted from klang 2026-09-28, a theory under observation)

Every agent in the fleet, whatever its role, is told in the first line of its brief that it is
world-class at that role, and right after it the other half of the bar from the top of `CLAUDE.md`:
mistakes are fine, a defect found is a good day, hiding or brushing over one is what would hurt. Then
the constraints and the task. Where it helps, state the bar for the work itself too ("the e2e must run
against both DB backends", "only CRITICAL and MAJOR force a round, so be precise about severity").

**Why (klang's maintainer):** the training data holds work of every quality, and an agent not told
otherwise reaches for the middle of it — the ordinary fix, the test that restates the implementation,
the review that files whitespace. The second half matters as much: an agent told it is world-class
must also be told that reporting a weakness in its own work is part of that, or the line invites
arrogance.

**Status: a theory.** It might tip an agent into arrogance, or do nothing (every sub-agent already reads
`CLAUDE.md`). Signs it went wrong: a finding dismissed without a scenario, a mutation check reported
as "obviously red", a recorded decision "corrected", a brief's scope widened because the agent knew
better. Signs it worked: unsettled decisions named, weak tests called weak by their own author, an
asymmetry reported rather than papered over. Klang's first observations (2026-09-18, nine reports):
no arrogance signal, candid self-reports. The numbers live in
[`defect-density-ledger.md`](defect-density-ledger.md); record observations in the changelog, dated.
If the density does not move, the line goes and `CLAUDE.md`'s opening stays the one place the bar is
stated.

## Notes

- This skill governs model/effort selection and fan-out safety only. Whether to fan out at all is
  governed by the Agent/Workflow tool rules (e.g. workflows require explicit user opt-in).
- Quality is the goal; cheap tiers are a means to afford more coverage, not an end. A wrong
  answer from Haiku is more expensive than a right answer from Opus.

## Changelog

- **2026-07-17** — Initial version. Opus↔coding and Sonnet↔retrieval mapping set by the user.
  Haiku/inherit tiers, effort table, escalation rule, and cheap-finders-expensive-verifier
  pattern proposed by Claude; not yet validated in practice.
- **2026-07-17** — First validation run (SaaS-foundation deep scan): 3 Sonnet scouts (module map,
  building-block inventory, doc extraction) + 1 Opus deep-diver (auth/tenancy analysis) +
  coordinator synthesis. Mapping held: Sonnet inventories were sufficient; the Opus deep-dive
  earned its tier (found the realm≠tenant distinction and JWT-schema refactor risks). No
  adjustments needed. Note: `thebizz/CLAUDE.md` carries its own copy of this rule — keep the two
  in sync when either changes.
- **2026-07-20** — Fan-out safety rules added (see "Concurrency & fan-out safety"). Trigger: a
  task-file deepening run launched 6 workers at once, one of them (`inherit`-tier) spawned its own
  sub-fleet; 4 of the ~8 total agents stalled/disconnected and wrote nothing, including two orphaned
  grandchildren whose completed reports were discarded with their dead parent. Recovery (one
  synchronous retry with sub-fan-out forbidden) worked cleanly. **Caveat — not a proven failure
  mode:** the run also happened over a flaky network connection, which could have caused the stalls
  independent of concurrency; the sample is one run. The limits are cheap-insurance defaults, not a
  demonstrated ceiling — deliberately left open to broaden again in small increments if heavy
  concurrency proves reliable. What did clearly hold: the tier mapping (Sonnet workers were fine),
  and that worker sub-fan-out orphans results on failure regardless of the root cause.
- **2026-09-28** — Took back what klang's port grew since 2026-08-04: review rounds on the effort
  ladder via pinned `reviewer-*` agent definitions (new in `.claude/agents/`); `Explore` as the
  read-only worker; "decide who owns the build" and the single-writer rules (kept on ultra's advisory
  `BUILD-LOCK.md`; klang's `flock` wrapper not ported); no two workers on one file; give workers the
  constraints; the opening-line bar and its defect-density ledger (started empty). Left in klang: the
  audio-reviewer tiering, the sprudel KSP cache, the "no Agent tool unless requested" project rule.
