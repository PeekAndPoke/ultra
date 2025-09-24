# Examples for Mutator

## TOC

1. [Basic Mutator Concepts](#BASIC-MUTATOR-CONCEPTS)

    1. [Mutating data classes directly](#MUTATING-DATA-CLASSES-DIRECTLY)
    2. [Mutating data classes using a Mutator](#MUTATING-DATA-CLASSES-USING-A-MUTATOR)
   3. [Mutating lists of data classes](#MUTATING-LISTS-OF-DATA-CLASSES)
   4. [Mutating nested data classes](#MUTATING-NESTED-DATA-CLASSES)

2. [Code Generation](#CODE-GENERATION)

    1. [Generated code for data classes](#GENERATED-CODE-FOR-DATA-CLASSES)

## Basic Mutator Concepts

### Mutating data classes directly

This example shows how to apply the @Mutate annotation to a data class.

We will then mutate the data class directly using `mutate { ... }`.

(see the full [example](../../src/examples/basic_mutations/MutatingDataClassDirectly.kt))

```kotlin
// We defined the data class
@Mutable
data class Person(
    val name: String,
    val age: Int,
)

// Get an instance
val person = Person("John", 42)

// We mutate the instance directly
val result = person.mutate {
    name = "Jane"
    age -= 10
}

// Results in
println("Original: $person")
println()
println("Mutated:  $result")
```
Will output:
```
Original: Person(name=John, age=42)

Mutated:  Person(name=Jane, age=32)
```

### Mutating data classes using a Mutator

This example shows how to apply the @Mutate annotation to a data class.

We will then mutate the data class by creating and using a `mutator()`.

(see the full [example](../../src/examples/basic_mutations/MutatingDataClassViaMutator.kt))

```kotlin
// We defined the data class
@Mutable
data class Person(
    val name: String,
    val age: Int,
)

// Get an instance
val person = Person("John", 42)

// Create the mutator
val mutator = person.mutator()

// We can invoke the mutator ...
mutator {
    name = "Jane"
}

// Or we can change properties directly ...
mutator.age -= 10

// Get the result
val result = mutator()
val isModified = mutator.isModified()

// Results in
println("Is modified: $isModified")
println()
println("Original: $person")
println()
println("Mutated:  $result")
```
Will output:
```
Is modified: true

Original: Person(name=John, age=42)

Mutated:  Person(name=Jane, age=32)
```

### Mutating lists of data classes

This example shows how to mutate lists of data classes.

(see the full [example](../../src/examples/basic_mutations/MutatingListsOfDataClasses.kt))

```kotlin
// We defined the data class
@Mutable
data class Person(
    val name: String,
    val age: Int,
)

// Get an instance
val persons = listOf(
    Person("John", 42),
    Person("Jane", 40),
    Person("Jill", 30),
    Person("Jack", 20),
)

// We mutate the instance directly
val result = persons.mutate {
    // We only keep persons with age <= 30
    retainAll { it.age <= 30 }

    // And then we modify them
    forEach { it.name += "-changed" }
}

// Results in
println("Original:")
persons.forEach { println("  - $it") }
println()
println("Mutated:")
result.forEach { println("  - $it") }
```
Will output:
```
Original:
  - Person(name=John, age=42)
  - Person(name=Jane, age=40)
  - Person(name=Jill, age=30)
  - Person(name=Jack, age=20)

Mutated:
  - Person(name=Jill-changed, age=30)
  - Person(name=Jack-changed, age=20)
```

### Mutating nested data classes

This example shows how to mutate nested data classes.

(see the full [example](../../src/examples/basic_mutations/basic_mutations/MutatingNestedDataClasses.kt))

```kotlin
// We defined the data class
@Mutable
data class Person(
    val name: String,
    val age: Int,
    val address: Address,
)

@Mutable
data class Address(
    val street: String,
    val city: String,
)

// Get an instance
val person = Person(name = "John", age = 42, address = Address(street = "street", city = "city"))

// We mutate the instance directly
val result = person.mutate {
    // Mutate the nested object directly
    address.street = "newStreet"
    // Or mutate it in the context of it's mutator
    address {
        city = "newCity"
    }
}

// Results in
println("Original: $person")
println()
println("Mutated:  $result")
```
Will output:
```
Original: Person(name=John, age=42, address=Address(street=street, city=city))

Mutated:  Person(name=John, age=42, address=Address(street=newStreet, city=newCity))
```

## Code Generation

### Generated code for data classes

This example shows how to apply the @Mutate annotation to a data class.

We will then mutate the data class by creating and using a `mutator()`.

(see the full [example](../../src/examples/code_generation/CodeGeneratedForDataClasses.kt))

```kotlin
// We defined the data class
@Mutable
data class Person(
    val name: String,
    val age: Int,
    val address: Address? = null,
)

@Mutable
data class Address(
    val street: String,
    val city: String,
)

@Mutable
data class AddressBook(
    val persons: List<Person>,
)
```
Code generated for the Person class


```kotlin
@file:Suppress("RemoveRedundantBackticks", "unused", "NOTHING_TO_INLINE")

package de.peekandpoke.mutator.examples.code_generation

import de.peekandpoke.mutator.*

// Mutator for data class `CodeGeneratedForDataClasses`.`Person` (de.peekandpoke.mutator.examples.code_generation.CodeGeneratedForDataClasses.Person)
// Generated by the 'BuiltIn: Mutable Objects Plugin' plugin
@MutatorDsl
inline fun `CodeGeneratedForDataClasses`.`Person`.mutator() = ObjectMutator(this)

@MutatorDsl
inline fun `CodeGeneratedForDataClasses`.`Person`.mutate(
    mutation: ObjectMutator<`CodeGeneratedForDataClasses`.`Person`>.() -> Unit,
): `CodeGeneratedForDataClasses`.`Person` = mutator().apply(mutation).get()

@MutatorDsl
inline fun List<`CodeGeneratedForDataClasses`.`Person`>.mutator() = mutator(child = { mutator() })

@MutatorDsl
inline fun List<`CodeGeneratedForDataClasses`.`Person`>.mutate(
    mutation: ListMutator<`CodeGeneratedForDataClasses`.`Person`>.() -> Unit,
): List<`CodeGeneratedForDataClasses`.`Person`> = mutator().apply(mutation).get()

@MutatorDsl
inline fun Set<`CodeGeneratedForDataClasses`.`Person`>.mutator() = mutator(child = { mutator() })

@MutatorDsl
inline fun Set<`CodeGeneratedForDataClasses`.`Person`>.mutate(
    mutation: SetMutator<`CodeGeneratedForDataClasses`.`Person`>.() -> Unit,
): Set<`CodeGeneratedForDataClasses`.`Person`> = mutator().apply(mutation).get()

@MutatorDsl
inline var Mutator<`CodeGeneratedForDataClasses`.`Person`>.`name`
    get() = get().`name`
    set(v) = modifyIfChanged(get().`name`, v) { it.copy(`name` = v) }

@MutatorDsl
inline var Mutator<`CodeGeneratedForDataClasses`.`Person`>.`age`
    get() = get().`age`
    set(v) = modifyIfChanged(get().`age`, v) { it.copy(`age` = v) }

@MutatorDsl
inline val Mutator<`CodeGeneratedForDataClasses`.`Person`>.`address`
    get() = get().`address`?.mutator()
        ?.onChange { `address` -> modifyValue { get().copy(`address` = `address`) } }

```

Code generated for the Address class


```kotlin
@file:Suppress("RemoveRedundantBackticks", "unused", "NOTHING_TO_INLINE")

package de.peekandpoke.mutator.examples.code_generation

import de.peekandpoke.mutator.*

// Mutator for data class `CodeGeneratedForDataClasses`.`Address` (de.peekandpoke.mutator.examples.code_generation.CodeGeneratedForDataClasses.Address)
// Generated by the 'BuiltIn: Mutable Objects Plugin' plugin
@MutatorDsl
inline fun `CodeGeneratedForDataClasses`.`Address`.mutator() = ObjectMutator(this)

@MutatorDsl
inline fun `CodeGeneratedForDataClasses`.`Address`.mutate(
    mutation: ObjectMutator<`CodeGeneratedForDataClasses`.`Address`>.() -> Unit,
): `CodeGeneratedForDataClasses`.`Address` = mutator().apply(mutation).get()

@MutatorDsl
inline fun List<`CodeGeneratedForDataClasses`.`Address`>.mutator() = mutator(child = { mutator() })

@MutatorDsl
inline fun List<`CodeGeneratedForDataClasses`.`Address`>.mutate(
    mutation: ListMutator<`CodeGeneratedForDataClasses`.`Address`>.() -> Unit,
): List<`CodeGeneratedForDataClasses`.`Address`> = mutator().apply(mutation).get()

@MutatorDsl
inline fun Set<`CodeGeneratedForDataClasses`.`Address`>.mutator() = mutator(child = { mutator() })

@MutatorDsl
inline fun Set<`CodeGeneratedForDataClasses`.`Address`>.mutate(
    mutation: SetMutator<`CodeGeneratedForDataClasses`.`Address`>.() -> Unit,
): Set<`CodeGeneratedForDataClasses`.`Address`> = mutator().apply(mutation).get()

@MutatorDsl
inline var Mutator<`CodeGeneratedForDataClasses`.`Address`>.`street`
    get() = get().`street`
    set(v) = modifyIfChanged(get().`street`, v) { it.copy(`street` = v) }

@MutatorDsl
inline var Mutator<`CodeGeneratedForDataClasses`.`Address`>.`city`
    get() = get().`city`
    set(v) = modifyIfChanged(get().`city`, v) { it.copy(`city` = v) }

```

Code generated for the AddressBook class


```kotlin
@file:Suppress("RemoveRedundantBackticks", "unused", "NOTHING_TO_INLINE")

package de.peekandpoke.mutator.examples.code_generation

import de.peekandpoke.mutator.*

// Mutator for data class `CodeGeneratedForDataClasses`.`AddressBook` (de.peekandpoke.mutator.examples.code_generation.CodeGeneratedForDataClasses.AddressBook)
// Generated by the 'BuiltIn: Mutable Objects Plugin' plugin
@MutatorDsl
inline fun `CodeGeneratedForDataClasses`.`AddressBook`.mutator() = ObjectMutator(this)

@MutatorDsl
inline fun `CodeGeneratedForDataClasses`.`AddressBook`.mutate(
    mutation: ObjectMutator<`CodeGeneratedForDataClasses`.`AddressBook`>.() -> Unit,
): `CodeGeneratedForDataClasses`.`AddressBook` = mutator().apply(mutation).get()

@MutatorDsl
inline fun List<`CodeGeneratedForDataClasses`.`AddressBook`>.mutator() = mutator(child = { mutator() })

@MutatorDsl
inline fun List<`CodeGeneratedForDataClasses`.`AddressBook`>.mutate(
    mutation: ListMutator<`CodeGeneratedForDataClasses`.`AddressBook`>.() -> Unit,
): List<`CodeGeneratedForDataClasses`.`AddressBook`> = mutator().apply(mutation).get()

@MutatorDsl
inline fun Set<`CodeGeneratedForDataClasses`.`AddressBook`>.mutator() = mutator(child = { mutator() })

@MutatorDsl
inline fun Set<`CodeGeneratedForDataClasses`.`AddressBook`>.mutate(
    mutation: SetMutator<`CodeGeneratedForDataClasses`.`AddressBook`>.() -> Unit,
): Set<`CodeGeneratedForDataClasses`.`AddressBook`> = mutator().apply(mutation).get()

@MutatorDsl
inline val Mutator<`CodeGeneratedForDataClasses`.`AddressBook`>.`persons`
    get() = get().`persons`.mutator()
        .onChange { `persons` -> modifyValue { get().copy(`persons` = `persons`) } }

```
    

