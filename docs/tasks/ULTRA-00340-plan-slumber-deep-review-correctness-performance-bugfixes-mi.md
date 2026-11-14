# ULTRA-00340: Plan: Slumber deep review (correctness, performance, bugfixes, missing features)

> **Status (2026-11-14): DESIGNED.** 
>
> **Part of:** ULTRA-00255 (Epic: Ultra libraries (module scans and defects))

**Plan:** part of ULTRA-00316 (close the open epics), wave 2, track A. Tickets it takes in: ULTRA-00163 (module scan,
74 open rows), ULTRA-00147 (arrays), ULTRA-00154 (intermediate sealed class), ULTRA-00180 (`@Slumber.As` residue).

## Why this exists

The maintainer (2026-11-14): "my idea is to harden everything we have here, starting from the foundational libs";
"slumber would be next in my opinion"; reviews "ideally always with the sharpest models we have available". For
Slumber he named "correctness, performance, bugfixes, missing features" (ULTRA-00316). Slumber reads request bodies
and every stored document; it had a scan in July (ULTRA-00163, never adjudicated to the end) and no whole-module
review under the three charters (`docs/module-reviews.md`).

**This plan is NOT confirmed.** Nothing starts before the maintainer has answered the decisions in section 6 below
and agreed to the steps (CLAUDE.md: re-confirm the plan before implementing).

## The plan in one table

| Step | What | Changes production code? |
|---|---|---|
| A | Characterization: a round-trip matrix of every builtin type, today's behaviour pinned, defects included and marked | no |
| B | Baseline benchmark, as a tagged spec | no |
| C | Deep review, blind, seven parts x three charters at the top reviewer level | no |
| D | Adjudication: C's findings merged with the 74 open rows of ULTRA-00163, each confirmed by a failing test, refuted, or put to the maintainer | no (failing specs only) |
| E | The maintainer's decisions | no |
| F | Fix batches by risk, F1 to F8, each through the review gate | yes |
| G | The open feature tickets, inside F4 and F5 | yes |
| H | A red-team ticket of attack scenarios | no |
| I | Docs | no |

Rules added by the coordinator from today's incident (`docs/tasks/_incidents.md`, 2026-11-14): a small batch that
touches production code gets a delta review before the merge, and a suite with threads is run under load before it
counts as green.

## Briefing (read-only groundwork, 2026-11-14)

Collected by a planning agent by reading; nothing was run, so nothing below is probed. Every "open" state comes from
reading the source and the tickets on that day. Section numbers are the agent's.

### 1. Module map

**Targets.** `ultra/slumber/build.gradle.kts` declares Kotlin multiplatform with `js` (browser) and `jvm`. In practice the engine is JVM only:

- `commonMain` holds 5 files (312 lines): `Polymorphic.kt`, `AdditionalSerialName.kt`, `JsonUtil.kt`, `slumber_helpers.kt` (kotlinx-serialization helpers) and an index file.
- `jsMain` is empty.
- All codecs live in `jvmMain`, which depends on `kotlin-reflect`, `classindex` (via kapt), `ultra:cache`, `ultra:datetime` and `ultra:reflection`.
- The `@Slumber.Field` and `@Slumber.As` annotations live in `ultra/common/src/commonMain/kotlin/slumber/Slumber.kt` since 4902d6bf.
- `ultra/slumber-test-classes` is a JVM module with one fixture (`DataClassWithPrivateCtor.kt`, 12 lines).

**Size.** 67 main files with 4,049 lines; 71 test files with 7,128 lines.

| Package (main) | Files | Lines | Contents |
|---|---|---|---|
| `commonMain` | 5 | 312 | `Polymorphic.Parent/Child`, `AdditionalSerialName`, `JsonUtil`, kotlinx helpers |
| `jvmMain` root | 10 | 709 | `Codec`, `SlumberConfig` (+ `Lookup`), `SlumberModule`, `Awaker`, `Slumberer`, `NonNullAwaker/Slumberer`, `exceptions`, `codec_helpers`, `polymorphic.kt` |
| `builtin` | 1 | 268 | `BuiltInModule` (the dispatch) |
| `builtin/primitive` | 10 | 339 | Boolean, Byte, Char, Double, Float, Int, Long, Number, Short, String |
| `builtin/collections` | 4 | 270 | `CollectionAwaker` (List, Set, arrays), `CollectionSlumberer`, `MapAwaker`, `MapSlumberer` |
| `builtin/objects` | 8 | 581 | `DataClassAwaker/Slumberer` (+ `SlumberCache`, `Cached`), `ValueClassAwaker/Slumberer`, `EnumCodec`, `ObjectInstanceCodec`, `NullCodec`, `any.kt` |
| `builtin/polymorphism` | 4 | 388 | `PolymorphicParentUtil`, `PolymorphicChildUtil`, `PolymorphicAwaker`, parent and child slumberers |
| `builtin/model` | 1 | 72 | `RedactedCodec` |
| `builtin/kotlinx` | 5 | 157 | `JsonElement/Object/Array/Primitive/Null` codecs |
| `builtin/datetime` | 19 | 953 | `common.kt`; `javatime` (8 files: Date, Instant, LocalDate, LocalDateTime, LocalTime, ZonedDateTime, ZoneId); `mp` (7 files: MpInstant, MpLocalDate, MpLocalDateTime, MpLocalTime, MpZonedDateTime, MpTimezone); `kotlinx` (3 files: LocalDate, LocalDateTime) |

**Flow of a value.**

- **Resolution.** `Codec.awake(type, data)` calls `SlumberConfig.getAwaker(type)`. That asks the modules in order (MpDateTime, KotlinxTime, JavaTime, BuiltIn); the first non-null wins and is memoized per `KType` in `SlumberConfig.Lookup` (`SlumberConfig.kt:82-99`).
- **Awake dispatches on the declared type.** `BuiltInModule.getAwaker` (`BuiltInModule.kt:86-166`) wraps every awaker for a non-nullable type in `NonNullAwaker`.
- **Awaker contract.** An awaker returns null for input it cannot read; `NonNullAwaker` turns that into an `AwakerException`.
- **Two passes.** Pass one uses the shared `Context.Fast` with no path. On `AwakerException` the whole graph is awoken again with `Context.Tracking` (path and logs) (`Codec.kt:117-127`).
- **Slumber dispatches on the runtime class.** `Codec.slumber(data, context)` takes `data::class`, builds a type via `createType()` and looks it up (`Codec.kt:149-157`, `:43-47`). The declared field type is not consulted (`DataClassSlumberer.kt:192-208`).
- **Output.** Maps, lists and scalars; data classes become `MutableMap<String, Any?>`.

**Extension points.**

- `SlumberModule` with `prependModules` / `appendModules`.
- `TypedAttributes` on the config, reachable from the contexts. Vault passes `Database` and `EntityCache` this way.
- `withSlumberCache` (a `FastCache` for data-class slumbering).
- `Polymorphic.Parent` / `Polymorphic.Child` companions, `@SerialName`, `@AdditionalSerialName`, `indexedSubClasses()` (ClassIndex).
- `@Slumber.Field`, and `@Slumber.As` (descriptive only).
- Subclassing `Codec`: `KarangoCodec`, `MonkoCodec`, `SlumberRestCodec`, `InsightsCodec`.

**Consumers (main files that import Slumber).**

| Module | Files | Use |
|---|---|---|
| `ultra/vault` | 6 | `VaultSlumberModule`, `StoredAwaker/Slumberer`, `RefCodec`, `Repository` |
| `ultra/datetime` | 6 | `@Slumber.As` on the Mp types |
| `ultra/codegen` | 3 | `TypeWalker` mirrors the dispatch and polymorphism rules |
| `ultra/security` | 2 | annotations |
| `karango/core`, `karango/ksp` | 3 + 1 | `KarangoCodec`, printer, KSP |
| `monko/core`, `monko/ksp` | 4 + 1 | `MonkoCodec`, driver, printer, KSP |
| `funktor/rest` | 2 | `SlumberRestCodec` for request bodies and responses; wiring in `index_jvm.kt:50-87` |
| `funktor/core` | 2 | `AppConfig`, `JsonPrinter` |
| `funktor/cluster` | 4 | background jobs, random data storage, locks |
| `funktor/insights` | 3 | `InsightsCodec` |
| `funktor/auth` | 2 | auth records |
| `funktor/inspect`, `funktor/codegen` | 1 each | |
| `kraft`, `funktor-demo`, `mutator` | 0 | no direct import |

**What Slumber uses from below.**

- `ultra/reflection` (6 files, 583 lines): `ReifiedKType` (ctor params and fields with reified types), `TypeRef`, `kType`, and two annotation helpers.
- `ultra/common`: `TypedAttributes`, `TypedKey`, `Redacted`, the `Slumber` annotations.
- `ultra/cache`: `FastCache`, `Cache`.

### 2. Test map

66 spec files. The last recorded run is 1,254 tests with 0 failures (slumber jvmTest, 2026-11-14, recorded in ULTRA-00211). Specs are Kotest `StringSpec`; the primitive specs are table-driven.

| Area | Spec files | Coverage |
|---|---|---|
| Core (`Codec`, `SlumberConfig`, helpers, NonNull, `@Slumber.As` round trip) | 7 | API surface, module order, attributes |
| Error reporting | 2 + helpers | Path and logs on the awake side |
| Primitives | 10 | Coercion tables per type; NaN and MAX_VALUE appear but the known holes are not pinned |
| Collections | 6 | List, Set, Map, arrays (16 array tests, mutation-checked in ULTRA-00147) |
| Objects | 12 | Data classes (511 lines awake), enums, objects, no-arg ctor, value classes (199 lines), annotated fields |
| Polymorphism | 4 + domain | Awake, slumber, identifiers, round trip (1,008 lines) |
| kotlinx JSON | 4 | Element, object, array, primitive |
| Redacted | 1 | 7 tests |
| Datetime | 19 | Per codec; `DateAwakersMalformedInputSpec` (ULTRA-00294) is the only hostile-input spec |
| commonTest | 1 | `JsonUtilSpec` (8 tests); the only spec that also runs on JS |

**No test today (by reading):**

- Concurrency of `Lookup` and `class2typeCache`.
- `SlumberCache` semantics: the key, the shared live map, attribute loss.
- Star-projected collections.
- Value class implementing a sealed interface.
- Enum constant with a body.
- Nullable parameter with a default.
- Custom discriminator on a root with an intermediate sealed class.
- Duplicate identifiers.
- Instantiable polymorphic parent.
- Map key collisions.
- One-shot iterables.
- Declared `Collection<T>` / `ArrayList<T>`.
- `polymorphic()` and `addChildren()` in `slumber_helpers.kt` (marked `// TODO: Test me`).
- Depth and size limits (there are none).
- Cyclic object graphs on slumber.
- Slumber-side error paths through data classes.

**Round trips.** There is no single matrix. Round trips are spread over five `*RoundTripSpec` files plus per-codec specs. I did not verify that every builtin type has a slumber-then-awake equality test; the matrix in step A closes that.

**Hostile input.** Only dates are tested. Nothing covers depth, size, type confusion across all types, or discriminator abuse.

### 3. State of the known findings

**ULTRA-00163 has 77 table rows; five rows bundle several claims, giving 98 individual claims.**

- 38 rows are recorded as defects, 1 as security (E13), 2 as security-adjacent (E12, F8); the rest are design or smell.
- 32 rows are coordinator-verified (all of A and C, 9 of B, 9 of D).
- 45 rows are agent-reported only (all of E, F, G, H, I, plus B9, B10, D2, D7, D12, D13, D14, D15).
- 3 rows are fixed since: H1 and H3 (ULTRA-00294), H5 (the slumberer now writes the literal "UTC").
- 74 rows are open. 87 `// TODO(scan)` markers are still in the source.
- No design decision listed under the ticket's "Next steps" has been taken.

Severity is as recorded. In the State column, "open (verified)" means verified by the scan coordinator and the `TODO(scan)` marker is still in today's source; "open (agent only)" means reported by a scan agent, never re-verified.

| ID | Severity | State | What it is |
|---|---|---|---|
| A1 | defect | open (verified) | `Lookup` is plain mutable maps, shared by every config made through `copy` / `plusAttributes`, written from request threads (`SlumberConfig.kt:40-43`) |
| A2 | defect | open (verified) | `appendModules` / `prependModules` drop `attributes`, and with them the slumber cache (`SlumberConfig.kt:54,63`) |
| A3 | defect | open (verified) | `plusAttributes` keeps the same `Lookup` although attributes feed resolution; the first resolver decides Cached or Default for all sharers (`:73`). funktor patches around it (`funktor/rest/src/jvmMain/kotlin/index_jvm.kt:62`) |
| A4 | defect | open (verified) | `Codec.class2typeCache` is process-wide, unsynchronized, unbounded (`Codec.kt:30`) |
| A5, A6 | smell, design | open (verified) | `lookup` in data-class equality; stored null re-resolved every call; `error()` raises `IllegalStateException`, not a Slumber exception |
| B1 | defect | open (verified) | `SlumberException : Throwable`, so `catch (e: Exception)` misses it (`exceptions.kt:14`) |
| B2 | defect | open (verified) | `(data != null) implies returnsNotNull()` contracts are unsound (`Codec.kt:94,139`) |
| B3 | defect | open (verified) | `slumber(KClass<T>): T?` types raw data as the source type (`codec_helpers.kt:70`) |
| B4, B5 | design | open (verified) | Slumber side retries on the base exception; a failing first pass re-runs the whole graph and a succeeding second pass hides the first failure |
| B6-B11 | smell, design | open (B9, B10 agent only) | Erased `as? T?`, self-referential bounds, no `cause`, dead parameter, thin slumber context, two cast behaviours |
| C1 | defect | open (verified) | Value class implementing a sealed interface slumbers as a bare scalar with no discriminator (`BuiltInModule.kt:209`); regression of 5d59d61c |
| C2 | defect | open (verified, probe pending) | Enum constant with a body has no slumberer (`BuiltInModule.kt:244`) |
| C7 | defect | open (verified) | `codec.slumber(Any())` recurses to `StackOverflowError` (`any.kt:16`) |
| C3, C5 | design | open (verified) | Awaker matches collections by exact class, slumberer by assignability; a data class that is an `Iterable` slumbers as a bare list |
| C4, C6, C8 | smell | open (verified) | Eager `primaryConstructor`; `Unit` awakes to null; dead `MutableX` operands |
| D1 | defect | open (verified) | Nullable parameter with a default awakes to null, never the default (`DataClassAwaker.kt:100-125`) |
| D2 | defect | open (agent only) | `val x: N?` over a value class with nullable inner awakes null to `N(null)` (`ValueClassAwaker.kt:53`) |
| D3 | defect | open (verified) | `Cached` keyed on `data` equality: context ignored, equal instances with differing `@Slumber.Field` values collide, deep hash per call (`DataClassSlumberer.kt:147`) |
| D4 | defect | open (verified) | Returns a live `MutableMap`; with the cache the same instance is handed out repeatedly (`:194`) |
| D5 | defect | open (verified) | No `stepInto` on slumber: every nested slumber error reports path 'root' (`:192`) |
| D6-D14 | smell, design | open (D7, D12-D14 agent only) | `remove` guard, object singleton accepts any input, key mismatch, wrong diagnosis, per-call `withNullability`, dead `nullables`, dispatch divergence |
| D15 | docs | unverified | KDoc claims `init { require }` runs when a value class is awoken; load-bearing security advice, 2-line probe never run |
| E1 | defect | open (agent only; reproduced in ULTRA-00154) | Awaker reads the discriminator from `cls`, slumberers hop to the parent (`polymorphism/Polymorphic.kt:139` vs `:172-174`) |
| E2 | defect | open (agent only) | Instantiable parent not in the child map recurses to `StackOverflowError` (`PolymorphicParentSlumberer.kt:33`) |
| E3 | defect | open (agent only) | Unregistered child: parent overwrites the correct discriminator with null |
| E4 | defect | open (agent only) | Duplicate identifiers: last wins silently (`Polymorphic.kt:157`) |
| E5 | defect | open (agent only) | `getChildren` returns duplicates (`:245-250`) |
| E6 | defect | open (agent only) | Generic child awoken through the KClass overload loses its type arguments (`PolymorphicAwaker.kt:41`) |
| E7 | defect | open (agent only) | Discriminator overwrites a real property of the same name |
| E10 | defect | open (agent only) | `indexedSubClasses()` swallows every `Throwable` into an empty set (`polymorphic.kt:16-21`) |
| E8, E9, E11, E12 | design | open (agent only) | Bare `@SerialName` makes a child; no diagnostics on any polymorphic failure; seven minor items; the FQCN as identifier discloses layout and breaks on rename |
| E13 | security | collected | ClassIndex-fed `childTypes` widens the awakable set to every indexed subclass on the classpath |
| F1 | defect | open (probed 2026-07-29) | `List<*>`, `Set<*>`, `Array<*>` awake with a `NullPointerException` (`CollectionAwaker.kt:40,46,79`) |
| F2 | defect | open (agent only) | Null-value relaxation for `Map<K, *>` is dead code (`MapAwaker.kt:29-39`) |
| F3 | defect | open (agent only) | Map key collisions collapse silently |
| F4 | defect | open (agent only) | One-shot iterable plus the retry yields an empty collection instead of an error |
| F5-F7, F9 | design, smell | open (agent only) | Convention-only null safety of primitive arrays, arg-less arrays, `Map<*, V>` key fallback is String, five minor items |
| F8 | security-adjacent | open (agent only) | Raw map key goes into the path unbounded and unescaped (`MapAwaker.kt:61`) |
| G1 | defect | open (agent only) | Boolean: `toInt()` truncation, string rules differ from number rules (`boolean.kt:25,28`) |
| G2 | defect | open (agent only) | NaN awakes as 0 for Byte, Short, Int |
| G3 | defect | open (agent only) | Long: 2^63 passes the range check; strings parse via Double, so longs above 2^53 collapse (`long.kt:32,41`) |
| G4 | defect | open (agent only) | Double to Float without range check gives Infinity |
| G5 | smell, design | open (agent only) | Three minor items |
| H1 | defect | fixed (ULTRA-00294) | Unsafe timezone cast in the Mp awakers |
| H2 | defect | open (agent only) | `MpLocalTime` writes millis of day, `java.time.LocalTime` seconds of day; no range validation on the Mp side |
| H3 | defect | fixed (ULTRA-00294) | Unknown zone threw in `ZonedDateTimeCodec` |
| H4 | defect | open (agent only) | `ZoneOffset` field slumbers but cannot awake (`JavaTimeModule.kt:42`) |
| H5 | defect | fixed | kotlinx `LocalDateTime` wrote "Z" |
| H6, H7 | smell, design | open (agent only) | `Date` human field in the JVM zone; family drift; `MpTimezone` accepts any string (left out of ULTRA-00294 on purpose) |
| I1 | defect | open (agent only) | JSON integers beyond Long become a lossy Double (`JsonUtil.kt:34`) |
| I2 | defect | open (agent only) | Unchecked `Map<String, Any?>` cast gives a raw `ClassCastException` (`JsonUtil.kt:62`) |
| I3 | smell, design | open (agent only) | Seven minor items, among them the misleading multiplatform surface |

**Other tickets.**

| Ticket | State | What is left |
|---|---|---|
| ULTRA-00147 arrays | PAUSED; code landed 305d7c45 | The review gate was never run (review record empty). Follow-ups: star projection (= F1) and the dead `MutableX` branches (= C8) |
| ULTRA-00154 intermediate sealed | NOT STARTED | Same defect as E1; reproduced 2026-07-30; fix direction written, not decided |
| ULTRA-00180 `@Slumber.As` residue | NOT STARTED | Five work items and one decision (section 3: a public round-trip check). Sections 1, 2 and 4 are mostly `karango/ksp`, `monko/ksp`, `monko/core`, outside a slumber track |
| ULTRA-00179 docs `@Slumber.As` | NOT STARTED | Waits on the maintainer's look and on ULTRA-00180 section 3 |
| ULTRA-00294 date awakers | DONE 2026-11-06 | Gate passed in one round at `reviewer-high` |
| ULTRA-00159 "Deferred to the slumber pass" | open | `Cached` ignores the context (= D3), `SlumberCache.remove` guard (= D6) |
| ULTRA-00211 ledger row | confirmed, no ticket | Any Number is a `ts`: NaN decodes to 1970, Infinity clamps. Same `is Number -> ts.toLong()` in `MpZonedDateTimeCodec.kt:25`, `MpInstantCodec.kt:20` and kotlinx `LocalDateTimeCodec.kt:36` |
| ULTRA-00151 rest codec config | NOT STARTED | Apps cannot register a `SlumberModule` in funktor-rest (wave 3; depends on A2 and A3) |
| ULTRA-00175 red team Redacted | collected | Includes the `BuiltInModule` branch order |
| ULTRA-00058 red team error disclosure | collected | Has the awaker failure scenario |

**Reflection and common, as far as Slumber's correctness goes.**

- ULTRA-00148 fixed the star projection, nullable `T?`, and the caches.
- ULTRA-00148 left open, as far as I read: #9 (`ctorFields2Types` uses `first`, so a non-property ctor parameter throws `NoSuchElementException`), the inner-class part of #10, #12 (raw `ClassCastException` for a type-parameter classifier), #13 (duplicate meta-annotations, harmless today), #8 (no eviction), #16.
- ULTRA-00143 has nothing open that touches Slumber (`MutableTypedAttributes` reads are fixed).

### 4. Risk areas, ranked

1. **Shared mutable caches under concurrency (A1, A3, A4, D3, D4).**
   - Every request thread writes the same `LinkedHashMap`s.
   - funktor-rest shares one `SlumberCache` sized at 10 % of heap across per-request configs that carry different `Database` and `EntityCache` attributes (`index_jvm.kt:64-87`).
   - If any slumberer's output depends on the context, one request can be served another's shape. I did not check whether Vault's slumberers do.
   - This is the highest risk because it is live in every production server and has no test.
2. **Untrusted input through `SlumberRestCodec`.**
   - No depth or size limit exists anywhere. `JsonUtil.unwrap` and `toJsonElement` recurse (`JsonUtil.kt:26,57`), and every awaker recurses. A `StackOverflowError` is an `Error` and passes every catch. Hypothesis, not probed.
   - A rejected body is awoken twice (`Codec.kt:122-126`).
   - `AwakerException` keeps the whole input and puts up to 8,192 characters of it in the message (`Awaker.kt:61-71`). The REST codec cleans the message but keeps the cause.
   - Coercion is lenient: any non-numeric string awakes to `false` for a Boolean (`boolean.kt:28-33`); any non-null input yields an object singleton (D7).
   - Map keys go into paths raw (F8).
   - The polymorphic allow-list is closed but wider than the declaration (E13).
3. **Silent data loss on stored data.**
   - An optional field that fails to awake silently takes its default (`DataClassAwaker.kt:93-97`); written back, the stored value is gone.
   - Also D1, D2, F3, G2-G4, I1, C1, E1, E3, E4.
   - These affect karango and monko documents, so every fix needs a rule about stored shapes.
4. **Polymorphism (E1-E13).** Round-trip asymmetries and two stack overflows; the whole cluster was never coordinator-verified. `ultra/codegen` mirrors these rules (`TypeWalker`), so a change here changes generated TypeScript.
5. **Dispatch order in `BuiltInModule` (C1-C5).** Load-bearing branch order; awake and slumber dispatch on different things (declared type against runtime class).
6. **Datetime drift (H2, H4, H6, H7, the `ts` Number hole).** Three families read the same wire shape three ways.
7. **Performance.**
   - Every nested value resolves its codec by hashing a `KType` (`SlumberConfig.kt:83,94`).
   - `withNullability(true)` allocates per optional field per call (D10).
   - `Cached` hashes whole object graphs per call (D3).
   - `primaryConstructor` is computed eagerly (C4).
   - The claim "fast pass, no overhead" in the docs has no number behind it. No benchmark exists in the module; only a playground at `src/jvmMain/kotlin/slumber_test.kt`.
8. **API and contracts (B1-B3, B11).** `SlumberException : Throwable` is the costliest to change, as it is a public break.
9. **Platform.** The JS target carries almost nothing; the docs say "runs on the JVM". Low risk, but the build claims more than exists (I3/DS2).
10. **Docs.** `ultra/slumber/README.MD:22-25` shows `Slumber.awake(user)` and `Slumber.unawake(...)`, an API that does not exist.

### 5. Proposed plan

Track ownership per ULTRA-00316: the Slumber track owns `ultra/slumber` and `ultra/slumber-test-classes` only. Consumers are read, never edited; findings there become tickets for their owners.

| Step | Goal | Reads / produces | Checked by | Size |
|---|---|---|---|---|
| **A. Characterization** | Pin today's behaviour before any change | A round-trip matrix spec: every builtin type × (non-null, nullable, default, in List, in Map value, as Map key, in data class, under a sealed parent); expected value is today's output, defects included and marked with their finding ID. A second table: awake of wrong-typed input per type | Mutation checks on the matrix (one mutant per codec family, about 20, as for Kontainer); full slumber jvmTest green | 1 implementer, 2-3 new spec files, no main change; 1 delta review |
| **B. Baseline benchmark** | Numbers before any change | A benchmark spec in `jvmTest`, excluded from the normal run by a tag (no new module, so no `settings.gradle` change). Shapes: flat data class (10 scalars); nested (3 levels, 50-element list); sealed hierarchy list; map-heavy; datetime-heavy; cold resolution (fresh `Lookup`); a rejected input (two-pass cost); with and without `SlumberCache`; 8 threads on one codec. Measures ns/op and allocated bytes/op after warm-up | Run three times; spread under 10 % or the shape is reworked; numbers go into the ticket | 1 implementer, 1 file |
| **C. Deep review, round 1, blind** | The whole module under three charters at the top level | Seven parts, below. Reviewers get no ULTRA-00163 text | Every finding verified by a failing test or a probe before it enters the ledger | 7 parts × 3 charters = 21 reviewer runs |
| **D. Adjudication** | One ledger | Merge C's findings with the 74 open rows of ULTRA-00163; probe the 45 agent-only rows and D15, C2, C4, B7; each row ends as confirmed (with a failing spec), refuted, or decision needed | A failing spec per confirmed defect, committed as pending or expected-failure | 1-2 runs per cluster (9 clusters) |
| **E. Decisions** | The maintainer settles the design questions | A decision list (section 6 and what D adds) | Maintainer's word in the ticket | none |
| **F. Fix batches by risk** | Close the defects | F1 concurrency and caching (A1-A6, D3, D4, D6; files: `SlumberConfig.kt`, `Codec.kt`, `DataClassSlumberer.kt`). F2 hostile input (depth guard, error content, F8, coercion as decided, the `ts` hole). F3 data-class and value-class integrity (D1, D2, D5, D8-D11). F4 polymorphism (E1 with ULTRA-00154, E2-E10, C1). F5 collections (F1-F4 with the follow-ups of ULTRA-00147, C3, C5). F6 primitives (G1-G4). F7 datetime (H2, H4, H6, H7). F8 API (B1-B3, B11) | Each batch through the review gate. Strict tier for F1, F2, F4; standard for the rest. Mutation checks on every new test. Step A's matrix updated where a pinned defect is fixed. Compile sweep and the suites of vault, karango, monko, funktor-rest. Benchmark re-run after F1 and F3 | 8 batches, 2-4 rounds each |
| **G. Features** | The open feature tickets | ULTRA-00147: run its missing gate as part of C part 4, close with F5. ULTRA-00154: inside F4. ULTRA-00180: section 3 is a decision, then a small Slumber change; sections 1, 2, 4 go to the persistence track | The gate of the batch they ride in | inside F4, F5 |
| **H. Red-team ticket** | Collected attack scenarios, not run in this track | One ticket: depth and width bombs, two-pass amplification, cache poisoning and cross-request shapes, discriminator abuse (E13, defaults, aliases), numeric edges, singleton and Boolean confusion, key-into-path forging, input echo in exceptions and logs, placeholder read-back, value-class invariants (D15) | Run per ULTRA-00316 when the area closes | 1 run to write |
| **I. Docs** | Docs match settled code | README fix; error-handling and coercion pages; ULTRA-00179; a page on thread safety and caches. In `docs-site`, by the docs track | Docs review | after F |

**The seven parts of step C** (each small enough to read completely):

| Part | Scope | Files | Lines |
|---|---|---|---|
| 1 | Engine: jvmMain root without `polymorphic.kt`, plus `BuiltInModule` | 10 | ~950 |
| 2 | Objects and Redacted: `builtin/objects`, `builtin/model` | 9 | 653 |
| 3 | Polymorphism: `builtin/polymorphism`, root `polymorphic.kt`, common `Polymorphic.kt`, `AdditionalSerialName.kt` | 7 | ~570 |
| 4 | Collections and primitives | 14 | 609 |
| 5 | Datetime | 19 | 953 |
| 6 | kotlinx interop: `builtin/kotlinx`, `JsonUtil.kt`, `slumber_helpers.kt` | 7 | ~320 |
| 7 | The seam, read-only: `SlumberRestCodec`, `funktor/rest/.../index_jvm.kt`, `KarangoCodec`, `MonkoCodec`, the four files in `ultra/vault/.../slumber/`, `ReifiedKType.kt`. Question: does each caller use Slumber within its contract? | ~10 | |

**Sequence and parallelism.**

- A and B touch only new test files and can run in parallel with each other. C is read-only, but the review-loop rule forbids edits or mutation checks in a tree while a round reads it, so C reads a snapshot (a commit) or starts after A and B are merged.
- The seven parts of C run in parallel.
- D follows C. E follows D.
- Fix batches F1, F3, F4 and F5 all touch `BuiltInModule.kt` or `DataClassSlumberer.kt`, so they are sequential in one track: F1, F2, F3, F4, F5.
- F6 (`builtin/primitive`) and F7 (`builtin/datetime`) share no files with the others and could be a second worktree. That stays within the three-track limit only if another track is idle.
- F8 goes last and alone (public break).
- Wave 2 track B (vault, karango, monko) depends on Slumber's public behaviour. By the plan's rule, Slumber batches that change behaviour merge first and track B rebases.
- H can be written any time after D.

### 6. Decisions for the maintainer

1. **Scope: the whole module or JVM first?** Recommendation: the whole module in one pass. It is 67 files and the common part is 5.
2. **Reflection first or alongside?** Recommendation: alongside. `ReifiedKType.kt` (155 lines) goes into part 7; its open findings #9, #10, #12 are fixed by the track that owns reflection (wave 1 track B) before F3 starts.
3. **May stored shapes change?** Recommendation: no written shape changes without a migration note. Reads may become stricter only where today's result is already wrong (NaN to 0, 2^63). Anything that would make existing documents unreadable (for example validating `MpTimezone`) needs your yes per case. H2 (`MpLocalTime` millis against `LocalTime` seconds) stays as is and gets documented.
4. **May public API break?** Candidates: `SlumberException` to `Exception` (B1), the `slumber(KClass<T>): T?` return type (B3), the contracts (B2), `Lookup` out of the data class (A5). Recommendation: yes, in one batch (F8) with one changelog entry, since 0.x releases already carry breaks.
5. **Coercion strictness for request bodies.** Should "abc" stay `false` for a Boolean, a fractional string truncate to an Int, any input yield an object singleton? Recommendation: keep lenient coercion where it is lossless ("42" to 42); refuse where it invents a value. Decide once, as a table, in step E.
6. **Optional fields that fail to awake take their default silently.** Keep (documented as graceful degradation), or fail? Recommendation: keep for now, add a diagnostic log, and put a strict mode on the feature list. This is the single largest silent-data-loss path.
7. **Nullable parameter with a default and an absent key (D1): null or the default?** Recommendation: the default, as kotlinx.serialization does; an explicit null stays null.
8. **Limits for untrusted input.** A maximum depth in the codec (configurable, default around 200 or 500)? Recommendation: yes for depth. Size belongs to the HTTP layer.
9. **The cache strategy (A1-A4, D3, D4).** Recommendation: concurrent maps with a sentinel for "no codec"; a fresh `Lookup` whenever attributes change; the slumber cache returns immutable or copied maps. Whether the cache key stays equality-based waits for the benchmark numbers.
10. **Which "missing features" do you have in mind?** Candidates I found:
    - star projections;
    - declared `Collection<T>` and concrete collection types;
    - enum constants with bodies;
    - `ByteArray` as base64 (F9/C14);
    - `Duration` and unsigned types (refused today, `BuiltInModule.kt:55-67`);
    - a strict mode;
    - app-registered modules in funktor-rest (ULTRA-00151);
    - a public round-trip check for `@Slumber.As` (ULTRA-00180 section 3).
    
    Recommendation: decide after step D; features come after the fixes.
11. **Gate strictness.** Recommendation: strict for F1, F2 and F4 (shared state, untrusted input, deserialization surface); standard for the rest; step C at the top reviewer level throughout, per your word.
12. **Blind round or guided by ULTRA-00163?** Recommendation: blind, then merged. Overlap between the two is a free measure of what each missed.
13. **Benchmark home.** A tagged spec inside `ultra/slumber` (my recommendation; no build change), or a JMH module (better numbers, but a coordinator change to `settings.gradle`)?
14. **JS target.** Keep the near-empty JS target, or say plainly that Slumber is JVM with a small common part? Recommendation: keep the build, fix the wording.

### 7. What I could not determine by reading

- Whether any of the 74 open rows still reproduce. Nothing was run. The `TODO(scan)` markers are still present, which is the only evidence.
- Whether deep nesting overflows the stack, and at what depth, in `JsonUtil.unwrap`, in kotlinx's parser, or in the awakers.
- Whether any Vault slumberer's output depends on the context, which decides whether D3 is a cross-request leak or only a correctness smell.
- Whether `init { require }` runs when a value class is awoken (D15).
- Whether the recorded 75 specs (July) and my count of 66 spec files differ only by counting method. Today's test count was not re-run.
- The actual performance profile. The hot spots in section 4 are from reading, not from a profiler.
- Whether production projects outside this repo use `Polymorphic.Parent` companions, custom modules or `withSlumberCache`. Inside the repo there are no production `Polymorphic.Parent` companions (ULTRA-00154), and only funktor-rest installs a slumber cache.
- The model and effort of the ULTRA-00163 scan; the ticket does not record them, and its verbatim agent reports were in a session scratchpad that is not committed.
- The remaining "Open" list of ULTRA-00148 in full; I read the findings tables and the done table, not every later section.

## History

- 2026-11-14: created on the maintainer's word ("plan the deep review of the slumber module next"); briefing by a
  read-only planning agent; not yet confirmed.
