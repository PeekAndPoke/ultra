# `@Slumber.As` — declare a custom-coded type's wire shape once

**Status:** IDEA — maintainer's, 2026-07-31. Descriptive-only is settled (§4); §5 still open.
**Plan:** none yet. Touches `ultra/slumber`, `karango`, `monko`, `ultra/codegen`.
**Security-critical:** no.

## 1. The idea

```kotlin
@Slumber.As(MpDateTimeRawData::class)
data class MpInstant(...)

data class MpDateTimeRawData(val ts: Long, val timezone: String, val human: String)
```

A type whose JSON shape comes from a custom codec **declares** that shape, instead of every consumer
re-deriving or hand-mirroring it. Fits the existing annotation nest — `Slumber` has `@Target()` empty
and holds nested annotations (`Slumber.Field`), so `Slumber.As` is a natural sibling.

## 2. Why it is worth doing: the shape is currently mirrored by hand, N times

The wire shape lives in imperative codec code —
`MpInstantSlumberer.slumber` returns `toMap(ts = …, timezone = utc, human = …)`
(`ultra/slumber/src/jvmMain/kotlin/builtin/datetime/mp/MpInstantCodec.kt:26-40`) — so nothing can read
it. Every consumer therefore restates it:

| Consumer | How it mirrors the shape today |
|---|---|
| **karango** | five hand-written path helpers, `AqlPathExpr<MpInstant>.ts` and friends (`karango/core/src/main/kotlin/aql/base_slumber.kt:23-40`) |
| **monko** | the same five, again (`monko/core/src/main/kotlin/lang/base_slumber.kt:24-37`) |
| **ultra/codegen** | `MpDateTimeTsContributor` claims six types, a hand-written `runtime/datetime.ts` spells the shape out, and `MpDateTimeFieldParitySpec` exists purely to stop the two drifting |

**The duplication is already visibly incomplete**, which is the tell. Both karango and monko expose
only `.ts` — there is no helper for `timezone` or `human`, though both are on the wire. Nobody decided
that; it is what happens when a shape is copied by hand three times.

And the codegen parity spec says outright why it must exist: *"a claimed type is never declared by the
walker … a wrong claim produces confidently wrong TypeScript and the generator stays silent"*. Its
KDoc records that the first draft got two of six types wrong. **A declaration gives all three
consumers one thing to read instead of three things to keep in step** — see §4 for what that does and
does not remove.

## 3. What it would generate

- **karango / monko**: typed path accessors for every field, not just the one someone needed —
  `AqlPathExpr<MpInstant>.ts`, `.timezone`, `.human`. Deletes the hand-written blocks in both.
- **ultra/codegen**: the TS type and zod schema derive from `MpDateTimeRawData` like any other data
  class. `MpDateTimeTsContributor`'s claims become derivable and much of `runtime/datetime.ts` stops
  being hand-written. `MpDateTimeFieldParitySpec` is superseded rather than deleted — the check it
  performs moves next to the annotation (§4), where one generic test covers every annotated type.
- Possibly docs, and anything else that needs to know what a value looks like at rest.

## 4. SETTLED: the annotation is PURELY DESCRIPTIVE (maintainer, 2026-07-31)

It declares the shape. It does **not** generate the codec, and the codec is not derived from it.
`ultra/slumber` is battle-tested and stays as it is.

**The consequence worth naming: verification does not disappear, it MOVES — and that is the win.**

A descriptive annotation can be wrong, exactly as a `TsTypeClaim` can be wrong today. So something
must still assert "the codec really produces what `@Slumber.As` says". But instead of **each consumer
needing its own guard** — which is why `MpDateTimeFieldParitySpec` exists in `ultra/codegen`, and why
karango and monko have no guard at all and simply hope — there is **one** guard, next to the
annotation, checking one thing:

> slumber a real value of the annotated type, and assert its shape matches the declared class.

That test is generic and can be written once for every annotated type, rather than per consumer per
type. Consumers downstream then trust the annotation without re-verifying, because it is verified at
the source.

So the honest framing of the benefit is not "removes the need to check". It is: **one check instead of
N, in the place that can actually perform it.**

## 5. The constraint that decides the design

**Not every custom-coded type slumbers to an object.** Of the six ultra/datetime types:

| Type | Wire shape |
|---|---|
| `MpInstant`, `MpZonedDateTime`, `MpLocalDateTime`, `MpLocalDate` | `{ts, timezone, human}` — an object |
| `MpLocalTime` | a bare `Long` (`MpLocalTimeCodec.kt:34` returns `data.inWholeMilliSeconds()`) |
| `MpTimezone` | a bare `String` (`MpTimezoneCodec.kt:32` returns `data.id`) |

So `As(SomeDataClass::class)` covers **four of six**. The two scalars are exactly the ones the codegen
parity spec records as having been got wrong first time — i.e. the cases most worth declaring.

The annotation must therefore express scalars too, or those types keep a hand-written claim and the
idea only half-lands. Options: allow `As(Long::class)` / `As(String::class)`; or a separate
`@Slumber.AsScalar`; or lean on the value-class precedent, since `ValueClassSlumberer` already emits
the bare underlying value and nothing has to declare that.

## 6. Open questions

- **Where does the annotation live?** `ultra/slumber`'s `commonMain` is the natural home, but karango
  and monko KSP would then depend on it — check they already do, transitively or otherwise.
- **Does it apply to types you do not own?** `java.time.*` and `kotlinx.datetime.*` are custom-coded
  too and cannot be annotated. A registry-style escape hatch is needed regardless, which is what
  `TsTypeClaims` already is on the codegen side. Do not let the annotation's existence delete the
  escape hatch.
- **Generic custom-coded types** — does anything need `As` with type arguments?
- **KSP vs reflection.** karango/monko read it at build time via KSP; ultra/codegen reads it at run
  time via reflection. The annotation must be visible to both (`@Retention(RUNTIME)`).
- **Migration order.** codegen can consume it first and cheaply (its claims are already a registry
  with one call site per type); karango/monko involve KSP work and deleting public inline extensions,
  which is a source-compatible change only if the generated names match exactly.

## 7. What I would check first

- [ ] Confirm no annotation like this exists already under another name.
- [ ] Confirm karango/monko KSP can see a `commonMain` annotation from `ultra/slumber`.
- [ ] Enumerate every custom-coded type across the codebase, not just ultra/datetime —
      `SlumberConfig.default`'s module list is the honest starting point, plus `JavaTimeModule` and
      the kotlinx-json codecs. That count decides whether this pays for itself.
- [ ] Decide §5 (how scalars are expressed) with the maintainer. §4 is settled: descriptive only.
- [ ] Write the generic parity check FIRST, before any consumer reads the annotation. A descriptive
      annotation that nothing verifies is a `TsTypeClaim` with better syntax — same failure mode,
      wider blast radius, because now three code generators trust it instead of one.
