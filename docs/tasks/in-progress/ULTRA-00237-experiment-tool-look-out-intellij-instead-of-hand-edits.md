# ULTRA-00237: Experiment: tool look-out (IntelliJ instead of hand edits)

> **Status (2026-10-23): IN PROGRESS.** Until the meeting on 2026-10-30 (MC-00029), which decides which IntelliJ write actions become allowed, one by one.

## Why this exists

The maintainer (2026-10-23, via Mission Control): "For the next week agents will look out for where they would like
to use tools instead of tweaking code "by hand". This might include: IntelliJ Optimize Imports -> make sure the no
wildcard imports setting is on in each projects; IntelliJ Auto-Format code -> record differences in code-style so we
can update the code-style settings in IntelliJ; IntelliJ Refactor Rename Symbol; IntelliJ Refactor Move". And: "we
need to make sure that these tool uses really land in the correct project. Not sure how stable the IntelliJ MCP is".

## Decide before implementing

- **Setting fixed 2026-10-23** (maintainer: "yes add the no wildcard to the .idea/codeStyles/"): an empty
  `PACKAGES_TO_USE_STAR_IMPORTS` and `NAME_COUNT_TO_USE_STAR_IMPORT_FOR_MEMBERS` = MAX in the tracked
  `.idea/codeStyles/Project.xml`. Open: clean the 102 files with an Optimize Imports run — a write action, the
  maintainer's call.
- Write actions stay off this week unless the maintainer allows one (the IntelliJ Repo note in
  `.claude/skills/project-layout/`).

## The work

Safeguards for any IntelliJ write action: pass ultra's absolute `projectPath`; before it, `get_repositories` with the
same path lists ultra's root; after it, `git status`/`git diff` show the change in ultra and nothing elsewhere —
anything else is an incident (`docs/tasks/_incidents.md`); never for work meant for a worktree (IntelliJ edits the
main checkout); only where the maintainer expects it (it lands in their open editor). Record every call that lands
wrong or fails.

### Wildcard-import setting (checked 2026-10-23)

`.idea/codeStyles/Project.xml:4` sets `NAME_COUNT_TO_USE_STAR_IMPORT` to MAX_INT, but leaves
`PACKAGES_TO_USE_STAR_IMPORTS` at the Kotlin default (`java.util.*`, `kotlinx.android.synthetic.**`, `io.ktor.**`) and
`NAME_COUNT_TO_USE_STAR_IMPORT_FOR_MEMBERS` at its default. So Optimize Imports still WRITES `io.ktor` and `java.util`
wildcards: 102 tracked Kotlin files have one (`git grep -l -E "^import [a-zA-Z0-9_.]+\.\*$"`), led by
`io.ktor.server.application.*` (55), `io.ktor.http.*` (32), `io.ktor.server.routing.*` (27), `java.util.*` (7).

### Cases: a tool would have beaten a hand edit

| date | what | tool | what it would have saved |
|---|---|---|---|
| 2026-10-23 | 102 files with wildcard imports against the CLAUDE.md rule | Optimize Imports (after the setting fix) | a hand edit per file; a compiler-checked result |

### Auto-Format differences from ultra's code style

| date | file | what Auto-Format changes | which side is right |
|---|---|---|---|

### IntelliJ calls that landed wrong or failed

| date | tool | projectPath | what happened |
|---|---|---|---|

## Acceptance

- [ ] The tables are ready for the 2026-10-30 meeting.

## History

- 2026-10-23: created.
- 2026-10-23: wildcard settings fixed in `.idea/codeStyles/Project.xml` (maintainer's go).
