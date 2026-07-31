# `Redacted<T>` and the removal of Jackson

**Status:** PLANNED — decided 2026-07-31, not started
**Security-critical:** yes — this is what finally closes
`.claude/tasks/20260731-config-secrets-in-insights.md`
**Supersedes:** the `@JsonIgnore` constraint recorded in `.claude/tasks/20260731-depot-findings.md`

## Why

`@JsonIgnore` is opt-in, and it has already failed silently four times: `JwtConfig.signingKey`,
`UltraSecurityConfig.csrfSecret`, `ArangoDbConfig.password` and `MongoDbConfig.connectionString` are all
written verbatim into every insights record. Two of them carry a hand-written redacting `toString()` and
a KDoc promising protection, which is worse than nothing — an auditor reads it and moves on.

**Maintainer position (2026-07-31): Jackson is a repeat security offender and the goal is to remove it
entirely.** The failure mode is structural, not accidental: Jackson serialises everything by default, so
safety depends on someone remembering an annotation on every new field, forever.

`Redacted<T>` inverts that. The type states the constraint at the declaration site, the compiler carries
it, and no serializer can be talked out of it.

## The design

```kotlin
data class JwtConfig(
    val signingKey: Redacted<String>,
    val issuer: String,
)

data class FunktorDemoConfig(
    val aws: Redacted<AwsConfig>,        // a whole SUBTREE, not just a leaf
    val arangodb: Redacted<ArangoDbConfig>,
)

JwtGenerator(config.signingKey.value)    // accessor is `value`, as on every other wrapper in the repo
```

| Decision | Choice | Why |
|---|---|---|
| Name | **`Redacted<T>`** | Reads naturally at a declaration and on a subtree |
| Accessor | **`.value`** | Consistent with `OrgId.value`, `EmailAddress.value` |
| Not a value class | **plain class** | A `@JvmInline value class` is *inlined*, and both Slumber's `ValueClassSlumberer` and Jackson unwrap straight past any custom codec — **measured, see below**. A plain class has a real runtime type for a codec to match on |
| Generic | **yes, `<T>`** | Subtree redaction is the point; a `String` wrapper cannot express `Redacted<AwsConfig>` |
| Round trip | **deliberately broken** | Deserialises the real value, serialises `"REDACTED"`. One-way by design |
| Home | **`ultra/common`, `commonMain`, package `io.peekandpoke.ultra.common.model`** | See below — zero new dependency edges, and it is where `ultra:model` is eventually headed |
| `toString()` | **redacts** | Not optional. This is the `JwtConfig` lesson inverted — safe by construction, so nobody hand-writes it |

## Measured, not assumed (2026-07-31)

A probe wrapping a secret in a **value class** with a custom kotlinx serializer, run through all three
serializers:

```
KOTLINX      = {"issuer":"iss","signingKey":"***redacted***"}   ← honoured
KOTLINX-BACK = Conf(issuer=iss, signingKey=SUPER-SECRET)        ← asymmetry works
SLUMBER      = {issuer=iss, signingKey=SUPER-SECRET}            ← LEAKED
JACKSON      = {"issuer":"iss","signingKey":"SUPER-SECRET"}     ← LEAKED
```

**This is why `Redacted<T>` must not be a value class.** kotlinx honours a custom serializer on one;
Slumber and Jackson both inline it and never consult the codec.

## Where it lives, and why not `ultra/model`

**`ultra/common/src/commonMain/kotlin/model/Redacted.kt`**, package `io.peekandpoke.ultra.common.model`.

Maintainer direction (2026-07-31): `ultra:model` is not pulling its weight, and the intent is to fold it
into `ultra:common` under `io.peekandpoke.ultra.common.model` eventually. This type lands where that is
going rather than where it is coming from.

It is also the better answer on the merits, which is worth recording because the first draft of this plan
said `ultra/model`:

| | `ultra/common` | `ultra/model` |
|---|---|---|
| New dependency edges | **none** | five — `ultra:slumber`, `ultra:security`, `karango:core`, `monko:core`, `ultra:vault` all reach only `ultra:common` today |
| kotlinx serialization | must be added | already present |
| Native targets | jvm, js, linuxX64, macosX64, macosArm64, mingwX64 | same |

The one cost is adding `Deps.KotlinX.serialization_core` to `ultra/common`'s `commonMain` — needed
because `@Serializable(with = …)` must sit on the class, so the *defining* module needs kotlinx on its
classpath. That is not speculative: **`ultra/maths`, `ultra/streams` and `ultra/datetime` already combine
the identical native target set with kotlinx serialization**, so the configuration is proven in this repo.
It is also required anyway the day `ultra:model` merges in.

`ultra:slumber` reaching `ultra:common` creates no cycle — `common` has no project dependencies at all.

## Slumber fits this better than expected

Verified against `ultra/slumber/src/jvmMain/kotlin/builtin/BuiltInModule.kt`:

- `getAwaker(type, …)` is asked with the **declared** type, so `Redacted<AwsConfig>` arrives with its
  type argument intact and the inner value can be awoken by the normal machinery.
- `getSlumberer(type, …)` is asked with the **runtime** class (`data::class`), where the generic is
  erased — which does not matter, because it emits a constant.
- Both are `when` chains whose KDoc already states *"branch order is load-bearing"*, so a first branch is
  the documented way to pre-empt every other codec.

The asymmetry `Redacted<T>` needs is the asymmetry Slumber already has.

## Staging

Ordered so the security-critical part lands first and nothing is blocked on hygiene.

### Stage 1 — the type and its codecs

- `Redacted<T>` in `ultra/common/src/commonMain/kotlin/model/`, with a redacting `toString()`.
- Add `Deps.KotlinX.serialization_core` to `ultra/common`'s `commonMain`.
- Slumber `Awaker` + `Slumberer`, mounted as the **first** branch in `BuiltInModule` so it is available
  out of the box and cannot be shadowed.
- kotlinx serializer (a generic custom serializer taking the element serializer).
- **`ultra/slumber` has another owner — coordinate before touching `BuiltInModule`.**
- Tests: both serializers emit the identical placeholder; the inner value awakes correctly for a scalar
  AND a subtree; `toString()` redacts; a known secret does not survive a slumber.

### Stage 2 — adopt it, deleting the annotations

`JwtConfig.signingKey`, `UltraSecurityConfig.csrfSecret`, `ArangoDbConfig.password`,
`MongoDbConfig.connectionString`, `KtorConfig.Security`'s two passwords, `AwsSesConfig`, `AwsS3Config`,
`ultra/vault/domain.kt`. Call sites unwrap with `.value`.

Also decide `AppConfig.keys: Map<String, String>` — the bag reached by `getKeyOrNull(name)`, i.e. exactly
where an app puts its own secrets. Not yet decided.

**While checking `KtorConfig.Security`:** it uses `@get:JsonIgnore` with no `@JsonProperty`, which in
Jackson normally suppresses **deserialization** too — `keyStorePassword` may never have loaded from
config at all. Verify rather than port the bug.

### Stage 3 — the insights write path off Jackson

`InsightsMapper.kt` and `InsightsFull`'s `convertValue`. Gated on Slumber being able to describe every
collector `Data` class: `AppConfigCollector`'s two `Any` fields, `HttpMethod`, `HttpStatusCode`,
`DebugInfo`, `QueryProfiler.Entry.Impl`, `UserRecord`. These are the plan's six Slumber-compat blockers
(`20260730-frontend-sdk-vue-contributors.md`).

**Delete the interim redaction here** — see below.

### Stage 4 — the rest of Jackson

- `funktor/rest/codec/SlumberRestCodec.kt` — Jackson is the JSON *text* layer beneath Slumber here, on
  every API request and response. The largest remaining user and the one to treat most carefully.
- `funktor/core/JsonPrinter.kt`, `karango/core/aql/printer.kt`, `monko/core/lang/printer.kt` — debug
  printers, no wire impact.
- Then drop the dependency. Note only `funktor/messaging/build.gradle.kts` declares Jackson directly;
  everywhere else it arrives transitively, so removal must check the transitive source too.

## Interim, until stage 3 lands — MUST BE DELETED

Insights is enabled in the demo dev config and writes the signing key today, and stages 1–3 are a
multi-module project. So: **name-based redaction inside `AppConfigCollector`**, reusing the
`HeaderLogging` sensitive-name policy over the serialised config tree. ~20 lines, entirely inside
`funktor/insights`.

**This is throwaway. Stage 3 must delete it**, or the repo ends up with two redaction mechanisms and
nobody knows which is load-bearing. It is listed here rather than only in the code so that removal is
tracked rather than remembered.

## Not decided

- `AppConfig.keys` — redact the whole map, or type it `Map<String, Redacted<String>>` and change
  `getKeyOrNull`?
(Nothing outstanding here beyond the `AppConfig.keys` question above.)

## Handed to the codegen agent (2026-07-31)

**`Redacted<T>` must emit `string`, never the shape of `T`.** Recorded in
`.claude/tasks/20260729-ts-sdk-codegen.md` under "INCOMING". The generator resolves generics by descending
into the type argument, and this is the case where the declared argument is deliberately not what travels
on the wire — descending would generate a zod schema expecting an object where `"REDACTED"` arrives, i.e.
a browser-side parse failure on the one field guaranteed never to arrive intact. Same precedence rule as
Slumber: check before any generic-descent logic, not after.
