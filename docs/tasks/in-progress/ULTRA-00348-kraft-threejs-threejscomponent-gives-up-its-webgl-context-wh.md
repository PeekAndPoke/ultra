# ULTRA-00348: Kraft threejs: ThreeJsComponent gives up its WebGL context when it stops

> **Status (2026-11-23): IN PROGRESS.** Scheduled by the maintainer ("We will come back to the container work after the threejs fix"). Built on master, gate pending.
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

## Built (2026-11-23, on master)

- `ThreeJsComponent.stop()` (on unmount): `renderer.dispose()`, then `renderer.forceContextLoss()`; after `dispose()`
  three.js has removed its own context listeners, so the loss fires no handler. KDoc of `ThreeJs` says it.
- `WebGLRenderer.forceContextLoss()` declared, with a KDoc on it and on `dispose()`.
- Tests: `ThreeJsComponentSpec` "unmounted, the component disposes its renderer and gives up the WebGL context, on every
  visit": a fake three.js module records the teardown, mounted, unmounted, mounted and unmounted again: `["dispose",
  "forceContextLoss"]` twice. `ThreeJsMembersSpec` "WebGLRenderer still has forceContextLoss": three.js defines it
  inside the constructor, and constructing a renderer needs WebGL, which headless Chrome may not give, so the bundled
  source is read.
- Not tested in a real browser here: no test creates a real WebGL context (headless Chrome does not reliably give one;
  `ThreeJsAddonSpec` keeps its render test disabled for that reason). Klang's manual check (30 visits in Chrome, no
  warning) covers the same call.
- Mutation: `forceContextLoss()` removed from `stop()`: RED, 1 of 24, `expected:<["dispose", "forceContextLoss"]> but
  was:<["dispose"]>`; restored byte-identical (`cmp`).
- Counts (XML, `--rerun`): `:kraft:addons:threejs:jsBrowserTest` 24, `:kraft:addons:pixijs:jsBrowserTest` 21, 0
  failures; `:kraft:examples:addons:compileKotlinJs` green.

### The other addons, checked (maintainer: "check if any other addon like pixijs has the same issue")

| Addon | Context | Verdict |
|---|---|---|
| pixijs | WebGL (or WebGPU), pixi 8.22.0 | no leak in pixi: `Application.destroy()` destroys the renderer, whose context system calls `WEBGL_lose_context.loseContext()` (`GlContextSystem.mjs`, `destroy()`), whatever the argument. The addon has no component: the caller owns the `Application` and must call `destroy()` on unmount. Nothing said so: the KDoc of `createApplication()` and of `Application.destroy()` now does. The Breakout example and the specs already call it. |
| chartjs, pdfjs, signaturepad | canvas 2D | not affected: a 2D context has no per-page limit like WebGL's |
| visnetwork | canvas 2D (vis-network draws on its own canvas) | not affected, same reason |
| avatars, browserdetect, datetime, jwtdecode, marked, nxcompile, prismjs, sourcemappedstacktrace | no canvas | not affected |

## Acceptance

- After the component stops, its WebGL context is lost; mounting it repeatedly raises no "Too many active WebGL
  contexts" warning.

## History

- 2026-11-23: filed from Klang-Research's request; checked against the code.
- 2026-11-23: scheduled by the maintainer; built on master; the other addons checked.
- 2026-11-23: IN PROGRESS.
