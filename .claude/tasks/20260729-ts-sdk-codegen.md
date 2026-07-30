# TypeScript SDK code generation (`ultra/codegen` + `funktor/codegen`)

**Status:** TODO **Plan:** `.claude/tasks/v1-roadmap.md` → Post-v1 Backlog item 4 ("Dart codegen rewrite") —
**superseded by this task**
**Security-critical:** no (dev-time generator; no runtime auth path. See "Security notes" for why, and the one thing
that is worth a conscious decision.)

## Why

Kraft frontends are type-safe and ergonomic, but the dev loop is slow compared to a Vite/HMR frontend. A generated
TypeScript SDK lets a TS/Vue/React frontend consume the funktor API with full type safety while keeping HMR — the only
slow step becomes regenerating the SDK, which happens only when the API changes.

Everything needed is already available at runtime:

- `ApiFeature.getRouteGroups()` → `ApiRoutes.all` → `List<ApiRoute<*>>` is the complete endpoint inventory (15
  `ApiFeature` implementations, 89 `.mount(` sites in this repo today).
- Every route carries `TypeRef` (a `KType` wrapper) for PARAMS / BODY / RESPONSE
  (`funktor/rest/src/jvmMain/kotlin/ApiRoute.kt:46`, `:364`, `:439`), plus `method`, `pattern`, and
  `TypedRoute.parsedUriParams` which already separates path params from query params
  (`funktor/core/src/jvmMain/kotlin/broker/TypedRoute.kt:30`).
- `ReifiedKType.ctorFields2Types` (`ultra/reflection/src/main/kotlin/ReifiedKType.kt:95`) resolves generic type
  arguments — the same machinery Slumber serializes with, so an emitter driven from it matches the wire format by
  construction.
- Polymorphism maps 1:1 onto TS discriminated unions via `PolymorphicParentUtil.getChildren()` /
  `getDiscriminator()` / `PolymorphicChildUtil.getIdentifier()`
  (`ultra/slumber/src/jvmMain/kotlin/builtin/polymorphism/Polymorphic.kt:113-211`).
- `CodeGenHints` (`funktor/rest/src/jvmMain/kotlin/docs/CodeGenHints.kt`) already exists and is used at
  ~30 call sites in the demo.

## Scope

**In:**

- Delete the Dart codegen entirely (Phase 0).
- New `ultra/codegen` module: printer, TS AST, language-neutral type model + walker, contributor interface, builder,
  validation, datetime contributor, hand-written TS runtime resources.
- New `funktor/codegen` module: `RestApiTsContributor`, CLI command, kontainer module.
- Wire into `funktor-demo` and generate a working SDK for at least one feature end to end.

**Out:**

- Porting the Dart emitter (deleted, not migrated — see Phase 0 rationale).
- Retrofitting `ReflectivePathFinder` onto the new walker (follow-up, see "Follow-ups").
- Publishing the SDK as an npm package (`--package` flag deferred; bare `.ts` output only).

---

## Phase 0 — Delete the Dart codegen

Decision (2026-07-29, maintainer): **throw it away, do not port.** No downstream consumer needs it, and a new version
can be built on the new structure at any time. Keeping it would mean maintaining 3 kLOC through the `ultra/codegen`
refactor for zero current benefit.

This also removes dead weight from a **runtime** dependency: `funktor/rest` is on the classpath of every production
funktor server, and the Dart emitter is compiled into all of them.

**Delete:**

- [x] `funktor/rest/src/jvmMain/kotlin/codegen/` — entire directory, 2808 LOC (`dart/`, `dart/addons/`, `dart/printer/`,
  plus `index.kt`, `tags.kt`, `utils.kt`) — DONE 2026-07-29
- [x] `funktor/rest/src/jvmTest/kotlin/codegen/` — entire directory, 295 LOC — DONE 2026-07-29
- [x] `funktor/rest/build.gradle.kts:81` — `implementation(Deps.JavaLibs.diffutils)` in `jvmTest`
  (only consumer was `jvmTest/kotlin/codegen/index_codegen.kt`) — DONE 2026-07-29

**Keep — do NOT delete:**

- [x] `funktor/rest/src/jvmMain/kotlin/docs/CodeGenHints.kt` — the per-route `codeGen { }` hints. Used at
  ~30 sites (`FunktorConfApi.kt` ×19, `B2bMembersApi.kt` ×4, `OperatorApi.kt`, …). The TS generator consumes `funcName`
  and `tags`. — kept, verified still compiling 2026-07-29
- [x] `ApiFeature.codeGenName` (`funktor/rest/src/jvmMain/kotlin/ApiFeature.kt:11`) — client naming. — kept
- [x] `Deps.JavaLibs.diffutils` in `buildSrc/src/main/kotlin/Deps.kt:384` — still used by `ultra/meta`
  (`ultra/meta/build.gradle.kts:33`). — kept

**Carry over (rewrite, not move):**

- [ ] `shouldHaveNoDiffs` golden-file diff helper (`funktor/rest/src/jvmTest/kotlin/codegen/index_codegen.kt:7`) →
  `ultra/codegen` test source set. Its `normalizeForDiff` strips the Dart `splash` banner, so it needs adapting.
  *Alternative considered:* move it into `ultra/meta/testing` (already has diffutils and is the testing-helpers module).
  Rejected for now — `ultra/meta` drags in kapt + compile-testing, too heavy for a golden-file assert. Add
  `Deps.JavaLibs.diffutils` to `ultra/codegen` test deps instead.

**Verification gates for Phase 0:**

- [x] Nothing outside `codegen/dart/` referenced `Tags` / `tagged()` / `joinSimpleNames()` / `splash` /
  `CodeGenDsl` — confirmed by grep on 2026-07-29, only hit was the Dart test helper itself.
- [x] `funktor/rest/src/*/kotlin/docs/` has zero coupling to `codegen` — confirmed 2026-07-29.
- [x] Post-deletion grep for dangling refs (`funktor.rest.codegen`, `Dart*`, `dartProject`,
  `shouldHaveNoDiffs`) — zero hits, 2026-07-29.
- [x] Compile sweep — `BUILD SUCCESSFUL in 3m 58s`, 354 tasks, no `^e:`. Only pre-existing warnings
  (`ultra/kontainer` DSL-marker-on-local-variable, `karango/core` redundant casts). 2026-07-29.

**Roadmap updates required when this lands:**

- [x] `.claude/tasks/v1-roadmap.md:303` — Post-v1 item 4 "Dart codegen rewrite … move to
  `funktor:dart-codegen` package first" → marked superseded, 2026-07-29.
- [x] `.claude/tasks/v1-roadmap.md:125` — "Dart codegen test TODOs (6)" → removed (resolved by deletion),
  2026-07-29.

---

## Phase 1 — `ultra/codegen`

Plain `kotlin("jvm")` module (the walker needs `kotlin-reflect`; `ultra/reflection` has the same shape). Use
`ultra/reflection/build.gradle.kts` as the build-file template.

```
ultra/codegen/                       kotlin("jvm")
  src/main/kotlin/
    printer/        CodePrinter, indent handling
    ts/             TsFile, TsInterface, TsUnion, TsEnum, TsZodSchema, TsNameSanitizer
    model/          TypeId, TypeModel, TypeClaims, the closure walker
    sdk/            TsSdkContributor, TsSdkBuilder, TsSdkOutput, validation
    contributors/   MpDateTimeTsContributor
  src/main/resources/ts/runtime/     hand-written: http.ts, apiResponse.ts, datetime.ts, sse.ts
```

**Dependencies:** `api(project(":ultra:slumber"))` and `api(project(":ultra:common"))`.
`ultra:slumber`'s jvmMain already declares `api(project(":ultra:reflection"))` and
`api(project(":ultra:datetime"))` (`ultra/slumber/build.gradle.kts:71-73`), so reflection and datetime arrive
transitively — **no new dependency edges**.

**`ultra/codegen` must NOT depend on `ultra/kontainer`.** `TsSdkBuilder(contributors: List<TsSdkContributor>)`
is a plain constructor; kontainer injects the list but the class never imports it — same shape as
`ApiApp(val features: List<ApiFeature>)`. This keeps the core unit-testable with a hand-built contributor list, no
container required. All container wiring lives in `funktor/codegen`.

### 1.1 The contributor contract

- [ ] Phase the **contract**, not the contributors. Kontainer gives no ordering guarantee, and relying on registration
  order would reproduce the exact Dart fragility.

```kotlin
interface TsSdkContributor {
    val name: String                                   // for errors and --verbose

    /** Phase 1 — claim Kotlin types this contributor owns, and how they appear in TS. */
    fun claimTypes(claims: TsTypeClaims) {}

    /** Phase 2 — contribute roots: endpoints, extra types, anything that must be reachable. */
    fun contribute(model: TsSdkModel.Builder) {}

    /** Phase 4 — emit. The model is frozen and validated; nothing can be added. */
    fun emit(model: TsSdkModel, out: TsSdkOutput) {}
}
```

`TsSdkBuilder` runs **all `claimTypes` → all `contribute` → validate → all `emit`.** Within a phase every operation is a
keyed insert, so it is commutative and contributor order genuinely cannot matter. This is a strictly stronger property
than the Dart gen's "make the builders lazy".

### 1.2 The type model — symbolic references, not resolved nodes

- [ ] Every type reference in the model is a `TypeId` (value class over `KClass<*>`), **never** a resolved node. This is
  the fix for the Dart gen's ordering problem: a `TypeId` does not require its target to exist yet, so cycles cost
  nothing. A plain worklist with a `seen` set, ~150 LOC.
- [ ] `TypeId` replaces `Tags`/`tagged()`. Same "find it back by KClass" idea, but typed: O (1) map key, no
  `Any`, typos become compile errors. (Old: `Tags(Array<out Any>)` comparing `qualifiedName`,
  `funktor/rest/src/jvmMain/kotlin/codegen/tags.kt:17-26`.)
- [ ] TS makes this easier than Dart: type declarations hoist and `import type` is erased, so **circular type imports
  are legal**. No topological ordering needed at all.
- [ ] Walk transitively from the contributed roots. The Dart gen had **no closure** — the caller had to enumerate every
  type via `addType`
  (`funktor/rest/src/jvmMain/kotlin/codegen/dart/addons/serialization.kt:97-101`), and a forgotten type silently became
  `dynamic`.

Structural cases the walker must handle: data class, enum, `@JvmInline value class`, sealed/polymorphic parent,
collections (`List`/`Set`/`Map`), generics with reified arguments, nullability.

### 1.3 Claims registry

- [ ] `TsTypeClaims` is `Map<KClass<*>, TsTypeClaim>`, and each claim records the **claiming contributor's name**.
- [ ] **Double claim → hard error naming both contributors.** Never last-wins. With kontainer auto-discovery, adding a
  module could otherwise silently shadow a mapping.
- [ ] `claims.opaque<Foo>()` → emits `unknown`, and is listed in the run summary. A deliberate, loud escape hatch.
- [ ] Claims must carry both the TS **type** and (per the zod decision) a **schema expression**. See 1.6.

### 1.4 Validation — the phase that earns the whole design

For every `TypeId` in the transitive closure:

1. Claimed → fine.
2. Structural (data class / enum / value class / sealed / collection / primitive) → generate it.
3. Otherwise → **fail the build.**

Plus the check that makes custom Slumber codecs safe:

- [ ] Ask the live `SlumberConfig.getSlumberer(type)` what handles the type. If it resolves to something that is **not**
  one of the structural slumberers (`DataClassSlumberer`, `EnumCodec`,
  `ValueClassSlumberer`, collection/map slumberers) and nobody claimed the type → error.

This is the core lesson from the Dart gen. `MpInstant` does not slumber to a number or an ISO string — it slumbers to
`{ts, timezone, human}` (`ultra/slumber/src/jvmMain/kotlin/builtin/datetime/mp/MpInstantCodec.kt:31-40`). Nothing in
`KType` says so, and `SlumberConfig.getSlumberer` returns an opaque `Slumberer` with no introspectable schema
(`ultra/slumber/src/jvmMain/kotlin/SlumberConfig.kt:77`). The Dart gen handled this by pattern-matching classifiers and
attaching `@InstantConverter()` annotations (`codegen/dart/addons/serialization.kt:168-190`) — which means a **newly
added** custom codec silently produces a wrong type. The check above converts that into a build failure.

Target error format:

```
[ultra:codegen] 2 types have no TypeScript mapping:

  io.peekandpoke.ultra.datetime.MpInstant
      custom Slumber codec: MpInstantSlumberer  (JSON shape cannot be derived)
      reached via: FunktorConfApi.getTalks → ApiResponse<List<Talk>> → Talk.startsAt
      fix: claims.map<MpInstant>(tsName = …, from = …)   in a TsSdkContributor

  com.acme.Weird
      reached via: OperatorApi.stats → Stats.blob
      fix: claim it, or claims.opaque<Weird>()
```

- [ ] The `reached via` trail is required, not optional — it is what makes the error actionable.
  `ReflectivePathFinder` already produces exactly this shape (`FoundItem.path: List<String>`,
  `funktor/rest/src/jvmMain/kotlin/security/ReflectivePathFinder.kt:61`); reuse the idea.

### 1.5 Output collision rule

- [ ] `TsSdkOutput` tracks `path → owning contributor`. **Double file write → hard error naming both.**
  Same rationale as double claims: auto-injection makes silent collisions likely.

### 1.6 zod schemas (decided 2026-07-29)

Emit a zod schema per type and derive the TS type from it:

```ts
export const Talk = z.object({
    id: z.string(),
    title: z.string(),
    startsAt: MpInstant,
    speakers: z.array(Speaker),
})
export type Talk = z.infer<typeof Talk>
```

Consequences to design for:

- [ ] Every claim needs a **schema** as well as a type — a claimed type without a schema breaks
  `z.infer`. Bake this into `TsTypeClaim` from the start, do not bolt it on.
- [ ] Recursive types need `z.lazy(() => …)`. The walker knows which `TypeId`s participate in a cycle; emit `z.lazy`
  only for those, with an explicit `z.ZodType<T>` annotation (zod cannot infer through
  `z.lazy`).
- [ ] Discriminated unions → `z.discriminatedUnion('_type', [...])`, using the discriminator from
  `PolymorphicParentUtil.getDiscriminator()` (it is **not** always `_type` — a parent companion can override it,
  `ultra/slumber/src/commonMain/kotlin/Polymorphic.kt:48`).
- [ ] Value classes → the underlying scalar's schema (`ValueClassSlumberer` emits the bare value,
  `ultra/slumber/src/jvmMain/kotlin/builtin/objects/ValueClassSlumberer.kt:12-14`). Branded types optional; decide when
  writing the emitter.
- [ ] Enums → `z.enum([...])` from `Enum.name` (`EnumCodec`, `.../builtin/objects/EnumCodec.kt:8`).

**Before writing the emitter: look up the current zod major online.** Repo rule — never from memory. zod's
schema-composition API has changed shape across majors and the emitter is written directly against it.

### 1.7 DSL scoping

- [ ] Put `@TsDsl` (a `@DslMarker`) on the **receiver types** — `TsFile.Builder`, `TsInterface.Builder`, etc.
- [ ] The Dart gen got this wrong: `@CodeGenDsl` **is** a `@DslMarker`
  (`funktor/rest/src/jvmMain/kotlin/codegen/utils.kt:5`) but was applied to the extension *functions*, where it does
  nothing, and to only some receivers — `DartClass.Definition` had it (`codegen/dart/DartClass.kt:24`),
  `DartFile.Definition` (`DartFile.kt:18`) and
  `DartProject.Definition` (`DartProject.kt:11`) did not, leaving the outer receivers implicitly reachable from inner
  blocks.
- [ ] The identical trap was already diagnosed and fixed for `@RestDsl` — see the comment at
  `funktor/rest/src/jvmMain/kotlin/ApiRoutes.kt:29-33`: *"@DslMarker only has an effect when the RECEIVER TYPES are
  annotated — annotating functions does nothing."*

### 1.8 Datetime contributor

```kotlin
class MpDateTimeTsContributor : TsSdkContributor {
    override val name = "ultra:datetime"

    override fun claimTypes(claims: TsTypeClaims) {
        claims.map<MpInstant>(tsName = "MpInstant", from = "./runtime/datetime")
        // MpLocalDate, MpLocalDateTime, MpLocalTime, MpZonedDateTime
    }

    override fun emit(model: TsSdkModel, out: TsSdkOutput) {
        if (model.usesAny(MpInstant::class, /* … */)) {
            out.copyResource("ts/runtime/datetime.ts")   // only when reachable — no dead code
        }
    }
}
```

- [ ] The `.ts` file is a **checked-in resource, not generated output** — hand-written, versioned, living next to the
  codec it mirrors. This is the thing that was done ad hoc for Dart, now first-class.
- [ ] Cover every Mp type that has a codec under
  `ultra/slumber/src/jvmMain/kotlin/builtin/datetime/mp/`: `MpInstant`, `MpLocalDate`,
  `MpLocalDateTime`, `MpLocalTime`, `MpZonedDateTime`, `MpTimezone`.
- [ ] **Drift test (required):** slumber a known value of each Mp type in Kotlin and assert the resulting JSON keys
  match the fields declared in `runtime/datetime.ts`. The resource is on the classpath, so this is cheap. Without it the
  hand-written TS and the codec drift silently — the single highest-risk failure mode in this design.

---

## Phase 2 — `funktor/codegen`

Depends on `ultra:codegen` + `funktor:rest`. A **separate module, not part of `funktor/rest`** — codegen is a dev-time
concern and `funktor/rest` is a runtime dependency of every server. (This is the mistake Phase 0 is undoing.)

```
funktor/codegen/src/jvmMain/kotlin/
  RestApiTsContributor.kt
  cli/TsSdkGenerateCliCommand.kt
  index_jvm.kt                     funktorCodegen() kontainer module
```

### 2.1 `RestApiTsContributor`

```kotlin
class RestApiTsContributor(private val features: Lazy<List<ApiFeature>>) : TsSdkContributor {
    override val name = "funktor:rest"

    override fun contribute(model: TsSdkModel.Builder) {
        features.value.forEach { feature ->
            val client = model.addClient(feature.codeGenName)
            feature.getRouteGroups().forEach { group ->
                group.all.forEach { client.addEndpoint(group.name, it) }
            }
        }
    }

    override fun emit(model: TsSdkModel, out: TsSdkOutput) { /* clients + models */
    }
}
```

- [ ] `Lazy<List<ApiFeature>>` injection — the proven pattern, identical to
  `ValidateRoutesOnAppStarting` (`funktor/rest/src/jvmMain/kotlin/ValidateRoutesOnAppStarting.kt:18`).
- [ ] Handle all five `ApiRoute` variants: `Plain`, `WithParams`, `WithBody`, `WithBodyAndParams`, `Sse`.
- [ ] Method naming from `CodeGenHints.funcName` when present, else derive from the route.
- [ ] Path vs query params: `TypedRoute.parsedUriParams` gives the path params parsed from the pattern
  (`funktor/core/src/jvmMain/kotlin/broker/TypedRoute.kt:30`); the remaining PARAMS ctor params are query params.
- [ ] **Match `TypedRouteRenderer` exactly** (`funktor/core/src/jvmMain/kotlin/broker/TypedRouteRenderer.kt:28`)
  — null/empty omission, one value per key. Do not reinvent. (Earlier drafts of this plan named
  `UriParamBuilder`; **no such class exists**. `runtime/http.ts`'s `buildUrl` is already written against
  the renderer and checked in `ts-verify/verifyRuntime.ts`.)
- [ ] **Build it profile-shaped from day one**, even though profiles land later (see the incoming
  requirements below). `contribute` is exactly where a root predicate belongs, so take one as a
  constructor parameter defaulting to "all routes". The alternative is writing the loop and then
  rewriting it. Read `CodeGenHints.tags` here too — it is the natural carrier for the predicate, and
  reading it now stops it staying vestigial.

### 2.2 CLI command

- [ ] `TsSdkGenerateCliCommand : CliktCommand(name = "sdk:ts:generate")`, registered as
  `singleton(...)`. `CliRunner` auto-collects every `CliktCommand` in the kontainer
  (`funktor/core/src/jvmMain/kotlin/cli/CliRunner.kt:14`) and `--cli` is already the launch discriminator
  (`funktor/core/src/jvmMain/kotlin/app.kt:160`). Template:
  `funktor/auth/src/jvmMain/kotlin/cli/AuthGenerateJwtSigningSecretCliCommand.kt`. ~60 LOC.
- [ ] Options: `--out <dir>`, `--dry-run` (print planned files + diff, write nothing),
  `--check` (exit non-zero when output differs from disk), `--verbose` (per-contributor summary).
- [ ] **`--check` is not optional.** Without it, a stale checked-in SDK is invisible until a frontend dev hits a runtime
  shape mismatch. With it, CI catches an API change nobody regenerated for.

```
./gradlew :funktor-demo:server:run --args="--cli sdk:ts:generate --out ../frontend/src/api"
```

- [ ] The CLI must stay a thin wrapper. `ultra/codegen` exposes
  `TsSdkBuilder.generate(target, mode): SdkGenResult` and knows nothing about clikt — so a Gradle task or a test can
  call the same entry point.

### 2.3 Kontainer module

```kotlin
val Funktor_Codegen = module { builder: FunktorCodegenBuilder.() -> Unit ->
    dynamic(TsSdkBuilder::class)
    singleton(RestApiTsContributor::class)
    singleton(MpDateTimeTsContributor::class)
    singleton(TsSdkGenerateCliCommand::class)
    FunktorCodegenBuilder(this).apply(builder)
}
```

- [ ] `dynamic(TsSdkBuilder::class)` **deliberately**: it reaches `RestCodec`, which is registered
  `dynamic` (`funktor/rest/src/jvmMain/kotlin/index_jvm.kt:61`). Per the repo's scoping rule a singleton would silently
  become `SemiDynamic` anyway; declaring it dynamic makes that explicit. Costs nothing for a one-shot CLI.
- [ ] Follow the `funktorAuth` registration shape (`funktor/auth/src/jvmMain/kotlin/index_jvm.kt:21-49`).
- [ ] Do **not** add to the all-in-one `Funktor` module (`funktor/all/src/jvmMain/kotlin/funktor.kt`) — opt-in via
  `funktorCodegen()`, so production servers don't carry the generator.
- [ ] User-defined contributors: `singleton(MyTypesTsContributor::class)` next to `funktorCodegen()`. Same extension
  story as `ApiFeature` and `CliktCommand`.

### 2.4 `SlumberConfig` access

The validation in 1.4 needs the live `SlumberConfig`. `Codec.config` is public
(`ultra/slumber/src/jvmMain/kotlin/Codec.kt:23`) and `SlumberRestCodec : RestCodec, Codec(config)`, so it is reachable
by downcasting the injected `RestCodec`.

- [ ] **Preferred:** add `instance(codecConfig)` to `Funktor_Rest`
  (`funktor/rest/src/jvmMain/kotlin/index_jvm.kt:48` builds it as a local) so `SlumberConfig` is injectable directly.
  One line, avoids the downcast.

---

## Phase 3 — TS runtime resources — DONE 2026-07-29

Hand-written, checked in under `ultra/codegen/src/main/resources/ts/runtime/`. All four modules are
inventoried by `TsRuntime.Module` (`ultra/codegen/src/main/kotlin/ts/TsRuntime.kt`), so a contributor
names a module rather than a path, and `datetime.ts` stopped being addressed by string literal.

**Two constraints discovered while building it, both now enforced by the toolchain:**

- **No non-erasable TypeScript, ever.** `SseError` was first written with constructor parameter
  properties; Node's type stripping rejects them outright, and so does esbuild. A generated SDK is
  consumed by exactly those toolchains. `ts-verify/tsconfig.json` now sets `erasableSyntaxOnly: true`,
  which moves the failure from run time to `tsc`. Also rules out `enum` and `namespace`.
- **A runtime check that throws must not abort the run.** Found by mutation-testing: sabotaging
  `fetchTransport` to throw on non-2xx took every later check down with it, so the SSE checks silently
  did not run. `verifyRuntime` now isolates each group; `verify.ts` isolates each fixture import.

### 3.1 Transport — `runtime/http.ts`

- [x] Emit against a minimal transport interface with a **fetch-based default**. A generated SDK is a library — baking
  in axios forces the dep on every consumer and collides with their own interceptors / auth-refresh / tracing.

```ts
export interface HttpTransport {
    send(req: HttpRequest): Promise<HttpResponse>
}

export interface HttpRequest {
    method: string;
    url: string;
    headers: Record<string, string>;
    body?: string;
    signal?: AbortSignal
}

export interface HttpResponse {
    status: number;
    statusText: string;
    body: string
}
```

Mirrors `RemoteResponse` (`ultra/remote/src/commonMain/kotlin/RemoteResponse.kt:8-27`) so the Kotlin and TS SDKs stay
conceptually aligned. Auth becomes a one-line transport wrapper, not a generated concern.

- [x] **Non-2xx must NOT throw.** This is a deliberate, documented and defended semantic on the Kotlin side —
  `ApiClient.Config` KDoc (`ultra/remote/src/commonMain/kotlin/ApiClient.kt:24-29`): *"Non-2xx responses are surfaced as
  a decoded `ApiResponse` envelope, not thrown — the transport forces
  `expectSuccess = false` per request, so a client-level `expectSuccess = true` /
  `HttpResponseValidator` cannot silently reintroduce throw-on-error."* Verified at
  `ultra/remote/src/commonMain/kotlin/RemoteRequestImpl.kt:54`. The TS SDK must match or the two clients diverge.
- [x] This is *why* `fetch` is the right default: not rejecting on 4xx/5xx — the thing everyone complains about — is
  exactly the behaviour needed here. axios would mean fighting `validateStatus` on every call.
- [x] `apiRespond` derives the HTTP status **from** the envelope (`funktor/rest/src/jvmMain/kotlin/respond.kt:35`), so
  the two always agree — but parse the envelope, since it also carries `messages` and `insights`.
- [x] `buildUrl` mirrors `TypedRouteRenderer` (`funktor/core/src/jvmMain/kotlin/broker/TypedRouteRenderer.kt:28`),
  which turned out to matter more than expected: it **drops query params that are null or the empty
  string** rather than sending them empty (`:52-55`). Sending `page=` where Kotlin sends nothing would
  hand the server `""` instead of the parameter's default. Query keys also appear at most once —
  `OutgoingConverter.convert` produces a single string per param, so repeated keys are not part of the
  protocol and `buildUrl` must not invent them. Encoding uses `encodeURIComponent`; it leaves a few
  sub-delimiters unescaped that ktor's `encodeURLQueryComponent(encodeFull = true)` escapes, but both
  decode identically server-side.

### 3.2 `ApiResponse` envelope — `runtime/apiResponse.ts`

- [x] Hand-written TS mirror of `ApiResponse<T>` (`ultra/remote/src/commonMain/kotlin/ApiResponse.kt:10-19`):
  `status`, `data`, `messages`, `insights`. Small and stable — do not generate it.
- [x] **Stays generic rather than monomorphized.** TypeScript has real generics, so `apiResponse(Talk)`
  is a schema factory returning `z.ZodType<ApiResponse<Talk>>`. Monomorphizing the envelope the way
  the model emitter monomorphizes `PageOf<Talk>` would mean one `ApiResponseTalk` per payload for no
  gain. The walker therefore never sees `ApiResponse` — the REST contributor roots the PAYLOAD type.
- [x] `HttpStatusCode`, `Message` and `Insights` ship with it. Verified by slumbering real values:
  `HttpStatusCode` is `{value, description}`, **not** a bare number, and `Message.ts` is an `MpInstant`.
- [x] Optionality rule, stated in the file: **optional in TS iff the Kotlin ctor param has a default** —
  the same rule the generator applies to generated types. So `messages`/`insights` are `.nullish()`
  and `data` is required-but-nullable. Slumber writes every key including nulls (probed, not assumed),
  but the Kotlin awaker accepts them missing and the client should too.
- [x] `apiResponse.ts` re-declares the `{ts, timezone, human}` instant shape locally instead of importing
  `./datetime.ts`, so it stands alone — `datetime.ts` ships only when a datetime type is reachable,
  and the envelope is needed regardless. `ApiResponseParitySpec` pins the copy against the codec AND
  against `datetime.ts`'s own `timestamped`, so the duplication cannot drift.

### 3.3 SSE — `runtime/sse.ts`

- [x] **`EventSource` cannot send an `Authorization` header** — not in the spec. `ApiRoute.Sse` routes go through the
  same auth floor as everything else, so an authenticated SSE endpoint is unreachable via
  `EventSource` unless the token goes in the query string, where it lands in access logs.
- [x] Implement SSE over `fetch` + `ReadableStream` with a small SSE frame parser (the approach
  `@microsoft/fetch-event-source` takes).
- [x] **SSE routes carry no typed payload**: `ApiRoute.Sse.responseType` is `TypeRef<Unit>`
  (`funktor/rest/src/jvmMain/kotlin/ApiRoute.kt:213`) and the handler returns `Any`. So events are
  delivered as raw `data` strings and the caller applies whatever schema fits — a generated per-route
  event type is not derivable today. Revisit if `Sse` ever gains an event type parameter.
- [x] Two deliberate divergences from `EventSource`, both documented in the file: no automatic
  reconnect (the server's `retry` is surfaced instead), and a non-2xx **does** throw `SseError` — there
  is no stream to return and an async generator has nowhere to put an envelope.
- Note: `routing.kt:225` records that the ktor SSE plugin is not installed in any funktor app, so SSE
  routes are unmountable today. The runtime is ready ahead of that.
- [ ] This has not bitten yet only because the demo's SSE routes are `public()`
  (`funktor-demo/server/src/main/kotlin/api/showcase/SseShowcaseApi.kt:16`).

---

## Phase 4 — Wiring and demo

- [ ] Add `:ultra:codegen` and `:funktor:codegen` to `settings.gradle`.
- [ ] Register `funktorCodegen()` in the demo server's kontainer.
- [ ] Generate an SDK for at least `FunktorConfApiFeature` end to end, into a scratch dir.
- [ ] Type-check the generated output with `tsc --noEmit` as part of the test evidence — generated TS that does not
  compile is the failure mode a Kotlin-side test cannot catch.

**Output shape (decided 2026-07-29):** bare `.ts` sources into an existing frontend folder. Frontend owns tsconfig,
build and deps; Vite HMR picks up changes with no extra wiring.

```
src/api/
  models/    funktorconf.ts, auth.ts
  clients/   FunktorConfApi.ts, AuthApi.ts
  runtime/   http.ts, apiResponse.ts, datetime.ts, sse.ts
  index.ts
```

---

## Progress log

**2026-07-29 — Phase 0 DONE. Phase 1 model layer DONE.**

Phase 0: Dart codegen deleted (3103 LOC), `diffutils` removed from `funktor/rest`, compile sweep green
(`BUILD SUCCESSFUL in 3m 58s`, 354 tasks, no `^e:`).

Phase 1 so far — `ultra/codegen` created and wired into `settings.gradle`:

| File | What |
|---|---|
| `printer/CodePrinter.kt` | Indent-aware printer. Always emits `\n`, never `System.lineSeparator()` — the Dart printer used the platform separator and then had to normalize it away in its tests |
| `model/TypeId.kt` | Canonical identity, keyed by a qualified-name string |
| `model/TsTypeRef.kt` | Symbolic refs (`Named`/`ArrayOf`/`RecordOf`/`Nullable` + primitives) |
| `model/TsTypeDecl.kt` | `Obj` / `Union` / `EnumDecl` / `Alias` |
| `model/TsTypeClaims.kt` | Claims registry, per-contributor scope, double-claim hard error |
| `model/TypeWalker.kt` | The closure walk |
| `model/TsNames.kt` | TS naming incl. nested-class prefixing |
| `model/TypeModel.kt` | Frozen result |

Tests: `CodePrinterSpec` 11, `TypeWalkerSpec` 24 — all green, counts confirmed from
`build/test-results/**/TEST-*.xml` rather than from the gradle task result.

### Decisions taken while implementing

- **`TypeId` equality is by canonical STRING, not `KType` equality.** The same logical type reaches the
  walker as different `KType` instances depending on whether it came from `typeOf<T>()` (via `TypeRef`) or
  `KClass.createType()` (via `ReifiedKType` reification), and those do not reliably compare equal. This is
  the old `Tags.contains` qualified-name comparison (deleted `codegen/tags.kt:17-26`) made total. Pinned by
  a test.
- **Generics are monomorphized** — `PageOf<Talk>` → `PageOfTalk`, not generic `PageOf<T>`. The walker
  reifies type arguments anyway (staying generic would mean *un*-reifying), and generic zod schemas need
  function-valued schemas that `z.infer` cannot see through. Cost: one declaration per instantiation.
  Reversible if the instantiation count gets unpleasant.
  **⚠ REVERSED 2026-07-30 (`63f9f186`).** The second reason was measured and found false. Generics are
  now emitted GENERICALLY: one declaration per class, arguments on the reference — `PageOf<Talk>` in
  type position, `PageOf(Talk)` in schema position. See `20260730-codegen-generic-emission.md`.
- **`Set` and `List` share one reference shape** (`ArrayOf`) — both slumber to a JSON array. `Map` becomes
  `RecordOf` with a `string` key, since JSON object keys are always strings regardless of the Kotlin key type.
- **Optional = constructor parameter has a default.** Deliberately loose and direction-dependent: responses
  always carry every key (`DataClassSlumberer` writes nulls explicitly), but a client sending this type as a
  request body may omit a defaulted field. Loose-on-parse never spuriously rejects valid server data.

### Findings

- **`Long` silently loses precision, and the generator cannot fix it.** Slumber writes a `Long` as a JSON
  number, so `JSON.parse` truncates above 2^53 before the generated types are involved at all. The walker
  emits `number` (accurate about what you actually receive) and records every reachable `Long` in
  `TypeModel.longValued`. **Open question for the maintainer:** should a reachable `Long` be a warning, a
  hard error, or opt-in mapped to `string`? It is a wire-format property, so it is not the generator's call.
- **Mutation testing caught a vacuous test.** The claim guard in `TypeWalker.declare` is unreachable via
  property references (`resolveNonNullRef` short-circuits on a claim first), so mutating it away changed
  nothing. It *is* load-bearing when a claimed type is a polymorphic child, since `declareUnion` enqueues
  variants directly. Added the `FxPartlyClaimed` fixture and a test for that path. Now 4/4 mutations killed
  (claim guard, custom discriminator, ctor-default optionality, enum constant names).

### Slumber dispatch parity — 5 divergences found and fixed (2026-07-29)

Read `BuiltInModule.getSlumberer` (`ultra/slumber/src/jvmMain/kotlin/builtin/BuiltInModule.kt:137-206`)
line by line against the walker's classification. Five real divergences, all of which would have
produced wrong or missing types. All fixed, each pinned by a test.

| # | Divergence | Consequence had it shipped |
|---|---|---|
| 1 | Walker used `isValue`; Slumber uses `isUserValueClass()` = `isValue && !qualifiedName.startsWith("kotlin.")` (`BuiltInModule.kt:63`) | `Duration`, `UInt`, `ULong`, `Result` aliased to a scalar — but Slumber *refuses* them, so the type described output the server cannot produce |
| 2 | No `objectInstance` branch | **`sealed class X { object A : X() }` — the most common Kotlin sealed shape — reported unresolved and failed the build.** `ObjectInstanceCodec` writes `{}` (`ObjectInstanceCodec.kt:22`), so it is an empty object type |
| 3 | No no-arg-constructor branch | A non-data class with a no-arg ctor reported unresolved, though Slumber routes it to `DataClassSlumberer` (`BuiltInModule.kt:195`) |
| 4 | Collections keyed on `Collection`; Slumber keys on `Iterable` (`BuiltInModule.kt:179`) | A custom `Iterable` that is not a `Collection` typed as an object instead of an array |
| 5 | Walker treated `Array` as a collection | `Array` is not `Iterable`, so Slumber had **no slumberer at all** for a declared array type. RESOLVED differently: rather than keep refusing arrays, Slumber gained array support — see `20260729-slumber-array-support.md` (landed 2026-07-29). Arrays now map to `ArrayOf`, with primitive arrays resolving their element type from a lookup table since `IntArray` carries no type argument |

**Classification order is also load-bearing and now mirrors Slumber's**: a user value class is resolved
*before* primitives and collections, so `value class Ids(val v: List<String>)` aliases to `string[]`
rather than being flattened into an array reference.

This is the concrete argument for the design rule "defer to the slumber-side utility, never re-derive
the rule". Four of the five came from re-deriving.

### Mutation testing — 2 vacuous tests caught

Both times the suite was green on the first run, which is exactly when it is least trustworthy.

1. **Claim guard in `TypeWalker.declare`** — unreachable via property references, because
   `resolveNonNullRef` short-circuits on a claim first. It *is* load-bearing when a claimed type is a
   polymorphic child, since `declareUnion` enqueues variants directly. Fixture `FxPartlyClaimed` added.
2. **Sealed `object` variant** — the fixture used `data object`, which also satisfies `isData`, so it
   fell through to the data-class branch and produced an identical empty result. The test could not
   distinguish the branches. Changed to a **plain** `object` (plus a separate `data object` case).

Final: 8/8 mutations killed across both rounds. 41 tests (`TypeWalkerSpec` 30, `CodePrinterSpec` 11).

### zod emitter (2026-07-29)

`ts/TsDeclOrder.kt`, `ts/TsRenderer.kt`, `ts/TsModelEmitter.kt` — models emit to a single `models.ts`.

**One file, not one per package.** A zod schema is a `const`, i.e. a VALUE, so a cross-file cycle would
be a circular value import and crash at module-evaluation time. Keeping models together reduces the
ordering problem to one file, where `TsDeclOrder` solves it outright. Splitting later is possible but
needs `z.lazy` on every cross-file edge.

**Ordering rule (got this wrong first).** A DFS post-order alone is not enough. My first attempt marked
the node a back-edge LANDS ON as recursive, but the declaration that actually needs deferring is the
one emitted FIRST while referencing something later. Laziness is now decided by position after
ordering: a decl is lazy iff it references itself or anything at/after its own index. For a mutual
cycle that correctly makes exactly ONE of the pair lazy, not both.

**Recursive decls need an explicit type**, because `z.infer` cannot see through `z.lazy`:
`export type X = {...}` plus `export const X: z.ZodType<X> = z.lazy(() => ...)`.

**`z.discriminatedUnion` needs concrete object options**, so a lazily-emitted variant forces a fallback
to `z.union` — still correct, just worse error messages. Detected and handled.

`TsVoid` renamed to `TsNull`: `Unit` slumbers via `NullCodec` to `null`, so the TypeScript type is
`null` and the schema `z.null()`. The old name described the Kotlin side and would have misled.

### End-to-end verification with the real toolchain (2026-07-29)

Not just eyeballed — the generated output was run against the actual TypeScript compiler and zod
(zod 4.4.3, typescript 7.0.2, both looked up rather than assumed):

1. **`tsc --noEmit` under `strict: true`** passes on all six generated fixtures, recursive ones included.
2. **The schemas parse what Slumber actually writes.** Kotlin slumbered real instances to JSON, node
   parsed that JSON with the generated schemas: talk, node (recursive), event (custom discriminator)
   and shape (polymorphic) all PASS.
3. **The schemas genuinely discriminate** — a passing `parse()` proves nothing if the schema accepts
   anything. 7 malformed inputs all rejected (wrong field type, missing required field, invalid enum
   member, bad nested object, null for a non-nullable, unknown discriminator, bad recursive child) and
   2 valid edge cases correctly accepted (omitted optional, null for nullable).

**Now automated as a Gradle gate** (`:ultra:codegen:tsVerify`, wired into `check`). Originally a manual
sandbox run; made reproducible on 2026-07-29.

`ultra/codegen/ts-verify/` is **self-contained by design** — its own `package.json`, `pnpm-lock.yaml`,
`.npmrc` and `tsconfig.json`, sharing nothing with docs-site or the Kotlin/JS build. Those churn for
unrelated reasons (site deps, Kotlin upgrades); this gate should only ever break when the generator
breaks.

Reproducibility levers, all pinned:

| Lever | Mechanism |
|---|---|
| Dependency versions | `pnpm-lock.yaml` checked in, installed `--frozen-lockfile` (fails on drift, never silently re-resolves) |
| pnpm version | `packageManager: pnpm@10.26.2` + `manage-package-manager-versions=true` in `.npmrc` |
| Node version | `use-node-version=22.12.0` in `.npmrc` — pnpm downloads and manages it, so PATH does not matter |
| Fixture freshness | Gradle regenerates from the current emitter every run; nothing generated is checked in |

**Deliberately NOT corepack.** Corepack 0.29.x (bundled with Node 22) ships a rotated npm registry
signing key and dies with `Cannot find matching keyid`. The common workaround is
`COREPACK_INTEGRITY_KEYS=0`, i.e. disabling supply-chain signature verification — the opposite of why
this repo standardised on pnpm. pnpm enforces `packageManager` itself, so corepack buys nothing here.

**The harness is manifest-driven, not hand-maintained.** `TsFixtureGenerator` emits, per fixture, the
`.ts`, the JSON Slumber really produced, and a manifest naming the schema export plus its REQUIRED
field names (optional-by-default props excluded). `verify.ts` then generates the negative cases from
that manifest — parse the real sample, reject a non-object, and reject each required key dropped in
turn. So the negative cases cannot drift from the model, and adding a fixture needs no TypeScript
edits. Currently 28 assertions over 5 fixtures.

**The gate was itself mutation-tested** — an always-green gate is worthless. Breaking the emitter three
ways (all props emitted `.optional()`, wrong discriminator literal, every prop degenerating to
`z.unknown()`) is caught every time.

Opt out with `-PskipTsVerify=true`. It fails loudly rather than skipping silently when the toolchain is
absent — a verification that quietly does nothing is worse than none.

The Kotlin half is permanent too: `SlumberFieldParitySpec` asserts the emitted field set is exactly the
key set Slumber writes, across containers, `@Slumber.Field`, both discriminator styles, plain sealed
objects and recursion.

Mutation-tested 4/4 killed on the emitter (self-reference laziness, `.optional()`, discriminator
literal, `discriminatedUnion` preference).

### Contributor orchestration + first two contributors (2026-07-29)

`sdk/TsSdkContributor.kt`, `sdk/TsSdkBuilder.kt`, `sdk/TsSdkOutput.kt`, plus
`contributors/KotlinxJsonTsContributor.kt` and `contributors/MpDateTimeTsContributor.kt`.

Phase order is enforced globally by the builder (all `claimTypes` -> all `contribute` -> validate ->
all `emit`), so contributor order is structurally irrelevant. Pinned by a test that runs the same two
contributors in both orders and asserts byte-identical output.

`TsSdkOutput` PLANS files rather than writing them, so a validation failure leaves nothing
half-generated and `--check` / `--dry-run` can inspect the result without touching the target. It
already carries `diffAgainst()` for `--check`.

**Decisions confirmed by the maintainer (2026-07-29):**

- **`Long` stays `number`.** Worth recording that the risk model is the opposite of the intuitive one:
  money in minor units is SAFE (2^53 cents is about $90 trillion). The live hazard is nanosecond
  timestamps -- 55 `Long` fields exist today with `startedNs`/`endedNs`/`timeNs` prominent in insights
  (`funktor/insights/.../InsightsGuiData.kt:16-17`), and `System.nanoTime()` on a box up ~200 days
  exceeds 2^53. It does not bite there because those are consumed as differences for profiling display.
  What WOULD corrupt silently is an opaque 64-bit id / snowflake.
- **kotlinx JSON types get permissive claims** -- all FIVE that Slumber has codecs for, not just the
  two obvious ones; claiming a subset would leave the rest failing validation the first time anyone
  used one. `JsonElement` -> `unknown`, `JsonObject` -> `Record<string, unknown>`, `JsonArray` ->
  `unknown[]`, `JsonPrimitive` -> `string | number | boolean | null` (verified:
  `KotlinXJsonPrimitiveCodec.kt:31` unwraps to a bare scalar), `JsonNull` -> `null`.

**Bug found while claiming those:** a CLAIMED type can render as a union, which the array
parenthesisation did not account for. `List<JsonPrimitive>` would have emitted
`string | number | boolean | null[]`, which parses as `string | number | boolean | (null[])`. The check
now inspects the rendered TEXT rather than the ref shape, because no inspection of the ref can reveal
a claim's shape.

**Datetime claims: 2 of 6 shapes were wrong in the first draft**, written from assumption.
`MpLocalTime` slumbers to a **bare number**, not `{milliSeconds}`, and `MpTimezone` (a **bare string**)
was omitted entirely. Caught only by reading the codecs -- see the standing rule above. The drift guard
`MpDateTimeFieldParitySpec` now kills all four sabotage variants, including that exact mistake.

Tests: 89 green, plus the TypeScript gate. Mutation-tested 4/4 on the drift guard.

### Order-independence had a hole (2026-07-29)

Raised by the maintainer asking whether contributor order matters. It does not for CLAIMS — all
`claimTypes` run before any walking, so the registry is always complete — but it DID for ROOTS.

The set of declarations never depends on contributor order, yet the discovery ORDER did, and
declarations were emitted in `model.decls` insertion order. Two contributors both supplying roots
therefore produced different `models.ts` text depending on registration order. Semantically identical,
byte-wise different — invisible to `tsc`, fatal to `--check`, which compares generated output against
disk.

The existing "contributor order does not matter" test could not catch it: only one of its two
contributors supplied roots, so the root list was identical in both orders.

Fixed by seeding the emission walk from `decls.keys.sortedBy { it.key }` rather than insertion order.
Pinned by a test with TWO root-supplying contributors, and mutation-tested.

Worth remembering as a class of bug: "output is deterministic" needs a test with at least two
independent contributors, not one.

### Phase 2 prerequisite: apps cannot register their own SlumberModule (2026-07-29)

Raised by the maintainer asking how a funktor app would pass its own `SlumberConfig`.

**Reading the live config already works.** `RestCodec` is the service that serializes API models
(`funktor/rest/src/jvmMain/kotlin/codec/RestCodec.kt`), and `apiRespond` goes through it
(`respond.kt:32`) — so it is exactly what the generator should consult, being what actually writes the
bytes the SDK parses. `SlumberRestCodec : RestCodec, Codec(config)` and `Codec.config` is public
(`ultra/slumber/src/jvmMain/kotlin/Codec.kt:23`), so the generator can inject `RestCodec`, downcast to
`Codec` and see every registered codec. `TsSdkBuilder` already takes `SlumberConfig` as a constructor
parameter, so nothing on the codegen side needs changing.

**Registering an app's own module does NOT work.** `index_jvm.kt:48` builds
`codecConfig = SlumberConfig.default.prependModules(VaultSlumberModule)` as a HARDCODED LOCAL inside
the module lambda, and `FunktorRestBuilder` exposes only `jwt()`. An app cannot contribute a
`SlumberModule` at all; it would have to override the entire `RestCodec` registration.

That is a RUNTIME limitation, not a codegen one — an app type needing a custom codec cannot be wired in
today either. The SDK generator merely surfaces it, being the first thing that asks the config what it
contains. Latent rather than painful: zero `SlumberModule` registrations exist in `funktor-demo`.

- [ ] **The actual gap:** `FunktorRestBuilder.slumberModules(vararg SlumberModule)` so apps can
      contribute codecs. Benefits runtime serialization first, codegen second.
- [ ] **Cosmetic only:** `instance(codecConfig)` in `Funktor_Rest`, to avoid downcasting `RestCodec`
      to `Codec`. Not required — the downcast works.

### On hardcoding against the built-in codecs

`TsModelValidator.matches` names the six structural slumberers (`DataClassSlumberer`,
`PolymorphicChildSlumberer`, `ObjectInstanceCodec`, `PolymorphicParentSlumberer`, `EnumCodec`,
`ValueClassSlumberer`) explicitly. Deliberate, and the same coupling the walker has — both mirror
`BuiltInModule`'s dispatch rather than re-deriving it, so both move together.

Confirmed sound by the maintainer (2026-07-29): the built-in codecs are the machinery modelling the
"normal" stuff that is always present, so coding hard against them is the right move — and they are on
par with kotlinx-serialization, which anchors the set to an external spec rather than to Slumber's own
choices. The code states that intent directly: `ValueClassSlumberer`'s KDoc says it "matches
kotlinx.serialization exactly", and `isUserValueClass` (`BuiltInModule.kt:63`) excludes stdlib value
classes precisely because their generic form "would diverge from kotlinx". So the hardcoded set is
stable by construction, not by luck.

Acceptable because the failure direction is safe: an UNRECOGNISED slumberer is reported as "custom
codec, claim it". A new structural codec in Slumber would therefore cause a spurious error demanding a
claim, never silently wrong TypeScript. Wrong-and-loud, never wrong-and-quiet.

### Generics: verified across all positions, one naming bug fixed (2026-07-29)

Prompted by the maintainer asking whether generics work at top level, as properties, and nested like
`List<MyType<Something>>`. They do — all positions resolve with nothing left unclassified:

| Kotlin | TypeScript |
|---|---|
| `FxPageOf<FxSpeaker>` (property) | `FxPageOfFxSpeaker` |
| `List<FxBox<FxSpeaker>>` | `FxBoxFxSpeaker[]` |
| `FxBox<List<FxSpeaker>>` | `FxBoxListFxSpeaker` |
| `FxPageOf<FxBox<FxSpeaker>>` (nested generic) | `FxPageOfFxBoxFxSpeaker`, substituted at BOTH levels |
| `FxPair<FxSpeaker, FxStatus>` (two params) | `FxPairFxSpeakerFxStatus` |
| `Map<String, FxBox<FxTalkId>>` | `Record<string, FxBoxFxTalkId>` |
| `FxBox<A>` and `FxBox<B>` in one model | two separate declarations |

**Bug found and fixed:** `TsNames` stripped the package using `cls.java.package`, but for Kotlin's
mapped types that disagrees with `qualifiedName` — `List::class.java` is `java.util.List` while
`qualifiedName` is `kotlin.collections.List`, so nothing was stripped and `FxBox<List<FxSpeaker>>`
emitted as `FxBoxkotlincollectionsListFxSpeaker`. Now built by walking the enclosing-class chain,
which sidesteps the mismatch. Nested classes keep their outer prefix as before; two same-named classes
in different packages still collapse to one name, which the validator's collision check catches.

Only found because the probe covered a collection AS a type argument. The pre-existing generics test
used a plain type argument and would never have shown it.

Mutation-tested 2/2 (type args included in the name; outer-class prefix retained).

**2026-07-29 — Phase 3 DONE (the hand-written TS runtime). Phase 1 is complete.**

`runtime/http.ts`, `runtime/apiResponse.ts` and `runtime/sse.ts` added alongside `datetime.ts`, all
four now inventoried by `TsRuntime.Module` and copied into the verified directory by the fixture
generator — so `tsc` type-checks the runtime, which nothing did before.

`ultra/codegen` gained `testImplementation(project(":ultra:remote"))`, deliberately test-only: main
code just ships the `.ts` as a resource, so the generator stays independent of `ultra/remote`.

**Envelope shape was probed, not recalled** — the lesson from `datetime.ts` applied up front. Slumber
writes every key including nulls; `HttpStatusCode` is `{value, description}` rather than a bare number.

**Verification, 51 assertions in `ts-verify` + 16 Kotlin (`ApiResponseParitySpec`, `TsRuntimeSpec`):**

- `ApiResponseParitySpec` reads the field sets back out of the `.ts` source and compares them against
  what Slumber really produces. Comparing against a hand-kept Kotlin list would only ever have proven
  the list was copied correctly.
- `verifyRuntime.ts` runs the runtime for real: envelope parsing against a slumbered
  `ApiResponse<FxSpeaker>`, `buildUrl` against `TypedRouteRenderer`'s rules, `fetchTransport` against a
  fake `fetch`, and seven SSE frame-parser cases.

**Mutation-tested 14/14** — 6 against `ApiResponseParitySpec` (drop a schema field, drop an enum entry,
rename a field in the interface only, degenerate `HttpStatusCode` to a number, …) and 8 against
`ts-verify` (empty query params, unfilled placeholder, throw-on-non-2xx, CR hold-back, empty-frame
dispatch, id persistence, ignore the payload schema, space stripping).

**One test was vacuous and is now sharp.** The CRLF-split-across-chunks case originally ended the frame
at the split, so removing the hold-back produced an identical result — the spurious blank line
dispatches nothing, because a frame with no data is dropped. It only discriminates when the frame
*continues* past the split: `push('data: a\r')` then `push('\ndata: b\n\n')` must yield one event with
`data: 'a\nb'`, not two events.

**Caught by `tsc`, not by an assertion:** making the envelope ignore its payload schema
(`data: z.unknown()`) fails to type-check against `z.ZodType<ApiResponse<T>>`. Worth noting that the
type-check layer catches a class of sabotage the runtime checks would have to be written for.

**2026-07-30 — frontend-SDK requirements folded in; two design questions settled.**

The maintainer settled the frontend direction (Vue, generated on the fly, nothing published) in
`.claude/tasks/20260730-frontend-sdk-vue-contributors.md`, then raised two doubts about it. Both are
now decided and recorded above and in the design doc:

1. **Config emission → an ownership boundary, not a merge mechanism.** The worry was that regenerating
   would destroy hand-made changes in the target project. Merging was rejected as the answer: it can
   only be wrong-and-quiet on JSONC and on TypeScript config. `<out>/` is owned outright; nothing
   outside it is ever written; `scaffold` seeds app-owned files without overwriting. The concern
   correctly exposed a real gap that merging would NOT have fixed — stale output — which needs an
   output manifest and a `diffAgainst` that can see ghosts.
2. **Profiles → select roots, no tree-shaking, no contributor filtering.** Tree-shaking turned out to
   be `TypeWalker` run twice. Contributor filtering turned out to be actively unsafe. Narrowing the
   root set gives every wanted consequence for free.

Net effect on this plan: **nothing already built needs redesigning.** All four additions remain keyed
inserts into the existing phases, and `TsSdkEmitContext` is a class, so gaining a `registry` is
additive. Three concrete items landed in the current scope: fix the two `resource()` bugs, and add the
claim-`importFrom`-is-emitted validation.

Worth recording as a design observation: **`models.ts` is the first instance of the aggregation
registry, not a special case.** The builder owns it because every contributor feeds it
(`sdk/TsSdkBuilder.kt:101-103`), which is exactly what the registry generalises. Build the registry so
`models.ts` becomes its first target, or the codebase ends up with two mechanisms doing one job.

### Next

**Review round first** (maintainer, 2026-07-30: fold the above into the plan, review, then start new
features). Then Phase 2 (`funktor/codegen`: `RestApiTsContributor` + `sdk:ts:generate` CLI) — built
profile-shaped per 2.1 — then the codegen additions (profiles, shared emission, registries, `out.vue`,
config emission), then Phase 4. Phase 2's prerequisite is
`.claude/tasks/20260730-funktor-rest-codec-config.md`.

Independent of all of it, and the only item with a deadline shape: **gate the existing insights GUI**
(step 1 of the design doc's ordering). No codegen dependency.

---

## Design decisions (locked — do not re-litigate without a reason)

| Decision                                        | Rationale                                                                                                                 |
|-------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------|
| Delete Dart, don't port                         | No downstream consumer; rebuildable anytime; 3 kLOC out of a runtime module                                               |
| Core in `ultra/codegen`, not `ultra/sdkbuilder` | Printer + AST *are* codegen; SDK-building is one consumer. Correct home if a Dart emitter returns                         |
| Plain JVM module, not MPP                       | Walker needs `kotlin-reflect`; matches `ultra/reflection`                                                                 |
| Core has no kontainer dep                       | Keeps it unit-testable without a container; wiring lives in `funktor/codegen`                                             |
| CLI lives funktor-side                          | The CLI *convention* (`--cli`, auto-discovery, `namespace:verb`) is funktor's. Core exposing it would invert the layering |
| `funktor/codegen` separate from `funktor/rest`  | Dev-time vs runtime; `funktor/rest` ships in every server                                                                 |
| Phased contract, not ordered contributors       | Makes contributor order structurally irrelevant — stronger than "lazy builders"                                           |
| Symbolic `TypeId` refs                          | Cycles cost nothing; no topological sort; TS type imports are legal circular                                              |
| Hard error on unmapped types                    | The Dart gen's silent `dynamic` fallback is the defect being fixed                                                        |
| Generics emitted generically, not monomorphized | Chosen 2026-07-30, reversing the original. The `z.infer` objection was measured and is false; monomorphization silently collapsed generic sealed hierarchies onto `unknown` |
| zod schemas, not types-only                     | Chosen 2026-07-29. Parse-time errors at the boundary with a precise path                                                  |
| Bare `.ts`, not npm package                     | Chosen 2026-07-29. Still no npm publishing — but **config emission inside `<out>/` is now in scope** (2026-07-30), so `--package` is narrower than it was, not merely deferred |
| Generator owns `<out>/`, writes nothing outside | Chosen 2026-07-30. Makes "regenerating destroys my edits" impossible by construction, so no file-merging mechanism is needed — merging JSONC/TS config could only be wrong-and-quiet |
| Profiles select roots; no tree-shaking stage    | Chosen 2026-07-30. Tree-shaking is the walker run twice; filtering contributors severs claim from emit                     |
| fetch default behind a transport interface      | Matches the documented no-throw-on-non-2xx semantic; no forced dep                                                        |

## Standing rule: every claim needs a drift test

**Claims are trusted, never verified.** The codec-parity check in `TsModelValidator` only iterates
DECLARED types, and a claimed type is deliberately never declared — that is the point of a claim, its
shape cannot be derived from the type graph. So a claim that is simply *wrong* produces confidently
wrong TypeScript and nothing in the generator notices.

The only defence is a test that slumbers a real value and compares it against what the claim asserts.

Demonstrated the hard way on 2026-07-29: writing `runtime/datetime.ts` from memory got two of six
shapes wrong — `MpLocalTime` slumbers to a **bare number**, not `{milliSeconds}`
(`MpLocalTimeCodec.kt`), and `MpTimezone` (a **bare string**, `MpTimezoneCodec.kt`) was missed
entirely. Both were caught only by reading the codecs.

Corollary for `Long`: "claim it as `string`" is NOT a fix for precision loss, because the claim cannot
change what Slumber writes. Making a `Long` arrive as a string needs a value class with its OWN Slumber
codec emitting a string, plus a matching claim. Both ends move together or the type lies.

## Known traps

- **`@DslMarker` on receivers, not functions** — `ApiRoutes.kt:29-33`.
- **`shouldBe` is untyped**, so `valueClass shouldBe "literal"` rots silently.
- **Module test tasks do not compile everything** — the root project has its own `src/jvmMain` that is not a module in
  `settings.gradle`. Run the compile sweep (below) before claiming Phase 0 is contained.
- **kotest ignores `--tests`** — confirm a spec ran via `build/test-results/**/TEST-*.xml`, but use the console to see
  *which* case failed (the XML mis-attributes failures to the wrong `<testcase>`).
- **Never emit `\uXXXX` escapes or raw control characters in an edit** — relevant here because the printer deals with
  indentation and line separators. Use `Char(0xNN)`.
- **`DartFile.Definition.implement()` ordering comment** (`codegen/dart/DartFile.kt:31`, "order is important here!
  Building elements might create more imports") is the smell the new design removes — imports are a function of the
  model, computed after it is frozen.

## Prior art

- **`ReflectivePathFinder`** (`funktor/rest/src/jvmMain/kotlin/security/ReflectivePathFinder.kt`) already does a
  cycle-safe transitive walk from a `KType` root through data-class ctor params and collections, with hard-excludes,
  producing a `path: List<String>` per hit — exactly the "reached via" trail needed for the validation errors. **But it
  is not sufficient as-is:** it silently stops at non-data-class types (`// TODO: what if we reach here?`, line 131), so
  sealed hierarchies, value classes and interfaces fall through. Read it before writing the walker; converging the two
  is a follow-up, not this task.

## Security notes

Marked **not** security-critical: this is a dev-time generator with no runtime auth path, and it emits types for data
that already crosses the wire.

One item worth a conscious decision rather than a default:

- [ ] `@SensitiveData` (`funktor/rest/src/commonMain/kotlin/security/SensitiveData.kt`) marks classes and properties
  that should not be logged or exposed, and `endpoint_security.kt` has a matching
  `allowsSensitiveData` declaration per endpoint. Decide explicitly whether the generator should surface these (e.g. a
  doc comment on the emitted field, or a warning in `--verbose`) or ignore them. Ignoring is defensible — the field is
  already in the API response — but it should be a decision, not an oversight.

## Test evidence

- [x] Unit: walker closure (cycles, generics, sealed hierarchies, value classes, collections, nullability)
      — `TypeWalkerSpec` (31), `GenericsSpec` (9)
- [x] Unit: claims registry — double-claim error, opaque escape, unclaimed-custom-codec error
      — `TsModelValidatorSpec` (8), `ThirdPartyContributorSpec` (3)
- [x] Unit: output collision error — `TsSdkBuilderSpec` (12)
- [x] Golden-file: emitted TS for a representative fixture set (`shouldHaveNoDiffs`, carried over)
      — `TsModelEmitterSpec` (14)
- [x] Drift test: Mp datetime slumber output keys vs `runtime/datetime.ts` field names
      — `MpDateTimeFieldParitySpec` (8); envelope equivalent is `ApiResponseParitySpec` (8)
- [x] Cross-check: query-param encoding vs `TypedRouteRenderer` — `buildUrl` in `runtime/http.ts`,
      checked in `ts-verify/verifyRuntime.ts` (omits null/empty, single key per param, encodes both
      path and query). Note the reference is `TypedRouteRenderer`, not `UriParamBuilder` — no such
      class exists.
- [x] `tsc --noEmit` on generated output — automated as `:ultra:codegen:tsVerify`, wired into `check`
- [x] Compile sweep after Phase 0:
  `./gradlew compileKotlinJvm compileTestKotlinJvm compileKotlinJs compileTestKotlinJs compileKotlin
  compileTestKotlin --continue` — check for `^e:` — green 2026-07-29 (re-run after Phase 3)
- [ ] Full test command (s) run + green: `./gradlew :ultra:codegen:check` — 118 Kotlin tests + 51
  `ts-verify` assertions green 2026-07-29. Phases 2 and 4 still to add their own.

## Review record (filled by /feature-review)

| Reviewer                       | Verdict | Confirmed findings |
|--------------------------------|---------|--------------------|
| 1. Implementation & code style | 12 raised | 11 confirmed, 1 not verified independently |
| 2. Domain expert               | 8 raised | 8 confirmed |
| 3. Security                    | 3 raised | 3 confirmed (1 line-number error, substance right) |

Run 2026-07-30 over `git diff 8e86aed5..HEAD -- ultra/codegen ultra/slumber funktor/rest settings.gradle`
(88 files, +5629/-3154). All findings adversarially verified against the code before acting.

### One finding did NOT survive verification

**The `.bufferedReader()` platform-charset claim is WRONG** — it originated in
`20260730-frontend-sdk-vue-contributors.md`, was repeated into this doc, and was handed to the
reviewers as established fact. `InputStream.bufferedReader(charset: Charset = Charsets.UTF_8)`
(kotlin-stdlib 2.4.10, `jvmMain/kotlin/io/IOStreams.kt:87`) — the default IS UTF-8. Only
`InputStreamReader(stream)` without a charset takes the platform default, and that is not what the
code does. Corrected in both docs. The classloader half of that pair is real and was fixed.

Worth keeping as a caution: this claim was believed by three reviewers and the coordinator because it
was written down, not because anyone checked it. A cited line number is not a verification.

### Fixed in this round

| Finding | Where | Note |
|---|---|---|
| No escaping layer anywhere in the emitter | new `ts/TsLiterals.kt`; 5 sites in `ts/TsModelEmitter.kt` | `identifier = "O'Brien"` emitted a file that does not parse. Security framing (injection via `@SerialName`) is real but secondary |
| Discriminator field in identifier position | `ts/TsModelEmitter.kt` | a field named `@type` emitted invalid TS |
| `TypeId` dropped type-argument nullability | `model/TypeId.kt` | `Box<String>` and `Box<String?>` collapsed onto one declaration — silent wrong output |
| Claimed polymorphic child never recorded in `usedClaims` | `model/TypeWalker.kt` | a documented claims-API use hard-failed with "internal walker invariant violation, please report it" |
| `declareUnion` read discriminator/children from the declared class, not the root parent | `model/TypeWalker.kt` | the two halves of one union disagreed; the child path already did the hop |
| `appendAlias` ignored `isRecursive` | `ts/TsModelEmitter.kt` | a value class on a cycle emitted an eager forward reference — TDZ error at module eval |
| `resource()` used ultra:codegen's classloader | `sdk/TsSdkOutput.kt`, `sdk/TsSdkBuilder.kt` | scope now carries the contributor's loader |
| Unresolved KDoc ref, 12 FQN sites | `model/TypeModel.kt` + 4 test files | repo style rules |

**All of the above are now regression-tested and mutation-tested** (2026-07-30, commits `ef5eba72`,
`2623a08d`, `3339e2ca`, `a5182d35`, `b44b7549`, `ceef31b0`). 136 tests green, 7 ts-verify fixtures,
compile sweep green.

**Two of the six fixes turned out to be wrong or incomplete, and writing the tests is what found it:**

- **`TypeId` nullability was half-done.** `canonicalKey` was fixed but `TsNames.of` was not, so the two
  now-distinct declarations competed for one const named `FxBoxString`. Found by asserting the three
  properties SEPARATELY — only the name assertion was red.
- **The root-parent hop was over-applied.** Copying `createParentSlumberer` literally moved BOTH the
  discriminator and the children to the root. The discriminator belongs there (it is what the server
  writes); the children do not, because a field typed as an intermediate sealed class cannot hold the
  root's other children, and widening emitted declarations for unreachable types. Found by a mutant
  that SURVIVED — which meant the fix was wrong, not that a test was missing.

Method note worth keeping: for anything that changes emitted TEXT, add a ts-verify fixture and run
`:ultra:codegen:tsVerify` against the REVERTED code. That turned two "the string looks right" claims
into "a real compiler rejects the alternative" — an unterminated string literal for the escaping, and
TS2448 for the recursive alias.

### Generics spike — the monomorphization decision rests on a false premise (2026-07-30)

Prompted by the maintainer asking why TypeScript generics are not emitted at all. Run as a throwaway
`ts-verify` fixture against the real toolchain (tsc 7.0.2 + zod 4.4.3, full harness config: `strict`,
`erasableSyntaxOnly`, `verbatimModuleSyntax`). **tsc clean, 8/8 runtime checks pass.**

**`z.infer` CAN see through a factory.** `type PageOf<T> = z.infer<ReturnType<typeof pageOf<T>>>`
compiles — TypeScript instantiation expressions handle it, so no hand-written interface is needed. That
was the load-bearing justification for monomorphizing and it does not hold.

What the spike verified, in both the factory-only and explicit-interface forms: parsing real payloads,
REJECTING bad ones, nesting (`pageOf(Box(Talk))`), a generic as a `z.discriminatedUnion` option (a
factory returns a concrete object schema at call time), and recursion via `z.lazy`.

Each form carries a `@ts-expect-error` assigning a wrong shape. Had inference degraded to `any` those
lines would not have errored, and `@ts-expect-error` would itself have failed the build — so the types
genuinely bind, including element types inside containers. Without that, "it compiled" would have
proven nothing.

**A cost claim of mine was also wrong and is corrected here:** generic emission does NOT turn every
reference into a call. Type positions stay plain references (`PageOf<Talk>`, nicer than `PageOfTalk`);
only SCHEMA positions become calls (`pageOf(Talk)`), with the same ordering constraints as today, and
`TsRenderer` already splits `type()` from `schema()`. The real cost is in the WALKER, which currently
reifies and would need the un-reified declaration plus a parameter mapping.

**Three open problems trace back to monomorphization**, so this is worth more than tidiness: the
generic sealed-hierarchy defect (below), the `expects<T>` proposal in the Vue design (which exists only
because `PageOf<Lock>` → `PageOfLock` is a computed name authors must guess), and declaration count
growing with instantiation count.

Decision pending with the maintainer.

### Post-review hardening run, 2026-07-30 (13 iterations, autonomous)

Backlog and per-iteration detail in `.claude/tasks/20260730-codegen-loop-handoff.md`. Final state:
**163 codegen tests + 7 ts-verify fixtures green, `:ultra:slumber:jvmTest` 1232 green, compile sweep
clean.** Commits `ef5eba72` … `29a1015d`.

Beyond regression-testing all six review fixes, the run fixed eight further confirmed findings and
turned up two nobody had raised:

| Fix | Note |
|---|---|
| `appendUnion` used TYPE names in SCHEMA position | a claimed variant emitted `z.union([CustomType])`, a type where a value is required. Found by reproducing an earlier reading-only finding |
| `slumberConfig` was optional | the codec-parity check was off by default; now required, with `TsSdkBuilder.forTesting` as the named entry point and NO way to disable it |
| Walker degraded to `unknown` in 5 positions silently | new `TypeModel.Undetermined` channel, now a hard failure. `List<*>` used to emit `unknown[]` with no report — the Dart `dynamic` defect returning |
| Collision check ignored claimed names | an app type named `MpInstant` beside the datetime claim was TS2440 inside generated code, reported by nothing |
| kotlinx JSON claims had no drift test | all five probed and found correct; guard now derives the expected zod combinator FROM the observed codec shape |
| Two vacuous assertions | one compared a value with itself; one compared a list against a hand-written copy of itself |
| `JsonElement` claim was silently opaque | the one construct that disables validation was the one the run summary never mentioned |

**The finding that matters most for future work on this module.** Of the six fixes applied during the
review round, **three were wrong, incomplete or over-applied** — and **four assertions written across
the review and this run passed for the wrong reason**, two of them tests written minutes earlier. Every
single one was caught by mutation testing; none by reading the code, including when read carefully and
more than once. Treat "the suite is green" as meaning nothing until the suite has been watched going
red. This is now the strongest evidence in the repo for the mutation-testing rule in `CLAUDE.md`.

A defect in `ultra/slumber` was also found and reproduced but deliberately NOT fixed — battle-tested
code gets its own task and review round. See `.claude/tasks/20260730-slumber-intermediate-sealed-roundtrip.md`.

### Confirmed, NOT yet fixed — tracked

| Finding | Where | Why deferred |
|---|---|---|
| `slumberConfig` defaults to `null`, silently disabling the codec-parity check | `sdk/TsSdkBuilder.kt:46`, `sdk/TsModelValidator.kt:166` | raised independently by TWO reviewers; the module's headline check is off by default. Needs an API decision (required param vs named test factory) |
| Walker degrades to `unknown` in 5 positions with no advisory | `model/TypeWalker.kt` (record value, array item, alias target, non-KClass classifier) | this is the Dart `dynamic` defect returning. `List<*>` emits `unknown[]` silently. Needs a third `TypeModel` channel |
| Generic sealed hierarchy loses its payload type | `model/TypeWalker.kt` (`createBareType()` for variants) | `Storable<Organisation>` and `Storable<Talk>` become the same TS type carrying `unknown`. Needs type-argument substitution design |
| Name-collision check ignores claimed `tsName`s | `sdk/TsModelValidator.kt` | app type named `MpInstant` + the datetime claim → TS2440 inside generated code. Also: `nameCollisionProblems`/`danglingReferenceProblems` have ZERO test coverage |
| `KotlinxJsonTsContributor`'s 5 claims have no drift test | `contributors/KotlinxJsonTsContributor.kt` | violates this doc's own standing rule. `JsonObject`'s shape is whatever `JsonUtil.unwrap` does, and nothing pins it |
| Tautological test assertion | `ts/TsModelEmitterSpec.kt:62` | `x shouldBe x`; the two neighbours pass when the variant is ABSENT (`indexOf` → -1) |
| Mp claim-coverage test compares the contributor against a copy of itself | `contributors/MpDateTimeFieldParitySpec.kt:108` | cannot detect the thing it says it detects |
| `@Slumber.Field` non-ctor props emitted required | `model/TypeWalker.kt` | `DataClassAwaker` never reads them. Ties into request-vs-response shapes, a Phase 2 design question |
| `@Slumber.Field` selection re-derived from `DataClassSlumberer` | `model/TypeWalker.kt:250-257` | verbatim copy; violates "defer to the slumber-side utility". Needs a slumber-side API |
| `JsonElement` → `z.unknown()` never reaches the advisory list | `contributors/KotlinxJsonTsContributor.kt:27` | `map()` hardcodes `opaque = false` |
| No scalar refinement (`Char` → bare `z.string()`) | `model/TypeWalker.kt:114` | `CharAwaker` maps `""` to null, so the client passes input the server rejects |
| `readArrayElements` duplicated verbatim | `ultra/slumber` collections | production code; cosmetic |
| `ThirdPartyContributorSpec` clue asserts what it cannot observe | `sdk/ThirdPartyContributorSpec.kt:128` | passes BECAUSE of the classloader bug; needs a child-loader fixture now that the bug is fixed |

### Probed and CLEAN — do not re-tread

- **Slumber array support has no security finding.** Component type comes only from the declared
  `KType`, never from input; `newInstance` size comes from an already-materialized list, so there is no
  amplification; no `ArrayStoreException` path (every element awaker returns its declared type or null,
  and `NonNullAwaker` catches null first). Multi-dimensional arrays verified correct by probe —
  `Array<Array<String>>` yields `String[][]`, not `Object[][]`.
- **No behaviour change for existing non-array types**: both new `BuiltInModule` branches are guarded
  by `cls.java.isArray` and sit below every branch that could otherwise match.
- **Dart deletion is complete** — zero dangling references; `CodeGenHints` survives with 87 call sites.
- **ts-verify supply chain is correct** — exact-pinned versions, `--frozen-lockfile`, pnpm and Node
  pinned, no corepack, sha512 integrity throughout.
- **`ultra:codegen` reaches no production classpath** — no module depends on it.
- **Emission determinism holds** — `TsDeclOrder` seeds from sorted keys, so bytes do not depend on
  contributor order.
- **Kotlin-version sensitivity, already guarded**: on 2.1.10 `typeOf<Array<Int>>().classifier` was
  `IntArray::class`, which would break `forArray`; on the pinned 2.4.10 it is `Array::class`, and
  `ArrayCodecSpec`'s boxed-array case fails loudly if that regresses.

## Incoming requirements from the frontend-SDK design (2026-07-30)

The maintainer settled the frontend direction: **Vue + Tailwind, nothing published to npm, the framework
generates the whole frontend SDK on the fly — API clients AND prebuilt components and pages.** Design
and rationale in `.claude/tasks/20260730-frontend-sdk-vue-contributors.md`; only the parts that land in
`ultra/codegen` are summarised here.

Nothing below contradicts the phased contract — all four additions are keyed inserts, so contributor
order stays structurally irrelevant.

**Emitting custom code already works.** `out.file` / `out.resource` do not care that content is an API
client, so a contributor copying `.vue` files out of its own jar resources needs no new capability. What
is missing is *coordination* between contributors, which is what these four are:

1. **Profiles / tagging.** `TsSdkBuilder(contributors)` takes the whole list with no filter, and
   `CodeGenHints.tags` has **zero call sites** (`tag(` appears twice in the repo, both in its own
   declaration, `docs/CodeGenHints.kt:29`) — vestigial from the Dart gen. Needed: a `TsSdkProfile`,
   because the ops app needs a different SDK assembled than b2b does. Profiles are **not** access
   control; see the design doc.

   **A profile selects ROOTS, never contributors, and there is no tree-shaking stage** (decided
   2026-07-30 — full reasoning in the design doc). Tree-shaking is `TypeWalker` run twice: it already
   computes a closure from entry points, and the model is a pure function of its roots, so building the
   full model first buys nothing. Filtering *contributors* is worse than useless — it silently severs
   claim from emit, because `models.ts` imports from `claim.importFrom` (`ts/TsModelEmitter.kt:43-45`)
   while the file at that path is shipped by the claiming contributor's `emit`. Narrowing the root set
   instead makes every consequence fall out for free: an unreached claim leaves `usedClaims`, so
   nothing imports it and the conditional emit ships nothing. `MpDateTimeTsContributor` is already
   written that way (`contributors/MpDateTimeTsContributor.kt:52-58`) — it is the pattern, not a
   special case. Contributor SELECTION thereby becomes as structurally irrelevant as contributor ORDER.
2. **Shared / idempotent emission.** `TsSdkOutput.add` errors on any duplicate path, even byte-identical
   (`sdk/TsSdkOutput.kt:37`). `TsRuntime`'s KDoc encodes the workaround as a rule — *"a generator that
   needs a shared module must be the single one asking for it"* (`ts/TsRuntime.kt:43`) — which will not
   survive N Vue contributors that all want `components/JsonTree.vue`. Needed:
   `out.shared(path, content)`, where identical content dedupes and **differing** content is a hard
   error naming both.
3. **Aggregation registries.** `models.ts` works only because the *builder* owns it
   (`sdk/TsSdkBuilder.kt:103`); two contributors cannot currently write **into one file**. Needed for the
   router table, the nav, the insights tab table, `tsconfig.json`/vite alias entries and the npm
   requirements manifest. A phase-3.5 registry — the emit-phase analogue of `TsTypeClaims`.
   **Config is a registry target, not a plain file emit**, precisely because it is
   N-contributors-into-one-file.
4. **`out.vue(resourceDir, files, to)`.** An **explicit file list, never classpath directory scanning** —
   scanning is how a stale file ships. A test asserts the list matches the directory.

**Import resolution is settled — do NOT build an emit-time rewriter.** Hand-written resources import via
a `@sdk/*` **path alias**, so the import text in the resource file is byte-identical to the import text
in the emitted file. This is viable because config emission is in scope: the generator writes
`<out>/tsconfig.json` with the `paths` mapping plus a vite alias fragment, both **inside the SDK dir it
owns**, so it never edits a file it does not own. The same alias name resolves to the generated output in
a consuming app and to overlaid live resource dirs in the dev harness — which is what gets IDE support,
hot reload and correct generation simultaneously. Make the alias name configurable, defaulting to
something unlikely to collide.

**Config emission is bounded by an ownership rule, and the generator will NOT merge into existing
files** (refined 2026-07-30 — reasoning in the design doc). The maintainer's concern is that
regenerating must not destroy hand-made changes in the target project; merging is the wrong answer to
it, because `vite.config.ts` is TypeScript with arbitrary expressions (no sound structural edit) and
`tsconfig.json` is JSONC (any round-trip drops comments) — and a merge is wrong-and-quiet by
construction, the failure mode this plan exists to remove. The rule is a boundary instead: **everything
under `<out>/` is owned outright — created, overwritten and deleted; nothing outside `<out>/` is ever
written.** App-side needs are met by `out.scaffold(path)` (writes only when absent, never overwrites)
and a `requires(...)` check that fails with the exact lines to paste.

That exposes the actual gap, which is **stale output, not merging**: wholesale replacement without
deletion leaves ghosts that still compile, and blind deletion of `<out>/` would eat a user file. Needs
`<out>/.sdk-manifest.json` so a run deletes exactly its predecessor's paths and no others — and
`TsSdkOutput.diffAgainst` (`sdk/TsSdkOutput.kt:61`) extended to see them, since it walks only planned
entries today and is therefore blind to a ghost.

**Two latent bugs in `TsSdkOutput.Scope.resource()` (`sdk/TsSdkOutput.kt:86`)** — harmless today, bad
once resources ship a whole frontend:

- `this::class.java.classLoader` is **`ultra:codegen`'s** loader, not the contributor's. Fine on a flat
  Gradle classpath, breaks under any isolating loader. Capture `contributor::class.java.classLoader`.
- ~~`.bufferedReader()` uses the platform default charset.~~ **NOT TRUE — see the review record.**
  `InputStream.bufferedReader()` already defaults to `Charsets.UTF_8`.

The classloader half is real and was **FIXED 2026-07-30**: `resource()` is an inner-class member, so
`this::class` really is `TsSdkOutput.Scope` and really did resolve `ultra:codegen`'s loader; `scopeFor`
now carries the contributor's loader instead.

**One check to add alongside them:** a claim declares `importFrom`, but nothing verifies anyone emits a
file there. Claiming `importFrom = "./runtime/foo"` without emitting `runtime/foo.ts` surfaces as a
module-resolution error inside generated output instead of naming the contributor. Validate that every
used claim's **relative** `importFrom` matches a planned output path — bare specifiers like `zod` are
external packages and are exempt. This is what survives of a rejected proposal to move runtime-module
ownership onto the claim; the profile decision above made the restructuring unnecessary, but the gap it
found is real and general.

**`claims.expects<T>(tsName)` is WITHDRAWN, not deferred.** It was proposed as the inverse of a claim, to
verify that a hand-written component's assumed TS name matches what the model emits — but its only
motivation was monomorphization's computed names (`PageOf<Lock>` → `PageOfLock`). Real generics landed the
same day (`20260730-codegen-generic-emission.md`, `63f9f186`), so a component author writes `PageOf<Lock>`
verbatim and there is nothing left to guess. Do not build it.

### i18n also lands here (analysed 2026-07-30)

Full analysis in the design doc's "i18n in the SDK" section. What it means for `ultra/codegen`:

- **The TS is emitted by the BUILDER, from the live generated catalog object** — not pre-generated as a
  jar resource by the Gradle plugin. Reason: the builder then sees every key, so the cross-module
  namespace collision check and the "every accessor has a fallback entry" check are real rather than
  derived claims that can drift; and the TS shape evolves with the generator instead of being frozen at
  each module's build time. **The Kotlin pass is lossless** — the model is a pure function of the flat
  catalog entries, which the generated object bakes verbatim, so re-deriving the tree at SDK-build time
  reproduces it exactly. Only `fallbackLang` and `moduleName` are dropped; bake both onto the generated
  object.
- **DONE — the key-tree model is already yours to use.** `9dd2befb` moved `I18nModelBuilder`, the node
  types and `LocaleCatalog` from the unpublished `:tooling` into `io.peekandpoke.ultra.i18n.model`
  (published), along with the `{{name}}` pattern (`I18nPlaceholders`), the plural suffixes
  (`pluralSuffixes`, `splitPluralSuffix`, `basePluralKey`) and the locale-tag grammar
  (`splitLocaleTag`, `normalizeLocaleTag`). The YAML parser, Kotlin emitter and checker stayed in
  `:tooling`. **Do not unify `:tooling` into `ultra/codegen`** — that would ship a YAML parser and a
  Kotlin source emitter to every SDK consumer and inverts the dependency sense.
  - `ultra/codegen` does **not** yet declare `api(project(":ultra:i18n"))` — deliberately, since nothing
    consumes it. Add it when you start.
  - The one rule on `ultra/i18n/src/commonMain/kotlin/model/`: it is source-included into buildSrc, so it
    must reference nothing outside its own directory. Breaking that fails the buildSrc compile before any
    project builds. (It was also restricted to Kotlin 1.8 until `9dd2befb` took buildSrc off `kotlin-dsl`;
    that limit is gone, which is why `LocaleCatalog` still carries a `String` tag it no longer needs to.)
- **A 6th registry target:** the i18n catalog set + merged accessor root.
- **A registry entry MUST carry a declared layer** (`Framework` < `App`), and emission order derives from
  it. This is the one place where the "contributor order is structurally irrelevant" property is not
  enough: catalog precedence is genuinely order-dependent — `I18n.Builder.build()` reverses the install
  list so the app's override wins (`ultra/i18n/src/commonMain/kotlin/I18n.kt:61`). Aggregate by
  contribution order and whether an override takes effect depends on DI iteration order, silently and
  per-key. Ties within a layer are a hard error.
- **Two modules claiming the same top-level namespace is a hard error naming both.** Kotlin gets this
  free (ambiguous extension at the call site); merging TS objects would silently drop one.
- **A new `TsRuntime.Module.I18n`** — hand-written `ts/runtime/i18n.ts` mirroring `MessageResolver`
  (locale chain, catalog precedence, single-pass `{{name}}` substitution, `_other` plural fallback,
  key-as-miss-marker). Standing rule applies: it needs a parity test, and the corpus already exists in
  `tooling/i18n-fixture` — one expectation table read by both the Kotlin spec and the node harness.
- **No new name-mangling module.** Quoting every key in the emitted object literal makes TS reserved
  words and hyphenated keys work as-is, so there is no `TsNames` sibling to `KotlinNames.kt`.
- **i18n is an emit action + registry entry, not a contributor kind** — a catalog is not a type, so an
  i18n contributor would have no root to condition on and could not be profiled. Whichever contributor
  ships a component ships its strings too, under one emit condition.

#### Sequencing — what to pick up when

The design above is settled; nothing here is waiting on a maintainer decision. But most of it sits behind
the aggregation registry, so the order matters:

1. **`TsRuntime.Module.I18n` — startable NOW, independent of everything else.** Hand-written
   `ts/runtime/i18n.ts`, no registry, no profiles, no catalog enumeration. Take it first: it is the
   highest-risk piece (a resolver divergence means the frontend renders different text than the server —
   silently, in one language only), and its corpus already exists. Extend `tooling/i18n-fixture` with a
   single declarative expectation table (locale, accessor path, args, expected) read by **both** the
   Kotlin spec and the node harness; two hand-maintained lists would drift, which is the exact failure
   this test exists to catch.
2. **`EnumerableI18nCatalog` + `moduleName`/`fallbackTag` on the generated catalog object.** Small, and
   it lands in `ultra/i18n` + `:tooling`/`KotlinEmitter` rather than in `ultra/codegen` — say so if you
   would rather it came from this side.
3. **The single-module accessor emitter** — catalog data + key tree → TS. Testable against one module
   before aggregation exists.
4. **Everything multi-module** (the registry target, the layer ordering, the namespace-collision check)
   waits on aggregation registries, addition #3 above.

Two things stay open and block **none** of the above:

- The `Message` key-vs-text wire fork — a maintainer decision, and it governs whether the framework's
  *message* catalogs ship at all, not how the emitter works.
- Bake-all-locales vs per-locale lazy chunks — the file layout is identical either way; only the index
  differs, so it can be decided after the emitter exists.

**Also:** a `JavaTimeTsContributor` will be needed (`java.time.LocalDateTime`/`Instant` go through custom
Slumber codecs in `builtin/datetime/javatime/`, so they need claims plus a parity test — the standing
rule above). And `MpDateTimeTsContributor.kt:32` uses a fully-qualified `kotlin.reflect.KClass<*>` in its
companion, which breaks the repo's no-FQN rule; the maintainer expects this to be picked up in the
review rounds, which have not run yet.

## Follow-ups

- [ ] **DOCS task** — required on archive. This adds two public modules (`ultra/codegen`,
  `funktor/codegen`) with a public extension point (`TsSdkContributor`). Needs a docs-site page plus the LLM mirror
  (`docs-site/src/data/llms/*.md`).
- [ ] **`ReflectivePathFinder` convergence** — retrofit it onto the `ultra/codegen` walker, which also fixes its
  non-data-class gap (line 131). Security-relevant code, so a separate task with its own review.
- [ ] **Array support in Slumber** (own task — touches battle-tested code). Raised 2026-07-29 while
      fixing walker/Slumber parity. `Array` is not `Iterable`, so `BuiltInModule.getSlumberer` never
      dispatches for a declared array type and `SlumberConfig.getSlumberer` throws. **Deferred, and
      currently not blocking anything: zero array-typed properties exist across `funktor/`,
      `funktor-demo/`, `ultra/model/` and `ultra/remote/`.** The work is asymmetric — slumbering is
      nearly free (`CollectionSlumberer.kt:13` already handles `Array<*>` values, it is just never
      reached), but awaking needs `java.lang.reflect.Array.newInstance` plus separate handling for the
      six primitive array types, which do not unify with `Array<T>`. Adding slumber-only would create a
      type that serializes but cannot round-trip — strictly worse than the current fail-fast. Arguably
      the right long-term answer is to keep refusing: `Array` has reference equality and is mutable, so
      `List` is the correct DTO shape regardless.
- [ ] **`--package` mode** — package.json/tsconfig emission, when a second SDK consumer appears.
- [ ] **Dart emitter v2** — if ever needed, build it on the `ultra/codegen` model; it inherits the closure and
  validation for free.
