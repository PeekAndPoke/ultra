# Phase 2 — `funktor/codegen`: the REST contributor and the generate CLI

**Status:** IN PROGRESS
**Plan:** `.claude/tasks/20260729-ts-sdk-codegen.md` → Phase 2 (§2.1–2.4)
**Security-critical:** no — dev-time code generation, never on a production server's classpath.

## Decisions taken with the maintainer, 2026-07-30

Settled before any code was written, because all three touch every emitted file.

### 1. The generated client mirrors the backend structure — classes, not factories

`ApiFeature` → one client class, `ApiRoutes` group → one group class, route → one member. Nothing is
invented, so a frontend dev reading `FunktorConfApi.kt` finds the same names in `FunktorConfApi.ts`.

Chosen over a factory returning an object literal for three reasons the maintainer named: a class is a
**named nominal type for free** (a factory needs `ReturnType<typeof x>` at every Vue `inject` key and
in every hover), IDE navigation and structure view work on class members, and it requires no
restructuring of the backend shape.

Two constraints came out of running the pinned compiler rather than reasoning about it:

- **Parameter properties are banned.** `constructor(private readonly config: SdkConfig)` is
  **TS1294** under `erasableSyntaxOnly` (`ts-verify/tsconfig.json:13`). Generated classes declare the
  field and assign it in the constructor body.
- **Members are arrow-function class fields, not prototype methods.** A prototype method type-checks
  when destructured and then throws at run time — `const { getEvent } = client.funktorConf` is the
  Vue-composable idiom, and it is wrong-and-quiet, the failure class this module exists to remove.
  Measured:

  ```
  tsc exit=0                                        ← both styles compile
  method destructured THREW: Cannot read properties of undefined (reading 'config')
  arrow destructured:  https://api.example.com/events/d
  ```

  Arrow class fields are standard JS class fields, so they survive `erasableSyntaxOnly` and Node's
  type stripping. Verified: tsc exit 0, runtime correct.

Group classes are the primary unit and are separately importable; the feature-level aggregate is a
convenience. That matters for bundling — an aggregate that constructs every group retains all of them.

### 2. Endpoints return the `ApiResponse<T>` envelope

Non-2xx comes back as a value, never a throw. This is the contract `runtime/http.ts:39-47` already
documents and defends on the Kotlin side, and it keeps `messages` / `insights` reachable at the call
site. Throwing is opt-in per call through a runtime `unwrap` helper.

Note for anyone tempted to revisit: unwrapping would **not** remove the null check. `data` is nullable
on 2xx too — `noContent()` and `okOrNotFound()` both send `null` — so the payload type stays
`T | null` either way.

### 3. Relative imports in emitted files carry a `.ts` extension

Found while probing, and it is a **latent defect in the existing generator**, not a new concern:
`MpDateTimeTsContributor.kt:30,48` claims `importFrom = "./runtime/datetime"`, extensionless.

```
--- node extensionless ---   Error [ERR_MODULE_NOT_FOUND]: Cannot find module '.../sub/dep'
--- node with .ts ---        with .ts: hi
```

`tsc` exits 0 for **both** under `moduleResolution: bundler`. Node's type stripping resolves only the
second. The harness never caught it because **no generated fixture imports a runtime module** — all
four are copied into `generated/runtime/` and type-checked, but nothing imports one, so the specifier
had only ever been resolved by tsc's bundler mode.

`.ts` extensions resolve identically under bundler, node16/nodenext, Node type stripping and Vite, so
ts-verify can **execute** generated clients rather than only type-check them. The cost is
`allowImportingTsExtensions: true` in the consuming app's tsconfig — which requires `noEmit`, which a
Vite app sets anyway, and which is exactly what the planned `requires(...)` check reports.

### 4. URL parameters are typed precisely where provable, refused otherwise (2026-07-30)

Path and query parameters go through funktor's `OutgoingConverter`, which turns **every** value into a
String (`funktor/core/src/jvmMain/kotlin/broker/OutgoingConverter.kt:29`). So the TypeScript type of a
parameter is its **wire** type, not its Kotlin type — `EventParam(val id: Stored<Event>)` is
`{ id: string }` on the wire, because the converter writes the entity's id.

Decision: map the types whose wire form is provable — `String`, the numeric types, `Boolean`, enums,
and value classes over those — and **refuse anything else, naming the parameter and its type**. Chosen
over emitting `string` for whatever `OutgoingConverter.canHandle` accepts, and over typing everything
as `UrlParam`. Consistent with the rest of the module: wrong-and-loud, never wrong-and-quiet.

**Consequence, and it is not optional: entity-binding params do not work until a param-claim
mechanism exists.** `Stored<T>` is the common case and the demo uses it on nearly every detail route.
Sketch, to be designed properly rather than assumed:

- A URL-parameter type is a DIFFERENT axis from a body-shape claim. `MpInstant` claims an object shape
  for JSON, but as a URL parameter it is a string. So this is either a new optional field on
  `TsTypeClaim` (`urlParam: "string" | "number" | "boolean"`) or its own registry.
- One registry is preferable — one conflict-detection path, one "claimed twice" message.
- `Stored<T>` is claimed by class, so one claim covers every instantiation.
- Who claims it is a funktor-side question (`funktor:rest`, or a vault contributor).

## Spec

- [x] **DONE `e924ac53`** — emitted relative imports carry `.ts`; the `dated` fixture imports a runtime
      module **at run time**, closing the hole above.
- [x] **DONE `b9669ae8`** — `runtime/client.ts` (`SdkConfig`, `request`, `ApiProtocolError`, `ApiError`,
      `unwrap`) plus `TsRuntime.Module.requires` and the closure in `emit`.
- [x] **DONE** — `TypeModel.rootRefs` / `refForRoot`, and a duplicate-root-label check in
      `TsSdkBuilder` naming both contributors.
- [x] **DONE** — `funktor/codegen` module: `ultra:codegen` + `funktor:rest`, deliberately NOT part of
      `funktor/rest` (that ships in every production server). Plain `kotlin("jvm")`, not multiplatform.
- [x] **DONE** — `TsClientEmitter` / `TsClientSpec` on the **ultra** side; `RestApiTsContributor` only
      walks and names. See "the split" below.
- [x] **DONE** — `RestApiTsContributor`, **profile-shaped from day one**: `include: (ApiRoute<*>) -> Boolean`
      defaulting to all routes, and a test drives it off `CodeGenHints.tags`.
- [ ] All five `ApiRoute` variants. **Only `Plain` is implemented**; the other four are REJECTED with a
      message naming the route, rather than silently emitting a half client.
- [ ] Path vs query split from `TypedRoute.parsedUriParams`; URL building **matches
      `TypedRouteRenderer`** (`funktor/core/src/jvmMain/kotlin/broker/TypedRouteRenderer.kt:28`).
      There is no `UriParamBuilder` — earlier plan drafts named one that does not exist.
- [ ] `TsSdkGenerateCliCommand` — clikt, `sdk:ts:generate`, `--out --dry-run --check --verbose`.
- [ ] `funktorCodegen()` kontainer module; `dynamic(TsSdkBuilder::class)` deliberately.
- [ ] `instance(codecConfig)` added to `Funktor_Rest` — the one authorized change to that module.

## Implementation notes

### The split: rendering lives in `ultra/codegen`, walking in `funktor/codegen`

`TsClientEmitter` + `TsClientSpec` (`ultra/codegen/src/main/kotlin/ts/TsClientEmitter.kt`) render a
framework-neutral client description; `RestApiTsContributor` builds one from an `ApiFeature`.

The forcing reason is verification, not tidiness: `ts-verify` lives in `ultra/codegen`, so a renderer
that could only be driven from `funktor/codegen` would need a second copy of the whole pinned
toolchain to prove anything about its output. With the split, the emitted client is compiled AND
executed by the existing harness. It also happens to be the right seam for the Vue contributors, which
will emit against the same renderer.

### The envelope must be unwrapped — found by fixtures, not by reading

A route's `responseType` is the **envelope**, not the payload. A handler returns `ApiResponse.ok(x)`,
so `RESPONSE` binds to `ApiResponse<List<Talk>>`. Measured:

```
PROBE route=/api/fx/talks responseType=io.peekandpoke.ultra.remote.ApiResponse<kotlin.collections.List<...FxTalkModel>>
```

The first version rooted that directly, which would have walked the hand-written envelope into
`models.ts` and made `request` wrap it a **second** time — every parse failing against output no
server produces. `payloadTypeOf` now unwraps it and fails loudly on a non-envelope or a star
projection.

### `TypeModel.rootRefs`

`TypeWalker.walk` used to discard each root's resolved reference ("a root is a position, not a
declaration"). A client member needs exactly that reference — `List<Talk>` declares nothing of its
own, so `z.array(Talk)` can only come from the ref. Re-deriving it at emit time was the obvious
alternative and the wrong one: two resolution paths drift, and the emitted schema would stop
describing what the walk validated.

Root labels are therefore unique-by-contract, and `TsSdkBuilder` rejects a collision naming both
contributors. That check immediately caught a real one — both helpers in `TsSdkBuilderSpec` labelled
their root `"root"`.

### Emitted shape

```ts
export class FxTalksApi {
    private readonly config: SdkConfig

    constructor(config: SdkConfig) {
        this.config = config
    }

    /** List all talks */
    readonly listTalks = () =>
        request(this.config, 'GET', '/api/fx/talks', z.array(FxTalkModel))
}
```

Claimed types are imported from their own runtime module, never from `models.ts` — `models.ts`
imports them, it does not re-export them.

## Test evidence

- [x] Unit/behaviour tests — `RestApiTsContributorSpec` (11), `TsSdkBuilderSpec` root-reference and
      label cases, `TsRuntimeSpec` extension and closure cases.
- [x] ts-verify fixture that **executes** a generated client — `fxDemoClient.ts` is constructed
      against a stub transport and called, covering the class shape, imports resolving, the schema
      expression, `request`, and the envelope parse.
- [x] Mutation-tested. Every guarantee below was broken and confirmed red:
      prototype methods instead of arrow fields (**tsc stays silent**; only execution catches it),
      claimed types imported from `models.ts` (TS2459), envelope not unwrapped, unsupported variants
      accepted, `include` ignored, duplicate members allowed, root label unqualified, duplicate-label
      check removed, root refs not recorded, extensionless specifiers.
- [x] `./gradlew :ultra:codegen:check :funktor:codegen:check` — **215 + 11 tests, 0 failures**,
      10 ts-verify fixtures.
- [x] Compile sweep (6 targets, `--continue`) — no `^e:`.

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | | |
| 2. Domain expert | | |
| 3. Security | | |

Fixes applied: ...
