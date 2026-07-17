---
name: agent-fleet
description: Use when spawning sub-agents, launching multiple agents in parallel, fanning out tasks with the Agent tool, or writing Workflow scripts. Defines how to pick model and effort per sub-agent to balance result quality against token spend.
---

## What This Skill Does

> **Status: PROVISIONAL.** This policy is new and expected to evolve. When a fleet run shows a
> mapping is wrong (a tier too weak for the task, or wastefully strong), propose a concrete
> adjustment to this file and apply it once the user approves. Log accepted changes in the
> changelog at the bottom.

Defines how to assign models and effort levels when distributing work across sub-agents. Applies
to every Agent tool call and every Workflow `agent()` call. Invoke it (mentally or via
`/agent-fleet`) before launching any multi-agent fan-out.

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

## Effort tiers

| Stage | Effort |
|---|---|
| Mechanical / bulk stages | `low` |
| Standard work | omit (inherit session effort) |
| Hardest verify / judge stages | `high` or `xhigh` |

## Where the dials live

- **Agent tool**: set the `model` parameter per call. There is no per-call effort override —
  effort comes from the agent definition. `fork`-type agents always inherit the parent model;
  don't set `model` on them.
- **Workflow `agent()`**: set both `model` and `effort` in the opts, per stage.
- **Custom agents** (`.claude/agents/*.md`): can pin model/effort in frontmatter; prefer that
  for agents whose task type never varies.

## Decision procedure

1. Before launching, classify each sub-task into a tier using the tables above.
2. Set `model` (and `effort`, where available) explicitly on every call — don't let a whole
   fleet silently inherit the session model.
3. When launching a fleet, state the mapping in one line (e.g. "3 Sonnet finders + 1 Opus
   verifier") so the user can correct it — that feedback is how this provisional policy improves.

## Rules of thumb

- **Fan-out multiplies cost.** For large sweeps (~10+ agents), use cheap finders
  (`sonnet`/`haiku`) feeding a narrow, expensive verify stage (`opus`, high effort) — not an
  expensive model on every item.
- **When unsure between tiers:** tier up for correctness-critical work, tier down for
  volume/coverage work.
- **Escalate, don't accept.** If a cheap agent returns a weak or suspect result, re-run that one
  task a tier up instead of patching around bad output.
- **Don't use `haiku`** for anything whose output the coordinator can't cheaply sanity-check.

## Notes

- This skill governs model/effort selection only. Whether to fan out at all is governed by the
  Agent/Workflow tool rules (e.g. workflows require explicit user opt-in).
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
