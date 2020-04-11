# Examples for ultra::mutator

## TOC

1. [Introduction](#introduction)

    1. [A simple example](#a-simple-example)
    2. [A more complex example](#a-more-complex-example)
    3. [Empty Mutation](#empty-mutation)
    4. [Mutating an object without changing it](#mutating-an-object-without-changing-it)

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

This example shows what happens when we apply a empty mutation.

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

This example shows what happens when we we mutate an object without changing any of it's fields.

When we mutate an object and re-assign fields with the exact same values, this is as if no mutation has
occurred. This means:
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
