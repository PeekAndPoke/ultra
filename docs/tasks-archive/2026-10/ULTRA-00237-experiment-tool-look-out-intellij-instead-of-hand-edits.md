# ULTRA-00237: Experiment: tool look-out (IntelliJ instead of hand edits)

> **Status (2026-10-30): DONE.** The meeting of 2026-10-30 (MC-00029, topic 6) decided: Refactor Rename, Move, Move Members and Safe Delete allowed with safeguards; Reformat Code stays off.

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
| 2026-10-26 | ULTRA-00242: `HandClock` cut out of `SmoothSpec.kt` into its own test file, by a script | Move (extract the class to a file) | little: the tests compiled at once; Move would have adjusted visibility and imports by itself |
| 2026-10-26 | ULTRA-00242: `@JvmName` in common code without `import kotlin.jvm.JvmName`, found by a failed JS/native compile | `get_file_problems` on the new file before the build | one failed 30 s Gradle run |

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

**Result:** committed as `59431271`.

- **Tally:** 11 calls. 10 succeeded, 1 was denied by the classifier, and 1 returned early. 89 of 102 files done;
  13 left in `funktor/insights/reference/`.
- **Compile:** green over the 17 modules.
- **Tests:** 5948 green, 16 test tasks run fresh. Remote's JS tests were UP-TO-DATE from the earlier run, which had
  the same changes. mutator/ksp, funktor/testing and funktor/staticweb have no tests.
- **The first test run had no databases.** The `arangodb` and `mongodb` containers had exited hours before it
  started. Every failure was a DB timeout followed by `NoClassDefFoundError` cascades; none came from imports. The
  run was parked across a reboot, then rerun once the maintainer started the containers.

## Acceptance

- [ ] The tables are ready for the 2026-10-30 meeting.

## History

- 2026-10-23: created.
- 2026-10-23: wildcard settings fixed in `.idea/codeStyles/Project.xml` (maintainer's go).
- 2026-10-26: two cases from ULTRA-00241/00242; no IntelliJ call this session, none landed wrong.
- 2026-10-30: the week's cases in the meeting prep (`docs/housekeeping/2026-10-30-meeting-prep-Ultra-Main-Dev.md`); no IntelliJ write call this week. Decided at the meeting; the maintainer's OK in session: `mcp__idea__rename_refactoring` allowed in `.claude/settings.json`, `Move` and `SafeDelete` on `.claude/hooks/idea-actions.allow` with the safeguards; the CLAUDE.md experiment line removed. DONE.
- 2026-10-30: DONE.
