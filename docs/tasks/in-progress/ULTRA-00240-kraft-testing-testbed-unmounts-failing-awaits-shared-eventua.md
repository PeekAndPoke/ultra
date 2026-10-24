# ULTRA-00240: Kraft testing: TestBed unmounts, failing awaits, shared eventually, KQuery members

> **Status (2026-10-24): IN PROGRESS.** From the TestBed/KQuery findings of ULTRA-00233 (every modal answers once).
> The maintainer (2026-10-24): "do the testbed ticket first. When this is done we will prepare a release."

**Security-critical:** no

## Why this exists

Writing ULTRA-00233's browser tests showed where kraft/testing gets in the way (the findings table in ULTRA-00233).
The maintainer asked for that report "so we can improve there too", and for the modal tests to use the fixes.

## Decided

- Scope, as proposed to the maintainer on 2026-10-24: TestBed unmounts; `awaitCss` fails on timeout and
  `awaitGone` is new; one shared `eventually`; `first` / `last` / `nth` as KQuery members; `hasFocus()`; the modal
  specs use the helpers. Real key presses (CDP input) stay out.
- Breaking for test code only. Klang and Spross import none of the changed functions (checked: no `awaitCss`, no
  `first` / `last` / `nth` import, 2026-10-24).

## The work

- [x] `VDomEngine.unmount(element)`, Preact `render(null, element)`; `TestBed.render` unmounts in its `finally`
  before removing the div (`kraft/testing/src/jsMain/kotlin/TestBed.kt`).
- [x] `awaitCss` throws `AssertionError` naming the selector; `awaitGone(css)` (`kquery_select.kt`).
- [x] `eventually(timeout, poll) { }` in kraft/testing (`eventually.kt`, monotonic clock); the 12 private copies
  (9 addon specs, chartjs's `test_helpers.kt`, 3 core-tests specs) removed. ULTRA-00233 had counted 4.
- [x] `first()` / `last()` / `nth()` are members of `KQuery`; the extensions are gone. 4 callers that wanted the
  stdlib's `Element` now use `[0]`.
- [x] `KQuery.hasFocus()`.
- [x] `OkCancelModalSpec`: no `closeAll()` in a `finally` (TestBed's unmount does it), `until` and the
  `document.querySelectorAll` counting replaced by `eventually`, `awaitGone`, `hasFocus`.
- [x] Docs (`testing.astro`, LLM mirror) and CHANGELOG.

## Acceptance

- [x] `TestBedSpec`: a component's `onUnmount` runs after the test, and the testbed div is gone.
- [x] `KQueryDomSpec`: `awaitCss` fails with the selector, `awaitGone` both ways, `eventually` retries and rethrows,
  `hasFocus`, members without an import.
- [x] All JS tests of the repo green (`./gradlew jsTest`): 3492 tests, 0 failed, 204 result files fresh. The first run
  found one more caller of the old `first()` (`funktor/auth/.../ActivateAccountPageSpec.kt:101`), fixed.
- [x] Mutation checks: TestBed without the unmount turns `TestBedSpec` and modal tests 15, 21, 22 RED (the leak the
  old `closeAll()` in a `finally` hid); `awaitCss` returning empty turns "awaitCss() fails on timeout" RED. The
  `awaitCss` mutant first crashed the incremental Kotlin/JS compiler ("Cannot deserialize inline function",
  `cssSelector`); it ran with `-Pkotlin.incremental.js=false -Pkotlin.incremental.js.klib=false
  -Pkotlin.incremental.js.ir=false`.
- [ ] Feature review.

## History

- 2026-10-24: created and implemented.
