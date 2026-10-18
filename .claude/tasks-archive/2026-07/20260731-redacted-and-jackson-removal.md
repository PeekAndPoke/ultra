# `Redacted<T>` and the removal of Jackson

**Status:** DONE and ARCHIVED (2026-07-31). All stages complete, review gate PASSED. **Zero Jackson in Kotlin source, and no first-party module declares it — the coordinates are gone from `Deps.kt` entirely.** What remains on the classpath is internal to third-party libraries; see "Where Jackson still is" below.
**Security-critical:** yes — this is what finally closes
`.claude/tasks-archive/2026-07/20260731-config-secrets-in-insights.md`
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

> **Note (2026-07-31, after `9fe2a21a`):** the `JwtConfig` snippet below is how it looked when this
> work landed. `signingKey` has since become `keys: List<JwtSigningKey>`, each key holding its own
> `secret: Redacted<String>`. The `Redacted` design is unchanged — only its host moved one level down.

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
| Home | **`ultra/common`, `commonMain`, package `io.peekandpoke.ultra.common.model`** | See below — zero new dependency edges, it is where `ultra:model` is eventually headed, and it is the ONLY home that allows both the declaration-site annotation and a built-in codec |
| Wire shape | **`@Slumber.As(String::class)`** | Declares the shape once instead of every code generator special-casing it. Requires the annotation nest to move to `ultra:common` — see below |
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

### `ultra:model` was considered and does NOT work — the reason is worth keeping

The maintainer's first instinct was `ultra:model`, on the grounds that `Redacted<T>` should carry
`@Slumber.As(String::class)` and `ultra:common` cannot depend on `ultra:slumber` (correct — `slumber`
declares `api(project(":ultra:common"))`, so the reverse is a cycle).

But that trades one cycle for another. `ultra:slumber` must **see** `Redacted<T>` to register its codec
as the first branch in `BuiltInModule`. If `Redacted<T>` sits in `ultra:model` and `model → slumber` for
the annotation, then `slumber → model` closes the loop. **You get the declaration-site annotation or the
built-in codec, never both** — and an opt-in codec is the wrong failure mode for a security default,
because an app that forgets to register it writes secrets in the clear and says nothing.

Resolved by moving the annotation instead: the `Slumber` nest goes to `ultra:common`, package
`io.peekandpoke.ultra.common.slumber` (see `.claude/tasks/20260731-slumber-as-declared-wire-shape.md`
§6). It is annotations only, with no machinery and no slumber dependency, so nothing is dragged along.
Then `Redacted<T>` lives in `ultra:common` **with** its annotation, and `ultra:slumber` sees both.

## Slumber fits this better than expected

Verified against `ultra/slumber/src/jvmMain/kotlin/builtin/BuiltInModule.kt`:

- `getAwaker(type, …)` is asked with the **declared** type, so `Redacted<AwsConfig>` arrives with its
  type argument intact and the inner value can be awoken by the normal machinery.
- `getSlumberer(type, …)` is asked with the **runtime** class (`data::class`), where the generic is
  erased — which does not matter, because it emits a constant.
- Both are `when` chains whose KDoc already states *"branch order is load-bearing"*, so a first branch is
  the documented way to pre-empt every other codec.

The asymmetry `Redacted<T>` needs is the asymmetry Slumber already has.

## Config loading is pure Slumber — and the Awaker is what keeps HOCON unchanged

Raised by the maintainer (2026-07-31): *can a server app actually load a config containing
`Redacted<T>`, given it comes in through ktor and HOCON?* Answered by probing, not reading.

**The path has no Jackson and no ktor typing in it:**

```
application.<env>.conf  →  HOCON Config  →  config.root().unwrapped()  →  Map<String, Any?>
                        →  Codec.default.awake(type, data)              ← Slumber, and only Slumber
```

`funktor/core/src/jvmMain/kotlin/config/AppConfig.kt:94-108`. HOCON produces an untyped map; every bit
of the typing is Slumber's.

**So it works — but ONLY because of the Awaker, and that is a bigger deal than it sounds.** Measured
with a plain generic wrapper and no custom codec registered:

| HOCON shape | Result |
|---|---|
| `signingKey = "abc"` — what every config file has today | **`AwakerException: Value at path 'root.secret' must not be null`** |
| `signingKey { value = "abc" }` — the wrapper's constructor shape | awakes fine |

Without the custom Awaker, Slumber falls through to `DataClassAwaker`, which wants the wrapper's own
constructor shape. Adopting `Redacted<T>` would then mean **rewriting every config file in every app**
to nest its secrets one level deeper — including apps outside this repo.

**Consequence for the Awaker's contract, worth stating precisely because a naive implementation gets it
wrong:** it must take the RAW node — whatever the inner type would accept — awake `T` from it by the
normal machinery, and wrap the result. It must NOT delegate to the data-class awaker or expect a
`value` key. The declared type is available (`getAwaker` receives it), so `T` is known.

Stage 1 therefore has a concrete acceptance test beyond round-tripping: **load a config from
natural-shaped HOCON where a field is `Redacted<String>` and another is `Redacted<SomeObject>`, and
assert both come back with their real values.** If that passes, no config file anywhere has to change.

## Staging

Ordered so the security-critical part lands first and nothing is blocked on hygiene.

### Stage 1 — the type and its codecs — **DONE 2026-07-31**

`ultra/common/src/commonMain/kotlin/model/Redacted.kt` (+ `RedactedSerializer`),
`ultra/slumber/src/jvmMain/kotlin/builtin/model/RedactedCodec.kt`, registered as the **first** branch of
both `getAwaker` and `getSlumberer`. 7 specs in `ultra:common`, 5 in `ultra:slumber`.

Two things worth carrying forward:

- **`ultra:common` needed the serialization COMPILER PLUGIN, not just the dependency.** `.serializer()`
  does not resolve without `kotlin("plugin.serialization")`. The runtime artifact alone compiles the
  class and fails the tests.
- **Re-reading the type's own output differs by `T`.** `Redacted<String>` comes back holding the
  placeholder; `Redacted<SomeObject>` **throws**, because a string is not that object's shape. Throwing
  is the better outcome and is pinned by a test — the alternative is silently handing back an object
  whose fields were invented.

Mutations, each killing the right test: dropping the Slumberer branch, dropping the Awaker branch,
leaking the inner value from the Slumberer, writing the real value from the kotlinx serializer, and a
leaking `toString()`. Full compile sweep clean; `ultra:vault` (285), `funktor:all` (152) and every other
slumber-dependent suite unaffected.

Deferred deliberately: `@Slumber.As(String::class)` and the annotation-nest move, since the annotation
does not exist yet. The type works without it; codegen has the explicit rule meanwhile.

#### original scope

- **Prerequisite:** move the `Slumber` annotation nest to `ultra:common`, package
  `io.peekandpoke.ultra.common.slumber`. Tracked in
  `.claude/tasks/20260731-slumber-as-declared-wire-shape.md` §6.
- `Redacted<T>` in `ultra/common/src/commonMain/kotlin/model/`, package
  `io.peekandpoke.ultra.common.model`, carrying `@Slumber.As(String::class)` and a redacting
  `toString()`.
- Add `Deps.KotlinX.serialization_core` to `ultra/common`'s `commonMain`.
- Slumber `Awaker` + `Slumberer`, mounted as the **first** branch in `BuiltInModule` so it is available
  out of the box and cannot be shadowed.
- kotlinx serializer (a generic custom serializer taking the element serializer).
- **`ultra/slumber` has another owner — coordinate before touching `BuiltInModule`.**
- Tests: both serializers emit the identical placeholder; the inner value awakes correctly for a scalar
  AND a subtree; `toString()` redacts; a known secret does not survive a slumber.

### Stage 2 — adopt it, deleting the annotations — **DONE 2026-07-31**

Converted: `JwtConfig.signingKey`, `UltraSecurityConfig.csrfSecret`, `ArangoDbConfig.password`,
`MongoDbConfig.connectionString`, `KtorConfig.Security`'s two passwords, `AwsSesConfig.secretAccessKey`,
`AwsS3Config.secretAccessKey`, and `AppConfig.keys` → `Map<String, Redacted<String>>`. Every
`@JsonIgnore` on a secret is gone, and both hand-written redacting `toString()` implementations with it
— `Redacted` redacts itself, so the *generated* `toString` is safe.

The compiler enumerated the call sites, which is the point of using a type: ~20 across
`ultra/security`, `karango`, `monko`, `funktor/auth`, `funktor/messaging`, `funktor/cluster`,
`funktor-demo` and the **root project's own `src/jvmMain`** — the source set CLAUDE.md warns no module
test task ever compiles.

**⚠ THE INTERIM `ConfigRedaction` IS STILL LOAD-BEARING. Do not delete it here.** Measured by removing
it after the conversion: a real record then contains
`"signingKey": { "value": "ka2fEBWhmjPFpaPhg5Iir6tAX1COT0lG…" }`. `Redacted<T>` teaches Slumber and
kotlinx; **Jackson knows nothing about it** and serialises the wrapper as an ordinary object, so the
secret survives one level deeper. `AppConfigCollector` writes through Jackson, so the name-based
redaction is what keeps the key out of records until stage 3. The insights e2e catches this — it failed
exactly as it should when the interim was removed.

Two defects found in existing tests while converting:

- `UltraSecurityConfigSpec` had `config.csrfSecret.isNotBlank()` — a **dangling expression asserting
  nothing**, which passed whatever the value was.
- `MongoDbConfigSpec`/`MonkoModuleSpec` compared `connectionString shouldBe "literal"`. `shouldBe` is
  untyped, the CLAUDE.md trap — these failed loudly here rather than rotting silently, but they are the
  same shape.

#### original scope

`JwtConfig.signingKey`, `UltraSecurityConfig.csrfSecret`, `ArangoDbConfig.password`,
`MongoDbConfig.connectionString`, `KtorConfig.Security`'s two passwords, `AwsSesConfig.secretAccessKey`,
`AwsS3Config.secretKey`. Call sites unwrap with `.value`.

**NOT `ultra/vault/domain.kt`** — corrected 2026-07-31. Its three `@get:JsonIgnore` hide `collection`,
`asRef` and `asStored`, which are *derived* properties excluded to stop recursion and duplication, not
secrets. `Redacted<T>` would be wrong for them and would emit a placeholder where the field is currently
absent. Both this plan and the depot-findings table listed them, from reading the annotation rather than
the fields.

**`AppConfig.keys` becomes `Map<String, Redacted<String>>`** (decided 2026-07-31). Three call sites, two
files: `GoogleSsoAuth.kt:79`, `GithubSsoAuth.kt:69,70`. Secret-by-default is right for a bag whose
purpose is app-supplied keys; the OAuth client IDs it also holds stay redacted, which is already true
under the interim.

Also decide `AppConfig.keys: Map<String, String>` — the bag reached by `getKeyOrNull(name)`, i.e. exactly
where an app puts its own secrets. Not yet decided.

**While checking `KtorConfig.Security`:** it uses `@get:JsonIgnore` with no `@JsonProperty`, which in
Jackson normally suppresses **deserialization** too — `keyStorePassword` may never have loaded from
config at all. Verify rather than port the bug.

### Stage 3 — the insights write path off Jackson — **DONE 2026-07-31**

**The six Slumber-compat blockers were not blockers for this direction.** Probed before committing to
the work: `getSlumberer` dispatches on the **runtime** class, so `AppConfigCollector.Data(info: Any,
config: Any)` slumbers fine — the declared `Any` only obstructs *awaking*, and insights reads with
kotlinx, never with Slumber. Every collector `Data` slumbered on the first try, and `HttpMethod` /
`HttpStatusCode` produce byte-identical shapes to Jackson's.

The only wire change is `ts`: Jackson wrote a `LocalDateTime` as `[2026,7,31,…]`, Slumber writes
`{ts, timezone, human}`. Harmless — `InsightsDataLoader` never reads `ts`; `recordedAt` comes from the
depot file.

- `InsightsMapper` (Jackson) → `InsightsCodec : Codec(SlumberConfig.default)`. A distinct type rather
  than a bare `Codec` binding, for the reason `InsightsMapper` was a distinct `ObjectMapper` subclass:
  registering `Codec` would claim a very general type in every app's kontainer for one module's benefit.
- Record text is now kotlinx over `JsonUtil.toJsonElement()`, as with the printers.
- **`ConfigRedaction` and its spec are DELETED.** Slumber honours `Redacted<T>`, so the signing key, the
  CSRF secret and the database password leave as `***redacted***` from the type, not from a name list.

**Mutation — removing the `RedactedSlumberer` branch fails all six recording e2e tests**, with
`IllegalStateException: There is no known way to slumber the type 'Redacted<Any?>'`. So the failure mode
without the codec is *no record written*, not *secret written*: it fails **closed**. That is a second
reason `Redacted` is a plain class — not being a data class, `DataClassSlumberer` does not claim it
either, so there is no silent `{"value": …}` fallback.

#### original scope

`InsightsMapper.kt` and `InsightsFull`'s `convertValue`. Gated on Slumber being able to describe every
collector `Data` class: `AppConfigCollector`'s two `Any` fields, `HttpMethod`, `HttpStatusCode`,
`DebugInfo`, `QueryProfiler.Entry.Impl`, `UserRecord`. These are the plan's six Slumber-compat blockers
(`20260730-frontend-sdk-vue-contributors.md`).

**Delete the interim redaction here** — see below.

### Stage 4 — the rest of Jackson — **PARTIALLY DONE 2026-07-31**

**Done: all three JSON printers.** The maintainer's observation was the key one — a `JsonObject` can be
built from any `Map`, so a Jackson mapper used only to *render* a tree is pure incidental coupling. In
each case Slumber had already produced a plain tree and Jackson only turned it into text, which
`JsonUtil.toJsonElement()` in `ultra/slumber/commonMain` already covers. This was a deletion, not a
rewrite.

| Converted | Was |
|---|---|
| `funktor/core/JsonPrinter.kt` | `writerWithDefaultPrettyPrinter().writeValueAsString(codec.slumber(obj))` |
| `karango/core/aql/printer.kt` | same, inlining a query parameter value |
| `monko/core/lang/printer.kt` | same |

**`JsonPrinter`'s Jackson fallback was also a hole**, not merely coupling. It read
`writeValueAsString(try { codec.slumber(obj) } catch { obj })`, so a value Slumber could not describe
was handed RAW to Jackson, which reflects over anything — bypassing every Slumber codec including
`Redacted`. `AppConfigCliCommand` prints the whole `AppConfig` through this. It now reports an error
instead of dumping the object.

Cost: `karango:core` swapped `jackson-annotations` for `kotlinx-serialization-json`; `monko:core` gained
kotlinx-json and **lost its Jackson dependency entirely**. Eight assertions in
`karango` `OperationBooleanSpec` moved — Jackson renders a single-element array as `[ 1 ]`, kotlinx
multi-line. Everything else in 1898 karango+monko tests matched byte for byte.

**Open question for the maintainer:** those printers inline a value into a query string, and
`prettyPrint = true` was kept to stay faithful to `writerWithDefaultPrettyPrinter`. Multi-line arrays
inside a one-line query read worse than Jackson's `[ 1 ]`. Non-pretty would give `[1]` — arguably better
here, and a smaller diff than what landed. Say if that is preferred.

**Also done, completing stage 4:**

- `funktor/rest/codec/SlumberRestCodec.kt` — the last real user. Jackson did two jobs and a helper
  existed for each: `JsonUtil.toJsonElement()` for writing, `JsonUtil.unwrap()` for reading. Two Jackson
  settings did **not** need carrying over, which is worth recording so nobody re-adds them:
  `FAIL_ON_UNKNOWN_PROPERTIES = false` was moot (the body was read into a `Map<String, Any?>`, which
  accepts anything — tolerance for unknown fields comes from Slumber's awakers), and
  `maxStringLength(50_000_000)`, raised to clear Jackson's 20 MB default, has no kotlinx counterpart
  because kotlinx imposes no such limit. One behaviour improved: `readValue<Map<String, Any?>>` assumed
  the body was a JSON **object** and threw for a top-level array or scalar; unwrapping any element is
  strictly more permissive, and Slumber's awaker still decides what is acceptable.
- `ultra/vault/domain.kt` — three `@get:JsonIgnore` on `collection`, `asRef`, `asStored`. Dead: they are
  computed properties with no `@Slumber.Field`, and `DataClassSlumberer` emits constructor params only,
  so Slumber never touched them; with Jackson gone from insights and rest, nothing serialised them
  reflectively either. Removed with the `jackson-databind` dependency.
- `funktor/core` — four `api(Deps.JavaLibs.Jackson.*)` exports that nothing in the module imported.

## Where Jackson still is, and why that is the end of it

**Zero Jackson in Kotlin source. No first-party module declares it, and the coordinate constants were
deleted from `Deps.kt`** — so re-adding it is now a deliberate act, not a one-line import.

When this section was first written, `com.auth0:java-jwt` was listed here as an irreducible transitive
source, "cannot be removed without replacing the library". It was then replaced: see
`.claude/tasks-archive/2026-07/20260718-jwt-lib-consolidation-options.md` (RESOLVED 2026-07-31) and commit `004a97fe`.
Removing it also took out two ktor artifacts that were carrying Jackson for no first-party user:
`ktor-server-auth-jwt` (funktor authenticates via `bearer()`; the plugin API had zero imports) and
`ktor-serialization-jackson` (`SendgridSender` now posts the JSON `sendgrid-java`'s own `Mail.build()`
produces — verified byte-identical to what the plugin sent).

What is left on the runtime classpath, all of it internal to a third-party library talking to its own
service:

| Source | Note |
|---|---|
| ArangoDB driver → `jackson-serde-json` | The driver's own wire format. Not reachable from our code |
| `sendgrid-java` → `jackson-databind` | Used inside `Mail.build()`. Not reachable from our code |
| AWS SDK → `third-party-jackson-core` | **Shaded/relocated**, so not the same classes |

The distinction is the security framing that started this: Jackson is no longer *our* serializer, so
nothing of ours hands it an object to reflect over, and nothing parses attacker-facing input with it on
the auth path. A Jackson CVE would still land on the classpath through the three rows above, but no code
path of ours reaches it.

**Evicting the sendgrid row** would mean replacing `sendgrid-java`'s helper POJOs with kotlinx DTOs for
`/v3/mail/send`. Small, but it re-specifies an external wire format and that path has no tests — it
needs its own task and contract tests, not a drive-by.

**Full regression: 5513 tests across 16 modules, 0 failures.** Compile sweep clean.

#### remaining (all cleared)

- `funktor/rest/codec/SlumberRestCodec.kt` — the JSON **text** layer beneath Slumber, on every API
  request and response. The largest remaining user; treat most carefully.
- `funktor/insights` — `InsightsMapper`, `InsightsFull` and `AppConfigCollector`. This is **stage 3**
  and is what finally allows `ConfigRedaction` to be deleted.
- `ultra/vault/domain.kt` — one `@JsonIgnore` import, on the derived properties that are NOT secrets.
  Needs a Slumber-side equivalent (or nothing, since Slumber emits constructor params only) before the
  `jackson-databind` dependency can go.
- `funktor/core/build.gradle.kts` exports four Jackson artifacts via `api(...)` although nothing in
  `funktor/core` imports Jackson any more — they exist for downstream modules. Drop them last.

#### original stage 4 scope

- `funktor/rest/codec/SlumberRestCodec.kt` — Jackson is the JSON *text* layer beneath Slumber here, on
  every API request and response. The largest remaining user and the one to treat most carefully.
- `funktor/core/JsonPrinter.kt`, `karango/core/aql/printer.kt`, `monko/core/lang/printer.kt` — debug
  printers, no wire impact.
- Then drop the dependency. Note only `funktor/messaging/build.gradle.kts` declares Jackson directly;
  everywhere else it arrives transitively, so removal must check the transitive source too.

## Interim redaction — DELETED as planned (2026-07-31)

Stage 3 shipped the interim and then removed it, which is what this section existed to guarantee.

While stages 1–3 were in flight, insights was enabled in the demo dev config and writing the signing
key, so `AppConfigCollector` carried **name-based redaction** reusing the `HeaderLogging` sensitive-name
policy — explicitly throwaway, tracked here so its removal could not depend on anyone remembering.

`funktor/insights/.../ConfigRedaction.kt` and `ConfigRedactionSpec.kt` are gone; `Redacted<T>` is the
single mechanism. Confirmed by measurement at the time: while insights still went through Jackson, the
interim was genuinely load-bearing — Jackson wrote `"signingKey": { "value": "<secret>" }`, wrapping
rather than redacting — so it could only be deleted once insights left Jackson. That ordering is the
reason the two were one project rather than two.

## Decided

- `AppConfig.keys` — **`Map<String, Redacted<String>>`** (maintainer, 2026-07-31), rather than redacting
  the map wholesale. Types the secret at the leaf, so `getKeyOrNull` hands back a `Redacted<String>` and
  the protection travels with the value instead of depending on the container it happens to sit in.

## Handed to the codegen agent (2026-07-31)

**`Redacted<T>` must emit `string`, never the shape of `T`.** Recorded in
`.claude/tasks/20260729-ts-sdk-codegen.md` under "INCOMING". The generator resolves generics by descending
into the type argument, and this is the case where the declared argument is deliberately not what travels
on the wire — descending would generate a zod schema expecting an object where `"REDACTED"` arrives, i.e.
a browser-side parse failure on the one field guaranteed never to arrive intact. Same precedence rule as
Slumber: check before any generic-descent logic, not after.


## Review record

| Reviewer | Charter | Outcome |
|---|---|---|
| 1 (opus, high) | Implementation & code style | Findings confirmed and fixed in `5c4bc615`. One claim (L10, "value-based `hashCode` is load-bearing for the karango/monko connection caches") was REFUTED against the code: that argues against *identity* hashing, which breaks the equals/hashCode contract; a *constant* preserves it — equal configs still hash equally, so caches still hit. |
| 2 (opus, high) | Domain expert | H1 confirmed: `AppConfigImpl` was a private plain class that Slumber cannot describe, so any app using `AppConfig.of(...)` with insights at FULL would throw on every request. Fixed by making it a `data class`; `AppConfigSlumberSpec` pins it. |
| 3 (opus, high) | Security | F1 confirmed and it was the important one: deleting `ConfigRedaction` silently UN-redacted `SendgridConfig.apiKey` — a live credential going verbatim into every FULL insights record. The deleted regex matched `.*(key|...)` across the whole tree, so converting only `@JsonIgnore` fields was a net loss of coverage. Fixed by making `apiKey`, `AwsSesConfig.accessKeyId` and `AwsS3Config.accessKeyId` `Redacted<String>`. |

Follow-up fixes landed in `5c4bc615` and `166adaa7` (reading the redaction placeholder back now
throws, on both the Slumber and kotlinx paths — D1). Mutation-tested; the guards were killed
individually, including confirming `ultra:slumber`'s guard independently of `ultra:common`'s.

**Follow-ups created:**
- `.claude/tasks/20260731-redteam-redacted.md` — red-team scenarios (collected, not executed).
- `.claude/tasks/20260731-docs-redacted.md` — DOCS task. `Redacted<T>` is public API with a genuinely
  sharp edge (the deliberately broken round trip), so it needs a docs page and an LLM-mirror entry.
  The type is settled: it survived a three-agent gate and two rounds of fixes, and the only change
  since was moving where it is *used*, not what it is.
