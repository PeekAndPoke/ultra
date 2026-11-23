# ULTRA-00348: Kraft threejs: ThreeJsComponent gives up its WebGL context when it stops

> **Status (2026-11-23): IDEA.** 
>
> **Part of:** ULTRA-00258 (Epic: Kraft addons)

**Requested by:** Klang-Research, 2026-11-23, from KLANG-00478 (klang's live act); klang's workaround is commit
`ae56c90e0` (`klangviz/src/jsMain/kotlin/liveact/LiveAct.kt`, `.../liveact/meet/MeetTheBand.kt`).

## Why this exists

A page with a `ThreeJs` component that can be left and entered again makes a new WebGL context on every visit and
gives none up. A browser allows only a handful of live contexts (Chrome warns "Too many active WebGL contexts.
Oldest context will be lost" at the 17th), and under memory pressure the current page's context can be taken.

Checked against the code (2026-11-23), both claims hold:

- `ThreeJsComponent.stop()` (`kraft/addons/threejs/src/jsMain/kotlin/ThreeJsComponent.kt:204-213`) unsubscribes,
  stops the render loop, calls `renderer.dispose()` and nulls its fields. Nothing else.
- three.js 0.186.1 (`Deps.kt:449`), `WebGLRenderer.dispose()`: removes the canvas's context listeners and frees
  programs, lists, properties and the XR state; it does not call `forceContextLoss()` (which exists,
  `WebGLRenderer.js:607`). So the context and its drawing buffer live until the detached canvas is collected.
- The Kotlin declaration of `WebGLRenderer` (`js/three-definitions.kt:165-174`) has no `forceContextLoss()`.

Klang's workaround: in the page's own `onUnmount`, `renderer.asDynamic().forceContextLoss()`; 30 visits in Chrome
without the warning.

## Decide before implementing

- The order in `stop()`: `dispose()` first removes three.js's own `webglcontextlost` listener, so a
  `forceContextLoss()` after it logs no "Context Lost" and triggers no handler; the other order works too.
  Proposed: `dispose()`, then `forceContextLoss()`.
- Whether a component whose props change restarts through `stop()` and so gives up a context it could have kept.

## The work

- `ThreeJsComponent.stop()` calls `renderer.forceContextLoss()`.
- A typed `fun forceContextLoss()` on the `WebGLRenderer` declaration (and `ThreeJsMembersSpec` checks it exists).
- A browser test: mount and unmount the component more often than the browser's context limit; the first
  renderer's context reports `isContextLost()`.
- Changelog, and a reply to Klang when a release has it, so klang's two workaround lines can go.

## Acceptance

- After the component stops, its WebGL context is lost; mounting it repeatedly raises no "Too many active WebGL
  contexts" warning.

## History

- 2026-11-23: filed from Klang-Research's request; checked against the code.
