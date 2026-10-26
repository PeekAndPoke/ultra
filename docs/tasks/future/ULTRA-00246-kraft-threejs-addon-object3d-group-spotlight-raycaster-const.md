# ULTRA-00246: Kraft threejs addon: Object3D/Group/SpotLight/Raycaster constructors, missing members, a generic create

> **Status (2026-10-26): IDEA.** Request from Spross (Spross-Main-Dev, 2026-10-26), found building the workshop's declarative scenes (SPROSS-00023, "Kulisse", SPROSS-00069). Klang fills other gaps of the same addon (`klang/src/jsMain/kotlin/externals/kraftThreejsExt/`): one ticket for both.

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

## History

- 2026-10-26: created from Spross's request; Klang's extensions noted (the same addon).
- 2026-10-26: Klang confirmed its three.js extensions as part of the request; AGPL header noted.
