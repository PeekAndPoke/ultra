# Slumber cannot round-trip an intermediate sealed class with a root-level discriminator

**Status:** TODO — found and reproduced 2026-07-30, NOT fixed
**Plan:** none. Found incidentally while testing `ultra/codegen` (`.claude/tasks/20260729-ts-sdk-codegen.md`)
**Security-critical:** no — but it is a silent data-shape mismatch on a serialization path, so it wants
a careful review rather than a quick patch.

## The defect

`PolymorphicParentUtil` resolves the discriminator differently on the two sides:

| Side | Code | Resolves via |
|---|---|---|
| Slumber (write) | `createParentSlumberer` (`ultra/slumber/src/jvmMain/kotlin/builtin/polymorphism/Polymorphic.kt:120-126`) | `getParent(cls) ?: cls` — **hops to the root** |
| Awake (read) | `createParentAwaker` (`:100-110`) | `cls` — **does not hop** |

So when a `Polymorphic.Parent` companion sits on the ROOT of a hierarchy and a value is typed as an
INTERMEDIATE sealed class, the two disagree: slumbering writes the root's custom discriminator, awaking
looks for `_type`.

## Reproduction (executed 2026-07-30, not merely read)

Fixture — `ultra/codegen/src/test/kotlin/model/walker_fixtures.kt`, `FxDeepRoot`:

```kotlin
sealed class FxDeepRoot {
    companion object : Polymorphic.Parent {
        override val discriminator: String = "kind"
        override val childTypes: Set<KClass<*>> = emptySet()
    }

    sealed class Middle : FxDeepRoot()

    data class Leaf(val v: String) : Middle()

    data class Direct(val w: String) : FxDeepRoot()
}
```

With `Codec.default`:

```
slumber as Middle : {v=x, kind=io.peekandpoke.ultra.codegen.model.FxDeepRoot.Leaf}
slumber as Root   : {v=x, kind=io.peekandpoke.ultra.codegen.model.FxDeepRoot.Leaf}
awake   as Middle : AwakerException: Value at path 'root' must not be null
awake   as Root   : Leaf(v=x)
```

Writing is consistent; only reading through the intermediate type fails. It surfaces as
`must not be null` rather than anything naming the discriminator, because the awaker simply fails to
match a child and falls through to `getDefaultType`, which is null.

## Why it has not bitten

Zero `Polymorphic.Parent` companions exist outside test fixtures — `getDiscriminator` returns the
default `_type` everywhere in production today, so both sides agree by accident. The bug needs BOTH a
custom discriminator on a root AND a field typed as an intermediate sealed class.

## Fix direction (not decided)

Make `createParentAwaker` hop like the slumberer does:

```kotlin
val parent = getParent(cls) ?: cls
val discriminator = getDiscriminator(parent)
```

Note the child-class map is a SEPARATE question and should probably stay keyed on `cls` — a field typed
as `Middle` can only hold a `Middle` subclass, and widening it would let a payload naming a sibling
branch deserialize into a field that cannot hold it. `ultra/codegen` made exactly this split
deliberately (`model/TypeWalker.kt`, `declareUnion`) and the reasoning transfers.

## Gates

- [ ] A round-trip test per hierarchy shape: companion on the root, on an intermediate, on neither.
- [ ] Mutation-test the fix — reverting the hop must fail.
- [ ] `ultra/slumber` is battle-tested and on every production server's classpath: full
      `:ultra:slumber:jvmTest` plus the compile sweep, and a `/feature-review` round before DONE.
- [ ] Check whether `getIdentifiersToChildClasses` should widen too, or deliberately must not.

## Notes

Found while writing regression tests for the `ultra/codegen` review round. The codegen side is already
correct and needs no change: a generated schema parses what the SERVER WRITES, and the server writes
through the slumberer, so reading the root's discriminator is right. This task is only about Slumber's
own read path disagreeing with its write path.
