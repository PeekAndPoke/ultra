# Slumber: module scan — findings & adjudication

**Status:** SCAN COMPLETE, adjudication IN PROGRESS (2026-07-31). KDoc landed, tests green
(75 specs, 1232 tests, 0 failures). No fixes applied yet.
**Plan:** module-by-module review (method model: `20260730-cache-scan-findings.md`, archived)
**Security-critical:** partially — polymorphic awaking is a deserialization surface. Security
findings are COLLECTED (→ red-team task), never executed.

## How this was produced

8 read-only scan agents (core dispatch, codec core, objects, collections, polymorphism, primitives,
datetime, kotlinx+common), each adding concise KDoc directly and REPORTING findings without fixing.
Every defective site carries a `// TODO(scan): ...` marker in the source instead of KDoc.
Verbatim agent reports: `scratchpad/slumber-reports/A*.md` (session scratchpad, not committed).
The whole diff was checked: only KDoc/comments plus two import fixes (`java.util.*` → `Date` in
DateCodec.kt; `NonNullAwaker` import for a KDoc link). Suite re-run afterwards: 1232/0/0.

**Verification legend:** ✔ = coordinator re-verified against current code. (agent) = reported by
agent(s), coordinator verification pending. Findings found independently by 2+ agents note both IDs.

## The big picture

Slumber's engine (dispatch, contexts, error paths) is battle-tested and holds up. The defects
cluster in exactly the places flagged as recent rework, plus two systemic issues that predate it:

1. **Concurrency/caching**: every cache in the module (`SlumberConfig.Lookup`,
   `Codec.class2typeCache`) is a plain `LinkedHashMap` written from request threads, and config
   derivation (`copy`/`plusAttributes`) SHARES the lookup while attributes feed resolution.
2. **The new SlumberCache layer** (commit 134b0ae7): keyed on `data` equality ignoring context,
   hands out live mutable maps, and first-resolver-wins decides whether caching applies at all.
3. **Recent dispatch additions** (value classes 5d59d61c) shadow the polymorphic-child branch.
4. **Cross-family drift** in datetime codecs (wire-format and validation inconsistencies).
5. **Numeric coercion holes** in the primitive codecs (NaN, 2^63, String→Long via Double).

## A. Config & caching

| # | Where | Claim | Sev | Status |
|---|---|---|---|---|
| A1 | `SlumberConfig.kt:40-43,83,94` | `Lookup` = plain mutable maps; ONE instance shared by every config derived via `copy`/`plusAttributes`; `getOrPut` raced by request threads (funktor/karango/monko all derive per-request configs) | defect | ✔ (SLB-1, SL-X2, A4) |
| A2 | `SlumberConfig.kt:54,63` | `appendModules`/`prependModules` construct without `attributes` → silently reset to empty; `withSlumberCache{}.prependModules(...)` drops the cache. Latent: all repo callers prepend first | defect | ✔ (SLB-2, O-10, SL-X1) |
| A3 | `SlumberConfig.kt:73` | `plusAttributes` keeps the SAME Lookup while `attributes` are an input to resolution → whichever config resolves a type FIRST decides (e.g. Cached vs Default slumberer) for all sharers. funktor hand-patches one instance (`funktor/rest/index_jvm.kt:62`) | defect | ✔ (SLB-3, O-7) |
| A4 | `Codec.kt:30` | `class2typeCache`: process-wide, unsynchronized, unbounded, pins ClassLoaders; hot per-value path | defect | ✔ (SL-C1, P19) |
| A5 | `SlumberConfig.kt:19` | `lookup` participates in data-class equals/hashCode (by identity); `Lookup.awakers/slumberers` are public mutable | smell | ✔ (SLB-11) |
| A6 | `SlumberConfig.kt:83-99` | `getOrPut` stores null then treats it as a miss → unresolvable types re-traverse all modules per call; `error()` raises ISE, not a SlumberException | design | ✔ (SLB-10) |

## B. Exceptions & public contracts

| # | Where | Claim | Sev | Status |
|---|---|---|---|---|
| B1 | `exceptions.kt:14` | `SlumberException : Throwable` — `catch (e: Exception)` misses it; live consequence in `funktor/cluster/.../BackgroundJobQueued.kt` fallback | defect | ✔ code; caller cite (agent SLB-8) |
| B2 | `Codec.kt:94,139` | `(data != null) implies returnsNotNull()` contracts are UNSOUND (Unit→NullCodec unwrapped; nullable target + garbage → null). Caller with unsound smart-cast: `funktor/core/.../AppConfig.kt:104` | defect | ✔ (SL-C2) |
| B3 | `codec_helpers.kt:70` | `slumber(KClass<T>): T?` types raw slumbered data as the SOURCE type — unchecked cast, CCE far from call | defect | ✔ (SL-H1) |
| B4 | `Codec.kt:172-181` | `slumberInternal` catches base `SlumberException` (awake side correctly catches only `AwakerException`) → retries on awaker errors and deliberate custom throws | design | ✔ (SL-C4) |
| B5 | `Codec.kt:117-127` | Failing first pass re-runs the WHOLE graph; a second pass that succeeds silently swallows the first failure | design | ✔ (SL-C3) |
| B6 | `Codec.kt:126,181` | `as? T?` on erased param = no-op that reads like a guard | smell | ✔ (SL-C5) |
| B7 | `Codec.kt:43-47` | `createType()` with self-referential bound (`Box<T: Comparable<T>>`) synthesizes an unhandleable KType → ISE, no second pass | smell | ✔ mechanism (SL-C6); probe pending |
| B8 | `exceptions.kt` | No `cause` anywhere — underlying reflection failures lose their stack | design | ✔ (SLB-12) |
| B9 | `Awaker.kt` reportNullError | Dead `context` parameter (receiver==argument at only call site) | smell | (agent SL-A1, O-21) |
| B10 | `Slumberer.kt` | `Slumberer.Context` has no logs/rootType — slumber errors carry far less than awake errors; DataClassSlumberer additionally never calls stepInto (→ D5) | design | (agent SL-A2) |
| B11 | `codec_helpers.kt:24-42` | Reified awake overload = checked cast (CCE); KClass/TypeRef overloads = unchecked (silent mistype). Same-looking API, two failure modes | smell | ✔ (SL-H3) |

## C. Dispatch (BuiltInModule)

| # | Where | Claim | Sev | Status |
|---|---|---|---|---|
| C1 | `BuiltInModule.kt:191` | Value-class branch sits ABOVE polymorphic-child → `@JvmInline value class` implementing a sealed interface slumbers as bare scalar, no discriminator; unawakable through parent. REGRESSION of 5d59d61c | defect | ✔ structurally (SLB-4) |
| C2 | `BuiltInModule.kt:226` + runtime-class dispatch | Enum constant WITH BODY (`E$A`) has `isEnum == false` → no slumberer at all (error or KotlinReflectionInternalError via C4). Awake side fine (declared type) | defect | ✔ structurally (SLB-5, O-3); probe pending on which error surfaces |
| C3 | `BuiltInModule.kt:125-133` vs `:216-221` | Awaker matches collections by EXACT class; slumberer by assignability → declared `Collection<T>`/`ArrayList<T>`/`LinkedHashSet<T>` fields slumber but cannot awake | design | ✔ (SLB-6) |
| C4 | `BuiltInModule.kt:92,173` | `primaryConstructor` computed eagerly for every classifier; Kotlin reflection throws `KotlinReflectionInternalError` (an Error) for synthetic classes → masks the clean "no known way" error | smell | ✔ eager; Error-type claim plausible, probe pending (SLB-9) |
| C5 | `BuiltInModule.kt:216` | `Iterable`/`Map` assignability ahead of data-class branch → a data class implementing a collection interface is emitted as bare list/map (latent, no such type in repo) | design | ✔ (SLB-7) |
| C6 | `BuiltInModule.kt:96,177` | `Unit` → NullCodec → a `Unit` field awakes to null (also proves B2); `cls in listOf(...)` allocates per resolution | smell | ✔ (SLB-14) |
| C7 | `any.kt` + `BuiltInModule.kt:195` | `AnySlumberer` for exact `Any::class` runtime class delegates straight back to itself → StackOverflowError (`codec.slumber(Any())`, or an `Any()` sentinel in a field). SOE is an Error → no diagnostic pass | defect | ✔ (O-9, SL-H2, SL-X3) |
| C8 | `BuiltInModule.kt` awake side, MutableX operands | `MutableList::class == List::class` etc. (same KClass) — the `|| cls == MutableX::class` operands never fire | cosmetic | ✔ pre-collected P2 |

## D. Objects (data classes, value classes, SlumberCache)

| # | Where | Claim | Sev | Status |
|---|---|---|---|---|
| D1 | `DataClassAwaker.kt:100-125` | `isMarkedNullable` tested BEFORE `isOptional` in both branches → a nullable param WITH a default never falls back to it: `val a: String? = "x"` + absent key → `null`, not `"x"` (kotlinx.serialization: `"x"`). Schema-migration trap | defect | ✔ (O-2) |
| D2 | `ValueClassAwaker.kt` | Use-site nullability never consulted → `H(x = null)` for `val x: N?` where `N` wraps `String?` awakes to `H(N(null))`, not `H(null)` — round-trip broken | defect | (agent O-1, traced end-to-end; verify with file read) |
| D3 | `DataClassSlumberer.kt:147-155` | `Cached` keyed on `data` alone: (1) context ignored (= pre-collected P3) → shared cache across configs serves each other's shapes; (2) EQUALITY key → instances equal-by-ctor but differing in a `@Slumber.Field` non-ctor property collide (spec fixture of exactly that shape exists); (3) strong refs + deep hash per call | defect | ✔ (O-4, O-5, SLB-3) |
| D4 | `DataClassSlumberer.kt:196-208` | `Default.slumber` returns a live `MutableMap`; with `Cached` the SAME instance is handed out repeatedly → any caller mutation corrupts the cache. Vault's StoredSlumberer copies today — nothing in the contract says it must | defect | ✔ (O-6) |
| D5 | `DataClassSlumberer.kt:203-205` | No `stepInto(prop.name)` → every slumber error from a nested field reports path 'root'; the tracking second pass is inert for data classes | defect (diagnostics) | ✔ (O-8) |
| D6 | `DataClassSlumberer.kt:137` | `SlumberCache.remove` skips the null/excludedClasses guard the other four members honour — consequence NIL (nothing is ever stored under excluded/null keys). Uniformity fix only | smell | ✔ (O-11; pre-collected P4 DOWNGRADED) |
| D7 | `ObjectInstanceCodec.kt` | `awake`: any non-null input yields the singleton (zero validation, spec-pinned); `slumber`: always `emptyMap()`, dropping `@Slumber.Field` props — and the polymorphic path uses DataClassSlumberer for the same class, so the two disagree | design | (agent O-12, O-13; dispatch side ✔) |
| D8 | `DataClassAwaker.kt:79-84` | `contains(param.name)` but `data[paramName]` — different keys when `param.name == null` (inner classes): probes null, reads "n/a" | smell | ✔ (O-16) |
| D9 | `DataClassAwaker.kt:134-142` | Null primaryCtor lands in "misses parameters" with an empty list — wrong diagnosis (public-ctor path only) | smell | ✔ (O-17) |
| D10 | `DataClassAwaker.kt:91-97` | `withNullability(true)` allocates a fresh KType per optional field PER CALL, each memoized forever in Lookup | smell (perf) | ✔ (O-18) |
| D11 | `DataClassAwaker.kt:41-44` | `nullables` pre-map is dead weight (every entry overwritten); its existence likely hid D1 | smell | ✔ (O-19) |
| D12 | `ValueClassSlumberer` vs `ValueClassAwaker` | Slumber dispatches on RUNTIME class of unwrapped value, awake on DECLARED inner type — diverges where those pick different codecs (C2 enum case) | smell | (agent O-14) |
| D13 | `EnumCodec.kt` | Inert `@Suppress("UNCHECKED_CAST")`; NPE for non-enum via public ctor (dispatch guards it) | smell | (agent O-15) |
| D14 | `NonNullAwaker`/`NonNullSlumberer` | `open` + private `inner`, no subclasses — wrapped codec unrecoverable; codegen documents a workaround | smell | (agent O-20) |
| D15 | `ValueClassAwaker` KDoc | Security paragraph claims `init { require(...) }` runs via callBy — UNVERIFIED, load-bearing. 2-line probe needed; if false the guidance is inverted | docs | (agent A3, judgement call #1) |

## E. Polymorphism — agent-verified, coordinator pass PENDING

| # | Where | Claim | Sev |
|---|---|---|---|
| E1 | `polymorphism/Polymorphic.kt:139` vs `:172-174` | Awaker reads discriminator from `cls`, slumberers hop to `getParent(cls) ?: cls` → custom discriminator + companion-less sealed intermediate: slumber writes `"kind"`, awake looks for `"_type"`. Round-trip broken; `getDefaultType` same shape; zero test coverage of that combination (P1) | defect |
| E2 | `PolymorphicParentSlumberer.kt:33` | Instantiable (non-sealed) parent instance not in child map → re-dispatch on its own runtime class → SAME slumberer → StackOverflowError. Vault's `getAllStoredClasses` includes mainClass itself (P2) | defect |
| E3 | `PolymorphicParentSlumberer.kt:31-34` | Unregistered child: parent overwrites the CORRECT discriminator the child slumberer just wrote with `discriminator to null` (P3) | defect |
| E4 | `polymorphism/Polymorphic.kt:157` | Duplicate identifiers (same `@SerialName`, or alias colliding with a primary) silently last-wins → one child unawakable (P4) | defect |
| E5 | `polymorphism/Polymorphic.kt:245-250` | `getChildren` returns duplicates (annotated + sealed overlap, no distinct) → codegen emits duplicate TS union variants; map-building callers dedupe by accident (P5) | defect |
| E6 | `PolymorphicAwaker.kt:41` | Generic child awoken via KClass overload → type args erased to upper bounds → `Box<T>` fields come back raw (P6) | defect |
| E7 | `PolymorphicChildSlumberer.kt:30` | Discriminator silently overwrites a same-named real property (`val _type`) (P7) | defect |
| E8 | `polymorphism/Polymorphic.kt:65` | Bare `@SerialName` (no polymorphic parent) makes a class a "child" → `_type` injected on slumber, ignored on awake — asymmetric wire change (P8) | design |
| E9 | `PolymorphicAwaker.kt:28-42` | All four failure modes → bare null, zero `context.log` — the most common polymorphic support case has no diagnostics (P9) | design |
| E10 | `polymorphic.kt:16-21` (root) | `indexedSubClasses()` swallows every Throwable → `emptySet()`: top-level (non-companion) Parent object NPEs silently; missing kapt processor indexes nothing silently (SL-P1) | defect |
| E11 | misc | P10 PROPERTY target unread; P11 getParent order unspecified; P12 no cycle guard on hand-written childTypes; P15 discriminator written twice (hides E3); P16 public unchecked cast; P17 self-imports; P18 silent non-subclass drop | design/smell |
| E12 | `polymorphism/Polymorphic.kt:101` | Default identifier = FQCN → wire/storage discloses internal layout; renames break persisted data (P13) | design (security-lite) |
| E13 | **SECURITY, COLLECTED** | ClassIndex-fed `childTypes` widens the awakable allow-list to every indexed subclass on the classpath, incl. dependencies — deserialization surface grows with no code change at the declaration site. Closed enumeration (no Class.forName) — "unbounded-by-accident allow-list". → red-team task item (P14) | security |

## F. Collections — agent-verified, coordinator pass PENDING (C1 pre-probed earlier)

| # | Where | Claim | Sev |
|---|---|---|---|
| F1 | `CollectionAwaker.kt:40,46,79` | Star-projection `!!` NPEs (`List<*>`, `Set<*>`, `Array<*>`) — pre-collected P1, probed 2026-07-29 | defect |
| F2 | `MapAwaker.kt:29-39` | Null-value relaxation for `Map<K, *>` is INVERTED AND DEAD (elvis substitutes non-null `Any` before the null-check) — the exact case its commit (a69b2f65) was written for throws. One-token fix: `?: TypeRef.AnyNull.type` | defect |
| F3 | `MapAwaker.kt:57-67` / `MapSlumberer.kt:15-24` | Key collisions after awaking/slumbering silently collapse (last wins) | defect |
| F4 | `CollectionAwaker.kt:113` / `CollectionSlumberer.kt:17` + `Codec` retry | One-shot Iterable: first pass consumes it, second pass reads exhausted input → error becomes EMPTY collection. Root cause in Codec's re-run (B5) | defect |
| F5 | `CollectionAwaker.kt:67-99` | Null-safety of primitive-array elements rests on the per-module `wrapIfNonNull` CONVENTION; a module that forgets → bare IAE from `JavaArray.set`, no path (C6/C7) | design |
| F6 | `CollectionAwaker.kt:79` | `arguments[0]` IOOBE for arg-less array KType; `awake(Array<String>::class, ...)` erases element type → heap pollution (C8) | smell |
| F7 | `MapAwaker.kt:25-27` | `Map<*, V>` KEY fallback is `String` (silently retypes Int keys), inconsistent with everything else (C9) — settle together with F1/F2 | design |
| F8 | `MapAwaker/MapSlumberer` paths | Raw map key interpolated into diagnostic path unbounded/unescaped — 1 MB attacker key echoes into exception + logs; `.` in key forges path separators. Relates to `20260729-redteam-log-forging.md` (C10) | smell (security-adjacent) |
| F9 | misc | C11 non-scalar slumbered keys vs JSON transport; C12 null-for-wrong-shape convention (decide once); C13 duplicated `readArrayElements`; C14 ByteArray → List<Byte> boxing + unsigned rejection (base64 candidate); C15 double map copy | design/smell |

## G. Primitives — agent-verified (Kotlin-semantics claims, matrix in report A6)

| # | Where | Claim | Sev |
|---|---|---|---|
| G1 | `boolean.kt` | `Number.toInt()` truncation: 2^32-multiples → `false`; string truthiness 32-bit + rejects fractions — disagrees with Number path (F1, F2) | defect |
| G2 | `byte/short/int.kt` | `Double.NaN.toLong() == 0` sails through range checks → NaN awakes as 0 (Long codec correctly rejects NaN) (F3) | defect |
| G3 | `long.kt` | Range check widens `Long.MAX_VALUE` to Double → exactly 2^63 passes, saturates silently; String→Long parses via Double → distinct longs >2^53 collapse silently (F4, F5) | defect |
| G4 | `float.kt` | No range check Double→Float → `Double.MAX_VALUE` becomes `Infinity` "successfully" (F6) | defect |
| G5 | misc | F7 fractional strings truncate for int targets (untested, consistent); F8 `when` style; F9 Number strings always → Double (spec-pinned design) | smell/design |

## H. Datetime — agent-verified

| # | Where | Claim | Sev |
|---|---|---|---|
| H1 | `mp/MpLocalDateCodec`, `MpLocalDateTimeCodec`, `MpZonedDateTimeCodec` | `data[TIMEZONE] as String` unsafe cast → map without "timezone" key THROWS TypeCastException (violates null-contract); kotlinx siblings default to UTC (F05-F07) | defect |
| H2 | `mp/MpLocalTimeCodec` vs `javatime/LocalTimeCodec` | WIRE MISMATCH: millis-of-day (Long) vs seconds-of-day (Int) — 1000x apart for the same concept; mp side also has NO range validation (negative input → internally inconsistent MpLocalTime) (F08, F09) | defect |
| H3 | `javatime/ZonedDateTimeCodec` | Unknown zone id → DateTimeException thrown (not null), escapes awakeInternal (F04) | defect |
| H4 | `javatime/JavaTimeModule` | ZoneId awake = exact classifier, slumber = isSuperclassOf → `ZoneOffset` field slumbers, cannot awake (F02) | defect |
| H5 | `kotlinx/LocalDateTimeCodec` | Writes `timezone: "Z"` while every sibling writes `"UTC"`; "Z" fails javatime's whitelist (F11 + F03) | defect |
| H6 | `javatime/DateCodec` | `human` field rendered in JVM default TZ next to `timezone: "UTC"` (F01) | smell |
| H7 | family drift | Same wire shape read 3 ways (timezone ignored / optional / required) (F13); kotlinx family covers only 2 of 5 concepts (F12); MpTimezone accepts any string, fails later at `.kotlinx` (F10); dead `format` val (F14) | design |

## I. KotlinX interop & commonMain — agent-verified

| # | Where | Claim | Sev |
|---|---|---|---|
| I1 | `JsonUtil.kt:33-36` | JSON integer beyond Long.MAX_VALUE silently → lossy Double (D1) | defect |
| I2 | `JsonUtil.kt:60-64`, `KotlinXJsonObjectCodec` | Unchecked `Map<String, Any?>` cast → raw CCE for non-String keys, bypasses the diagnostic pass (D2) | defect |
| I3 | misc | S1 unquoted-literal → silent null (exotic); S2 dead casts; S3 untested public helpers (`polymorphic()`, `addChildren()` — zero callers); S4 shadowed type param; S5 wildcard import in Slumber.kt; DS1 `@Slumber` empty `@Target` (inapplicable annotation); DS2 misleading multiplatform surface (engine is JVM-only) | smell/design |

## Verified CLEAN (do not re-tread; per-agent details in the A*.md reports)

- Path tracking (stepInto) immutable, no push/pop bug; Fast contexts shareable; log buffers fresh per top-level call.
- `wrapIfNonNull` coverage on the awake side complete except deliberate NullCodec/value-class skips (commented).
- KotlinX branch ordering correct on both sides (Null before Primitive; KotlinX before sealed-parent and Iterable/Map).
- Polymorphic ordering (parent > child > objectInstance > isData) correct; enum/value-class not hijacked by it.
- NO dynamic class loading anywhere in the polymorphic path — `_type` only selects from a closed, startup-computed set.
- `AdditionalSerialName` is genuinely awake-only; deep sealed nesting and sealed interfaces fully enumerated; `object` children work.
- DataClassAwaker uses `callBy` → defaults genuinely apply (non-nullable optional case); unknown keys ignored; private ctors handled.
- Value classes: VC-over-VC, VC as map key/value, generic VC, stdlib VC fail-fast — all spec-pinned and hold.
- Primitive-array element coercion routes through the normal codec (consistent with scalars); `Array<T?>` nulls preserved; nested arrays get exact component types (suspected `Object[][]` — DISPROVED).
- JsonUtil: 1 vs 1.0 distinguishable; full Long range exact; `isString` honoured; JsonNull vs absent preserved.
- kotlinx-datetime 0.6.2: no ERROR-deprecated API in use (only `Instant.now()` is, and it is not used).
- All 1232 existing tests pass; every defect above lives in an input no spec exercises.

## Next steps (pending user decisions)

1. Coordinator verification of clusters E and F (+ spot-probes for C2, C4, D2).
2. Split mechanical vs design; design questions become FAILING specs first. Likely design questions:
   value-class/polymorphic dispatch order (C1), nullable-with-default semantics (D1), SlumberCache
   keying (D3/D4), Lookup thread-safety strategy (A1-A4), datetime wire-format reconciliation (H2/H5),
   primitive coercion strictness (G1-G4), polymorphic discriminator hop (E1) + diagnostics (E9).
3. E13 (ClassIndex allow-list) → new red-team task file when fixes start, per security policy.
4. F8 feeds the existing `20260729-redteam-log-forging.md` scenarios.
