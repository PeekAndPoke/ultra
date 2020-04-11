package de.peekandpoke.ultra.mutator.examples.introduction

import de.peekandpoke.ultra.common.docs.SimpleExample
import de.peekandpoke.ultra.mutator.Mutable

@Mutable
class MoreComplexExample : SimpleExample() {

    override val title = "A more complex example"

    override val description = """
        Imagine we have some nested immutable data structure with
        - data classes
        - lists of objects
        - etc.

        This example show how to easily mutate data structure of any depth.   
        There is no need for things like:
        - nested copy(copy(copy(...)))
        - or complicated list manipulations

        This example also shows, that we only need to put the @Mutable annotation on the a top level type.  
        The annotation processor / code generator will recursively pick up all types that are referenced / used
        by the top level type.
        
    """.trimIndent()

    // !BEGIN! //

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

    // !END! //

    override fun run() {
        // !BEGIN! //

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

        // !END! //
    }
}

