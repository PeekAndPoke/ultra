# ULTRA-00246: Kraft threejs addon: Object3D/Group/SpotLight/Raycaster constructors, missing members, a generic create

> **Status (2026-10-29): DONE.** Done 2026-10-29: gate PASS in round 2 (ULTRA-00264 Review record). Request from Spross (Spross-Main-Dev, 2026-10-26), found building the workshop's declarative scenes (SPROSS-00023, "Kulisse", SPROSS-00069). Klang fills other gaps of the same addon (`klang/src/jsMain/kotlin/externals/kraftThreejsExt/`): one ticket for both.
>
> **Part of:** ULTRA-00258 (Kraft addons epic)

**Security-critical:** no

## The request, checked against the code (2026-10-26)

From Spross (`workshop/.../scenes/three/js/_three_gaps.kt`, `ThreeNatives.kt`):
- **Constructors missing** on `ThreeJsAddon` (`kraft/addons/threejs/src/jsMain/kotlin/threejs_addon.kt`): `Object3D`,
  `Group`, `SpotLight`, `Raycaster` (the addon has scene, cameras, renderer, geometries, materials, lights).
- **Members missing** (`js/three-definitions.kt`): `Object3D` (`:54`) `renderOrder`, `parent`, `removeFromParent()`,
  `updateWorldMatrix(parents, children)`, `localToWorld(v)`, `lookAt(Vector3)` (only `lookAt(x, y, z)`, `:67`);
  `Light` (`:242`) `dispose()` and `shadow` (`LightShadow` with `mapSize`, `map`); `HemisphereLight.groundColor` (a
  constructor parameter only, `:273-277`); `Material.needsUpdate` (`:183`); `WebGLRenderer.shadowMap.enabled`
  (`:126`).
- **Suggestion:** a generic `create<T>(className, vararg args)` on the addon (Spross's `ThreeNatives` does it with
  `Reflect.construct`), so a missing constructor never blocks anyone. `raw` exists already (`threejs_addon.kt:40`).

From Klang (Klang-Main-Dev, 2026-10-26, confirmed as a request: "no reason for the three.js gap fillers to stay
in Klang"), in package `io.peekandpoke.kraft.addons.threejs` already:
- `src/jsMain/kotlin/externals/kraftThreejsExt/js/three-definitions-ext.kt`: `Vector2` (x, y, set, copy), `Texture`
  (wrapS, wrapT, magFilter, minFilter, needsUpdate, anisotropy, dispose), `CanvasTexture(canvas)`, the wrapping
  constants 1000..1002 and the filter constants 1003..1008.
- `threejs_addon_ext.kt`: `ThreeJsAddon.createVector2(x, y)`, `createCanvasTexture(canvas)` (the `js("new ctor(...)")`
  pattern a generic `create<T>` would replace).
- Klang's users: `MotorBackground.kt`, `index.kt`. Klang drops the folder once Ultra has released them.
- **License:** Klang's files carry its AGPL header (2026-04). Copying them into Apache-2.0 Ultra is the maintainer's
  call; the default is to rewrite them from three.js's own API (small). Spross's gap fillers are workshop code,
  Apache-2.0 by the workshop rules.

**Heads-up from Spross, not a request:** in headless Chrome on this machine three.js gets no WebGL context
("BindToCurrentSequence failed", also with SwiftShader flags), while pixi starts. Kraft's own three.js spec avoids
WebGL; Spross's `ThreeStageSpec` uses a fake renderer through an internal prop. Specs for this ticket need the same.

## Depends on / unblocks

- Same shape as ULTRA-00245 (pixi): `create<T>` written once in the same style for both addons.
- ULTRA-00270 (threejs pause) touches `ThreeJsComponent` in the same module; separate files.

## Decide before implementing

1. **Klang's AGPL files.** Not copied. They are small and follow three.js's own API, so they are rewritten from the
   three.js docs (types, members, constants), which is the default the ticket already names. No maintainer decision
   needed unless you want them copied.
2. **Spross's gap fillers** (`spross/workshop/.../scenes/three/js/_three_gaps.kt`, `ThreeNatives.kt`): Apache-2.0
   workshop code, taken as they are with a CREDITS line.
3. **Members, not extensions** (as ULTRA-00245); the constants as `const val`s in an `object ThreeConstants` (or on
   the addon) rather than bare numbers. Recommendation: `object TextureWrapping { Repeat = 1000, ClampToEdge = 1001,
   MirroredRepeat = 1002 }` and `object TextureFilter { 1003..1008 }`, named as three.js names them.
4. **WebGL in specs:** none here (headless Chrome has no context for three.js). Recommendation: specs check the
   objects three.js builds without a renderer (`Object3D`, `Group`, lights, `Raycaster`, textures from a canvas);
   the renderer's `shadowMap` member gets a compile-only use.

## The work

1. `kraft/addons/threejs/src/jsMain/kotlin/js/three-definitions.kt`:
   - `Object3D`: `renderOrder`, `parent`, `removeFromParent()`, `updateWorldMatrix(updateParents, updateChildren)`,
     `localToWorld(vector)`, `lookAt(vector: Vector3)` beside `lookAt(x, y, z)`;
   - `Light`: `dispose()`, `shadow: LightShadow` (`mapSize: Vector2`, `map`); `HemisphereLight.groundColor`;
   - `Material.needsUpdate`; `WebGLRenderer.shadowMap` (`enabled`, `type`);
   - new: `Group`, `SpotLight`, `Raycaster` (`setFromCamera`, `intersectObjects`), `Vector2` (x, y, set, copy),
     `Texture` (wrapS, wrapT, magFilter, minFilter, needsUpdate, anisotropy, dispose), `CanvasTexture`;
   - the constants objects of decision 3.
2. `kraft/addons/threejs/src/jsMain/kotlin/threejs_addon.kt`: `createObject3D()`, `createGroup()`,
   `createSpotLight(...)`, `createRaycaster()`, `createVector2(x, y)`, `createCanvasTexture(canvas)`, and
   `create<T>(className, vararg args)`.
3. Specs in `ThreeJsAddonSpec.kt`: each constructor gives an instance of the three.js class (`instanceof raw.X`);
   `removeFromParent`, `renderOrder`, `lookAt(Vector3)` and `localToWorld` behave as three.js documents (a child of a
   moved group maps its origin to the group's position); a `CanvasTexture` from a 2D canvas has `needsUpdate` settable;
   the constants equal three.js's (`raw.RepeatWrapping == 1000` etc.).
4. Docs: the threejs addon page and LLM mirror; CHANGELOG; CREDITS (Spross workshop).
5. Tell Klang and Spross which files they can drop after the release.

## Acceptance

- Every member Spross's and Klang's gap files use has a typed Ultra equivalent; neither project needs `asDynamic()`
  for them.
- `:kraft:addons:threejs:jsTest` green.

## Review record

In ULTRA-00264's Review record (the batch's gate, both groups).

## History

- 2026-10-26: created from Spross's request; Klang's extensions noted (the same addon).
- 2026-10-26: Klang confirmed its three.js extensions as part of the request; AGPL header noted.
- 2026-10-29: plan written (maintainer: "write a detailed plan into each ticket").
- 2026-10-29: DESIGNED.
- 2026-10-29: gate PASS in round 2 (record in ULTRA-00264); open MINORs handed to the maintainer as a list. Docs written with the change, so no DOCS follow-up.
- 2026-10-29: DONE.
- 2026-10-29: the planned CREDITS line is not written: ultra has not adopted CREDITS.MD (project-layout repo note). Spross's workshop code is credited in the CHANGELOG entry and the commit message instead.
