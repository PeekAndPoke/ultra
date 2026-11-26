# ULTRA-00348: Kraft threejs: ThreeJsComponent gives up its WebGL context when it stops

> **Status (2026-11-23): DONE.** Built on master (`b8f3995b`, `e324951c`). Gate: round 1 NOT CLEAN only for two older leaks in other addons, filed and fixed as ULTRA-00349 and ULTRA-00350; the round 1 fixes reviewed in their round 1 (no CRITICAL or MAJOR). Klang is told once the maintainer has published a release with it.
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

## Review record

### Round 1 (2026-11-23, level 1 of a standard gate, `reviewer-opus-high`, all three charters in one pass)

**NOT CLEAN**, for two MAJORs outside the diff, both older than this change and found by the check of the other addons;
the change itself had no CRITICAL or MAJOR. The reviewer ran nothing. Both MAJORs verified against the code by the
coordinator:

- MAJOR 1, pdfjs: neither viewer destroys its document (`PagedPdfViewer.kt`, `ScrollingPdfViewer.kt` have no unmount
  cleanup; `PDFDocumentProxy` declares no `destroy`), and pdf.js makes a new `PDFWorker` per `getDocument` while
  `GlobalWorkerOptions.workerPort` is unset (`pdf.mjs:11412-11414`): every mount leaves a worker. Out of scope: needs
  its own ticket (asked the maintainer).
- MAJOR 2, signaturepad: `SignaturePad.kt:115` adds `::onWindowResize`, `:124` removes another `::onWindowResize` (each
  `::x` is a new function object), the same for `endStroke` (`:158`, `:127`): the window listener outlives the pad and
  calls the caller's `onChange` with a dead pad. The pattern of ULTRA-00272. Also in
  `kraft/semanticui/.../SelectFieldComponent.kt:284` and two examples. Out of scope: needs its own ticket (asked the
  maintainer).
- MINOR 3, the source check matched one spelling only: FIXED, it accepts a constructor field or a class method. The
  experiment the reviewer proposed was run: the test browser gives no WebGL context at all (`webgl2=false
  webgl=false`, HeadlessChrome 158, despite `karma.config.d/webgl.js`), so no real-context test is possible here.
- MINOR 4, the second visit's renderer was not shown to be new: FIXED, constructions are counted (2).
- MINOR 5, `stop()` unguarded: FIXED, the fields are cleared first and `forceContextLoss()` runs in a `finally` after
  `dispose()`; the renderer is owned right after construction. Not tested: TestBed reports an error thrown in an
  unmount on its own, so a throwing-`dispose()` test cannot be written cleanly; its mutant (no `finally`) was seen to
  drop the context loss.
- MINOR 6, pixi unmount during `init`: FIXED in the KDoc of `createApplication()`. The Breakout example has the race;
  it goes with the follow-up for MAJOR 2, which touches that example anyway.
- MINOR 7, the open question of "Decide before implementing": answered by the code, `stop()` runs only on unmount; a
  new clock or `paused` stream never stops the renderer.

The fix batch was read by the coordinator, not by a reviewer. Mutation on the final code: `forceContextLoss()` removed
from `stop()`: RED, `["dispose"]`; restored byte-identical. Counts (XML, `--rerun`): threejs 24, pixijs 21, 0 failures.

## Acceptance

- After the component stops, its WebGL context is lost; mounting it repeatedly raises no "Too many active WebGL
  contexts" warning.

## History

- 2026-11-23: filed from Klang-Research's request; checked against the code.
- 2026-11-23: scheduled by the maintainer; built on master; the other addons checked.
- 2026-11-23: IN PROGRESS.
- 2026-11-23: done; gate closed.
- 2026-11-23: DONE.
