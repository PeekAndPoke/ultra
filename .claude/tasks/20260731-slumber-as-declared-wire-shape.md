# `@Slumber.As` — declare a custom-coded type's wire shape once

**Status:** IDEA — maintainer's, 2026-07-31. §4 and §5 settled; ready to be specced.
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

**SETTLED (maintainer, 2026-07-31): ONE annotation form, `As(KClass)`. Scalars use it too.**

```kotlin
@Slumber.As(MpDateTimeRawData::class)  data class MpInstant(...)
@Slumber.As(Long::class)               data class MpLocalTime(...)
@Slumber.As(String::class)             data class MpTimezone(...)
```

`AsLong` / `AsString` shorthands were considered and **rejected**, for consumers rather than for
authors:

- A consumer checks for ONE annotation and gets a `KClass`, then proceeds with its own machinery.
  That is exactly the shape karango KSP, monko KSP and `ultra/codegen` already want.
- With a family of annotations every consumer must look for all of them, and every new shorthand is a
  change in three code generators.
- **It makes an invalid state unrepresentable.** `@AsLong` and `@AsString` on the same type is a
  conflict each consumer would have to detect and report; with one annotation there is nothing to
  detect.

### `As` is a usable name — verified, not assumed

Kotlin keywords are case-sensitive: `as` is hard-reserved, `As` is an ordinary identifier. Confirmed
by compiling and running a probe (2026-07-31): a nested `annotation class As(val shape: KClass<*>)`
declares, applies at a use site, and reads back through runtime reflection — which is how
`ultra/codegen` would consume it — with no backticks needed anywhere. Java interop is unaffected;
`as` is not a Java keyword.

## 6. Open questions

- ~~**Where does the annotation live?**~~ **SETTLED (maintainer, 2026-07-31): move the whole `Slumber`
  annotation nest to `ultra:common`'s `commonMain`.**

  `ultra/slumber` looked like the natural home until `Redacted<T>` needed the annotation and forced the
  question (`.claude/tasks-archive/2026-07/20260731-redacted-and-jackson-removal.md`). The conflict: a type must be able
  to CARRY `@Slumber.As`, and `ultra:slumber` must be able to SEE that type to register its codec. With
  the annotation in `ultra:slumber`, any annotated type sits downstream of slumber — and
  `slumber → thatModule` is then a cycle, because `slumber` already declares
  `api(project(":ultra:common"))`. So you get the declaration-site annotation OR the built-in codec, not
  both.

  `ultra:common` dissolves it, and costs nothing:

  | | outcome |
  |---|---|
  | New dependency edges | **none** — `slumber`, `vault`, `security`, `karango`, `monko`, `model` and `codegen` all already depend on `ultra:common` |
  | Cycle | none — `ultra:common` has no project dependencies at all |
  | karango/monko KSP visibility | yes, via a dependency they already have (answers the check in §7) |

  The nest is **annotations only** — `Slumber.Field` and nothing else, no machinery, no slumber
  dependency — so nothing moves with it. Its two external users, `ultra/vault/src/commonMain/kotlin/
  annotations.kt` and `ultra/security/src/commonMain/kotlin/user/UserRecord.kt`, both already depend on
  `ultra:common`.

  **Package: `io.peekandpoke.ultra.common.slumber`** (maintainer, 2026-07-31), i.e.
  `ultra/common/src/commonMain/kotlin/slumber/Slumber.kt`. That name settles the one oddity the move
  would otherwise leave — annotations called `Slumber.*` sitting outside `ultra:slumber` — by saying what
  they are: the slumber-facing contract, held low enough in the tree for every consumer to read. A
  descriptive annotation consumed by slumber, two KSP processors and a code generator is a **contract**,
  not an implementation.

  Import sites change from `io.peekandpoke.ultra.slumber.Slumber` to
  `io.peekandpoke.ultra.common.slumber.Slumber` — `ultra/vault`, `ultra/security`, four files inside
  `ultra/slumber` itself, and three codegen test fixtures.

- ~~**Generic custom-coded types** — does anything need `As` with type arguments?~~ **Answered by the
  first real consumer: no.** `Redacted<T>` slumbers to a `String` regardless of `T` — that IS the type's
  purpose — so it wants exactly `@Slumber.As(String::class)` and no type-argument machinery. A generic
  type whose wire shape is independent of its argument is evidence FOR the settled simple form (§5),
  not against it.

- **Does it apply to types you do not own?** `java.time.*` and `kotlinx.datetime.*` are custom-coded
  too and cannot be annotated. **DEFERRED (maintainer, 2026-08-01) — not solved in this task.** The
  registry-style escape hatch therefore stays, and `TsTypeClaims` is already exactly that on the
  codegen side. The constraint this puts on the implementation: **no consumer may treat the
  annotation as the only source of a wire shape.** Read it where present, fall back to the registry
  where absent. Revisit once the annotation carries real weight.
- **KSP vs reflection.** karango/monko read it at build time via KSP; ultra/codegen reads it at run
  time via reflection. The annotation must be visible to both (`@Retention(RUNTIME)`).
- **Migration order.** codegen can consume it first and cheaply (its claims are already a registry
  with one call site per type); karango/monko involve KSP work and deleting public inline extensions,
  which is a source-compatible change only if the generated names match exactly.

## 7. What I would check first

- [ ] Confirm no annotation like this exists already under another name.
- [x] ~~Confirm karango/monko KSP can see a `commonMain` annotation from `ultra/slumber`.~~ Moot — the
      nest moves to `ultra:common`, which both already depend on. See §6.
- [x] ~~Enumerate every custom-coded type across the codebase~~ **Done 2026-08-01 — see §10.**
      7 annotatable, 9 third-party, 5 not declarable at all.
- [ ] Confirm every currently-claimed type can be expressed by `As(KClass)` — §5.
- [ ] The generic check is OPTIONAL, not a precondition — maintainer, 2026-07-31: the annotation
      ships with the type, so keeping it correct is the type author's obligation, the same way the
      codec is. Worth adding as cheap insurance because a wrong declaration fails silently and
      downstream. **If it is written, it must be the FULL round trip — see §8.**

## 8. How to check a declaration, if you check it at all

Maintainer's proposal (2026-07-31): slumber a real value of `T`, then awake the result **into the
declared `As` class**. If that fails, something is off.

The idea is right — it uses Slumber itself as the checker instead of hand-comparing field lists, which
is what makes it generic. But **awaking alone is not sufficient**. Measured against the real
`MpInstant` codec (2026-07-31):

| declared shape | `awake` succeeds | re-slumber `==` raw |
|---|---|---|
| **too small** — `(ts)` only | **YES** — passes wrongly | no |
| exact — `(ts, timezone, human)` | yes | **yes** |
| too big — an extra non-null field | no | — |
| **wrong type** — `ts: String` | **YES** — passes wrongly | no |

Awaking catches only the too-big case. A declaration that omits a field awakes fine, because extra
keys in the data are ignored; and one that types `ts` as `String` awakes fine too, because the value
converts.

**The full round trip catches all three**: slumber `T` → awake into the declared class → slumber that
→ compare with the first result.

```
raw1 = slumber(T, value)
raw2 = slumber(As, awake(As, raw1))
assert raw1 == raw2
```

One subtlety that makes it work: the comparison must be type-sensitive. In the wrong-type row above
both maps PRINT identically — `{ts=1785492930000, …}` — and differ only because `"1785492930000"` is
not `1785492930000L`. Map equality gives that for free; comparing rendered strings would not.

## 9. Removing the hand-written `.ts` helpers — the call sites are real

**SETTLED (maintainer, 2026-08-01): migrate the call sites, do not leave the helpers behind as
deprecated aliases.** Both forms in use get replaced by whatever KSP generates.

Two forms exist, and a grep for one misses the other:

1. **The helper** — `AqlPathExpr<MpInstant>.ts` / `MongoPathExpr<MpInstant>.ts`
   (`karango/core/src/main/kotlin/aql/base_slumber.kt:23-37`,
   `monko/core/src/main/kotlin/lang/base_slumber.kt:23-37`). Four overloads each, distinguished by
   `@JvmName`, and there is no `.timezone` or `.human` — see §2.
   Roughly 20 uses across `funktor/auth`, `funktor/messaging` and `funktor/cluster`, each with an
   explicit `import io.peekandpoke.karango.aql.ts` / `io.peekandpoke.monko.lang.ts` — those imports
   are the reliable way to enumerate them, not the `.ts` text itself, which also matches unrelated
   receivers (`funktor/insights/reference/gui/InsightsGuiTemplate.kt:141`,
   `funktor/inspect/src/jsMain/kotlin/cluster/devtools/DevtoolsRequestHistoryPage.kt:54`).
2. **The raw property call**, bypassing the helper entirely —
   `funktor/cluster/src/jvmMain/kotlin/backgroundjobs/karango/KarangoBackgroundJobsArchiveRepo.kt:36,46`
   uses `archivedAt.property<Long>("ts")` while its Monko twin
   (`MonkoBackgroundJobsArchiveRepo.kt:38,43`) uses `.ts`. Same repo, same field, two spellings —
   which is the duplication argument in §2 showing up in application code.

**Method: rename, then compile.** A grep for `.ts` cannot find receiver-less or inlined uses, and
`property<Long>("ts")` is a string literal no refactoring tool tracks. Renaming the helper and letting
the compiler enumerate the callers is the only enumeration that is complete — `CLAUDE.md` records this
exact trap being hit twice. The raw call sites then come from a literal search for
`property<Long>("ts")`, which is exhaustive because it is one string.

Do the migration in that order: generate → rename old helper → fix every compile error → delete.
Never delete first; a missing extension and a wrong extension look identical at the call site.

## 10. Enumeration result (2026-08-01) — 7 annotatable, and one asymmetric

Every custom-coded type reachable from `SlumberConfig.default`
(`ultra/slumber/src/jvmMain/kotlin/SlumberConfig.kt:26-33`), i.e. the four modules it lists.

| Module | Types | Wire shape | Annotatable |
|---|---|---|---|
| `MpDateTimeModule` | `MpInstant`, `MpZonedDateTime`, `MpLocalDateTime`, `MpLocalDate` | `{ts: Long, timezone: String, human: String}` — all four via one shared `toMap` (`builtin/datetime/common.kt:24,31`) | **yes** |
| `MpDateTimeModule` | `MpLocalTime` | `Long` (`MpLocalTimeCodec.kt:34`) | **yes** |
| `MpDateTimeModule` | `MpTimezone` | `String` (`MpTimezoneCodec.kt:32`) | **yes** |
| `BuiltInModule` | `Redacted<T>` | `String` — always `Redacted.PLACEHOLDER` (`RedactedCodec.kt:53`) | **yes**, with a caveat below |
| `JavaTimeModule` | `java.util.Date`, `java.time.{LocalDate, LocalDateTime, LocalTime, Instant, ZonedDateTime, ZoneId}` | various | no — not ours |
| `KotlinxTimeModule` | `kotlinx.datetime.{LocalDate, LocalDateTime}` | various | no — not ours |
| `BuiltInModule` | `JsonElement`, `JsonObject`, `JsonArray`, `JsonPrimitive`, `JsonNull` | passthrough, arbitrary JSON | no — **and not declarable at all**, there is no fixed shape to state |

**7 annotatable, 9 third-party-but-declarable, 5 with no declarable shape.** Value-class types are not
in scope: `ValueClassSlumberer` derives the shape from the backing field, so nothing is hand-mirrored.

**Does it pay for itself?** Yes, but the count that matters is not 7 — it is 7 types × 3 consumers
(karango, monko, `ultra:codegen`) = **21 hand-written mirrorings** the annotation replaces with one
declaration each, in a repo where the parity spec's own KDoc records the first draft getting two of six
claims wrong. The third-party rows are what §6 defers; they are also why the registry escape hatch
survives.

### `Redacted<T>` is asymmetric — and it is the reason to say "slumber direction" out loud

`RedactedSlumberer` emits the placeholder `String` whatever `T` is, but `RedactedAwaker` takes **`T`'s
own raw shape** — deliberately, so a config file can write `secret = "abc"` rather than nesting
(`RedactedCodec.kt:9-20`). So `@Slumber.As(String::class)` is true of its output and says nothing about
its input.

That is not a defect in the annotation, but it must be stated in its KDoc, because a consumer
generating a *write* model from it would be wrong. karango and monko generate query paths over stored
data — the slumber direction — so they are unaffected. Recorded on `Slumber.As`
(`ultra/common/src/commonMain/kotlin/slumber/Slumber.kt`).

Annotating `Redacted` is NOT part of this task — it is `ultra:common`'s own type and the round-trip
check needs a decision about the fail-loud placeholder path first. The six `Mp*` types are the scope.

## 11. DECIDED (maintainer, 2026-08-01): annotation only. Do NOT build the alternatives.

Two alternatives were raised mid-implementation and **rejected**: a module-supplied annotation map
(`SlumberModule.getCustomAnnotations()`), and asking the codec — `Codec.getSlumberer(T).getWireType()`,
defaulting to the type the slumberer implements. Stick to §1–§5.

Kept because it is a real argument someone will re-derive: `getWireType()` is the better **runtime**
mechanism on three counts — it sits in the same class as the `slumber()` that builds the map; it erases
the owned/not-owned split, so `java.time.Instant` is handled like `MpInstant` and §6's escape hatch
disappears; and it composes with `prependModules`, which the annotation cannot. A user who prepends a
module to replace a codec has no way to change an annotation on a class they do not own, so the
declaration silently lies for that application.

It was rejected because it delivers **nothing to KSP**. karango and monko generate at build time from
symbols; `getSlumberer()` is a runtime call needing a configured `SlumberConfig`. Deleting the `.ts`
helpers (§9) is a KSP deliverable, so the runtime-only mechanism cannot carry this task.

There is a loophole, and it is a trap worth naming: `karango/ksp` has `:ultra:slumber` on its own
classpath, so the processor *could* `Class.forName` a type and call `getWireType()` reflectively. That
works for `MpInstant`, which the processor depends on, and silently does nothing for a user's own
custom-coded type, which it does not. A mechanism that works for our types and not the user's is worse
than one that works for neither.

## 12. PRECONDITION, still unverified: can KSP read the annotation at all?

Independent of §11 — this is a precondition of the plan itself, not of the rejected alternatives.

**Can a KSP processor read a RUNTIME-retained annotation off a CLASSPATH symbol**, i.e. one not in the
compilation being processed? `MpInstant` is a classpath type for every consumer of karango. If the
answer is no, §3's karango/monko half needs a different delivery route (a resource generated by
`ultra:slumber` at build time) and §9's deletions wait on it.

`resolver.getClassDeclarationByName(...)` returning usable `.annotations` is the thing to probe.
NOT `getSymbolsWithAnnotation`, which only covers the current round's sources — that distinction is the
whole question. NOT YET RUN.
