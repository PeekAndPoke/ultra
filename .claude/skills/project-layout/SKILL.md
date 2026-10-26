---
name: project-layout
description: The standard layout of a peekandpoke project, for root files (README, CLAUDE.md, DEV-DIARY, CREDITS, AUTHORS) and the docs/ tree (plans, tasks, in-progress, future, archive, maintainer questions). Use when creating, moving, archiving or looking for a task, plan, diary entry or credit, and when setting up a new project.
---

# Project layout

The same layout in every project, so an agent that knows one repo can find its way in any of them.
The reference projects are **spross** and **klang**. The master copy of this skill lives in the nexus
(`/opt/dev/peekandpoke/missioncontrol/blueprints/skills/project-layout/`). A repo copies it to
`.claude/skills/project-layout/` and records its deviations under "Repo notes" at the end.

**A starting point.** A project deviates from anything here when its purpose and its work call for it, and
says so in a line under "Repo notes". It doesn't need the nexus's OK.

## Root files

| File | What it holds |
|---|---|
| `README.MD` | What the project is, for people. Build and run instructions. |
| `CLAUDE.md` | Agent rules, short: a rules register (`rule \| since \| where it is detailed`) and pointers. Details live in skills and docs, not here. |
| `SEED.md` | *(optional)* The founding plan: purpose, architecture, module layout, roadmap. The tickets in `docs/tasks/` grow out of it. |
| `DEV-DIARY.MD` | The running diary, **newest first**. One `### (yyyy-mm-dd) Title` entry per session or milestone, a few bullets: what changed, what was learned, what is next. |
| `CREDITS.MD` | Every person, paper, standard, algorithm, dataset or tool the project builds on, added **in the change that brings it in**. The source is also named in the code's KDoc. |
| `AUTHORS.MD` + `LICENSE` | The copyright holders, and the license. The license header goes on every source file. |
| `CHANGELOG.md` | Only for published libraries (ultra): one section per release. |

There is no `TODO.MD` and no task list in the README. Open work is a task file.

## docs/

```
docs/
  tasks/                     one ticket file per piece of work: <PREFIX>-<nnnnn>-<title>.md
    _next-ticket.txt         the counter: "next ticket: XYZ-00042" (written only by the ticket tool)
    in-progress/             the tickets being worked on RIGHT NOW (+ README.md: the ticket-file shape)
    future/                  plans and ideas: loose ideas, or work still to break into tickets
    _maintainer-questions.md open questions for the maintainer, numbered (Q1, Q2 ...); not tickets
    _priorities.md           (optional) the order of the work streams
  tasks-archive/
    <yyyy-mm>/<PREFIX>-<nnnnn>-<title>.md   finished tickets, in the month they closed
  research/                  research notes, reading, measurements that inform tickets
  history/                   (optional) <yyyy>-Q<n>.md: quarterly summaries of the diary
  housekeeping/              (optional) dated inventories and audits of the repo itself,
                             and the ticket migration record (<yyyymmdd>-ticket-migration.md)
```

There is no `docs/plans/`: a plan is a ticket too (maintainer, 2026-10-20). A project adds its own
folders beside these (klang: `blog/`, `benchmarks/`, `funding/` ...). The folders above keep their names
and meaning everywhere.

### Tickets

Every task and plan has a ticket number: the project's prefix and five digits (`SPROSS-`, `KLANG-`,
`ULTRA-`, `MC-` for the nexus; examples here use `XYZ-`, a prefix no project has, so `check` never
mistakes an example for a reference). The number never changes, so a reference
names the number and a moved file breaks nothing.

- **The `ticket` tool does every step** (`console/tools/ticket/`, see `/console-tools`): `new` takes
  the next number (safe with several sessions at once), `move` sets the status and puts the file in its
  folder, `find` prints where a ticket is (also by an old file name or code), `check` runs in the
  pre-commit and on every review.
- **The folder follows the status word:**

  | Folder | Status words |
  |---|---|
  | `docs/tasks/future/` | IDEA, NOT SCHEDULED |
  | `docs/tasks/` | DESIGNED, SCHEDULED, NOT STARTED, PAUSED |
  | `docs/tasks/in-progress/` | IN PROGRESS (and PAUSED while the whole project pauses, with `in-progress/NEXT-STEPS.md`) |
  | `docs/tasks-archive/<yyyy-mm>/` | DONE, WON'T IMPLEMENT |

- **References name the number, with a short title on first mention**: "XYZ-00042 (engine
  follow-ups)". A part of a ticket is "XYZ-00042 item 30" or "XYZ-00042 step 7". Never a path or a
  file name: `check` refuses those. Tools that store a path (e.g. `.claude/reviews/*.json`) may.
- **Commits name the ticket** they work on: `XYZ-00042: ...`. Then `git log --grep XYZ-00042` shows
  the whole history of a ticket.
- Maintainer questions (Q1, Q2 ...) are not tickets. They keep their own numbering.
- Old codes and names ("task 04i", `20261017-02-sun.md`) stay findable after a migration: each file's
  History says "numbered XYZ-00012 (was `<old path>`)", and `ticket find 04i` resolves it. Dated
  history (the diary, review records, ledgers) keeps its words.

### Epics (optional)

An epic is an ordinary ticket that other tickets belong to. A project that doesn't need epics skips this.

- **A ticket names its one parent** in its status block: the status line, a bare `>`, then
  `> **Part of:** XYZ-00042 (short title)`. The bare `>` makes it a paragraph of its own. `ticket new "Title" --part-of XYZ-00042` writes it, and
  `move` keeps it.
- **The epic keeps no list of its children.** `ticket list --epic XYZ-00042` derives it, with counts:
  closed (DONE and WON'T IMPLEMENT), open, paused, ideas. `ticket epics --json` gives every epic for
  the dashboard snapshot.
- **The epic's own status never counts.** Its progress is its children's statuses. An epic marked
  DONE with open children is a note from `check`, not an error, and nothing closes an epic by itself.
- **An epic may have a parent of its own.** Counts are of direct children (`--deep` for all). `check`
  refuses a ticket that is its own parent, a loop, a parent with no ticket file, and a short title
  that shares no word with the epic's title (a paraphrase is fine).
- **Membership only.** "Follow-up of X" and "split out of X" say where a ticket came from; they stay
  in History. Numbered sections inside one ticket ("XYZ-00042 item 30") stay sections, not children.
- **An umbrella over everything is not an epic**: it would swallow every progress bar.

### Task lifecycle

1. A new ticket: `ticket new "Title"` (an IDEA in `future/`) or `--status DESIGNED` (in `docs/tasks/`).
2. When work starts: `ticket move <ID> IN PROGRESS`. A paused ticket: `ticket move <ID> PAUSED`.
3. When it is done: `ticket move <ID> DONE` (or `WON'T IMPLEMENT`, a first-class outcome, with the
   reason in its History). It goes to `docs/tasks-archive/<yyyy-mm>/`.

One look at `in-progress/` answers "what is being worked on". The status line in each file says how far it is.

### Task file shape

0. **Title line**: `# <ID>: <title>`.
1. **Status line**: `> **Status (yyyy-mm-dd): <WORD>.**` plus one sentence on where it stands and
   what blocks it. `ticket move` writes it. An epic's child adds a bare `>` and
   `> **Part of:** <ID> (short title)`. The words:
   - before work starts: IDEA (not designed yet), DESIGNED (ready to schedule), SCHEDULED, NOT SCHEDULED,
     NOT STARTED
   - during: IN PROGRESS, PAUSED
   - at the end: DONE, or WON'T IMPLEMENT (a first-class outcome: the task is archived like a
     done one, with the reason in its History)
2. **Why this exists**: the goal in two or three sentences, plus the ticket or SEED section it serves.
3. **Depends on / unblocks**: the tickets before and after it, by number.
4. **Decide before implementing**: each open design question with a recommendation. An open
   question goes to `_maintainer-questions.md`. Never build on an unanswered question in silence.
5. **The work**: numbered steps, each small enough to review and commit on its own, each naming the
   files it touches and the tests it adds.
6. **Acceptance**: what must be true when it is done, as checks someone can run.
7. **Out of scope**: what is left for a later task.
8. **History**: one dated line per commit or decision, newest last.

A project may add sections, such as ultra's review ledger. It doesn't drop any of these.

### Maintainer questions

`docs/tasks/_maintainer-questions.md` collects questions in one place, numbered, so the maintainer can
answer "Q3: yes". An answered question moves into the task it belongs to, with the date and the
answer.

## Memory lives in the repo

- `<module>/MEMORY.md` says what is true in that module **now**, and stays short. A change updates
  its section in place and adds one History line. The narrative goes in the task record.
- Durable facts go in the repo: module MEMORY.md, task files, skills, CLAUDE.md. Not in the
  home-directory auto-memory. If a fact has no home, ask where it belongs.

## console/ and .claude/

- `console/`: scripts people and agents run. `console/tools/<tool>/` holds the shared mechanical
  tools (see `/console-tools`). The build lock wrapper (e.g. `console/with-build-lock.sh`) lives here too.
- `.claude/` holds **agent configuration and agent machinery**: `skills/`, `agents/`,
  `settings.local.json`, the build lock and its log, records written by tools (e.g. review records
  in `.claude/reviews/`). No tasks, plans or docs for people. No worktrees (below).

## Worktrees

**Each project decides how it uses worktrees** (maintainer, 2026-10-20) and writes its way down in Repo
notes, for example a measurement tree at a fixed commit for long simulations. The starting point:

- Work goes sequentially on the repo's own checkout. That is the one IntelliJ has open, so its index sees
  the work. The maintainer prefers "a steady sequential workload rather than spikes".
- A worktree lives in the sibling directory `<repo>-worktrees/<name>`, never inside the repo: inside, it
  puts a second copy of every class into the IDE's index. For the same reason, don't spawn sub-agents with
  `isolation: "worktree"` (Claude Code puts those under `.claude/worktrees/`).
- A finished worktree is removed together with its merged branch.

## workshop/: code bound for Ultra

Ultra (with Kraft inside it) is the toolmaker; the other projects build on it (maintainer, 2026-10-26). Every
project that uses Ultra has a `workshop/` module: framework code that might move into Ultra or Kraft grows there
first and is battle-tested in the project's own use (klang's key manager, spross's Kulisse scenes), then moves.
Maven Central limits how often Ultra can publish (io.peekandpoke: 17 releases and 200 MB a month), so a piece
waits in the workshop rather than in a stream of small Ultra releases. Ultra itself has no workshop: it is where
the pieces go.

The rules, from klang's and spross's `workshop/CLAUDE.md`:
- Project agnostic: depends on Ultra, Kraft and Kotlin's own libraries only, never on a project module, not even
  through a test; no project domain in names, KDoc or tests. The move is then a copy.
- Apache-2.0 like Ultra: `LICENSE` in the folder, no project license header in its files.
- Kraft's patterns (a service as an app attribute, a component's machinery as a controller); machinery and look
  apart, the look replaceable by the app.
- Tests for everything, mutation-checked; pure logic in `commonMain` where it can be.
- Package `io.peekandpoke.<project>.workshop.<piece>`; the move renames it once. The app module uses a piece like
  any library; domain modules never depend on the workshop.
- `workshop/CLAUDE.md` holds these rules and one section per piece; each piece has a ticket.

A piece that has proven itself goes to Ultra as a request (`/mission-control-link`, "Requests to Ultra"). It
leaves the workshop once Ultra has released it and the project uses it from there.

## Commits

Every commit an agent wrote or co-wrote carries a `Co-Authored-By:` trailer naming the model. A commit
the maintainer wrote alone carries none.

## Repo notes

<!-- Each repo records here where it deviates from this layout, and why. -->

- **Ticket prefix `ULTRA-`.** Tickets migrated on 2026-10-21 keep their old dated name behind the number
  (`20260802-foo.md` became `<ID>-20260802-foo.md`), so the creation date stays visible; new tickets have none.
- **`CHANGELOG.md`** is kept: ultra publishes libraries.
- **`docs/tasks/TEMPLATE.md` is ultra's ticket shape.** It adds the review record (the ledger of
  `.claude/skills/review-loop/`) to the standard sections. `in-progress/README.md` points to it.
- **User docs live in `docs-site/`** (peekandpoke.io), not in `docs/`. `docs/kraft/examples/` holds built
  Kraft example bundles that `docs-site/public/kraft-examples` links to; they are build output, not docs.
- **"What will bite me" lives in the library's skill** (CLAUDE.md, "Docs vs skills"), not in a module
  `MEMORY.md`. Ultra has none.
- **Worktrees:** none by default; when one is needed, `console/worktree-new.sh <track>` puts it in
  `../ultra-worktrees/<track>`, and `console/wt-gradle.sh` gives it its own test databases
  (`console/wt-gradle.sh:12`).
- **IntelliJ has this checkout open**, so work goes here, in sequence (maintainer, 2026-10-20: "a steady
  sequential workload rather than spikes"). Read-only `mcp__idea__*` queries (symbol search, call graphs,
  structural search, inspections) are allowed; write and build IDE tools (apply_patch, rename_refactoring,
  reformat, build_project, run configurations) stay off until the maintainer approves them one by one. IDE
  actions go through `.claude/hooks/idea-action-gate.py`; ultra's `idea-actions.allow` holds only
  `OptimizeImports` (the blueprint also lists `ReformatCode`, not approved here).
- **Git hooks (MC-00018):** `console/tools/hooks/hooks.json` switches the three dash checks off (ultra has no
  dash rule); control bytes and the Claude trailer stay on. Installed 2026-10-24 by the maintainer.
- **`docs/tasks/_incidents.md` replaces the review-loop escape ledger:** a review escape is one class of
  incident (MC-00028). `.claude/skills/review-loop/escape-ledger.md` goes when the review-loop core is taken
  from the blueprint (MC-00012).
- **Not adopted yet:** `TODO.MD` still exists, and there is no `DEV-DIARY.MD`, `CREDITS.MD` or `AUTHORS.MD`.

## Proposed upstream

<!-- Findings this repo sent back to Mission Control, not merged into the blueprint yet:
     - yyyy-mm-dd: <what, one line> (<where it happened>)
     Remove an item when an update arrives that contains it. -->

## Changes

<!-- Kept by the nexus, newest first. Tells a repo what an update brought. -->
- 2026-10-26: `workshop/`: every project that uses Ultra keeps framework code bound for Ultra there first, battle-tested, then moved (maintainer; Maven Central release limits).
- 2026-10-20: every blueprint is a starting point; a project deviates where its work needs it (maintainer). Worktrees: each project decides.
- 2026-10-20: ticket numbers: every task and plan is `<PREFIX>-<nnnnn>-<title>.md`, kept in its status's folder by the `ticket` tool; references name the number; no `docs/plans/` (maintainer).
- 2026-10-20: worktrees: none by default; when used, in the sibling `<repo>-worktrees/`, never `.claude/worktrees/` (maintainer).
- 2026-10-19: more status words (IDEA, DESIGNED, SCHEDULED, NOT SCHEDULED, WON'T IMPLEMENT); `.claude/` may hold tool-written records such as `.claude/reviews/` (both from spross).
- 2026-10-19: first version, spross and klang as the reference.
