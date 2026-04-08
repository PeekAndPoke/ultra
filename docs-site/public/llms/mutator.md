## Mutator — Immutable Data Mutation

### Overview {#mutator-overview}

# Mutator — Type-Safe Mutation of Immutable Data

KSP-generated code for mutating deeply nested immutable Kotlin data classes. No `copy()` chains, no boilerplate.
Changes propagate automatically from any depth back to the root.

## The problem Mutator solves

Kotlin's immutable data classes are great, but updating nested fields is painful:

```kotlin
val updated = person.copy(
    address = person.address.copy(
        city = person.address.city.copy(
            name = "Springfield"
        )
    )
)
```

With Mutator:

```kotlin
val updated = person.mutate {
    address.city.name = "Springfield"
}
```

## How it works

1. Annotate data classes with `@Mutable`
2. KSP generates `.mutator()` and `.mutate { }` extension functions
3. Generated accessors propagate changes automatically via `onChange` callbacks

```kotlin
@Mutable
data class Address(val street: String, val city: String, val zip: String)

@Mutable
data class Person(val name: String, val address: Address)

// One-shot mutation — returns a new immutable Person
val updated = person.mutate {
    name = "Jane"
    address.street = "Oak Ave"
}

// Long-lived mutator — accumulates changes
val mutator = person.mutator()
mutator.name = "Jane"
mutator.address.street = "Oak Ave"
mutator.isModified()  // true
val result = mutator.get()
```

## Two accessor styles

For each primary constructor parameter, KSP generates one of:

**Simple get/set** (for types without mutators — primitives, strings, non-@Mutable types):

```kotlin
inline var Mutator<Address>.street
    get() = get().street
    set(v) = modifyIfChanged(get().street, v) { it.copy(street = v) }
```

**Sub-mutator** (for @Mutable types — deep mutation with change propagation):

```kotlin
inline val Mutator<Person>.address
    get() = get().address.mutator()
        .onChange { address -> modifyValue { get().copy(address = address) } }
```

## Collections

Lists, Sets, and Maps of @Mutable types get collection mutators:

```kotlin
@Mutable
data class AddressBook(val addresses: List<Address>)

val updated = book.mutate {
    addresses[0].street = "Elm St"   // deep mutation into list element
    addresses.add(newAddress)         // MutableList operations
    addresses.removeAt(1)
}
```

Generated collection mutators: `List<T>.mutator()`, `Set<T>.mutator()`, `Map<K, V>.mutator()`.

## @Mutable(deep = true | false)

Controls whether the processor transitively discovers referenced types:

```kotlin
// Default: deep = true — discovers all referenced types recursively
@Mutable
data class Person(val address: Address)
// Address gets a mutator even without its own @Mutable annotation

// Shallow: deep = false — only this class, no auto-discovery
@Mutable(deep = false)
data class Order(
    val id: String,
    val meta: ExternalConfig,    // simple get/set (not in processing set)
    val address: Address,        // .mutator() accessor (Address IS @Mutable)
)
```

With `deep = false`, referenced types only get `.mutator()` accessors if they are independently `@Mutable`
or pulled in by another `deep = true` class. Otherwise they get simple get/set.

**Trigger pattern** for external types you don't own:

```kotlin
@Mutable(deep = true)
data class TriggerMutators(val ext: SomeExternalLibraryType)
// SomeExternalLibraryType now gets a mutator generated
```

## @NotMutable

Explicitly prevents mutator generation, even for transitively discovered types:

```kotlin
@NotMutable
data class Secret(val key: String)

@Mutable
data class Config(val secret: Secret)
// secret gets simple get/set — Secret is excluded from processing
```

## Dirty tracking

```kotlin
val mutator = address.mutator()
mutator.isModified()    // false
mutator.street = "Oak"
mutator.isModified()    // true
mutator.commit()        // accept as new baseline
mutator.isModified()    // false
mutator.reset()         // revert to initial value
```

## Structural equality

If a mutation results in a value equal to the initial, Mutator returns the exact same instance:

```kotlin
val result = address.mutate { street = address.street }
result === address  // true — same reference
```

## Sealed classes

Sealed classes and their subtypes are supported:

```kotlin
@Mutable
sealed interface Shape {
    data class Circle(val radius: Double) : Shape
    data class Rect(val w: Double, val h: Double) : Shape
}

// Filter mutators by subtype
val circles = shapes.mutator().filterMutatorsOf<Shape.Circle>()
```

## Plugin system

The KSP processor is plugin-extensible via `ServiceLoader`:

- `MutatorKspPlugin.vetoesType()` — block generation for specific types
- `MutatorKspPlugin.generatesMutatorFor()` — handle type-level generation
- `MutatorKspPlugin.generatesMutatorFieldFor()` — handle property-level generation
- `MutatorKspPlugins.isInProcessingSet(declaration)` — query whether a type will get a mutator generated

Built-in plugins:

- `BuiltInPlatformTypesVetoingPlugin` — vetoes `kotlin.*`, `java.*`, primitives, String
- `BuiltInMutableObjectsPlugin` — generates mutators for data classes and sealed types
