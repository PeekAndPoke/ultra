# Meeting prep, Ultra-Main-Dev, 2026-10-30 (MC-00017)

## 1. Last week's action items (topic 4)

- CLAUDE.md about 250 to about 100 lines: **done**, 251 to 96 (`34111f65`); 104 today (the "Current experiments"
  section `f51cfa88`, the top-rung line `aadc5018`).
- The "Motör" line: **done**, deleted in `34111f65` (no "Motör" or "Motor" left in CLAUDE.md).
- The maintainer's three quotes written with dates ("Let me do the publishing please", "You merge… and i push",
  "show a before/after of CLAUDE.md"): **done**, `34111f65`.
- The two escape-ledger rows (one mutant per arm of a cached-state branch, ULTRA-00205; "who owns objects handed to a
  library?" in the domain charter, ULTRA-00206): **not done**. Both sit as proposed rules in `docs/tasks/_incidents.md`
  for this meeting (item 3).

## 2. The week in ultra (ten lines)

- **Worked on:** the UI batch for Klang and Spross: Kraft leave guards, TestBed with app and router, the shared
  WindowController, streams `smooth`/`sampled`/shared frames, pixi and three.js members. Also the new `kraft:dnd`
  module, a WakeLock and a ResponsiveController for portrait and landscape, and five bugs found by the reviews.
  20 tickets DONE; releases 0.115.0 and 0.116.0 prepared; the next one (0.117.0) is ready to start.
- **Obstacles:** nine review rounds on the router. Five of the seven MAJORs after round 1 came from the previous
  round's fix, all about browser timing: hash routing follows an address a task later, path routing at once,
  `popstate` fires inside the hash setter, Preact renders in a microtask. Our specs only cover the orderings we
  thought of.
- **What worked:** delta rounds after the valve (a third of a full round's tokens, aimed where the risk is), and
  mutants on every fix (about 90 this week; each red on its own row).
- **For the others:** run timing-sensitive rows under both strategies by default; never mutate the tree while
  reviewers read it (incident below).
- **Code worth a look:** `kraft/dnd/` (pointer events, lost releases, click swallowing, chords; `DndSpec.kt` has 56
  rows driving real PointerEvents); `Router.kt` `guardLeaving`, `takeLinkClick`, `pageOf` (`kraft/core/.../routing/`);
  `ultra/streams` `ownEntry` (`streams.kt`: one entry per subscription; a lambda that captures nothing is one shared
  instance) and `sampled`/`smooth`; `WakeLock.kt` (a counter over one lock).

## 3. Open incidents (`docs/tasks/_incidents.md`), a proposed verdict each

- **2026-10-17, ULTRA-00205 (Kronos `isInitialized()` arm untested):** rule, home review-loop Standard 2: "a branch on
  cached or lazy state gets one mutant per arm".
- **2026-10-18, ULTRA-00206 (Chart.js kept and mutated the caller's config):** rule, home the domain charter in
  `/feature-review`: "objects handed to a third-party library: does it keep and mutate them?".
- **2026-10-30, a reviewer read my mutant (PopupsStage) during round 6:** rule plus tool. The rule goes in review-loop:
  no mutation check and no edit in the working tree while a review round reads it. The tool: reviewers read a
  snapshot, e.g. `git worktree add --detach` of the round's commit, or a review-pack. Same group as 3a.

## 4. Tool look-out (ULTRA-00237) and top-rung rounds

**Tool cases this week**, where a tool would have beaten a hand edit; no IntelliJ write calls this week, none failed:
- `pagePath` (private in Router) became the public `Router.pageOf` across four files by a script. Refactor Rename plus
  Move would have done it with the compiler's help.
- A double blank line left by the script edit was found by a reviewer twice (rounds 7 and 9). Reformat Code would have
  caught it before any review.
- Import order fixed by hand in two specs: Optimize Imports.
- A block re-indented by a Python script after wrapping it in `try`: Reformat Code.

**Top-rung rounds** (level 3, Opus 5.5 max, three charters, two-phase). Tokens are per round, all three reviewers
together, phases 1 and 2. Rounds 3 and 4 ran before this context; their figures are in the session log, not here.
- **Round 5 (UI batch):** 1 MAJOR, an Ask cancelled before its coroutine ran left the router asking forever. The
  implementation charter rated it MAJOR; domain and security found the same and rated it MINOR. Also 14 MINOR.
  About 1.34M tokens.
- **Round 6:** clean, 10 MINOR. About 1.33M.
- **Round 8:** 1 MAJOR, a modal opened with a hash navigation closed at once. Domain rated it MAJOR, the other two
  MINOR. Also 8 MINOR. About 1.47M.
- **Round 9 (a delta):** clean, about 10 MINOR. About 0.87M.
- **Reading:** every MAJOR was seen by all three charters; only the severity differed. One Opus max per round would
  have found each defect, but whether it gets called MAJOR depends on the charter.

## Release

Not run before the meeting: 0.117.0 waits for the maintainer's `/release` and this month's Maven Central figures.
