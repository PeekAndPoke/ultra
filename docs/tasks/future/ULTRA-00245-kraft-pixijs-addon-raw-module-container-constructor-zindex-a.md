# ULTRA-00245: Kraft pixijs addon: raw module, Container constructor, zIndex and the missing members

> **Status (2026-10-26): IDEA.** Request from Spross (Spross-Main-Dev, 2026-10-26), found building the workshop's declarative scenes (SPROSS-00023, "Kulisse", SPROSS-00069). Spross fills every gap in its `workshop/` today as extensions on Kraft's types, so the code is there to take.

**Security-critical:** no

## The request, checked against the code (2026-10-26)

- **No `raw` module handle.** `PixiJsAddon` (`kraft/addons/pixijs/src/jsMain/kotlin/pixijs_addon.kt:19-44`) keeps
  `pixiModule` private and offers only `createApplication`, `createGraphics`, `createText`; the three.js addon has
  `raw` (`threejs_addon.kt:40`). Spross imports `pixi.js` a second time (`workshop/src/jsMain/kotlin/scenes/pixi/js/PixiNatives.kt`).
- **No plain `Container` constructor** on the addon.
- **Missing members** on `Container` (`js/pixi-definitions.kt:84-106` has x, y, visible, alpha, rotation, children,
  addChild, removeChild, removeChildren, removeFromParent, destroy): `zIndex`, `sortableChildren`,
  `scale: ObservablePoint`, `addChildAt(child, index)`; on `Text`: `anchor`; on `Graphics`: `poly(points, close)`.
  Spross's versions: `workshop/.../scenes/pixi/js/_pixi_gaps.kt`, `ObservablePoint.kt`.
- **A trap for the KDoc** (pixi 8.22): a `zIndex` set before the node has a parent does not turn on the parent's
  `sortableChildren`.

## Notes

- Klang pins `addons-pixijs` too (klang `buildSrc/.../Deps.kt:83`).
- Take Spross's code (Apache-2.0 workshop) with its tests when this is picked up.

## History

- 2026-10-26: created from Spross's request.
