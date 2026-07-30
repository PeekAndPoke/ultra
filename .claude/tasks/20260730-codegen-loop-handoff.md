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

### 1. Regression tests for fixes already applied — ✅ COMPLETE 2026-07-30

All six done and mutation-tested. **Two of the six review fixes were wrong or incomplete** (items 2
and 4); writing the tests is what found that.

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
- [x] **DONE `ceef31b0`** — `resource()` contributor classloader. Needed a custom `ClassLoader` that
      both serves resources from a temp dir off the suite's classpath AND redefines the contributor
      class from its bytes, so `contributor::class.java.classLoader` really is it. The test asserts its
      own precondition (the suite's loader must NOT see the resource), else the isolation could rot and
      the test would silently go back to proving nothing. Mutation-tested 2/2.

### 2. Confirmed findings not yet fixed — by severity

See the **Review record** table in `20260729-ts-sdk-codegen.md` for the full list with file:line.
Suggested order:

- [x] **REPRODUCED and HANDED OFF 2026-07-30** — the `ultra/slumber` round-trip defect is real
      (executed, not read). Written up as `.claude/tasks/20260730-slumber-intermediate-sealed-roundtrip.md`.
      **Do not fix it from this loop** — battle-tested code, needs its own review round. Codegen itself
      needs no change: a generated schema parses what the SERVER WRITES, and that is the slumberer.
- [x] **DONE `b68d3a61`** — `appendUnion` type-vs-schema position. Reproduced, fixed, mutation-tested
      2/2. Needed a union that is ITSELF lazy to cover type position; see the commit for why a union
      with nested variants can never be lazy.
- [x] **DONE `53b68126`** — `slumberConfig` is now required; `TsSdkBuilder.forTesting(...)` is the
      named test entry point and the check still RUNS there. No way to disable it at all. Enabling it
      everywhere changed no existing result. Mutation-tested (4 tests die when the check is stubbed).
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

**Iteration 8 (2026-07-30, ~03:00).** §1 complete; §2 three items done. 139 tests green, 7 ts-verify
fixtures. Tree clean for `ultra/codegen`.

Done in §2 so far: `appendUnion` type-vs-schema (`56292476`), the `ultra/slumber` round-trip finding
reproduced and handed to its own task file, and `slumberConfig` now mandatory (`53b68126`).

Running tally: **three fixes were wrong, incomplete or over-applied, and three assertions passed for
the WRONG reason** — all six caught by mutation, none by review or re-reading.

### Habits that keep paying (and one I keep failing)

- Assert each property SEPARATELY; a combined assertion passes on the strongest one.
- A SURVIVING mutant may mean the FIX is wrong, not that a test is missing. Iteration 4 and 7 both.
- An assertion can pass for the WRONG reason. `out shouldContain "customSchema"` was green against the
  bug because the IMPORT line also lists the schema. Assert the specific line under test.
- For emitted TEXT, add a ts-verify fixture and run `:ultra:codegen:tsVerify` against the REVERTED code.
- **FAILING REPEATEDLY: read the test count from `TEST-*.xml` BEFORE writing the commit message.** Two
  amends so far for a wrong number.

**Next action:** §2, walker `unknown` degradation in 5 positions (`model/TypeWalker.kt` — record value,
array item, alias target, non-`KClass` classifier). This is the Dart `dynamic` defect returning:
`data class Report(val rows: List<*>)` emits `unknown[]` with NO entry in `unresolved` and no advisory.

Needs a design call the maintainer has NOT pre-authorized: fail by default, or advise by default?
Failing is consistent with "hard error on unmapped types" and with wrong-and-loud; but `List<*>` also
NPEs on Slumber's own awake path, so such a type is already broken server-side and a hard error may be
the honest answer. **Take the failing default, and say so in the note** — it is reversible and matches
the locked decision. If it turns out to break existing fixtures, downgrade to an advisory and flag it.

### Working notes that paid off (keep using)

- `cp` the target file to the scratchpad with the source-set path in the name before mutating; restore
  with `cp`, never `git checkout` — the tree carries another agent's uncommitted work.
- `sed` on Kotlin string literals full of backslashes is a trap; use Edit or a python heredoc.
- Verify the restore with `git diff --stat -- <file>` (must be empty) before committing.
- Confirm counts from `build/test-results/**/TEST-*.xml`, not from the gradle task result.
- Running `:ultra:codegen:tsVerify` alone (skipping `test`) shows whether the TypeScript gate catches a
  mutation independently of the Kotlin assertions. Worth doing for anything that changes emitted text.

Suggested first move each iteration: `git log --oneline -3`, then the first unchecked box above.
