# Let apps configure the RestCodec via FunktorRestBuilder

**Status:** TODO
**Plan:** none — raised from `.claude/tasks/20260729-ts-sdk-codegen.md` (Phase 2 prerequisite)
**Security-critical:** no (serialization wiring; no auth or trust boundary change)

## Why

`RestCodec` is the service that serializes and deserializes all API models
(`funktor/rest/src/jvmMain/kotlin/codec/RestCodec.kt`); `apiRespond` goes through it
(`funktor/rest/src/jvmMain/kotlin/respond.kt:32`). Its configuration is currently **hardcoded**:

```kotlin
// funktor/rest/src/jvmMain/kotlin/index_jvm.kt:48
val codecConfig = SlumberConfig.default.prependModules(VaultSlumberModule)
```

That is a local inside the `Funktor_Rest` module lambda, and `FunktorRestBuilder` exposes only
`jwt()`. So **an application cannot register its own `SlumberModule`** — a type needing a custom codec
cannot be wired into the REST layer at all. The only workaround is overriding the whole `RestCodec`
registration, which discards the framework defaults (`VaultSlumberModule`, the slumber cache, the
database/entity-cache attributes) and is easy to get wrong.

Nothing hits this today — zero `SlumberModule` registrations exist in `funktor-demo` — so it is a
latent gap rather than active pain. It became visible while building the TypeScript SDK generator,
which asks the live `SlumberConfig` what codecs exist and therefore surfaces the fact that an app
cannot contribute any.

## Spec

The default must stay exactly what it is today for apps that configure nothing.

- [ ] `FunktorRestBuilder` gains a way to contribute serialization modules, e.g.
      `slumberModules(vararg module: SlumberModule)`, applied on top of the framework default rather
      than replacing it.
- [ ] Decide precedence deliberately and document it on the method: app modules should almost
      certainly be **prepended** (they win over built-ins, matching how `VaultSlumberModule` is
      prepended over `SlumberConfig.default`), so an app can override a framework codec for its own
      type. State it in the KDoc either way — silent precedence is the trap here.
- [ ] Apps that configure nothing get byte-identical behaviour to today. Pin with a test comparing
      the resulting `SlumberConfig.modules` against the current default.
- [ ] Consider a fuller escape hatch alongside it — e.g. `restCodec { config -> ... }` taking the
      framework-built config and returning a modified one — for apps that need more than appending
      modules (cache tuning, attributes). Only if it costs little; the module hook is the actual need.
- [ ] `Funktor_Rest` optionally registers `instance(codecConfig)` so `SlumberConfig` is injectable
      directly. **Cosmetic only** — `RestCodec` is a `Codec` and `Codec.config` is public
      (`ultra/slumber/src/jvmMain/kotlin/Codec.kt:23`), so the config is already reachable by
      downcasting. Do it for readability, not because anything is blocked.

## Test evidence

- [ ] Default path unchanged: an app configuring nothing produces the same module list as today
- [ ] An app-registered `SlumberModule` is consulted by `RestCodec` for both serialize and deserialize
- [ ] Precedence: an app module claiming a type already handled by a built-in wins (or loses —
      whichever was decided), asserted explicitly rather than left to chance
- [ ] End-to-end through the funktor testing harness: an endpoint returning an app type with a custom
      codec serializes through the app's codec
- [ ] Full test command(s) run + green: `...`

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | | |
| 2. Domain expert | | |
| 3. Security | | |

Fixes applied: ...

## Follow-ups

- [ ] Once this lands, `funktor/codegen`'s `RestApiTsContributor` can honestly claim to handle
      app-specific types: the generator injects `RestCodec`, reads `.config`, and its codec-parity
      check then sees the app's own codecs and demands claims for them. Update
      `20260729-ts-sdk-codegen.md`'s Phase 2 prerequisite list.
