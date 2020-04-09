package de.peekandpoke.ultra.mutator.examples.introduction

import de.peekandpoke.ultra.common.docs.SimpleExample
import de.peekandpoke.ultra.mutator.Mutable

@Mutable
class SimpleButComplexEnoughExample : SimpleExample() {

    override val title = "A simple but complex enough example"

    override val description = """
        Imagine you have some nested immutable data structure with
        - data classes
        - lists of objects
        - etc.
        
        You like immutability for all the benefits is brings.  
        But you also hate the amount of code you have to write to change a deeply nested value inside these kinds
        of structures.
        
        Well, here is a solution:
    """.trimIndent()

    // !BEGIN! //

    // Let's say we have a complex, nested and immutable domain model.
    @Mutable
    data class Company(val boss: Person, val employees: List<Employee>)

    @Mutable
    data class Employee(val person: Person, val address: Address, val salary: Salary)

    @Mutable
    data class Person(val name: String, val age: Int)

    @Mutable
    data class Address(val city: String)

    @Mutable
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
        println("Is 'Jerry' still the same:" + (company.employees[1] === updated.employees[1]))
        println()

        // !END! //
    }
}

