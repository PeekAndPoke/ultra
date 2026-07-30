# ultra/reflection scan — findings backlog

**Status:** IN PROGRESS — 8 main files documented, 33 findings collected, first fix round DONE
**Plan:** none — output of a 6-agent scan of `ultra/reflection`, 2026-07-29
**Security-critical:** no

## DONE — 2026-07-29

| Finding | Fix | Verified by |
|---|---|---|
| #1 star projections crash `ReifiedKType` | pass star projections through untouched | mutation: reverting fails exactly the star test, 1/65 |
| #2 `T?` reified as non-null, so null was rejected | `withNullability(true)` when the use-site is nullable | mutation: reverting fails exactly the 2 nullability tests; the non-null control stays green |
| #10 (part) inner-class `indexOf == -1` crash | `arguments.getOrNull(index)` | came free with #2 |
| #4/#17 unsynchronized caches | all five `TypeRef` caches + `NthParamNameCache` → `ConcurrentHashMap` | compile sweep + full dependent-module suites |
| #11 `LazyThreadSafetyMode.NONE` on JVM-wide shared instances | dropped to the default synchronized `lazy` in `TypeRef` (3) and `ReifiedKType` (6) | ditto |
| #5 `unlisted` bare NPE on `List<*>` | descriptive `error(...)`, matching the guard above it | — |
| #15 `kMutableMapType()` | **deleted** — it could not do what its name said. 4 test call sites moved to `kMapType` | counts reconcile exactly |
| #14 meta-annotation path untested | 4 tests added using the previously-orphaned fixtures | they pass, so the meta walk works |
| ChildFinder (all 13) | **deleted**, file + spec — zero consumers repo-wide | grep across all file types |
| hygiene | 2 fully-qualified `kotlin.reflect.KProperty<*>` casts in `AnnotationsSpec` → import | style guide |

Test counts reconcile with no collateral damage: reflection 65 → 58 (−10 `ChildFinderSpec`,
−1 vacuous `kMutableMapType` test, +4 meta-annotation); slumber 1219 → 1216 (−3 duplicates, below).
Dependent suites all green: vault 285, kontainer 186, karango/core 1649, monko/core 249.
Full compile sweep clean, including the root project's own `src/jvmMain`.

**Bonus finding while removing `kMutableMapType`:** `MapAwakerSpec` contained three tests that were
exact duplicates of three others. Because `Map::class` and `MutableMap::class` are the *same object*,
`Map::class.java` and `MutableMap::class.java` are too — so `MutableMap::class.java.isAssignableFrom`
and `Map::class.java.isAssignableFrom` are the identical assertion. Deleted. The remaining
"Awaking a MutableMap (in a data class)" test IS meaningful: a declared `MutableMap<K, V>` property
has a genuinely different `KType` from a `Map<K, V>` one, even though the `KClass` is shared.

## DONE — second round, 2026-07-29

| Finding | Fix |
|---|---|
| #3 `createForKClass` ignored `nullable` | `nullable = nullable` passed to `createType`; the original KDoc promise restored |
| #9 `ctorFields2Types` used `first` | `firstOrNull` + an error naming the class and the parameter |
| #13 `findAnnotationsRecursive` duplicates | one traversal over all direct annotations, sharing a single visited set |
| #16 `KClass.kType()` ignored upper bounds | each parameter filled from `upperBounds.firstOrNull()`, falling back to `Any` |
| #18 `types.kt` `isPrimitive()` | **deleted** with `TypesSpec.kt` — zero call sites, and the name meant the opposite of `ultra/common`'s |
| #12 `cls` cast | `as?` + a descriptive precondition naming the offending classifier |

**Consequence worth recording for #16:** an unbounded type parameter has upper bound `Any?`, not
`Any`, so `List::class.kType()` now yields `List<Any?>` where it used to yield `List<Any>`. That is
the correct fill — a `List<String?>` is a `List<Any?>` and *not* a `List<Any>` — but it is a wider
change than "honour the bound" suggests. Two `TypeRefSpec` assertions pinned the old value and were
updated. Everything downstream stayed green: slumber, vault, kontainer, karango and monko all pass
unchanged, so nothing depended on the non-null fill.

**Note on #13:** `.distinct()` and a shared visited set produce *identical* results — the union of
the per-branch reachable sets equals the globally reachable set. The shared set only avoids
re-walking shared subtrees. It was chosen to fix the cause rather than the symptom.

## STILL OPEN

- #6, #7, #8 — judged design consequences; see the tables below.

**Handed off to slumber**, written up in full (with probe output and suggested fixes) under
"Follow-ups" in `.claude/tasks/20260729-slumber-array-support.md`:

- the same `!!`-on-a-star bug at `CollectionAwaker.kt:39/:44/:73`, so `List<*>`, `Set<*>` and
  `Array<*>` NPE on awake — `:73` was added by the array work in `305d7c45`;
- the three redundant `|| cls == MutableX::class` branches in `BuiltInModule`.

That file was chosen over a new one because two of the three `!!` sites are its own code. No
dedicated slumber scan-findings backlog exists yet; when the slumber pass starts, fold these in.

**Answered, no task needed:** awaking into `MutableList`/`MutableSet`/`MutableMap` already works and
does not crash — probed as fields, as top-level types, and round-tripping back out. Slumber always
builds mutable instances (`ArrayList`, `LinkedHashSet`, `LinkedHashMap`) and `add`/`put` succeed on
them; a read-only declaration simply gets the same instance as a view.

All 8 main-source files were documented (comment-only). `functions.kt`, `types.kt` and `index.kt`
needed no doc changes. Findings below are marked `[verified]` when a probe was run and reproduced,
`[reported]` when they rest on code reading alone.

Module context: JVM-only (`src/main/kotlin`). Depended on by **karango/core, monko/core, kontainer,
slumber, vault** — so `TypeRef` and `ReifiedKType` sit underneath the whole serialization layer.
All 8 files were last touched by the maintainer in March 2026; none is recent agent work.

## The cross-cutting theme

Three separate agents independently reported the same structural issue: **every cache in this module
is an unsynchronized `mutableMapOf`, and the objects they hand out use
`LazyThreadSafetyMode.NONE` — while the instances are cached JVM-wide and read from request threads.**

- `TypeRef.kt:24,27,30` (global) and `:243,248` (per-instance, on globally shared instances)
- `functions.kt:41` — `NthParamNameCache`, reached from karango's `FOR(...)` per query build
- `ReifiedKType.kt:49,61,70,82,91,102` — all six lazies are `NONE`, on instances cached in
  `TypeRef.reified` and in funktor's `ConcurrentHashMap` at `broker/vault/value_class.kt:40,79`

Measured: 32 threads × 400 KTypes through `TypeRef.createForKType` left 5967/12000 KTypes with
duplicate instances, and `cachedKTypes.keys.toList()` returned 9890 entries containing only 324
distinct objects — a cyclic LinkedHashMap chain, with `containsKey` false for 9563 of them. The
single-threaded control over identical work was clean, so this is the race, not unstable hashing.

Mitigating: `TypeRef` is a data class with equality on `type`, so no *wrong* value is handed out.
The damage is a corrupted and permanently ineffective cache, unbounded retention, and a map whose
iteration does not terminate.

## Confirmed defects that break serialization

### 1. `[verified]` Star projections crash `ReifiedKType` — HIGH

`ReifiedKType.kt:131` does `it.copy(type = reifyType(it.type ?: TypeRef.Any.type))`.
`KTypeProjection.init` requires `(variance == null) == (type == null)`. A star projection has both
null, so filling the type is exactly what is forbidden — the `?:` fallback can only ever throw.

Probed through the real Slumber codec, not just the unit level:

```
PROBE-CONTROL:         OK -> {values=[a]}
PROBE-STAR-LIST:       THREW IllegalArgumentException: Star projection must have no type specified.
PROBE-STAR-MAP:        THREW IllegalArgumentException: Star projection must have no type specified.
PROBE-STAR-KCLASS:     THREW IllegalArgumentException: Star projection must have no type specified.
PROBE-AWAKE-STAR-LIST: THREW IllegalArgumentException: Star projection must have no type specified.
```

Any data class with a `List<*>`, `Map<K, *>` or `KClass<*>` member cannot be serialized **or**
deserialized. Reaches `DataClassAwaker`, `DataClassSlumberer`, vault's `DatabaseGraphBuilder` and
four funktor call sites.

### 2. `[verified]` Nullability dropped when substituting a type parameter — HIGH

`ReifiedKType.kt:123` returns the substituted argument verbatim and ignores `subject.isMarkedNullable`,
so `T?` reifies to the non-null substitution.

```
PROBE-NULL-AWAKE:       THREW AwakerException: Value at path 'root.value' must not be null
PROBE-NONNULL-AWAKE:    OK -> ProbeNullableTp(value=7)
PROBE-PLAIN-NULL-AWAKE: OK -> ProbePlainNullable(value=null)   <- non-generic control
```

A generic data class with a nullable type-parameter field rejects `null`, while the equivalent
non-generic field accepts it.

## Other findings

### TypeRef.kt

| # | Sev | Finding |
|---|---|---|
| 3 | HIGH `[verified]` | `createForKClass` never passes `nullable` to `cls.createType`, so it only picks a cache bucket; both buckets return the same non-nullable ref. Only in-repo caller passes `false`, so impact today is nil. Public-API landmine. |
| 4 | HIGH `[verified]` | Unsynchronized caches — see the cross-cutting section above. |
| 5 | MED `[verified]` | `unlisted` (`:237`) does `type.arguments[0].type!!`, so `kType<List<*>>().unList` dies on a bare NPE with a null message, right after a guard that raises a descriptive `error(...)` for the neighbouring case. |
| 6 | LOW `[verified]` | The reified `wrapWith<W>()` passes `null is W` with `W : Any`, a compile-time `false` (hence the existing `@Suppress("USELESS_IS_CHECK")`). `nullableWrapCache` is unreachable from it. Probably deliberate — Kotlin cannot express `W?` under that bound. |
| 7 | LOW `[verified]` | `createForKType<Int>(typeOf<String>())` compiles and yields a mistyped `TypeRef<Int>`. Deliberate: `KClass<*>` is needed so `List::class` can yield `TypeRef<List<Any>>`. |
| 8 | LOW `[reported]` | Caches never evict, pinning KClass/KType and their classloaders. No eviction path exists. |

### ReifiedKType.kt

| # | Sev | Finding |
|---|---|---|
| 9 | MED `[verified]` | `ctorFields2Types` (`:105`) uses `first` not `firstOrNull`, so a primary-ctor param that is not a `val`/`var` throws `NoSuchElementException`. Safe for data/value classes, which is why callers get away with it. |
| 10 | MED `[verified]` | `cls.typeParameters.indexOf(classifier)` (`:121`) is unchecked for `-1`; for `Outer<T>.Inner` the outer's parameter is not in the inner's list, so `arguments[-1]` throws. Inner classes also emit the outer receiver as a bogus `INSTANCE` param. |
| 11 | MED `[reported]` | All six lazies are `NONE` on shared instances — see cross-cutting section. No repro (data race). |
| 12 | LOW `[verified]` | `type.classifier as KClass<Any>` (`:35`) is eager, so a `KTypeParameter` classifier dies on a raw ClassCastException rather than a stated precondition. No path found where the library does this to itself. |

Checked and CLEAN (do not re-tread): variance survives `copy` (`List<out Number>` stays `out`); a root
type with too few arguments is unreachable because `createType` itself rejects it; inherited and
overridden properties reify correctly — kotlin-reflect already substitutes them against the derived
class, so plain inheritance does *not* trigger the `-1` above.

### ChildFinder.kt — DELETED 2026-07-29

13 findings, zero consumers repo-wide. Grep across all file types found no use outside its own file
and spec, so none was a live defect. Deleted rather than fixed; recorded here because the identity-hash
design flaw is worth remembering if anything similar is ever written again.

Highlights: cycle detection keys on `System.identityHashCode`, which collides (probed: two distinct
live nodes collided after 101,867 allocations, and the second subtree was silently dropped); any
`javax.*`/`jdk.*`/`sun.*` object aborts the whole search with `InaccessibleObjectException` on JDK 9+
because the blacklist only covers `java.` and `kotlin.`; `Set`, `Array` and `Sequence` children are
silently skipped (only `List` and `Map` are traversed); map **keys** are never visited; JVM-interned
values collapse to one result, so `find(String::class, TwoStrings("dup","dup"))` returns 1 but the
same call with runtime-built strings returns 2; superclass `private val`s are never traversed; the
predicate is non-monotone (loosening it can *remove* a nested result); `StackOverflowError` at ~2000
depth. All probed.

Checked and CLEAN: no shared mutable state across calls (the ctor is private and `find` returns the
list, not the instance); cyclic graphs terminate, including self-reference; a throwing getter is
harmless because values are read from the field; `startsWith("java.")` does not false-positive on
`javax.`.

### annotations.kt / kType.kt / functions.kt / types.kt

| # | Sev | Finding |
|---|---|---|
| 13 | MED `[verified]` | `findAnnotationsRecursive` runs cycle detection per direct annotation, so a meta-annotation reachable via two direct annotations is returned twice. Both current callers only check `isNotEmpty()`. |
| 14 | LOW `[verified]` | `AnnotationsSpec` declares `MetaAnnotation`/`AnnotatedAnnotation`/`MetaAnnotated` fixtures that **no test references** — the one behaviour that makes the function more than a filter has zero coverage. |
| 15 | MED `[verified]` | `kMutableMapType()` cannot do what its name says: `MutableMap::class` **is** `Map::class` on the JVM (probed: `===` is true, both print `kotlin.collections.Map`). It returns the identical cached instance as `kMapType()`, and its spec assertion `classifier shouldBe MutableMap::class` compares an object to itself — vacuous. |
| 16 | LOW `[verified]` | `KClass<T>.kType()` fills type arguments with `Any`, ignoring declared upper bounds, so `Boxed<T : Number>` yields `Boxed<Any>` — outside its own bound. Slumber solved the same problem correctly at `Codec.kt:36-39` using `upperBounds[0]`, which suggests oversight rather than choice. |
| 17 | MED `[reported]` | `NthParamNameCache` unsynchronized — see cross-cutting section. Values are pure functions of their key, so a lost entry is self-healing; the hazard is map corruption. |
| 18 | LOW `[verified]` | `types.kt`'s `isPrimitive()` excludes `Char` and includes `String` — the opposite of `ultra/common`'s same-named `KType.isPrimitive`. No call sites anywhere, but it is published API and the name clash is a trap. |

### Not defects — recorded so they are not re-reported

- `functions.kt:21` — `absoluteValue` does not fix the sign for `Int.MIN_VALUE`, so a fallback name
  could contain a `-`. Needs a KClass whose hashCode is exactly `-2147483648`; the KDoc promises
  determinism, not sign-freedom. Not a contract violation.
- `index.kt` — `REFLECTION = "reflection"` is the repo-wide source-set smoke marker (`CACHE_MP`,
  `MATHS_MP_JVM`, …), one constant because the module is not multiplatform. Correct as-is.
- `KClass.kType()` plain erasure (`List::class.kType()` → `List<Any>`) is inherent — a class carries
  no type arguments. Only the bound-violating substitution (#16) is reportable.
- Repeatable annotations, `@Inherited`, and interface-declared properties all behave correctly
  through `findAnnotationsRecursive` (probed).

## Method note

Three of the agents' doc edits **documented a defect as the contract**, a predictable consequence of
"fix wrong comments" + "report but do not fix logic":

- `ReifiedKType.kt` gained "Star-projected arguments ... fall back to `Any`" — outright false, they
  throw. Must be rewritten with the fix.
- `TypeRef.kt` gained "Note: `nullable` only picks the cache bucket" — accurate, but enshrines #3.
- `kType.kt` gained a note explaining `kMutableMapType` really yields `Map` — accurate, but
  enshrines #15.

Worth carrying into the next module's prompt: tell agents to leave a `TODO(scan)` marker instead of
documenting behaviour they are simultaneously reporting as wrong.
