# Mutator

**Documentation: [peekandpoke.io/ultra/mutator](https://peekandpoke.io/ultra/mutator/)**

KSP-generated mutable wrappers for immutable Kotlin data classes. Inspired
by [immer.js](https://immerjs.github.io/immer/).

Nested mutations, automatic change propagation, dirty tracking — zero boilerplate.

## The problem

Updating a deeply nested immutable data class in Kotlin means writing this:

```kotlin
val updated = company.copy(
    ceo = company.ceo.copy(
        address = company.ceo.address.copy(
            city = company.ceo.address.city.copy(
                name = "Amsterdam"
            )
        )
    )
)
```

## The solution

With Mutator, the same update is:

```kotlin
val updated = company.mutate {
    ceo.address.city.name = "Amsterdam"
}
```

That's it. The nested `copy()` chain is generated for you. And every level in the chain automatically
knows something changed.

## Features

- **Nested mutation** — Mutate any depth without nested `copy()` calls
- **Change tracking** — `isModified()` tells you if anything changed
- **Commit / reset** — `commit()` accepts the current state, enabling undo/reset
- **onChange propagation** — Sub-mutators notify parents automatically (ideal for form components)
- **Collections** — `ListMutator`, `SetMutator`, `MapMutator` with full mutation support
- **Sealed classes** — `cast()` and `filterMutatorsOf<T>()` for polymorphic editing
- **KSP code generation** — Annotate with `@Mutable`, get type-safe mutators at compile time
- **Kotlin Multiplatform** — JVM and JS targets

## When to use Mutator

Mutator shines when your data classes are **3+ levels deep** and you need to:

- Edit nested form state without wiring callbacks through every component
- Track dirty state across a complex object graph
- Provide undo/reset on deeply nested structures

## When NOT to use Mutator

For flat or shallow data classes, Kotlin's built-in `copy()` is perfectly fine.
Mutator adds value proportional to nesting depth.

## Quick start

```kotlin
// 1. Annotate your data classes
@Mutable
data class Person(val name: String, val address: Address)

@Mutable
data class Address(val street: String, val city: String)

// 2. Mutate
val updated = person.mutate {
    name = "Alice"
    address.city = "Berlin"
}

// 3. Track changes
val mutator = person.mutator()
mutator.address.city = "Berlin"
mutator.isModified()  // true
mutator.commit()      // accept changes, isModified() resets to false
```

## More examples

See the [documentation](https://peekandpoke.io/ultra/mutator/) and the
[test suite](core/src/jvmTest/kotlin) for more examples.
