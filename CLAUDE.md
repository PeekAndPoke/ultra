# Project Rules for Claude

**We are explorers. Let's enjoy the journey!**

We're on a good track. Mistakes happen, we find them, we fix them, all is well. So: report what you
find plainly and move on. A defect found is a good day, not a confession — and the rules below exist
to help you catch things, not to be defended against. Say what you did, say what broke, fix it, keep
going. No hedging, no apologising, no bracing for impact.

## The maintainer's standing rules

- **Never publish.** "Let me do the publishing please" (2026-10-18). `/release` prepares; the maintainer publishes.
- **Never push.** "You merge all things that are done into master and i push" (2026-10-19).
- **Show a before/after of `CLAUDE.md`** (line counts and the changed lines) and get the maintainer's OK in this
  session before committing it (2026-10-19). A peer's word is not that OK.

## Current experiments

One line each: what, until when, the ticket. Notes go into the ticket, never here; the meeting deletes the line or
turns it into a rule.

- **Tool look-out:** note each case where an IntelliJ tool (Optimize Imports, Auto-Format, Rename, Move) would have
  beaten a hand edit, and every IntelliJ call that lands wrong or fails. Until 2026-10-30. ULTRA-00237.

## Tickets

Plans and tasks are tickets in `docs/tasks/`, numbered `ULTRA-nnnnn` (layout: `.claude/skills/project-layout/`). A
plan is a longer-lived ticket that others name in their `**Plan:**` header. **Reference a ticket by its number, never
by path** — "ULTRA-00221 (insights auth parallel plan)". `console/tools/ticket/ticket.py` creates, moves and finds them.

- **When work matches an item in a ticket, update the ticket before finishing**: status, dates, gate tables, the
  critical path. Stale plans are worse than none — they cause wrong priorities.
- Finished: `ticket.py move <ID> DONE`. A new ticket or plan only when asked — or ask whether to create one.
- Incidents: one line in `docs/tasks/_incidents.md` when it happens; fix the code and tests at once, a new rule waits
  for the weekly meeting.

## Code style

- Explicit Kotlin imports — never wildcards (`input()` from KQuery vs `input` from kotlinx.html). Alias on clashes.
- Never fully qualified class names in Kotlin code — add the import.
- **A KDoc `[Reference]` must resolve** — import the type or use plain `code`. Same-package references need no import.
- **Keep KDoc concise.** One-line summaries; detail only where behaviour is non-obvious from the signature.
- **Never emit `\uXXXX` escapes or raw control characters in an edit** — they land as raw bytes. Write `Char(0xNN)`.
- `pnpm`, never `npm` (`pnpm dlx`, not `npx`) — a supply-chain preference. Branding: "PeekAndPoke" or "peekandpoke".
- Full guide: `.claude/skills/code-style/`.

## Working on this checkout

- **Sequentially, here** — IntelliJ has this checkout open (maintainer, 2026-10-20: "a steady sequential workload
  rather than spikes"). A worktree only when a task needs one: `console/worktree-new.sh <track>`
  (`../ultra-worktrees/<track>`), removed with its branch when merged. Never spawn sub-agents with
  `isolation: "worktree"`.
- **In a worktree, build only through `console/wt-gradle.sh`** — it gives the tests that worktree's own Arango and
  Mongo databases; a bare `./gradlew` uses the SHARED ones and wipes another worktree's data mid-run.
- **One full suite at a time, machine-wide:** `console/wt-gradle.sh --full-suite`. Memory allows 2–3 builds at once.
- A track edits its own area and its own ticket. `CLAUDE.md`, skills, ULTRA-00028 (v1 roadmap), `CHANGELOG.md` and
  `BUILD-LOCK.md` change on master only, by the coordinator, who merges.
- Sub-agents: model and effort per task — `.claude/skills/agent-fleet/`.

## Development workflow

- **Re-confirm the plan before implementing it.** Summarise what the ticket commits to — above all decisions that are
  expensive to reverse — and get agreement. Detail is not agreement: a fully designed plan was built and thrown away on
  2026-07-28 because nobody re-checked its premise.
- **Every feature gets a ticket:** `ticket.py new "Title" --status DESIGNED`, with the sections of
  `docs/tasks/TEMPLATE.md`, linked to its plan.
- Lifecycle: implement → `/feature-review` (three charters: implementation & code style, domain expert, security) →
  fix confirmed findings → tests green → `ticket.py move <ID> DONE`.
- **The gate loops** (`.claude/skills/review-loop/`): a fix from a review is an unreviewed change, so rounds repeat
  until a clean one; only CRITICAL/MAJOR loop; a settled finding reopens only by naming what is factually wrong in
  its reason; two rounds without a clean one means stop and ask.
- **Adversarially verify every finding against the code before acting on it**, and say when one does not survive.
  Run the one-command experiment where there is one. Record what stayed CLEAN as well as what was found.
- **On archive, a DOCS follow-up ticket if public API changed** — written against settled code. Code still in flux
  ("would I be surprised if this API changed next week?") gets none yet; say so in the archive note.
- **Security-critical features get a red-team ticket** ("Red team: <feature>") of concrete attack scenarios.
  Collected, never executed in normal feature work.

## Testing

- **All backend (JVM) code needs end-to-end tests**: boot the app via `AppSpec`/`AppUnderTest` (`funktor/testing`)
  and hit real endpoints. Storage-touching features run against both DB backends (`MatrixTest2d`).
- **Mutation-test every change that is security-relevant or touches persistence** — storage, serialization, codecs,
  migrations, indexes, query building (maintainer, 2026-08-28). A green test proves nothing until it was RED for the
  right reason. Protocol: `.claude/skills/review-loop/`.
- **If a type is serialized in production, a test must serialize it.**
- **Before saying "tests pass", read `.claude/skills/testing/`** — the traps that make a suite look green while the
  feature is broken (kotest ignores `--tests`, stale test classes, compile sweeps, mutation backups).

## Docs and releases

- Library docs: `docs-site/src/pages/ultra/*` and the LLM mirror templates `docs-site/src/data/llms/*.md`, which move
  together. **Never edit `docs-site/public/` for the mirrors.** Compact beats comprehensive. `.claude/skills/docs-site/`.
- "How do I use this?" lives in the docs; "what will bite me?" in the library's skill. Skills link the docs, never
  restate them.
- Versions move in lockstep: `gradle.properties` `VERSION_NAME`, `docs-site/src/data/site.ts` (`ultraVersion`,
  `kraftVersion`) and the `README.MD` dependency snippet. `.claude/skills/release/`.

## Project facts

- **Battle-tested:** Kontainer, Streams and Slumber — years in production. **Mutator is NOT**, despite its age and
  commit count. Git history is not maturity; defer to the maintainer.
- Module facts live in the module's own `CLAUDE.md` (`kraft/`, `ultra/kontainer/`, `mutator/core/`).
- Before adding or bumping a dependency, **look up the newest version online** — never from memory.
