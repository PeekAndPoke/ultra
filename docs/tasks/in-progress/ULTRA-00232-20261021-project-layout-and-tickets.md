# ULTRA-00232: Project layout and ticket numbers

> **Status (2026-10-21): IN PROGRESS.** Both steps done; CLAUDE.md waits for the maintainer's OK.

**Plan:** Mission-Control's project-layout blueprint (missioncontrol MC-00009); maintainer go 2026-10-21.
**Security-critical:** no

## Spec

- [x] Step 1: `.claude/tasks/` → `docs/tasks/` (the five subfolders flattened into it),
      `.claude/tasks-archive/` → `docs/tasks-archive/`, `.claude/future-plans/` → `docs/tasks/future/`,
      new `docs/tasks/in-progress/`. Every path reference rewritten, CLAUDE.md excepted (last step).
- [x] Generated-code resources (`ultra/codegen/.../ts/runtime/{auth,sse}.ts`,
      `funktor/codegen/.../ts/insights/{VaultTab.vue,slices.ts}`) carry no internal task references.
      The maintainer prefers generated code free of them; not a hard rule.
- [x] `console/tools/ticket/` copied from missioncontrol `89bd208`; `.claude/skills/project-layout/` with
      ultra's Repo notes.
- [x] Step 2: `ticket init ULTRA`, then `ticket adopt` one file at a time, oldest first, one commit each
      (the maintainer's choice over a bulk migrate). Old names stay behind the number.
- [x] Last: CLAUDE.md (paths, ticket rules, worktrees) and `console/wt-gradle.sh` / `worktree-new.sh` →
      `../ultra-worktrees/`, then `git worktree move` insights-kraft. CLAUDE.md before/after to the
      maintainer before committing.

## Implementation notes

- `adopt --commit` stages with `git add -A`, so the tree must be clean before each run. That is why
  CLAUDE.md waits until the end.
- The status lines are not converted to `> **Status (date): WORD.**` here; `adopt` places a file by its
  folder when it finds none. `ticket check` reports them until a later pass converts them.

- 232 tickets, ULTRA-00001 to ULTRA-00232, one commit each. Three stopped `adopt` and were fixed by hand:
  ULTRA-00034 (a stale path the file never had), ULTRA-00207 (an elided `…/name.md`), plus the trial run of
  ULTRA-00001, undone because `git log --follow` recorded `.gitkeep` and `gradle.properties` as former
  paths. ultra's copy of the tool now keeps task paths only, and resolves a stale path by its unique
  file name (Repo notes in `.claude/skills/project-layout/`).
- `ULTRA-00112` and `ULTRA-00114` (red-team value-class tickets) hold a raw NUL byte from before the migration.
- Follow-up, not done here: convert the 231 status lines to `> **Status (date): WORD.**` (`ticket check`
  reports them).

## Test evidence

- [x] `:ultra:codegen:test` 325/0, `:funktor:codegen:test` 174/0 after the resource edits; the resources
      did not change after that run.
- [x] Every changed line in `*.kt`, `*.kts`, `*.ts` and `*.vue` is a comment (diff against `e44c28c3`).
- [x] `ticket check`: 232 tickets, no reference problems; only the 231 status lines.
- [x] docs-lint: the same 5 broken links before and after, all older than the migration.
- [x] The tool's own tests: 18/18 after both patches.

## History

- 2026-10-21: numbered ULTRA-00232 (was `docs/tasks/20261021-project-layout-and-tickets.md`).
