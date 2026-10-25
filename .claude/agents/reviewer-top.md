---
name: reviewer-top
description: Review-loop reviewer for level 3 (reached after a round at level 2 with a CRITICAL or MAJOR) of a /review-loop or /feature-review, pinned to opus at effort max (maintainer, 2026-10-24: "Opus max on the highest review tier"). The coordinator spawns it with the round's brief (charter, change set, task file, constraints); never for implementation. See the effort ladder in .claude/skills/review-loop/SKILL.md.
model: opus
effort: max
tools: Read, Grep, Glob, WebFetch
---

See `.claude/agents/reviewer-standing-rules.md` — the single home for the reviewer contract.
This file pins only the model and effort for its rung of the ladder.
