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

From Klang (not sent as a request; seen while checking): `Vector2`, `Texture` (wrap, filters, `needsUpdate`,
`anisotropy`, `dispose`), `CanvasTexture`, the wrapping and filter constants, plus `threejs_addon_ext.kt`.

**Heads-up from Spross, not a request:** in headless Chrome on this machine three.js gets no WebGL context
("BindToCurrentSequence failed", also with SwiftShader flags), while pixi starts. Kraft's own three.js spec avoids
WebGL; Spross's `ThreeStageSpec` uses a fake renderer through an internal prop. Specs for this ticket need the same.

## History

- 2026-10-26: created from Spross's request; Klang's extensions noted (the same addon).
