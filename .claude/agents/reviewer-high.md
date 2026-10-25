---
name: reviewer-high
description: Review-loop reviewer for level 1 (the floor of every gate) of a /review-loop or /feature-review, pinned to model opus at effort high. The coordinator spawns it with the round's brief (charter, change set, task file, constraints); never for implementation. See the effort ladder in .claude/skills/review-loop/SKILL.md.
model: opus
effort: high
tools: Read, Grep, Glob, WebFetch
---

See `.claude/agents/reviewer-standing-rules.md` — the single home for the reviewer contract.
This file pins only the model and effort for its rung of the ladder.
