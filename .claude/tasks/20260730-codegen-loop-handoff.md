# Loop handoff — ts-sdk-codegen autonomous run

**Read this file FIRST each iteration. Do the next unchecked item. Update this file LAST, before
finishing the iteration.** Keep it short — it is read every loop, so it must stay cheap.

Full context lives in `.claude/tasks/20260729-ts-sdk-codegen.md` (design, locked decisions, and the
**Review record** listing every confirmed finding). Do NOT read that whole file every iteration —
grep the section you need. The Vue follow-on is `20260730-frontend-sdk-vue-contributors.md`.

## Standing authorization (maintainer, 2026-07-30)

| Question | Answer |
|---|---|
| Modify `funktor/rest`? | **Yes, but SCOPED TO THE CODEC CONFIG ONLY** — `FunktorRestBuilder.slumberModules(...)` and `instance(codecConfig)`. Nothing else in that module |
| Commit? | **Yes**, on `auth-increments`. **Explicit paths only — never `git add -A`.** Another agent works in `ultra/log`; check for `.idea/*` strays before every commit |
| `slumberConfig` API | **Add a test-friendly entry point** — make the parity check the default and name the unsafe path at the call site |
| Scope ceiling | **Continue into the new plan and/or Phase 2, whichever is more streamlined** |
| Push / PRs / anything outward-facing | **NO.** Never while unattended |

## Rules that bite here (learned the hard way, do not rediscover)

- **A fix without a failing-before test is not done.** Mutation-test every fix: break it, confirm red.
- **kotest ignores `--tests`** — confirm a spec ran via `build/test-results/**/TEST-*.xml`; use the
  console to see WHICH case failed (the XML mis-attributes that).
- **Module test tasks do not compile everything.** Before claiming a cross-module change is contained:
  `./gradlew compileKotlinJvm compileTestKotlinJvm compileKotlinJs compileTestKotlinJs compileKotlin
  compileTestKotlin --continue` and grep `^e:`.
- **Verify every claim before acting on it.** The review round had a finding that was simply wrong
  (`.bufferedReader()` charset) and it fooled three reviewers plus the coordinator because it was
  written down with a line number. A citation is not a verification.
- Explicit imports, no FQNs, KDoc `[refs]` must resolve, `Char(0xNN)` never `\uXXXX`.

## Backlog — in order

### 1. Regression tests for fixes already applied (NOTHING ELSE UNTIL THIS IS DONE)

These landed in the review round with **no tests**. Each needs a test that fails without the fix.

- [x] **DONE `ef5eba72`** — `tsStringLiteral` / `tsPropertyName`. `TsLiteralsSpec` (round-trip property
      through a strict JS decoder), `FxQuoted` fixture, and the fixture wired into ts-verify so `tsc`
      and `zod` check the hostile output for real. Mutation-tested 5/5.
      **Reusable technique:** a round-trip against a strict decoder beats one assertion per character —
      it fails for characters nobody thought to list. Worth copying for the remaining items.
- [x] **DONE `2623a08d`** — `TypeId` type-argument nullability. The open question resolved to: **the
      review fix was incomplete.** `canonicalKey` was fixed but `TsNames.of` was not, so the two
      declarations existed and then competed for one const. `TsNames.of` now reads arguments off the
      `KType` and appends `OrNull`; `TypeId.typeArguments` removed (no callers, and it cannot express
      nullability). Mutation-tested 2/2.
      **Lesson worth carrying:** asserting the three properties SEPARATELY is what exposed this — only
      the name assertion was red. A single "two declarations exist" test would have passed and hidden it.
- [x] **DONE `3339e2ca`** — claimed polymorphic child in `usedClaims`. Asserted at two levels: the
      registry entry (`TypeWalkerSpec`) and its observable consequence, the emitted import plus the
      variant rendering by its claimed name (`TsModelEmitterSpec`). Mutation-tested; restoring the early
      return kills both.
- [x] **DONE `a5182d35`** — `declareUnion` root-parent hop. A surviving mutant showed **half the
      review fix was wrong**: discriminator hops to the root (the server writes what
      `createParentSlumberer` decides), variants do NOT (a field typed as an intermediate sealed class
      cannot hold the root's other children, and widening emitted unreachable declarations). New
      `FxDeepRoot` fixture; plus a general invariant test that every union agrees with its variants on
      the discriminator field. Mutation-tested 2/2.
      **Lesson:** a mutant that survives is not always a missing test — here it meant the FIX was
      wrong. Check which before adding an assertion to make the mutant die.
- [x] **DONE `b44b7549`** — `appendAlias` recursion. `FxIds`/`FxIdHolder` fixture; targeted assertion,
      the general "no const referenced before declaration" invariant extended with this shape (it kills
      the mutant on its own), and an `idHolder` ts-verify fixture. Confirmed by execution that the
      eager form is TS2448 in real `tsc`, not just a different emitted string.
- [ ] `resource()` contributor classloader — fixture must be loader-observable, else it passes either
      way. Load the contributor via a child `URLClassLoader` whose resources ultra:codegen cannot see.
      This also fixes the false clue at `sdk/ThirdPartyContributorSpec.kt:128`.

### 2. Confirmed findings not yet fixed — by severity

See the **Review record** table in `20260729-ts-sdk-codegen.md` for the full list with file:line.
Suggested order:

- [ ] **NEW, found in iteration 4 — a defect in `ultra/slumber`, NOT in codegen.** Slumber cannot
      round-trip an intermediate sealed class when the custom discriminator lives on the root:
      `createParentSlumberer` hops via `getParent` and WRITES `kind`
      (`builtin/polymorphism/Polymorphic.kt:122-124`), while `createParentAwaker` does NOT hop and
      READS `_type` (`:104`). So slumber-then-awake of a `Middle`-typed value fails.
      **Evidence level: verified by reading both functions, NOT executed.** Prove it with a round-trip
      test before acting. Battle-tested code and out of scope for the codegen backlog — if real, it
      needs its own task and its own review, per the repo rules on `ultra/slumber`.
- [ ] **NEW, found in iteration 3 (not from the review).** `TsModelEmitter.appendUnion` builds
      `variants` once via `renderer.nameOf(it)` and uses that list in BOTH type position
      (`export type X = A | B`) and schema position (`z.union([A, B])`). For a declaration those
      coincide — the zod pattern exports a const and a type under one name — but for a CLAIM whose
      `tsName` differs from its `schema` they do not. `JsonPrimitive` is claimed as tsName
      `string | number | boolean | null` with a separate schema expression, so a polymorphic variant
      claimed that way would emit `z.union([string | number | boolean | null])`, which is not valid.
      Verified by reading; NOT yet reproduced with a test — do that first, it may be unreachable if a
      claimed union variant cannot occur. Fix is to use `schemaNameOf` in schema position.
- [ ] `slumberConfig` — parity check on by default + named test entry point (authorized above)
- [ ] Walker `unknown` degradation in 5 positions → a `TypeModel` channel + advisory, fail by default
- [ ] Name-collision check must include claimed `tsName`s (and `nameCollisionProblems` /
      `danglingReferenceProblems` have ZERO tests — add them)
- [ ] `KotlinxJsonTsContributor` drift test — 5 claims, standing rule, currently unprotected
- [ ] Tautological assertion `ts/TsModelEmitterSpec.kt:62` + neighbours that pass when absent
- [ ] `MpDateTimeFieldParitySpec:108` compares the contributor against a copy of itself
- [ ] `JsonElement` → `z.unknown()` should reach the advisory list
- [ ] Generic sealed hierarchy loses payload type (`createBareType()` for variants) — needs design
- [ ] `@Slumber.Field` non-ctor props emitted required — ties to request-vs-response shapes
- [ ] `@Slumber.Field` selection re-derived from `DataClassSlumberer` — needs a slumber-side API
- [ ] Scalar refinement (`Char` → `z.string().length(1)`, integral bounds)
- [ ] `readArrayElements` duplicated verbatim in ultra/slumber

### 3. Then: Phase 2 / the codegen additions

Pick whichever is more streamlined at the time. Phase 2 = `funktor/codegen`
(`RestApiTsContributor` + `sdk:ts:generate` CLI), built **profile-shaped** from day one (see §2.1).
Its prerequisite is the codec-config work, now authorized.

## STOP conditions — end the loop when any is true

- Backlog §1 and §2 are done and `./gradlew :ultra:codegen:check` is green, **and** the next item
  needs a design decision the maintainer has not pre-authorized.
- Three consecutive iterations produce no committed progress.
- Anything requires pushing, a PR, or touching `funktor/rest` beyond the codec config.
- The compile sweep goes red in a module nobody in this task touched.

To stop: call `ScheduleWakeup(stop: true)` and leave the final note below.

---

## Note to next loop

**Iteration 5 (2026-07-30, ~02:30).** Backlog §1 items 1–5 done (`ef5eba72`, `2623a08d`, `3339e2ca`,
`a5182d35`, `b44b7549`). 135 tests green (was 120 at review end), 7 ts-verify fixtures. Tree clean for
`ultra/codegen`.

**Two of the five review-round fixes verified so far were wrong or incomplete** (items 2 and 4). That
is the expectation, not the surprise. Assert each property separately, and when a mutant survives,
first ask whether the FIX is wrong before adding an assertion to kill it.

**ONE §1 ITEM LEFT — and it is the one most likely to be wrong**, because its current test passes
BECAUSE of the old bug. Do not accept a green run as evidence there; the fixture has to be genuinely
loader-isolated or the test proves nothing either way.

Two findings from iterations 3 and 4 are logged at the top of §2, both marked with their evidence
level. Neither has been reproduced. Reproduce before fixing.

**Pattern worth repeating:** for anything that changes emitted TEXT, add a ts-verify fixture and then
run `:ultra:codegen:tsVerify` alone against the REVERTED code. Twice now that turned "the string looks
right" into "a real compiler rejects the alternative" — escaping (unterminated literal) and alias
recursion (TS2448).

Do NOT re-fix the `.bufferedReader()` charset non-bug — it was a wrong finding; see the Review record.

**Do not touch these — another agent owns them:** `ultra/log/**`,
`.claude/tasks/20260729-log-scan-findings.md`, `.claude/tasks/20260729-redteam-log-forging.md`.
`.idea/compiler.xml` is modified in the tree and is NOT ours to commit — leave it.

**Next action:** backlog §1 item 6 — `resource()` contributor classloader, the LAST §1 item.

The trap: `ThirdPartyContributorSpec` currently loads `money.ts` from
`ultra/codegen/src/test/resources/`, i.e. from ultra:codegen's OWN loader — the very loader the old
buggy code used. So it passes with or without the fix and proves nothing. A green run is not evidence.

To make it observable, the contributor class must be loaded through a classloader whose resources
ultra:codegen's loader cannot see. Sketch: write a `.ts` to a temp dir, build a
`URLClassLoader(arrayOf(tempDir.toURI().toURL()), null)` — note the **null parent**, so it does not
delegate — load a contributor class through it, and assert `resource()` finds the file. Verify the
test FAILS with `scopeFor(name)` (no loader argument) before believing it.

If that proves impractical inside one Gradle test JVM, say so in the note and move to §2 rather than
leaving a test that cannot fail — the current one is worse than none, because it claims coverage.

### Working notes that paid off (keep using)

- `cp` the target file to the scratchpad with the source-set path in the name before mutating; restore
  with `cp`, never `git checkout` — the tree carries another agent's uncommitted work.
- `sed` on Kotlin string literals full of backslashes is a trap; use Edit or a python heredoc.
- Verify the restore with `git diff --stat -- <file>` (must be empty) before committing.
- Confirm counts from `build/test-results/**/TEST-*.xml`, not from the gradle task result.
- Running `:ultra:codegen:tsVerify` alone (skipping `test`) shows whether the TypeScript gate catches a
  mutation independently of the Kotlin assertions. Worth doing for anything that changes emitted text.

Suggested first move each iteration: `git log --oneline -3`, then the first unchecked box above.
