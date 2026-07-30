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

- [x] **DONE `d579099c` — `WithParams` routes.** One parameter object per member, split into
      `path`/`query` in the call. `UrlParamTypes` (funktor side) maps the wire form and refuses
      anything unprovable by name.
      **Learned from funktor, worth not re-deriving:** `TypedRoute.validateUriPattern`
      (`funktor/core/src/jvmMain/kotlin/broker/TypedRoute.kt:170`) requires every NON-OPTIONAL params
      property to be a URI placeholder — so a query parameter always has a Kotlin default, and is
      therefore always optional in TypeScript.
- [x] **DONE `60a96c19` — param-claim mechanism.** `TsUrlParamClaims` + a `claimUrlParams` phase, and
      `FunktorUrlParamsTsContributor` claiming the datetime and vault types. A SEPARATE registry,
      because the two memberships are independent (measured table in the `TsUrlParamClaims` KDoc).
      **The claim list mirrors funktor's converter registry and must not outgrow it** — a claim says
      "the server can parse this back", so `FunktorUrlParamsParitySpec` derives its expectation from
      `OutgoingMpDateTimeConverter.canHandle`, never from a second list.
- [x] **DONE `22014604` — `WithBody` / `WithBodyAndParams`.** Signature is `(params, body)`, mirroring
      the Kotlin argument order; `params` stays required whenever the route has any. The body is used
      in TYPE position and is ROOTED, so a request type reachable from nowhere else still gets
      declared.
- [x] **DONE `2a3df280` — `Sse`, emitted UNTYPED** (maintainer decision, 2026-07-30). A member returns
      `AsyncGenerator<SseEvent>` and takes a trailing `options?: SseOptions`; events carry raw `data`
      strings. `ApiRoute.Sse.responseType` is `TypeRef<Unit>`, so there is nothing to derive — typing
      it would mean changing how SSE endpoints declare themselves server-side.
      **An SSE stream does NOT go through `config.transport`** (the body is consumed as bytes, which
      `HttpTransport` cannot express), so auth is per-call via `SseOptions.headers`.
- [x] **DONE `51f2bd7e`** — `TsSdkGenerateCliCommand`, `funktorCodegen()`, and the one authorized
      `instance(codecConfig)` line in `Funktor_Rest`. `FunktorCodegenWiringSpec` proves the
      registrations RESOLVE, not merely compile — a module definition type-checks whether or not its
      dependencies can be satisfied.

**Rules specific to this phase:**

- Every change to emitted text needs a **ts-verify fixture**, and the mutant must be run against the
  reverted code. Emitting prototype methods instead of arrow class fields produces NO tsc error and
  dies only on execution — that is the standard to hold.
- **Calling a generated member correctly proves the signature EXISTS, not that it is ENFORCED.** The
  mutant emitting every parameter as optional passed every positive ts-verify check. Use
  `@ts-expect-error` for the negative side: tsc reports **TS2578** when such a line stops erroring, so
  it fails exactly when the emitted types get looser. Four sites are in `checkGeneratedClient`; add
  one for every new type constraint the emitter introduces.
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

## PHASE 2 COMPLETE 2026-07-30 (`2a3df280`)

**Every §3 box is checked.** All five `ApiRoute` variants emit, the CLI and kontainer module are
wired, and `funktor/rest` carries its one authorized line. There is no next item in this file — the
loop has nothing left to take.

**Baseline:** `:ultra:codegen:check` 221, `:funktor:codegen:check` 33, `:funktor:rest:jvmTest` 102,
0 failures, 10 ts-verify fixtures (7 `@ts-expect-error` sites), compile sweep clean.

**Next, and NOT in this file:** the mandatory `/feature-review` gate on
`.claude/tasks/20260730-funktor-codegen-rest-contributor.md`, then a DOCS follow-up — this added two
public modules and a public extension point, and CLAUDE.md requires a tracked docs task on archive.

**Known limitation, deliberately not fixed:** a feature whose routes are ALL streams contributes no
roots, and `TsSdkBuilder` rejects a run with none ("No contributor supplied any root type"). That
message is wrong for an SSE-only SDK, which is legitimate output. Not reachable today — the demo has
REST alongside SSE — so it is recorded rather than special-cased.

**What this iteration found:** the file EMISSION and the IMPORT of a conditional runtime module are
two separate conditions. Getting only one right leaves a client importing a module that was never
written, which no assertion about emitted paths notices and which `tsc` cannot catch in ts-verify,
because the harness copies every runtime module into place regardless.

---

## ITERATION 5 DONE 2026-07-30 — CLI + kontainer module landed (`51f2bd7e`)

**Phase 2 is COMPLETE except `Sse`.** Everything in §3 is checked off but that one box, and it is a
DECISION, not work — see the box for what has to be settled. The maintainer has the question.

**Baseline:** `:ultra:codegen:check` 220, `:funktor:codegen:check` 30, `:funktor:rest:jvmTest` 102,
0 failures, 10 ts-verify fixtures, compile sweep clean, at `51f2bd7e`.

**What this iteration found:** a module definition TYPE-CHECKS whether or not its dependencies can be
satisfied. A missing `SlumberConfig`, an unmatched constructor parameter or a wrong scope surfaces
only when something asks the container — for a CLI command that is the moment an operator runs it.
`FunktorCodegenWiringSpec` resolves the graph and drives generation end to end through it.

**AND I WALKED INTO A DOCUMENTED TRAP.** My mutation-test script backed files up by `basename`, and
BOTH modules have an `index_jvm.kt` — so restoring one wrote the other's content, and three mutants
"died" from a broken `funktor/rest` file rather than from their own mutation. This is verbatim the
collision in CLAUDE.md's verification traps. **Mirror the path in backup names
(`codegen__main__index_jvm.kt`), or back up one file at a time.** Redone; all four then died correctly.

---

## ITERATION 4 DONE 2026-07-30 — request bodies landed (`22014604`)

**THE LOOP IS PAUSED HERE ON PURPOSE.** The next §3 item is `Sse`, and it is a DECISION rather than
work: `ApiRoute.Sse.responseType` is `TypeRef<Unit>`, so the stream's payload type is not on the route
at all. Do not invent one. Everything after it (CLI, kontainer module, the `funktor/rest` codec-config
line) is unblocked, so **if the maintainer would rather keep moving, skip `Sse` and take the CLI.**

**Baseline:** `:ultra:codegen:check` 220 tests, `:funktor:codegen:check` 25 tests, 0 failures,
10 ts-verify fixtures, compile sweep clean, at `22014604`.

**What this iteration found — the sharpest lesson of the run so far.** Two mutants survived, and
NEITHER meant a missing test: both meant a **fixture too weak to distinguish the mutation**.

- The body type was also a response type, so "don't import body types" changed nothing.
- For a plain object, `renderer.type` and `renderer.schema` return the SAME string, so
  "emit the schema instead of the type" was a literal no-op.

One fixture change killed both: a body that is a LIST of a type reachable ONLY as a body. **When a
mutant survives, ask whether the fixture can even express the difference before writing a new test.**

**Standing rules from earlier iterations, still the two that pay:**

- A green POSITIVE check proves less than it looks — every ts-verify check passed while the emitter
  marked every parameter optional, because the harness only ever CALLED members correctly.
  `@ts-expect-error` is the negative side (6 sites now; tsc reports TS2578 when one stops erroring).
- When you copy a pattern, copy its SPEC in the same breath. The duplicate-claim guard was copied
  from `TsTypeClaims` without its test, so last-wins passed everything.

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
