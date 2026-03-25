# Plan: Decompose `:ultra:common` into Focused Modules

## Goal

Break the monolithic `:ultra:common` module into smaller, focused modules so that consumers
only pull in the dependencies they actually need. After decomposition, `:ultra:common` retains
only pure utility functions (strings, numbers, collections, enums, regex, etc.).

Backward compatibility of package names is NOT a concern. APIs must stay the same.

## Naming Convention

Modules live under `ultra/` and are referenced as `:ultra:<name>` in Gradle.
No "common-" prefix. Example: `:ultra:datetime`, `:ultra:reflection`.

---

## Proposed Modules

### 1. `:ultra:common` (slimmed down) — Pure utilities

**Keeps only:**

- Root-level utils: string extensions (`ucFirst`, `lcFirst`, `camelCaseSplit`, `ellipsis`, `encodeUriComponent`, ...),
  number extensions (`toFixed`, `roundWithPrecision`), collection/list/set extensions
- `ComparableTo<T>`
- `Observable`, `GetAndSet`, `Observer`
- `WeakReference`, `WeakSet`, `RunSync` (expect/actual)
- `Lookup`, `SimpleLookup`
- Enum utils (`safeEnumOf`, `safeEnumOrNull`, `safeEnumsOf`)
- `Random.nextBin()`
- Regex constants (`UrlWithProtocolRegex`, `EmailRegex`)
- `Placeholders`
- `TypedKey`, `TypedAttributes`, `MutableTypedAttributes`
- `recursion/` package
- `maths/` package — `Ease` object, all easing functions (`In.*`, `Out.*`, `InOut.*`, `linear`, etc.),
  `Ease.BoundFromTo`, `Ease.Timed`, `easeFn()`, `Ease.calc()`
- JVM utils: `hashing.kt`, `encoding.kt`, `files.kt`, `network/NetworkUtils.kt`
- JVM `classes.kt`, `strings.kt`, `numbers.kt` (platform actuals)
- JS platform actuals (`strings_mp.kt`, `numbers.kt`, `WeakReference.kt`, `WeakSet.kt`, `RunSync.kt`)

**Dependencies:** `kotlin-stdlib`, `kotlin-reflect` (for `Lookup` and `KType.isPrimitive`),
`:ultra:datetime` (Kronos for `Ease.Timed`)

**Rationale:** Leaf utilities with no heavy deps. Every other module can depend on this cheaply.

---

### 2. `:ultra:model` — Serializable data models

**What moves here:**

- `de.peekandpoke.ultra.common.model.*`
- `Message`, `MessageCollection` / `Messages`, `ResultWithMessages`
- `Paged`, `PagedSearchFilter`
- `Operators` (comparison enum + functions)
- `Tuple1` through `Tuple10` + `tuple()` factories + `plus()`/`append()`
- `ObjectKey`, `FileBase64`, `AuditLog`, `EmptyObject`

**Dependencies:** `:ultra:common`, `kotlinx-serialization-core`, `kotlinx-serialization-json`

**Rationale:** Reusable DTOs that many modules need but that don't require datetime, ktor, or caching.

---

### 3. `:ultra:datetime` — Multiplatform date/time system

**What moves here:**

- `de.peekandpoke.ultra.common.datetime.*`
- Core types: `MpInstant`, `MpLocalDate`, `MpLocalTime`, `MpLocalDateTime`, `MpZonedDateTime`
- `MpTimezone`, `MpLocalTimeSlot`
- Range types: `MpInstantRange`, `MpLocalDateRange`, `MpClosedLocalDateRange`, `MpZonedDateTimeRange`
- Period types: `MpDatePeriod`, `MpDateTimePeriod`, `MpTemporalPeriod`
- `Kronos` (pluggable time source) + `KronosDescriptor`
- All serializers (`MpInstantSerializer`, etc.)
- `MpDateTimeParser`, `DateTimeRangeConverter`
- Constants, extension functions
- JVM: `constants.kt`, `conversion.kt`
- JS: `JsJodaTimeZoneModule.kt`

**Dependencies:** `:ultra:common`, `kotlinx-datetime`, `korlibs-time` (to be removed later), `kotlinx-serialization`

**JS dependencies:** `js-joda-timezone` (npm)

**Rationale:** Largest subsystem in `:common`. Isolates `korlibs-time` and `kotlinx-datetime` to only the
modules that actually need date/time.

---

### 4. `:ultra:cache` — Caching infrastructure

**What moves here:**

- `de.peekandpoke.ultra.common.cache.*`
- `Cache<K, V>` interface
- `FastCache` + `Builder` + `Action` types + `ActionUpdates`
- Behaviours: `ExpireAfterAccessBehaviour`, `MaxEntriesBehaviour`, `MaxMemoryUsageBehaviour`
- `CacheEvictor`, `ValueSortedMap`
- `ObjectSizeEstimator`, `ObjectSizeEstimatorPlatform`

**Dependencies:** `:ultra:common`, `:ultra:datetime` (Kronos for TTL expiry), `kotlinx-coroutines-core`

**Rationale:** Scopes the coroutines dependency. Only modules that cache data need this.

---

### 5. `:ultra:remote` — HTTP client abstraction

**What moves here:**

- `de.peekandpoke.ultra.common.remote.*`
- `ApiClient` + `Config`
- `RemoteRequest`, `RemoteResponse` (expect/actual + platform impls)
- `RequestInterceptor`, `ResponseInterceptor` + built-in interceptors
- `ApiEndpoint` (Get/Post/Put/Delete/Patch), `TypedApiEndpoint`
- `ApiResponse`, `EmptyApiResponse`, `RemoteException`
- `buildUri()` + `UriParamBuilder` + `uriToParamsCache`
- `createRequest()`, `KSerializer.api()`, `.apiList()`, `.apiPaged()`
- `ApiAccessLevel`
- JS: `RemoteRequestImpl`, `RemoteResponseImpl`, `ErrorLoggingResponseInterceptor`, `SetBearerRequestInterceptor`

**Dependencies:** `:ultra:common`, `:ultra:model` (for `Paged` in `apiPaged()`), `ktor-client-core`,
`kotlinx-serialization`

**Rationale:** Main reason ktor-client is pulled in. Modules that don't make HTTP calls shouldn't depend on ktor.

---

### 6. `:ultra:reflection` — JVM reflection utilities

**What moves here:**

- `de.peekandpoke.ultra.common.reflection.*` (JVM only, not multiplatform)
- `TypeRef<T>` + pre-cached primitive type refs
- `ReifiedKType`
- `kType.kt`, `types.kt`, `functions.kt` extension utils
- `annotations.kt` — annotation scanning
- `ChildFinder`
- `classes.kt` — class reflection helpers

**Dependencies:** `:ultra:common`, `kotlin-reflect`

**Rationale:** JVM-only, heavy reflection usage. Only needed by Slumber (serialization) and Kontainer (DI).

---

### 7. `:ultra:fixture` — Test data generators

**What moves here:**

- `de.peekandpoke.ultra.common.fixture.*` — `LoremIpsum`, `LoremCat`, `LoremPicsum`

**Dependencies:** `:ultra:common`

**Rationale:** Test data generators are a distinct concern. Useful for demos, tests, and prototyping
but not needed by production code.

---

### 8. `:ultra:extras` — Niche / optional utilities

**What moves here:**

- `de.peekandpoke.ultra.common.markup.*` — `ImageSrcSet`, `ImageSizes`, `CloudinaryImage`,
  `CloudinaryImageSrcSetGenerator`, `placeholders.kt`
- `de.peekandpoke.ultra.common.docs.*` (JVM) — `ExampleCodeExtractor`, `ExampleRunner`, `ExamplesToDocs`

**Dependencies:** `:ultra:common`

**Rationale:** Very few consumers use these. Could be split further later if needed.

---

## Dependency Graph

```
:ultra:common          (utils + maths; depends on :ultra:datetime for Ease.Timed)
    |
    +-- :ultra:model           (+kotlinx-serialization)
    +-- :ultra:datetime        (+kotlinx-datetime, korlibs-time, kotlinx-serialization)
    +-- :ultra:reflection      (+kotlin-reflect, JVM only)
    +-- :ultra:fixture         (no additional deps)
    +-- :ultra:extras          (no additional deps)
    |
    +-- :ultra:cache           (+:ultra:datetime, +kotlinx-coroutines)
    +-- :ultra:remote          (+:ultra:model, +ktor-client)
```

Note: `:ultra:common` depends on `:ultra:datetime` because `Ease.Timed` uses `Kronos`.
This creates a circular-looking relationship but it's one-directional: `datetime` does NOT
depend on `common`. Alternatively, `Ease.Timed` could accept a `() -> Long` lambda instead
of `Kronos` to break this coupling entirely.

## Consumer Impact

| Consumer           | Today           | After decomposition                                                     |
|--------------------|-----------------|-------------------------------------------------------------------------|
| `:ultra:kontainer` | `:ultra:common` | `:ultra:common`, `:ultra:reflection`                                    |
| `:ultra:slumber`   | `:ultra:common` | `:ultra:common`, `:ultra:model`, `:ultra:datetime`, `:ultra:reflection` |
| `:ultra:vault`     | `:ultra:common` | `:ultra:common`, `:ultra:model`, `:ultra:datetime`, `:ultra:remote`     |
| `:ultra:security`  | `:ultra:common` | `:ultra:common`, `:ultra:model` (tbd)                                   |
| `:ultra:meta`      | `:ultra:common` | `:ultra:common` (tbd)                                                   |
| `:mutator:core`    | `:ultra:common` | `:ultra:common`, `:ultra:model`                                         |
| `:kraft:core`      | `:ultra:common` | `:ultra:common`, `:ultra:model`, `:ultra:datetime`, `:ultra:remote`     |
| `:funktor:core`    | `:ultra:common` | `:ultra:common`, `:ultra:model`, `:ultra:datetime`, `:ultra:remote`     |
| `:karango:core`    | `:ultra:common` | `:ultra:common`, `:ultra:model`, `:ultra:datetime`                      |

## Migration Steps

### Phase 1: Create new modules (no breaking changes)

1. Create module directories under `ultra/` with `build.gradle.kts` for each new module
2. Register modules in `settings.gradle.kts`
3. Move source files from `:ultra:common` into new modules (keeping same class/function names)
4. Move corresponding test files
5. Verify each new module compiles independently

### Phase 2: Update consumers

6. Update `build.gradle.kts` of each consumer to depend on the specific modules they need
7. Remove the blanket `:ultra:common` dependency where it was pulling in everything
8. Fix any import changes if package names change (though APIs stay the same)

### Phase 3: Clean up

9. Verify all tests pass across the entire project
10. Update Maven publishing configuration for new modules
11. Remove dead code from slimmed-down `:ultra:common`

## Open Questions

- **Package names:** Should moved classes keep their `de.peekandpoke.ultra.common.*` package or
  get new packages like `de.peekandpoke.ultra.datetime.*`? User said backward compat is not an issue
  for package names, so new packages are preferred for clarity.
- **`:ultra:extras` granularity:** Is it worth splitting markup/fixture/docs into 3 separate modules,
  or is one grab-bag sufficient?
- **`kotlin-reflect` in `:ultra:common`:** The slimmed-down common still needs `kotlin-reflect` for
  `Lookup` and `KType.isPrimitive`. Worth keeping, or move those to `:ultra:reflection`?
