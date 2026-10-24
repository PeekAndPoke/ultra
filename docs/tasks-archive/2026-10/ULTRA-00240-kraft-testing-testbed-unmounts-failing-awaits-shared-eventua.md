# ULTRA-00240: Kraft testing: TestBed unmounts, failing awaits, shared eventually, KQuery members

> **Status (2026-10-24): DONE.** From the TestBed/KQuery findings of ULTRA-00233 (every modal answers once). The maintainer (2026-10-24): "do the testbed ticket first. When this is done we will prepare a release."

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
- [x] Feature review: gate PASS (record below).

## Review record

Feature review, 2026-10-24, against commit 8b241af1. Round 1, blind, one `reviewer-high`: implementation & code style
with API fit. Scoped by risk: test tooling only, no security charter. No CRITICAL or MAJOR, so round 1 is clean and
the gate PASSES; the MINORs were applied.

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | MINOR | `TestBed.kt:113-118` | Background work started by unmount hooks (a trap's queued `history.back()`) can reach the next test | fix: `delay(1)` and two animation frames after the unmount | Timing race, not reproduced; the settle is cheap |
| 1 | MINOR | `TestBed.kt:114-115` | An unmount error only logs, so a leaking test stays green | fix: rethrown when the test passed, else added as suppressed | |
| 1 | MINOR | `TestBedSpec.kt` | Nothing checks the unmount after a FAILING test; unmount-in-try survives | fix: test "unmounts after a failing test too"; that mutant RED | Checked by mutation |
| 1 | MINOR | `CHANGELOG.md` | `.first()` followed by `as`, `unsafeCast`, `shouldBe`, `shouldNotBeNull()` compiles and changes meaning | fix: listed | |
| 1 | MINOR | `kquery_select.kt:66,87` | `intervalMs <= 0` loops forever | fix: `require(intervalMs > 0)` | Pre-existing in `awaitCss` |
| 1 | MINOR | `TestBed.kt:19-26` | The class KDoc attached to nothing (a helper sat between) | fix: helper moved above | Pre-existing layout |
| 1 | note | `eventually.kt` | kotest has its own `eventually` | keep ours | kraft/testing has no kotest dependency; recorded in the ticket's decision |

CLEAN (probed, held): TestBed order and a failed mount inside the try; `render(null)` on a partial root; unmount is
synchronous; `awaitCss` / `awaitGone` bounds; `eventually` lets cancellation through via `delay`; the 12 removed copies
had identical defaults; KQuery members valid with `out E`; `hasFocus`; CHANGELOG and docs match; code style.
Pre-existing, out of scope: `kraftApp(appSetup)` outside the try leaks the div on a failing setup; the Router's window
listeners are never removed.

## History

- 2026-10-24: created and implemented.
- 2026-10-24: feature review clean (gate PASS); 6 MINORs applied; all 3493 JS tests green. DONE. Docs were updated
  in the change itself (testing page, LLM mirror), so no separate DOCS follow-up.
- 2026-10-24: DONE.
