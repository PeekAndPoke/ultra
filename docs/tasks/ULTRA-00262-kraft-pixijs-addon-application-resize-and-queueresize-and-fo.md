# ULTRA-00262: Kraft pixijs addon: Application.resize() and queueResize(), and following the element itself

> **Status (2026-10-29): DESIGNED.** Plan written and decided 2026-10-29. Request from Klang (Klang-Research, 2026-10-27), found in KLANG-00349. Not scheduled; Klang is unblocked with a workaround. Releases are rare (Maven Central limits): when picked up, consider growing it in a workshop module first.
>
> **Part of:** ULTRA-00258 (Epic: Kraft addons)

**Security-critical:** no

## Why this exists

Klang-Research, 2026-10-27 (KLANG-00349): the binding of pixi's `Application` has no `resize()` or `queueResize()`,
and `resizeTo = element` only follows the window's `resize` event. A host element resized by layout (a panel docked
under it, a splitter drag) leaves the canvas at its old size; in klang the code editor then scrolled a second time.
Klang's workaround: a rAF-coalesced ResizeObserver on the element calling `app.asDynamic().resize()`
(klang `src/jsMain/kotlin/codemirror/CodeMirrorHighlightBuffer.kt`).

Checked against the code, 2026-10-27:

- Holds: `external class Application` (`kraft/addons/pixijs/src/jsMain/kotlin/js/pixi-definitions.kt:31`) has
  `init`, `ticker`, `destroy` and the rest, but neither `resize` nor `queueResize`; `ApplicationOptions.resizeTo` is
  there (`:27`).
- Holds: pixi.js 8.22.0 (`Deps.kt:477`), `lib/app/ResizePlugin.mjs`: the plugin adds `resize` and `queueResize` to the
  app (`:29`, `:42`) and listens only to `globalThis` `resize` (`:20`); no ResizeObserver. `_cancelResize` is
  private and stays out of the binding.
- Our own spec shows the gap: `PixiJsContractSpec.kt:140` ("resizeTo follows the element (ResizePlugin)") calls
  `app.asDynamic().resize()` by hand (`:151`). Its name reads as if the element were followed; it isn't.

## Decide before implementing

1. **The binding:** `fun resize()` and `fun queueResize()` on `Application`, KDoc saying they exist only when
   `resizeTo` is set (the ResizePlugin adds them in `init`). Recommendation: yes, and drop the `asDynamic()` in the spec.
2. **Following the element:** Klang suggests an option on the addon that observes the element (a ResizeObserver
   inside, rAF-coalesced through `queueResize()`) and disconnects on `destroy`. Recommendation: yes, opt-in, as a
   Kotlin-side helper rather than a fake pixi option; the name and where it lives (on `createApplication` or a
   separate `followElementSize(app, element)`) still open.

## The work

1. Binding members plus KDoc; the spec calls them typed.
2. The element follower, with a browser spec: resize the host by style alone (no window event), await a frame, the
   screen follows; after `destroy` the observer is gone.
3. Docs: the pixijs addon page and its LLM mirror.

## Acceptance

- `app.resize()` compiles without `asDynamic()`; the contract spec renamed to say what it checks.
- The follower spec is RED without the observer.

## History

- 2026-10-27: created from Klang's request; checked against the code the same day.
- 2026-10-29: DESIGNED.
