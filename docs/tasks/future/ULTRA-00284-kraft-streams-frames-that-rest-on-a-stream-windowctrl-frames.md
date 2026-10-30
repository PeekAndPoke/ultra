# ULTRA-00284: Kraft/streams: frames that rest on a stream (windowCtrl.frames or FrameClock.restingWhile)

> **Status (2026-10-30): IDEA.** A request from Klang-Research (weekly meeting 2026-10-30, topic 2.3).
>
> **Part of:** ULTRA-00257 (Epic: Kraft forms and components)

## Why this exists

Klang's Spine (klang `src/jsMain/kotlin/spine/UiWork.kt`) gives the whole UI one frame stream that rests while the
window has no focus: `ticker(FrameClock.animationFrames).cutoffWhen(window.hasFocus.map { !it }.distinct())` on
Kraft's WindowController. Every app that wants "no UI frames while the window has no focus" writes the same lines.

Checked against the code (2026-10-30):
- `FrameClock.animationFrames` (`ultra/streams/src/jsMain/kotlin/ops/FrameClock.js.kt`) only batches callers into one
  `requestAnimationFrame` per frame. The browser stops rAF in a hidden tab, not in a visible window without focus,
  so such a window draws for no one.
- Klang's lines do rest: `cutoffWhen` unsubscribes its source while cut off (`ops/cutoff.kt`), so the ticker asks
  for no frame.
- `ThreeJs(paused = ...)` has the same shape for one component.

## Decide before implementing

- The shape: `windowCtrl.frames` (Kraft, rests while `!hasFocus`) or `FrameClock.animationFrames.restingWhile(stream)`
  (streams, any predicate), or both, the first built on the second.
- Focus or visibility as the default (`hasFocus` vs `isVisible`).
- Klang also asks for the unpaused stream next to it, for a part with its own pause (its `UiWork.allFrames`).

## The work

## Acceptance

- A spec per strategy: no frame requested while resting, frames again when the predicate turns, the unpaused stream
  unaffected. Headless Chrome has no focus: CDP `Emulation.setFocusEmulationEnabled` gives it one, or the
  TestWindowController.

## History

- 2026-10-30: created from Klang-Research's request; checked against `FrameClock.js.kt` and `cutoff.kt`.
