# ULTRA-00247: Kraft testing: eventually returning its block's value; KQuery.elements

> **Status (2026-10-30): DONE.** Gate PASS 2026-10-30: the UI batch's review, clean in round 9 (record in ULTRA-00266). Request from Spross.
>
> **Part of:** ULTRA-00244 (Epic: Kraft testing (TestBed, KQuery and friends))

**Security-critical:** no

## The request, checked against the code (2026-10-26)

1. **`eventually` returns `Unit`** (`kraft/testing/src/jsMain/kotlin/eventually.kt:14-18`, `check: suspend () ->
   Unit`). A variant returning the block's value saves the `var found` dance
   (spross `workshop/src/jsTest/kotlin/scenes/pixi/PixiStageSpec.kt`), e.g.
   `val stage = eventually { findStage().shouldNotBeNull() }`. A generic `suspend fun <T> eventually(...): T` is
   source-compatible for callers whose block ends in a `Unit` expression; check the callers that end in an assertion
   returning a value.
2. **`KQuery.elements` is `internal`** (`KQuery.kt:12`). The list delegation works; the name invites use from
   outside. Rename it (e.g. `private val items`) or make it public, as part of the same pass.

## Decide before implementing

1. **Generic `eventually`.** `suspend fun <T> eventually(timeout, poll, check: suspend () -> T): T`. Recommendation:
   yes. Callers using it as a statement infer `T` from the block and ignore the result: no call site changes (16 files
   call it; all pass a trailing lambda).
2. **`KQuery.elements`.** It is `internal`, so no other module sees it; the name only shows in the source. It is used
   once in the module (`KQuery.kt:46`). Recommendation: rename to `private val items` and give that one use a
   `KQuery.items` internal accessor, or leave it: the delegation makes it redundant to expose. Minor; I take the rename.

## The work

1. `kraft/testing/src/jsMain/kotlin/eventually.kt`: the generic signature, `return check()` on success; KDoc
   example `val stage = eventually { findStage().shouldNotBeNull() }`.
2. `kraft/testing/src/jsMain/kotlin/KQuery.kt`: the rename.
3. Specs in `kraft/core-tests/src/jsTest/kotlin/KQueryDomSpec.kt` (where `eventually` is tested): returns the block's
   value once it passes; the value of the passing attempt, not an earlier one; still rethrows the last failure.
4. Docs (testing page, LLM mirror), CHANGELOG.

## Acceptance

- [ ] `val x: Int = eventually { 42 }` compiles and returns 42; all kraft jsTest suites green unchanged.

## Review record

In ULTRA-00266's Review record (the batch's gate, both groups).

## History

- 2026-10-26: created from Spross's request.
- 2026-10-29: plan written (maintainer: "write a detailed plan into each ticket").
- 2026-10-29: DESIGNED.
- 2026-10-29: implemented and through two review rounds (record in ULTRA-00266); round 2 not clean (one MAJOR on replaceUri keeping history.state): stopped at the safety valve for the maintainer.
- 2026-10-29: IN PROGRESS.
- 2026-10-30: DONE.
