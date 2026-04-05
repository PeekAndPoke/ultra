# Project Rules for Claude

This file is loaded automatically into every Claude Code session working on this repo. These are
the project's working rules.

## Plan maintenance

**When you complete work that matches an item in `.claude/plans/*.md`, update that plan doc
before finishing the task.**

- Update status markers (TODO → DONE, add dates).
- Update gate status tables if present.
- Update the critical path section if an item unblocks.
- Archive completed plans to `.claude/plans-archive/` with a `YYYY-MM-DD-` prefix.
- Do NOT create new plan docs unless explicitly asked — just update existing ones.

The goal is that anyone (human or agent) reading a plan sees the current state, not an outdated
snapshot. Stale plans are worse than no plans because they cause wrong priorities.

## Code style

- Use explicit imports in Kotlin — never wildcard/star imports.
- Never use fully qualified class names in Kotlin code — always add the import.
- See `.claude/skills/code-style/` for the full style guide.

## Tools & packages

- Use `pnpm`, never `npm`, for JavaScript package management.
- Use "PeekAndPoke" or "peekandpoke" for branding, never "peek&poke".

## Testing

- Addon tests live in each addon module's `src/jsTest/kotlin/`.
- Kraft core tests are in `kraft/core-tests/src/jsTest/kotlin/`.
- Test real browser behavior via `TestBed.preact { }` — don't mock what you can run.

## Documentation

- Library reference docs live in `docs-site/src/pages/ultra/*`.
- LLM-readable mirror in `docs-site/public/llms.txt` + `docs-site/public/llms/*.md`.
- Both files need updating when docs change. See `.claude/skills/docs-site/` for the workflow.

## Key locations

- Active plans: `.claude/plans/`
- Completed plans: `.claude/plans-archive/` (date-prefixed filenames)
- Skills: `.claude/skills/`
- Agents: `.claude/agents/`
