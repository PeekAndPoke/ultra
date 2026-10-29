# ULTRA-00245: Kraft pixijs addon: raw module, Container constructor, zIndex and the missing members

> **Status (2026-10-29): DESIGNED.** Plan written 2026-10-29, waiting for the maintainer's choices (Decide section). Request from Spross (Spross-Main-Dev, 2026-10-26), found building the workshop's declarative scenes (SPROSS-00023, "Kulisse", SPROSS-00069). Spross fills every gap in its `workshop/` today as extensions on Kraft's types, so the code is there to take.
>
> **Part of:** ULTRA-00258 (Kraft addons epic)

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

## Depends on / unblocks

- Same addon and same release as ULTRA-00262 (Klang's `Application.resize()`): one pass over `pixi-definitions.kt`.
- Same shape as ULTRA-00246 (three.js): `create<T>` and `raw` on both addons, written once in the same style.

## Decide before implementing

1. **Taking Spross's code.** `spross/workshop/` is Apache-2.0 (`spross/workshop/LICENSE`; spross `CLAUDE.md`: "bound
   for" Ultra, no header), so the gap fillers (`_pixi_gaps.kt`, `ObservablePoint.kt`, `PixiNatives.kt`) are taken as
   they are, with a CREDITS line. Recommendation: yes.
2. **Members or extensions.** Spross wrote extensions over `asDynamic()`. In Ultra they become real members of the
   `external class`es (typed, no dynamic). Recommendation: members.
3. **Constructors.** `createContainer()` like the existing `createGraphics()`, plus a generic
   `create<T>(className: String, vararg args: Any?): T` (via `Reflect.construct`) as the escape hatch, and `raw` like
   the three.js addon. Recommendation: all three.

## The work

1. `kraft/addons/pixijs/src/jsMain/kotlin/js/pixi-definitions.kt`: `external interface ObservablePoint { x, y, set }`;
   on `Container`: `zIndex`, `sortableChildren`, `scale: ObservablePoint`, `addChildAt(child, index)`; on `Text`:
   `anchor: ObservablePoint`; on `Graphics`: `poly(points: Array<Double>, close: Boolean = definedExternally)`;
   on `Application`: `backgroundColor` stays out (it is `renderer.background.color`: a `Renderer` binding, later).
   KDoc for `zIndex` names the trap: set before the node has a parent, it does not turn on the parent's
   `sortableChildren`.
2. `kraft/addons/pixijs/src/jsMain/kotlin/pixijs_addon.kt`: `val raw: dynamic`, `createContainer()`,
   `create<T>(className, vararg args)`.
3. Specs in `PixiJsContractSpec.kt` (no GPU needed for these: pixi starts in headless Chrome): each new member reads
   and writes through to pixi (`zIndex` sorts with `sortableChildren` after `render`-free `sortChildren()`,
   `addChildAt` puts the child at the index, `scale.set(2)` sets both, `anchor`, `poly` returns the graphics);
   `createContainer()` is a pixi `Container` (`instanceof raw.Container`); `create<Container>("Container")` too;
   `create` of an unknown class fails with the class name.
4. Docs: the pixijs addon page and LLM mirror; CHANGELOG; CREDITS (Spross workshop).
5. Tell Spross which of its workshop files it can drop after the release.

## Acceptance

- Spross's `_pixi_gaps.kt` and `PixiNatives.kt` have a typed Ultra equivalent for every member.
- `:kraft:addons:pixijs:jsTest` green.

## History

- 2026-10-26: created from Spross's request.
- 2026-10-29: plan written (maintainer: "write a detailed plan into each ticket").
- 2026-10-29: DESIGNED.
