# ULTRA-00247: Kraft testing: eventually returning its block's value; KQuery.elements

> **Status (2026-10-26): IDEA.** Request from Spross (Spross-Main-Dev, 2026-10-26), found writing the workshop's scene specs (SPROSS-00023).
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

## History

- 2026-10-26: created from Spross's request.
