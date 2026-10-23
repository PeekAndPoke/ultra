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
| 2026-10-23 | `invoke_ide_action` `OptimizeImports` on `ultra/common/src/jvmMain/kotlin/dates.kt` (probe, maintainer's go in session) | `/opt/dev/peekandpoke/ultra` (`get_repositories` listed ultra's root) | never reached IntelliJ: Claude Code's auto-mode classifier denied the call ("Modify Shared Resources"); no file changed. Needs a permission rule for `mcp__idea__invoke_ide_action`, which only the maintainer can add |
| 2026-10-23 | `invoke_ide_action` `OptimizeImports`, funktor/core batch (22 files) | `/opt/dev/peekandpoke/ultra` | returned OK before the files were on disk: a `git diff` right after showed all 22 unchanged, one re-check later all 22 changed. Check the diff a moment after a big batch, and never read UNCHANGED as "did nothing" until re-checked |
| 2026-10-23 | `OptimizeImports`, deliberately NOT run on `funktor/insights/reference/` (13 files) | — | the folder is no source set, so IntelliJ cannot resolve its names and would drop imports as unused; those 13 wildcards stay until someone decides what the folder is |

### The Optimize Imports run (2026-10-23, the maintainer's go in session)

After the permission rule for `mcp__idea__invoke_ide_action` (maintainer, in `.claude/settings.local.json`): one probe
file, then a call per module, 89 files across 17 modules. Before each, `get_repositories` listed ultra's root only;
after each, a script checked every changed line was an import line and no wildcard was left: 0 problems, and nothing
outside the 89 files changed. IntelliJ re-indexed during the run and showed its progress bar on the maintainer's
screen.

**PARKED 2026-10-23 for a machine reboot (Mission Control's request). Resume here:**

- The 89 changed files are in the working tree, UNCOMMITTED on purpose (`git diff --name-only -- '*.kt'`).
- Compile: green, 0 errors over the 17 touched modules. Tests: NOT proven. The run was stopped. Every failure
  traced to the databases being unreachable (`ArangoDBException`, `MongoTimeoutException`, then
  `NoClassDefFoundError` cascades from a failed class initialiser), none to imports.
- Next: check Arango and Mongo are up, take the build lock, rerun `console/wt-gradle.sh :ultra:common:jvmTest
  :ultra:kontainer:test :ultra:remote:check :ultra:slumber:jvmTest :karango:core:test :mutator:ksp:test
  :funktor:auth:jvmTest :funktor:logging:jvmTest :funktor:messaging:jvmTest :funktor:testing:jvmTest
  :funktor:all:jvmTest :funktor:staticweb:test :funktor:cluster:jvmTest :funktor:rest:jvmTest
  :funktor:insights:jvmTest :funktor:core:jvmTest :funktor-demo:server:test --continue`. On green, commit the
  89 files on their own, then this ticket.
- Tally so far: 11 calls, 10 succeeded, 1 denied (classifier), 1 early return; 89 of 102 files; 13 left in
  `funktor/insights/reference/`.

## Acceptance

- [ ] The tables are ready for the 2026-10-30 meeting.

## History

- 2026-10-23: created.
- 2026-10-23: wildcard settings fixed in `.idea/codeStyles/Project.xml` (maintainer's go).
