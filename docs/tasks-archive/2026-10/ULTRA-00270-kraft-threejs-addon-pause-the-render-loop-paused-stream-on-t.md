# ULTRA-00270: Kraft threejs addon: pause the render loop (paused stream), on the shared frame clock

> **Status (2026-10-29): DONE.** Request from Klang (Klang-Research, 2026-10-29, KLANG-00389). Plan written 2026-10-29; part of the frame-and-focus batch with ULTRA-00264, ULTRA-00267, ULTRA-00268 and ULTRA-00269.
>
> **Part of:** ULTRA-00258 (Epic: Kraft addons)

**Security-critical:** no

## Why this exists

Klang-Research, 2026-10-29 (KLANG-00389, the Spine): the maintainer wants all UI work to rest while the window has no
focus. Klang's Motor plate (`src/jsMain/kotlin/comp/MotorBackground.kt`) renders a full-screen three.js scene through
`ThreeJs(onReady, onFrame, createCamera)`, and nothing outside can stop its loop short of unmounting, which loses the
WebGL context and the textures.

Checked against the code, 2026-10-29 (`kraft/addons/threejs/src/jsMain/kotlin/ThreeJsComponent.kt`):

- **Holds:** `startLoop` (`:148-169`) requests a frame, calls `props.onFrame` and `renderer.render` unconditionally,
  and requests the next; only `stop()` on unmount (`:171-178`) cancels it.
- **Also:** it uses `window.requestAnimationFrame` directly, so it never shares a frame with the streams' clock
  (ULTRA-00269).
- **Testing limit:** headless Chrome has no WebGL here (`ThreeJsAddonSpec.kt:178-182`, the render row is disabled), so
  the loop must be testable without a renderer.

## Depends on / unblocks

- Uses ULTRA-00269 (shared animation frames). The pause signal for Klang is `windowCtrl.hasFocus`, which
  ULTRA-00268 (shared WindowController) makes reachable outside components.

## Decide before implementing

1. **The API.** Klang's first choice: `paused: Stream<Boolean>` (no frames at all while true); the second, `onFrame`
   returning "render this frame", still ticks. Recommendation: the stream, default a constant `false`; it fits the
   maintainer's "config is a stream" principle.
2. **Time across a pause.** Recommendation: the first frame after a resume has `deltaMs = 0`, and `elapsedMs` counts
   only running time (the sum of deltas), so a scene resumes where it rested instead of jumping. **Decided by the
   maintainer 2026-10-29: running time.**
3. **A frame clock prop.** `clock: FrameClock = FrameClock.animationFrames`, so the loop joins the shared frame and a
   spec drives it with a hand clock. Recommendation: yes.

## The work

1. Extract the loop into `internal class RenderLoop(clock: FrameClock, onFrame: (deltaMs: Double, elapsedMs: Double) -> Unit)`
   in `kraft/addons/threejs/src/jsMain/kotlin/RenderLoop.kt` with `start()`, `pause()`, `resume()`, `stop()`;
   timing per decision 2.
2. `ThreeJsComponent`: props `paused: Stream<Boolean>` and `clock: FrameClock`; subscribe to `paused` in the lifecycle,
   pause/resume the loop; `onFrame` then `renderer.render` from the loop. `Tag.ThreeJs(...)` gains both parameters
   with defaults (source-compatible).
3. Specs, `kraft/addons/threejs/src/jsTest/kotlin/RenderLoopSpec.kt` with a hand clock: frames while running;
   none requested while paused (the clock sees no `next()`); resume gives delta 0 and elapsed continuing; stop cancels
   the pending frame; pause then stop leaves nothing. RED rows: "no frame while paused" against an unconditional loop.
4. KDoc, the threejs addon docs page and LLM mirror; CHANGELOG.

## Acceptance

- `RenderLoopSpec` green, its pause row RED without the pause.
- The existing `ThreeJsAddonSpec` still green.

## Review record

In ULTRA-00264's Review record (the batch's gate, both groups).

## History

- 2026-10-29: request received, parked for the budget brake.
- 2026-10-29: created with its plan on the maintainer's go ("Start fully now").
- 2026-10-29: maintainer: across a pause `elapsedMs` counts running time only, the first frame after a resume has `deltaMs = 0`.
- 2026-10-29: gate PASS in round 2 (record in ULTRA-00264); open MINORs handed to the maintainer as a list. Docs written with the change, so no DOCS follow-up.
- 2026-10-29: DONE.
