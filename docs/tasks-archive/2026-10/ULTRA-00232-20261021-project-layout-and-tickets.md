# ULTRA-00232: Project layout and ticket numbers

> **Status (2026-10-23): DONE.** Both steps done; CLAUDE.md approved by the maintainer and committed (8d55fd8a).

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
- Status lines converted to `> **Status (date): WORD.** <the old status text>` (Mission-Control's request,
  so the dashboard can read them). The old word stays in the prose. Mapping:
  - archive: DONE, SUPERSEDED → WON'T IMPLEMENT.
  - future/: FUTURE → NOT SCHEDULED, else IDEA.
  - docs/tasks/: TODO, OPEN, BACKLOG, SEED, COLLECTED (red-team), none → NOT STARTED; PLANNED → DESIGNED.
    Started but not finished (IN PROGRESS, IN REVIEW, IMPLEMENTED, CODE DONE, PARTIAL, …) → PAUSED, because the
    project is paused.
  - The date is the first date in the old line, else the date in an archived file's name, else the file's last
    commit before the migration.
  - Archived: ULTRA-00070, 00072, 00073, 00074, 00076, 00077 (DONE with a passed gate) and ULTRA-00085
    (superseded → WON'T IMPLEMENT).
  - For the maintainer, left PAUSED: ULTRA-00071 and ULTRA-00191 (DONE, no gate named), ULTRA-00195 (DONE,
    `/feature-review` not run), ULTRA-00129 and ULTRA-00158 (IMPLEMENTED), ULTRA-00155 (CODE DONE, needs review).

## Test evidence

- [x] `:ultra:codegen:test` 325/0, `:funktor:codegen:test` 174/0 after the resource edits; the resources
      did not change after that run.
- [x] Every changed line in `*.kt`, `*.kts`, `*.ts` and `*.vue` is a comment (diff against `e44c28c3`).
- [x] `ticket check`: 232 tickets, 0 problems.
- [x] docs-lint: the same 5 broken links before and after, all older than the migration.
- [x] The tool's own tests: 18/18 after both patches.

## History

- 2026-10-21: numbered ULTRA-00232 (was `docs/tasks/20261021-project-layout-and-tickets.md`).
- 2026-10-23: CLAUDE.md migration committed (8d55fd8a) after the maintainer's OK; the slim version followed (34111f65).
- 2026-10-23: DONE.
