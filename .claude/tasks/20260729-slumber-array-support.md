# Slumber: array codec support

**Status:** IN REVIEW (implemented 2026-07-29; review gate pending with the SDK work)
**Plan:** none — raised from `.claude/tasks/20260729-ts-sdk-codegen.md` (walker/Slumber parity work)
**Security-critical:** no (serialization shape only; no auth or trust boundary changes)

## Why

`Array` is not `Iterable`, so `BuiltInModule.getSlumberer` never dispatches for a declared array type
and `SlumberConfig.getSlumberer` throws *"There is no known way to slumber the type"*. Same on the
awaker side. Arrays are simply unsupported as a declared property type today.

Found on 2026-07-29 while making the TypeScript walker mirror Slumber's dispatch exactly. Nothing is
currently blocked — zero array-typed properties exist across `funktor/`, `funktor-demo/`,
`ultra/model/` and `ultra/remote/` — but the gap is a papercut worth closing, and closing it removes a
special case from the SDK generator's error surface.

## The asymmetry (why this is not a one-liner)

- **Slumbering is nearly free.** `CollectionSlumberer` already handles `Array<*>` values
  (`ultra/slumber/src/jvmMain/kotlin/builtin/collections/CollectionSlumberer.kt:13`); it is just never
  reached, because `BuiltInModule` routes on `Iterable::class.java.isAssignableFrom(cls.java)`
  (`BuiltInModule.kt:179`), which is false for arrays.
- **BUT it does not cover primitive arrays.** `IntArray` is `int[]`, not `Object[]`, so
  `data is Array<*>` is FALSE for it. Kotlin's eight primitive array types do not unify with `Array<T>`.
- **Awaking needs real work.** `CollectionAwaker` handles an array as incoming *data*
  (`CollectionAwaker.kt:31`) but always awakes into a `List`. Producing a typed array needs
  `java.lang.reflect.Array.newInstance` plus per-primitive handling.

Slumber-only support would create a type that serializes but cannot round-trip — strictly worse than
today's honest fail-fast. So both directions land together or neither does.

## Spec

- [x] `CollectionSlumberer` handles every array kind uniformly, including the eight primitive arrays.
      Use `java.lang.reflect.Array.getLength` / `get` rather than nine `is` branches.
- [x] `BuiltInModule.getSlumberer` dispatches arrays to `CollectionSlumberer`.
- [x] `CollectionAwaker.forArray(type)` builds a correctly typed array via
      `java.lang.reflect.Array.newInstance`, for `Array<T>` and for each primitive array type.
- [x] `BuiltInModule.getAwaker` dispatches arrays to it.
- [x] Element types are awoken through the normal context, so nested types, nullability and
      polymorphism keep working inside arrays.
- [x] `Array<String?>` (nullable elements) round-trips; a primitive array rejects nulls with a clear
      error rather than an NPE or a silent zero.
- [x] The TS walker's array branch is revisited afterwards: `isIterableLike()` currently excludes
      arrays deliberately, with the "Use a List" message. Once Slumber supports arrays, arrays should
      map to `ArrayOf` and that unresolved reason should go.

## Ordering note

Deliberately placed AFTER the `ultra/codegen` model layer landed, so the two changes stay separately
reviewable: `ultra/slumber` is battle-tested production code, whereas the codegen work is all-new.
Do not fold them into one commit.

## Test evidence

- [x] Round-trip per array kind: `Array<String>`, `Array<SomeDataClass>`, `Array<String?>`,
      `IntArray`, `LongArray`, `ByteArray`, `ShortArray`, `FloatArray`, `DoubleArray`,
      `BooleanArray`, `CharArray`
- [x] Nested: `Array<List<String>>`, `List<Array<String>>`, `Array<Array<String>>`
- [x] Polymorphic element type inside an array keeps its discriminator
- [x] Empty array round-trips (and does not collapse to null)
- [x] Null rejected for a primitive-array element, with an actionable message
- [x] Mutation-tested — the round-trip tests must fail if element awaking is skipped
- [x] Full test command(s) run + green: `./gradlew :ultra:slumber:jvmTest` — 16 array tests, whole
      slumber suite green with no regressions (0 failures, 0 errors)
- [x] `./gradlew :ultra:codegen:test` — 50 green after the walker follow-up

### Implementation notes

- `rejectNullElements` was implemented, then DELETED as dead code. A mutation survived, which
  revealed that primitive-array element types are non-nullable, so `SlumberModule.wrapIfNonNull`
  already wraps their awakers in `NonNullAwaker` — the null is reported with the element path
  before the array is ever written to. The guard duplicated a framework guarantee.
- `CharSlumberer` maps a `Char` to a `Char`, not a `String` (`builtin/primitive/char.kt:18`); the
  JSON writer renders it as a string later. My first test asserted the wrong layer.
- Mutation testing: 6/6 killed. Two rounds — the first surfaced the dead guard above and a missing
  test for accepting a primitive array as INPUT data (the slumberer already did; the awaker's
  branch was untested).


## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | | |
| 2. Domain expert | | |
| 3. Security | | |

Fixes applied: ...

## Follow-ups

- [x] Revisit `ultra/codegen`'s `isIterableLike()` and the array unresolved-reason once this lands
      (see spec item above); update `20260729-ts-sdk-codegen.md`'s parity table accordingly.

### Star-projected collections NPE on awake — TODO

Found 2026-07-29 during the `ultra/reflection` scan
(`.claude/tasks/20260729-reflection-scan-findings.md`), landing here because two of the three sites
are this task's own code. **Probed, not speculative.**

`CollectionAwaker` reads its element type with `type.arguments[0].type!!` in three places. That
argument is `null` exactly when the type is star projected, so the `!!` throws a
`NullPointerException` with a `null` message:

| Site | Declared type that breaks |
|---|---|
| `CollectionAwaker.kt:39` `forList` | `List<*>` |
| `CollectionAwaker.kt:44` `forSet` | `Set<*>` |
| `CollectionAwaker.kt:73` `forArray` | `Array<*>` — added by this task |

Primitive arrays are unaffected: they take the early return at `:62-71`, so `:73` is only reached for
`Array<T>`.

Evidence — probed through the real codec. Before the reflection fix, `List<*>` failed earlier, inside
`ReifiedKType`; after it, serialization succeeds and deserialization reaches this `!!`:

```
PROBE-STAR-LIST (slumber): OK -> {values=[a]}
PROBE-AWAKE:               THREW NullPointerException: null
PROBE-AWAKE-TRACE:         CollectionAwaker$Companion.forList(CollectionAwaker.kt:19)
                           | BuiltInModule.getAwaker(BuiltInModule.kt:110)
```

(The trace line numbers predate this task's edits; the call is now at `:39`.)

Suggested fix: the element type of a star projection is `Any?`, so
`type.arguments[0].type ?: TypeRef.AnyNull.type` at all three sites. For `forArray` the component
type then becomes `Object[]`, which is correct for `Array<*>`. Worth a regression test per shape
(`List<*>`, `Set<*>`, `Array<*>`, `Map<String, *>`), and a mutation check — reverting any one `?:`
must fail exactly its own test.

Not urgent: no star-projected collection exists as a declared property anywhere in the repo (that is
why this has never been hit). It is a fail-with-a-useless-message rather than a wrong result.

### `BuiltInModule` dispatch has three redundant branches — TODO (cosmetic)

`MutableList::class` **is** `List::class` on the JVM — the same `KClass` object, not merely an equal
one (probed: `Map::class === MutableMap::class` is `true`, and both print `kotlin.collections.Map`).
So these three conditions each test the same thing twice:

- `BuiltInModule.kt:109` — `cls == List::class || cls == MutableList::class`
- `BuiltInModule.kt:112` — `cls == Set::class || cls == MutableSet::class`
- `BuiltInModule.kt:117` — `cls == Map::class || cls == MutableMap::class`

Dead, not broken — dispatch is correct either way. Dropping the `Mutable*` halves would remove the
false implication that mutability is distinguishable here.

**Related, already actioned in the reflection scan:** `kMutableMapType()` was deleted for the same
reason, and `MapAwakerSpec` lost three tests that were exact duplicates of three others — because
`Map::class.java` and `MutableMap::class.java` are the same object, `MutableMap::class.java
.isAssignableFrom(x)` and `Map::class.java.isAssignableFrom(x)` are the identical assertion. The
surviving "Awaking a MutableMap (in a data class)" test IS meaningful: a declared `MutableMap<K, V>`
property has a genuinely different `KType`, even though the `KClass` is shared.

**Checked and CLEAN:** awaking into `MutableList`/`MutableSet`/`MutableMap` works and does not crash
— probed as data-class fields, as top-level types, and round-tripping back out. Slumber always builds
mutable instances (`ArrayList`, `LinkedHashSet`, `LinkedHashMap`) and `add`/`put` succeed on them.
`MapAwaker` and `CollectionSlumberer` contain no `!!` of this kind.
