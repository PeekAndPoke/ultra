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
- [x] **DONE `ccab61d4`** — both vacuous assertions repaired. The Mp one is now driven from the
      ultra/datetime artifact plus `MpDateTimeModule` itself, and asserts the enumeration found
      something so it cannot pass by scanning nothing. Mutation-tested 2/2; both OLD versions survived
      their mutations, which is why they were rewritten rather than tweaked.
- [x] **DONE `29a1015d`** — `JsonElement` now claimed via `opaque()`, so it reaches the run summary. Emitted TypeScript unchanged. Mutation-tested.
**Everything below needs a MAINTAINER DECISION and is NOT pre-authorized — see the stop conditions.**

- [ ] Generic sealed hierarchy loses payload type (`createBareType()` for variants). Needs a design
      call: substitute the parent's reified arguments into each variant, or fail loudly on an
      unsubstituted parameter? Both are defensible; the second is smaller and matches wrong-and-loud.
- [ ] `@Slumber.Field` non-ctor props emitted required. Ties directly to request-vs-response shapes —
      one declaration genuinely cannot describe both directions, and picking one is a Phase 2 design
      decision, not a bug fix.
- [ ] `@Slumber.Field` selection re-derived from `DataClassSlumberer`. The fix needs a NEW PUBLIC API on
      `ultra/slumber` (expose the field selection), which is battle-tested code — its own task and
      review round, not a drive-by.
- [ ] Scalar refinement (`Char` → `z.string().length(1)`, integral bounds). A deliberate
      strictness choice: it makes generated schemas reject input the server would also reject, but it
      is a behaviour change for every existing SDK consumer.
- [ ] `readArrayElements` duplicated verbatim in `ultra/slumber`. Cosmetic, but it is battle-tested
      code; same rule as above.

### 3. Phase 2 — `funktor/codegen`. IN PROGRESS, first slice landed 2026-07-30

Task file: `.claude/tasks/20260730-funktor-codegen-rest-contributor.md`. **Read its "Decisions taken
with the maintainer" section before touching any of this** — four decisions are settled there and each
one touches every emitted file.

Landed (`57a749b5`, `b9669ae8`, `e924ac53`): the module, `TsClientEmitter`/`TsClientSpec` on the ultra
side, `RestApiTsContributor` (profile-shaped), `runtime/client.ts`, `TypeModel.rootRefs`, and a
ts-verify fixture that **executes** a generated client.

In order:

- [ ] **`WithParams` routes.** Params are typed by their WIRE form, precisely where provable
      (`String`, numerics, `Boolean`, enums, value classes over those) and **refused by name
      otherwise** — maintainer decision, see §4 of the task file. Path vs query split comes from
      `TypedRoute.parsedUriParams`; URL building must match `TypedRouteRenderer`
      (`funktor/core/src/jvmMain/kotlin/broker/TypedRouteRenderer.kt:28`). `runtime/http.ts`'s
      `buildUrl` is already written against it — **do not reinvent, and there is no `UriParamBuilder`.**
- [ ] **Param-claim mechanism.** Blocks the demo: `Stored<T>` entity params are refused without it.
      A URL-param type is a different axis from a body-shape claim — design it, do not assume the
      sketch in the task file is right.
- [ ] **`WithBody` / `WithBodyAndParams`.** Root `bodyType` as well; the body reaches `request` via
      `options.body`, already implemented and verified in `runtime/client.ts`.
- [ ] **`Sse`.** `responseType` is `TypeRef<Unit>`, so the stream payload is NOT on the route — decide
      what an SSE member should even return before writing it. `runtime/sse.ts` exists.
- [ ] **`TsSdkGenerateCliCommand`** — clikt, `sdk:ts:generate`, `--out --dry-run --check --verbose`.
      `--check` is not optional. Template:
      `funktor/auth/src/jvmMain/kotlin/cli/AuthGenerateJwtSigningSecretCliCommand.kt`.
- [ ] **`funktorCodegen()` kontainer module** — `dynamic(TsSdkBuilder::class)` deliberately; do NOT add
      it to the all-in-one `Funktor` module.
- [ ] **`instance(codecConfig)` in `Funktor_Rest`** — the ONE authorized change to that module.

**Rules specific to this phase:**

- Every change to emitted text needs a **ts-verify fixture**, and the mutant must be run against the
  reverted code. Emitting prototype methods instead of arrow class fields produces NO tsc error and
  dies only on execution — that is the standard to hold.
- Root labels must stay qualified (`funktor:rest:<feature>:<group>:<member>`), or two contributors
  silently swap types.
- A route's `responseType` is the **ENVELOPE**. Unwrap it. This was a real bug, found by a fixture.

## STOP conditions — end the loop when any is true

- Backlog §1 and §2 are done and `./gradlew :ultra:codegen:check` is green, **and** the next item
  needs a design decision the maintainer has not pre-authorized.
- Three consecutive iterations produce no committed progress.
- Anything requires pushing, a PR, or touching `funktor/rest` beyond the codec config.
- The compile sweep goes red in a module nobody in this task touched.

To stop: call `ScheduleWakeup(stop: true)` and leave the final note below.

---

## Note to next loop

## READY TO RESUME 2026-07-30 — Phase 2 slice 1 landed, decisions settled

**Start at §3.** All four cross-cutting decisions are settled with the maintainer and written down in
`.claude/tasks/20260730-funktor-codegen-rest-contributor.md`; read that section first, then take the
next unchecked §3 item.

**Baseline:** `:ultra:codegen:check` 215 tests, `:funktor:codegen:check` 11 tests, 0 failures,
10 ts-verify fixtures, compile sweep clean, at `57a749b5`.

**Two bugs this slice found, both by fixtures and neither by reading the code twice:**

- A route's `responseType` is the ENVELOPE (`ApiResponse<List<Talk>>`). Rooting it walks the
  hand-written envelope into `models.ts` and makes `request` wrap it twice — every parse would fail.
- The new duplicate-root-label check fired on real code the moment it existed: both helpers in
  `TsSdkBuilderSpec` labelled their root `"root"`.

**A shared tree is not a private one.** Another agent works in `ultra/log`, `ultra/i18n`, `tooling/`
and `buildSrc`. Their in-flight i18n move broke `buildSrc` for a while, which fails EVERY gradle task —
if the build is red in a module this task never touched, check `git status` before debugging. And
**never stage a file just because it is "yours"**: an uncommitted change in this task's own plan doc
turned out to be theirs and got committed by mistake in `47ece09c`.

---

## Previous run: LOOP STOPPED 2026-07-30 ~03:50 — stop condition met

**Every remaining backlog item needs a maintainer decision** (see §2, each one annotated with the
specific question). That is the first stop condition. The loop ended deliberately, not because it ran
out of road.

**Final state — all green:**

| Gate | Result |
|---|---|
| `:ultra:codegen:check` | 163 tests, 0 failures; 7 ts-verify fixtures |
| `:ultra:slumber:jvmTest` | 1232 tests, 0 failures — array support unaffected |
| Compile sweep (6 targets, `--continue`) | no `^e:` |
| Working tree | clean for `ultra/codegen` and this task's docs |

**Done across 13 iterations:** backlog §1 complete (regression tests for all six review-round fixes),
and §2 down to the decision-gated tail — 8 of 14 items.

**What the run actually found.** Starting from code that had already passed a 3-reviewer round and was
described as mutation-tested throughout:

- **Three of the six review-round FIXES were wrong, incomplete or over-applied.** `TypeId` nullability
  was half-done; the root-parent hop was applied to variants as well as the discriminator; and the
  `appendUnion` finding turned out real once reproduced.
- **Four assertions passed for the WRONG reason** — including two written during this run, one of them
  a drift guard that tested the codec instead of the claim it was meant to guard.
- **Two defects were found that no reviewer raised**: the union type-vs-schema position bug, and an
  `ultra/slumber` round-trip failure now handed to its own task file.

Every one was caught by mutation testing. None by reading the code, including when read carefully and
twice. **If you take one thing from this run into the next: a green suite says nothing until you have
watched it go red.**

### Habits that keep paying (and one I keep failing)

- Assert each property SEPARATELY; a combined assertion passes on the strongest one.
- A SURVIVING mutant may mean the FIX is wrong, not that a test is missing. Iteration 4 and 7 both.
- An assertion can pass for the WRONG reason. `out shouldContain "customSchema"` was green against the
  bug because the IMPORT line also lists the schema. Assert the specific line under test.
- For emitted TEXT, add a ts-verify fixture and run `:ultra:codegen:tsVerify` against the REVERTED code.
- **FAILING REPEATEDLY: read the test count from `TEST-*.xml` BEFORE writing the commit message.** Two
  amends so far for a wrong number.

**No next action — the loop is stopped.** Restart it only after the §2 decisions are made; each item
states its question. Phase 2 (`funktor/codegen`) is authorized but was deliberately NOT started
unattended: it is a large new surface, and beginning it straight after a run that found this many
defects in reviewed code is the wrong trade. A clean green tree with decisions queued is worth more.
