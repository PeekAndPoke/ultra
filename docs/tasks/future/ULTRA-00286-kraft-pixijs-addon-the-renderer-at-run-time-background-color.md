# ULTRA-00286: Kraft pixijs addon: the renderer at run time (background color, resize to a given size)

> **Status (2026-10-30): IDEA.** A request from Spross (Spross-Main-Dev, 2026-10-30), the rest of ULTRA-00245, found
> moving spross's workshop onto 0.117.0 (SPROSS-00023). Spross's code is Apache-2.0, to take as is.
>
> **Part of:** ULTRA-00258 (Epic: Kraft addons)

## Why this exists

Spross's workshop still fills in two members as extensions on Kraft's `Application`
(spross `workshop/src/jsMain/kotlin/scenes/pixi/js/_pixi_gaps.kt`):

1. **The background color at run time:** `renderer.background.color`, read and set after `init()`. The options'
   `backgroundColor` is only the start; the stage's live background prop needs the setter.
2. **A resize to a given size:** `renderer.resize(width, height)` changes the canvas in place. Spross's StageFit
   computes the size (a fixed size, or a width with an aspect), which `resizeTo` cannot express.

Checked against the code (2026-10-30):
- `Application` (`kraft/addons/pixijs/src/jsMain/kotlin/js/pixi-definitions.kt:31`) has `stage`, `canvas`, `ticker`,
  `screen`, `init`, `render`, `resize()`, `queueResize()` and `destroy`. It has no `renderer`, so both needs go through
  `asDynamic()` today.
- `resize()` and `queueResize()` are the ResizePlugin's: they take the `resizeTo` target's size, as Spross says.
  `followElementSize` is built on them.
- `ApplicationOptions.backgroundColor` exists (line 21), for the start only.
- Pixi v8's `Renderer.resize(width, height, resolution?)` and `renderer.background.color` (a `Color`, with
  `toNumber()`) are taken from Spross's file and pixi's API. They are UNVERIFIED here, because web tools are blocked;
  check them against the pinned pixi.js 8.22.0 `.d.ts` before building.

Spross also noted, needing nothing: `Object3D.updateWorldMatrix`'s parameters are now `updateParents` and
`updateChildren` (they were `parents` and `children` in its gap file), and the nullable `Light.shadow` on the base class
is good.

## Decide before implementing

- The shape. Spross suggests `Application.renderer` with `resize(w, h)` and `background.color`, which follows pixi.
  The other choice is two members on `Application` (Spross's `backgroundColor` and `resizeCanvas`). Following pixi's
  shape keeps the definitions a mirror of the library.
- The color's type. Pixi's `ColorSource` takes a number or a string; Spross's getter returns `Int` through
  `toNumber()`.

## The work

## Acceptance

- A spec that inits an app, sets the background color and reads it back, and that resizes to a given size and reads
  `screen` and `canvas` sizes.

## History

- 2026-10-30: created from Spross's request.
