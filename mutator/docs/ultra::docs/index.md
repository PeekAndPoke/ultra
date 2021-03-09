# Examples for ultra::mutator

## TOC

1. [Introduction](#introduction)

    1. [A simple example](#a-simple-example)
    2. [A more complex example](#a-more-complex-example)
    3. [Empty Mutation](#empty-mutation)
    4. [Mutating an object without changing it](#mutating-an-object-without-changing-it)

2. [Data Class Mutation](#data-class-mutation)

    1. [Scalar and String properties](#scalar-and-string-properties)
    2. [Nullable Scalar and String properties](#nullable-scalar-and-string-properties)
    3. ['Any' properties and unknown types](#'any'-properties-and-unknown-types)

## Introduction

### A simple example

Let us start with a very simple example.

(see the full [example](../../src/examples/introduction/SimpleExample.kt))

```kotlin
// Let's say we have a data class that has some fields.
// We also annotate the type, so the mutator annotation processing will generate some code for us.
@Mutable
data class Person(val name: String, val age: Int)

// We create an instance of our data class.
val angelina = Person("Angelina", 35)

// Through annotation processing we get the "mutate" extension method created.
// The result of a mutation is a new object, while keeping the old one as it is.
val olderAngelina = angelina.mutate {
    // Inside of the callback we can directly manipulate properties of the object
    age += 1
}

// Angelina did not change.
println("The original:")
println(angelina)
// But we got another version with increased age.
println("The new version with modified age:")
println(olderAngelina)

// We could also change the name or both.
val noMoreAngelina = angelina.mutate {
    age = 47
    name = "Brad"
}

// And so we got another mutation of the original object.
println("The renamed version:")
println(noMoreAngelina)
```
Will output:
```
The original:
Person(name=Angelina, age=35)
The new version with modified age:
Person(name=Angelina, age=36)
The renamed version:
Person(name=Brad, age=47)
```

### A more complex example

Imagine we have some nested immutable data structure with
- data classes
- lists of objects
- etc.

This example shows how to easily mutate data structures of any depth.   
There is no need for things like:
- nested copy(copy(copy(...)))
- or complicated list manipulations

This example also shows, that we only need to put the @Mutable annotation on the a top level type.  
The annotation processor / code generator will recursively pick up all types that are referenced / used
by the top level type.


(see the full [example](../../src/examples/introduction/MoreComplexExample.kt))

```kotlin
// Let's say we have a complex, nested and immutable domain model.
// We annotate our top level type with the @Mutable annotation.
@Mutable
data class Company(val boss: Person, val employees: List<Employee>)

// All the other types are recursively references by the top level type.
// So we do not need to annotate them with @Mutable (we still can, but it makes not difference)
data class Employee(val person: Person, val address: Address, val salary: Salary)

data class Person(val name: String, val age: Int)

data class Address(val city: String)

data class Salary(val currency: String, val amount: Float)

// Let's create an instance of our top level domain object
val company = Company(
    boss = Person("Maximus Grandiosus", 71),
    employees = listOf(
        Employee(
            person = Person("Tom", 35),
            address = Address("Boston"),
            salary = Salary("USD", 20_000f)
        ),
        Employee(
            person = Person("Jerry", 35),
            address = Address("New York"),
            salary = Salary("USD", 17_000f)
        )
        // There could be a lot more employees...
    )
)

// Now let's give all people below 40 in Boston a raise of 15%
val updated = company.mutate {
    employees
        .filter { it.person.age < 40 }
        .filter { it.address.city == "Boston" }
        .forEach {
            // Notice how we directly manipulate the field again:
            it.salary.amount *= 1.15f
        }
}

println("Initial company")
println(company)
println()

println("Updated company")
println(updated)
println()

println("'Tom' before the raise")
println(company.employees[0])
println("'Tom' after his raise")
println(updated.employees[0])
println("Is 'Tom' still the same: " + (company.employees[0] === updated.employees[0]))
println()

println("'Jerry' before the raise")
println(company.employees[1])
println("'Jerry' after the raise - he did not get one")
println(updated.employees[1])
println("Is 'Jerry' still the same: " + (company.employees[1] === updated.employees[1]))
println()
```
Will output:
```
Initial company
Company(boss=Person(name=Maximus Grandiosus, age=71), employees=[Employee(person=Person(name=Tom, age=35), address=Address(city=Boston), salary=Salary(currency=USD, amount=20000.0)), Employee(person=Person(name=Jerry, age=35), address=Address(city=New York), salary=Salary(currency=USD, amount=17000.0))])

Updated company
Company(boss=Person(name=Maximus Grandiosus, age=71), employees=[Employee(person=Person(name=Tom, age=35), address=Address(city=Boston), salary=Salary(currency=USD, amount=23000.0)), Employee(person=Person(name=Jerry, age=35), address=Address(city=New York), salary=Salary(currency=USD, amount=17000.0))])

'Tom' before the raise
Employee(person=Person(name=Tom, age=35), address=Address(city=Boston), salary=Salary(currency=USD, amount=20000.0))
'Tom' after his raise
Employee(person=Person(name=Tom, age=35), address=Address(city=Boston), salary=Salary(currency=USD, amount=23000.0))
Is 'Tom' still the same: false

'Jerry' before the raise
Employee(person=Person(name=Jerry, age=35), address=Address(city=New York), salary=Salary(currency=USD, amount=17000.0))
'Jerry' after the raise - he did not get one
Employee(person=Person(name=Jerry, age=35), address=Address(city=New York), salary=Salary(currency=USD, amount=17000.0))
Is 'Jerry' still the same: true
```

### Empty Mutation

This example shows what happens when we apply an empty mutation.

When we call **mutate** on an object without changing anything, the result will be 
the exact same object.

(see the full [example](../../src/examples/introduction/EmptyMutationExample.kt))

```kotlin
@Mutable
data class Person(val name: String, val age: Int)

val before = Person("Angelina", 35)

// We call mutate but we do not do anything
val after = before.mutate {
}

println("Before:")
println(before)
println()

println("After:")
println(after)
println()

println("It is the exact same object (before === after): " + (before === after))
```
Will output:
```
Before:
Person(name=Angelina, age=35)

After:
Person(name=Angelina, age=35)

It is the exact same object (before === after): true
```

### Mutating an object without changing it

This example shows what happens when we mutate an object without changing any of it's fields.

Re-assign fields with the exact same values, behaves as if no mutation is done. This means:
- the result will be the original object.
- there is no new object created.

The same behavior is implemented for other kinds of mutations as well, like:
- List mutation
- Map mutation
- ...

Only real changes lead to the creation of object copies.

(see the full [example](../../src/examples/introduction/MutationWithoutChangeExample.kt))

```kotlin
@Mutable
data class Person(val name: String, val age: Int)

val before = Person("Angelina", 35)

// We call mutate but we do not do anything
val after = before.mutate {
    name = "Angelina"
    age = 35
}

println("Before:")
println(before)
println()

println("After:")
println(after)
println()

println("It is the exact same object (before === after): " + (before === after))
```
Will output:
```
Before:
Person(name=Angelina, age=35)

After:
Person(name=Angelina, age=35)

It is the exact same object (before === after): true
```

## Data Class Mutation

**ultra::mutator** supports the mutation of data classes.

This chapter shows how to mutate immutable data classes and nested data classes.

Notice that inside all the mutate { } closures, we are not working on our objects directly. We rather work on
wrappers. The code for these wrappers is generated for us, as we put the @Mutable annotation on our classes.

### Scalar and String properties

This examples shows how we can mutate scalar (Int, Boolean, Float, ...) and String properties.

Notice that inside of the mutate { } closure, we are not working on our objects itself, but rather on
a wrapper. The code for this wrapper is generated for us, as we put the @Mutable annotation on our class.

(see the full [example](../../src/examples/dataclasses/ScalarAndStringPropertiesExample.kt))

```kotlin
// Here is out data class
@Mutable
data class ExampleClassWithScalars(
    val anInt: Int,
    val aFloat: Float,
    val aBoolean: Boolean,
    val aString: String
)

// We create an instance
val original = ExampleClassWithScalars(
    anInt = 10,
    aFloat = 3.14f,
    aBoolean = false,
    aString = "Hello World!"
)

// Now we can mutate our object
val result = original.mutate {
    anInt += 1
    aFloat = 4.669f
    aBoolean = !aBoolean
    aString = "Hey, $aString"
}

println("The original:")
println(original)

println("The result:")
println(result)
```
Will output:
```
The original:
ExampleClassWithScalars(anInt=10, aFloat=3.14, aBoolean=false, aString=Hello World!)
The result:
ExampleClassWithScalars(anInt=11, aFloat=4.669, aBoolean=true, aString=Hey, Hello World!)
```

The generated mutator code for our data class looks like this:
```kotlin
@file:Suppress("UNUSED_ANONYMOUS_PARAMETER")

package de.peekandpoke.ultra.mutator.examples.dataclasses

import de.peekandpoke.ultra.mutator.*


@JvmName("mutateExampleClassWithScalarsMutator")
fun ExampleClassWithScalars.mutate(mutation: ExampleClassWithScalarsMutator.() -> Unit) = 
    mutator({ x: ExampleClassWithScalars -> Unit }).apply(mutation).getResult()

@JvmName("mutatorExampleClassWithScalarsMutator")
fun ExampleClassWithScalars.mutator(onModify: OnModify<ExampleClassWithScalars> = {}) = 
    ExampleClassWithScalarsMutator(this, onModify)

class ExampleClassWithScalarsMutator(
    target: ExampleClassWithScalars, 
    onModify: OnModify<ExampleClassWithScalars> = {}
) : DataClassMutator<ExampleClassWithScalars>(target, onModify) {

    /**
     * Mutator for field [ExampleClassWithScalars.anInt]
     *
     * Info:
     *   - type:         [Int]
     *   - reflected by: [com.squareup.kotlinpoet.ClassName]
     */ 
    var anInt
        get() = getResult().anInt
        set(v) = modify(getResult()::anInt, getResult().anInt, v)

    /**
     * Mutator for field [ExampleClassWithScalars.aFloat]
     *
     * Info:
     *   - type:         [Float]
     *   - reflected by: [com.squareup.kotlinpoet.ClassName]
     */ 
    var aFloat
        get() = getResult().aFloat
        set(v) = modify(getResult()::aFloat, getResult().aFloat, v)

    /**
     * Mutator for field [ExampleClassWithScalars.aBoolean]
     *
     * Info:
     *   - type:         [Boolean]
     *   - reflected by: [com.squareup.kotlinpoet.ClassName]
     */ 
    var aBoolean
        get() = getResult().aBoolean
        set(v) = modify(getResult()::aBoolean, getResult().aBoolean, v)

    /**
     * Mutator for field [ExampleClassWithScalars.aString]
     *
     * Info:
     *   - type:         [String]
     *   - reflected by: [com.squareup.kotlinpoet.ClassName]
     */ 
    var aString
        get() = getResult().aString
        set(v) = modify(getResult()::aString, getResult().aString, v)

}

```
    

### Nullable Scalar and String properties

This examples shows how we can mutate nullable scalar (Int, Boolean, Float, ...) and String properties.

(see the full [example](../../src/examples/dataclasses/NullableScalarAndStringPropertiesExample.kt))

```kotlin
// Here is out data class
@Mutable
data class ExampleClassWithNullableScalars(
    val anInt: Int?,
    val aFloat: Float?,
    val aBoolean: Boolean?,
    val aString: String?
)

// We create an instance
val original = ExampleClassWithNullableScalars(
    anInt = 10,
    aFloat = 3.14f,
    aBoolean = false,
    aString = "Hello World!"
)

// Now we can mutate our object
val result = original.mutate {
    anInt = null
    aFloat = null
    aBoolean = aBoolean ?: true
    aString = null
}

println("The original:")
println(original)

println("The result:")
println(result)
```
Will output:
```
The original:
ExampleClassWithNullableScalars(anInt=10, aFloat=3.14, aBoolean=false, aString=Hello World!)
The result:
ExampleClassWithNullableScalars(anInt=null, aFloat=null, aBoolean=false, aString=null)
```

The generated mutator code for our data class looks like this:
```kotlin
@file:Suppress("UNUSED_ANONYMOUS_PARAMETER")

package de.peekandpoke.ultra.mutator.examples.dataclasses

import de.peekandpoke.ultra.mutator.*


@JvmName("mutateExampleClassWithNullableScalarsMutator")
fun ExampleClassWithNullableScalars.mutate(mutation: ExampleClassWithNullableScalarsMutator.() -> Unit) = 
    mutator({ x: ExampleClassWithNullableScalars -> Unit }).apply(mutation).getResult()

@JvmName("mutatorExampleClassWithNullableScalarsMutator")
fun ExampleClassWithNullableScalars.mutator(onModify: OnModify<ExampleClassWithNullableScalars> = {}) = 
    ExampleClassWithNullableScalarsMutator(this, onModify)

class ExampleClassWithNullableScalarsMutator(
    target: ExampleClassWithNullableScalars, 
    onModify: OnModify<ExampleClassWithNullableScalars> = {}
) : DataClassMutator<ExampleClassWithNullableScalars>(target, onModify) {

    /**
     * Mutator for field [ExampleClassWithNullableScalars.anInt]
     *
     * Info:
     *   - type:         [Int]
     *   - reflected by: [com.squareup.kotlinpoet.ClassName]
     */ 
    var anInt
        get() = getResult().anInt
        set(v) = modify(getResult()::anInt, getResult().anInt, v)

    /**
     * Mutator for field [ExampleClassWithNullableScalars.aFloat]
     *
     * Info:
     *   - type:         [Float]
     *   - reflected by: [com.squareup.kotlinpoet.ClassName]
     */ 
    var aFloat
        get() = getResult().aFloat
        set(v) = modify(getResult()::aFloat, getResult().aFloat, v)

    /**
     * Mutator for field [ExampleClassWithNullableScalars.aBoolean]
     *
     * Info:
     *   - type:         [Boolean]
     *   - reflected by: [com.squareup.kotlinpoet.ClassName]
     */ 
    var aBoolean
        get() = getResult().aBoolean
        set(v) = modify(getResult()::aBoolean, getResult().aBoolean, v)

    /**
     * Mutator for field [ExampleClassWithNullableScalars.aString]
     *
     * Info:
     *   - type:         [String]
     *   - reflected by: [com.squareup.kotlinpoet.ClassName]
     */ 
    var aString
        get() = getResult().aString
        set(v) = modify(getResult()::aString, getResult().aString, v)

}

```
    

### 'Any' properties and unknown types

This examples shows how we can mutate 'Any" properties.  
It also shows how mutations behave for types of properties that are unknown to Mutator.

In short, the behaviour is exactly the same as with scalar types.
 
When we have a property in your class of type 'Any" or 'Any?' we can read it and re-assign it.

When we have properties of types, for which Mutator does not support any special mutation mechanism, e.g. 
java.time.LocalDate, we can do the same: read the value and re-assign the value. 

(see the full [example](../../src/examples/dataclasses/AnyPropertiesExample.kt))

```kotlin
// Here is out data class
@Mutable
data class ExampleClassWithAny(
    val any: Any,
    val nullableAny: Any?,
    val aDate: LocalDate
)

// We create an instance
val original = ExampleClassWithAny(
    any = "any",
    nullableAny = null,
    aDate = LocalDate.now()
)

// Now we can mutate our object
val result = original.mutate {
    any = 10
    nullableAny = 4.669f
    aDate = aDate.plusDays(1)
}

println("The original:")
println(original)

println("The result:")
println(result)
```
Will output:
```
The original:
ExampleClassWithAny(any=any, nullableAny=null, aDate=2021-03-09)
The result:
ExampleClassWithAny(any=10, nullableAny=4.669, aDate=2021-03-10)
```

The generated mutator code for our data class looks like this:
```kotlin
@file:Suppress("UNUSED_ANONYMOUS_PARAMETER")

package de.peekandpoke.ultra.mutator.examples.dataclasses

import de.peekandpoke.ultra.mutator.*
import java.time.LocalDate


@JvmName("mutateExampleClassWithAnyMutator")
fun ExampleClassWithAny.mutate(mutation: ExampleClassWithAnyMutator.() -> Unit) = 
    mutator({ x: ExampleClassWithAny -> Unit }).apply(mutation).getResult()

@JvmName("mutatorExampleClassWithAnyMutator")
fun ExampleClassWithAny.mutator(onModify: OnModify<ExampleClassWithAny> = {}) = 
    ExampleClassWithAnyMutator(this, onModify)

class ExampleClassWithAnyMutator(
    target: ExampleClassWithAny, 
    onModify: OnModify<ExampleClassWithAny> = {}
) : DataClassMutator<ExampleClassWithAny>(target, onModify) {

    /**
     * Mutator for field [ExampleClassWithAny.any]
     *
     * Info:
     *   - type:         [Any]
     *   - reflected by: [com.squareup.kotlinpoet.ClassName]
     */ 
    var any
        get() = getResult().any
        set(v) = modify(getResult()::any, getResult().any, v)

    /**
     * Mutator for field [ExampleClassWithAny.nullableAny]
     *
     * Info:
     *   - type:         [Any]
     *   - reflected by: [com.squareup.kotlinpoet.ClassName]
     */ 
    var nullableAny
        get() = getResult().nullableAny
        set(v) = modify(getResult()::nullableAny, getResult().nullableAny, v)

    /**
     * Mutator for field [ExampleClassWithAny.aDate]
     *
     * Info:
     *   - type:         [LocalDate]
     *   - reflected by: [com.squareup.kotlinpoet.ClassName]
     */ 
    var aDate
        get() = getResult().aDate
        set(v) = modify(getResult()::aDate, getResult().aDate, v)

}

```
    

