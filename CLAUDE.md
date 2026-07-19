# Project Rules for Claude

This file is loaded automatically into every Claude Code session working on this repo. These are
the project's working rules.

## Plan & task maintenance

Plans and tasks live together in `.claude/tasks/`. A plan is just a longer-lived task doc that
other tasks reference from their `**Plan:**` header.

**When you complete work that matches an item in `.claude/tasks/*.md`, update that doc before
finishing the task.**

- Update status markers (TODO → DONE, add dates).
- Update gate status tables if present.
- Update the critical path section if an item unblocks.
- Archive completed docs to `.claude/tasks-archive/<YYYY-MM>/` — one folder per month, based on the
  date in the filename. Create the month folder if it does not exist.
- Do NOT create new plan docs unless explicitly asked — just update existing ones.

The goal is that anyone (human or agent) reading a doc sees the current state, not an outdated
snapshot. Stale plans are worse than no plans because they cause wrong priorities.

## Code style

- Use explicit imports in Kotlin — never wildcard/star imports.
- Never use fully qualified class names in Kotlin code — always add the import.
- See `.claude/skills/code-style/` for the full style guide.

## Tools & packages

- Use `pnpm`, never `npm`, for JavaScript package management.
- Use "PeekAndPoke" or "peekandpoke" for branding, never "peek&poke".

## Sub-agent orchestration

- When fanning out work across sub-agents, pick model and effort per task — cheap tiers for
  retrieval/mechanical work, strong tiers for coding/verification; the coordinator stays on the
  user-selected model. See `.claude/skills/agent-fleet/` (provisional).

## Development workflow

- **Every feature gets a task file** in `.claude/tasks/`, named `YYYYMMDD-<slug>.md` (copy
  `.claude/tasks/TEMPLATE.md`). Features usually come from plan phases — link the plan in the task.
- Lifecycle: implement → run `/feature-review` (mandatory multi-agent review: 1. implementation &
  code style, 2. domain expert, 3. security) → fix confirmed findings → tests green → mark DONE
  and move the task file to `.claude/tasks-archive/<YYYY-MM>/` (filename is already dated).
- **Security-critical features** get a follow-up red-team task (`YYYYMMDD-redteam-<slug>.md`) in
  `.claude/tasks/`, describing concrete break-in/attack scenarios to attempt. These are COLLECTED,
  not executed — dedicated penetration-test sessions sweep them later. Never run attack scenarios
  as part of normal feature work.

## Testing

- Addon tests live in each addon module's `src/jsTest/kotlin/`.
- Kraft core tests are in `kraft/core-tests/src/jsTest/kotlin/`.
- Test real browser behavior via `TestBed.preact { }` — don't mock what you can run.
- **All backend (JVM) code needs end-to-end tests**: boot the app via the funktor testing harness
  (`AppSpec`/`AppUnderTest` in `funktor/testing`) and exercise real endpoints, not just units.
  Storage-touching features must run against both DB backends (`MatrixTest2d` pattern).

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

- Active tasks and plans: `.claude/tasks/` (task template: `.claude/tasks/TEMPLATE.md`)
- Completed tasks and plans: `.claude/tasks-archive/<YYYY-MM>/` (grouped by month)
- Not-yet-scheduled ideas: `.claude/future-plans/`
- Skills: `.claude/skills/`
- Agents: `.claude/agents/`
