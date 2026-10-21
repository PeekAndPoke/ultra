# ULTRA-00232: Project layout and ticket numbers

> **Status (2026-10-21): IN PROGRESS.** Step 1 (layout) committed; step 2 (numbering) running.

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
- [ ] Step 2: `ticket init ULTRA`, then `ticket adopt` one file at a time, oldest first, one commit each
      (the maintainer's choice over a bulk migrate). Old names stay behind the number.
- [ ] Last: CLAUDE.md (paths, ticket rules, worktrees) and `console/wt-gradle.sh` / `worktree-new.sh` →
      `../ultra-worktrees/`, then `git worktree move` insights-kraft. CLAUDE.md before/after to the
      maintainer before committing.

## Implementation notes

- `adopt --commit` stages with `git add -A`, so the tree must be clean before each run. That is why
  CLAUDE.md waits until the end.
- The status lines are not converted to `> **Status (date): WORD.**` here; `adopt` places a file by its
  folder when it finds none. `ticket check` reports them until a later pass converts them.

## Test evidence

- [x] `:ultra:codegen:test` 325/0, `:funktor:codegen:test` 174/0 after the resource edits.

## History

- 2026-10-21: numbered ULTRA-00232 (was `docs/tasks/20261021-project-layout-and-tickets.md`).
