# Loop handoff — ts-sdk-codegen autonomous run

> ## ⛔ BEFORE ANY GRADLE COMMAND OR COMMIT: read `.claude/BUILD-LOCK.md`
>
> Another agent shares this worktree. If that file says `STATE: LOCKED` and you are not the holder,
> **do not run gradle, do not commit, do not stage.** Poll it, and start when it says `STATE: FREE`.
> Reading, grepping and planning are always fine. Two interleaved gradle builds corrupt Kotlin's
> incremental state here — it has already happened once and it reads like a logic bug, not a build
> bug. Check it every iteration; the holder changes underneath you.

**Read this file FIRST each iteration. Do the next unchecked item. Update this file LAST, before
finishing the iteration.** Keep it short — it is read every loop, so it must stay cheap.

Full context lives in `.claude/tasks/20260729-ts-sdk-codegen.md` (design, locked decisions, and the
**Review record** listing every confirmed finding). Do NOT read that whole file every iteration —
grep the section you need. The Vue follow-on is `20260730-frontend-sdk-vue-contributors.md`.

## RULE ZERO — the build lock. Read `.claude/BUILD-LOCK.md` BEFORE EVERY GRADLE COMMAND

Not once per iteration — **before every build and every commit.** The holder changes underneath you.

| The file says | You may |
|---|---|
| `STATE: LOCKED`, holder is not you | read, grep, plan, DRAFT EDITS. **No gradle. No commit. No stage.** |
| `STATE: FREE` | take it: rewrite HOLDER/SINCE/STATE, **commit that change alone first**, then build |

**Why, so you do not reason your way around it:** Kotlin's incremental state under `build/kotlin/` is
not safe against two interleaved gradle builds. A task reports UP-TO-DATE while its outputs are
stale, and it surfaces as an `AbstractMethodError` wrapped in an `AssertionFailedError` — it reads
like a logic bug in your own code and costs an iteration. This happened here on 2026-07-30;
`CLAUDE.md:120-124` records it. `ultra/codegen` declares `api(project(":ultra:slumber"))`, so you are
directly downstream of the work in flight.

**A locked interval is not dead time.** It is exactly when to read ahead and prepare edits, then
apply them once you hold the lock. Do not idle-poll it.

**If the lock looks stale** — `SINCE` more than a day old with no commits from the holder — do NOT
take it silently. Ask the maintainer. A stale lock costs a wait; a wrongly-taken one costs a
debugging session that looks like a real bug.

## What is landing from the other agent (`@Slumber.As`)

`.claude/tasks/20260731-slumber-as-declared-wire-shape.md`. Two things reach you:

1. **The `Slumber` annotation nest moves** from `io.peekandpoke.ultra.slumber.Slumber` to
   `io.peekandpoke.ultra.common.slumber.Slumber`. **Two files import it, and one is MAIN source**:
   `ultra/codegen/src/main/kotlin/model/TypeWalker.kt:6` and
   `ultra/codegen/src/test/kotlin/model/walker_fixtures.kt:3`. Expect a mechanical import fixup, not
   a breakage. (The other agent's brief said "three test fixtures"; it is two files and one of them
   ships.)
2. **The six ultra/datetime types gain `@Slumber.As`**, RUNTIME retention, readable reflectively.

**Item 2 is a FOLLOW-ON, not this loop's work.** Do not start it until the annotation is actually on
the types, and do not design from a summary — read §3, §4 and §6 of that task file. One constraint
that shapes it: `java.time.*` and `kotlinx.datetime.*` cannot be annotated, so `TsTypeClaims` stays.
Read the annotation where present; fall back to the registry where absent.

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

## ITERATION 4, 2026-08-01 — LOCK TAKEN. Items 1 and 2 VERIFIED and COMMITTED.

**I HOLD THE LOCK.** `.claude/BUILD-LOCK.md` says `codegen agent`. Release it when this loop stops or
goes idle for long — and say what changed, as the previous holder did for me. That note was genuinely
useful: it named an error in my own uncommitted edit before I built.

**Baseline after their `@Slumber.As` work:** `:ultra:codegen:check` 241, `:funktor:codegen:check` 50,
0 failures, 10 ts-verify fixtures, compile sweep clean.

**What three iterations of blind drafting actually cost — worth knowing for next time.** Two real
compile errors, neither found by proofreading alone:

- `FunktorCodegenWiringSpec` missing `string.shouldContain` — I caught this by proofreading (iter 3).
- `FunktorCodegenWiringSpec:101` missing the `codeGen` import — **the OTHER AGENT caught this**, in
  their handover note, from a sweep I could not run. I had proofread that same file and missed it.

Drafting under a lock is worthwhile, but it is not verification, and a careful re-read is not either.

**Four exact-file-list assertions had to learn about `index.ts`.** Kept EXACT rather than loosened to
`shouldContainAll` — "nothing else ships" is the property that caught the barrel in the first place.

**One mutant survived and needed a better FIXTURE, not a new assertion:** `profileTagged` swapping
ANY for ALL passed everything, because every test used a single tag, where the two are identical. Two
tags with the route carrying one is the smallest distinguishing case. **Third time this pattern has
appeared** (options-spread, body-type-vs-schema, now this): when a mutant survives, ask whether the
fixture can express the difference before writing another assertion.

**Next: items 3 and 4** — `--check` in a CI-shaped run, then `out.shared()`. Then the `@Slumber.As`
follow-on, which is now genuinely unblocked: read §3, §4 and §10 of that task file. §10 records that
`Redacted<T>` is asymmetric and that the annotation describes the SLUMBER direction only —
`MpDateTimeFieldParitySpec` is superseded by `ultra/slumber`'s `SlumberAsRoundTripSpec`.

---

## ITERATION 3, 2026-08-01 — PROOFREAD instead of drafting. Lock still held.

Third iteration under the lock. Holder still active (`4c4daf69`, `eca57201`, `8fc6d673` — they have
now deleted the hand-written karango/monko `.ts` helpers). **Not stale. Do not take it.**

**Deliberately did NOT draft item 3.** Iteration 2's own note said a third unverified item would make
a failure hard to attribute, and that still holds. Instead: proofread the two existing drafts against
the real APIs, which is the only verification available while locked.

**FOUND ONE REAL BUG — the draft would not have compiled.** `FunktorCodegenWiringSpec` used
`client shouldContain "listSpeakers"` on a **String** receiver while importing only
`io.kotest.matchers.collections.shouldContain`. Fixed. All three touched specs now carry both the
collections and string variants; they disambiguate by receiver type, which is fine.

**Verified by reading the real declarations, not assumed:**

| Assumption | Checked against |
|---|---|
| `CodePrinter.print { }` is the emitter idiom | `TsModelEmitter.kt:29` uses exactly it; signature at `CodePrinter.kt:23` |
| `tsStringLiteral` is reachable from `ts/` | now `fun`, not `internal fun` (`TsLiterals.kt:13`) |
| `output.entries()` gives `.path` | `TsSdkOutput.kt:29` returns `List<Entry>` |
| the profiled fixture's file is `fxProfiledClient.ts` | `TsClientNames.clientFile` = camel + `Client.ts` |
| `Result.model` exists for the walk assertion | `TsSdkBuilder.kt:78` |
| `profile()` overrides the default registration | module registers first, `FunktorCodegenBuilder(this).apply(builder)` runs last |

**The one thing still unverifiable statically:** that kontainer is last-wins when the same type is
re-registered. `funktorAuth`'s `useKarango()` relies on exactly this (`Adapter.Null` → `Adapter.Vault`)
and runs in production, so the pattern is proven even if this instance is not. **If `profile()` turns
out not to override, that is where to look first** — not at the predicate.

---

## ITERATION 2, 2026-08-01 — items 1 AND 2 drafted, still NOTHING verified. Lock held throughout.

The lock has been `LOCKED / slumber-as agent` for two iterations. It is NOT stale — the holder has
committed five times (`4902d6bf` … `907ac482`), so they are working. Do not take it.

**Everything from iterations 1 and 2 is written to disk and has NEVER BEEN COMPILED.** Two items'
worth of drafted code is now stacked up. **Verify item 1 fully before touching item 2's draft** —
piling a third unverified item on top would make a failure hard to attribute.

**Good news from their commits — read before your first build:**

- The `Slumber` import fixup is **already done for both my files**: `TypeWalker.kt:3` and
  `walker_fixtures.kt:3` both read `io.peekandpoke.ultra.common.slumber.Slumber`. Expect no work there.
- `@Slumber.As` has **landed on all six Mp types** (`3237c91a`), exactly as specced:
  `As(MpDateTimeRawData::class)` ×4, `As(Long::class)` on `MpLocalTime`, `As(String::class)` on
  `MpTimezone`. **The follow-on is unblocked** — but it stays AFTER backlog items 1–4, and read
  §3/§4/§6 of the task file rather than this summary.

**What was drafted for item 2 (root filtering):**

- `funktor/codegen/.../index_jvm.kt` — `FunktorCodegenBuilder.profile(include)` and
  `profileTagged(vararg tags)`. Overrides the default `RestApiTsContributor` registration, which is
  the established funktor pattern (`funktorAuth`'s `useKarango()` does the same).
- `profileTagged` matches ANY tag, not all: a tag marks an audience, and a route serving two
  audiences carries both — requiring all would make a second tag NARROW its reach, the opposite of
  what adding one reads like. Empty varargs is refused at the call site.
- `FunktorCodegenWiringSpec` — default admits everything; `profile` and `profileTagged` narrow it;
  empty `profileTagged` refused. The load-bearing assertion is that a profile narrows **the WALK**:
  an excluded route's payload type must not appear in `model.decls` either. If the client assertion
  passes while that one fails, the profile is filtering the wrong thing.
- `rest_fixtures.kt` — `FxProfiledApiFeature`, mixing one tagged route with untagged ones.

---

## ITERATION 1, 2026-08-01 — item 1 DRAFTED, NOT VERIFIED. Lock held throughout.

`.claude/BUILD-LOCK.md` said `LOCKED / slumber-as agent` for the whole iteration, so: no gradle, no
commit, no staging. Everything below is **written to disk and unverified** — it has never been
compiled. Treat it as a draft by someone else.

**FIRST ACTIONS WHEN THE LOCK FREES, in order:**

1. Take the lock (rewrite HOLDER/SINCE/STATE) and commit that alone.
2. Expect a MECHANICAL IMPORT FIXUP: the `Slumber` annotation nest moved to
   `io.peekandpoke.ultra.common.slumber.Slumber`. `walker_fixtures.kt:3` has already flipped;
   `TypeWalker.kt:6` may still need it. Not a breakage.
3. `./gradlew :ultra:codegen:check :funktor:codegen:check` — establish that the baseline still holds
   (was 232 + 45) BEFORE trusting anything drafted below.
4. Then verify the draft, and mutation-test it.

**What was drafted for item 1 (the `index.ts` barrel):**

- `ts/TsBarrelEmitter.kt` — NEW. Renders `export * from './x.ts'` per emitted module. Beside the other
  emitters rather than inside the builder, because `TsFixtureGenerator` needs the same function and
  duplicating it would be exactly the drift this module keeps finding.
- `sdk/TsSdkBuilder.kt` — emits the barrel as **Phase 5, LAST**, because it re-exports what
  contributors wrote and can only be built once they have all run.
- `ts/TsBarrelEmitterSpec.kt` — NEW. Sorting/stability, self-exclusion, non-`.ts` filtering, dedupe,
  empty input.
- `sdk/TsSdkBuilderSpec.kt` — barrel is emitted, covers contributor files, does not re-export itself,
  and individual files stay separately importable.
- `TsFixtureGenerator` + `verifyRuntime.ts` — the fixture emits a barrel over the SDK-shaped subset
  and `verifyRuntime` imports `FxDemoClient` THROUGH it, so `tsc` compiles the barrel.

**Two findings from this iteration, both worth keeping:**

- **`export *` is only safe if no two emitted modules export the same name**, and a collision is
  otherwise a silent hole. Checked against the REAL demo SDK: 213 exported names, **zero cross-file
  collisions**, and `models.ts` only imports the datetime claims rather than re-exporting them. So
  the approach is viable — but the ts-verify barrel import is what keeps it honest.
- **My first collision check was wrong and nearly cost an hour.** Grepping `^export (const|type)`
  and counting occurrences reports every declaration twice, because the zod pattern emits
  `export const X` AND `export type X` in the same file — which TypeScript merges. It looked like 150
  collisions. **Count DISTINCT FILES per name, not occurrences.**

**Deliberately NOT done:** a `clients/` subdirectory, and splitting `models.ts`. See the backlog entry
for why — the split needs an owner rule for shared types and risks cross-file circular value imports.

**Next items after 1 is verified:** 2 root filtering, 3 `--check` in a CI-shaped run, 4 `out.shared()`.

## BACKLOG for this loop — decided 2026-08-01, work top-down

Phases 1–4 are done, the review gate passed, and the findings table is audited (`70109f0f`) — the
codegen backlog from the reviews is EMPTY. What follows is new work, in order.

- [x] **DONE `84775a8e` — 1. `index.ts` barrel.** Layout decision (maintainer, 2026-08-01): **stay FLAT, add a barrel.**
      `models.ts` and the client files remain at the SDK root; `index.ts` re-exports them so a caller
      writes `import { FunktorConfClient } from '@sdk'` instead of knowing filenames.
      **Individual files must stay importable** — a barrel that becomes the only door defeats
      tree-shaking in some bundlers. Emitted by the BUILDER, not a contributor: every contributor
      feeds it, so no single one owns it, exactly as `models.ts` is emitted in `TsSdkBuilder`.
      *Deliberately NOT doing:* a `clients/` subdirectory, and splitting `models.ts` per feature. The
      split needs an owner rule for a type two features both reach, and risks cross-file circular
      VALUE imports — `z.lazy` breaks cycles within a file, but across files a circular import of zod
      schemas fails at RUNTIME, not compile time. Revisit alongside profiles, since narrowing roots
      shrinks `models.ts` anyway.

- [x] **DONE `460621e9` — 2. Root filtering / profiles.** Decision: **keep allow-all as the default**, add opt-in
      filtering by `CodeGenHints.tags`. Nothing changes for existing callers. The seam already exists
      and is tested — `RestApiTsContributor(features, include = { true })` — so this is about exposing
      it through `funktorCodegen { }`, not building it. Narrowing ROOTS is the whole mechanism: an
      unreached claim ships no runtime and `models.ts` shrinks, with no tree-shaking stage anywhere.

- [ ] **3. `--check` in a CI-shaped run.** The one CLI path nothing has exercised end to end.
      Generate, assert clean; mutate one emitted file, assert non-zero exit and that the message names
      it; delete one, same. `--check` is the entire reason a stale SDK is catchable, and it has never
      been run in anger.

- [ ] **4. `out.shared(path, content)`.** From the frontend-SDK doc's incoming requirements:
      `TsSdkOutput.add` errors on ANY duplicate path, even byte-identical, so two contributors cannot
      both ask for one shared module. Identical content should dedupe; differing content stays a hard
      error naming both. A prerequisite for the Vue contributors, self-contained here.

**STOP and ask rather than guessing** if: an item needs a decision not written above; the lock looks
stale; or the build is red in a module this task never touched.

---

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
