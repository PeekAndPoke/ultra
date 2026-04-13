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
- LLM-readable mirror templates live in `docs-site/src/data/llms/*.md` and `llms.txt` / `llms-full.txt` in the same dir.
  Version strings in those templates use `{{ultraVersion}}` / `{{kraftVersion}}` placeholders, substituted at build time
  by endpoints under `docs-site/src/pages/llms*.ts` (renderer: `docs-site/src/data/llmsTemplate.ts`).
  **Edit the templates when docs change — never edit `docs-site/public/` for LLM mirrors.**
- See `.claude/skills/docs-site/` for the docs workflow.

## Releases

- Canonical version lives in `gradle.properties` → `VERSION_NAME`. Bumping it flows through all Gradle modules.
- Docs-site versions (`ultraVersion`, `kraftVersion` in `docs-site/src/data/site.ts`) must move in lockstep.
- README dependency snippet (`README.MD` ~line 123) has its own hardcoded version list.
- See `.claude/skills/release/` for the full release workflow and checklist.

## Key locations

- Active plans: `.claude/plans/`
- Completed plans: `.claude/plans-archive/` (date-prefixed filenames)
- Skills: `.claude/skills/`
- Agents: `.claude/agents/`
