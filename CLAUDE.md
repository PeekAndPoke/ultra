# Project Rules for Claude

**We are explorers. Let's enjoy the journey!**

We're on a good track. Mistakes happen, we find them, we fix them, all is well. So: report what you
find plainly and move on. A defect found is a good day, not a confession — and the rules below exist
to help you catch things, not to be defended against. Say what you did, say what broke, fix it, keep
going. No hedging, no apologising, no bracing for impact.

This file is loaded automatically into every Claude Code session working on this repo. These are
the project's working rules.

## Plan & task maintenance

Plans and tasks live together in `.claude/tasks/`. A plan is just a longer-lived task doc that
other tasks reference from their `**Plan:**` header.

**When you complete work that matches an item in `.claude/tasks/*.md`, update that doc before
finishing the task.**

- Update status markers (TODO → DONE, add dates).
- Update gate status tables if present.
- Update the critical path section if an item unblocks.
- Archive completed docs to `.claude/tasks-archive/<YYYY-MM>/` — one folder per month, based on the
  date in the filename. Create the month folder if it does not exist.
- Do NOT create new plan docs unless explicitly asked — just update existing ones.

The goal is that anyone (human or agent) reading a doc sees the current state, not an outdated
snapshot. Stale plans are worse than no plans because they cause wrong priorities.

## Code style

- Use explicit imports in Kotlin — never wildcard/star imports. Wildcards cause real conflicts here
  (e.g. `input()` from KQuery vs `input` from kotlinx.html). Use an import alias for clashes.
- Never use fully qualified class names in Kotlin code — always add the import.
- **A KDoc `[Reference]` must resolve.** If it does not, either import the type or drop the brackets
  and use plain `code` formatting — an unresolved link is an IDE warning and a dead link in the docs.
  Same-package references resolve without an import, so do not add one for those.
- **Keep KDoc concise.** One-line summaries; add detail only where behaviour is genuinely non-obvious
  from the signature. Verbose KDoc drifts out of sync and becomes misleading.
- See `.claude/skills/code-style/` for the full style guide.

## Tools & packages

- Use `pnpm`, never `npm`, for JavaScript package management (`pnpm dlx`, not `npx`) — supply-chain
  preference, not a style one.
- Use "PeekAndPoke" or "peekandpoke" for branding, never "peek&poke".
- Spell "Klang Audio Motör" with the ö. Never "Motor".

## Sub-agent orchestration

- When fanning out work across sub-agents, pick model and effort per task — cheap tiers for
  retrieval/mechanical work, strong tiers for coding/verification; the coordinator stays on the
  user-selected model. See `.claude/skills/agent-fleet/` (provisional).

## Development workflow

- **Re-confirm the plan before implementing it.** Summarise what an open task/plan doc commits to —
  especially decisions that would be expensive to reverse — and get agreement before writing code.
  Detail is not agreement: a fully-designed plan was implemented and then thrown away on 2026-07-28
  because nobody re-checked whether its core premise was still wanted.
- **Every feature gets a task file** in `.claude/tasks/`, named `YYYYMMDD-<slug>.md` (copy
  `.claude/tasks/TEMPLATE.md`). Features usually come from plan phases — link the plan in the task.
- Lifecycle: implement → run `/feature-review` (mandatory: three charters — implementation & code
  style, domain expert, security) → fix confirmed findings → tests green → mark DONE and move the task
  file to `.claude/tasks-archive/<YYYY-MM>/` (filename is already dated).
- **The gate LOOPS — see `.claude/skills/review-loop/`.** A fix produced by a review is itself an
  unreviewed change, so rounds repeat until a clean one. The loop is built so it cannot flip-flop: round
  1 is blind, later rounds review BEFORE seeing the previous findings and then reconcile against them,
  and **a settled finding can only be reopened by naming what is factually wrong in the reason it was
  settled** — not by re-asserting it. Only CRITICAL/MAJOR loop; two rounds without a clean one means
  stop and ask.
- **When archiving a task, create a follow-up DOCS task if the change touched public API.** Docs are
  written against SETTLED code, not reviewed code — the user's own review comes after the gate, so
  documenting at review time just means rewriting. A tracked follow-up also stops "update the docs"
  depending on anyone remembering. Skip it for internals, tests and refactors.
- **Settled is a precondition, not just a preference. Code still in flux gets NO docs task yet** —
  research, prototypes, and anything under active redesign where the public surface is still moving.
  Documenting a shape that changes next week produces two costs and no benefit: the rewrite, and a
  reader who trusted the stale version. Ask "would I be surprised if this API changed next week?" —
  if not, skip the docs task and say so in the archive note. Revisit when it stabilises.
- **Security-critical features** get a follow-up red-team task (`YYYYMMDD-redteam-<slug>.md`) in
  `.claude/tasks/`, describing concrete break-in/attack scenarios to attempt. These are COLLECTED,
  not executed — dedicated penetration-test sessions sweep them later. Never run attack scenarios
  as part of normal feature work.
- **Adversarially verify every review finding against the code before acting on it**, and say when one
  does not survive. Reviewers have been confidently wrong; so has the coordinator. Where a one-command
  experiment settles it, run it. Record what was probed and stayed CLEAN as well as what was found —
  it stops the next session re-treading the same ground.

## Testing

- Addon tests live in each addon module's `src/jsTest/kotlin/`.
- Kraft core tests are in `kraft/core-tests/src/jsTest/kotlin/`.
- Test real browser behavior via `TestBed.preact { }` — don't mock what you can run.
- **All backend (JVM) code needs end-to-end tests**: boot the app via the funktor testing harness
  (`AppSpec`/`AppUnderTest` in `funktor/testing`) and exercise real endpoints, not just units.
  Storage-touching features must run against both DB backends (`MatrixTest2d` pattern).

### Verification traps

These make a suite look green while the feature is broken. Treat them as preconditions for saying
"tests pass", not as extras.

- **kotest ignores `--tests`.** Confirm a spec actually ran via `build/test-results/**/TEST-*.xml`,
  never by trusting a filtered gradle invocation. Avoid `--rerun-tasks` (kapt flakiness). MPP modules
  use `:jvmTest`; `funktor-demo:server` uses `:test`.
- **…but that XML mis-attributes WHICH test failed.** Counts (`tests`/`failures`/`errors`) are
  reliable; the `<testcase name>` a `<failure>` is nested under is not — it can name a test that
  cannot produce that failure, and it disagrees with the gradle console for the same run. Use the XML
  to confirm a spec ran and how many failed; use the console output to see which case broke.
  Confirmed twice on 2026-07-28 (`HelpersSpec`, `MonkoSlashKeyTest`).
- **Mutation-test every change that is security-relevant OR touches persistence** — storage,
  serialization, codecs, migrations, indexes, query building (maintainer, 2026-08-28). Deliberately
  narrower than the klang standard the review-loop skill was adopted from, which mutation-checks every
  test. Protocol in `.claude/skills/review-loop/`. A green test proves nothing until it has been RED for
  the right reason. **Persistence earns the same bar as security because it fails the same way: silently,
  and the damage is already on disk by the time anyone looks.** This repeatedly catches vacuous or right-for-the-wrong-reason
  tests, including ones written in the same session — a "single-use token" e2e also satisfied by a
  cooldown, and a `validate()` test that constructed the healthy object so it passed whether or not
  validation ran.
- **If a type is serialized in production, a test must serialize it.** Not a mutation rule, a coverage
  one, and it earns its own line: `VaultCollector.Data` held a type Slumber had no codec for, so the
  slice threw in a post-response coroutine and silently dropped the WHOLE record — `vault.entries` was
  `[]` across all 1193 depot records. The collector had tests and the API had tests; the one operation
  the data actually undergoes in production had none.
- **A surviving mutant is not always a missing test** — it can mean the FIX was wrong, the fixture
  cannot express the difference, or a comment claimed something false. Work out which before adding an
  assertion to make it die.
- **Back up with `cp` before mutating a file; never restore with `git checkout`** — the file usually
  carries other uncommitted work. **Keep the source-set path in the backup name.** Backing several
  files into one directory by `basename` silently collides in a multiplatform layout — `commonMain/
  strings.kt` and `jvmMain/strings.kt` flatten onto the same name, and the restore puts one source
  set's content into the other. Mirror the path (`cp x/commonMain/y.kt bak/commonMain__y.kt`) or back
  up one file at a time.
- **Module test tasks do not compile everything.** The root project has its own `src/jvmMain` that is
  not a module in `settings.gradle`, so `:some:module:allTests` never touches it — a change to
  `ultra/common` broke it on 2026-07-29 while every module suite stayed green. Before claiming a
  cross-module change is contained, run a compile sweep:
  `./gradlew compileKotlinJvm compileTestKotlinJvm compileKotlinJs compileTestKotlinJs compileKotlin
  compileTestKotlin --continue` and check for `^e:`. Native targets need their own
  `compileKotlinLinuxX64` and are not covered either.
- **A green compile sweep can be measured against STALE test classes.** After an ABI change to a
  published interface in `ultra/log`, Gradle held `:ultra:vault:compileTestKotlinJvm` UP-TO-DATE, so
  the sweep reported zero errors while the compiled test double still implemented the *old*
  signature. It surfaced only at runtime, as `AbstractMethodError` wrapped inside an
  `AssertionFailedError` — i.e. it looked like a logic bug, not a build problem. `touch` does not
  help (Gradle hashes content, not mtime). **Most likely cause: a concurrent build.** Another agent
  was building the same worktree at the time, and Kotlin's incremental state under `build/kotlin/`
  is not safe against two interleaved builds — so a task's snapshot can claim up-to-date while its
  outputs are not. This was not proven, only inferred; the observation itself is solid.
  Recovery: `rm -rf <module>/build/classes/kotlin/**/test` for every dependent module and re-run.
  Only then is "compiles clean" evidence. Observed 2026-07-30.
- **A grep for call sites misses receiver-less calls.** Searching `.observe(` will not find
  `observe(x) { }`, where the receiver is implicit — that is how the same break was missed twice.
  When removing an extension, prefer renaming it and compiling: the compiler finds every caller,
  a grep finds the ones you thought of.
- **…and a grep for implementors misses anonymous objects.** Sizing the `Log` interface change by
  grepping `: Log` found the named classes but not three `object : Log { }` literals inside a monko
  spec. They surfaced only after the stale test classes above were deleted and the module actually
  recompiled. Same rule applies: let the compiler enumerate implementors, and treat a grep-derived
  blast radius as a lower bound. Confirmed 2026-07-30.
- **`shouldBe` is untyped**, so `valueClass shouldBe "literal"` rots silently.
- **Never emit `\uXXXX` escapes or raw control characters in an edit** — they land as raw bytes, in
  file writes and match strings alike. Write `Char(0xNN)` instead, and pin the property that makes a
  code point special rather than one example of it.
- **Local DBs run in docker:** `docker start mongodb arangodb`. They match `application.test.conf`;
  Arango must already have `funktor-demo-test` (Karango creates collections and indexes, never a
  database — a missing one surfaces as an opaque Arango 1228).

## Documentation

- Library reference docs live in `docs-site/src/pages/ultra/*`.
- LLM-readable mirror templates live in `docs-site/src/data/llms/*.md` and `llms.txt` / `llms-full.txt` in the same dir.
  Version strings in those templates use `{{ultraVersion}}` / `{{kraftVersion}}` placeholders, substituted at build time
  by endpoints under `docs-site/src/pages/llms*.ts` (renderer: `docs-site/src/data/llmsTemplate.ts`).
  **Edit the templates when docs change — never edit `docs-site/public/` for LLM mirrors.**
- See `.claude/skills/docs-site/` for the docs workflow.

### Keep docs compact

- **Precise and concise beats comprehensive.** Overflowing docs are not useful — they hide the part the
  reader needed and they rot faster, because nobody re-reads 800 lines to check one claim.
- **When an entry is stale, REWRITE it rather than nudging it.** Appending a correction to a drifted
  section leaves both readings in the file and doubles its length. Delete and restate.
- One concept per page. If a page covers more than three or four topics, split it or cut it.
- The LLM mirror is a reference, not a transcript: signatures, defaults, gotchas, one good example
  each. It should be shorter than the pages it mirrors, not longer.
- Prefer deleting an example over adding a second one that makes the same point.

### Docs vs skills — the split

Three places could describe a library. Only two are allowed to, or they drift.

| Question | Lives in |
|---|---|
| "How do I use this?" — concepts, API, examples | docs site (`docs-site/src/pages/ultra/*`) and its LLM mirror (`docs-site/src/data/llms/*.md`) |
| "What will bite me?" — traps, invariants, scoping rules, known defects, repo-specific patterns | the library's skill in `.claude/skills/` |

- **Skills reference the docs; they never restate them.** For API reference, link
  `docs-site/src/data/llms/<lib>.md` instead of copying it.
- **Acceptance test for a line in a skill:** if it would be equally at home on a docs page, it belongs
  on the docs page, not in the skill.
- Skills therefore drift with CODE changes only, never with docs edits — a much smaller surface.
- **Cite `path/File.kt:line` for every concrete claim in a skill**, so a reader can verify it in one
  jump. Uncited claims are how these rot.
- The docs site and its LLM mirror are the pair that must move together; that rule is above.

## Releases

- Canonical version lives in `gradle.properties` → `VERSION_NAME`. Bumping it flows through all Gradle modules.
- Docs-site versions (`ultraVersion`, `kraftVersion` in `docs-site/src/data/site.ts`) must move in lockstep.
- README dependency snippet (`README.MD` ~line 123) has its own hardcoded version list.
- See `.claude/skills/release/` for the full release workflow and checklist.

## Project facts worth not re-deriving

- **Battle-tested:** Kontainer, Streams and Slumber — years in production. **Mutator is NOT**, despite
  its age and commit count. Git history is not maturity; defer to the maintainer on this.
- **Kontainer scoping:** a singleton that transitively injects a `dynamic` becomes `SemiDynamic`, i.e.
  one instance per request. Shared-state services must take zero constructor dependencies — and
  "constructor runs once at boot" is usually false for realms, which are rebuilt per request.
- **Known defect:** `ListMutator.subList()` throws `UnsupportedOperationException`
  (`mutator/core/src/commonMain/kotlin/ListMutator.kt:260`). Stdlib functions such as `chunked()` call
  it internally, so even indirect use crashes. Collection mutators are supposed to implement their
  `MutableList`/`Set`/`Map` contracts fully; this one is an outstanding gap.
- Before adding or bumping any dependency, **look up the newest version online** — never from memory.

## Key locations

- Active tasks and plans: `.claude/tasks/` (task template: `.claude/tasks/TEMPLATE.md`)
- Completed tasks and plans: `.claude/tasks-archive/<YYYY-MM>/` (grouped by month)
- Not-yet-scheduled ideas: `.claude/future-plans/`
- Skills: `.claude/skills/`
- Agents: `.claude/agents/`
