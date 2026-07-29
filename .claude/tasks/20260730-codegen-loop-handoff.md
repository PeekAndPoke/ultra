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
- [ ] `TypeId` type-argument nullability — `Box<String>` and `Box<String?>` in ONE model must produce
      two declarations. Assert both are emitted and differ.
- [ ] Claimed polymorphic child lands in `usedClaims` — assert the import IS emitted and the variant
      does not render as the literal `unknown`. Run it through `TsSdkBuilder`, not `TypeWalker` alone.
- [ ] `declareUnion` root-parent hop — needs an intermediate sealed class whose companion is on the
      ROOT (`Base` has the `Polymorphic.Parent` companion, `Mid : Base()`, `A : Mid()`).
- [ ] `appendAlias` recursion — value class on a cycle (`FxIds(val items: List<FxHolder>)` +
      `FxHolder(val ids: FxIds)`). Assert `z.lazy` is emitted.
- [ ] `resource()` contributor classloader — fixture must be loader-observable, else it passes either
      way. Load the contributor via a child `URLClassLoader` whose resources ultra:codegen cannot see.
      This also fixes the false clue at `sdk/ThirdPartyContributorSpec.kt:128`.

### 2. Confirmed findings not yet fixed — by severity

See the **Review record** table in `20260729-ts-sdk-codegen.md` for the full list with file:line.
Suggested order:

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

**Iteration 1 (2026-07-30, ~02:00).** Backlog §1 item 1 done and committed (`ef5eba72`). 127 tests
green (was 120), 6 ts-verify fixtures (was 5). Working tree clean for `ultra/codegen`.

Do NOT re-fix the `.bufferedReader()` charset non-bug — it was a wrong finding; see the Review record.

**Do not touch these — another agent owns them:** `ultra/log/**`,
`.claude/tasks/20260729-log-scan-findings.md`, `.claude/tasks/20260729-redteam-log-forging.md`.
`.idea/compiler.xml` is modified in the tree and is NOT ours to commit — leave it.

**Next action:** backlog §1 item 2 (`TypeId` type-argument nullability). Add `Box<String>` and
`Box<String?>` as properties of ONE fixture; assert two distinct declarations are emitted. Note the
name must differ too, else `TsNames.of` collides — check whether the fix needs a `TsNames` change as
well, since the review only altered `TypeId.canonicalKey`. **This is a real open question, resolve it
before writing the test.**

Still nothing from §2 until §1 is fully done.

### Working notes that paid off (keep using)

- `cp` the target file to the scratchpad with the source-set path in the name before mutating; restore
  with `cp`, never `git checkout` — the tree carries another agent's uncommitted work.
- `sed` on Kotlin string literals full of backslashes is a trap; use Edit or a python heredoc.
- Verify the restore with `git diff --stat -- <file>` (must be empty) before committing.
- Confirm counts from `build/test-results/**/TEST-*.xml`, not from the gradle task result.
- Running `:ultra:codegen:tsVerify` alone (skipping `test`) shows whether the TypeScript gate catches a
  mutation independently of the Kotlin assertions. Worth doing for anything that changes emitted text.

Suggested first move each iteration: `git log --oneline -3`, then the first unchecked box above.
