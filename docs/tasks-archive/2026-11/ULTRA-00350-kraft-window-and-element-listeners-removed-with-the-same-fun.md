# ULTRA-00350: Kraft: window and element listeners removed with the same function reference (signaturepad, select field, examples)

> **Status (2026-11-23): DONE.** Built on master (`1d8e5c96`, the `jsObject` import in `191925af`). Gate: round 1 (with ULTRA-00349) no CRITICAL or MAJOR for this ticket.
>
> **Part of:** ULTRA-00258 (Epic: Kraft addons)

**Found by:** the review of ULTRA-00348 (three.js context loss), 2026-11-23, while checking the other addons.

## Why this exists

In Kotlin/JS every `::onWindowResize` is a new function object, so `removeEventListener("resize", ::onWindowResize)`
removes nothing: the listener added on mount outlives the component. The pattern ULTRA-00272 fixed in
`PopupsStage`. In `SignaturePad` (`kraft/addons/signaturepad`) every window resize after unmount still called the
caller's `onChange` with the dead pad, and kept the component and its canvas alive.

## Decided (maintainer, 2026-11-23)

"Yes file both as tickets and fix them now please."

## Built (2026-11-23, on master)

A repo-wide search for `add/removeEventListener(..., ::x)` found five places; each now keeps one reference in a field
and passes it to both calls:

| File | Listeners |
|---|---|
| `kraft/addons/signaturepad/src/jsMain/kotlin/SignaturePad.kt` | window `resize`, pad `endStroke` |
| `kraft/semanticui/src/jsMain/kotlin/forms/old/select/SelectFieldComponent.kt` | document `mousedown` |
| `kraft/examples/addons/src/jsMain/kotlin/signaturepad/SignaturePadExample.kt` | window `resize`, pad `endStroke` |
| `kraft/examples/addons/src/jsMain/kotlin/pixijs/BreakoutExample.kt` | window `keydown`, `keyup` |
| `kraft/core/src/jsMain/kotlin/utils/WindowController.kt` | not changed: adds only, never removes (app-wide) |

- `BreakoutExample` also destroys an `Application` whose page unmounted while `init` ran (from the review of
  ULTRA-00348, MINOR 6), and imports `Event` instead of naming it fully.
- Test (`SignaturePadLifecycleSpec`): mounted, a resize reports a change; unmounted, a resize reports none. RED on the
  old code (`expected:<1> but was:<2>`), green after.
- Not tested: the select field (no test of its own exists; the same fix as the pad) and the two examples (compiled).
- Counts (XML, `--rerun`): signaturepad 4, semanticui 31, 0 failures; `:kraft:examples:addons:compileKotlinJs` green.

## Acceptance

- No listener in Kraft is removed through a different function reference than it was added with.

## History

- 2026-11-23: filed on the maintainer's word and built on master.
- 2026-11-23: IN PROGRESS.
- 2026-11-23: done; gate closed.
- 2026-11-23: DONE.
