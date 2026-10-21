# Ultra's rules for the agent meeting, 2026-10-23

Mission Control's MC-00017. On Friday 23 October, after the 13:00 refill, the agents of all three projects go through
each other's rules to set a common baseline. Prepared on 2026-10-21 by Ultra-Main-Dev. Nothing here is changed yet:
the maintainer said "no trimming right now anywhere", and any change to `CLAUDE.md` needs the maintainer's OK in
ultra's session.

Each rule gets one line: what it says, why it exists, and my leaning going in. The leaning is not a decision.
Klang's file (klang `docs/housekeeping/2026-10-23-agent-meeting-rules.md`) is the model, and these are its leanings:

- **keep**: stays repo-wide.
- **area**: stays, but moves to the module, tier or skill it governs, with a pointer left behind.
- **merge**: one home with another rule.
- **slim**: keep the point, drop words.
- **ask**: the maintainer's call.
- **drop**: retire it.

A reason marked *(my reading)* is my understanding, not a recorded one.

**What makes ultra different:** it is a foundational library. Klang, Spross and outside users compile against it,
and a release to Maven Central can't be taken back. Most of ultra's rules come from that, from the testing traps it
hit, or from a review that missed something. The maintainer asked for ultra's own review thresholds; they are in
section 5.

## 1. `CLAUDE.md` (251 lines, 51 rules)

### Opening and tickets

| # | Rule | Why | Leaning |
|---|---|---|---|
| C1 | We are explorers: a defect found is a good day; report plainly, no hedging | Agents braced for blame hide or soften defects *(my reading)* | keep |
| C2 | Tickets in `docs/tasks/`, referenced by number, moved by `ticket.py` | A number survives a move; a path breaks (2026-10-21 migration) | keep, merge C19 |
| C3 | Work that matches a ticket updates it before finishing (status, gate tables, critical path, archive) | "Stale plans are worse than no plans: they cause wrong priorities" | keep, slim: the bullets restate `/project-layout`'s lifecycle |
| C4 | No new plan docs unless asked | Plans multiplied and drifted *(my reading)* | merge into C3 |

### Code style and tools

| # | Rule | Why | Leaning |
|---|---|---|---|
| C5 | Explicit imports, never wildcards; alias on a clash | Real conflicts: KQuery `input()` against kotlinx.html `input` | keep, merge with `/code-style` §2 (one home) |
| C6 | Never fully qualified names in Kotlin code | Readability; same rule as C5's family | merge with `/code-style` §1 |
| C7 | A KDoc `[Reference]` must resolve, or use plain code formatting | An unresolved link is an IDE warning and a dead link in the docs | area: `/code-style` |
| C8 | KDoc concise: one-line summaries | Verbose KDoc drifts and misleads | area: `/code-style` |
| C9 | `pnpm`, never `npm` (`pnpm dlx`, not `npx`) | Supply-chain preference | keep, merge with `/code-style` §4 |
| C10 | "PeekAndPoke"/"peekandpoke", never "peek&poke" | Branding | keep, merge with `/code-style` §3 |
| C11 | Spell "Klang Audio Motör" with the ö, never "Motor" | Branding of the sister project | **drop**: klang's own register (S9) now says "Klangmotor" with a plain o ("too much ego", 2026-08-25). Ultra's line is out of date and contradicts it |
| C12 | Pick model and effort per sub-agent; coordinator stays on the user's model | Cost against quality (2026-07-17) | keep as a pointer to `/agent-fleet` |

### Worktrees

| # | Rule | Why | Leaning |
|---|---|---|---|
| C13 | Work goes sequentially on the checkout IntelliJ has open | The IDE's index sees the work; "a steady sequential workload rather than spikes" (2026-10-20) | keep |
| C14 | A worktree only when needed, via `console/worktree-new.sh`, in `../ultra-worktrees/`; never `isolation: "worktree"`; remove when merged | A tree inside the repo doubles the IDE index | slim |
| C15 | In a worktree, build only through `console/wt-gradle.sh` | A bare `./gradlew` uses the shared test DBs and wipes another tree's data mid-run | area: the worktree scripts' header and `/project-layout` Repo notes |
| C16 | One full suite at a time, machine-wide; memory allows 2 to 3 builds | Machine memory *(my reading)* | merge with the BUILD-LOCK rule (see 3.2) |
| C17 | A track edits its own area; CLAUDE.md, skills, ULTRA-00028, CHANGELOG, BUILD-LOCK change on master only; never push | Shared files stay in one writer's hands | slim; "never push" goes repo-wide (3.1) |

### Development workflow

| # | Rule | Why | Leaning |
|---|---|---|---|
| C18 | Re-confirm a plan before implementing it; detail is not agreement | A fully designed plan was built and thrown away on 2026-07-28 | keep |
| C19 | Every feature gets a ticket with the TEMPLATE sections | One place for spec, ledger and history | merge into C2 |
| C20 | Lifecycle: implement, `/feature-review` (three charters), fix, tests green, `move DONE` | The gate is mandatory | keep; MC-00016's light lane (section 5) hooks in here |
| C21 | The gate loops; settled findings reopen only on a named factual error; 2-round valve | One-shot gates left confirmed findings with no path (2026-08-24) | slim to one pointer line: `/review-loop` is its home |
| C22 | Archiving a public-API change creates a follow-up DOCS ticket | Docs are written against settled code, and "update the docs" needs a tracked owner | area: `/feature-review` "On PASS" (already there) |
| C23 | Code still in flux gets no docs ticket yet | Documenting a shape that changes next week costs twice | merge into C22 |
| C24 | Security-critical features get a "Red team: <feature>" ticket; collected, never executed in feature work | Attack scenarios need their own sessions | area: `/feature-review` (already there) |
| C25 | Adversarially verify every review finding; record what stayed CLEAN | Reviewers and the coordinator have been confidently wrong | keep repo-wide; it is also stated in `/review-loop` step 4 and `/feature-review`: one home, two pointers |

### Testing

| # | Rule | Why | Leaning |
|---|---|---|---|
| C26 | Addon tests in `src/jsTest/kotlin/`; Kraft core tests in `kraft/core-tests` | Where things are | area: a Kraft skill (ULTRA-00134, library know-how skills, NOT STARTED) |
| C27 | Test real browser behaviour via `TestBed.preact { }`; don't mock what you can run | Mocks pass where the browser fails *(my reading)* | area: Kraft skill |
| C28 | All JVM backend code needs e2e tests via `AppSpec`/`AppUnderTest`; storage on both DB backends (`MatrixTest2d`) | Unit-green, endpoint-broken features *(my reading)*; a missing e2e test is a gate failure | keep, but scope it to funktor and backend modules |

### Verification traps

| # | Rule | Why | Leaning |
|---|---|---|---|
| C29 | kotest ignores `--tests`; confirm a spec ran via the XML | A filtered run looked green while the spec never ran | area: testing skill (new, see section 6) |
| C30 | The XML mis-attributes which test failed; counts are reliable | Confirmed twice, 2026-07-28 | area: testing skill, merge with C29 |
| C31 | Mutation-test every security-relevant or persistence change | Toothless guards shipped repeatedly; persistence fails silently, like security | area: strict tier (section 5); its home is `/review-loop` Standard 2, which already restates it |
| C32 | If a type is serialized in production, a test must serialize it | `VaultCollector.Data`: every one of 1193 records silently lost its vault entries | area: strict tier (persistence) |
| C33 | A surviving mutant is not always a missing test | The fix, the fixture or a comment can be what is wrong | drop from CLAUDE.md: `/review-loop` Standard 2 says it word for word |
| C34 | Back up with `cp` before mutating, keep the source-set path in the name; never restore with `git checkout` | `git checkout` loses other uncommitted work; a `basename` backup swaps commonMain and jvmMain | merge into `/review-loop` protocol step 4 |
| C35 | Module test tasks don't compile everything; run the compile sweep for cross-module changes | The root `src/jvmMain` broke on 2026-07-29 while every module suite was green | area: testing skill |
| C36 | A green sweep can rest on stale test classes; delete `build/classes/kotlin/**/test` and re-run | `AbstractMethodError` at runtime after an ABI change (2026-07-30), likely a concurrent build | area: testing skill, merge with the BUILD-LOCK rule's reason |
| C37 | A grep for call sites misses receiver-less calls: rename and compile | The same break was missed twice | area: testing skill, merge with C38 |
| C38 | A grep for implementors misses anonymous objects | Three `object : Log { }` in a monko spec (2026-07-30) | merge into C37: "the compiler enumerates, a grep is a lower bound" |
| C39 | `shouldBe` is untyped | `valueClass shouldBe "literal"` rots silently | area: testing skill |
| C40 | Never `\uXXXX` escapes or raw control characters in an edit; write `Char(0xNN)` | They land as raw bytes; ULTRA-00112 and ULTRA-00114 still carry two NUL bytes | keep: it is about the agent's edit tool, so it applies to every file |
| C41 | Local DBs in docker; Arango needs `funktor-demo-test` to exist | A missing DB surfaces as an opaque Arango 1228 | area: testing skill |

### Documentation, releases, facts, locations

| # | Rule | Why | Leaning |
|---|---|---|---|
| C42 | Library docs live in `docs-site/src/pages/ultra/*` | Where things are | area: `/docs-site` (already there) |
| C43 | Edit the LLM mirror templates, never `docs-site/public/`; versions are placeholders | `public/` stopped being authoritative | area: `/docs-site` and `/release` (both already say it) |
| C44 | Keep docs compact (5 bullets) | Long pages hide the part the reader needed and rot faster | area: `/docs-site` (lines 115-118 already say it) |
| C45 | Docs vs skills: "how do I use it" goes in docs, "what will bite me" in skills; skills cite `path:line` | Three places describing one library drift | keep, slimmed to three lines; the table moves to `/skill-builder` |
| C46 | Version in `gradle.properties`; `site.ts` and the README snippet move in lockstep | A release touches three places | area: `/release` (already there) |
| C47 | Battle-tested: Kontainer, Streams, Slumber. Mutator is NOT | A fresh agent reads commit count as maturity | keep: the maintainer's judgement, needed by every reviewer |
| C48 | Kontainer scoping: a singleton injecting a `dynamic` becomes per request | Shared-state services silently lose their state | area: a kontainer skill (ULTRA-00134) |
| C49 | Known defect: `ListMutator.subList()` throws; `chunked()` calls it | It bites in forms | **ticket** it, one line in a mutator skill; a known defect belongs in a ticket, not the rules |
| C50 | Look up the newest dependency version online, never from memory | Training data is out of date | keep |
| C51 | Key locations | Orientation | slim: `/project-layout` holds the layout |

**If the leanings hold:** about 15 repo-wide rules and a short pointer block (review loop, testing skill, docs,
release, code style, library skills). That is roughly 250 lines down to 100, and no rule loses any force: each
moves to where the work that needs it happens.

## 2. Rules in skills and agent files

### `/review-loop` (447 lines)

Its own changelog (2026-09-28, at 416 lines) says "next substantive change should shorten rather than extend". It
has grown since.

| Rule | Why | Leaning |
|---|---|---|
| Round 1 blind; later rounds two-phase with the same agent continued | Fresh eyes without re-arguing settled findings | keep |
| A written ledger with a Reason column, before the next round | A ledger held in memory doesn't survive a compaction, and a reviewer who can't see a reason must withdraw | keep |
| Dispositions: fix, reject (checkable), maintainer-decision, out-of-scope with a follow-up, withdrawn | Each closed a hole seen in practice (2026-09-28) | keep |
| A severity downgrade needs a checkable reason | A downgraded MINOR was batched and never applied for a month | keep |
| Rejecting your own code's CRITICAL/MAJOR is provisional until a fresh reviewer accepts it | The flat ban produced zero rejections and an unused audit path (maintainer, 2026-09-28) | keep |
| Re-derive the change set each round | A fix that adds a file would ship unreviewed | keep |
| Rejections loop too | Otherwise rejecting is cheaper than fixing | keep |
| Termination: clean round, a maintainer decision on CRITICAL/MAJOR, a wall, or the 2-round valve (re-arms) | Klang's five rounds were mostly churn | keep |
| MINORs: a batch with no re-review (step 3) against "a list, never a batch" (termination) | Both were written after the lost MINOR | **merge**: the two contradict each other. My RFC answer to MC proposed a batch plus a scoped one-reviewer delta check |
| Fresh agents each round; comment findings only when factually wrong; one scale CRITICAL/MAJOR/MINOR; never drop a finding | Anchoring; prose churn; a reported HIGH was silently dropped | keep; the scale is stated in four files, so give it one home |
| `.claude/**` and CLAUDE.md get the full review set | They govern every review | **ask** (see section 4): it has kept two rule fixes waiting since 2026-10-17 |
| Missing e2e test is a gate failure | Same as C28 | drop here: pointer |
| A claim about existing code is read before it is written anywhere | Klang, three recurrences | keep |
| A correction closes only by a repo-wide grep, then one home and pointers | Corrections fixed in one file and not the others | keep |
| Unifying two paths defuses tests outside the diff | *(from klang)* | keep |
| A replaced expression lists every old clause | Klang, twice: an implicit floor dropped | keep |
| A rule with a closed list is reviewed by table | Klang: one table found what three prose rounds missed | keep |
| A reported fix is not a landed fix: re-read what was wrong | A small regression hides in a clean round | keep |
| Every "green" names its run | Runs older than the last edit backed claims | keep |
| The effort ladder: reviewer-high, -xhigh, -top | More scrutiny for a smaller target | keep; MC-00016 adds the light lane below it |
| The SURPLUS question in every brief | Klang: it shrank a step by a quarter | keep |
| Standard 2 scope: security and persistence, mandatory; elsewhere encouraged | Maintainer, 2026-08-28: narrower than klang on purpose | keep: it is the strict tier |
| Mutation protocol: green, one mutant in the code, red for the right reason, restore with `cp`/`cmp`, report one line | Kotest and XML traps make a false red easy | keep |
| A scripted runner treats "No tests found" as an error | Klang logged three spurious REDs | keep |
| A takeover worker `cmp`s the predecessor's files first | An agent can stop with a mutant applied | keep |
| A test never derives its expectation from the constant it guards; a mapping table is guarded by its full literal map | Klang's worst survivor followed a 60x widening | keep |
| No value-echo tests; "X is untested" must name a failure reading can't catch | Change-detectors guard nothing | keep |
| Not a retrofit mandate | Scope control | keep |
| Standard 3: classify every escape and close it once; the escape ledger | Improvement without experiments | keep; the ledger has 2 rows, both still OPEN |
| Gotchas: read the lock as its own step; `git diff <path>` before `git commit -- <path>`; scripted renames know words; background Gradle dies under memory pressure | One incident each | keep; the lock line merges with the BUILD-LOCK rule |

### `/feature-review` (129 lines)

| Rule | Why | Leaning |
|---|---|---|
| Preconditions: scope by file set; e2e present or STOP; load the ticket; read the lock | Interleaved commits; e2e is a gate failure | keep |
| The three charters: implementation and style, domain, security | Each caught what the others didn't *(my reading)* | keep for strict and standard; the light lane uses one |
| Ask every charter the SURPLUS question; "NO FINDINGS" is valid; don't spawn sub-agents | Padding and orphaned agents | keep |
| Scale CRITICAL/MAJOR/MINOR, no HIGH | Same as `/review-loop` | merge: pointer |
| The record is written after every round | Same as `/review-loop` | merge: pointer |
| On PASS: DONE, docs ticket, red-team ticket | Steps that were skipped | keep (CLAUDE.md C22-C24 move here) |
| Out-of-scope findings fail the gate until their ticket exists | A confirmed CRITICAL had nowhere to go (2026-08-24) | keep |
| Scoped means start from the diff, then follow its dependencies | The insights gate's worst findings were outside the diff | keep |

### `/agent-fleet` (193 lines)

| Rule | Why | Leaning |
|---|---|---|
| Coordinator on the user's model; workers on the cheapest tier that does the task well | Cost against quality | keep |
| Tier table: opus codes, sonnet retrieves, haiku counts | Set by the maintainer, 2026-07-17 | keep |
| Reviews use the ladder, never sonnet | Correctness-critical | keep |
| Set `model` explicitly; state the fleet mapping in one line | So the maintainer can correct it | keep |
| The coordinator owns the build; workers run no Gradle; one owner mutates production code | Stale classes; a build racing a restore gives a wrong verdict silently | keep |
| Advisory `BUILD-LOCK.md`, read as its own step | Concurrent Gradle corrupts incremental state | keep; see 3.2 for its missing CLAUDE.md line |
| Cheap finders feeding an expensive verifier; escalate, don't patch; no haiku for unchecked output | Fan-out multiplies cost | keep |
| Give workers the constraints | Fresh reviewers "fix" deliberate decisions | keep |
| Workers don't fan out; cap heavy agents at 2 to 3; retry the one agent | Orphaned grandchildren (2026-07-20), though "causation is not proven" (flaky network) | keep the first and third; **ask** on the cap: test it once |
| No two workers on one file: "partition by file, or use `isolation: \"worktree\"`" | Interleaved edits | **slim**: drop the isolation half, it contradicts CLAUDE.md C14 |
| Every brief opens with the bar ("world-class", plus "a defect found is a good day") | Klang's theory, adopted 2026-09-28 | **drop or measure**: ultra's defect-density ledger has no rows. The skill says the line goes if the density doesn't move, and here nobody has measured it |

### `/code-style`, `/docs-site`, `/release`, `/project-layout`

| Rule | Why | Leaning |
|---|---|---|
| `/code-style` §1-4: no FQCN, explicit imports, branding, pnpm | Same as C5, C6, C9, C10 | keep here as the one home; CLAUDE.md keeps a one-line pointer |
| `/docs-site`: the constants file is CRITICAL; page conventions; compact pages | Version drift; one look across pages | keep |
| `/release`: never push or publish without explicit confirmation; never edit `public/` or the llms templates for versions; never skip verification; never amend the release commit; never `--no-verify` | Maven Central is irreversible | keep |
| `/project-layout`: the blueprint plus 8 Repo notes | Common layout across projects | keep; the note "Status lines are not converted yet" is stale since 2026-10-21 (fixed when ULTRA-00232 closes) |
| `/project-layout`: "Memory lives in the repo, never the home directory" | Home memories are invisible to others | **ask**: ultra keeps 3 home memories (section 3.3) |

### Agent definitions

| Rule | Why | Leaning |
|---|---|---|
| `reviewer-standing-rules.md`: read-only (tools pinned), severity scale, cite `path:line`, comment findings only when wrong, don't "fix" a recorded maintainer decision, a self-derived expectation is a finding, phase-2 duties (a) to (c) | One home for the three ladder agents; copies had drifted | keep; `/review-loop` could point here for the phase-2 duties instead of restating them |
| `reviewer-high/-xhigh/-top.md`: model and effort only | The ladder | keep; rename to model+effort names (RFC answer, agreed) |
| `developer-advocate.md` (139 lines) | A docs-and-visibility persona *(my reading)* | **ask**: is it still used? |

### Procedure skills (no rules of their own)

`/update-kraft-examples`, `/karakum-setup`, `/six-hats`, `/skill-builder` (313 lines) are how-tos. Leaning: keep.
**Ask** whether `/six-hats` and `/skill-builder` are still used; together they are about 590 lines.

## 3. What I carry that is written nowhere, or only in the wrong place

These reach me through this session's summaries as "standing constraints", from the maintainer's words earlier in a
session or from the harness. Klang described the same thing: carried items harden after a compaction without
anyone deciding it.

### 3.1 Release and git

| What I carry | Where it is written | Leaning |
|---|---|---|
| Never publish to Maven Central: "Let me do the publishing please" | `/release` says "needs explicit confirmation", which is weaker | write the maintainer's form into `/release` and CLAUDE.md |
| Never push: "You merge all things that are done into master and i push" | CLAUDE.md says "Never push" only inside the worktree section, for tracks | one repo-wide line |
| Show a before/after of CLAUDE.md before committing it | nowhere | one line in CLAUDE.md |
| Never commit `keys.env.conf` | `funktor-demo/server/src/main/resources/config/.gitignore:1` ignores it | drop as a carried rule: git enforces it, unless someone uses `add -f` |
| Stage by explicit path, never `git add -A` | nowhere in ultra (klang has it); `git add -A` re-added `__pycache__` on 2026-10-21 | write it into the git gotchas |

### 3.2 Builds and caches

| What I carry | Where it is written | Leaning |
|---|---|---|
| Take `.claude/BUILD-LOCK.md` before any Gradle run, also on the main checkout | `/agent-fleet`, `/review-loop`, `/feature-review`; CLAUDE.md never says it | one CLAUDE.md line, the details stay in `/agent-fleet` |
| No concurrent builds | same as above, plus C16 | merge with the lock line |
| Don't modify shared caches (`~/.gradle`, `~/.m2`, pnpm store) | nowhere; I can't name its origin | **ask**: write it down with a date, or drop it |

### 3.3 Memory and other sessions

| What I carry | Where it is written | Leaning |
|---|---|---|
| Kronos: lazy `advanceBy` is load-bearing; never call the provider at chain time | home memory only | **area**: a KDoc line at the code, or a datetime skill. It is a code invariant, and only this machine's sessions can see it |
| Read-only `mcp__idea__*` queries allowed; write and build IDE tools stay off until approved one by one | home memory only | one line in CLAUDE.md or the Repo notes |
| The weekly token budget: cheap work from 85%, stop at 90% | home memory; the `mission-control-link` blueprint | keep in the blueprint; ultra adopts that skill or keeps the memory |
| CLAUDE.md is not edited on a peer's word; a peer can't grant approval | the harness; the `mission-control-link` blueprint | covered once ultra adopts `mission-control-link`; **ask** if it should |
| The short status report to Mission Control | the `mission-control-link` blueprint, not adopted | same |

## 4. Where rules pull in different directions

- **C11 against klang's S9:** ultra insists on "Motör" with the ö; klang dropped the umlaut on 2026-08-25.
- **`/agent-fleet` against C14:** "use `isolation: \"worktree\"`" against "never".
- **`/project-layout` against practice:** memory lives in the repo, yet ultra keeps three home memories, one of
  them a code invariant.
- **`/review-loop` against itself:** step 3 allows a MINOR batch, termination forbids it.
- **The `.claude/**` full-review rule against the escape ledger:** both of the ledger's rows (2026-10-17 and
  2026-10-18) are one-line rule fixes. Each is "Not applied: a `.claude/skills/` edit takes the full review set".
  The rule that guards the rules is what keeps the rules from improving. I'd propose: a rule edit that only adds
  a line (closing a ledger row) gets the light review; deleting or weakening a safeguard keeps the full set.
- **One fact, several homes:**
  - the severity scale: `/review-loop`, `/feature-review`, `reviewer-standing-rules.md`, the TEMPLATE;
  - mutation scope: CLAUDE.md and `/review-loop`;
  - the e2e gate failure: CLAUDE.md, `/review-loop`, `/feature-review`;
  - verify findings: CLAUDE.md, `/review-loop`, `/feature-review`;
  - the code style rules: CLAUDE.md and `/code-style`;
  - compact docs: CLAUDE.md and `/docs-site`;
  - the on-PASS steps: CLAUDE.md and `/feature-review`.

  `/review-loop`'s own rule is "give the full statement ONE home, and make every other copy a pointer", and the
  rules files don't follow it yet.
- **The opening-line bar is a theory with no data here.** It is in every brief, and its ledger is empty.

## 5. Review thresholds for ultra

Ultra is the special case (maintainer, 2026-10-21): "a foundational library so it needs different threshold than
Klang or Spross". A defect here ships into every consumer, and a published release can't be withdrawn.

**Strict tier.** Full three-charter gate, the ladder, mutation checks mandatory:

- security: `funktor/auth/**`, `funktor/rest/**` ACL and auth phases, `ultra/security/**`, sessions, tokens,
  tenancy;
- persistence and formats: `ultra/slumber/**` (codecs), `ultra/vault/**`, `karango/**`, `monko/**` query building
  and indexes, any `**/db/**`, migrations;
- code generators: `ultra/codegen/**`, `*/ksp/**`, because their output lands in consumers' code;
- `funktor/cluster/**` locks and workers;
- `ultra/kontainer/**` scoping and wiring: one change can turn a singleton into a per-request instance;
- the build: `build.gradle*`, `buildSrc`, `gradle.properties`, publishing config.

**Standard tier.** Everything else in production code: the three charters per "scope by risk", mutation at
judgement.

**Light lane (MC-00016).** One reviewer (`reviewer-high`, the charter that fits), one round, a mutation check only
if the change touches logic. Only if all of these hold:

- no strict-tier file;
- at most **50 changed production lines** (tests and docs not counted);
- no boundary change: no signature or documented behaviour visible to consumers, no schema, no new dependency, no
  config or build file. A fix that makes code do what its KDoc already says is not a boundary change. In a library
  almost every file is public API, so without this reading nothing would qualify;
- no changed or removed tests (new tests are fine).

**No review.** Docs or typos only: the checks run (`ticket check`, docs-lint, a docs-site build when pages
changed).

**`.claude/**` and CLAUDE.md:** a pure addition of up to about 10 lines goes through the light lane, for example
closing an escape-ledger row. Deleting or weakening a safeguard keeps the full set. **Ask.**

The full review is always available; I'd choose it whenever I'm unsure which tier a file belongs to.

## 6. Proposed outcome, for the discussion

1. CLAUDE.md as a register of about 15 repo-wide rules plus pointers, roughly 100 lines.
2. One new `testing` skill holding the verification traps (C29, C30, C35-C39, C41) and the Kraft test notes
   (C26, C27), until ULTRA-00134's library skills exist.
3. Module facts (C48 kontainer scoping, C49 the ListMutator defect) go to ULTRA-00134's library skills, and C49
   becomes a ticket.
4. One home per fact (section 4), with pointers elsewhere.
5. The four contradictions in section 4 resolved: drop C11, slim `/agent-fleet`'s isolation line, decide on home
   memory, merge the MINOR rule.
6. The carried items in section 3 each written down with a date or dropped.
7. The light lane and the ultra thresholds of section 5, including the proposal for rule edits.
