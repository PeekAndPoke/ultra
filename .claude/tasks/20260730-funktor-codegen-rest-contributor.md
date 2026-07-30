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

## Spec

- [ ] Emitted relative imports carry `.ts`; a fixture actually imports a runtime module **at run time**,
      closing the hole above.
- [ ] `runtime/client.ts` — `SdkConfig`, `request(...)`, `ApiError`, `unwrap(...)`. Hand-written,
      shipped as a resource like the other runtime modules.
- [ ] `TsRuntime.Module` gains a dependency closure — `Client` needs `Http` + `ApiResponse`, and
      `emit` today takes a flat set with no closure, so asking for one would ship a broken SDK.
- [ ] `funktor/codegen` module: depends on `ultra:codegen` + `funktor:rest`, deliberately NOT part of
      `funktor/rest` (that ships in every production server).
- [ ] `RestApiTsContributor`, **profile-shaped from day one** — a root predicate as a constructor
      parameter defaulting to "all routes", reading `CodeGenHints.tags`.
- [ ] All five `ApiRoute` variants: `Plain`, `WithParams`, `WithBody`, `WithBodyAndParams`, `Sse`.
- [ ] Path vs query split from `TypedRoute.parsedUriParams`; URL building **matches
      `TypedRouteRenderer`** (`funktor/core/src/jvmMain/kotlin/broker/TypedRouteRenderer.kt:28`).
      There is no `UriParamBuilder` — earlier plan drafts named one that does not exist.
- [ ] `TsSdkGenerateCliCommand` — clikt, `sdk:ts:generate`, `--out --dry-run --check --verbose`.
- [ ] `funktorCodegen()` kontainer module; `dynamic(TsSdkBuilder::class)` deliberately.
- [ ] `instance(codecConfig)` added to `Funktor_Rest` — the one authorized change to that module.

## Implementation notes

## Test evidence

- [ ] Unit/behavior tests
- [ ] ts-verify fixture that executes a generated client
- [ ] Full test command(s) run + green: `...`

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | | |
| 2. Domain expert | | |
| 3. Security | | |

Fixes applied: ...
