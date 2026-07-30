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
- [x] **DONE `efe2b6ad`** — walker `unknown` degradation. New `TypeModel.Undetermined(path, reason)`
      channel, all five sites recorded, promoted to a BLOCKING problem. `Any` included deliberately.
      `undeterminable()` returns rather than throws so one run reports every bad position.
      `claims.opaque` stays the loud escape hatch. Mutation-tested 2/2.
- [x] **DONE `d605b996`** — collision check now counts claimed names (only those with an `importFrom`,
      mirroring `appendImports`). Both previously-untested validator checks are now pinned, including
      `danglingReferenceProblems` via a hand-built `TypeModel`. New `model/other` fixture package for
      the cross-package name clash. Mutation-tested 4/4.
- [x] **DONE `3bc64c41`** — kotlinx JSON claim drift guard. All five claims probed and found CORRECT,
      including `JsonObject` (`JsonUtil.unwrap` does flatten nested elements). The guard derives each
      expected zod combinator FROM the observed codec shape, so it fails if either side moves.
      Mutation-tested 4/4 against the CLAIMS. Also found: a non-nullable `JsonElement` holding
      `JsonNull` refuses to slumber — asserted because it is surprising, not wrong.
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

**Iteration 11 (2026-07-30, ~03:30).** §1 complete; §2 six done, six left. 162 tests green, 7
ts-verify fixtures. Tree clean for `ultra/codegen`.

§2 done: `appendUnion` type-vs-schema (`56292476`), `ultra/slumber` round-trip reproduced and handed to
its own task, `slumberConfig` mandatory (`53b68126`), walker `unknown` now a hard failure (`efe2b6ad`),
collision check counts claims (`d605b996`), kotlinx JSON drift guard (`3bc64c41`).

Running tally: **three fixes were wrong, incomplete or over-applied, and FOUR assertions passed for the
WRONG reason** — the newest being iteration 11's first draft, which tested the codec instead of the
claim and so would not have failed if a claim were wrong. All caught by mutation.

### Habits that keep paying (and one I keep failing)

- Assert each property SEPARATELY; a combined assertion passes on the strongest one.
- A SURVIVING mutant may mean the FIX is wrong, not that a test is missing. Iteration 4 and 7 both.
- An assertion can pass for the WRONG reason. `out shouldContain "customSchema"` was green against the
  bug because the IMPORT line also lists the schema. Assert the specific line under test.
- For emitted TEXT, add a ts-verify fixture and run `:ultra:codegen:tsVerify` against the REVERTED code.
- **FAILING REPEATEDLY: read the test count from `TEST-*.xml` BEFORE writing the commit message.** Two
  amends so far for a wrong number.

**Next action:** §2, the two known-vacuous test assertions. Both are already diagnosed, so this is
small and mechanical:

1. `ts/TsModelEmitterSpec.kt` — `out.indexOf(x) shouldBe out.indexOf(x)` compares a value with itself,
   and the two neighbouring `<` comparisons pass when the variant is ABSENT because `indexOf` returns
   -1. Assert presence first, then order.
2. `contributors/MpDateTimeFieldParitySpec.kt:~108` — "the contributor claims every Mp type that has a
   codec" compares `CLAIMED.keys` against a hand-written list of the same six names, with no reference
   to Slumber at all, so it cannot detect the thing it claims to. Drive it from something real: iterate
   the `Mp*` classes and assert a codec exists iff a claim exists. **Use the technique from
   `3bc64c41`** — derive the expectation from observed behaviour, never from a second copy of the list.

### Working notes that paid off (keep using)

- `cp` the target file to the scratchpad with the source-set path in the name before mutating; restore
  with `cp`, never `git checkout` — the tree carries another agent's uncommitted work.
- `sed` on Kotlin string literals full of backslashes is a trap; use Edit or a python heredoc.
- Verify the restore with `git diff --stat -- <file>` (must be empty) before committing.
- Confirm counts from `build/test-results/**/TEST-*.xml`, not from the gradle task result.
- Running `:ultra:codegen:tsVerify` alone (skipping `test`) shows whether the TypeScript gate catches a
  mutation independently of the Kotlin assertions. Worth doing for anything that changes emitted text.

Suggested first move each iteration: `git log --oneline -3`, then the first unchecked box above.
